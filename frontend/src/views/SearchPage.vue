<template>
  <div class="search-page">
    <!-- 豪华背景装饰 -->
    <div class="luxury-background">
      <div class="floating-orb orb-1"></div>
      <div class="floating-orb orb-2"></div>
      <div class="floating-orb orb-3"></div>
    </div>

    <!-- 顶部导航 -->
    <header class="header">
      <div class="logo" @click="$router.push('/')">
        <span class="logo-icon">🏛️</span>
        <span class="logo-text">尊贵房产</span>
      </div>
      <nav class="top-nav">
        <button class="nav-link active" @click="$router.push('/search')">
          <span class="nav-icon">🔍</span>
          <span>搜索</span>
        </button>
        <button class="nav-link" @click="$router.push('/tools')">
          <span class="nav-icon">📊</span>
          <span>工具</span>
        </button>
        <button class="nav-link" @click="$router.push('/profile')">
          <span class="nav-icon">👤</span>
          <span>我的</span>
        </button>
      </nav>
      <div class="header-actions">
        <button class="icon-btn" @click="$router.push('/login')">
          <span>🔑</span>
        </button>
      </div>
    </header>

    <!-- 搜索栏 -->
    <div class="search-section">
      <div class="search-box">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="输入小区名称..."
          @focus="showSuggestions = true"
          @blur="onSearchBlur"
          @keydown.enter="performSearch"
        />
        <button @click="performSearch" class="search-btn">搜索</button>
      </div>
      
      <!-- 价格区间筛选 -->
      <div class="price-filter">
        <div class="price-input-group">
          <label class="price-label">最低价（万元）</label>
          <input
            v-model.number="minPrice"
            type="number"
            placeholder="最低价"
            class="price-input"
            min="0"
            step="0.1"
          />
        </div>
        <div class="price-separator">-</div>
        <div class="price-input-group">
          <label class="price-label">最高价（万元）</label>
          <input
            v-model.number="maxPrice"
            type="number"
            placeholder="最高价"
            class="price-input"
            min="0"
            step="0.1"
          />
        </div>
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

        <!-- 其他用户也在看 -->
        <div v-if="othersAlsoViewed.length > 0" class="others-section">
          <div class="section-header">
            <h3>🔥 其他用户也在看</h3>
            <span class="data-source-badge">{{ dataSourceText }}</span>
          </div>
          <div class="property-list">
            <div
              v-for="property in othersAlsoViewed"
              :key="property.propertyId"
              class="property-card recommendation-card"
              @click="viewRecommendationProperty(property)"
            >
              <img :src="property.cover" :alt="property.title" class="property-image" />
              <div class="property-info">
                <h3 class="property-title">{{ property.title }}</h3>
                <p class="property-location">{{ property.summary }}</p>
                <div class="property-price" v-if="property.totalPrice">
                  <span class="price">¥{{ property.totalPrice }}</span>
                  <span class="unit">万</span>
                </div>
                <div class="property-tags">
                  <span v-for="tag in property.tags" :key="tag" class="tag">{{ tag }}</span>
                  <span class="similarity-score">相似度 {{ property.score && !isNaN(property.score) ? Math.round(property.score * 100) : '计算中' }}%</span>
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
                <p class="property-location">{{ property.community_name }}</p>
                <div class="property-meta">
                  <span>{{ property.layoutInfo.bedroom_count }}室{{ property.layoutInfo.living_room_count }}厅</span>
                  <span>{{ property.layoutInfo.area }}㎡</span>
                </div>
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
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { queryAPI } from '@/api/query.api'
import { recommendationAPI } from '@/api/recommendation.api'
import PropertyDetailModal from '@/components/Common/PropertyDetailModal.vue'
import type { PropertyDetail, PropertyCard, RecommendationItem, PopularProperty } from '@/types/api.types'
import { useAuthStore } from '@/stores/auth.store'

const route = useRoute()
const authStore = useAuthStore()

// 获取当前用户ID
const currentUserId = computed(() => {
  if (authStore.userId) {
    return authStore.userId
  }
  // 从 localStorage 获取
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    try {
      return JSON.parse(userInfo).userId
    } catch {
      return null
    }
  }
  return null
})

// 搜索状态
const searchQuery = ref('')
const minPrice = ref<number | null>(null)
const maxPrice = ref<number | null>(null)
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
const selectedProperty = ref<PropertyDetail | PopularProperty | null>(null)

// 其他用户也在看
const othersAlsoViewed = ref<RecommendationItem[]>([])
const dataSource = ref<string>('database')

// 数据源文本
const dataSourceText = computed(() => {
  return dataSource.value === 'hadoop_cache' ? '基于 Hadoop 智能推荐' : '基于数据库推荐'
})

// 快速筛选
const quickFilters = ref([
  { value: 'all', label: '全部' },
  { value: 'cheap', label: '低价' },
  { value: 'new', label: '最新' },
])

