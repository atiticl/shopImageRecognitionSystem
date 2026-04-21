<template>
  <div class="classification-results">
    <!-- 单张图片测试结果展示 -->
    <el-card v-if="isSingleImageMode" class="single-image-card">
      <template #header>
        <div class="card-header">
          <span>图片分类结果</span>
          <el-button type="primary" size="small" @click="backToList">返回列表</el-button>
        </div>
      </template>
      
      <div class="single-image-container">
        <div class="image-section">
          <div class="image-wrapper">
            <img :src="singleResult.imageUrl" :alt="singleResult.fileName" class="single-image" />
          </div>
          <div class="image-info">
            <h3>{{ singleResult.fileName }}</h3>
            <p class="upload-time">上传时间: {{ formatTime(singleResult.createdAt) }}</p>
          </div>
        </div>
        
        <div class="result-section">
          <div class="confidence-display">
              <div class="confidence-circle" :class="getConfidenceLevel(singleResult.confidence)">
                <span class="confidence-number">{{ formatAccuracy(singleResult.confidence) }}</span>
                <span class="confidence-label">置信度</span>
              </div>
            </div>
          
          <div class="prediction-result">
            <div class="predicted-category">
              <el-icon><Collection /></el-icon>
              <span class="category-label">预测类别:</span>
              <el-tag :type="getConfidenceType(singleResult.confidence)" size="large">
                {{ singleResult.predictedCategoryName }}
              </el-tag>
            </div>
            
            <div class="auto-classification" v-if="singleResult.confidence >= 90">
              <el-icon class="success-icon"><CircleCheck /></el-icon>
              <span class="auto-text">置信度高于90%，已自动归类</span>
            </div>
            
            <div class="manual-classification" v-else>
              <el-icon class="warning-icon"><Warning /></el-icon>
              <span class="manual-text">置信度较低，建议人工确认</span>
            </div>
          </div>
          
          <div class="action-buttons">
            <el-button 
              type="success" 
              size="large"
              v-if="singleResult.confidence >= 90 && !singleResult.isCorrected"
              @click="confirmAutoClassification"
            >
              <el-icon><Check /></el-icon>
              确认分类
            </el-button>
            
            <el-button 
              type="primary" 
              size="large"
              @click="correctResult(singleResult)"
              :disabled="singleResult.isCorrected"
            >
              <el-icon><Edit /></el-icon>
              {{ singleResult.isCorrected ? '已修正' : '手动修正' }}
            </el-button>
            
            <el-button 
              type="info" 
              size="large"
              @click="reprocessImage"
            >
              <el-icon><Refresh /></el-icon>
              重新处理
            </el-button>
          </div>
          
          <div v-if="singleResult.correctedCategory" class="correction-info">
            <el-divider />
            <div class="corrected-result">
              <el-icon class="success-icon"><CircleCheck /></el-icon>
              <span>已修正为: </span>
              <el-tag type="success" size="large">{{ singleResult.correctedCategory }}</el-tag>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 压缩包处理结果展示 -->
    <el-card v-if="isBatchMode" class="batch-results-card">
      <template #header>
        <div class="card-header">
          <span>压缩包处理结果</span>
          <div class="header-actions">
            <el-button type="info" size="small" @click="showBatchDetails = !showBatchDetails">
              {{ showBatchDetails ? '隐藏详情' : '查看详情' }}
            </el-button>
            <el-button type="primary" size="small" @click="backToList">返回列表</el-button>
          </div>
        </div>
      </template>
      
      <div class="batch-summary">
        <div class="summary-stats">
          <div class="stat-item">
            <div class="stat-number">{{ batchResults.totalImages }}</div>
            <div class="stat-label">总图片数</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ batchResults.processedImages }}</div>
            <div class="stat-label">已处理</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ batchResults.highConfidenceCount }}</div>
            <div class="stat-label">高置信度(≥90%)</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ batchResults.needReviewCount }}</div>
            <div class="stat-label">需要审核</div>
          </div>
        </div>
        
        <div class="batch-actions">
          <el-button type="success" @click="autoConfirmHighConfidence">
            批量确认高置信度结果
          </el-button>
          <el-button type="primary" @click="enterBatchReview">
            进入批量审核
          </el-button>
          <el-button type="info" @click="exportBatchResults">
            导出结果
          </el-button>
        </div>
      </div>
      
      <!-- 压缩包内图片列表 -->
      <div v-if="showBatchDetails" class="batch-details">
        <el-divider>压缩包内容</el-divider>
        <div class="batch-grid">
          <div 
            v-for="result in batchResults.images" 
            :key="result.id"
            class="batch-item"
            @click="viewBatchImage(result)"
          >
            <div class="batch-image">
              <img :src="result.imageUrl" :alt="result.fileName" />
              <div class="batch-overlay">
                <el-icon><ZoomIn /></el-icon>
              </div>
            </div>
            <div class="batch-info">
              <div class="file-name">{{ result.fileName }}</div>
              <div class="prediction">
                <el-tag :type="getConfidenceType(result.confidence)" size="small">
                  {{ result.predictedCategory }}
                </el-tag>
                <span class="confidence">{{ formatAccuracy(result.confidence) }}</span>
              </div>
              <div class="status">
                <el-tag 
                  :type="result.confidence >= 90 ? 'success' : 'warning'" 
                  size="small"
                >
                  {{ result.confidence >= 90 ? '自动归类' : '需审核' }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 普通列表模式 -->
    <div v-if="!isSingleImageMode && !isBatchMode">
      <!-- 筛选条件 -->
      <el-card class="filter-card">
        <el-form :model="filters" inline>
          <el-form-item label="任务">
            <el-select v-model="filters.taskId" placeholder="选择任务" clearable @change="searchResults" style="width: 200px">
              <el-option
                v-for="task in tasks"
                :key="task.id"
                :label="task.taskName || task.name || `任务#${task.id}`"
                :value="task.id"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="预测类别">
            <el-select v-model="filters.predictedCategoryId" placeholder="选择类别" clearable @change="searchResults" style="width: 150px">
              <el-option
                v-for="category in categories"
                :key="category.id"
                :label="category.name"
                :value="category.id"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="置信度">
            <el-slider
              v-model="filters.confidenceRange"
              range
              :min="0"
              :max="100"
              :format-tooltip="(val) => val + '%'"
              style="width: 200px"
            />
          </el-form-item>
          
          <el-form-item label="是否已修正">
            <el-select v-model="filters.isCorrected" placeholder="选择状态" clearable style="width: 120px">
              <el-option label="是" :value="true" />
              <el-option label="否" :value="false" />
            </el-select>
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="searchResults">搜索</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 操作栏 -->
      <el-card class="action-card">
        <div class="action-bar">
          <div class="action-left">
            <el-checkbox v-model="selectAll" @change="handleSelectAll">全选</el-checkbox>
            <el-button 
              type="primary" 
              :disabled="selectedResults.length === 0"
              @click="batchCorrect"
            >
              批量修正 ({{ selectedResults.length }})
            </el-button>
            <el-button 
              type="success" 
              :disabled="selectedResults.length === 0"
              @click="exportResults"
            >
              导出结果
            </el-button>
          </div>
          <div class="action-right">
            <span class="result-count">共 {{ pagination.total }} 条结果</span>
          </div>
        </div>
      </el-card>

      <!-- 结果列表 -->
      <el-card class="results-card">
        <div v-loading="loading" class="results-grid">
          <div v-if="results.length === 0 && !loading" class="empty-state">
            <el-empty description="暂无分类结果" />
          </div>
          <div 
            v-for="result in results" 
            :key="result.id"
            class="result-item"
            :class="{ 'selected': selectedResults.includes(result.id) }"
          >
            <div class="result-checkbox">
              <el-checkbox 
                :model-value="selectedResults.includes(result.id)"
                @change="(checked) => handleSelectResult(result.id, checked)"
              />
            </div>
            
            <div class="result-image" @click="previewImage(result)">
              <img :src="result.imageUrl" :alt="result.fileName" />
              <div class="image-overlay">
                <el-icon><ZoomIn /></el-icon>
              </div>
            </div>
            
            <div class="result-info">
              <div class="file-name" :title="result.fileName">{{ result.fileName }}</div>
              
              <div class="prediction-info">
                <div class="predicted-category">
                  <span class="label">预测:</span>
                  <el-tag :type="getConfidenceType(result.confidence)">
                    {{ result.predictedCategory }}
                  </el-tag>
                </div>
                <div class="confidence">
                  <span class="label">置信度:</span>
                  <span class="confidence-value" :class="getConfidenceClass(result.confidence)">
                    {{ formatAccuracy(result.confidence) }}
                  </span>
                </div>
              </div>
              
              <div v-if="result.correctedCategoryName" class="correction-info">
                <span class="label">修正:</span>
                <el-tag type="success">{{ result.correctedCategoryName }}</el-tag>
              </div>
              
              <div class="result-status">
                <el-tag :type="result.isCorrected ? 'success' : 'info'" size="small">
                  {{ result.isCorrected ? '已修正' : '待确认' }}
                </el-tag>
              </div>
              
              <div class="process-time">
                  处理时间: {{ formatTime(result.createdAt) }}
                </div>
            </div>
            
            <div class="result-actions">
              <el-button 
                type="primary" 
                size="small" 
                @click="correctResult(result)"
                :disabled="result.isCorrected"
              >
                {{ result.isCorrected ? '已修正' : '修正' }}
              </el-button>
            </div>
          </div>
        </div>
        
        <!-- 分页 -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :page-sizes="[12, 24, 48, 96]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </el-card>
    </div>

    <!-- 修正对话框 -->
     <el-dialog v-model="correctDialogVisible" title="修正分类结果" width="600px">
       <div class="correct-content">
         <div class="current-result">
           <h4>当前预测结果</h4>
           <div class="prediction-display">
             <img 
               :src="currentResult?.imageUrl" 
               :alt="currentResult?.fileName" 
               class="preview-image"
             />
             <div class="prediction-info">
               <p><strong>文件名:</strong> {{ currentResult?.fileName }}</p>
               <p>
                 <strong>预测类别:</strong> 
                 <el-tag :type="getConfidenceType(currentResult?.confidence)" style="margin-left: 8px;">
                   {{ currentResult?.predictedCategory }}
                 </el-tag>
               </p>
               <p><strong>置信度:</strong> {{ formatAccuracy(currentResult?.confidence) }}</p>
             </div>
           </div>
         </div>
         
         <el-divider />
         
         <el-form :model="correctForm" :rules="correctRules" ref="correctFormRef" label-width="100px">
           <el-form-item label="正确类别" prop="correctCategory">
             <el-select 
               v-model="correctForm.correctCategory" 
               placeholder="请选择正确的类别" 
               style="width: 100%"
               filterable
             >
               <el-option
                 v-for="category in categories"
                 :key="category.id"
                 :label="category.categoryCode ? `${category.name} (${category.categoryCode})` : category.name"
                 :value="category.id"
               >
                 <div class="category-option">
                   <div>
                     <span>{{ category.name }}</span>
                     <span v-if="category.categoryCode" class="category-code">[{{ category.categoryCode }}]</span>
                   </div>
                   <div v-if="category.description" class="category-description">{{ category.description }}</div>
                 </div>
               </el-option>
             </el-select>
           </el-form-item>
           
           <el-form-item label="修正说明" prop="note">
             <el-input
               v-model="correctForm.note"
               type="textarea"
               :rows="3"
               placeholder="请输入修正说明（可选）"
               maxlength="200"
               show-word-limit
             />
           </el-form-item>
         </el-form>
       </div>
       
       <template #footer>
         <el-button @click="correctDialogVisible = false">取消</el-button>
         <el-button type="primary" @click="submitCorrection">确定修正</el-button>
       </template>
     </el-dialog>

    <!-- 图片预览对话框 -->
    <el-dialog v-model="previewVisible" title="图片预览" width="60%" center>
      <div class="preview-container">
        <img :src="previewUrl" :alt="previewName" class="preview-image" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { 
  ZoomIn, Collection, CircleCheck, Warning, Check, Edit, Refresh 
} from '@element-plus/icons-vue'
import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { useRoute, useRouter } from 'vue-router'
import * as XLSX from 'xlsx'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

// 显示模式控制
const isSingleImageMode = ref(false)
const isBatchMode = ref(false)
const showBatchDetails = ref(false)

// 单张图片结果
const singleResult = ref({
  id: null,
  fileName: '',
  imageUrl: '',
  predictedCategory: '',
  confidence: 0,
  correctedCategory: '',
  isCorrected: false,
  processTime: null
})

// 批量处理结果
const batchResults = reactive({
  totalImages: 0,
  processedImages: 0,
  highConfidenceCount: 0,
  needReviewCount: 0,
  images: []
})

// 筛选条件
const filters = reactive({
  taskId: '',
  predictedCategoryId: null as number | null,
  confidenceRange: [0, 100],
  isCorrected: null as boolean | null
})

// 分页
const pagination = reactive({
  page: 1,
  size: 24,
  total: 0
})

// 数据
const loading = ref(false)
const tasks = ref([])
const categories = ref([])
const results = ref([])

// 选择状态
const selectAll = ref(false)
const selectedResults = ref<string[]>([])

// 修正对话框
const correctDialogVisible = ref(false)
const currentResult = ref<any>(null)
const correctFormRef = ref<FormInstance>()
const correctForm = reactive({
  correctCategory: '',
  note: ''
})

const correctRules: FormRules = {
  correctCategory: [
    { required: true, message: '请选择正确的类别', trigger: 'change' }
  ]
}

// 预览
const previewVisible = ref(false)
const previewUrl = ref('')
const previewName = ref('')

// 获取置信度等级
const getConfidenceLevel = (confidence: number | string) => {
  const conf = typeof confidence === 'number' ? confidence : parseFloat(confidence || '0')
  if (conf >= 90) return 'high'
  if (conf >= 70) return 'medium'
  return 'low'
}

// 获取置信度类型
const getConfidenceType = (confidence: number | string) => {
  const conf = typeof confidence === 'number' ? confidence : parseFloat(confidence || '0')
  if (conf >= 90) return 'success'
  if (conf >= 70) return 'warning'
  return 'danger'
}

// 获取置信度样式类
const getConfidenceClass = (confidence: number | string) => {
  const conf = typeof confidence === 'number' ? confidence : parseFloat(confidence || '0')
  if (conf >= 90) return 'high-confidence'
  if (conf >= 70) return 'medium-confidence'
  return 'low-confidence'
}

// 格式化时间
const formatTime = (time: string) => {
  return new Date(time).toLocaleString()
}

// 转换图片URL - 统一使用后端代理访问
const convertToImageUrl = (fileUrl: string | undefined, imageId: number) => {
  // 始终使用后端代理URL来访问图片，这样可以确保：
  // 1. 统一的访问控制
  // 2. 避免跨域问题
  // 3. 不需要暴露MinIO的直接访问地址
  return `/api/images/view/${imageId}`
}

// 格式化准确率
const formatAccuracy = (accuracy: number | string | null | undefined): string => {
  if (accuracy === null || accuracy === undefined) {
    return '0.00%'
  }
  const num = typeof accuracy === 'string' ? parseFloat(accuracy) : accuracy
  // 将0-1的小数转换为百分比
  return isNaN(num) ? '0.00%' : (num * 100).toFixed(2) + '%'
}

// 返回列表
const backToList = () => {
  isSingleImageMode.value = false
  isBatchMode.value = false
  loadResults()
}

// 确认自动分类
const confirmAutoClassification = async () => {
  try {
    await axios.put(`/api/classification-results/${singleResult.value.id}/confirm`, {}, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    ElMessage.success('分类确认成功')
    singleResult.value.isCorrected = true
  } catch (error) {
    console.error('确认分类失败:', error)
    ElMessage.error('确认分类失败')
  }
}

// 重新处理图片
const reprocessImage = async () => {
  try {
    await axios.post(`/api/classification-results/${singleResult.value.id}/reprocess`, {}, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    ElMessage.success('重新处理成功')
    // 重新加载结果
    loadSingleResult(singleResult.value.id)
  } catch (error) {
    console.error('重新处理失败:', error)
    ElMessage.error('重新处理失败')
  }
}

// 批量确认高置信度结果
const autoConfirmHighConfidence = async () => {
  try {
    const highConfidenceIds = batchResults.images
      .filter(img => img.confidence >= 90 && !img.isCorrected)
      .map(img => img.id)
    
    if (highConfidenceIds.length === 0) {
      ElMessage.warning('没有需要确认的高置信度结果')
      return
    }
    
    await axios.put('/api/classification-results/batch-confirm', {
      resultIds: highConfidenceIds
    }, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    ElMessage.success(`已确认 ${highConfidenceIds.length} 个高置信度结果`)
    loadBatchResults()
  } catch (error) {
    console.error('批量确认失败:', error)
    ElMessage.error('批量确认失败')
  }
}

// 进入批量审核
const enterBatchReview = () => {
  // 跳转到批量审核页面
  router.push('/classification/batch-review')
}

// 导出批量结果
const exportBatchResults = async () => {
  try {
    const response = await axios.get('/api/classification-results/batch-export', {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      },
      responseType: 'blob'
    })
    
    const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `batch_results_${new Date().getTime()}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// 查看批量图片
const viewBatchImage = (result: any) => {
  singleResult.value = {
    ...result,
    imageUrl: convertToImageUrl(result.fileUrl, result.imageId)
  }
  isBatchMode.value = false
  isSingleImageMode.value = true
}

// 加载单个结果
const loadSingleResult = async (id: string) => {
  try {
    const response = await axios.get(`/api/classification-results/${id}`, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    const data = response.data.data
    singleResult.value = {
      ...data,
      imageUrl: convertToImageUrl(data.fileUrl, data.imageId)
    }
  } catch (error) {
    console.error('加载结果失败:', error)
    ElMessage.error('加载结果失败')
  }
}

// 加载批量结果
const loadBatchResults = async () => {
  try {
    const response = await axios.get('/api/classification-results/batch', {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    const data = response.data.data
    // 转换批量结果中的图片URL
    if (data.images && Array.isArray(data.images)) {
      data.images = data.images.map((item: any) => ({
        ...item,
        imageUrl: convertToImageUrl(item.fileUrl, item.imageId)
      }))
    }
    Object.assign(batchResults, data)
  } catch (error) {
    console.error('加载批量结果失败:', error)
    ElMessage.error('加载批量结果失败')
  }
}

// 搜索结果
const searchResults = () => {
  loadResults()
}

// 重置筛选条件
const resetFilters = () => {
  Object.assign(filters, {
    taskId: '',
    predictedCategoryId: null,
    confidenceRange: [0, 100],
    isCorrected: null
  })
  loadResults()
}

// 加载任务列表
const loadTasks = async () => {
  try {
    const response = await axios.get('/api/classification-tasks', {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    const data = response.data.data
    tasks.value = data?.list || data?.records || (Array.isArray(data) ? data : [])
  } catch (error) {
    console.error('加载任务列表失败:', error)
    ElMessage.error('加载任务列表失败')
  }
}

// 加载分类类别
const loadCategories = async () => {
  try {
    // 使用 /all 端点获取所有激活的类别，不分页
    const response = await axios.get('/api/categories/all', {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    // 直接获取数据数组
    categories.value = response.data.data || []
    
    console.log('加载的类别数据:', categories.value)
    console.log('类别数量:', categories.value.length)
    
    if (categories.value.length === 0) {
      console.warn('警告：没有加载到任何类别数据')
      ElMessage.warning('系统中暂无可用的分类类别，请先添加类别')
    }
  } catch (error) {
    console.error('加载分类类别失败:', error)
    ElMessage.error('加载分类类别失败')
  }
}

// 加载结果
const loadResults = async () => {
  loading.value = true
  try {
    const minConfidence = filters.confidenceRange[0] / 100
    const maxConfidence = filters.confidenceRange[1] / 100
    const params = {
      page: pagination.page,
      size: pagination.size,
      taskId: filters.taskId || undefined,
      predictedCategoryId: filters.predictedCategoryId || undefined,
      minConfidence,
      maxConfidence,
      isCorrected: filters.isCorrected
    }
    
    const response = await axios.get('/api/classification-results', {
      params,
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    // 处理API返回的数据，映射字段名
    const rawData = response.data.data?.data || []
    results.value = rawData.map((item: any) => ({
      id: item.id,
      imageId: item.imageId,
      taskId: item.taskId,
      fileName: item.fileName,
      fileUrl: item.fileUrl,
      imageUrl: convertToImageUrl(item.fileUrl, item.imageId), // 转换图片URL
      predictedCategory: item.predictedCategoryName, // 映射预测分类名称
      predictedCategoryId: item.predictedCategoryId,
      confidence: item.confidence ? parseFloat(item.confidence) : 0, // 确保置信度是数字类型
      correctedCategory: item.correctedCategoryName,
      correctedCategoryId: item.correctedCategoryId,
      isCorrected: item.isCorrected,
      createdAt: item.createdAt,
      processTime: item.createdAt // 使用创建时间作为处理时间
    }))
    pagination.total = response.data.data?.total || 0
  } catch (error) {
    console.error('加载分类结果失败:', error)
    ElMessage.error('加载分类结果失败')
  } finally {
    loading.value = false
  }
}

// 全选处理
const handleSelectAll = (checked: boolean) => {
  if (checked) {
    selectedResults.value = results.value.map(r => r.id)
  } else {
    selectedResults.value = []
  }
}

// 选择单个结果
const handleSelectResult = (id: string, checked: boolean) => {
  if (checked) {
    selectedResults.value.push(id)
  } else {
    const index = selectedResults.value.indexOf(id)
    if (index > -1) {
      selectedResults.value.splice(index, 1)
    }
  }
  
  // 更新全选状态
  selectAll.value = selectedResults.value.length === results.value.length
}

// 批量修正
const batchCorrect = async () => {
  if (selectedResults.value.length === 0) {
    ElMessage.warning('请先选择要修正的结果')
    return
  }
  
  try {
    let selectedCategoryId = ''
    await ElMessageBox({
      title: '批量修正',
      message: h('div', [
        h('p', '请选择正确的分类：'),
        h('el-select', {
          modelValue: selectedCategoryId,
          'onUpdate:modelValue': (val) => {
            selectedCategoryId = val
          },
          placeholder: '请选择分类',
          style: 'width: 100%'
        }, categories.value.map(cat => 
          h('el-option', {
            key: cat.id,
            label: cat.name,
            value: cat.id
          })
        ))
      ]),
      showCancelButton: true,
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    
    if (!selectedCategoryId) {
      ElMessage.warning('请选择分类')
      return
    }
    
    await axios.put('/api/classification-results/batch-correct', {
      resultIds: selectedResults.value,
      correctedCategoryId: selectedCategoryId
    }, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    ElMessage.success('批量修正成功')
    selectedResults.value = []
    selectAll.value = false
    loadResults()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量修正失败:', error)
      ElMessage.error('批量修正失败')
    }
  }
}

// 导出结果
const exportResults = async () => {
  try {
    const minConfidence = filters.confidenceRange[0] / 100
    const maxConfidence = filters.confidenceRange[1] / 100
    const response = await axios.get('/api/classification-results/export', {
      params: {
        taskId: filters.taskId || undefined,
        predictedCategoryId: filters.predictedCategoryId || undefined,
        minConfidence,
        maxConfidence,
        isCorrected: filters.isCorrected
      },
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      },
      responseType: 'blob'
    })
    
    const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `classification_results_${new Date().getTime()}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// 修正单个结果
const correctResult = (result: any) => {
  currentResult.value = result
  correctForm.correctCategory = ''
  correctForm.note = ''
  correctDialogVisible.value = true
}

// 提交修正
const submitCorrection = async () => {
  if (!correctFormRef.value) return
  
  try {
    await correctFormRef.value.validate()
    
    await axios.put(`/api/classification-results/${currentResult.value.id}/correct`, {
      correctedCategoryId: correctForm.correctCategory,
      note: correctForm.note
    }, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    ElMessage.success('修正成功')
    correctDialogVisible.value = false
    
    // 更新结果
    if (isSingleImageMode.value) {
      singleResult.value.correctedCategoryName = correctForm.correctCategory
      singleResult.value.isCorrected = true
    } else {
      loadResults()
    }
  } catch (error) {
    if (error.response) {
      console.error('修正失败:', error)
      ElMessage.error('修正失败')
    } else {
      console.log('表单验证失败:', error)
    }
  }
}

// 预览图片
const previewImage = (result: any) => {
  previewUrl.value = result.imageUrl
  previewName.value = result.fileName
  previewVisible.value = true
}

// 分页处理
const handleSizeChange = (size: number) => {
  pagination.size = size
  loadResults()
}

const handlePageChange = (page: number) => {
  pagination.page = page
  loadResults()
}

onMounted(() => {
  // 检查路由参数，确定显示模式
  const mode = route.query.mode as string
  const resultId = route.query.resultId as string
  const taskId = route.query.taskId as string
  
  if (mode === 'single' && resultId) {
    isSingleImageMode.value = true
    loadSingleResult(resultId)
  } else if (mode === 'batch') {
    isBatchMode.value = true
    loadBatchResults()
  } else {
    // 普通列表模式
    if (taskId) {
      filters.taskId = taskId
    }
    loadResults()
  }
  
  loadTasks()
  loadCategories()
})
</script>

<style scoped>
.classification-results {
  padding: 0;
}

.filter-card, .action-card, .results-card {
  margin-bottom: 20px;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.result-count {
  color: #666;
  font-size: 14px;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.result-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s;
  position: relative;
}

.result-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.result-item.selected {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.result-checkbox {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 2;
}

.result-image {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  cursor: pointer;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.result-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.3s ease;
}

.result-image:hover img {
  transform: scale(1.05);
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  color: white;
  font-size: 24px;
}

.result-image:hover .image-overlay {
  opacity: 1;
}

.result-info {
  padding: 16px;
}

.file-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.prediction-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.predicted-category, .confidence {
  display: flex;
  align-items: center;
  gap: 4px;
}

.label {
  font-size: 12px;
  color: #666;
}

.confidence-value {
  font-weight: 600;
}

.confidence-value.high-confidence {
  color: #67c23a;
}

.confidence-value.medium-confidence {
  color: #e6a23c;
}

.confidence-value.low-confidence {
  color: #f56c6c;
}

.correction-info {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
}

.result-status {
  margin-bottom: 8px;
}

.process-time {
  font-size: 12px;
  color: #999;
  margin-bottom: 12px;
}

.result-actions {
  padding: 0 16px 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.confidence-text {
  margin-left: 8px;
  font-size: 12px;
  color: #666;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

@media (max-width: 768px) {
  .results-grid {
    grid-template-columns: 1fr;
  }
  
  .action-bar {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .action-left {
    justify-content: center;
  }
  
  .prediction-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}

/* 单张图片模式样式 */
.single-image-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.single-image-container {
  display: flex;
  gap: 30px;
  padding: 20px 0;
}

.image-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.image-wrapper {
  width: 400px;
  height: 300px;
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #ffffff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.image-wrapper:hover {
  border-color: #409eff;
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.2);
}

.single-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 8px;
}

.image-info {
  margin-top: 15px;
  text-align: center;
}

.image-info h3 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 18px;
}

.upload-time {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.result-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.confidence-display {
  display: flex;
  justify-content: center;
}

.confidence-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  border: 4px solid;
}

.confidence-circle.high {
  border-color: #67c23a;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
}

.confidence-circle.medium {
  border-color: #e6a23c;
  background: linear-gradient(135deg, #fffbf0 0%, #fff7e6 100%);
}

.confidence-circle.low {
  border-color: #f56c6c;
  background: linear-gradient(135deg, #fff5f5 0%, #fef0f0 100%);
}

.confidence-number {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.confidence-label {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.prediction-result {
  text-align: center;
}

.predicted-category {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 15px;
}

.category-label {
  font-size: 16px;
  color: #606266;
}

.auto-classification, .manual-classification {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  border-radius: 6px;
  font-size: 14px;
}

.auto-classification {
  background: #f0f9ff;
  color: #67c23a;
  border: 1px solid #b3e19d;
}

.manual-classification {
  background: #fdf6ec;
  color: #e6a23c;
  border: 1px solid #f5dab1;
}

.success-icon {
  color: #67c23a;
}

.warning-icon {
  color: #e6a23c;
}

.action-buttons {
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
}

.corrected-result {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 16px;
}

/* 批量处理样式 */
.batch-results-card {
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.batch-summary {
  padding: 20px 0;
}

.summary-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 30px;
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.batch-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
  flex-wrap: wrap;
}

.batch-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.batch-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.batch-item:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.batch-image {
  position: relative;
  height: 150px;
  overflow: hidden;
}

.batch-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.batch-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  color: white;
  font-size: 24px;
}

.batch-item:hover .batch-overlay {
  opacity: 1;
}

.batch-info {
  padding: 15px;
}

.batch-info .file-name {
  font-size: 14px;
  color: #303133;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.batch-info .prediction {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.batch-info .confidence {
  font-size: 12px;
  color: #909399;
}

.batch-info .status {
  text-align: center;
}

/* 修正对话框样式 */
.correct-content {
  .current-result {
    margin-bottom: 20px;
    
    h4 {
      margin: 0 0 15px 0;
      color: #303133;
      font-size: 16px;
    }
    
    .prediction-display {
      display: flex;
      gap: 20px;
      align-items: flex-start;
      
      .preview-image {
        width: 120px;
        height: 120px;
        object-fit: cover;
        border-radius: 8px;
        border: 1px solid #dcdfe6;
      }
      
      .prediction-info {
        flex: 1;
        
        p {
          margin: 8px 0;
          color: #606266;
          
          strong {
            color: #303133;
          }
        }
      }
    }
  }
}

.preview-container {
  text-align: center;

  .preview-image {
    max-width: 100%;
    max-height: 70vh;
    object-fit: contain;
  }
}

.category-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
  
  .category-code {
    color: #909399;
    font-size: 12px;
    margin-left: 8px;
  }
  
  .category-description {
    color: #909399;
    font-size: 12px;
    line-height: 1.2;
  }
}

.category-code {
  color: #909399;
  font-size: 12px;
}

.category-description {
  color: #909399;
  font-size: 12px;
}
</style>
