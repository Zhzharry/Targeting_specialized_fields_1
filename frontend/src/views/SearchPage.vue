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
          @focus="showSuggestions = true"
          @blur="onSearchBlur"
          @keydown.enter="performSearch"
        />
        <button @click="performSearch" class="search-btn">搜索</button>
      </div>

      <!-- 搜索建议和搜索历史 -->
      <div v-if="showSuggestions" class="suggestions">
        <!-- 热门搜索 -->
        <div class="hot-search-section">
          <div class="section-title">热门搜索</div>
          <div class="hot-search-tags">
            <span
              v-for="word in hotSearchWords"
              :key="word"
              class="hot-search-tag"
              @click="selectHotSearch(word)"
            >
              🔥 {{ word }}
            </span>
          </div>
        </div>

        <!-- 搜索历史 -->
        <div v-if="searchHistory.length > 0" class="history-section">
          <div class="section-title">搜索历史</div>
          <div
            v-for="item in searchHistory"
            :key="item.id"
            class="suggestion-item history-item"
            @click="searchFromHistory(item)"
          >
            <div class="history-content">
              <span class="history-keyword">🕒 {{ item.keyword }}</span>
              <span class="history-meta">
                <span class="search-count" v-if="item.count > 1">{{ item.count }}次</span>
                <span class="search-time">{{ item.time }}</span>
              </span>
            </div>
            <span class="delete-history" @click.stop="deleteHistory(item)">×</span>
          </div>
          <div v-if="searchHistory.length > 0" class="clear-history" @click="clearAllHistory">
            清空搜索历史
          </div>
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
              :key="property.propertyId"
              class="property-card"
              @click="viewProperty(property.propertyId)"
            >
              <img :src="getPropertyImage(property)" :alt="property.title" class="property-image" />
              <div class="property-info">
                <h3 class="property-title">{{ property.title }}</h3>
                <p class="property-location">{{ property.communityName }}</p>
                <div class="property-meta">
                  <span>{{ property.layoutInfo.bedroom_count }}室{{ property.layoutInfo.living_room_count }}厅{{ property.layoutInfo.bathroom_count }}卫</span>
                  <span>{{ property.layoutInfo.area }}㎡</span>
                  <span>{{ property.locationInfo.district }}</span>
                </div>
                <div class="property-price">
                  <span class="price">¥{{ property.priceInfo.total_price }}</span>
                  <span class="unit">万</span>
                  <span class="unit-price">({{ Math.round(property.priceInfo.unit_price / 10000) }}万/㎡)</span>
                </div>
                <div class="property-tags">
                  <span class="tag">{{ property.basicInfo.property_type === 'apartment' ? '公寓' : '住宅' }}</span>
                  <span class="tag">{{ property.status === 'for_sale' ? '在售' : '已售' }}</span>
                  <span v-if="property.viewCount > 20" class="tag">热门</span>
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
            <p>点击换一换发现更多惊喜房源</p>
          </div>
              <div class="property-list">
            <div
              v-for="property in discoverProperties"
              :key="property.propertyId"
              class="property-card"
              @click="viewDiscoverProperty(property)"
            >
              <img :src="property.cover" :alt="property.title" class="property-image" />
              <div class="property-info">
                <h3 class="property-title">{{ property.title }}</h3>
                <p class="property-location">{{ property.summary }}</p>
                <div class="property-price">
                  <span class="price">¥{{ property.totalPrice }}</span>
                  <span class="unit">万</span>
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
              :key="property.propertyId"
              class="property-card"
              @click="viewProperty(property.propertyId)"
            >
              <img :src="getPropertyImage(property)" :alt="property.title" class="property-image" />
              <div class="property-info">
                <h3 class="property-title">{{ property.title }}</h3>
                <p class="property-location">{{ property.communityName }}</p>
                <div class="property-meta">
                  <span>{{ property.layoutInfo.bedroom_count }}室{{ property.layoutInfo.living_room_count }}厅</span>
                  <span>{{ property.layoutInfo.area }}㎡</span>
                </div>
                <div class="property-price">
                  <span class="price">¥{{ property.priceInfo.total_price }}</span>
                  <span class="unit">万</span>
                </div>
                <div class="property-tags">
                  <span class="tag">{{ property.basicInfo.property_type === 'apartment' ? '公寓' : '住宅' }}</span>
                  <span class="tag">热门</span>
                  <span class="tag">浏览{{ property.viewCount }}次</span>
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

  <!-- 房源详情弹窗 -->
  <PropertyDetailModal
    v-model:visible="showPropertyModal"
    :property="selectedProperty"
    @favorite="handleFavorite"
    @purchase="handlePurchase"
  />
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { queryAPI } from '@/api/query.api'
import PropertyDetailModal from '@/components/Common/PropertyDetailModal.vue'
import type { PropertyDetail, PropertyCard } from '@/types/api.types'

