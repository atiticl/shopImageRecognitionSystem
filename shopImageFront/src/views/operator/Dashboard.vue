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
        <el-card title="每日处理量趋势">
          <div ref="dailyChart" class="chart-container"></div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :lg="12">
        <el-card title="分类结果分布">
          <div ref="categoryChart" class="chart-container"></div>
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
        <el-table-column prop="createTime" label="创建时间" />
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
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Upload,
  Loading,
  Check,
  TrendCharts
} from '@element-plus/icons-vue'
import { webSocketService, type ProgressMessage } from '@/utils/websocket'

const router = useRouter()

// 统计数据
const stats = ref({
  todayUploads: 156,
  processing: 23,
  completed: 1234,
  accuracy: 94.5
})

// 最近任务
const recentTasks = ref([
  {
    id: 1,
    taskName: '商品图片分类-批次001',
    imageCount: 150,
    status: 'COMPLETED',
    progress: 100,
    createTime: '2024-01-15 10:30:00'
  },
  {
    id: 2,
    taskName: '商品图片分类-批次002',
    imageCount: 89,
    status: 'PROCESSING',
    progress: 65,
    createTime: '2024-01-15 14:20:00'
  },
  {
    id: 3,
    taskName: '商品图片分类-批次003',
    imageCount: 200,
    status: 'PENDING',
    progress: 0,
    createTime: '2024-01-15 16:45:00'
  }
])

const dailyChart = ref()
const categoryChart = ref()

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
  router.push(`/operator/tasks/${task.id}`)
}

// 格式化准确率
const formatAccuracy = (accuracy: number | undefined) => {
  if (accuracy === undefined || accuracy === null) {
    return '-'
  }
  return `${accuracy.toFixed(2)}%`
}

// WebSocket 消息处理
const handleProgressMessage = (message: ProgressMessage) => {
  // 更新统计数据
  if (message.type === 'PROGRESS' || message.type === 'COMPLETE') {
    // 这里可以根据需要更新仪表板统计数据
    // 例如重新加载统计数据或直接更新相关字段
    loadDashboardStats()
  }
  
  // 更新最近任务列表中的任务状态
  const taskIndex = recentTasks.value.findIndex(task => task.id === message.taskId)
  if (taskIndex !== -1) {
    const task = recentTasks.value[taskIndex]
    
    if (message.type === 'PROGRESS') {
      task.status = 'PROCESSING'
      task.progress = Math.round((message.data.processedImages / message.data.totalImages) * 100)
    } else if (message.type === 'COMPLETE') {
      task.status = 'COMPLETED'
      task.progress = 100
    } else if (message.type === 'ERROR') {
      task.status = 'FAILED'
    }
  }
}

// 加载仪表板统计数据
const loadDashboardStats = async () => {
  try {
    // 这里应该调用实际的API来获取最新的统计数据
    // const response = await axios.get('/api/dashboard/stats')
    // stats.value = response.data.data
    console.log('加载仪表板统计数据')
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const initCharts = () => {
  // 这里可以集成 ECharts 或其他图表库
  // 由于没有安装图表库，这里只是占位
  console.log('初始化图表')
}

onMounted(() => {
  nextTick(() => {
    initCharts()
  })
  
  // 连接WebSocket并订阅进度更新
  try {
    webSocketService.connect()
    webSocketService.subscribeToProgress(handleProgressMessage)
  } catch (error) {
    console.error('Failed to initialize WebSocket:', error)
  }
})

onUnmounted(() => {
  // 断开WebSocket连接
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
  align-items: center;
  justify-content: center;
  background-color: #f8f9fa;
  border-radius: 8px;
  color: #666;
  font-size: 14px;
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
}
</style>