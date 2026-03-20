<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '240px'" class="sidebar" :class="{ 'sidebar-mobile': isMobile }">
      <div class="logo" @click="handleLogoClick">
        <img src="/logo.svg" alt="智能识别系统" v-if="!isCollapse" />
        <img src="/logo.svg" alt="智能识别系统" v-else class="logo-mini" />
        <span v-if="!isCollapse" class="logo-text">智能识别系统</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :unique-opened="true"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/admin" class="menu-item-dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>数据看板</template>
        </el-menu-item>
        
        <el-menu-item index="/admin/categories" class="menu-item-categories">
          <el-icon><Grid /></el-icon>
          <template #title>类别管理</template>
        </el-menu-item>
        
        <el-menu-item index="/admin/models" class="menu-item-models">
          <el-icon><Setting /></el-icon>
          <template #title>模型管理</template>
        </el-menu-item>
        
        <el-menu-item index="/admin/users" class="menu-item-users">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        
        <el-menu-item index="/admin/logs" class="menu-item-logs">
          <el-icon><Document /></el-icon>
          <template #title>系统日志</template>
        </el-menu-item>
      </el-menu>
      
      <!-- 侧边栏底部信息 -->
      <div class="sidebar-bottom" v-if="!isCollapse">
        <div class="system-info">
          <div class="time">{{ currentTime }}</div>
          <div class="status">系统正常</div>
        </div>
      </div>
    </el-aside>
    
    <!-- 主内容区 -->
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-left">
          <el-button
            text
            @click="toggleCollapse"
            class="collapse-btn"
          >
            <el-icon size="20">
              <Expand v-if="isCollapse" />
              <Fold v-else />
            </el-icon>
          </el-button>
          
          <el-breadcrumb separator="/">
            <el-breadcrumb-item
              v-for="item in breadcrumbs"
              :key="item.path"
              :to="item.path"
            >
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar">
                {{ userStore.userInfo?.realName?.charAt(0) || userStore.userInfo?.username?.charAt(0) }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="settings">系统设置</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <!-- 主要内容 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
    
    <!-- 个人资料弹窗 -->
    <el-dialog
      v-model="profileDialogVisible"
      title="个人资料"
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-loading="profileLoading">
        <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-width="100px"
          label-position="left"
        >
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="用户名">
                <el-input v-model="profileForm.username" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="角色">
                <el-tag :type="profileForm.role === 'ADMIN' ? 'danger' : 'primary'">
                  {{ profileForm.role === 'ADMIN' ? '管理员' : '操作员' }}
                </el-tag>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="真实姓名" prop="realName">
                <el-input 
                  v-model="profileForm.realName" 
                  :disabled="!isEditingProfile"
                  placeholder="请输入真实姓名"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="邮箱" prop="email">
                <el-input 
                  v-model="profileForm.email" 
                  :disabled="!isEditingProfile"
                  placeholder="请输入邮箱"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="手机号" prop="phone">
                <el-input 
                  v-model="profileForm.phone" 
                  :disabled="!isEditingProfile"
                  placeholder="请输入手机号"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="部门">
                <el-input 
                  v-model="profileForm.department" 
                  :disabled="!isEditingProfile"
                  placeholder="请输入部门"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-form-item label="头像">
            <div class="avatar-section">
              <div class="avatar-preview">
                <el-avatar :size="80" :src="profileForm.avatar">
                  {{ profileForm.username?.charAt(0).toUpperCase() }}
                </el-avatar>
              </div>
              <div class="avatar-controls">
                <el-input 
                  v-model="profileForm.avatar" 
                  :disabled="!isEditingProfile"
                  placeholder="请输入头像URL"
                  class="avatar-input"
                />
                <el-upload
                  v-if="isEditingProfile"
                  :show-file-list="false"
                  :before-upload="beforeAvatarUpload"
                  :http-request="handleAvatarUpload"
                  accept="image/*"
                  class="avatar-upload"
                >
                  <el-button type="primary" :loading="avatarUploading">
                    <el-icon><Upload /></el-icon>
                    上传头像
                  </el-button>
                </el-upload>
              </div>
            </div>
          </el-form-item>
          
          <el-form-item label="备注">
            <el-input 
              v-model="profileForm.remark" 
              :disabled="!isEditingProfile"
              type="textarea"
              :rows="3"
              placeholder="请输入备注信息"
            />
          </el-form-item>
          
          <!-- 统计信息（只读） -->
          <el-divider content-position="left">统计信息</el-divider>
          <el-row :gutter="20">
            <el-col :span="6">
              <el-statistic title="登录次数" :value="profileForm.loginCount" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="创建任务" :value="profileForm.taskCount" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="处理图片" :value="profileForm.imageCount" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="在线时长" :value="profileForm.onlineTime" suffix="小时" />
            </el-col>
          </el-row>
          
          <el-divider />
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="info-item">
                <span class="label">账户状态：</span>
                <el-tag :type="getStatusTagType(profileForm.status)">
                  {{ getStatusText(profileForm.status) }}
                </el-tag>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="info-item">
                <span class="label">创建时间：</span>
                <span>{{ formatDateTime(profileForm.createTime) }}</span>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="info-item">
                <span class="label">最后登录：</span>
                <span>{{ formatDateTime(profileForm.lastLoginTime) }}</span>
              </div>
            </el-col>
          </el-row>
        </el-form>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="profileDialogVisible = false">关闭</el-button>
          <el-button v-if="!isEditingProfile" type="primary" @click="toggleEditMode">
            编辑资料
          </el-button>
          <template v-else>
            <el-button @click="cancelEdit">取消</el-button>
            <el-button type="primary" :loading="profileLoading" @click="saveProfile">
              保存
            </el-button>
          </template>
        </div>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCurrentUser, updateCurrentUser, type UserDTO, type UserUpdateRequest } from '@/api/user'
