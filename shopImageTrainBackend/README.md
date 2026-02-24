# 商品智能识别分类管理系统 - 训练后端

基于YOLOv8的商品图像识别和分类系统，支持自定义数据集训练和模型推理。

## 项目结构

```
shopImageTrainBackend/
├── requirements.txt          # Python依赖包
├── data_processor.py        # 数据预处理脚本
├── train_model.py          # 模型训练脚本
├── predict.py              # 模型推理脚本
├── app.py                  # Flask后端API
├── templates/              # 前端模板
│   └── upload.html         # 上传页面
├── static/                 # 静态资源
├── models/                 # 训练好的模型
├── datasets/               # 处理后的数据集
└── 自建数据集/              # 原始数据集
```

## 数据集分类

当前数据集包含以下商品类别：
- 文具类 (46张图片)
- 糖果类 (37张图片)
- 食品类 (43张图片)
- 饮料类 (26张图片)
- 洗漱用品 (25张图片)
- 其他类 (45张图片)
- 综合训练 (84张图片)

## 安装依赖

```bash
pip install -r requirements.txt
```

## 使用方法

1. **数据预处理**：
   ```bash
   python data_processor.py
   ```

2. **训练模型**：
   ```bash
   python train_model.py
   ```

3. **启动Web服务**：
   ```bash
   python app.py
   ```

4. **访问上传页面**：
   打开浏览器访问 `http://localhost:5000`

## 环境要求

- Python 3.8+
- CUDA (可选，用于GPU加速)
- 至少4GB内存