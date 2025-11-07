"""
MLP Severity Classification for GaitGuardian
Handles severe class imbalance with multiple training strategies:
1. Binary classification (Severity 0 vs Severity 1+) - RECOMMENDED for your dataset
2. Multi-class (0-4) with heavy class rebalancing
"""

import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split, StratifiedKFold, cross_val_score
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.neural_network import MLPClassifier
from sklearn.metrics import classification_report, confusion_matrix, roc_auc_score, f1_score
from imblearn.over_sampling import SMOTE, BorderlineSMOTE, ADASYN
from imblearn.under_sampling import RandomUnderSampler
from imblearn.combine import SMOTETomek
from collections import Counter
import joblib
import json
import os
import warnings
warnings.filterwarnings('ignore')

# ==================== CONFIGURATION ====================
DATASET_PATH = "dataset_labelling.xlsx"  # Your labeled dataset
RAW_LANDMARKS_DIR = "raw_landmarks_test"  # Directory with landmark CSVs
OUTPUT_DIR = "severity_models"
FPS = 30

# Training mode: 'binary' (0 vs 1+) or 'multiclass' (0-4)
TRAINING_MODE = 'binary'  # RECOMMENDED for your imbalanced dataset

os.makedirs(OUTPUT_DIR, exist_ok=True)

# ==================== DATA LOADING ====================

def load_labels(excel_path):
    """Load and clean severity labels from Excel"""
    df = pd.read_excel(excel_path)
    
    # Clean severity labels
    df = df[df['Severity Classification'].notna()].copy()
    df = df[df['Severity Classification'] != '0-4']  # Remove ambiguous label
    
    # Convert to integer
    df['Severity'] = df['Severity Classification'].astype(int)
    
    # Clean video names
    df['VideoID'] = df['Video Name'].str.replace('.mp4', '').str.strip()
    
    print(f"📊 Loaded {len(df)} labeled videos")
    print(f"\nSeverity distribution:")
    severity_dist = df['Severity'].value_counts().sort_index()
    for sev, count in severity_dist.items():
        pct = (count / len(df)) * 100
        print(f"  Severity {sev}: {count:3d} videos ({pct:5.1f}%)")
    
    return df[['VideoID', 'Severity']]

def calculate_phase_durations(row, fps=30):
    """Calculate phase durations from frame annotations"""
    phases = {}
    
    # Sit-To-Stand
    if pd.notna(row['Sit-To-Stand-Start-Frame']) and pd.notna(row['Sit-To-Stand-End-Frame']):
        phases['sit_to_stand'] = (row['Sit-To-Stand-End-Frame'] - row['Sit-To-Stand-Start-Frame']) / fps
    
    # Walk-From-Chair
    if pd.notna(row['Walk-From-Chair-Start-Frame']) and pd.notna(row['Walk-From-Chair-End-Frame']):
        phases['walk_from_chair'] = (row['Walk-From-Chair-End-Frame'] - row['Walk-From-Chair-Start-Frame']) / fps
    
    # Turn-First
    if pd.notna(row['Turn-First-Start-Frame']) and pd.notna(row['Turn-First-End-Frame']):
        phases['turn_first'] = (row['Turn-First-End-Frame'] - row['Turn-First-Start-Frame']) / fps
    
    # Walk-To-Chair
    if pd.notna(row['Walk-To-Chair-Start-Frame']) and pd.notna(row['Walk-To-Chair-End-Frame']):
        phases['walk_to_chair'] = (row['Walk-To-Chair-End-Frame'] - row['Walk-To-Chair-Start-Frame']) / fps
    
    # Turn-Second
    if pd.notna(row['Turn-Second-Start-Frame']) and pd.notna(row['Turn-Second-End-Frame']):
        phases['turn_second'] = (row['Turn-Second-End-Frame'] - row['Turn-Second-Start-Frame']) / fps
    
    # Stand-To-Sit
    if pd.notna(row['Stand-To-Sit-Start-Frame']) and pd.notna(row['Stand-To-Sit-End-Frame']):
        phases['stand_to_sit'] = (row['Stand-To-Sit-End-Frame'] - row['Stand-To-Sit-Start-Frame']) / fps
    
    return phases

