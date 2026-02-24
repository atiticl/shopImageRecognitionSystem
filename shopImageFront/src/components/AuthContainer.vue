<template>
  <div class="auth-container">
    <!-- 背景大圆 -->
    <div class="background-circles">
      <!-- 右侧大圆 - 登录区域 -->
      <div 
        class="circle circle-right" 
        :class="{ 'active': !isRegisterMode }"
      ></div>
      <!-- 左侧大圆 - 注册区域 -->
      <div 
        class="circle circle-left" 
        :class="{ 'active': isRegisterMode }"
      ></div>
    </div>

    <!-- 内容区域 -->
    <div class="content-wrapper">
      <!-- 登录表单 -->
      <transition name="slide-right">
        <div 
          v-if="!isRegisterMode" 
          class="form-container login-container"
        >
          <div class="auth-card">
            <div class="auth-header">
              <h2>欢迎回来</h2>
              <p>请登录您的账户以继续使用</p>
            </div>
              
            <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              size="large"
              @submit.prevent="handleLogin"
              class="auth-form"
            >
              <el-form-item prop="username">
                <el-input
                  v-model="loginForm.username"
                  placeholder="请输入用户名"
                  prefix-icon="User"
                  clearable
                />
              </el-form-item>
              
              <el-form-item prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="请输入密码"
                  prefix-icon="Lock"
                  show-password
                  clearable
                  @keyup.enter="handleLogin"
                />
              </el-form-item>
              
              <el-form-item>
                <div class="form-options">
                  <el-checkbox v-model="loginForm.remember">
                    记住我
                  </el-checkbox>
                  <el-link type="primary" :underline="false">
                    忘记密码？
                  </el-link>
                </div>
              </el-form-item>
              
              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  :loading="loginLoading"
                  @click="handleLogin"
                  style="width: 100%"
                >
                  登录
                </el-button>
              </el-form-item>
              
              <div class="switch-link">
                还没有账户？
                <el-link type="primary" :underline="false" @click="switchToRegister">
                  立即注册
                </el-link>
              </div>
            </el-form>
          </div>
        </div>
      </transition>

      <!-- 注册表单 -->
      <transition name="slide-left">
        <div 
          v-if="isRegisterMode" 
          class="form-container register-container"
        >
          <div class="auth-card">
            <div class="auth-header">
              <h2>用户注册</h2>
              <p>创建您的操作员账户</p>
            </div>
            
            <el-form
              ref="registerFormRef"
              :model="registerForm"
              :rules="registerRules"
              size="large"
              @submit.prevent="handleRegister"
              class="auth-form"
            >
              <el-form-item prop="username">
                <el-input
                  v-model="registerForm.username"
                  placeholder="请输入用户名"
                  prefix-icon="User"
                  clearable
                />
              </el-form-item>
              
              <el-form-item prop="password">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  placeholder="请输入密码"
                  prefix-icon="Lock"
                  show-password
                  clearable
                />
              </el-form-item>
              
              <el-form-item prop="confirmPassword">
                <el-input
                  v-model="registerForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入密码"
                  prefix-icon="Lock"
                  show-password
                  clearable
                />
              </el-form-item>
              
              <el-form-item prop="email">
                <el-input
                  v-model="registerForm.email"
                  placeholder="请输入邮箱地址"
                  prefix-icon="Message"
                  clearable
                />
              </el-form-item>
              
              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  :loading="registerLoading"
                  @click="handleRegister"
                  style="width: 100%"
                >
                  {{ registerLoading ? '注册中...' : '注册' }}
                </el-button>
              </el-form-item>
              
              <div class="switch-link">
                已有账户？
                <el-link type="primary" :underline="false" @click="switchToLogin">
                  立即登录
                </el-link>
              </div>
            </el-form>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue"
import { useRouter } from "vue-router"
import { ElMessage, type FormInstance, type FormRules } from "element-plus"
import { useUserStore } from "@/stores/user"
import { login, register, type LoginRequest, type RegisterRequest } from "@/api/auth"

const router = useRouter()
const userStore = useUserStore()

// 状态管理
const isRegisterMode = ref(false)
const loginLoading = ref(false)
const registerLoading = ref(false)

// 表单引用
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()

