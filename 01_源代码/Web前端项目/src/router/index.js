import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: () => import('../views/Login.vue') },
  { 
    path: '/student', 
    component: () => import('../views/Layout.vue'),
    redirect: '/student/rooms', // 关键：进入 /student 默认跳到查座页
    children: [
      { path: 'rooms', component: () => import('../views/student/RoomList.vue') },
      { path: 'orders', component: () => import('../views/student/MyOrders.vue') }
    ]
  },
  { 
    path: '/admin', 
    component: () => import('../views/Layout.vue'),
    redirect: '/admin/seats', // 关键：进入 /admin 默认跳到管理页
    children: [
      { path: 'seats', component: () => import('../views/admin/SeatManage.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.path !== '/login' && !localStorage.getItem('token')) {
    next('/login')
  } else {
    next()
  }
})

export default router