// 搜索结果 - 使用真实的API数据结构
const searchResults = ref<PropertyDetail[]>([])
const discoverProperties = ref<PropertyCard[]>([])
const hotProperties = ref<PopularProperty[]>([])

// 辅助函数：根据筛选条件生成API参数
const getFilterParams = (filter: string): Partial<{
  minArea: number
  maxArea: number
  maxPrice: number
  minViewCount: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}> => {
  const params: Partial<{
    minArea: number
    maxArea: number
    maxPrice: number
    minViewCount: number
    sortBy?: string
    sortOrder?: 'asc' | 'desc'
  }> = {}

  switch (filter) {
    case 'cheap':
      // 低价房源
      params.maxPrice = 500
      break
    case 'new':
      // 最新房源 - 后端默认就是按更新时间倒序，这里可以明确指定
      params.sortBy = 'updated_at'
      params.sortOrder = 'desc'
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
    },
    {
      propertyId: 3,
      title: '复式四居室 带花园 拎包入住',
      summary: '湖景花园 · 180㎡ · 4室2厅3卫',
      totalPrice: 1200,
      cover: 'https://picsum.photos/seed/3/300/200',
      detailUrl: 'https://example.com/property/3',
      tags: ['花园洋房', '拎包入住']
    },
    {
      propertyId: 4,
      title: '高档公寓 景观阳台 智能家居',
      summary: 'CBD中心 · 95㎡ · 2室1厅2卫',
      totalPrice: 580,
      cover: 'https://picsum.photos/seed/4/300/200',
      detailUrl: 'https://example.com/property/4',
      tags: ['景观房', '智能家居']
    },
    {
      propertyId: 5,
      title: '温馨一居室 地铁口 投资首选',
      summary: '地铁小区 · 55㎡ · 1室1厅1卫',
      totalPrice: 280,
      cover: 'https://picsum.photos/seed/5/300/200',
      detailUrl: 'https://example.com/property/5',
      tags: ['地铁房', '投资']
    },
    {
      propertyId: 6,
      title: '宽敞五居室 豪华装修 车位赠送',
      summary: '豪宅区 · 220㎡ · 5室3厅4卫',
      totalPrice: 2500,
      cover: 'https://picsum.photos/seed/6/300/200',
      detailUrl: 'https://example.com/property/6',
      tags: ['豪华装修', '赠车位']
    },
    {
      propertyId: 7,
      title: '创意LOFT 工业风格 艺术氛围',
      summary: '艺术区 · 110㎡ · 1室1厅2卫',
      totalPrice: 420,
      cover: 'https://picsum.photos/seed/7/300/200',
      detailUrl: 'https://example.com/property/7',
      tags: ['LOFT', '艺术设计']
    },
    {
      propertyId: 8,
      title: '联排别墅 私家花园 泳池',
      summary: '别墅区 · 350㎡ · 6室4厅5卫',
      totalPrice: 3800,
      cover: 'https://picsum.photos/seed/8/300/200',
      detailUrl: 'https://example.com/property/8',
      tags: ['独栋别墅', '泳池']
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

// 模拟热门推荐数据（PopularProperty格式）
const getMockPopularProperties = (): PopularProperty[] => {
  return [
    {
      propertyId: 1,
      title: "万科城市花园 精装三房 南向采光好",
      summary: "万科城市花园 · 89㎡ · 3室2厅",
      totalPrice: 650.5,
      viewCount: 156,
      favoriteCount: 23,
      priceInfo: {
        unit_price: 85000,
        total_price: 650.5,
        price_history: []
      },
      layoutInfo: {
        area: 89.5,
        floor: 15,
        orientation: "south",
        total_floors: 28,
        bedroom_count: 3,
        bathroom_count: 2,
        living_room_count: 2
      },
      basicInfo: {
        build_year: 2018,
        decoration: "hard",
        property_type: "apartment"
      },
      locationInfo: {
        city: "深圳市",
        district: "南山区",
        address: "科技园路123号",
        province: "广东省"
      },
      community_name: "万科城市花园",
      cover: "https://picsum.photos/seed/1/300/200",
      detailUrl: "/property/1",
      tags: ["热门房源", "超热门", "多人收藏"]
    },
    {
      propertyId: 2,
      title: "深业上城 复式公寓",
      summary: "深业上城 · 120㎡ · 4室2厅",
      totalPrice: 880.0,
      viewCount: 89,
      favoriteCount: 15,
      priceInfo: {
        unit_price: 95000,
        total_price: 880.0,
        price_history: []
      },
      layoutInfo: {
        area: 120.0,
        floor: 8,
        orientation: "north",
        total_floors: 32,
        bedroom_count: 4,
        bathroom_count: 2,
        living_room_count: 2
      },
      basicInfo: {
        build_year: 2020,
        decoration: "fine",
        property_type: "apartment"
      },
      locationInfo: {
        city: "深圳市",
        district: "南山区",
        address: "高新南四道18号",
        province: "广东省"
      },
      community_name: "深业上城",
      cover: "https://picsum.photos/seed/2/300/200",
      detailUrl: "/property/2",
      tags: ["热门房源", "多人收藏"]
    },
    {
      propertyId: 3,
      title: "华润城润府 精装四房",
      summary: "华润城润府 · 140㎡ · 4室2厅",
      totalPrice: 1200.0,
      viewCount: 67,
      favoriteCount: 8,
      priceInfo: {
        unit_price: 110000,
        total_price: 1200.0,
        price_history: []
      },
      layoutInfo: {
        area: 140.0,
        floor: 12,
        orientation: "east",
        total_floors: 45,
        bedroom_count: 4,
        bathroom_count: 2,
        living_room_count: 2
      },
      basicInfo: {
        build_year: 2019,
        decoration: "hard",
        property_type: "apartment"
      },
      locationInfo: {
        city: "深圳市",
        district: "南山区",
        address: "润府路1号",
        province: "广东省"
      },
      community_name: "华润城润府",
      cover: "https://picsum.photos/seed/3/300/200",
      detailUrl: "/property/3",
      tags: ["热门房源"]
    },
    {
      propertyId: 4,
      title: "招商雍景湾 景观大宅",
      summary: "招商雍景湾 · 180㎡ · 5室3厅",
      totalPrice: 2200.0,
      viewCount: 45,
      favoriteCount: 12,
      priceInfo: {
        unit_price: 130000,
        total_price: 2200.0,
        price_history: []
      },
      layoutInfo: {
        area: 180.0,
        floor: 25,
        orientation: "south",
        total_floors: 38,
        bedroom_count: 5,
        bathroom_count: 3,
        living_room_count: 2
      },
      basicInfo: {
        build_year: 2021,
        decoration: "fine",
        property_type: "villa"
      },
      locationInfo: {
        city: "深圳市",
        district: "南山区",
        address: "雍景湾路88号",
        province: "广东省"
      },
      community_name: "招商雍景湾",
      cover: "https://picsum.photos/seed/4/300/200",
      detailUrl: "/property/4",
      tags: ["热门房源", "多人收藏"]
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
    const params: any = {
      keyword: searchQuery.value.trim() || undefined,
      // 价格区间筛选
      minPrice: minPrice.value || undefined,
      maxPrice: maxPrice.value || undefined,
      // 可以根据activeFilter添加更多筛选条件
      ...(activeFilter.value !== 'all' && getFilterParams(activeFilter.value))
    }

    console.log('发送搜索请求，参数:', params)
    const response = await queryAPI.searchProperties(params)
    console.log('搜索API响应:', response)
    
    if (response && response.items) {
    searchResults.value = response.items
      console.log('搜索结果数量:', response.items.length)
      if (response.items.length === 0) {
        console.warn('搜索返回空结果，关键词:', searchQuery.value)
      }
    } else {
      console.warn('搜索响应格式异常:', response)
      searchResults.value = []
    }
  } catch (error) {
    console.error('搜索失败:', error)
    // 不再使用模拟数据，直接显示空结果，避免误导用户
    searchResults.value = []
    predictionError.value = '搜索失败，请稍后重试'
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
    // 随机选择8个房源（两排显示）
    discoverProperties.value = response.items.sort(() => Math.random() - 0.5).slice(0, 8)
  } catch (error) {
    console.error('获取猜你喜欢失败:', error)
    // fallback到模拟数据
    discoverProperties.value = getMockDiscoverProperties()
  }

  // 同时加载"其他用户也在看"
  loadOthersAlsoViewed()
}

// 加载"其他用户也在看"
const loadOthersAlsoViewed = async () => {
  if (!currentUserId.value) {
    console.log('用户未登录，跳过加载"其他用户也在看"')
    return
  }

  try {
    const response = await recommendationAPI.getOthersAlsoViewed({
      userId: currentUserId.value,
      limit: 6,
      excludeViewed: true,
      useCache: true
    })

    // 字段映射：将recommendationScore映射为score，以便前端显示
    othersAlsoViewed.value = response.items.map(item => ({
      ...item,
      score: item.recommendationScore || item.score || 0
    }))
    dataSource.value = response.dataSource
    console.log('其他用户也在看数据加载成功:', response)
  } catch (error) {
    console.error('加载"其他用户也在看"失败:', error)
    // 失败时清空数据
    othersAlsoViewed.value = []
  }
}

const resetSearch = () => {
  searchQuery.value = ''
  searchResults.value = []
  fromHistory.value = false
}

const viewProperty = async (propertyId: number) => {
  try {
    // 获取当前用户ID（如果已登录）
    const userId = currentUserId.value || undefined
    
    // 调用新的房源详情接口（会自动增加浏览次数、检查收藏状态）
    const response = await queryAPI.getPropertyDetail(propertyId, userId)
    
    if (response.success && response.property) {
      // 将isFavorited添加到property对象中，供PropertyDetailModal使用
      const propertyWithFavorite = {
        ...response.property,
        isFavorited: response.isFavorited
      }
      
      selectedProperty.value = propertyWithFavorite as PropertyDetail
      showPropertyModal.value = true
      
      console.log('房源详情加载成功，收藏状态:', response.isFavorited)
      console.log('浏览次数已更新为:', response.property.viewCount)
      console.log('收藏次数:', response.property.favoriteCount)
    } else {
      alert('房源信息未找到')
    }
  } catch (error) {
    console.error('获取房源详情失败:', error)
    // 降级处理：如果新接口失败，尝试使用旧的方式
  const property = searchResults.value.find(p => p.propertyId === propertyId) ||
                   hotProperties.value.find(p => p.propertyId === propertyId)
  if (property) {
    selectedProperty.value = property
    showPropertyModal.value = true

      // 尝试记录浏览
      if (currentUserId.value) {
        try {
          await queryAPI.recordBrowse(currentUserId.value, propertyId)
        } catch (err) {
          console.error('记录浏览失败:', err)
        }
    }
  } else {
    alert('房源信息未找到')
    }
  }
}

// 查看推荐的房源（其他用户也在看）
const viewRecommendationProperty = async (item: RecommendationItem) => {
  try {
    // 使用新的房源详情接口获取完整信息
    const userId = currentUserId.value || undefined
    const response = await queryAPI.getPropertyDetail(item.propertyId, userId)
    
    if (response.success && response.property) {
      const propertyWithFavorite = {
        ...response.property,
        isFavorited: response.isFavorited
      }
      
      selectedProperty.value = propertyWithFavorite as PropertyDetail
      showPropertyModal.value = true
    } else {
      // 降级处理：如果接口失败，使用原有逻辑
  const property: PropertyDetail = {
    propertyId: item.propertyId,
    title: item.title,
    status: 'for_sale',
    communityName: '',
    viewCount: 0,
    favoriteCount: 0,
    updatedAt: new Date().toISOString(),
    priceInfo: {
      total_price: item.totalPrice || 0,
      unit_price: 0
    },
    layoutInfo: {
      bedroom_count: 0,
      living_room_count: 0,
      bathroom_count: 0,
      area: 0
    },
    basicInfo: {
      property_type: 'apartment',
      build_year: 0
    },
    locationInfo: {
      province: '',
      city: '',
      district: ''
        },
        isFavorited: false
      } as any

  selectedProperty.value = property
  showPropertyModal.value = true
    }
    } catch (error) {
    console.error('获取推荐房源详情失败:', error)
    alert('获取房源详情失败')
  }
}

// 处理猜你喜欢卡片点击（PropertyCard -> PropertyDetail）
const viewDiscoverProperty = async (card: PropertyCard) => {
  if (!card) return

  try {
    // 使用新的房源详情接口获取完整信息
    const userId = currentUserId.value || undefined
    const response = await queryAPI.getPropertyDetail(card.propertyId, userId)
    
    if (response.success && response.property) {
      const propertyWithFavorite = {
        ...response.property,
        isFavorited: response.isFavorited
      }
      
      selectedProperty.value = propertyWithFavorite as PropertyDetail
      showPropertyModal.value = true
      return
    }
  } catch (error) {
    console.error('获取房源详情失败:', error)
    // 降级处理：使用原有逻辑
  }

  // 降级处理：从 summary 解析社区名、面积与卧室数量（容错处理）
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

  (mapped as any).isFavorited = false
  selectedProperty.value = mapped
  showPropertyModal.value = true
}

// 处理收藏
const handleFavorite = async (propertyId: number) => {
  try {
    if (!currentUserId.value) {
      alert('请先登录')
      return
    }

    // 检查当前是否已收藏
    const currentProperty = selectedProperty.value
    const isCurrentlyFavorited = (currentProperty as any)?.isFavorited || false

    if (isCurrentlyFavorited) {
      // 取消收藏
      console.log('取消收藏请求 - userId:', currentUserId.value, 'propertyId:', propertyId)
      await queryAPI.removeFavorite(currentUserId.value, propertyId)
      alert('已取消收藏！')
    } else {
      // 添加收藏
    console.log('收藏请求 - userId:', currentUserId.value, 'propertyId:', propertyId)
      await queryAPI.addFavorite(currentUserId.value, propertyId)
    alert('收藏成功！')
    }

    // 重新获取房源详情，更新收藏状态和收藏次数
    try {
      const detailResponse = await queryAPI.getPropertyDetail(propertyId, currentUserId.value)
      if (detailResponse.success && detailResponse.property) {
        const updatedProperty = {
          ...detailResponse.property,
          isFavorited: detailResponse.isFavorited
        }
        selectedProperty.value = updatedProperty as PropertyDetail
        console.log('房源详情已更新，收藏次数:', detailResponse.property.favoriteCount)
      }
    } catch (detailError) {
      console.error('更新房源详情失败:', detailError)
      // 如果获取详情失败，至少更新收藏状态
      if (currentProperty) {
        (currentProperty as any).isFavorited = !isCurrentlyFavorited
        // 更新收藏次数（粗略估计）
        if (!isCurrentlyFavorited) {
          currentProperty.favoriteCount = (currentProperty.favoriteCount || 0) + 1
        } else {
          currentProperty.favoriteCount = Math.max(0, (currentProperty.favoriteCount || 1) - 1)
        }
      }
    }
  } catch (error: unknown) {
    console.error('收藏操作失败:', error)
    let errorMsg = '操作失败，请重试'
    if (error && typeof error === 'object') {
      const err = error as { response?: { data?: { message?: string } }; message?: string }
      console.error('错误详情:', err.response?.data)
      errorMsg = err.response?.data?.message || err.message || errorMsg
    }
    alert(errorMsg)
  }
}

// 处理购买
const handlePurchase = async (property: PropertyDetail | PopularProperty) => {
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
    // 调用热门推荐接口
    const response = await queryAPI.getPopularRecommendations()
    hotProperties.value = response.items
  } catch (error) {
    console.error('获取热门推荐失败:', error)
    // fallback到模拟数据
    hotProperties.value = getMockPopularProperties()
  }
}

// 获取房源图片
const getPropertyImage = (property: PropertyDetail | PopularProperty) => {
  // 优先使用接口返回的cover图片，如果没有则使用picsum.photos根据propertyId生成稳定图片
  const popularProperty = property as PopularProperty
  return popularProperty.cover || `https://picsum.photos/seed/${property.propertyId}/300/200`
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
  background: linear-gradient(135deg, #0f1419 0%, #1a1f2e 50%, #0f1419 100%);
  position: relative;
  overflow-x: hidden;
}

/* 豪华背景装饰 */
.luxury-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.floating-orb {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(212, 175, 55, 0.15), transparent 70%);
  animation: floatOrb 25s ease-in-out infinite;
  filter: blur(40px);
}

.orb-1 {
  width: 500px;
  height: 500px;
  top: -200px;
  right: -150px;
  animation-delay: 0s;
}

.orb-2 {
  width: 400px;
  height: 400px;
  bottom: -100px;
  left: -150px;
  animation-delay: 8s;
}

.orb-3 {
  width: 350px;
  height: 350px;
  top: 40%;
  right: 20%;
  animation-delay: 15s;
}

@keyframes floatOrb {
  0%, 100% {
    transform: translate(0, 0) scale(1);
    opacity: 0.3;
  }
  33% {
    transform: translate(50px, -50px) scale(1.1);
    opacity: 0.5;
  }
  66% {
    transform: translate(-30px, 40px) scale(0.95);
    opacity: 0.4;
  }
}

.search-keyword {
  margin-left: 10px;
  color: #667eea;
  font-size: 12px;
  font-style: italic;
  font-weight: 500;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 40px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(22, 33, 62, 0.98) 100%);
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(212, 175, 55, 0.2),
    inset 0 1px 0 rgba(212, 175, 55, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(20px);
  gap: 40px;
  border-bottom: 1px solid rgba(212, 175, 55, 0.2);
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.logo-icon {
  font-size: 32px;
  filter: drop-shadow(0 2px 8px rgba(212, 175, 55, 0.5));
  animation: iconFloat 3s ease-in-out infinite;
}

@keyframes iconFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

.logo-text {
  font-size: 24px;
  font-weight: 800;
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 50%, #ffd700 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 1px;
  text-shadow: 0 2px 10px rgba(212, 175, 55, 0.3);
}

.logo:hover {
  transform: scale(1.05);
}

.logo:hover .logo-icon {
  transform: translateY(-5px) rotate(5deg);
}

.top-nav {
  display: flex;
  gap: 8px;
  flex: 1;
  max-width: 400px;
}

.nav-link {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 24px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  color: rgba(212, 175, 55, 0.6);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
  position: relative;
  overflow: hidden;
}

.nav-link::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.1), transparent);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.nav-link:hover::before {
  opacity: 1;
}

.nav-link:hover {
  border-color: rgba(212, 175, 55, 0.3);
  color: #d4af37;
  transform: translateY(-2px);
}

.nav-link.active {
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.15) 0%, rgba(255, 215, 0, 0.15) 100%);
  color: #ffd700;
  border-color: rgba(212, 175, 55, 0.5);
  box-shadow:
    0 4px 15px rgba(212, 175, 55, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.nav-link.active::before {
  opacity: 1;
}

.nav-icon {
  font-size: 18px;
  filter: drop-shadow(0 0 4px currentColor);
}

.header-actions {
  display: flex;
  gap: 15px;
}

.icon-btn {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.8) 0%, rgba(22, 33, 62, 0.8) 100%);
  border: 2px solid rgba(212, 175, 55, 0.3);
  font-size: 20px;
  cursor: pointer;
  padding: 12px;
  border-radius: 12px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.icon-btn:hover {
  transform: translateY(-3px);
  box-shadow:
    0 6px 20px rgba(212, 175, 55, 0.3),
    0 0 0 1px rgba(212, 175, 55, 0.5);
  border-color: #d4af37;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.2) 0%, rgba(255, 215, 0, 0.2) 100%);
}

