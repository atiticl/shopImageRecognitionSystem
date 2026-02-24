import { http } from '@/utils/request'

// 用户状态枚举
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED'

// 用户角色枚举
export type UserRole = 'ADMIN' | 'OPERATOR'

// 用户DTO接口
export interface UserDTO {
  id: number
  username: string
  email: string
  realName?: string
  phone?: string
  department?: string
  role: UserRole
  status: UserStatus
  avatar?: string
  remark?: string
  loginCount: number
  taskCount: number
  imageCount: number
  onlineTime: number
  lastActiveDays: number
  lastLoginTime?: string
  createTime?: string
  updateTime?: string
}

// 分页响应接口
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
  numberOfElements: number
  pageable: {
    pageNumber: number
    pageSize: number
    sort: {
      empty: boolean
      sorted: boolean
      unsorted: boolean
    }
    offset: number
    paged: boolean
    unpaged: boolean
  }
  sort: {
    empty: boolean
    sorted: boolean
    unsorted: boolean
  }
}

// API响应包装接口
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 用户创建请求接口
export interface UserCreateRequest {
  username: string
  email: string
  realName?: string
  password: string
  role: UserRole
  status?: UserStatus
  phone?: string
  remark?: string
}

// 用户更新请求接口
export interface UserUpdateRequest {
  username?: string
  email?: string
  realName?: string
  role?: UserRole
  status?: UserStatus
  phone?: string
  avatar?: string
  remark?: string
}

// 用户搜索参数接口
export interface UserSearchParams {
  page?: number
  size?: number
  sortBy?: string
  sortDir?: 'asc' | 'desc'
  keyword?: string
  role?: UserRole
  status?: UserStatus
}

/**
 * 获取用户列表（分页）
 * @param params 搜索参数
 * @returns 分页用户列表
 */
export const getUsers = (params: UserSearchParams = {}): Promise<ApiResponse<PageResponse<UserDTO>>> => {
  const {
    page = 0,
    size = 10,
    sortBy = 'id',
    sortDir = 'desc',
    keyword,
    role,
    status
  } = params

  const queryParams = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
    sortBy,
    sortDir
  })

  if (keyword) queryParams.append('keyword', keyword)
  if (role) queryParams.append('role', role)
  if (status) queryParams.append('status', status)

  return http.get(`/api/users?${queryParams.toString()}`)
}

/**
 * 根据ID获取用户详情
 * @param id 用户ID
 * @returns 用户详情
 */
export const getUserById = (id: number): Promise<ApiResponse<UserDTO>> => {
  return http.get(`/api/users/${id}`)
}

/**
 * 创建用户
 * @param data 用户创建请求数据
 * @returns 创建的用户信息
 */
export const createUser = (data: UserCreateRequest): Promise<ApiResponse<UserDTO>> => {
  return http.post('/api/users', data)
}

/**
 * 更新用户信息
 * @param id 用户ID
 * @param data 用户更新请求数据
 * @returns 更新后的用户信息
 */
export const updateUser = (id: number, data: UserUpdateRequest): Promise<ApiResponse<UserDTO>> => {
  return http.put(`/api/users/${id}`, data)
}

/**
 * 删除用户
 * @param id 用户ID
 * @returns 删除结果
 */
export const deleteUser = (id: number): Promise<ApiResponse<boolean>> => {
  return http.delete(`/api/users/${id}`)
}

/**
 * 批量删除用户
 * @param ids 用户ID数组
 * @returns 删除结果
 */
export const batchDeleteUsers = (ids: number[]): Promise<ApiResponse<boolean>> => {
  return http.delete('/api/users/batch', { data: ids })
}

/**
 * 更新用户状态
 * @param id 用户ID
 * @param status 新状态
 * @returns 更新后的用户信息
 */
export const updateUserStatus = (id: number, status: UserStatus): Promise<ApiResponse<UserDTO>> => {
  return http.put(`/api/users/${id}/status?status=${status}`)
}

/**
 * 重置用户密码
 * @param id 用户ID
 * @returns 重置结果
 */
export const resetUserPassword = (id: number): Promise<ApiResponse<boolean>> => {
  return http.put(`/api/users/${id}/password/reset`)
}

/**
 * 获取当前用户信息
 * @returns 当前用户信息
 */
export const getCurrentUser = (): Promise<ApiResponse<UserDTO>> => {
  return http.get('/api/users/current')
}

/**
 * 更新当前用户信息
 * @param data 用户更新请求数据
 * @returns 更新后的用户信息
 */
export const updateCurrentUser = (data: UserUpdateRequest): Promise<ApiResponse<UserDTO>> => {
  return http.put('/api/users/current', data)
}

/**
 * 获取用户统计信息
 * @returns 统计信息
 */
export const getUserStatistics = (): Promise<ApiResponse<any>> => {
  return http.get('/api/users/statistics')
}

/**
 * 获取可用角色列表
 * @returns 角色数组
 */
export const getUserRoles = (): Promise<ApiResponse<UserRole[]>> => {
  return http.get('/api/users/roles')
}

/**
 * 获取可用状态列表
 * @returns 状态数组
 */
export const getUserStatuses = (): Promise<ApiResponse<UserStatus[]>> => {
  return http.get('/api/users/statuses')
}