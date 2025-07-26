import os
import cv2
import json
import joblib
import numpy as np
import pandas as pd
import mediapipe as mp
from scipy.stats import mode
from sklearn.preprocessing import LabelEncoder
from scipy.signal import find_peaks

# === Config ===
input_dir = "./computervision/videos"
output_video_dir = "./computervision/video_results"
output_csv_dir = "./computervision/keypoints_and_durations"
model_path = "./computervision/xgb_model.pkl"
label_path = "./computervision/xgb_label_encoder.pkl"
feature_path = "./computervision/xgb_features.json"

os.makedirs(output_video_dir, exist_ok=True)
os.makedirs(output_csv_dir, exist_ok=True)

fps_default = 30
model = joblib.load(model_path)
label_encoder = joblib.load(label_path)
with open(feature_path, "r") as jf:
    expected_cols = json.load(jf)

# === MediaPipe Pose Setup ===
mp_pose = mp.solutions.pose
mp_drawing = mp.solutions.drawing_utils
pose = mp_pose.Pose(static_image_mode=False, model_complexity=1, enable_segmentation=True)

def smooth_with_window(preds, window=7):
    le = LabelEncoder()
    int_preds = le.fit_transform(preds)
    pad = window // 2
    padded = [int_preds[0]] * pad + list(int_preds) + [int_preds[-1]] * pad
    smoothed = [int(mode(padded[i - pad:i + pad + 1]).mode) for i in range(pad, len(int_preds) + pad)]
    return le.inverse_transform(smoothed)

def force_phase_sequence(preds, phase_order=None, min_frames=5):
    if phase_order is None:
        phase_order = [
            'Sit-To-Stand', 'Walk-From-Chair', 'Turn-First',
            'Walk-To-Chair', 'Turn-Second', 'Stand-To-Sit'
        ]
    result = [''] * len(preds)
    current_phase, count = 0, 0
    for i in range(len(preds)):
        if current_phase >= len(phase_order):
            result[i] = phase_order[-1]
            continue
        expected = phase_order[current_phase]
        result[i] = expected
        if i + 5 < len(preds):
            window = preds[i:i+5]
            if list(window).count(phase_order[min(current_phase + 1, len(phase_order)-1)]) >= 3:
                count += 1
                if count >= min_frames:
                    current_phase += 1
                    count = 0
            else:
                count = 0
    return result

def extract_gait_features(df, fps):
    walk_df = df[df['tug_subtask'].isin(['Walk-From-Chair', 'Walk-To-Chair'])].copy()
    turn1_df = df[df['tug_subtask'] == 'Turn-First']
    turn2_df = df[df['tug_subtask'] == 'Turn-Second']

    walk_df['ankle_distance'] = np.abs(walk_df['x_27'] - walk_df['x_28'])
    peaks, _ = find_peaks(walk_df['ankle_distance'], distance=10)
    step_lengths = walk_df.iloc[peaks]['ankle_distance'].values
    step_times = np.array(peaks) / fps if len(peaks) > 0 else []

    step_count = len(peaks)
    step_durations = np.diff(step_times) if step_count >= 2 else [0]
    stride_time = np.mean(step_durations) * 2 if len(step_durations) > 1 else 0
    cadence = (step_count / (len(walk_df) / fps)) * 60 if len(walk_df) > 0 else 0
    mean_step_length = np.mean(step_lengths) if step_count > 0 else 0

    if step_count >= 4:
        left_steps = step_lengths[::2]
        right_steps = step_lengths[1::2]
        symmetry = np.abs(np.mean(left_steps) - np.mean(right_steps)) / max(np.mean([*left_steps, *right_steps]), 1e-6)
    else:
        symmetry = np.nan

    def joint_angle(a, b, c):
        ba = a - b
        bc = c - b
        cos_angle = np.clip(np.dot(ba, bc) / (np.linalg.norm(ba) * np.linalg.norm(bc) + 1e-6), -1.0, 1.0)
        return np.degrees(np.arccos(cos_angle))

    def compute_knee_range(df, side='LEFT'):
        hip = df[[f'x_{23 if side=="LEFT" else 24}', f'y_{23 if side=="LEFT" else 24}']].values
        knee = df[[f'x_{25 if side=="LEFT" else 26}', f'y_{25 if side=="LEFT" else 26}']].values
        ankle = df[[f'x_{27 if side=="LEFT" else 28}', f'y_{27 if side=="LEFT" else 28}']].values
        angles = [joint_angle(hip[i], knee[i], ankle[i]) for i in range(len(df))]
        return max(angles) - min(angles) if angles else 0

    left_knee = compute_knee_range(walk_df, 'LEFT')
    right_knee = compute_knee_range(walk_df, 'RIGHT')

    if 'x_11' in walk_df and 'x_12' in walk_df:
        shoulders = (walk_df['x_11'] + walk_df['x_12']) / 2
        sway = np.std(shoulders)
    else:
        sway = np.nan

    return {
        'step_count': step_count,
        'mean_step_length': mean_step_length,
        'stride_time': stride_time,
        'cadence': cadence,
        'step_symmetry': symmetry,
        'left_knee_range': left_knee,
        'right_knee_range': right_knee,
        'upper_body_sway': sway,
        'turn1_duration': len(turn1_df) / fps,
        'turn2_duration': len(turn2_df) / fps
    }

