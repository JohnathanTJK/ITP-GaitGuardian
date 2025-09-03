import os
import pandas as pd
import numpy as np
from math import degrees, acos, atan2
from numpy.linalg import norm

# === Config ===
# input_dir = "raw_landmarks_csv"
# output_dir = "engineered_features_csv"
input_dir = "testrawlandmarks"
output_dir = "test_engineered_features_csv"
os.makedirs(output_dir, exist_ok=True)
fps = 30

# === TUG Phase-Specific Feature Extraction ===
# Based on SAIL research: Deep-Learning-Based System for Automatic Gait Assessment From TUG Videos
# Focus on clinically relevant biomechanical features for phase classification

def angle_between(a, b, c):
    """Calculate angle between three points"""
    ba = a - b
    bc = c - b
    cos_angle = np.clip(np.dot(ba, bc) / (norm(ba) * norm(bc) + 1e-6), -1.0, 1.0)
    return degrees(acos(cos_angle))

def extract_tug_features(df):
    """
    Extract features specifically designed for TUG phase classification based on SAIL research:
    
    Key insights from deep learning TUG analysis:
    1. Temporal patterns are crucial for phase detection
    2. Joint kinematics provide strongest discrimination
    3. Center of mass trajectory reveals gait patterns
    4. Multi-scale features capture both micro and macro movements
    5. Spatial-temporal gait parameters are clinical gold standard
    
    TUG Phases:
    - Sit-To-Stand: Vertical rise, hip/knee extension, momentum generation
    - Walk-From-Chair: Gait initiation, step patterns, forward progression
    - Turn-First: Direction change, rotational kinematics, balance
    - Walk-To-Chair: Steady gait, spatial-temporal parameters
    - Turn-Second: Second direction change, deceleration preparation
    - Stand-To-Sit: Controlled descent, joint flexion, stability
    """
    features = []
    
    # Initialize previous values for velocity calculations
    prev_vals = {
        'hip_y': 0, 'hip_x': 0, 'shoulder_angle': 0, 'torso_angle': 0,
        'head_yaw': 0, 'com_x': 0, 'com_y': 0, 'com_x_velocity': 0, 'com_y_velocity': 0,
        'forward_momentum': 0, 'hip_rotation': 0, 'left_knee_angle': 0, 'right_knee_angle': 0
    }
    
    # Step detection variables
    last_step_frame = 0
    step_frames = []
    
    print(f"🔍 Extracting TUG-specific features from {len(df)} frames...")
    
    for i in range(len(df)):
        row = df.iloc[i]
        prev = df.iloc[i - 1] if i > 0 else row
        
        # === Helper function for landmark coordinates ===
        def point(coord, idx):
            return np.array([row[f'{coord}_{idx}'], row[f'y_{idx}']])
        
        def point_3d(idx):
            return np.array([row[f'x_{idx}'], row[f'y_{idx}'], row[f'z_{idx}']])
        
        # === Key Body Points ===
        # Hips: 23=left, 24=right
        lh, rh = point('x', 23), point('x', 24)
        # Knees: 25=left, 26=right  
        lk, rk = point('x', 25), point('x', 26)
        # Ankles: 27=left, 28=right
        la, ra = point('x', 27), point('x', 28)
        # Shoulders: 11=left, 12=right
        ls, rs = point('x', 11), point('x', 12)
        # Feet: 29=left heel, 30=right heel, 31=left toe, 32=right toe
        lheel, rheel = point('x', 29), point('x', 30)
        ltoe, rtoe = point('x', 31), point('x', 32)
        # Head: 0=nose
        nose = point('x', 0)
        
        # === 1. CLINICAL GAIT PARAMETERS (SAIL Research Focus) ===
        
        # Spatial-temporal gait parameters - gold standard for TUG analysis
        hip_y = (row['y_23'] + row['y_24']) / 2
        hip_x = (row['x_23'] + row['x_24']) / 2
        prev_hip_y = (prev['y_23'] + prev['y_24']) / 2
        prev_hip_x = (prev['x_23'] + prev['x_24']) / 2
        
        # Velocity and acceleration (key for phase transitions)
        hip_y_velocity = (hip_y - prev_hip_y) * fps
        hip_x_velocity = (hip_x - prev_hip_x) * fps
        hip_vertical_acceleration = hip_y_velocity - prev_vals['hip_y'] if i > 0 else 0
        hip_horizontal_acceleration = hip_x_velocity - prev_vals['hip_x'] if i > 0 else 0
        
        # Clinical joint angles (primary discriminators for TUG phases)
        left_knee_angle = angle_between(lh, lk, la)
        right_knee_angle = angle_between(rh, rk, ra)
        left_hip_angle = angle_between(ls, lh, lk)
        right_hip_angle = angle_between(rs, rh, rk)
        
        # Ankle angles (important for gait analysis)
        left_ankle_angle = angle_between(lk, la, ltoe)
        right_ankle_angle = angle_between(rk, ra, rtoe)
        
        # Trunk kinematics (critical for sit-to-stand and stand-to-sit)
        shoulder_mid = (ls + rs) / 2
        hip_mid = (lh + rh) / 2
        torso_angle = angle_between(hip_mid + np.array([0, -1]), hip_mid, shoulder_mid)
        torso_angle_velocity = torso_angle - prev_vals['torso_angle']
        
        # Center of mass trajectory (SAIL research emphasis)
        com_x = (row['x_23'] + row['x_24'] + row['x_11'] + row['x_12']) / 4
        com_y = (row['y_23'] + row['y_24'] + row['y_11'] + row['y_12']) / 4
        com_x_velocity = (com_x - prev_vals['com_x']) * fps if i > 0 else 0
        com_y_velocity = (com_y - prev_vals['com_y']) * fps if i > 0 else 0
        com_acceleration = np.sqrt(com_x_velocity**2 + com_y_velocity**2)
        
        # Base of support and stability metrics
        base_of_support_width = abs(row['x_29'] - row['x_30'])  # Heel-to-heel distance
        base_of_support_length = abs(row['y_29'] - row['y_30'])  # Heel anterior-posterior separation
        
        # === 2. DEEP LEARNING INSPIRED KINEMATIC FEATURES ===
        
        # Multi-joint coordination patterns (learned by deep networks)
        knee_coordination = abs(left_knee_angle - right_knee_angle)  # Bilateral symmetry
        hip_coordination = abs(left_hip_angle - right_hip_angle)
        ankle_coordination = abs(left_ankle_angle - right_ankle_angle)
        
        # Sagittal plane kinematics (primary plane for TUG)
        knee_extension_power = left_knee_angle + right_knee_angle  # Higher when standing
        hip_extension_power = left_hip_angle + right_hip_angle     # Higher when standing
        ankle_power = left_ankle_angle + right_ankle_angle         # Plantar/dorsiflexion
        
        # Joint velocity patterns (temporal derivatives)
        left_knee_velocity = (left_knee_angle - prev.get('left_knee_angle', left_knee_angle)) * fps if i > 0 else 0
        right_knee_velocity = (right_knee_angle - prev.get('right_knee_angle', right_knee_angle)) * fps if i > 0 else 0
        
        # === 3. PHASE-SPECIFIC DISCRIMINATORS ===
        
        # Sit-to-Stand specific features (vertical momentum generation)
        vertical_momentum = abs(hip_y_velocity)
        sit_to_stand_power = (knee_extension_power * vertical_momentum) / (torso_angle + 1e-6)
        trunk_flexion_velocity = abs(torso_angle_velocity)
        
        # Walking phase features (rhythmic patterns)
        ankle_distance = abs(row['x_27'] - row['x_28'])
        heel_distance = abs(row['x_29'] - row['x_30'])
        toe_distance = abs(row['x_31'] - row['x_32'])
        
        # Step detection with clinical relevance
        lheel_y_velocity = (row['y_29'] - prev['y_29']) * fps
        rheel_y_velocity = (row['y_30'] - prev['y_30']) * fps
        lheel_x_velocity = (row['x_29'] - prev['x_29']) * fps
        rheel_x_velocity = (row['x_30'] - prev['x_30']) * fps
        
        # Gait asymmetry (clinical indicator)
        step_asymmetry = abs(lheel_y_velocity - rheel_y_velocity)
        stride_asymmetry = abs(lheel_x_velocity - rheel_x_velocity)
        
        # Forward progression velocity (key for walking phases)
        forward_momentum = (lheel_x_velocity + rheel_x_velocity) / 2
        forward_acceleration = forward_momentum - prev_vals.get('forward_momentum', 0) if i > 0 else 0
        
        # === 4. TURNING KINEMATICS (DEEP LEARNING FOCUS) ===
        
        # Axial rotation patterns (challenging for traditional methods)
        shoulder_angle = degrees(atan2(row['y_11'] - row['y_12'], row['x_11'] - row['x_12']))
        hip_rotation = degrees(atan2(row['y_23'] - row['y_24'], row['x_23'] - row['x_24']))
        
        # Angular velocities with wraparound handling
        shoulder_rotation_velocity = shoulder_angle - prev_vals['shoulder_angle']
        hip_rotation_velocity = hip_rotation - prev_vals['hip_rotation']
        
        # Handle angle wraparound (-180 to 180)
        if abs(shoulder_rotation_velocity) > 180:
            shoulder_rotation_velocity = shoulder_rotation_velocity - 360 * np.sign(shoulder_rotation_velocity)
        if abs(hip_rotation_velocity) > 180:
            hip_rotation_velocity = hip_rotation_velocity - 360 * np.sign(hip_rotation_velocity)
        
        # Axial dissociation (trunk rotation relative to pelvis)
        axial_dissociation = abs(shoulder_angle - hip_rotation)
        axial_dissociation_velocity = abs(shoulder_rotation_velocity - hip_rotation_velocity)
        
        # Head-trunk coordination for turning
        head_vec = nose - shoulder_mid
        head_yaw = degrees(atan2(head_vec[1], head_vec[0]))
        head_yaw_velocity = head_yaw - prev_vals['head_yaw']
        
        # Handle head yaw wraparound
        if abs(head_yaw_velocity) > 180:
            head_yaw_velocity = head_yaw_velocity - 360 * np.sign(head_yaw_velocity)
        
        # === 5. BALANCE AND STABILITY METRICS ===
        
        # Postural control (critical during transitions)
        mediolateral_sway = abs(com_x - hip_x)  # Lateral displacement from base
        anteroposterior_sway = abs(com_y - hip_y)  # Forward-backward displacement
        
        # Weight distribution
        weight_shift_x = abs(row['x_23'] - row['x_24'])  # Hip asymmetry
        weight_shift_y = abs(row['y_23'] - row['y_24'])  # Hip height difference
        
        # Dynamic stability margin
        stability_margin = base_of_support_width - abs(com_x - hip_x)
        
        # === 6. TEMPORAL PATTERN RECOGNITION ===
        
        # Step timing and rhythm (for walking phases)
        is_step = False
        if i > 2:
            prev_ankle_distance = abs(df.iloc[i-1]['x_27'] - df.iloc[i-1]['x_28'])
            if ankle_distance > prev_ankle_distance and ankle_distance > 0.12:
                is_step = True
                last_step_frame = row['frame']
                step_frames.append(row['frame'])
        
        time_since_last_step = (row['frame'] - last_step_frame) / fps
        recent_step_count = len([s for s in step_frames if row['frame'] - s < 2 * fps])
        step_frequency = recent_step_count / 2.0  # Steps per second
        
        # Cadence variability (clinical gait parameter)
        step_timing_variability = np.std([time_since_last_step] + 
            [((row['frame'] - s) / fps) for s in step_frames[-5:]])  # Last 5 steps
        
        # === 7. ENERGY AND POWER METRICS ===
        
        # Mechanical energy (important for efficiency assessment)
        kinetic_energy = (hip_x_velocity**2 + hip_y_velocity**2) / 2
        potential_energy = hip_y  # Relative to ground
        total_mechanical_energy = kinetic_energy + potential_energy
        
        # Rotational energy
        rotational_energy = (shoulder_rotation_velocity**2 + hip_rotation_velocity**2) / 2
        
        # Power generation/absorption
        concentric_power = max(0, hip_y_velocity * vertical_momentum)  # Positive work
        eccentric_power = max(0, -hip_y_velocity * vertical_momentum)  # Negative work
        
        # === 8. TASK PROGRESSION AND CONTEXT ===
        
        # Temporal context within TUG task
        task_progression = row['frame'] / len(df) if len(df) > 1 else 0
        
        # Phase likelihood based on temporal progression (refined estimates)
        # These act as weak priors for the classifier
        progression_velocity = abs(task_progression - 0.5) * 2  # 0 at middle, 1 at extremes
        
        sit_to_stand_likelihood = np.exp(-((task_progression - 0.05)**2) / 0.01)  # Peak at 5%
        walk_from_likelihood = np.exp(-((task_progression - 0.25)**2) / 0.02)     # Peak at 25%
        turn_first_likelihood = np.exp(-((task_progression - 0.45)**2) / 0.01)    # Peak at 45%
        walk_to_likelihood = np.exp(-((task_progression - 0.65)**2) / 0.02)       # Peak at 65%
        turn_second_likelihood = np.exp(-((task_progression - 0.8)**2) / 0.01)    # Peak at 80%
        stand_to_sit_likelihood = np.exp(-((task_progression - 0.95)**2) / 0.01)  # Peak at 95%
        
        # === 9. DERIVED AND COMPOSITE FEATURES ===
        
        # Stride and gait variability metrics
        stride_length_variation = np.std([ankle_distance] + 
            [abs(df.iloc[j]['x_27'] - df.iloc[j]['x_28']) for j in range(max(0, i-10), i)])
        
        # Torso coordination metrics
        torso_twist = abs(shoulder_angle - hip_rotation)
        torso_twist_velocity = abs(shoulder_rotation_velocity - hip_rotation_velocity)
        
        # Direction change and turning metrics
        forward_direction = degrees(atan2(com_y_velocity, com_x_velocity)) if com_x_velocity != 0 else 0
        prev_forward_direction = degrees(atan2(prev_vals.get('com_y_velocity', 0), prev_vals.get('com_x_velocity', 0))) if prev_vals.get('com_x_velocity', 0) != 0 else 0
        direction_change_magnitude = abs(forward_direction - prev_forward_direction)
        if direction_change_magnitude > 180:
            direction_change_magnitude = 360 - direction_change_magnitude
        
        # Comprehensive turn preparation score
        turn_preparation_score = (
            abs(shoulder_rotation_velocity) * 0.3 +
            abs(hip_rotation_velocity) * 0.3 +
            abs(head_yaw_velocity) * 0.2 +
            axial_dissociation_velocity * 0.2
        )
        
        # Postural and balance metrics
        body_sway = np.std([shoulder_mid[0], hip_mid[0]]) if i > 5 else 0  # Lateral sway
        postural_control = 1.0 / (com_acceleration + 1e-6)  # Inverse of acceleration
        
        # Movement complexity combining all domains
        movement_complexity = (
            abs(hip_y_velocity) * 0.2 +      # Vertical movement
            abs(forward_momentum) * 0.2 +     # Forward movement  
            turn_preparation_score * 0.2 +    # Rotational movement
            vertical_momentum * 0.2 +         # Overall activity
            axial_dissociation * 0.2          # Coordination complexity
        )
        
        # Total body momentum
        total_body_momentum = np.sqrt(com_x_velocity**2 + com_y_velocity**2)
        
        # Update previous values for next iteration
        prev_vals.update({
            'hip_y': hip_y_velocity, 'hip_x': hip_x_velocity,
            'shoulder_angle': shoulder_angle, 'torso_angle': torso_angle,
            'head_yaw': head_yaw, 'com_x': com_x, 'com_y': com_y,
            'com_x_velocity': com_x_velocity, 'com_y_velocity': com_y_velocity,
            'forward_momentum': forward_momentum
        })
        
        # === COMPILE COMPREHENSIVE FEATURE VECTOR ===
        features.append({
            'frame': row['frame'],
            
            # === CLINICAL GAIT PARAMETERS ===
            'hip_height': hip_y,
            'hip_y_velocity': hip_y_velocity,
            'hip_x_velocity': hip_x_velocity,
            'hip_vertical_acceleration': hip_vertical_acceleration,
            'hip_horizontal_acceleration': hip_horizontal_acceleration,
            'com_x': com_x,
            'com_y': com_y,
            'com_x_velocity': com_x_velocity,
            'com_y_velocity': com_y_velocity,
            'com_acceleration': com_acceleration,
            
            # === JOINT KINEMATICS ===
            'left_knee_angle': left_knee_angle,
            'right_knee_angle': right_knee_angle,
            'left_hip_angle': left_hip_angle,
            'right_hip_angle': right_hip_angle,
            'left_ankle_angle': left_ankle_angle,
            'right_ankle_angle': right_ankle_angle,
            'torso_angle': torso_angle,
            'torso_angle_velocity': torso_angle_velocity,
            
            # === JOINT COORDINATION ===
            'knee_coordination': knee_coordination,
            'hip_coordination': hip_coordination,
            'ankle_coordination': ankle_coordination,
            'knee_extension_power': knee_extension_power,
            'hip_extension_power': hip_extension_power,
            'ankle_power': ankle_power,
            'left_knee_velocity': left_knee_velocity,
            'right_knee_velocity': right_knee_velocity,
            
            # === PHASE-SPECIFIC FEATURES ===
            'vertical_momentum': vertical_momentum,
            'sit_to_stand_power': sit_to_stand_power,
            'trunk_flexion_velocity': trunk_flexion_velocity,
            'forward_momentum': forward_momentum,
            'forward_acceleration': forward_acceleration,
            
            # === GAIT ANALYSIS ===
            'ankle_distance': ankle_distance,
            'heel_distance': heel_distance,
            'toe_distance': toe_distance,
            'base_of_support_width': base_of_support_width,
            'base_of_support_length': base_of_support_length,
            'lheel_y_velocity': lheel_y_velocity,
            'rheel_y_velocity': rheel_y_velocity,
            'lheel_x_velocity': lheel_x_velocity,
            'rheel_x_velocity': rheel_x_velocity,
            'step_asymmetry': step_asymmetry,
            'stride_asymmetry': stride_asymmetry,
            'time_since_last_step': time_since_last_step,
            'step_frequency': step_frequency,
            'step_timing_variability': step_timing_variability,
            'stride_length_variation': stride_length_variation,
            
            # === TURNING KINEMATICS ===
            'shoulder_angle': shoulder_angle,
            'shoulder_rotation_velocity': shoulder_rotation_velocity,
            'hip_rotation': hip_rotation,
            'hip_rotation_velocity': hip_rotation_velocity,
            'axial_dissociation': axial_dissociation,
            'axial_dissociation_velocity': axial_dissociation_velocity,
            'head_yaw': head_yaw,
            'head_yaw_velocity': head_yaw_velocity,
            'torso_twist': torso_twist,
            'torso_twist_velocity': torso_twist_velocity,
            'direction_change_magnitude': direction_change_magnitude,
            'turn_preparation_score': turn_preparation_score,
            
            # === BALANCE AND STABILITY ===
            'mediolateral_sway': mediolateral_sway,
            'anteroposterior_sway': anteroposterior_sway,
            'weight_shift_x': weight_shift_x,
            'weight_shift_y': weight_shift_y,
            'stability_margin': stability_margin,
            'body_sway': body_sway,
            'postural_control': postural_control,
            'movement_complexity': movement_complexity,
            
            # === ENERGY AND POWER ===
            'kinetic_energy': kinetic_energy,
            'potential_energy': potential_energy,
            'total_mechanical_energy': total_mechanical_energy,
            'rotational_energy': rotational_energy,
            'concentric_power': concentric_power,
            'eccentric_power': eccentric_power,
            'total_body_momentum': total_body_momentum,
            
            # === TEMPORAL CONTEXT ===
            'task_progression': task_progression,
            'progression_velocity': progression_velocity,
            'sit_to_stand_likelihood': sit_to_stand_likelihood,
            'walk_from_likelihood': walk_from_likelihood,
            'turn_first_likelihood': turn_first_likelihood,
            'walk_to_likelihood': walk_to_likelihood,
            'turn_second_likelihood': turn_second_likelihood,
            'stand_to_sit_likelihood': stand_to_sit_likelihood
        })
    
    df_features = pd.DataFrame(features)
    
    # === Add temporal smoothing and multi-scale features ===
    print(f"🔧 Adding temporal features...")
    
    # Multi-scale rolling windows for temporal patterns
    for window in [5, 10, 15, 30]:  # Different time scales (5-30 frames)
        # Movement smoothing
        df_features[f'hip_y_velocity_smooth_{window}'] = df_features['hip_y_velocity'].rolling(window, center=True).mean()
        df_features[f'turn_score_smooth_{window}'] = df_features['turn_preparation_score'].rolling(window, center=True).mean()
        df_features[f'movement_complexity_smooth_{window}'] = df_features['movement_complexity'].rolling(window, center=True).mean()
        
        # Variation detection
        df_features[f'hip_height_std_{window}'] = df_features['hip_height'].rolling(window, center=True).std()
        df_features[f'forward_momentum_std_{window}'] = df_features['forward_momentum'].rolling(window, center=True).std()
        df_features[f'rotation_variation_{window}'] = df_features['shoulder_rotation_velocity'].rolling(window, center=True).std()
    
    # Transition detection (sudden changes)
    df_features['hip_y_acceleration'] = df_features['hip_y_velocity'].diff()
    df_features['movement_jerk'] = df_features['movement_complexity'].diff().abs()
    df_features['rotation_acceleration'] = df_features['shoulder_rotation_velocity'].diff().abs()
    
    # Phase consistency (how long in current movement pattern)
    df_features['sustained_vertical_movement'] = (df_features['vertical_momentum'] > df_features['vertical_momentum'].quantile(0.7)).astype(int)
    df_features['sustained_forward_movement'] = (df_features['forward_momentum'].abs() > df_features['forward_momentum'].abs().quantile(0.7)).astype(int)
    df_features['sustained_turning'] = (df_features['turn_preparation_score'] > df_features['turn_preparation_score'].quantile(0.7)).astype(int)
    
    return df_features