import {
  DataAnalysis,
  Grid,
  Setting,
  User,
  Document,
  Expand,
  Fold,
  ArrowDown,
  Upload,
  Clock,
  Monitor
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const isMobile = ref(false)
const currentTime = ref('')

// 检测移动端设备
const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) {
    isCollapse.value = true
  }
}

// 更新当前时间
const updateCurrentTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

// Logo点击交互
const handleLogoClick = () => {
  if (route.path !== '/admin') {
    router.push('/admin')
  } else {
    // 如果已经在首页，则触发一个小动画效果
    const logoEl = document.querySelector('.logo')
    if (logoEl) {
      logoEl.style.transform = 'scale(0.95)'
      setTimeout(() => {
        logoEl.style.transform = 'scale(1)'
      }, 150)
    }
  }
}

const activeMenu = computed(() => route.path)

const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta?.title)
  return matched.map(item => ({
    path: item.path,
    title: item.meta?.title as string
  }))
})

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

// 初始化时拉取当前用户信息并同步到store，确保右上角头像显示
onMounted(async () => {
  try {
    // 检测移动端
    checkMobile()
    window.addEventListener('resize', checkMobile)
    
    // 更新时间
    updateCurrentTime()
    setInterval(updateCurrentTime, 60000) // 每分钟更新一次
    
    userStore.initUserInfo()
    if (userStore.token) {
      const resp = await getCurrentUser()
      if (resp.success && resp.data) {
        userStore.updateUserInfo({
          id: resp.data.id,
          username: resp.data.username,
          role: resp.data.role,
          realName: resp.data.realName,
          email: resp.data.email,
          phone: resp.data.phone,
          department: resp.data.department,
          avatar: resp.data.avatar,
          status: resp.data.status,
          taskCount: resp.data.taskCount,
          imageCount: resp.data.imageCount,
          onlineTime: resp.data.onlineTime,
          lastLoginAt: resp.data.lastLoginTime,
          loginCount: resp.data.loginCount,
          remark: resp.data.remark
        })
      }
    }
  } catch (e) {
    console.error('初始化用户信息失败:', e)
  }
})