const route = useRoute()

// 搜索状态
const searchQuery = ref('')
const showSuggestions = ref(false)
const loading = ref(false)
const activeTab = ref('recommend')
const activeFilter = ref('all')
const fromHistory = ref(false)

// 搜索历史项类型定义
interface SearchHistoryItem {
  id: number
  keyword: string
  time: string
  count: number
  lastSearch: string
}

// 搜索历史 - 优化结构，添加频率统计
const searchHistory = ref<Array<SearchHistoryItem>>([])

// 热门搜索词
const hotSearchWords = ref<string[]>([
  '万科城市花园',
  '华润城',
  '南山区',
  '福田区',
  '三房',
  '地铁房',
  '学区房'
])

// 房源详情弹窗
const showPropertyModal = ref(false)
const selectedProperty = ref<PropertyDetail | null>(null)

// 快速筛选
const quickFilters = ref([
  { value: 'all', label: '全部' },
  { value: 'nearby', label: '附近' },
  { value: 'cheap', label: '低价' },
  { value: 'new', label: '最新' },
  { value: 'hot', label: '热门' },
])

// 搜索结果 - 使用真实的API数据结构
const searchResults = ref<PropertyDetail[]>([])
const discoverProperties = ref<PropertyCard[]>([])
const hotProperties = ref<PropertyDetail[]>([])

// 辅助函数：根据筛选条件生成API参数
const getFilterParams = (filter: string): Partial<{
  minArea: number
  maxArea: number
  maxPrice: number
  minViewCount: number
}> => {
  const params: Partial<{
    minArea: number
    maxArea: number
    maxPrice: number
    minViewCount: number
  }> = {}

  switch (filter) {
    case 'nearby':
      // 附近房源 - 可以根据用户位置设置距离参数
      params.minArea = 50
      params.maxArea = 150
      break
    case 'cheap':
      // 低价房源
      params.maxPrice = 500
      break
    case 'new':
      // 最新房源 - 按更新时间排序（后端需要支持）
      break
    case 'hot':
      // 热门房源 - 按浏览次数排序（后端需要支持）
      params.minViewCount = 10
      break
  }

  return params
}

// 模拟猜你喜欢数据（PropertyCard格式）
const getMockDiscoverProperties = (): PropertyCard[] => {
  return [
    {
      propertyId: 1,
      title: '精装修两居室 南向采光好',
      summary: '阳光小区 · 85㎡ · 2室1厅1卫',
      totalPrice: 450,
      cover: 'https://picsum.photos/seed/1/300/200',
      detailUrl: 'https://example.com/property/1',
      tags: ['近地铁', '精装修']
    },
    {
      propertyId: 2,
      title: '豪华三居室 电梯房 学区房',
      summary: '花园社区 · 120㎡ · 3室2厅2卫',
      totalPrice: 680,
      cover: 'https://picsum.photos/seed/2/300/200',
      detailUrl: 'https://example.com/property/2',
      tags: ['学区房', '电梯房']
    }
  ]
}

