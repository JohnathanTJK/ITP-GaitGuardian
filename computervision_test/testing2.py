import os
import pandas as pd
import numpy as np

# === Config ===
input_data_dir = "./computervision_test/data"
output_data_path = os.path.join(input_data_dir, "gait_features_with_severity.csv")
fps_default = 30  # Change if your videos use a different FPS

def calculate_tug_metrics(df, fps):
    # Count frames for each subtask
    phase_durations = df['tug_subtask'].value_counts().to_dict()
    phase_durations_sec = {k: v / fps for k, v in phase_durations.items()}

    # Individual phase times
    sit_to_stand_time = phase_durations_sec.get('Sit-To-Stand', 0)
    walk_from_chair_time = phase_durations_sec.get('Walk-From-Chair', 0)
    turn_first_time = phase_durations_sec.get('Turn-First', 0)
    walk_to_chair_time = phase_durations_sec.get('Walk-To-Chair', 0)
    turn_second_time = phase_durations_sec.get('Turn-Second', 0)
    stand_to_sit_time = phase_durations_sec.get('Stand-To-Sit', 0)

    total_walking_time = walk_from_chair_time + walk_to_chair_time
    total_turning_time = turn_first_time + turn_second_time
    total_time = sum(phase_durations_sec.values())
    turn_walk_ratio = total_turning_time / max(total_walking_time, 0.1)

    return {
        'sit_to_stand_time': sit_to_stand_time,
        'walk_from_chair_time': walk_from_chair_time,
        'turn_first_time': turn_first_time,
        'walk_to_chair_time': walk_to_chair_time,
        'turn_second_time': turn_second_time,
        'stand_to_sit_time': stand_to_sit_time,
        'total_walking_time': total_walking_time,
        'total_turning_time': total_turning_time,
        'total_time': total_time,
        'turn_walk_ratio': turn_walk_ratio
    }

def extract_gait_features(df, fps):
    # Step count and step features (using ankle distance peaks)
    walk_df = df[df['tug_subtask'].isin(['Walk-From-Chair', 'Walk-To-Chair'])].copy()
    if 'x_27' in walk_df and 'x_28' in walk_df:
        walk_df['ankle_distance'] = np.abs(walk_df['x_27'] - walk_df['x_28'])
        from scipy.signal import find_peaks
        peaks, _ = find_peaks(walk_df['ankle_distance'], distance=10)
        step_lengths = walk_df.iloc[peaks]['ankle_distance'].values
        step_times = np.array(peaks) / fps if len(peaks) > 0 else []
        step_count = len(peaks)
        step_durations = np.diff(step_times) if step_count >= 2 else [0]
        stride_time = np.mean(step_durations) * 2 if len(step_durations) > 1 else 0
        cadence = (step_count / (len(walk_df) / fps)) * 60 if len(walk_df) > 0 else 0
        mean_step_length = np.mean(step_lengths) if step_count > 0 else 0
        if step_count >= 4:
            left_steps = step_lengths[::2]
            right_steps = step_lengths[1::2]
            symmetry = np.abs(np.mean(left_steps) - np.mean(right_steps)) / max(np.mean([*left_steps, *right_steps]), 1e-6)
        else:
            symmetry = np.nan
    else:
        step_count = mean_step_length = stride_time = cadence = symmetry = np.nan

    # Dummy knee range and sway calculation (customize as needed)
    left_knee_range = right_knee_range = upper_body_sway = 0.0

    # Turn durations
    turn1_duration = len(df[df['tug_subtask'] == 'Turn-First']) / fps
    turn2_duration = len(df[df['tug_subtask'] == 'Turn-Second']) / fps

    return {
        'step_count': step_count,
        'mean_step_length': mean_step_length,
        'stride_time': stride_time,
        'cadence': cadence,
        'step_symmetry': symmetry,
        'left_knee_range': left_knee_range,
        'right_knee_range': right_knee_range,
        'upper_body_sway': upper_body_sway,
        'turn1_duration': turn1_duration,
        'turn2_duration': turn2_duration
    }

def severity_label_from_metrics(metrics):
    total_time = metrics.get('total_time', 0)
    turn_walk_ratio = metrics.get('turn_walk_ratio', 0)
    walk_time = metrics.get('total_walking_time', 0)
    turn_time = metrics.get('total_turning_time', 0)
    # Simple rule-based severity
    if total_time <= 7 and turn_walk_ratio < 1.0:
        return 'Normal'
    elif total_time <= 13:
        if turn_walk_ratio < 1.0:
            return 'Slight'
        else:
            return 'Mild'
    else:
        if turn_walk_ratio > 1.0:
            return 'Severe'
        else:
            return 'Moderate'

# Collect all per-video labelled CSVs
gait_feature_files = [
    os.path.join(input_data_dir, f)
    for f in os.listdir(input_data_dir)
    if f.endswith("_labelled.csv")
]

all_gait_features = []
for csv_path in gait_feature_files:
    df = pd.read_csv(csv_path)
    # Try to get FPS from metadata if available, else use default
    fps = fps_default
    # Calculate metrics
    tug_metrics = calculate_tug_metrics(df, fps)
    gait_metrics = extract_gait_features(df, fps)
    metrics = {**gait_metrics, **tug_metrics}
    metrics['video'] = os.path.basename(csv_path).replace("_labelled.csv", "")
    metrics['severity'] = severity_label_from_metrics(metrics)
    all_gait_features.append(metrics)

# Save aggregated gait features with severity
gait_df = pd.DataFrame(all_gait_features)
gait_df.to_csv(output_data_path, index=False)
print(f"Saved gait features and TUG metrics with severity labels to {output_data_path}")