// 个人资料相关状态
const profileDialogVisible = ref(false)
const profileFormRef = ref()
const isEditingProfile = ref(false)
const profileForm = ref<UserDTO>({
  id: 0,
  username: '',
  email: '',
  realName: '',
  phone: '',
  department: '',
  role: 'OPERATOR',
  status: 'ACTIVE',
  avatar: '',
  remark: '',
  loginCount: 0,
  taskCount: 0,
  imageCount: 0,
  onlineTime: 0,
  lastActiveDays: 0,
  lastLoginTime: '',
  createTime: '',
  updateTime: ''
})
const profileLoading = ref(false)
const avatarUploading = ref(false)

// 表单验证规则
const profileRules = {
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
}

const handleCommand = async (command: string) => {
  switch (command) {
    case 'profile':
      await openProfileDialog()
      break
    case 'settings':
      ElMessage.info('系统设置功能开发中')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        await userStore.logout()
        ElMessage.success('已退出登录')
        router.push('/login')
      } catch (error) {
        // 用户取消操作
      }
      break
  }
}

// 打开个人资料弹窗
const openProfileDialog = async () => {
  try {
    const response = await getCurrentUser()
    if (response.success && response.data) {
      profileForm.value = { ...response.data }
      profileDialogVisible.value = true
      isEditingProfile.value = false
    } else {
      ElMessage.error(response.message || '获取用户信息失败')
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  }
}

// 切换编辑模式
const toggleEditMode = () => {
  isEditingProfile.value = !isEditingProfile.value
}

// 保存个人资料
const saveProfile = async () => {
  try {
    await profileFormRef.value?.validate()
    
    const updateData: UserUpdateRequest = {
      username: profileForm.value.username,
      email: profileForm.value.email,
      realName: profileForm.value.realName,
      phone: profileForm.value.phone,
      avatar: profileForm.value.avatar,
      remark: profileForm.value.remark
    }
    
    const response = await updateCurrentUser(updateData)
    if (response.success && response.data) {
      ElMessage.success('个人资料更新成功')
      profileForm.value = { ...response.data }
      isEditingProfile.value = false
      
      // 更新用户store中的信息
      userStore.updateUserInfo({
        username: response.data.username,
        role: response.data.role,
        email: response.data.email,
        realName: response.data.realName,
        phone: response.data.phone,
        avatar: response.data.avatar
      })
    } else {
      ElMessage.error(response.message || '更新失败')
    }
  } catch (error) {
    console.error('保存个人资料失败:', error)
    ElMessage.error('保存失败，请检查输入信息')
  }
}

// 取消编辑
const cancelEdit = () => {
  isEditingProfile.value = false
  // 重新获取用户信息，恢复原始数据
  openProfileDialog()
}

// 格式化日期时间
const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

// 获取状态标签类型
const getStatusTagType = (status: string) => {
  switch (status) {
    case 'ACTIVE':
      return 'success'
    case 'INACTIVE':
      return 'warning'
    case 'LOCKED':
      return 'danger'
    default:
      return 'info'
  }
}

// 获取状态文本
const getStatusText = (status: string) => {
  switch (status) {
    case 'ACTIVE':
      return '正常'
    case 'INACTIVE':
      return '未激活'
    case 'LOCKED':
      return '已锁定'
    default:
      return '未知'
  }
}

// 头像上传前的验证
const beforeAvatarUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

// 处理头像上传
const handleAvatarUpload = async (options: any) => {
  const { file } = options
  
  try {
    avatarUploading.value = true
    
    const formData = new FormData()
    formData.append('file', file)
    
    // 调用头像上传API
    const response = await fetch('/api/users/avatar/upload', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      },
      body: formData
    })
    
    if (!response.ok) {
      throw new Error('上传失败')
    }
    
    const result = await response.json()
    
    if (result.success) {
      profileForm.value.avatar = result.data.fileUrl
      // 立即更新用户store中的头像信息
      userStore.updateUserInfo({
        avatar: result.data.fileUrl
      })
      ElMessage.success('头像上传成功')
    } else {
      throw new Error(result.message || '上传失败')
    }
  } catch (error: any) {
    console.error('头像上传失败:', error)
    ElMessage.error(error.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

// 监听路由变化，自动收起移动端侧边栏
watch(
  () => route.path,
  () => {
    if (window.innerWidth < 768) {
      isCollapse.value = true
    }
  }
)
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

.sidebar {
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 30%, #e2e8f0 100%);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 4px 0 20px rgba(59, 130, 246, 0.15);
  border-right: 1px solid rgba(59, 130, 246, 0.2);
  position: relative;
  overflow: hidden;
}

.sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 20%, rgba(59, 130, 246, 0.08) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(139, 92, 246, 0.06) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.logo {
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 20px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.3);
  position: relative;
  z-index: 1;
  transition: all 0.3s ease;
  cursor: pointer;
}

.logo:hover {
  background: linear-gradient(135deg, #1d4ed8 0%, #1e40af 100%);
}

.logo img {
  width: 40px;
  height: 40px;
  margin-right: 12px;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.3));
  transition: all 0.3s ease;
}

