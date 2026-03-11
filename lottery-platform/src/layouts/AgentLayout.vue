<template>
  <div class="agent-layout" v-if="isMobile">
    <router-view />
    <van-tabbar route>
      <van-tabbar-item icon="chart-trending-o" to="/agent/dashboard">概览</van-tabbar-item>
      <van-tabbar-item icon="friends-o" to="/agent/players">团队</van-tabbar-item>
      <van-tabbar-item icon="gold-coin-o" to="/agent/commission">佣金</van-tabbar-item>
      <van-tabbar-item icon="user-o" to="/agent/profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
  <DesktopSidebar v-else title="代理中心" :menu-groups="menuGroups">
    <router-view />
  </DesktopSidebar>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/userStore'
import { agentApi } from '@/api'
import { showToast } from 'vant'
import { useIsMobile } from '@/composables/useIsMobile'
import DesktopSidebar from '@/components/DesktopSidebar.vue'
import { DataBoard, User, Money } from '@element-plus/icons-vue'

const { isMobile } = useIsMobile()
const userStore = useUserStore()

const menuGroups = [
  {
    label: '代理中心',
    items: [
      { path: '/agent/dashboard', title: '数据概览', icon: DataBoard },
      { path: '/agent/players', title: '旗下玩家', icon: User },
      { path: '/agent/commission', title: '佣金统计', icon: Money },
      { path: '/agent/profile', title: '我的', icon: User },
    ]
  }
]

onMounted(async () => {
  try {
    const res = await agentApi.getInfo()
    userStore.updateUserInfo(res.data)
  } catch (e) {
    showToast(e.message || '获取用户信息失败')
  }
})
</script>

<style scoped>
.agent-layout { min-height: 100vh; padding-bottom: 50px; background: #111; }
</style>
