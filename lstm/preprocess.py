import json
import numpy as np
import pandas as pd
from collections import defaultdict
import os

# --- CONFIG ---
SEQUENCE_LENGTH = 100
NUM_KEYPOINTS = 17
NUM_FEATURES = NUM_KEYPOINTS * 2
IMAGE_WIDTH = 640
IMAGE_HEIGHT = 480
EXCEL_PATH = "./lstm/label_parameters_abnormal.xlsx"
KEYPOINTS_JSON_PATH = "./lstm/keyPoints.json"

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

# --- Create sequences and labels ---
X = []
y = []
video_ids_lstm = []
for vid, frames in video_frames.items():
    if vid not in label_map:
        continue
    sequence = np.array(frames)
    label = label_map[vid]
    if len(sequence) > SEQUENCE_LENGTH:
        sequence = sequence[:SEQUENCE_LENGTH]
    elif len(sequence) < SEQUENCE_LENGTH:
        padding = np.zeros((SEQUENCE_LENGTH - len(sequence), NUM_FEATURES))
        sequence = np.vstack([sequence, padding])
    X.append(sequence)
    y.append(label)
    video_ids_lstm.append(vid)  # save vid order here
    
keypoint_names = [
    "Nose", "LEye", "REye", "LEar", "REar",
    "LShoulder", "RShoulder", "LElbow", "RElbow",
    "LWrist", "RWrist", "LHip", "RHip",
    "LKnee", "RKnee", "LAnkle", "RAnkle"
]

# Example: print normalized x,y for first frame of first video
frame = X[0][0]  # first video, first frame

for i, name in enumerate(keypoint_names):
    x = frame[2*i]
    y_coord = frame[2*i + 1]  # renamed to avoid conflict
    print(f"{name}: x={x:.3f}, y={y_coord:.3f}")


print("Raw y content (first 5):", y[:5])
print("Raw y type:", type(y))


X = np.array(X)
y = np.array(y)

print("Dataset ready:")
print("X shape:", X.shape)
print("y shape:", y.shape)
print("First 5 labels:", y[:5])


# --- Save arrays to disk ---
np.save("./lstm/X.npy", X)
np.save("./lstm/y.npy", y)
print("Saved X.npy and y.npy")

# Save video IDs for alignment with features CSV
with open("./lstm/video_ids_lstm.txt", "w") as f:
    for vid in video_ids_lstm:
        f.write(vid + "\n")
print("Saved video_ids_lstm.txt")
