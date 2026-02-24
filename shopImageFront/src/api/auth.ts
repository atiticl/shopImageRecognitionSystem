import { http } from '@/utils/request'

// 登录请求参数接口
export interface LoginRequest {
  username: string
  password: string
}

// 注册请求参数接口
export interface RegisterRequest {
  username: string
  password: string
  email?: string
  role: 'ADMIN' | 'OPERATOR'
}

// 认证响应接口
export interface AuthResponse {
  token: string
  username: string
  role: 'ADMIN' | 'OPERATOR'
  message?: string
}

// API响应包装接口
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

/**
 * 用户登录
 * @param data 登录请求参数
 * @returns 认证响应
 */
export const login = (data: LoginRequest): Promise<ApiResponse<AuthResponse>> => {
  return http.post('/api/auth/login', data)
}

/**
 * 用户注册
 * @param data 注册请求参数
 * @returns 认证响应
 */
export const register = (data: RegisterRequest): Promise<ApiResponse<AuthResponse>> => {
  return http.post('/api/auth/register', data)
}

/**
 * 获取可用角色列表
 * @returns 角色数组
 */
export const getRoles = (): Promise<ApiResponse<string[]>> => {
  return http.get('/api/auth/roles')
}

/**
 * 退出登录（客户端处理）
 */
export const logout = (): void => {
  // 清除本地存储的token
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  // 清除sessionStorage
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('user')
}