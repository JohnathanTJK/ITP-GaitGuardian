"""
🚀 Enhanced XGBoost Training for TUG Phase Classification
Addresses temporal learning and data leakage issues
Enhanced with SAIL research-inspired features for 6-phase TUG assessment
"""

import os
import pandas as pd
import numpy as np
from xgboost import XGBClassifier
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
from sklearn.utils.class_weight import compute_class_weight
import matplotlib.pyplot as plt
import seaborn as sns
import joblib
from collections import Counter

def video_based_train_val_split(df, val_size=0.2, random_state=42):
    """Split data by videos to prevent temporal leakage (train/validation)"""
    print("🔄 Performing video-based train/validation split to prevent temporal leakage...")
    
    # Get unique videos
    unique_videos = df['video_id'].unique()
    np.random.seed(random_state)
    np.random.shuffle(unique_videos)
    
    # Split videos (80% train, 20% validation)
    n_val_videos = int(len(unique_videos) * val_size)
    val_videos = unique_videos[:n_val_videos]
    train_videos = unique_videos[n_val_videos:]
    
    # Split dataframe
    train_df = df[df['video_id'].isin(train_videos)].copy()
    val_df = df[df['video_id'].isin(val_videos)].copy()
    
    print(f"📊 Video split: {len(train_videos)} train videos, {len(val_videos)} validation videos")
    print(f"📊 Frame split: {len(train_df)} train frames, {len(val_df)} validation frames")
    print(f"📊 Split ratio: {len(train_df)/(len(train_df)+len(val_df))*100:.1f}% train, {len(val_df)/(len(train_df)+len(val_df))*100:.1f}% validation")
    
    return train_df, val_df, train_videos, val_videos

def analyze_temporal_features(feature_cols):
    """Identify and weight temporal features"""
    temporal_keywords = [
        'velocity', 'acceleration', 'diff', 'change', 'rate', 'momentum',
        'rolling', 'mean', 'std', 'range', 'jerk', 'angular', 'rotation',
        'timing', 'variability', 'frequency', 'power', 'coordination'
    ]
    
    temporal_features = []
    static_features = []
    
    for feature in feature_cols:
        is_temporal = any(keyword in feature.lower() for keyword in temporal_keywords)
        if is_temporal:
            temporal_features.append(feature)
        else:
            static_features.append(feature)
    
    print(f"🕒 Temporal features: {len(temporal_features)} ({len(temporal_features)/len(feature_cols)*100:.1f}%)")
    print(f"📐 Static features: {len(static_features)} ({len(static_features)/len(feature_cols)*100:.1f}%)")
    
    return temporal_features, static_features

def load_and_prepare_data(features_dir):
    """Load all feature files and prepare training dataset"""
    print("📊 Loading feature files with TUG subtask labels...")
    
    all_data = []
    file_count = 0
    
    for file in os.listdir(features_dir):
        if not file.endswith("_features.csv"):
            continue
            
        file_path = os.path.join(features_dir, file)
        try:
            df = pd.read_csv(file_path)
            
            # Check if labels are present
            if 'tug_subtask' not in df.columns:
                print(f"⚠️ No labels found in {file}, skipping...")
                continue
                
            # Add video identifier
            video_id = file.replace("_color_features.csv", "")
            df['video_id'] = video_id
            
            all_data.append(df)
            file_count += 1
            
            if file_count % 50 == 0:
                print(f"   📁 Loaded {file_count} files...")
                
        except Exception as e:
            print(f"❌ Error loading {file}: {e}")
            continue
    
    if not all_data:
        raise ValueError("❌ No valid feature files found with labels!")
    
    # Combine all data
    combined_df = pd.concat(all_data, ignore_index=True)
    print(f"✅ Loaded {file_count} files with {len(combined_df)} total frames")
    
    return combined_df

def prepare_features_and_labels(df, label_encoder=None):
    """Prepare features and labels for training"""
    print("🔧 Preparing features and labels...")

    # Remove rows with NaN labels
    df_clean = df.dropna(subset=['tug_subtask'])
    print(f"🧹 Removed {len(df) - len(df_clean)} rows with missing labels")

    # Remove non-feature columns
    feature_cols = [col for col in df_clean.columns if col not in ['frame', 'tug_subtask', 'video_id']]

    # Remove label-derived features to prevent leakage
    leakage_features = ['phase_change_forward', 'phase_change_backward', 'phase_duration']
    feature_cols = [col for col in feature_cols if col not in leakage_features]

    X = df_clean[feature_cols]
    y = df_clean['tug_subtask']

    # Handle missing values in features
    X = X.fillna(0)

    # Remove infinite values
    X = X.replace([np.inf, -np.inf], 0)

    # Encode labels
    if label_encoder is None:
        label_encoder = LabelEncoder()
        y_encoded = label_encoder.fit_transform(y)
        is_new_encoder = True
    else:
        y_encoded = label_encoder.transform(y)
        is_new_encoder = False

    if is_new_encoder:
        print(f"📈 Features shape: {X.shape}")
        print(f"🏷️ Label distribution:")
        for i, label in enumerate(label_encoder.classes_):
            count = sum(y_encoded == i)
            print(f"   {label}: {count} samples ({count/len(y_encoded)*100:.1f}%)")

    return X, y_encoded, label_encoder, feature_cols

