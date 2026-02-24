import { http } from '@/utils/request'
import axios from 'axios'

// 模型接口类型定义
export interface Model {
  id: number
  name: string
  version: string
  type: string
  framework: string
  accuracy: number
  status: 'ACTIVE' | 'INACTIVE' | 'TRAINING' | 'ERROR'
  fileSize: string | number // 支持格式化字符串和数字
  trainingSamples: number
  description: string
  createTime: string
  updateTime: string
  fileUrl: string
  threshold: number
  precision?: number // 精确度（可选）
  recall?: number // 召回率（可选）
  map50?: number // mAP@0.5（可选）
  f1Score?: number // F1分数（可选）
  inferenceTime?: number // 推理时间（可选）
}

export interface ModelTestResult {
  modelId: number
  modelName: string
  testImageName: string
  testImageUrl: string
  prediction: string
  confidence: number
  predictions: Array<{
    class: string
    confidence: number
  }>
  testTime: string
  inferenceTime: number
}

export interface ModelSearchParams {
  page?: number
  size?: number
  sortBy?: string
  sortDir?: string
  name?: string
  status?: string
  type?: string
}

export interface TrainingParams {
  datasetId: number
  baseModelId?: number
  epochs: number
  learningRate: number
  batchSize: number
}

// 模型管理API
export const modelApi = {
  // 获取模型列表
  getModels(params: ModelSearchParams = {}) {
    // 直接调用训练后端API获取模型列表
    return axios.get('http://localhost:5000/api/models', { params }).then(res => {
      // 转换后端数据格式以匹配前端期望的格式
      const models = res.data.models || []
      return {
        content: models.map((model: any, index: number) => {
          // 从模型名称中提取版本信息
          const nameMatch = model.name.match(/(\d{8}_\d{6})/)
          const version = nameMatch ? nameMatch[1] : '1.0'
          
          // 根据模型类型确定框架
          const framework = model.type === 'saved_model' ? 'YOLOv8' : 'Unknown'
          
          // 确定模型状态（基于文件是否存在和大小）
          const status = model.size > 0 ? 'ACTIVE' : 'INACTIVE'
          
          // 生成更详细的描述
          const description = [
            model.epochs ? `训练轮数: ${model.epochs}` : null,
            model.training_samples ? `训练样本: ${model.training_samples}` : null,
            `来源: ${model.source}`,
            `类型: ${model.type}`
          ].filter(Boolean).join(', ')
          
          return {
            id: index + 1, // 使用索引+1作为ID
            name: model.name,
            version: version,
            type: model.type?.toUpperCase() || 'GENERAL',
            framework: framework,
            accuracy: model.accuracy || 0,
            status: status,
            fileSize: model.size_formatted || `${Math.round(model.size / 1024 / 1024 * 100) / 100} MB`,
            trainingSamples: model.training_samples || 0,
            description: description,
            createTime: model.created_time,
            updateTime: model.created_time, // 使用created_time作为更新时间
            fileUrl: model.path,
            threshold: 0.5, // 默认阈值
            precision: model.precision || 0,
            recall: model.recall || 0,
            map50: model.map50 || 0,
            f1Score: model.precision && model.recall ? 
              Math.round((2 * model.precision * model.recall) / (model.precision + model.recall) * 100) / 100 : 0,
            inferenceTime: model.inference_time || 50 // 使用实际推理时间或默认值
          }
        }),
        totalElements: models.length,
        totalPages: Math.ceil(models.length / (params.size || 10)),
        size: params.size || models.length,
        number: params.page || 0
      }
    })
  },

  // 获取单个模型详情
  getModel(id: number) {
    return http.get(`/api/models/${id}`).then(res => res.data)
  },

  // 创建模型
  createModel(data: {
    name: string
    version: string
    type: string
    framework: string
    fileUrl: string
    status?: string
    description?: string
    threshold?: number
  }) {
    const formData = new FormData()
    formData.append('name', data.name)
    formData.append('version', data.version)
    formData.append('type', data.type)
    formData.append('framework', data.framework)
    formData.append('fileUrl', data.fileUrl)
    if (data.description) formData.append('description', data.description)
    formData.append('threshold', String(data.threshold ?? 80))
    formData.append('status', data.status ?? 'ACTIVE')
    return http.post('/api/models', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).then(res => res.data)
  },

  // 更新模型信息
  updateModel(id: number, data: {
    name: string
    version: string
    type: string
    framework: string
    description?: string
    threshold?: number
  }) {
    const params = {
      name: data.name,
      version: data.version,
      type: data.type,
      framework: data.framework,
      description: data.description,
      threshold: data.threshold ?? 80
    }
    return http.put(`/api/models/${id}`, null, { params }).then(res => res.data)
  },

  // 切换模型状态（启用/禁用）
  toggleModelStatus(id: number) {
    return http.put(`/api/models/${id}/status`).then(res => res.data)
  },

  // 删除模型
  deleteModel(id: number) {
    return http.delete(`/api/models/${id}`).then(res => res.data)
  },

  // 测试模型
  testModel(id: number, imageFile: File) {
    const formData = new FormData()
    formData.append('file', imageFile)
    return http.upload(`/api/models/${id}/test`, formData).then(res => res.data)
  },

  // 开始训练模型
  startTraining(id: number, params: TrainingParams) {
    return http.post(`/api/models/${id}/train`, params).then(res => res.data)
  },

  // 新增：上传数据集并开始训练，确保准确率来源真实数据
  startTrainingWithDataset(id: number, formData: FormData) {
    return http.upload(`/api/models/${id}/train-with-dataset`, formData).then(res => res.data)
  },

  // 获取训练日志
  getTrainingLogs(id: number, page = 0, size = 10) {
    return http.get(`/api/models/${id}/training-logs`, { params: { page, size } }).then(res => res.data)
  },

  // 获取模型版本
  getModelVersions(id: number, page = 0, size = 10) {
    return http.get(`/api/models/${id}/versions`, { params: { page, size } }).then(res => res.data)
  },

  // 获取模型统计信息
  getModelStatistics() {
    return http.get('/api/models/statistics').then(res => res.data)
  },

  // 上传模型文件
  uploadModelFile(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return http.upload('/api/models/upload', formData).then(res => res.data)
  },

  // 调用训练后端测试模型接口
  testModelWithTrainBackend(imageFile: File) {
    const formData = new FormData()
    formData.append('file', imageFile)
    // 直接使用axios调用训练后端的预测接口，绕过响应拦截器
    return axios.post('http://localhost:5000/api/predict', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).then(res => res.data)
  }
}

export default modelApi