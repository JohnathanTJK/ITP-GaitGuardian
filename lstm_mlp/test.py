import numpy as np
import pandas as pd
from tensorflow.keras.models import Model
from tensorflow.keras.layers import Input, LSTM, Dense, Dropout, Concatenate
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.callbacks import EarlyStopping
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler

# === CONFIG - update paths ===
LSTM_NPY_PATH = './lstm/X.npy'          # LSTM input sequences
LSTM_VID_PATH = './lstm/video_ids_lstm.txt'  # Text file with one video ID per line, same order as LSTM sequences
FEATURES_CSV_PATH = './mlp/keypoints_posefeatures.csv'  # CSV with video, mean_stride, cadence, label

# === Load LSTM data ===
X_lstm = np.load(LSTM_NPY_PATH)  # shape (N, 100, 34)
with open(LSTM_VID_PATH, 'r') as f:
    video_ids_lstm = [line.strip() for line in f.readlines()]  # list of strings, length N

print(f"Loaded LSTM sequences: {X_lstm.shape}, video IDs: {len(video_ids_lstm)}")
print(f"Sample LSTM video IDs (first 10): {video_ids_lstm[:10]}")

# === Load features CSV ===
features_df = pd.read_csv(FEATURES_CSV_PATH)
print(f"Loaded features CSV: {features_df.shape}")
print(f"Sample features CSV video IDs (first 10): {features_df['video'].values[:10]}")

# === Extract base video_id from features filenames ===
features_df['video_id'] = features_df['video'].apply(lambda x: str(x).split('_')[0])
print(f"Extracted base video_id from CSV filenames:\n{features_df['video_id'].unique()[:10]}")

# === Aggregate features per video_id ===
agg_df = features_df.groupby('video_id').agg({
    'mean_stride': 'mean',
    'cadence': 'mean',
    'label': lambda x: x.mode()[0]  # most frequent label per video
}).reset_index()

print(f"Aggregated features per video_id: {agg_df.shape}")
print(agg_df.head())

# === Align data by video_id ===
lstm_df = pd.DataFrame({'video_id': video_ids_lstm})
merged_df = lstm_df.merge(agg_df, on='video_id', how='inner')
print(f"After merge, samples: {merged_df.shape[0]}")

# Find indices of matched samples in original LSTM data
matched_video_set = set(merged_df['video_id'])
matched_indices = [i for i, vid in enumerate(video_ids_lstm) if vid in matched_video_set]

# Filter and align inputs
X_lstm_aligned = X_lstm[matched_indices]
# Extract corresponding rows in merged_df ordered as matched_indices
merged_df_sorted = merged_df.set_index('video_id').loc[[video_ids_lstm[i] for i in matched_indices]].reset_index()

X_mlp_aligned = merged_df_sorted[['mean_stride', 'cadence']].values
y_aligned = merged_df_sorted['label'].values

print(f"Aligned data shapes — X_lstm: {X_lstm_aligned.shape}, X_mlp: {X_mlp_aligned.shape}, y: {y_aligned.shape}")

# === Scale MLP features ===
scaler = StandardScaler()
X_mlp_scaled = scaler.fit_transform(X_mlp_aligned)

# === Split train/test ===
X_lstm_train, X_lstm_test, X_mlp_train, X_mlp_test, y_train, y_test = train_test_split(
    X_lstm_aligned, X_mlp_scaled, y_aligned,
    test_size=0.2, random_state=42, stratify=y_aligned
)

# === Build combined model ===
lstm_input = Input(shape=(100, 34), name='lstm_input')
x1 = LSTM(64, return_sequences=True)(lstm_input)
x1 = Dropout(0.2)(x1)
x1 = LSTM(32)(x1)
x1 = Dropout(0.2)(x1)

mlp_input = Input(shape=(2,), name='mlp_input')
x2 = Dense(32, activation='relu')(mlp_input)
x2 = Dropout(0.2)(x2)
x2 = Dense(16, activation='relu')(x2)

combined = Concatenate()([x1, x2])
x = Dense(16, activation='relu')(combined)
output = Dense(1, activation='sigmoid')(x)

model = Model(inputs=[lstm_input, mlp_input], outputs=output)
model.compile(optimizer=Adam(0.001), loss='binary_crossentropy', metrics=['accuracy'])
model.summary()

# === Train ===
early_stopping = EarlyStopping(monitor='val_loss', patience=5, restore_best_weights=True)
history = model.fit(
    [X_lstm_train, X_mlp_train], y_train,
    validation_split=0.2,
    epochs=30,
    batch_size=32,
    callbacks=[early_stopping]
)

# === Evaluate ===
loss, acc = model.evaluate([X_lstm_test, X_mlp_test], y_test)
print(f"Test accuracy: {acc:.4f}")

# === Save model and scaler ===
model.save('./combined_lstm_mlp_model.h5')
import joblib
joblib.dump(scaler, './mlp_scaler.pkl')
print("Saved combined model and scaler.")
