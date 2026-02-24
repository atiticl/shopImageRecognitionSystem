<template>
  <div class="category-management">
    <!-- 操作栏 -->
    <el-card class="operation-card">
      <div class="operation-row">
        <div class="operation-left">
          <el-input
            v-model="searchForm.name"
            placeholder="搜索类别名称"
            clearable
            @keyup.enter="searchCategories"
            style="width: 200px;"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          
          <el-select v-model="searchForm.isActive" placeholder="状态" clearable style="width: 120px; margin-left: 12px;">
            <el-option label="启用" :value="true" />
            <el-option label="禁用" :value="false" />
          </el-select>
          
          <el-button type="primary" @click="searchCategories" style="margin-left: 12px;">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>
        
        <div class="operation-right">
          <el-button type="success" @click="showAddDialog">
            <el-icon><Plus /></el-icon>
            新增类别
          </el-button>
          <el-button @click="exportCategories">
            <el-icon><Download /></el-icon>
            导出
          </el-button>
          <el-upload
            :show-file-list="false"
            :before-upload="beforeImport"
            accept=".xlsx,.xls"
            style="display: inline-block; margin-left: 8px;"
          >
            <el-button>
              <el-icon><Upload /></el-icon>
              导入
            </el-button>
          </el-upload>
        </div>
      </div>
    </el-card>

    <!-- 类别表格 - 使用固定高度容器 -->
    <el-card class="table-card">
      <div class="table-container">
        <el-table
          :data="categories"
          v-loading="loading"
          stripe
          border
          height="100%"
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          
          <el-table-column prop="id" label="ID" width="60" />
          
          <el-table-column prop="name" label="类别名称" min-width="65">
            <template #default="{ row }">
              <div class="category-name">
                <el-image
                  v-if="row.iconUrl"
                  :src="row.iconUrl"
                  class="category-icon"
                  fit="cover"
                />
                {{ row.name }}
              </div>
            </template>
          </el-table-column>
          
          <el-table-column prop="categoryCode" label="类别编码" width="165" />
          
          <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
          
          <el-table-column prop="parentName" label="父类别" width="120">
            <template #default="{ row }">
              {{ row.parentName || '顶级类别' }}
            </template>
          </el-table-column>
          
          <el-table-column prop="level" label="层级" width="80" />
          
          <el-table-column prop="sortOrder" label="排序" width="80" />
          
          <el-table-column prop="isActive" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.isActive ? 'success' : 'danger'">
                {{ row.isActive ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="imageCount" label="图片数量" width="100" />
          
          <el-table-column prop="createdAt" label="创建时间" width="160">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="editCategory(row)">
                编辑
              </el-button>
              <el-button 
                :type="row.isActive ? 'warning' : 'success'" 
                size="small" 
                @click="toggleStatus(row)"
              >
                {{ row.isActive ? '禁用' : '启用' }}
              </el-button>
              <el-button type="danger" size="small" @click="deleteCategory(row)">
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
      :title="isEdit ? '编辑类别' : '新增类别'"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="categoryForm"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="类别名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入类别名称" />
        </el-form-item>
        
        <el-form-item label="类别编码" prop="categoryCode">
          <el-input v-model="categoryForm.categoryCode" placeholder="请输入类别编码" />
        </el-form-item>
        
        <el-form-item label="父类别" prop="parentId">
          <el-tree-select
            v-model="categoryForm.parentId"
            :data="categoryTree"
            :props="{ label: 'name', value: 'id' }"
            placeholder="选择父类别（可选）"
            clearable
            check-strictly
            :render-after-expand="false"
          />
        </el-form-item>
        
        <el-form-item label="图标URL" prop="iconUrl">
          <el-input v-model="categoryForm.iconUrl" placeholder="请输入图标URL" />
        </el-form-item>
        
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number 
            v-model="categoryForm.sortOrder" 
            :min="0" 
            :max="9999"
            placeholder="排序值"
          />
        </el-form-item>
        
        <el-form-item label="状态" prop="isActive">
          <el-radio-group v-model="categoryForm.isActive">
            <el-radio :value="true">启用</el-radio>
            <el-radio :value="false">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="categoryForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入类别描述"
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

    <!-- 批量操作对话框 -->
    <el-dialog v-model="batchDialogVisible" title="批量操作" width="400px">
      <div class="batch-content">
        <p>已选择 {{ selectedCategories.length }} 个类别</p>
        <el-radio-group v-model="batchAction">
          <el-radio value="enable">批量启用</el-radio>
          <el-radio value="disable">批量禁用</el-radio>
          <el-radio value="delete">批量删除</el-radio>
        </el-radio-group>
      </div>
      
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="executeBatchAction">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, Download, Upload } from '@element-plus/icons-vue'
import type { Category, CategoryRequest } from '@/types'
import { categoryApi } from '@/api/category'

// 搜索表单
const searchForm = reactive({
  name: '',
  isActive: undefined as boolean | undefined
})

// 分页
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 表格数据
const categories = ref<Category[]>([])

// 类别树形数据
const categoryTree = ref<Category[]>([])

// 表单相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const categoryForm = reactive<CategoryRequest & { id?: number }>({
  id: undefined,
  name: '',
  categoryCode: '',
  parentId: undefined,
  iconUrl: '',
  sortOrder: 0,
  isActive: true,
  description: ''
})

