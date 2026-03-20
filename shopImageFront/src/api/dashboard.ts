import { http } from '@/utils/request'

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
    const response = await http.get<ApiResponse<DashboardStats>>('/api/dashboard/stats')
    const data = response.data || {}
    return {
      totalUsers: Number(data.totalUsers || 0),
      totalTasks: Number(data.totalTasks || 0),
      totalImages: Number(data.totalImages || 0),
      totalModels: Number(data.totalModels || 0)
    }
  } catch (error) {
    console.error('获取仪表板统计数据失败:', error)
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
    const response = await http.get<ApiResponse<SystemStatus>>('/api/dashboard/system-status')
    const data = response.data || {}
    return {
      cpu: Number(data.cpu || 0),
      memory: Number(data.memory || 0),
      disk: Number(data.disk || 0),
      network: data.network === 'normal' ? 'normal' : 'error'
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
    const response = await http.get<ApiResponse<ActivityRecord[]>>('/api/dashboard/recent-activities', {
      params: { size: 10 }
    })
    const activities = response.data || []
    return activities.slice(0, 10)
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
    const response = await http.get<ApiResponse<ChartData>>('/api/dashboard/chart-data', {
      params: { period }
    })
    const data = response.data || {
      taskTrend: { dates: [], values: [] },
      accuracy: { categories: [], values: [] }
    }
    return {
      taskTrend: {
        dates: data.taskTrend?.dates || [],
        values: data.taskTrend?.values || []
      },
      accuracy: {
        categories: data.accuracy?.categories || [],
        values: data.accuracy?.values || []
      }
    }
  } catch (error) {
    console.error('获取图表数据失败:', error)
    return {
      taskTrend: { dates: [], values: [] },
      accuracy: { categories: [], values: [] }
    }
  }
}

export interface OperatorDashboardStats {
  todayUploads: number
  processing: number
  completed: number
  accuracy: number
}

export interface OperatorRecentTask {
  id: number
  taskName: string
  imageCount: number
  totalImages: number
  processedImages: number
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  progress: number
  createTime: string
}

export interface OperatorOverviewData {
  stats: OperatorDashboardStats
  recentTasks: OperatorRecentTask[]
}

export interface OperatorChartData {
  dailyTrend: {
    dates: string[]
    values: number[]
  }
  categoryDistribution: {
    categories: string[]
    values: number[]
  }
}

export const getOperatorOverview = async (): Promise<OperatorOverviewData> => {
  try {
    const response = await http.get<ApiResponse<OperatorOverviewData>>('/api/dashboard/operator/overview', {
      params: { recentSize: 5 }
    })
    const data = response.data || { stats: {}, recentTasks: [] }
    return {
      stats: {
        todayUploads: Number(data.stats?.todayUploads || 0),
        processing: Number(data.stats?.processing || 0),
        completed: Number(data.stats?.completed || 0),
        accuracy: Number(data.stats?.accuracy || 0)
      },
      recentTasks: (data.recentTasks || []).map((task: any) => ({
        id: Number(task.id || 0),
        taskName: task.taskName || '未命名任务',
        imageCount: Number(task.imageCount || task.totalImages || 0),
        totalImages: Number(task.totalImages || task.imageCount || 0),
        processedImages: Number(task.processedImages || 0),
        status: task.status || 'PENDING',
        progress: Number(task.progress || 0),
        createTime: task.createTime || ''
      }))
    }
  } catch (error) {
    console.error('获取运营工作台概览失败:', error)
    return {
      stats: {
        todayUploads: 0,
        processing: 0,
        completed: 0,
        accuracy: 0
      },
      recentTasks: []
    }
  }
}

export const getOperatorChartData = async (period: string = '7d'): Promise<OperatorChartData> => {
  try {
    const response = await http.get<ApiResponse<OperatorChartData>>('/api/dashboard/operator/chart-data', {
      params: { period }
    })
    const data = response.data || {
      dailyTrend: { dates: [], values: [] },
      categoryDistribution: { categories: [], values: [] }
    }
    return {
      dailyTrend: {
        dates: data.dailyTrend?.dates || [],
        values: data.dailyTrend?.values || []
      },
      categoryDistribution: {
        categories: data.categoryDistribution?.categories || [],
        values: data.categoryDistribution?.values || []
      }
    }
  } catch (error) {
    console.error('获取运营工作台图表数据失败:', error)
    return {
      dailyTrend: { dates: [], values: [] },
      categoryDistribution: { categories: [], values: [] }
    }
  }
}

export default {
  getDashboardStats,
  getSystemStatus,
  getRecentActivities,
  getChartData,
  getOperatorOverview,
  getOperatorChartData
}