// 模拟搜索结果（API调用失败时的fallback）
const getMockSearchResults = (): PropertyDetail[] => {
  return [
    {
      propertyId: 1,
      title: '精装修两居室 南向采光好',
      status: 'for_sale',
      communityName: '阳光小区',
      viewCount: 35,
      favoriteCount: 8,
      updatedAt: '2025-01-15T10:30:00Z',
      priceInfo: {
        total_price: 450,
        unit_price: 52941
      },
      layoutInfo: {
        bedroom_count: 2,
        living_room_count: 1,
        bathroom_count: 1,
        area: 85
      },
      basicInfo: {
        property_type: 'apartment',
        build_year: 2018
      },
      locationInfo: {
        province: '广东省',
        city: '深圳市',
        district: '南山区'
      }
    },
    {
      propertyId: 2,
      title: '豪华三居室 电梯房 学区房',
      status: 'for_sale',
      communityName: '花园社区',
      viewCount: 42,
      favoriteCount: 12,
      updatedAt: '2025-01-14T15:20:00Z',
      priceInfo: {
        total_price: 680,
        unit_price: 56667
      },
      layoutInfo: {
        bedroom_count: 3,
        living_room_count: 2,
        bathroom_count: 2,
        area: 120
      },
      basicInfo: {
        property_type: 'apartment',
        build_year: 2020
      },
      locationInfo: {
        province: '广东省',
        city: '深圳市',
        district: '福田区'
      }
    }
  ]
}

// 监听路由参数变化
watch(() => route.query, (newQuery) => {
  if (newQuery.keyword) {
    // 设置搜索框内容
    searchQuery.value = newQuery.keyword as string

    // 标记来自历史记录
    if (newQuery.fromHistory === 'true') {
      fromHistory.value = true
    }

    // 自动执行搜索
    setTimeout(() => {
      performSearch()
    }, 100)
  }
}, { immediate: true })

// 方法

const onSearchBlur = () => {
  // 延迟隐藏，给点击历史记录的时间
  setTimeout(() => {
    showSuggestions.value = false
  }, 200)
}

const selectHotSearch = (word: string) => {
  searchQuery.value = word
  showSuggestions.value = false
  fromHistory.value = false
  performSearch()
}

// 修改 performSearch 方法中的搜索逻辑
const performSearch = async () => {
  loading.value = true
  showSuggestions.value = false

  // 如果不是来自历史记录的跳转，添加到搜索历史
  if (searchQuery.value && !fromHistory.value) {
    addToSearchHistory(searchQuery.value)
  }

  try {
    // 调用真实的房源查询API
    const params = {
      keyword: searchQuery.value.trim() || undefined,
      // 可以根据activeFilter添加更多筛选条件
      ...(activeFilter.value !== 'all' && getFilterParams(activeFilter.value))
    }

    const response = await queryAPI.searchProperties(params)
    searchResults.value = response.items

    console.log('搜索结果:', response)
  } catch (error) {
    console.error('搜索失败:', error)
    // 如果API调用失败，使用模拟数据作为fallback
    searchResults.value = getMockSearchResults()
  } finally {
    loading.value = false

    // 重置历史记录标记
    fromHistory.value = false

    // 确保显示搜索结果标签页
    activeTab.value = 'search'
  }
}

const setActiveFilter = (filter: string) => {
  activeFilter.value = filter
  performSearch()
}

const refreshDiscover = async () => {
  try {
    // 调用猜你喜欢API
    const response = await queryAPI.getGuessYouLike()
    // 随机选择2个房源
    discoverProperties.value = response.items.sort(() => Math.random() - 0.5).slice(0, 2)
  } catch (error) {
    console.error('获取猜你喜欢失败:', error)
    // fallback到模拟数据
    discoverProperties.value = getMockDiscoverProperties()
  }
}

const resetSearch = () => {
  searchQuery.value = ''
  searchResults.value = []
  fromHistory.value = false
}

const viewProperty = (propertyId: number) => {
  // 查找房源详情（从搜索结果或热门推荐中查找）
  const property = searchResults.value.find(p => p.propertyId === propertyId) ||
                   hotProperties.value.find(p => p.propertyId === propertyId)

  if (property) {
    selectedProperty.value = property
    showPropertyModal.value = true
  } else {
    alert('房源信息未找到')
  }
}

