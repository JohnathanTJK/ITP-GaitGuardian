"""
🚀 XGBoost Model Training for TUG Phase Classification
Enhanced with SAIL research-inspired features for 6-phase TUG assessment
"""

import os
import pandas as pd
import numpy as np
from xgboost import XGBClassifier
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
import matplotlib.pyplot as plt
import seaborn as sns
import joblib
from collections import Counter

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

def prepare_features_and_labels(df):
    """Prepare features and labels for training"""
    print("🔧 Preparing features and labels...")
    
    # Remove rows with NaN labels
    df_clean = df.dropna(subset=['tug_subtask'])
    print(f"🧹 Removed {len(df) - len(df_clean)} rows with missing labels")
    
    # Remove non-feature columns
    feature_cols = [col for col in df_clean.columns if col not in ['frame', 'tug_subtask', 'video_id']]
    X = df_clean[feature_cols]
    y = df_clean['tug_subtask']
    
    # Handle missing values in features
    X = X.fillna(0)
    
    # Remove infinite values
    X = X.replace([np.inf, -np.inf], 0)
    
    # Encode labels
    label_encoder = LabelEncoder()
    y_encoded = label_encoder.fit_transform(y)
    
    print(f"📈 Features shape: {X.shape}")
    print(f"🏷️ Label distribution:")
    for i, label in enumerate(label_encoder.classes_):
        count = sum(y_encoded == i)
        print(f"   {label}: {count} samples ({count/len(y_encoded)*100:.1f}%)")
    
    return X, y_encoded, label_encoder, feature_cols

def train_xgboost_model(X, y, feature_cols):
    """Train XGBoost model with sklearn API"""
    print("🚀 Training XGBoost model...")
    
    # Split data: 80% training, 20% testing
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )
    
    print(f"📊 Data split - Training: {X_train.shape[0]} samples ({X_train.shape[0]/(X_train.shape[0]+X_test.shape[0])*100:.1f}%)")
    print(f"📊 Data split - Testing: {X_test.shape[0]} samples ({X_test.shape[0]/(X_train.shape[0]+X_test.shape[0])*100:.1f}%)")
    
    # Initialize XGBoost classifier
    num_classes = len(np.unique(y))
    model = XGBClassifier(
        objective='multi:softprob',
        n_estimators=200,
        max_depth=8,
        learning_rate=0.1,
        subsample=0.9,
        colsample_bytree=0.9,
        random_state=42,
        n_jobs=-1,
        eval_metric='mlogloss'
    )
    
    print("🔍 Training with parameters:")
    print(f"   n_estimators: {model.n_estimators}")
    print(f"   max_depth: {model.max_depth}")
    print(f"   learning_rate: {model.learning_rate}")
    print(f"   subsample: {model.subsample}")
    print(f"   colsample_bytree: {model.colsample_bytree}")
    
    # Train model
    print("🎯 Training XGBoost model...")
    model.fit(
        X_train, y_train,
        eval_set=[(X_train, y_train), (X_test, y_test)],
        verbose=50
    )
    
    # Make predictions
    y_pred = model.predict(X_test)
    
    # Calculate accuracy
    test_accuracy = accuracy_score(y_test, y_pred)
    print(f"🎯 Test accuracy: {test_accuracy:.4f}")
    
    return model, X_test, y_test, y_pred, feature_cols

def evaluate_model(model, X_test, y_test, y_pred, label_encoder):
    """Evaluate model performance"""
    print("\n📊 Model Evaluation Results:")
    print("=" * 50)
    
    # Classification report
    class_names = label_encoder.classes_
    report = classification_report(y_test, y_pred, target_names=class_names)
    print(report)
    
    # Confusion matrix
    cm = confusion_matrix(y_test, y_pred)
    
    plt.figure(figsize=(12, 8))
    sns.heatmap(cm, annot=True, fmt='d', cmap='Blues', 
                xticklabels=class_names, yticklabels=class_names)
    plt.title('TUG Phase Classification - Confusion Matrix')
    plt.xlabel('Predicted Phase')
    plt.ylabel('True Phase')
    plt.xticks(rotation=45)
    plt.yticks(rotation=0)
    plt.tight_layout()
    plt.savefig('confusion_matrix.png', dpi=300, bbox_inches='tight')
    plt.show()
    
    return cm

