<template>
  <div class="profile-container">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="logo" @click="$router.push('/search')">房产平台</div>
      <nav class="top-nav">
        <button class="nav-link" @click="$router.push('/search')">
          <span class="nav-icon">🔍</span>
          <span>搜索</span>
        </button>
        <button class="nav-link" @click="$router.push('/tools')">
          <span class="nav-icon">📊</span>
          <span>工具</span>
        </button>
        <button class="nav-link active" @click="$router.push('/profile')">
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

    <!-- 用户信息卡片 -->
    <div class="user-card">
      <div class="user-avatar">
        <img :src="userInfo.avatar" alt="头像" class="avatar-img" />
        <div class="avatar-edit" @click="editAvatar">📷</div>
      </div>
      <div class="user-info">
        <h2 class="username">{{ userInfo.username }}</h2>
        <p class="user-desc">{{ userInfo.bio }}</p>
        <div class="user-tags">
          <span class="user-tag">{{ userInfo.phone }}</span>
          <span class="user-tag"> {{ userInfo.location }}</span>
        </div>
      </div>
    </div>

    <!-- 数据统计 -->
    <div class="stats-grid">
    <div class="stat-item" @click="fetchFavoritesData">
        <div class="stat-icon">❤️</div>

        <div class="stat-label">我的收藏</div>
      </div>
      <div class="stat-item" @click="fetchHistoryData">

        <div class="stat-icon">🕒</div>

        <div class="stat-label">浏览记录</div>
      </div>
      <div class="stat-item" @click="showPreferences = true">
        <div class="stat-icon">⭐</div>

        <div class="stat-label">偏好设置</div>
      </div>
    </div>

    <!-- 功能列表 -->
    <div class="function-list">
      <div class="list-section">
        <div class="list-item" @click="showHelp">
          <div class="item-icon">❓</div>
          <div class="item-text">
            <div class="item-title">帮助中心</div>
            <div class="item-desc">使用指南和常见问题</div>
          </div>
          <div class="item-arrow">›</div>
        </div>

        <div class="list-item" @click="showSettings = true">
          <div class="item-icon">⚙️</div>
          <div class="item-text">
            <div class="item-title">作者信息</div>
            <div class="item-desc">通知、主题等设置</div>
          </div>
          <div class="item-arrow">›</div>
        </div>
      </div>

     <div class="list-section">
  <!-- 修改：使用三重检查确保登录状态正确 -->
  <div
    class="list-item"
    @click="handleLogout"
    v-if="isLoggedIn || authStore.isLoggedIn || hasLocalStorageToken()"
  >
    <div class="item-icon logout">🚪</div>
    <div class="item-text">
      <div class="item-title">退出登录</div>
    </div>
  </div>

  <div class="list-item" @click="$router.push('/login')" v-else>
    <div class="item-icon">🔑</div>
    <div class="item-text">
      <div class="item-title">登录/注册</div>
      <div class="item-desc">立即登录体验更多功能</div>
    </div>
    <div class="item-arrow">›</div>
  </div>
</div>
    </div>

    <!-- 偏好设置弹窗 -->
    <div v-if="showPreferences" class="modal-overlay" @click="showPreferences = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">偏好设置</h3>
          <div class="modal-actions">
            <button class="text-btn" @click="resetPreferences">重置</button>
            <button class="close-btn" @click="showPreferences = false">×</button>
          </div>
        </div>

        <div class="modal-body">
          <!-- 价格范围 -->
<div class="pref-section">
  <h4 class="pref-title">预算范围（万元）</h4>
  <div class="price-inputs">
    <div class="input-group">
      <label>最低预算</label>
      <input
        type="number"
        v-model="preferences.budget.min"
        placeholder="0"
        min="0"
        class="price-input"
      />
    </div>
    <div class="separator">-</div>
    <div class="input-group">
      <label>最高预算</label>
      <input
        type="number"
        v-model="preferences.budget.max"
        placeholder="1000"
        min="0"
        class="price-input"
      />
    </div>
  </div>
</div>

          <!-- 房源类型 -->
<div class="pref-section">
  <h4 class="pref-title">房源类型</h4>
  <div class="room-grid">
    <div
      v-for="type in houseTypeOptions"
      :key="type.value"
      class="room-item"
      :class="{ active: preferences.house_types.includes(type.value) }"
      @click="toggleHouseType(type.value)"
    >
      <div class="room-icon">{{ type.icon }}</div>
      <div class="room-name">{{ type.label }}</div>
    </div>
  </div>
</div>

       <!-- 城市选择 -->
<div class="pref-section">
  <h4 class="pref-title">城市</h4>
  <select v-model="preferences.city" class="city-select" @change="updateDistrictOptions">
    <option value="">请选择城市</option>
    <option v-for="city in cityOptions" :key="city.value" :value="city.value">
      {{ city.label }}
    </option>
  </select>
</div>

<!-- 区域偏好（依赖城市选择） -->
<div class="pref-section" v-if="preferences.city">
  <h4 class="pref-title">偏好区域</h4>
  <div class="checkbox-group">
    <label v-for="district in filteredDistrictOptions" :key="district.value" class="checkbox-label">
      <input
        type="checkbox"
        :value="district.value"
        v-model="preferences.districts"
      />
      <span>{{ district.label }}</span>
    </label>
  </div>
</div>

<!-- 朝向偏好 -->
<div class="pref-section">
  <h4 class="pref-title">朝向偏好</h4>
  <div class="checkbox-group">
    <label class="checkbox-label">
      <input type="checkbox" value="south" v-model="preferences.orientations">
      <span>南向</span>
    </label>
    <label class="checkbox-label">
      <input type="checkbox" value="north" v-model="preferences.orientations">
      <span>北向</span>
    </label>
    <label class="checkbox-label">
      <input type="checkbox" value="east" v-model="preferences.orientations">
      <span>东向</span>
    </label>
    <label class="checkbox-label">
      <input type="checkbox" value="west" v-model="preferences.orientations">
      <span>西向</span>
    </label>
  </div>
</div>

