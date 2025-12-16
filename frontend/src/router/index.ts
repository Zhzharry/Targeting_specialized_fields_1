import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'Home',
      redirect: '/tools', // 根路径重定向到主页
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/LoginPage.vue'),
    },
    {
      path: '/tools',
      name: 'Tools',
      component: () => import('../views/ToolsPage.vue'),
      meta: { requiresAuth: true } // 需要登录
    },
    {
      path: '/search',
      name: 'Search',
      component: () => import('../views/SearchPage.vue'),
      meta: { requiresAuth: true } // 需要登录
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('../views/ProfilePage.vue'),
      meta: { requiresAuth: true } // 需要登录
    },
  ],
})

// 添加路由守卫
router.beforeEach((to, from, next) => {
  const isAuthenticated = localStorage.getItem('token') !== null

  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login')
  } else {
    next()
  }
})

export default router
