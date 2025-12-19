import api from './index'
import type {
  QueryParams,
  QueryResponse,
  PropertyCardList,
  FavoriteResponse,
  PopularRecommendationResponse
} from '../types/api.types'

export const queryAPI = {
  // 3.1 房源查询 - 用于SearchPage.vue
  searchProperties(params: QueryParams): Promise<QueryResponse> {
    return api.get('/query', { params })
  },

  // 3.2 收藏房源
  addFavorite(userId: number, propertyId: number): Promise<FavoriteResponse> {
    return api.post('/query/favorite', null, {
      params: { userId, propertyId }
    })
  },

  // 3.3 取消收藏房源
  removeFavorite(userId: number, propertyId: number): Promise<FavoriteResponse> {
    return api.delete('/query/favorite', {
      params: { userId, propertyId }
    })
  },

  // 3.4 记录房源浏览
  recordBrowse(userId: number, propertyId: number, source?: 'history' | 'favorite' | 'recommendation'): Promise<{ message: string; userId: number; propertyId: number }> {
    const params: Record<string, string | number> = { userId, propertyId }
    if (source) {
      params.source = source
    }
    return api.post('/query/browse', null, { params })
  },

  // 2.3 猜你喜欢列表 - 这个其实应该放在auth.api.ts，因为属于主页模块
  // 但既然已经在queryAPI里了，先保持原样，或者可以移动到auth.api.ts
  getGuessYouLike(): Promise<PropertyCardList> {
    return api.get('/home/guess-you-like')
  },

  // 2.4 查询页入口推荐
  getQueryRecommendations(): Promise<PropertyCardList> {
    return api.get('/home/go-query')
  },

  // 2.4 热门推荐
  getPopularRecommendations(): Promise<PopularRecommendationResponse> {
    return api.get('/home/popular')
  }
}
