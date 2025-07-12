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
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Dropout, BatchNormalization, Input
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.callbacks import EarlyStopping

# === Config ===
input_dir = "./computervision_test/videos"
output_data_dir = "./computervision_test/data"
os.makedirs(output_data_dir, exist_ok=True)

# Load pre-trained XGBoost model and label encoder
model_path = "./computervision_test/model/xgb_model.pkl"
label_path = "./computervision_test/model/xgb_label_encoder.pkl"
feature_path = "./computervision_test/model/xgb_features.json"
model = joblib.load(model_path)
label_encoder = joblib.load(label_path)
with open(feature_path, "r") as jf:
    expected_cols = json.load(jf)

# === MediaPipe Pose Setup ===
mp_pose = mp.solutions.pose
mp_drawing = mp.solutions.drawing_utils
pose = mp_pose.Pose(static_image_mode=False, model_complexity=1, enable_segmentation=True)
fps_default = 30

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
        if preds[i] == expected:
            count += 1
            if count >= min_frames and current_phase < len(phase_order) - 1:
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

    # Dummy knee range and sway calculation (customize as needed)
    left_knee_range = right_knee_range = upper_body_sway = 0.0

    gait_features = {
        'step_count': step_count,
        'mean_step_length': mean_step_length,
        'stride_time': stride_time,
        'cadence': cadence,
        'step_symmetry': symmetry,
        'left_knee_range': left_knee_range,
        'right_knee_range': right_knee_range,
        'upper_body_sway': upper_body_sway,
        'turn1_duration': len(turn1_df) / fps,
        'turn2_duration': len(turn2_df) / fps,
        'total_time': len(df) / fps
    }
    return gait_features

def severity_label_from_features(features):
    total_time = features.get('total_time', 0)
    turning_time = features.get('turn1_duration', 0) + features.get('turn2_duration', 0)
    walking_time = features.get('turn1_duration', 0) + features.get('turn2_duration', 0)
    turn_walk_ratio = turning_time / (walking_time if walking_time > 0 else 1)
    # Simple rule-based severity
    if total_time <= 7 and turn_walk_ratio < 1.0:
        return 'Normal'
    elif total_time <= 13:
        if turn_walk_ratio < 1.0:
            return 'Slight'
        else:
            return 'Mild'
    else:
        if turn_walk_ratio > 1.0:
            return 'Severe'
        else:
            return 'Moderate'

# Step 1-3: Process videos, extract keypoints, classify subtasks, compute gait features
all_gait_features = []
for filename in os.listdir(input_dir):
    if not filename.endswith(".mp4"):
        continue
    print(f"\nProcessing: {filename}")
    cap = cv2.VideoCapture(os.path.join(input_dir, filename))
    fps = cap.get(cv2.CAP_PROP_FPS) or fps_default
    keypoints_data, frame_id = [], 0
    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break
        frame = cv2.resize(frame, (640, 480))
        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        result = pose.process(rgb)
        if result.pose_landmarks:
            for i, lm in enumerate(result.pose_landmarks.landmark):
                keypoints_data.append([frame_id, i, lm.x, lm.y, lm.z, lm.visibility])
        frame_id += 1
    cap.release()

    # Convert keypoints to DataFrame
    df = pd.DataFrame(keypoints_data, columns=["frame", "id", "x", "y", "z", "visibility"])
    df_pivot = df.pivot(index='frame', columns='id', values=['x', 'y', 'z', 'visibility'])
    df_pivot.columns = [f'{coord}_{kpt}' for coord, kpt in df_pivot.columns]
    df_pivot.reset_index(inplace=True)
    frame_col = df_pivot['frame']
    df_features_only = df_pivot.reindex(columns=expected_cols, fill_value=0)
    X = df_features_only.fillna(0).values

    # Subtask classification
    raw_preds = model.predict(X)
    decoded_preds = label_encoder.inverse_transform(raw_preds)
    smoothed_preds = smooth_with_window(decoded_preds)
    forced_labels = force_phase_sequence(smoothed_preds)
    df_features_only.insert(0, 'frame', frame_col)
    df_features_only['tug_subtask'] = forced_labels

    # Save per-frame CSV
    csv_path = os.path.join(output_data_dir, f"{filename}_labelled.csv")
    df_features_only.to_csv(csv_path, index=False)
    print(f"Saved: {csv_path}")

    # Extract gait features
    gait_metrics = extract_gait_features(df_features_only, fps)
    gait_metrics['video'] = filename
    all_gait_features.append(gait_metrics)

# Step 4: Train ML model for severity classification
gait_df = pd.DataFrame(all_gait_features)
gait_df['severity'] = gait_df.apply(severity_label_from_features, axis=1)
gait_df.to_csv(os.path.join(output_data_dir, "gait_features.csv"), index=False)
print("Saved gait features with severity labels.")

# Prepare data for ML
feature_cols = [c for c in gait_df.columns if c not in ['video', 'severity']]
X = gait_df[feature_cols].fillna(0).values
y = gait_df['severity'].values
label_enc = LabelEncoder()
y_encoded = label_enc.fit_transform(y)
y_categorical = to_categorical(y_encoded, num_classes=len(label_enc.classes_))

# Simple train/test split
from sklearn.model_selection import train_test_split
X_train, X_test, y_train, y_test = train_test_split(X, y_categorical, test_size=0.2, random_state=42)

# Build simple MLP model
model_severity = Sequential([
    Input(shape=(X.shape[1],)),
    Dense(64, activation='relu'),
    BatchNormalization(),
    Dropout(0.3),
    Dense(32, activation='relu'),
    BatchNormalization(),
    Dropout(0.3),
    Dense(len(label_enc.classes_), activation='softmax')
])
model_severity.compile(optimizer=Adam(learning_rate=0.001), loss='categorical_crossentropy', metrics=['accuracy'])

# Train model
callbacks = [EarlyStopping(monitor='val_loss', patience=10, restore_best_weights=True)]
history = model_severity.fit(X_train, y_train, validation_data=(X_test, y_test), epochs=50, batch_size=8, callbacks=callbacks, verbose=1)

# Evaluate
loss, acc = model_severity.evaluate(X_test, y_test, verbose=0)
print(f"Severity classification accuracy: {acc:.3f}")

# Save model and label encoder
model_severity.save(os.path.join(output_data_dir, "severity_mlp_model.h5"))
joblib.dump(label_enc, os.path.join(output_data_dir, "severity_label_encoder.pkl"))
with open(os.path.join(output_data_dir, "severity_features.json"), "w") as f:
    json.dump(feature_cols, f)
print("Saved severity classification model and metadata.")