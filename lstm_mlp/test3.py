import numpy as np
from tensorflow.keras.models import load_model, Model
from tensorflow.keras.layers import Input, Concatenate, Dense, Dropout
from tensorflow.keras.optimizers import Adam
from sklearn.metrics import classification_report, confusion_matrix, roc_auc_score
from sklearn.model_selection import train_test_split

# === Load pretrained models ===
lstm_model = load_model('./lstm/new_lstm_model_final.h5')
mlp_model = load_model('./mlp/mlp_features_model_optimized.keras')

print("Loaded pretrained LSTM and MLP models.")

# === Load preprocessed and aligned data ===
X_lstm = np.load('./lstm/new_X.npy')       # Shape: (samples, time_steps, features)
X_mlp = np.load('./mlp/X_selected.npy')    # Shape: (samples, selected_features)
y = np.load('./mlp/y_selected.npy')        # Shape: (samples,)

# === Split data for evaluation ===
X_lstm_train, X_lstm_test, X_mlp_train, X_mlp_test, y_train, y_test = train_test_split(
    X_lstm, X_mlp, y, test_size=0.2, random_state=42, stratify=y
)

# === Create feature extractor models by applying all layers except the last ===
# For LSTM model
input_lstm = Input(shape=X_lstm.shape[1:], name="lstm_input")
x = input_lstm
for layer in lstm_model.layers[:-1]:  # skip final Dense output layer
    x = layer(x)
lstm_feature_extractor = Model(inputs=input_lstm, outputs=x)
print("Created LSTM feature extractor.")

# For MLP model
input_mlp = Input(shape=X_mlp.shape[1:], name="mlp_input")
x = input_mlp
for layer in mlp_model.layers[:-1]:  # skip final Dense output layer
    x = layer(x)
mlp_feature_extractor = Model(inputs=input_mlp, outputs=x)
print("Created MLP feature extractor.")

# === Define combined model ===
combined_input_lstm = Input(shape=X_lstm_train.shape[1:], name="combined_lstm_input")
combined_input_mlp = Input(shape=X_mlp_train.shape[1:], name="combined_mlp_input")

lstm_features = lstm_feature_extractor(combined_input_lstm)
mlp_features = mlp_feature_extractor(combined_input_mlp)

combined = Concatenate()([lstm_features, mlp_features])
combined = Dense(64, activation='relu')(combined)
combined = Dropout(0.3)(combined)
combined = Dense(32, activation='relu')(combined)
output = Dense(1, activation='sigmoid')(combined)

final_model = Model(inputs=[combined_input_lstm, combined_input_mlp], outputs=output)
final_model.compile(optimizer=Adam(learning_rate=0.001),
                    loss='binary_crossentropy',
                    metrics=['accuracy'])
final_model.summary()

# === Train the combined model ===
final_model.fit(
    [X_lstm_train, X_mlp_train], y_train,
    epochs=30,
    batch_size=32,
    validation_split=0.2,
    verbose=1
)

# === Evaluate on test set ===
test_loss, test_accuracy = final_model.evaluate([X_lstm_test, X_mlp_test], y_test, verbose=2)
y_pred_prob = final_model.predict([X_lstm_test, X_mlp_test])
y_pred = (y_pred_prob > 0.5).astype(int)

# === Print evaluation metrics ===
print("\nEvaluation Metrics:")
print(f"Test Loss: {test_loss:.4f}")
print(f"Test Accuracy: {test_accuracy:.4f}")
print("\nClassification Report:")
print(classification_report(y_test, y_pred))
print("\nConfusion Matrix:")
print(confusion_matrix(y_test, y_pred))
print(f"\nROC AUC Score: {roc_auc_score(y_test, y_pred_prob):.4f}")

# === Save the final combined model ===
final_model.save('./lstm_mlp/combined_pretrained_lstm_mlp.keras')
print("\nSaved combined model as combined_pretrained_lstm_mlp.keras")
