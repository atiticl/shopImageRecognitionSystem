import request from '@/utils/request'

export interface SystemLog {
  id: number
  level: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG'
  module: 'USER' | 'IMAGE' | 'TASK' | 'SYSTEM' | 'AUTH'
  message: string
  username?: string
  ip?: string
  userAgent?: string
  stackTrace?: string
  requestData?: string
  createTime: string
}

export interface LogSearchParams {
  level?: string
  module?: string
  keyword?: string
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}

export interface LogStatistics {
  info: number
  warn: number
  error: number
  debug: number
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// 获取系统日志列表
export function getSystemLogs(params: LogSearchParams) {
  return request<ApiResponse<any>>({
    url: '/api/system-logs',
    method: 'get',
    params
  })
}

// 获取日志统计信息
export function getLogStatistics() {
  return request<ApiResponse<LogStatistics>>({
    url: '/api/system-logs/statistics',
    method: 'get'
  })
}

// 清空所有日志
export function clearAllLogs() {
  return request<ApiResponse<string>>({
    url: '/api/system-logs/clear',
    method: 'delete'
  })
}

// 获取日志详情
export function getLogDetail(id: number) {
  return request<ApiResponse<SystemLog>>({
    url: `/api/system-logs/${id}`,
    method: 'get'
  })
}

// 测试日志记录
export function testLog(level: string, module: string, message: string) {
  return request<ApiResponse<string>>({
    url: '/api/system-logs/test',
    method: 'post',
    params: {
      level,
      module,
      message
    }
  })
}