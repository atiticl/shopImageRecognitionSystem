<template>
  <div class="mall-app">
    <!-- 顶部栏 -->
    <header class="mall-header">
      <div class="header-inner">
        <div class="logo">
          <span class="logo-icon">🛍️</span>
          <span class="logo-text">商品智能展示</span>
        </div>
        <div class="search-box">
          <el-input
            v-model="keyword"
            placeholder="搜索商品图片..."
            clearable
            size="large"
            @keyup.enter="onSearch"
          >
            <template #prepend>
              <el-select v-model="searchScope" style="width: 110px">
                <el-option label="全部商品" value="all" />
                <el-option label="当前类目" value="current" />
              </el-select>
            </template>
            <template #append>
              <el-button type="primary" :icon="Search" @click="onSearch">搜索</el-button>
            </template>
          </el-input>
        </div>
        <div class="header-actions">
          <span class="stat-badge">共 {{ totalCount }} 件商品</span>
        </div>
      </div>
    </header>

    <!-- 主体 -->
    <div class="mall-body">
      <!-- 左侧分类栏 -->
      <aside class="mall-sidebar">
        <div class="sidebar-title">
          <el-icon><Menu /></el-icon>
          <span>全部分类</span>
        </div>
        <ul class="category-list">
          <li
            v-for="cat in categories"
            :key="cat.id"
            class="category-item"
            :class="{ active: selectedCategoryId === cat.id }"
            @click="selectCategory(cat.id)"
          >
            <span class="cat-name">{{ cat.name }}</span>
            <span class="cat-count">{{ cat.count }}</span>
          </li>
          <li v-if="!loadingCategories && categories.length === 0" class="empty-hint">
            暂无分类数据
          </li>
        </ul>
      </aside>

      <!-- 右侧内容 -->
      <main class="mall-main">
        <!-- 面包屑 -->
        <div class="breadcrumb-bar">
          <span class="bc-home">首页</span>
          <el-icon><ArrowRight /></el-icon>
          <span class="bc-current">{{ currentCategoryName }}</span>
          <span class="bc-total">共 {{ total }} 件</span>
        </div>

        <!-- 图片网格 -->
        <div v-loading="loadingImages" class="product-grid">
          <div
            v-for="item in images"
            :key="item.imageId"
            class="product-card"
            @click="openPreview(item)"
          >
            <div class="product-image">
              <img :src="item.imageUrl" :alt="item.fileName" loading="lazy" />
              <span v-if="item.corrected" class="badge corrected">已人工确认</span>
              <span
                v-else-if="confidenceLevel(item.confidence) === 'high'"
                class="badge high"
              >自动归类</span>
            </div>
            <div class="product-info">
              <div class="product-name" :title="item.fileName">{{ item.fileName }}</div>
              <div class="product-meta">
                <el-tag size="small" :type="tagType(item.confidence)">{{ item.categoryName }}</el-tag>
                <span v-if="item.confidence != null" class="confidence">
                  {{ formatConfidence(item.confidence) }}
                </span>
              </div>
            </div>
          </div>
          <div v-if="!loadingImages && images.length === 0" class="empty-grid">
            <el-empty description="暂无商品图片" />
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="total > 0" class="pagination-bar">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :page-sizes="[12, 24, 48, 96]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="loadImages"
            @size-change="onSizeChange"
          />
        </div>
      </main>
    </div>

    <!-- 图片预览 -->
    <el-dialog v-model="previewVisible" width="60%" :title="previewItem?.fileName" center>
      <div class="preview-body">
        <img v-if="previewItem" :src="previewItem.imageUrl" :alt="previewItem.fileName" />
        <div v-if="previewItem" class="preview-meta">
          <p><strong>类别：</strong>{{ previewItem.categoryName }}</p>
          <p v-if="previewItem.confidence != null">
            <strong>置信度：</strong>{{ formatConfidence(previewItem.confidence) }}
          </p>
          <p><strong>状态：</strong>{{ previewItem.corrected ? '已人工确认' : '系统自动分类' }}</p>
          <p><strong>上传时间：</strong>{{ formatTime(previewItem.uploadedAt) }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Search, Menu, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  fetchCategories,
  fetchImages,
  type CategoryItem,
  type ImageItem
} from '@/api/exhibition'

const categories = ref<CategoryItem[]>([])
const images = ref<ImageItem[]>([])
const selectedCategoryId = ref<number>(0)
const keyword = ref('')
const searchScope = ref<'all' | 'current'>('all')
const page = ref(1)
const pageSize = ref(24)
const total = ref(0)
const loadingCategories = ref(false)
const loadingImages = ref(false)

const previewVisible = ref(false)
const previewItem = ref<ImageItem | null>(null)

const totalCount = computed(() => {
  const all = categories.value.find(c => c.id === 0)
  return all ? all.count : 0
})

const currentCategoryName = computed(() => {
  const cat = categories.value.find(c => c.id === selectedCategoryId.value)
  return cat ? cat.name : '全部'
})

const loadCategories = async () => {
  loadingCategories.value = true
  try {
    categories.value = await fetchCategories()
  } catch (e: any) {
    ElMessage.error('加载分类失败: ' + (e.message || '未知错误'))
  } finally {
    loadingCategories.value = false
  }
}

