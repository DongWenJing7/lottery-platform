<template>
  <div class="dashboard">
    <div class="page-header">
      <div>
        <h2>数据概览</h2>
        <p class="page-desc">{{ currentTime }}</p>
      </div>
    </div>

    <!-- 待处理 -->
    <div class="pending-bar" v-if="hasPending">
      <div class="pending-item" v-for="item in pendingCards" :key="item.label" @click="item.onClick?.()">
        <span class="pending-dot"></span>
        <span class="pending-label">{{ item.label }}</span>
        <span class="pending-count">{{ item.value }}</span>
      </div>
    </div>

    <!-- 累计数据 -->
    <div class="section-label">累计数据</div>
    <div class="stat-grid">
      <div class="stat-card" v-for="item in totalCards" :key="item.label">
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-value">{{ item.value }}</div>
      </div>
    </div>

    <!-- 今日数据 -->
    <div class="section-label">今日数据</div>
    <div class="today-grid">
      <div class="today-card" v-for="item in todayCards" :key="item.label">
        <div class="today-icon" :style="{ background: item.iconBg, color: item.iconColor }">
          <el-icon :size="22"><component :is="item.icon" /></el-icon>
        </div>
        <div class="today-info">
          <div class="today-value">{{ item.value }}</div>
          <div class="today-label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="chart-grid">
      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title">数据趋势</span>
          <div class="days-toggle">
            <span :class="{ active: chartDays === 7 }" @click="switchDays(7)">近7天</span>
            <span :class="{ active: chartDays === 30 }" @click="switchDays(30)">近30天</span>
          </div>
        </div>
        <div ref="trendChartRef" class="chart-container"></div>
      </div>
      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title">代理拉新排行</span>
          <span class="chart-sub">TOP 10</span>
        </div>
        <div ref="rankChartRef" class="chart-container"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'
import { User, Coin, ShoppingCart, Van, Wallet, Connection } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts/core'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const router = useRouter()
const data = ref({})
const chartDays = ref(7)
const trendChartRef = ref(null)
const rankChartRef = ref(null)
let trendChart = null
let rankChart = null

const currentTime = ref('')
let timer = null
function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
}

const totalCards = computed(() => [
  { label: '总玩家数', value: formatNum(data.value.totalUsers ?? 0) },
  { label: '总充值 (代币)', value: formatNum(data.value.totalRecharge ?? 0) },
  { label: '总代理数', value: formatNum(data.value.totalAgents ?? 0) },
  { label: '代理总拉新', value: formatNum(data.value.totalAgentRefer ?? 0) },
  { label: '代理总佣金', value: '¥' + formatNum(data.value.totalCommission ?? 0) },
])

const todayCards = computed(() => [
  { label: '新增用户', value: formatNum(data.value.todayNewUsers ?? 0), icon: User, iconBg: 'rgba(102,126,234,0.12)', iconColor: '#667eea' },
  { label: '充值代币', value: formatNum(data.value.todayRecharge ?? 0), icon: Coin, iconBg: 'rgba(103,194,58,0.12)', iconColor: '#67c23a' },
  { label: '充值金额', value: '¥' + formatNum(data.value.todayRechargeAmount ?? 0), icon: Wallet, iconBg: 'rgba(245,87,108,0.12)', iconColor: '#f5576c' },
  { label: '抽奖次数', value: formatNum(data.value.todayDraw ?? 0), icon: ShoppingCart, iconBg: 'rgba(230,162,60,0.12)', iconColor: '#e6a23c' },
  { label: '发货订单', value: formatNum(data.value.todayDelivery ?? 0), icon: Van, iconBg: 'rgba(144,147,153,0.12)', iconColor: '#909399' },
  { label: '代理拉新', value: formatNum(data.value.todayAgentRefer ?? 0), icon: Connection, iconBg: 'rgba(67,199,123,0.12)', iconColor: '#43c77b' },
])

const pendingCards = computed(() => [
  { label: '待审核充值', value: data.value.pendingRecharge ?? 0, onClick: () => router.push('/admin/recharge') },
  { label: '待发货订单', value: data.value.pendingDelivery ?? 0, onClick: () => router.push('/admin/orders') },
])

const hasPending = computed(() => pendingCards.value.some(c => c.value > 0))

function formatNum(n) {
  if (typeof n !== 'number') return n
  if (n >= 100000000) return (n / 100000000).toFixed(1) + '亿'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
}

async function loadChartData() {
  try {
    const res = await adminApi.getChartData(chartDays.value)
    const { trend, agentRanking } = res.data
    renderTrendChart(trend)
    renderRankChart(agentRanking)
  } catch (e) { /* silent */ }
}

const colors = [
  { line: '#4C7CF3', area: ['rgba(76,124,243,0.18)', 'rgba(76,124,243,0)'] },
  { line: '#67C23A', area: ['rgba(103,194,58,0.18)', 'rgba(103,194,58,0)'] },
  { line: '#F56C6C', area: ['rgba(245,108,108,0.15)', 'rgba(245,108,108,0)'] },
  { line: '#E6A23C', area: ['rgba(230,162,60,0.15)', 'rgba(230,162,60,0)'] },
]

function makeSeries(name, data, ci) {
  const c = colors[ci]
  return {
    name, type: 'line', data, smooth: 0.35, symbol: 'circle', symbolSize: 3,
    showSymbol: false, emphasis: { focus: 'series', itemStyle: { borderWidth: 2 } },
    lineStyle: { width: 2, color: c.line },
    itemStyle: { color: c.line },
    areaStyle: {
      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: c.area[0] }, { offset: 1, color: c.area[1] }
      ])
    },
  }
}

