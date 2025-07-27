# GaitGuardian

🗂️ Project Structure
File Structure of Pose Detection and Machine Learning

computervision
├── keypoints_and_durations/            ← Labelled Pose landmark coordinates CSVs and Duration Json
├── video_results/                      ← Videos with Pose landmarks
├── videos/                             ← Some SAIL-TUG Videos
├── frame.py                            ← Frame-by-frame video viewer
├── pose_and_subtask.py                 ← # Video pose estimation, Subtask classification, and Gait feature extraction (MediaPipe, XGBoost)
├── pose_est_mlp.py                     ← # MLP-based classification for TUG subtask and severity prediction (TensorFlow, sklearn, Optuna)
├── test2.py                            ← 
├── train_xgb.py                        ← Train XGBoost classifier for TUG subtask prediction
├── tug_analyzer.py                     ← 
├── xgb_features.json                   ← Pose Feature columns for XGBoost model input
├── xgb_label_encoder.pkl               ← LabelEncoder for TUG subtask labels
└── xgb_model.pkl                       ← XGBoost model

computervision_test
├── data/                               ← Labelled Pose landmark coordinates CSVs
├── model/
│   ├── xgb_features.json               ← Pose Feature columns for XGBoost model input
│   ├── xgb_label_encoder.pkl           ← LabelEncoder for TUG subtask labels
│   └── xgb_model.pkl                   ← XGBoost model
│
├── ml_analysis.py/                     ← SHAP analysis of MLP model
├── testing.py/                         ← Training MLP model for Severity Classification using Gait Features and keypoints
├── testing2.py/                        ← Gait Feature Extraction
├── testing3.py/                        ← Training MLP model for Severity Classification using Extracted Gait Features
├── testingml.py/                       ← MLP Model testing
└── tug_analyzer.py/                    ← General Use model analysis code.