<!-- 卧室数量范围 -->
<div class="pref-section">
  <h4 class="pref-title">卧室数量</h4>
  <div class="range-inputs">
    <select v-model="preferences.bedroom_range.min" class="range-select">
      <option value="">不限</option>
      <option value="1">1室</option>
      <option value="2">2室</option>
      <option value="3">3室</option>
      <option value="4">4室</option>
      <option value="5">5室及以上</option>
    </select>
    <span class="range-separator">-</span>
    <select v-model="preferences.bedroom_range.max" class="range-select">
      <option value="">不限</option>
      <option value="1">1室</option>
      <option value="2">2室</option>
      <option value="3">3室</option>
      <option value="4">4室</option>
      <option value="5">5室及以上</option>
    </select>
  </div>
</div>

          <button class="save-btn" @click="savePreferences">保存设置</button>
        </div>
      </div>
    </div>

    <!-- 收藏弹窗 -->
    <div v-if="showFavorites" class="modal-overlay" @click="showFavorites = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">我的收藏</h3>
          <button class="close-btn" @click="showFavorites = false">×</button>
        </div>
        <div class="modal-body">
          <div v-if="favorites.length === 0" class="empty-state">
            <div class="empty-icon">❤️</div>
            <p class="empty-text">暂无收藏房源</p>
            <button class="primary-btn" @click="$router.push('/search')">去浏览房源</button>
          </div>
<div v-else class="favorites-list">
  <!-- 收藏弹窗中的显示 -->
  <div v-for="item in favorites" :key="item.id" class="favorite-item" @click="showPropertyDetailFromFavorite(item)">
    <img :src="item.image" alt="房源" class="favorite-img" />
    <div class="favorite-info">
      <h4 class="favorite-title">{{ item.title }}</h4>
      <p class="favorite-location">{{ item.community }}</p>
      <div class="property-meta" v-if="item.layoutInfo">
        <span v-if="item.layoutInfo.bedroom_count">
          {{ item.layoutInfo.bedroom_count }}室
        </span>
        <span v-if="item.layoutInfo.area">{{ item.layoutInfo.area }}㎡</span>
      </div>
      <div class="favorite-price" v-if="item.price">¥{{ item.price }}万</div>
    </div>
    <button class="remove-btn" @click.stop="removeFavorite(item.id)">×</button>
  </div>
</div>
        </div>
      </div>
    </div>
<!-- 帮助中心弹窗 -->
<div v-if="showHelpModal" class="modal-overlay" @click="showHelpModal = false">
  <div class="modal-content help-modal" @click.stop>
    <div class="modal-header">
      <h3 class="modal-title">帮助中心</h3>
      <button class="close-btn" @click="showHelpModal = false">×</button>
    </div>
    <div class="modal-body help-body">
      <div class="help-section">
        <h4 class="help-section-title">📖 使用指南</h4>
        <ul class="help-list">
          <li><strong>个人资料管理：</strong>还没有实现，你注册是啥名字以后就得是啥名字</li>
          <li><strong>偏好设置：</strong>点击"偏好设置"可设置预算、房源类型、城市等筛选条件</li>
          <li><strong>收藏功能：</strong>在房源详情页点击爱心图标即可收藏房源</li>
          <li><strong>浏览记录：</strong>系统会自动记录您浏览过的房源</li>
        </ul>
      </div>

      <div class="help-section">
        <h4 class="help-section-title">❓ 常见问题</h4>
        <div class="faq-item">
          <div class="faq-question">Q: 如何搜索特定区域的房源？</div>
          <div class="faq-answer">A: 在偏好设置中选择城市和区域，系统会根据您的偏好推荐房源</div>
        </div>
        <div class="faq-item">
          <div class="faq-question">Q: 为什么看不到浏览记录？</div>
          <div class="faq-answer">A: 请确保您已登录账号，浏览记录会同步到您的账户中</div>
        </div>
        <div class="faq-item">
          <div class="faq-question">Q: 如何清空收藏列表？</div>
          <div class="faq-answer">A: 目前需要逐个取消收藏，后续会添加批量删除功能</div>
        </div>
        <div class="faq-item">
          <div class="faq-question">Q: 预算范围如何设置？</div>
          <div class="faq-answer">A: 在偏好设置中输入最低和最高预算（单位：万元），如：100-300</div>
        </div>
      </div>

      <div class="help-section">
        <h4 class="help-section-title">📞 联系我们</h4>
        <div class="contact-info">
          <p><strong>客服热线：</strong>400-123-4567</p>
          <p><strong>服务时间：</strong>周一至周五 9:00-18:00</p>
          <p><strong>邮箱：</strong>support@example.com</p>
          <p><strong>微信公众号：</strong>房产助手</p>
        </div>
      </div>

      <div class="help-footer">
        <p class="version-info">当前版本：v1.0.0</p>
        <p class="copyright">© 2025 房产助手 版权所有</p>
      </div>
    </div>
  </div>
</div>
    <!-- 浏览记录弹窗 -->
    <div v-if="showHistory" class="modal-overlay" @click="showHistory = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">浏览记录</h3>
          <div class="modal-actions">
            <button class="text-btn" @click="clearHistory" v-if="history.length > 0">清空</button>
            <button class="close-btn" @click="showHistory = false">×</button>
          </div>
        </div>
        <div class="modal-body">
          <div v-if="history.length === 0" class="empty-state">
            <div class="empty-icon">🕒</div>
            <p class="empty-text">暂无浏览记录</p>
          </div>
          <div v-else class="history-list">
            <!-- 浏览记录弹窗中的显示 -->
       <div v-for="item in history"
     :key="item.id"
     class="history-item"
     @click="showPropertyDetailFromHistory(item)"
>
  <img :src="item.image" alt="房源" class="history-img" />
  <div class="history-info">
    <h4 class="history-title">{{ item.title }}</h4>
    <p class="history-location">{{ item.community }}</p>
    <div class="history-meta">
      <span class="history-price">¥{{ item.price }}万</span>
      <span class="history-time">{{ item.time }}</span>
    </div>
  </div>
