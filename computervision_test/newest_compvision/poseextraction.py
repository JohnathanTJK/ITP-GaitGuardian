import cv2
import mediapipe as mp
import os
import pandas as pd
import numpy as np
import time

# === Config ===
# input_dir = "videos"
# output_video_dir = "resultsVID"
# output_csv_dir = "raw_landmarks_csv"
input_dir = "testvideo"
output_video_dir = "testresultsVID"
output_csv_dir = "testrawlandmarks"

# Performance optimization settings
FRAME_SKIP = 1  # Process every N frames (1 = all frames, 2 = every other frame, etc.)
# NOTE: FRAME_SKIP set to 1 to ensure ALL frames are processed for downstream analysis
ENABLE_VIDEO_OUTPUT = False  # Set to False to skip video generation for faster processing
VIDEO_QUALITY = 0.7  # 0.5-1.0, lower = faster processing
PROGRESS_UPDATE_INTERVAL = 30  # Show progress every N frames

# Try to find pose_landmarker_full.task in current directory or parent
model_path = "poseextraction_helper/pose_landmarker_full.task"
print(f"📁 Using pose model: {model_path}")
print(f"⚡ Performance settings:")
print(f"   - Frame skip: {FRAME_SKIP} (processing ALL frames)")
print(f"   - Video output: {'Enabled' if ENABLE_VIDEO_OUTPUT else 'Disabled (faster)'}")
print(f"   - Video quality: {VIDEO_QUALITY:.1f}")

os.makedirs(output_video_dir, exist_ok=True)
os.makedirs(output_csv_dir, exist_ok=True)

fps_default = 30

# === MediaPipe Setup with Task API ===
BaseOptions = mp.tasks.BaseOptions
PoseLandmarker = mp.tasks.vision.PoseLandmarker
PoseLandmarkerOptions = mp.tasks.vision.PoseLandmarkerOptions
VisionRunningMode = mp.tasks.vision.RunningMode

# Configure the pose landmarker
options = PoseLandmarkerOptions(
    base_options=BaseOptions(model_asset_path=model_path),
    running_mode=VisionRunningMode.VIDEO,
    num_poses=1,
    min_pose_detection_confidence=0.5,
    min_pose_presence_confidence=0.5,
    min_tracking_confidence=0.5,
    output_segmentation_masks=False
)

# Drawing utilities for visualization
mp_drawing = mp.solutions.drawing_utils
mp_pose = mp.solutions.pose

# === Process Each Video ===
video_files = [f for f in os.listdir(input_dir) if f.endswith(".mp4")]
total_videos = len(video_files)

print(f"\n🎬 Found {total_videos} video(s) to process")

