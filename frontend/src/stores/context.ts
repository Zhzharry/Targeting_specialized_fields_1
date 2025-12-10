import type { authAPI } from '../api/auth.api'
import type { profileAPI } from '../api/profile.api'
import type { queryAPI } from '../api/query.api'

export interface RootStoreContext {
  api: {
    auth: typeof authAPI
    profile: typeof profileAPI
    query: typeof queryAPI
  }
}

// 创建全局上下文容器
let rootContext: RootStoreContext | null = null

// 设置和获取上下文的方法
export function setRootStoreContext(context: RootStoreContext): void {
  rootContext = context
}

export function getRootStoreContext(): RootStoreContext {
  if (!rootContext) {
    throw new Error('RootStoreContext 未初始化！请在 main.ts 中调用 setRootStoreContext')
  }
  return rootContext
}
