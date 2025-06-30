import json
import numpy as np
import pandas as pd
from collections import defaultdict
import os
from sklearn.preprocessing import LabelEncoder
import pickle

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
    # Normalize keypoints by image dimensions
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

    # Pad or truncate sequences to fixed length
    if len(sequence) > SEQUENCE_LENGTH:
        sequence = sequence[:SEQUENCE_LENGTH]
    elif len(sequence) < SEQUENCE_LENGTH:
        padding = np.zeros((SEQUENCE_LENGTH - len(sequence), NUM_FEATURES))
        sequence = np.vstack([sequence, padding])

    X.append(sequence)
    y.append(label)
    video_ids_lstm.append(vid)  # Save video id order

# Convert lists to arrays
X = np.array(X)
y = np.array(y)

# Encode string labels to integers
le = LabelEncoder()
y = le.fit_transform(y)

# Print label classes and distribution
print("Label classes:", le.classes_)
unique, counts = np.unique(y, return_counts=True)
print("Class distribution:", dict(zip(unique, counts)))

# Ensure correct data types
X = X.astype(np.float32)
y = y.astype(np.int32)

# Save arrays to disk
os.makedirs("./lstm", exist_ok=True)
np.save("./lstm/new_X.npy", X)
np.save("./lstm/new_y.npy", y)
print(f"Saved X.npy and y.npy with shapes {X.shape} and {y.shape}")

# Save video IDs for alignment
with open("./lstm/new_video_ids_lstm.txt", "w") as f:
    for vid in video_ids_lstm:
        f.write(vid + "\n")
print("Saved video_ids_lstm.txt")

# Save label encoder for decoding later
with open("./lstm/new_label_encoder.pkl", "wb") as f:
    pickle.dump(le, f)
print("Saved label_encoder.pkl")

# Optional: print example normalized keypoints for first frame of first video
keypoint_names = [
    "Nose", "LEye", "REye", "LEar", "REar",
    "LShoulder", "RShoulder", "LElbow", "RElbow",
    "LWrist", "RWrist", "LHip", "RHip",
    "LKnee", "RKnee", "LAnkle", "RAnkle"
]

print("\nExample normalized keypoints for first frame of first video:")
frame = X[0][0]
for i, name in enumerate(keypoint_names):
    x = frame[2*i]
    y_coord = frame[2*i + 1]
    print(f"  {name}: x={x:.3f}, y={y_coord:.3f}")