.search-section {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.8) 0%, rgba(22, 33, 62, 0.9) 100%);
  padding: 32px 40px;
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(212, 175, 55, 0.2);
  position: relative;
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(212, 175, 55, 0.2);
}

.search-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #d4af37, transparent);
  animation: shimmerLine 3s ease-in-out infinite;
}

@keyframes shimmerLine {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 0.8; }
}

.search-box {
  display: flex;
  gap: 12px;
  position: relative;
}

.search-box input {
  flex: 1;
  padding: 18px 24px;
  border: 2px solid rgba(212, 175, 55, 0.3);
  border-radius: 16px;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.05);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  color: #e8e8e8;
  font-weight: 500;
  backdrop-filter: blur(10px);
}

.search-box input:focus {
  outline: none;
  border-color: #d4af37;
  background: rgba(255, 255, 255, 0.08);
  box-shadow:
    0 0 0 4px rgba(212, 175, 55, 0.15),
    0 4px 20px rgba(212, 175, 55, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  transform: translateY(-2px);
}

.search-box input::placeholder {
  color: rgba(212, 175, 55, 0.5);
}

.price-filter {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 20px;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  border: 1px solid rgba(212, 175, 55, 0.2);
  backdrop-filter: blur(10px);
}

.price-input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.price-label {
  font-size: 13px;
  font-weight: 600;
  color: rgba(212, 175, 55, 0.8);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.price-input {
  padding: 12px 16px;
  border: 2px solid rgba(212, 175, 55, 0.3);
  border-radius: 10px;
  font-size: 15px;
  background: rgba(255, 255, 255, 0.05);
  transition: all 0.3s ease;
  color: #e8e8e8;
  font-weight: 500;
  backdrop-filter: blur(10px);
}

.price-input:focus {
  outline: none;
  border-color: #d4af37;
  background: rgba(255, 255, 255, 0.08);
  box-shadow:
    0 0 0 3px rgba(212, 175, 55, 0.15),
    0 2px 8px rgba(212, 175, 55, 0.2);
}

.price-input::placeholder {
  color: rgba(212, 175, 55, 0.4);
}

.price-separator {
  font-size: 20px;
  font-weight: 700;
  color: #d4af37;
  margin-top: 24px;
  text-shadow: 0 2px 4px rgba(212, 175, 55, 0.3);
}

.search-btn {
  padding: 18px 40px;
  background: linear-gradient(135deg, #d4af37 0%, #ffd700 100%);
  color: #1a1a2e;
  border: 2px solid rgba(255, 215, 0, 0.5);
  border-radius: 16px;
  cursor: pointer;
  font-weight: 700;
  font-size: 16px;
  letter-spacing: 1px;
  text-transform: uppercase;
  box-shadow:
    0 6px 20px rgba(212, 175, 55, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.search-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  transition: left 0.6s ease;
}

.search-btn:hover::before {
  left: 100%;
}

.search-btn:hover {
  transform: translateY(-3px);
  box-shadow:
    0 10px 30px rgba(212, 175, 55, 0.5),
    0 0 0 1px rgba(255, 215, 0, 0.8),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  border-color: #ffd700;
}

.search-btn:active {
  transform: translateY(-1px);
}

.suggestions {
  margin-top: 20px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(22, 33, 62, 0.98) 100%);
  border-radius: 16px;
  box-shadow:
    0 10px 40px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(212, 175, 55, 0.2);
  overflow: hidden;
  max-height: 450px;
  overflow-y: auto;
  animation: slideDown 0.4s ease;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(212, 175, 55, 0.2);
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.history-section,
.hot-search-section {
  border-bottom: 1px solid rgba(212, 175, 55, 0.1);
}

.history-section:last-child,
.hot-search-section:last-child {
  border-bottom: none;
}

.section-title {
  padding: 16px 24px;
  font-size: 11px;
  color: rgba(212, 175, 55, 0.8);
  text-transform: uppercase;
  font-weight: 700;
  letter-spacing: 2px;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.05) 0%, rgba(255, 215, 0, 0.05) 100%);
  border-bottom: 1px solid rgba(212, 175, 55, 0.1);
}

.suggestion-item {
  padding: 16px 24px;
  border-bottom: 1px solid rgba(212, 175, 55, 0.05);
  cursor: pointer;
  transition: all 0.3s ease;
}

.suggestion-item:hover {
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.08) 0%, rgba(255, 215, 0, 0.05) 100%);
  transform: translateX(8px);
  border-left: 3px solid #d4af37;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.history-keyword {
  font-size: 15px;
  color: #ffd700;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(212, 175, 55, 0.2);
}