</div>
        </div>
      </div>
    </div>
  </div>

  <!-- 我添加的缺失的结束标签 -->
  </div>

  <!-- 房源详情弹窗 -->
  <div v-if="showPropertyDetail" class="modal-overlay" @click="showPropertyDetail = false">
    <div class="modal-content property-detail-modal" @click.stop>
      <div class="modal-header">
        <h3 class="modal-title">房源详情</h3>
        <button class="close-btn" @click="showPropertyDetail = false">×</button>
      </div>
      <div class="modal-body property-detail-body">
        <div v-if="selectedProperty" class="property-detail-content">
          <div class="property-image-section">
            <img :src="selectedProperty.image" :alt="selectedProperty.title" class="property-detail-image" />
          </div>

          <div class="property-basic-info">
            <h2 class="property-title">{{ selectedProperty.title }}</h2>
            <div class="property-price">¥{{ selectedProperty.price }}万</div>
            <div class="property-community">{{ selectedProperty.community }}</div>
          </div>

          <div class="property-details">
            <div class="detail-row">
              <span class="detail-label">房源ID:</span>
              <span class="detail-value">{{ selectedProperty.id }}</span>
            </div>

            <div class="detail-row" v-if="selectedProperty.time">
              <span class="detail-label">浏览时间:</span>
              <span class="detail-value">{{ selectedProperty.time }}</span>
            </div>

            <div class="actions-section">
              <button
                class="action-button favorite-button"
                :class="{ favorited: isFavorited(selectedProperty.id) }"
                @click="toggleFavorite(selectedProperty)"
              >
                {{ isFavorited(selectedProperty.id) ? '已收藏' : '收藏' }}
              </button>
              <button class="action-button contact-button">联系经纪人</button>
              <button class="action-button buy-button" @click="handlePurchase(selectedProperty)">购买</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>


<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { profileAPI } from '@/api/profile.api'
import { useAuthStore } from '@/stores/auth.store'  // 新增：导入 auth store
import { watch } from 'vue'  // 添加导入
import { queryAPI } from '@/api/query.api'
// 在 <script setup> 顶部添加这个接口定义
interface HistoryItem {
  id: number
  title: string
  community: string
  price: number
  image: string
  time: string
}

// 添加房源详情接口定义
interface PropertyDetail {
  id: number
  propertyId?: number  // 房源ID（用于浏览记录）
  title: string
  community: string
  price: number
  image: string
  time?: string
}

// 新增：检查 localStorage 中是否有 token
const hasLocalStorageToken = (): boolean => {
  try {
    const token = localStorage.getItem('token')
    const userInfo = localStorage.getItem('userInfo')
    return !!(token && userInfo)
  } catch (err) {
    console.error('检查 localStorage 失败:', err)
    return false
  }
}
// 房源类型选项 - 与 LoginPage.vue 的 select 选项对应
const houseTypeOptions = [
  { value: 'apartment', label: '公寓', icon: '🏢' },
  { value: 'villa', label: '别墅', icon: '🏠' },
  { value: 'townhouse', label: '联排别墅', icon: '🏘️' },
  { value: 'loft', label: 'loft', icon: '🏭' }
]
const router = useRouter()
const authStore = useAuthStore()  // 新增：使用 auth store
const showHelpModal = ref(false)



// 使用计算属性获取当前用户ID
const currentUserId = computed(() => {
  // 1. 优先从 auth store 获取
  if (authStore.userId) {
    return authStore.userId
  }

  // 2. 如果 store 中没有，从 localStorage 获取
  try {
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr) {
      const userInfo = JSON.parse(userInfoStr)
      return userInfo.userId || null
    }
  } catch (err) {
    console.error('从 localStorage 解析用户信息失败:', err)
  }

  // 3. 如果都没有，返回 null
  return null
})

// 添加：用户是否已登录
const isUserLoggedIn = computed(() => {
  return authStore.isLoggedIn || currentUserId.value !== null
})

