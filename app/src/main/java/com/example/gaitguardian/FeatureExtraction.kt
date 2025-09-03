package com.example.gaitguardian

import android.util.Log
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.*

/**
 * Direct Python-to-Kotlin port of extract_tug_features() function
 * This is a line-by-line translation of the Python featureextraction.py
 */
class FeatureExtraction {
    
    companion object {
        private const val TAG = "FeatureExtraction"
    }
    
    // Python equivalent: def angle_between(a, b, c):
    private fun angleBetween(a: FloatArray, b: FloatArray, c: FloatArray): Float {
        // ba = a - b
        val baX = a[0] - b[0]
        val baY = a[1] - b[1]
        
        // bc = c - b  
        val bcX = c[0] - b[0]
        val bcY = c[1] - b[1]
        
        // norm(ba) * norm(bc)
        val normBa = sqrt(baX * baX + baY * baY)
        val normBc = sqrt(bcX * bcX + bcY * bcY)
        
        if (normBa == 0f || normBc == 0f) return 0f
        
        // np.dot(ba, bc) / (norm(ba) * norm(bc) + 1e-6)
        val dotProduct = baX * bcX + baY * bcY
        val cosAngle = (dotProduct / (normBa * normBc + 1e-6f)).coerceIn(-1f, 1f)
        
        // degrees(acos(cos_angle))
        return acos(cosAngle) * 180f / PI.toFloat()
    }
    
