<template>
  <div class="login-page">
    <header class="header">
      <div class="logo">房产平台</div>
    </header>

    <div class="main-content">
      <div v-if="!authStore.isLoggedIn" class="auth-container">
        <div class="auth-tabs">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'login' }"
            @click="activeTab = 'login'"
          >
            登录
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'register' }"
            @click="activeTab = 'register'"
          >
            注册
          </button>
        </div>

        <div v-if="activeTab === 'login'" class="auth-form">
          <h2>欢迎回来</h2>
          <form @submit.prevent="handleLogin">
            <div class="form-group">
              <label>用户名</label>
              <input
                v-model="loginForm.username"
                type="text"
                placeholder="请输入用户名"
                required
              />
            </div>

            <div class="form-group">
              <label>密码</label>
              <input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                required
              />
            </div>

            <div v-if="loginError" class="error-message">
              {{ loginError }}
            </div>

            <button type="submit" class="submit-btn" :disabled="authStore.loginLoading">
              {{ authStore.loginLoading ? '登录中...' : '登录' }}
            </button>
          </form>
        </div>

  <div v-if="activeTab === 'register'" class="auth-form">
  <h2>创建账号</h2>
  <form @submit.prevent="handleRegister">
    <!-- 添加基本注册信息字段 -->
    <div class="form-group">
      <label>用户名</label>
      <input
        v-model="registerForm.username"
        type="text"
        placeholder="请输入用户名"
        required
      />
    </div>

    <div class="form-group">
      <label>手机号</label>
      <input
        v-model="registerForm.phone_number"
        type="tel"
        placeholder="请输入手机号"
        required
      />
    </div>

    <div class="form-group">
      <label>密码</label>
      <input
        v-model="registerForm.password"
        type="password"
        placeholder="请输入密码"
        required
      />
    </div>

    <div class="form-group">
      <label>确认密码</label>
      <input
        v-model="registerForm.confirmPassword"
        type="password"
        placeholder="请再次输入密码"
        required
      />
    </div>

    <!-- 偏好设置部分（保持不变） -->
    <div class="form-group">
      <label>偏好设置（可选）</label>

      <div class="preference-section">
        <!-- 预算范围 -->
        <div class="preference-item">
          <label>预算范围（万元）</label>
          <div class="range-inputs">
            <input
              v-model="preferences.budget.min"
              type="number"
              placeholder="最低预算"
              min="0"
              class="range-input"
            />
            <span class="range-separator">-</span>
            <input
              v-model="preferences.budget.max"
              type="number"
              placeholder="最高预算"
              min="0"
              class="range-input"
            />
          </div>
        </div>

        <!-- 城市选择 -->
        <div class="preference-item">
          <label>城市</label>
          <select v-model="preferences.city" class="city-select" @change="updateDistrictOptions">
            <option value="">请选择城市</option>
            <option v-for="city in cityOptions" :key="city.value" :value="city.value">
              {{ city.label }}
            </option>
          </select>
        </div>

        <!-- 区域选择（依赖城市选择） -->
        <div class="preference-item" v-if="preferences.city">
          <label>偏好区域（可多选）</label>
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

        <!-- 房源类型 -->
        <div class="preference-item">
          <label>房源类型</label>
          <select v-model="preferences.house_types" multiple class="multi-select">
            <option value="apartment">公寓</option>
            <option value="villa">别墅</option>
            <option value="townhouse">联排别墅</option>
            <option value="loft">loft</option>
          </select>
          <small class="hint">按住Ctrl/Cmd键可多选</small>
        </div>

        <!-- 朝向偏好 -->
        <div class="preference-item">
          <label>朝向偏好</label>
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
        <div class="preference-item">
          <label>卧室数量</label>
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
      </div>

      <small class="hint">这些设置可以在个人中心随时修改</small>
    </div>

    <div v-if="registerError" class="error-message">
      {{ registerError }}
    </div>

    <button type="submit" class="submit-btn" :disabled="registerLoading">
      {{ registerLoading ? '注册中...' : '注册' }}
    </button>
  </form>
