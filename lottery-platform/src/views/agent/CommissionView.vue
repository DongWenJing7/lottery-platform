<template>
  <div class="page">
    <div class="top-section">
      <div class="page-title">佣金统计</div>

      <div class="total-card">
        <div class="card-row">
          <div class="card-main">
            <div class="total-label">累计佣金</div>
            <div class="total-value">¥{{ totalCommission }}</div>
          </div>
          <div class="card-side">
            <span class="level-badge">Lv.{{ level }}</span>
            <span class="rate-text">佣金率 {{ commissionRate }}%</span>
          </div>
        </div>
      </div>

      <div class="year-bar">
        <span v-for="y in years" :key="y" :class="['year-tab', { active: year === y }]" @click="year = y; loadData()">{{ y }}年</span>
      </div>
    </div>

    <div class="month-list">
      <div v-for="item in list" :key="item.month" class="month-card">
        <div class="month-header" @click="toggle('month', item.month)">
          <div class="month-left">
            <span class="month-label">{{ item.month }}月</span>
            <span class="month-recharge">充值 ¥{{ item.rechargeAmount }}</span>
          </div>
          <div class="month-right">
            <span class="month-commission">佣金 ¥{{ item.commission }}</span>
            <span :class="['arrow', { open: expanded.month === item.month }]">›</span>
          </div>
        </div>

        <div v-if="expanded.month === item.month" class="detail-list">
          <div v-for="d in item.details" :key="d.playerId" class="player-item">
            <div class="player-header" @click.stop="toggle('player', d.playerId)">
              <div class="player-left">
                <span class="avatar-circle">{{ (d.nickname || '?').charAt(0) }}</span>
                <span class="player-name">{{ d.nickname }}</span>
              </div>
              <div class="player-right">
                <span class="player-stat">充值 ¥{{ d.rechargeAmount }}</span>
                <span class="player-stat gold">佣金 ¥{{ d.commission }}</span>
                <span :class="['arrow', { open: expanded.player === d.playerId }]">›</span>
              </div>
            </div>

            <div v-if="expanded.player === d.playerId" class="order-list">
              <div v-for="o in d.orders" :key="o.orderNo" class="order-item">
                <div class="order-top">
                  <span class="order-no">{{ o.orderNo }}</span>
                  <span class="order-commission">+¥{{ o.commission }}</span>
                </div>
                <div class="order-bottom">
                  <span>{{ o.tokens }}代币 / ¥{{ o.amount }}</span>
                  <span class="order-time">{{ formatTime(o.createdAt) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
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
const years = ref([new Date().getFullYear()])
const list = ref([])
const totalCommission = ref(0)
const commissionRate = ref(0)
const level = ref(0)
const expanded = ref({ month: null, player: null })

function toggle(type, id) {
  if (type === 'month') {
    expanded.value = { month: expanded.value.month === id ? null : id, player: null }
  } else {
    expanded.value = { ...expanded.value, player: expanded.value.player === id ? null : id }
  }
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

async function loadData() {
  try {
    const res = await agentApi.getCommission({ year: year.value })
    list.value = res.data.list
    totalCommission.value = res.data.total
    commissionRate.value = res.data.commissionRate ?? 0
    level.value = res.data.level ?? 0
    // 根据后端返回的最早年份生成年份列表
    const startYear = res.data.startYear || new Date().getFullYear()
    const currentYear = new Date().getFullYear()
    const arr = []
    for (let y = currentYear; y >= startYear; y--) arr.push(y)
    years.value = arr
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
  border-radius: 12px; padding: 20px; margin-bottom: 20px; color: #111;
}
.card-row { display: flex; justify-content: space-between; align-items: center; }
.total-label { font-size: 13px; opacity: 0.7; }
.total-value { font-size: 32px; font-weight: 700; margin-top: 4px; }
.card-side { display: flex; flex-direction: column; align-items: flex-end; gap: 6px; }
.level-badge {
  background: rgba(0,0,0,0.2); padding: 2px 10px; border-radius: 10px;
  font-size: 13px; font-weight: 600;
}
.rate-text { font-size: 12px; opacity: 0.8; }

.year-bar { display: flex; gap: 10px; margin-bottom: 16px; }
.year-tab {
  padding: 6px 16px; border-radius: 20px; background: #1e1e1e;
  font-size: 13px; color: #aaa; cursor: pointer;
}
.year-tab.active { background: #f0c040; color: #111; font-weight: 600; }

.month-list { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; padding: 0 16px 16px; }

.month-card { background: #1e1e1e; border-radius: 10px; overflow: hidden; flex-shrink: 0; }
.month-header {
  padding: 14px 16px; display: flex; justify-content: space-between; align-items: center; cursor: pointer;
}
.month-left { display: flex; align-items: center; gap: 10px; }
.month-label { font-size: 15px; font-weight: 600; }
.month-recharge { font-size: 12px; color: #aaa; }
.month-right { display: flex; align-items: center; gap: 8px; }
.month-commission { font-size: 15px; font-weight: 600; color: #f0c040; }

.arrow {
  display: inline-block; font-size: 16px; color: #666;
  transition: transform 0.2s; transform: rotate(0deg);
}
.arrow.open { transform: rotate(90deg); }

.detail-list { padding: 0 12px 12px; }
.player-item { background: #282828; border-radius: 8px; margin-bottom: 6px; overflow: hidden; }
.player-header {
  padding: 10px 12px; display: flex; justify-content: space-between; align-items: center; cursor: pointer;
}
.player-left { display: flex; align-items: center; gap: 8px; }
.avatar-circle {
  width: 28px; height: 28px; border-radius: 50%; background: #f0c040; color: #111;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 600; flex-shrink: 0;
}
.player-name { font-size: 14px; }
.player-right { display: flex; align-items: center; gap: 8px; }
.player-stat { font-size: 12px; color: #aaa; }
.player-stat.gold { color: #f0c040; }

.order-list { padding: 0 12px 10px; }
.order-item {
  background: #1e1e1e; border-radius: 6px; padding: 10px 12px; margin-bottom: 4px;
}
.order-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.order-no { font-size: 12px; color: #aaa; word-break: break-all; }
.order-commission { font-size: 14px; font-weight: 600; color: #f0c040; white-space: nowrap; margin-left: 8px; }
.order-bottom { display: flex; justify-content: space-between; font-size: 12px; color: #888; }
.order-time { white-space: nowrap; }
</style>
