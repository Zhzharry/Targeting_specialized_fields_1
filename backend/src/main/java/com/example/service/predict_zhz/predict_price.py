import json
import pickle
import sys
import warnings
from pathlib import Path

import numpy as np

# 抑制所有警告信息，避免污染 JSON 输出
warnings.filterwarnings('ignore')
import os
# 设置环境变量，抑制并行处理的输出
os.environ['JOBLIB_TEMP_FOLDER'] = '/tmp'


def load_model(path: Path):
    """
    加载 pickle 模型文件，兼容不同 Python 版本和协议版本
    """
    # 首先尝试使用 joblib（scikit-learn 推荐方式）
    try:
        import joblib
        return joblib.load(path)
    except ImportError:
        # 如果没有 joblib，使用 pickle
        pass
    except Exception:
        # joblib 加载失败，尝试 pickle
        pass
    
    # 使用 pickle 加载
    with path.open("rb") as f:
        try:
            # 首先尝试使用默认方式加载
            return pickle.load(f)
        except (pickle.UnpicklingError, ValueError, TypeError, AttributeError) as e:
            # 如果失败，尝试指定编码
            f.seek(0)  # 重置文件指针
            try:
                # Python 3.6 及以下版本可能需要指定编码
                return pickle.load(f, encoding='latin1')
            except Exception:
                f.seek(0)
                try:
                    # 尝试使用 bytes 编码（Python 3.2+）
                    return pickle.load(f, encoding='bytes')
                except Exception as e2:
                    # 如果都失败，抛出更详细的错误信息
                    raise RuntimeError(f"无法加载模型文件 {path}: {str(e)} (尝试 latin1 编码失败: {str(e2)})")


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
    # 将 stderr 重定向，避免警告信息污染输出
    import io
    stderr_backup = sys.stderr
    sys.stderr = io.StringIO()
    
    try:
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

        # 抑制 scikit-learn 的警告和并行处理输出
        # 使用 contextlib 来临时重定向 stdout，捕获所有输出
        from contextlib import redirect_stdout
        stdout_capture = io.StringIO()
        
        with redirect_stdout(stdout_capture):
            with warnings.catch_warnings():
                warnings.simplefilter("ignore")
                # 设置环境变量抑制 joblib 输出
                os.environ['JOBLIB_START_METHOD'] = 'threading'
                # 设置 n_jobs=1 来避免并行处理的输出
                if hasattr(model, 'set_params'):
                    try:
                        model.set_params(n_jobs=1)
                    except:
                        pass
                prediction = model.predict(np.array([vector]))[0]
        
        # 只输出 JSON 到 stdout，确保输出是干净的 JSON
        result = {"prediction": float(prediction)}
        json.dump(result, sys.stdout, ensure_ascii=False)
        sys.stdout.flush()  # 确保立即输出
    except Exception as e:
        # 恢复 stderr 以输出错误信息
        sys.stderr = stderr_backup
        error_result = {"error": str(e)}
        json.dump(error_result, sys.stdout)
        sys.exit(1)
    finally:
        # 恢复 stderr
        sys.stderr = stderr_backup


if __name__ == "__main__":
    main()