// 添加：获取用户信息函数
const fetchUserInfo = async () => {
  console.log('=== 调试信息 ===')
  console.log('1. authStore.userId:', authStore.userId)
  console.log('2. currentUserId.value:', currentUserId.value)
  console.log('3. localStorage userInfo:', localStorage.getItem('userInfo'))
  console.log('4. authStore.isLoggedIn:', authStore.isLoggedIn)
  console.log('5. authStore.username:', authStore.username)

  if (!currentUserId.value) {
    console.error('用户未登录，无法获取用户信息')
    router.push('/login')
    return
  }
  try {
    console.log('开始调用 getMyProfile API, userId:', currentUserId.value)
    const response = await profileAPI.getMyProfile(currentUserId.value)
    console.log('getMyProfile API 响应:', response)

    // 检查响应结构
    if (response) {
      console.log('响应类型:', typeof response)
      console.log('响应键:', Object.keys(response))

      // 根据你的 api.types.ts，响应应该是 ProfileDetailResponse
      if ('profile' in response) {
        const profile = response.profile
        console.log('6. API响应 username:', profile.username)
        console.log('7. API响应 userId:', profile.userId)
        console.log('8. API响应 stats:', profile.stats)

        // 更新用户信息
        userInfo.username = profile.username
        userInfo.bio = ``

        // 更新统计数据
        favoritesCount.value = profile.stats.favorites
        historyCount.value = profile.stats.browsed
        preferencesCount.value = profile.stats.recommendations

        console.log('用户信息获取成功:', profile)
      } else {
        console.error('响应中没有 profile 字段，响应结构:', response)
      }
    }
  } catch (err) {
    console.error('获取用户信息失败:', err)
  }
}
// 加载用户已有偏好设置
const loadExistingPreferences = async () => {
  if (!currentUserId.value) return

  try {
    console.log('开始加载用户偏好设置，userId:', currentUserId.value)
    const response = await profileAPI.getMyProfile(currentUserId.value)
    console.log('获取用户信息响应:', response)

    if (response && response.profile) {
      const profile = response.profile

      // 重置偏好设置
      Object.assign(preferences, {
        budget: { min: 0, max: 0 },
        city: '',
        districts: [],
        house_types: [],
        orientations: [],
        bedroom_range: { min: '', max: '' }
      })

      // 从 userProfile 字段解析 - 处理类型不一致问题
      if (profile.userProfile) {
        console.log('原始 profile.userProfile:', profile.userProfile)
        console.log('原始 profile.userProfile 类型:', typeof profile.userProfile)

        try {
          let userProfileData: Record<string, unknown> = {}

          // 处理类型不一致：可能是字符串或对象
          if (typeof profile.userProfile === 'string') {
            // 如果是字符串，解析它
            userProfileData = JSON.parse(profile.userProfile)
          } else if (typeof profile.userProfile === 'object' && profile.userProfile !== null) {
            // 如果已经是对象，直接使用（虽然类型定义有问题）
            userProfileData = profile.userProfile as Record<string, unknown>
          } else {
            console.warn('profile.userProfile 不是字符串也不是对象:', profile.userProfile)
            return
          }

          console.log('解析后的 userProfileData:', userProfileData)

          // 处理预算
          if (userProfileData.budget) {
            const budget = userProfileData.budget as { min?: number; max?: number }
            preferences.budget.min = budget.min || 0
            preferences.budget.max = budget.max || 0
          }

          // 处理 price_range（兼容字段）
          if (userProfileData.price_range) {
            const priceRange = userProfileData.price_range as { min?: number; max?: number }
            preferences.budget.min = priceRange.min || preferences.budget.min
            preferences.budget.max = priceRange.max || preferences.budget.max
          }

          // 处理城市和区域
          if (userProfileData.locations && Array.isArray(userProfileData.locations) && userProfileData.locations.length > 0) {
            preferences.city = (userProfileData.locations[0] as string) || ''
          }

          if (userProfileData.preferred_locations && Array.isArray(userProfileData.preferred_locations)) {
            preferences.districts = userProfileData.preferred_locations as string[]
          }

          if (userProfileData.districts && Array.isArray(userProfileData.districts)) {
            preferences.districts = userProfileData.districts as string[]
          }

          if (userProfileData.city && typeof userProfileData.city === 'string') {
            preferences.city = userProfileData.city
          }

          if (userProfileData.house_types && Array.isArray(userProfileData.house_types)) {
            preferences.house_types = userProfileData.house_types as string[]
          }

          if (userProfileData.orientations && Array.isArray(userProfileData.orientations)) {
            preferences.orientations = userProfileData.orientations as string[]
          }

          if (userProfileData.bedroom_range) {
            const bedroomRange = userProfileData.bedroom_range as { min?: string | number; max?: string | number }
            preferences.bedroom_range.min = String(bedroomRange.min || '')
            preferences.bedroom_range.max = String(bedroomRange.max || '')
          }

        } catch (parseErr) {
          console.error('解析 userProfile 失败:', parseErr)
        }
      }

      console.log('最终加载的偏好设置:', preferences)
    }
  } catch (err) {
    console.error('加载偏好设置失败:', err)
  }
}

// 添加：获取浏览历史
const fetchHistoryData = async () => {
  if (!currentUserId.value) {
    router.push('/login')
    return
  }
  try {
    const response = await profileAPI.getHistory(currentUserId.value)
    if (response && response.items) {
      // 转换数据格式以匹配你的界面
      history.value = response.items.map(item => ({
        id: item.historyId,
        title: item.title,
        community: `${item.layoutInfo.area}㎡ ${item.layoutInfo.bedroom_count}室`,
        price: item.priceInfo.total_price,
        image: `https://picsum.photos/seed/${item.propertyId}/100/100`,
        time: formatTime(item.createdAt)
      }))
      historyCount.value = response.count
    }
    showHistory.value = true
  } catch (err) {
    console.error('获取浏览记录失败:', err)
    showHistory.value = true
  }
}
// 添加：获取收藏列表函数
// 添加：获取收藏列表函数
// 添加：获取收藏列表函数
// 添加：获取收藏列表函数
const fetchFavoritesData = async () => {
  if (!currentUserId.value) {
    router.push('/login')
    return
  }
  try {
    const response = await profileAPI.getFavorites(currentUserId.value)
    console.log('收藏列表API响应:', response)

    if (response && response.items) {
      // 转换数据格式以匹配你的界面
      favorites.value = response.items.map(item => ({
        id: item.favoriteId,
        favoriteId: item.favoriteId,      // 收藏记录ID
        propertyId: item.propertyId,      // 房源ID（重要！用于取消收藏）
        title: item.title,
        community: `${item.layoutInfo?.area || 0}㎡ ${item.layoutInfo?.bedroom_count || 0}室`,
        price: item.priceInfo?.total_price,
        image: `https://picsum.photos/seed/${item.propertyId}/100/100`,
        // 添加更多详细信息
        layoutInfo: item.layoutInfo ? {
          bedroom_count: item.layoutInfo.bedroom_count || 0,
          living_room_count: 0, // API不返回此字段
          bathroom_count: 0,    // API不返回此字段
          area: item.layoutInfo.area || 0
        } : undefined,
        priceInfo: item.priceInfo
      }))
      favoritesCount.value = response.count
    } else {
      favorites.value = []
      favoritesCount.value = 0
    }
    showFavorites.value = true
  } catch (err) {
    console.error('获取收藏列表失败:', err)
    favorites.value = []
    favoritesCount.value = 0
    showFavorites.value = true
  }
}
// 格式化时间函数
const formatTime = (timeString: string) => {
  const time = new Date(timeString)
  const now = new Date()
  const diff = Math.floor((now.getTime() - time.getTime()) / 1000 / 60) // 分钟差

  if (diff < 1) return '刚刚'
  if (diff < 60) return `${diff}分钟前`
  if (diff < 1440) return `${Math.floor(diff / 60)}小时前`
  return `${Math.floor(diff / 1440)}天前`
}
// 添加：页面加载时调用
onMounted(() => {
  // 检查用户是否已登录
  if (!isUserLoggedIn.value) {
    console.log('用户未登录，跳转到登录页面')
    router.push('/login')
    return
  }
  fetchUserInfo()
  loadExistingPreferences()
})
watch(
  () => authStore.isLoggedIn,
  (newVal) => {
    isLoggedIn.value = newVal
    if (newVal && currentUserId.value) {
      fetchUserInfo()
    } else {
      // 用户登出，重置数据
      Object.assign(userInfo, {
        username: '未登录用户',
        phone: '未绑定手机号',
        bio: '点击登录体验完整功能',
        location: '未知',
      })
      favoritesCount.value = 0
      historyCount.value = 0
      preferencesCount.value = 0
      favorites.value = []
      history.value = []
    }
  }
)
// 用户信息
const userInfo = reactive({
  username: '房产达人',
  phone: '',
  bio: '专注于寻找理想的家',
  location: '',
  avatar: '../../assets/image/zhz.png',
})

