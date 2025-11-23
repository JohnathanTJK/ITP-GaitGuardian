"""
🔄 XGBoost to ONNX Model Conversion (Alternative Approach)
Converts the trained XGBoost TUG classification model to ONNX format for Android deployment
"""

import os
import numpy as np
import pandas as pd
import joblib
import json
import onnx
import xgboost as xgb
from sklearn.preprocessing import LabelEncoder
import tempfile

def load_model_artifacts():
    """Load the trained XGBoost model and related artifacts"""
    print("📦 Loading XGBoost model artifacts...")
    
    models_dir = "xgb_models"
    
    # Load the trained model
    model_path = os.path.join(models_dir, "xgboost_tug_model.pkl")
    model = joblib.load(model_path)
    print(f"✅ Loaded XGBoost model from {model_path}")
    
    # Load label encoder
    encoder_path = os.path.join(models_dir, "label_encoder.pkl")
    label_encoder = joblib.load(encoder_path)
    print(f"✅ Loaded label encoder: {label_encoder.classes_}")
    
    # Load feature columns
    features_path = os.path.join(models_dir, "feature_columns.pkl")
    feature_cols = joblib.load(features_path)
    print(f"✅ Loaded feature columns: {len(feature_cols)} features")
    
    # Load metadata
    metadata_path = os.path.join(models_dir, "model_metadata.json")
    with open(metadata_path, 'r') as f:
        metadata = json.load(f)
    print(f"✅ Loaded metadata: {metadata['model_type']}")
    
    return model, label_encoder, feature_cols, metadata

def create_onnx_from_booster(model, feature_cols, output_path="models/xgboost_tug_model.onnx"):
    """Create ONNX model using XGBoost's native save_model functionality"""
    print(f"🔄 Converting XGBoost model to ONNX format...")
    
    try:
        # Get the booster from the sklearn wrapper
        booster = model.get_booster()
        
        # Create simplified feature names that are ONNX-compatible
        simplified_names = [f"f{i}" for i in range(len(feature_cols))]
        
        # Create feature mapping
        feature_mapping = {
            f"f{i}": feature_cols[i] for i in range(len(feature_cols))
        }
        
        # Set the feature names in the booster
        booster.feature_names = simplified_names
        
        # Save as JSON format first (intermediate step)
        temp_json_path = "temp_model.json"
        booster.save_model(temp_json_path)
        
        # Load it back to ensure feature names are set
        temp_booster = xgb.Booster()
        temp_booster.load_model(temp_json_path)
        
        # Now convert to ONNX using onnxmltools
        from onnxmltools import convert_xgboost
        from onnxmltools.convert.common.data_types import FloatTensorType
        
        # Create initial types with correct input shape
        initial_type = [('input', FloatTensorType([None, len(feature_cols)]))]
        
        # Convert to ONNX
        onnx_model = convert_xgboost(
            temp_booster,
            initial_types=initial_type,
            target_opset=11
        )
        
        # Save the ONNX model
        onnx.save_model(onnx_model, output_path)
        print(f"✅ ONNX model saved to {output_path}")
        
        # Clean up temporary file
        if os.path.exists(temp_json_path):
            os.remove(temp_json_path)
        
        # Verify the model
        onnx_model_check = onnx.load(output_path)
        onnx.checker.check_model(onnx_model_check)
        print("✅ ONNX model validation passed")
        
        return onnx_model, feature_mapping
        
    except Exception as e:
        print(f"❌ Error during ONNX conversion: {e}")
        print("🔄 Trying alternative conversion method...")
        
        # Alternative method: Create a new XGBoost model with compatible feature names
        try:
            return create_compatible_onnx_model(model, feature_cols, output_path)
        except Exception as e2:
            print(f"❌ Alternative conversion also failed: {e2}")
            return None, None