const formRules = {
  name: [
    { required: true, message: '请输入类别名称', trigger: 'blur' }
  ],
  categoryCode: [
    { required: true, message: '请输入类别编码', trigger: 'blur' },
    { pattern: /^[A-Z_]+$/, message: '编码只能包含大写字母和下划线', trigger: 'blur' }
  ]
}

// 其他状态
const loading = ref(false)
const submitting = ref(false)
const selectedCategories = ref<Category[]>([])
const batchDialogVisible = ref(false)
const batchAction = ref('enable')

// 搜索类别
const searchCategories = () => {
  console.log('搜索条件:', searchForm)
  loadCategories()
}

// 重置搜索
const resetSearch = () => {
  Object.assign(searchForm, {
    name: '',
    isActive: undefined
  })
  loadCategories()
}

// 加载类别列表
const loadCategories = async () => {
  loading.value = true
  try {
    const params: { 
      name?: string; 
      isActive?: boolean; 
      page?: number; 
      size?: number 
    } = {
      page: pagination.page,
      size: pagination.size
    }
    
    if (searchForm.name) {
      params.name = searchForm.name
    }
    if (searchForm.isActive !== undefined) {
      params.isActive = searchForm.isActive
    }
    
    const response = await categoryApi.getCategories(params)
    if (response.success) {
      // 处理分页数据
      if (response.data.content) {
        categories.value = response.data.content
        pagination.total = response.data.totalElements
      } else {
        // 兼容旧的非分页数据格式
        categories.value = response.data
        pagination.total = response.data.length
      }
    } else {
      ElMessage.error(response.message || '加载类别列表失败')
    }
  } catch (error) {
    console.error('加载类别列表失败:', error)
    ElMessage.error('加载类别列表失败')
  } finally {
    loading.value = false
  }
}

// 显示新增对话框
const showAddDialog = () => {
  isEdit.value = false
  dialogVisible.value = true
}

// 编辑类别
const editCategory = (row: Category) => {
  isEdit.value = true
  Object.assign(categoryForm, { ...row })
  dialogVisible.value = true
}

// 切换状态
const toggleStatus = async (row: Category) => {
  const action = row.isActive ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}类别 "${row.name}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const formData: CategoryRequest = {
      parentId: row.parentId,
      name: row.name,
      description: row.description,
      categoryCode: row.categoryCode,
      level: row.level,
      sortOrder: row.sortOrder,
      isActive: !row.isActive,
      iconUrl: row.iconUrl
    }
    
    const response = await categoryApi.updateCategory(row.id, formData)
    if (response.success) {
      ElMessage.success(`${action}成功`)
      await loadCategories()
    } else {
      ElMessage.error(response.message || `${action}失败`)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`${action}类别失败:`, error)
      ElMessage.error(`${action}失败`)
    }
  }
}