// 状态数据
const isLoggedIn = ref(authStore.isLoggedIn)  // 从 auth store 获取
const favoritesCount = ref(0)  // 初始化为0
const historyCount = ref(0)    // 初始化为0
const preferencesCount = ref(0) // 初始化为0

// 弹窗状态
const showPreferences = ref(false)
const showFavorites = ref(false)
const showHistory = ref(false)
const showSettings = ref(false)

// 偏好设置
// 用户偏好设置的状态 - 与 LoginPage.vue 完全一致
const preferences = reactive({
  budget: {
    min: 0,
    max: 0
  },
  city: '', // 城市
  districts: [] as string[], // 区域（区县）
  house_types: [] as string[],
  orientations: [] as string[],
  bedroom_range: {
    min: '',
    max: ''
  }
})

// 选项数据
// 区域选项 - 与 LoginPage.vue 完全一致
const cityOptions = [
  { value: '北京', label: '北京市' },
  { value: '上海', label: '上海市' },
  { value: '天津', label: '天津市' },
  { value: '石家庄', label: '石家庄市' },
  { value: '深圳', label: '深圳市' },
  { value: '广州', label: '广州市' }
]

// 所有区域数据 - 与 LoginPage.vue 完全一致
// 所有区域数据
const allDistrictOptions = {
  '北京': [
    { value: '东城区', label: '东城区' },
    { value: '西城区', label: '西城区' },
    { value: '朝阳区', label: '朝阳区' },
    { value: '海淀区', label: '海淀区' },
    { value: '丰台区', label: '丰台区' },
    { value: '石景山区', label: '石景山区' },
    { value: '通州区', label: '通州区' },
    { value: '顺义区', label: '顺义区' },
    { value: '大兴区', label: '大兴区' },
    { value: '房山区', label: '房山区' }
  ],
  '上海': [
    { value: '黄浦区', label: '黄浦区' },
    { value: '徐汇区', label: '徐汇区' },
    { value: '长宁区', label: '长宁区' },
    { value: '静安区', label: '静安区' },
    { value: '普陀区', label: '普陀区' },
    { value: '虹口区', label: '虹口区' },
    { value: '杨浦区', label: '杨浦区' },
    { value: '浦东新区', label: '浦东新区' },
    { value: '闵行区', label: '闵行区' },
    { value: '宝山区', label: '宝山区' }
  ],
  '天津': [
    { value: '和平区', label: '和平区' },
    { value: '河东区', label: '河东区' },
    { value: '河西区', label: '河西区' },
    { value: '南开区', label: '南开区' },
    { value: '河北区', label: '河北区' },
    { value: '红桥区', label: '红桥区' },
    { value: '滨海新区', label: '滨海新区' },
    { value: '东丽区', label: '东丽区' },
    { value: '西青区', label: '西青区' },
    { value: '津南区', label: '津南区' }
  ],
  '石家庄': [
    { value: '长安区', label: '长安区' },
    { value: '桥西区', label: '桥西区' },
    { value: '新华区', label: '新华区' },
    { value: '裕华区', label: '裕华区' },
    { value: '井陉矿区', label: '井陉矿区' },
    { value: '藁城区', label: '藁城区' },
    { value: '鹿泉区', label: '鹿泉区' },
    { value: '栾城区', label: '栾城区' }
  ],
  '深圳': [
    { value: '福田区', label: '福田区' },
    { value: '罗湖区', label: '罗湖区' },
    { value: '南山区', label: '南山区' },
    { value: '盐田区', label: '盐田区' },
    { value: '宝安区', label: '宝安区' },
    { value: '龙岗区', label: '龙岗区' },
    { value: '龙华区', label: '龙华区' },
    { value: '坪山区', label: '坪山区' },
    { value: '光明区', label: '光明区' }
  ],
  '广州': [
    { value: '越秀区', label: '越秀区' },
    { value: '荔湾区', label: '荔湾区' },
    { value: '海珠区', label: '海珠区' },
    { value: '天河区', label: '天河区' },
    { value: '白云区', label: '白云区' },
    { value: '黄埔区', label: '黄埔区' },
    { value: '番禺区', label: '番禺区' },
    { value: '花都区', label: '花都区' },
    { value: '南沙区', label: '南沙区' },
    { value: '从化区', label: '从化区' },
    { value: '增城区', label: '增城区' }
  ]
}

// 计算属性：根据选择的城市过滤区域选项
const filteredDistrictOptions = computed(() => {
  if (preferences.city && allDistrictOptions[preferences.city as keyof typeof allDistrictOptions]) {
    return allDistrictOptions[preferences.city as keyof typeof allDistrictOptions]
  }
  return []
})

// 方法：城市变更时清空已选的区域
const updateDistrictOptions = () => {
  preferences.districts = []
}

// 收藏列表（初始为空，从API获取）

// 收藏列表（初始为空，从API获取）
const favorites = ref<Array<{
  id: number
  favoriteId: number      // 新增：收藏记录ID
  propertyId: number      // 新增：房源ID
  title: string
  community: string
  price: number
  image: string
  // 新增详细信息字段
  layoutInfo?: {
    bedroom_count: number
    living_room_count: number
    bathroom_count: number
    area: number
  }
  priceInfo?: {
    total_price: number
    unit_price: number
  }
}>>([])

// 浏览记录（初始为空，从API获取）
const history = ref<HistoryItem[]>([])

// 删除未使用的 goToSearchWithHistory 函数

// 方法
const toggleHouseType = (houseType: string) => {
  const index = preferences.house_types.indexOf(houseType)
  if (index > -1) {
    preferences.house_types.splice(index, 1)
  } else {
    preferences.house_types.push(houseType)
  }
}



