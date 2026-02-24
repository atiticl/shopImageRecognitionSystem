#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Flask后端API服务
提供商品识别模型训练和预测接口
"""

import os
import sys
import json
import zipfile
import shutil
from pathlib import Path
from datetime import datetime
import threading
import time
import requests

from flask import Flask, request, jsonify, render_template, send_file
from flask_cors import CORS
from flask_socketio import SocketIO, emit
from werkzeug.utils import secure_filename
import cv2
import numpy as np
from PIL import Image

# 导入自定义模块
from data_processor import DataProcessor
from train_model import ProductModelTrainer
from predict import ProductPredictor
from minio_client import minio_client

app = Flask(__name__)
CORS(app, origins="*")  # 允许跨域请求
socketio = SocketIO(app, cors_allowed_origins="*")

# 配置
app.config['MAX_CONTENT_LENGTH'] = 500 * 1024 * 1024  # 500MB
app.config['ALLOWED_EXTENSIONS'] = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'zip', '7z'}

# 全局变量
training_status = {
    'is_training': False,
    'progress': 0,
    'message': '',
    'start_time': None,
    'model_path': None
}

current_predictor = None

# Java后端WebSocket API配置
JAVA_BACKEND_URL = "http://localhost:8080"

def send_websocket_message(message_type, task_id, progress=None, message=None, error=None):
    """直接通过SocketIO发送WebSocket消息到前端"""
    try:
        payload = {
            "taskId": task_id,
            "type": message_type.upper(),
            "timestamp": datetime.now().isoformat()
        }
        
        if progress is not None:
            payload["progress"] = progress
        if message is not None:
            payload["message"] = message
        if error is not None:
            payload["error"] = error
            
        # 直接发送到前端
        socketio.emit('progress', payload)
        print(f"WebSocket消息发送成功: {message_type} - {task_id}")
            
    except Exception as e:
        print(f"发送WebSocket消息异常: {str(e)}")

@socketio.on('connect')
def handle_connect():
    """处理WebSocket连接"""
    print('客户端已连接')

@socketio.on('disconnect')
def handle_disconnect():
    """处理WebSocket断开连接"""
    print('客户端已断开连接')

def allowed_file(filename):
    """检查文件扩展名是否允许"""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in app.config['ALLOWED_EXTENSIONS']

def ensure_directories():
    """确保必要的目录存在"""
    directories = [
        'uploads',
        'models',
        'datasets',
        'static',
        'templates'
    ]
    
    for directory in directories:
        Path(directory).mkdir(exist_ok=True)

@app.route('/')
def index():
    """主页"""
    return render_template('upload.html')

@app.route('/api/upload', methods=['POST'])
def upload_file():
    """上传文件到MinIO"""
    print("=== 收到文件上传请求 ===")
    try:
        if 'file' not in request.files:
            return jsonify({'error': '没有文件被上传'}), 400
        
        file = request.files['file']
        if file.filename == '':
            return jsonify({'error': '没有选择文件'}), 400
        
        if file and allowed_file(file.filename):
            # 保留原始扩展名
            original_filename = file.filename
            name, ext = os.path.splitext(original_filename)
            safe_name = secure_filename(name)
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"{timestamp}_{safe_name}{ext}"
            
            # 上传到MinIO而不是本地存储
            file_data = file.read()
            minio_path = f"datasets/{filename}"
            
            # 上传到MinIO - 使用upload_bytes方法上传字节数据
            print(f"正在上传文件到MinIO: {minio_path}")
            success = minio_client.upload_bytes(file_data, minio_path, 'application/zip')
            print(f"MinIO上传结果: {success}")
            
            if success:
                print(f"文件上传成功: {filename}")
                return jsonify({
                    'message': '文件上传成功',
                    'filename': filename,
                    'filepath': minio_path,
                    'minio_path': minio_path
                })
            else:
                print("MinIO上传失败")
                return jsonify({'error': 'MinIO上传失败'}), 500
        else:
            return jsonify({'error': '不支持的文件类型'}), 400
            
    except Exception as e:
        return jsonify({'error': f'上传失败: {str(e)}'}), 500

@app.route('/api/extract_dataset', methods=['POST'])
def extract_dataset():
    print("=== 收到数据集解压请求 ===")
    try:
        data = request.get_json()
        minio_path = data.get('filepath')
        print(f"要解压的文件路径: {minio_path}")
        
        if not minio_path:
            return jsonify({'error': '文件路径不存在'}), 400
        
        # 从MinIO下载文件到临时目录
        filename = minio_path.split('/')[-1]
        temp_zip_path = Path('temp') / filename
        temp_zip_path.parent.mkdir(exist_ok=True)
        
        # 从MinIO下载文件
        success = minio_client.download_file(minio_path, str(temp_zip_path))
        if not success:
            return jsonify({'error': '从MinIO下载文件失败'}), 400
        
        # 创建解压目录
        extract_dir = Path('datasets') / 'uploaded_dataset'
        if extract_dir.exists():
            shutil.rmtree(extract_dir)
        extract_dir.mkdir(parents=True)
        
        # 解压文件
        if temp_zip_path.suffix.lower() == '.zip':
            with zipfile.ZipFile(temp_zip_path, 'r') as zip_ref:
                zip_ref.extractall(extract_dir)
        else:
            return jsonify({'error': '目前只支持ZIP格式'}), 400
        
        # 清理临时文件
        temp_zip_path.unlink()
        
        # 扫描解压后的目录结构
        class_dirs = []
        
        # 检查是否有嵌套的数据集目录
        dataset_root = extract_dir
        for item in extract_dir.iterdir():
            if item.is_dir() and not item.name.startswith('.'):
                # 检查是否是数据集根目录（包含多个类别文件夹）
                subdirs = [d for d in item.iterdir() if d.is_dir() and not d.name.startswith('.')]
                if len(subdirs) > 1:  # 如果有多个子目录，可能是数据集根目录
                    dataset_root = item
                    break
        
        # 扫描类别目录
        for item in dataset_root.iterdir():
            if item.is_dir() and not item.name.startswith('.') and item.name != '综合训练':
                # 统计图片数量（支持多种格式）
                image_files = []
                for ext in ['*.jpg', '*.jpeg', '*.png', '*.JPG', '*.JPEG', '*.PNG']:
                    image_files.extend(list(item.glob(ext)))
                
                # 排除数据库文件
                image_files = [f for f in image_files if not f.name.endswith('.db')]
                
                if len(image_files) > 0:
                    class_dirs.append({
                        'name': item.name,
                        'image_count': len(image_files)
                    })
        
        print(f"发现的类别: {[cls['name'] for cls in class_dirs]}")
        print(f"各类别图片数量: {[(cls['name'], cls['image_count']) for cls in class_dirs]}")
        
        # 将解压后的数据集上传到MinIO
        dataset_minio_path = f"datasets/extracted/{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        
        # 递归上传整个数据集目录到MinIO
        def upload_directory_to_minio(local_dir, minio_prefix):
            for item in local_dir.rglob('*'):
                if item.is_file() and not item.name.startswith('.'):
                    relative_path = item.relative_to(local_dir)
                    minio_object_path = f"{minio_prefix}/{relative_path}".replace('\\', '/')
                    
                    with open(item, 'rb') as f:
                        file_data = f.read()
                    
                    minio_client.upload_bytes(file_data, minio_object_path)
        
        upload_directory_to_minio(extract_dir, dataset_minio_path)
        
        return jsonify({
            'message': '数据集解压成功并上传到MinIO',
            'extract_path': str(extract_dir),
            'minio_path': dataset_minio_path,
            'classes': class_dirs
        })
        
    except Exception as e:
        return jsonify({'error': f'解压失败: {str(e)}'}), 500

@app.route('/api/start_training', methods=['POST'])
def start_training():
    """开始训练模型"""
    global training_status
    
    try:
        if training_status['is_training']:
            return jsonify({'error': '模型正在训练中'}), 400
        
        data = request.get_json()
        dataset_path = data.get('dataset_path', 'datasets/uploaded_dataset')
        epochs = data.get('epochs', 50)
        batch_size = data.get('batch_size', 8)
        
        # 重置训练状态
        training_status = {
            'is_training': True,
            'progress': 0,
            'message': '开始训练...',
            'start_time': datetime.now(),
            'model_path': None
        }
        
        # 在后台线程中开始训练
        training_thread = threading.Thread(
            target=train_model_background,
            args=(dataset_path, epochs, batch_size)
        )
        training_thread.daemon = True
        training_thread.start()
        
        return jsonify({
            'message': '训练已开始',
            'status': training_status
        })
        
    except Exception as e:
        training_status['is_training'] = False
        return jsonify({'error': f'启动训练失败: {str(e)}'}), 500

def train_model_background(dataset_path, epochs, batch_size):
    """后台训练模型"""
    global training_status
    
    # 生成训练任务ID
    task_id = f"training_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
    
    try:
        # 数据预处理
        training_status['message'] = '正在处理数据集...'
        training_status['progress'] = 10
        send_websocket_message("progress", task_id, progress=10, message="正在处理数据集...")
        
        processor = DataProcessor(dataset_path, 'datasets/processed_dataset')
        config_path = processor.process_dataset()
        
        # 开始训练
        training_status['message'] = '正在训练模型...'
        training_status['progress'] = 20
        send_websocket_message("progress", task_id, progress=20, message="正在训练模型...")
        
        trainer = ProductModelTrainer(config_path, model_size='n')
        trainer.load_model()
        
        # 训练模型
        results = trainer.train_model(
            epochs=epochs,
            batch=batch_size,
            patience=10
        )
        
        if results:
            training_status['progress'] = 90
            training_status['message'] = '正在保存模型...'
            send_websocket_message("progress", task_id, progress=90, message="正在保存模型...")
            
            # 保存模型
            model_path = trainer.save_model()
            
            training_status['progress'] = 100
            training_status['message'] = '训练完成!'
            training_status['model_path'] = str(model_path) if model_path else None
            send_websocket_message("complete", task_id, progress=100, message="训练完成!")
        else:
            training_status['message'] = '训练失败'
            send_websocket_message("error", task_id, error="训练失败")
            
    except Exception as e:
        training_status['message'] = f'训练出错: {str(e)}'
        send_websocket_message("error", task_id, error=f"训练出错: {str(e)}")
    finally:
        training_status['is_training'] = False

@app.route('/api/training_status', methods=['GET'])
def get_training_status():
    """获取训练状态"""
    return jsonify(training_status)

@app.route('/api/predict', methods=['POST'])
def predict_image():
    """预测图片"""
    global current_predictor
    
    try:
        if 'file' not in request.files:
            return jsonify({'error': '没有文件被上传'}), 400
        
        file = request.files['file']
        if file.filename == '':
            return jsonify({'error': '没有选择文件'}), 400
        
        # 获取请求中指定的模型名称
        model_name = request.form.get('model_name', None)
        
        # 查找可用的模型文件
        selected_model_path = None
        selected_model_name = None
        
        if model_name:
            # 如果指定了模型名称，查找对应的模型文件
            # 1. 先在runs/detect目录中查找
            runs_dir = Path('runs/detect')
            if runs_dir.exists():
                for train_dir in runs_dir.iterdir():
                    if train_dir.is_dir():
                        weights_dir = train_dir / 'weights'
                        if weights_dir.exists():
                            for model_file in weights_dir.glob('*.pt'):
                                full_model_name = f"{train_dir.name}_{model_file.name}"
                                if full_model_name == model_name:
                                    selected_model_path = str(model_file)
                                    selected_model_name = full_model_name
                                    break
                        if selected_model_path:
                            break
            
            # 2. 如果没找到，在models目录中查找
            if not selected_model_path:
                models_dir = Path('models')
                if models_dir.exists():
                    for model_file in models_dir.glob('*.pt'):
                        if model_file.name == model_name:
                            selected_model_path = str(model_file)
                            selected_model_name = model_file.name
                            break
        
        # 如果没有指定模型或没找到指定模型，使用最新的训练模型
        if not selected_model_path:
            # 获取所有可用模型并按创建时间排序
            all_models = []
            
            # 扫描runs/detect目录
            runs_dir = Path('runs/detect')
            if runs_dir.exists():
                for train_dir in runs_dir.iterdir():
                    if train_dir.is_dir():
                        weights_dir = train_dir / 'weights'
                        if weights_dir.exists():
                            for model_file in weights_dir.glob('*.pt'):
                                stat = model_file.stat()
                                all_models.append({
                                    'name': f"{train_dir.name}_{model_file.name}",
                                    'path': str(model_file),
                                    'created_time': stat.st_ctime,
                                    'type': 'training_result'
                                })
            
            # 扫描models目录
            models_dir = Path('models')
            if models_dir.exists():
                for model_file in models_dir.glob('*.pt'):
                    stat = model_file.stat()
                    all_models.append({
                        'name': model_file.name,
                        'path': str(model_file),
                        'created_time': stat.st_ctime,
                        'type': 'saved_model'
                    })
            
            if not all_models:
                return jsonify({'error': '没有找到可用的模型文件'}), 400
            
            # 按创建时间排序，选择最新的模型
            all_models.sort(key=lambda x: x['created_time'], reverse=True)
            latest_model = all_models[0]
            selected_model_path = latest_model['path']
            selected_model_name = latest_model['name']
        
        # 初始化预测器
        if current_predictor is None or current_predictor.model_path != Path(selected_model_path):
            config_path = 'datasets/processed_dataset/dataset.yaml'
            current_predictor = ProductPredictor(selected_model_path, config_path)
        
        # 准备上传图片到MinIO
        original_filename = secure_filename(file.filename)
        # 获取文件扩展名
        file_ext = Path(original_filename).suffix
        if not file_ext:
            # 如果没有扩展名，尝试从content-type推断
            content_type = file.content_type
            if 'jpeg' in content_type or 'jpg' in content_type:
                file_ext = '.jpg'
            elif 'png' in content_type:
                file_ext = '.png'
            elif 'gif' in content_type:
                file_ext = '.gif'
            elif 'bmp' in content_type:
                file_ext = '.bmp'
            else:
                file_ext = '.jpg'  # 默认扩展名
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"predict_{timestamp}_{Path(original_filename).stem}{file_ext}"
        
        # 将图片上传到MinIO
        try:
            file_data = file.read()
            file.seek(0)  # 重置文件指针
            minio_url = minio_client.upload_prediction_image(file_data, filename)
        except Exception as e:
            return jsonify({'error': f'图片上传到MinIO失败: {str(e)}'}), 500
        
        # 保存图片到临时本地文件进行预测
        temp_images_dir = "temp_images"
        os.makedirs(temp_images_dir, exist_ok=True)
        temp_image_path = os.path.join(temp_images_dir, f"temp_{filename}")
        file.save(temp_image_path)
        
        # 进行预测
        result = current_predictor.predict_single_image(temp_image_path, conf_threshold=0.3)
        
        # 清理临时文件
        try:
            if os.path.exists(temp_image_path):
                os.remove(temp_image_path)
        except:
            pass  # 忽略清理错误
        
        if result:
            return jsonify({
                'message': '预测成功',
                'result': result,
                'model_used': selected_model_name,
                'image_url': minio_url  # 返回MinIO中的图片URL
            })
        else:
            return jsonify({'error': '预测失败'}), 500
            
    except Exception as e:
        return jsonify({'error': f'预测出错: {str(e)}'}), 500

@app.route('/api/models', methods=['GET'])
def list_models():
    """列出所有可用的模型"""
    try:
        models = []
        
        # 1. 扫描models目录下的模型文件
        models_dir = Path('models')
        if models_dir.exists():
            for model_file in models_dir.glob('*.pt'):
                stat = model_file.stat()
                models.append({
                    'name': model_file.name,
                    'path': str(model_file),
                    'size': stat.st_size,
                    'size_formatted': format_file_size(stat.st_size),
                    'created_time': datetime.fromtimestamp(stat.st_ctime).isoformat(),
                    'type': 'saved_model',
                    'source': 'models',
                    'accuracy': None,
                    'training_samples': None,
                    'epochs': None
                })
        
        # 2. 扫描runs/detect目录下的训练结果
        runs_dir = Path('runs/detect')
        if runs_dir.exists():
            for train_dir in runs_dir.iterdir():
                if train_dir.is_dir():
                    weights_dir = train_dir / 'weights'
                    if weights_dir.exists():
                        for model_file in weights_dir.glob('*.pt'):
                            stat = model_file.stat()
                            
                            # 读取训练配置信息
                            args_file = train_dir / 'args.yaml'
                            epochs = None
                            batch_size = None
                            if args_file.exists():
                                try:
                                    import yaml
                                    with open(args_file, 'r', encoding='utf-8') as f:
                                        args = yaml.safe_load(f)
                                        epochs = args.get('epochs', None)
                                        batch_size = args.get('batch', None)
                                except:
                                    pass
                            
                            # 读取训练结果（准确率等指标）
                            results_file = train_dir / 'results.csv'
                            accuracy = None
                            precision = None
                            recall = None
                            map50 = None
                            if results_file.exists():
                                try:
                                    import pandas as pd
                                    df = pd.read_csv(results_file)
                                    if not df.empty:
                                        # 清理列名中的空格
                                        df.columns = df.columns.str.strip()
                                        # 获取最后一轮的指标（最佳结果）
                                        last_row = df.iloc[-1]
                                        # CSV中的数据已经是小数形式（0-1），直接乘以100转为百分比
                                        precision_val = last_row.get('metrics/precision(B)', 0)
                                        recall_val = last_row.get('metrics/recall(B)', 0)
                                        map50_val = last_row.get('metrics/mAP50(B)', 0)
                                        
                                        if precision_val > 0:
                                            precision = round(precision_val * 100, 1)
                                        if recall_val > 0:
                                            recall = round(recall_val * 100, 1)
                                        if map50_val > 0:
                                            map50 = round(map50_val * 100, 1)
                                            accuracy = map50  # 使用mAP50作为准确率指标
                                        
                                        print(f"模型 {model_file.name} 指标: precision={precision}, recall={recall}, mAP50={map50}")
                                except Exception as e:
                                    print(f"读取训练结果失败: {e}")
                                    import traceback
                                    traceback.print_exc()
                            
                            # 估算训练样本数量（从数据集配置文件读取）
                            training_samples = None
                            dataset_yaml = Path('datasets/processed_dataset/dataset.yaml')
                            if dataset_yaml.exists():
                                try:
                                    import yaml
                                    with open(dataset_yaml, 'r', encoding='utf-8') as f:
                                        dataset_config = yaml.safe_load(f)
                                        # 尝试从train路径统计图片数量
                                        train_path = dataset_config.get('train', '')
                                        if train_path:
                                            train_full_path = Path(train_path)
                                            if not train_full_path.is_absolute():
                                                train_full_path = Path('datasets/processed_dataset') / train_path
                                            if train_full_path.exists():
                                                # 统计所有图片文件
                                                image_extensions = ['.jpg', '.jpeg', '.png', '.bmp', '.tiff', '.webp']
                                                training_samples = sum(1 for f in train_full_path.rglob('*') 
                                                                     if f.suffix.lower() in image_extensions)
                                except Exception as e:
                                    print(f"读取数据集配置失败: {e}")
                            
                            models.append({
                                'name': f"{train_dir.name}_{model_file.name}",
                                'path': str(model_file),
                                'size': stat.st_size,
                                'size_formatted': format_file_size(stat.st_size),
                                'created_time': datetime.fromtimestamp(stat.st_ctime).isoformat(),
                                'type': 'training_result',
                                'source': f'runs/detect/{train_dir.name}',
                                'epochs': epochs,
                                'batch_size': batch_size,
                                'model_type': model_file.stem,  # best, last, epoch10, etc.
                                'accuracy': accuracy,
                                'precision': round(precision, 2) if precision else None,
                                'recall': round(recall, 2) if recall else None,
                                'map50': round(map50, 2) if map50 else None,
                                'training_samples': training_samples
                            })
        
        # 按创建时间排序
        models.sort(key=lambda x: x['created_time'], reverse=True)
        
        return jsonify({'models': models})
        
    except Exception as e:
        return jsonify({'error': f'获取模型列表失败: {str(e)}'}), 500

def format_file_size(size_bytes):
    """格式化文件大小"""
    if size_bytes == 0:
        return "0B"
    size_names = ["B", "KB", "MB", "GB"]
    import math
    i = int(math.floor(math.log(size_bytes, 1024)))
    p = math.pow(1024, i)
    s = round(size_bytes / p, 2)
    return f"{s} {size_names[i]}"

@app.route('/api/download_model/<model_name>', methods=['GET'])
def download_model(model_name):
    """下载模型文件"""
    try:
        model_path = Path('models') / secure_filename(model_name)
        if not model_path.exists():
            return jsonify({'error': '模型文件不存在'}), 404
        
        return send_file(model_path, as_attachment=True)
        
    except Exception as e:
        return jsonify({'error': f'下载失败: {str(e)}'}), 500

@app.errorhandler(413)
def too_large(e):
    return jsonify({'error': '文件太大'}), 413

if __name__ == '__main__':
    # 确保必要的目录存在
    ensure_directories()
    
    print("=== 商品识别系统后端服务 ===")
    print("服务地址: http://localhost:5000")
    print("WebSocket地址: http://localhost:5000")
    print("上传页面: http://localhost:5000")
    print("API文档:")
    print("  POST /api/upload - 上传文件")
    print("  POST /api/extract_dataset - 解压数据集")
    print("  POST /api/start_training - 开始训练")
    print("  GET  /api/training_status - 获取训练状态")
    print("  POST /api/predict - 预测图片")
    print("  GET  /api/models - 列出模型")
    
    # 使用SocketIO运行应用
    socketio.run(app, host='0.0.0.0', port=5000, debug=True, allow_unsafe_werkzeug=True)