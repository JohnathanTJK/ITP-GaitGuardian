import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split, StratifiedKFold, GridSearchCV
from sklearn.preprocessing import StandardScaler, RobustScaler
from sklearn.neural_network import MLPClassifier
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier, VotingClassifier
from sklearn.metrics import classification_report, confusion_matrix, roc_auc_score, f1_score, make_scorer
from sklearn.feature_selection import SelectKBest, f_classif, mutual_info_classif
from imblearn.over_sampling import SMOTE, BorderlineSMOTE, ADASYN
from imblearn.pipeline import Pipeline as ImbPipeline
from collections import Counter
import joblib
import json
import os
import warnings
warnings.filterwarnings('ignore')

# ==================== CONFIGURATION ====================
DATASET_PATH = "dataset_labelling.xlsx"
OUTPUT_DIR = "severity_models_enhanced"
FPS = 30
TRAINING_MODE = 'binary'  # 'binary' or 'multiclass'
RANDOM_STATE = 42

os.makedirs(OUTPUT_DIR, exist_ok=True)

# ==================== ENHANCED FEATURE ENGINEERING ====================

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

def extract_enhanced_features(row, fps=30):
    """
    Extract comprehensive features from TUG phase data
    Inspired by clinical TUG research and biomechanical analysis
    """
    phases = calculate_phase_durations(row, fps)
    
    # Basic phase durations
    sit_to_stand = phases.get('sit_to_stand', 0)
    walk_from = phases.get('walk_from_chair', 0)
    turn_first = phases.get('turn_first', 0)
    walk_to = phases.get('walk_to_chair', 0)
    turn_second = phases.get('turn_second', 0)
    stand_to_sit = phases.get('stand_to_sit', 0)
    
    # Aggregate metrics
    total_time = sum(phases.values())
    walk_time = walk_from + walk_to
    turn_time = turn_first + turn_second
    transition_time = sit_to_stand + stand_to_sit
    
    # Avoid division by zero
    eps = 1e-6
    
    features = {
        # ===== BASIC TIMING FEATURES =====
        'total_duration': total_time,
        'sit_to_stand_time': sit_to_stand,
        'walk_from_chair_time': walk_from,
        'turn_first_time': turn_first,
        'walk_to_chair_time': walk_to,
        'turn_second_time': turn_second,
        'stand_to_sit_time': stand_to_sit,
        'total_walk_time': walk_time,
        'total_turn_time': turn_time,
        'total_transition_time': transition_time,
        
        # ===== RATIO FEATURES (PHASE PROPORTIONS) =====
        'sit_to_stand_ratio': sit_to_stand / (total_time + eps),
        'walk_ratio': walk_time / (total_time + eps),
        'turn_ratio': turn_time / (total_time + eps),
        'transition_ratio': transition_time / (total_time + eps),
        'stand_to_sit_ratio': stand_to_sit / (total_time + eps),
        
        # ===== CLINICAL RATIOS (KEY DISCRIMINATORS) =====
        'turn_walk_ratio': turn_time / (walk_time + eps),  # Higher = worse turning
        'transition_walk_ratio': transition_time / (walk_time + eps),
        'sit_stand_asymmetry': abs(sit_to_stand - stand_to_sit) / (transition_time + eps),
        
        # ===== AVERAGE PHASE TIMES =====
        'avg_walk_time': walk_time / 2 if walk_time > 0 else 0,
        'avg_turn_time': turn_time / 2 if turn_time > 0 else 0,
        
        # ===== PHASE VARIABILITY =====
        'walk_asymmetry': abs(walk_from - walk_to) / (walk_time + eps),
        'turn_asymmetry': abs(turn_first - turn_second) / (turn_time + eps),
        
        # ===== PACE FEATURES =====
        'walk_pace': walk_time / (total_time + eps),  # Proportion spent walking
        'turn_pace': turn_time / (total_time + eps),  # Proportion spent turning
        
        # ===== SPEED INDICATORS (inverse of time) =====
        'sit_to_stand_speed': 1 / (sit_to_stand + eps),
        'walk_from_speed': 1 / (walk_from + eps),
        'turn_first_speed': 1 / (turn_first + eps),
        'walk_to_speed': 1 / (walk_to + eps),
        'turn_second_speed': 1 / (turn_second + eps),
        'stand_to_sit_speed': 1 / (stand_to_sit + eps),
        'overall_speed': 1 / (total_time + eps),
        
        # ===== CLINICAL THRESHOLDS =====
        # Binary flags based on clinical cutoffs
        'exceeds_normal_threshold': 1 if total_time > 10 else 0,  # >10s is abnormal
        'exceeds_risk_threshold': 1 if total_time > 13.5 else 0,  # >13.5s is fall risk
        'slow_sit_to_stand': 1 if sit_to_stand > 2.5 else 0,
        'slow_walking': 1 if walk_time > 5 else 0,
        'slow_turning': 1 if turn_time > 4 else 0,
        'slow_stand_to_sit': 1 if stand_to_sit > 3 else 0,
        
        # ===== INTERACTION FEATURES =====
        'turn_walk_product': turn_time * walk_time,
        'transition_turn_product': transition_time * turn_time,
        'sit_walk_product': sit_to_stand * walk_time,
        
        # ===== PHASE SEQUENCE FEATURES =====
        'first_half_time': sit_to_stand + walk_from + turn_first,
        'second_half_time': walk_to + turn_second + stand_to_sit,
        'first_second_asymmetry': abs((sit_to_stand + walk_from + turn_first) - 
                                      (walk_to + turn_second + stand_to_sit)) / (total_time + eps),
        
        # ===== POLYNOMIAL FEATURES (for non-linear relationships) =====
        'total_duration_squared': total_time ** 2,
        'turn_walk_ratio_squared': (turn_time / (walk_time + eps)) ** 2,
        'sit_to_stand_squared': sit_to_stand ** 2,
        
        # ===== LOG FEATURES (for skewed distributions) =====
        'log_total_duration': np.log1p(total_time),
        'log_turn_walk_ratio': np.log1p(turn_time / (walk_time + eps)),
        'log_sit_to_stand': np.log1p(sit_to_stand),
    }
    
    return features