# === Main Video Loop ===
for filename in os.listdir(input_dir):
    if not filename.endswith(".mp4"):
        continue

    print(f"\n🎥 Processing: {filename}")
    cap = cv2.VideoCapture(os.path.join(input_dir, filename))
    fps = cap.get(cv2.CAP_PROP_FPS) or fps_default

    out_video = cv2.VideoWriter(
        os.path.join(output_video_dir, filename),
        cv2.VideoWriter_fourcc(*'mp4v'), fps, (640, 480)
    )

    keypoints_data, frame_id = [], 0
    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break
        frame = cv2.resize(frame, (640, 480))
        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        result = pose.process(rgb)
        if result.pose_landmarks:
            mp_drawing.draw_landmarks(frame, result.pose_landmarks, mp_pose.POSE_CONNECTIONS)
            for i, lm in enumerate(result.pose_landmarks.landmark):
                keypoints_data.append([frame_id, i, lm.x, lm.y, lm.z, lm.visibility])
        out_video.write(frame)
        frame_id += 1

    cap.release()
    out_video.release()

    try:
        df = pd.DataFrame(keypoints_data, columns=["frame", "id", "x", "y", "z", "visibility"])
        df_pivot = df.pivot(index='frame', columns='id', values=['x', 'y', 'z', 'visibility'])
        df_pivot.columns = [f'{coord}_{kpt}' for coord, kpt in df_pivot.columns]
        df_pivot.reset_index(inplace=True)

        frame_col = df_pivot['frame']
        df_features_only = df_pivot.reindex(columns=expected_cols, fill_value=0)

        X = df_features_only.fillna(0).values
        if X.shape[1] != len(expected_cols):
            raise ValueError(f"Feature shape mismatch: expected {len(expected_cols)}, got {X.shape[1]}")

        raw_preds = model.predict(X)
        decoded_preds = label_encoder.inverse_transform(raw_preds)
        smoothed_preds = smooth_with_window(decoded_preds)
        forced_labels = force_phase_sequence(smoothed_preds)

        df_features_only.insert(0, 'frame', frame_col)
        df_features_only['tug_subtask'] = forced_labels

        csv_path = os.path.join(output_csv_dir, f"{filename}_labelled.csv")
        df_features_only.to_csv(csv_path, index=False)
        print(f"Saved: {csv_path}")

        durations = df_features_only['tug_subtask'].value_counts().to_dict()
        durations_sec = {k: round(v / fps, 2) for k, v in durations.items()}
        with open(csv_path.replace("_labelled.csv", "_durations.json"), "w") as jf:
            json.dump(durations_sec, jf, indent=4)

        # === Extract gait features and append to master CSV ===
        gait_metrics = extract_gait_features(df_features_only, fps)
        gait_metrics['video'] = filename  # Add video reference

        gait_summary_path = os.path.join(output_csv_dir, "gait_features.csv")

        if os.path.exists(gait_summary_path):
            existing = pd.read_csv(gait_summary_path)
            # Overwrite if the video already exists
            existing = existing[existing['video'] != filename]
            updated = pd.concat([existing, pd.DataFrame([gait_metrics])], ignore_index=True)
        else:
            updated = pd.DataFrame([gait_metrics])

        updated.to_csv(gait_summary_path, index=False)
        print(f" Gait features updated in: {gait_summary_path}")


    except Exception as e:
        print(f"[ERROR] Failed processing {filename}: {e}")

print("\n✅ Finished all test videos.")