</div>
      </div>

      <div v-else class="user-container">
        <div class="user-profile">
          <div class="avatar-section">
            <div class="avatar">{{ authStore.username?.charAt(0) || 'U' }}</div>
          </div>

          <div class="user-info">
            <h2>{{ authStore.username }}</h2>
            <p class="user-id">用户ID: {{ authStore.userId }}</p>
            <div v-if="parsedUserProfile" class="user-preferences">
              <div class="preference-item" v-if="parsedUserProfile.budget">
                <span class="label">预算：</span>
                <span class="value">{{ parsedUserProfile.budget.min }} - {{ parsedUserProfile.budget.max || '不限' }} 万</span>
              </div>
              <div class="preference-item" v-if="parsedUserProfile.preferred_locations">
                <span class="label">偏好区域：</span>
                <span class="value">{{ parsedUserProfile.preferred_locations.join('、') }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="action-buttons">
          <button @click="goToSearch" class="primary-btn">开始找房</button>
          <button @click="handleLogout" class="logout-btn">退出登录</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import type { UserProfile } from '@/types/api.types'

const router = useRouter()
const authStore = useAuthStore()

// 状态
const activeTab = ref('login')
const registerLoading = ref(false)
const loginError = ref('')
const registerError = ref('')

// 登录表单
const loginForm = reactive({
  username: '',
  password: '',
})

// 注册表单
const registerForm = reactive({
  username: '',
  phone_number: '',
  password: '',
  confirmPassword: '',
})



// 用户偏好设置的状态 - 修复：添加 city 和 districts
const preferences = reactive({
  budget: {
    min: 0,
    max: 0
  },
  city: '', // 添加：城市
  districts: [] as string[], // 添加：区域（区县）
  house_types: [] as string[],
  orientations: [] as string[],
  bedroom_range: {
    min: '',
    max: ''
  }
})

// 区域选项 - 修复：添加这个定义
const cityOptions = [
  { value: '北京', label: '北京市' },
  { value: '上海', label: '上海市' },
  { value: '天津', label: '天津市' },
  { value: '石家庄', label: '石家庄市' },
  { value: '深圳', label: '深圳市' },
  { value: '广州', label: '广州市' }
]

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

// 移除未使用的 locationOptions 变量（如果存在）
// const locationOptions = [...] // 删除这行

// 计算属性：从localStorage解析用户偏好
const parsedUserProfile = computed(() => {
  try {
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr) {
      const userInfo = JSON.parse(userInfoStr)
      if (userInfo.userProfile) {
        return JSON.parse(userInfo.userProfile) as UserProfile
      }
    }
    return null
  } catch {
    console.error('解析用户偏好失败')
    return null
  }
})

// --- 方法 - 登录 ---
const handleLogin = async (event?: Event) => {
  if (event) {
    event.preventDefault()
  }

  if (!loginForm.username || !loginForm.password) {
    loginError.value = '用户名和密码不能为空'
    return
  }

  loginError.value = ''

  try {
    const response = await fetch('http://localhost:5000/api/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify({
        username: loginForm.username,
        password: loginForm.password
      })
    })

    console.log('响应状态:', response.status, response.statusText)

    if (response.ok) {
      const data = await response.json()
      console.log('登录成功:', data)

      // 手动更新store
      authStore.token = `token-${Date.now()}`
      authStore.userId = data.userId
      authStore.username = data.username
      authStore.isLoggedIn = true

      localStorage.setItem('token', authStore.token)
      localStorage.setItem('userInfo', JSON.stringify({
        userId: data.userId,
        username: data.username,
        userProfile: data.userProfile || '{}'
      }))

      console.log('跳转到 /search')
      router.push('/search')
    } else {
      const errorText = await response.text()
      console.error('登录失败响应文本:', errorText)

      try {
        const errorData = JSON.parse(errorText)
        if (response.status === 401) {
          loginError.value = errorData.message === '用户不存在'
            ? '用户不存在'
            : '用户名或密码错误'
        } else {
          loginError.value = errorData.message || `登录失败（${response.status}）`
        }
      } catch {
        if (response.status === 401) {
          loginError.value = '用户名或密码错误'
        } else {
          loginError.value = `登录失败（${response.status} ${response.statusText}）`
        }
      }
    }
  } catch (error: unknown) {
    const err = error as Error
    console.error('网络请求失败:', err)
    loginError.value = '网络错误，请检查后端服务是否运行'
  }
}