    /**
     * Direct port of Python extract_tug_features(df) function
     * Input: List of pose landmarks for each frame (639 frames)
     * Output: List of feature vectors, one per frame (639 vectors × ~80 features each)
     */
    fun extractTugFeatures(landmarksList: List<List<NormalizedLandmark>>, fps: Float): List<Map<String, Float>> {
        Log.d(TAG, "Direct Python port: extracting TUG features from ${landmarksList.size} frames")
        
        if (landmarksList.isEmpty()) {
            Log.w(TAG, "Empty landmarks list, returning empty feature list")
            return emptyList()
        }
        
        val features = mutableListOf<Map<String, Float>>()
        
        // Python: prev_vals = {'hip_y': 0, 'hip_x': 0, ...}
        val prevVals = mutableMapOf<String, Float>(
            "hip_y" to 0f, "hip_x" to 0f, "shoulder_angle" to 0f, "torso_angle" to 0f,
            "head_yaw" to 0f, "com_x" to 0f, "com_y" to 0f, "com_x_velocity" to 0f, "com_y_velocity" to 0f,
            "forward_momentum" to 0f, "hip_rotation" to 0f, "left_knee_angle" to 0f, "right_knee_angle" to 0f
        )
        
        // Python: last_step_frame = 0, step_frames = []
        var lastStepFrame = 0
        val stepFrames = mutableListOf<Int>()
        
        Log.d(TAG, "Processing ${landmarksList.size} frames...")
        
        // Python: for i in range(len(df)):
        for (i in landmarksList.indices) {
            val landmarks = landmarksList[i]
            if (landmarks.size < 33) continue
            
            val prevLandmarks = if (i > 0 && landmarksList[i - 1].size >= 33) landmarksList[i - 1] else landmarks
            
            // Python helper functions - def point(coord, idx):
            fun point(idx: Int): FloatArray = floatArrayOf(landmarks[idx].x(), landmarks[idx].y())
            fun prevPoint(idx: Int): FloatArray = floatArrayOf(prevLandmarks[idx].x(), prevLandmarks[idx].y())
            
            // Python: Key Body Points
            val lh = point(23)  // left hip
            val rh = point(24)  // right hip  
            val lk = point(25)  // left knee
            val rk = point(26)  // right knee
            val la = point(27)  // left ankle
            val ra = point(28)  // right ankle
            val ls = point(11)  // left shoulder
            val rs = point(12)  // right shoulder
            val lheel = point(29) // left heel
            val rheel = point(30) // right heel
            val ltoe = point(31) // left toe
            val rtoe = point(32) // right toe
            val nose = point(0) // nose
            
            // Previous frame points
            val prevLh = prevPoint(23)
            val prevRh = prevPoint(24)
            
            // Python: hip_y = (row['y_23'] + row['y_24']) / 2
            val hipY = (lh[1] + rh[1]) / 2f
            val hipX = (lh[0] + rh[0]) / 2f
            
            // Python: prev_hip_y = (prev['y_23'] + prev['y_24']) / 2
            val prevHipY = (prevLh[1] + prevRh[1]) / 2f
            val prevHipX = (prevLh[0] + prevRh[0]) / 2f
            
            // Python: hip_y_velocity = (hip_y - prev_hip_y) * fps
            val hipYVelocity = (hipY - prevHipY) * fps
            val hipXVelocity = (hipX - prevHipX) * fps
            
            // Python: hip_vertical_acceleration = hip_y_velocity - prev_vals['hip_y'] if i > 0 else 0
            val hipVerticalAcceleration = if (i > 0) hipYVelocity - prevVals["hip_y"]!! else 0f
            val hipHorizontalAcceleration = if (i > 0) hipXVelocity - prevVals["hip_x"]!! else 0f
            
            // Python: Clinical joint angles
            val leftKneeAngle = angleBetween(lh, lk, la)
            val rightKneeAngle = angleBetween(rh, rk, ra)
            val leftHipAngle = angleBetween(ls, lh, lk)
            val rightHipAngle = angleBetween(rs, rh, rk)
            val leftAnkleAngle = angleBetween(lk, la, ltoe)
            val rightAnkleAngle = angleBetween(rk, ra, rtoe)
            
            // Python: Trunk kinematics
            val shoulderMid = floatArrayOf((ls[0] + rs[0]) / 2f, (ls[1] + rs[1]) / 2f)
            val hipMid = floatArrayOf((lh[0] + rh[0]) / 2f, (lh[1] + rh[1]) / 2f)
            
            // Python: torso_angle = angle_between(hip_mid + np.array([0, -1]), hip_mid, shoulder_mid)
            val torsoAngle = angleBetween(
                floatArrayOf(hipMid[0], hipMid[1] - 1f), 
                hipMid, 
                shoulderMid
            )
            val torsoAngleVelocity = torsoAngle - prevVals["torso_angle"]!!
            
            // Python: Center of mass trajectory
            val comX = (lh[0] + rh[0] + ls[0] + rs[0]) / 4f
            val comY = (lh[1] + rh[1] + ls[1] + rs[1]) / 4f
            val comXVelocity = if (i > 0) (comX - prevVals["com_x"]!!) * fps else 0f
            val comYVelocity = if (i > 0) (comY - prevVals["com_y"]!!) * fps else 0f
            val comAcceleration = sqrt(comXVelocity * comXVelocity + comYVelocity * comYVelocity)
            
            // Python: Base of support
            val baseOfSupportWidth = abs(lheel[0] - rheel[0])
            val baseOfSupportLength = abs(lheel[1] - rheel[1])
            
            // Python: Multi-joint coordination patterns
            val kneeCoordination = abs(leftKneeAngle - rightKneeAngle)
            val hipCoordination = abs(leftHipAngle - rightHipAngle)
            val ankleCoordination = abs(leftAnkleAngle - rightAnkleAngle)
            
            // Python: Sagittal plane kinematics
            val kneeExtensionPower = leftKneeAngle + rightKneeAngle
            val hipExtensionPower = leftHipAngle + rightHipAngle
            val anklePower = leftAnkleAngle + rightAnkleAngle
            
            // Python: Joint velocity patterns
            val leftKneeVelocity = if (i > 0) (leftKneeAngle - prevVals["left_knee_angle"]!!) * fps else 0f
            val rightKneeVelocity = if (i > 0) (rightKneeAngle - prevVals["right_knee_angle"]!!) * fps else 0f
            
            // Python: Phase-specific features
            val verticalMomentum = abs(hipYVelocity)
            val sitToStandPower = (kneeExtensionPower * verticalMomentum) / (torsoAngle + 1e-6f)
            val trunkFlexionVelocity = abs(torsoAngleVelocity)
            
            // Python: Walking phase features
            val ankleDistance = abs(la[0] - ra[0])
            val heelDistance = abs(lheel[0] - rheel[0])
            val toeDistance = abs(ltoe[0] - rtoe[0])
            
            // Python: Step detection velocities
            val prevLheel = prevPoint(29)
            val prevRheel = prevPoint(30)
            val lheelYVelocity = (lheel[1] - prevLheel[1]) * fps
            val rheelYVelocity = (rheel[1] - prevRheel[1]) * fps
            val lheelXVelocity = (lheel[0] - prevLheel[0]) * fps
            val rheelXVelocity = (rheel[0] - prevRheel[0]) * fps
            
            // Python: Gait asymmetry
            val stepAsymmetry = abs(lheelYVelocity - rheelYVelocity)
            val strideAsymmetry = abs(lheelXVelocity - rheelXVelocity)
            
            // Python: Forward progression
            val forwardMomentum = (lheelXVelocity + rheelXVelocity) / 2f
            val forwardAcceleration = if (i > 0) forwardMomentum - prevVals["forward_momentum"]!! else 0f
            
            // Python: Turning kinematics - degrees(atan2(y, x))
            val shoulderAngle = atan2(ls[1] - rs[1], ls[0] - rs[0]) * 180f / PI.toFloat()
            val hipRotation = atan2(lh[1] - rh[1], lh[0] - rh[0]) * 180f / PI.toFloat()
            
            // Python: Angular velocities with wraparound
            var shoulderRotationVelocity = shoulderAngle - prevVals["shoulder_angle"]!!
            var hipRotationVelocity = hipRotation - prevVals["hip_rotation"]!!
            
            // Python: Handle angle wraparound (-180 to 180)
            if (abs(shoulderRotationVelocity) > 180f) {
                shoulderRotationVelocity = shoulderRotationVelocity - 360f * sign(shoulderRotationVelocity)
            }
            if (abs(hipRotationVelocity) > 180f) {
                hipRotationVelocity = hipRotationVelocity - 360f * sign(hipRotationVelocity)
            }
            
            // Python: Axial dissociation
            val axialDissociation = abs(shoulderAngle - hipRotation)
            val axialDissociationVelocity = abs(shoulderRotationVelocity - hipRotationVelocity)
            
            // Python: Head-trunk coordination
            val headVec = floatArrayOf(nose[0] - shoulderMid[0], nose[1] - shoulderMid[1])
            val headYaw = atan2(headVec[1], headVec[0]) * 180f / PI.toFloat()
            var headYawVelocity = headYaw - prevVals["head_yaw"]!!
            
            // Python: Handle head yaw wraparound
            if (abs(headYawVelocity) > 180f) {
                headYawVelocity = headYawVelocity - 360f * sign(headYawVelocity)
            }
            
            // Python: Balance and stability metrics
            val mediolateralSway = abs(comX - hipX)
            val anteroposteriorSway = abs(comY - hipY)
            val weightShiftX = abs(lh[0] - rh[0])
            val weightShiftY = abs(lh[1] - rh[1])
            val stabilityMargin = baseOfSupportWidth - abs(comX - hipX)
            
            // Python: Step detection logic
            var isStep = false
            if (i > 2) {
                val prevAnkleDistance = abs(landmarksList[i-1][27].x() - landmarksList[i-1][28].x())
                if (ankleDistance > prevAnkleDistance && ankleDistance > 0.12f) {
                    isStep = true
                    lastStepFrame = i
                    stepFrames.add(i)
                }
            }
            
            val timeSinceLastStep = (i - lastStepFrame).toFloat() / fps
            val recentStepCount = stepFrames.count { i - it < 2 * fps }
            val stepFrequency = recentStepCount / 2f
            
            // Python: Cadence variability - need at least some steps for std calculation
            val recentStepTimes = stepFrames.takeLast(5).map { (i - it).toFloat() / fps }
            val stepTimingVariability = if (recentStepTimes.size > 1) {
                val allTimes = listOf(timeSinceLastStep) + recentStepTimes
                val mean = allTimes.average().toFloat()
                val variance = allTimes.map { (it - mean) * (it - mean) }.average().toFloat()
                sqrt(variance)
            } else 0f
            
            // Python: Energy and power metrics
            val kineticEnergy = (hipXVelocity * hipXVelocity + hipYVelocity * hipYVelocity) / 2f
            val potentialEnergy = hipY
            val totalMechanicalEnergy = kineticEnergy + potentialEnergy
            val rotationalEnergy = (shoulderRotationVelocity * shoulderRotationVelocity + 
                                   hipRotationVelocity * hipRotationVelocity) / 2f
            val concentricPower = maxOf(0f, hipYVelocity * verticalMomentum)
            val eccentricPower = maxOf(0f, -hipYVelocity * verticalMomentum)
            
            // Python: Task progression
            val taskProgression = i.toFloat() / landmarksList.size.toFloat()
            val progressionVelocity = abs(taskProgression - 0.5f) * 2f
            
            // Python: Phase likelihoods (Gaussian peaks)
            val sitToStandLikelihood = exp(-((taskProgression - 0.05f) * (taskProgression - 0.05f)) / 0.01f)
            val walkFromLikelihood = exp(-((taskProgression - 0.25f) * (taskProgression - 0.25f)) / 0.02f)
            val turnFirstLikelihood = exp(-((taskProgression - 0.45f) * (taskProgression - 0.45f)) / 0.01f)
            val walkToLikelihood = exp(-((taskProgression - 0.65f) * (taskProgression - 0.65f)) / 0.02f)
            val turnSecondLikelihood = exp(-((taskProgression - 0.8f) * (taskProgression - 0.8f)) / 0.01f)
            val standToSitLikelihood = exp(-((taskProgression - 0.95f) * (taskProgression - 0.95f)) / 0.01f)
            
            // Python: Derived features - stride length variation
            val recentAnkleDistances = (maxOf(0, i - 10) until i).mapNotNull { j ->
                val lmarks = landmarksList[j]
                if (lmarks.size >= 33) abs(lmarks[27].x() - lmarks[28].x()) else null
            }
            val strideLength = if (recentAnkleDistances.isNotEmpty()) {
                val allDistances = recentAnkleDistances + ankleDistance
                val mean = allDistances.average().toFloat()
                val variance = allDistances.map { (it - mean) * (it - mean) }.average().toFloat()
                sqrt(variance)
            } else 0f
            
            // Python: More derived features
            val torsoTwist = abs(shoulderAngle - hipRotation)
            val torsoTwistVelocity = abs(shoulderRotationVelocity - hipRotationVelocity)
            
            // Python: Direction change
            val forwardDirection = if (comXVelocity != 0f) atan2(comYVelocity, comXVelocity) * 180f / PI.toFloat() else 0f
            val prevForwardDirection = if (prevVals["com_x_velocity"]!! != 0f) 
                atan2(prevVals["com_y_velocity"]!!, prevVals["com_x_velocity"]!!) * 180f / PI.toFloat() else 0f
            var directionChangeMagnitude = abs(forwardDirection - prevForwardDirection)
            if (directionChangeMagnitude > 180f) {
                directionChangeMagnitude = 360f - directionChangeMagnitude
            }
            
            // Python: Turn preparation score
            val turnPreparationScore = (
                abs(shoulderRotationVelocity) * 0.3f +
                abs(hipRotationVelocity) * 0.3f +
                abs(headYawVelocity) * 0.2f +
                axialDissociationVelocity * 0.2f
            )
            
            // Python: Body sway - std of lateral positions
            val bodySway = if (i > 5) {
                val recentShoulderX = (maxOf(0, i - 5) until i).mapNotNull { j ->
                    val lmarks = landmarksList[j]
                    if (lmarks.size >= 33) (lmarks[11].x() + lmarks[12].x()) / 2f else null
                }
                val recentHipX = (maxOf(0, i - 5) until i).mapNotNull { j ->
                    val lmarks = landmarksList[j]
                    if (lmarks.size >= 33) (lmarks[23].x() + lmarks[24].x()) / 2f else null
                }
                val positions = recentShoulderX + recentHipX
                if (positions.isNotEmpty()) {
                    val mean = positions.average().toFloat()
                    val variance = positions.map { (it - mean) * (it - mean) }.average().toFloat()
                    sqrt(variance)
                } else 0f
            } else 0f
            
            val posturalControl = 1f / (comAcceleration + 1e-6f)
            
            // Python: Movement complexity
            val movementComplexity = (
                abs(hipYVelocity) * 0.2f +
                abs(forwardMomentum) * 0.2f +
                turnPreparationScore * 0.2f +
                verticalMomentum * 0.2f +
                axialDissociation * 0.2f
            )
            
            val totalBodyMomentum = sqrt(comXVelocity * comXVelocity + comYVelocity * comYVelocity)
            
            // Debug frame 630 specifically - show ALL feature values for comparison
            if (i == 630) {
                Log.d(TAG, "=== FRAME 630 DEBUG - COORDINATE ANALYSIS ===")
                Log.d(TAG, "Raw coordinates - hipY: $hipY, prevHipY: $prevHipY")
                Log.d(TAG, "Raw difference - (hipY - prevHipY): ${hipY - prevHipY}")
                Log.d(TAG, "fps: $fps")
                Log.d(TAG, "velocity calculation: (${hipY - prevHipY}) * $fps = $hipYVelocity")
                Log.d(TAG, "Expected Python hip_y_velocity: 0.0004157423973083496")
                Log.d(TAG, "Our hip_y_velocity: $hipYVelocity")
                Log.d(TAG, "Ratio (ours/python): ${hipYVelocity / 0.0004157423973083496}")
                
                Log.d(TAG, "=== COM VELOCITY DEBUG ===")
                Log.d(TAG, "Raw comX: $comX, prevComX: ${prevVals["com_x"]}")
                Log.d(TAG, "Raw comY: $comY, prevComY: ${prevVals["com_y"]}")
                Log.d(TAG, "Raw COM X diff: ${comX - prevVals["com_x"]!!}")
                Log.d(TAG, "Raw COM Y diff: ${comY - prevVals["com_y"]!!}")
                Log.d(TAG, "COM X velocity: $comXVelocity (expected: 0.03682108595967293)")
                Log.d(TAG, "COM Y velocity: $comYVelocity (expected: -0.00408366322517395)")
                
                Log.d(TAG, "=== ALL FEATURES ===")
                Log.d(TAG, "Frame: $i")
                
                // Clinical gait parameters
                Log.d(TAG, "hip_height: $hipY")
                Log.d(TAG, "hip_y_velocity: $hipYVelocity")
                Log.d(TAG, "hip_x_velocity: $hipXVelocity")
                Log.d(TAG, "hip_vertical_acceleration: $hipVerticalAcceleration")
                Log.d(TAG, "hip_horizontal_acceleration: $hipHorizontalAcceleration")
                Log.d(TAG, "com_x: $comX, com_y: $comY")
                Log.d(TAG, "com_x_velocity: $comXVelocity, com_y_velocity: $comYVelocity")
                Log.d(TAG, "com_acceleration: $comAcceleration")
                
                // Joint kinematics
                Log.d(TAG, "left_knee_angle: $leftKneeAngle, right_knee_angle: $rightKneeAngle")
                Log.d(TAG, "left_hip_angle: $leftHipAngle, right_hip_angle: $rightHipAngle")
                Log.d(TAG, "left_ankle_angle: $leftAnkleAngle, right_ankle_angle: $rightAnkleAngle")
                Log.d(TAG, "torso_angle: $torsoAngle, torso_angle_velocity: $torsoAngleVelocity")
                
                // Joint coordination
                Log.d(TAG, "knee_coordination: $kneeCoordination, hip_coordination: $hipCoordination")
                Log.d(TAG, "ankle_coordination: $ankleCoordination")
                Log.d(TAG, "knee_extension_power: $kneeExtensionPower, hip_extension_power: $hipExtensionPower")
                Log.d(TAG, "ankle_power: $anklePower")
                Log.d(TAG, "left_knee_velocity: $leftKneeVelocity, right_knee_velocity: $rightKneeVelocity")
                
                // Phase-specific features
                Log.d(TAG, "vertical_momentum: $verticalMomentum")
                Log.d(TAG, "sit_to_stand_power: $sitToStandPower")
                Log.d(TAG, "trunk_flexion_velocity: $trunkFlexionVelocity")
                Log.d(TAG, "forward_momentum: $forwardMomentum, forward_acceleration: $forwardAcceleration")
                
                // Gait analysis
                Log.d(TAG, "ankle_distance: $ankleDistance, heel_distance: $heelDistance, toe_distance: $toeDistance")
                Log.d(TAG, "base_of_support_width: $baseOfSupportWidth, base_of_support_length: $baseOfSupportLength")
                Log.d(TAG, "lheel_y_velocity: $lheelYVelocity, rheel_y_velocity: $rheelYVelocity")
                Log.d(TAG, "lheel_x_velocity: $lheelXVelocity, rheel_x_velocity: $rheelXVelocity")
                Log.d(TAG, "step_asymmetry: $stepAsymmetry, stride_asymmetry: $strideAsymmetry")
                Log.d(TAG, "time_since_last_step: $timeSinceLastStep, step_frequency: $stepFrequency")
                Log.d(TAG, "step_timing_variability: $stepTimingVariability, stride_length_variation: $strideLength")
                
                // Turning kinematics
                Log.d(TAG, "shoulder_angle: $shoulderAngle, shoulder_rotation_velocity: $shoulderRotationVelocity")
                Log.d(TAG, "hip_rotation: $hipRotation, hip_rotation_velocity: $hipRotationVelocity")
                Log.d(TAG, "axial_dissociation: $axialDissociation, axial_dissociation_velocity: $axialDissociationVelocity")
                Log.d(TAG, "head_yaw: $headYaw, head_yaw_velocity: $headYawVelocity")
                Log.d(TAG, "torso_twist: $torsoTwist, torso_twist_velocity: $torsoTwistVelocity")
                Log.d(TAG, "direction_change_magnitude: $directionChangeMagnitude")
                Log.d(TAG, "turn_preparation_score: $turnPreparationScore")
                
                // Balance and stability
                Log.d(TAG, "mediolateral_sway: $mediolateralSway, anteroposterior_sway: $anteroposteriorSway")
                Log.d(TAG, "weight_shift_x: $weightShiftX, weight_shift_y: $weightShiftY")
                Log.d(TAG, "stability_margin: $stabilityMargin, body_sway: $bodySway")
                Log.d(TAG, "postural_control: $posturalControl, movement_complexity: $movementComplexity")
                
                // Energy and power
                Log.d(TAG, "kinetic_energy: $kineticEnergy, potential_energy: $potentialEnergy")
                Log.d(TAG, "total_mechanical_energy: $totalMechanicalEnergy, rotational_energy: $rotationalEnergy")
                Log.d(TAG, "concentric_power: $concentricPower, eccentric_power: $eccentricPower")
                Log.d(TAG, "total_body_momentum: $totalBodyMomentum")
                
                // Temporal context
                Log.d(TAG, "task_progression: $taskProgression, progression_velocity: $progressionVelocity")
                Log.d(TAG, "sit_to_stand_likelihood: $sitToStandLikelihood")
                Log.d(TAG, "walk_from_likelihood: $walkFromLikelihood, turn_first_likelihood: $turnFirstLikelihood")
                Log.d(TAG, "walk_to_likelihood: $walkToLikelihood, turn_second_likelihood: $turnSecondLikelihood")
                Log.d(TAG, "stand_to_sit_likelihood: $standToSitLikelihood")
                
                Log.d(TAG, "=== END FRAME 630 DEBUG ===")
            }
            
            // Update prevVals for next iteration (this was missing!)
            prevVals["hip_x"] = hipX
            prevVals["hip_y"] = hipY
            prevVals["com_x"] = comX
            prevVals["com_y"] = comY
            prevVals["torso_angle"] = torsoAngle
            prevVals["shoulder_angle"] = shoulderAngle
            prevVals["hip_rotation"] = hipRotation
            prevVals["axial_dissociation"] = axialDissociation
            prevVals["head_yaw"] = headYaw
            prevVals["torso_twist"] = torsoTwist
            prevVals["left_knee_angle"] = leftKneeAngle
            prevVals["right_knee_angle"] = rightKneeAngle
            prevVals["left_heel_x"] = lheel[0]
            prevVals["left_heel_y"] = lheel[1]
            prevVals["right_heel_x"] = rheel[0]
            prevVals["right_heel_y"] = rheel[1]
            prevVals["vertical_momentum"] = verticalMomentum
            prevVals["forward_momentum"] = forwardMomentum
            
            // Python: Note - prev_vals is NEVER updated in Python code, stays at initial values
            
            // Python: Build feature map exactly matching Python dictionary structure
            val frameFeatures = mapOf(
                "frame" to i.toFloat(),
                
                // CLINICAL GAIT PARAMETERS
                "hip_height" to hipY,
                "hip_y_velocity" to hipYVelocity,
                "hip_x_velocity" to hipXVelocity,
                "hip_vertical_acceleration" to hipVerticalAcceleration,
                "hip_horizontal_acceleration" to hipHorizontalAcceleration,
                "com_x" to comX,
                "com_y" to comY,
                "com_x_velocity" to comXVelocity,
                "com_y_velocity" to comYVelocity,
                "com_acceleration" to comAcceleration,
                
                // JOINT KINEMATICS
                "left_knee_angle" to leftKneeAngle,
                "right_knee_angle" to rightKneeAngle,
                "left_hip_angle" to leftHipAngle,
                "right_hip_angle" to rightHipAngle,
                "left_ankle_angle" to leftAnkleAngle,
                "right_ankle_angle" to rightAnkleAngle,
                "torso_angle" to torsoAngle,
                "torso_angle_velocity" to torsoAngleVelocity,
                
                // JOINT COORDINATION
                "knee_coordination" to kneeCoordination,
                "hip_coordination" to hipCoordination,
                "ankle_coordination" to ankleCoordination,
                "knee_extension_power" to kneeExtensionPower,
                "hip_extension_power" to hipExtensionPower,
                "ankle_power" to anklePower,
                "left_knee_velocity" to leftKneeVelocity,
                "right_knee_velocity" to rightKneeVelocity,
                
                // PHASE-SPECIFIC FEATURES
                "vertical_momentum" to verticalMomentum,
                "sit_to_stand_power" to sitToStandPower,
                "trunk_flexion_velocity" to trunkFlexionVelocity,
                "forward_momentum" to forwardMomentum,
                "forward_acceleration" to forwardAcceleration,
                
                // GAIT ANALYSIS
                "ankle_distance" to ankleDistance,
                "heel_distance" to heelDistance,
                "toe_distance" to toeDistance,
                "base_of_support_width" to baseOfSupportWidth,
                "base_of_support_length" to baseOfSupportLength,
                "lheel_y_velocity" to lheelYVelocity,
                "rheel_y_velocity" to rheelYVelocity,
                "lheel_x_velocity" to lheelXVelocity,
                "rheel_x_velocity" to rheelXVelocity,
                "step_asymmetry" to stepAsymmetry,
                "stride_asymmetry" to strideAsymmetry,
                "time_since_last_step" to timeSinceLastStep,
                "step_frequency" to stepFrequency,
                "step_timing_variability" to stepTimingVariability,
                "stride_length_variation" to strideLength,
                
                // TURNING KINEMATICS
                "shoulder_angle" to shoulderAngle,
                "shoulder_rotation_velocity" to shoulderRotationVelocity,
                "hip_rotation" to hipRotation,
                "hip_rotation_velocity" to hipRotationVelocity,
                "axial_dissociation" to axialDissociation,
                "axial_dissociation_velocity" to axialDissociationVelocity,
                "head_yaw" to headYaw,
                "head_yaw_velocity" to headYawVelocity,
                "torso_twist" to torsoTwist,
                "torso_twist_velocity" to torsoTwistVelocity,
                "direction_change_magnitude" to directionChangeMagnitude,
                "turn_preparation_score" to turnPreparationScore,
                
                // BALANCE AND STABILITY
                "mediolateral_sway" to mediolateralSway,
                "anteroposterior_sway" to anteroposteriorSway,
                "weight_shift_x" to weightShiftX,
                "weight_shift_y" to weightShiftY,
                "stability_margin" to stabilityMargin,
                "body_sway" to bodySway,
                "postural_control" to posturalControl,
                "movement_complexity" to movementComplexity,
                
                // ENERGY AND POWER
                "kinetic_energy" to kineticEnergy,
                "potential_energy" to potentialEnergy,
                "total_mechanical_energy" to totalMechanicalEnergy,
                "rotational_energy" to rotationalEnergy,
                "concentric_power" to concentricPower,
                "eccentric_power" to eccentricPower,
                "total_body_momentum" to totalBodyMomentum,
                
                // TEMPORAL CONTEXT
                "task_progression" to taskProgression,
                "progression_velocity" to progressionVelocity,
                "sit_to_stand_likelihood" to sitToStandLikelihood,
                "walk_from_likelihood" to walkFromLikelihood,
                "turn_first_likelihood" to turnFirstLikelihood,
                "walk_to_likelihood" to walkToLikelihood,
                "turn_second_likelihood" to turnSecondLikelihood,
                "stand_to_sit_likelihood" to standToSitLikelihood
            )
            
            features.add(frameFeatures)
        }
        
        // Python: Add temporal smoothing and multi-scale features
        Log.d(TAG, "🔧 Adding temporal features...")
        
        // Convert to DataFrame-like structure for rolling window operations
        val featureKeys = features[0].keys.toList()
        val featureArrays = mutableMapOf<String, MutableList<Float>>()
        
        // Initialize arrays
        for (key in featureKeys) {
            featureArrays[key] = mutableListOf()
        }
        
        // Fill arrays
        for (frameFeatures in features) {
            for (key in featureKeys) {
                featureArrays[key]!!.add(frameFeatures[key] ?: 0f)
            }
        }
        
        // Python: Multi-scale rolling windows for temporal patterns
        val windows = listOf(5, 10, 15, 30)
        val rollingFeatures = mutableMapOf<String, MutableList<Float>>()
        
        for (window in windows) {
            // Python: df_features[f'hip_y_velocity_smooth_{window}'] = df_features['hip_y_velocity'].rolling(window, center=True).mean()
            rollingFeatures["hip_y_velocity_smooth_$window"] = rollingMean(featureArrays["hip_y_velocity"]!!, window)
            rollingFeatures["turn_score_smooth_$window"] = rollingMean(featureArrays["turn_preparation_score"]!!, window)
            rollingFeatures["movement_complexity_smooth_$window"] = rollingMean(featureArrays["movement_complexity"]!!, window)
            rollingFeatures["hip_height_std_$window"] = rollingStd(featureArrays["hip_height"]!!, window)
            rollingFeatures["forward_momentum_std_$window"] = rollingStd(featureArrays["forward_momentum"]!!, window)
            rollingFeatures["rotation_variation_$window"] = rollingStd(featureArrays["shoulder_rotation_velocity"]!!, window)
        }
        
        // Python: Transition detection (sudden changes)
        val hipYAcceleration = diff(featureArrays["hip_y_velocity"]!!)
        val movementJerk = diff(featureArrays["movement_complexity"]!!).map { abs(it) }.toMutableList()
        val rotationAcceleration = diff(featureArrays["shoulder_rotation_velocity"]!!).map { abs(it) }.toMutableList()
        
        // Python: Phase consistency flags
        val verticalMomentumQuantile = quantile(featureArrays["vertical_momentum"]!!, 0.7)
        val forwardMomentumQuantile = quantile(featureArrays["forward_momentum"]!!.map { abs(it) }, 0.7)
        val turnScoreQuantile = quantile(featureArrays["turn_preparation_score"]!!, 0.7)
        
        val sustainedVerticalMovement = featureArrays["vertical_momentum"]!!.map { 
            if (it > verticalMomentumQuantile) 1f else 0f 
        }.toMutableList()
        val sustainedForwardMovement = featureArrays["forward_momentum"]!!.map { 
            if (abs(it) > forwardMomentumQuantile) 1f else 0f 
        }.toMutableList()
        val sustainedTurning = featureArrays["turn_preparation_score"]!!.map { 
            if (it > turnScoreQuantile) 1f else 0f 
        }.toMutableList()
        
        // Python: Add temporal features to each frame (matching Python DataFrame approach)
        val enhancedFeatures = mutableListOf<Map<String, Float>>()
        
        for (i in features.indices) {
            val frameFeatures = features[i].toMutableMap()
            
            // Add rolling window features for this frame
            for (window in windows) {
                frameFeatures["hip_y_velocity_smooth_$window"] = rollingFeatures["hip_y_velocity_smooth_$window"]!![i]
                frameFeatures["turn_score_smooth_$window"] = rollingFeatures["turn_score_smooth_$window"]!![i]
                frameFeatures["movement_complexity_smooth_$window"] = rollingFeatures["movement_complexity_smooth_$window"]!![i]
                frameFeatures["hip_height_std_$window"] = rollingFeatures["hip_height_std_$window"]!![i]
                frameFeatures["forward_momentum_std_$window"] = rollingFeatures["forward_momentum_std_$window"]!![i]
                frameFeatures["rotation_variation_$window"] = rollingFeatures["rotation_variation_$window"]!![i]
            }
            
            // Add derived features for this frame
            frameFeatures["hip_y_acceleration"] = hipYAcceleration[i]
            frameFeatures["movement_jerk"] = movementJerk[i]
            frameFeatures["rotation_acceleration"] = rotationAcceleration[i]
            frameFeatures["sustained_vertical_movement"] = sustainedVerticalMovement[i]
            frameFeatures["sustained_forward_movement"] = sustainedForwardMovement[i]
            frameFeatures["sustained_turning"] = sustainedTurning[i]
            
            enhancedFeatures.add(frameFeatures)
        }
        
        Log.d(TAG, "Generated ${enhancedFeatures.size} feature vectors with ${enhancedFeatures[0].keys.size} features each")
        
        return enhancedFeatures
    }
    
