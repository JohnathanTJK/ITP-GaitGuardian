"""
Convert trained MLP models to ONNX format for Android deployment
Supports both binary and multi-class models
"""

import joblib
import numpy as np
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType
import json
import os
import onnx
from onnx import helper, TensorProto

MODEL_DIR = "severity_models"
OUTPUT_DIR = "android_models"
os.makedirs(OUTPUT_DIR, exist_ok=True)

def convert_model_to_onnx(mode='binary'):
    """Convert scikit-learn MLP model to ONNX format"""
    print(f"\n🔄 Converting {mode} MLP model to ONNX...")
    
    # Load model and metadata
    model_path = os.path.join(MODEL_DIR, f"mlp_severity_{mode}.pkl")
    scaler_path = os.path.join(MODEL_DIR, f"severity_scaler_{mode}.pkl")
    metadata_path = os.path.join(MODEL_DIR, f"severity_metadata_{mode}.json")
    
    if not os.path.exists(model_path):
        print(f"❌ Model not found: {model_path}")
        return False
    
    model = joblib.load(model_path)
    scaler = joblib.load(scaler_path)
    
    with open(metadata_path, 'r') as f:
        metadata = json.load(f)
    
    num_features = metadata['num_features']
    
    print(f"  Features: {num_features}")
    print(f"  Classes: {len(metadata['class_names'])}")
    print(f"  Architecture: {metadata['architecture']}")
    
    # Define input type for ONNX
    initial_type = [('input', FloatTensorType([None, num_features]))]
    
    # Convert model to ONNX
    try:
        onnx_model = convert_sklearn(
            model, 
            initial_types=initial_type, 
            target_opset=12,
            options={type(model): {'zipmap': False}}  # Disable zipmap for cleaner output
        )
        
        # Save ONNX model
        onnx_output_path = os.path.join(OUTPUT_DIR, f"mlp_severity_{mode}.onnx")
        with open(onnx_output_path, "wb") as f:
            f.write(onnx_model.SerializeToString())
        
        print(f"✅ Model ONNX saved: {onnx_output_path}")
        
        # Verify ONNX model
        onnx_model_check = onnx.load(onnx_output_path)
        onnx.checker.check_model(onnx_model_check)
        print(f"✅ ONNX model verified successfully")
        
    except Exception as e:
        print(f"❌ Model conversion failed: {e}")
        return False
    
    # Convert scaler to ONNX
    try:
        scaler_onnx = convert_sklearn(
            scaler, 
            initial_types=initial_type, 
            target_opset=12
        )
        
        scaler_output_path = os.path.join(OUTPUT_DIR, f"severity_scaler_{mode}.onnx")
        with open(scaler_output_path, "wb") as f:
            f.write(scaler_onnx.SerializeToString())
        
        print(f"✅ Scaler ONNX saved: {scaler_output_path}")
        
    except Exception as e:
        print(f"⚠️  Scaler conversion failed: {e}")
        print("   Manual scaling will be used in Android code")
    
    # Save Android-compatible metadata
    android_metadata = {
        'mode': mode,
        'feature_names': metadata['feature_names'],
        'class_names': metadata['class_names'],
        'num_features': num_features,
        'num_classes': len(metadata['class_names']),
        'architecture': metadata['architecture'],
        'scaler_mean': metadata['scaler_mean'],
        'scaler_scale': metadata['scaler_scale']
    }
    
    android_metadata_path = os.path.join(OUTPUT_DIR, f"severity_metadata_{mode}.json")
    with open(android_metadata_path, 'w') as f:
        json.dump(android_metadata, f, indent=2)
    
    print(f"✅ Android metadata saved: {android_metadata_path}")
    
    # Test model with dummy input
    print(f"\n🧪 Testing ONNX model with dummy input...")
    dummy_input = np.random.randn(1, num_features).astype(np.float32)
    
    # Test with sklearn model
    sklearn_prediction = model.predict_proba(scaler.transform(dummy_input))
    print(f"  sklearn prediction shape: {sklearn_prediction.shape}")
    
    # Test with ONNX Runtime
    try:
        import onnxruntime as ort
        ort_session = ort.InferenceSession(onnx_output_path)
        ort_input = {ort_session.get_inputs()[0].name: scaler.transform(dummy_input).astype(np.float32)}
        ort_output = ort_session.run(None, ort_input)
        print(f"  ONNX prediction shape: {ort_output[1].shape}")  # Probabilities
        print(f"✅ ONNX model test passed")
    except Exception as e:
        print(f"⚠️  ONNX Runtime test failed: {e}")
    
    return True

def main():
    print("="*60)
    print("🔄 MLP ONNX CONVERSION")
    print("="*60)
    
    # Convert binary model (recommended)
    if os.path.exists(os.path.join(MODEL_DIR, "mlp_severity_binary.pkl")):
        print("\n📦 Converting BINARY model...")
        convert_model_to_onnx(mode='binary')
    
    # Convert multiclass model if exists
    if os.path.exists(os.path.join(MODEL_DIR, "mlp_severity_multiclass.pkl")):
        print("\n📦 Converting MULTICLASS model...")
        convert_model_to_onnx(mode='multiclass')
    
    print("\n" + "="*60)
    print("🎉 CONVERSION COMPLETE!")
    print("="*60)
    print(f"\n📁 ONNX models saved to: {OUTPUT_DIR}/")
    print("\n📋 FILES TO COPY TO ANDROID ASSETS:")
    for filename in os.listdir(OUTPUT_DIR):
        print(f"   • {filename}")
    print(f"\n📱 Copy these files to: app/src/main/assets/")

if __name__ == "__main__":
    # Check dependencies
    try:
        import skl2onnx
        import onnx
        import onnxruntime
        print("✅ All dependencies available")
    except ImportError as e:
        print(f"❌ Missing dependency: {e}")
        print("\nInstall with: pip install skl2onnx onnx onnxruntime")
        exit(1)
    
    main()