def load_features_from_excel(excel_path, fps=30):
    """
    Extract enhanced features directly from Excel annotations
    """
    df = pd.read_excel(excel_path)
    df = df[df['Severity Classification'].notna()].copy()
    df = df[df['Severity Classification'] != '0-4']
    
    features_list = []
    
    for idx, row in df.iterrows():
        video_id = row['Video Name'].replace('.mp4', '').strip()
        
        # Extract enhanced features
        features = extract_enhanced_features(row, fps)
        features['video_id'] = video_id
        features['severity'] = int(row['Severity Classification'])
        
        features_list.append(features)
    
    df_features = pd.DataFrame(features_list)
    print(f"\n✅ Extracted features from {len(df_features)} videos")
    print(f"📊 Feature columns: {len(df_features.columns) - 2} (excluding video_id and severity)")
    
    return df_features

# ==================== DATA PREPARATION ====================

def prepare_data_binary(df_features):
    """Prepare data for binary classification"""
    df = df_features.copy()
    
    # Convert to binary: 0 = Normal, 1 = Impaired
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
    """Prepare data for multi-class classification"""
    df = df_features.copy()
    
    print("\n🌈 MULTI-CLASS CLASSIFICATION MODE")
    severity_counts = df['severity'].value_counts().sort_index()
    for sev, count in severity_counts.items():
        print(f"Severity {sev}: {count} videos")
    
    feature_cols = [col for col in df.columns if col not in ['video_id', 'severity']]
    X = df[feature_cols].values
    y = df['severity'].values
    
    class_names = [f'Severity {i}' for i in sorted(df['severity'].unique())]
    
    return X, y, feature_cols, class_names

# ==================== TRAINING WITH HYPERPARAMETER TUNING ====================

