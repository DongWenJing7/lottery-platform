<template>
  <div>
    <div class="page-header">
      <h2>数据概览</h2>
      <p class="page-desc">平台核心数据一览</p>
    </div>

    <!-- 总计 -->
    <div class="section-title">累计统计</div>
    <el-row :gutter="16">
      <el-col :span="4" v-for="item in totalCards" :key="item.label">
        <div class="stat-card" :style="{ borderTopColor: item.color }">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value">{{ item.value }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 今日 -->
    <div class="section-title" style="margin-top: 24px;">今日统计</div>
    <el-row :gutter="16">
      <el-col :span="4" v-for="item in todayCards" :key="item.label">
        <div class="stat-card" :style="{ borderTopColor: item.color }">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value">{{ item.value }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 待处理 -->
    <div class="section-title" style="margin-top: 24px;">待处理</div>
    <el-row :gutter="16">
      <el-col :span="4" v-for="item in pendingCards" :key="item.label">
        <div class="stat-card" :style="{ borderTopColor: item.color }">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value" :class="{ highlight: item.value > 0 }">{{ item.value }}</div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'

const data = ref({})

const totalCards = computed(() => [
  { label: '总玩家数', value: data.value.totalUsers ?? 0, color: '#409eff' },
  { label: '总充值(代币)', value: data.value.totalRecharge ?? 0, color: '#67c23a' },
])

const todayCards = computed(() => [
  { label: '今日新增用户', value: data.value.todayNewUsers ?? 0, color: '#409eff' },
  { label: '今日充值(代币)', value: data.value.todayRecharge ?? 0, color: '#67c23a' },
  { label: '今日充值(金额)', value: '¥' + (data.value.todayRechargeAmount ?? 0), color: '#67c23a' },
  { label: '今日抽奖', value: data.value.todayDraw ?? 0, color: '#e6a23c' },
  { label: '今日发货', value: data.value.todayDelivery ?? 0, color: '#909399' },
])

const pendingCards = computed(() => [
  { label: '待审核充值', value: data.value.pendingRecharge ?? 0, color: '#f56c6c' },
  { label: '待发货订单', value: data.value.pendingDelivery ?? 0, color: '#e6a23c' },
  { label: '待处理售后', value: data.value.pendingAfterSale ?? 0, color: '#909399' },
])

onMounted(async () => {
  try {
    const res = await adminApi.getDashboard()
    data.value = res.data
  } catch (e) { ElMessage.error(e.message || '操作失败') }
})
</script>

<style scoped>
.page-header h2 { margin: 0; font-size: 20px; color: #1a1a1a; }
.page-desc { margin: 4px 0 0; font-size: 13px; color: #999; }
.section-title { font-size: 15px; font-weight: 600; color: #333; margin: 20px 0 12px; }
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  border-top: 3px solid #409eff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.stat-label { font-size: 13px; color: #999; }
.stat-value { font-size: 28px; font-weight: 700; color: #1a1a1a; margin-top: 8px; }
.stat-value.highlight { color: #f56c6c; }
</style>
