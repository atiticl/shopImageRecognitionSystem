#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
商品识别模型预测脚本
使用训练好的YOLOv8模型进行商品识别和分类
"""

import os
import sys
from pathlib import Path
import cv2
import numpy as np
import torch
from ultralytics import YOLO
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from PIL import Image, ImageFont, ImageDraw
import yaml

# PyTorch 2.6版本兼容性设置
import os
# 设置环境变量禁用PyTorch安全检查
os.environ['TORCH_SERIALIZATION_SAFE_GLOBALS'] = '0'

try:
    # 添加更多必要的类到安全全局变量
    torch.serialization.add_safe_globals([
        'ultralytics.nn.tasks.DetectionModel',
        'ultralytics.nn.modules.conv.Conv',
        'ultralytics.nn.modules.block.C2f',
        'ultralytics.nn.modules.head.Detect',
        'ultralytics.nn.modules.block.SPPF',
        'collections.OrderedDict',
        'torch.nn.modules.conv.Conv2d',
        'torch.nn.modules.batchnorm.BatchNorm2d',
        'torch.nn.modules.activation.SiLU'
    ])
except AttributeError:
    # 如果是旧版本PyTorch，忽略此设置
    pass

class ProductPredictor:
    def __init__(self, model_path, config_path=None):
        """
        初始化预测器
        
        Args:
            model_path: 训练好的模型路径
            config_path: 数据集配置文件路径（用于获取类别名称）
        """
        self.model_path = Path(model_path)
        self.config_path = Path(config_path) if config_path else None
        self.model = None
        self.class_names = []
        
        self.load_model()
        self.load_class_names()
        
    def load_model(self):
        """加载训练好的模型"""
        if not self.model_path.exists():
            print(f"模型文件不存在: {self.model_path}")
            # 如果训练模型不存在，尝试使用预训练模型
            pretrained_model = Path('yolov8n.pt')
            if pretrained_model.exists():
                print(f"使用预训练模型: {pretrained_model}")
                self.model_path = pretrained_model
            else:
                sys.exit(1)
            
        try:
            # 设置环境变量禁用PyTorch安全检查
            os.environ['TORCH_SERIALIZATION_SAFE_GLOBALS'] = '0'
            os.environ['PYTORCH_ENABLE_MPS_FALLBACK'] = '1'
            
            # 尝试最简单的加载方式
            print(f"正在加载模型: {self.model_path}")
            
            # 方法1: 直接使用YOLO加载，不进行任何额外设置
            try:
                from ultralytics import YOLO
                self.model = YOLO(str(self.model_path))
                print(f"模型加载成功: {self.model_path}")
                return
            except Exception as e1:
                print(f"方法1失败: {e1}")
            
            # 方法2: 尝试使用torch.jit.load
            try:
                import torch
                # 先尝试用torch直接加载
                torch_model = torch.load(str(self.model_path), map_location='cpu')
                print(f"Torch加载成功，模型类型: {type(torch_model)}")
                # 然后用YOLO包装
                self.model = YOLO(str(self.model_path))
                print(f"模型加载成功: {self.model_path}")
                return
            except Exception as e2:
                print(f"方法2失败: {e2}")
            
            # 方法3: 使用预训练模型作为备选
            try:
                pretrained_path = Path('yolov8n.pt')
                if pretrained_path.exists() and str(self.model_path) != str(pretrained_path):
                    print(f"尝试使用预训练模型: {pretrained_path}")
                    self.model = YOLO(str(pretrained_path))
                    print(f"预训练模型加载成功: {pretrained_path}")
                    return
            except Exception as e3:
                print(f"方法3失败: {e3}")
                
            # 如果所有方法都失败，抛出异常
            raise Exception("所有模型加载方法都失败了")
                
        except Exception as e:
            print(f"模型加载失败: {e}")
            # 不要直接退出，而是抛出异常让上层处理
            raise e
            
    def load_class_names(self):
        """加载类别名称"""
        if self.config_path and self.config_path.exists():
            try:
                with open(self.config_path, 'r', encoding='utf-8') as f:
                    config = yaml.safe_load(f)
                    self.class_names = config.get('names', [])
                print(f"加载类别名称: {self.class_names}")
            except Exception as e:
                print(f"加载类别名称失败: {e}")
                self.class_names = []
        else:
            # 使用默认类别名称
            self.class_names = ['文具类', '糖果类', '食品类', '饮料类', '洗漱用品', '其他类']
            
    def predict_single_image(self, image_path, conf_threshold=0.5):
        """
        预测单张图片
        
        Args:
            image_path: 图片路径
            conf_threshold: 置信度阈值
            
        Returns:
            预测结果
        """
        image_path = Path(image_path)
        if not image_path.exists():
            print(f"图片文件不存在: {image_path}")
            return None
            
        try:
            # 进行预测
            results = self.model(str(image_path), conf=conf_threshold)
            
            # 解析结果
            predictions = []
            for result in results:
                boxes = result.boxes
                if boxes is not None:
                    for box in boxes:
                        # 获取边界框坐标
                        x1, y1, x2, y2 = box.xyxy[0].cpu().numpy()
                        
                        # 获取置信度和类别
                        confidence = box.conf[0].cpu().numpy()
                        class_id = int(box.cls[0].cpu().numpy())
                        
                        # 获取类别名称
                        class_name = self.class_names[class_id] if class_id < len(self.class_names) else f"Class_{class_id}"
                        
                        predictions.append({
                            'bbox': [float(x1), float(y1), float(x2), float(y2)],
                            'confidence': float(confidence),
                            'class_id': class_id,
                            'class_name': class_name
                        })
            
            return {
                'image_path': str(image_path),
                'predictions': predictions,
                'image_shape': results[0].orig_shape
            }
            
        except Exception as e:
            print(f"预测图片 {image_path} 时出错: {e}")
            return None
            
    def predict_batch(self, image_dir, conf_threshold=0.5):
        """
        批量预测图片
        
        Args:
            image_dir: 图片目录
            conf_threshold: 置信度阈值
            
        Returns:
            批量预测结果
        """
        image_dir = Path(image_dir)
        if not image_dir.exists():
            print(f"图片目录不存在: {image_dir}")
            return []
            
        # 支持的图片格式
        image_extensions = ['.jpg', '.jpeg', '.png', '.bmp', '.tiff']
        
        # 获取所有图片文件
        image_files = []
        for ext in image_extensions:
            image_files.extend(image_dir.glob(f"*{ext}"))
            image_files.extend(image_dir.glob(f"*{ext.upper()}"))
            
        print(f"找到 {len(image_files)} 张图片")
        
        # 批量预测
        batch_results = []
        for image_file in image_files:
            result = self.predict_single_image(image_file, conf_threshold)
            if result:
                batch_results.append(result)
                
        return batch_results
        
    def visualize_prediction(self, result, save_path=None, show=True):
        """
        可视化预测结果
        
        Args:
            result: 预测结果
            save_path: 保存路径
            show: 是否显示图片
        """
        if not result or not result['predictions']:
            print("没有预测结果可视化")
            return
            
        # 读取图片
        image = cv2.imread(result['image_path'])
        image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        
        # 创建图表
        fig, ax = plt.subplots(1, 1, figsize=(12, 8))
        ax.imshow(image)
        
        # 绘制预测框
        for pred in result['predictions']:
            x1, y1, x2, y2 = pred['bbox']
            width = x2 - x1
            height = y2 - y1
            
            # 绘制边界框
            rect = patches.Rectangle(
                (x1, y1), width, height,
                linewidth=2, edgecolor='red', facecolor='none'
            )
            ax.add_patch(rect)
            
            # 添加标签
            label = f"{pred['class_name']}: {pred['confidence']:.2f}"
            ax.text(x1, y1-10, label, 
                   bbox=dict(boxstyle="round,pad=0.3", facecolor='red', alpha=0.7),
                   fontsize=10, color='white')
        
        ax.set_title(f"商品识别结果 - {Path(result['image_path']).name}")
        ax.axis('off')
        
        # 保存图片
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            print(f"结果图片已保存到: {save_path}")
            
        # 显示图片
        if show:
            plt.show()
        else:
            plt.close()
            
    def get_statistics(self, batch_results):
        """
        获取批量预测统计信息
        
        Args:
            batch_results: 批量预测结果
            
        Returns:
            统计信息
        """
        if not batch_results:
            return {}
            
        # 统计各类别数量
        class_counts = {}
        total_predictions = 0
        
        for result in batch_results:
            for pred in result['predictions']:
                class_name = pred['class_name']
                class_counts[class_name] = class_counts.get(class_name, 0) + 1
                total_predictions += 1
                
        # 计算置信度统计
        confidences = []
        for result in batch_results:
            for pred in result['predictions']:
                confidences.append(pred['confidence'])
                
        stats = {
            'total_images': len(batch_results),
            'total_predictions': total_predictions,
            'class_counts': class_counts,
            'avg_confidence': np.mean(confidences) if confidences else 0,
            'min_confidence': np.min(confidences) if confidences else 0,
            'max_confidence': np.max(confidences) if confidences else 0
        }
        
        return stats

def main():
    """主函数"""
    print("=== YOLOv8 商品识别预测 ===")
    
    # 模型路径（需要先训练模型）
    model_path = "models/product_model_n_20240101_120000.pt"  # 示例路径
    config_path = "datasets/product_dataset/dataset.yaml"
    
    # 检查模型文件
    if not Path(model_path).exists():
        print(f"模型文件不存在: {model_path}")
        print("请先运行 train_model.py 训练模型")
        
        # 尝试查找其他模型文件
        models_dir = Path("models")
        if models_dir.exists():
            model_files = list(models_dir.glob("*.pt"))
            if model_files:
                model_path = str(model_files[0])
                print(f"使用找到的模型文件: {model_path}")
            else:
                print("未找到任何模型文件")
                return
        else:
            print("models目录不存在")
            return
    
    # 创建预测器
    predictor = ProductPredictor(model_path, config_path)
    
    # 示例：预测单张图片
    test_image = "自建数据集/文具类/20200501_110911.jpg"
    if Path(test_image).exists():
        print(f"\\n预测单张图片: {test_image}")
        result = predictor.predict_single_image(test_image, conf_threshold=0.3)
        
        if result:
            print("预测结果:")
            for i, pred in enumerate(result['predictions']):
                print(f"  {i+1}. {pred['class_name']} (置信度: {pred['confidence']:.3f})")
                
            # 可视化结果
            predictor.visualize_prediction(result, save_path="prediction_result.jpg")
        else:
            print("预测失败")
    
    # 示例：批量预测
    test_dir = "自建数据集/糖果类"
    if Path(test_dir).exists():
        print(f"\\n批量预测目录: {test_dir}")
        batch_results = predictor.predict_batch(test_dir, conf_threshold=0.3)
        
        if batch_results:
            # 显示统计信息
            stats = predictor.get_statistics(batch_results)
            print("\\n批量预测统计:")
            print(f"  - 总图片数: {stats['total_images']}")
            print(f"  - 总预测数: {stats['total_predictions']}")
            print(f"  - 平均置信度: {stats['avg_confidence']:.3f}")
            print("  - 类别分布:")
            for class_name, count in stats['class_counts'].items():
                print(f"    {class_name}: {count}")

if __name__ == "__main__":
    main()