.logo img:hover {
  transform: scale(1.05);
  filter: drop-shadow(0 4px 12px rgba(59, 130, 246, 0.4));
}

.logo-mini {
  width: 32px !important;
  height: 32px !important;
  margin-right: 0 !important;
}

.logo-text {
  transition: all 0.3s ease;
}

.sidebar-menu {
  border: none;
  background: transparent;
  position: relative;
  z-index: 1;
  padding: 12px 0;
}

/* 侧边栏底部信息样式 */
.sidebar-bottom {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.8) 0%, rgba(226, 232, 240, 0.9) 100%);
  border-top: 1px solid rgba(59, 130, 246, 0.2);
  backdrop-filter: blur(10px);
}

.system-info {
  font-size: 12px;
  color: #64748b;
  text-align: center;
  line-height: 1.4;
}

.system-info .time {
  font-weight: 600;
  color: #3b82f6;
  margin-bottom: 4px;
}

.system-info .status {
  color: #10b981;
  font-size: 11px;
}

.system-info .info-item span {
  letter-spacing: 0.3px;
}

/* 移动端适配 */
.sidebar-mobile {
  transform: translateX(-100%);
  transition: transform 0.3s ease;
}

.sidebar-mobile.show {
  transform: translateX(0);
}

:deep(.el-menu-item) {
  color: #374151;
  margin: 4px 12px;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-weight: 500;
  letter-spacing: 0.3px;
  position: relative;
  overflow: hidden;
}

:deep(.el-menu-item::before) {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(59, 130, 246, 0.1), transparent);
  transition: left 0.5s ease;
}

:deep(.el-menu-item:hover::before) {
  left: 100%;
}

:deep(.el-menu-item:hover) {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(139, 92, 246, 0.1) 100%) !important;
  color: #1e40af;
  transform: translateX(4px);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.2);
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%) !important;
  color: white;
  box-shadow: 
    0 6px 20px rgba(59, 130, 246, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transform: translateX(6px);
}