.history-meta {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: rgba(212, 175, 55, 0.6);
}

.search-count {
  color: #ffd700;
  font-weight: 600;
  background: rgba(212, 175, 55, 0.15);
  padding: 2px 8px;
  border-radius: 12px;
  border: 1px solid rgba(212, 175, 55, 0.3);
}

.search-time {
  color: rgba(212, 175, 55, 0.5);
}

.delete-history {
  color: rgba(212, 175, 55, 0.5);
  cursor: pointer;
  padding: 6px 10px;
  font-size: 20px;
  transition: all 0.3s ease;
  border-radius: 8px;
}

.delete-history:hover {
  color: #ff6b6b;
  background: rgba(255, 107, 107, 0.15);
  box-shadow: 0 2px 8px rgba(255, 107, 107, 0.2);
}

.clear-history {
  text-align: center;
  padding: 14px 20px;
  color: #d4af37;
  cursor: pointer;
  border-top: 2px solid rgba(212, 175, 55, 0.2);
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.clear-history:hover {
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.15), rgba(255, 215, 0, 0.1));
  color: #ffd700;
  box-shadow: 0 2px 8px rgba(212, 175, 55, 0.2);
}

/* 热门搜索样式 */
.hot-search-section {
  padding: 12px 20px 16px;
}

