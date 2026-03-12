<template>
  <div class="page">
    <div class="top-section">
      <div class="header">
        <div class="greeting">你好，{{ userStore.userInfo?.nickname || '玩家' }}</div>
        <div class="balance-card">
          <div class="balance-label">我的代币</div>
          <div class="balance-value">{{ info.balance ?? 0 }}</div>
        </div>
      </div>
      <div class="actions">
        <div class="action-item" @click="$router.push('/player/lottery')">
          <div class="action-icon">🎰</div>
          <div class="action-text">去抽奖</div>
        </div>
        <div class="action-item" @click="$router.push('/player/recharge')">
          <div class="action-icon">💰</div>
          <div class="action-text">充值</div>
        </div>
        <div class="action-item" @click="$router.push('/player/warehouse')">
          <div class="action-icon">📦</div>
          <div class="action-text">仓库</div>
        </div>
        <div class="action-item" @click="$router.push('/player/market')">
          <div class="action-icon">🏪</div>
          <div class="action-text">市场</div>
        </div>
      </div>
      <div class="section-title">最近流水</div>
    </div>
    <div class="log-list">
      <div v-for="log in logs" :key="log.id" class="log-item">
        <div class="log-left">
          <div class="log-type">{{ typeMap[log.type] || log.type }}</div>
          <div class="log-remark">{{ log.remark }}</div>
        </div>
        <div :class="['log-amount', log.amount > 0 ? 'green' : 'red']">
          {{ log.amount > 0 ? '+' : '' }}{{ log.amount }}
        </div>
      </div>
      <van-empty v-if="!logs.length" description="暂无记录" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/userStore'
import { playerApi } from '@/api'

const userStore = useUserStore()
const info = ref({})
const logs = ref([])
const typeMap = { recharge: '充值', draw: '抽奖', draw_reward: '抽奖奖励', recycle: '回收', sell: '出售', buy: '购买', manual: '调整' }

onMounted(async () => {
  try {
    const res = await playerApi.getInfo()
    info.value = res.data
    userStore.updateBalance(res.data.balance)
  } catch (e) {
    showToast(e.message || '获取信息失败')
  }
  try {
    const res = await playerApi.getLogs({ page: 1, size: 10 })
    logs.value = res.data.list
  } catch (e) {
    showToast(e.message || '获取流水失败')
  }
})
</script>

<style scoped>
.page { color: #fff; display: flex; flex-direction: column; height: calc(100vh - 50px); }
.top-section { flex-shrink: 0; padding: 16px 16px 0; }
.header { margin-bottom: 20px; }
.greeting { font-size: 18px; font-weight: 600; margin-bottom: 16px; }
.balance-card { background: linear-gradient(135deg, #f0c040, #d4a017); border-radius: 12px; padding: 20px; color: #111; }
.balance-label { font-size: 13px; opacity: 0.8; }
.balance-value { font-size: 36px; font-weight: 700; margin-top: 4px; }
.actions { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 24px; }
.action-item { background: #1e1e1e; border-radius: 12px; padding: 16px 8px; text-align: center; cursor: pointer; }
.action-item:active { opacity: 0.7; }
.action-icon { font-size: 28px; margin-bottom: 6px; }
.action-text { font-size: 12px; color: #aaa; }
.section-title { font-size: 16px; font-weight: 600; margin-bottom: 12px; }
.log-list { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; padding: 0 16px 16px; }
.log-item { background: #1e1e1e; border-radius: 10px; padding: 12px 16px; display: flex; justify-content: space-between; align-items: center; flex-shrink: 0; }
.log-type { font-size: 14px; font-weight: 500; }
.log-remark { font-size: 12px; color: #666; margin-top: 2px; }
.log-amount { font-size: 16px; font-weight: 600; }
.green { color: #4caf50; }
.red { color: #e55; }
</style>