# === Main Processing Loop ===
print(f"🎯 TUG Phase Detection Feature Extraction")
print(f"📁 Input directory: {input_dir}")
print(f"📁 Output directory: {output_dir}")

# Process all landmark CSV files
landmark_files = [f for f in os.listdir(input_dir) if f.endswith("_landmarks.csv")]
total_files = len(landmark_files)

print(f"🎬 Found {total_files} landmark files to process")

for file_idx, filename in enumerate(landmark_files, 1):
    print(f"\n{'='*60}")
    print(f"📊 Processing {file_idx}/{total_files}: {filename}")
    print(f"{'='*60}")
    
    try:
        # Load landmarks
        input_path = os.path.join(input_dir, filename)
        df = pd.read_csv(input_path)
        
        # Fill missing values
        df = df.ffill().bfill().fillna(0)
        
        print(f"📈 Loaded {len(df)} frames with {len(df.columns)} landmark features")
        
        # Extract TUG-specific features
        df_features = extract_tug_features(df)
        
        # Save engineered features
        base_name = filename.replace("_landmarks.csv", "")
        output_path = os.path.join(output_dir, f"{base_name}_features.csv")
        
        # Ensure no empty values - replace any remaining NaN with 0
        df_features = df_features.fillna(0)
        
        df_features.to_csv(output_path, index=False)
        
        print(f"✅ Extracted {len(df_features)} feature vectors with {len(df_features.columns)-1} features")
        print(f"📁 Saved to: {output_path}")
        
        # Feature summary
        key_features = ['hip_y_velocity', 'forward_momentum', 'turn_preparation_score', 'movement_complexity']
        print(f"📊 Key feature ranges:")
        for feature in key_features:
            if feature in df_features.columns:
                min_val, max_val = df_features[feature].min(), df_features[feature].max()
                print(f"   {feature}: {min_val:.3f} to {max_val:.3f}")
        
    except Exception as e:
        print(f"❌ Error processing {filename}: {e}")
        import traceback
        traceback.print_exc()

