<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth.store'

// 初始化 auth store（从 localStorage 恢复登录状态）
const authStore = useAuthStore()

onMounted(() => {
  authStore.initialize()
  console.log('✅ Auth Store 已初始化，登录状态:', authStore.isLoggedIn)
})
</script>

<template>
  <div id="app">
    <router-view></router-view>
  </div>
</template>

<style scoped>
/* 样式保持不变 */
</style>
