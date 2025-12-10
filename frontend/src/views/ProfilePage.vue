<template>
  <div class="profile-container">
    <!-- 顶部导航栏 -->
    <div class="nav-header">
      <div class="header-left">
        <span class="back-btn" @click="$router.back()">‹</span>
      </div>
      <div class="header-title">个人中心</div>
      <div class="header-right">
        <span class="settings-btn" @click="showSettings = true">⚙️</span>
      </div>
    </div>

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
          <span class="user-tag">📱 {{ userInfo.phone }}</span>
          <span class="user-tag">📍 {{ userInfo.location }}</span>
        </div>
      </div>
      <button class="edit-btn" @click="editProfile">编辑资料</button>
    </div>

    <!-- 数据统计 -->
    <div class="stats-grid">
      <div class="stat-item" @click="showFavorites = true">
        <div class="stat-icon">❤️</div>
        <div class="stat-number">{{ favoritesCount }}</div>
        <div class="stat-label">我的收藏</div>
      </div>
      <div class="stat-item" @click="showHistory = true">
        <div class="stat-icon">🕒</div>
        <div class="stat-number">{{ historyCount }}</div>
        <div class="stat-label">浏览记录</div>
      </div>
      <div class="stat-item" @click="showPreferences = true">
        <div class="stat-icon">⭐</div>
        <div class="stat-number">{{ preferencesCount }}</div>
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
            <div class="item-title">应用设置</div>
            <div class="item-desc">通知、主题等设置</div>
          </div>
          <div class="item-arrow">›</div>
        </div>
      </div>

      <div class="list-section">
        <div class="list-item" @click="handleLogout" v-if="isLoggedIn">
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

    <!-- 底部导航 -->
    <div class="bottom-nav">
      <button class="nav-btn active">
        <span>👤</span>
        <span>我的</span>
      </button>
      <button class="nav-btn" @click="$router.push('/search')">
        <span>🔍</span>
        <span>搜索</span>
      </button>
      <button class="nav-btn" @click="$router.push('/tools')">
        <span>📊</span>
        <span>工具</span>
      </button>
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
            <h4 class="pref-title">价格范围（元/月）</h4>
            <div class="price-inputs">
              <div class="input-group">
                <label>最低价格</label>
                <input
                  type="number"
                  v-model="preferences.minPrice"
                  placeholder="0"
                  class="price-input"
                />
              </div>
              <div class="separator">-</div>
              <div class="input-group">
                <label>最高价格</label>
                <input
                  type="number"
                  v-model="preferences.maxPrice"
                  placeholder="10000"
                  class="price-input"
                />
              </div>
            </div>
          </div>

          <!-- 户型偏好 -->
          <div class="pref-section">
            <h4 class="pref-title">户型偏好</h4>
            <div class="room-grid">
              <div
                v-for="room in roomTypes"
                :key="room.value"
                class="room-item"
                :class="{ active: preferences.roomTypes.includes(room.value) }"
                @click="toggleRoomType(room.value)"
              >
                <div class="room-icon">{{ room.icon }}</div>
                <div class="room-name">{{ room.label }}</div>
              </div>
            </div>
          </div>

          <!-- 区域偏好 -->
          <div class="pref-section">
            <h4 class="pref-title">区域偏好</h4>
            <div class="region-tags">
              <span
                v-for="region in regions"
                :key="region"
                class="region-tag"
                :class="{ active: preferences.regions.includes(region) }"
                @click="toggleRegion(region)"
              >
                {{ region }}
              </span>
            </div>
          </div>

          <!-- 其他偏好 -->
          <div class="pref-section">
            <h4 class="pref-title">其他偏好</h4>
            <div class="switch-list">
              <div class="switch-item">
                <span class="switch-label">仅看近地铁</span>
                <label class="switch">
                  <input type="checkbox" v-model="preferences.nearSubway" />
                  <span class="slider"></span>
                </label>
              </div>
              <div class="switch-item">
                <span class="switch-label">精装修优先</span>
                <label class="switch">
                  <input type="checkbox" v-model="preferences.renovated" />
                  <span class="slider"></span>
                </label>
              </div>
              <div class="switch-item">
                <span class="switch-label">电梯房优先</span>
                <label class="switch">
                  <input type="checkbox" v-model="preferences.hasElevator" />
                  <span class="slider"></span>
                </label>
              </div>
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
            <div v-for="item in favorites" :key="item.id" class="favorite-item">
              <img :src="item.image" alt="房源" class="favorite-img" />
              <div class="favorite-info">
                <h4 class="favorite-title">{{ item.title }}</h4>
                <p class="favorite-location">{{ item.community }}</p>
                <div class="favorite-price">¥{{ item.price }}/月</div>
              </div>
              <button class="remove-btn" @click="removeFavorite(item.id)">×</button>
            </div>
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
            <div v-for="item in history"
            :key="item.id"
            class="history-item"
            @click="goToSearchWithHistory(item)"
            >
              <img :src="item.image" alt="房源" class="history-img" />
              <div class="history-info">
                <h4 class="history-title">{{ item.title }}</h4>
                <p class="history-location">{{ item.community }}</p>
                <div class="history-meta">
                  <span class="history-price">¥{{ item.price }}/月</span>
                  <span class="history-time">{{ item.time }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
// 在 <script setup> 顶部添加这个接口定义
interface HistoryItem {
  id: number
  title: string
  community: string
  price: number
  image: string
  time: string
}
const router = useRouter()
// 用户信息
const userInfo = reactive({
  username: '房产达人',
  phone: '138****8888',
  bio: '专注于寻找理想的家',
  location: '北京市',
  avatar: 'https://img95.699pic.com/photo/50149/6896.jpg_wh860.jpg',
})

// 状态数据
const isLoggedIn = ref(true)
const favoritesCount = ref(3)
const historyCount = ref(12)
const preferencesCount = ref(5)

// 弹窗状态
const showPreferences = ref(false)
const showFavorites = ref(false)
const showHistory = ref(false)
const showSettings = ref(false)

// 偏好设置
const preferences = reactive({
  minPrice: 2000,
  maxPrice: 6000,
  roomTypes: ['1', '2'],
  regions: ['朝阳区', '海淀区'],
  nearSubway: true,
  renovated: true,
  hasElevator: false,
})

// 选项数据
const roomTypes = [
  { value: '1', label: '一居', icon: '1️⃣' },
  { value: '2', label: '二居', icon: '2️⃣' },
  { value: '3', label: '三居', icon: '3️⃣' },
  { value: '4', label: '四居+', icon: '4️⃣' },
]

const regions = ['朝阳区', '海淀区', '西城区', '东城区', '丰台区', '石景山区', '通州区', '昌平区']

// 模拟数据
const favorites = ref([
  {
    id: 1,
    title: '精装修两居室',
    community: '阳光小区',
    price: 4500,
    image: 'https://img95.699pic.com/photo/50149/6896.jpg_wh860.jpg',
  },
])

const history = ref<HistoryItem[]>([
  {
    id: 1,
    title: '精装修两居室',
    community: '阳光小区',
    price: 4500,
    image: 'https://img95.699pic.com/photo/50149/6896.jpg_wh860.jpg',
    time: '2小时前',
  },
])


const goToSearchWithHistory = (item: HistoryItem) => {
  // 关闭历史记录弹窗
  showHistory.value = false

  // 构建搜索关键词（使用标题或小区名）
  const searchKeyword = item.title || item.community

  // 跳转到搜索页面，传递搜索关键词
  router.push({
    path: '/search',
    query: {
      keyword: searchKeyword,  // 搜索关键词
      historyId: item.id,      // 历史记录ID（可选）
      autoSearch: 'true'       // 自动搜索标记
    }
  })
}
// 方法
const toggleRoomType = (roomType: string) => {
  const index = preferences.roomTypes.indexOf(roomType)
  if (index > -1) {
    preferences.roomTypes.splice(index, 1)
  } else {
    preferences.roomTypes.push(roomType)
  }
}

const toggleRegion = (region: string) => {
  const index = preferences.regions.indexOf(region)
  if (index > -1) {
    preferences.regions.splice(index, 1)
  } else {
    preferences.regions.push(region)
  }
}

const resetPreferences = () => {
  Object.assign(preferences, {
    minPrice: 0,
    maxPrice: 10000,
    roomTypes: [],
    regions: [],
    nearSubway: false,
    renovated: false,
    hasElevator: false,
  })
}

const savePreferences = () => {
  console.log('保存偏好:', preferences)
  alert('偏好设置已保存！')
  showPreferences.value = false
}

const editAvatar = () => {
  alert('头像编辑功能')
}

const editProfile = () => {
  alert('编辑资料功能')
}

const showHelp = () => {
  alert('帮助中心')
}

const removeFavorite = (id: number) => {
  const index = favorites.value.findIndex((item) => item.id === id)
  if (index > -1) {
    favorites.value.splice(index, 1)
    favoritesCount.value = favorites.value.length
  }
}

const clearHistory = () => {
  history.value = []
  historyCount.value = 0
  showHistory.value = false
}

const handleLogout = () => {
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
  router.push('/login')
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
}

/* 导航头部 */
.nav-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 20px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
}

