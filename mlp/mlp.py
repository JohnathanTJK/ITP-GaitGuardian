import numpy as np
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Dropout, Input
from tensorflow.keras.optimizers import Adam
from sklearn.model_selection import train_test_split, StratifiedKFold
from tensorflow.keras.callbacks import EarlyStopping
from sklearn.utils.class_weight import compute_class_weight
from sklearn.metrics import classification_report, confusion_matrix, roc_auc_score
import pickle

# Load preprocessed data
X = np.load('./mlp/X_mlp_features.npy')
y = np.load('./mlp/y_mlp_features.npy')

print(f"Loaded data:")
print(f"X shape: {X.shape}")
print(f"y shape: {y.shape}")

# Load feature names for reference
with open('./mlp/feature_names.txt', 'r') as f:
    feature_names = [line.strip() for line in f.readlines()]
print(f"Features: {feature_names}")

# Check class distribution
unique, counts = np.unique(y, return_counts=True)
print(f"Class distribution: {dict(zip(unique, counts))}")

# Handle class imbalance with class weights
class_weights = compute_class_weight('balanced', classes=np.unique(y), y=y)
class_weight_dict = dict(zip(np.unique(y), class_weights))
print(f"Class weights: {class_weight_dict}")

# Split data: 80% train, 20% test
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

print(f"Training set: {X_train.shape}")
print(f"Test set: {X_test.shape}")
print(f"Train class distribution: {np.unique(y_train, return_counts=True)}")
print(f"Test class distribution: {np.unique(y_test, return_counts=True)}")

# Build MLP model for extracted features
def create_mlp_model(input_dim):
    model = Sequential([
        Input(shape=(input_dim,)),
        
        # First hidden layer
        Dense(64, activation='relu'),
        Dropout(0.3),
        
        # Second hidden layer  
        Dense(32, activation='relu'),
        Dropout(0.3),
        
        # Third hidden layer
        Dense(16, activation='relu'),
        Dropout(0.2),
        
        # Output layer
        Dense(1, activation='sigmoid')  # Binary classification
    ])
    
    return model

model = create_mlp_model(X_train.shape[1])

# Compile model
model.compile(
    optimizer=Adam(learning_rate=0.001),
    loss='binary_crossentropy',
    metrics=['accuracy']
)

model.summary()

# Callbacks
early_stopping = EarlyStopping(
    monitor='val_loss', 
    patience=15, 
    restore_best_weights=True,
    verbose=1
)

# Train model with class weights to handle imbalance
print("Starting training...")
history = model.fit(
    X_train, y_train,
    epochs=100,
    batch_size=16,  # Smaller batch size for limited features
    validation_split=0.2,
    class_weight=class_weight_dict,  # Handle class imbalance
    callbacks=[early_stopping],
    verbose=1
)

# Evaluate on test set
print("\nEvaluating on test set...")
test_loss, test_accuracy = model.evaluate(X_test, y_test, verbose=2)
print(f"Test Loss: {test_loss:.4f}")
print(f"Test Accuracy: {test_accuracy:.4f}")

# Make predictions
y_pred_prob = model.predict(X_test)
y_pred_binary = (y_pred_prob > 0.5).astype(int).flatten()

# Detailed evaluation
print("\nClassification Report:")
print(classification_report(y_test, y_pred_binary))

print("\nConfusion Matrix:")
cm = confusion_matrix(y_test, y_pred_binary)
print(cm)

# Calculate AUC score
auc_score = roc_auc_score(y_test, y_pred_prob)
print(f"\nAUC Score: {auc_score:.4f}")

# Feature importance analysis (approximate)
print("\nFeature Analysis:")
print("Input feature statistics on test set:")
for i, name in enumerate(feature_names):
    normal_mean = np.mean(X_test[y_test == 0, i])
    abnormal_mean = np.mean(X_test[y_test == 1, i])
    print(f"  {name}: Normal={normal_mean:.4f}, Abnormal={abnormal_mean:.4f}, Diff={abs(abnormal_mean-normal_mean):.4f}")

# Cross-validation for more robust evaluation
print("\nPerforming 5-fold cross-validation...")
cv_scores = []
skf = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)

for fold, (train_idx, val_idx) in enumerate(skf.split(X, y)):
    X_cv_train, X_cv_val = X[train_idx], X[val_idx]
    y_cv_train, y_cv_val = y[train_idx], y[val_idx]
    
    # Create and train model for this fold
    cv_model = create_mlp_model(X.shape[1])
    cv_model.compile(optimizer=Adam(learning_rate=0.001),
                     loss='binary_crossentropy',
                     metrics=['accuracy'])
    
    cv_model.fit(X_cv_train, y_cv_train, 
                 epochs=50, batch_size=16, 
                 class_weight=class_weight_dict,
                 verbose=0)
    
    # Evaluate
    _, cv_accuracy = cv_model.evaluate(X_cv_val, y_cv_val, verbose=0)
    cv_scores.append(cv_accuracy)
    print(f"  Fold {fold+1}: {cv_accuracy:.4f}")

print(f"Cross-validation mean accuracy: {np.mean(cv_scores):.4f} (+/- {np.std(cv_scores)*2:.4f})")

# Save model and results
model.save('./mlp/mlp_features_model.h5')
print("\nModel saved as mlp_features_model.h5")

# Save training history and results
results = {
    'history': history.history,
    'test_accuracy': test_accuracy,
    'test_loss': test_loss,
    'auc_score': auc_score,
    'cv_scores': cv_scores,
    'feature_names': feature_names,
    'class_weights': class_weight_dict
}

with open('./mlp/mlp_features_results.pkl', 'wb') as f:
    pickle.dump(results, f)
print("Results saved as mlp_features_results.pkl")

print(f"\nFinal Results Summary:")
print(f"Test Accuracy: {test_accuracy:.4f}")
print(f"Cross-validation Accuracy: {np.mean(cv_scores):.4f}")
print(f"AUC Score: {auc_score:.4f}")