// 处理猜你喜欢卡片点击（PropertyCard -> PropertyDetail）
const viewDiscoverProperty = (card: PropertyCard) => {
  if (!card) return

  // 从 summary 解析社区名、面积与卧室数量（容错处理）
  // 示例："阳光小区 · 85㎡ · 2室1厅1卫"
  const parts = (card.summary || '').split('·').map(s => s.trim())
  const community = parts[0] || ''
  const areaMatch = (card.summary || '').match(/(\d+\.?\d*)㎡/)
  const bedroomMatch = (card.summary || '').match(/(\d+)室/)

  const area = areaMatch ? Number(areaMatch[1]) : undefined
  const bedroomCount = bedroomMatch ? Number(bedroomMatch[1]) : undefined

  const mapped: PropertyDetail = {
    propertyId: card.propertyId,
    title: card.title,
    status: 'for_sale',
    communityName: community,
    viewCount: 0,
    favoriteCount: 0,
    updatedAt: new Date().toISOString(),
    priceInfo: {
      total_price: card.totalPrice,
      unit_price: area && card.totalPrice ? Math.round((card.totalPrice * 10000) / area) : 0
    },
    layoutInfo: {
      bedroom_count: bedroomCount || 0,
      living_room_count: 0,
      bathroom_count: 0,
      area: area || 0
    },
    basicInfo: {
      property_type: 'apartment',
      build_year: undefined as unknown as number
    },
    locationInfo: {
      province: '',
      city: '',
      district: ''
    }
  }

  selectedProperty.value = mapped
  showPropertyModal.value = true
}

// 处理收藏
const handleFavorite = async (propertyId: number) => {
  try {
    const userId = 1 // 暂时使用固定用户ID，后续从store获取
    await queryAPI.addFavorite(userId, propertyId)
    alert('收藏成功！')
  } catch (error) {
    console.error('收藏失败:', error)
    alert('收藏失败，请重试')
  }
}

// 处理购买
const handlePurchase = async (property: PropertyDetail) => {
  // 这里可以实现购买逻辑，比如跳转到购买页面或调用购买API
  alert(`正在处理购买房源: ${property.title}\n价格: ¥${property.priceInfo?.total_price}万`)

  // 示例：可以跳转到购买确认页面
  // router.push(`/purchase/${property.propertyId}`)
}

// 搜索历史相关方法
// 修改 loadSearchHistory 方法
const loadSearchHistory = () => {
  const savedHistory = localStorage.getItem('searchHistory')
  if (savedHistory) {
    try {
      const parsed: Array<Partial<SearchHistoryItem>> = JSON.parse(savedHistory)
      // 过滤确保数据结构正确，并迁移旧数据
      searchHistory.value = parsed
        .filter((item): item is Partial<SearchHistoryItem> & { keyword: string } =>
          item && typeof item === 'object' && 'keyword' in item && typeof item.keyword === 'string')
        .map(item => ({
          id: item.id || Date.now(),
          keyword: item.keyword,
          time: item.time || new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          count: item.count || 1,
          lastSearch: item.lastSearch || item.time || new Date().toISOString()
        }))
        .sort((a, b) => {
          // 优先按搜索频率排序，相同频率按时间倒序
          if (a.count !== b.count) {
            return b.count - a.count
          }
          return new Date(b.lastSearch).getTime() - new Date(a.lastSearch).getTime()
        })
    } catch (error) {
      console.error('加载搜索历史失败:', error)
      searchHistory.value = []
    }
  }
}

const addToSearchHistory = (keyword: string) => {
  if (!keyword || keyword.trim() === '') {
    return
  }

  const trimmedKeyword = keyword.trim()
  const now = new Date().toISOString()

  // 创建新数组操作
  const newHistory = [...searchHistory.value]
  const existingIndex = newHistory.findIndex(item => item.keyword === trimmedKeyword)

  if (existingIndex !== -1) {
    // 如果已存在，增加计数并更新时间
    newHistory[existingIndex]!.count += 1
    newHistory[existingIndex]!.lastSearch = now
    newHistory[existingIndex]!.time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  } else {
    // 添加新的搜索历史
    const newItem = {
      id: Date.now(),
      keyword: trimmedKeyword,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      count: 1,
      lastSearch: now
    }
    newHistory.unshift(newItem)
  }

  // 限制历史记录数量
  if (newHistory.length > 15) {
    newHistory.pop()
  }

  // 重新排序：优先按搜索频率，相同频率按时间倒序
  newHistory.sort((a, b) => {
    if (a.count !== b.count) {
      return b.count - a.count
    }
    return new Date(b.lastSearch).getTime() - new Date(a.lastSearch).getTime()
  })

  // 更新数组
  searchHistory.value = newHistory

  // 保存到localStorage
  localStorage.setItem('searchHistory', JSON.stringify(newHistory))
}

