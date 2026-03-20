<template>
  <div class="system-logs">
    <!-- 操作栏 -->
    <el-card class="operation-card">
      <el-row :gutter="16" class="operation-row">
        <el-col :span="3">
          <el-select v-model="searchForm.level" placeholder="日志级别" clearable>
            <el-option label="全部" value="" />
            <el-option label="信息" value="INFO" />
            <el-option label="警告" value="WARN" />
            <el-option label="错误" value="ERROR" />
            <el-option label="调试" value="DEBUG" />
          </el-select>
        </el-col>
        
        <el-col :span="3">
          <el-select v-model="searchForm.module" placeholder="模块" clearable>
            <el-option label="全部" value="" />
            <el-option label="用户管理" value="USER" />
            <el-option label="图片分类" value="IMAGE" />
            <el-option label="任务管理" value="TASK" />
            <el-option label="系统管理" value="SYSTEM" />
            <el-option label="认证授权" value="AUTH" />
          </el-select>
        </el-col>
        
        <el-col :span="4">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索关键词"
            clearable
            @keyup.enter="searchLogs"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        
        <el-col :span="8">
          <el-button type="primary" @click="searchLogs">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button @click="exportLogs">
            <el-icon><Download /></el-icon>
            导出
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon info">
              <el-icon><InfoFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.info }}</div>
              <div class="stat-label">信息日志</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon warning">
              <el-icon><WarningFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.warn }}</div>
              <div class="stat-label">警告日志</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon error">
              <el-icon><CircleCloseFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.error }}</div>
              <div class="stat-label">错误日志</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon debug">
              <el-icon><Tools /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ stats.debug }}</div>
              <div class="stat-label">调试日志</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 日志表格 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>系统日志</span>
          <div class="header-actions">
            <el-button 
              :type="autoRefresh ? 'success' : 'info'" 
              size="small" 
              @click="toggleAutoRefresh"
            >
              <el-icon><Refresh /></el-icon>
              {{ autoRefresh ? '停止刷新' : '自动刷新' }}
            </el-button>
            <el-button size="small" @click="clearLogs">
              <el-icon><Delete /></el-icon>
              清空日志
            </el-button>
          </div>
        </div>
      </template>
      
      <el-table
        :data="logs"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        :row-class-name="getRowClassName"
      >
        <el-table-column prop="id" label="ID" width="80" />
        
        <el-table-column prop="level" label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.level)" size="small">
              {{ row.level }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="module" label="模块" width="100">
          <template #default="{ row }">
            <el-tag type="info" size="small">
              {{ getModuleText(row.module) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="message" label="日志内容" min-width="300">
          <template #default="{ row }">
            <div class="log-message" :title="row.message">
              {{ row.message }}
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="username" label="操作用户" width="120" />
        
        <el-table-column prop="ip" label="IP地址" width="140" />
        
        <el-table-column prop="userAgent" label="用户代理" width="200">
          <template #default="{ row }">
            <div class="user-agent" :title="row.userAgent">
              {{ row.userAgent }}
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewLogDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页组件 -->
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

    <!-- 日志详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="日志详情" width="800px">
      <div v-if="currentLog" class="log-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="日志ID">{{ currentLog.id }}</el-descriptions-item>
          <el-descriptions-item label="日志级别">
            <el-tag :type="getLevelType(currentLog.level)">
              {{ currentLog.level }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="模块">
            <el-tag type="info">{{ getModuleText(currentLog.module) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作用户">{{ currentLog.username || '-' }}</el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ currentLog.ip }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(currentLog.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="日志内容" :span="2">
            <div class="log-content">{{ currentLog.message }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="用户代理" :span="2">
            <div class="user-agent-detail">{{ currentLog.userAgent }}</div>
          </el-descriptions-item>
          <el-descriptions-item v-if="currentLog.stackTrace" label="堆栈信息" :span="2">
            <pre class="stack-trace">{{ currentLog.stackTrace }}</pre>
          </el-descriptions-item>
          <el-descriptions-item v-if="currentLog.requestData" label="请求数据" :span="2">
            <pre class="request-data">{{ JSON.stringify(currentLog.requestData, null, 2) }}</pre>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Search, 
  Download, 
  InfoFilled, 
  WarningFilled, 
  CircleCloseFilled, 
  Tools, 
  Refresh, 
  Delete 
} from '@element-plus/icons-vue'
import { 
  getSystemLogs, 
  getLogStatistics, 
  clearAllLogs, 
  getLogDetail,
  type SystemLog,
  type LogSearchParams 
} from '@/api/systemLog'

// 搜索表单
const searchForm = reactive({
  level: '',
  module: '',
  keyword: '',
  dateRange: []
})

// 分页
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 统计数据
const stats = reactive({
  info: 0,
  warn: 0,
  error: 0,
  debug: 0
})

// 表格数据
const logs = ref<SystemLog[]>([])

// 其他状态
const loading = ref(false)
const detailDialogVisible = ref(false)
const currentLog = ref<SystemLog | null>(null)
const autoRefresh = ref(false)
let refreshTimer: NodeJS.Timeout | null = null

// 获取级别类型
const getLevelType = (level: string) => {
  const levelMap: Record<string, string> = {
    'INFO': 'success',
    'WARN': 'warning',
    'ERROR': 'danger',
    'DEBUG': 'info'
  }
  return levelMap[level] || 'info'
}

// 获取模块文本
const getModuleText = (module: string) => {
  const moduleMap: Record<string, string> = {
    'USER': '用户管理',
    'IMAGE': '图片分类',
    'TASK': '任务管理',
    'SYSTEM': '系统管理',
    'AUTH': '认证授权'
  }
  return moduleMap[module] || module
}

// 获取行类名
const getRowClassName = ({ row }: { row: any }) => {
  if (row.level === 'ERROR') {
    return 'error-row'
  } else if (row.level === 'WARN') {
    return 'warning-row'
  }
  return ''
}

// 搜索日志
const searchLogs = async () => {
  console.log('搜索条件:', searchForm)
  await loadLogs()
}

// 重置搜索
const resetSearch = async () => {
  Object.assign(searchForm, {
    level: '',
    module: '',
    keyword: '',
    dateRange: []
  })
  pagination.page = 1
  await loadLogs()
}

// 加载日志列表
const loadLogs = async () => {
  loading.value = true
  try {
    const params: LogSearchParams = {
      level: searchForm.level || undefined,
      module: searchForm.module || undefined,
      keyword: searchForm.keyword || undefined,
      page: pagination.page - 1, // 后端从0开始
      size: pagination.size
    }
    
    // 处理时间范围
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startTime = searchForm.dateRange[0]
      params.endTime = searchForm.dateRange[1]
    }
    
    const response = await getSystemLogs(params)
    if (response.success) {
      const data = response.data
      logs.value = data.content || []
      pagination.total = data.totalElements || 0
    } else {
      ElMessage.error(response.message || '获取日志失败')
    }
  } catch (error) {
    console.error('加载日志失败:', error)
    ElMessage.error('加载日志失败')
  } finally {
    loading.value = false
  }
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const response = await getLogStatistics()
    if (response.success) {
      Object.assign(stats, response.data)
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 查看日志详情
const viewLogDetail = async (row: SystemLog) => {
  try {
    const response = await getLogDetail(row.id)
    if (response.success) {
      currentLog.value = response.data
      detailDialogVisible.value = true
    } else {
      ElMessage.error(response.message || '获取日志详情失败')
    }
  } catch (error) {
    console.error('获取日志详情失败:', error)
    ElMessage.error('获取日志详情失败')
  }
}

// 导出日志
const exportLogs = () => {
  ElMessage.info('导出功能开发中...')
}

// 清空日志
const clearLogs = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有日志吗？此操作不可恢复。', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })
    
    const response = await clearAllLogs()
    if (response.success) {
      ElMessage.success('日志已清空')
      await loadLogs()
      await loadStatistics()
    } else {
      ElMessage.error(response.message || '清空日志失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('清空日志失败:', error)
      ElMessage.error('清空日志失败')
    }
  }
}

// 切换自动刷新
const toggleAutoRefresh = () => {
  autoRefresh.value = !autoRefresh.value
  
  if (autoRefresh.value) {
    refreshTimer = setInterval(async () => {
      await loadLogs()
      await loadStatistics()
    }, 5000) // 每5秒刷新一次
    ElMessage.success('已开启自动刷新')
  } else {
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
    ElMessage.info('已停止自动刷新')
  }
}

// 格式化时间
const formatTime = (time: string) => {
  return new Date(time).toLocaleString()
}

// 分页处理
const handleSizeChange = async (size: number) => {
  pagination.size = size
  pagination.page = 1
  await loadLogs()
}

const handlePageChange = async (page: number) => {
  pagination.page = page
  await loadLogs()
}

onMounted(async () => {
  await loadLogs()
  await loadStatistics()
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.system-logs {
  padding: 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 50%, #cbd5e1 100%);
  min-height: 100%;
  position: relative;
}

.system-logs::before {
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

.operation-card, .table-card {
  margin-bottom: 32px;
  border-radius: 24px;
  border: none;
  box-shadow: 
    0 12px 40px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 1;
}

.operation-card:hover, .table-card:hover {
  transform: translateY(-4px);
  box-shadow: 
    0 20px 60px rgba(0, 0, 0, 0.12),
    0 0 0 1px rgba(255, 255, 255, 0.9);
}

:deep(.operation-card .el-card__body),
:deep(.table-card .el-card__body) {
  padding: 24px;
}

.operation-row {
  align-items: center;
}

/* 统计卡片样式 */
.stats-row {
  margin-bottom: 32px;
  position: relative;
  z-index: 1;
}

.stat-card {
  border-radius: 20px;
  border: none;
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.stat-card:hover {
  transform: translateY(-6px);
  box-shadow: 
    0 16px 48px rgba(0, 0, 0, 0.12),
    0 0 0 1px rgba(255, 255, 255, 0.9);
}

:deep(.stat-card .el-card__body) {
  padding: 24px;
}

.stat-content {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  font-size: 24px;
  color: white;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.stat-icon.info {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

.stat-icon.warning {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.stat-icon.error {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
}

.stat-icon.debug {
  background: linear-gradient(135deg, #6b7280 0%, #4b5563 100%);
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 4px;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 600;
  letter-spacing: 0.5px;
}

/* 卡片头部样式 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 0.5px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 搜索框样式 */
:deep(.el-input) {
  border-radius: 12px;
}

:deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(226, 232, 240, 0.8);
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  border-color: rgba(59, 130, 246, 0.3);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.1);
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

/* 选择器样式 */
:deep(.el-select) {
  border-radius: 12px;
}

:deep(.el-select .el-input__wrapper) {
  border-radius: 12px;
}

/* 按钮样式 */
:deep(.el-button) {
  border-radius: 12px;
  font-weight: 600;
  letter-spacing: 0.5px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

:deep(.el-button:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

:deep(.el-button--success) {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

:deep(.el-button--warning) {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

:deep(.el-button--danger) {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
}

:deep(.el-button--info) {
  background: linear-gradient(135deg, #6b7280 0%, #4b5563 100%);
}

:deep(.el-button.is-plain) {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(226, 232, 240, 0.8);
  backdrop-filter: blur(10px);
}

/* 表格样式 */
:deep(.el-table) {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

:deep(.el-table__header) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

:deep(.el-table th) {
  background: transparent;
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  font-weight: 700;
  color: #1e293b;
  font-size: 14px;
  letter-spacing: 0.5px;
}

:deep(.el-table td) {
  border-bottom: 1px solid rgba(226, 232, 240, 0.4);
  padding: 16px 12px;
}

:deep(.el-table__row:hover) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

:deep(.el-table__row:hover td) {
  background: transparent;
}

/* 日志消息样式 */
.log-message {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
  color: #374151;
}

.user-agent {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: #64748b;
}

/* 标签样式 */
:deep(.el-tag) {
  border-radius: 8px;
  font-weight: 600;
  letter-spacing: 0.5px;
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

:deep(.el-tag--success) {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

:deep(.el-tag--danger) {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
}

:deep(.el-tag--warning) {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
}

:deep(.el-tag--info) {
  background: linear-gradient(135deg, #6b7280 0%, #4b5563 100%);
  color: white;
}

/* 表格行样式 */
:deep(.error-row) {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.05) 0%, rgba(220, 38, 38, 0.05) 100%) !important;
}

:deep(.warning-row) {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.05) 0%, rgba(217, 119, 6, 0.05) 100%) !important;
}

/* 日志详情样式 */
.log-detail {
  margin-bottom: 20px;
}

:deep(.el-descriptions) {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

:deep(.el-descriptions__header) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

:deep(.el-descriptions__label) {
  font-weight: 600;
  color: #374151;
}

:deep(.el-descriptions__content) {
  color: #1e293b;
  font-weight: 500;
}

.log-content {
  word-break: break-all;
  white-space: pre-wrap;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  padding: 16px;
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.user-agent-detail {
  word-break: break-all;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  padding: 12px;
  border-radius: 8px;
  font-size: 12px;
  color: #64748b;
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.stack-trace, .request-data {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  padding: 16px;
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  font-size: 12px;
  line-height: 1.4;
  max-height: 300px;
  overflow-y: auto;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  color: #374151;
}

/* 分页样式 */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 32px;
  padding: 0 20px 20px;
  position: relative;
  z-index: 1;
}

:deep(.el-pagination) {
  background: rgba(255, 255, 255, 0.9);
  padding: 16px 24px;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

:deep(.el-pagination .el-pager li) {
  border-radius: 8px;
  margin: 0 2px;
  transition: all 0.3s ease;
  font-weight: 600;
}

:deep(.el-pagination .el-pager li:hover) {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
  transform: translateY(-1px);
}

:deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next) {
  border-radius: 8px;
  font-weight: 600;
  transition: all 0.3s ease;
}

:deep(.el-pagination .btn-prev:hover),
:deep(.el-pagination .btn-next:hover) {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
}

/* 对话框样式 */
:deep(.el-dialog) {
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
}

:deep(.el-dialog__header) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 24px 24px 0 0;
  padding: 24px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
}

:deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 0.5px;
}

:deep(.el-dialog__body) {
  padding: 24px;
}

:deep(.el-dialog__footer) {
  padding: 24px;
  border-top: 1px solid rgba(226, 232, 240, 0.6);
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 0 0 24px 24px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .system-logs {
    padding: 16px;
  }
}

@media (max-width: 768px) {
  .system-logs {
    padding: 12px;
  }
  
  .operation-card, .table-card {
    margin-bottom: 20px;
  }
  
  .stats-row {
    margin-bottom: 20px;
  }
  
  .operation-row .el-col {
    margin-bottom: 16px;
  }
  
  .stats-row .el-col {
    margin-bottom: 16px;
  }
  
  .stat-content {
    flex-direction: column;
    text-align: center;
  }
  
  .stat-icon {
    margin-right: 0;
    margin-bottom: 12px;
    width: 50px;
    height: 50px;
    font-size: 20px;
  }
  
  .stat-number {
    font-size: 24px;
  }
  
  .header-actions {
    flex-direction: column;
    gap: 8px;
  }
  
  .pagination-wrapper {
    margin-top: 20px;
    padding: 0 12px 12px;
  }
  
  :deep(.el-pagination) {
    padding: 12px 16px;
  }
  
  :deep(.el-dialog) {
    margin: 5vh auto;
    width: 95%;
  }
  
  :deep(.el-dialog__header),
  :deep(.el-dialog__body),
  :deep(.el-dialog__footer) {
    padding: 16px;
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

.operation-card, .table-card {
  animation: fadeInUp 0.6s ease-out;
}

.operation-card {
  animation-delay: 0.1s;
}

.table-card {
  animation-delay: 0.2s;
}

.stat-card {
  animation: fadeInUp 0.6s ease-out;
}

.stat-card:nth-child(1) {
  animation-delay: 0.1s;
}

.stat-card:nth-child(2) {
  animation-delay: 0.2s;
}

.stat-card:nth-child(3) {
  animation-delay: 0.3s;
}

.stat-card:nth-child(4) {
  animation-delay: 0.4s;
}

/* 表格行动画 */
:deep(.el-table__row) {
  transition: all 0.3s ease;
}

/* 统计图标悬停效果 */
.stat-icon:hover {
  transform: scale(1.05);
  transition: all 0.3s ease;
}

/* 按钮组动画 */
:deep(.el-button-group .el-button) {
  transition: all 0.3s ease;
}

:deep(.el-button-group .el-button:hover) {
  z-index: 1;
}

/* 加载状态样式 */
:deep(.el-loading-mask) {
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
}

:deep(.el-loading-spinner) {
  color: #3b82f6;
}

/* 滚动条美化 */
.stack-trace::-webkit-scrollbar,
.request-data::-webkit-scrollbar {
  width: 6px;
}

.stack-trace::-webkit-scrollbar-track,
.request-data::-webkit-scrollbar-track {
  background: rgba(226, 232, 240, 0.3);
  border-radius: 3px;
}

.stack-trace::-webkit-scrollbar-thumb,
.request-data::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  border-radius: 3px;
}

.stack-trace::-webkit-scrollbar-thumb:hover,
.request-data::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, #1d4ed8 0%, #1e40af 100%);
}
</style>