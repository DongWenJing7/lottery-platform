<template>
  <div>
    <div class="page-header">
      <h2>数据概览</h2>
      <p class="page-desc">平台核心数据一览</p>
    </div>
    <el-row :gutter="16" style="margin-top:20px">
      <el-col :span="4" v-for="item in cards" :key="item.label">
        <div class="stat-card" :style="{ borderTopColor: item.color }">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value">{{ item.value }}</div>
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
const cards = computed(() => [
  { label: '总玩家数', value: data.value.totalUsers ?? 0, color: '#409eff' },
  { label: '总充值(代币)', value: data.value.totalRecharge ?? 0, color: '#67c23a' },
  { label: '今日抽奖', value: data.value.todayDraw ?? 0, color: '#e6a23c' },
  { label: '待审核充值', value: data.value.pendingRecharge ?? 0, color: '#f56c6c' },
  { label: '待发货订单', value: data.value.pendingDelivery ?? 0, color: '#909399' },
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
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  border-top: 3px solid #409eff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.stat-label { font-size: 13px; color: #999; }
.stat-value { font-size: 28px; font-weight: 700; color: #1a1a1a; margin-top: 8px; }
</style>
