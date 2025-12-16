import api from './index'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  HomeOverview,
  ProfileDetailResponse,  // 注意：这里应该是 ProfileDetailResponse
  PropertyCardList
} from '../types/api.types'

export const authAPI = {
  // 1.1 用户登录
  login(data: LoginRequest): Promise<LoginResponse> {
    return api.post('/login', data)
  },

  // 1.2 用户注册
  register(data: RegisterRequest): Promise<RegisterResponse> {
    return api.post('/login/register', data)
  },

  // 2.1 首页概览
  getHomeOverview(): Promise<HomeOverview> {
    return api.get('/home/overview')
  },

  // 2.2 我的页面信息
  getMyProfile(userId: number): Promise<ProfileDetailResponse> {  // 修正返回类型
    return api.get('/home/me', { params: { userId } })
  },

  // 2.3 猜你喜欢列表
  getGuessYouLike(): Promise<PropertyCardList> {
    return api.get('/home/guess-you-like')
  },

  // 2.4 查询页入口推荐
  getQueryRecommendations(): Promise<PropertyCardList> {
    return api.get('/home/go-query')
  },

  logout(): Promise<{ message: string }> {
    return api.post('/logout')
  }
}
