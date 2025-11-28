import json
import pickle
import sys
from pathlib import Path

import numpy as np


def load_model(path: Path):
    with path.open("rb") as f:
        return pickle.load(f)


def load_config(path: Path):
    with path.open("r", encoding="utf-8") as f:
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
    scaler_info = config.get("scaler_info") or {}
    mean_values = scaler_info.get("means", {})

    vector = []
    for column in feature_columns:
        value = features_input.get(column, mean_values.get(column))
        vector.append(safe_float(value))

    if not vector:
        raise ValueError("feature_columns 为空或特征缺失")

    prediction = model.predict(np.array([vector]))[0]
    json.dump({"prediction": float(prediction)}, sys.stdout)


if __name__ == "__main__":
    main()

