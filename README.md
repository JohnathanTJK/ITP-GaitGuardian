# GaitGuardian

## 🗂️ Project Structure
### File Structure of Pose Detection and Machine Learning
```
computervision/
├── keypoints_and_durations/         # Labelled pose landmark CSVs and duration JSONs
├── video_results/                   # Videos with pose landmark annotations
├── videos/                          # Sample SAIL-TUG videos
├── frame.py                         # Frame-by-frame video viewer (OpenCV)
├── pose_and_subtask.py              # Pose estimation, subtask classification, and gait feature extraction (MediaPipe + XGBoost)
├── pose_est_mlp.py                  # MLP-based classification for TUG subtask & severity prediction (TensorFlow, sklearn, Optuna)
├── test2.py                         # Miscellaneous testing script
├── train_xgb.py                     # Train XGBoost model for subtask prediction
├── tug_analyzer.py                  # General-purpose TUG analysis
├── xgb_features.json                # Pose feature columns used by XGBoost
├── xgb_label_encoder.pkl            # LabelEncoder for subtask labels
└── xgb_model.pkl                    # Pretrained XGBoost model

computervision_test/
├── data/                            # Labelled pose landmark CSVs
├── model/
│   ├── xgb_features.json            # Pose feature columns used by XGBoost
│   ├── xgb_label_encoder.pkl        # LabelEncoder for subtask labels
│   └── xgb_model.pkl                # Pretrained XGBoost model
│
├── ml_analysis.py                   # SHAP explainability analysis of MLP model
├── testing.py                       # Train MLP model for severity classification using gait features & keypoints
├── testing2.py                      # Gait feature extraction from pose data
├── testing3.py                      # Train MLP using extracted gait features
├── testingml.py                     # Test MLP model performance
└── tug_analyzer.py                  # General-purpose model analysis and visualization
```