const resetPreferences = () => {
  Object.assign(preferences, {
    budget: { min: 0, max: 0 },
    city: '',
    districts: [],
    house_types: [],
    orientations: [],
    bedroom_range: { min: '', max: '' }
  })
}

const savePreferences = async () => {
  if (!currentUserId.value) {
    router.push('/login')
    return
  }
  try {
    // 构建用户偏好JSON对象 - 与 LoginPage.vue 完全一致
    const preferenceData: Record<string, unknown> = {}

    // 1. 预算（只发送有正值的）
    if (preferences.budget.min > 0 || preferences.budget.max > 0) {
      const budget: Record<string, number> = {}
      if (preferences.budget.min > 0) budget.min = preferences.budget.min
      if (preferences.budget.max > 0) budget.max = preferences.budget.max
      preferenceData.budget = budget
      // 同时发送 price_range 以兼容接口
      preferenceData.price_range = budget
    }

    // 2. 城市（非空字符串）
    if (preferences.city && preferences.city.trim()) {
      preferenceData.city = preferences.city.trim()
    }

    // 3. 区域（非空数组）
    if (preferences.districts.length > 0) {
      preferenceData.districts = preferences.districts.filter(district => district && district.trim())
      if (preferences.city && preferences.city.trim()) {
        preferenceData.locations = [preferences.city.trim()]
      }
    }

    // 4. 房源类型（非空数组）
    if (preferences.house_types.length > 0) {
      preferenceData.house_types = preferences.house_types.filter(type => type && type.trim())
    }

    // 5. 朝向（非空数组）
    if (preferences.orientations.length > 0) {
      preferenceData.orientations = preferences.orientations.filter(orientation => orientation && orientation.trim())
    }

    // 6. 卧室数量范围（有值的）
    const hasMinBedroom = preferences.bedroom_range.min && preferences.bedroom_range.min !== ''
    const hasMaxBedroom = preferences.bedroom_range.max && preferences.bedroom_range.max !== ''

    if (hasMinBedroom || hasMaxBedroom) {
      const bedroomRange: Record<string, string> = {}
      if (hasMinBedroom) bedroomRange.min = preferences.bedroom_range.min
      if (hasMaxBedroom) bedroomRange.max = preferences.bedroom_range.max
      preferenceData.bedroom_range = bedroomRange
    }

    const requestData = {
      userId: currentUserId.value,
      preferenceData: preferenceData
    }

    console.log('保存偏好数据:', requestData)
    const response = await profileAPI.setPreferences(requestData)

    if (response && response.message) {
      alert('偏好设置已保存！')
      showPreferences.value = false
      await fetchUserInfo()  // 重新获取用户信息
    } else {
      alert('保存失败')
    }
  } catch (err) {
    console.error('保存偏好设置失败:', err)
    alert('保存失败：网络错误')
  }
}

const editAvatar = () => {
  alert('头像编辑功能')
}



const showHelp = () => {
  showHelpModal.value = true
}

const removeFavorite = async (favoriteId: number) => {
  if (!currentUserId.value) {
    router.push('/login')
    return
  }

  try {
    // 1. 找到要删除的收藏项
    const favoriteItem = favorites.value.find(item => item.id === favoriteId)
    if (!favoriteItem) {
      console.error('未找到收藏项:', favoriteId)
      return
    }

    console.log('开始取消收藏:', {
      userId: currentUserId.value,
      propertyId: favoriteItem.propertyId,
      favoriteId: favoriteItem.favoriteId
    })

    // 2. 调用取消收藏API
    const response = await queryAPI.removeFavorite(
      currentUserId.value,
      favoriteItem.propertyId
    )

    console.log('取消收藏API响应:', response)

    // 3. 根据响应处理
    if (response && response.message) {
      // 从本地列表中移除
      const index = favorites.value.findIndex(item => item.id === favoriteId)
      if (index > -1) {
        favorites.value.splice(index, 1)
        favoritesCount.value = favorites.value.length
        console.log('已成功取消收藏:', favoriteItem.propertyId)

        // 可以显示成功提示
        // alert('已取消收藏')
      }
    } else {
      const errorMsg = response?.message || '取消收藏失败'
      console.error('取消收藏失败:', errorMsg)
      alert(`取消收藏失败: ${errorMsg}`)
    }
  } catch (err) {
    console.error('取消收藏API调用失败:', err)
    alert('取消收藏失败，请重试')
  }
}

const clearHistory = () => {
  history.value = []
  historyCount.value = 0
  showHistory.value = false
}

const handleLogout = () => {
  // 使用 auth store 的 logout 方法，它会清理 localStorage 和 store
  authStore.logout()

  // 重置本地状态
  isLoggedIn.value = false
  Object.assign(userInfo, {
    username: '未登录用户',
    phone: '未绑定手机号',
    bio: '点击登录体验完整功能',
    location: '未知',
  })
  favoritesCount.value = 0
  historyCount.value = 0
  preferencesCount.value = 0

  // 跳转到登录页面
  router.push('/login')
}

// 添加新的响应式数据
const showPropertyDetail = ref(false)
const selectedProperty = ref<PropertyDetail | null>(null)

// 显示房源详情模态框（从收藏）
const showPropertyDetailFromFavorite = async (property: PropertyDetail & { propertyId?: number }) => {
  selectedProperty.value = property
  // 关闭当前打开的弹窗
  showFavorites.value = false
  showHistory.value = false
  // 显示详情弹窗
  showPropertyDetail.value = true

  // 记录浏览（从收藏进入）
  if (currentUserId.value && property.propertyId) {
    try {
      await queryAPI.recordBrowse(currentUserId.value, property.propertyId, 'favorite')
    } catch (error) {
      console.error('记录浏览失败:', error)
    }
  }
}

// 显示房源详情模态框（从历史记录）
const showPropertyDetailFromHistory = async (property: PropertyDetail & { propertyId?: number }) => {
  selectedProperty.value = property
  // 关闭当前打开的弹窗
  showFavorites.value = false
  showHistory.value = false
  // 显示详情弹窗
  showPropertyDetail.value = true

  // 记录浏览（从历史记录进入）
  if (currentUserId.value && property.propertyId) {
    try {
      await queryAPI.recordBrowse(currentUserId.value, property.propertyId, 'history')
    } catch (error) {
      console.error('记录浏览失败:', error)
    }
  }
}

