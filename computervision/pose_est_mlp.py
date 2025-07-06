import os
import glob
import json
import joblib
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.model_selection import train_test_split, StratifiedKFold, cross_val_score
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
from sklearn.utils.class_weight import compute_class_weight
import tensorflow as tf
from tensorflow.keras.models import Sequential, Model
from tensorflow.keras.layers import Dense, Dropout, BatchNormalization, Input
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau, ModelCheckpoint
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.regularizers import l1_l2
import optuna
from collections import Counter
import warnings
warnings.filterwarnings('ignore')

class GaitGuardianMLP:
    """
    Multi-Layer Perceptron for GaitGuardian project
    Handles both subtask classification and severity prediction for Parkinson's disease patients
    """
    
    def __init__(self, task='subtask', random_state=42):
        """
        Initialize the MLP model
        
        Args:
            task (str): 'subtask' for TUG subtask classification or 'severity' for severity prediction
            random_state (int): Random seed for reproducibility
        """
        self.task = task
        self.random_state = random_state
        self.model = None
        self.scaler = StandardScaler()
        self.label_encoder = LabelEncoder()
        self.feature_cols = None
        self.history = None
        self.best_params = None
        
        # Set random seeds
        np.random.seed(random_state)
        tf.random.set_seed(random_state)
        
        # Task-specific configurations
        if task == 'subtask':
            self.classes = ['Sit-To-Stand', 'Walk-From-Chair', 'Turn-First', 
                          'Walk-To-Chair', 'Turn-Second', 'Stand-To-Sit']
            self.output_dim = len(self.classes)
        elif task == 'severity':
            self.classes = ['Normal', 'Minimal', 'Mild', 'Moderate', 'Severe']
            self.output_dim = len(self.classes)
        else:
            raise ValueError("Task must be 'subtask' or 'severity'")
    
    def load_data(self, input_dir="./computervision/keypoints_and_durations", gait_features_path="./computervision/keypoints_and_durations/gait_features.csv"):
        """
        Load and preprocess data from CSV files
        
        Args:
            input_dir (str): Directory containing labeled CSV files
            gait_features_path (str): Path to gait features CSV
        """
        print(f"[INFO] Loading data for {self.task} task...")
        
        if self.task == 'subtask':
            # Load pose keypoints data for subtask classification
            csv_files = glob.glob(os.path.join(input_dir, "*.csv"))
            if not csv_files:
                raise FileNotFoundError(f"No CSVs found in {input_dir}")
            
            dataframes = []
            for csv in csv_files:
                df = pd.read_csv(csv)
                df['video'] = os.path.basename(csv).replace("_labelled.csv", "")
                dataframes.append(df)
            
            self.df = pd.concat(dataframes, ignore_index=True)
            self.df = self.df.dropna(subset=["tug_subtask"])
            
            # Smooth labels per video
            self.df = self._smooth_labels(self.df)
            
            # Prepare features and labels
            self.feature_cols = [c for c in self.df.columns if c.startswith(('x_', 'y_', 'z_', 'visibility_'))]
            self.X = self.df[self.feature_cols].fillna(0).values
            self.y = self.df['tug_subtask'].values
            
        elif self.task == 'severity':
            # Load gait features for severity prediction
            if not os.path.exists(gait_features_path):
                raise FileNotFoundError(f"Gait features file not found: {gait_features_path}")
            
            self.df = pd.read_csv(gait_features_path)
            
            # Create synthetic severity labels based on gait metrics (for demonstration)
            # In practice, these would come from clinical assessments
            self.df['severity'] = self._generate_severity_labels_improved(self.df)
            
            # Prepare features and labels
            self.feature_cols = [c for c in self.df.columns if c not in ['video', 'severity']]
            self.X = self.df[self.feature_cols].fillna(0).values
            self.y = self.df['severity'].values
        
        print(f"[INFO] Loaded {len(self.df)} samples with {len(self.feature_cols)} features")
        print(f"[INFO] Class distribution: {Counter(self.y)}")
    
    def _smooth_labels(self, df, window=7):
        """Smooth labels per video to reduce noise"""
        smoothed_dfs = []
        for video in df['video'].unique():
            sub_df = df[df['video'] == video].copy()
            labels = sub_df['tug_subtask'].tolist()
            pad = window // 2
            padded = [labels[0]] * pad + labels + [labels[-1]] * pad
            smoothed = [
                Counter(padded[i - pad:i + pad + 1]).most_common(1)[0][0]
                for i in range(pad, len(labels) + pad)
            ]
            sub_df['tug_subtask'] = smoothed
            smoothed_dfs.append(sub_df)
        return pd.concat(smoothed_dfs, ignore_index=True)
    
    def debug_severity_classification(self):
        """Debug the severity classification by examining the data"""
        print("\n[DEBUG] Examining gait features data...")
        print("-" * 50)
        
        # Print column names
        print("Available columns:")
        print(self.df.columns.tolist())
        
        # Print first few rows
        print("\nFirst 3 rows of data:")
        print(self.df.head(3))
        
        # Check for expected duration columns
        expected_duration_cols = [
            'sit_to_stand_duration', 'walk_from_chair_duration', 
            'turn1_duration', 'walk_to_chair_duration', 
            'turn2_duration', 'stand_to_sit_duration'
        ]
        
        missing_cols = [col for col in expected_duration_cols if col not in self.df.columns]
        if missing_cols:
            print(f"\nMissing expected columns: {missing_cols}")
            print("Available duration-related columns:")
            duration_cols = [col for col in self.df.columns if 'duration' in col.lower() or 'time' in col.lower()]
            print(duration_cols)
        
        return self.df.columns.tolist()

    def _generate_severity_labels(self, df):
        """
        Generate severity labels based on TUG test performance
        
        Classification criteria:
        - Normal: ≤7s total time, turning/walking ratio <0.5
        - Slight: ≤13s total time, turning/walking ratio <1.0
        - Mild: ≤13s total time, turning/walking ratio ≥1.0
        - Moderate: >13s total time, turning/walking ratio >1.0, but straight walking still reasonable
        - Severe: >13s total time with obvious issues in both turning and straight walking
        
        Args:
            df (pd.DataFrame): DataFrame containing gait features
            
        Returns:
            list: Severity labels for each sample
        """
        severity_labels = []
        
        for _, row in df.iterrows():
            # Calculate total TUG time (assuming we have these components)
            # You may need to adjust these column names based on your actual data
            total_time = (row.get('sit_to_stand_duration', 0) + 
                        row.get('walk_from_chair_duration', 0) + 
                        row.get('turn1_duration', 0) + 
                        row.get('walk_to_chair_duration', 0) + 
                        row.get('turn2_duration', 0) + 
                        row.get('stand_to_sit_duration', 0))
            
            # Calculate turning time (both turns combined)
            turning_time = row.get('turn1_duration', 0) + row.get('turn2_duration', 0)
            
            # Calculate straight walking time
            straight_walking_time = (row.get('walk_from_chair_duration', 0) + 
                                row.get('walk_to_chair_duration', 0))
            
            # Calculate turning to walking ratio
            if straight_walking_time > 0:
                turning_ratio = turning_time / straight_walking_time
            else:
                turning_ratio = float('inf')  # Handle division by zero
            
            # Additional gait quality indicators
            cadence = row.get('cadence', 120)  # steps per minute
            step_symmetry = row.get('step_symmetry', 0)
            upper_body_sway = row.get('upper_body_sway', 0)
            
            # Classify severity based on clinical criteria
            if total_time <= 7 and turning_ratio < 0.5:
                # Normal: Healthy adult performance
                severity_labels.append('Normal')
                
            elif total_time <= 13 and turning_ratio < 1.0:
                # Slight: Can complete within normal time with minimal turning issues
                severity_labels.append('Slight')
                
            elif total_time <= 13 and turning_ratio >= 1.0:
                # Mild: Can complete within normal time but turning is problematic
                severity_labels.append('Mild')
                
            elif total_time > 13 and turning_ratio > 1.0:
                # Need to distinguish between Moderate and Severe
                # Moderate: Turning is the main issue, straight walking still reasonable
                # Severe: Both turning and straight walking are problematic
                
                # Define "reasonable" straight walking (based on expected walking speed)
                # Normal walking speed for 6m total distance should be around 3-4 seconds
                expected_walking_time = 4.0  # seconds for 6m at normal speed
                walking_impairment_ratio = straight_walking_time / expected_walking_time
                
                # Additional severity indicators
                severe_indicators = 0
                
                # Very slow cadence
                if cadence < 80:
                    severe_indicators += 1
                
                # Poor step symmetry
                if step_symmetry > 0.3:
                    severe_indicators += 1
                    
                # Excessive upper body sway
                if upper_body_sway > 0.15:
                    severe_indicators += 1
                    
                # Very long straight walking time
                if walking_impairment_ratio > 2.0:
                    severe_indicators += 1
                    
                # Very long turning time
                if turning_time > 8.0:
                    severe_indicators += 1
                
                # Classify as Moderate or Severe
                if severe_indicators >= 3 or walking_impairment_ratio > 2.5:
                    severity_labels.append('Severe')
                else:
                    severity_labels.append('Moderate')
                    
            else:
                # Fallback for edge cases (total_time > 13 but turning_ratio <= 1.0)
                # This suggests straight walking is the main issue
                if total_time > 20:  # Very slow overall
                    severity_labels.append('Severe')
                else:
                    severity_labels.append('Moderate')
        
        return severity_labels
    
    def _generate_severity_labels_fixed(self, df):
        """
        Fixed severity classification with proper column handling and debugging
        """
        print("\n[DEBUG] Generating severity labels...")
        
        severity_labels = []
        
        # First, let's examine what columns we actually have
        print("Available columns:", df.columns.tolist())
        
        for idx, row in df.iterrows():
            print(f"\n[DEBUG] Processing sample {idx}:")
            
            # Try to identify actual duration columns in your data
            # You'll need to adjust these based on your actual column names
            duration_cols = [col for col in df.columns if 'duration' in col.lower() or 'time' in col.lower()]
            print(f"Duration columns found: {duration_cols}")
            
            # Calculate total time using available columns
            total_time = 0
            turning_time = 0
            straight_walking_time = 0
            
            # Add up all duration columns for total time
            for col in duration_cols:
                value = row.get(col, 0)
                if pd.notna(value):
                    total_time += value
                    print(f"  {col}: {value}s")
                    
                    # Categorize as turning or walking
                    if 'turn' in col.lower():
                        turning_time += value
                    elif 'walk' in col.lower():
                        straight_walking_time += value
            
            print(f"  Total time: {total_time}s")
            print(f"  Turning time: {turning_time}s")
            print(f"  Walking time: {straight_walking_time}s")
            
            # Calculate ratio
            if straight_walking_time > 0:
                turning_ratio = turning_time / straight_walking_time
            else:
                turning_ratio = 0
            
            print(f"  Turning ratio: {turning_ratio:.2f}")
            
            # Apply classification rules
            if total_time <= 7 and turning_ratio < 0.5:
                severity = 'Normal'
            elif total_time <= 13 and turning_ratio < 1.0:
                severity = 'Slight'
            elif total_time <= 13 and turning_ratio >= 1.0:
                severity = 'Mild'
            elif total_time > 13:
                # For demonstration, let's be more generous with Moderate classification
                if turning_ratio > 2.0 or total_time > 25:
                    severity = 'Severe'
                else:
                    severity = 'Moderate'
            else:
                severity = 'Normal'  # fallback
            
            print(f"  Classified as: {severity}")
            severity_labels.append(severity)
        
        return severity_labels
    
    def _generate_severity_labels_improved(self, df):
        """
        Improved severity classification with better thresholds and validation
        """
        print("\n[DEBUG] Generating improved severity labels...")
        
        severity_labels = []
        
        # Print available columns for debugging
        print("Available columns:", df.columns.tolist())
        
        # Find all duration columns
        duration_cols = [col for col in df.columns if 'duration' in col.lower() or 'time' in col.lower()]
        print(f"Duration columns found: {duration_cols}")
        
        for idx, row in df.iterrows():
            # Calculate total time using all available duration columns
            total_time = 0
            turning_time = 0
            walking_time = 0
            
            for col in duration_cols:
                value = row.get(col, 0)
                if pd.notna(value) and value > 0:
                    total_time += value
                    
                    # Categorize duration types
                    if 'turn' in col.lower():
                        turning_time += value
                    elif 'walk' in col.lower():
                        walking_time += value
            
            # Validate total time makes sense
            if total_time < 5:  # Suspiciously fast
                print(f"[WARNING] Sample {idx}: Total time {total_time:.2f}s seems too fast")
                # Could be subsection durations rather than full TUG
                total_time *= 2  # Adjust if needed
            
            # Calculate movement efficiency metrics
            if walking_time > 0:
                turning_ratio = turning_time / walking_time
            else:
                turning_ratio = 0
            
            # Improved classification thresholds based on clinical literature
            if total_time <= 10:
                if turning_ratio < 0.3:
                    severity = 'Normal'
                elif turning_ratio < 0.8:
                    severity = 'Slight'
                else:
                    severity = 'Mild'
            elif total_time <= 15:
                if turning_ratio < 0.5:
                    severity = 'Slight'
                elif turning_ratio < 1.2:
                    severity = 'Mild'
                else:
                    severity = 'Moderate'
            elif total_time <= 20:
                if turning_ratio < 1.0:
                    severity = 'Mild'
                else:
                    severity = 'Moderate'
            else:  # > 20 seconds
                if turning_ratio > 2.0:
                    severity = 'Severe'
                else:
                    severity = 'Moderate'
            
            print(f"Sample {idx}: Total={total_time:.2f}s, Turning ratio={turning_ratio:.2f} → {severity}")
            severity_labels.append(severity)
        
        return severity_labels
    
    def _validate_severity_classification(self, df, severity_labels):
        """
        Validate the severity classification and provide statistics
        
        Args:
            df (pd.DataFrame): DataFrame containing gait features
            severity_labels (list): Generated severity labels
        """
        print("\n[INFO] Severity Classification Validation")
        print("-" * 50)
        
        # Create a summary DataFrame
        summary_df = df.copy()
        summary_df['severity'] = severity_labels
        
        # Calculate statistics for each severity level
        for severity in ['Normal', 'Slight', 'Mild', 'Moderate', 'Severe']:
            subset = summary_df[summary_df['severity'] == severity]
            if len(subset) > 0:
                print(f"\n{severity} ({len(subset)} samples):")
                
                # Calculate total time for each sample
                total_times = []
                turning_ratios = []
                
                for _, row in subset.iterrows():
                    total_time = (row.get('sit_to_stand_duration', 0) + 
                                row.get('walk_from_chair_duration', 0) + 
                                row.get('turn1_duration', 0) + 
                                row.get('walk_to_chair_duration', 0) + 
                                row.get('turn2_duration', 0) + 
                                row.get('stand_to_sit_duration', 0))
                    
                    turning_time = row.get('turn1_duration', 0) + row.get('turn2_duration', 0)
                    straight_walking_time = (row.get('walk_from_chair_duration', 0) + 
                                        row.get('walk_to_chair_duration', 0))
                    
                    total_times.append(total_time)
                    if straight_walking_time > 0:
                        turning_ratios.append(turning_time / straight_walking_time)
                
                if total_times:
                    print(f"  Average total time: {np.mean(total_times):.2f}s (±{np.std(total_times):.2f}s)")
                    print(f"  Time range: {min(total_times):.2f}s - {max(total_times):.2f}s")
                    
                if turning_ratios:
                    print(f"  Average turning ratio: {np.mean(turning_ratios):.2f} (±{np.std(turning_ratios):.2f})")
                    print(f"  Ratio range: {min(turning_ratios):.2f} - {max(turning_ratios):.2f}")
        
        # Distribution
        from collections import Counter
        distribution = Counter(severity_labels)
        print(f"\n[INFO] Severity Distribution:")
        for severity in ['Normal', 'Slight', 'Mild', 'Moderate', 'Severe']:
            count = distribution.get(severity, 0)
            percentage = (count / len(severity_labels)) * 100
            print(f"  {severity}: {count} ({percentage:.1f}%)")


    def plot_severity_distribution(self):
        """Plot the distribution of severity levels and their characteristics"""
        if not hasattr(self, 'df') or 'severity' not in self.df.columns:
            print("[ERROR] No severity data available for plotting")
            return
        
        fig, axes = plt.subplots(2, 2, figsize=(15, 12))
        
        # 1. Severity distribution
        severity_counts = self.df['severity'].value_counts()
        axes[0, 0].bar(severity_counts.index, severity_counts.values)
        axes[0, 0].set_title('Severity Distribution')
        axes[0, 0].set_xlabel('Severity Level')
        axes[0, 0].set_ylabel('Number of Samples')
        axes[0, 0].tick_params(axis='x', rotation=45)
        
        # 2. Total time by severity
        severity_order = ['Normal', 'Slight', 'Mild', 'Moderate', 'Severe']
        total_times = []
        
        for _, row in self.df.iterrows():
            total_time = (row.get('sit_to_stand_duration', 0) + 
                        row.get('walk_from_chair_duration', 0) + 
                        row.get('turn1_duration', 0) + 
                        row.get('walk_to_chair_duration', 0) + 
                        row.get('turn2_duration', 0) + 
                        row.get('stand_to_sit_duration', 0))
            total_times.append(total_time)
        
        self.df['total_time'] = total_times
        
        # Box plot of total times by severity
        severity_present = [s for s in severity_order if s in self.df['severity'].values]
        data_for_boxplot = [self.df[self.df['severity'] == s]['total_time'].values 
                        for s in severity_present]
        
        axes[0, 1].boxplot(data_for_boxplot, labels=severity_present)
        axes[0, 1].set_title('Total TUG Time by Severity')
        axes[0, 1].set_xlabel('Severity Level')
        axes[0, 1].set_ylabel('Total Time (seconds)')
        axes[0, 1].tick_params(axis='x', rotation=45)
        axes[0, 1].axhline(y=13, color='r', linestyle='--', label='13s threshold')
        axes[0, 1].legend()
        
        # 3. Turning ratio by severity
        turning_ratios = []
        for _, row in self.df.iterrows():
            turning_time = row.get('turn1_duration', 0) + row.get('turn2_duration', 0)
            straight_walking_time = (row.get('walk_from_chair_duration', 0) + 
                                row.get('walk_to_chair_duration', 0))
            if straight_walking_time > 0:
                turning_ratios.append(turning_time / straight_walking_time)
            else:
                turning_ratios.append(0)
        
        self.df['turning_ratio'] = turning_ratios
        
        data_for_boxplot_ratio = [self.df[self.df['severity'] == s]['turning_ratio'].values 
                                for s in severity_present]
        
        axes[1, 0].boxplot(data_for_boxplot_ratio, labels=severity_present)
        axes[1, 0].set_title('Turning/Walking Ratio by Severity')
        axes[1, 0].set_xlabel('Severity Level')
        axes[1, 0].set_ylabel('Turning/Walking Time Ratio')
        axes[1, 0].tick_params(axis='x', rotation=45)
        axes[1, 0].axhline(y=1.0, color='r', linestyle='--', label='1:1 ratio')
        axes[1, 0].legend()
        
        # 4. Scatter plot: Total time vs Turning ratio
        colors = {'Normal': 'green', 'Slight': 'blue', 'Mild': 'orange', 
                'Moderate': 'red', 'Severe': 'darkred'}
        
        for severity in severity_present:
            subset = self.df[self.df['severity'] == severity]
            axes[1, 1].scatter(subset['total_time'], subset['turning_ratio'], 
                            c=colors.get(severity, 'gray'), label=severity, alpha=0.7)
        
        axes[1, 1].set_xlabel('Total TUG Time (seconds)')
        axes[1, 1].set_ylabel('Turning/Walking Time Ratio')
        axes[1, 1].set_title('TUG Performance Characteristics')
        axes[1, 1].axvline(x=13, color='r', linestyle='--', alpha=0.5, label='13s threshold')
        axes[1, 1].axhline(y=1.0, color='r', linestyle='--', alpha=0.5, label='1:1 ratio')
        axes[1, 1].legend()
        axes[1, 1].grid(True, alpha=0.3)
        
        plt.tight_layout()
        plt.savefig(f'severity_analysis_{self.task}.png', dpi=300, bbox_inches='tight')
        plt.show()
        
    def handle_small_dataset(self, test_size=0.2, val_size=0.2):
        """
        Modified preprocessing to handle small datasets
        """
        print("[INFO] Preprocessing data for small dataset...")
        
        # Check dataset size
        if len(self.df) < 10:
            print(f"[WARNING] Very small dataset ({len(self.df)} samples). Consider:")
            print("  1. Collecting more data")
            print("  2. Using cross-validation instead of train-test split")
            print("  3. Synthetic data augmentation")
            
            # For very small datasets, use a different approach
            if len(self.df) < 5:
                raise ValueError("Dataset too small for meaningful ML. Need at least 5 samples.")
            
            # Encode labels
            y_encoded = self.label_encoder.fit_transform(self.y)
            y_categorical = to_categorical(y_encoded, num_classes=len(self.label_encoder.classes_))
            
            # For tiny datasets, use simple split without stratification
            X_train, X_test, y_train, y_test = train_test_split(
                self.X, y_categorical, test_size=0.2, random_state=self.random_state
            )
            
            # Use training data for validation too (not ideal but necessary)
            X_val, y_val = X_train, y_train
            
        else:
            # Check class distribution
            class_counts = Counter(self.y)
            min_class_count = min(class_counts.values())
            
            if min_class_count < 2:
                print(f"[WARNING] Class with only {min_class_count} sample(s). Adjusting split strategy...")
                
                # Encode labels
                y_encoded = self.label_encoder.fit_transform(self.y)
                y_categorical = to_categorical(y_encoded, num_classes=len(self.label_encoder.classes_))
                
                # Use simple split without stratification
                X_train, X_test, y_train, y_test = train_test_split(
                    self.X, y_categorical, test_size=test_size, random_state=self.random_state
                )
                
                X_val, y_val = train_test_split(
                    X_train, y_train, test_size=val_size, random_state=self.random_state
                )[0:2]
            else:
                # Normal stratified split
                y_encoded = self.label_encoder.fit_transform(self.y)
                y_categorical = to_categorical(y_encoded, num_classes=len(self.label_encoder.classes_))
                
                X_train_val, X_test, y_train_val, y_test = train_test_split(
                    self.X, y_categorical, test_size=test_size, 
                    random_state=self.random_state, stratify=y_encoded
                )
                
                X_train, X_val, y_train, y_val = train_test_split(
                    X_train_val, y_train_val, test_size=val_size, 
                    random_state=self.random_state, stratify=y_train_val.argmax(axis=1)
                )
        
        # Scale features
        self.scaler.fit(X_train)
        X_train_scaled = self.scaler.transform(X_train)
        X_val_scaled = self.scaler.transform(X_val)
        X_test_scaled = self.scaler.transform(X_test)
        
        # Store preprocessed data
        self.X_train = X_train_scaled
        self.X_val = X_val_scaled
        self.X_test = X_test_scaled
        self.y_train = y_train
        self.y_val = y_val
        self.y_test = y_test
        
        print(f"[INFO] Training set: {X_train_scaled.shape}")
        print(f"[INFO] Validation set: {X_val_scaled.shape}")
        print(f"[INFO] Test set: {X_test_scaled.shape}")
        
        # Compute class weights for imbalanced data
        y_train_labels = y_train.argmax(axis=1)
        if len(np.unique(y_train_labels)) > 1:
            class_weights = compute_class_weight(
                'balanced', classes=np.unique(y_train_labels), y=y_train_labels
            )
            self.class_weights = dict(enumerate(class_weights))
        else:
            self.class_weights = {0: 1.0}  # Single class case
        
        print(f"[INFO] Class weights: {self.class_weights}")

    
    def preprocess_data(self, test_size=0.2, val_size=0.2):
        """
        Preprocess data: encode labels, scale features, split data
        
        Args:
            test_size (float): Proportion of data for testing
            val_size (float): Proportion of training data for validation
        """
        print("[INFO] Preprocessing data...")
        
        # Encode labels
        y_encoded = self.label_encoder.fit_transform(self.y)
        y_categorical = to_categorical(y_encoded, num_classes=len(self.label_encoder.classes_))
        
        # Split data
        X_train_val, X_test, y_train_val, y_test = train_test_split(
            self.X, y_categorical, test_size=test_size, 
            random_state=self.random_state, stratify=y_encoded
        )
        
        # Further split training into train and validation
        X_train, X_val, y_train, y_val = train_test_split(
            X_train_val, y_train_val, test_size=val_size, 
            random_state=self.random_state, stratify=y_train_val.argmax(axis=1)
        )
        
        # Scale features
        self.scaler.fit(X_train)
        X_train_scaled = self.scaler.transform(X_train)
        X_val_scaled = self.scaler.transform(X_val)
        X_test_scaled = self.scaler.transform(X_test)
        
        # Store preprocessed data
        self.X_train = X_train_scaled
        self.X_val = X_val_scaled
        self.X_test = X_test_scaled
        self.y_train = y_train
        self.y_val = y_val
        self.y_test = y_test
        
        print(f"[INFO] Training set: {X_train_scaled.shape}")
        print(f"[INFO] Validation set: {X_val_scaled.shape}")
        print(f"[INFO] Test set: {X_test_scaled.shape}")
        
        # Compute class weights for imbalanced data
        y_train_labels = y_train.argmax(axis=1)
        class_weights = compute_class_weight(
            'balanced', classes=np.unique(y_train_labels), y=y_train_labels
        )
        self.class_weights = dict(enumerate(class_weights))
        print(f"[INFO] Class weights: {self.class_weights}")
    
    def build_model(self, params=None):
        """
        Build MLP model with given parameters
        
        Args:
            params (dict): Model hyperparameters
        """
        if params is None:
            params = self._get_default_params()
        
        model = Sequential([
            Input(shape=(len(self.feature_cols),)),
            
            # First hidden layer
            Dense(params['hidden_units_1'], activation='relu',
                  kernel_regularizer=l1_l2(l1=params['l1_reg'], l2=params['l2_reg'])),
            BatchNormalization(),
            Dropout(params['dropout_1']),
            
            # Second hidden layer
            Dense(params['hidden_units_2'], activation='relu',
                  kernel_regularizer=l1_l2(l1=params['l1_reg'], l2=params['l2_reg'])),
            BatchNormalization(),
            Dropout(params['dropout_2']),
            
            # Third hidden layer (optional)
            Dense(params['hidden_units_3'], activation='relu',
                  kernel_regularizer=l1_l2(l1=params['l1_reg'], l2=params['l2_reg'])),
            BatchNormalization(),
            Dropout(params['dropout_3']),
            
            # Output layer
            Dense(self.output_dim, activation='softmax')
        ])
        
        optimizer = Adam(learning_rate=params['learning_rate'])
        model.compile(
            optimizer=optimizer,
            loss='categorical_crossentropy',
            metrics=['accuracy']
        )
        
        self.model = model
        return model
    
    def _get_default_params(self):
        """Get default hyperparameters"""
        return {
            'hidden_units_1': 512,
            'hidden_units_2': 256,
            'hidden_units_3': 128,
            'dropout_1': 0.3,
            'dropout_2': 0.4,
            'dropout_3': 0.5,
            'learning_rate': 0.001,
            'l1_reg': 0.01,
            'l2_reg': 0.01,
            'batch_size': 32,
            'epochs': 100
        }
    
    def hyperparameter_tuning(self, n_trials=50):
        """
        Perform hyperparameter tuning using Optuna
        
        Args:
            n_trials (int): Number of optimization trials
        """
        print("[INFO] Starting hyperparameter tuning...")
        
        def objective(trial):
            params = {
                'hidden_units_1': trial.suggest_int('hidden_units_1', 128, 1024),
                'hidden_units_2': trial.suggest_int('hidden_units_2', 64, 512),
                'hidden_units_3': trial.suggest_int('hidden_units_3', 32, 256),
                'dropout_1': trial.suggest_float('dropout_1', 0.1, 0.5),
                'dropout_2': trial.suggest_float('dropout_2', 0.1, 0.6),
                'dropout_3': trial.suggest_float('dropout_3', 0.1, 0.7),
                'learning_rate': trial.suggest_float('learning_rate', 1e-4, 1e-2, log=True),
                'l1_reg': trial.suggest_float('l1_reg', 1e-5, 1e-1, log=True),
                'l2_reg': trial.suggest_float('l2_reg', 1e-5, 1e-1, log=True),
                'batch_size': trial.suggest_categorical('batch_size', [16, 32, 64, 128]),
                'epochs': 50  # Reduced for faster tuning
            }
            
            # Build and train model
            model = self.build_model(params)
            
            # Early stopping for tuning
            early_stopping = EarlyStopping(
                monitor='val_loss', patience=10, restore_best_weights=True
            )
            
            history = model.fit(
                self.X_train, self.y_train,
                validation_data=(self.X_val, self.y_val),
                epochs=params['epochs'],
                batch_size=params['batch_size'],
                callbacks=[early_stopping],
                verbose=0,
                class_weight=self.class_weights
            )
            
            # Return best validation accuracy
            return max(history.history['val_accuracy'])
        
        study = optuna.create_study(direction='maximize')
        study.optimize(objective, n_trials=n_trials)
        
        self.best_params = study.best_params
        print(f"[INFO] Best parameters: {self.best_params}")
        print(f"[INFO] Best validation accuracy: {study.best_value:.4f}")
        
        return study.best_params
    
    def train(self, params=None, use_best_params=True):
        """
        Train the MLP model
        
        Args:
            params (dict): Training parameters
            use_best_params (bool): Whether to use hyperparameter tuning results
        """
        if use_best_params and self.best_params is not None:
            params = self.best_params
        elif params is None:
            params = self._get_default_params()
        
        print("[INFO] Training MLP model...")
        
        # Build model
        self.build_model(params)
        
        # Callbacks
        callbacks = [
            EarlyStopping(
                monitor='val_loss',
                patience=20,
                restore_best_weights=True,
                verbose=1
            ),
            ReduceLROnPlateau(
                monitor='val_loss',
                factor=0.5,
                patience=10,
                min_lr=1e-6,
                verbose=1
            ),
            ModelCheckpoint(
                f'best_model_{self.task}.h5',
                monitor='val_accuracy',
                save_best_only=True,
                verbose=1
            )
        ]
        
        # Train model
        self.history = self.model.fit(
            self.X_train, self.y_train,
            validation_data=(self.X_val, self.y_val),
            epochs=params.get('epochs', 100),
            batch_size=params.get('batch_size', 32),
            callbacks=callbacks,
            verbose=1,
            class_weight=self.class_weights
        )
        
        print("[INFO] Training completed!")
    
    def evaluate(self):
        """Evaluate the trained model"""
        if self.model is None:
            raise ValueError("Model not trained yet!")
        
        print("[INFO] Evaluating model...")
        
        # Predictions
        y_pred_proba = self.model.predict(self.X_test)
        y_pred = np.argmax(y_pred_proba, axis=1)
        y_true = np.argmax(self.y_test, axis=1)
        
        # Accuracy
        accuracy = accuracy_score(y_true, y_pred)
        print(f"Test Accuracy: {accuracy:.4f}")
        
        # Classification report
        report = classification_report(
            y_true, y_pred, 
            target_names=self.label_encoder.classes_,
            output_dict=True
        )
        print("\nClassification Report:")
        print(classification_report(y_true, y_pred, target_names=self.label_encoder.classes_))
        
        # Confusion matrix
        cm = confusion_matrix(y_true, y_pred)
        
        return {
            'accuracy': accuracy,
            'classification_report': report,
            'confusion_matrix': cm,
            'predictions': y_pred,
            'true_labels': y_true,
            'prediction_probabilities': y_pred_proba
        }
    
    def plot_training_history(self):
        """Plot training history"""
        if self.history is None:
            raise ValueError("No training history available!")
        
        fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 5))
        
        # Accuracy
        ax1.plot(self.history.history['accuracy'], label='Training Accuracy')
        ax1.plot(self.history.history['val_accuracy'], label='Validation Accuracy')
        ax1.set_title('Model Accuracy')
        ax1.set_xlabel('Epoch')
        ax1.set_ylabel('Accuracy')
        ax1.legend()
        ax1.grid(True)
        
        # Loss
        ax2.plot(self.history.history['loss'], label='Training Loss')
        ax2.plot(self.history.history['val_loss'], label='Validation Loss')
        ax2.set_title('Model Loss')
        ax2.set_xlabel('Epoch')
        ax2.set_ylabel('Loss')
        ax2.legend()
        ax2.grid(True)
        
        plt.tight_layout()
        plt.savefig(f'training_history_{self.task}.png', dpi=300, bbox_inches='tight')
        plt.show()
    
    def plot_confusion_matrix(self, cm, classes):
        """Plot confusion matrix"""
        plt.figure(figsize=(10, 8))
        sns.heatmap(cm, annot=True, fmt='d', cmap='Blues', 
                   xticklabels=classes, yticklabels=classes)
        plt.title(f'Confusion Matrix - {self.task.title()} Classification')
        plt.xlabel('Predicted')
        plt.ylabel('Actual')
        plt.tight_layout()
        plt.savefig(f'confusion_matrix_{self.task}.png', dpi=300, bbox_inches='tight')
        plt.show()
    
    def predict(self, X):
        """Make predictions on new data"""
        if self.model is None:
            raise ValueError("Model not trained yet!")
        
        X_scaled = self.scaler.transform(X)
        predictions = self.model.predict(X_scaled)
        predicted_classes = np.argmax(predictions, axis=1)
        predicted_labels = self.label_encoder.inverse_transform(predicted_classes)
        
        return predicted_labels, predictions
    
    def save_model(self, filepath=None):
        """Save the trained model and preprocessors"""
        if filepath is None:
            filepath = f'gait_guardian_mlp_{self.task}'
        
        # Save model
        self.model.save(f'{filepath}_model.h5')
        
        # Save preprocessors
        joblib.dump(self.scaler, f'{filepath}_scaler.pkl')
        joblib.dump(self.label_encoder, f'{filepath}_label_encoder.pkl')
        
        # Save feature columns
        with open(f'{filepath}_features.json', 'w') as f:
            json.dump(self.feature_cols, f)
        
        print(f"[INFO] Model saved to {filepath}")
    
    def load_model(self, filepath):
        """Load a saved model and preprocessors"""
        # Load model
        self.model = tf.keras.models.load_model(f'{filepath}_model.h5')
        
        # Load preprocessors
        self.scaler = joblib.load(f'{filepath}_scaler.pkl')
        self.label_encoder = joblib.load(f'{filepath}_label_encoder.pkl')
        
        # Load feature columns
        with open(f'{filepath}_features.json', 'r') as f:
            self.feature_cols = json.load(f)
        
        print(f"[INFO] Model loaded from {filepath}")