.hot-search-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.hot-search-tag {
  display: inline-flex;
  align-items: center;
  padding: 8px 16px;
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 100%);
  color: #0f1419;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(212, 175, 55, 0.4);
  border: 1px solid rgba(255, 215, 0, 0.5);
}

.hot-search-tag:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 6px 16px rgba(212, 175, 55, 0.6);
  border-color: #ffd700;
}

.quick-filters {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.8), rgba(22, 33, 62, 0.9));
  padding: 18px 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
  border-radius: 16px;
  border: 1px solid rgba(212, 175, 55, 0.2);
  backdrop-filter: blur(10px);
}

.filter-tags {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 5px;
}

.filter-tags::-webkit-scrollbar {
  height: 4px;
}

.filter-tags::-webkit-scrollbar-thumb {
  background: #cbd5e0;
  border-radius: 4px;
}

.filter-tag {
  padding: 10px 20px;
  border: 2px solid rgba(212, 175, 55, 0.3);
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(26, 26, 46, 0.6);
  color: rgba(212, 175, 55, 0.8);
  backdrop-filter: blur(10px);
}

.filter-tag:hover {
  border-color: #d4af37;
  background: rgba(212, 175, 55, 0.15);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.3);
}

.filter-tag.active {
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 100%);
  color: #0f1419;
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.5);
  font-weight: 700;
}

