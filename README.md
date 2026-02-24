# 商品智能识别分类管理系统

基于 Spring Boot + Vue 3 + Python 的商品图像智能识别与分类管理系统。

## 项目结构

```
├── shopImageBackend/       # 后端服务 (Spring Boot)
├── shopImageFront/         # 前端界面 (Vue 3 + Element Plus)
└── shopImageTrainBackend/  # 训练服务 (Python + Flask)
```

## 技术栈

### 后端 (shopImageBackend)
- Spring Boot 2.7+
- Spring Security
- MyBatis Plus
- MySQL
- MinIO (对象存储)
- JWT 认证

### 前端 (shopImageFront)
- Vue 3
- TypeScript
- Element Plus
- Vite
- Pinia (状态管理)
- Vue Router

### 训练服务 (shopImageTrainBackend)
- Python 3.8+
- Flask
- PyTorch / TensorFlow
- OpenCV
- YOLO / ResNet (图像分类模型)

## 功能特性

- 🔐 用户认证与权限管理
- 📊 数据看板与统计分析
- 🏷️ 商品类别管理
- 🤖 AI 模型管理与训练
- 📸 图像上传与智能识别
- 📋 分类任务管理
- 📝 系统日志记录

## 快速开始

### 前置要求

- JDK 11+
- Node.js 16+
- Python 3.8+
- MySQL 8.0+
- MinIO

### 后端启动

```bash
cd shopImageBackend
mvn clean install
mvn spring-boot:run
```

### 前端启动

```bash
cd shopImageFront
npm install
npm run dev
```

### 训练服务启动

```bash
cd shopImageTrainBackend
pip install -r requirements.txt
python app.py
```

## 配置说明

### 数据库配置
修改 `shopImageBackend/src/main/resources/application.yml` 中的数据库连接信息。

### MinIO 配置
配置对象存储服务地址和访问密钥。

### 模型配置
在训练服务中配置模型路径和参数。

## 开发团队

毕业设计项目

## 许可证

MIT License