const loadImages = async () => {
  loadingImages.value = true
  try {
    const result = await fetchImages({
      categoryId: selectedCategoryId.value,
      keyword: keyword.value.trim() || undefined,
      page: page.value,
      size: pageSize.value
    })
    images.value = result.list
    total.value = result.total
  } catch (e: any) {
    ElMessage.error('加载图片失败: ' + (e.message || '未知错误'))
  } finally {
    loadingImages.value = false
  }
}

const selectCategory = (id: number) => {
  selectedCategoryId.value = id
  page.value = 1
  loadImages()
}

const onSearch = () => {
  if (searchScope.value === 'all') {
    selectedCategoryId.value = 0
  }
  page.value = 1
  loadImages()
}

const onSizeChange = (size: number) => {
  pageSize.value = size
  page.value = 1
  loadImages()
}

const openPreview = (item: ImageItem) => {
  previewItem.value = item
  previewVisible.value = true
}

const confidenceLevel = (c: number | null) => {
  if (c == null) return 'low'
  if (c >= 0.9) return 'high'
  if (c >= 0.6) return 'mid'
  return 'low'
}

const tagType = (c: number | null): 'success' | 'warning' | 'info' => {
  const lvl = confidenceLevel(c)
  if (lvl === 'high') return 'success'
  if (lvl === 'mid') return 'warning'
  return 'info'
}

const formatConfidence = (c: number | null) => {
  if (c == null) return '-'
  return (c * 100).toFixed(1) + '%'
}

const formatTime = (t: string | null) => {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

onMounted(async () => {
  await loadCategories()
  await loadImages()
})
</script>

<style scoped>
.mall-app {
  min-height: 100vh;
  background: #f5f5f5;
}

.mall-header {
  background: linear-gradient(135deg, #ff6b35, #f7931e);
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 700;
  white-space: nowrap;
}

.logo-icon {
  font-size: 28px;
}

.search-box {
  flex: 1;
  max-width: 640px;
}

.header-actions {
  margin-left: auto;
}

.stat-badge {
  background: rgba(255, 255, 255, 0.2);
  padding: 6px 14px;
  border-radius: 16px;
  font-size: 13px;
}

.mall-body {
  max-width: 1400px;
  margin: 16px auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 16px;
  align-items: flex-start;
}

.mall-sidebar {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  position: sticky;
  top: 88px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.sidebar-title {
  background: linear-gradient(135deg, #ff6b35, #f7931e);
  color: #fff;
  padding: 14px 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.category-list {
  list-style: none;
  margin: 0;
  padding: 8px 0;
  max-height: calc(100vh - 180px);
  overflow-y: auto;
}

.category-item {
  padding: 11px 18px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.2s;
  border-left: 3px solid transparent;
  color: #333;
}

.category-item:hover {
  background: #fff5f0;
  color: #ff6b35;
}

.category-item.active {
  background: #fff5f0;
  color: #ff6b35;
  border-left-color: #ff6b35;
  font-weight: 600;
}

.cat-count {
  background: #f0f0f0;
  color: #888;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  min-width: 20px;
  text-align: center;
}

.category-item.active .cat-count {
  background: #ff6b35;
  color: #fff;
}

.empty-hint {
  padding: 20px;
  text-align: center;
  color: #999;
  font-size: 13px;
}

.mall-main {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px 24px;
  min-height: 600px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.breadcrumb-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0 16px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
  font-size: 14px;
  color: #666;
}

.bc-current {
  color: #ff6b35;
  font-weight: 600;
}

.bc-total {
  margin-left: auto;
  color: #999;
  font-size: 13px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  min-height: 400px;
}

.product-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 20px rgba(255, 107, 53, 0.15);
  border-color: #ff6b35;
}

.product-image {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  overflow: hidden;
  background: #fafafa;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.product-card:hover .product-image img {
  transform: scale(1.05);
}

.badge {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 11px;
  color: #fff;
  font-weight: 500;
}

.badge.corrected {
  background: #67c23a;
}

.badge.high {
  background: #ff6b35;
}

.product-info {
  padding: 10px 12px 12px;
}

.product-name {
  font-size: 13px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 8px;
}

.product-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 6px;
}

.confidence {
  font-size: 12px;
  color: #ff6b35;
  font-weight: 600;
}

.empty-grid {
  grid-column: 1 / -1;
  padding: 60px 0;
}

.pagination-bar {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.preview-body {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.preview-body img {
  max-width: 65%;
  max-height: 70vh;
  object-fit: contain;
  border-radius: 4px;
}

.preview-meta {
  flex: 1;
  color: #555;
}

.preview-meta p {
  margin: 8px 0;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .mall-body {
    grid-template-columns: 1fr;
  }
  .mall-sidebar {
    position: static;
  }
  .category-list {
    max-height: none;
    display: flex;
    flex-wrap: wrap;
    padding: 8px;
  }
  .category-item {
    border-left: none;
    padding: 8px 14px;
  }
  .header-inner {
    flex-wrap: wrap;
    gap: 12px;
  }
  .preview-body {
    flex-direction: column;
  }
  .preview-body img {
    max-width: 100%;
  }
}
</style>
