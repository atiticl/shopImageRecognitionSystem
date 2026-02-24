#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据预处理脚本
将自建数据集转换为YOLOv8训练格式
"""

import os
import shutil
import random
from pathlib import Path
import yaml
from PIL import Image
import cv2
import numpy as np

class DataProcessor:
    def __init__(self, source_dir, output_dir):
        self.source_dir = Path(source_dir)
        self.output_dir = Path(output_dir)
        self.class_names = []
        self.class_mapping = {}
        
    def create_directory_structure(self):
        """创建YOLOv8数据集目录结构"""
        dirs = [
            self.output_dir / 'images' / 'train',
            self.output_dir / 'images' / 'val',
            self.output_dir / 'labels' / 'train',
            self.output_dir / 'labels' / 'val'
        ]
        
        for dir_path in dirs:
            dir_path.mkdir(parents=True, exist_ok=True)
            
    def scan_classes(self):
        """扫描数据集中的类别"""
        self.class_names = []
        
        # 检查是否有嵌套的数据集目录
        nested_dataset_dir = None
        for item in self.source_dir.iterdir():
            if item.is_dir() and not item.name.startswith('.'):
                # 检查是否是嵌套的数据集目录
                sub_dirs = [sub for sub in item.iterdir() if sub.is_dir() and not sub.name.startswith('.')]
                if len(sub_dirs) > 3:  # 如果子目录很多，可能是嵌套的数据集
                    nested_dataset_dir = item
                    break
        
        # 如果发现嵌套目录，使用嵌套目录作为源目录
        scan_dir = nested_dataset_dir if nested_dataset_dir else self.source_dir
        
        for item in scan_dir.iterdir():
            if item.is_dir() and not item.name.startswith('.'):
                # 排除一些特殊目录和数据库文件
                if item.name not in ['综合训练'] and not item.name.endswith('.db'):
                    # 检查目录中是否有图片文件
                    image_files = list(item.glob('*.jpg')) + list(item.glob('*.png')) + list(item.glob('*.jpeg'))
                    if len(image_files) > 0:
                        self.class_names.append(item.name)
        
        # 如果使用了嵌套目录，更新源目录
        if nested_dataset_dir:
            self.source_dir = nested_dataset_dir
            print(f"检测到嵌套数据集目录，使用: {self.source_dir}")
        
        # 创建类别映射
        self.class_mapping = {name: idx for idx, name in enumerate(self.class_names)}
        print(f"发现类别: {self.class_names}")
        print(f"类别映射: {self.class_mapping}")
        
    def process_images(self, train_ratio=0.8):
        """处理图片并分配到训练集和验证集"""
        all_images = []
        
        # 收集所有图片
        for class_name in self.class_names:
            class_dir = self.source_dir / class_name
            if class_dir.exists():
                # 支持多种图片格式
                image_extensions = ['*.jpg', '*.jpeg', '*.png', '*.JPG', '*.JPEG', '*.PNG']
                for ext in image_extensions:
                    for img_file in class_dir.glob(ext):
                        # 排除数据库文件
                        if not img_file.name.endswith('.db'):
                            all_images.append((img_file, class_name))
        
        # 随机打乱
        random.shuffle(all_images)
        
        # 分割训练集和验证集
        split_idx = int(len(all_images) * train_ratio)
        train_images = all_images[:split_idx]
        val_images = all_images[split_idx:]
        
        print(f"训练集图片数量: {len(train_images)}")
        print(f"验证集图片数量: {len(val_images)}")
        
        # 处理训练集
        self._process_image_set(train_images, 'train')
        
        # 处理验证集
        self._process_image_set(val_images, 'val')
        
    def _process_image_set(self, image_list, split):
        """处理单个数据集分割"""
        for idx, (img_path, class_name) in enumerate(image_list):
            try:
                # 读取图片 - 使用cv2.imdecode处理中文路径
                img_bytes = np.fromfile(str(img_path), dtype=np.uint8)
                img = cv2.imdecode(img_bytes, cv2.IMREAD_COLOR)
                if img is None:
                    print(f"无法读取图片: {img_path}")
                    continue
                
                h, w = img.shape[:2]
                
                # 生成新的文件名
                new_name = f"{class_name}_{idx:04d}"
                
                # 复制图片到目标目录
                img_dst = self.output_dir / 'images' / split / f"{new_name}.jpg"
                # 使用cv2.imencode处理中文路径
                success, encoded_img = cv2.imencode('.jpg', img)
                if success:
                    encoded_img.tofile(str(img_dst))
                else:
                    print(f"无法编码图片: {img_path}")
                    continue
                
                # 创建标签文件（整个图片作为一个目标）
                label_dst = self.output_dir / 'labels' / split / f"{new_name}.txt"
                class_id = self.class_mapping[class_name]
                
                # YOLOv8格式: class_id center_x center_y width height (归一化坐标)
                # 整个图片作为目标，所以中心在(0.5, 0.5)，宽高为(1.0, 1.0)
                with open(label_dst, 'w', encoding='utf-8') as f:
                    f.write(f"{class_id} 0.5 0.5 1.0 1.0\n")
                    
            except Exception as e:
                print(f"处理图片 {img_path} 时出错: {e}")
                
    def create_yaml_config(self):
        """创建YOLOv8配置文件"""
        config = {
            'path': str(self.output_dir.absolute()),
            'train': 'images/train',
            'val': 'images/val',
            'nc': len(self.class_names),
            'names': self.class_names
        }
        
        yaml_path = self.output_dir / 'dataset.yaml'
        with open(yaml_path, 'w', encoding='utf-8') as f:
            yaml.dump(config, f, default_flow_style=False, allow_unicode=True)
            
        print(f"配置文件已保存到: {yaml_path}")
        return yaml_path
        
    def process_dataset(self):
        """完整的数据集处理流程"""
        print("开始处理数据集...")
        
        # 创建目录结构
        self.create_directory_structure()
        
        # 扫描类别
        self.scan_classes()
        
        # 处理图片
        self.process_images()
        
        # 创建配置文件
        config_path = self.create_yaml_config()
        
        print("数据集处理完成!")
        return config_path

def main():
    # 设置路径
    source_dir = r"d:\桌面\新建文件夹\AAAA毕业单\基于Springboot+Vue的商品智能识别分类管理系统\shopImageTrainBackend\自建数据集"
    output_dir = r"d:\桌面\新建文件夹\AAAA毕业单\基于Springboot+Vue的商品智能识别分类管理系统\shopImageTrainBackend\datasets\processed_dataset"
    
    print(f"源目录: {source_dir}")
    print(f"输出目录: {output_dir}")
    
    # 创建处理器
    processor = DataProcessor(source_dir, output_dir)
    
    # 处理数据集
    try:
        config_path = processor.process_dataset()
        print(f"\n数据集配置文件: {config_path}")
        print("可以使用此配置文件进行YOLOv8模型训练")
    except Exception as e:
        print(f"处理数据集时出错: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()