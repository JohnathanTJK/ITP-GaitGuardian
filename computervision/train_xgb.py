import os
import glob
import pandas as pd
import numpy as np
from xgboost import XGBClassifier, DMatrix, cv
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import classification_report
from collections import Counter
import joblib
import optuna
import json

# === CONFIG ===
input_dir = "./computervision/keypoints_and_durations"
model_path = "./computervision/xgb_model.pkl"
label_path = "./computervision/xgb_label_encoder.pkl"
feature_path = "./computervision/xgb_features.json"

# === Load labeled CSVs ===
csv_files = glob.glob(os.path.join(input_dir, "*.csv"))
if not csv_files:
    raise FileNotFoundError(f"No CSVs found in {input_dir}")
print(f"[INFO] Loading {len(csv_files)} labeled CSVs...")

dataframes = []
for csv in csv_files:
    df = pd.read_csv(csv)
    df['video'] = os.path.basename(csv).replace("_labelled.csv", "")
    dataframes.append(df)

full_df = pd.concat(dataframes, ignore_index=True)
full_df = full_df.dropna(subset=["tug_subtask"])
print(f"[INFO] Using {len(full_df)} labeled frames from {len(csv_files)} videos")

# === Smooth Labels Per Video ===
def smooth_labels(df, window=7):
    smoothed_dfs = []
    for video in df['video'].unique():
        sub_df = df[df['video'] == video].copy()
        labels = sub_df['tug_subtask'].tolist()
        pad = window // 2
        padded = [labels[0]] * pad + labels + [labels[-1]] * pad
        smoothed = [
            Counter(padded[i - pad:i + pad + 1]).most_common(1)[0][0]
            for i in range(pad, len(labels) + pad)
        ]
        sub_df['tug_subtask'] = smoothed
        smoothed_dfs.append(sub_df)
    return pd.concat(smoothed_dfs, ignore_index=True)

full_df = smooth_labels(full_df)
print("[INFO] Labels smoothed per video")

# === Encode labels ===
le = LabelEncoder()
full_df["label_id"] = le.fit_transform(full_df["tug_subtask"])
joblib.dump(le, label_path)
print(f"[INFO] Saved label encoder to {label_path}")
print(f"[INFO] Classes: {list(le.classes_)}")

# === Features and Labels ===
pose_cols = [c for c in full_df.columns if c.startswith(('x_', 'y_', 'z_', 'visibility_'))]
X = full_df[pose_cols].fillna(0).values
y = full_df['label_id'].values

# Save feature list
with open(feature_path, "w") as jf:
    json.dump(pose_cols, jf)
print(f"[INFO] Saved feature list to {feature_path}")

# === Optuna Hyperparameter Tuning ===
def objective(trial):
    params = {
        "verbosity": 0,
        "objective": "multi:softprob",
        "num_class": len(le.classes_),
        "tree_method": "hist",
        "eval_metric": "mlogloss",
        "learning_rate": trial.suggest_float("learning_rate", 0.01, 0.2),
        "max_depth": trial.suggest_int("max_depth", 4, 12),
        "subsample": trial.suggest_float("subsample", 0.7, 1.0),
        "colsample_bytree": trial.suggest_float("colsample_bytree", 0.7, 1.0),
        "n_estimators": trial.suggest_int("n_estimators", 100, 400),
    }
    dtrain = DMatrix(X, label=y)
    scores = cv(params, dtrain, nfold=3, stratified=True, early_stopping_rounds=10, verbose_eval=False)
    return scores['test-mlogloss-mean'].min()

print("[INFO] Starting Optuna hyperparameter search...")
study = optuna.create_study(direction="minimize")
study.optimize(objective, n_trials=50)

print("\nBest Parameters:")
for k, v in study.best_params.items():
    print(f"  {k}: {v}")

# === Train Final Model ===
best_params = study.best_params
best_params.update({
    "objective": "multi:softprob",
    "num_class": len(le.classes_),
    "eval_metric": "mlogloss",
    "tree_method": "hist",
    "use_label_encoder": False
})
final_model = XGBClassifier(**best_params)
final_model.fit(X, y)

# === Optional: Evaluate on same data (for sanity) ===
y_pred = final_model.predict(X)
print("\nClassification Report (Trained Data):")
print(classification_report(y, y_pred, target_names=le.classes_))

# === Save Final Model ===
joblib.dump(final_model, model_path)
print(f"\nSaved XGBoost model to {model_path}")
