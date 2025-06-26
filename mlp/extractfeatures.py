import json
import numpy as np
import pandas as pd
from collections import defaultdict
import os
from scipy.signal import find_peaks
from scipy.spatial.distance import euclidean

# --- CONFIG ---
SEQUENCE_LENGTH = 100
NUM_KEYPOINTS = 17
NUM_FEATURES = NUM_KEYPOINTS * 2
IMAGE_WIDTH = 640
IMAGE_HEIGHT = 480
EXCEL_PATH = "./mlp/label_parameters_abnormal.xlsx"
KEYPOINTS_JSON_PATH = "./mlp/keyPoints.json"

# --- Load labels ---
df = pd.read_excel(EXCEL_PATH, sheet_name="Machine automatic")
df["video_id"] = df["NO."].str.replace("_color.json", "", regex=False)
label_map = dict(zip(df["video_id"], df["Gait abnormal(golden)"]))

# --- Load keypoints ---
with open(KEYPOINTS_JSON_PATH, "r") as f:
    data = json.load(f)

imgnames = data["imgname"]
keypoints = data["part"]

# --- Group frames by video ID ---
video_frames = defaultdict(list)
for img, kps in zip(imgnames, keypoints):
    base = img.split("_down")[0].replace("_color", "")
    frame = np.array(kps).flatten()
    frame[0::2] /= IMAGE_WIDTH
    frame[1::2] /= IMAGE_HEIGHT
    video_frames[base].append(frame)

# Keypoint indices (based on COCO format)
KEYPOINT_INDICES = {
    'nose': 0, 'left_eye': 1, 'right_eye': 2, 'left_ear': 3, 'right_ear': 4,
    'left_shoulder': 5, 'right_shoulder': 6, 'left_elbow': 7, 'right_elbow': 8,
    'left_wrist': 9, 'right_wrist': 10, 'left_hip': 11, 'right_hip': 12,
    'left_knee': 13, 'right_knee': 14, 'left_ankle': 15, 'right_ankle': 16
}

def extract_gait_features(sequence):
    """Extract gait-specific features from keypoint sequence"""
    features = {}
    
    if len(sequence) < 5:  # Need minimum frames for analysis
        return {f: 0.0 for f in ['stride_length', 'cadence', 'step_width', 'walking_speed', 
                                'hip_movement', 'knee_movement', 'ankle_movement', 'symmetry']}
    
    # Extract key joint positions over time
    left_ankle = sequence[:, [KEYPOINT_INDICES['left_ankle']*2, KEYPOINT_INDICES['left_ankle']*2+1]]
    right_ankle = sequence[:, [KEYPOINT_INDICES['right_ankle']*2, KEYPOINT_INDICES['right_ankle']*2+1]]
    left_hip = sequence[:, [KEYPOINT_INDICES['left_hip']*2, KEYPOINT_INDICES['left_hip']*2+1]]
    right_hip = sequence[:, [KEYPOINT_INDICES['right_hip']*2, KEYPOINT_INDICES['right_hip']*2+1]]
    left_knee = sequence[:, [KEYPOINT_INDICES['left_knee']*2, KEYPOINT_INDICES['left_knee']*2+1]]
    right_knee = sequence[:, [KEYPOINT_INDICES['right_knee']*2, KEYPOINT_INDICES['right_knee']*2+1]]
    
    # 1. Stride Length (distance between consecutive heel strikes)
    left_ankle_x = left_ankle[:, 0]
    right_ankle_x = right_ankle[:, 0]
    
    # Find peaks in ankle movement (approximate heel strikes)
    left_peaks, _ = find_peaks(-left_ankle[:, 1], height=0.1, distance=5)  # heel strikes (lowest y)
    right_peaks, _ = find_peaks(-right_ankle[:, 1], height=0.1, distance=5)
    
    if len(left_peaks) > 1:
        left_strides = np.diff(left_ankle_x[left_peaks])
        features['stride_length'] = np.mean(np.abs(left_strides)) if len(left_strides) > 0 else 0.0
    else:
        features['stride_length'] = 0.0
    
    # 2. Cadence (steps per frame, normalized)
    total_steps = len(left_peaks) + len(right_peaks)
    features['cadence'] = total_steps / len(sequence) if len(sequence) > 0 else 0.0
    
    # 3. Step Width (lateral distance between feet)
    step_widths = np.abs(left_ankle[:, 0] - right_ankle[:, 0])
    features['step_width'] = np.mean(step_widths)
    
    # 4. Walking Speed (forward progression)
    center_x = (left_ankle[:, 0] + right_ankle[:, 0]) / 2
    if len(center_x) > 1:
        speed = np.diff(center_x)
        features['walking_speed'] = np.mean(np.abs(speed))
    else:
        features['walking_speed'] = 0.0
    
    # 5. Hip Movement Variability
    hip_center = (left_hip + right_hip) / 2
    hip_movement = np.std(hip_center, axis=0)
    features['hip_movement'] = np.mean(hip_movement)
    
    # 6. Knee Movement Variability
    knee_movement_left = np.std(left_knee, axis=0)
    knee_movement_right = np.std(right_knee, axis=0)
    features['knee_movement'] = np.mean([np.mean(knee_movement_left), np.mean(knee_movement_right)])
    
    # 7. Ankle Movement Variability
    ankle_movement_left = np.std(left_ankle, axis=0)
    ankle_movement_right = np.std(right_ankle, axis=0)
    features['ankle_movement'] = np.mean([np.mean(ankle_movement_left), np.mean(ankle_movement_right)])
    
    # 8. Gait Symmetry (left vs right similarity)
    left_ankle_range = np.ptp(left_ankle, axis=0)  # peak-to-peak
    right_ankle_range = np.ptp(right_ankle, axis=0)
    symmetry = 1 - np.mean(np.abs(left_ankle_range - right_ankle_range) / (left_ankle_range + right_ankle_range + 1e-8))
    features['symmetry'] = max(0, symmetry)  # Ensure non-negative
    
    return features

