<template>
  <div class="login-page">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="logo">房产平台</div>
    </header>

    <!-- 主要内容 -->
    <div class="main-content">
      <!-- 未登录状态：登录/注册 -->
      <div v-if="!isLoggedIn" class="auth-container">
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

        <!-- 登录表单 -->
        <div v-if="activeTab === 'login'" class="auth-form">
          <h2>欢迎回来</h2>
          <form @submit.prevent="handleLogin">
            <div class="form-group">
              <label>手机号/用户名</label>
              <input
                v-model="loginForm.username"
                type="text"
                placeholder="请输入手机号或用户名"
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

            <div class="form-options">
              <label class="remember-me">
                <input type="checkbox" v-model="loginForm.rememberMe" />
                <span>记住我</span>
              </label>
              <a href="#" class="forgot-password">忘记密码？</a>
            </div>

            <button type="submit" class="submit-btn" :disabled="loginLoading">
              {{ loginLoading ? '登录中...' : '登录' }}
            </button>
          </form>
        </div>

        <!-- 注册表单 -->
        <div v-if="activeTab === 'register'" class="auth-form">
          <h2>创建账号</h2>
          <form @submit.prevent="handleRegister">
            <div class="form-group">
              <label>用户名</label>
              <input
                v-model="registerForm.username"
                type="text"
                placeholder="请输入用户名"
                @blur="validateUsername"
                required
              />
              <span class="error-msg" v-if="registerErrors.username">{{
                registerErrors.username
              }}</span>
            </div>

            <div class="form-group">
              <label>手机号</label>
              <input
                v-model="registerForm.phone"
                type="tel"
                placeholder="请输入手机号"
                @blur="validatePhone"
                required
              />
              <span class="error-msg" v-if="registerErrors.phone">{{ registerErrors.phone }}</span>
            </div>

            <div class="form-group">
              <label>验证码</label>
              <div class="code-input-group">
                <input
                  v-model="registerForm.verificationCode"
                  type="text"
                  placeholder="请输入验证码"
                  required
                />
                <button
                  type="button"
                  class="code-btn"
                  :disabled="countdown > 0"
                  @click="sendVerificationCode"
                >
                  {{ countdown > 0 ? `${countdown}s后重新发送` : '发送验证码' }}
                </button>
              </div>
            </div>

            <div class="form-group">
              <label>密码</label>
              <input
                v-model="registerForm.password"
                type="password"
                placeholder="请输入密码"
                @blur="validatePassword"
                required
              />
              <span class="error-msg" v-if="registerErrors.password">{{
                registerErrors.password
              }}</span>
            </div>

            <div class="form-group">
              <label>确认密码</label>
              <input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                @blur="validateConfirmPassword"
                required
              />
              <span class="error-msg" v-if="registerErrors.confirmPassword">{{
                registerErrors.confirmPassword
              }}</span>
            </div>

            <button type="submit" class="submit-btn" :disabled="registerLoading">
              {{ registerLoading ? '注册中...' : '注册' }}
            </button>
          </form>
        </div>
      </div>

      <!-- 已登录状态：用户信息和偏好设置 -->
      <div v-else class="user-container">
        <!-- 用户信息 -->
        <div class="user-profile">
          <div class="avatar-section">
            <img :src="userInfo.avatar" alt="用户头像" class="avatar" />
            <button @click="editAvatar" class="edit-avatar-btn">更换头像</button>
          </div>

          <div class="user-info">
            <h2>{{ userInfo.username }}</h2>
            <p class="user-phone">📱 {{ userInfo.phone }}</p>
            <p class="user-bio">{{ userInfo.bio }}</p>
          </div>
        </div>

        <!-- 资料编辑表单 -->
        <div class="profile-edit">
          <h3>编辑资料</h3>
          <form @submit.prevent="updateProfile">
            <div class="form-group">
              <label>用户名</label>
              <input v-model="profileForm.username" type="text" placeholder="请输入用户名" />
            </div>

            <div class="form-group">
              <label>个人简介</label>
              <textarea v-model="profileForm.bio" placeholder="请输入个人简介" rows="3"></textarea>
            </div>

            <button type="submit" class="submit-btn">保存修改</button>
          </form>
        </div>

        <!-- 偏好设置 -->
        <div class="preference-section">
          <h3>偏好设置</h3>
          <PreferenceSettings :preferences="userPreferences" @update="updatePreferences" />
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <button @click="goToSearch" class="primary-btn">开始找房</button>
          <button @click="handleLogout" class="logout-btn">退出登录</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PreferenceSettings from '../components/PreferenceSettings.vue'

