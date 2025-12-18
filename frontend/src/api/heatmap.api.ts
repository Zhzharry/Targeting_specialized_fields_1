import api from './index'

export interface HeatmapParams {
  city: string
  priceRange?: string
}

export interface HeatmapDataPoint {
  longitude: number
  latitude: number
  price: number
  propertyName?: string
  district?: string
}

export interface HeatmapResponse {
  success: boolean
  data: HeatmapDataPoint[]
  message?: string
}

export const heatmapAPI = {
  // 获取房价热力图数据
  getHeatmapData(params: HeatmapParams): Promise<HeatmapResponse> {
    return api.get('/heatmap/data', { params })
  }
}
