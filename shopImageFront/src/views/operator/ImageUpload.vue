<template>
  <div class="image-upload">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>图片上传</span>
          <el-button type="primary" @click="createTask" :disabled="!canCreateTask">
            创建分类任务
          </el-button>
        </div>
      </template>

      <!-- 上传模式选择 -->
      <div class="upload-mode-section">
        <el-tabs v-model="uploadMode" @tab-change="handleModeChange">
          <el-tab-pane label="单图片上传" name="single">
            <!-- 单图片上传区域 -->
            <div class="upload-section">
              <el-upload
                ref="uploadRef"
                class="upload-dragger"
                drag
                :action="uploadAction"
                :headers="uploadHeaders"
                :data="uploadData"
                :file-list="fileList"
                :before-upload="beforeUpload"
                :on-success="handleUploadSuccess"
                :on-error="handleUploadError"
                :on-remove="handleRemove"
                :on-preview="handlePreview"
                multiple
                accept="image/*"
                :limit="50"
                :on-exceed="handleExceed"
              >
                <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                <div class="el-upload__text">
                  将图片拖拽到此处，或<em>点击上传</em>
                </div>
                <template #tip>
                  <div class="el-upload__tip">
                    支持 jpg/png/gif 格式，单个文件不超过 10MB，最多上传 50 张图片
                  </div>
                </template>
              </el-upload>
            </div>
          </el-tab-pane>
          
          <el-tab-pane label="压缩包上传" name="batch">
            <!-- 压缩包上传区域 -->
            <div class="upload-section">
              <el-upload
                ref="batchUploadRef"
                class="upload-dragger"
                drag
                :action="batchUploadAction"
                :headers="uploadHeaders"
                :data="batchUploadData"
                accept=".zip,application/zip,application/x-zip-compressed"
                :before-upload="beforeBatchUpload"
                :on-success="handleBatchUploadSuccess"
                :on-error="handleBatchUploadError"
                :file-list="batchFileList"
                :limit="1"
                :on-exceed="handleBatchExceed"
              >
                <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                <div class="el-upload__text">
                  将压缩包拖拽到此处，或<em>点击上传</em>
                </div>
                <template #tip>
                  <div class="el-upload__tip">
                    仅支持 zip 格式，单个文件不超过 500MB，包含多张图片
                  </div>
                </template>
              </el-upload>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 上传进度 -->
      <div v-if="uploading" class="upload-progress">
        <el-progress 
          :percentage="uploadProgress" 
          :status="uploadStatus"
          :stroke-width="8"
        />
        <p class="progress-text">
          {{ uploadMode === 'batch' ? '正在处理压缩包...' : `正在上传... ${uploadedCount}/${totalCount}` }}
        </p>
      </div>

      <!-- 批量上传成功提示 -->
      <div v-if="batchTaskCreated" class="batch-success">
        <el-alert
          title="压缩包上传成功"
          :description="`任务已创建，正在后台处理压缩包中的图片。任务ID: ${createdTaskId}`"
          type="success"
          show-icon
          :closable="false"
        />
        <div class="batch-actions">
          <el-button type="primary" @click="goToTaskList">查看任务列表</el-button>
          <el-button @click="resetUpload">继续上传</el-button>
        </div>
      </div>

      <!-- 已上传文件列表 -->
      <div v-if="uploadedFiles.length > 0 && uploadMode === 'single'" class="uploaded-files">
        <h3>已上传文件 ({{ uploadedFiles.length }})</h3>
        <el-row :gutter="16" class="file-grid">
          <el-col 
            v-for="file in uploadedFiles" 
            :key="file.id"
            :xs="12" :sm="8" :md="6" :lg="4"
          >
            <div class="file-item">
              <div class="file-preview" @click="previewImage(file)">
                <img :src="file.url" :alt="file.name" />
                <div class="file-overlay">
                  <el-icon><ZoomIn /></el-icon>
                </div>
              </div>
              <div class="file-info">
                <div class="file-name" :title="file.name">{{ file.name }}</div>
                <div class="file-size">{{ formatFileSize(file.size) }}</div>
              </div>
              <div class="file-actions">
                <el-button 
                  type="danger" 
                  size="small" 
                  text
                  @click="removeFile(file)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 任务配置 -->
      <div v-if="canShowTaskConfig" class="task-config">
        <h3>任务配置</h3>
        <el-form :model="taskForm" :rules="taskRules" ref="taskFormRef" label-width="100px">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="任务名称" prop="taskName">
                <el-input 
                  v-model="taskForm.taskName" 
                  placeholder="请输入任务名称"
                  maxlength="50"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="分类模型" prop="modelId">
                <el-select v-model="taskForm.modelId" placeholder="请选择分类模型" style="width: 100%">
                  <el-option
                    v-for="model in availableModels"
                    :key="model.id"
                    :label="model.name"
                    :value="model.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="任务描述" prop="description">
            <el-input
              v-model="taskForm.description"
              type="textarea"
              :rows="3"
              placeholder="请输入任务描述（可选）"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <!-- 图片预览对话框 -->
    <el-dialog v-model="previewVisible" title="图片预览" width="60%" center>
      <div class="preview-container">
        <img :src="previewUrl" :alt="previewName" class="preview-image" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadInstance, UploadProps, UploadUserFile } from 'element-plus'