def analyze_feature_importance(model, feature_cols, top_n=20):
    """Analyze and visualize feature importance"""
    print(f"\n🔬 Top {top_n} Most Important Features:")
    print("=" * 50)
    
    # Get feature importance from sklearn XGBoost model
    importance = model.feature_importances_
    
    # Create feature importance dataframe
    feature_importance = pd.DataFrame({
        'feature_name': feature_cols,
        'importance': importance
    }).sort_values('importance', ascending=False)
    
    # Print top features
    for i, (_, row) in enumerate(feature_importance.head(top_n).iterrows()):
        print(f"{i+1:2d}. {row['feature_name']:<30} {row['importance']:.4f}")
    
    # Plot feature importance
    plt.figure(figsize=(12, 8))
    top_features = feature_importance.head(top_n)
    sns.barplot(data=top_features, x='importance', y='feature_name', palette='viridis')
    plt.title(f'Top {top_n} Feature Importance - XGBoost TUG Classification')
    plt.xlabel('Feature Importance')
    plt.tight_layout()
    plt.savefig('feature_importance.png', dpi=300, bbox_inches='tight')
    plt.show()
    
    return feature_importance

def save_model_and_artifacts(model, label_encoder, feature_cols, feature_importance):
    """Save trained model and related artifacts"""
    print("\n💾 Saving model and artifacts...")
    
    # Create models directory
    os.makedirs('models', exist_ok=True)
    
    # Save XGBoost model (sklearn format)
    joblib.dump(model, 'models/xgboost_tug_model.pkl')
    print("✅ Saved XGBoost model: models/xgboost_tug_model.pkl")
    
    # Also save in XGBoost native format
    model.save_model('models/xgboost_tug_model.json')
    print("✅ Saved XGBoost model (native): models/xgboost_tug_model.json")
    
    # Save label encoder
    joblib.dump(label_encoder, 'models/label_encoder.pkl')
    print("✅ Saved label encoder: models/label_encoder.pkl")
    
    # Save feature columns
    joblib.dump(feature_cols, 'models/feature_columns.pkl')
    print("✅ Saved feature columns: models/feature_columns.pkl")
    
    # Save feature importance
    feature_importance.to_csv('models/feature_importance.csv', index=False)
    print("✅ Saved feature importance: models/feature_importance.csv")
    
    # Save model metadata
    metadata = {
        'model_type': 'XGBoost_Sklearn',
        'num_features': len(feature_cols),
        'num_classes': len(label_encoder.classes_),
        'classes': label_encoder.classes_.tolist(),
        'feature_count': len(feature_cols)
    }
    
    import json
    with open('models/model_metadata.json', 'w') as f:
        json.dump(metadata, f, indent=2)
    print("✅ Saved metadata: models/model_metadata.json")

def main():
    """Main training pipeline"""
    print("🚀 Starting XGBoost TUG Phase Classification Training")
    print("=" * 60)
    
    # Configuration
    features_dir = "engineered_features_csv"
    
    try:
        # Load and prepare data
        df = load_and_prepare_data(features_dir)
        X, y, label_encoder, feature_cols = prepare_features_and_labels(df)
        
        # Train model
        model, X_test, y_test, y_pred, feature_cols = train_xgboost_model(X, y, feature_cols)
        
        # Evaluate model
        confusion_matrix = evaluate_model(model, X_test, y_test, y_pred, label_encoder)
        
        # Analyze feature importance
        feature_importance = analyze_feature_importance(model, feature_cols)
        
        # Save model and artifacts
        save_model_and_artifacts(model, label_encoder, feature_cols, feature_importance)
        
        print("\n🎉 Training completed successfully!")
        print("📁 Check the 'models' directory for saved artifacts")
        print("📊 Check confusion_matrix.png and feature_importance.png for visualizations")
        
    except Exception as e:
        print(f"❌ Training failed: {e}")
        raise

if __name__ == "__main__":
    main()
