"""
🎯 TUG Subtask Prediction Script
Uses the trained XGBoost model to predict TUG phases from pose landmark data
"""

import os
import pandas as pd
import numpy as np
import joblib
from xgboost import XGBClassifier
import json
from collections import defaultdict

class TUGSubtaskPredictor:
    """Class for predicting TUG subtasks using trained XGBoost model"""
    
    def __init__(self, model_dir="models"):
        """Initialize the predictor with trained model artifacts"""
        self.model_dir = model_dir
        self.model = None
        self.label_encoder = None
        self.feature_cols = None
        self.metadata = None
        
    def load_model(self):
        """Load the trained model and artifacts"""
        print("📦 Loading trained XGBoost model...")
        
        try:
            # Load model
            model_path = os.path.join(self.model_dir, "xgboost_tug_model.pkl")
            self.model = joblib.load(model_path)
            print(f"✅ Loaded XGBoost model from {model_path}")
            
            # Load label encoder
            encoder_path = os.path.join(self.model_dir, "label_encoder.pkl")
            self.label_encoder = joblib.load(encoder_path)
            print(f"✅ Loaded label encoder from {encoder_path}")
            
            # Load feature columns
            features_path = os.path.join(self.model_dir, "feature_columns.pkl")
            self.feature_cols = joblib.load(features_path)
            print(f"✅ Loaded feature columns ({len(self.feature_cols)} features)")
            
            # Load metadata
            import json
            metadata_path = os.path.join(self.model_dir, "model_metadata.json")
            with open(metadata_path, 'r') as f:
                self.metadata = json.load(f)
            print(f"✅ Loaded metadata: {self.metadata['model_type']}")
            
            print(f"🏷️ Available TUG phases: {self.label_encoder.classes_}")
            
        except Exception as e:
            print(f"❌ Error loading model: {e}")
            raise
    
    def predict_from_features(self, features_df):
        """Predict TUG subtasks from engineered features DataFrame"""
        print(f"🔮 Predicting TUG subtasks for {len(features_df)} frames...")
        
        if self.model is None:
            raise ValueError("Model not loaded. Call load_model() first.")
        
        # Prepare features
        X = self._prepare_features(features_df)
        
        # Make predictions
        y_pred_encoded = self.model.predict(X)
        y_pred_proba = self.model.predict_proba(X)
        
        # Decode predictions
        y_pred = self.label_encoder.inverse_transform(y_pred_encoded)
        
        # Create results DataFrame
        results = features_df[['frame']].copy()
        results['predicted_subtask'] = y_pred
        results['confidence'] = np.max(y_pred_proba, axis=1)
        
        # Add probability for each class
        for i, class_name in enumerate(self.label_encoder.classes_):
            results[f'prob_{class_name}'] = y_pred_proba[:, i]
        
        print(f"✅ Prediction completed!")
        print(f"📊 Prediction distribution:")
        pred_counts = pd.Series(y_pred).value_counts()
        for phase, count in pred_counts.items():
            print(f"   {phase}: {count} frames ({count/len(y_pred)*100:.1f}%)")
        
        return results
    
    def predict_from_csv(self, csv_path):
        """Predict TUG subtasks from a feature CSV file"""
        print(f"📁 Loading features from {csv_path}...")
        
        try:
            features_df = pd.read_csv(csv_path)
            print(f"📈 Loaded {len(features_df)} frames with {len(features_df.columns)} columns")
            
            # Predict subtasks
            results = self.predict_from_features(features_df)
            
            return results
            
        except Exception as e:
            print(f"❌ Error loading CSV: {e}")
            raise
    
    def _prepare_features(self, features_df):
        """Prepare features for prediction (same preprocessing as training)"""
        # Select only the feature columns used in training
        X = features_df[self.feature_cols]
        
        # Handle missing values (same as training)
        X = X.fillna(0)
        
        # Remove infinite values (same as training)
        X = X.replace([np.inf, -np.inf], 0)
        
        return X
    

    
    def calculate_phase_durations(self, results, fps=30):
        """Calculate duration of each TUG phase in seconds"""
        phase_durations = {}
        
        # Count frames for each phase
        phase_counts = results['predicted_subtask'].value_counts()
        
        # Convert frame counts to seconds - only duration
        for phase, count in phase_counts.items():
            duration_seconds = count / fps
            phase_durations[phase] = float(round(duration_seconds, 2))
        
        return phase_durations
    
    def save_durations_json(self, results, output_path, video_id=None, fps=30):
        """Save TUG phase durations as JSON file - only durations"""
        phase_durations = self.calculate_phase_durations(results, fps)
        
        # Save as JSON - just the durations
        with open(output_path, 'w') as f:
            json.dump(phase_durations, f, indent=2)
        
        print(f"💾 Durations saved to {output_path}")
        
        # Print summary
        print(f"📊 TUG Phase Durations:")
        for phase, duration in phase_durations.items():
            print(f"   {phase}: {duration}s")
        
        return phase_durations
    
    def predict_and_label_csv(self, csv_path, output_path=None):
        """Predict TUG subtasks and add labels to the original CSV file"""
        print(f"📁 Loading features from {csv_path}...")
        
        try:
            # Load the original feature CSV
            features_df = pd.read_csv(csv_path)
            print(f"📈 Loaded {len(features_df)} frames with {len(features_df.columns)} columns")
            
            # Make predictions
            results = self.predict_from_features(features_df)
            
            # Add predictions to the original dataframe
            features_df['tug_subtask'] = results['predicted_subtask'].values
            
            # Save the updated CSV
            if output_path is None:
                output_path = csv_path  # Overwrite original file
            
            features_df.to_csv(output_path, index=False)
            print(f"💾 Updated CSV saved to {output_path}")
            
            # Print summary
            phase_counts = features_df['tug_subtask'].value_counts()
            print(f"📊 TUG Phase Distribution:")
            for phase, count in phase_counts.items():
                percentage = count / len(features_df) * 100
                print(f"   {phase}: {count} frames ({percentage:.1f}%)")
            
            # Calculate durations
            durations = self.calculate_phase_durations(results)
            
            return features_df, durations
            
        except Exception as e:
            print(f"❌ Error processing CSV: {e}")
            raise
    






