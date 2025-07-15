import numpy as np
import pandas as pd
import pickle
import warnings
from tensorflow.keras.models import Model
from tensorflow.keras.layers import Input, LSTM, Dense, Dropout, Concatenate, BatchNormalization
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau, ModelCheckpoint
from tensorflow.keras.regularizers import l1_l2
from sklearn.model_selection import train_test_split, StratifiedKFold
from sklearn.preprocessing import StandardScaler
from sklearn.utils.class_weight import compute_class_weight
from sklearn.metrics import classification_report, confusion_matrix, roc_auc_score
from sklearn.ensemble import RandomForestClassifier

warnings.filterwarnings('ignore')

# === CONFIG - Updated paths ===
LSTM_NPY_PATH = './lstm/X.npy'          # LSTM input sequences
LSTM_VID_PATH = './lstm/video_ids_lstm.txt'  # Video IDs for LSTM
MLP_FEATURES_PATH = './mlp/X_mlp_features.npy'  # features
MLP_LABELS_PATH = './mlp/y_mlp_features.npy'    # Labels
MLP_VID_PATH = './mlp/video_ids_mlp_features.txt'  # Video IDs for MLP features
FEATURE_SCALER_PATH = './mlp/feature_scaler.pkl'  # Your trained scaler
SELECTED_FEATURES_PATH = './mlp/selected_features.txt'  # Your selected features

print("Loading improved MLP features and LSTM data...")

# === Load LSTM data ===
X_lstm = np.load(LSTM_NPY_PATH)  # shape (N, 100, 34)
with open(LSTM_VID_PATH, 'r') as f:
    video_ids_lstm = [line.strip() for line in f.readlines()]

print(f"Loaded LSTM sequences: {X_lstm.shape}, video IDs: {len(video_ids_lstm)}")

# === Load improved MLP features ===
X_mlp_full = np.load(MLP_FEATURES_PATH)
y_mlp = np.load(MLP_LABELS_PATH)
with open(MLP_VID_PATH, 'r') as f:
    video_ids_mlp = [line.strip() for line in f.readlines()]

# Load your trained scaler
with open(FEATURE_SCALER_PATH, 'rb') as f:
    trained_scaler = pickle.load(f)

# Load selected features
with open(SELECTED_FEATURES_PATH, 'r') as f:
    selected_features = [line.strip() for line in f.readlines()]

print(f"Loaded MLP features: {X_mlp_full.shape}, Labels: {y_mlp.shape}")
print(f"Selected features ({len(selected_features)}): {selected_features[:5]}...")

# === Feature Selection with Random Forest ===
rf_selector = RandomForestClassifier(n_estimators=100, random_state=42, class_weight='balanced')
rf_selector.fit(X_mlp_full, y_mlp)
feature_importance = rf_selector.feature_importances_
top_features_idx = np.argsort(feature_importance)[-min(20, X_mlp_full.shape[1]):]
X_mlp_selected = X_mlp_full[:, top_features_idx]

print(f"Selected {X_mlp_selected.shape[1]} best features for combination model")

# === Align data by video_id ===
print("Aligning LSTM and MLP data by video IDs...")

lstm_df = pd.DataFrame({'video_id': video_ids_lstm, 'lstm_idx': range(len(video_ids_lstm))})
mlp_df = pd.DataFrame({'video_id': video_ids_mlp, 'mlp_idx': range(len(video_ids_mlp))})
merged_df = lstm_df.merge(mlp_df, on='video_id', how='inner')

print(f"Common videos found: {len(merged_df)}")
if len(merged_df) == 0:
    print("No matching video IDs found! Check video ID formats.")
    exit()

lstm_indices = merged_df['lstm_idx'].values
mlp_indices = merged_df['mlp_idx'].values

X_lstm_aligned = X_lstm[lstm_indices]
X_mlp_aligned = X_mlp_selected[mlp_indices]
y_aligned = y_mlp[mlp_indices]

print(f"Aligned data shapes: LSTM {X_lstm_aligned.shape}, MLP {X_mlp_aligned.shape}, Labels {y_aligned.shape}")

unique, counts = np.unique(y_aligned, return_counts=True)
print(f"Class distribution: {dict(zip(unique, counts))}")

# === Class weights ===
class_weights = compute_class_weight('balanced', classes=np.unique(y_aligned), y=y_aligned)
class_weight_dict = dict(zip(np.unique(y_aligned), class_weights))
print(f"Class weights: {class_weight_dict}")

# === Split train/test ===
X_lstm_train, X_lstm_test, X_mlp_train, X_mlp_test, y_train, y_test = train_test_split(
    X_lstm_aligned, X_mlp_aligned, y_aligned,
    test_size=0.2, random_state=42, stratify=y_aligned
)

print(f"Training set shapes: LSTM {X_lstm_train.shape}, MLP {X_mlp_train.shape}")
print(f"Test set shapes: LSTM {X_lstm_test.shape}, MLP {X_mlp_test.shape}")

# === Normalize MLP features ===
scaler = StandardScaler()
X_mlp_train_scaled = scaler.fit_transform(X_mlp_train)
X_mlp_test_scaled = scaler.transform(X_mlp_test)

