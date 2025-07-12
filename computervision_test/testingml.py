import os
import json
import joblib
import numpy as np
import pandas as pd
from tensorflow.keras.models import load_model

# === Paths ===
input_csv = "./computervision_test/data/gait_features_with_severity.csv"
model_path = "./computervision_test/data/severity_mlp_model.h5"
label_encoder_path = "./computervision_test/data/severity_label_encoder.pkl"
feature_list_path = "./computervision_test/data/severity_features.json"
output_csv = "./computervision_test/data/gait_features_with_severity_ml.csv"

# === Load data ===
df = pd.read_csv(input_csv)

# Drop the existing severity label if present
if 'severity' in df.columns:
    df = df.drop(columns=['severity'])

# === Load model and encoders ===
model = load_model(model_path)
label_encoder = joblib.load(label_encoder_path)
with open(feature_list_path, "r") as f:
    feature_cols = json.load(f)

# === Prepare input features ===
X = df[feature_cols].fillna(0).values

# === Predict severity ===
probs = model.predict(X)
predicted_indices = np.argmax(probs, axis=1)
predicted_labels = label_encoder.inverse_transform(predicted_indices)

# Add predicted severity column
df["severity"] = predicted_labels

# === Save output ===
df.to_csv(output_csv, index=False)
print(f"✅ Saved predictions with ML severity to {output_csv}")