// 检查房源是否已收藏
const isFavorited = (propertyId: number) => {
  return favorites.value.some(fav => fav.id === propertyId)
}

// 切换收藏状态
const toggleFavorite = async (property: PropertyDetail) => {
  if (!currentUserId.value) {
    router.push('/login')
    return
  }

  try {
    if (isFavorited(property.id)) {
      // 取消收藏
      await removeFavorite(property.id)
    } else {
      // 添加收藏
      const response = await queryAPI.addFavorite(currentUserId.value, property.id)
      if (response && response.message) {
        // 更新本地收藏列表
        favorites.value.push({
          id: property.id,
          favoriteId: Date.now(), // 临时ID
          propertyId: property.id,
          title: property.title,
          community: property.community,
          price: property.price,
          image: property.image
        })
        favoritesCount.value = favorites.value.length
      }
    }
  } catch (err) {
    console.error('操作失败:', err)
    alert('操作失败，请重试')
  }
}

// 处理购买操作
const handlePurchase = (property: PropertyDetail) => {
  alert(`正在购买房源: ${property.title}`)
  // 这里可以添加实际的购买逻辑
}

</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.profile-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding-bottom: 80px;
  position: relative;
}

.profile-container::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: moveBackground 20s linear infinite;
}

@keyframes moveBackground {
  0% { transform: translate(0, 0); }
  100% { transform: translate(50px, 50px); }
}