const router = useRouter()

// 登录状态
const isLoggedIn = ref(false)
const activeTab = ref('login')
const countdown = ref(0)

// 登录表单
const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false,
})

const loginLoading = ref(false)

// 注册表单
const registerForm = reactive({
  username: '',
  phone: '',
  verificationCode: '',
  password: '',
  confirmPassword: '',
})

const registerErrors = reactive({
  username: '',
  phone: '',
  password: '',
  confirmPassword: '',
})

const registerLoading = ref(false)

// 用户信息
const userInfo = reactive({
  username: '用户名',
  phone: '138****8888',
  bio: '这个人很懒，什么都没有写～',
  avatar: 'https://img95.699pic.com/photo/50149/6896.jpg_wh860.jpg',
})

// 资料编辑表单
const profileForm = reactive({
  username: '',
  bio: '',
})

// 用户偏好
const userPreferences = reactive({
  interests: [] as string[],
  propertyTypes: [] as string[],
  priceRange: [2000, 8000] as [number, number],
  city: '',
})

// 方法 - 登录相关
const handleLogin = async () => {
  loginLoading.value = true
  try {
    // 模拟登录API调用
    await new Promise((resolve) => setTimeout(resolve, 1500))

    // 登录成功
    isLoggedIn.value = true
    Object.assign(userInfo, {
      username: loginForm.username,
      phone: '138****8888',
      bio: '欢迎来到房产平台！',
    })
    Object.assign(profileForm, {
      username: loginForm.username,
      bio: '欢迎来到房产平台！',
    })

    console.log('登录成功:', loginForm)

    // 新增：登录后直接跳转到搜索页面
    router.push('/search')
  } catch (error) {
    console.error('登录失败:', error)
    alert('登录失败，请检查用户名和密码')
  } finally {
    loginLoading.value = false
  }
}

// 方法 - 注册相关
const validateUsername = () => {
  if (registerForm.username.length < 2) {
    registerErrors.username = '用户名至少2个字符'
  } else if (registerForm.username.length > 20) {
    registerErrors.username = '用户名不能超过20个字符'
  } else {
    registerErrors.username = ''
  }
}

const validatePhone = () => {
  const phoneRegex = /^1[3-9]\d{9}$/
  if (!phoneRegex.test(registerForm.phone)) {
    registerErrors.phone = '请输入正确的手机号码'
  } else {
    registerErrors.phone = ''
  }
}

const validatePassword = () => {
  if (registerForm.password.length < 6) {
    registerErrors.password = '密码至少6位'
  } else {
    registerErrors.password = ''
  }
}

const validateConfirmPassword = () => {
  if (registerForm.password !== registerForm.confirmPassword) {
    registerErrors.confirmPassword = '两次输入的密码不一致'
  } else {
    registerErrors.confirmPassword = ''
  }
}

const sendVerificationCode = async () => {
  if (!registerForm.phone) {
    alert('请输入手机号')
    return
  }

  if (registerErrors.phone) {
    alert('请先修正手机号格式')
    return
  }

  // 模拟发送验证码
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)

  console.log('发送验证码到:', registerForm.phone)
}

const handleRegister = async () => {
  // 验证表单
  validateUsername()
  validatePhone()
  validatePassword()
  validateConfirmPassword()

  // 如果有错误，不提交
  if (Object.values(registerErrors).some((error) => error)) {
    return
  }

  registerLoading.value = true
  try {
    // 模拟注册API调用
    await new Promise((resolve) => setTimeout(resolve, 1500))

    // 注册成功，自动登录
    isLoggedIn.value = true
    Object.assign(userInfo, {
      username: registerForm.username,
      phone: registerForm.phone,
      bio: '欢迎新用户！',
    })
    Object.assign(profileForm, {
      username: registerForm.username,
      bio: '欢迎新用户！',
    })

    console.log('注册成功:', registerForm)
  } catch (error) {
    console.error('注册失败:', error)
    alert('注册失败，请重试')
  } finally {
    registerLoading.value = false
  }
}

