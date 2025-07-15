import numpy as np
import pickle
from tensorflow.keras.models import Sequential, load_model
from tensorflow.keras.layers import LSTM, Dense, Dropout, BatchNormalization, Input
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau, ModelCheckpoint
from sklearn.model_selection import StratifiedKFold, train_test_split
from sklearn.utils.class_weight import compute_class_weight
from sklearn.metrics import classification_report, confusion_matrix, roc_auc_score

# Load data and label encoder
X = np.load('./lstm/new_X.npy')
y = np.load('./lstm/new_y.npy')
with open('./lstm/new_label_encoder.pkl', 'rb') as f:
    label_encoder = pickle.load(f)

print(f"Data shape: {X.shape}, Labels shape: {y.shape}")

def create_lstm_model(input_shape):
    model = Sequential([
        Input(shape=input_shape),
        LSTM(64, return_sequences=True),
        Dropout(0.2),
        BatchNormalization(),
        LSTM(32),
        Dropout(0.2),
        BatchNormalization(),
        Dense(16, activation='relu'),
        Dense(1, activation='sigmoid')
    ])
    model.compile(
        optimizer=Adam(learning_rate=0.001),
        loss='binary_crossentropy',
        metrics=['accuracy']
    )
    return model

# --- Cross-validation setup ---
n_splits = 5
skf = StratifiedKFold(n_splits=n_splits, shuffle=True, random_state=42)

cv_accuracies = []
cv_aucs = []
best_fold_model_path = './lstm/best_lstm_fold_model.h5'
best_fold_acc = 0

for fold, (train_idx, val_idx) in enumerate(skf.split(X, y), 1):
    print(f"\n--- Fold {fold}/{n_splits} ---")
    X_train, X_val = X[train_idx], X[val_idx]
    y_train, y_val = y[train_idx], y[val_idx]

    # Compute class weights for this fold
    class_weights = compute_class_weight('balanced', classes=np.unique(y_train), y=y_train)
    class_weight_dict = dict(zip(np.unique(y_train), class_weights))
    print(f"Class weights: {class_weight_dict}")

    model = create_lstm_model((X.shape[1], X.shape[2]))

    callbacks = [
        EarlyStopping(monitor='val_loss', patience=10, restore_best_weights=True, verbose=1),
        ReduceLROnPlateau(monitor='val_loss', factor=0.5, patience=5, min_lr=1e-6, verbose=1),
        ModelCheckpoint(f'./lstm/lstm_fold_{fold}.h5', save_best_only=True, verbose=1)
    ]

    history = model.fit(
        X_train, y_train,
        epochs=100,
        batch_size=32,
        validation_data=(X_val, y_val),
        class_weight=class_weight_dict,
        callbacks=callbacks,
        verbose=1
    )

    val_loss, val_acc = model.evaluate(X_val, y_val, verbose=0)
    y_val_pred_prob = model.predict(X_val).flatten()
    y_val_pred = (y_val_pred_prob > 0.5).astype(int)
    val_auc = roc_auc_score(y_val, y_val_pred_prob)

    print(f"Fold {fold} val accuracy: {val_acc:.4f}")
    print(f"Fold {fold} val ROC AUC: {val_auc:.4f}")
    print("Classification Report:")
    print(classification_report(y_val, y_val_pred, target_names=[str(c) for c in label_encoder.classes_]))
    print("Confusion Matrix:")
    print(confusion_matrix(y_val, y_val_pred))

    cv_accuracies.append(val_acc)
    cv_aucs.append(val_auc)

    if val_acc > best_fold_acc:
        best_fold_acc = val_acc
        model.save(best_fold_model_path)
        print(f"Best fold model updated and saved to {best_fold_model_path}")

print(f"\nCross-validation accuracy: {np.mean(cv_accuracies):.4f} (+/- {np.std(cv_accuracies)*2:.4f})")
print(f"Cross-validation ROC AUC: {np.mean(cv_aucs):.4f} (+/- {np.std(cv_aucs)*2:.4f})")