// 删除类别
const deleteCategory = async (row: Category) => {
  try {
    await ElMessageBox.confirm(`确定要删除类别 "${row.name}" 吗？删除后无法恢复。`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })
    
    const response = await categoryApi.deleteCategory(row.id)
    if (response.success) {
      ElMessage.success('删除成功')
      await loadCategories()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除类别失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    submitting.value = true
    
    const formData: CategoryRequest = {
      parentId: categoryForm.parentId,
      name: categoryForm.name,
      description: categoryForm.description,
      categoryCode: categoryForm.categoryCode,
      level: categoryForm.level,
      sortOrder: categoryForm.sortOrder,
      isActive: categoryForm.isActive,
      iconUrl: categoryForm.iconUrl
    }
    
    let response
    if (isEdit.value) {
      response = await categoryApi.updateCategory(categoryForm.id!, formData)
    } else {
      response = await categoryApi.createCategory(formData)
    }
    
    if (response.success) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      resetForm()
      await loadCategories()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    console.error('提交表单失败:', error)
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  Object.assign(categoryForm, {
    id: undefined,
    name: '',
    categoryCode: '',
    parentId: undefined,
    iconUrl: '',
    sortOrder: 0,
    isActive: true,
    description: ''
  })
}

// 处理选择变化
const handleSelectionChange = (selection: Category[]) => {
  selectedCategories.value = selection
}

// 导出类别
const exportCategories = () => {
  ElMessage.info('导出功能开发中...')
}

// 导入前处理
const beforeImport = (file: File) => {
  const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
                  file.type === 'application/vnd.ms-excel'
  
  if (!isExcel) {
    ElMessage.error('只能上传 Excel 文件!')
    return false
  }
  
  ElMessage.info('导入功能开发中...')
  return false
}

// 执行批量操作
const executeBatchAction = async () => {
  if (selectedCategories.value.length === 0) {
    ElMessage.warning('请先选择要操作的类别')
    return
  }
  
  const actionMap: Record<string, string> = {
    'enable': '启用',
    'disable': '禁用',
    'delete': '删除'
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要${actionMap[batchAction.value]} ${selectedCategories.value.length} 个类别吗？`,
      '批量操作确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 执行批量操作
    selectedCategories.value.forEach((category: Category) => {
      if (batchAction.value === 'enable') {
        category.isActive = true
      } else if (batchAction.value === 'disable') {
        category.isActive = false
      } else if (batchAction.value === 'delete') {
        const index = categories.value.findIndex(item => item.id === category.id)
        if (index > -1) {
          categories.value.splice(index, 1)
        }
      }
    })
    
    if (batchAction.value === 'delete') {
      pagination.total -= selectedCategories.value.length
    }
    
    ElMessage.success(`批量${actionMap[batchAction.value]}成功`)
    batchDialogVisible.value = false
    selectedCategories.value = []
  } catch (error) {
    // 用户取消
  }
}

// 格式化时间
const formatTime = (time: string) => {
  return new Date(time).toLocaleString()
}

// 分页处理
const handleSizeChange = (size: number) => {
  pagination.size = size
  loadCategories()
}

const handlePageChange = (page: number) => {
  pagination.page = page
  loadCategories()
}

// 加载类别树
const loadCategoryTree = async () => {
  try {
    const response = await categoryApi.getCategoryTree()
    if (response.success) {
      categoryTree.value = response.data
    }
  } catch (error) {
    console.error('加载类别树失败:', error)
  }
}

onMounted(() => {
  loadCategories()
  loadCategoryTree()
})
</script>

<style scoped>
.category-management {
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  padding: 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 50%, #cbd5e1 100%);
  position: relative;
  overflow: hidden;
}

.category-management::before {
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

/* 上传按钮样式 */
:deep(.el-upload) {
  display: inline-block;
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

/* 类别名称样式 */
.category-name {
  display: flex;
  align-items: center;
  font-weight: 600;
  color: #1e293b;
}

.category-icon {
  width: 24px;
  height: 24px;
  border-radius: 8px;
  margin-right: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(226, 232, 240, 0.6);
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
:deep(.el-pagination) {
  background: transparent;
  padding: 0;
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

/* 树形选择器样式 */
:deep(.el-tree-select) {
  border-radius: 12px;
}

:deep(.el-tree-select .el-input__wrapper) {
  border-radius: 12px;
}

:deep(.el-tree-select__popper) {
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

/* 批量操作样式 */
.batch-content {
  text-align: center;
  padding: 20px;
}

.batch-content p {
  margin-bottom: 20px;
  color: #64748b;
  font-size: 16px;
  font-weight: 500;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .category-management {
    padding: 16px;
  }
}

@media (max-width: 768px) {
  .category-management {
    padding: 12px;
    height: 100vh;
  }
  
  .operation-card {
    margin-bottom: 16px;
  }
  
  .operation-row .el-col {
    margin-bottom: 12px;
  }
  
  .operation-right {
    text-align: left;
  }
  
  .pagination-wrapper {
    padding: 12px 0 0 0;
    margin-top: 12px;
  }
  
  :deep(.el-pagination) {
    padding: 0;
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
  
  .category-name {
    font-size: 14px;
  }
  
  .category-icon {
    width: 20px;
    height: 20px;
    margin-right: 8px;
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

/* 类别图标悬停效果 */
.category-icon:hover {
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
</style>