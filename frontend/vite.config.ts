import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueJsx(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  // ===================================
  // 新增的解决 EACCES 错误的配置
  // ===================================
  server: {
    host: '0.0.0.0', // 绑定到所有网络接口，允许外部访问（Docker 容器需要）
    port: 5173        // 可选：保持默认端口
  }
})