def train_mlp_with_tuning(X_train, y_train, X_test, y_test, mode='binary'):
    """
    Train MLP with comprehensive hyperparameter tuning
    """
    print("\n🔧 HYPERPARAMETER TUNING")
    
    # Define pipeline with SMOTE + Scaler + MLP
    # Determine k_neighbors for SMOTE
    min_samples = min(Counter(y_train).values())
    k_neighbors = min(5, min_samples - 1) if min_samples > 1 else 1
    
    pipeline = ImbPipeline([
        ('sampling', SMOTE(k_neighbors=k_neighbors, random_state=RANDOM_STATE)),
        ('scaler', RobustScaler()),  # RobustScaler is better for outliers
        ('classifier', MLPClassifier(random_state=RANDOM_STATE, max_iter=1000, early_stopping=True))
    ])
    
    # Hyperparameter grid
    param_grid = {
        'classifier__hidden_layer_sizes': [
            (64,),
            (128,),
            (64, 32),
            (128, 64),
            (128, 64, 32),
            (256, 128),
        ],
        'classifier__activation': ['relu', 'tanh'],
        'classifier__alpha': [0.0001, 0.001, 0.01],  # L2 regularization
        'classifier__learning_rate_init': [0.001, 0.01],
        'classifier__solver': ['adam'],
    }
    
    # Use F1-score for optimization (better for imbalanced data)
    scorer = make_scorer(f1_score, average='weighted')
    
    # Grid search with stratified K-fold
    grid_search = GridSearchCV(
        pipeline,
        param_grid,
        cv=StratifiedKFold(n_splits=5, shuffle=True, random_state=RANDOM_STATE),
        scoring=scorer,
        n_jobs=-1,
        verbose=1
    )
    
    print("🚀 Running GridSearchCV (this may take a few minutes)...")
    grid_search.fit(X_train, y_train)
    
    print(f"\n✅ Best parameters found:")
    for param, value in grid_search.best_params_.items():
        print(f"  {param}: {value}")
    print(f"\n📊 Best CV F1 Score: {grid_search.best_score_:.4f}")
    
    # Evaluate on test set
    y_pred = grid_search.predict(X_test)
    
    return grid_search.best_estimator_, y_pred, grid_search.best_params_

def train_ensemble(X_train, y_train, X_test, y_test):
    """
    Train ensemble of classifiers for better robustness
    """
    print("\n🎭 TRAINING ENSEMBLE")
    
    # Handle imbalance
    min_samples = min(Counter(y_train).values())
    k_neighbors = min(5, min_samples - 1) if min_samples > 1 else 1
    smote = SMOTE(k_neighbors=k_neighbors, random_state=RANDOM_STATE)
    X_train_resampled, y_train_resampled = smote.fit_resample(X_train, y_train)
    
    # Scale data
    scaler = RobustScaler()
    X_train_scaled = scaler.fit_transform(X_train_resampled)
    X_test_scaled = scaler.transform(X_test)
    
    # Individual classifiers
    mlp = MLPClassifier(
        hidden_layer_sizes=(128, 64),
        activation='relu',
        alpha=0.001,
        learning_rate_init=0.001,
        max_iter=1000,
        early_stopping=True,
        random_state=RANDOM_STATE
    )
    
    rf = RandomForestClassifier(
        n_estimators=200,
        max_depth=10,
        min_samples_split=5,
        random_state=RANDOM_STATE
    )
    
    gb = GradientBoostingClassifier(
        n_estimators=100,
        learning_rate=0.1,
        max_depth=5,
        random_state=RANDOM_STATE
    )
    
    # Voting ensemble
    ensemble = VotingClassifier(
        estimators=[('mlp', mlp), ('rf', rf), ('gb', gb)],
        voting='soft'
    )
    
    print("🚀 Training ensemble...")
    ensemble.fit(X_train_scaled, y_train_resampled)
    
    y_pred = ensemble.predict(X_test_scaled)
    
    return ensemble, y_pred, scaler

# ==================== FEATURE IMPORTANCE ====================

def analyze_feature_importance(X_train, y_train, feature_names, top_k=20):
    """
    Analyze feature importance using Random Forest
    """
    print(f"\n🔍 FEATURE IMPORTANCE ANALYSIS (Top {top_k})")
    
    # Handle imbalance
    min_samples = min(Counter(y_train).values())
    k_neighbors = min(5, min_samples - 1) if min_samples > 1 else 1
    smote = SMOTE(k_neighbors=k_neighbors, random_state=RANDOM_STATE)
    X_resampled, y_resampled = smote.fit_resample(X_train, y_train)
    
    # Train Random Forest
    rf = RandomForestClassifier(n_estimators=200, random_state=RANDOM_STATE)
    rf.fit(X_resampled, y_resampled)
    
    # Get feature importances
    importances = rf.feature_importances_
    indices = np.argsort(importances)[::-1]
    
    print("\nTop features:")
    for i in range(min(top_k, len(feature_names))):
        idx = indices[i]
        print(f"  {i+1}. {feature_names[idx]:<30} {importances[idx]:.4f}")
    
    return importances, indices

# ==================== MAIN TRAINING PIPELINE ====================