function renderTrendChart(trend) {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff',
      borderColor: '#eee',
      borderWidth: 1,
      textStyle: { color: '#333', fontSize: 12 },
      axisPointer: { lineStyle: { color: '#ddd', type: 'dashed' } },
    },
    legend: {
      data: ['新增用户', '充值金额', '抽奖次数', '代理拉新'],
      bottom: 0, itemWidth: 12, itemHeight: 3, itemGap: 16,
      textStyle: { fontSize: 11, color: '#999' },
    },
    grid: { left: 42, right: 12, top: 12, bottom: 38, containLabel: false },
    xAxis: {
      type: 'category', data: trend.dates, boundaryGap: false,
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { fontSize: 11, color: '#bbb', margin: 10 },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { fontSize: 11, color: '#bbb' },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    series: [
      makeSeries('新增用户', trend.newUsers, 0),
      makeSeries('充值金额', trend.rechargeAmount, 1),
      makeSeries('抽奖次数', trend.drawCount, 2),
      makeSeries('代理拉新', trend.agentRefers, 3),
    ],
  }, true)
}

function renderRankChart(ranking) {
  if (!rankChartRef.value) return
  if (!rankChart) rankChart = echarts.init(rankChartRef.value)
  const sorted = [...ranking].reverse()
  const max = Math.max(...sorted.map(a => a.teamCount), 1)
  rankChart.setOption({
    tooltip: {
      trigger: 'axis', axisPointer: { type: 'shadow' },
      backgroundColor: '#fff', borderColor: '#eee', borderWidth: 1,
      textStyle: { color: '#333', fontSize: 12 },
      formatter: (p) => `${p[0].name}<br/>拉新人数：<b>${p[0].value}</b>`,
    },
    grid: { left: 8, right: 28, top: 4, bottom: 4, containLabel: true },
    xAxis: {
      type: 'value', show: false,
    },
    yAxis: {
      type: 'category', data: sorted.map(a => a.nickname),
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { fontSize: 12, color: '#666' },
    },
    series: [{
      type: 'bar', data: sorted.map((a, i) => {
        const ratio = a.teamCount / max
        const alpha = 0.35 + ratio * 0.65
        return { value: a.teamCount, itemStyle: { color: `rgba(76,124,243,${alpha})` } }
      }),
      barMaxWidth: 16, barMinWidth: 8,
      itemStyle: { borderRadius: [0, 3, 3, 0] },
      label: { show: true, position: 'right', fontSize: 11, color: '#999', formatter: '{c}人' },
    }],
  }, true)
}

function switchDays(d) {
  chartDays.value = d
  loadChartData()
}

let resizeHandler = null

onMounted(async () => {
  updateTime()
  timer = setInterval(updateTime, 60000)
  try {
    const res = await adminApi.getDashboard()
    data.value = res.data
  } catch (e) { ElMessage.error(e.message || '操作失败') }
  await nextTick()
  loadChartData()
  resizeHandler = () => { trendChart?.resize(); rankChart?.resize() }
  window.addEventListener('resize', resizeHandler)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  trendChart?.dispose()
  rankChart?.dispose()
})
</script>

<style scoped>
.dashboard { max-width: 1100px; }

.page-header { margin-bottom: 24px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 600; color: #1a1a1a; }
.page-desc { margin: 4px 0 0; font-size: 13px; color: #999; }

/* 待处理 */
.pending-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}
.pending-item {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff4f4;
  border: 1px solid #ffe0e0;
  border-radius: 8px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.2s;
}
.pending-item:hover { background: #ffecec; }
.pending-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #e74c3c;
  flex-shrink: 0;
}
.pending-label { font-size: 13px; color: #666; }
.pending-count { font-size: 15px; font-weight: 700; color: #e74c3c; }

/* 段落标题 */
.section-label {
  font-size: 13px;
  font-weight: 600;
  color: #999;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 12px;
}

/* 累计数据 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 28px;
}
.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.stat-label { font-size: 12px; color: #999; }
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin-top: 8px;
}

/* 今日数据 */
.today-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.today-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  text-align: center;
  transition: transform 0.2s, box-shadow 0.2s;
}
.today-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.today-icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
}
.today-value {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.2;
}
.today-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

/* 图表区域 */
.chart-grid {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 12px;
  margin-top: 28px;
}
.chart-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}
.chart-sub {
  font-size: 11px;
  color: #bbb;
  letter-spacing: 1px;
}
.days-toggle {
  display: inline-flex;
  gap: 2px;
  background: #f5f5f5;
  border-radius: 6px;
  padding: 2px;
}
.days-toggle span {
  font-size: 12px;
  padding: 3px 12px;
  border-radius: 4px;
  cursor: pointer;
  color: #999;
  font-weight: 500;
  transition: all 0.2s;
}
.days-toggle span.active {
  background: #fff;
  color: #333;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.chart-container {
  width: 100%;
  height: 280px;
}

@media (max-width: 900px) {
  .stat-grid { grid-template-columns: repeat(3, 1fr); }
  .today-grid { grid-template-columns: repeat(3, 1fr); }
  .pending-bar { flex-direction: column; }
  .chart-grid { grid-template-columns: 1fr; }
}
@media (max-width: 600px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .today-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
