<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h2>欢迎回来</h2>
        <p>请登录您的账户以继续使用</p>
      </div>
        
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          @submit.prevent="handleSubmit"
          class="login-form"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              clearable
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
              clearable
              @keyup.enter="handleSubmit"
            />
          </el-form-item>
          
          <el-form-item>
            <div class="form-options">
              <el-checkbox v-model="form.remember">
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
              :loading="loading"
              @click="handleSubmit"
              style="width: 100%"
            >
              登录
            </el-button>
          </el-form-item>
          
          <div class="register-link">
            还没有账户？
            <el-link type="primary" :underline="false" @click="goToRegister">
              立即注册
            </el-link>
          </div>
        </el-form>
    </div>
  </div>
</template>


<script setup lang="ts">
import { ref, reactive, onMounted } from "vue"
import { useRouter } from "vue-router"
import { ElMessage, type FormInstance, type FormRules } from "element-plus"
import { useUserStore } from "@/stores/user"
import { login, type LoginRequest } from "@/api/auth"

const router = useRouter()
const userStore = useUserStore()

// 表单引用
const formRef = ref<FormInstance>()

// 登录表单
const form = reactive({
  username: "",
  password: "",
  remember: false
})

// 状态
const loading = ref(false)

// 登录表单验证规则
const rules: FormRules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 3, max: 20, message: "用户名长度在 3 到 20 个字符", trigger: "blur" }
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 20, message: "密码长度在 6 到 20 个字符", trigger: "blur" }
  ]
}

// 处理登录
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    loading.value = true
    
    const loginData: LoginRequest = {
      username: form.username,
      password: form.password
    }
    
    await userStore.login(loginData)
    
    // 保存记住我状态
    if (form.remember) {
      localStorage.setItem("remember", "true")
      localStorage.setItem("username", form.username)
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
    loading.value = false
  }
}

// 跳转到注册页面
const goToRegister = () => {
  router.push("/register")
}

// 组件挂载时初始化
onMounted(() => {
  // 检查是否有记住的用户名
  const remember = localStorage.getItem("remember")
  const savedUsername = localStorage.getItem("username")
  
  if (remember === "true" && savedUsername) {
    form.username = savedUsername
    form.remember = true
  }
  
  // 检查是否从注册页面跳转过来，如果有用户名参数则自动填充
  const route = router.currentRoute.value
  if (route.query.username) {
    form.username = route.query.username as string
  }
})
</script>


<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 50%, #cbd5e1 100%);
  position: relative;
  padding: 20px;
  overflow: hidden;
}

/* 简约几何背景元素 */
.login-container::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 80%;
  height: 200%;
  background: linear-gradient(45deg, rgba(59, 130, 246, 0.03) 0%, rgba(147, 51, 234, 0.03) 100%);
  border-radius: 50%;
  transform: rotate(-15deg);
  z-index: 1;
}

.login-container::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -10%;
  width: 60%;
  height: 120%;
  background: linear-gradient(-45deg, rgba(16, 185, 129, 0.02) 0%, rgba(59, 130, 246, 0.02) 100%);
  border-radius: 50%;
  transform: rotate(20deg);
  z-index: 1;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  box-shadow: 
    0 25px 50px -12px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.8);
  padding: 48px 40px;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  position: relative;
  z-index: 2;
  transition: all 0.3s ease;
}

.login-card:hover {
  transform: translateY(-2px);
  box-shadow: 
    0 32px 64px -12px rgba(0, 0, 0, 0.12),
    0 0 0 1px rgba(255, 255, 255, 0.9);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-header h2 {
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

.login-header p {
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

.register-link {
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

/* 响应式设计 */
@media (max-width: 480px) {
  .login-card {
    padding: 32px 24px;
    margin: 0 16px;
    border-radius: 20px;
  }
  
  .login-header h2 {
    font-size: 28px;
  }
  
  :deep(.el-input__wrapper),
  :deep(.el-button) {
    height: 48px;
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

.login-card {
  animation: fadeInUp 0.6s ease-out;
}
</style>