// 登录表单
const loginForm = reactive({
  username: "",
  password: "",
  remember: false
})

// 注册表单
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  role: 'OPERATOR' as const
})

// 登录表单验证规则
const loginRules: FormRules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 3, max: 20, message: "用户名长度在 3 到 20 个字符", trigger: "blur" }
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 20, message: "密码长度在 6 到 20 个字符", trigger: "blur" }
  ]
}

// 注册表单验证规则
const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)/, message: '密码必须包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

// 切换到注册模式
const switchToRegister = () => {
  isRegisterMode.value = true
}

// 切换到登录模式
const switchToLogin = () => {
  isRegisterMode.value = false
}

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  try {
    await loginFormRef.value.validate()
    loginLoading.value = true
    
    const loginData: LoginRequest = {
      username: loginForm.username,
      password: loginForm.password
    }
    
    await userStore.login(loginData)
    
    // 保存记住我状态
    if (loginForm.remember) {
      localStorage.setItem("remember", "true")
      localStorage.setItem("username", loginForm.username)
    } else {
      localStorage.removeItem("remember")
      localStorage.removeItem("username")
    }
    
    ElMessage.success("登录成功")
    
    // 根据角色跳转到对应页面
    if (userStore.isAdmin) {
      router.push("/admin/dashboard")
    } else if (userStore.isOperator) {
      router.push("/operator/dashboard")
    } else {
      router.push("/")
    }
  } catch (error: any) {
    console.error("登录失败:", error)
    ElMessage.error(error.message || "登录失败，请检查用户名和密码")
  } finally {
    loginLoading.value = false
  }
}

// 处理注册
const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  try {
    await registerFormRef.value.validate()
    registerLoading.value = true
    
    const registerData: RegisterRequest = {
      username: registerForm.username,
      password: registerForm.password,
      email: registerForm.email,
      role: registerForm.role
    }
    
    const response = await register(registerData)
    
    if (response.success) {
      ElMessage.success('注册成功，即将跳转到登录页面')
      
      // 延迟切换到登录模式并填充用户名
      setTimeout(() => {
        loginForm.username = registerForm.username
        switchToLogin()
      }, 1500)
    } else {
      ElMessage.error(response.message || '注册失败')
    }
  } catch (error: any) {
    console.error('注册失败:', error)
    ElMessage.error(error.message || '注册失败，请稍后重试')
  } finally {
    registerLoading.value = false
  }
}

// 组件挂载时初始化
onMounted(() => {
  // 初始化用户信息
  userStore.initUserInfo()
  
  // 如果已经登录，跳转到对应页面
  if (userStore.isLoggedIn) {
    if (userStore.isAdmin) {
      router.push('/admin/dashboard')
    } else if (userStore.isOperator) {
      router.push('/operator/dashboard')
    }
  }

  // 检查是否有记住的用户名
  const remember = localStorage.getItem("remember")
  const savedUsername = localStorage.getItem("username")
  
  if (remember === "true" && savedUsername) {
    loginForm.username = savedUsername
    loginForm.remember = true
  }
  
  // 检查路由参数
  const route = router.currentRoute.value
  if (route.query.username) {
    loginForm.username = route.query.username as string
  }
  if (route.path === '/register') {
    isRegisterMode.value = true
  }
})
</script>

<style scoped>
.auth-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 50%, #cbd5e1 100%);
  position: relative;
  overflow: hidden;
}

/* 背景大圆 */
.background-circles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.circle {
  position: absolute;
  width: 60vw;
  height: 60vw;
  max-width: 800px;
  max-height: 800px;
  border-radius: 50%;
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0.6;
}

.circle-right {
  top: 50%;
  right: -30vw;
  transform: translateY(-50%);
  background: linear-gradient(135deg, 
    rgba(59, 130, 246, 0.15) 0%, 
    rgba(139, 92, 246, 0.15) 50%, 
    rgba(6, 182, 212, 0.15) 100%);
}

.circle-left {
  top: 50%;
  left: -30vw;
  transform: translateY(-50%);
  background: linear-gradient(135deg, 
    rgba(16, 185, 129, 0.15) 0%, 
    rgba(59, 130, 246, 0.15) 50%, 
    rgba(139, 92, 246, 0.15) 100%);
}

