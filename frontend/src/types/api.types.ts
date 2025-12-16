// types/api.types.ts

// ==================== 登录模块 ====================
export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  userId: number
  username: string
  userProfile: string  // JSON字符串，实际是UserProfile对象
  message: string
}

export interface RegisterRequest {
  username: string
  password: string
  phone_number: string
  user_profile?: UserProfile
}

export interface RegisterResponse {
  userId: number
  username: string
  message: string
}

// ==================== 用户资料 ====================
export interface BudgetRange {
  min: number
  max?: number
}

export interface UserProfile {
  budget?: BudgetRange
  preferred_locations?: string[]
}

export interface UserPreference {
  house_types?: string[]
  orientations?: string[]
  // 根据接口文档的可用偏好字段
  price_range?: { min: number; max: number }
  area_range?: { min: number; max: number }
  locations?: string[]
  districts?: string[]
  city?: string
  bedroom_range?: { min: number; max: number }
  decorations?: string[]
  keywords?: string[]
}

// ==================== 主页模块 ====================
export interface HomeOverview {
  title: string
  welcomeMessage: string
  notifications: number
  shortcuts: string[]
}

export interface UserStats {
  favorites: number
  browsed: number
  recommendations: number
}

export interface UserProfileDetail {
  userId: number
  username: string
  joinedAt: string
  userProfile: UserProfile
  preferences: UserPreference
  stats: UserStats
}

export interface ProfileDetailResponse {
  profile: UserProfileDetail
  message: string
}

// ==================== 房源卡片 ====================
export interface PropertyCard {
  propertyId: number
  title: string
  summary: string
  totalPrice: number
  cover: string
  detailUrl: string
  tags: string[]
}

export interface PropertyCardList {
  items: PropertyCard[]
  message: string
}

// ==================== 查询模块 ====================
export interface QueryParams {
  keyword?: string
  district?: string
  minPrice?: number
  maxPrice?: number
  propertyType?: string
  orientation?: string
  status?: 'for_sale' | 'sold'
  minBedrooms?: number
  maxBedrooms?: number
  minArea?: number
  maxArea?: number
  minViewCount?: number
  maxViewCount?: number
}

export interface PriceInfo {
  total_price: number
  unit_price: number
}

export interface LayoutInfo {
  bedroom_count: number
  living_room_count: number
  bathroom_count: number
  area: number
}

export interface BasicInfo {
  property_type: string
  build_year: number
}

export interface LocationInfo {
  province: string
  city: string
  district: string
}

export interface PropertyDetail {
  propertyId: number
  title: string
  status: 'for_sale' | 'sold'
  communityName: string
  viewCount: number
  favoriteCount: number
  updatedAt: string
  priceInfo: PriceInfo
  layoutInfo: LayoutInfo
  basicInfo: BasicInfo
  locationInfo: LocationInfo
}

export interface QueryResponse {
  items: PropertyDetail[]
  count: number
  message: string
}

export interface FavoriteRequest {
  userId: number
  propertyId: number
}

export interface FavoriteResponse {
  message: string
  userId?: number
  propertyId?: number
}

// ==================== 我的页面模块 ====================
export interface PreferenceData {
  price_range?: { min: number; max: number }
  area_range?: { min: number; max: number }
  locations?: string[]
  districts?: string[]
  city?: string
  house_types?: string[]
  bedroom_range?: { min: number; max: number }
  orientations?: string[]
  decorations?: string[]
  keywords?: string[]
  budget?: BudgetRange
  main_districts?: string[]
}

export interface PreferenceRequest {
  userId: number
  preferenceData: PreferenceData
}

export interface PreferenceResponse {
  message: string
  userId?: number
}

// 房价预测相关类型
export interface PricePredictionFeatures {
  经度?: number
  纬度?: number
  到市中心距离_km?: number
  面积?: number
  室数?: number
  厅数?: number
  卫数?: number
  房龄?: number
  朝向评分?: number
  area_encoded?: number
  小区面积_数值?: number
  小区户数_数值?: number
  容积率_数值?: number
  绿化率_数值?: number
  物业费_数值?: number
}

export interface PricePredictionRequest {
  city: string
  features: PricePredictionFeatures
}

export interface PricePredictionResponse {
  city: string
  features: PricePredictionFeatures
  predictedPricePerSquareMeter: number
  unit: string
  message: string
}

// 历史记录相关
export interface BehaviorData {
  duration: number
  device: string
}

export interface SimplePriceInfo {
  total_price: number
  unit_price: number
}

export interface SimpleLayoutInfo {
  bedroom_count: number
  area: number
}

export interface BrowsingHistoryItem {
  historyId: number
  propertyId: number
  title: string
  behaviorData: BehaviorData
  priceInfo: SimplePriceInfo
  layoutInfo: SimpleLayoutInfo
  createdAt: string
}

export interface HistoryResponse {
  items: BrowsingHistoryItem[]
  count: number
  message: string
}

// 收藏列表相关
export interface FavoriteItem {
  favoriteId: number
  propertyId: number
  title: string
  priceInfo: SimplePriceInfo
  layoutInfo: SimpleLayoutInfo
  createdAt: string
}

export interface FavoritesResponse {
  items: FavoriteItem[]
  count: number
  message: string
}

// ==================== 通用响应 ====================
export interface ApiResponse<T = unknown> {
  message: string
  data?: T
  [key: string]: unknown
}

// ==================== 错误响应 ====================
export interface ErrorResponse {
  message: string
  error?: string
  userId?: number
  propertyId?: number
  timestamp?: string
  status?: number
  path?: string
}
