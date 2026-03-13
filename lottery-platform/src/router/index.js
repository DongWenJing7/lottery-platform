import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/userStore'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: () => import('@/views/auth/LoginView.vue'), meta: { guest: true } },
  { path: '/register', name: 'Register', component: () => import('@/views/auth/RegisterView.vue'), meta: { guest: true } },
  {
    path: '/player', component: () => import('@/layouts/PlayerLayout.vue'), meta: { role: 'player' },
    children: [
      { path: '', redirect: '/player/home' },
      { path: 'home', name: 'PlayerHome', component: () => import('@/views/player/HomeView.vue') },
      { path: 'lottery', name: 'Lottery', component: () => import('@/views/player/LotteryView.vue') },
      { path: 'recharge', name: 'Recharge', component: () => import('@/views/player/RechargeView.vue') },
      { path: 'warehouse', name: 'Warehouse', component: () => import('@/views/player/WarehouseView.vue') },
      { path: 'market', name: 'Market', component: () => import('@/views/player/MarketView.vue') },
      { path: 'orders', name: 'PlayerOrders', component: () => import('@/views/player/OrdersView.vue') },
      { path: 'friends', name: 'Friends', component: () => import('@/views/player/FriendsView.vue') },
      { path: 'chat', name: 'ChatList', component: () => import('@/views/player/ChatListView.vue') },
      { path: 'chat/:userId', name: 'Chat', component: () => import('@/views/player/ChatView.vue') },
      { path: 'profile', name: 'Profile', component: () => import('@/views/player/ProfileView.vue') },
    ]
  },
  {
    path: '/agent', component: () => import('@/layouts/AgentLayout.vue'), meta: { role: 'agent' },
    children: [
      { path: '', redirect: '/agent/dashboard' },
      { path: 'dashboard', name: 'AgentDashboard', component: () => import('@/views/agent/DashboardView.vue') },
      { path: 'players', name: 'AgentPlayers', component: () => import('@/views/agent/PlayersView.vue') },
      { path: 'commission', name: 'AgentCommission', component: () => import('@/views/agent/CommissionView.vue') },
      { path: 'profile', name: 'AgentProfile', component: () => import('@/views/agent/ProfileView.vue') },
    ]
  },
  {
    path: '/admin', component: () => import('@/layouts/AdminLayout.vue'), meta: { role: 'admin' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/DashboardView.vue') },
      { path: 'players', name: 'AdminPlayers', component: () => import('@/views/admin/PlayersView.vue') },
      { path: 'prizes', name: 'AdminPrizes', component: () => import('@/views/admin/PrizesView.vue') },
      { path: 'recharge', name: 'AdminRecharge', component: () => import('@/views/admin/RechargeView.vue') },
      { path: 'orders', name: 'AdminOrders', component: () => import('@/views/admin/OrdersView.vue') },
      { path: 'agents', name: 'AdminAgents', component: () => import('@/views/admin/AgentsView.vue') },
      { path: 'market', name: 'AdminMarket', component: () => import('@/views/admin/MarketView.vue') },
      { path: 'after-sale', name: 'AdminAfterSale', component: () => import('@/views/admin/AfterSaleView.vue') },
    ]
  },
  { path: '/:pathMatch(.*)*', component: () => import('@/views/auth/NotFoundView.vue') }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const { token, role } = userStore
  if (!token && !to.meta.guest) return next('/login')
  if (token && to.meta.guest) return next(roleHome(role))
  if (token && to.meta.role && to.meta.role !== role) return next(roleHome(role))
  next()
})

function roleHome(role) {
  return { player: '/player/home', agent: '/agent/dashboard', admin: '/admin/dashboard' }[role] || '/login'
}

export default router