print(f"\n🎉 SAIL-Inspired TUG Feature Extraction Complete!")
print(f"📊 Advanced biomechanical features extracted based on deep learning research:")
print(f"   - Clinical Gait Parameters: Spatial-temporal metrics, joint kinematics")
print(f"   - Multi-Joint Coordination: Bilateral symmetry, power generation patterns")
print(f"   - Phase-Specific Discriminators: Sit-to-stand, walking, turning transitions")
print(f"   - Balance & Stability: Postural control, center of mass dynamics")
print(f"   - Temporal Patterns: Multi-scale smoothing, transition detection")
print(f"   - Energy & Power: Mechanical energy, concentric/eccentric work")
print(f"📈 Total features per frame: ~90 (optimized for TUG phase classification)")
print(f"📁 All feature files saved to: {output_dir}")
print(f"\n🔬 Features designed for 6-phase TUG classification:")
print(f"   1. Sit-To-Stand: Vertical momentum, joint extension, trunk dynamics")
print(f"   2. Walk-From-Chair: Gait initiation, step patterns, forward progression")
print(f"   3. Turn-First: Axial rotation, direction change, balance adaptation")
print(f"   4. Walk-To-Chair: Steady gait, spatial-temporal consistency")
print(f"   5. Turn-Second: Deceleration preparation, rotational coordination")
print(f"   6. Stand-To-Sit: Controlled descent, eccentric muscle work, stability")
