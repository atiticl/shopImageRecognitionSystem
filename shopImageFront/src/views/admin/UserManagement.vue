<template>
  <div class="user-management">
    <!-- 操作栏 -->
    <el-card class="operation-card">
      <div class="operation-row">
        <div class="operation-left">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索用户名/邮箱"
            clearable
            @keyup.enter="searchUsers"
            style="width: 220px;"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          
          <el-select v-model="searchForm.role" placeholder="角色" clearable style="width: 120px; margin-left: 12px;">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="运营人员" value="OPERATOR" />
          </el-select>
          
          <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 120px; margin-left: 12px;">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="禁用" value="INACTIVE" />
            <el-option label="锁定" value="LOCKED" />
          </el-select>
          
          <el-button type="primary" @click="searchUsers" style="margin-left: 12px;">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>
        
        <div class="operation-right">
          <el-button type="success" @click="showAddDialog">
            <el-icon><Plus /></el-icon>
            新增用户
          </el-button>
          <el-button @click="exportUsers">
            <el-icon><Download /></el-icon>
            导出
          </el-button>
          <el-button 
            v-if="selectedUsers.length > 0"
            @click="showBatchDialog"
          >
            批量操作({{ selectedUsers.length }})
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 用户表格 -->
    <el-card class="table-card">
      <div class="table-container">
        <el-table
          :data="users"
          v-loading="loading"
          stripe
          border
          height="100%"
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          
          <el-table-column prop="id" label="ID" width="80" />
          
          <el-table-column prop="avatar" label="头像" width="80">
            <template #default="{ row }">
              <el-avatar :src="row.avatar" :size="40">
                {{ row.username.charAt(0).toUpperCase() }}
              </el-avatar>
            </template>
          </el-table-column>
          
          <el-table-column prop="username" label="用户名" min-width="80" />
          
          <el-table-column prop="email" label="邮箱" min-width="140" />
          
          <el-table-column prop="realName" label="真实姓名" width="120" />
          
          <el-table-column prop="role" label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="getRoleType(row.role)">
                {{ getRoleText(row.role) }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="loginCount" label="登录次数" width="100" />
          
          <el-table-column prop="lastLoginTime" label="最后登录" width="160">
            <template #default="{ row }">
              {{ row.lastLoginTime ? formatTime(row.lastLoginTime) : '-' }}
            </template>
          </el-table-column>
          
          <el-table-column prop="createTime" label="创建时间" width="160">
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="viewUser(row)">
                查看
              </el-button>
              <el-button size="small" @click="editUser(row)">
                编辑
              </el-button>
              <el-button 
                :type="row.status === 'ACTIVE' ? 'warning' : 'success'" 
                size="small" 
                @click="toggleStatus(row)"
              >
                {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
              </el-button>
              <el-button type="danger" size="small" @click="deleteUserAction(row)">
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
          :total="pagination.total || 0"
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
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="userForm"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input 
            v-model="userForm.password" 
            type="password" 
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="userForm.role">
            <el-radio value="ADMIN">管理员</el-radio>
            <el-radio value="OPERATOR">运营人员</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio value="ACTIVE">正常</el-radio>
            <el-radio value="INACTIVE">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="userForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
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

    <!-- 用户详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="用户详情" width="800px">
      <div v-if="currentUser" class="user-detail">
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="user-avatar">
              <el-avatar :src="currentUser.avatar" :size="120">
                {{ currentUser.username.charAt(0).toUpperCase() }}
              </el-avatar>
            </div>
          </el-col>
          <el-col :span="18">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="用户ID">{{ currentUser.id }}</el-descriptions-item>
              <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ currentUser.email }}</el-descriptions-item>
              <el-descriptions-item label="真实姓名">{{ currentUser.realName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ currentUser.phone || '-' }}</el-descriptions-item>
              <el-descriptions-item label="角色">
                <el-tag :type="getRoleType(currentUser.role)">
                  {{ getRoleText(currentUser.role) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="getStatusType(currentUser.status)">
                  {{ getStatusText(currentUser.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="登录次数">{{ currentUser.loginCount }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatTime(currentUser.createTime) }}</el-descriptions-item>
              <el-descriptions-item label="最后登录">
                {{ currentUser.lastLoginTime ? formatTime(currentUser.lastLoginTime) : '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">
                {{ currentUser.remark || '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </el-col>
        </el-row>
        
        <div class="user-stats">
          <h4>用户统计</h4>
          <el-row :gutter="16">
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-number">{{ currentUser.taskCount || 0 }}</div>
                <div class="stat-label">创建任务</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-number">{{ currentUser.imageCount || 0 }}</div>
                <div class="stat-label">处理图片</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-number">{{ currentUser.onlineTime || 0 }}h</div>
                <div class="stat-label">在线时长</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-number">{{ currentUser.lastActiveDays || 0 }}</div>
                <div class="stat-label">活跃天数</div>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="editUser(currentUser)">编辑用户</el-button>
      </template>
    </el-dialog>

    <!-- 批量操作对话框 -->
    <el-dialog v-model="batchDialogVisible" title="批量操作" width="400px">
      <div class="batch-content">
        <p>已选择 {{ selectedUsers.length }} 个用户</p>
        <el-radio-group v-model="batchAction">
          <el-radio value="enable">批量启用</el-radio>
          <el-radio value="disable">批量禁用</el-radio>
          <el-radio value="delete">批量删除</el-radio>
          <el-radio value="resetPassword">重置密码</el-radio>
        </el-radio-group>
      </div>
      
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="executeBatchAction">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="resetPasswordDialogVisible" title="重置密码" width="400px">
      <el-form :model="resetPasswordForm" label-width="80px">
        <el-form-item label="新密码">
          <el-input 
            v-model="resetPasswordForm.newPassword" 
            type="password" 
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input 
            v-model="resetPasswordForm.confirmPassword" 
            type="password" 
            placeholder="请确认新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="resetPasswordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Download } from '@element-plus/icons-vue'
import { 
  getUsers, 
  getUserById, 
  createUser, 
  updateUser, 
  deleteUser, 
  batchDeleteUsers,
  updateUserStatus,
  resetUserPassword,
  getUserStatistics,
  getUserRoles,
  getUserStatuses,
  type UserDTO,
  type UserCreateRequest,
  type UserUpdateRequest,
  type UserSearchParams,
  type UserRole,
  type UserStatus
} from '@/api/user'

// 搜索表单
const searchForm = reactive<UserSearchParams>({
  keyword: '',
  role: undefined,
  status: undefined
})

// 分页
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 表格数据
const users = ref<UserDTO[]>([])

// 表单相关
const dialogVisible = ref(false)
const detailDialogVisible = ref(false)
const batchDialogVisible = ref(false)
const resetPasswordDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const currentUser = ref<UserDTO | null>(null)

const userForm = reactive<UserCreateRequest & { id?: number }>({
  id: undefined,
  username: '',
  email: '',
  realName: '',
  password: '',
  role: 'OPERATOR',
  status: 'ACTIVE',
  phone: '',
  remark: ''
})

const resetPasswordForm = reactive({
  newPassword: '',
  confirmPassword: ''
})

const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 个字符', trigger: 'blur' }
  ]
}

// 其他状态
const loading = ref(false)
const submitting = ref(false)
const selectedUsers = ref<UserDTO[]>([])
const batchAction = ref('enable')

// 获取角色类型
const getRoleType = (role: UserRole) => {
  const roleMap: Record<UserRole, string> = {
    'ADMIN': 'danger',
    'OPERATOR': 'primary'
  }
  return roleMap[role] || 'info'
}

// 获取角色文本
const getRoleText = (role: UserRole) => {
  const roleMap: Record<UserRole, string> = {
    'ADMIN': '管理员',
    'OPERATOR': '运营人员'
  }
  return roleMap[role] || '未知'
}

// 获取状态类型
const getStatusType = (status: UserStatus) => {
  const statusMap: Record<UserStatus, string> = {
    'ACTIVE': 'success',
    'INACTIVE': 'danger',
    'LOCKED': 'warning'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: UserStatus) => {
  const statusMap: Record<UserStatus, string> = {
    'ACTIVE': '正常',
    'INACTIVE': '禁用',
    'LOCKED': '锁定'
  }
  return statusMap[status] || '未知'
}

// 格式化时间
const formatTime = (time: string | null | undefined) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 搜索用户
const searchUsers = () => {
  pagination.page = 1
  loadUsers()
}

// 重置搜索
const resetSearch = () => {
  Object.assign(searchForm, {
    keyword: '',
    role: undefined,
    status: undefined
  })
  pagination.page = 1
  loadUsers()
}

// 加载用户列表
const loadUsers = async () => {
  loading.value = true
  try {
    const params: UserSearchParams = {
      page: pagination.page - 1, // 后端从0开始
      size: pagination.size,
      sortBy: 'id',
      sortDir: 'asc',
      ...searchForm
    }
    
    console.log('请求参数:', params)
    const response = await getUsers(params)
    console.log('完整响应:', response)
    console.log('response.data:', response.data)
    console.log('response.data.content:', response.data?.content)
    console.log('response.data.totalElements:', response.data?.totalElements)
    
    users.value = response.data?.content || []
    pagination.total = response.data?.totalElements || 0
    
    console.log('设置后的值:', { 
      usersLength: users.value.length, 
      paginationTotal: pagination.total 
    })
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.error('加载用户列表失败')
    // 确保即使失败也有默认值
    users.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// 显示新增对话框
const showAddDialog = () => {
  isEdit.value = false
  dialogVisible.value = true
}

// 显示批量操作对话框
const showBatchDialog = () => {
  batchDialogVisible.value = true
}

// 查看用户详情
const viewUser = async (row: UserDTO) => {
  try {
    const response = await getUserById(row.id)
    currentUser.value = response.data
    detailDialogVisible.value = true
  } catch (error) {
    console.error('获取用户详情失败:', error)
    ElMessage.error('获取用户详情失败')
  }
}

// 编辑用户
const editUser = (row: UserDTO) => {
  isEdit.value = true
  Object.assign(userForm, { 
    ...row, 
    password: '' // 编辑时不显示密码
  })
  dialogVisible.value = true
  detailDialogVisible.value = false
}

// 切换状态
const toggleStatus = async (row: UserDTO) => {
  const newStatus: UserStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  const action = newStatus === 'ACTIVE' ? '启用' : '禁用'
  
  try {
    await ElMessageBox.confirm(`确定要${action}用户 "${row.username}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await updateUserStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success(`${action}成功`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('更新用户状态失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

// 删除用户
const deleteUserAction = async (row: UserDTO) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户 "${row.username}" 吗？删除后无法恢复。`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })
    
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    loadUsers() // 重新加载列表
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除用户失败:', error)
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
    
    if (isEdit.value && userForm.id) {
      // 编辑用户
      const updateData: UserUpdateRequest = {
        username: userForm.username,
        email: userForm.email,
        realName: userForm.realName,
        role: userForm.role,
        status: userForm.status,
        phone: userForm.phone,
        remark: userForm.remark
      }
      await updateUser(userForm.id, updateData)
      ElMessage.success('更新成功')
    } else {
      // 创建用户
      const createData: UserCreateRequest = {
        username: userForm.username,
        email: userForm.email,
        realName: userForm.realName,
        password: userForm.password,
        role: userForm.role,
        status: userForm.status,
        phone: userForm.phone,
        remark: userForm.remark
      }
      await createUser(createData)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    loadUsers() // 重新加载列表
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
  Object.assign(userForm, {
    id: undefined,
    username: '',
    email: '',
    realName: '',
    password: '',
    role: 'OPERATOR',
    status: 'ACTIVE',
    phone: '',
    remark: ''
  })
}

// 处理选择变化
const handleSelectionChange = (selection: UserDTO[]) => {
  selectedUsers.value = selection
}

// 处理页面大小变化
const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  loadUsers()
}

// 处理页面变化
const handlePageChange = (page: number) => {
  pagination.page = page
  loadUsers()
}

// 导出用户
const exportUsers = () => {
  ElMessage.info('导出功能开发中...')
}

// 执行批量操作
const executeBatchAction = async () => {
  if (selectedUsers.value.length === 0) {
    ElMessage.warning('请先选择要操作的用户')
    return
  }
  
  const actionMap: Record<string, string> = {
    'enable': '批量启用',
    'disable': '批量禁用', 
    'delete': '批量删除',
    'resetPassword': '重置密码'
  }
  
  const actionText = actionMap[batchAction.value]
  
  try {
    await ElMessageBox.confirm(`确定要${actionText}选中的 ${selectedUsers.value.length} 个用户吗？`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const userIds = selectedUsers.value.map(user => user.id)
    
    switch (batchAction.value) {
      case 'enable':
        // 批量启用 - 需要逐个调用API
        for (const id of userIds) {
          await updateUserStatus(id, 'ACTIVE')
        }
        break
      case 'disable':
        // 批量禁用 - 需要逐个调用API
        for (const id of userIds) {
          await updateUserStatus(id, 'INACTIVE')
        }
        break
      case 'delete':
        // 批量删除
        await batchDeleteUsers(userIds)
        break
      case 'resetPassword':
        // 批量重置密码 - 需要逐个调用API
        for (const id of userIds) {
          await resetUserPassword(id)
        }
        break
    }
    
    ElMessage.success(`${actionText}成功`)
    batchDialogVisible.value = false
    selectedUsers.value = []
    loadUsers() // 重新加载列表
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量操作失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

// 确认重置密码
const confirmResetPassword = async () => {
  if (!resetPasswordForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  
  if (resetPasswordForm.newPassword !== resetPasswordForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  
  try {
    if (currentUser.value) {
      await resetUserPassword(currentUser.value.id)
      ElMessage.success('密码重置成功')
      resetPasswordDialogVisible.value = false
      resetPasswordForm.newPassword = ''
      resetPasswordForm.confirmPassword = ''
    }
  } catch (error) {
    console.error('重置密码失败:', error)
    ElMessage.error('重置密码失败')
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 20px;
  background: transparent;
  position: relative;
  overflow: hidden;
}

.user-management::before {
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
  min-height: 0;
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

/* 头像样式 */
:deep(.el-avatar) {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border: 2px solid rgba(255, 255, 255, 0.8);
}

/* 标签样式 */
:deep(.el-tag) {
  border-radius: 8px;
  font-weight: 600;
  letter-spacing: 0.5px;
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

:deep(.el-tag--primary) {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
}

:deep(.el-tag--success) {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

:deep(.el-tag--warning) {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
}

:deep(.el-tag--danger) {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
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
  display: flex !important;
  visibility: visible !important;
  width: 100%;
  justify-content: flex-end;
}

:deep(.el-pagination > *) {
  display: inline-flex !important;
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

/* 用户详情样式 */
.user-detail {
  margin-bottom: 20px;
}

.user-avatar {
  text-align: center;
  margin-bottom: 20px;
}

:deep(.el-descriptions) {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

:deep(.el-descriptions__header) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

:deep(.el-descriptions__label) {
  font-weight: 700;
  color: #374151;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

:deep(.el-descriptions__content) {
  background: rgba(255, 255, 255, 0.8);
}

.user-stats {
  margin-top: 32px;
  padding: 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.user-stats h4 {
  margin-bottom: 20px;
  color: #1e293b;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.stat-item {
  text-align: center;
  padding: 20px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.stat-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  border-color: rgba(59, 130, 246, 0.3);
}

.stat-number {
  font-size: 24px;
  font-weight: 800;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
  transition: all 0.3s ease;
}

.stat-item:hover .stat-number {
  transform: scale(1.1);
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 600;
  letter-spacing: 0.5px;
}

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
  .user-management {
    padding: 16px;
  }
}

@media (max-width: 768px) {
  .user-management {
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
  
  .stat-item {
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

/* 按钮组动画 */
:deep(.el-button-group .el-button) {
  transition: all 0.3s ease;
}

:deep(.el-button-group .el-button:hover) {
  z-index: 1;
}
</style>