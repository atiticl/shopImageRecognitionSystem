<template>
  <div class="operator-dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon upload">
              <el-icon size="32"><Upload /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.todayUploads }}</div>
              <div class="stat-label">今日上传</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon processing">
              <el-icon size="32"><Loading /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.processing }}</div>
              <div class="stat-label">处理中</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon completed">
              <el-icon size="32"><Check /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.completed }}</div>
              <div class="stat-label">已完成</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon accuracy">
              <el-icon size="32"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ formatAccuracy(stats.accuracy) }}</div>
              <div class="stat-label">准确率</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>
            <div class="chart-header">
              <div class="chart-title-wrap">
                <span class="chart-title">每日处理量趋势</span>
                <span class="chart-desc">统计范围内每天新建任务数量</span>
              </div>
              <el-select v-model="chartPeriod" size="small" class="chart-period-select">
                <el-option label="近7天" value="7d" />
                <el-option label="近30天" value="30d" />
                <el-option label="近90天" value="90d" />
              </el-select>
            </div>
          </template>
          <div class="chart-container">
            <template v-if="trendPoints.length">
              <svg class="line-chart-svg" viewBox="0 0 100 100" preserveAspectRatio="none">
                <polyline class="line-chart-path" :points="trendPath" />
                <circle
                  v-for="point in trendPoints"
                  :key="`${point.date}-${point.x}`"
                  class="line-chart-point"
                  :cx="point.x"
                  :cy="point.y"
                  r="1.5"
                />
              </svg>
              <div class="line-chart-footer">
                <span v-for="label in trendLabels" :key="label">{{ label }}</span>
              </div>
            </template>
            <div v-else class="chart-empty">
              <div>当前时间范围暂无任务数据</div>
              <div class="chart-empty-tip">可切换近30天或近90天查看历史趋势</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>
            <div class="chart-header">
              <div class="chart-title-wrap">
                <span class="chart-title">分类结果分布</span>
                <span class="chart-desc">统计范围内识别结果Top分类数量</span>
              </div>
            </div>
          </template>
          <div class="chart-container">
            <div v-if="categoryBars.length" class="bar-chart">
              <div v-for="bar in categoryBars" :key="bar.category" class="bar-item">
                <div class="bar-track">
                  <div class="bar-fill" :style="{ height: `${bar.percent}%` }">
                    <span class="bar-value">{{ bar.value }}</span>
                  </div>
                </div>
                <div class="bar-label">{{ bar.category }}</div>
              </div>
            </div>
            <div v-else class="chart-empty">
              <div>当前时间范围暂无分类结果</div>
              <div class="chart-empty-tip">请先上传图片并完成分类任务</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近任务 -->
    <el-card title="最近任务" class="recent-tasks">
      <el-table :data="recentTasks" style="width: 100%">
        <el-table-column prop="taskName" label="任务名称" />
        <el-table-column prop="imageCount" label="图片数量" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度">
          <template #default="{ row }">
            <el-progress :percentage="row.progress" :status="row.progress === 100 ? 'success' : ''" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewTask(row)">
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Upload,
  Loading,
  Check,
  TrendCharts
} from '@element-plus/icons-vue'
import { webSocketService, type ProgressMessage } from '@/utils/websocket'
import {
  getOperatorOverview,
  getOperatorChartData,
  type OperatorDashboardStats,
  type OperatorRecentTask,
  type OperatorChartData
} from '@/api/dashboard'

const router = useRouter()

const stats = ref<OperatorDashboardStats>({
  todayUploads: 0,
  processing: 0,
  completed: 0,
  accuracy: 0
})

const recentTasks = ref<OperatorRecentTask[]>([])
const chartPeriod = ref<'7d' | '30d' | '90d'>('30d')

const chartData = ref<OperatorChartData>({
  dailyTrend: {
    dates: [],
    values: []
  },
  categoryDistribution: {
    categories: [],
    values: []
  }
})

const trendPoints = computed(() => {
  const values = chartData.value.dailyTrend.values
  const dates = chartData.value.dailyTrend.dates
  if (!values.length) return []
  const maxValue = Math.max(...values, 1)
  return values.map((value, index) => ({
    x: values.length === 1 ? 50 : (index / (values.length - 1)) * 100,
    y: 86 - (value / maxValue) * 62,
    value,
    date: dates[index] || ''
  }))
})

const trendPath = computed(() => trendPoints.value.map(point => `${point.x},${point.y}`).join(' '))

