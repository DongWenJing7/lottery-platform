<template>
  <div>
    <div class="page-header"><h2>发货订单</h2></div>
    <div class="card">
    <div class="toolbar">
      <el-radio-group v-model="status" @change="loadData">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="pending">待发货</el-radio-button>
        <el-radio-button label="shipped">已发货</el-radio-button>
        <el-radio-button label="done">已完成</el-radio-button>
      </el-radio-group>
    </div>
    <el-table :data="list" stripe>
      <el-table-column type="index" label="序号" width="70" :index="i => (page - 1) * size + i + 1" />
      <el-table-column prop="orderNo" label="订单号" width="200" />
      <el-table-column label="用户" width="100">
        <template #default="{ row }">{{ row.user?.nickname || '-' }}</template>
      </el-table-column>
      <el-table-column label="奖品" width="120">
        <template #default="{ row }">{{ row.prize?.name || '-' }}</template>
      </el-table-column>
      <el-table-column prop="receiverName" label="收件人" width="90" />
      <el-table-column prop="receiverPhone" label="手机号" width="130" />
      <el-table-column prop="receiverAddress" label="地址" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="{ pending: 'warning', shipped: 'primary', done: 'success' }[row.status]" size="small">
            {{ { pending: '待发货', shipped: '已发货', done: '已完成' }[row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button v-if="row.status === 'pending'" size="small" type="primary" @click="handleShip(row)">发货</el-button>
          <el-button v-if="row.status === 'shipped'" size="small" type="success" @click="handleDone(row)">完成</el-button>
          <span v-if="row.status === 'done'" style="color:#999">已完成</span>
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const list = ref([])
const status = ref('pending')
const page = ref(1)
const size = ref(20)
const total = ref(0)

async function loadData() {
  try {
    const res = await adminApi.getOrders({ page: page.value, size: size.value, status: status.value || undefined })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

async function handleShip(row) {
  try {
    await ElMessageBox.confirm('确认发货？', '提示')
    await adminApi.shipOrder(row.id)
    ElMessage.success('已标记发货')
    loadData()
  } catch (e) { if (e !== 'cancel' && e?.message) ElMessage.error(e.message) }
}

async function handleDone(row) {
  try {
    await ElMessageBox.confirm('确认收货完成？', '提示')
    await adminApi.completeOrder(row.id)
    ElMessage.success('已完成')
    loadData()
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
