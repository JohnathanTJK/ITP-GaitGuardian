import os
import json
import joblib
import numpy as np
import pandas as pd
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
from sklearn.utils import class_weight
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Dropout, BatchNormalization, Input
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.callbacks import EarlyStopping

# === Config ===
data_path = "./computervision_test/data/gait_features_with_severity.csv"
output_dir = "./computervision_test/data"
os.makedirs(output_dir, exist_ok=True)

# === Load data ===
df = pd.read_csv(data_path)
df = df.dropna(subset=['severity'])  # Ensure no missing labels

# === Prepare features and labels ===
feature_cols = [c for c in df.columns if c not in ['video', 'severity', 'severity_mlp']]
X = df[feature_cols].fillna(0).values

label_enc = LabelEncoder()
y = label_enc.fit_transform(df['severity'].values)
y_cat = to_categorical(y, num_classes=len(label_enc.classes_))

# === Train/test split ===
X_train, X_test, y_train, y_test = train_test_split(
    X, y_cat, test_size=0.2, random_state=42, stratify=y
)

# === Handle class imbalance ===
y_train_int = np.argmax(y_train, axis=1)
class_weights = class_weight.compute_class_weight(
    class_weight='balanced',
    classes=np.unique(y_train_int),
    y=y_train_int
)
class_weights_dict = dict(enumerate(class_weights))
print("Class weights:", class_weights_dict)

# === Build MLP model ===
model = Sequential([
    Input(shape=(X.shape[1],)),
    Dense(64, activation='relu'),
    BatchNormalization(),
    Dropout(0.3),
    Dense(32, activation='relu'),
    BatchNormalization(),
    Dropout(0.3),
    Dense(len(label_enc.classes_), activation='softmax')
])
model.compile(
    optimizer=Adam(learning_rate=0.001),
    loss='categorical_crossentropy',
    metrics=['accuracy']
)

# === Train model ===
callbacks = [EarlyStopping(monitor='val_loss', patience=10, restore_best_weights=True)]
history = model.fit(
    X_train, y_train,
    validation_data=(X_test, y_test),
    epochs=50,
    batch_size=8,
    callbacks=callbacks,
    class_weight=class_weights_dict,
    verbose=1
)

# === Evaluate ===
loss, acc = model.evaluate(X_test, y_test, verbose=0)
print(f"\nSeverity classification accuracy: {acc:.3f}")

# === Save model and metadata ===
model.save(os.path.join(output_dir, "severity_mlp_model.h5"))
joblib.dump(label_enc, os.path.join(output_dir, "severity_label_encoder.pkl"))

print("Features used for training:", feature_cols)
print("Number of features:", len(feature_cols))

with open(os.path.join(output_dir, "severity_features.json"), "w") as f:
    json.dump(feature_cols, f)

print("✅ Retrained MLP model and metadata saved.")
