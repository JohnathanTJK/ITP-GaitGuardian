# Purpose: Train MLP 

import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.neural_network import MLPClassifier
from sklearn.metrics import classification_report
import joblib

# === Load features CSV ===
df = pd.read_csv('./mlp/keypoints_posefeatures.csv')
print("Loaded features:", df.head())

# === Prepare X, y ===
X = df.drop(columns=['video', 'label'])
y = df['label']

# === Scale ===
scaler = StandardScaler()
X_scaled = scaler.fit_transform(X)

# === Split ===
X_train, X_test, y_train, y_test = train_test_split(
    X_scaled, y, test_size=0.2, random_state=42
)

# === Train MLP ===
mlp = MLPClassifier(hidden_layer_sizes=(64, 32), max_iter=500, random_state=42)
mlp.fit(X_train, y_train)

# === Evaluate ===
y_pred = mlp.predict(X_test)
print("\n=== Classification Report ===")
print(classification_report(y_test, y_pred))

# === Save ===
joblib.dump(mlp, './mlp/mlp_model_new.pkl')
joblib.dump(scaler, './mlp/mlp_scaler_new.pkl')
print("✅ Saved: mlp_model_new.pkl and mlp_scaler_new.pkl")