// --- 方法 - 注册 ---
// --- 方法 - 注册 ---
const handleRegister = async () => {
  // 验证输入
  if (!registerForm.username || !registerForm.password || !registerForm.phone_number) {
    registerError.value = '用户名、密码和手机号不能为空'
    return
  }

  if (registerForm.password !== registerForm.confirmPassword) {
    registerError.value = '两次输入的密码不一致'
    return
  }

  console.log('🔄 开始注册处理')
  console.log('📝 表单数据:', {
    username: registerForm.username,
    phone: registerForm.phone_number,
    passwordLength: registerForm.password.length,
    city: preferences.city,
    districts: preferences.districts,
    hasBudget: preferences.budget.min > 0 || preferences.budget.max > 0,
    houseTypesCount: preferences.house_types.length,
    orientationsCount: preferences.orientations.length
  })

  // 构建用户偏好JSON对象 - 严格检查，避免发送无效数据
  const userProfile: Record<string, unknown> = {}

  // 1. 预算（只发送有正值的）
  if (preferences.budget.min > 0 || preferences.budget.max > 0) {
    const budget: Record<string, number> = {}
    if (preferences.budget.min > 0) budget.min = preferences.budget.min
    if (preferences.budget.max > 0) budget.max = preferences.budget.max
    userProfile.budget = budget
    // 同时发送 price_range 以兼容接口
    userProfile.price_range = budget
  }

  // 2. 城市（非空字符串）
  if (preferences.city && preferences.city.trim()) {
    userProfile.city = preferences.city.trim()
  }

  // 3. 区域（非空数组）
  if (preferences.districts.length > 0) {
    userProfile.districts = preferences.districts.filter(district => district && district.trim())
    if (preferences.city && preferences.city.trim()) {
      userProfile.locations = [preferences.city.trim()]
    }
  }

  // 4. 房源类型（非空数组）
  if (preferences.house_types.length > 0) {
    userProfile.house_types = preferences.house_types.filter(type => type && type.trim())
  }

  // 5. 朝向（非空数组）
  if (preferences.orientations.length > 0) {
    userProfile.orientations = preferences.orientations.filter(orientation => orientation && orientation.trim())
  }

  // 6. 卧室数量范围（有值的）
  const hasMinBedroom = preferences.bedroom_range.min && preferences.bedroom_range.min !== ''
  const hasMaxBedroom = preferences.bedroom_range.max && preferences.bedroom_range.max !== ''

  if (hasMinBedroom || hasMaxBedroom) {
    const bedroomRange: Record<string, string> = {}
    if (hasMinBedroom) bedroomRange.min = preferences.bedroom_range.min
    if (hasMaxBedroom) bedroomRange.max = preferences.bedroom_range.max
    userProfile.bedroom_range = bedroomRange
  }

  console.log('📦 清理后的偏好设置:', userProfile)
  console.log('📤 最终发送的数据结构:', {
    username: registerForm.username,
    password: '***'.repeat(registerForm.password.length),
    phone_number: registerForm.phone_number,
    user_profile: Object.keys(userProfile).length > 0 ? userProfile : undefined
  })

  registerLoading.value = true
  registerError.value = ''

  try {
    // 只有在 userProfile 有内容时才发送
    const userProfileToSend = Object.keys(userProfile).length > 0 ? userProfile : undefined

    const result = await authStore.register(
      registerForm.username,
      registerForm.password,
      registerForm.phone_number,
      userProfileToSend
    )

    console.log('📨 注册store返回结果:', result)

    if (result && (result as { success: boolean }).success) {
      console.log('✅ 注册成功')
      alert('注册成功！已自动登录')

      // 清空表单
      registerForm.username = ''
      registerForm.phone_number = ''
      registerForm.password = ''
      registerForm.confirmPassword = ''

      // 清空偏好设置
      preferences.budget.min = 0
      preferences.budget.max = 0
      preferences.city = ''
      preferences.districts = []
      preferences.house_types = []
      preferences.orientations = []
      preferences.bedroom_range.min = ''
      preferences.bedroom_range.max = ''

      // 切换到登录页
      activeTab.value = 'login'
      // 跳转到搜索页
      router.push('/search')
    } else {
      const errorResult = result as { error?: string }
      console.error('❌ 注册store返回失败:', errorResult)
      registerError.value = errorResult.error || '注册失败'
    }
  } catch (error: unknown) {
    const err = error as Error
    console.error('🔥 注册捕获错误详情:', {
      message: err.message,
      stack: err.stack,
      error: error
    })

    if (err.message.includes('已存在')) {
      registerError.value = err.message
    } else if (err.message.includes('400')) {
      registerError.value = '注册失败：请求数据格式错误，请检查输入'
    } else {
      registerError.value = err.message || '注册失败，请重试'
    }
  } finally {
    registerLoading.value = false
  }
}

// --- 方法 - 用户操作 ---
const goToSearch = () => {
  router.push('/search')
}

const handleLogout = () => {
  authStore.logout()
  activeTab.value = 'login'
  router.push('/login')
}

