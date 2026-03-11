<template>
  <div class="page">
    <div class="top-section">
      <div class="page-title">发货订单</div>
      <div class="tabs">
        <span v-for="t in tabs" :key="t.value" :class="['tab', { active: tab === t.value }]" @click="tab = t.value; loadData()">{{ t.label }}</span>
      </div>
    </div>
    <div class="order-list">
      <div v-for="order in list" :key="order.id" class="order-card">
        <div class="order-top">
          <span class="order-no">{{ order.orderNo }}</span>
          <van-tag :type="statusType(order.status)">{{ statusText(order.status) }}</van-tag>
        </div>
        <div class="order-prize">{{ order.prize?.name }}</div>
        <div class="order-info">{{ order.receiverName }} {{ order.receiverPhone }}</div>
        <div class="order-info">{{ order.receiverAddress }}</div>
        <div class="order-time">{{ order.createdAt }}</div>
      </div>
      <van-empty v-if="!list.length" description="暂无订单" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showToast } from 'vant'
import { playerApi } from '@/api'

const list = ref([])
const tab = ref('')
const tabs = [
  { label: '全部', value: '' },
  { label: '待发货', value: 'pending' },
  { label: '已发货', value: 'shipped' },
  { label: '已完成', value: 'done' },
]

function statusType(s) { return { pending: 'warning', shipped: 'primary', done: 'success' }[s] || 'default' }
function statusText(s) { return { pending: '待发货', shipped: '已发货', done: '已完成' }[s] || s }

async function loadData() {
  try {
    const res = await playerApi.getOrders({ page: 1, size: 50, status: tab.value || undefined })
    list.value = res.data.list
  } catch (e) { showToast(e.message || '操作失败') }
}

onMounted(loadData)
</script>

<style scoped>
.page { color: #fff; display: flex; flex-direction: column; height: calc(100vh - 50px); }
.top-section { flex-shrink: 0; padding: 16px 16px 0; }
.page-title { font-size: 18px; font-weight: 600; margin-bottom: 12px; }
.tabs { display: flex; gap: 10px; margin-bottom: 16px; }
.tab { padding: 6px 16px; border-radius: 20px; background: #1e1e1e; font-size: 13px; color: #aaa; cursor: pointer; }
.tab.active { background: #f0c040; color: #111; font-weight: 600; }
.order-list { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; padding: 0 16px 16px; }
.order-card { background: #1e1e1e; border-radius: 10px; padding: 14px 16px; flex-shrink: 0; }
.order-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.order-no { font-size: 12px; color: #666; }
.order-prize { font-size: 15px; font-weight: 500; margin-bottom: 4px; }
.order-info { font-size: 13px; color: #aaa; }
.order-time { font-size: 12px; color: #444; margin-top: 6px; }
</style>
