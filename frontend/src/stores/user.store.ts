import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import type { RootStoreContext } from './context'
import type {
  ProfileDetailResponse,  // 修正类型导入
  FavoritesResponse,
  HistoryResponse,
  PreferenceRequest
} from '@/types/api.types'

// 创建用户Store的工厂函数
export function createUserStore(context: RootStoreContext) {
  const { api } = context

  return defineStore('user', () => {
    const userProfile = reactive({
      bio: '',
      phone: '',
      location: '',
      avatar: '',
      joinedAt: '',
      stats: {
        favorites: 0,
        browsed: 0,
        recommendations: 0
      }
    })

    const favorites = ref<Array<{
      favoriteId: number
      propertyId: number
      title: string
      priceInfo: { total_price: number; unit_price: number }
      layoutInfo: { bedroom_count: number; area: number }
      createdAt: string
    }>>([])

    const favoritesCount = ref(0)
    const history = ref<Array<{
      historyId: number
      propertyId: number
      title: string
      behaviorData: { duration: number; device: string }
      priceInfo: { total_price: number; unit_price: number }
      layoutInfo: { bedroom_count: number; area: number }
      createdAt: string
    }>>([])

    const historyCount = ref(0)
    const loading = ref(false)

    // 获取当前用户ID（辅助函数）
    const getCurrentUserId = (): number | null => {
      try {
        const userInfo = localStorage.getItem('userInfo')
        if (userInfo) {
          const parsed = JSON.parse(userInfo)
          return parsed.userId || null
        }
      } catch {
        // 忽略解析错误
      }
      return null
    }

    // 加载用户信息
    const loadUserInfo = async () => {
      const userId = getCurrentUserId()
      if (!userId || loading.value) return

      loading.value = true
      try {
        // 注意：这里调用的是 authAPI.getMyProfile
        const response = await api.auth.getMyProfile(userId) as ProfileDetailResponse
        console.log('用户信息API响应:', response)

        if (response && response.profile) {
          const profile = response.profile

          // 更新用户资料
          Object.assign(userProfile, {
            bio: '专注于寻找理想的家',  // 可以从userProfile中获取
            phone: '138****8888',      // 可以从userProfile中获取
            location: profile.userProfile.preferred_locations?.[0] || '未知',
            avatar: 'https://img95.699pic.com/photo/50149/6896.jpg_wh860.jpg',
            joinedAt: profile.joinedAt || '',
            stats: profile.stats || { favorites: 0, browsed: 0, recommendations: 0 }
          })
        }
      } catch (error) {
        console.error('加载用户信息失败:', error)
        // 降级处理
        Object.assign(userProfile, {
          bio: '专注于寻找理想的家',
          phone: '未知',
          location: '未知',
          avatar: '../../assets/image/zhz.png',
          joinedAt: '',
          stats: { favorites: 0, browsed: 0 }
        })
      } finally {
        loading.value = false
      }
    }

    // 加载收藏列表
    const loadFavorites = async () => {
      const userId = getCurrentUserId()
      if (!userId) return

      try {
        const response = await api.profile.getFavorites(userId) as FavoritesResponse
        console.log('收藏API响应:', response)

        if (response && response.items) {
          favorites.value = response.items
          favoritesCount.value = response.count || response.items.length
        }
      } catch (error) {
        console.error('加载收藏失败:', error)
        favorites.value = []
        favoritesCount.value = 0
      }
    }

    // 加载历史记录
    const loadHistory = async () => {
      const userId = getCurrentUserId()
      if (!userId) return

      try {
        const response = await api.profile.getHistory(userId) as HistoryResponse
        console.log('历史记录API响应:', response)

        if (response && response.items) {
          history.value = response.items
          historyCount.value = response.count || response.items.length
        }
      } catch (error) {
        console.error('加载历史记录失败:', error)
        history.value = []
        historyCount.value = 0
      }
    }

    // 保存偏好设置
    const savePreferences = async (preferenceData: Record<string, unknown>) => {
      const userId = getCurrentUserId()
      if (!userId) return { success: false, error: '用户未登录' }

      try {
        const requestData: PreferenceRequest = {
          userId,
          preferenceData
        }

        const response = await api.profile.setPreferences(requestData)
        console.log('保存偏好设置响应:', response)
        return { success: true, data: response }
      } catch (error) {
        console.error('保存偏好设置失败:', error)
        return {
          success: false,
          error: error instanceof Error ? error.message : '未知错误'
        }
      }
    }

    // 加载所有用户数据
    const loadAllUserData = async () => {
      await Promise.all([
        loadUserInfo(),
        loadFavorites(),
        loadHistory()
      ])
    }

    return {
      userProfile,
      favorites,
      favoritesCount,
      history,
      historyCount,
      loading,
      loadUserInfo,
      loadFavorites,
      loadHistory,
      savePreferences,
      loadAllUserData
    }
  })
}

// 便捷use函数
import { getRootStoreContext } from './context'
export const useUserStore = () => {
  const context = getRootStoreContext()
  const storeFactory = createUserStore(context)
  return storeFactory()
}
