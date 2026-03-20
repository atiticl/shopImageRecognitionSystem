<template>
  <div class="admin-dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon user-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.totalUsers }}</div>
              <div class="stat-label">总用户数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon task-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.totalTasks }}</div>
              <div class="stat-label">总任务数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon image-icon">
              <el-icon><Picture /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.totalImages }}</div>
              <div class="stat-label">总图片数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon model-icon">
              <el-icon><Setting /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.totalModels }}</div>
              <div class="stat-label">分类模型数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 任务趋势图 -->
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>任务处理趋势</span>
              <el-select v-model="taskTrendPeriod" size="small" style="width: 100px">
                <el-option label="7天" value="7d" />
                <el-option label="30天" value="30d" />
                <el-option label="90天" value="90d" />
              </el-select>
            </div>
          </template>
          <div class="chart-container">
            <template v-if="taskTrendPoints.length">
              <svg class="line-chart-svg" viewBox="0 0 100 100" preserveAspectRatio="none">
                <polyline class="line-chart-path" :points="taskTrendPolyline" />
                <circle
                  v-for="point in taskTrendPoints"
                  :key="`${point.date}-${point.x}`"
                  class="line-chart-point"
                  :cx="point.x"
                  :cy="point.y"
                  r="1.5"
                />
              </svg>
              <div class="line-chart-footer">
                <span v-for="label in taskTrendLabels" :key="label">{{ label }}</span>
              </div>
            </template>
            <div v-else class="chart-empty">暂无趋势数据</div>
          </div>
        </el-card>
      </el-col>
      
      <!-- 分类准确率图 -->
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>分类准确率统计</span>
              <el-select v-model="accuracyPeriod" size="small" style="width: 100px">
                <el-option label="本周" value="week" />
                <el-option label="本月" value="month" />
                <el-option label="本季" value="quarter" />
              </el-select>
            </div>
          </template>
          <div class="chart-container">
            <div v-if="accuracyBars.length" class="bar-chart">
              <div v-for="bar in accuracyBars" :key="bar.category" class="bar-item">
                <div class="bar-track">
                  <div class="bar-fill" :style="{ height: `${bar.value}%` }">
                    <span class="bar-value">{{ bar.rawValue.toFixed(1) }}%</span>
                  </div>
                </div>
                <div class="bar-label">{{ bar.category }}</div>
              </div>
            </div>
            <div v-else class="chart-empty">暂无准确率数据</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 系统状态和最近活动 -->
    <el-row :gutter="20" class="bottom-row">
      <!-- 系统状态 -->
      <el-col :xs="24" :lg="8">
        <el-card class="status-card">
          <template #header>
            <span>系统状态</span>
          </template>
          
          <div class="status-list">
            <div class="status-item">
              <div class="status-label">
                <el-icon class="status-icon"><Monitor /></el-icon>
                CPU使用率
              </div>
              <div class="status-value">
                <el-progress 
                  :percentage="systemStatus.cpu" 
                  :color="getProgressColor(systemStatus.cpu)"
                  :stroke-width="8"
                />
              </div>
            </div>
            
            <div class="status-item">
              <div class="status-label">
                <el-icon class="status-icon"><Coin /></el-icon>
                内存使用率
              </div>
              <div class="status-value">
                <el-progress 
                  :percentage="systemStatus.memory" 
                  :color="getProgressColor(systemStatus.memory)"
                  :stroke-width="8"
                />
              </div>
            </div>
            
            <div class="status-item">
              <div class="status-label">
                <el-icon class="status-icon"><FolderOpened /></el-icon>
                磁盘使用率
              </div>
              <div class="status-value">
                <el-progress 
                  :percentage="systemStatus.disk" 
                  :color="getProgressColor(systemStatus.disk)"
                  :stroke-width="8"
                />
              </div>
            </div>
            
            <div class="status-item">
              <div class="status-label">
                <el-icon class="status-icon"><Connection /></el-icon>
                网络状态
              </div>
              <div class="status-value">
                <el-tag :type="systemStatus.network === 'normal' ? 'success' : 'danger'">
                  {{ systemStatus.network === 'normal' ? '正常' : '异常' }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <!-- 最近活动 -->
      <el-col :xs="24" :lg="8">
        <el-card class="activity-card">
          <template #header>
            <span>最近活动</span>
          </template>
          
          <div class="activity-list">
            <div 
              v-for="activity in recentActivities" 
              :key="activity.id"
              class="activity-item"
            >
              <div class="activity-icon">
                <el-icon :class="getActivityIconClass(activity.type)">
                  <component :is="getActivityIcon(activity.type)" />
                </el-icon>
              </div>
              <div class="activity-content">
                <div class="activity-text">{{ activity.description }}</div>
                <div class="activity-time">{{ formatTime(activity.time) }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <!-- 快速操作 -->
      <el-col :xs="24" :lg="8">
        <el-card class="quick-actions-card">
          <template #header>
            <span>快速操作</span>
          </template>
          
          <div class="quick-actions">
            <el-button 
              type="primary" 
              class="action-btn"
              @click="$router.push('/admin/users')"
            >
              <el-icon><User /></el-icon>
              用户管理
            </el-button>
            
            <el-button 
              type="success" 
              class="action-btn"
              @click="$router.push('/admin/categories')"
            >
              <el-icon><Collection /></el-icon>
              类别管理
            </el-button>
            
            <el-button 
              type="warning" 
              class="action-btn"
              @click="$router.push('/admin/models')"
            >
              <el-icon><Setting /></el-icon>
              模型管理
            </el-button>
            
            <el-button 
              type="info" 
              class="action-btn"
              @click="$router.push('/admin/logs')"
            >
              <el-icon><Document /></el-icon>
              系统日志
            </el-button>
            
            <el-button 
              type="danger" 
              class="action-btn"
              @click="showSystemMaintenance"
            >
              <el-icon><Tools /></el-icon>
              系统维护
            </el-button>
            
            <el-button 
              class="action-btn"
              @click="refreshDashboard"
            >
              <el-icon><Refresh /></el-icon>
              刷新数据
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User, Document, Picture, Setting, Monitor, Coin, 
  FolderOpened, Connection, Collection, Tools, Refresh
} from '@element-plus/icons-vue'
import { 
  getDashboardStats, 
  getSystemStatus, 
  getRecentActivities, 
  getChartData,
  type DashboardStats,
  type SystemStatus,
  type ActivityRecord,
  type ChartData
} from '@/api/dashboard'

// 统计数据
const stats = reactive<DashboardStats>({
  totalUsers: 0,
  totalTasks: 0,
  totalImages: 0,
  totalModels: 0
})

// 系统状态
const systemStatus = reactive<SystemStatus>({
  cpu: 0,
  memory: 0,
  disk: 0,
  network: 'normal'
})

// 图表时间范围
const taskTrendPeriod = ref('7d')
const accuracyPeriod = ref('week')

// 最近活动
const recentActivities = ref<ActivityRecord[]>([])

// 加载状态
const loading = ref(false)

const chartData = ref<ChartData>({
  taskTrend: {
    dates: [],
    values: []
  },
  accuracy: {
    categories: [],
    values: []
  }
})

const taskTrendPoints = computed(() => {
  const values = chartData.value.taskTrend.values
  const dates = chartData.value.taskTrend.dates
  if (!values.length) return []
  const maxValue = Math.max(...values, 1)
  return values.map((value, index) => ({
    x: values.length === 1 ? 50 : (index / (values.length - 1)) * 100,
    y: 86 - (value / maxValue) * 62,
    value,
    date: dates[index] || ''
  }))
})

const taskTrendPolyline = computed(() => {
  return taskTrendPoints.value.map(point => `${point.x},${point.y}`).join(' ')
})

const taskTrendLabels = computed(() => {
  const dates = chartData.value.taskTrend.dates
  if (dates.length <= 6) {
    return dates.map(formatChartDate)
  }
  const step = Math.ceil(dates.length / 6)
  const sampled = dates.filter((_, index) => index % step === 0).map(formatChartDate)
  const lastLabel = formatChartDate(dates[dates.length - 1])
  if (sampled[sampled.length - 1] !== lastLabel) {
    sampled[sampled.length - 1] = lastLabel
  }
  return sampled.slice(0, 6)
})

const accuracyBars = computed(() => {
  const categories = chartData.value.accuracy.categories || []
  const values = chartData.value.accuracy.values || []
  return categories.map((category, index) => {
    const rawValue = Number(values[index] || 0)
    return {
      category,
      rawValue,
      value: Math.max(0, Math.min(100, rawValue))
    }
  })
})

// 获取进度条颜色
const getProgressColor = (percentage: number) => {
  if (percentage < 50) return '#67c23a'
  if (percentage < 80) return '#e6a23c'
  return '#f56c6c'
}

// 获取活动图标
const getActivityIcon = (type: string) => {
  const iconMap: Record<string, any> = {
    'user': User,
    'task': Document,
    'model': Setting,
    'system': Monitor,
    'error': Tools
  }
  return iconMap[type] || Document
}

// 获取活动图标样式类
const getActivityIconClass = (type: string) => {
  const classMap: Record<string, string> = {
    'user': 'user-activity',
    'task': 'task-activity',
    'model': 'model-activity',
    'system': 'system-activity',
    'error': 'error-activity'
  }
  return classMap[type] || 'default-activity'
}

// 格式化时间
const formatTime = (time: string) => {
  const now = new Date()
  const activityTime = new Date(time)
  const diff = now.getTime() - activityTime.getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return activityTime.toLocaleDateString()
}

const formatChartDate = (dateText: string) => {
  if (!dateText) return ''
  const date = new Date(dateText)
  if (Number.isNaN(date.getTime())) return dateText
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${month}-${day}`
}

// 刷新仪表板
const refreshDashboard = async () => {
  ElMessage.info('正在刷新数据...')
  await loadDashboardData()
  ElMessage.success('数据已刷新')
}

// 显示系统维护对话框
const showSystemMaintenance = async () => {
  try {
    await ElMessageBox.confirm(
      '系统维护将会影响正在进行的任务，确定要继续吗？',
      '系统维护',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    ElMessage.info('系统维护功能开发中...')
  } catch (error) {
    // 用户取消
  }
}

// 加载仪表板数据
const loadDashboardData = async () => {
  if (loading.value) return
  
  loading.value = true
  try {
    // 并行加载所有数据
    const [statsData, statusData, activitiesData] = await Promise.all([
      getDashboardStats(),
      getSystemStatus(),
      getRecentActivities()
    ])
    
    // 更新统计数据
    Object.assign(stats, statsData)
    
    // 更新系统状态
    Object.assign(systemStatus, statusData)
    
    // 更新活动记录
    recentActivities.value = activitiesData
    
    console.log('仪表板数据加载完成:', { stats: statsData, status: statusData, activities: activitiesData })
  } catch (error) {
    console.error('加载仪表板数据失败:', error)
    
    // 如果是认证错误，提示用户重新登录
    if (error.response?.status === 401 || error.response?.status === 403) {
      ElMessage.error('登录已过期或权限不足，请重新登录')
      // 可以在这里跳转到登录页面
      // router.push('/login')
    } else {
      ElMessage.error('加载数据失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

const getAccuracyPeriod = () => {
  if (accuracyPeriod.value === 'month') return '30d'
  if (accuracyPeriod.value === 'quarter') return '90d'
  return '7d'
}

const loadChartData = async () => {
  try {
    const [taskTrendData, accuracyData] = await Promise.all([
      getChartData(taskTrendPeriod.value),
      getChartData(getAccuracyPeriod())
    ])
    chartData.value = {
      taskTrend: taskTrendData.taskTrend,
      accuracy: accuracyData.accuracy
    }
  } catch (error) {
    console.error('更新图表数据失败:', error)
    chartData.value = {
      taskTrend: { dates: [], values: [] },
      accuracy: { categories: [], values: [] }
    }
  }
}

// 监听时间范围变化，重新加载图表数据
watch([taskTrendPeriod, accuracyPeriod], loadChartData)

onMounted(() => {
  loadDashboardData()
  loadChartData()
})
</script>

<style scoped>
.admin-dashboard {
  padding: 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 50%, #cbd5e1 100%);
  min-height: 100%;
  position: relative;
}

.admin-dashboard::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(139, 92, 246, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 40% 40%, rgba(16, 185, 129, 0.02) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.stats-row, .charts-row, .bottom-row {
  margin-bottom: 32px;
  position: relative;
  z-index: 1;
}

.stat-card {
  height: 140px;
  border-radius: 20px;
  border: none;
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: relative;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6, #06b6d4, #10b981);
  background-size: 300% 100%;
  animation: gradientShift 3s ease-in-out infinite;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.stat-card:hover {
  transform: translateY(-8px);
  box-shadow: 
    0 20px 40px rgba(0, 0, 0, 0.12),
    0 0 0 1px rgba(255, 255, 255, 0.9);
}

:deep(.stat-card .el-card__body) {
  padding: 24px;
  height: 100%;
}

.stat-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stat-icon {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  font-size: 28px;
  color: white;
  position: relative;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.stat-card:hover .stat-icon {
  transform: scale(1.1) rotate(5deg);
}

.user-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.task-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.image-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.model-icon {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 36px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1;
  margin-bottom: 8px;
  background: linear-gradient(135deg, #1e293b 0%, #475569 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  transition: all 0.3s ease;
}

.stat-card:hover .stat-number {
  transform: scale(1.05);
}

.stat-label {
  font-size: 16px;
  color: #64748b;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.chart-card, .status-card, .activity-card, .quick-actions-card {
  height: 480px;
  border-radius: 24px;
  border: none;
  box-shadow: 
    0 12px 40px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.chart-card:hover, .status-card:hover, .activity-card:hover, .quick-actions-card:hover {
  transform: translateY(-4px);
  box-shadow: 
    0 20px 60px rgba(0, 0, 0, 0.12),
    0 0 0 1px rgba(255, 255, 255, 0.9);
}

:deep(.chart-card .el-card__header),
:deep(.status-card .el-card__header),
:deep(.activity-card .el-card__header),
:deep(.quick-actions-card .el-card__header) {
  padding: 24px 24px 0;
  border-bottom: none;
}

:deep(.chart-card .el-card__body),
:deep(.status-card .el-card__body),
:deep(.activity-card .el-card__body),
:deep(.quick-actions-card .el-card__body) {
  padding: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 20px;
}

.chart-container {
  height: 360px;
  width: 100%;
  border-radius: 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  padding: 16px 18px 14px;
  color: #64748b;
  font-size: 16px;
  font-weight: 500;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.line-chart-svg {
  width: 100%;
  height: 300px;
}

.line-chart-path {
  fill: none;
  stroke: #3b82f6;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.line-chart-point {
  fill: #3b82f6;
  stroke: #ffffff;
  stroke-width: 0.7;
}

.line-chart-footer {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
}

.bar-chart {
  width: 100%;
  height: 100%;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(64px, 1fr));
  gap: 12px;
}

.bar-item {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.bar-track {
  flex: 1;
  width: 100%;
  border-radius: 10px;
  background: rgba(148, 163, 184, 0.2);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: flex-end;
}

.bar-fill {
  width: 100%;
  border-radius: 10px 10px 0 0;
  background: linear-gradient(180deg, #22d3ee 0%, #3b82f6 100%);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  min-height: 2px;
  transition: height 0.35s ease;
}

.bar-value {
  margin-top: 6px;
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
}

.bar-label {
  margin-top: 8px;
  max-width: 90px;
  text-align: center;
  color: #475569;
  font-size: 12px;
  line-height: 1.2;
  word-break: break-word;
}

.chart-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}

.status-list, .activity-list {
  height: 380px;
  overflow-y: auto;
  padding-right: 8px;
}

.status-list::-webkit-scrollbar,
.activity-list::-webkit-scrollbar {
  width: 6px;
}

.status-list::-webkit-scrollbar-track,
.activity-list::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}

.status-list::-webkit-scrollbar-thumb,
.activity-list::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.status-list::-webkit-scrollbar-thumb:hover,
.activity-list::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

.status-item {
  margin-bottom: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  transition: all 0.3s ease;
}

.status-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: rgba(59, 130, 246, 0.2);
}

.status-label {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  font-size: 15px;
  color: #475569;
  font-weight: 600;
}

.status-icon {
  margin-right: 10px;
  color: #3b82f6;
  font-size: 18px;
}

:deep(.status-item .el-progress) {
  margin-top: 8px;
}

:deep(.status-item .el-progress__text) {
  font-weight: 600;
  color: #1e293b;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 20px;
  padding: 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  transition: all 0.3s ease;
}

.activity-item:hover {
  transform: translateX(4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  border-color: rgba(59, 130, 246, 0.2);
}

.activity-item:last-child {
  margin-bottom: 0;
}

.activity-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 18px;
  color: white;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.user-activity {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

.task-activity {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.model-activity {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.system-activity {
  background: linear-gradient(135deg, #6b7280 0%, #4b5563 100%);
}

.error-activity {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
}

.activity-content {
  flex: 1;
}

.activity-text {
  font-size: 15px;
  color: #1e293b;
  margin-bottom: 6px;
  font-weight: 500;
  line-height: 1.4;
}

.activity-time {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  height: 380px;
  align-content: start;
}

:deep(.action-btn) {
  height: 56px;
  border-radius: 16px;
  font-weight: 600;
  font-size: 15px;
  letter-spacing: 0.5px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

:deep(.action-btn:hover) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

:deep(.action-btn.el-button--primary) {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

:deep(.action-btn.el-button--success) {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

:deep(.action-btn.el-button--warning) {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

:deep(.action-btn.el-button--info) {
  background: linear-gradient(135deg, #6b7280 0%, #4b5563 100%);
}

:deep(.action-btn.el-button--danger) {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
}

:deep(.action-btn.el-button--default) {
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  color: #1e293b;
  border: 1px solid rgba(226, 232, 240, 0.8);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .admin-dashboard {
    padding: 16px;
  }
  
  .quick-actions {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}

@media (max-width: 768px) {
  .admin-dashboard {
    padding: 12px;
  }
  
  .stats-row .el-col {
    margin-bottom: 16px;
  }
  
  .charts-row .el-col {
    margin-bottom: 20px;
  }
  
  .bottom-row .el-col {
    margin-bottom: 20px;
  }
  
  .stat-card {
    height: 120px;
  }
  
  .stat-icon {
    width: 60px;
    height: 60px;
    font-size: 24px;
  }
  
  .stat-number {
    font-size: 28px;
  }
  
  .stat-label {
    font-size: 14px;
  }
  
  .chart-card, .status-card, .activity-card, .quick-actions-card {
    height: auto;
    min-height: 300px;
  }
  
  .chart-container {
    height: 250px;
    padding: 12px;
  }

  .line-chart-svg {
    height: 190px;
  }

  .line-chart-footer {
    font-size: 10px;
  }

  .bar-label {
    font-size: 11px;
  }
  
  .status-list, .activity-list {
    height: auto;
    max-height: 300px;
  }
  
  :deep(.action-btn) {
    height: 48px;
    font-size: 14px;
  }
}

/* 加载动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stat-card, .chart-card, .status-card, .activity-card, .quick-actions-card {
  animation: fadeInUp 0.6s ease-out;
}

.stat-card:nth-child(1) { animation-delay: 0.1s; }
.stat-card:nth-child(2) { animation-delay: 0.2s; }
.stat-card:nth-child(3) { animation-delay: 0.3s; }
.stat-card:nth-child(4) { animation-delay: 0.4s; }
</style>