def main():
    print("=" * 60)
    print("🎯 MLP SEVERITY CLASSIFICATION")
    print("=" * 60)
    
    # Load and prepare data
    print(f"\n📂 Loading dataset from {DATASET_PATH}...")
    df_features = load_features_from_excel(DATASET_PATH, FPS)
    
    # Prepare data based on mode
    if TRAINING_MODE == 'binary':
        X, y, feature_cols, class_names = prepare_data_binary(df_features)
    else:
        X, y, feature_cols, class_names = prepare_data_multiclass(df_features)
    
    # Train-test split
    print(f"\n📊 Splitting data (80% train, 20% test)...")
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=RANDOM_STATE, stratify=y
    )
    
    print(f"Training set: {len(X_train)} samples")
    print(f"Test set: {len(X_test)} samples")
    print(f"Features: {len(feature_cols)}")
    
    # Feature importance analysis
    importances, indices = analyze_feature_importance(X_train, y_train, feature_cols)
    
    # Train with hyperparameter tuning
    best_model, y_pred_tuned, best_params = train_mlp_with_tuning(
        X_train, y_train, X_test, y_test, TRAINING_MODE
    )
    
    # Train ensemble
    ensemble, y_pred_ensemble, scaler = train_ensemble(
        X_train, y_train, X_test, y_test
    )
    
    # Evaluate both models
    print("\n" + "=" * 60)
    print("📊 TUNED MLP PERFORMANCE:")
    print("=" * 60)
    print(classification_report(y_test, y_pred_tuned, target_names=class_names))
    print("\nConfusion Matrix:")
    print(confusion_matrix(y_test, y_pred_tuned))
    if TRAINING_MODE == 'binary':
        print(f"\n🎯 ROC-AUC Score: {roc_auc_score(y_test, y_pred_tuned):.4f}")
    
    print("\n" + "=" * 60)
    print("📊 ENSEMBLE PERFORMANCE:")
    print("=" * 60)
    print(classification_report(y_test, y_pred_ensemble, target_names=class_names))
    print("\nConfusion Matrix:")
    print(confusion_matrix(y_test, y_pred_ensemble))
    if TRAINING_MODE == 'binary':
        print(f"\n🎯 ROC-AUC Score: {roc_auc_score(y_test, y_pred_ensemble):.4f}")
    
    # Save best model (choose based on F1 score)
    f1_tuned = f1_score(y_test, y_pred_tuned, average='weighted')
    f1_ensemble = f1_score(y_test, y_pred_ensemble, average='weighted')
    
    if f1_tuned >= f1_ensemble:
        print("\n💾 Saving tuned MLP model...")
        best_pipeline = best_model
        model_type = 'mlp_tuned'
        best_f1 = f1_tuned
    else:
        print("\n💾 Saving ensemble model...")
        best_pipeline = ensemble
        model_type = 'ensemble'
        best_f1 = f1_ensemble
    
    # Save artifacts
    model_path = os.path.join(OUTPUT_DIR, f'{model_type}_severity_{TRAINING_MODE}.pkl')
    joblib.dump(best_pipeline, model_path)
    print(f"✅ Model saved: {model_path}")
    
    # Save metadata
    metadata = {
        'mode': TRAINING_MODE,
        'model_type': model_type,
        'n_features': len(feature_cols),
        'feature_names': feature_cols,
        'class_names': class_names,
        'test_f1_score': float(best_f1),
        'best_params': best_params if model_type == 'mlp_tuned' else 'ensemble',
        'top_features': [feature_cols[idx] for idx in indices[:20]]
    }
    
    metadata_path = os.path.join(OUTPUT_DIR, f'{model_type}_metadata_{TRAINING_MODE}.json')
    with open(metadata_path, 'w') as f:
        json.dump(metadata, f, indent=2)
    print(f"✅ Metadata saved: {metadata_path}")
    
    print("\n" + "=" * 60)
    print("🎉 TRAINING COMPLETE!")
    print("=" * 60)
    print(f"📁 Model artifacts saved to: {OUTPUT_DIR}/")
    print(f"📊 Mode: {TRAINING_MODE}")
    print(f"🏆 Best Model: {model_type}")
    print(f"📈 Test F1 Score: {best_f1:.4f}")
    print(f"🔬 Total Features: {len(feature_cols)}")
    
    print("\n💡 TOP 10 MOST IMPORTANT FEATURES:")
    for i in range(min(10, len(feature_cols))):
        idx = indices[i]
        print(f"  {i+1}. {feature_cols[idx]}")

if __name__ == "__main__":
    main()