# --- Create feature vectors and labels ---
X = []
y = []
video_ids_mlp = []
feature_names = ['stride_length', 'cadence', 'step_width', 'walking_speed', 
                'hip_movement', 'knee_movement', 'ankle_movement', 'symmetry']

for vid, frames in video_frames.items():
    if vid not in label_map:
        continue
    
    sequence = np.array(frames)
    label = label_map[vid]
    
    # Pad or truncate sequence
    if len(sequence) > SEQUENCE_LENGTH:
        sequence = sequence[:SEQUENCE_LENGTH]
    elif len(sequence) < SEQUENCE_LENGTH:
        padding = np.zeros((SEQUENCE_LENGTH - len(sequence), NUM_FEATURES))
        sequence = np.vstack([sequence, padding])
    
    # Extract gait features
    gait_features = extract_gait_features(sequence)
    feature_vector = [gait_features[name] for name in feature_names]
    
    X.append(feature_vector)
    y.append(label)
    video_ids_mlp.append(vid)

X = np.array(X)
y = np.array(y)

# Check for data imbalance
unique, counts = np.unique(y, return_counts=True)
print(f"Class distribution: {dict(zip(unique, counts))}")
print(f"Class imbalance ratio: {counts[1]/counts[0] if len(counts) > 1 else 'N/A'}")

# Normalize features (important for MLP)
from sklearn.preprocessing import StandardScaler
scaler = StandardScaler()
X_normalized = scaler.fit_transform(X)

print("Dataset ready:")
print("X shape:", X_normalized.shape)
print("y shape:", y.shape)
print("Feature names:", feature_names)
print("Feature statistics:")
for i, name in enumerate(feature_names):
    print(f"  {name}: mean={np.mean(X[:, i]):.4f}, std={np.std(X[:, i]):.4f}")

print("First 5 labels:", y[:5])
print("First 5 feature vectors:")
for i in range(min(5, len(X_normalized))):
    print(f"  Video {i}: {X_normalized[i]}")

# --- Save arrays to disk ---
np.save("./mlp/X_mlp_features.npy", X_normalized)
np.save("./mlp/y_mlp_features.npy", y)

# Save scaler for later use
import pickle
with open("./mlp/feature_scaler.pkl", "wb") as f:
    pickle.dump(scaler, f)

print("Saved X_mlp_features.npy, y_mlp_features.npy, and feature_scaler.pkl")

# Save video IDs and feature info
with open("./mlp/video_ids_mlp_features.txt", "w") as f:
    for vid in video_ids_mlp:
        f.write(vid + "\n")

with open("./mlp/feature_names.txt", "w") as f:
    for name in feature_names:
        f.write(name + "\n")

print("Saved video_ids_mlp_features.txt and feature_names.txt")