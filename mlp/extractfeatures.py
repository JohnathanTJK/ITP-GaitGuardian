import json
import numpy as np
import pandas as pd
from collections import defaultdict
import os
from scipy.signal import find_peaks, savgol_filter
from scipy.spatial.distance import euclidean
from scipy.stats import entropy
import warnings
warnings.filterwarnings('ignore')

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

def calculate_angle_3_points(p1, p2, p3):
    """Calculate angle at p2 between points p1-p2-p3"""
    v1 = p1 - p2
    v2 = p3 - p2
    cos_angle = np.dot(v1, v2) / (np.linalg.norm(v1) * np.linalg.norm(v2) + 1e-8)
    cos_angle = np.clip(cos_angle, -1, 1)
    return np.arccos(cos_angle)

def extract_comprehensive_gait_features(sequence):
    """Extract comprehensive gait-specific features from keypoint sequence"""
    features = {}
    
    if len(sequence) < 10:  # Need minimum frames for reliable analysis
        return {f: 0.0 for f in ['stride_length_mean', 'stride_length_std', 'cadence', 'step_width_mean', 
                                'step_width_std', 'walking_speed_mean', 'walking_speed_std',
                                'hip_movement_x', 'hip_movement_y', 'knee_movement_x', 'knee_movement_y',
                                'ankle_movement_x', 'ankle_movement_y', 'gait_symmetry_x', 'gait_symmetry_y',
                                'knee_angle_left_mean', 'knee_angle_left_std', 'knee_angle_right_mean', 'knee_angle_right_std',
                                'hip_angle_left_mean', 'hip_angle_left_std', 'hip_angle_right_mean', 'hip_angle_right_std',
                                'step_time_variability', 'double_support_ratio', 'swing_phase_ratio',
                                'postural_stability', 'arm_swing_asymmetry', 'trunk_stability',
                                'heel_strike_angle_left', 'heel_strike_angle_right', 'toe_off_angle_left', 'toe_off_angle_right']}
    
    # Extract key joint positions over time
    left_ankle = sequence[:, [KEYPOINT_INDICES['left_ankle']*2, KEYPOINT_INDICES['left_ankle']*2+1]]
    right_ankle = sequence[:, [KEYPOINT_INDICES['right_ankle']*2, KEYPOINT_INDICES['right_ankle']*2+1]]
    left_hip = sequence[:, [KEYPOINT_INDICES['left_hip']*2, KEYPOINT_INDICES['left_hip']*2+1]]
    right_hip = sequence[:, [KEYPOINT_INDICES['right_hip']*2, KEYPOINT_INDICES['right_hip']*2+1]]
    left_knee = sequence[:, [KEYPOINT_INDICES['left_knee']*2, KEYPOINT_INDICES['left_knee']*2+1]]
    right_knee = sequence[:, [KEYPOINT_INDICES['right_knee']*2, KEYPOINT_INDICES['right_knee']*2+1]]
    left_shoulder = sequence[:, [KEYPOINT_INDICES['left_shoulder']*2, KEYPOINT_INDICES['left_shoulder']*2+1]]
    right_shoulder = sequence[:, [KEYPOINT_INDICES['right_shoulder']*2, KEYPOINT_INDICES['right_shoulder']*2+1]]
    left_wrist = sequence[:, [KEYPOINT_INDICES['left_wrist']*2, KEYPOINT_INDICES['left_wrist']*2+1]]
    right_wrist = sequence[:, [KEYPOINT_INDICES['right_wrist']*2, KEYPOINT_INDICES['right_wrist']*2+1]]
    
    # Smooth the trajectories to reduce noise
    def smooth_trajectory(traj, window_length=5):
        if len(traj) < window_length:
            return traj
        try:
            return savgol_filter(traj, window_length, 2, axis=0)
        except:
            return traj
    
    left_ankle = smooth_trajectory(left_ankle)
    right_ankle = smooth_trajectory(right_ankle)
    left_knee = smooth_trajectory(left_knee)
    right_knee = smooth_trajectory(right_knee)
    left_hip = smooth_trajectory(left_hip)
    right_hip = smooth_trajectory(right_hip)
    
    # 1. Enhanced Stride Analysis
    left_ankle_y = left_ankle[:, 1]
    right_ankle_y = right_ankle[:, 1]
    
    # Find heel strikes (local minima in ankle y-coordinate)
    left_peaks, _ = find_peaks(-left_ankle_y, prominence=0.005, distance=8)
    right_peaks, _ = find_peaks(-right_ankle_y, prominence=0.005, distance=8)
    
    # Stride length calculations
    if len(left_peaks) > 1:
        left_strides = np.diff(left_ankle[left_peaks, 0])
        features['stride_length_mean'] = np.mean(np.abs(left_strides))
        features['stride_length_std'] = np.std(left_strides)
    else:
        features['stride_length_mean'] = 0.0
        features['stride_length_std'] = 0.0
    
    # 2. Enhanced Cadence and Temporal Features
    total_steps = len(left_peaks) + len(right_peaks)
    features['cadence'] = total_steps / len(sequence) if len(sequence) > 0 else 0.0
    
    # Step time variability
    all_peaks = np.sort(np.concatenate([left_peaks, right_peaks]))
    if len(all_peaks) > 1:
        step_times = np.diff(all_peaks)
        features['step_time_variability'] = np.std(step_times) / (np.mean(step_times) + 1e-8)
    else:
        features['step_time_variability'] = 0.0
    
    # 3. Enhanced Step Width Analysis
    step_widths = np.abs(left_ankle[:, 0] - right_ankle[:, 0])
    features['step_width_mean'] = np.mean(step_widths)
    features['step_width_std'] = np.std(step_widths)
    
    # 4. Enhanced Walking Speed Analysis
    center_x = (left_ankle[:, 0] + right_ankle[:, 0]) / 2
    if len(center_x) > 1:
        speeds = np.diff(center_x)
        features['walking_speed_mean'] = np.mean(speeds)
        features['walking_speed_std'] = np.std(speeds)
    else:
        features['walking_speed_mean'] = 0.0
        features['walking_speed_std'] = 0.0
    
    # 5. Joint Movement Variability (X and Y separately)
    hip_center = (left_hip + right_hip) / 2
    features['hip_movement_x'] = np.std(hip_center[:, 0])
    features['hip_movement_y'] = np.std(hip_center[:, 1])
    
    features['knee_movement_x'] = np.mean([np.std(left_knee[:, 0]), np.std(right_knee[:, 0])])
    features['knee_movement_y'] = np.mean([np.std(left_knee[:, 1]), np.std(right_knee[:, 1])])
    
    features['ankle_movement_x'] = np.mean([np.std(left_ankle[:, 0]), np.std(right_ankle[:, 0])])
    features['ankle_movement_y'] = np.mean([np.std(left_ankle[:, 1]), np.std(right_ankle[:, 1])])
    
    # 6. Enhanced Gait Symmetry
    left_ankle_range_x = np.ptp(left_ankle[:, 0])
    right_ankle_range_x = np.ptp(right_ankle[:, 0])
    left_ankle_range_y = np.ptp(left_ankle[:, 1])
    right_ankle_range_y = np.ptp(right_ankle[:, 1])
    
    features['gait_symmetry_x'] = 1 - np.abs(left_ankle_range_x - right_ankle_range_x) / (left_ankle_range_x + right_ankle_range_x + 1e-8)
    features['gait_symmetry_y'] = 1 - np.abs(left_ankle_range_y - right_ankle_range_y) / (left_ankle_range_y + right_ankle_range_y + 1e-8)
    
    # 7. Joint Angles Analysis
    # Knee angles (hip-knee-ankle)
    knee_angles_left = []
    knee_angles_right = []
    
    for i in range(len(sequence)):
        try:
            # Left knee angle
            angle_left = calculate_angle_3_points(left_hip[i], left_knee[i], left_ankle[i])
            knee_angles_left.append(angle_left)
            
            # Right knee angle
            angle_right = calculate_angle_3_points(right_hip[i], right_knee[i], right_ankle[i])
            knee_angles_right.append(angle_right)
        except:
            knee_angles_left.append(0)
            knee_angles_right.append(0)
    
    if knee_angles_left:
        features['knee_angle_left_mean'] = np.mean(knee_angles_left)
        features['knee_angle_left_std'] = np.std(knee_angles_left)
    else:
        features['knee_angle_left_mean'] = 0.0
        features['knee_angle_left_std'] = 0.0
        
    if knee_angles_right:
        features['knee_angle_right_mean'] = np.mean(knee_angles_right)
        features['knee_angle_right_std'] = np.std(knee_angles_right)
    else:
        features['knee_angle_right_mean'] = 0.0
        features['knee_angle_right_std'] = 0.0
    
    # Hip angles (simplified - using shoulder-hip-knee)
    hip_angles_left = []
    hip_angles_right = []
    
    for i in range(len(sequence)):
        try:
            # Left hip angle
            angle_left = calculate_angle_3_points(left_shoulder[i], left_hip[i], left_knee[i])
            hip_angles_left.append(angle_left)
            
            # Right hip angle
            angle_right = calculate_angle_3_points(right_shoulder[i], right_hip[i], right_knee[i])
            hip_angles_right.append(angle_right)
        except:
            hip_angles_left.append(0)
            hip_angles_right.append(0)
    
    if hip_angles_left:
        features['hip_angle_left_mean'] = np.mean(hip_angles_left)
        features['hip_angle_left_std'] = np.std(hip_angles_left)
    else:
        features['hip_angle_left_mean'] = 0.0
        features['hip_angle_left_std'] = 0.0
        
    if hip_angles_right:
        features['hip_angle_right_mean'] = np.mean(hip_angles_right)
        features['hip_angle_right_std'] = np.std(hip_angles_right)
    else:
        features['hip_angle_right_mean'] = 0.0
        features['hip_angle_right_std'] = 0.0
    
    # 8. Gait Phase Analysis
    # Double support ratio (both feet on ground)
    # Simplified: when both ankles are at similar low positions
    double_support_frames = 0
    for i in range(len(sequence)):
        if abs(left_ankle_y[i] - right_ankle_y[i]) < 0.02:  # Both feet near ground
            double_support_frames += 1
    features['double_support_ratio'] = double_support_frames / len(sequence)
    
    # Swing phase ratio (one foot in air)
    swing_frames = len(sequence) - double_support_frames
    features['swing_phase_ratio'] = swing_frames / len(sequence)
    
    # 9. Postural Stability
    # Center of mass stability (approximated by hip center movement)
    com_velocity = np.diff(hip_center, axis=0)
    features['postural_stability'] = 1 / (np.mean(np.linalg.norm(com_velocity, axis=1)) + 1e-8)
    
    # 10. Arm Swing Analysis
    left_arm_swing = np.std(left_wrist[:, 0])
    right_arm_swing = np.std(right_wrist[:, 0])
    features['arm_swing_asymmetry'] = abs(left_arm_swing - right_arm_swing) / (left_arm_swing + right_arm_swing + 1e-8)
    
    # 11. Trunk Stability
    shoulder_center = (left_shoulder + right_shoulder) / 2
    trunk_movement = np.std(shoulder_center, axis=0)
    features['trunk_stability'] = 1 / (np.mean(trunk_movement) + 1e-8)
    
    # 12. Heel Strike and Toe Off Angles
    if len(left_peaks) > 0:
        heel_strike_angles_left = [knee_angles_left[i] for i in left_peaks if i < len(knee_angles_left)]
        features['heel_strike_angle_left'] = np.mean(heel_strike_angles_left) if heel_strike_angles_left else 0.0
        
        # Toe off is approximately mid-swing (between heel strikes)
        if len(left_peaks) > 1:
            toe_off_indices = [(left_peaks[i] + left_peaks[i+1]) // 2 for i in range(len(left_peaks)-1)]
            toe_off_angles_left = [knee_angles_left[i] for i in toe_off_indices if i < len(knee_angles_left)]
            features['toe_off_angle_left'] = np.mean(toe_off_angles_left) if toe_off_angles_left else 0.0
        else:
            features['toe_off_angle_left'] = 0.0
    else:
        features['heel_strike_angle_left'] = 0.0
        features['toe_off_angle_left'] = 0.0
    
    if len(right_peaks) > 0:
        heel_strike_angles_right = [knee_angles_right[i] for i in right_peaks if i < len(knee_angles_right)]
        features['heel_strike_angle_right'] = np.mean(heel_strike_angles_right) if heel_strike_angles_right else 0.0
        
        # Toe off is approximately mid-swing (between heel strikes)
        if len(right_peaks) > 1:
            toe_off_indices = [(right_peaks[i] + right_peaks[i+1]) // 2 for i in range(len(right_peaks)-1)]
            toe_off_angles_right = [knee_angles_right[i] for i in toe_off_indices if i < len(knee_angles_right)]
            features['toe_off_angle_right'] = np.mean(toe_off_angles_right) if toe_off_angles_right else 0.0
        else:
            features['toe_off_angle_right'] = 0.0
    else:
        features['heel_strike_angle_right'] = 0.0
        features['toe_off_angle_right'] = 0.0
    
    return features

# Updated feature names list
feature_names = [
    'stride_length_mean', 'stride_length_std', 'cadence', 'step_width_mean', 'step_width_std',
    'walking_speed_mean', 'walking_speed_std', 'hip_movement_x', 'hip_movement_y',
    'knee_movement_x', 'knee_movement_y', 'ankle_movement_x', 'ankle_movement_y',
    'gait_symmetry_x', 'gait_symmetry_y', 'knee_angle_left_mean', 'knee_angle_left_std',
    'knee_angle_right_mean', 'knee_angle_right_std', 'hip_angle_left_mean', 'hip_angle_left_std',
    'hip_angle_right_mean', 'hip_angle_right_std', 'step_time_variability', 'double_support_ratio',
    'swing_phase_ratio', 'postural_stability', 'arm_swing_asymmetry', 'trunk_stability',
    'heel_strike_angle_left', 'heel_strike_angle_right', 'toe_off_angle_left', 'toe_off_angle_right'
]

# --- Create feature vectors and labels ---
X = []
y = []
video_ids_mlp = []

print("Extracting features from videos...")
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
    
    # Extract comprehensive gait features
    gait_features = extract_comprehensive_gait_features(sequence)
    feature_vector = [gait_features[name] for name in feature_names]
    
    # Check for invalid values
    feature_vector = [0.0 if np.isnan(x) or np.isinf(x) else x for x in feature_vector]
    
    X.append(feature_vector)
    y.append(label)
    video_ids_mlp.append(vid)

X = np.array(X)
y = np.array(y)

# Detailed class distribution analysis
unique, counts = np.unique(y, return_counts=True)
print(f"\nClass distribution: {dict(zip(unique, counts))}")
print(f"Total samples: {len(y)}")
if len(counts) > 1:
    print(f"Class imbalance ratio (abnormal/normal): {counts[1]/counts[0]:.3f}")
    print(f"Minority class percentage: {(min(counts)/len(y))*100:.1f}%")

# Handle severe class imbalance with SMOTE if needed
from imblearn.over_sampling import SMOTE
from sklearn.preprocessing import StandardScaler

# First normalize, then apply SMOTE if needed
scaler = StandardScaler()
X_normalized = scaler.fit_transform(X)

# Apply SMOTE only if class imbalance is severe (< 30% minority class)
minority_percentage = (min(counts)/len(y))*100
if minority_percentage < 30:
    print(f"\nApplying SMOTE due to severe class imbalance ({minority_percentage:.1f}% minority class)")
    smote = SMOTE(random_state=42, k_neighbors=min(5, min(counts)-1))
    X_balanced, y_balanced = smote.fit_resample(X_normalized, y)
    print(f"After SMOTE: {X_balanced.shape[0]} samples")
    unique_balanced, counts_balanced = np.unique(y_balanced, return_counts=True)
    print(f"Balanced class distribution: {dict(zip(unique_balanced, counts_balanced))}")
    X_final, y_final = X_balanced, y_balanced
else:
    print("Class imbalance is manageable, using original normalized data")
    X_final, y_final = X_normalized, y

print(f"\nFinal dataset:")
print(f"X shape: {X_final.shape}")
print(f"y shape: {y_final.shape}")
print(f"Feature names ({len(feature_names)}): {feature_names}")

# Feature statistics and quality check
print(f"\nFeature statistics:")
for i, name in enumerate(feature_names):
    mean_val = np.mean(X_final[:, i])
    std_val = np.std(X_final[:, i])
    min_val = np.min(X_final[:, i])
    max_val = np.max(X_final[:, i])
    print(f"  {name}: mean={mean_val:.4f}, std={std_val:.4f}, range=[{min_val:.4f}, {max_val:.4f}]")

# Check for constant features (zero variance)
zero_var_features = []
for i, name in enumerate(feature_names):
    if np.std(X_final[:, i]) < 1e-8:
        zero_var_features.append(name)

if zero_var_features:
    print(f"\nWarning: Features with zero variance: {zero_var_features}")
else:
    print(f"\nAll features have non-zero variance ✓")

# --- Save arrays to disk ---
np.save("./mlp/X_mlp_features.npy", X_final)
np.save("./mlp/y_mlp_features.npy", y_final)

# Save scaler for later use
import pickle
with open("./mlp/feature_scaler.pkl", "wb") as f:
    pickle.dump(scaler, f)

print(f"\nSaved X_mlp_features.npy, y_mlp_features.npy, and feature_scaler.pkl")

# Save metadata
with open("./mlp/video_ids_mlp_features.txt", "w") as f:
    for vid in (video_ids_mlp * (len(y_final) // len(video_ids_mlp) + 1))[:len(y_final)]:
        f.write(vid + "\n")

with open("./mlp/feature_names.txt", "w") as f:
    for name in feature_names:
        f.write(name + "\n")

print("Saved video_ids_mlp_features.txt and feature_names.txt")

# Save feature extraction report
with open("./mlp/feature_extraction_report.txt", "w") as f:
    f.write("Feature Extraction Report\n")
    f.write("=" * 50 + "\n\n")
    f.write(f"Total videos processed: {len(video_ids_mlp)}\n")
    f.write(f"Total features extracted: {len(feature_names)}\n")
    f.write(f"Final dataset size: {X_final.shape}\n")
    f.write(f"Class distribution: {dict(zip(unique, counts))}\n")
    if minority_percentage < 30:
        f.write(f"SMOTE applied: Yes\n")
        f.write(f"Balanced dataset size: {X_final.shape}\n")
    else:
        f.write(f"SMOTE applied: No\n")
    f.write(f"\nFeature List:\n")
    for i, name in enumerate(feature_names):
        f.write(f"{i+1:2d}. {name}\n")

print("Saved feature_extraction_report.txt")
print("\nFeature extraction completed successfully!")