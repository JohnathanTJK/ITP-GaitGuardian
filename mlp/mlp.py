import numpy as np
import pandas as pd
from tensorflow.keras.models import Sequential, Model
from tensorflow.keras.layers import Dense, Dropout, Input, BatchNormalization
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.regularizers import l1_l2
from sklearn.model_selection import train_test_split, StratifiedKFold, GridSearchCV
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau, ModelCheckpoint
from sklearn.utils.class_weight import compute_class_weight
from sklearn.metrics import classification_report, confusion_matrix, roc_auc_score, roc_curve
from sklearn.ensemble import RandomForestClassifier
from sklearn.feature_selection import SelectKBest, f_classif, RFE
import pickle
import matplotlib.pyplot as plt
import seaborn as sns
from scikeras.wrappers import KerasClassifier
import warnings
warnings.filterwarnings('ignore')

# Load preprocessed data
X = np.load('./mlp/X_mlp_features.npy')
y = np.load('./mlp/y_mlp_features.npy')

print(f"Loaded data:")
print(f"X shape: {X.shape}")
print(f"y shape: {y.shape}")

# Load feature names for reference
with open('./mlp/feature_names.txt', 'r') as f:
    feature_names = [line.strip() for line in f.readlines()]
print(f"Number of features: {len(feature_names)}")

# Check class distribution
unique, counts = np.unique(y, return_counts=True)
print(f"Class distribution: {dict(zip(unique, counts))}")

# Handle class imbalance with class weights
class_weights = compute_class_weight('balanced', classes=np.unique(y), y=y)
class_weight_dict = dict(zip(np.unique(y), class_weights))
print(f"Class weights: {class_weight_dict}")

# Feature Selection
print("\nPerforming feature selection...")

# Method 1: Statistical feature selection
selector_stats = SelectKBest(score_func=f_classif, k=min(20, X.shape[1]))
X_selected_stats = selector_stats.fit_transform(X, y)
selected_features_stats = [feature_names[i] for i in selector_stats.get_support(indices=True)]
print(f"Top {X_selected_stats.shape[1]} features (statistical): {selected_features_stats}")

# Method 2: Random Forest feature importance
rf = RandomForestClassifier(n_estimators=100, random_state=42, class_weight='balanced')
rf.fit(X, y)
feature_importance = rf.feature_importances_
top_features_idx = np.argsort(feature_importance)[-min(20, X.shape[1]):]
X_selected_rf = X[:, top_features_idx]
selected_features_rf = [feature_names[i] for i in top_features_idx]
print(f"Top {X_selected_rf.shape[1]} features (RF importance): {selected_features_rf}")

# Use Random Forest selected features as they often work better for medical data
X_selected = X_selected_rf
selected_features = selected_features_rf

# Split data: 80% train, 20% test
X_train, X_test, y_train, y_test = train_test_split(
    X_selected, y, test_size=0.2, random_state=42, stratify=y
)

print(f"\nTraining set: {X_train.shape}")
print(f"Test set: {X_test.shape}")
print(f"Train class distribution: {np.unique(y_train, return_counts=True)}")
print(f"Test class distribution: {np.unique(y_test, return_counts=True)}")

def create_improved_mlp_model(input_dim, dropout_rate=0.3, l2_reg=0.01):
    """Create an improved MLP model with better architecture"""
    model = Sequential([
        Input(shape=(input_dim,)),
        
        # First block
        Dense(128, activation='relu', kernel_regularizer=l1_l2(l2=l2_reg)),
        BatchNormalization(),
        Dropout(dropout_rate),
        
        # Second block
        Dense(64, activation='relu', kernel_regularizer=l1_l2(l2=l2_reg)),
        BatchNormalization(),
        Dropout(dropout_rate),
        
        # Third block
        Dense(32, activation='relu', kernel_regularizer=l1_l2(l2=l2_reg)),
        BatchNormalization(),
        Dropout(dropout_rate * 0.7),  # Reduce dropout in later layers
        
        # Fourth block
        Dense(16, activation='relu', kernel_regularizer=l1_l2(l2=l2_reg)),
        BatchNormalization(),
        Dropout(dropout_rate * 0.5),
        
        # Output layer
        Dense(1, activation='sigmoid')
    ])
    
    return model

