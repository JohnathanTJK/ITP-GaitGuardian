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

def classify_severity(total_time, turn_walk_ratio, walk_straight_time, turn_time):
    """
    Classify TUG test severity based on total time and turn-to-walk ratio
    
    Args:
        total_time: Total time to complete TUG test in seconds
        turn_walk_ratio: Ratio of turning time to walking straight time
        walk_straight_time: Time spent walking straight in seconds
        turn_time: Time spent turning in seconds
    
    Returns:
        dict: Classification result with severity level and rationale
    """
    
    # Calculate some additional metrics for better classification
    has_walking_issues = walk_straight_time > 4.0  # Normal walking should be ~2-3 seconds
    has_turning_issues = turn_time > 4.0  # Normal turning should be ~2-3 seconds
    
    classification = {
        'severity_level': 'Normal',
        'severity_score': 0,
        'total_time': total_time,
        'turn_walk_ratio': turn_walk_ratio,
        'walk_straight_time': walk_straight_time,
        'turn_time': turn_time,
        'rationale': ''
    }
    
    if total_time <= 7.0:
        # Normal - healthy individuals
        classification['severity_level'] = 'Normal'
        classification['severity_score'] = 0
        classification['rationale'] = f"Completed in {total_time:.1f}s (≤7s), indicating normal mobility"
        
    elif total_time <= 13.0 and turn_walk_ratio < 1.0:
        # Slight - least severe
        classification['severity_level'] = 'Slight'
        classification['severity_score'] = 1
        classification['rationale'] = f"Completed in {total_time:.1f}s (≤13s) with turning ratio {turn_walk_ratio:.2f} (<1.0), indicating slight mobility issues"
        
    elif total_time <= 13.0 and 1.0 <= turn_walk_ratio <= 1.2:
        # Mild - slightly more severe, low risk of falling
        classification['severity_level'] = 'Mild'
        classification['severity_score'] = 2
        classification['rationale'] = f"Completed in {total_time:.1f}s (≤13s) with turning ratio {turn_walk_ratio:.2f} (≈1.0), indicating mild mobility issues with prolonged turning"
        
    elif total_time <= 13.0 and turn_walk_ratio > 1.2:
        # Moderate - turning takes significantly longer than walking
        classification['severity_level'] = 'Moderate'
        classification['severity_score'] = 3
        classification['rationale'] = f"Completed in {total_time:.1f}s (≤13s) but turning ratio {turn_walk_ratio:.2f} (>1.2), indicating moderate issues with turning"
        
    elif total_time > 13.0:
        # Severe - takes more than 13s
        if has_walking_issues and has_turning_issues:
            classification['severity_level'] = 'Severe'
            classification['severity_score'] = 4
            classification['rationale'] = f"Completed in {total_time:.1f}s (>13s) with issues in both walking ({walk_straight_time:.1f}s) and turning ({turn_time:.1f}s)"
        elif turn_walk_ratio > 1.0:
            classification['severity_level'] = 'Moderate'
            classification['severity_score'] = 3
            classification['rationale'] = f"Completed in {total_time:.1f}s (>13s) with turning ratio {turn_walk_ratio:.2f} (>1.0), indicating moderate issues primarily with turning"
        else:
            classification['severity_level'] = 'Severe'
            classification['severity_score'] = 4
            classification['rationale'] = f"Completed in {total_time:.1f}s (>13s), indicating severe mobility issues"
    
    return classification

def calculate_tug_metrics(df, fps):
    """
    Calculate TUG test metrics including timing and severity classification
    
    Args:
        df: DataFrame with pose keypoints and subtask labels
        fps: Frames per second of the video
    
    Returns:
        dict: TUG metrics including severity classification
    """
    
    # Calculate phase durations
    phase_durations = df['tug_subtask'].value_counts().to_dict()
    phase_durations_sec = {k: v / fps for k, v in phase_durations.items()}
    
    # Calculate specific metrics
    sit_to_stand_time = phase_durations_sec.get('Sit-To-Stand', 0)
    walk_from_chair_time = phase_durations_sec.get('Walk-From-Chair', 0)
    turn_first_time = phase_durations_sec.get('Turn-First', 0)
    walk_to_chair_time = phase_durations_sec.get('Walk-To-Chair', 0)
    turn_second_time = phase_durations_sec.get('Turn-Second', 0)
    stand_to_sit_time = phase_durations_sec.get('Stand-To-Sit', 0)
    
    # Calculate combined metrics
    total_walking_time = walk_from_chair_time + walk_to_chair_time
    total_turning_time = turn_first_time + turn_second_time
    total_time = sum(phase_durations_sec.values())
    
    # Calculate turn-to-walk ratio (avoid division by zero)
    turn_walk_ratio = total_turning_time / max(total_walking_time, 0.1)
    
    # Get severity classification
    severity_classification = classify_severity(
        total_time=total_time,
        turn_walk_ratio=turn_walk_ratio,
        walk_straight_time=total_walking_time,
        turn_time=total_turning_time
    )
    
    # Combine all metrics
    tug_metrics = {
        'total_time': round(total_time, 2),
        'sit_to_stand_time': round(sit_to_stand_time, 2),
        'walk_from_chair_time': round(walk_from_chair_time, 2),
        'turn_first_time': round(turn_first_time, 2),
        'walk_to_chair_time': round(walk_to_chair_time, 2),
        'turn_second_time': round(turn_second_time, 2),
        'stand_to_sit_time': round(stand_to_sit_time, 2),
        'total_walking_time': round(total_walking_time, 2),
        'total_turning_time': round(total_turning_time, 2),
        'turn_walk_ratio': round(turn_walk_ratio, 2),
        'severity_level': severity_classification['severity_level'],
        'severity_score': severity_classification['severity_score'],
        'severity_rationale': severity_classification['rationale']
    }
    
    return tug_metrics

def extract_gait_features(df, fps):
    """Enhanced gait feature extraction with TUG metrics and severity classification"""
    
    # Get TUG metrics first
    tug_metrics = calculate_tug_metrics(df, fps)
    
    # Original gait analysis
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

    # Combine original gait features with TUG metrics
    gait_features = {
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
    
    # Add TUG metrics
    gait_features.update(tug_metrics)
    
    return gait_features

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
        print(f"✅ Saved: {csv_path}")

        # Save detailed durations with severity classification
        gait_metrics = extract_gait_features(df_features_only, fps)
        gait_metrics['video'] = filename

        # Save detailed analysis
        analysis_path = csv_path.replace("_labelled.csv", "_analysis.json")
        with open(analysis_path, "w") as jf:
            json.dump(gait_metrics, jf, indent=4)
        print(f"🔍 Detailed analysis saved: {analysis_path}")

        # Print severity classification
        print(f"📊 Severity Classification: {gait_metrics['severity_level']} (Score: {gait_metrics['severity_score']})")
        print(f"💡 Rationale: {gait_metrics['severity_rationale']}")

        # Update master CSV with all metrics
        gait_summary_path = os.path.join(output_csv_dir, "gait_features_with_severity.csv")

        if os.path.exists(gait_summary_path):
            existing = pd.read_csv(gait_summary_path)
            existing = existing[existing['video'] != filename]
            updated = pd.concat([existing, pd.DataFrame([gait_metrics])], ignore_index=True)
        else:
            updated = pd.DataFrame([gait_metrics])

        updated.to_csv(gait_summary_path, index=False)
        print(f"🦶 Complete analysis updated in: {gait_summary_path}")

    except Exception as e:
        print(f"[ERROR] Failed processing {filename}: {e}")

print("\n✅ Finished all test videos with severity classification.")