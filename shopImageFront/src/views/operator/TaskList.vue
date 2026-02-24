<template>
  <div class="task-list">
    <!-- 筛选条件 -->
    <el-card class="filter-card">
      <template #header>
        <div class="card-header">
          <span>筛选条件</span>
          <el-button type="primary" @click="searchTasks">搜索</el-button>
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="任务状态">
            <el-select v-model="filters.status" placeholder="请选择状态" clearable>
              <el-option label="全部" value="" />
              <el-option label="待处理" value="PENDING" />
              <el-option label="处理中" value="PROCESSING" />
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="已失败" value="FAILED" />
            </el-select>
          </el-form-item>
        </el-col>
        
        <el-col :span="8">
          <el-form-item label="创建时间">
            <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
        </el-col>
        
        <el-col :span="6">
          <el-form-item label="任务名称">
            <el-input 
              v-model="filters.taskName" 
              placeholder="请输入任务名称"
              clearable
              @keyup.enter="searchTasks"
            />
          </el-form-item>
        </el-col>
        
        <el-col :span="4">
          <el-form-item>
            <el-button @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-card>

    <!-- 任务列表 -->
    <el-card class="task-card">
      <template #header>
        <div class="card-header">
          <span>任务列表</span>
          <span class="task-count">共 {{ pagination.total }} 个任务</span>
        </div>
      </template>
      
      <div v-loading="loading" class="task-grid">
        <div 
          v-for="task in tasks" 
          :key="task.id" 
          class="task-item"
          @click="viewTaskDetail(task)"
        >
          <div class="task-header">
            <div class="task-name">{{ task.taskName }}</div>
            <el-tag :type="getStatusType(task.status)">{{ getStatusText(task.status) }}</el-tag>
          </div>
          
          <div class="task-info">
            <div class="info-item">
              <span class="label">图片数量:</span>
              <span class="value">{{ task.totalImages }}</span>
            </div>
            <div class="info-item">
              <span class="label">已处理:</span>
              <span class="value">{{ task.processedImages || 0 }}</span>
            </div>
            <div class="info-item">
              <span class="label">准确率:</span>
              <span class="value">{{ formatAccuracy(task.accuracy) }}</span>
            </div>
            <div class="info-item">
              <span class="label">分类模型:</span>
              <span class="value">{{ task.modelName || '未指定' }}</span>
            </div>
            <div class="info-item">
              <span class="label">创建时间:</span>
              <span class="value">{{ formatTime(task.createdAt) }}</span>
            </div>
          </div>
          
          <div v-if="task.status === 'PROCESSING'" class="task-progress">
            <div class="progress-info">
              <span>处理进度</span>
              <span class="percentage">{{ Math.round((task.processedImages / task.totalImages) * 100) }}%</span>
            </div>
            <el-progress 
              :percentage="Math.round((task.processedImages / task.totalImages) * 100)"
              :stroke-width="8"
            />
          </div>
          
          <div v-if="task.description" class="task-description">
            <span class="label">描述:</span>
            {{ task.description }}
          </div>
          
          <div class="task-actions" @click.stop>
            <el-button 
              v-if="task.status === 'COMPLETED'" 
              type="primary" 
              size="small"
              @click="viewResults(task)"
            >
              查看结果
            </el-button>
            <el-button 
              v-if="task.status === 'PENDING'" 
              type="success" 
              size="small"
              @click="startTask(task)"
            >
              开始任务
            </el-button>
            <el-button 
              v-if="task.status === 'PROCESSING'" 
              type="warning" 
              size="small"
              @click="pauseTask(task)"
            >
              暂停任务
            </el-button>
            <el-button 
              type="danger" 
              size="small"
              @click="deleteTask(task)"
            >
              删除
            </el-button>
          </div>
        </div>
        
        <div v-if="!loading && tasks.length === 0" class="empty-state">
          <el-empty description="暂无任务数据" />
        </div>
      </div>
      
      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 任务详情对话框 -->
    <el-dialog v-model="detailDialogVisible" :title="currentTask?.taskName" width="800px">
      <div v-if="currentTask" class="task-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务ID">{{ currentTask.id }}</el-descriptions-item>
          <el-descriptions-item label="任务状态">
            <el-tag :type="getStatusType(currentTask.status)">
              {{ getStatusText(currentTask.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="图片数量">{{ currentTask.totalImages }}</el-descriptions-item>
          <el-descriptions-item label="已处理">{{ currentTask.processedImages }}</el-descriptions-item>
          <el-descriptions-item label="准确率">{{ formatAccuracy(currentTask.accuracy) }}</el-descriptions-item>
          <el-descriptions-item label="成功数量">{{ currentTask.successCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="失败数量">{{ currentTask.failedCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="分类模型">{{ currentTask.modelName || '未指定' }}</el-descriptions-item>
          <el-descriptions-item label="模型版本">{{ currentTask.modelVersion || '未指定' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(currentTask.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ currentTask.updatedAt ? formatTime(currentTask.updatedAt) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="处理耗时" :span="2">
            {{ currentTask.processingTime || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="任务描述" :span="2">
            {{ currentTask.description || '-' }}
          </el-descriptions-item>
        </el-descriptions>
        
        <div v-if="currentTask.status === 'PROCESSING'" class="processing-info">
          <h4>处理进度</h4>
          <el-progress 
            :percentage="Math.round((currentTask.processedImages / currentTask.totalImages) * 100)"
            :stroke-width="8"
          />
          <p class="progress-text">
            已处理 {{ currentTask.processedImages }} / {{ currentTask.totalImages }} 张图片
          </p>
        </div>
        
        <div v-if="currentTask.status === 'COMPLETED'" class="completion-stats">
          <h4>完成统计</h4>
          <el-row :gutter="16">
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-number">{{ currentTask.successCount || 0 }}</div>
                <div class="stat-label">成功</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-number">{{ currentTask.failedCount || 0 }}</div>
                <div class="stat-label">失败</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-number">{{ currentTask.accuracy || 0 }}%</div>
                <div class="stat-label">准确率</div>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button 
          type="primary" 
          @click="viewResults(currentTask)"
          :disabled="!currentTask || currentTask.processedImages === 0"
        >
          查看结果
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { webSocketService, type ProgressMessage } from '@/utils/websocket'

const router = useRouter()
const userStore = useUserStore()

// 筛选条件
const filters = reactive({
  status: '',
  dateRange: null as [string, string] | null,
  taskName: ''
})

// 分页
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 任务列表
const tasks = ref<any[]>([])
const loading = ref(false)

// 任务详情对话框
const detailDialogVisible = ref(false)
const currentTask = ref<any>(null)

// 获取状态类型
const getStatusType = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': 'info',
    'PROCESSING': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': '待处理',
    'PROCESSING': '处理中',
    'COMPLETED': '已完成',
    'FAILED': '失败'
  }
  return statusMap[status] || '未知'
}

// 格式化时间
const formatTime = (time: string) => {
  return new Date(time).toLocaleString()
}

// 搜索任务
const searchTasks = () => {
  console.log('搜索条件:', filters)
  loadTasks()
}

// 重置筛选条件
const resetFilters = () => {
  Object.assign(filters, {
    status: '',
    dateRange: null,
    taskName: ''
  })
  loadTasks()
}

// 加载任务列表
const loadTasks = async () => {
  try {
    loading.value = true
    const params: any = {
      page: pagination.page,
      size: pagination.size
    }
    
    // 只添加有值的参数，避免传递undefined
    if (filters.status) {
      params.status = filters.status
    }
    if (filters.taskName) {
      params.taskName = filters.taskName
    }
    if (filters.dateRange?.[0]) {
      params.startDate = filters.dateRange[0]
    }
    if (filters.dateRange?.[1]) {
      params.endDate = filters.dateRange[1]
    }
    
    const response = await axios.get('/api/classification-tasks', {
      params,
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (response.data.success) {
      const data = response.data.data
      tasks.value = data.list || data.records || data
      pagination.total = data.total || (Array.isArray(data) ? data.length : 0)
    } else {
      ElMessage.error(response.data.message || '加载任务列表失败')
    }
  } catch (error: any) {
    console.error('加载任务列表失败:', error)
    ElMessage.error('加载任务列表失败')
  } finally {
    loading.value = false
  }
}

// 查看任务详情
const viewTaskDetail = (task: any) => {
  currentTask.value = task
  detailDialogVisible.value = true
}

// 查看结果
const viewResults = (task: any) => {
  router.push({
    path: '/operator/results',
    query: { taskId: task.id }
  })
}

// 开始任务
const startTask = async (task: any) => {
  try {
    await ElMessageBox.confirm('确定要开始这个任务吗？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    
    const response = await axios.post(`/api/classification-tasks/${task.id}/start`, {}, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (response.data.success) {
      ElMessage.success('任务已开始')
      loadTasks() // 重新加载任务列表
    } else {
      ElMessage.error(response.data.message || '开始任务失败')
    }
  } catch (error: any) {
    if (error.response) {
      ElMessage.error(error.response.data.message || '开始任务失败')
    } else if (error.message !== 'cancel') {
      ElMessage.error('开始任务失败')
    }
  }
}

// 暂停任务
const pauseTask = async (task: any) => {
  try {
    await ElMessageBox.confirm('确定要暂停这个任务吗？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const response = await axios.post(`/api/classification-tasks/${task.id}/pause`, {}, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (response.data.success) {
      ElMessage.success('任务已暂停')
      loadTasks() // 重新加载任务列表
    } else {
      ElMessage.error(response.data.message || '暂停任务失败')
    }
  } catch (error: any) {
    if (error.response) {
      ElMessage.error(error.response.data.message || '暂停任务失败')
    } else if (error.message !== 'cancel') {
      ElMessage.error('暂停任务失败')
    }
  }
}

// 删除任务
const deleteTask = async (task: any) => {
  try {
    await ElMessageBox.confirm('确定要删除这个任务吗？删除后无法恢复。', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })
    
    const response = await axios.delete(`/api/classification-tasks/${task.id}`, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (response.data.success) {
      ElMessage.success('任务已删除')
      loadTasks() // 重新加载任务列表
    } else {
      ElMessage.error(response.data.message || '删除任务失败')
    }
  } catch (error: any) {
    if (error.response) {
      ElMessage.error(error.response.data.message || '删除任务失败')
    } else if (error.message !== 'cancel') {
      ElMessage.error('删除任务失败')
    }
  }
}

// 分页处理
const handleSizeChange = (size: number) => {
  pagination.size = size
  loadTasks()
}

// 格式化准确率
const formatAccuracy = (accuracy: number | undefined) => {
  if (accuracy === undefined || accuracy === null) {
    return '-'
  }
  return `${accuracy.toFixed(2)}%`
}

// WebSocket 消息处理
const handleProgressMessage = (message: any) => {
  console.log('收到WebSocket消息:', message)
  
  // 处理不同的消息格式
  let taskId: number
  let messageType: string
  let messageData: any = {}
  
  // 检查消息格式
  if (message.taskId) {
    taskId = Number(message.taskId)
    messageType = message.type || 'PROGRESS'
    
    // 如果有data字段，使用data中的数据
    if (message.data) {
      messageData = message.data
    } else {
      // 否则直接使用消息中的字段
      messageData = {
        processedImages: message.processed,
        totalImages: message.total,
        accuracy: message.accuracy,
        successCount: message.successCount,
        failedCount: message.failedCount,
        processingTime: message.processingTime
      }
    }
  } else {
    console.warn('WebSocket消息格式不正确:', message)
    return
  }
  
  const taskIndex = tasks.value.findIndex(task => task.id === taskId)
  if (taskIndex !== -1) {
    const task = tasks.value[taskIndex]
    
    // 更新任务状态和进度
    if (messageType === 'PROGRESS') {
      task.processedImages = messageData.processedImages || task.processedImages
      task.accuracy = messageData.accuracy
      task.successCount = messageData.successCount
      task.failedCount = messageData.failedCount
      task.status = 'PROCESSING'
    } else if (messageType === 'COMPLETE') {
      task.status = 'COMPLETED'
      task.processedImages = task.totalImages
      task.accuracy = messageData.accuracy
      task.successCount = messageData.successCount
      task.failedCount = messageData.failedCount
      task.processingTime = messageData.processingTime
    } else if (messageType === 'ERROR') {
      task.status = 'FAILED'
    }
    
    // 如果当前正在查看该任务的详情，也更新详情对话框
    if (currentTask.value && currentTask.value.id === taskId) {
      currentTask.value = { ...task }
    }
  }
}

const handlePageChange = (page: number) => {
  pagination.page = page
  loadTasks()
}

onMounted(() => {
  loadTasks()
  
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
.task-list {
  padding: 0;
}

.filter-card, .task-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.task-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.task-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.task-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.task-info {
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.label {
  color: #666;
  font-size: 14px;
}

.value {
  color: #333;
  font-size: 14px;
  font-weight: 500;
}

.task-progress {
  margin-bottom: 16px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
  color: #666;
}

.percentage {
  font-weight: 600;
  color: #409eff;
}

.task-description {
  margin-bottom: 16px;
  padding: 12px;
  background-color: #f8f9fa;
  border-radius: 4px;
  font-size: 14px;
}

.task-description .label {
  font-weight: 600;
  margin-right: 8px;
}

.task-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.task-detail {
  margin-bottom: 20px;
}

.processing-info, .completion-stats {
  margin-top: 20px;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.processing-info h4, .completion-stats h4 {
  margin-bottom: 16px;
  color: #333;
  font-size: 16px;
}

.progress-text {
  margin-top: 8px;
  text-align: center;
  color: #666;
  font-size: 14px;
}

.task-count {
  color: #666;
  font-size: 14px;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}

.stat-item {
  text-align: center;
  padding: 16px;
  background-color: white;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

@media (max-width: 768px) {
  .task-grid {
    grid-template-columns: 1fr;
  }
  
  .card-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .task-actions {
    justify-content: center;
  }
}
</style>