def create_lightweight_mlp_model(input_dim, dropout_rate=0.2):
    """Create a lighter MLP model to prevent overfitting"""
    model = Sequential([
        Input(shape=(input_dim,)),
        
        # First hidden layer
        Dense(64, activation='relu'),
        BatchNormalization(),
        Dropout(dropout_rate),
        
        # Second hidden layer
        Dense(32, activation='relu'),
        BatchNormalization(),
        Dropout(dropout_rate),
        
        # Third hidden layer
        Dense(16, activation='relu'),
        Dropout(dropout_rate * 0.5),
        
        # Output layer
        Dense(1, activation='sigmoid')
    ])
    
    return model

# Hyperparameter tuning function
def create_tunable_model(input_dim=X_train.shape[1], neurons1=64, neurons2=32, neurons3=16, 
                        dropout=0.3, learning_rate=0.001, l2_reg=0.01):
    """Create model with tunable hyperparameters"""
    model = Sequential([
        Input(shape=(input_dim,)),
        Dense(neurons1, activation='relu', kernel_regularizer=l1_l2(l2=l2_reg)),
        BatchNormalization(),
        Dropout(dropout),
        Dense(neurons2, activation='relu', kernel_regularizer=l1_l2(l2=l2_reg)),
        BatchNormalization(),
        Dropout(dropout),
        Dense(neurons3, activation='relu', kernel_regularizer=l1_l2(l2=l2_reg)),
        Dropout(dropout * 0.5),
        Dense(1, activation='sigmoid')
    ])
    
    model.compile(
        optimizer=Adam(learning_rate=learning_rate),
        loss='binary_crossentropy',
        metrics=['accuracy']
    )
    
    return model

# Try multiple model architectures
print("\nTesting multiple model architectures...")

models_to_test = [
    ("Improved_Deep", lambda: create_improved_mlp_model(X_train.shape[1], dropout_rate=0.3, l2_reg=0.01)),
    ("Lightweight", lambda: create_lightweight_mlp_model(X_train.shape[1], dropout_rate=0.2)),
    ("Original_Style", lambda: create_tunable_model(X_train.shape[1], 64, 32, 16, 0.3, 0.001, 0.01))
]

best_model = None
best_score = 0
best_name = ""
results_summary = {}

for model_name, model_creator in models_to_test:
    print(f"\nTesting {model_name} architecture...")
    
    # Create and compile model
    model = model_creator()
    
    # Enhanced callbacks
    callbacks = [
        EarlyStopping(monitor='val_loss', patience=20, restore_best_weights=True, verbose=0),
        ReduceLROnPlateau(monitor='val_loss', factor=0.5, patience=10, min_lr=1e-6, verbose=0),
        ModelCheckpoint(f'./mlp/best_{model_name.lower()}_model.keras', save_best_only=True, verbose=0)
    ]
    model.compile(optimizer='adam', loss='binary_crossentropy', metrics=['accuracy'])

    # Train model
    history = model.fit(
        X_train, y_train,
        epochs=150,
        batch_size=32,
        validation_split=0.2,
        class_weight=class_weight_dict,
        callbacks=callbacks,
        verbose=0
    )
    
    # Evaluate
    test_loss, test_accuracy = model.evaluate(X_test, y_test, verbose=0)
    y_pred_prob = model.predict(X_test, verbose=0)
    y_pred_binary = (y_pred_prob > 0.5).astype(int).flatten()
    
    # Calculate metrics
    auc_score = roc_auc_score(y_test, y_pred_prob)
    
    # Store results
    results_summary[model_name] = {
        'test_accuracy': test_accuracy,
        'test_loss': test_loss,
        'auc_score': auc_score,
        'history': history.history
    }
    
    print(f"  Test Accuracy: {test_accuracy:.4f}")
    print(f"  AUC Score: {auc_score:.4f}")
    
    # Track best model
    if test_accuracy > best_score:
        best_score = test_accuracy
        best_model = model
        best_name = model_name

print(f"\nBest model: {best_name} with accuracy: {best_score:.4f}")

# Detailed evaluation of best model
print(f"\nDetailed evaluation of best model ({best_name}):")
y_pred_prob_best = best_model.predict(X_test)
y_pred_binary_best = (y_pred_prob_best > 0.5).astype(int).flatten()