def load_features_from_excel(excel_path, fps=30):
    """
    Extract features directly from Excel annotations
    This avoids needing the raw CSV files
    """
    df = pd.read_excel(excel_path)
    df = df[df['Severity Classification'].notna()].copy()
    df = df[df['Severity Classification'] != '0-4']
    
    features_list = []
    
    for idx, row in df.iterrows():
        video_id = row['Video Name'].replace('.mp4', '').strip()
        
        # Calculate phase durations
        phases = calculate_phase_durations(row, fps)
        
        # Calculate total duration and derived metrics
        total_time = sum(phases.values())
        walk_time = phases.get('walk_from_chair', 0) + phases.get('walk_to_chair', 0)
        turn_time = phases.get('turn_first', 0) + phases.get('turn_second', 0)
        turn_walk_ratio = turn_time / (walk_time + 1e-6)
        
        # Create feature vector
        features = {
            'video_id': video_id,
            'total_duration': total_time,
            'sit_to_stand_time': phases.get('sit_to_stand', 0),
            'walk_from_chair_time': phases.get('walk_from_chair', 0),
            'turn_first_time': phases.get('turn_first', 0),
            'walk_to_chair_time': phases.get('walk_to_chair', 0),
            'turn_second_time': phases.get('turn_second', 0),
            'stand_to_sit_time': phases.get('stand_to_sit', 0),
            'total_walk_time': walk_time,
            'total_turn_time': turn_time,
            'turn_walk_ratio': turn_walk_ratio,
            'avg_walk_time': walk_time / 2 if walk_time > 0 else 0,
            'avg_turn_time': turn_time / 2 if turn_time > 0 else 0,
            'sit_to_stand_ratio': phases.get('sit_to_stand', 0) / (total_time + 1e-6),
            'walk_ratio': walk_time / (total_time + 1e-6),
            'turn_ratio': turn_time / (total_time + 1e-6),
            'stand_to_sit_ratio': phases.get('stand_to_sit', 0) / (total_time + 1e-6),
            'severity': int(row['Severity Classification'])
        }
        
        features_list.append(features)
    
    df_features = pd.DataFrame(features_list)
    print(f"\n✅ Extracted features from {len(df_features)} videos")
    print(f"📊 Feature columns: {len(df_features.columns) - 2} (excluding video_id and severity)")
    
    return df_features

# ==================== TRAINING FUNCTIONS ====================

def prepare_data_binary(df_features):
    """Prepare data for binary classification: Severity 0 vs Severity 1+"""
    df = df_features.copy()
    
    # Convert to binary: 0 = Normal, 1 = Impaired (any severity > 0)
    df['binary_severity'] = (df['severity'] > 0).astype(int)
    
    print("\n🔵 BINARY CLASSIFICATION MODE")
    print(f"Class 0 (Normal): {(df['binary_severity'] == 0).sum()} videos")
    print(f"Class 1 (Impaired): {(df['binary_severity'] == 1).sum()} videos")
    
    # Extract features
    feature_cols = [col for col in df.columns if col not in ['video_id', 'severity', 'binary_severity']]
    X = df[feature_cols].values
    y = df['binary_severity'].values
    
    return X, y, feature_cols, ['Normal', 'Impaired']

def prepare_data_multiclass(df_features):
    """Prepare data for multi-class classification: Severity 0-4"""
    df = df_features.copy()
    
    print("\n🌈 MULTI-CLASS CLASSIFICATION MODE")
    severity_counts = df['severity'].value_counts().sort_index()
    for sev, count in severity_counts.items():
        print(f"Severity {sev}: {count} videos")
    
    # Extract features
    feature_cols = [col for col in df.columns if col not in ['video_id', 'severity']]
    X = df[feature_cols].values
    y = df['severity'].values
    
    class_names = [f'Severity {i}' for i in sorted(df['severity'].unique())]
    
    return X, y, feature_cols, class_names

