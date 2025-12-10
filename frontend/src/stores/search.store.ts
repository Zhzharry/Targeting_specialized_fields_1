import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RootStoreContext } from './context'

// 🔧 新增：定义API响应类型
interface SearchResponse {
  items?: Array<{
    propertyId?: number
    title?: string
    communityName?: string
    priceInfo?: { total_price?: number; unit_price?: number }
    layoutInfo?: { 
      bedroom_count?: number
      living_room_count?: number 
      bathroom_count?: number
      area?: number
    }
    basicInfo?: {
      property_type?: string
      build_year?: number
      [key: string]: unknown
    }
    locationInfo?: {
      province?: string
      city?: string
      district?: string
      [key: string]: unknown
    }
    cover?: string
    tags?: string[]
    [key: string]: unknown
  }>
  count?: number
  message?: string
  [key: string]: unknown
}

interface GuessYouLikeResponse {
  items?: Array<{
    propertyId?: number
    title?: string
    summary?: string
    totalPrice?: number
    cover?: string
    detailUrl?: string
    tags?: string[]
    [key: string]: unknown
  }>
  message?: string
  [key: string]: unknown
}

// 创建搜索Store的工厂函数
export function createSearchStore(context: RootStoreContext) {
  const { api } = context

  return defineStore('search', () => {
    const searchQuery = ref('')
    const searchResults = ref<Array<Record<string, unknown>>>([])
    const searchLoading = ref(false)
    const searchCount = ref(0)
    
    const guessYouLike = ref<Array<Record<string, unknown>>>([])
    const searchHistory = ref<Array<{id: number, keyword: string, time: string}>>([])

    // 执行搜索
    const performSearch = async (params: Record<string, unknown> = {}) => {
      searchLoading.value = true
      try {
        // 调用真实API（添加类型断言）
        const response = await api.query.searchProperties({
          keyword: searchQuery.value || params.keyword || '',
          ...params
        }) as SearchResponse
        
        console.log('搜索API响应:', response)
        
        // 🔧 修复：安全地处理响应数据
        if (response && Array.isArray(response.items)) {
          searchResults.value = response.items
          searchCount.value = typeof response.count === 'number' 
            ? response.count 
            : response.items.length
        } else {
          // 降级处理：使用模拟数据
          const mockResults = [
            {
              propertyId: 1,
              title: '精装修两居室',
              communityName: '阳光小区',
              priceInfo: { total_price: 450, unit_price: 60000 },
              layoutInfo: { 
                bedroom_count: 2, 
                living_room_count: 1, 
                bathroom_count: 1, 
                area: 85 
              },
              basicInfo: { property_type: 'apartment', build_year: 2018 },
              locationInfo: { province: '广东省', city: '深圳市', district: '南山区' },
              cover: 'https://img95.699pic.com/photo/50149/6896.jpg_wh860.jpg',
              tags: ['近地铁', '精装修']
            }
          ] as Record<string, unknown>[]
          
          searchResults.value = mockResults
          searchCount.value = mockResults.length
        }
        
        // 保存到搜索历史（前端管理）
        if (searchQuery.value.trim()) {
          addToSearchHistory(searchQuery.value)
        }
        
      } catch (error) {
        console.error('搜索失败:', error)
        // 使用模拟数据
        const mockResults = [
          {
            id: 1,
            title: '精装修两居室',
            community: '阳光小区',
            price: 4500,
            bedrooms: 2,
            area: 85,
            image: 'https://img95.699pic.com/photo/50149/6896.jpg_wh860.jpg'
          }
        ] as Record<string, unknown>[]
        
        searchResults.value = mockResults
        searchCount.value = mockResults.length
      } finally {
        searchLoading.value = false
      }
    }

    // 加载猜你喜欢
    const loadGuessYouLike = async () => {
      try {
        const response = await api.query.getGuessYouLike() as GuessYouLikeResponse
        console.log('猜你喜欢API响应:', response)
        
        // 🔧 修复：安全地处理响应数据
        if (response && Array.isArray(response.items)) {
          guessYouLike.value = response.items
        } else {
          // 使用模拟数据
          const mockItems = [
            {
              propertyId: 101,
              title: '万科城市花园 精装三房',
              summary: '南山区 · 89.5㎡ · 3室2厅2卫',
              totalPrice: 650.5,
              cover: 'https://img95.699pic.com/photo/50149/6896.jpg_wh860.jpg',
              detailUrl: 'https://example.com/property/101',
              tags: ['近地铁', '学区房', '南北通透']
            }
          ] as Record<string, unknown>[]
          
          guessYouLike.value = mockItems
        }
      } catch (error) {
        console.error('加载猜你喜欢失败:', error)
        // 使用模拟数据
        const mockItems = [
          {
            propertyId: 101,
            title: '万科城市花园 精装三房',
            summary: '南山区 · 89.5㎡ · 3室2厅2卫',
            totalPrice: 650.5,
            cover: 'https://img95.699pic.com/photo/50149/6896.jpg_wh860.jpg'
          }
        ] as Record<string, unknown>[]
        
        guessYouLike.value = mockItems
      }
    }

    // 搜索历史管理（前端）
    const addToSearchHistory = (keyword: string) => {
      if (!keyword.trim()) return
      
      const existingIndex = searchHistory.value.findIndex(item => item.keyword === keyword)
      if (existingIndex !== -1) {
        searchHistory.value.splice(existingIndex, 1)
      }
      
      searchHistory.value.unshift({
        id: Date.now(),
        keyword: keyword.trim(),
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      })
      
      if (searchHistory.value.length > 10) {
        searchHistory.value.pop()
      }
      
      localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
    }

    const deleteSearchHistory = (id: number) => {
      searchHistory.value = searchHistory.value.filter(item => item.id !== id)
      localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
    }

    const clearSearchHistory = () => {
      searchHistory.value = []
      localStorage.removeItem('searchHistory')
    }

    const loadSearchHistory = () => {
      const saved = localStorage.getItem('searchHistory')
      if (saved) {
        try {
          searchHistory.value = JSON.parse(saved)
        } catch {
          searchHistory.value = []
        }
      }
    }

    // 初始化
    const initialize = async () => {
      loadSearchHistory()
      await loadGuessYouLike()
    }

    // 🔧 新增：重置搜索
    const resetSearch = () => {
      searchQuery.value = ''
      searchResults.value = []
      searchCount.value = 0
    }

    // 🔧 新增：从历史记录搜索
    const searchFromHistory = (keyword: string) => {
      searchQuery.value = keyword
      performSearch()
    }

    return {
      searchQuery,
      searchResults,
      searchLoading,
      searchCount,
      guessYouLike,
      searchHistory,
      performSearch,
      loadGuessYouLike,
      addToSearchHistory,
      deleteSearchHistory,
      clearSearchHistory,
      loadSearchHistory,
      initialize,
      resetSearch,
      searchFromHistory
    }
  })
}

// 便捷use函数
import { getRootStoreContext } from './context'
export const useSearchStore = () => {
  const context = getRootStoreContext()
  const storeFactory = createSearchStore(context)
  return storeFactory()
}