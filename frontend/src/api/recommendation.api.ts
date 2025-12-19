import api from './index'

// 推荐接口相关类型
export interface OthersAlsoViewedParams {
  userId: number
  limit?: number
  excludeViewed?: boolean
  useCache?: boolean
}

export interface RecommendationItem {
  propertyId: number
  title: string
  summary: string
  score: number
  source: string
  cover: string
  totalPrice?: number
  tags?: string[]
  recommendationScore?: number
}

export interface OthersAlsoViewedResponse {
  items: RecommendationItem[]
  count: number
  userId: number
  dataSource: string
  lastHadoopUpdate?: string
  message: string
}

export const recommendationAPI = {
  /**
   * 获取"其他用户也在看"推荐
   * @param params 推荐参数
   * @returns 推荐房源列表
   */
  getOthersAlsoViewed(params: OthersAlsoViewedParams): Promise<OthersAlsoViewedResponse> {
    return api.get('/recommendation/others-also-viewed', { params })
  }
}
