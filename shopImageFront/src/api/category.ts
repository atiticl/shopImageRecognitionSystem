import { http } from '@/utils/request'
import type { Category, CategoryRequest, ApiResponse, PageResponse } from '@/types'

// 类别管理API
export const categoryApi = {
  // 获取所有类别列表
  getCategories(params?: { 
    name?: string; 
    isActive?: boolean; 
    page?: number; 
    size?: number 
  }): Promise<ApiResponse<any>> {
    return http.get('/api/categories', { params })
  },

  // 创建类别
  createCategory(data: CategoryRequest): Promise<ApiResponse<Category>> {
    return http.post('/api/categories', data)
  },

  // 更新类别
  updateCategory(id: number, data: CategoryRequest): Promise<ApiResponse<Category>> {
    return http.put(`/api/categories/${id}`, data)
  },

  // 删除类别
  deleteCategory(id: number): Promise<ApiResponse<boolean>> {
    return http.delete(`/api/categories/${id}`)
  },

  // 获取单个类别详情
  getCategory(id: number): Promise<ApiResponse<Category>> {
    return http.get(`/api/categories/${id}`)
  },

  // 获取类别树结构
  getCategoryTree(): Promise<ApiResponse<Category[]>> {
    return http.get('/api/categories/tree')
  }
}

export default categoryApi