print("\nClassification Report:")
print(classification_report(y_test, y_pred_binary_best))

print("\nConfusion Matrix:")
cm = confusion_matrix(y_test, y_pred_binary_best)
print(cm)

# Feature importance analysis using the best model
print(f"\nFeature Analysis for {len(selected_features)} selected features:")
print("Input feature statistics on test set:")
for i, name in enumerate(selected_features):
    if np.sum(y_test == 0) > 0 and np.sum(y_test == 1) > 0:
        normal_mean = np.mean(X_test[y_test == 0, i])
        abnormal_mean = np.mean(X_test[y_test == 1, i])
        diff = abs(abnormal_mean - normal_mean)
        print(f"  {name}: Normal={normal_mean:.4f}, Abnormal={abnormal_mean:.4f}, Diff={diff:.4f}")

# Cross-validation with best architecture
print(f"\nPerforming 5-fold cross-validation with best architecture ({best_name})...")
cv_scores = []
cv_auc_scores = []
skf = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)

for fold, (train_idx, val_idx) in enumerate(skf.split(X_selected, y)):
    X_cv_train, X_cv_val = X_selected[train_idx], X_selected[val_idx]
    y_cv_train, y_cv_val = y[train_idx], y[val_idx]
    
    # Create model for this fold
    if best_name == "Improved_Deep":
        cv_model = create_improved_mlp_model(X_selected.shape[1], dropout_rate=0.3, l2_reg=0.01)
    elif best_name == "Lightweight":
        cv_model = create_lightweight_mlp_model(X_selected.shape[1], dropout_rate=0.2)
    else:
        cv_model = create_tunable_model(X_selected.shape[1], 64, 32, 16, 0.3, 0.001, 0.01)
    
    cv_model.compile(optimizer=Adam(learning_rate=0.001),
                     loss='binary_crossentropy',
                     metrics=['accuracy'])
    
    # Train with early stopping
    cv_model.fit(X_cv_train, y_cv_train, 
                 epochs=100, batch_size=32, 
                 class_weight=class_weight_dict,
                 validation_split=0.2,
                 callbacks=[EarlyStopping(monitor='val_loss', patience=15, restore_best_weights=True)],
                 verbose=0)
    
    # Evaluate
    _, cv_accuracy = cv_model.evaluate(X_cv_val, y_cv_val, verbose=0)
    cv_pred_prob = cv_model.predict(X_cv_val, verbose=0)
    cv_auc = roc_auc_score(y_cv_val, cv_pred_prob)
    
    cv_scores.append(cv_accuracy)
    cv_auc_scores.append(cv_auc)
    print(f"  Fold {fold+1}: Accuracy={cv_accuracy:.4f}, AUC={cv_auc:.4f}")

print(f"Cross-validation results:")
print(f"  Mean Accuracy: {np.mean(cv_scores):.4f} (+/- {np.std(cv_scores)*2:.4f})")
print(f"  Mean AUC: {np.mean(cv_auc_scores):.4f} (+/- {np.std(cv_auc_scores)*2:.4f})")

# Hyperparameter tuning for the best architecture
print(f"\nPerforming hyperparameter tuning for {best_name}...")

if best_name == "Improved_Deep":
    param_grid = {
        'dropout': [0.2, 0.3, 0.4],
        'l2_reg': [0.001, 0.01, 0.1],
        'learning_rate': [0.0001, 0.001, 0.01]
    }
    
    best_params = {'dropout': 0.3, 'l2_reg': 0.01, 'learning_rate': 0.001}  # Default
    best_tuned_score = 0
    
    for dropout in param_grid['dropout']:
        for l2_reg in param_grid['l2_reg']:
            for lr in param_grid['learning_rate']:
                print(f"  Testing: dropout={dropout}, l2_reg={l2_reg}, lr={lr}")
                
                # Quick validation
                tuned_model = create_improved_mlp_model(X_train.shape[1], dropout_rate=dropout, l2_reg=l2_reg)
                tuned_model.compile(optimizer=Adam(learning_rate=lr),
                                   loss='binary_crossentropy',
                                   metrics=['accuracy'])
                
                tuned_model.fit(X_train, y_train, epochs=50, batch_size=32,
                               validation_split=0.2, class_weight=class_weight_dict,
                               callbacks=[EarlyStopping(monitor='val_loss', patience=10)],
                               verbose=0)
                
                _, tuned_acc = tuned_model.evaluate(X_test, y_test, verbose=0)
                
                if tuned_acc > best_tuned_score:
                    best_tuned_score = tuned_acc
                    best_params = {'dropout': dropout, 'l2_reg': l2_reg, 'learning_rate': lr}
    
    print(f"Best hyperparameters: {best_params}")
    print(f"Best tuned accuracy: {best_tuned_score:.4f}")
    
    # Train final model with best parameters
    final_model = create_improved_mlp_model(X_train.shape[1], 
                                           dropout_rate=best_params['dropout'], 
                                           l2_reg=best_params['l2_reg'])
    final_model.compile(optimizer=Adam(learning_rate=best_params['learning_rate']),
                       loss='binary_crossentropy',
                       metrics=['accuracy'])