def train_temporal_aware_xgboost(X_train, y_train, X_val, y_val):
    """Train XGBoost with class balancing and early stopping"""
    print("🚀 Training XGBoost model...")
    
    # Calculate class weights for imbalanced data
    classes = np.unique(y_train)
    class_weights = compute_class_weight('balanced', classes=classes, y=y_train)
    sample_weights = np.array([class_weights[y] for y in y_train])
    
    print("⚖️ Using class balancing with sample weights")
    
    # Enhanced XGBoost parameters for temporal learning
    model = XGBClassifier(
        objective='multi:softprob',
        n_estimators=300,
        max_depth=12,
        learning_rate=0.03,
        subsample=0.8,
        colsample_bytree=0.8,
        colsample_bylevel=0.8,
        gamma=1,
        min_child_weight=3,
        reg_alpha=0.1,
        reg_lambda=1.0,
        random_state=42,
        n_jobs=-1,
        eval_metric='mlogloss',
        early_stopping_rounds=50
    )
    
    print("🔍 Training parameters:")
    print(f"   n_estimators: {model.n_estimators}")
    print(f"   max_depth: {model.max_depth}")
    print(f"   learning_rate: {model.learning_rate}")
    print(f"   Sample weighting: Enabled for class balance")
    print(f"   Early stopping: {model.early_stopping_rounds} rounds")
    print(f"   Regularization: L1={model.reg_alpha}, L2={model.reg_lambda}")
    
    # Train with sample weights and early stopping
    model.fit(
        X_train, y_train,
        sample_weight=sample_weights,
        eval_set=[(X_train, y_train), (X_val, y_val)],
        verbose=25
    )
    
    # Report best iteration
    best_iteration = model.best_iteration
    print(f"\n🏆 Best iteration: {best_iteration}")
    print(f"📊 Best validation score: {model.best_score:.4f}")
    
    return model

def evaluate_model(model, X_val, y_val, y_pred, label_encoder, dataset_name="Validation"):
    """Evaluate model performance"""
    print(f"\n📊 {dataset_name} Set Evaluation Results:")
    print("=" * 50)
    
    # Classification report
    class_names = label_encoder.classes_
    report = classification_report(y_val, y_pred, target_names=class_names)
    print(report)
    
    # Confusion matrix
    cm = confusion_matrix(y_val, y_pred)
    
    plt.figure(figsize=(12, 8))
    sns.heatmap(cm, annot=True, fmt='d', cmap='Blues', 
                xticklabels=class_names, yticklabels=class_names)
    plt.title(f'TUG Phase Classification - {dataset_name} Confusion Matrix')
    plt.xlabel('Predicted Phase')
    plt.ylabel('True Phase')
    plt.xticks(rotation=45, ha='right')
    plt.yticks(rotation=0)
    plt.tight_layout()
    filename = f'confusion_matrix_{dataset_name.lower()}.png'
    plt.savefig(filename, dpi=300, bbox_inches='tight')
    plt.close()
    print(f"✅ Saved confusion matrix: {filename}")
    
    return cm

def analyze_feature_importance(model, feature_cols, top_n=20):
    """Analyze and visualize feature importance"""
    print(f"\n🔬 Feature Importance Analysis:")
    print("=" * 50)
    
    # Get feature importance
    importance = model.feature_importances_
    feature_importance = pd.DataFrame({
        'feature_name': feature_cols,
        'importance': importance
    }).sort_values('importance', ascending=False)
    
    # Top features overall
    print(f"\n🔝 Top {top_n} Most Important Features:")
    for i, (_, row) in enumerate(feature_importance.head(top_n).iterrows()):
        print(f"{i+1:2d}. {row['feature_name']:<40} {row['importance']:.4f}")
    
    # Visualization
    plt.figure(figsize=(10, 8))
    top_features = feature_importance.head(top_n)
    plt.barh(range(len(top_features)), top_features['importance'], color='steelblue')
    plt.yticks(range(len(top_features)), top_features['feature_name'])
    plt.xlabel('Feature Importance')
    plt.title(f'Top {top_n} Most Important Features')
    plt.gca().invert_yaxis()
    plt.tight_layout()
    plt.savefig('feature_importance.png', dpi=300, bbox_inches='tight')
    plt.close()
    print(f"✅ Saved feature importance plot: feature_importance.png")
    
    return feature_importance