.content-area {
  padding: 0 24px;
}

.tab-container {
  display: flex;
  background: white;
  border-radius: 16px;
  margin-top: 20px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  padding: 8px;
  gap: 6px;
}

.tab-btn {
  flex: 1;
  padding: 16px 24px;
  background: transparent;
  border: 1px solid rgba(212, 175, 55, 0.2);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 12px;
  color: rgba(212, 175, 55, 0.7);
}

.tab-btn:hover {
  background: rgba(212, 175, 55, 0.1);
  color: #ffd700;
  border-color: rgba(212, 175, 55, 0.4);
}

.tab-btn.active {
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 100%);
  color: #0f1419;
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.4);
  border-color: #d4af37;
}

.tab-content {
  background: transparent;
  padding: 24px 0;
  min-height: 400px;
}

.loading-state {
  text-align: center;
  padding: 80px 0;
  color: rgba(212, 175, 55, 0.7);
}

.spinner {
  width: 60px;
  height: 60px;
  border: 5px solid rgba(212, 175, 55, 0.2);
  border-top: 5px solid #d4af37;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 28px;
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.2);
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
  padding: 100px 20px;
  color: rgba(212, 175, 55, 0.7);
}

.empty-state h3 {
  color: #ffd700;
  font-size: 22px;
  margin-bottom: 14px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.empty-state p {
  color: rgba(212, 175, 55, 0.7);
  font-size: 14px;
  margin-bottom: 28px;
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 28px;
  opacity: 0.6;
  filter: drop-shadow(0 4px 8px rgba(212, 175, 55, 0.3));
}

.reset-btn {
  margin-top: 24px;
  padding: 16px 40px;
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 100%);
  color: #0f1419;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-weight: 700;
  box-shadow: 0 6px 20px rgba(212, 175, 55, 0.4);
  transition: all 0.3s ease;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.reset-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 28px rgba(212, 175, 55, 0.6);
}

