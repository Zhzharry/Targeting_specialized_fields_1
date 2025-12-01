<!-- src/views/SearchPage.vue -->
<template>
  <div class="search-page">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="logo" @click="$router.push('/')">房产平台</div>
      <div class="header-actions">
        <button class="icon-btn" @click="$router.push('/login')">
          <span>👤</span>
        </button>
      </div>
    </header>

    <!-- 搜索栏 -->
    <div class="search-section">
      <div class="search-box">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="输入小区名、区域或关键词..."
          @input="handleSearchInput"
          @focus="showSuggestions = true"
        />
        <button @click="performSearch" class="search-btn">搜索</button>
      </div>

      <!-- 搜索建议 -->
      <div v-if="showSuggestions && searchSuggestions.length" class="suggestions">
        <div
          v-for="suggestion in searchSuggestions"
          :key="suggestion"
          class="suggestion-item"
          @click="selectSuggestion(suggestion)"
        >
          🔍 {{ suggestion }}
        </div>
      </div>
    </div>

    <!-- 快速筛选 -->
    <div class="quick-filters">
      <div class="filter-tags">
        <span
          v-for="filter in quickFilters"
          :key="filter.value"
          class="filter-tag"
          :class="{ active: activeFilter === filter.value }"
          @click="setActiveFilter(filter.value)"
        >
          {{ filter.label }}
        </span>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="content-area">
      <!-- 标签页切换 -->
      <div class="tab-container">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'search' }"
          @click="activeTab = 'search'"
        >
          搜索结果
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'recommend' }"
          @click="activeTab = 'recommend'"
        >
          猜你喜欢
        </button>
        <button class="tab-btn" :class="{ active: activeTab === 'hot' }" @click="activeTab = 'hot'">
          热门推荐
        </button>
      </div>

      <!-- 搜索结果 -->
      <div v-if="activeTab === 'search'" class="tab-content">
        <div v-if="loading" class="loading-state">
          <div class="spinner"></div>
          <p>搜索中...</p>
        </div>

        <div v-else-if="searchResults.length === 0" class="empty-state">
          <div class="empty-icon">🏠</div>
          <h3>暂无搜索结果</h3>
          <p>尝试调整搜索关键词或筛选条件</p>
          <button @click="resetSearch" class="reset-btn">重新搜索</button>
        </div>

        <div v-else class="results-container">
          <div class="results-count">找到 {{ searchResults.length }} 个房源</div>
          <div class="property-list">
            <div
              v-for="property in searchResults"
              :key="property.id"
              class="property-card"
              @click="viewProperty(property.id)"
            >
              <img :src="property.image" :alt="property.title" class="property-image" />
              <div class="property-info">
                <h3 class="property-title">{{ property.title }}</h3>
                <p class="property-location">{{ property.community }}</p>
                <div class="property-meta">
                  <span>{{ property.bedrooms }}室{{ property.livingRooms }}厅</span>
                  <span>{{ property.area }}㎡</span>
                  <span>{{ property.floor }}</span>
                </div>
                <div class="property-price">
                  <span class="price">¥{{ property.price }}</span>
                  <span class="unit">/月</span>
                </div>
                <div class="property-tags">
                  <span v-for="tag in property.tags" :key="tag" class="tag">{{ tag }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 猜你喜欢 -->
      <div v-if="activeTab === 'recommend'" class="tab-content">
        <div class="discover-section">
          <div class="section-header">
            <h3>猜你喜欢</h3>
            <button @click="refreshDiscover" class="refresh-btn">🔄 换一换</button>
          </div>
          <div class="discover-hint">
            <p>摇一摇手机或点击换一换发现更多惊喜房源</p>
          </div>
          <div class="property-list">
            <div
              v-for="property in discoverProperties"
              :key="property.id"
              class="property-card"
              @click="viewProperty(property.id)"
            >
              <img :src="property.image" :alt="property.title" class="property-image" />
              <div class="property-info">
                <h3 class="property-title">{{ property.title }}</h3>
                <p class="property-location">{{ property.community }}</p>
                <div class="property-meta">
                  <span>{{ property.bedrooms }}室{{ property.livingRooms }}厅</span>
                  <span>{{ property.area }}㎡</span>
                </div>
                <div class="property-price">
                  <span class="price">¥{{ property.price }}</span>
                  <span class="unit">/月</span>
                </div>
                <div class="property-tags">
                  <span v-for="tag in property.tags" :key="tag" class="tag">{{ tag }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 热门推荐 -->
      <div v-if="activeTab === 'hot'" class="tab-content">
        <div class="hot-section">
          <h3>热门推荐</h3>
          <div class="property-list">
            <div
              v-for="property in hotProperties"
              :key="property.id"
              class="property-card"
              @click="viewProperty(property.id)"
            >
              <img :src="property.image" :alt="property.title" class="property-image" />
              <div class="property-info">
                <h3 class="property-title">{{ property.title }}</h3>
                <p class="property-location">{{ property.community }}</p>
                <div class="property-meta">
                  <span>{{ property.bedrooms }}室{{ property.livingRooms }}厅</span>
                  <span>{{ property.area }}㎡</span>
                </div>
                <div class="property-price">
                  <span class="price">¥{{ property.price }}</span>
                  <span class="unit">/月</span>
                </div>
                <div class="property-tags">
                  <span v-for="tag in property.tags" :key="tag" class="tag">{{ tag }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部导航 -->
    <div class="bottom-nav">
      <button class="nav-btn" @click="$router.push('/profile')">
        <span>👤</span>
        <span>我的</span>
      </button>
      <button class="nav-btn active">
        <span>🔍</span>
        <span>搜索</span>
      </button>
      <button class="nav-btn" @click="$router.push('/tools')">
        <span>📊</span>
        <span>工具</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

// 搜索状态
const searchQuery = ref('')
const showSuggestions = ref(false)
const loading = ref(false)
const activeTab = ref('search')
const activeFilter = ref('all')

// 搜索建议
const searchSuggestions = ref<string[]>([])

// 快速筛选
const quickFilters = ref([
  { value: 'all', label: '全部' },
  { value: 'nearby', label: '附近' },
  { value: 'cheap', label: '低价' },
  { value: 'new', label: '最新' },
  { value: 'hot', label: '热门' },
])

// 模拟数据
interface Property {
  id: number
  title: string
  community: string
  bedrooms: number
  livingRooms: number
  area: number
  price: number
  image: string
  tags: string[]
  floor?: string
}

const searchResults = ref<Property[]>([])
const discoverProperties = ref<Property[]>([])
const hotProperties = ref<Property[]>([])

// 模拟房源数据
const mockProperties: Property[] = [
  {
    id: 1,
    title: '精装修两居室',
    community: '阳光小区',
    bedrooms: 2,
    livingRooms: 1,
    area: 85,
    price: 4500,
    image: 'https://img95.699pic.com/photo/50149/6896.jpg_wh860.jpg',
    tags: ['近地铁', '精装修'],
    floor: '中层/18层',
  },
  {
    id: 2,
    title: '豪华三居室',
    community: '花园社区',
    bedrooms: 3,
    livingRooms: 2,
    area: 120,
    price: 6800,
    image: 'https://img95.699pic.com/photo/50149/6896.jpg_wh860.jpg',
    tags: ['学区房', '电梯房'],
    floor: '高层/24层',
  },
  {
    id: 3,
    title: '温馨一居室',
    community: '幸福家园',
    bedrooms: 1,
    livingRooms: 1,
    area: 60,
    price: 3200,
    image: 'https://img95.699pic.com/photo/50149/6896.jpg_wh860.jpg',
    tags: ['拎包入住', '朝南'],
    floor: '低层/6层',
  },
  {
    id: 4,
    title: '现代LOFT公寓',
    community: '创意园区',
    bedrooms: 1,
    livingRooms: 1,
    area: 45,
    price: 3800,
    image: 'https://img95.699pic.com/photo/50149/6896.jpg_wh860.jpg',
    tags: ['LOFT', '创意空间'],
    floor: '中层/12层',
  },
]

// 方法
const handleSearchInput = () => {
  if (searchQuery.value.length > 1) {
    searchSuggestions.value = [
      `${searchQuery.value}小区`,
      `${searchQuery.value}家园`,
      `${searchQuery.value}公寓`,
      `${searchQuery.value}社区`,
    ]
  } else {
    searchSuggestions.value = []
  }
}

const selectSuggestion = (suggestion: string) => {
  searchQuery.value = suggestion
  showSuggestions.value = false
  performSearch()
}

const performSearch = async () => {
  loading.value = true
  showSuggestions.value = false

  // 模拟搜索API调用
  setTimeout(() => {
    if (searchQuery.value) {
      searchResults.value = mockProperties.filter(
        (property) =>
          property.community.includes(searchQuery.value) ||
          property.title.includes(searchQuery.value),
      )
    } else {
      searchResults.value = [...mockProperties]
    }
    loading.value = false
  }, 1000)
}

const setActiveFilter = (filter: string) => {
  activeFilter.value = filter
  // 这里可以根据筛选条件过滤结果
  performSearch()
}

const refreshDiscover = () => {
  // 随机打乱显示猜你喜欢
  discoverProperties.value = [...mockProperties].sort(() => Math.random() - 0.5).slice(0, 2)
}

const resetSearch = () => {
  searchQuery.value = ''
  searchResults.value = []
  activeTab.value = 'search'
}

const viewProperty = (propertyId: number) => {
  // 这里可以跳转到详情页，暂时用alert代替
  alert(`查看房源详情: ${propertyId}`)
  // router.push(`/property/${propertyId}`)
}

// 摇一摇功能
let lastShakeTime = 0
const handleDeviceMotion = (event: DeviceMotionEvent) => {
  const acceleration = event.accelerationIncludingGravity
  if (!acceleration) return

  const shakeThreshold = 15
  const currentTime = Date.now()

  if (currentTime - lastShakeTime > 1000) {
    const totalForce =
      Math.abs(acceleration.x!) + Math.abs(acceleration.y!) + Math.abs(acceleration.z!)

    if (totalForce > shakeThreshold) {
      lastShakeTime = currentTime
      activeTab.value = 'recommend'
      refreshDiscover()
    }
  }
}

// 初始化
onMounted(() => {
  // 初始加载热门推荐
  hotProperties.value = [...mockProperties]
  discoverProperties.value = [...mockProperties].slice(0, 2)

  // 监听摇一摇
  if (window.DeviceMotionEvent) {
    window.addEventListener('devicemotion', handleDeviceMotion)
  }
})

onUnmounted(() => {
  if (window.DeviceMotionEvent) {
    window.removeEventListener('devicemotion', handleDeviceMotion)
  }
})
</script>

<style scoped>
.search-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 60px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: white;
  border-bottom: 1px solid #eee;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  color: #007bff;
  cursor: pointer;
}

.header-actions {
  display: flex;
  gap: 15px;
}

.icon-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 8px;
}

.search-section {
  background: white;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.search-box {
  display: flex;
  gap: 10px;
}

.search-box input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 16px;
  background: #f8f9fa;
}

.search-btn {
  padding: 12px 20px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
}

.suggestions {
  margin-top: 10px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.suggestion-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
}

.suggestion-item:hover {
  background: #f8f9fa;
}

.quick-filters {
  background: white;
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
}

.filter-tags {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 5px;
}

.filter-tag {
  padding: 6px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 16px;
  font-size: 14px;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.3s ease;
}

.filter-tag.active {
  background: #007bff;
  color: white;
  border-color: #007bff;
}

.content-area {
  padding: 0 20px;
}

.tab-container {
  display: flex;
  background: white;
  border-radius: 8px 8px 0 0;
  margin-top: 15px;
  overflow: hidden;
}

.tab-btn {
  flex: 1;
  padding: 15px;
  background: none;
  border: none;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab-btn.active {
  background: #007bff;
  color: white;
}

.tab-content {
  background: white;
  padding: 20px;
  border-radius: 0 0 8px 8px;
  min-height: 400px;
}

.loading-state {
  text-align: center;
  padding: 40px 0;
  color: #666;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #007bff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.reset-btn {
  margin-top: 20px;
  padding: 10px 20px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.results-count {
  margin-bottom: 16px;
  color: #666;
  font-size: 14px;
}

.property-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.property-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: transform 0.2s ease;
}

.property-card:hover {
  transform: translateY(-2px);
}

.property-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.property-info {
  padding: 16px;
}

.property-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 6px;
  color: #333;
}

.property-location {
  color: #666;
  font-size: 14px;
  margin-bottom: 10px;
}

.property-meta {
  display: flex;
  gap: 12px;
  margin-bottom: 10px;
  color: #888;
  font-size: 13px;
}

.property-price {
  margin-bottom: 12px;
}

.price {
  font-size: 20px;
  font-weight: 700;
  color: #ff4757;
}

.unit {
  color: #888;
  font-size: 14px;
  margin-left: 2px;
}

.property-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag {
  padding: 4px 8px;
  background: #e3f2fd;
  color: #1976d2;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.refresh-btn {
  background: #f8f9fa;
  border: 1px solid #e0e0e0;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.discover-hint {
  text-align: center;
  padding: 20px;
  color: #666;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 15px;
}

.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  background: white;
  border-top: 1px solid #e0e0e0;
  padding: 8px 0;
}

.nav-btn {
  flex: 1;
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
  transition: color 0.3s ease;
}

.nav-btn.active {
  color: #007bff;
}
</style>
