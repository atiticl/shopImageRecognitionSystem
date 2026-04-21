import axios from 'axios'

const api = axios.create({
  baseURL: '/api/public/exhibition',
  timeout: 15000
})

export interface CategoryItem {
  id: number
  name: string
  description?: string
  count: number
}

export interface ImageItem {
  imageId: number
  fileName: string
  uploadedAt: string
  categoryId: number | null
  categoryName: string
  confidence: number | null
  corrected: boolean
  imageUrl: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export const fetchCategories = async (): Promise<CategoryItem[]> => {
  const res = await api.get('/categories')
  return res.data?.data || []
}

export const fetchImages = async (params: {
  categoryId?: number
  keyword?: string
  page?: number
  size?: number
}): Promise<PageResult<ImageItem>> => {
  const res = await api.get('/images', { params })
  return res.data?.data || { list: [], total: 0, page: 1, size: 24 }
}