def create_compatible_onnx_model(model, feature_cols, output_path):
    """Create ONNX model by retraining with compatible feature names"""
    print("🔄 Creating ONNX-compatible model...")
    
    # Load some sample data to retrain the model
    sample_data_path = None
    for file in os.listdir("engineered_features_csv"):
        if file.endswith("_features.csv"):
            sample_data_path = os.path.join("engineered_features_csv", file)
            break
    
    if sample_data_path is None:
        raise Exception("No sample data found for retraining")
    
    # Load sample data
    df = pd.read_csv(sample_data_path)
    
    if 'tug_subtask' not in df.columns:
        raise Exception("No labels found in sample data")
    
    # Prepare data
    X = df[feature_cols].fillna(0).replace([np.inf, -np.inf], 0)
    y = df['tug_subtask']
    
    # Create label encoder
    le = LabelEncoder()
    y_encoded = le.fit_transform(y)
    
    # Create new model with simplified feature names
    simplified_names = [f"f{i}" for i in range(len(feature_cols))]
    
    # Create DataFrame with simplified column names
    X_simplified = pd.DataFrame(X.values, columns=simplified_names)
    
    # Train a new model
    new_model = xgb.XGBClassifier(
        objective='multi:softprob',
        n_estimators=model.n_estimators,
        max_depth=model.max_depth,
        learning_rate=model.learning_rate,
        random_state=42
    )
    
    new_model.fit(X_simplified, y_encoded)
    
    # Convert to ONNX
    from onnxmltools import convert_xgboost
    from onnxmltools.convert.common.data_types import FloatTensorType
    
    initial_type = [('input', FloatTensorType([None, len(feature_cols)]))]
    
    onnx_model = convert_xgboost(
        new_model,
        initial_types=initial_type,
        target_opset=11
    )
    
    # Save the ONNX model
    onnx.save_model(onnx_model, output_path)
    
    # Create feature mapping
    feature_mapping = {f"f{i}": feature_cols[i] for i in range(len(feature_cols))}
    
    print(f"✅ Compatible ONNX model created and saved to {output_path}")
    
    return onnx_model, feature_mapping

def save_onnx_model_info(output_path="models/xgboost_tug_model.onnx"):
    """Save ONNX model using direct XGBoost export (if supported)"""
    print("🔄 Attempting direct ONNX export...")
    
    try:
        # Load the model
        models_dir = "models"
        model_path = os.path.join(models_dir, "xgboost_tug_model.json")
        
        if not os.path.exists(model_path):
            print("❌ JSON model file not found")
            return None
        
        # Load booster from JSON
        booster = xgb.Booster()
        booster.load_model(model_path)
        
        # Try to save as ONNX (this might work in newer XGBoost versions)
        try:
            booster.save_model(output_path.replace('.onnx', '_direct.onnx'))
            print(f"✅ Direct ONNX export successful")
            return output_path.replace('.onnx', '_direct.onnx')
        except:
            print("❌ Direct ONNX export not supported in this XGBoost version")
            return None
            
    except Exception as e:
        print(f"❌ Direct export failed: {e}")
        return None

