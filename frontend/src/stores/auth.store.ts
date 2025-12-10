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

// 定义Axios错误类型
interface AxiosError {
  response?: {
    status?: number
    data?: {
      message?: string
    }
  }
  message?: string
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
        } else if (error && typeof error === 'object') {
          // 类型安全的错误处理
          const axiosError = error as AxiosError

          if (axiosError.response?.status === 401) {
            errorMessage = '用户名或密码错误'
          } else if (axiosError.response?.data?.message) {
            errorMessage = axiosError.response.data.message
          } else if (axiosError.message) {
            errorMessage = axiosError.message
          }
        }

        throw new Error(errorMessage)
      } finally {
        loginLoading.value = false
      }
    }

    // 注册方法
// auth.store.ts 中的 register 方法 - 修复版本（没有any）
const register = async (
  usernameInput: string,
  password: string,
  phoneNumber: string,
  userProfile?: Record<string, unknown>
) => {
  try {
    // 构建请求数据 - 使用正确类型
    const requestData: {
      username: string
      password: string
      phone_number: string
      user_profile?: Record<string, unknown>
    } = {
      username: usernameInput,
      password: password,
      phone_number: phoneNumber
    }

    // 只有 userProfile 有内容时才添加
    if (userProfile && Object.keys(userProfile).length > 0) {
      console.log('📤 Store层发送的user_profile:', userProfile)
      requestData.user_profile = userProfile
    } else {
      console.log('📤 Store层: 不发送user_profile')
    }

    console.log('📨 Store层完整请求数据:', JSON.stringify(requestData, null, 2))

    const response = await api.auth.register(requestData) as RegisterResponse

    console.log('✅ Store层注册API响应:', response)

    if (response && typeof response.userId === 'number') {
      // 注册成功后自动登录
      return login(usernameInput, password)
    }

    const errorMsg = response?.message || '注册失败：服务器返回无效数据'
    console.error('❌ Store层注册失败:', errorMsg)
    return { success: false, error: errorMsg }

  } catch (error: unknown) {
    console.error('🔥 Store层API注册失败详细:', error)

    let errorMessage = '注册失败，请重试'

    // 类型安全的错误处理
    if (error instanceof Error) {
      errorMessage = error.message
    } else if (error && typeof error === 'object') {
      // 定义Axios错误类型
      interface AxiosErrorType {
        response?: {
          status?: number
          data?: {
            message?: string
            error?: string
          }
        }
        message?: string
      }

      const axiosError = error as AxiosErrorType

      console.error('🔥 Axios错误详情:', {
        status: axiosError.response?.status,
        data: axiosError.response?.data,
        message: axiosError.message
      })

      if (axiosError.response?.status === 400) {
        if (axiosError.response?.data?.message) {
          errorMessage = `注册失败：${axiosError.response.data.message}`
        } else if (axiosError.response?.data?.error) {
          errorMessage = `注册失败：${axiosError.response.data.error}`
        } else {
          errorMessage = '注册失败：请求数据格式错误'
        }
      } else if (axiosError.response?.data?.message) {
        errorMessage = axiosError.response.data.message
      } else if (axiosError.message) {
        errorMessage = axiosError.message
      }
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
      register,
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
