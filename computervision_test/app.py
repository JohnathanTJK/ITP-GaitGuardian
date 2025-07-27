import os
import cv2
import json
import joblib
import numpy as np
import pandas as pd
import mediapipe as mp
from scipy.stats import mode
from sklearn.preprocessing import LabelEncoder
from scipy.signal import find_peaks
import tempfile
import logging
from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
from werkzeug.utils import secure_filename
import traceback
from datetime import datetime
import uuid

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)  # Enable CORS for Android requests

# Configuration
app.config['MAX_CONTENT_LENGTH'] = 100 * 1024 * 1024  # 100MB max file size
app.config['UPLOAD_FOLDER'] = 'temp_uploads'

# Create upload directory
os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

# Model paths - Update these paths to match your setup
MODEL_PATH = "./model/xgb_model.pkl"
LABEL_PATH = "./model/xgb_label_encoder.pkl"
FEATURE_PATH = "./model/xgb_features.json"

# Global variables for models
model = None
label_encoder = None
expected_cols = None
mp_pose = None
pose = None

def initialize_models():
    """Initialize ML models and MediaPipe"""
    global model, label_encoder, expected_cols, mp_pose, pose
    
    try:
        # Load ML models
        model = joblib.load(MODEL_PATH)
        label_encoder = joblib.load(LABEL_PATH)

        # Patch missing attribute for compatibility
        if not hasattr(model, 'use_label_encoder'):
            model.use_label_encoder = False
        
        with open(FEATURE_PATH, "r") as jf:
            expected_cols = json.load(jf)
        
        # Initialize MediaPipe
        mp_pose = mp.solutions.pose
        pose = mp_pose.Pose(
            static_image_mode=False, 
            model_complexity=1, 
            enable_segmentation=True,
            min_detection_confidence=0.5,
            min_tracking_confidence=0.5
        )
        
        logger.info("All models initialized successfully")
        return True
        
    except Exception as e:
        logger.error(f"Error initializing models: {str(e)}")
        return False