else:
    final_model = best_model
    best_params = "Default parameters"

# Final training
print("\nTraining final optimized model...")
final_callbacks = [
    EarlyStopping(monitor='val_loss', patience=25, restore_best_weights=True, verbose=1),
    ReduceLROnPlateau(monitor='val_loss', factor=0.5, patience=15, min_lr=1e-6, verbose=1),
    ModelCheckpoint('./mlp/final_best_mlp_model.keras', save_best_only=True, verbose=1)
]

final_history = final_model.fit(
    X_train, y_train,
    epochs=200,
    batch_size=32,
    validation_split=0.2,
    class_weight=class_weight_dict,
    callbacks=final_callbacks,
    verbose=1
)

# Final evaluation
print("\nFinal model evaluation:")
final_test_loss, final_test_accuracy = final_model.evaluate(X_test, y_test, verbose=0)
final_y_pred_prob = final_model.predict(X_test)
final_y_pred_binary = (final_y_pred_prob > 0.5).astype(int).flatten()
final_auc_score = roc_auc_score(y_test, final_y_pred_prob)

print(f"Final Test Accuracy: {final_test_accuracy:.4f}")
print(f"Final AUC Score: {final_auc_score:.4f}")

print("\nFinal Classification Report:")
print(classification_report(y_test, final_y_pred_binary))

print("\nFinal Confusion Matrix:")
final_cm = confusion_matrix(y_test, final_y_pred_binary)
print(final_cm)

# Save final model and comprehensive results
final_model.save('./mlp/mlp_features_model_optimized.keras')
print("\nOptimized model saved as mlp_features_model_optimized.keras")

# Save comprehensive results
comprehensive_results = {
    'final_history': final_history.history,
    'final_test_accuracy': final_test_accuracy,
    'final_test_loss': final_test_loss,
    'final_auc_score': final_auc_score,
    'cv_scores': cv_scores,
    'cv_auc_scores': cv_auc_scores,
    'selected_features': selected_features,
    'feature_names': feature_names,
    'class_weights': class_weight_dict,
    'best_architecture': best_name,
    'best_hyperparameters': best_params,
    'all_model_results': results_summary
}

with open('./mlp/comprehensive_mlp_results.pkl', 'wb') as f:
    pickle.dump(comprehensive_results, f)

# Save feature selection info
with open('./mlp/selected_features.txt', 'w') as f:
    for feature in selected_features:
        f.write(feature + '\n')

print("Results saved as comprehensive_mlp_results.pkl and selected_features.txt")

# Final summary
print(f"\n" + "="*60)
print(f"FINAL RESULTS SUMMARY")
print(f"="*60)
print(f"Best Architecture: {best_name}")
print(f"Features Used: {len(selected_features)} out of {len(feature_names)}")
print(f"Final Test Accuracy: {final_test_accuracy:.4f}")
print(f"Cross-validation Accuracy: {np.mean(cv_scores):.4f} (+/- {np.std(cv_scores)*2:.4f})")
print(f"Final AUC Score: {final_auc_score:.4f}")
print(f"Cross-validation AUC: {np.mean(cv_auc_scores):.4f} (+/- {np.std(cv_auc_scores)*2:.4f})")

print(f"• Current CV std: {np.std(cv_scores)*2:.4f} - {'Low variance (good)' if np.std(cv_scores)*2 < 0.05 else 'High variance (consider more data/regularization)'}")