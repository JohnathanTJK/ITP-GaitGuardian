# === extractfeatures.py ===
# Purpose: Extract gait features from raw pose CSV + labels

import pandas as pd
import numpy as np

# === CONFIG ===
POSE_CSV = './mlp/pose_raw1_15.csv'
LABEL_XLSX = './mlp/label_parameters_abnormal.xlsx'

# === Load pose data ===
df = pd.read_csv(POSE_CSV)
print("Pose CSV loaded:", df.head())

# === Example: MediaPipe full body IDs ===
# Check your pose IDs first!
# Common: LANKLE (id 27), RANKLE (id 28)
ANKLE_LEFT_ID = 27
ANKLE_RIGHT_ID = 28

features_list = []

# === Process each video ===
for video_name, group in df.groupby('video'):
    frames = group['frame'].unique()
    stride_lengths = []

    for f in frames:
        frame_data = group[group['frame'] == f]
        left_ankle = frame_data[frame_data['id'] == ANKLE_LEFT_ID]
        right_ankle = frame_data[frame_data['id'] == ANKLE_RIGHT_ID]

        if not left_ankle.empty and not right_ankle.empty:
            lx, ly = left_ankle.iloc[0][['x', 'y']]
            rx, ry = right_ankle.iloc[0][['x', 'y']]
            dist = np.sqrt((lx - rx)**2 + (ly - ry)**2)
            stride_lengths.append(dist)

    mean_stride = np.mean(stride_lengths) if stride_lengths else 0
    cadence = len(frames) / (frames[-1] - frames[0] + 1)  # simple proxy

    features_list.append({
        'video': video_name,
        'mean_stride': mean_stride,
        'cadence': cadence
    })

features_df = pd.DataFrame(features_list)
print("Extracted features:", features_df.head())

# === Load labels ===
labels_df = pd.read_excel(LABEL_XLSX, sheet_name='Machine automatic')
print("Label columns:", labels_df.columns)

# ✅ SAME name for both
features_df['video_id'] = features_df['video']
labels_df['video_id'] = labels_df['NO.']

print(labels_df[['video_id', 'Gait abnormal(golden)']].head())

merged = features_df.merge(
    labels_df[['video_id', 'Gait abnormal(golden)']],
    on='video_id',
    how='inner'
)

merged = merged.rename(columns={'Gait abnormal(golden)': 'label'})
merged = merged.drop(columns=['video_id'])

print("Merged features + labels:", merged.head())

merged.to_csv('./mlp/1_15_posefeatures.csv', index=False)
print("✅ Saved: pose_features.csv")