def handle_class_imbalance(X_train, y_train, strategy='smote'):
    """
    Handle severe class imbalance with multiple strategies
    
    Strategies:
    - 'smote': Standard SMOTE oversampling
    - 'borderline': Borderline-SMOTE (focuses on boundary cases)
    - 'adasyn': ADASYN (adaptive synthetic sampling)
    - 'smotetomek': SMOTE + Tomek links cleaning
    - 'undersample': Random undersampling of majority class
    - 'combined': SMOTE + undersampling
    """
    print(f"\n⚖️ Handling class imbalance with strategy: {strategy}")
    print(f"Original distribution: {Counter(y_train)}")
    
    # Determine k_neighbors based on smallest class
    min_samples = min(Counter(y_train).values())
    k_neighbors = min(5, min_samples - 1) if min_samples > 1 else 1
    
    if strategy == 'smote':
        sampler = SMOTE(random_state=42, k_neighbors=k_neighbors)
    elif strategy == 'borderline':
        sampler = BorderlineSMOTE(random_state=42, k_neighbors=k_neighbors)
    elif strategy == 'adasyn':
        sampler = ADASYN(random_state=42, n_neighbors=k_neighbors)
    elif strategy == 'smotetomek':
        sampler = SMOTETomek(random_state=42, smote=SMOTE(k_neighbors=k_neighbors))
    elif strategy == 'undersample':
        sampler = RandomUnderSampler(random_state=42)
    elif strategy == 'combined':
        # SMOTE to oversample minorities + undersample majority
        from imblearn.combine import SMOTEENN
        sampler = SMOTEENN(random_state=42, smote=SMOTE(k_neighbors=k_neighbors))
    else:
        raise ValueError(f"Unknown strategy: {strategy}")
    
    X_resampled, y_resampled = sampler.fit_resample(X_train, y_train)
    
    print(f"Resampled distribution: {Counter(y_resampled)}")
    print(f"Training samples: {len(X_train)} → {len(X_resampled)}")
    
    return X_resampled, y_resampled

def train_mlp_classifier(X_train, y_train, X_test, y_test, class_names, mode='binary'):
    """Train MLP with optimized architecture for your dataset size"""
    
    print(f"\n🧠 Training MLP Classifier ({mode} mode)...")
    print(f"Training samples: {len(X_train)}")
    print(f"Test samples: {len(X_test)}")
    print(f"Features: {X_train.shape[1]}")
    
    # Standardize features
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)
    
    # Architecture choices for small dataset (100 videos)
    # Using smaller networks to prevent overfitting
    architectures = [
        (32,),           # Single layer - simplest
        (64, 32),        # Two layers - moderate
        (32, 16),        # Two layers - smaller
    ]
    
    best_model = None
    best_score = 0
    best_arch = None
    
    for arch in architectures:
        print(f"\n  Testing architecture: {arch}")
        
        # Use lighter regularization for small dataset
        mlp = MLPClassifier(
            hidden_layer_sizes=arch,
            activation='relu',
            solver='adam',
            alpha=0.001,  # L2 regularization
            learning_rate_init=0.001,
            max_iter=500,
            early_stopping=True,
            validation_fraction=0.15,
            n_iter_no_change=20,
            random_state=42,
            verbose=False
        )
        
        # Cross-validation on training set
        cv_scores = cross_val_score(mlp, X_train_scaled, y_train, cv=5, scoring='f1_weighted')
        avg_cv_score = cv_scores.mean()
        
        print(f"    CV F1 Score: {avg_cv_score:.4f} (±{cv_scores.std():.4f})")
        
        if avg_cv_score > best_score:
            best_score = avg_cv_score
            best_arch = arch
            best_model = mlp
    
    print(f"\n✅ Best architecture: {best_arch} (CV F1: {best_score:.4f})")
    
    # Train final model on full training set
    best_model.fit(X_train_scaled, y_train)
    
    # Evaluate on test set
    y_pred = best_model.predict(X_test_scaled)
    y_pred_proba = best_model.predict_proba(X_test_scaled)
    
    print("\n📊 TEST SET PERFORMANCE:")
    print(classification_report(y_test, y_pred, target_names=class_names, zero_division=0))
    
    print("\n📊 Confusion Matrix:")
    cm = confusion_matrix(y_test, y_pred)
    print(cm)
    
    # Additional metrics for binary classification
    if mode == 'binary' and len(class_names) == 2:
        try:
            auc = roc_auc_score(y_test, y_pred_proba[:, 1])
            print(f"\n🎯 ROC-AUC Score: {auc:.4f}")
        except:
            pass
    
    return best_model, scaler, best_arch

