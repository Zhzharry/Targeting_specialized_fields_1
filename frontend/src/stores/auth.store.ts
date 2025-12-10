import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RootStoreContext } from './context'

// 🔧 新增：定义API响应类型
interface LoginResponse {
  userId?: number
  username?: string
  userProfile?: string
  message?: string
  [key: string]: unknown
}

interface RegisterResponse {
  userId?: number
  username?: string
  message?: string
  [key: string]: unknown
}

// 创建认证Store的工厂函数
export function createAuthStore(context: RootStoreContext) {
  const { api } = context

  return defineStore('auth', () => {
    // 状态
    const token = ref('')
    const isLoggedIn = ref(false)
    const userId = ref(0)
    const username = ref('')
    const loginLoading = ref(false)

    // 方法
    const initialize = () => {
      const savedToken = localStorage.getItem('token')
      const savedUserInfo = localStorage.getItem('userInfo')

      if (savedToken && savedUserInfo) {
        try {
          const userInfo = JSON.parse(savedUserInfo)
          token.value = savedToken
          userId.value = userInfo.userId || 0
          username.value = userInfo.username || ''
          isLoggedIn.value = true
        } catch {
          clearAuth()
        }
      }
    }

    // 登录方法
    const login = async (usernameInput: string, password: string) => {
      loginLoading.value = true
      try {
        const response = await api.auth.login({
          username: usernameInput,
          password: password
        }) as LoginResponse

        console.log('登录API响应:', response)

        if (response && typeof response.userId === 'number') {
          token.value = `real-token-${Date.now()}`
          userId.value = response.userId
          username.value = response.username || usernameInput
          isLoggedIn.value = true

          localStorage.setItem('token', token.value)
          localStorage.setItem('userInfo', JSON.stringify({
            userId: response.userId,
            username: response.username || usernameInput,
            userProfile: response.userProfile || '{}'
          }))

          return { success: true, data: response }
        }

        const errorMsg = response?.message || '登录失败：服务器返回无效数据'
        return { success: false, error: errorMsg }

      } catch (error: unknown) {
        console.error('API登录失败:', error)

        let errorMessage = '网络错误，请重试'
        if (error instanceof Error) {
          errorMessage = error.message
        } else if (typeof error === 'object' && error !== null) {
          const err = error as { message?: string; response?: { status?: number } }
          if (err.response?.status === 401) {
            errorMessage = '用户名或密码错误'
          } else if (err.message) {
            errorMessage = err.message
          }
        }

        throw new Error(errorMessage)
      } finally {
        loginLoading.value = false
      }
    }

    // 注册方法 - 修正：添加phone_number参数
    const register = async (
      usernameInput: string,
      password: string,
      phoneNumber: string,  // 新增：手机号参数
      userProfile?: Record<string, unknown>  // 可选的用户资料
    ) => {
      try {
        const response = await api.auth.register({
          username: usernameInput,
          password: password,
          phone_number: phoneNumber,  // 添加手机号
          user_profile: userProfile || {}  // 可选的用户资料
        }) as RegisterResponse

        console.log('注册API响应:', response)

        if (response && typeof response.userId === 'number') {
          // 注册成功后自动登录
          return login(usernameInput, password)
        }

        const errorMsg = response?.message || '注册失败：服务器返回无效数据'
        return { success: false, error: errorMsg }

      } catch (error: unknown) {
        console.error('API注册失败:', error)

        let errorMessage = '注册失败，请重试'
        if (error instanceof Error) {
          errorMessage = error.message
        } else if (typeof error === 'object' && error !== null) {
          const err = error as { message?: string }
          if (err.message) errorMessage = err.message
        }

        throw new Error(errorMessage)
      }
    }

    const logout = () => {
      // 可以调用 api.auth.logout() 如果后端需要
      clearAuth()
    }

    const clearAuth = () => {
      token.value = ''
      userId.value = 0
      username.value = ''
      isLoggedIn.value = false
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }

    return {
      // 状态
      token,
      isLoggedIn,
      userId,
      username,
      loginLoading,

      // 方法
      initialize,
      login,
      register,  // 现在需要传入phone_number
      logout,
      clearAuth
    }
  })
}

// 创建便捷的use函数
import { getRootStoreContext } from './context'
export const useAuthStore = () => {
  const context = getRootStoreContext()
  const storeFactory = createAuthStore(context)
  return storeFactory()
}