def save_model_and_artifacts(model, label_encoder, feature_cols, feature_importance):
    """Save trained model and related artifacts"""
    print("\n💾 Saving model and artifacts...")
    
    # Create models directory
    os.makedirs('xgb_models', exist_ok=True)
    
    # Save XGBoost model (sklearn format)
    joblib.dump(model, 'xgb_models/xgboost_tug_model.pkl')
    print("✅ Saved XGBoost model: xgb_models/xgboost_tug_model.pkl")
    
    # Save label encoder
    joblib.dump(label_encoder, 'xgb_models/label_encoder.pkl')
    print("✅ Saved label encoder: xgb_models/label_encoder.pkl")
    
    # Save feature columns
    joblib.dump(feature_cols, 'xgb_models/feature_columns.pkl')
    print("✅ Saved feature columns: xgb_models/feature_columns.pkl")
    
    # Save feature importance
    feature_importance.to_csv('xgb_models/feature_importance.csv', index=False)
    print("✅ Saved feature importance: xgb_models/feature_importance.csv")
    
    # Save model metadata
    metadata = {
        'model_type': 'XGBoost_Sklearn',
        'num_features': len(feature_cols),
        'num_classes': len(label_encoder.classes_),
        'classes': label_encoder.classes_.tolist(),
        'feature_count': len(feature_cols)
    }
    
    import json
    with open('xgb_models/model_metadata.json', 'w') as f:
        json.dump(metadata, f, indent=2)
    print("✅ Saved metadata: xgb_models/model_metadata.json")

def main():
    """XGBoost TUG Phase Classification Training"""
    print("🚀 Starting XGBoost TUG Phase Classification Training")
    print("=" * 70)
    
    features_dir = "training_features_csv"
    # features_dir = "all_features_csv"
    
    try:
        # Load training data
        print(f"\n📂 Loading data from: {features_dir}")
        df = load_and_prepare_data(features_dir)
        
        # Video-based 80/20 train/validation split to prevent temporal leakage
        train_df, val_df, train_videos, val_videos = video_based_train_val_split(df, val_size=0.2)
        
        # Prepare features and labels
        print("\n🔧 Preparing training data...")
        X_train, y_train, label_encoder, feature_cols = prepare_features_and_labels(train_df)
        
        print("\n🔧 Preparing validation data...")
        X_val, y_val, _, _ = prepare_features_and_labels(val_df, label_encoder=label_encoder)
        
        # Analyze temporal features
        print("\n🔍 Analyzing feature types...")
        temporal_features, static_features = analyze_temporal_features(feature_cols)
        
        # Train model with validation set
        print("\n" + "=" * 70)
        model = train_temporal_aware_xgboost(X_train, y_train, X_val, y_val)
        
        # Validation set predictions and evaluation
        y_val_pred = model.predict(X_val)
        val_accuracy = accuracy_score(y_val, y_val_pred)
        print(f"\n🎯 Validation accuracy: {val_accuracy:.4f}")
        
        # Evaluate on validation set
        cm_val = evaluate_model(model, X_val, y_val, y_val_pred, label_encoder, dataset_name="Validation")
        feature_importance = analyze_feature_importance(model, feature_cols)
        
        # Save model and artifacts
        save_model_and_artifacts(model, label_encoder, feature_cols, feature_importance)
        
        print("\n" + "=" * 70)
        print("🎉 Training completed successfully!")
        print("\n📁 Saved artifacts:")
        print("   - xgb_models/xgboost_tug_model.pkl (trained model)")
        print("   - xgb_models/label_encoder.pkl (for decoding predictions)")
        print("   - xgb_models/feature_columns.pkl (feature order)")
        print("   - xgb_models/feature_importance.csv (feature rankings)")
        print("   - xgb_models/model_metadata.json (model info)")
        print("\n📊 Saved visualizations:")
        print("   - confusion_matrix_validation.png")
        print("   - feature_importance.png")
        print("\n✅ Model is ready to test on test_features_csv folder!")
        
    except Exception as e:
        print(f"❌ Training failed: {e}")
        import traceback
        traceback.print_exc()
        raise

if __name__ == "__main__":
    main()