.results-count {
  margin-bottom: 24px;
  color: #d4af37;
  font-size: 15px;
  font-weight: 600;
  padding: 14px 24px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.8), rgba(22, 33, 62, 0.9));
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(212, 175, 55, 0.2);
  letter-spacing: 0.3px;
}

.property-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  justify-items: center;
}

.property-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.85) 0%, rgba(22, 33, 62, 0.9) 100%);
  border-radius: 20px;
  overflow: hidden;
  box-shadow:
    0 8px 24px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(212, 175, 55, 0.2);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(212, 175, 55, 0.2);
  backdrop-filter: blur(10px);
}

.property-card:hover {
  transform: translateY(-8px);
  box-shadow:
    0 16px 40px rgba(0, 0, 0, 0.6),
    0 0 0 1px rgba(212, 175, 55, 0.5),
    0 0 20px rgba(212, 175, 55, 0.3);
  border-color: #d4af37;
}

.property-image {
  width: 100%;
  height: 220px;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.property-card:hover .property-image {
  transform: scale(1.05);
}

.property-info {
  padding: 20px;
}

.property-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 10px;
  color: #ffd700;
  line-height: 1.4;
  letter-spacing: 0.3px;
}

.property-location {
  color: rgba(212, 175, 55, 0.7);
  font-size: 14px;
  margin-bottom: 14px;
  font-weight: 500;
}

