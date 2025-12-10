import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

// 1. 导入所有API服务
import { authAPI } from './api/auth.api'
import { profileAPI } from './api/profile.api'
import { queryAPI } from './api/query.api'

// 2. 导入上下文设置函数
import { setRootStoreContext } from './stores/context'

// 3. 初始化并注入API依赖（必须在创建App之前！）
console.log('🚀 初始化Pinia API上下文...')
setRootStoreContext({
  api: {
    auth: authAPI,
    profile: profileAPI,
    query: queryAPI
  }
})
console.log('✅ Pinia API上下文初始化完成')

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

app.mount('#app')
