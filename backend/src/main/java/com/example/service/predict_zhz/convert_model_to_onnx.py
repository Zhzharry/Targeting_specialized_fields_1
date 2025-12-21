"""
一次性模型转换脚本：将scikit-learn的pkl模型转换为ONNX格式
使用方法：
    python convert_model_to_onnx.py <城市文件夹> [输出路径]
    
例如：
    python convert_model_to_onnx.py beijng
    python convert_model_to_onnx.py shanghai
    python convert_model_to_onnx.py tianjin

转换后的ONNX模型将保存在对应城市文件夹中，文件名为 house_price_model.onnx
"""

import json
import pickle
import sys
from pathlib import Path

try:
    import joblib
    HAS_JOBLIB = True
except ImportError:
    HAS_JOBLIB = False

try:
    from skl2onnx import convert_sklearn
    from skl2onnx.common.data_types import FloatTensorType
    HAS_SKL2ONNX = True
except ImportError:
    HAS_SKL2ONNX = False
    print("错误: 需要安装 skl2onnx 库")
    print("安装命令: pip install skl2onnx")
    sys.exit(1)


def load_model(path: Path):
    """加载pickle模型文件"""
    if HAS_JOBLIB:
        try:
            return joblib.load(path)
        except Exception:
            pass
    
    with path.open("rb") as f:
        try:
            return pickle.load(f)
        except Exception:
            f.seek(0)
            try:
                return pickle.load(f, encoding='latin1')
            except Exception:
                f.seek(0)
                return pickle.load(f, encoding='bytes')


def main():
    if len(sys.argv) < 2:
        print("使用方法: python convert_model_to_onnx.py <城市文件夹> [输出路径]")
        print("例如: python convert_model_to_onnx.py beijng")
        sys.exit(1)
    
    city_folder = sys.argv[1]
    base_dir = Path(__file__).parent
    city_dir = base_dir / city_folder
    
    if not city_dir.exists():
        print(f"错误: 城市文件夹不存在: {city_dir}")
        sys.exit(1)
    
    model_path = city_dir / "house_price_model.pkl"
    config_path = city_dir / "model_config.json"
    
    if not model_path.exists():
        print(f"错误: 模型文件不存在: {model_path}")
        sys.exit(1)
    
    if not config_path.exists():
        print(f"错误: 配置文件不存在: {config_path}")
        sys.exit(1)
    
    # 加载模型和配置
    print(f"正在加载模型: {model_path}")
    model = load_model(model_path)
    
    print(f"正在加载配置: {config_path}")
    with config_path.open("r", encoding="utf-8") as f:
        config = json.load(f)
    
    feature_columns = config.get("feature_columns", [])
    num_features = len(feature_columns)
    
    print(f"模型类型: {type(model).__name__}")
    print(f"特征数量: {num_features}")
    
    # 定义输入类型（ONNX需要知道输入张量的形状和类型）
    initial_type = [('float_input', FloatTensorType([None, num_features]))]
    
    # 转换为ONNX格式
    output_path = city_dir / "house_price_model.onnx"
    print(f"正在转换为ONNX格式: {output_path}")
    
    try:
        onnx_model = convert_sklearn(model, initial_types=initial_type)
        
        with output_path.open("wb") as f:
            f.write(onnx_model.SerializeToString())
        
        print(f"✓ 转换成功! ONNX模型已保存到: {output_path}")
        print(f"  文件大小: {output_path.stat().st_size / 1024 / 1024:.2f} MB")
        
    except Exception as e:
        print(f"✗ 转换失败: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()