for video_idx, filename in enumerate(video_files, 1):
    print(f"\n{'='*60}")
    print(f"🎥 Processing video {video_idx}/{total_videos}: {filename}")
    print(f"{'='*60}")
    
    video_start_time = time.time()
    cap = cv2.VideoCapture(os.path.join(input_dir, filename))
    
    # Get video properties
    fps = cap.get(cv2.CAP_PROP_FPS)
    if fps == 0 or np.isnan(fps):
        fps = fps_default
    
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    duration_sec = total_frames / fps
    
    print(f"📊 Video info:")
    print(f"   - Total frames: {total_frames}")
    print(f"   - FPS: {fps:.1f}")
    print(f"   - Duration: {duration_sec:.1f}s")
    print(f"   - Processing every {FRAME_SKIP} frame(s) - ALL FRAMES")
    
    effective_frames = total_frames  # Process all frames
    print(f"   - Frames to process: {effective_frames} (complete dataset)")

    # Setup video writer for annotated output (if enabled)
    out_video = None
    if ENABLE_VIDEO_OUTPUT:
        # Reduce output resolution for faster processing
        output_width = int(640 * VIDEO_QUALITY)
        output_height = int(480 * VIDEO_QUALITY)
        
        out_video = cv2.VideoWriter(
            os.path.join(output_video_dir, filename),
            cv2.VideoWriter_fourcc(*'mp4v'),
            fps,
            (output_width, output_height)
        )
        print(f"🎬 Output video: {output_width}x{output_height}")
    else:
        print(f"🚀 Video output disabled for faster processing")

    keypoints_data = []
    frame_id = 0
    processed_frames = 0
    poses_detected = 0
    last_progress_time = time.time()

    print(f"\n🔄 Starting pose detection...")

    # Create pose landmarker instance for this video
    with PoseLandmarker.create_from_options(options) as landmarker:
        while cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                break

            # Frame skipping for faster processing
            if frame_id % FRAME_SKIP != 0:
                frame_id += 1
                continue

            # Progress logging
            current_time = time.time()
            if (processed_frames % PROGRESS_UPDATE_INTERVAL == 0 and processed_frames > 0) or (current_time - last_progress_time > 2.0):
                progress_pct = (frame_id / total_frames) * 100
                elapsed_time = current_time - video_start_time
                
                if processed_frames > 0:
                    frames_per_sec = processed_frames / elapsed_time
                    remaining_frames = effective_frames - processed_frames
                    eta_seconds = remaining_frames / frames_per_sec if frames_per_sec > 0 else 0
                    
                    print(f"⏳ Progress: {progress_pct:.1f}% | Frame {frame_id}/{total_frames} | "
                          f"Poses: {poses_detected}/{processed_frames} | "
                          f"Speed: {frames_per_sec:.1f} fps | "
                          f"ETA: {eta_seconds:.0f}s")
                
                last_progress_time = current_time

            processed_frames += 1

            # Resize frame for consistent processing
            frame = cv2.resize(frame, (640, 480))
            
            # Convert BGR to RGB for MediaPipe
            rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            
            # Create MediaPipe Image
            mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
            
            # Calculate timestamp in microseconds
            timestamp_ms = int(frame_id * (1000 / fps))
            
            # Process pose detection
            detection_result = landmarker.detect_for_video(mp_image, timestamp_ms)

            pose_detected = False
            # Draw pose landmarks if detected
            if detection_result.pose_landmarks:
                poses_detected += 1
                pose_detected = True
                
                for pose_landmarks in detection_result.pose_landmarks:
                    # Only draw if video output is enabled
                    if ENABLE_VIDEO_OUTPUT and out_video is not None:
                        # Draw landmarks manually since TaskAPI format is different
                        h, w, _ = frame.shape
                        
                        # Draw connections
                        for connection in mp_pose.POSE_CONNECTIONS:
                            start_idx, end_idx = connection
                            if start_idx < len(pose_landmarks) and end_idx < len(pose_landmarks):
                                start_point = pose_landmarks[start_idx]
                                end_point = pose_landmarks[end_idx]
                                
                                start_x = int(start_point.x * w)
                                start_y = int(start_point.y * h)
                                end_x = int(end_point.x * w)
                                end_y = int(end_point.y * h)
                                
                                cv2.line(frame, (start_x, start_y), (end_x, end_y), (0, 0, 255), 2)
                        
                        # Draw landmarks
                        for landmark in pose_landmarks:
                            x = int(landmark.x * w)
                            y = int(landmark.y * h)
                            cv2.circle(frame, (x, y), 3, (0, 255, 0), -1)
                    
                    # Extract keypoint coordinates (use first pose if multiple detected)
                    for i, landmark in enumerate(pose_landmarks):
                        keypoints_data.append([
                            frame_id, 
                            i, 
                            landmark.x, 
                            landmark.y, 
                            landmark.z, 
                            landmark.visibility if hasattr(landmark, 'visibility') else 1.0
                        ])
                    break  # Only process first detected pose

            # Write annotated frame to output video (if enabled)
            if ENABLE_VIDEO_OUTPUT and out_video is not None:
                # Resize for output quality setting
                if VIDEO_QUALITY != 1.0:
                    output_width = int(640 * VIDEO_QUALITY)
                    output_height = int(480 * VIDEO_QUALITY)
                    frame = cv2.resize(frame, (output_width, output_height))
                
                out_video.write(frame)
            
            frame_id += 1

    # Release video resources
    cap.release()
    if out_video is not None:
        out_video.release()
    
    processing_time = time.time() - video_start_time
    
    print(f"\n📊 Processing complete for {filename}:")
    print(f"   - Total processing time: {processing_time:.1f}s")
    print(f"   - Frames processed: {processed_frames}/{total_frames}")
    print(f"   - Poses detected: {poses_detected}/{processed_frames} ({poses_detected/processed_frames*100:.1f}%)")
    print(f"   - Average speed: {processed_frames/processing_time:.1f} fps")

    try:
        # === Save Raw Landmarks CSV ===
        if keypoints_data:
            print(f"💾 Saving landmarks data...")
            df = pd.DataFrame(keypoints_data, columns=["frame", "landmark_id", "x", "y", "z", "visibility"])
            
            # Pivot to get landmarks as columns
            df_pivot = df.pivot_table(
                index='frame', 
                columns='landmark_id', 
                values=['x', 'y', 'z', 'visibility'],
                fill_value=0
            )
            
            # Flatten column names
            df_pivot.columns = [f'{coord}_{landmark}' for coord, landmark in df_pivot.columns]
            df_pivot = df_pivot.reset_index()

            # Save raw landmarks
            base_name = filename.replace(".mp4", "")
            csv_output_path = os.path.join(output_csv_dir, f"{base_name}_landmarks.csv")
            df_pivot.to_csv(csv_output_path, index=False)
            
            print(f"✅ Results:")
            print(f"   - Extracted {len(df_pivot)} frames with {len(df_pivot.columns)-1} landmark features")
            print(f"   - CSV saved: {csv_output_path}")
            if ENABLE_VIDEO_OUTPUT:
                print(f"   - Video saved: {os.path.join(output_video_dir, filename)}")
            else:
                print(f"   - Video output: Skipped (disabled for speed)")
        else:
            print(f"⚠️  No pose landmarks detected in {filename}")

    except Exception as e:
        print(f"❌ Failed processing {filename}: {e}")
        import traceback
        traceback.print_exc()

print(f"\n🎉 All video processing complete!")
print(f"� Summary:")
print(f"   - Videos processed: {total_videos}")
print(f"   - Raw landmarks saved to: {output_csv_dir}")
if ENABLE_VIDEO_OUTPUT:
    print(f"   - Annotated videos saved to: {output_video_dir}")
else:
    print(f"   - Video output: Disabled for faster processing")

print(f"\n💡 Performance tips:")
print(f"   - Frame skip: {FRAME_SKIP} (ALL frames processed for complete analysis)")
print(f"   - To speed up: Set ENABLE_VIDEO_OUTPUT = False")
print(f"   - To speed up: Reduce VIDEO_QUALITY (currently {VIDEO_QUALITY})")
print(f"   - Note: Frame skipping disabled to ensure complete dataset")
