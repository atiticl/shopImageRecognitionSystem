import { http } from '@/utils/request'
import axios from 'axios'

// 仪表板统计数据接口
export interface DashboardStats {
  totalUsers: number
  totalTasks: number
  totalImages: number
  totalModels: number
}

// 系统状态接口
export interface SystemStatus {
  cpu: number
  memory: number
  disk: number
  network: 'normal' | 'error'
}

// 活动记录接口
export interface ActivityRecord {
  id: number
  type: 'user' | 'task' | 'model' | 'system' | 'error'
  description: string
  time: string
}

// API响应包装接口
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 图表数据接口
export interface ChartData {
  taskTrend: {
    dates: string[]
    values: number[]
  }
  accuracy: {
    categories: string[]
    values: number[]
  }
}

/**
 * 获取仪表板统计数据
 * @returns 统计数据
 */
export const getDashboardStats = async (): Promise<DashboardStats> => {
  try {
    // 首先获取用户统计数据（最重要的数据）
    const userStats = await http.get('/api/users/statistics')
    
    // 并行获取其他统计数据，允许这些失败
    const [modelStats, logStats] = await Promise.allSettled([
      axios.get('http://localhost:5000/api/models').catch(() => ({ data: { models: [] } })),
      http.get('/api/system-logs/statistics').catch(() => ({ data: { data: { totalLogs: 0 } } }))
    ])

    // 从用户统计中获取用户数和任务数
    const userData = userStats.data || {}
    const totalUsers = userData.totalUsers || userData.total || 0
    const totalTasks = userData.totalTasks || userData.activeTasks || 0
    const totalImages = userData.totalImages || userData.processedImages || 0

    // 从模型统计中获取模型数
    let totalModels = 0
    if (modelStats.status === 'fulfilled') {
      const modelData = modelStats.value?.data?.models || []
      totalModels = modelData.length
    }

    return {
      totalUsers,
      totalTasks,
      totalImages,
      totalModels
    }
  } catch (error) {
    console.error('获取仪表板统计数据失败:', error)
    // 认证错误直接抛出，让上层处理
    if (error.response?.status === 401 || error.response?.status === 403) {
      throw error
    }
    // 其他错误返回默认数据
    return {
      totalUsers: 0,
      totalTasks: 0,
      totalImages: 0,
      totalModels: 0
    }
  }
}

/**
 * 获取系统状态
 */
export const getSystemStatus = async (): Promise<SystemStatus> => {
  try {
    // 模拟系统状态数据，实际项目中应该调用真实的系统监控API
    return {
      cpu: Math.floor(Math.random() * 30) + 20, // 20-50%
      memory: Math.floor(Math.random() * 40) + 40, // 40-80%
      disk: Math.floor(Math.random() * 20) + 20, // 20-40%
      network: Math.random() > 0.1 ? 'normal' : 'error' // 90%概率正常
    }
  } catch (error) {
    console.error('获取系统状态失败:', error)
    return {
      cpu: 0,
      memory: 0,
      disk: 0,
      network: 'error'
    }
  }
}

/**
 * 获取最近活动记录
 */
export const getRecentActivities = async (): Promise<ActivityRecord[]> => {
  try {
    // 获取系统日志作为活动记录
    const response = await http.get('/api/system-logs', {
      params: { page: 0, size: 10, sortBy: 'createTime', sortDir: 'desc' }
    }).catch(() => ({ data: { data: { content: [] } } }))

    const logs = response.data?.data?.content || []
    
    // 将日志转换为活动记录格式
    const activities: ActivityRecord[] = logs.map((log: any, index: number) => ({
      id: log.id || index + 1,
      type: getActivityType(log.module || log.level),
      description: log.message || '系统活动',
      time: log.createTime || new Date().toISOString()
    }))

    // 如果没有日志数据，返回模拟数据
    if (activities.length === 0) {
      const now = new Date()
      return [
        {
          id: 1,
          type: 'user',
          description: '新用户注册成功',
          time: new Date(now.getTime() - 5 * 60000).toISOString()
        },
        {
          id: 2,
          type: 'task',
          description: '图片分类任务处理完成',
          time: new Date(now.getTime() - 10 * 60000).toISOString()
        },
        {
          id: 3,
          type: 'model',
          description: '模型训练任务启动',
          time: new Date(now.getTime() - 15 * 60000).toISOString()
        },
        {
          id: 4,
          type: 'system',
          description: '系统定时备份完成',
          time: new Date(now.getTime() - 20 * 60000).toISOString()
        }
      ]
    }

    return activities.slice(0, 10) // 最多返回10条记录
  } catch (error) {
    console.error('获取活动记录失败:', error)
    return []
  }
}

/**
 * 获取图表数据
 */
export const getChartData = async (period: string = '7d'): Promise<ChartData> => {
  try {
    // 生成模拟的任务趋势数据
    const days = period === '7d' ? 7 : period === '30d' ? 30 : 90
    const dates: string[] = []
    const values: number[] = []
    
    for (let i = days - 1; i >= 0; i--) {
      const date = new Date()
      date.setDate(date.getDate() - i)
      dates.push(date.toLocaleDateString())
      values.push(Math.floor(Math.random() * 50) + 10) // 10-60的随机值
    }

    // 生成模拟的准确率数据
    const categories = ['服装', '电子产品', '家居用品', '食品', '图书', '其他']
    const accuracyValues = categories.map(() => Math.floor(Math.random() * 20) + 80) // 80-100%

    return {
      taskTrend: { dates, values },
      accuracy: { categories, values: accuracyValues }
    }
  } catch (error) {
    console.error('获取图表数据失败:', error)
    return {
      taskTrend: { dates: [], values: [] },
      accuracy: { categories: [], values: [] }
    }
  }
}

/**
 * 根据日志模块或级别确定活动类型
 */
function getActivityType(module: string): ActivityRecord['type'] {
  const moduleMap: Record<string, ActivityRecord['type']> = {
    'USER': 'user',
    'TASK': 'task',
    'IMAGE': 'task',
    'SYSTEM': 'system',
    'AUTH': 'user',
    'ERROR': 'error',
    'WARN': 'error'
  }
  return moduleMap[module?.toUpperCase()] || 'system'
}

export default {
  getDashboardStats,
  getSystemStatus,
  getRecentActivities,
  getChartData
}