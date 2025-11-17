"""
🧪 Test Trained XGBoost Model on Test Set
Uses the saved model to evaluate on test_features_csv
"""

import os
import pandas as pd
import numpy as np
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
import matplotlib.pyplot as plt
import seaborn as sns
import joblib

def load_test_data(features_dir):
    """Load all feature files from test directory"""
    print("📊 Loading test feature files...")
    
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
    print(f"✅ Loaded {file_count} test files with {len(combined_df)} total frames")
    
    return combined_df

def prepare_test_features(df, feature_cols):
    """Prepare test features matching training feature set"""
    print("🔧 Preparing test features...")
    
    # Remove rows with NaN labels
    df_clean = df.dropna(subset=['tug_subtask'])
    print(f"🧹 Removed {len(df) - len(df_clean)} rows with missing labels")
    
    # Select only the features that were used in training
    X = df_clean[feature_cols]
    y = df_clean['tug_subtask']
    
    # Handle missing values
    X = X.fillna(0)
    
    # Remove infinite values
    X = X.replace([np.inf, -np.inf], 0)
    
    print(f"📈 Test features shape: {X.shape}")
    
    return X, y, df_clean['video_id']

def evaluate_test_set(model, X_test, y_test, y_pred, label_encoder):
    """Evaluate model on test set"""
    print("\n📊 Test Set Evaluation Results:")
    print("=" * 70)
    
    # Overall accuracy
    test_accuracy = accuracy_score(y_test, y_pred)
    print(f"\n🎯 Test Set Accuracy: {test_accuracy:.4f} ({test_accuracy*100:.2f}%)")
    
    # Classification report
    class_names = label_encoder.classes_
    report = classification_report(y_test, y_pred, target_names=class_names, digits=4)
    print("\n" + report)
    
    # Confusion matrix
    cm = confusion_matrix(y_test, y_pred)
    
    plt.figure(figsize=(12, 8))
    sns.heatmap(cm, annot=True, fmt='d', cmap='Greens', 
                xticklabels=class_names, yticklabels=class_names)
    plt.title('TUG Phase Classification - Test Set Confusion Matrix')
    plt.xlabel('Predicted Phase')
    plt.ylabel('True Phase')
    plt.xticks(rotation=45, ha='right')
    plt.yticks(rotation=0)
    plt.tight_layout()
    plt.savefig('confusion_matrix_test.png', dpi=300, bbox_inches='tight')
    plt.close()
    print(f"\n✅ Saved test confusion matrix: confusion_matrix_test.png")
    
    return cm, test_accuracy

def analyze_per_video_performance(video_ids, y_true, y_pred, label_encoder):
    """Analyze performance per video"""
    print("\n📹 Per-Video Performance Analysis:")
    print("=" * 70)
    
    unique_videos = video_ids.unique()
    video_accuracies = []
    
    for video_id in unique_videos:
        mask = video_ids == video_id
        video_y_true = y_true[mask]
        video_y_pred = y_pred[mask]
        
        accuracy = accuracy_score(video_y_true, video_y_pred)
        video_accuracies.append({
            'video_id': video_id,
            'accuracy': accuracy,
            'num_frames': len(video_y_true)
        })
    
    # Sort by accuracy
    video_df = pd.DataFrame(video_accuracies).sort_values('accuracy')
    
    print(f"\n📊 Performance across {len(unique_videos)} test videos:")
    print(f"   Mean accuracy: {video_df['accuracy'].mean():.4f}")
    print(f"   Std accuracy:  {video_df['accuracy'].std():.4f}")
    print(f"   Min accuracy:  {video_df['accuracy'].min():.4f}")
    print(f"   Max accuracy:  {video_df['accuracy'].max():.4f}")
    
    # Show worst and best performing videos
    print(f"\n⚠️ Worst 5 performing videos:")
    for i, row in video_df.head(5).iterrows():
        print(f"   {row['video_id']}: {row['accuracy']:.4f} ({row['num_frames']} frames)")
    
    print(f"\n✅ Best 5 performing videos:")
    for i, row in video_df.tail(5).iterrows():
        print(f"   {row['video_id']}: {row['accuracy']:.4f} ({row['num_frames']} frames)")
    
    # Save to CSV
    video_df.to_csv('per_video_performance_test.csv', index=False)
    print(f"\n✅ Saved per-video results: per_video_performance_test.csv")
    
    return video_df

def main():
    """Test the trained XGBoost model on test set"""
    print("🧪 Testing XGBoost Model on Test Set")
    print("=" * 70)
    
    test_features_dir = "test_features_csv"
    models_dir = "xgb_models"
    
    try:
        # Load saved model and artifacts
        print("\n📦 Loading trained model and artifacts...")
        model = joblib.load(os.path.join(models_dir, 'xgboost_tug_model.pkl'))
        label_encoder = joblib.load(os.path.join(models_dir, 'label_encoder.pkl'))
        feature_cols = joblib.load(os.path.join(models_dir, 'feature_columns.pkl'))
        
        print(f"✅ Loaded model with {len(feature_cols)} features")
        print(f"✅ Model trained on {len(label_encoder.classes_)} classes: {list(label_encoder.classes_)}")
        
        # Load test data
        print(f"\n📂 Loading test data from: {test_features_dir}")
        test_df = load_test_data(test_features_dir)
        
        # Prepare test features
        X_test, y_test_labels, video_ids = prepare_test_features(test_df, feature_cols)
        
        # Encode test labels
        y_test = label_encoder.transform(y_test_labels)
        
        print(f"\n🏷️ Test label distribution:")
        for i, label in enumerate(label_encoder.classes_):
            count = sum(y_test == i)
            print(f"   {label}: {count} samples ({count/len(y_test)*100:.1f}%)")
        
        # Make predictions
        print("\n🔮 Making predictions on test set...")
        y_pred = model.predict(X_test)
        
        # Evaluate on test set
        cm, test_accuracy = evaluate_test_set(model, X_test, y_test, y_pred, label_encoder)
        
        # Per-video analysis
        video_performance = analyze_per_video_performance(video_ids, y_test, y_pred, label_encoder)
        
        print("\n" + "=" * 70)
        print("🎉 Testing completed successfully!")
        print(f"\n🎯 Final Test Accuracy: {test_accuracy:.4f} ({test_accuracy*100:.2f}%)")
        print("\n📁 Generated files:")
        print("   - confusion_matrix_test.png")
        print("   - per_video_performance_test.csv")
        
    except FileNotFoundError as e:
        print(f"❌ Error: Required files not found!")
        print(f"   {e}")
        print("\n💡 Make sure you have:")
        print("   1. Trained the model first (run train_xgboost.py)")
        print("   2. Test features in test_features_csv folder")
        raise
    except Exception as e:
        print(f"❌ Testing failed: {e}")
        import traceback
        traceback.print_exc()
        raise

if __name__ == "__main__":
    main()