.property-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  color: rgba(212, 175, 55, 0.6);
  font-size: 13px;
}

.property-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.property-price {
  margin-bottom: 16px;
  padding: 14px 18px;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.15), rgba(255, 215, 0, 0.1));
  border-radius: 12px;
  display: inline-block;
  border: 1px solid rgba(212, 175, 55, 0.3);
}

.price {
  font-size: 26px;
  font-weight: 800;
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 2px 4px rgba(212, 175, 55, 0.3);
}

.unit {
  color: #d4af37;
  font-size: 14px;
  margin-left: 4px;
  font-weight: 600;
}

.unit-price {
  color: rgba(212, 175, 55, 0.6);
  font-size: 12px;
  margin-left: 8px;
}

.property-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tag {
  padding: 7px 14px;
  background: rgba(212, 175, 55, 0.15);
  color: #d4af37;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid rgba(212, 175, 55, 0.3);
  transition: all 0.3s ease;
}

.tag:hover {
  background: rgba(212, 175, 55, 0.25);
  border-color: rgba(212, 175, 55, 0.5);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 18px 24px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.8), rgba(22, 33, 62, 0.9));
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(212, 175, 55, 0.2);
}

.section-header h3 {
  font-size: 22px;
  font-weight: 700;
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 50%, #ffd700 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.5px;
}

.refresh-btn {
  background: rgba(26, 26, 46, 0.8);
  border: 2px solid rgba(212, 175, 55, 0.3);
  padding: 12px 24px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  color: #d4af37;
  transition: all 0.3s ease;
}

.refresh-btn:hover {
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 100%);
  color: #0f1419;
  border-color: transparent;
  transform: rotate(180deg);
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.4);
}

.discover-hint {
  text-align: center;
  padding: 28px;
  color: rgba(212, 175, 55, 0.8);
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.7), rgba(22, 33, 62, 0.8));
  border-radius: 16px;
  margin-bottom: 24px;
  border: 2px dashed rgba(212, 175, 55, 0.3);
  backdrop-filter: blur(10px);
}

.discover-hint p {
  font-weight: 500;
  font-size: 14px;
  letter-spacing: 0.3px;
}

/* 其他用户也在看 */
.others-section {
  margin-top: 36px;
  padding: 28px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.85), rgba(22, 33, 62, 0.9));
  border-radius: 20px;
  box-shadow:
    0 8px 24px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(212, 175, 55, 0.2);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(212, 175, 55, 0.2);
}

.others-section .section-header {
  margin-bottom: 20px;
  padding: 0;
  background: transparent;
  box-shadow: none;
}

.data-source-badge {
  display: inline-block;
  padding: 7px 16px;
  background: linear-gradient(135deg, #ffd700 0%, #d4af37 100%);
  color: #0f1419;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 8px rgba(212, 175, 55, 0.3);
}

.recommendation-card {
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

.recommendation-card:hover {
  border-color: #667eea;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.25);
  transform: translateY(-4px);
}

.similarity-score {
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.2), rgba(255, 215, 0, 0.15));
  color: #ffd700;
  padding: 7px 14px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 700;
  margin-left: auto;
  border: 1px solid rgba(212, 175, 55, 0.4);
  box-shadow: 0 2px 6px rgba(212, 175, 55, 0.2);
}
</style>

```
