<template>
  <el-container class="desktop-layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <span class="logo-icon">🎰</span>
        <span class="logo-text">抽奖平台</span>
      </div>
      <el-menu
        :router="true"
        :default-active="$route.path"
        background-color="#0d1117"
        text-color="rgba(255,255,255,0.65)"
        active-text-color="#f0c040"
        :collapse="false"
      >
        <template v-for="group in menuGroups" :key="group.label">
          <div class="menu-group">{{ group.label }}</div>
          <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <div class="topbar-left">
          <span class="page-title">{{ title }}</span>
        </div>
        <div class="topbar-right">
          <span class="user-name">{{ userInfo?.nickname }}</span>
          <el-button type="danger" text size="small" @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main-content"><slot /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'

defineProps({
  title: { type: String, default: '' },
  menuGroups: { type: Array, default: () => [] }
})

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.desktop-layout {
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  background: #0d1117;
  border-right: 1px solid #1e2633;
  overflow-y: auto;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 24px 16px;
  border-bottom: 1px solid #1e2633;
}

.logo-icon {
  font-size: 28px;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #f0c040;
  letter-spacing: 2px;
  white-space: nowrap;
}

.menu-group {
  padding: 20px 20px 8px;
  font-size: 12px;
  color: rgba(255,255,255,0.3);
  text-transform: uppercase;
  letter-spacing: 1px;
  font-weight: 600;
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  margin: 2px 8px;
  border-radius: 8px;
  font-size: 14px;
}

:deep(.el-menu-item:hover) {
  background: rgba(255,255,255,0.06) !important;
}

:deep(.el-menu-item.is-active) {
  background: rgba(240, 192, 64, 0.12) !important;
  color: #f0c040 !important;
}

:deep(.el-menu-item .el-icon) {
  font-size: 18px;
  margin-right: 8px;
  color: inherit;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #eef0f3;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.topbar-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 14px;
  color: #666;
}

.main-content {
  background: #111;
  overflow-y: auto;
  padding: 24px;
}

/* 子视图内容限宽居中，桌面端不拉伸全屏 */
:deep(.main-content > *) {
  max-width: 600px;
  margin: 0 auto;
}

:deep(.main-content .page) {
  height: auto;
  padding-bottom: 24px;
}

/* 桌面端隐藏 Vant 相关的固定定位元素 */
:deep(.van-tabbar) {
  display: none !important;
}
</style>