onMounted(() => {
  // 初始化auth store
  authStore.initialize()

  // 如果已登录，则跳转到主页
  // if (authStore.isLoggedIn) {
  //   router.push('/search')
  // }
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

/* 添加动画背景效果 */
.login-page::before {
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

.header {
  padding: 30px 20px;
  text-align: center;
  position: relative;
  z-index: 1;
}

.logo {
  font-size: 32px;
  font-weight: 800;
  color: white;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  letter-spacing: 1px;
}

.main-content {
  max-width: 520px;
  margin: 0 auto;
  padding: 20px;
  position: relative;
  z-index: 1;
}

.auth-container {
  background: rgba(255, 255, 255, 0.98);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
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

.auth-tabs {
  display: flex;
  margin-bottom: 35px;
  background: #f8f9fa;
  border-radius: 12px;
  padding: 6px;
  position: relative;
}

.tab-btn {
  flex: 1;
  padding: 12px 24px;
  background: transparent;
  border: none;
  font-size: 16px;
  font-weight: 500;
  color: #718096;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
  z-index: 1;
}

.tab-btn.active {
  color: #667eea;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.auth-form h2 {
  text-align: center;
  margin-bottom: 35px;
  color: #2d3748;
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: block;
  margin-bottom: 10px;
  color: #2d3748;
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 0.3px;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 14px 18px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  font-size: 15px;
  transition: all 0.3s ease;
  background: #f7fafc;
  color: #2d3748;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
  transform: translateY(-1px);
}

.form-group input::placeholder {
  color: #a0aec0;
}

/* 偏好设置样式 */
.preference-section {
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid #e2e8f0;
}

.preference-item {
  margin-bottom: 18px;
  padding-bottom: 18px;
  border-bottom: 1px solid #e2e8f0;
}

.preference-item:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.preference-item label {
  display: block;
  margin-bottom: 10px;
  color: #2d3748;
  font-weight: 600;
  font-size: 14px;
}

.range-inputs {
  display: flex;
  align-items: center;
  gap: 10px;
}

.range-input {
  flex: 1;
  padding: 10px 14px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  transition: all 0.3s ease;
}

.range-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.range-select {
  flex: 1;
  padding: 10px 14px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  transition: all 0.3s ease;
  cursor: pointer;
}

.range-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.range-separator {
  color: #718096;
  font-weight: bold;
  font-size: 18px;
}

.checkbox-group {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-size: 14px;
  color: #2d3748;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.2s ease;
  background: white;
  border: 1px solid #e2e8f0;
}

.checkbox-label:hover {
  background: #f7fafc;
  border-color: #cbd5e0;
}

.checkbox-label input[type="checkbox"] {
  width: 18px;
  height: 18px;
  margin: 0;
  cursor: pointer;
  accent-color: #667eea;
}

.multi-select {
  width: 100%;
  padding: 10px 14px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  min-height: 100px;
  transition: all 0.3s ease;
}

.multi-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.city-select {
  width: 100%;
  padding: 10px 14px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  transition: all 0.3s ease;
  cursor: pointer;
}

.city-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.hint {
  color: #718096;
  font-size: 12px;
  margin-top: 8px;
  display: block;
  font-style: italic;
}

.error-message {
  color: #c53030;
  font-size: 14px;
  margin-bottom: 20px;
  padding: 14px 18px;
  background: linear-gradient(135deg, #fed7d7 0%, #feb2b2 100%);
  border: 2px solid #fc8181;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  animation: shake 0.5s ease;
}

.error-message::before {
  content: '⚠️';
  font-size: 18px;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-10px); }
  75% { transform: translateX(10px); }
}

.submit-btn {
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  position: relative;
  overflow: hidden;
}

.submit-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.5s ease;
}

.submit-btn:hover::before {
  left: 100%;
}

.submit-btn:disabled {
  background: linear-gradient(135deg, #a0aec0 0%, #718096 100%);
  cursor: not-allowed;
  box-shadow: none;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

/* 用户容器样式 */
.user-container {
  background: rgba(255, 255, 255, 0.98);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
  animation: slideUp 0.5s ease-out;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 35px;
  padding-bottom: 25px;
  border-bottom: 2px solid #edf2f7;
}

.avatar-section {
  text-align: center;
}

.avatar {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: 800;
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
  border: 4px solid white;
}

.user-info h2 {
  margin: 0 0 10px 0;
  color: #2d3748;
  font-size: 26px;
  font-weight: 700;
}

.user-id {
  margin: 0 0 15px 0;
  color: #718096;
  font-size: 14px;
  font-weight: 500;
}

.user-preferences {
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  padding: 16px;
  border-radius: 12px;
  margin-top: 12px;
  border: 1px solid #e2e8f0;
}

.preference-item {
  margin-bottom: 10px;
  font-size: 14px;
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.preference-item:last-child {
  margin-bottom: 0;
}

.preference-item .label {
  color: #718096;
  font-weight: 600;
  min-width: 80px;
}

.preference-item .value {
  color: #2d3748;
  font-weight: 500;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.primary-btn,
.logout-btn {
  padding: 16px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.primary-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.logout-btn {
  background: white;
  color: #718096;
  border: 2px solid #e2e8f0;
}

.logout-btn:hover {
  background: #f7fafc;
  border-color: #cbd5e0;
  color: #2d3748;
}
</style>
