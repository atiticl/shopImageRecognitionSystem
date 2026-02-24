// 用户相关类型
export interface User {
  id: number
  username: string
  role: 'ADMIN' | 'OPERATOR'
  email?: string
  createdAt: string
  updatedAt: string
}

export interface LoginForm {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  user: User
}

// 分类相关类型
export interface Category {
  id: number
  parentId?: number
  name: string
  description?: string
  categoryCode?: string
  level?: number
  sortOrder?: number
  isActive?: boolean
  iconUrl?: string
  createdAt: string
  updatedAt: string
  children?: Category[]
}

// 分类请求类型
export interface CategoryRequest {
  parentId?: number
  name: string
  description?: string
  categoryCode?: string
  level?: number
  sortOrder?: number
  isActive?: boolean
  iconUrl?: string
}

// 图片相关类型
export interface ProductImage {
  id: number
  userId: number
  fileUrl: string
  thumbUrl?: string
  status: 'UPLOADED' | 'PREPROCESSED' | 'CLASSIFIED'
  imageMd5?: string
  uploadedAt: string
}

// 分类任务相关类型
export interface ClassificationTask {
  id: number
  taskName: string
  userId: number
  totalImages: number
  processedImages: number
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  createdAt: string
  updatedAt: string
}

export interface ClassificationResult {
  id: number
  imageId: number
  taskId: number
  predictedCategoryId?: number
  confidence?: number
  correctedCategoryId?: number
  createdAt: string
  image?: ProductImage
  predictedCategory?: Category
  correctedCategory?: Category
}

// 模型相关类型
export interface Model {
  id: number
  modelName: string
  version?: string
  fileUrl: string
  framework: 'TENSORFLOW' | 'PYTORCH'
  accuracy?: number
  threshold: number
  status: 'ACTIVE' | 'INACTIVE'
  uploadedAt: string
}

// API响应类型
export interface ApiResponse<T = any> {
  success: boolean
  message: string
  data: T
}

// 分页相关类型
export interface PageRequest {
  page: number
  size: number
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

// WebSocket消息类型
export interface WebSocketMessage {
  type: 'PROGRESS' | 'COMPLETE' | 'ERROR'
  taskId: number
  data: any
}