.circle.active {
  opacity: 1;
  transform: translateY(-50%) scale(1.1);
}

/* 内容区域 */
.content-wrapper {
  position: relative;
  z-index: 2;
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-container {
  position: absolute;
  width: 100%;
  max-width: 420px;
  padding: 20px;
}

.login-container {
  right: 15%;
}

.register-container {
  left: 15%;
}

.auth-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  box-shadow: 
    0 25px 50px -12px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.8);
  padding: 48px 40px;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
}

.auth-card:hover {
  transform: translateY(-2px);
  box-shadow: 
    0 32px 64px -12px rgba(0, 0, 0, 0.2),
    0 0 0 1px rgba(255, 255, 255, 0.9);
}

.auth-header {
  text-align: center;
  margin-bottom: 40px;
}

.auth-header h2 {
  color: #1e293b;
  font-size: 32px;
  font-weight: 800;
  margin: 0 0 12px 0;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 50%, #06b6d4 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.5px;
}

.auth-header p {
  color: #64748b;
  font-size: 16px;
  margin: 0;
  font-weight: 400;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.switch-link {
  text-align: center;
  width: 100%;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
}

:deep(.el-form-item) {
  margin-bottom: 24px;
}

:deep(.el-input__wrapper) {
  border-radius: 16px;
  box-shadow: 
    0 1px 3px rgba(0, 0, 0, 0.05),
    0 0 0 1px rgba(226, 232, 240, 0.8);
  border: none;
  background: rgba(248, 250, 252, 0.8);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  height: 52px;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 
    0 4px 6px -1px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(59, 130, 246, 0.2);
  background: rgba(255, 255, 255, 0.9);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 
    0 4px 6px -1px rgba(59, 130, 246, 0.15),
    0 0 0 2px rgba(59, 130, 246, 0.2);
  background: rgba(255, 255, 255, 1);
}

:deep(.el-input__inner) {
  font-weight: 500;
  color: #1e293b;
}

:deep(.el-input__inner::placeholder) {
  color: #94a3b8;
  font-weight: 400;
}

:deep(.el-button) {
  border-radius: 16px;
  font-weight: 600;
  font-size: 16px;
  height: 52px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  border: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 14px 0 rgba(59, 130, 246, 0.25);
  letter-spacing: 0.5px;
}

:deep(.el-button:hover) {
  transform: translateY(-1px);
  box-shadow: 0 8px 25px 0 rgba(59, 130, 246, 0.35);
  background: linear-gradient(135deg, #2563eb 0%, #7c3aed 100%);
}

:deep(.el-button:active) {
  transform: translateY(0);
}

:deep(.el-checkbox__label) {
  color: #64748b;
  font-weight: 500;
}

:deep(.el-link) {
  font-weight: 600;
  color: #3b82f6;
}

:deep(.el-link:hover) {
  color: #2563eb;
}

:deep(.el-form-item__error) {
  font-size: 12px;
  color: #ef4444;
  margin-top: 6px;
  font-weight: 500;
}

/* 切换动画 */
.slide-right-enter-active,
.slide-right-leave-active,
.slide-left-enter-active,
.slide-left-leave-active {
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-right-enter-from {
  opacity: 0;
  transform: translateX(100px);
}

.slide-right-leave-to {
  opacity: 0;
  transform: translateX(-100px);
}

.slide-left-enter-from {
  opacity: 0;
  transform: translateX(-100px);
}

.slide-left-leave-to {
  opacity: 0;
  transform: translateX(100px);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .login-container {
    right: 10%;
  }
  
  .register-container {
    left: 10%;
  }
}

@media (max-width: 768px) {
  .login-container,
  .register-container {
    position: static;
    max-width: 100%;
    padding: 20px;
  }
  
  .circle {
    width: 80vw;
    height: 80vw;
  }
  
  .circle-right {
    right: -40vw;
  }
  
  .circle-left {
    left: -40vw;
  }
  
  .auth-card {
    padding: 32px 24px;
    border-radius: 20px;
  }
  
  .auth-header h2 {
    font-size: 28px;
  }
  
  :deep(.el-input__wrapper),
  :deep(.el-button) {
    height: 48px;
  }
}
</style>