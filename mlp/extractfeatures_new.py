# === extractfeatures.py ===
# Purpose: Extract gait features from keyPoints.json + labels

import pandas as pd
import numpy as np
import json
import os
import re
from collections import defaultdict

# === CONFIG ===
KEYPOINTS_JSON = './mlp/keyPoints.json'
LABEL_XLSX = './mlp/label_parameters_abnormal.xlsx'
IMAGE_WIDTH = 640  # adjust if different
IMAGE_HEIGHT = 480

# === Load keyPoints.json ===
with open(KEYPOINTS_JSON, 'r') as f:
    data = json.load(f)

imgnames = data['imgname']  # e.g., 001_color_down_0.png
parts = data['part']        # list of 17x2 keypoints

# === Preprocess into a dict grouped by video ===
video_dict = defaultdict(list)

for name, keypoints in zip(imgnames, parts):
    if '_down_' in name:
        prefix, suffix = name.split('_down_', 1)
        suffix = suffix.replace('.png', '.jpg.json')
        video_name = prefix + '_down' + suffix
    else:
        video_name = os.path.splitext(name)[0] + '.json'

    video_dict[video_name].append(keypoints)


# === Keypoint index for ankles (adjust as needed!) ===
ANKLE_LEFT_ID = 15
ANKLE_RIGHT_ID = 16

features_list = []

for video_name, frames in video_dict.items():
    stride_lengths = []

    for keypoints in frames:
        if len(keypoints) <= max(ANKLE_LEFT_ID, ANKLE_RIGHT_ID):
            continue  # skip invalid frame

        lx, ly = keypoints[ANKLE_LEFT_ID]
        rx, ry = keypoints[ANKLE_RIGHT_ID]

        if lx == 0 and ly == 0 and rx == 0 and ry == 0:
            continue  # skip if both missing

        dist = np.sqrt((lx - rx)**2 + (ly - ry)**2)
        stride_lengths.append(dist)

    mean_stride = np.mean(stride_lengths) if stride_lengths else 0
    cadence = len(frames) / len(frames)  # placeholder cadence = 1.0

    features_list.append({
        'video': video_name,
        'mean_stride': mean_stride,
        'cadence': cadence
    })

features_df = pd.DataFrame(features_list)
print("Extracted features:", features_df.head())

# === Normalize feature video IDs to match labels ===

def normalize_video_id(video_str):
    # Extract pattern like '001_color' from '001_color_down0.jpg.json'
    m = re.match(r'(\d+_color)', video_str)
    if m:
        return m.group(1) + '.json'
    else:
        return video_str  # fallback, keep original

features_df['video_id'] = features_df['video'].apply(normalize_video_id)

# === Load labels ===
labels_df = pd.read_excel(LABEL_XLSX, sheet_name='Machine automatic')
labels_df['video_id'] = labels_df['NO.'].astype(str)


print("Features video_id samples:", features_df['video_id'].unique()[:10])
print("Labels video_id samples:", labels_df['video_id'].unique()[:10])

# === Merge with labels ===
merged = features_df.merge(
    labels_df[['video_id', 'Gait abnormal(golden)']],
    on='video_id',
    how='inner'
)

merged = merged.rename(columns={'Gait abnormal(golden)': 'label'})
merged = merged.drop(columns=['video_id'])

print("Merged features + labels:", merged.head())

merged.to_csv('./mlp/keypoints_posefeatures.csv', index=False)
print("✅ Saved: keypoints_posefeatures.csv")
