<template>
  <div class="player-layout" v-if="isMobile">
    <router-view />
    <van-tabbar route>
      <van-tabbar-item icon="home-o" to="/player/home">首页</van-tabbar-item>
      <van-tabbar-item icon="gift-o" to="/player/lottery">抽奖</van-tabbar-item>
      <van-tabbar-item icon="bag-o" to="/player/warehouse">仓库</van-tabbar-item>
      <van-tabbar-item icon="exchange" to="/player/market">市场</van-tabbar-item>
      <van-tabbar-item icon="user-o" to="/player/profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
  <DesktopSidebar v-else title="玩家中心" :menu-groups="menuGroups">
    <router-view />
  </DesktopSidebar>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/userStore'
import { playerApi } from '@/api'
import { showToast } from 'vant'
import { useIsMobile } from '@/composables/useIsMobile'
import DesktopSidebar from '@/components/DesktopSidebar.vue'
import { HomeFilled, Present, Box, Shop, Tickets, List, User } from '@element-plus/icons-vue'

const { isMobile } = useIsMobile()
const userStore = useUserStore()

const menuGroups = [
  {
    label: '游戏',
    items: [
      { path: '/player/home', title: '首页', icon: HomeFilled },
      { path: '/player/lottery', title: '抽奖', icon: Present },
    ]
  },
  {
    label: '资产',
    items: [
      { path: '/player/warehouse', title: '仓库', icon: Box },
      { path: '/player/market', title: '市场', icon: Shop },
    ]
  },
  {
    label: '账户',
    items: [
      { path: '/player/recharge', title: '充值记录', icon: Tickets },
      { path: '/player/orders', title: '发货订单', icon: List },
      { path: '/player/profile', title: '我的', icon: User },
    ]
  }
]

onMounted(async () => {
  try {
    const res = await playerApi.getInfo()
    userStore.updateUserInfo(res.data)
  } catch (e) {
    showToast(e.message || '获取用户信息失败')
  }
})
</script>

<style scoped>
.player-layout { min-height: 100vh; padding-bottom: 50px; background: #111; }
</style>