.header-left,
.header-right {
  width: 60px;
}

.back-btn,
.settings-btn {
  font-size: 24px;
  cursor: pointer;
  padding: 5px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

/* 用户卡片 */
.user-card {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.95);
  margin: 20px;
  padding: 20px;
  border-radius: 16px;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.user-avatar {
  position: relative;
  margin-right: 15px;
}

.avatar-img {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-edit {
  position: absolute;
  bottom: 0;
  right: 0;
  background: #007bff;
  color: white;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  cursor: pointer;
}

.user-info {
  flex: 1;
}

.username {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 5px;
  color: #333;
}

.user-desc {
  color: #666;
  font-size: 14px;
  margin-bottom: 8px;
}

.user-tags {
  display: flex;
  gap: 10px;
}

.user-tag {
  font-size: 12px;
  color: #888;
}

.edit-btn {
  background: #007bff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

/* 数据统计 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin: 0 20px 20px;
}

.stat-item {
  background: rgba(255, 255, 255, 0.95);
  padding: 15px;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  backdrop-filter: blur(10px);
}

.stat-icon {
  font-size: 20px;
  margin-bottom: 5px;
}

.stat-number {
  font-size: 18px;
  font-weight: bold;
  color: #007bff;
  margin-bottom: 2px;
}

.stat-label {
  font-size: 12px;
  color: #666;
}

/* 功能列表 */
.function-list {
  background: rgba(255, 255, 255, 0.95);
  margin: 0 20px;
  border-radius: 16px;
  overflow: hidden;
  backdrop-filter: blur(10px);
}

.list-section {
  padding: 10px 0;
}

.list-section:not(:last-child) {
  border-bottom: 1px solid #f0f0f0;
}

.list-item {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.list-item:active {
  background: #f8f9fa;
}

.item-icon {
  font-size: 20px;
  margin-right: 15px;
  width: 24px;
  text-align: center;
}

.item-icon.logout {
  color: #ff4757;
}

.item-text {
  flex: 1;
}

.item-title {
  font-size: 16px;
  color: #333;
  margin-bottom: 2px;
}

.item-desc {
  font-size: 12px;
  color: #888;
}

.item-arrow {
  color: #ccc;
  font-size: 18px;
}

/* 底部导航 */
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
/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 500px;
  max-height: 80vh;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.modal-actions {
  display: flex;
  gap: 15px;
  align-items: center;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 4px;
}

.text-btn {
  background: none;
  border: none;
  color: #007bff;
  cursor: pointer;
  font-size: 14px;
}

.modal-body {
  padding: 20px;
  max-height: 60vh;
  overflow-y: auto;
}

/* 偏好设置样式 */
.pref-section {
  margin-bottom: 24px;
}

.pref-section:last-child {
  margin-bottom: 0;
}

.pref-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #333;
}

.price-inputs {
  display: flex;
  align-items: center;
  gap: 10px;
}

.input-group {
  flex: 1;
}

.input-group label {
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 5px;
}

.price-input {
  width: 100%;
  padding: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
}

.separator {
  color: #999;
  margin-top: 20px;
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.room-item {
  padding: 12px 8px;
  border: 2px solid #f0f0f0;
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.room-item.active {
  border-color: #007bff;
  background: #f0f8ff;
}

.room-icon {
  font-size: 18px;
  margin-bottom: 5px;
}

.room-name {
  font-size: 12px;
  color: #333;
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
  padding: 14px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 20px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #666;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-text {
  margin-bottom: 16px;
}

.primary-btn {
  padding: 12px 24px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
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
</style>