def save_model_artifacts(model, scaler, feature_names, class_names, mode, architecture):
    """Save all model artifacts for deployment"""
    
    print(f"\n💾 Saving model artifacts to {OUTPUT_DIR}/...")
    
    # Save model
    model_path = os.path.join(OUTPUT_DIR, f"mlp_severity_{mode}.pkl")
    joblib.dump(model, model_path)
    print(f"✅ Model saved: {model_path}")
    
    # Save scaler
    scaler_path = os.path.join(OUTPUT_DIR, f"severity_scaler_{mode}.pkl")
    joblib.dump(scaler, scaler_path)
    print(f"✅ Scaler saved: {scaler_path}")
    
    # Save metadata
    metadata = {
        'mode': mode,
        'feature_names': feature_names,
        'class_names': class_names,
        'num_features': len(feature_names),
        'num_classes': len(class_names),
        'architecture': list(architecture),
        'scaler_mean': scaler.mean_.tolist(),
        'scaler_scale': scaler.scale_.tolist()
    }
    
    metadata_path = os.path.join(OUTPUT_DIR, f"severity_metadata_{mode}.json")
    with open(metadata_path, 'w') as f:
        json.dump(metadata, f, indent=2)
    print(f"✅ Metadata saved: {metadata_path}")
    
    return model_path, scaler_path, metadata_path

# ==================== MAIN TRAINING PIPELINE ====================

def main():
    print("="*60)
    print("🎯 MLP SEVERITY CLASSIFICATION TRAINING")
    print("="*60)
    
    # Load features from Excel
    print(f"\n📂 Loading dataset from {DATASET_PATH}...")
    df_features = load_features_from_excel(DATASET_PATH, FPS)
    
    # Choose training mode
    if TRAINING_MODE == 'binary':
        X, y, feature_cols, class_names = prepare_data_binary(df_features)
        imbalance_strategy = 'smote'  # Works well for binary with moderate imbalance
    else:
        X, y, feature_cols, class_names = prepare_data_multiclass(df_features)
        imbalance_strategy = 'smotetomek'  # More aggressive for severe multiclass imbalance
    
    # Train/test split (80/20) with stratification
    print("\n📊 Splitting data (80% train, 20% test)...")
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, stratify=y, random_state=42
    )
    
    print(f"Training set: {len(X_train)} samples")
    print(f"Test set: {len(X_test)} samples")
    
    # Handle class imbalance
    X_train_balanced, y_train_balanced = handle_class_imbalance(
        X_train, y_train, strategy=imbalance_strategy
    )
    
    # Train model
    model, scaler, architecture = train_mlp_classifier(
        X_train_balanced, y_train_balanced, 
        X_test, y_test, 
        class_names, 
        mode=TRAINING_MODE
    )
    
    # Save artifacts
    save_model_artifacts(model, scaler, feature_cols, class_names, TRAINING_MODE, architecture)
    
    print("\n" + "="*60)
    print("🎉 TRAINING COMPLETE!")
    print("="*60)
    print(f"\n📁 Model artifacts saved to: {OUTPUT_DIR}/")
    print(f"📊 Mode: {TRAINING_MODE}")
    print(f"🏗️ Architecture: {architecture}")
    print(f"📦 Ready for ONNX conversion and Android deployment")
    
    # Recommendations
    print("\n💡 RECOMMENDATIONS:")
    if TRAINING_MODE == 'binary':
        print("✅ Binary classification (0 vs 1+) is RECOMMENDED for your dataset")
        print("   due to severe class imbalance in higher severity levels.")
    else:
        print("⚠️  Multi-class (0-4) classification may suffer from low precision")
        print("   on Severity 2-4 due to limited training samples.")
        print("   Consider using binary mode or collecting more high-severity videos.")

if __name__ == "__main__":
    main()