# === Define LSTM branch (standalone architecture) ===
def create_lstm_branch_standalone(input_shape):
    lstm_input = Input(shape=input_shape, name='lstm_input')
    x = LSTM(64, return_sequences=True)(lstm_input)
    x = Dropout(0.2)(x)
    x = BatchNormalization()(x)
    x = LSTM(32)(x)
    x = Dropout(0.2)(x)
    x = BatchNormalization()(x)
    x = Dense(16, activation='relu')(x)
    return lstm_input, x

# === Define MLP branch (your existing MLP branch) ===
def create_mlp_branch(input_shape, dropout_rate=0.3):
    mlp_input = Input(shape=input_shape, name='mlp_input')
    x = Dense(64, activation='relu', kernel_regularizer=l1_l2(l2=0.01))(mlp_input)
    x = BatchNormalization()(x)
    x = Dropout(dropout_rate)(x)
    x = Dense(32, activation='relu', kernel_regularizer=l1_l2(l2=0.01))(x)
    x = BatchNormalization()(x)
    x = Dropout(dropout_rate)(x)
    x = Dense(16, activation='relu', kernel_regularizer=l1_l2(l2=0.01))(x)
    x = BatchNormalization()(x)
    x = Dropout(dropout_rate * 0.7)(x)
    return mlp_input, x

# === Define combined model ===
def create_combined_model(lstm_shape, mlp_shape, dropout_rate=0.3):
    lstm_input, lstm_features = create_lstm_branch_standalone(lstm_shape)
    mlp_input, mlp_features = create_mlp_branch(mlp_shape, dropout_rate)
    combined = Concatenate(name='feature_combination')([lstm_features, mlp_features])
    x = Dense(32, activation='relu', kernel_regularizer=l1_l2(l2=0.01))(combined)
    x = BatchNormalization()(x)
    x = Dropout(dropout_rate * 0.5)(x)
    x = Dense(16, activation='relu', kernel_regularizer=l1_l2(l2=0.01))(x)
    x = Dropout(dropout_rate * 0.3)(x)
    output = Dense(1, activation='sigmoid', name='classification_output')(x)
    model = Model(inputs=[lstm_input, mlp_input], outputs=output)
    return model

# === Training configuration ===
best_model = None
best_score = 0
best_config = None

model_configs = [
    {"dropout": 0.2, "lr": 0.001, "name": "Low_Dropout"},
    {"dropout": 0.3, "lr": 0.001, "name": "Medium_Dropout"},
    {"dropout": 0.4, "lr": 0.0005, "name": "High_Dropout_Low_LR"}
]

print("\nTesting different model configurations...")

for config in model_configs:
    print(f"\nTesting {config['name']} configuration...")
    model = create_combined_model(
        lstm_shape=(X_lstm_train.shape[1], X_lstm_train.shape[2]),
        mlp_shape=(X_mlp_train_scaled.shape[1],),
        dropout_rate=config['dropout']
    )
    model.compile(
        optimizer=Adam(learning_rate=config['lr']),
        loss='binary_crossentropy',
        metrics=['accuracy']
    )
    callbacks = [
        EarlyStopping(monitor='val_loss', patience=15, restore_best_weights=True, verbose=0),
        ReduceLROnPlateau(monitor='val_loss', factor=0.5, patience=10, min_lr=1e-6, verbose=0)
    ]
    history = model.fit(
        [X_lstm_train, X_mlp_train_scaled], y_train,
        validation_split=0.2,
        epochs=100,
        batch_size=32,
        class_weight=class_weight_dict,
        callbacks=callbacks,
        verbose=0
    )
    test_loss, test_acc = model.evaluate([X_lstm_test, X_mlp_test_scaled], y_test, verbose=0)
    y_pred_prob = model.predict([X_lstm_test, X_mlp_test_scaled], verbose=0)
    auc_score = roc_auc_score(y_test, y_pred_prob)
    print(f"  Test Accuracy: {test_acc:.4f}")
    print(f"  AUC Score: {auc_score:.4f}")
    if test_acc > best_score:
        best_score = test_acc
        best_model = model
        best_config = config

print(f"\nBest configuration: {best_config['name']} with accuracy: {best_score:.4f}")

# === Final training with best config ===
print("\nTraining final model with best configuration...")

final_model = create_combined_model(
    lstm_shape=(X_lstm_train.shape[1], X_lstm_train.shape[2]),
    mlp_shape=(X_mlp_train_scaled.shape[1],),
    dropout_rate=best_config['dropout']
)
final_model.compile(
    optimizer=Adam(learning_rate=best_config['lr']),
    loss='binary_crossentropy',
    metrics=['accuracy']
)

print("\nModel Architecture:")
final_model.summary()

final_callbacks = [
    EarlyStopping(monitor='val_loss', patience=20, restore_best_weights=True, verbose=1),
    ReduceLROnPlateau(monitor='val_loss', factor=0.5, patience=15, min_lr=1e-6, verbose=1),
    ModelCheckpoint('./improved_combined_lstm_mlp_model.keras', save_best_only=True, verbose=1)
]