class GaitAnalysisService:
    def __init__(self):
        self.fps_default = 30
        
    def process_video(self, video_path):
        """Main video processing function"""
        processing_start = datetime.now()
        
        try:
            # Open video
            cap = cv2.VideoCapture(video_path)
            if not cap.isOpened():
                raise Exception("Could not open video file")
            
            fps = cap.get(cv2.CAP_PROP_FPS) or self.fps_default
            total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
            
            logger.info(f"Processing video: {total_frames} frames at {fps} FPS")

            keypoints_data = []
            frame_id = 0
            processed_frames = 0

            while cap.isOpened():
                ret, frame = cap.read()
                if not ret:
                    break
                
                # Resize frame for consistent processing
                frame = cv2.resize(frame, (640, 480))
                rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                
                # Process with MediaPipe
                result = pose.process(rgb)
                
                if result.pose_landmarks:
                    for i, lm in enumerate(result.pose_landmarks.landmark):
                        keypoints_data.append([frame_id, i, lm.x, lm.y, lm.z, lm.visibility])
                    processed_frames += 1
                
                frame_id += 1
                
                # Log progress every 100 frames
                if frame_id % 100 == 0:
                    logger.info(f"Processed {frame_id}/{total_frames} frames")

            cap.release()
            
            if not keypoints_data:
                raise Exception("No pose landmarks detected in video")
            
            logger.info(f"Extracted keypoints from {processed_frames} frames")

            # Process keypoints into features
            df = pd.DataFrame(keypoints_data, columns=["frame", "id", "x", "y", "z", "visibility"])
            df_pivot = df.pivot(index='frame', columns='id', values=['x', 'y', 'z', 'visibility'])
            df_pivot.columns = [f'{coord}_{kpt}' for coord, kpt in df_pivot.columns]
            df_pivot.reset_index(inplace=True)

            # Prepare features for ML model
            frame_col = df_pivot['frame']
            df_features_only = df_pivot.reindex(columns=expected_cols, fill_value=0)

            # Make predictions
            X = df_features_only.fillna(0).values
            if X.shape[1] != len(expected_cols):
                logger.warning(f"Feature shape mismatch: expected {len(expected_cols)}, got {X.shape[1]}")
                # Pad or truncate as needed
                if X.shape[1] < len(expected_cols):
                    padding = np.zeros((X.shape[0], len(expected_cols) - X.shape[1]))
                    X = np.hstack([X, padding])
                else:
                    X = X[:, :len(expected_cols)]

            raw_preds = model.predict(X)
            decoded_preds = label_encoder.inverse_transform(raw_preds)
            smoothed_preds = self.smooth_with_window(decoded_preds)
            forced_labels = self.force_phase_sequence(smoothed_preds)

            # Add labels back to dataframe
            df_features_only.insert(0, 'frame', frame_col)
            df_features_only['tug_subtask'] = forced_labels

            # Extract metrics
            gait_metrics = self.extract_gait_features(df_features_only, fps)
            tug_metrics = self.calculate_tug_metrics(df_features_only, fps)
            severity = self.severity_label_from_metrics(tug_metrics, gait_metrics)

            processing_time = (datetime.now() - processing_start).total_seconds()
            
            # Add debug info for severity
            print(f"DEBUG: total_time={tug_metrics.get('total_time', 0):.2f}")
            print(f"DEBUG: walking_time={(tug_metrics.get('walk_from_chair_time', 0) + tug_metrics.get('walk_to_chair_time', 0)):.2f}")
            print(f"DEBUG: turning_time={(tug_metrics.get('turn_first_time', 0) + tug_metrics.get('turn_second_time', 0)):.2f}")
            walking_time = (tug_metrics.get('walk_from_chair_time', 0) + tug_metrics.get('walk_to_chair_time', 0))
            turning_time = (tug_metrics.get('turn_first_time', 0) + tug_metrics.get('turn_second_time', 0))
            turn_walk_ratio = turning_time / max(walking_time, 0.1)
            print(f"DEBUG: turn_walk_ratio={turn_walk_ratio:.2f}")
            print(f"DEBUG: severity={severity}")
            
            result = {
                'success': True,
                'gait_metrics': gait_metrics,
                'tug_metrics': tug_metrics,
                'severity': severity,
                'processing_info': {
                    'total_frames': total_frames,
                    'processed_frames': processed_frames,
                    'fps': fps,
                    'processing_time_seconds': processing_time
                }
            }
            
            logger.info(f"Analysis completed in {processing_time:.2f} seconds")
            return result

        except Exception as e:
            logger.error(f"Error processing video: {str(e)}")
            logger.error(traceback.format_exc())
            return {
                'success': False,
                'error': str(e),
                'error_type': type(e).__name__
            }
        
    def smooth_with_window(self, preds, window=7):
        """Smooth predictions using sliding window"""
        try:
            le = LabelEncoder()
            int_preds = le.fit_transform(preds)
            pad = window // 2
            padded = [int_preds[0]] * pad + list(int_preds) + [int_preds[-1]] * pad
            smoothed = [int(mode(padded[i - pad:i + pad + 1]).mode) 
                       for i in range(pad, len(int_preds) + pad)]
            return le.inverse_transform(smoothed)
        except Exception as e:
            logger.error(f"Error in smoothing: {str(e)}")
            return preds

    def force_phase_sequence(self, preds, phase_order=None, min_frames=5):
        """Force logical phase sequence"""
        if phase_order is None:
            phase_order = [
                'Sit-To-Stand', 'Walk-From-Chair', 'Turn-First',
                'Walk-To-Chair', 'Turn-Second', 'Stand-To-Sit'
            ]
        
        result = [''] * len(preds)
        current_phase, count = 0, 0
        
        for i in range(len(preds)):
            if current_phase >= len(phase_order):
                result[i] = phase_order[-1]
                continue
                
            expected = phase_order[current_phase]
            result[i] = expected
            
            if i + 5 < len(preds):
                window = preds[i:i+5]
                next_phase_idx = min(current_phase + 1, len(phase_order)-1)
                if list(window).count(phase_order[next_phase_idx]) >= 3:
                    count += 1
                    if count >= min_frames:
                        current_phase += 1
                        count = 0
                else:
                    count = 0
        
        return result

    def extract_gait_features(self, df, fps):
        """Extract gait features from processed dataframe"""
        try:
            walk_df = df[df['tug_subtask'].isin(['Walk-From-Chair', 'Walk-To-Chair'])].copy()
            turn1_df = df[df['tug_subtask'] == 'Turn-First']
            turn2_df = df[df['tug_subtask'] == 'Turn-Second']

            if len(walk_df) == 0:
                logger.warning("No walking data found")
                return self._default_gait_features()

            # Calculate ankle distance for step detection
            if 'x_27' in walk_df.columns and 'x_28' in walk_df.columns:
                walk_df['ankle_distance'] = np.abs(walk_df['x_27'] - walk_df['x_28'])
                peaks, _ = find_peaks(walk_df['ankle_distance'], distance=10)
                step_lengths = walk_df.iloc[peaks]['ankle_distance'].values
                step_times = np.array(peaks) / fps if len(peaks) > 0 else []
            else:
                peaks = []
                step_lengths = []
                step_times = []

            step_count = len(peaks)
            step_durations = np.diff(step_times) if step_count >= 2 else [0]
            stride_time = np.mean(step_durations) * 2 if len(step_durations) > 1 else 0
            cadence = (step_count / (len(walk_df) / fps)) * 60 if len(walk_df) > 0 else 0
            mean_step_length = np.mean(step_lengths) if step_count > 0 else 0

            # Step symmetry calculation
            if step_count >= 4:
                left_steps = step_lengths[::2]
                right_steps = step_lengths[1::2]
                symmetry = np.abs(np.mean(left_steps) - np.mean(right_steps)) / max(
                    np.mean([*left_steps, *right_steps]), 1e-6)
            else:
                symmetry = 0

            return {
                'step_count': int(step_count),
                'mean_step_length': float(mean_step_length),
                'stride_time': float(stride_time),
                'cadence': float(cadence),
                'step_symmetry': float(symmetry),
                'left_knee_range': 0.0,  # Simplified for API
                'right_knee_range': 0.0,  # Simplified for API
                'upper_body_sway': 0.0,  # Simplified for API
                'turn1_duration': float(len(turn1_df) / fps),
                'turn2_duration': float(len(turn2_df) / fps)
            }
            
        except Exception as e:
            logger.error(f"Error extracting gait features: {str(e)}")
            return self._default_gait_features()

    def _default_gait_features(self):
        """Return default gait features when calculation fails"""
        return {
            'step_count': 0,
            'mean_step_length': 0.0,
            'stride_time': 0.0,
            'cadence': 0.0,
            'step_symmetry': 0.0,
            'left_knee_range': 0.0,
            'right_knee_range': 0.0,
            'upper_body_sway': 0.0,
            'turn1_duration': 0.0,
            'turn2_duration': 0.0
        }

    def calculate_tug_metrics(self, df, fps):
        """Calculate TUG phase metrics"""
        try:
            phase_durations = df['tug_subtask'].value_counts().to_dict()
            phase_durations_sec = {k: v / fps for k, v in phase_durations.items()}

            return {
                'sit_to_stand_time': phase_durations_sec.get('Sit-To-Stand', 0),
                'walk_from_chair_time': phase_durations_sec.get('Walk-From-Chair', 0),
                'turn_first_time': phase_durations_sec.get('Turn-First', 0),
                'walk_to_chair_time': phase_durations_sec.get('Walk-To-Chair', 0),
                'turn_second_time': phase_durations_sec.get('Turn-Second', 0),
                'stand_to_sit_time': phase_durations_sec.get('Stand-To-Sit', 0),
                'total_time': sum(phase_durations_sec.values())
            }
        except Exception as e:
            logger.error(f"Error calculating TUG metrics: {str(e)}")
            return {
                'sit_to_stand_time': 0,
                'walk_from_chair_time': 0,
                'turn_first_time': 0,
                'walk_to_chair_time': 0,
                'turn_second_time': 0,
                'stand_to_sit_time': 0,
                'total_time': 0
            }

    def severity_label_from_metrics(self, tug_metrics, gait_metrics):
        """Classify severity based on metrics (from testing2.py)"""
        try:
            total_time = tug_metrics.get('total_time', 0)
            walking_time = (tug_metrics.get('walk_from_chair_time', 0) + 
                        tug_metrics.get('walk_to_chair_time', 0))
            turning_time = (tug_metrics.get('turn_first_time', 0) + 
                        tug_metrics.get('turn_second_time', 0))
            
            turn_walk_ratio = turning_time / max(walking_time, 0.1)
            
            # Add debug logging
            logger.info(f"=== SEVERITY CLASSIFICATION DEBUG ===")
            logger.info(f"Total time: {total_time:.2f}s")
            logger.info(f"Walking time: {walking_time:.2f}s")
            logger.info(f"Turning time: {turning_time:.2f}s")
            logger.info(f"Turn/Walk ratio: {turn_walk_ratio:.2f}")
            
            # Rule-based classification from testing2.py
            if total_time <= 7 and turn_walk_ratio < 1.0:
                severity = 'Normal'
            elif total_time <= 13:
                if turn_walk_ratio < 1.0:
                    severity = 'Slight'
                else:
                    severity = 'Mild'
            else:
                if turn_walk_ratio > 1.0:
                    severity = 'Severe'
                else:
                    severity = 'Moderate'
            
            logger.info(f"Classification: {severity}")
            logger.info(f"=====================================")
            return severity
                        
        except Exception as e:
            logger.error(f"Error in severity classification: {str(e)}")
            return 'Unknown'

    def process_video(self, video_path):
        """Main video processing function"""
        processing_start = datetime.now()
        
        try:
            # Open video
            cap = cv2.VideoCapture(video_path)
            if not cap.isOpened():
                raise Exception("Could not open video file")
            
            fps = cap.get(cv2.CAP_PROP_FPS) or self.fps_default
            total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
            
            logger.info(f"Processing video: {total_frames} frames at {fps} FPS")

            keypoints_data = []
            frame_id = 0
            processed_frames = 0

            while cap.isOpened():
                ret, frame = cap.read()
                if not ret:
                    break
                
                # Resize frame for consistent processing
                frame = cv2.resize(frame, (640, 480))
                rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                
                # Process with MediaPipe
                result = pose.process(rgb)
                
                if result.pose_landmarks:
                    for i, lm in enumerate(result.pose_landmarks.landmark):
                        keypoints_data.append([frame_id, i, lm.x, lm.y, lm.z, lm.visibility])
                    processed_frames += 1
                
                frame_id += 1
                
                # Log progress every 100 frames
                if frame_id % 100 == 0:
                    logger.info(f"Processed {frame_id}/{total_frames} frames")

            cap.release()
            
            if not keypoints_data:
                raise Exception("No pose landmarks detected in video")
            
            logger.info(f"Extracted keypoints from {processed_frames} frames")

            # Process keypoints into features
            df = pd.DataFrame(keypoints_data, columns=["frame", "id", "x", "y", "z", "visibility"])
            df_pivot = df.pivot(index='frame', columns='id', values=['x', 'y', 'z', 'visibility'])
            df_pivot.columns = [f'{coord}_{kpt}' for coord, kpt in df_pivot.columns]
            df_pivot.reset_index(inplace=True)

            # Prepare features for ML model
            frame_col = df_pivot['frame']
            df_features_only = df_pivot.reindex(columns=expected_cols, fill_value=0)

            # Make predictions
            X = df_features_only.fillna(0).values
            if X.shape[1] != len(expected_cols):
                logger.warning(f"Feature shape mismatch: expected {len(expected_cols)}, got {X.shape[1]}")
                # Pad or truncate as needed
                if X.shape[1] < len(expected_cols):
                    padding = np.zeros((X.shape[0], len(expected_cols) - X.shape[1]))
                    X = np.hstack([X, padding])
                else:
                    X = X[:, :len(expected_cols)]

            raw_preds = model.predict(X)
            decoded_preds = label_encoder.inverse_transform(raw_preds)
            smoothed_preds = self.smooth_with_window(decoded_preds)
            forced_labels = self.force_phase_sequence(smoothed_preds)

            # Add labels back to dataframe
            df_features_only.insert(0, 'frame', frame_col)
            df_features_only['tug_subtask'] = forced_labels

            # Extract metrics
            gait_metrics = self.extract_gait_features(df_features_only, fps)
            tug_metrics = self.calculate_tug_metrics(df_features_only, fps)
            severity = self.severity_label_from_metrics(tug_metrics, gait_metrics)

            processing_time = (datetime.now() - processing_start).total_seconds()
            
            result = {
                'success': True,
                'gait_metrics': gait_metrics,
                'tug_metrics': tug_metrics,
                'severity': severity,
                'processing_info': {
                    'total_frames': total_frames,
                    'processed_frames': processed_frames,
                    'fps': fps,
                    'processing_time_seconds': processing_time
                }
            }
            
            logger.info(f"Analysis completed in {processing_time:.2f} seconds")
            return result

        except Exception as e:
            logger.error(f"Error processing video: {str(e)}")
            logger.error(traceback.format_exc())
            return {
                'success': False,
                'error': str(e),
                'error_type': type(e).__name__
            }

