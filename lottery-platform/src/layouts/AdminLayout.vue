<template>
  <el-container class="admin-layout">
    <!-- 桌面端侧边栏 -->
    <el-aside v-if="!isMobile" width="220px" class="sidebar">
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
      >
        <template v-if="role === 'agent'">
          <div class="menu-group">代理中心</div>
          <el-menu-item index="/agent/dashboard"><el-icon><DataBoard /></el-icon><span>数据概览</span></el-menu-item>
          <el-menu-item index="/agent/players"><el-icon><User /></el-icon><span>旗下玩家</span></el-menu-item>
          <el-menu-item index="/agent/commission"><el-icon><Money /></el-icon><span>佣金统计</span></el-menu-item>
        </template>
        <template v-if="role === 'admin'">
          <div class="menu-group">数据</div>
          <el-menu-item index="/admin/dashboard"><el-icon><DataBoard /></el-icon><span>数据概览</span></el-menu-item>
          <div class="menu-group">运营管理</div>
          <el-menu-item index="/admin/players"><el-icon><User /></el-icon><span>玩家管理</span></el-menu-item>
          <el-menu-item index="/admin/prizes"><el-icon><Present /></el-icon><span>奖品管理</span></el-menu-item>
          <el-menu-item index="/admin/recharge">
            <el-icon><CreditCard /></el-icon>
            <span>充值管理</span>
            <span v-if="pendingRecharge" class="badge">{{ pendingRecharge }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/orders">
            <el-icon><Box /></el-icon>
            <span>发货订单</span>
            <span v-if="pendingDelivery" class="badge">{{ pendingDelivery }}</span>
          </el-menu-item>
          <el-menu-item index="/admin/after-sale">
            <el-icon><Service /></el-icon>
            <span>售后管理</span>
            <span v-if="pendingAfterSale" class="badge">{{ pendingAfterSale }}</span>
          </el-menu-item>
          <div class="menu-group">代理 & 市场</div>
          <el-menu-item index="/admin/agents"><el-icon><UserFilled /></el-icon><span>代理管理</span></el-menu-item>
          <el-menu-item index="/admin/market"><el-icon><Shop /></el-icon><span>转售市场</span></el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 移动端抽屉侧边栏 -->
    <el-drawer
      v-if="isMobile"
      v-model="drawerOpen"
      direction="ltr"
      :size="260"
      :with-header="false"
      :z-index="2000"
    >
      <div class="drawer-sidebar">
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
          @select="drawerOpen = false"
        >
          <template v-if="role === 'agent'">
            <div class="menu-group">代理中心</div>
            <el-menu-item index="/agent/dashboard"><el-icon><DataBoard /></el-icon><span>数据概览</span></el-menu-item>
            <el-menu-item index="/agent/players"><el-icon><User /></el-icon><span>旗下玩家</span></el-menu-item>
            <el-menu-item index="/agent/commission"><el-icon><Money /></el-icon><span>佣金统计</span></el-menu-item>
          </template>
          <template v-if="role === 'admin'">
            <div class="menu-group">数据</div>
            <el-menu-item index="/admin/dashboard"><el-icon><DataBoard /></el-icon><span>数据概览</span></el-menu-item>
            <div class="menu-group">运营管理</div>
            <el-menu-item index="/admin/players"><el-icon><User /></el-icon><span>玩家管理</span></el-menu-item>
            <el-menu-item index="/admin/prizes"><el-icon><Present /></el-icon><span>奖品管理</span></el-menu-item>
            <el-menu-item index="/admin/recharge">
              <el-icon><CreditCard /></el-icon>
              <span>充值管理</span>
              <span v-if="pendingRecharge" class="badge">{{ pendingRecharge }}</span>
            </el-menu-item>
            <el-menu-item index="/admin/orders">
              <el-icon><Box /></el-icon>
              <span>发货订单</span>
              <span v-if="pendingDelivery" class="badge">{{ pendingDelivery }}</span>
            </el-menu-item>
            <div class="menu-group">代理 & 市场</div>
            <el-menu-item index="/admin/agents"><el-icon><UserFilled /></el-icon><span>代理管理</span></el-menu-item>
            <el-menu-item index="/admin/market"><el-icon><Shop /></el-icon><span>转售市场</span></el-menu-item>
          </template>
        </el-menu>
      </div>
    </el-drawer>

    <el-container>
      <el-header class="topbar">
        <div class="topbar-left">
          <el-icon v-if="isMobile" class="menu-trigger" @click="drawerOpen = true"><Expand /></el-icon>
          <span class="page-title">{{ role === 'admin' ? '管理后台' : '代理后台' }}</span>
        </div>
        <div class="topbar-right">
          <span class="user-name">{{ userInfo?.nickname }}</span>
          <el-button type="danger" text size="small" @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main-content"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, provide, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/userStore'
import { adminApi } from '@/api'
import { useIsMobile } from '@/composables/useIsMobile'

const { isMobile } = useIsMobile()
const drawerOpen = ref(false)

const router = useRouter()
const userStore = useUserStore()
const role = computed(() => userStore.role)
const userInfo = computed(() => userStore.userInfo)
function handleLogout() { userStore.logout(); router.push('/login') }

// 待处理数量
const pendingRecharge = ref(0)
const pendingDelivery = ref(0)
const pendingAfterSale = ref(0)

async function loadPendingCounts() {
  if (role.value !== 'admin') return
  try {
    const res = await adminApi.getDashboard()
    pendingRecharge.value = res.data.pendingRecharge || 0
    pendingDelivery.value = res.data.pendingDelivery || 0
    pendingAfterSale.value = res.data.pendingAfterSale || 0
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

// 暴露给子页面，操作后立即刷新红点
provide('refreshPendingCounts', loadPendingCounts)

let timer = null
onMounted(() => {
  loadPendingCounts()
  timer = setInterval(loadPendingCounts, 30000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  background: #0d1117;
  border-right: 1px solid #1e2633;
  overflow-y: auto;
}

.drawer-sidebar {
  height: 100%;
  background: #0d1117;
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
  gap: 12px;
}

.menu-trigger {
  font-size: 22px;
  cursor: pointer;
  color: #333;
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
  background: #f5f7fa;
  overflow-y: auto;
  padding: 24px;
}

.badge {
  margin-left: auto;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #e55;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  text-align: center;
}

/* 移动端抽屉样式 */
:deep(.el-drawer__body) {
  padding: 0;
  background: #0d1117;
}

/* 移动端适配 */
@media (max-width: 767px) {
  .topbar {
    padding: 0 16px;
    height: 48px;
  }
  .main-content {
    padding: 12px;
  }
}
</style>