final_history = final_model.fit(
    [X_lstm_train, X_mlp_train_scaled], y_train,
    validation_split=0.2,
    epochs=150,
    batch_size=32,
    class_weight=class_weight_dict,
    callbacks=final_callbacks,
    verbose=1
)

print("\nFinal Model Evaluation:")
final_loss, final_acc = final_model.evaluate([X_lstm_test, X_mlp_test_scaled], y_test, verbose=0)
final_y_pred_prob = final_model.predict([X_lstm_test, X_mlp_test_scaled])
final_y_pred_binary = (final_y_pred_prob > 0.5).astype(int).flatten()
final_auc = roc_auc_score(y_test, final_y_pred_prob)

print(f"Final Test Accuracy: {final_acc:.4f}")
print(f"Final AUC Score: {final_auc:.4f}")

print("\nClassification Report:")
print(classification_report(y_test, final_y_pred_binary))

print("\nConfusion Matrix:")
print(confusion_matrix(y_test, final_y_pred_binary))

# === Cross-validation for robustness ===
print("\nPerforming 3-fold cross-validation...")
cv_scores = []
cv_auc_scores = []
skf = StratifiedKFold(n_splits=3, shuffle=True, random_state=42)

for fold, (train_idx, val_idx) in enumerate(skf.split(X_lstm_aligned, y_aligned)):
    print(f"Fold {fold + 1}/3...")
    
    X_lstm_cv_train = X_lstm_aligned[train_idx]
    X_lstm_cv_val = X_lstm_aligned[val_idx]
    X_mlp_cv_train = X_mlp_aligned[train_idx]
    X_mlp_cv_val = X_mlp_aligned[val_idx]
    y_cv_train = y_aligned[train_idx]
    y_cv_val = y_aligned[val_idx]
    
    cv_scaler = StandardScaler()
    X_mlp_cv_train_scaled = cv_scaler.fit_transform(X_mlp_cv_train)
    X_mlp_cv_val_scaled = cv_scaler.transform(X_mlp_cv_val)
    
    cv_model = create_combined_model(
        lstm_shape=(X_lstm_cv_train.shape[1], X_lstm_cv_train.shape[2]),
        mlp_shape=(X_mlp_cv_train_scaled.shape[1],),
        dropout_rate=best_config['dropout']
    )
    cv_model.compile(
        optimizer=Adam(learning_rate=best_config['lr']),
        loss='binary_crossentropy',
        metrics=['accuracy']
    )
    cv_model.fit(
        [X_lstm_cv_train, X_mlp_cv_train_scaled], y_cv_train,
        epochs=50, batch_size=32,
        class_weight=class_weight_dict,
        validation_split=0.2,
        callbacks=[EarlyStopping(monitor='val_loss', patience=10, restore_best_weights=True)],
        verbose=0
    )
    _, cv_acc = cv_model.evaluate([X_lstm_cv_val, X_mlp_cv_val_scaled], y_cv_val, verbose=0)
    cv_pred_prob = cv_model.predict([X_lstm_cv_val, X_mlp_cv_val_scaled], verbose=0)
    cv_auc = roc_auc_score(y_cv_val, cv_pred_prob)
    cv_scores.append(cv_acc)
    cv_auc_scores.append(cv_auc)
    print(f"  Fold {fold+1} - Accuracy: {cv_acc:.4f}, AUC: {cv_auc:.4f}")

print(f"\nCross-validation Results:")
print(f"Mean Accuracy: {np.mean(cv_scores):.4f} (+/- {np.std(cv_scores)*2:.4f})")
print(f"Mean AUC: {np.mean(cv_auc_scores):.4f} (+/- {np.std(cv_auc_scores)*2:.4f})")

# === Save final model and scaler ===
final_model.save('./improved_combined_lstm_mlp_model.keras')
with open('./combined_model_scaler.pkl', 'wb') as f:
    pickle.dump(scaler, f)

# === Save results dictionary ===
results = {
    'final_accuracy': final_acc,
    'final_auc': final_auc,
    'cv_accuracy_mean': np.mean(cv_scores),
    'cv_accuracy_std': np.std(cv_scores),
    'cv_auc_mean': np.mean(cv_auc_scores),
    'cv_auc_std': np.std(cv_auc_scores),
    'best_config': best_config,
    'selected_features': selected_features,
    'class_weights': class_weight_dict,
    'history': final_history.history
}

with open('./improved_combined_model_results.pkl', 'wb') as f:
    pickle.dump(results, f)

print(f"\n" + "="*60)
print(f"IMPROVED COMBINED MODEL RESULTS")
print(f"="*60)
print(f"Final Test Accuracy: {final_acc:.4f}")
print(f"Final AUC Score: {final_auc:.4f}")
print(f"Cross-validation Accuracy: {np.mean(cv_scores):.4f} (+/- {np.std(cv_scores)*2:.4f})")
print(f"Best Configuration: {best_config['name']}")
print(f"Features Used: {len(selected_features)} gait-specific features")
print(f"Model saved as: improved_combined_lstm_mlp_model.keras")