# Initialize service
gait_service = GaitAnalysisService()

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        'status': 'healthy',
        'timestamp': datetime.now().isoformat(),
        'models_loaded': model is not None and label_encoder is not None
    })

@app.route('/analyze_gait', methods=['POST'])
def analyze_gait():
    """Main gait analysis endpoint"""
    request_id = str(uuid.uuid4())[:8]
    logger.info(f"[{request_id}] Starting gait analysis request")
    
    try:
        # Check if video file is uploaded
        if 'video' not in request.files:
            return jsonify({'error': 'No video file provided'}), 400
        
        video_file = request.files['video']
        if video_file.filename == '':
            return jsonify({'error': 'No video file selected'}), 400

        # Validate file type
        allowed_extensions = {'.mp4', '.avi', '.mov', '.mkv'}
        file_ext = os.path.splitext(video_file.filename)[1].lower()
        if file_ext not in allowed_extensions:
            return jsonify({'error': f'Unsupported file type: {file_ext}'}), 400

        # Save uploaded video temporarily
        filename = secure_filename(f"{request_id}_{video_file.filename}")
        temp_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        video_file.save(temp_path)
        
        logger.info(f"[{request_id}] Video saved to {temp_path}")

        try:
            # Process video
            result = gait_service.process_video(temp_path)
            result['request_id'] = request_id
            
            return jsonify(result)

        finally:
            # Clean up temporary file
            try:
                os.unlink(temp_path)
                logger.info(f"[{request_id}] Cleaned up temporary file")
            except Exception as cleanup_error:
                logger.warning(f"[{request_id}] Could not clean up file: {cleanup_error}")

    except Exception as e:
        logger.error(f"[{request_id}] Unexpected error: {str(e)}")
        logger.error(traceback.format_exc())
        return jsonify({
            'error': str(e),
            'request_id': request_id,
            'error_type': type(e).__name__
        }), 500

@app.route('/models/info', methods=['GET'])
def model_info():
    """Get information about loaded models"""
    return jsonify({
        'model_loaded': model is not None,
        'label_encoder_loaded': label_encoder is not None,
        'expected_features': len(expected_cols) if expected_cols else 0,
        'pose_detector_loaded': pose is not None,
        'supported_formats': ['.mp4', '.avi', '.mov', '.mkv']
    })

if __name__ == '__main__':
    logger.info("Starting GaitGuardian API Service...")
    
    if not initialize_models():
        logger.error("Failed to initialize models. Exiting.")
        exit(1)
    
    logger.info("Models initialized successfully")
    
    # For development
    # port = int(os.environ.get('PORT', 5001))
    # app.run(host='0.0.0.0', port=port, debug=False)
    app.run(host='0.0.0.0', port=5001, debug=False)    
    # For production, use a WSGI server like Gunicorn:
    # gunicorn --bind 0.0.0.0:5000 --workers 2 --timeout 300 app:app