/* 导航头部 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 32px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(10px);
  gap: 40px;
}

.logo {
  font-size: 24px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  cursor: pointer;
  transition: transform 0.2s ease;
  white-space: nowrap;
}

.logo:hover {
  transform: scale(1.05);
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
  padding: 10px 20px;
  background: transparent;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #718096;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.nav-link:hover {
  background: #f7fafc;
  color: #2d3748;
}

.nav-link.active {
  color: white;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.nav-icon {
  font-size: 18px;
}

.header-actions {
  display: flex;
  gap: 15px;
}

.icon-btn {
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  border: 2px solid #e2e8f0;
  font-size: 20px;
  cursor: pointer;
  padding: 10px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.icon-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
  border-color: #667eea;
}

/* 用户卡片 */
.user-card {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.98);
  margin: 24px;
  padding: 28px;
  border-radius: 24px;
  backdrop-filter: blur(20px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
  position: relative;
  z-index: 1;
  animation: slideUp 0.5s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-avatar {
  position: relative;
  margin-right: 20px;
}

.avatar-img {
  width: 85px;
  height: 85px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid white;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
}

.avatar-edit {
  position: absolute;
  bottom: 2px;
  right: 2px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s ease;
}

.avatar-edit:hover {
  transform: scale(1.1);
}

.user-info {
  flex: 1;
}

.username {
  font-size: 22px;
  font-weight: 800;
  margin-bottom: 8px;
  color: #2d3748;
}

.user-desc {
  color: #718096;
  font-size: 14px;
  margin-bottom: 12px;
  font-weight: 500;
}

.user-tags {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.user-tag {
  font-size: 12px;
  color: #667eea;
  background: rgba(102, 126, 234, 0.1);
  padding: 4px 12px;
  border-radius: 12px;
  font-weight: 600;
}

.edit-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
}

.edit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

/* 数据统计 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin: 0 24px 24px;
  position: relative;
  z-index: 1;
}

.stat-item {
  background: rgba(255, 255, 255, 0.98);
  padding: 20px 16px;
  border-radius: 20px;
  text-align: center;
  cursor: pointer;
  backdrop-filter: blur(20px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.stat-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.2);
  border-color: #667eea;
}

.stat-icon {
  font-size: 32px;
  margin-bottom: 10px;
  display: block;
}

.stat-number {
  font-size: 24px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 6px;
}

.stat-label {
  font-size: 13px;
  color: #718096;
  font-weight: 600;
}

/* 功能列表 */
.function-list {
  background: rgba(255, 255, 255, 0.98);
  margin: 0 24px;
  border-radius: 20px;
  overflow: hidden;
  backdrop-filter: blur(20px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 1;
}

.list-section {
  padding: 8px 0;
}

.list-section:not(:last-child) {
  border-bottom: 2px solid #f7fafc;
}

.list-item {
  display: flex;
  align-items: center;
  padding: 18px 24px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.list-item::after {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  width: 0;
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.05), transparent);
  transition: width 0.3s ease;
}

.list-item:hover::after {
  width: 100%;
}

.list-item:hover {
  transform: translateX(4px);
}

.list-item:active {
  background: #f7fafc;
}

.item-icon {
  font-size: 24px;
  margin-right: 18px;
  width: 28px;
  text-align: center;
  position: relative;
  z-index: 1;
}

.item-icon.logout {
  color: #f56565;
}

.item-text {
  flex: 1;
  position: relative;
  z-index: 1;
}

.item-title {
  font-size: 16px;
  color: #2d3748;
  margin-bottom: 4px;
  font-weight: 600;
}

.item-desc {
  font-size: 12px;
  color: #a0aec0;
  font-weight: 500;
}

.item-arrow {
  color: #cbd5e0;
  font-size: 20px;
  position: relative;
  z-index: 1;
  transition: transform 0.2s ease;
}

.list-item:hover .item-arrow {
  transform: translateX(4px);
  color: #667eea;
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: white;
  border-radius: 24px;
  width: 100%;
  max-width: 520px;
  max-height: 85vh;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUpModal 0.3s ease;
}

@keyframes slideUpModal {
  from {
    opacity: 0;
    transform: translateY(40px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-bottom: 2px solid #f7fafc;
}

.modal-title {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.modal-actions {
  display: flex;
  gap: 16px;
  align-items: center;
}

.close-btn {
  background: #f7fafc;
  border: none;
  font-size: 26px;
  color: #a0aec0;
  cursor: pointer;
  padding: 6px;
  border-radius: 12px;
  transition: all 0.2s ease;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  background: #edf2f7;
  color: #2d3748;
}

.text-btn {
  background: none;
  border: none;
  color: #667eea;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.text-btn:hover {
  background: rgba(102, 126, 234, 0.1);
}

.modal-body {
  padding: 24px;
  max-height: 65vh;
  overflow-y: auto;
}

/* 偏好设置样式 */
.pref-section {
  margin-bottom: 28px;
  padding: 20px;
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}

.pref-section:last-child {
  margin-bottom: 0;
}

.pref-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 16px;
  color: #2d3748;
}

.price-inputs {
  display: flex;
  align-items: center;
  gap: 12px;
}

.input-group {
  flex: 1;
}

.input-group label {
  display: block;
  font-size: 13px;
  color: #718096;
  margin-bottom: 8px;
  font-weight: 600;
}

.price-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  font-size: 14px;
  background: white;
  transition: all 0.3s ease;
}

.price-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.separator {
  color: #718096;
  margin-top: 20px;
  font-weight: 700;
  font-size: 18px;
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.room-item {
  padding: 16px 10px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
}

.room-item:hover {
  border-color: #cbd5e0;
  transform: translateY(-2px);
}

.room-item.active {
  border-color: #667eea;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
}

.room-icon {
  font-size: 24px;
  margin-bottom: 8px;
  display: block;
}

.room-name {
  font-size: 13px;
  color: #2d3748;
  font-weight: 600;
}

.region-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.region-tag {
  padding: 8px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 16px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.region-tag.active {
  background: #007bff;
  color: white;
  border-color: #007bff;
}

.switch-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.switch-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.switch-label {
  font-size: 14px;
  color: #333;
}

.switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: 0.4s;
  border-radius: 24px;
}

.slider:before {
  position: absolute;
  content: '';
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.4s;
  border-radius: 50%;
}

input:checked + .slider {
  background-color: #007bff;
}

input:checked + .slider:before {
  transform: translateX(20px);
}

.save-btn {
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  margin-top: 24px;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
}

.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.save-btn:active {
  transform: translateY(0);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #718096;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
  opacity: 0.5;
}

.empty-text {
  margin-bottom: 24px;
  font-size: 16px;
  color: #a0aec0;
  font-weight: 500;
}

.primary-btn {
  padding: 14px 28px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
}

.primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

/* 列表项样式 */
.favorites-list,
.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.favorite-item,
.history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.favorite-img,
.history-img {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  object-fit: cover;
}

.favorite-info,
.history-info {
  flex: 1;
}

.favorite-title,
.history-title {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}

.favorite-location,
.history-location {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.favorite-price,
.history-price {
  color: #ff4757;
  font-weight: bold;
  font-size: 14px;
}

.history-meta {
  display: flex;
  justify-content: space-between;
}

.history-time {
  font-size: 11px;
  color: #999;
}

.remove-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: #999;
  cursor: pointer;
  padding: 4px;
}
/* 新增样式，与 LoginPage.vue 保持一致 */
.city-select {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 16px;
  background: white;
  transition: border-color 0.3s ease;
}

.city-select:focus {
  outline: none;
  border-color: #007bff;
}

.range-select {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  background: white;
}

.checkbox-group {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
}

.checkbox-label input[type="checkbox"] {
  width: auto;
  margin: 0;
}

/* 调整价格输入框的样式 */
.price-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 14px;
}
/* 帮助中心弹窗样式 */
.help-modal {
  max-width: 600px;
  max-height: 85vh;
}

.help-body {
  padding: 0 20px 20px;
}

.help-section {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.help-section:last-child {
  border-bottom: none;
}

.help-section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #007bff;
  display: flex;
  align-items: center;
  gap: 8px;
}

.help-list {
  padding-left: 20px;
  margin: 0;
}

.help-list li {
  margin-bottom: 8px;
  font-size: 14px;
  line-height: 1.5;
  color: #333;
}

.help-list li:last-child {
  margin-bottom: 0;
}

.help-list li strong {
  color: #333;
}

.faq-item {
  margin-bottom: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 12px;
  transition: background-color 0.2s;
}

.faq-item:hover {
  background: #f0f7ff;
}

.faq-question {
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
  font-size: 14px;
}

.faq-answer {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
}

.contact-info {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
}

.contact-info p {
  margin-bottom: 8px;
  font-size: 14px;
  color: #333;
}

.contact-info p:last-child {
  margin-bottom: 0;
}

.contact-info strong {
  color: #333;
  min-width: 80px;
  display: inline-block;
}

.help-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.version-info {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.copyright {
  font-size: 12px;
  color: #999;
}

/* 滚动条样式 */
.modal-body::-webkit-scrollbar {
  width: 6px;
}

.modal-body::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.modal-body::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.modal-body::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 房源详情弹窗样式 */
.property-detail-modal {
  max-width: 600px;
  max-height: 90vh;
}

.property-detail-body {
  padding: 0;
}

.property-detail-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.property-image-section {
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.property-detail-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.property-basic-info {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.property-title {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
}

.property-price {
  font-size: 24px;
  font-weight: bold;
  color: #ff4757;
  margin-bottom: 5px;
}

.property-community {
  font-size: 14px;
  color: #666;
}

.property-details {
  padding: 20px;
  flex: 1;
  overflow-y: auto;
}

.detail-row {
  display: flex;
  margin-bottom: 15px;
}

.detail-label {
  font-weight: bold;
  width: 80px;
  color: #333;
}

.detail-value {
  flex: 1;
  color: #666;
}

.actions-section {
  display: flex;
  gap: 10px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.action-button {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s;
}

.favorite-button {
  background-color: #f1f1f1;
  color: #333;
}

.favorite-button.favorited {
  background-color: #ff4757;
  color: white;
}

.contact-button {
  background-color: #007bff;
  color: white;
}

.buy-button {
  background-color: #2ed573;
  color: white;
}

.action-button:hover {
  opacity: 0.9;
  transform: translateY(-2px);
}
.property-meta {
  display: flex;
  gap: 12px;
  margin: 4px 0;
  color: #888;
  font-size: 12px;
}

.favorite-location {
  font-size: 13px;
  color: #666;
  margin: 2px 0;
}
</style>
