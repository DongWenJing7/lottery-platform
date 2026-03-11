<template>
  <div>
    <div class="page-header"><h2>售后管理</h2></div>
    <div class="card">
    <div class="toolbar">
      <el-radio-group v-model="status" @change="loadData">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="pending">待处理</el-radio-button>
        <el-radio-button label="approved">已同意</el-radio-button>
        <el-radio-button label="rejected">已驳回</el-radio-button>
        <el-radio-button label="done">已完成</el-radio-button>
      </el-radio-group>
    </div>
    <el-table :data="list" stripe>
      <el-table-column type="index" label="序号" width="70" :index="i => (page - 1) * size + i + 1" />
      <el-table-column prop="orderNo" label="售后单号" width="200" />
      <el-table-column prop="deliveryOrderNo" label="发货订单号" width="200" />
      <el-table-column label="用户" width="100">
        <template #default="{ row }">{{ row.user?.nickname || '-' }}</template>
      </el-table-column>
      <el-table-column label="奖品" width="120">
        <template #default="{ row }">{{ row.prize?.name || '-' }}</template>
      </el-table-column>
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.type === 'refund' ? 'danger' : 'warning'" size="small">
            {{ row.type === 'refund' ? '退款' : '换货' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="原因" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTypeMap[row.status]" size="small">{{ statusTextMap[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="rejectReason" label="驳回原因" width="140" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="时间" width="170">
        <template #default="{ row }">{{ row.createdAt?.replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <template v-if="row.status === 'pending'">
            <el-button size="small" type="success" @click="handleApprove(row)">同意</el-button>
            <el-button size="small" type="danger" @click="handleReject(row)">驳回</el-button>
          </template>
          <template v-else-if="row.status === 'approved'">
            <el-button size="small" type="primary" @click="handleDone(row)">完成</el-button>
          </template>
          <span v-else style="color:#999">已处理</span>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination :current-page="page" :page-size="size" :total="total" @current-change="p => { page = p; loadData() }" layout="total, prev, pager, next" />
    </div>
    </div>
  </div>
</template>

<script setup>
import { ref, inject, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const refreshPendingCounts = inject('refreshPendingCounts', () => {})

const list = ref([])
const status = ref('pending')
const page = ref(1)
const size = ref(20)
const total = ref(0)

const statusTypeMap = { pending: 'warning', approved: 'primary', rejected: 'danger', done: 'success' }
const statusTextMap = { pending: '待处理', approved: '已同意', rejected: '已驳回', done: '已完成' }

async function loadData() {
  try {
    const res = await adminApi.getAfterSales({ page: page.value, size: size.value, status: status.value || undefined })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

async function handleApprove(row) {
  try {
    await ElMessageBox.confirm('确认同意该售后申请？', '同意售后')
    await adminApi.approveAfterSale(row.id)
    ElMessage.success('已同意')
    loadData()
    refreshPendingCounts()
  } catch (e) { if (e !== 'cancel' && e?.message) ElMessage.error(e.message) }
}

async function handleReject(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回', { inputPlaceholder: '驳回原因' })
    await adminApi.rejectAfterSale(row.id, value)
    ElMessage.success('已驳回')
    loadData()
    refreshPendingCounts()
  } catch (e) { if (e !== 'cancel' && e?.message) ElMessage.error(e.message) }
}

async function handleDone(row) {
  try {
    await ElMessageBox.confirm('确认售后已完成（已退款/换货）？', '完成售后')
    await adminApi.completeAfterSale(row.id)
    ElMessage.success('已完成')
    loadData()
    refreshPendingCounts()
  } catch (e) { if (e !== 'cancel' && e?.message) ElMessage.error(e.message) }
}

onMounted(loadData)
</script>

<style scoped>
.page-header h2 { margin: 0 0 20px; font-size: 20px; color: #1a1a1a; }
.card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.toolbar { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