// 方法 - 用户操作
const editAvatar = () => {
  alert('头像编辑功能')
}

const updateProfile = async () => {
  // 更新用户资料
  Object.assign(userInfo, profileForm)
  console.log('更新资料:', profileForm)
  alert('资料更新成功！')
}

const updatePreferences = (newPreferences: {
  interests: string[]
  propertyTypes: string[]
  priceRange: [number, number]
  city: string
}) => {
  Object.assign(userPreferences, newPreferences)
  console.log('更新偏好:', userPreferences)
}

const goToSearch = () => {
  router.push('/search')
}

const handleLogout = () => {
  isLoggedIn.value = false
  // 重置表单
  Object.assign(loginForm, {
    username: '',
    password: '',
    rememberMe: false,
  })
  Object.assign(registerForm, {
    username: '',
    phone: '',
    verificationCode: '',
    password: '',
    confirmPassword: '',
  })
  activeTab.value = 'login'
}

// 初始化
onMounted(() => {
  // 检查本地存储的登录状态
  const savedLoginState = localStorage.getItem('isLoggedIn')
  if (savedLoginState === 'true') {
    isLoggedIn.value = true
  }
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.header {
  padding: 20px;
  text-align: center;
}

.logo {
  font-size: 24px;
  font-weight: bold;
  color: white;
}

.main-content {
  max-width: 400px;
  margin: 0 auto;
  padding: 20px;
}

.auth-container {
  background: white;
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.auth-tabs {
  display: flex;
  margin-bottom: 30px;
  border-bottom: 1px solid #eee;
}

.tab-btn {
  flex: 1;
  padding: 12px;
  background: none;
  border: none;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab-btn.active {
  color: #007bff;
  font-weight: 500;
  border-bottom: 2px solid #007bff;
}

.auth-form h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-size: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
}

.form-group input,
.form-group textarea {
  width: 90%;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 16px;
  transition: border-color 0.3s ease;
  margin: 0 auto;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #007bff;
}

.code-input-group {
  display: flex;
  gap: 10px;
}

.code-input-group input {
  flex: 1;
}

.code-btn {
  padding: 12px 16px;
  background: #f8f9fa;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
  font-size: 14px;
}

.code-btn:disabled {
  background: #e9ecef;
  color: #6c757d;
  cursor: not-allowed;
}

.error-msg {
  display: block;
  color: #dc3545;
  font-size: 12px;
  margin-top: 5px;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
  cursor: pointer;
}

.forgot-password {
  color: #007bff;
  text-decoration: none;
  font-size: 14px;
}

.submit-btn {
  width: 100%;
  padding: 14px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.submit-btn:disabled {
  background: #6c757d;
  cursor: not-allowed;
}

.submit-btn:hover:not(:disabled) {
  background: #0056b3;
}

/* 用户容器样式 */
.user-container {
  background: white;
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.avatar-section {
  text-align: center;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #007bff;
}

.edit-avatar-btn {
  margin-top: 8px;
  padding: 4px 12px;
  background: #f8f9fa;
  border: 1px solid #ddd;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
}

.user-info h2 {
  margin: 0 0 8px 0;
  color: #333;
}

.user-phone,
.user-bio {
  margin: 4px 0;
  color: #666;
  font-size: 14px;
}

.profile-edit,
.preference-section {
  margin-bottom: 30px;
}

.profile-edit h3,
.preference-section h3 {
  margin-bottom: 20px;
  color: #333;
  font-size: 18px;
}

.action-buttons {
  display: flex;
  gap: 15px;
}

.primary-btn,
.logout-btn {
  flex: 1;
  padding: 14px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.primary-btn {
  background: #007bff;
  color: white;
}

.primary-btn:hover {
  background: #0056b3;
}

.logout-btn {
  background: #f8f9fa;
  color: #666;
  border: 1px solid #ddd;
}

.logout-btn:hover {
  background: #e9ecef;
}
</style>