import { UploadFilled, ZoomIn } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import axios from 'axios'

const router = useRouter()
const userStore = useUserStore()

const uploadRef = ref<UploadInstance>()
const batchUploadRef = ref<UploadInstance>()
const taskFormRef = ref<FormInstance>()

// 上传模式
const uploadMode = ref('single')

// 上传配置
const uploadAction = '/api/images/upload'
const batchUploadAction = '/api/images/upload-batch'
const uploadHeaders = computed(() => ({
  'Authorization': `Bearer ${userStore.token}`
}))
const uploadData = reactive({
  type: 'classification'
})
const batchUploadData = reactive({
  type: 'batch'
})

// 文件列表
const fileList = ref<UploadUserFile[]>([])
const batchFileList = ref<UploadUserFile[]>([])
const uploadedFiles = ref<any[]>([])

// 上传状态
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadStatus = ref<'success' | 'exception' | ''>('')
const uploadedCount = ref(0)
const totalCount = ref(0)

// 批量上传状态
const batchTaskCreated = ref(false)
const createdTaskId = ref('')

// 任务表单
const taskForm = reactive({
  taskName: '',
  modelId: '',
  description: ''
})

const taskRules: FormRules = {
  taskName: [
    { required: true, message: '请输入任务名称', trigger: 'blur' },
    { min: 2, max: 50, message: '任务名称长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  modelId: [
    { required: true, message: '请选择分类模型', trigger: 'change' }
  ]
}

// 可用模型
const availableModels = ref<any[]>([])

// 加载可用模型
const loadAvailableModels = async () => {
  try {
    // 直接调用Python训练后端获取训练模型列表
    const response = await axios.get('http://localhost:5000/api/models')
    
    if (response.data && response.data.models) {
      const models = response.data.models
      
      // 转换模型数据格式，优先显示训练结果模型
      availableModels.value = models
        .filter((model: any) => {
          // 过滤出有效的模型文件（大小大于0）
          return model.size > 0 && model.name.endsWith('.pt')
        })
        .map((model: any, index: number) => {
          // 生成友好的显示名称
          let displayName = model.name
          if (model.source && model.source.includes('runs/detect')) {
            // 训练结果模型，提取训练名称和模型类型
            const parts = model.name.split('_')
            if (parts.length >= 2) {
              const trainName = parts[0] // train3, train4 等
              const modelType = parts[parts.length - 1].replace('.pt', '') // best, last, epoch10 等
              displayName = `${trainName} - ${modelType}`
            }
          }
          
          return {
            id: `model_${index}`, // 使用索引作为ID
            name: displayName,
            originalName: model.name,
            path: model.path,
            size: model.size,
            sizeFormatted: model.size_formatted,
            createdTime: model.created_time,
            type: model.type,
            source: model.source,
            accuracy: model.accuracy,
            precision: model.precision,
            recall: model.recall,
            map50: model.map50,
            epochs: model.epochs,
            trainingSamples: model.training_samples,
            // 标记为可用状态
            status: 'ACTIVE'
          }
        })
        .sort((a: any, b: any) => {
          // 按创建时间降序排序，最新的在前面
          return new Date(b.createdTime).getTime() - new Date(a.createdTime).getTime()
        })
      
      console.log('Loaded training models:', availableModels.value)
      
      if (availableModels.value.length === 0) {
        ElMessage.warning('暂无可用的训练模型，请先在模型管理中训练模型')
      } else {
        ElMessage.success(`加载了 ${availableModels.value.length} 个训练模型`)
      }
    } else {
      ElMessage.error('获取模型列表失败：响应格式错误')
    }
  } catch (error: any) {
    console.error('加载模型列表失败:', error)
    if (error.code === 'ECONNREFUSED' || error.message.includes('Network Error')) {
      ElMessage.error('无法连接到训练后端服务，请确保训练后端正在运行')
    } else {
      ElMessage.error('加载模型列表失败: ' + (error.response?.data?.error || error.message))
    }
  }
}

// 计算属性
const canCreateTask = computed(() => {
  if (uploadMode.value === 'single') {
    return uploadedFiles.value.length > 0
  }
  return false // 批量模式下不显示创建任务按钮，因为上传后自动创建
})

const canShowTaskConfig = computed(() => {
  return uploadMode.value === 'single' && uploadedFiles.value.length > 0
})

// 模式切换
const handleModeChange = (mode: string) => {
  // 清空之前的上传状态
  resetUploadState()
}

// 重置上传状态
const resetUploadState = () => {
  fileList.value = []
  batchFileList.value = []
  uploadedFiles.value = []
  uploading.value = false
  uploadProgress.value = 0
  uploadStatus.value = ''
  uploadedCount.value = 0
  totalCount.value = 0
  batchTaskCreated.value = false
  createdTaskId.value = ''
  
  // 重置表单
  taskForm.taskName = ''
  taskForm.modelId = ''
  taskForm.description = ''
}

// 批量上传前检查
const beforeBatchUpload: UploadProps['beforeUpload'] = (file) => {
  const allowedTypes = ['application/zip', 'application/x-zip-compressed']
  const isValidType = allowedTypes.includes(file.type) || 
                     file.name.toLowerCase().endsWith('.zip')
  const isLt500M = file.size / 1024 / 1024 < 500

  if (!isValidType) {
    ElMessage.error('只能上传 zip 格式的压缩包!')
    return false
  }
  if (!isLt500M) {
    ElMessage.error('上传压缩包大小不能超过 500MB!')
    return false
  }
  
  uploading.value = true
  uploadProgress.value = 0
  uploadStatus.value = ''
  
  return true
}

// 批量上传成功
const handleBatchUploadSuccess = (response: any, file: any) => {
  uploading.value = false
  uploadProgress.value = 100
  uploadStatus.value = 'success'
  
  if (response.success) {
    batchTaskCreated.value = true
    createdTaskId.value = response.data.taskId
    ElMessage.success('压缩包上传成功，任务已创建')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 批量上传失败
const handleBatchUploadError = (error: any) => {
  uploading.value = false
  uploadStatus.value = 'exception'
  ElMessage.error('上传失败: ' + (error.message || '未知错误'))
}

// 批量上传超出限制
const handleBatchExceed = () => {
  ElMessage.warning('一次只能上传一个压缩包')
}

// 跳转到任务列表
const goToTaskList = () => {
  router.push('/operator/tasks')
}

// 重置上传
const resetUpload = () => {
  resetUploadState()
}

// 预览
const previewVisible = ref(false)
const previewUrl = ref('')
const previewName = ref('')

// 上传前检查
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('上传图片大小不能超过 10MB!')
    return false
  }
  return true
}

// 上传成功
const handleUploadSuccess = (response: any, file: any) => {
  // 使用后端的图片查看端点来显示图片
  const imageUrl = `/api/images/view/${response.data.imageId}`
  
  uploadedFiles.value.push({
    id: response.data.imageId, // 修复：后端返回的是imageId而不是id
    name: file.name,
    url: imageUrl,
    size: file.size,
    originalFile: file
  })
  
  uploadedCount.value++
  uploadProgress.value = Math.round((uploadedCount.value / totalCount.value) * 100)
  
  if (uploadedCount.value === totalCount.value) {
    uploading.value = false
    uploadStatus.value = 'success'
    ElMessage.success(`成功上传 ${uploadedCount.value} 张图片`)
    
    // 自动生成任务名称
    if (!taskForm.taskName) {
      const now = new Date()
      taskForm.taskName = `图片分类任务-${now.getFullYear()}${(now.getMonth() + 1).toString().padStart(2, '0')}${now.getDate().toString().padStart(2, '0')}-${now.getHours().toString().padStart(2, '0')}${now.getMinutes().toString().padStart(2, '0')}`
    }
  }
}

// 上传失败
const handleUploadError = (error: any) => {
  uploading.value = false
  uploadStatus.value = 'exception'
  ElMessage.error('上传失败: ' + (error.message || '未知错误'))
}

// 移除文件
const handleRemove = (file: any) => {
  const index = uploadedFiles.value.findIndex(f => f.name === file.name)
  if (index > -1) {
    uploadedFiles.value.splice(index, 1)
  }
}

// 预览文件
const handlePreview = (file: any) => {
  previewImage(file)
}

// 超出限制
const handleExceed = () => {
  ElMessage.warning('最多只能上传 50 张图片')
}

// 预览图片
const previewImage = (file: any) => {
  // 如果是已上传的文件，使用服务器返回的URL
  if (file.url) {
    // 确保URL是完整的，如果是相对路径则添加基础URL
    previewUrl.value = file.url.startsWith('http') ? file.url : `http://localhost:8080${file.url}`
  } else if (file.raw) {
    // 如果是本地文件，创建临时URL
    previewUrl.value = URL.createObjectURL(file.raw)
  } else if (file.response?.data?.url) {
    // 如果是上传响应中的文件
    previewUrl.value = file.response.data.url.startsWith('http') ? file.response.data.url : `http://localhost:8080${file.response.data.url}`
  }
  previewName.value = file.name
  previewVisible.value = true
}

// 删除已上传文件
const removeFile = async (file: any) => {
  try {
    await ElMessageBox.confirm('确定要删除这张图片吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const index = uploadedFiles.value.findIndex(f => f.id === file.id)
    if (index > -1) {
      uploadedFiles.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  } catch (error) {
    // 用户取消
  }
}

// 格式化文件大小
const formatFileSize = (size: number) => {
  if (size < 1024) {
    return size + ' B'
  } else if (size < 1024 * 1024) {
    return (size / 1024).toFixed(1) + ' KB'
  } else {
    return (size / (1024 * 1024)).toFixed(1) + ' MB'
  }
}

// 创建任务
const createTask = async () => {
  if (!taskFormRef.value) return
  
  try {
    await taskFormRef.value.validate()
    
    // 找到选中的模型对象，获取真实的模型名称
    const selectedModel = availableModels.value.find(model => model.id === taskForm.modelId)
    const realModelName = selectedModel ? selectedModel.originalName : taskForm.modelId
    
    const taskData = {
      ...taskForm,
      modelId: realModelName, // 使用真实的模型名称
      imageIds: uploadedFiles.value.map(f => f.id),
      imageCount: uploadedFiles.value.length
    }
    
    // 调用API创建任务
    const response = await axios.post('/api/classification-tasks', taskData, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (response.data.success) {
      ElMessage.success('任务创建成功')
      router.push('/operator/tasks')
    } else {
      ElMessage.error(response.data.message || '任务创建失败')
    }
  } catch (error: any) {
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else {
      ElMessage.error('任务创建失败')
    }
    console.error('创建任务失败:', error)
  }
}

// 组件挂载时加载模型列表
onMounted(() => {
  loadAvailableModels()
})
</script>

<style scoped>
.upload-mode-section {
  margin-bottom: 30px;
}

.upload-mode-section :deep(.el-tabs__content) {
  padding-top: 20px;
}

.batch-success {
  margin: 20px 0;
  padding: 20px;
  background: #f0f9ff;
  border-radius: 8px;
}

.batch-actions {
  margin-top: 16px;
  text-align: center;
}

.batch-actions .el-button {
  margin: 0 8px;
}

.image-upload {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.upload-section {
  margin-bottom: 30px;
}

.upload-dragger {
  width: 100%;
}

:deep(.el-upload-dragger) {
  width: 100%;
  height: 200px;
}

.upload-progress {
  margin: 20px 0;
  text-align: center;
}

.progress-text {
  margin-top: 10px;
  color: #666;
  font-size: 14px;
}

.uploaded-files {
  margin: 30px 0;
}

.uploaded-files h3 {
  margin-bottom: 20px;
  color: #333;
  font-size: 16px;
  font-weight: 600;
}

.file-grid {
  margin-top: 16px;
}

.file-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
  transition: all 0.3s;
}

.file-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.file-preview {
  position: relative;
  width: 100%;
  height: 120px;
  overflow: hidden;
  cursor: pointer;
}

.file-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.file-overlay {
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
  font-size: 20px;
}

.file-preview:hover .file-overlay {
  opacity: 1;
}

.file-info {
  padding: 8px 12px;
}

.file-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 12px;
  color: #999;
}

.file-actions {
  padding: 0 12px 8px;
  text-align: right;
}

.task-config {
  margin-top: 30px;
  padding-top: 30px;
  border-top: 1px solid #e4e7ed;
}

.task-config h3 {
  margin-bottom: 20px;
  color: #333;
  font-size: 16px;
  font-weight: 600;
}

.preview-container {
  text-align: center;
}

.preview-image {
  max-width: 100%;
  max-height: 60vh;
  object-fit: contain;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .file-grid .el-col {
    margin-bottom: 16px;
  }
}
</style>
