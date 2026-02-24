import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

/**
 * 设置路由守卫
 * @param router 路由实例
 */
export function setupRouterGuards(router: Router) {
  // 全局前置守卫
  router.beforeEach((to, from, next) => {
    const userStore = useUserStore()
    
    // 初始化用户信息
    if (!userStore.token && !userStore.userInfo) {
      userStore.initUserInfo()
    }
    
    // 需要认证的路由
    const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
    
    // 如果需要认证但未登录，跳转到登录页
    if (requiresAuth && !userStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      next('/login')
      return
    }
    
    // 如果已登录但访问登录页或注册页，跳转到对应的首页
    if ((to.path === '/login' || to.path === '/register') && userStore.isLoggedIn) {
      if (userStore.isAdmin) {
        next('/admin/dashboard')
      } else if (userStore.isOperator) {
        next('/operator/dashboard')
      } else {
        next('/')
      }
      return
    }
    
    // 角色权限检查
    if (requiresAuth && userStore.isLoggedIn) {
      const requiredRole = to.meta.role as string
      
      if (requiredRole) {
        // 检查用户角色是否匹配
        if (requiredRole === 'ADMIN' && !userStore.isAdmin) {
          ElMessage.error('您没有权限访问该页面')
          next('/operator/dashboard')
          return
        }
        
        if (requiredRole === 'OPERATOR' && !userStore.isOperator) {
          ElMessage.error('您没有权限访问该页面')
          next('/admin/dashboard')
          return
        }
      }
    }
    
    next()
  })
  
  // 全局后置钩子
  router.afterEach((to) => {
    // 设置页面标题
    const title = to.meta.title as string
    if (title) {
      document.title = `${title} - 商品图片智能分类系统`
    } else {
      document.title = '商品图片智能分类系统'
    }
  })
}