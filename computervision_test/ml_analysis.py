import shap
import numpy as np
import pandas as pd
from tensorflow.keras.models import load_model
import json

# Load feature column names
with open("computervision_test/data/severity_features.json", "r") as f:
    feature_cols = json.load(f)

# Load model and data
model = load_model("computervision_test/data/severity_mlp_model.h5")
df = pd.read_csv("computervision_test/data/gait_features_with_severity.csv")
X = df[feature_cols].fillna(0)
print("Input feature matrix shape:", X.shape)

# Optional: Print model input info
print("Model type:", type(model))
print("Model input tensors:", model.inputs)
print("Model input tensor names:", [inp.name for inp in model.inputs])

# Create SHAP GradientExplainer with background data
explainer = shap.GradientExplainer(model, X.values[:100])

# Calculate SHAP values for all samples
shap_values = explainer.shap_values(X.values)

# Handle multi-class SHAP values (shape: samples x features x classes)
if isinstance(shap_values, list):
    raise NotImplementedError("Unexpected list type shap_values with shape:", [sv.shape for sv in shap_values])
else:
    mean_abs_shap = np.mean(np.abs(shap_values), axis=0)  # (features, classes)
    mean_abs_shap_per_feature = np.mean(mean_abs_shap, axis=1)  # average across classes, shape (features,)

# Sort features by importance descending
feature_importance = sorted(zip(feature_cols, mean_abs_shap_per_feature), key=lambda x: x[1], reverse=True)

print("\nFeature importance (mean absolute SHAP value averaged over classes):")
for feature, importance in feature_importance:
    print(f"{feature}: {importance:.4f}")

print(X.columns.tolist())
print(X.head())
print(X.describe())


# Plot SHAP summary plot showing all features
shap.summary_plot(shap_values, X, max_display=None, feature_names=feature_cols)
