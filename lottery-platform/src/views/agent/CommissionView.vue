<template>
  <div class="page">
    <div class="top-section">
      <div class="page-title">佣金统计</div>

      <div class="total-card">
        <div class="total-label">累计佣金</div>
        <div class="total-value">¥{{ totalCommission }}</div>
      </div>

      <div class="year-bar">
        <span v-for="y in years" :key="y" :class="['year-tab', { active: year === y }]" @click="year = y; loadData()">{{ y }}年</span>
      </div>
    </div>

    <div class="month-list">
      <div v-for="item in list" :key="item.month" class="month-card">
        <span class="month-label">{{ item.month }}月</span>
        <span class="month-amount">¥{{ item.amount }}</span>
      </div>
      <van-empty v-if="!list.length" description="暂无佣金记录" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showToast } from 'vant'
import { agentApi } from '@/api'

const year = ref(new Date().getFullYear())
const years = Array.from({ length: 3 }, (_, i) => new Date().getFullYear() - i)
const list = ref([])
const totalCommission = ref(0)

async function loadData() {
  try {
    const res = await agentApi.getCommission({ year: year.value })
    list.value = res.data.list
    totalCommission.value = res.data.total
  } catch (e) { showToast(e.message || '操作失败') }
}

onMounted(loadData)
</script>

<style scoped>
.page { color: #fff; display: flex; flex-direction: column; height: calc(100vh - 50px); }
.top-section { flex-shrink: 0; padding: 16px 16px 0; }
.page-title { font-size: 18px; font-weight: 600; margin-bottom: 16px; }

.total-card {
  background: linear-gradient(135deg, #f0c040, #d4a017);
  border-radius: 12px; padding: 20px; text-align: center; margin-bottom: 20px; color: #111;
}
.total-label { font-size: 13px; opacity: 0.7; }
.total-value { font-size: 32px; font-weight: 700; margin-top: 4px; }

.year-bar { display: flex; gap: 10px; margin-bottom: 16px; }
.year-tab {
  padding: 6px 16px; border-radius: 20px; background: #1e1e1e;
  font-size: 13px; color: #aaa; cursor: pointer;
}
.year-tab.active { background: #f0c040; color: #111; font-weight: 600; }

.month-list { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; padding: 0 16px 16px; }
.month-card {
  background: #1e1e1e; border-radius: 10px; padding: 14px 16px;
  display: flex; justify-content: space-between; align-items: center; flex-shrink: 0;
}
.month-label { font-size: 15px; }
.month-amount { font-size: 16px; font-weight: 600; color: #f0c040; }
</style>