const trendLabels = computed(() => {
  const dates = chartData.value.dailyTrend.dates
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

const categoryBars = computed(() => {
  const categories = chartData.value.categoryDistribution.categories || []
  const values = chartData.value.categoryDistribution.values || []
  const maxValue = Math.max(...values, 1)
  return categories.map((category, index) => {
    const value = Number(values[index] || 0)
    return {
      category,
      value,
      percent: Math.max(3, Math.round((value / maxValue) * 100))
    }
  })
})

const getStatusType = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': 'info',
    'PROCESSING': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': '待处理',
    'PROCESSING': '处理中',
    'COMPLETED': '已完成',
    'FAILED': '失败'
  }
  return statusMap[status] || '未知'
}

const viewTask = (task: any) => {
  router.push('/operator/tasks')
}

// 格式化准确率
const formatAccuracy = (accuracy: number | undefined) => {
  if (accuracy === undefined || accuracy === null) {
    return '-'
  }
  return `${accuracy.toFixed(2)}%`
}

const formatTime = (time: string | undefined) => {
  if (!time) return '-'
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return time
  return date.toLocaleString()
}

const formatChartDate = (dateText: string) => {
  if (!dateText) return ''
  const date = new Date(dateText)
  if (Number.isNaN(date.getTime())) return dateText
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${month}-${day}`
}

// WebSocket 消息处理
const handleProgressMessage = (message: ProgressMessage) => {
  if (message.type === 'PROGRESS' || message.type === 'COMPLETE') {
    loadOperatorDashboard()
  }
  
  const taskIndex = recentTasks.value.findIndex(task => task.id === message.taskId)
  if (taskIndex !== -1) {
    const task = recentTasks.value[taskIndex]
    
    if (message.type === 'PROGRESS') {
      task.status = 'PROCESSING'
      task.processedImages = message.data.processedImages
      task.progress = Math.round((message.data.processedImages / Math.max(message.data.totalImages, 1)) * 100)
    } else if (message.type === 'COMPLETE') {
      task.status = 'COMPLETED'
      task.processedImages = task.totalImages
      task.progress = 100
    } else if (message.type === 'ERROR') {
      task.status = 'FAILED'
    }
  }
}

const loadOperatorDashboard = async () => {
  try {
    const [overview, charts] = await Promise.all([
      getOperatorOverview(),
      getOperatorChartData(chartPeriod.value)
    ])
    stats.value = overview.stats
    recentTasks.value = overview.recentTasks
    chartData.value = charts
  } catch (error) {
    console.error('加载运营工作台数据失败:', error)
    ElMessage.error('加载运营工作台数据失败')
  }
}

watch(chartPeriod, () => {
  loadOperatorDashboard()
})

onMounted(() => {
  loadOperatorDashboard()
  try {
    webSocketService.connect()
    webSocketService.subscribeToProgress(handleProgressMessage)
  } catch (error) {
    console.error('Failed to initialize WebSocket:', error)
  }
})

onUnmounted(() => {
  try {
    webSocketService.disconnect()
  } catch (error) {
    console.error('Failed to disconnect WebSocket:', error)
  }
})
</script>

<style scoped>
.operator-dashboard {
  padding: 0;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  height: 120px;
}

.stat-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  color: white;
}

.stat-icon.upload {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.processing {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.completed {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.accuracy {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.charts-row {
  margin-bottom: 20px;
}

.chart-container {
  height: 300px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 12px 16px;
  color: #666;
  font-size: 14px;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.chart-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.chart-desc {
  font-size: 12px;
  color: #64748b;
}

.chart-period-select {
  width: 100px;
}

.line-chart-svg {
  width: 100%;
  height: 250px;
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
  grid-template-columns: repeat(auto-fit, minmax(56px, 1fr));
  gap: 10px;
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
  border-radius: 8px;
  background: rgba(148, 163, 184, 0.2);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: flex-end;
}

.bar-fill {
  width: 100%;
  border-radius: 8px 8px 0 0;
  background: linear-gradient(180deg, #22d3ee 0%, #3b82f6 100%);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  min-height: 3px;
}

.bar-value {
  margin-top: 4px;
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
  flex-direction: column;
  gap: 8px;
}

.chart-empty-tip {
  font-size: 12px;
  color: #a0aec0;
}

.recent-tasks {
  margin-bottom: 20px;
}

:deep(.el-card__header) {
  padding: 18px 20px;
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-card__body) {
  padding: 20px;
}

@media (max-width: 768px) {
  .stats-row .el-col {
    margin-bottom: 16px;
  }
  
  .charts-row .el-col {
    margin-bottom: 16px;
  }

  .line-chart-svg {
    height: 190px;
  }

  .line-chart-footer {
    font-size: 10px;
  }

  .chart-header {
    align-items: flex-start;
  }
}
</style>