:deep(.el-menu-item.is-active::after) {
  content: '';
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 60%;
  background: linear-gradient(180deg, #ffffff 0%, rgba(255, 255, 255, 0.6) 100%);
  border-radius: 2px 0 0 2px;
}

:deep(.el-menu-item .el-icon) {
  margin-right: 12px;
  font-size: 18px;
  transition: all 0.3s ease;
}

:deep(.el-menu-item:hover .el-icon) {
  transform: scale(1.1);
}

.header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
  position: relative;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.collapse-btn {
  padding: 8px;
  margin: 0;
  border-radius: 12px;
  transition: all 0.3s ease;
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.collapse-btn:hover {
  background: rgba(59, 130, 246, 0.2);
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

:deep(.el-breadcrumb) {
  font-weight: 500;
}

:deep(.el-breadcrumb__item) {
  color: #64748b;
  transition: color 0.3s ease;
}

:deep(.el-breadcrumb__item:hover) {
  color: #3b82f6;
}

:deep(.el-breadcrumb__item.is-link) {
  color: #3b82f6;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: rgba(59, 130, 246, 0.05);
  border: 1px solid rgba(59, 130, 246, 0.1);
}

.user-info:hover {
  background: rgba(59, 130, 246, 0.1);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.15);
}

:deep(.user-info .el-avatar) {
  border: 2px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.username {
  font-size: 14px;
  color: #1e293b;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.main-content {
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  padding: 0;
  position: relative;
  overflow-y: auto;
  overflow-x: hidden;
  height: calc(100vh - 60px);
}

.main-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.02) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(139, 92, 246, 0.02) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .sidebar-footer {
    display: none;
  }
  
  .logo {
    cursor: pointer;
    justify-content: flex-start;
    padding: 0 16px;
  }
  
  .logo img {
    width: 28px;
    height: 28px;
    margin-right: 8px;
  }
  
  .logo-text {
    font-size: 14px;
  }
  
  :deep(.el-menu-item) {
    margin: 2px 8px;
    font-size: 14px;
  }
  
  :deep(.el-menu-item .el-icon) {
    font-size: 16px;
    margin-right: 8px;
  }
  
  .main-content {
    padding: 0;
  }
}

@media (max-width: 480px) {
  .sidebar {
    width: 280px !important;
  }
  
  .logo {
    height: 56px;
    font-size: 14px;
  }
  
  .logo img {
    width: 24px;
    height: 24px;
  }
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .main-content {
    padding: 0;
  }
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    z-index: 1000;
    height: 100vh;
    box-shadow: 8px 0 32px rgba(0, 0, 0, 0.2);
  }
  
  .header {
    padding: 0 16px;
  }
  
  .header-left {
    gap: 16px;
  }
  
  .header-left .el-breadcrumb {
    display: none;
  }
  
  .main-content {
    padding: 0;
  }
  
  .logo {
    height: 60px;
    font-size: 16px;
  }
  
  .logo img {
    width: 32px;
    height: 32px;
  }
}

/* 个人资料弹窗样式 */
.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  padding: 8px 0;
}

.info-item .label {
  font-weight: 600;
  color: #374151;
  margin-right: 12px;
  min-width: 80px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
}

.avatar-section {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  padding: 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.avatar-preview {
  flex-shrink: 0;
}

.avatar-controls {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.avatar-input {
  width: 100%;
}

.avatar-upload {
  align-self: flex-start;
}

/* 对话框美化 */
:deep(.el-dialog) {
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
}

:deep(.el-dialog__header) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 20px 20px 0 0;
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
  border-radius: 0 0 20px 20px;
}

/* 表单美化 */
:deep(.el-form-item__label) {
  font-weight: 600;
  color: #374151;
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

:deep(.el-textarea__inner) {
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  transition: all 0.3s ease;
}

:deep(.el-textarea__inner:hover) {
  border-color: rgba(59, 130, 246, 0.3);
}

:deep(.el-textarea__inner:focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

/* 按钮美化 */
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

:deep(.el-button--default) {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(226, 232, 240, 0.8);
  backdrop-filter: blur(10px);
}

/* 统计卡片美化 */
:deep(.el-statistic) {
  text-align: center;
  padding: 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

:deep(.el-statistic:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

:deep(.el-statistic__number) {
  font-weight: 700;
  color: #1e293b;
}

:deep(.el-statistic__title) {
  font-weight: 600;
  color: #64748b;
}

/* 标签美化 */
:deep(.el-tag) {
  border-radius: 8px;
  font-weight: 600;
  letter-spacing: 0.3px;
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

:deep(.el-tag--primary) {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
}

:deep(.el-tag--danger) {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
}

/* 分割线美化 */
:deep(.el-divider) {
  border-color: rgba(226, 232, 240, 0.6);
}

:deep(.el-divider__text) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  color: #64748b;
  font-weight: 600;
  padding: 0 16px;
}

/* 加载动画 */
@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.sidebar {
  animation: slideInLeft 0.6s ease-out;
}

.header {
  animation: fadeInUp 0.6s ease-out 0.1s both;
}

.main-content {
  animation: fadeInUp 0.6s ease-out 0.2s both;
}

/* 滚动条美化 - 移除，因为主内容区不应该滚动 */
</style>