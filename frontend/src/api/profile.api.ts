import api from './index'
import type {
  PreferenceRequest,
  PreferenceResponse,
  PricePredictionRequest,
  PricePredictionResponse,
  HistoryResponse,
  FavoritesResponse
} from '../types/api.types'

export const profileAPI = {
  // 4.1 设置偏好
  setPreferences(data: PreferenceRequest): Promise<PreferenceResponse> {
    return api.post('/profile/preferences', data)
  },

  // 4.2 房价预测
  predictPrice(data: PricePredictionRequest): Promise<PricePredictionResponse> {
    return api.post('/profile/price-predict', data)
  },

  // 4.3 历史记录
  getHistory(userId: number): Promise<HistoryResponse> {
    return api.get('/profile/history', { params: { userId } })
  },

  // 4.4 收藏列表
  getFavorites(userId: number): Promise<FavoritesResponse> {
    return api.get('/profile/favorites', { params: { userId } })
  }
}