const searchFromHistory = (item: SearchHistoryItem) => {
  if (!item) return

  searchQuery.value = item.keyword
  showSuggestions.value = false
  fromHistory.value = true

  // 增加搜索计数
  item.count += 1
  item.lastSearch = new Date().toISOString()
  item.time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })

  // 重新排序
  searchHistory.value.sort((a, b) => {
    if (a.count !== b.count) {
      return b.count - a.count
    }
    return new Date(b.lastSearch).getTime() - new Date(a.lastSearch).getTime()
  })

  // 保存更新
  localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))

  console.log('从历史记录搜索:', item.keyword)

  performSearch()
}

const deleteHistory = (item: SearchHistoryItem) => {
  // 安全处理
  if (!item) return

  searchHistory.value = searchHistory.value.filter(historyItem => historyItem.id !== item.id)
  localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
}

const clearAllHistory = () => {
  searchHistory.value = []
  localStorage.removeItem('searchHistory')
}

// 加载热门推荐
const loadHotProperties = async () => {
  try {
    // 查询热门房源（浏览次数多的）
    const response = await queryAPI.searchProperties({
      minViewCount: 10,
      status: 'for_sale'
    })
    hotProperties.value = response.items.slice(0, 4) // 取前4个
  } catch (error) {
    console.error('获取热门推荐失败:', error)
    // fallback到模拟数据
    hotProperties.value = getMockSearchResults()
  }
}

// 获取房源图片
const getPropertyImage = (property: PropertyDetail) => {
  // 使用picsum.photos根据propertyId生成稳定图片
  return `https://picsum.photos/seed/${property.propertyId}/300/200`
}

// 初始化
onMounted(() => {
  // 加载搜索历史
  loadSearchHistory()

  // 初始加载热门推荐（浏览次数多的房源）
  loadHotProperties()

  // 初始加载猜你喜欢
  refreshDiscover()
})
</script>

<style scoped>
.search-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 60px;
}
.search-keyword {
  margin-left: 10px;
  color: #007bff;
  font-size: 12px;
  font-style: italic;
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
  max-height: 300px;
  overflow-y: auto;
}

.history-section,
.hot-search-section {
  border-bottom: 1px solid #f0f0f0;
}

.history-section:last-child,
.hot-search-section:last-child {
  border-bottom: none;
}

.section-title {
  padding: 8px 16px;
  font-size: 12px;
  color: #999;
  text-transform: uppercase;
  background: #f8f9fa;
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

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.history-keyword {
  font-size: 14px;
  color: #333;
}

.history-meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #999;
}

.search-count {
  color: #007bff;
  font-weight: 500;
}

.search-time {
  color: #666;
}

.delete-history {
  color: #ccc;
  cursor: pointer;
  padding: 4px 8px;
  font-size: 18px;
  transition: color 0.2s;
}

.delete-history:hover {
  color: #ff4757;
}

.clear-history {
  text-align: center;
  padding: 12px 16px;
  color: #007bff;
  cursor: pointer;
  border-top: 1px solid #f0f0f0;
  font-size: 14px;
}

.clear-history:hover {
  background: #f8f9fa;
}

/* 热门搜索样式 */
.hot-search-section {
  padding: 8px 16px;
}

.hot-search-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.hot-search-tag {
  display: inline-block;
  padding: 4px 8px;
  background: linear-gradient(135deg, #ff6b6b, #ffa500);
  color: white;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.hot-search-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(255, 107, 107, 0.3);
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
