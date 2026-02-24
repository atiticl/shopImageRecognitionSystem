<template>
  <div class="model-management">
    <!-- 操作栏 -->
    <el-card class="operation-card">
      <div class="operation-row">
        <div class="operation-left">
          <el-input
            v-model="searchForm.name"
            placeholder="搜索模型名称"
            clearable
            @keyup.enter="searchModels"
            style="width: 200px;"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          
          <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 120px; margin-left: 12px;">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="禁用" value="INACTIVE" />
            <el-option label="训练中" value="TRAINING" />
          </el-select>
          
          <el-select v-model="searchForm.type" placeholder="模型类型" clearable style="width: 120px; margin-left: 12px;">
            <el-option label="通用分类" value="GENERAL" />
            <el-option label="服装分类" value="CLOTHING" />
            <el-option label="电子产品" value="ELECTRONICS" />
          </el-select>
          
          <el-button type="primary" @click="searchModels" style="margin-left: 12px;">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>
        
        <div class="operation-right">
          <el-button type="success" @click="showAddDialog">
            <el-icon><Plus /></el-icon>
            新增模型
          </el-button>
          <el-button @click="goToTrainingPage">
            <el-icon><VideoPlay /></el-icon>
            训练模型
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 模型表格 -->
    <el-card class="table-card">
      <div class="table-container">
        <el-table
          :data="models"
          v-loading="loading"
          stripe
          border
          height="100%"
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="id" label="ID" width="60" />
        
        <el-table-column prop="name" label="模型名称" min-width="160">
          <template #default="{ row }">
            <div class="model-name">
              <el-icon class="model-icon"><Setting /></el-icon>
              {{ row.name }}
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="version" label="版本" width="140" />
        
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">
              {{ getTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="accuracy" label="准确率" width="80">
          <template #default="{ row }">
            <span :class="getAccuracyClass(row.accuracy)">
              {{ row.accuracy }}%
            </span>
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="状态" width="95">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="fileSize" label="文件大小" width="100" />
        
        <el-table-column prop="trainingSamples" label="训练样本" width="90" />
        
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewModel(row)">
              查看
            </el-button>
            <el-button type="success" size="small" @click="testModel(row)">
              测试
            </el-button>
            <el-button size="small" @click="editModel(row)">
              编辑
            </el-button>
            <el-button 
              :type="row.status === 'ACTIVE' ? 'warning' : 'success'" 
              size="small" 
              @click="toggleStatus(row)"
              :disabled="row.status === 'TRAINING'"
            >
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" size="small" @click="deleteModel(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>
      
      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          :hide-on-single-page="false"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑模型' : '新增模型'"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="modelForm"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="modelForm.name" placeholder="请输入模型名称" />
        </el-form-item>
        
        <el-form-item label="版本" prop="version">
          <el-input v-model="modelForm.version" placeholder="如: v1.0" />
        </el-form-item>
        
        <el-form-item label="模型类型" prop="type">
          <el-select v-model="modelForm.type" placeholder="选择模型类型">
            <el-option label="通用分类" value="GENERAL" />
            <el-option label="服装分类" value="CLOTHING" />
            <el-option label="电子产品" value="ELECTRONICS" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="模型文件" prop="modelFile" v-if="!isEdit">
          <el-upload
            ref="uploadRef"
            :action="`/api/models/upload`"
            :headers="uploadHeaders"
            :show-file-list="false"
            :before-upload="beforeUpload"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            accept=".onnx,.pb,.h5,.pth,.pt"
          >
            <el-button>
              <el-icon><Upload /></el-icon>
              选择文件
            </el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持 .onnx, .pb, .h5, .pth, .pt 格式，文件大小不超过 500MB
              </div>
            </template>
          </el-upload>
          <div v-if="modelForm.fileName" class="uploaded-file">
            已选择: {{ modelForm.fileName }}
          </div>
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="modelForm.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="INACTIVE">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="modelForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入模型描述"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 模型详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="模型详情" width="800px">
      <div v-if="currentModel" class="model-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="模型ID">{{ currentModel.id }}</el-descriptions-item>
          <el-descriptions-item label="模型名称">{{ currentModel.name }}</el-descriptions-item>
          <el-descriptions-item label="版本">{{ currentModel.version }}</el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag :type="getTypeTagType(currentModel.type)">
              {{ getTypeText(currentModel.type) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentModel.status)">
              {{ getStatusText(currentModel.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="准确率">
            <span :class="getAccuracyClass(currentModel.accuracy)">
              {{ currentModel.accuracy }}%
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ currentModel.fileSize }}</el-descriptions-item>
          <el-descriptions-item label="训练样本">{{ currentModel.trainingSamples }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(currentModel.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="最后更新">{{ formatTime(currentModel.updateTime) }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">
            {{ currentModel.description || '-' }}
          </el-descriptions-item>
        </el-descriptions>
        
        <div class="model-metrics">
          <h4>性能指标</h4>
          <el-row :gutter="16">
            <el-col :span="6">
              <div class="metric-item">
                <div class="metric-value">{{ currentModel.precision || 0 }}%</div>
                <div class="metric-label">精确率</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="metric-item">
                <div class="metric-value">{{ currentModel.recall || 0 }}%</div>
                <div class="metric-label">召回率</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="metric-item">
                <div class="metric-value">{{ currentModel.f1Score || 0 }}%</div>
                <div class="metric-label">F1分数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="metric-item">
                <div class="metric-value">{{ currentModel.inferenceTime || 0 }}ms</div>
                <div class="metric-label">推理时间</div>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="testModel(currentModel)">测试模型</el-button>
      </template>
    </el-dialog>

    <!-- 模型训练对话框 -->
    <!-- 
    <el-dialog v-model="trainingDialogVisible" title="训练模型" width="600px">
      <el-form :model="trainingForm" label-width="120px" ref="trainingFormRef">
        <el-form-item label="训练数据集（ZIP）">
          <el-upload
            class="dataset-upload"
            :show-file-list="true"
            :auto-upload="false"
            action="#"
            accept=".zip"
            :limit="1"
            :before-upload="beforeDatasetUpload"
            :on-change="handleDatasetChange"
            :on-remove="handleDatasetRemove"
          >
            <el-button type="primary">
              <el-icon><Upload /></el-icon>
              选择数据集ZIP文件
            </el-button>
            <template #tip>
              <div class="el-upload__tip">请上传包含图片及标签的ZIP压缩包（≤500MB），将使用该数据集进行真实训练以产出真实准确率。</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="或选择预置数据集">
          <el-select v-model="trainingForm.datasetId" placeholder="选择数据集">
            <el-option label="服装数据集 v1.0" value="1" />
            <el-option label="电子产品数据集 v1.0" value="2" />
            <el-option label="通用商品数据集 v2.0" value="3" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="基础模型">
          <el-select v-model="trainingForm.baseModelId" placeholder="选择基础模型">
            <el-option label="ResNet50" value="resnet50" />
            <el-option label="EfficientNet" value="efficientnet" />
            <el-option label="MobileNet" value="mobilenet" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="训练轮数">
          <el-input-number v-model="trainingForm.epochs" :min="1" :max="1000" />
        </el-form-item>
        
        <el-form-item label="学习率">
          <el-input-number v-model="trainingForm.learningRate" :min="0.0001" :max="1" :step="0.0001" :precision="4" />
        </el-form-item>
        
        <el-form-item label="批次大小">
          <el-input-number v-model="trainingForm.batchSize" :min="1" :max="512" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="trainingDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="startTraining">开始训练</el-button>
      </template>
    </el-dialog>
    -->

    <!-- 模型测试对话框 -->
    <el-dialog v-model="testDialogVisible" title="模型测试" width="600px">
      <div class="test-content">
        <el-upload
          :show-file-list="false"
          :before-upload="beforeTestUpload"
          :auto-upload="true"
          action="#"
          accept="image/*"
          :on-success="handleTestUploadSuccess"
          :http-request="handleTestUpload"
        >
          <el-button type="primary">
            <el-icon><Upload /></el-icon>
            上传测试图片
          </el-button>
          <template #tip>
            <div class="el-upload__tip">
              支持 jpg、png、jpeg 等图片格式，文件大小不超过 10MB
            </div>
          </template>
        </el-upload>
        
        <div v-if="testResult" class="test-result">
          <h4>测试结果</h4>
          <div class="result-item">
            <span class="label">预测类别:</span>
            <span class="value">{{ testResult.category }}</span>
          </div>
          <div class="result-item">
            <span class="label">置信度:</span>
            <span class="value">{{ testResult.confidence }}%</span>
          </div>
          <div class="result-item">
            <span class="label">处理时间:</span>
            <span class="value">{{ testResult.processingTime }}ms</span>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="testDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, VideoPlay, Setting, Upload } from '@element-plus/icons-vue'
import { http } from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { modelApi, type Model, type ModelTestResult } from '@/api/model'

const userStore = useUserStore()

// 上传配置
const uploadHeaders = computed(() => ({
  'Authorization': `Bearer ${userStore.token}`
}))
const uploadRef = ref()

// 搜索表单
const searchForm = reactive({
  name: '',
  status: '',
  type: ''
})

// 分页
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 表格数据
const models = ref([])

// 表单相关
const dialogVisible = ref(false)
const detailDialogVisible = ref(false)
// const trainingDialogVisible = ref(false)
const testDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
// const trainingFormRef = ref()
const currentModel = ref<any>(null)

const modelForm = reactive({
  id: null,
  name: '',
  version: '',
  type: '',
  fileName: '',
  fileUrl: '',
  status: 'ACTIVE',
  description: '',
  framework: '',
  threshold: 0.5
})



const formRules = {
  name: [
    { required: true, message: '请输入模型名称', trigger: 'blur' }
  ],
  version: [
    { required: true, message: '请输入版本号', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择模型类型', trigger: 'change' }
  ]
}

// 其他状态
const loading = ref(false)
const submitting = ref(false)
const selectedModels = ref([])
const testResult = ref<any>(null)

// 获取状态类型
const getStatusType = (status: string) => {
  const statusMap: Record<string, string> = {
    'ACTIVE': 'success',
    'INACTIVE': 'danger',
    'TRAINING': 'warning'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'ACTIVE': '启用',
    'INACTIVE': '禁用',
    'TRAINING': '训练中'
  }
  return statusMap[status] || '未知'
}

// 获取类型标签类型
const getTypeTagType = (type: string) => {
  const typeMap: Record<string, string> = {
    'GENERAL': 'primary',
    'CLOTHING': 'success',
    'ELECTRONICS': 'warning'
  }
  return typeMap[type] || 'info'
}

// 获取类型文本
const getTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    'GENERAL': '通用分类',
    'CLOTHING': '服装分类',
    'ELECTRONICS': '电子产品'
  }
  return typeMap[type] || '未知'
}

// 获取准确率样式类
const getAccuracyClass = (accuracy: number) => {
  if (accuracy >= 95) return 'accuracy-excellent'
  if (accuracy >= 90) return 'accuracy-good'
  if (accuracy >= 80) return 'accuracy-normal'
  return 'accuracy-poor'
}

// 搜索模型
const searchModels = () => {
  pagination.page = 1
  loadModels()
}

// 重置搜索
const resetSearch = () => {
  Object.assign(searchForm, {
    name: '',
    status: '',
    type: ''
  })
  pagination.page = 1
  loadModels()
}

// 加载模型列表
const loadModels = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1, // 后端从0开始
      size: pagination.size,
      name: searchForm.name || undefined,
      status: searchForm.status || undefined,
      type: searchForm.type || undefined
    }
    
    const response = await modelApi.getModels(params)
    // 修复：使用response.data来访问分页数据
    const data = response.data || response
    models.value = data.content || []
    pagination.total = data.totalElements || 0
  } catch (error: any) {
    console.error('加载模型列表失败:', error)
    ElMessage.error('加载模型列表失败')
    models.value = []
  } finally {
    loading.value = false
  }
}

// 显示新增对话框
const showAddDialog = () => {
  isEdit.value = false
  dialogVisible.value = true
}

// 显示训练对话框
/*
const showTrainingDialog = (row: any) => {
  currentModel.value = row
  // 重置训练表单
  trainingForm.datasetId = ''
  trainingForm.baseModelId = ''
  trainingForm.epochs = 10
  trainingForm.learningRate = 0.001
  trainingForm.batchSize = 32
  trainingDatasetFile.value = null
  trainingDialogVisible.value = true
}
*/

// 跳转到训练页面
const goToTrainingPage = () => {
  window.open('http://localhost:5000/', '_blank')
}

// 查看模型详情
const viewModel = async (row: any) => {
  try {
    const response = await modelApi.getModel(row.id)
    // 修复：使用response.data来访问模型详情数据
    currentModel.value = response.data || response || row
    detailDialogVisible.value = true
  } catch (error: any) {
    console.error('获取模型详情失败:', error)
    ElMessage.error('获取模型详情失败')
  }
}

// 测试模型
const testModel = (row: any) => {
  currentModel.value = row
  testResult.value = null
  testDialogVisible.value = true
}

// 编辑模型
const editModel = (row: any) => {
  isEdit.value = true
  modelForm.id = row.id
  modelForm.name = row.name
  modelForm.version = row.version
  modelForm.type = row.type
  modelForm.status = row.status
  modelForm.description = row.description
  modelForm.framework = row.framework || ''
  modelForm.threshold = row.threshold || 0.5
  modelForm.fileName = row.fileName || ''
  modelForm.fileUrl = row.fileUrl || ''
  dialogVisible.value = true
}

// 切换状态
const toggleStatus = async (row: any) => {
  const action = row.status === 'ACTIVE' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}模型 "${row.name}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await modelApi.toggleModelStatus(row.id)
    ElMessage.success(`${action}成功`)
    loadModels() // 刷新列表
  } catch (error: any) {
    if (error.message) {
      console.error('切换状态失败:', error)
      ElMessage.error(error.message || '操作失败')
    }
    // 用户取消操作不显示错误
  }
}

// 删除模型
const deleteModel = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除模型 "${row.name}" 吗？删除后无法恢复。`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })
    
    await modelApi.deleteModel(row.id)
    ElMessage.success('删除成功')
    loadModels() // 刷新列表
  } catch (error: any) {
    if (error.message) {
      console.error('删除模型失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
    // 用户取消操作不显示错误
  }
}

// 文件上传前处理
const beforeUpload = (file: File) => {
  const validExtensions = ['.onnx', '.pb', '.h5', '.pth', '.pt']
  const isValidFormat = validExtensions.some(ext => file.name.toLowerCase().endsWith(ext))
  
  if (!isValidFormat) {
    ElMessage.error('只能上传 .onnx, .pb, .h5, .pth, .pt 格式的模型文件!')
    return false
  }
  
  const isLt500M = file.size / 1024 / 1024 < 500
  if (!isLt500M) {
    ElMessage.error('文件大小不能超过 500MB!')
    return false
  }
  
  return true
}

// 文件上传成功
const handleUploadSuccess = (response: any, file: File) => {
  modelForm.fileName = file.name
  modelForm.fileUrl = response.fileUrl || response.data?.fileUrl
  ElMessage.success('文件上传成功')
}

// 文件上传失败
const handleUploadError = (error: any) => {
  console.error('文件上传失败:', error)
  ElMessage.error('文件上传失败，请重试')
}

// 测试图片上传前处理
const beforeTestUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB!')
    return false
  }
  
  return true
}

// 自定义测试图片上传方法
const handleTestUpload = async (options: any) => {
  try {
    ElMessage.success('图片上传成功，正在进行分类...')
    
    // 调用训练后端的测试接口
    const testResponse = await modelApi.testModelWithTrainBackend(options.file)
    
    console.log('预测响应:', testResponse) // 添加调试日志
    
    if (testResponse.message === '预测成功' && testResponse.result) {
      const result = testResponse.result
      
      console.log('预测结果:', result) // 添加调试日志
      
      // 处理预测结果，选择置信度最高的预测
      let bestPrediction = null
      if (result.predictions && result.predictions.length > 0) {
        console.log('预测列表:', result.predictions) // 添加调试日志
        bestPrediction = result.predictions.reduce((prev: any, current: any) => 
          (prev.confidence > current.confidence) ? prev : current
        )
        console.log('最佳预测:', bestPrediction) // 添加调试日志
      }
      
      if (bestPrediction) {
        testResult.value = {
          category: bestPrediction.class_name || bestPrediction.class || '未知类别',
          confidence: Math.round(bestPrediction.confidence * 100), // 转换为百分比
          processingTime: 100 // 默认处理时间，实际可以从响应中获取
        }
        ElMessage.success('模型测试完成')
        options.onSuccess(testResponse)
      } else {
        testResult.value = {
          category: '未识别',
          confidence: 0,
          processingTime: 100
        }
        ElMessage.warning('未检测到任何物体')
        options.onSuccess(testResponse)
      }
    } else {
      console.log('预测失败响应:', testResponse) // 添加调试日志
      ElMessage.error(testResponse.error || '模型测试失败')
      options.onError(new Error(testResponse.error || '模型测试失败'))
    }
  } catch (error: any) {
    console.error('模型测试失败:', error)
    ElMessage.error(error.response?.data?.error || error.message || '模型测试失败，请确保训练后端服务正在运行')
    options.onError(error)
  }
}

// 测试图片上传成功
const handleTestUploadSuccess = async (response: any, file: File) => {
  // 这个方法现在由 handleTestUpload 处理，保留为空以避免重复处理
}

// 开始训练
/*
const startTraining = async () => {
  if (!trainingFormRef.value) return
  
  try {
    await trainingFormRef.value.validate()
    trainingSubmitting.value = true
    // 如果选择了ZIP数据集，走真实训练接口
    if (trainingDatasetFile.value) {
      const formData = new FormData()
      formData.append('dataset', trainingDatasetFile.value)
      formData.append('epochs', String(trainingForm.epochs))
      formData.append('batchSize', String(trainingForm.batchSize))
      formData.append('learningRate', String(trainingForm.learningRate))
      // 可选参数：基础模型/优化器/模型类型，根据后端需要传递
      if (trainingForm.baseModelId) formData.append('modelType', trainingForm.baseModelId)
      
      await modelApi.startTrainingWithDataset(currentModel.value.id, formData)
      ElMessage.success('训练任务已启动（使用真实数据集）')
    } else {
      ElMessage.error('请上传训练数据集ZIP文件，以保证准确率为真实值')
      return
    }
    
    trainingDialogVisible.value = false
    loadModels() // 刷新列表以更新模型状态
    
  } catch (error: any) {
    console.error('启动训练失败:', error)
    ElMessage.error(error.message || '启动训练失败')
  } finally {
    trainingSubmitting.value = false
  }
}
*/

// 数据集上传校验（ZIP）
/*
const beforeDatasetUpload = (file: File) => {
  const isZip = file.name.toLowerCase().endsWith('.zip')
  if (!isZip) {
    ElMessage.error('请上传 .zip 格式的数据集压缩包')
    return false
  }
  const isLt500M = file.size / 1024 / 1024 < 500
  if (!isLt500M) {
    ElMessage.error('数据集大小不能超过 500MB')
    return false
  }
  return true
}

// 选择数据集文件
const handleDatasetChange = (file: any) => {
  // Element Plus Upload 的 file.raw 为原始File对象
  trainingDatasetFile.value = file?.raw || null
}

// 移除数据集文件
const handleDatasetRemove = () => {
  trainingDatasetFile.value = null
}
*/

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    submitting.value = true
    
    if (isEdit.value) {
      // 编辑模型 - 修复：添加status参数到updateModel调用
      await modelApi.updateModel(modelForm.id!, {
        name: modelForm.name,
        version: modelForm.version,
        type: modelForm.type,
        framework: modelForm.framework,
        description: modelForm.description,
        threshold: modelForm.threshold
      })
      
      ElMessage.success('更新成功')
    } else {
      // 新增模型
      if (!modelForm.fileUrl) {
        ElMessage.error('请先上传模型文件')
        return
      }
      
      await modelApi.createModel({
        name: modelForm.name,
        version: modelForm.version,
        type: modelForm.type,
        framework: modelForm.framework || 'ONNX',
        fileUrl: modelForm.fileUrl,
        status: modelForm.status,
        description: modelForm.description,
        threshold: modelForm.threshold
      })
      
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    loadModels() // 刷新列表
    
  } catch (error: any) {
    console.error('提交失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  Object.assign(modelForm, {
    id: null,
    name: '',
    version: '',
    type: '',
    fileName: '',
    status: 'ACTIVE',
    description: ''
  })
}

// 处理选择变化
const handleSelectionChange = (selection: any[]) => {
  selectedModels.value = selection
}

// 格式化时间
const formatTime = (time: string) => {
  return new Date(time).toLocaleString()
}

// 分页处理
const handleSizeChange = (size: number) => {
  pagination.size = size
  loadModels()
}

const handlePageChange = (page: number) => {
  pagination.page = page
  loadModels()
}

onMounted(() => {
  loadModels()
})
</script>

<style scoped>
.model-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 20px;
  background: transparent;
  position: relative;
  overflow: hidden;
}

.model-management::before {
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

.operation-card {
  flex-shrink: 0;
  margin-bottom: 16px;
  border-radius: 16px;
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

.table-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  border: none;
  box-shadow: 
    0 12px 40px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 1;
  overflow: hidden;
  min-height: 0;
}

.operation-card:hover, .table-card:hover {
  transform: translateY(-4px);
  box-shadow: 
    0 20px 60px rgba(0, 0, 0, 0.12),
    0 0 0 1px rgba(255, 255, 255, 0.9);
}

:deep(.operation-card .el-card__body) {
  padding: 16px 20px;
}

:deep(.table-card .el-card__body) {
  padding: 20px;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.operation-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: nowrap;
}

.operation-left {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
}

.operation-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.table-container {
  flex: 1;
  overflow: hidden;
  min-height: 0;
}

.pagination-wrapper {
  flex-shrink: 0;
  display: flex !important;
  justify-content: flex-end;
  padding: 12px 0 0 0;
  border-top: 1px solid rgba(226, 232, 240, 0.6);
  margin-top: 12px;
  visibility: visible !important;
  opacity: 1 !important;
  min-height: 48px;
  position: relative;
  z-index: 10;
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

/* 模型名称样式 */
.model-name {
  display: flex;
  align-items: center;
  font-weight: 600;
  color: #1e293b;
}

.model-icon {
  margin-right: 12px;
  color: #3b82f6;
  font-size: 18px;
}

/* 准确率样式 */
.accuracy-high {
  color: #10b981;
  font-weight: 700;
}

.accuracy-medium {
  color: #f59e0b;
  font-weight: 700;
}

.accuracy-low {
  color: #ef4444;
  font-weight: 700;
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

:deep(.el-tag--primary) {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
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

/* 分页样式 */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 32px;
  position: relative;
  z-index: 1;
}

.pagination-right {
  margin-left: auto;
}

/* 分页样式 */
:deep(.el-pagination) {
  background: transparent;
  padding: 0;
  display: flex !important;
  visibility: visible !important;
}

:deep(.el-pagination .el-pager li) {
  border-radius: 8px;
  margin: 0 2px;
  transition: all 0.3s ease;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

:deep(.el-pagination .el-pager li:hover) {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
  transform: translateY(-1px);
  border-color: transparent;
}

:deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
  border-color: transparent;
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next) {
  border-radius: 8px;
  font-weight: 600;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

:deep(.el-pagination .btn-prev:hover),
:deep(.el-pagination .btn-next:hover) {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
  border-color: transparent;
}

:deep(.el-pagination .el-select .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(226, 232, 240, 0.6);
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

/* 表单样式 */
:deep(.el-form-item__label) {
  font-weight: 600;
  color: #374151;
  font-size: 14px;
  letter-spacing: 0.5px;
}

:deep(.el-textarea .el-textarea__inner) {
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  transition: all 0.3s ease;
}

:deep(.el-textarea .el-textarea__inner:hover) {
  border-color: rgba(59, 130, 246, 0.3);
}

:deep(.el-textarea .el-textarea__inner:focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

/* 单选框样式 */
:deep(.el-radio) {
  margin-right: 24px;
  font-weight: 600;
}

:deep(.el-radio__input.is-checked .el-radio__inner) {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  border-color: #3b82f6;
}

/* 数字输入框样式 */
:deep(.el-input-number) {
  border-radius: 12px;
}

:deep(.el-input-number .el-input__wrapper) {
  border-radius: 12px;
}

/* 上传组件样式 */
:deep(.el-upload) {
  border-radius: 12px;
}

:deep(.el-upload-dragger) {
  border-radius: 12px;
  border: 2px dashed rgba(226, 232, 240, 0.8);
  background: rgba(248, 250, 252, 0.5);
  transition: all 0.3s ease;
}

:deep(.el-upload-dragger:hover) {
  border-color: #3b82f6;
  background: rgba(59, 130, 246, 0.05);
}

.uploaded-file {
  margin-top: 12px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  display: inline-block;
}

/* 模型详情样式 */
.model-detail {
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

.model-metrics {
  margin-top: 24px;
  padding: 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.model-metrics h4 {
  margin-bottom: 20px;
  color: #1e293b;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.metric-item {
  text-align: center;
  padding: 20px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.metric-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
}

.metric-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 600;
  letter-spacing: 0.5px;
}

/* 测试内容样式 */
.test-content {
  text-align: center;
  padding: 24px 0;
}

.test-result {
  margin-top: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 12px;
  text-align: left;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.test-result h4 {
  margin-bottom: 16px;
  color: #1e293b;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.result-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(226, 232, 240, 0.4);
}

.result-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.result-item .label {
  color: #64748b;
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 0.5px;
}

.result-item .value {
  color: #1e293b;
  font-weight: 700;
  font-size: 16px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .model-management {
    padding: 16px;
  }
}

@media (max-width: 768px) {
  .model-management {
    padding: 12px;
  }
  
  .operation-card, .table-card {
    margin-bottom: 20px;
  }
  
  .operation-row .el-col {
    margin-bottom: 16px;
  }
  
  .operation-right {
    text-align: left;
  }
  
  .metric-item {
    margin-bottom: 16px;
  }
  
  .pagination-wrapper {
    margin-top: 20px;
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
  
  .model-metrics {
    padding: 16px;
  }
  
  .metric-value {
    font-size: 20px;
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

/* 表格行动画 */
:deep(.el-table__row) {
  transition: all 0.3s ease;
}

/* 模型图标悬停效果 */
.model-icon:hover {
  transform: scale(1.1);
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

/* 上传提示样式 */
:deep(.el-upload__tip) {
  color: #64748b;
  font-size: 12px;
  margin-top: 8px;
  font-weight: 500;
}
</style>