def main():
    """Main function to demonstrate the MLP implementation"""
    
    # Initialize MLP for subtask classification
    print("\n1. SUBTASK CLASSIFICATION")
    print("-" * 30)
    
    mlp_subtask = GaitGuardianMLP(task='subtask')
    
    try:
        # Load data
        mlp_subtask.load_data()
        
        # Preprocess data
        mlp_subtask.preprocess_data()
        
        # Hyperparameter tuning (optional - comment out for faster execution)
        # mlp_subtask.hyperparameter_tuning(n_trials=20)
        
        # Train model
        mlp_subtask.train()
        
        # Evaluate model
        results = mlp_subtask.evaluate()
        
        # Plot results
        mlp_subtask.plot_training_history()
        mlp_subtask.plot_confusion_matrix(results['confusion_matrix'], 
                                         mlp_subtask.label_encoder.classes_)
        
        # Save model
        mlp_subtask.save_model()
        
    except Exception as e:
        print(f"[ERROR] Subtask classification failed: {e}")
    
    # Initialize MLP for severity prediction
    print("\n2. SEVERITY PREDICTION")
    print("-" * 30)
    
    mlp_severity = GaitGuardianMLP(task='severity')
    
    try:
        # Load data
        mlp_severity.load_data()
        
        # Debug the data first
        available_cols = mlp_severity.debug_severity_classification()
        
        # Use fixed severity classification
        mlp_severity.df['severity'] = mlp_severity._generate_severity_labels_improved(mlp_severity.df)
        
        # Validate the severity classification
        mlp_severity._validate_severity_classification(
            mlp_severity.df, 
            mlp_severity.df['severity'].values
        )
        
        # Update the target variable
        mlp_severity.y = mlp_severity.df['severity'].values
        
        # Check if we have enough samples for each class
        class_counts = Counter(mlp_severity.y)
        print(f"\n[INFO] Updated class distribution: {class_counts}")
        
        if len(mlp_severity.df) < 20:
            print("[WARNING] Dataset too small for reliable ML. Consider:")
            print("  1. Collecting more data")
            print("  2. Using rule-based classification instead")
            print("  3. Synthetic data generation")
            
            # For very small datasets, skip ML training
            return
        
        # Use modified preprocessing for small datasets
        mlp_severity.handle_small_dataset()
        
        # Train with reduced complexity for small datasets
        simple_params = {
            'hidden_units_1': 32,
            'hidden_units_2': 16,
            'hidden_units_3': 8,
            'dropout_1': 0.2,
            'dropout_2': 0.3,
            'dropout_3': 0.4,
            'learning_rate': 0.01,
            'l1_reg': 0.001,
            'l2_reg': 0.001,
            'batch_size': 4,  # Small batch for small dataset
            'epochs': 50
        }
        
        mlp_severity.train(params=simple_params, use_best_params=False)
        
        # Evaluate model
        results = mlp_severity.evaluate()
        
        # Plot results
        mlp_severity.plot_training_history()
        
        # Save model
        mlp_severity.save_model()
        
    except Exception as e:
        print(f"[ERROR] Severity prediction failed: {e}")
        import traceback
        traceback.print_exc()
    

if __name__ == "__main__":
    main()