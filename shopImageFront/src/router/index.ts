import { createRouter, createWebHistory } from 'vue-router'
import { setupRouterGuards } from './guards'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { 
        requiresAuth: false,
        title: '用户登录'
      }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { 
        requiresAuth: false,
        title: '用户注册'
      }
    },
    {
      path: '/operator',
      name: 'OperatorLayout',
      component: () => import('@/layouts/OperatorLayout.vue'),
      meta: { 
        requiresAuth: true, 
        role: 'OPERATOR',
        title: '运营工作台'
      },
      children: [
        {
          path: '',
          redirect: '/operator/dashboard'
        },
        {
          path: 'dashboard',
          name: 'OperatorDashboard',
          component: () => import('@/views/operator/Dashboard.vue'),
          meta: {
            title: '工作台'
          }
        },
        {
          path: 'upload',
          name: 'ImageUpload',
          component: () => import('@/views/operator/ImageUpload.vue'),
          meta: {
            title: '图片上传'
          }
        },
        {
          path: 'results',
          name: 'ClassificationResults',
          component: () => import('@/views/operator/ClassificationResults.vue'),
          meta: {
            title: '分类结果'
          }
        },
        {
          path: 'tasks',
          name: 'TaskList',
          component: () => import('@/views/operator/TaskList.vue'),
          meta: {
            title: '任务列表'
          }
        }
      ]
    },
    {
      path: '/admin',
      name: 'AdminLayout',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { 
        requiresAuth: true, 
        role: 'ADMIN',
        title: '管理后台'
      },
      children: [
        {
          path: '',
          redirect: '/admin/dashboard'
        },
        {
          path: 'dashboard',
          name: 'AdminDashboard',
          component: () => import('@/views/admin/Dashboard.vue'),
          meta: {
            title: '数据看板'
          }
        },
        {
          path: 'categories',
          name: 'CategoryManager',
          component: () => import('@/views/admin/CategoryManagement.vue'),
          meta: {
            title: '类别管理'
          }
        },
        {
          path: 'models',
          name: 'ModelManager',
          component: () => import('@/views/admin/ModelManagement.vue'),
          meta: {
            title: '模型管理'
          }
        },
        {
          path: 'users',
          name: 'UserManager',
          component: () => import('@/views/admin/UserManagement.vue'),
          meta: {
            title: '用户管理'
          }
        },
        {
          path: 'logs',
          name: 'SystemLogs',
          component: () => import('@/views/admin/SystemLogs.vue'),
          meta: {
            title: '系统日志'
          }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/NotFound.vue'),
      meta: {
        title: '页面未找到'
      }
    }
  ]
})

// 设置路由守卫
setupRouterGuards(router)

export default router