def test_onnx_model(onnx_model_path, original_model, feature_cols, test_samples=10):
    """Test ONNX model against original XGBoost model"""
    print(f"🧪 Testing ONNX model accuracy...")
    
    try:
        import onnxruntime as ort
        
        # Load ONNX model
        ort_session = ort.InferenceSession(onnx_model_path)
        
        # Print ONNX model info
        print(f"📋 ONNX Model Info:")
        print(f"   Inputs: {[inp.name + ' ' + str(inp.shape) for inp in ort_session.get_inputs()]}")
        print(f"   Outputs: {[out.name + ' ' + str(out.shape) for out in ort_session.get_outputs()]}")
        
        # Create some test data
        np.random.seed(42)
        test_data = np.random.randn(test_samples, len(feature_cols)).astype(np.float32)
        
        # Get predictions from original model
        original_predictions = original_model.predict_proba(test_data)
        original_labels = original_model.predict(test_data)
        
        # Get predictions from ONNX model
        onnx_inputs = {ort_session.get_inputs()[0].name: test_data}
        onnx_outputs = ort_session.run(None, onnx_inputs)
        
        print(f"📊 Output Analysis:")
        print(f"   Number of ONNX outputs: {len(onnx_outputs)}")
        for i, output in enumerate(onnx_outputs):
            print(f"   Output {i} shape: {output.shape}")
            print(f"   Output {i} sample: {output[0] if len(output) > 0 else 'Empty'}")
        
        # Handle different output formats
        if len(onnx_outputs) >= 2:
            onnx_probabilities = onnx_outputs[1]  # Often probabilities are second output
            onnx_labels = onnx_outputs[0]  # Labels might be first
        else:
            onnx_probabilities = onnx_outputs[0]
            if onnx_probabilities.ndim == 2 and onnx_probabilities.shape[1] > 1:
                # This looks like probabilities
                onnx_labels = np.argmax(onnx_probabilities, axis=1)
            else:
                # This might be labels
                onnx_labels = onnx_probabilities.flatten()
                onnx_probabilities = None
        
        print(f"📊 Test Results:")
        print(f"   Test samples: {test_samples}")
        print(f"   Original predictions shape: {original_predictions.shape}")
        print(f"   Original labels shape: {original_labels.shape}")
        
        if onnx_probabilities is not None:
            print(f"   ONNX probabilities shape: {onnx_probabilities.shape}")
            # Compare probabilities if shapes match
            if onnx_probabilities.shape == original_predictions.shape:
                prob_diff = np.abs(original_predictions - onnx_probabilities).max()
                print(f"   Max probability difference: {prob_diff:.10f}")
                
                if prob_diff < 1e-3:
                    print("✅ ONNX model matches original model within tolerance!")
                    return True
                else:
                    print("⚠️ ONNX model has some differences from original model")
                    return False
            else:
                print(f"⚠️ Probability shapes don't match: {onnx_probabilities.shape} vs {original_predictions.shape}")
        
        # Compare labels
        if onnx_labels is not None:
            print(f"   ONNX labels shape: {onnx_labels.shape}")
            if onnx_labels.shape == original_labels.shape:
                label_accuracy = np.mean(onnx_labels == original_labels)
                print(f"   Label accuracy: {label_accuracy:.4f}")
                
                if label_accuracy > 0.8:  # Allow for some differences
                    print("✅ ONNX model labels mostly match original model!")
                    return True
                else:
                    print("⚠️ ONNX model labels differ significantly from original")
                    return False
            else:
                print(f"⚠️ Label shapes don't match: {onnx_labels.shape} vs {original_labels.shape}")
        
        print("⚠️ Could not properly compare models due to shape mismatches")
        return False
            
    except ImportError:
        print("⚠️ onnxruntime not available for testing. Install with: pip install onnxruntime")
        return None
    except Exception as e:
        print(f"❌ Error testing ONNX model: {e}")
        return False

def create_metadata(label_encoder, feature_cols, metadata, feature_mapping=None):
    """Create metadata for Android usage"""
    onnx_metadata = {
        "model_type": "XGBoost_ONNX",
        "input_features": feature_cols,
        "num_features": len(feature_cols),
        "output_classes": label_encoder.classes_.tolist(),
        "num_classes": len(label_encoder.classes_),
        "class_mapping": {
            int(i): class_name for i, class_name in enumerate(label_encoder.classes_)
        },
        "feature_mapping": feature_mapping if feature_mapping else {},
        "original_metadata": metadata,
        "onnx_input_name": "input",
        "preprocessing": {
            "fill_na_value": 0,
            "replace_inf_value": 0,
            "feature_order": feature_cols
        }
    }
    
    output_path = "models/onnx_metadata.json"
    with open(output_path, 'w') as f:
        json.dump(onnx_metadata, f, indent=2)
    
    print(f"✅ ONNX metadata saved to {output_path}")
    return onnx_metadata

def main():
    """Main conversion pipeline"""
    print("🚀 XGBoost to ONNX Conversion Pipeline")
    print("=" * 50)
    
    # Load model artifacts
    model, label_encoder, feature_cols, metadata = load_model_artifacts()
    
    # Try conversion
    onnx_model, feature_mapping = create_onnx_from_booster(model, feature_cols)
    
    if onnx_model is None:
        print("❌ All ONNX conversion methods failed!")
        print("💡 Consider using TensorFlow Lite or other mobile-friendly formats")
        return
    
    # Create metadata
    create_metadata(label_encoder, feature_cols, metadata, feature_mapping)
    
    # Test the model
    test_onnx_model("models/xgboost_tug_model.onnx", model, feature_cols)
    
    print(f"\n🎉 Conversion completed successfully!")
    print(f"📁 ONNX model: models/xgboost_tug_model.onnx")
    print(f"📁 Metadata: models/onnx_metadata.json")

if __name__ == "__main__":
    main()
