import json
import pickle
import sys
from pathlib import Path

import numpy as np


def load_model(model_path: Path):
    with model_path.open("rb") as f:
        return pickle.load(f)


def load_config(config_path: Path):
    with config_path.open("r", encoding="utf-8") as f:
        return json.load(f)


def safe_float(value, default=0.0):
    if value is None:
        return default
    if isinstance(value, (int, float)):
        return float(value)
    try:
        return float(str(value))
    except Exception:
        return default


def main():
    payload = json.load(sys.stdin)
    model_path = Path(payload["modelPath"]).expanduser().resolve()
    config_path = Path(payload["configPath"]).expanduser().resolve()
    features_input = payload.get("features") or {}

    model = load_model(model_path)
    config = load_config(config_path)
    feature_columns = config.get("feature_columns", [])
    scaler_means = (config.get("scaler_info") or {}).get("means", {})

    feature_vector = []
    for column in feature_columns:
        value = features_input.get(column, scaler_means.get(column))
        feature_vector.append(safe_float(value))

    if not feature_vector:
        raise ValueError("No features provided")

    prediction = model.predict(np.array([feature_vector]))[0]
    json.dump({"prediction": float(prediction)}, sys.stdout)


if __name__ == "__main__":
    main()