def label_feature_csvs(features_dir="engineered_features_csv", output_dir="durations"):
    """Add TUG subtask labels to all feature CSV files and generate duration files"""
    print("🏷️ Batch TUG Subtask Labeling - Adding labels to CSV files and generating durations")
    print("=" * 60)
    
    # Create output directory for durations
    os.makedirs(output_dir, exist_ok=True)
    
    # Initialize predictor
    predictor = TUGSubtaskPredictor()
    predictor.load_model()
    
    # Find all feature files
    feature_files = [f for f in os.listdir(features_dir) if f.endswith("_color_features.csv")]
    print(f"📁 Found {len(feature_files)} feature files to label")
    
    processed_count = 0
    batch_durations = {}
    overall_stats = defaultdict(list)
    
    for i, feature_file in enumerate(feature_files, 1):
        video_name = feature_file.replace("_color_features.csv", "")
        print(f"\n📊 Processing {i}/{len(feature_files)}: {video_name}")
        
        try:
            # Load and predict, then save back to original file
            feature_path = os.path.join(features_dir, feature_file)
            updated_df, durations = predictor.predict_and_label_csv(feature_path)
            
            # Save individual duration JSON file
            json_output_path = os.path.join(output_dir, f"durations_{video_name}.json")
            with open(json_output_path, 'w') as f:
                json.dump(durations, f, indent=2)
            
            # Store for batch analysis
            batch_durations[video_name] = durations
            
            # Collect overall statistics
            for phase, duration in durations.items():
                overall_stats[phase].append(duration)
            
            total_duration = sum(durations.values())
            print(f"✅ Added tug_subtask column with {len(updated_df)} predictions")
            print(f"📊 Total duration: {total_duration:.2f}s")
            print(f"💾 Duration JSON saved: {json_output_path}")
            
            processed_count += 1
            
        except Exception as e:
            print(f"❌ Error processing {video_name}: {e}")
            continue
    
    # Create combined summary
    if batch_durations:
        summary_data = {
            "total_videos": len(batch_durations),
            "phase_statistics": {},
            "videos": batch_durations
        }
        
        # Calculate phase statistics across all videos
        for phase, durations_list in overall_stats.items():
            if durations_list:
                summary_data["phase_statistics"][phase] = {
                    "average_duration": float(round(sum(durations_list) / len(durations_list), 2)),
                    "min_duration": float(round(min(durations_list), 2)),
                    "max_duration": float(round(max(durations_list), 2)),
                    "videos_with_phase": int(len(durations_list))
                }
        
        # Save combined summary
        summary_path = os.path.join(output_dir, "all_durations_summary.json")
        with open(summary_path, 'w') as f:
            json.dump(summary_data, f, indent=2)
        
        print(f"\n💾 Combined summary saved to {summary_path}")
        
        # Print overall statistics
        print(f"\n📈 Overall Batch Statistics:")
        print(f"   Total videos processed: {len(batch_durations)}")
        
        for phase, stats in summary_data["phase_statistics"].items():
            print(f"   {phase}:")
            print(f"     Average duration: {stats['average_duration']}s")
            print(f"     Range: {stats['min_duration']}s - {stats['max_duration']}s")
            print(f"     Present in {stats['videos_with_phase']}/{len(batch_durations)} videos")
    
    print(f"\n🎉 Batch labeling completed!")
    print(f"📈 Successfully labeled {processed_count}/{len(feature_files)} CSV files")
    print(f"📁 All CSV files now have 'tug_subtask' column with predictions")
    print(f"📁 Duration JSON files saved in '{output_dir}' directory")

def main():
    """Main function for TUG subtask prediction - Label CSV files and generate durations"""
    print("🎯 TUG Subtask Prediction with XGBoost - Complete Pipeline")
    print("=" * 60)
    
    # Complete pipeline: Add labels to CSV files and generate duration JSON files
    label_feature_csvs()

if __name__ == "__main__":
    main()