    // Python pandas equivalent functions
    private fun rollingMean(values: List<Float>, window: Int): MutableList<Float> {
        val result = mutableListOf<Float>()
        for (i in values.indices) {
            val half = window / 2
            val start = maxOf(0, i - half)
            val end = minOf(values.size, i + half + 1)
            val windowValues = values.subList(start, end)
            result.add(windowValues.average().toFloat())
        }
        return result
    }
    
    private fun rollingStd(values: List<Float>, window: Int): MutableList<Float> {
        val result = mutableListOf<Float>()
        for (i in values.indices) {
            val half = window / 2
            val start = maxOf(0, i - half)
            val end = minOf(values.size, i + half + 1)
            val windowValues = values.subList(start, end)
            if (windowValues.size < 2) {
                result.add(0f)
            } else {
                val mean = windowValues.average().toFloat()
                val variance = windowValues.map { (it - mean) * (it - mean) }.average().toFloat()
                result.add(sqrt(variance))
            }
        }
        return result
    }
    
    private fun diff(values: List<Float>): MutableList<Float> {
        val result = mutableListOf<Float>()
        result.add(0f) // First element has no difference
        for (i in 1 until values.size) {
            result.add(values[i] - values[i - 1])
        }
        return result
    }
    
    private fun quantile(values: List<Float>, q: Double): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val index = (q * (sorted.size - 1)).toInt()
        return sorted[index.coerceIn(0, sorted.size - 1)]
    }
}
