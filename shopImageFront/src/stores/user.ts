import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AuthResponse, LoginRequest } from '@/api/auth'
import { login as apiLogin } from '@/api/auth'

export interface UserInfo {
  id?: number
  username: string
  role: 'ADMIN' | 'OPERATOR'
  email?: string
  realName?: string
  phone?: string
  department?: string
  avatar?: string
  status?: string
  taskCount?: number
  imageCount?: number
  onlineTime?: number
  createdAt?: string
  updatedAt?: string
  lastLoginAt?: string
  loginCount?: number
  remark?: string
}

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)
  const loading = ref(false)
  
  // 计算属性
  const isLoggedIn = computed(() => !!token.value && !!userInfo.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const isOperator = computed(() => userInfo.value?.role === 'OPERATOR')
  
  // 初始化用户信息（从localStorage读取）
  const initUserInfo = () => {
    const savedToken = localStorage.getItem('token')
    const savedUser = localStorage.getItem('user')
    
    if (savedToken) {
      token.value = savedToken
    }
    
    if (savedUser) {
      try {
        userInfo.value = JSON.parse(savedUser)
      } catch (error) {
        console.error('解析用户信息失败:', error)
        localStorage.removeItem('user')
      }
    }
  }
  
  // 登录
  const login = async (loginData: LoginRequest): Promise<void> => {
    loading.value = true
    try {
      const response = await apiLogin(loginData)
      
      if (response.success && response.data) {
        token.value = response.data.token
        userInfo.value = {
          username: response.data.username,
          role: response.data.role
        }
        
        // 保存到localStorage
        localStorage.setItem('token', response.data.token)
        localStorage.setItem('user', JSON.stringify(userInfo.value))
      } else {
        throw new Error(response.message || '登录失败')
      }
    } catch (error) {
      throw error
    } finally {
      loading.value = false
    }
  }
  
  // 登出
  const logout = () => {
    token.value = ''
    userInfo.value = null
    loading.value = false
    
    // 清除localStorage
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    localStorage.removeItem('remember')
  }
  
  // 更新用户信息
  const updateUserInfo = (newUserInfo: Partial<UserInfo>) => {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...newUserInfo }
      localStorage.setItem('user', JSON.stringify(userInfo.value))
    }
  }
  
  return {
    // 状态
    token,
    userInfo,
    loading,
    
    // 计算属性
    isLoggedIn,
    isAdmin,
    isOperator,
    
    // 方法
    initUserInfo,
    login,
    logout,
    updateUserInfo
  }
})