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
      <el-input v-model="keyword" placeholder="搜索订单号/用户/收件人/手机号" clearable style="width: 260px; margin-left: 12px;" @clear="loadData">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" @click="loadData" style="margin-left: 10px;">搜索</el-button>
      <el-button type="success" @click="handleExport" style="margin-left: 12px;">导出Excel</el-button>
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
      <el-table-column prop="receiverAddress" label="地址" show-overflow-tooltip />
      <el-table-column label="快递信息" width="160">
        <template #default="{ row }">
          <template v-if="row.expressCompany || row.expressNo">
            <div>{{ row.expressCompany }}</div>
            <div style="color:#999; font-size:12px;">{{ row.expressNo }}</div>
          </template>
          <span v-else style="color:#ccc">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="{ pending: 'warning', shipped: 'primary', done: 'success' }[row.status]" size="small">
            {{ { pending: '待发货', shipped: '已发货', done: '已完成' }[row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button v-if="row.status === 'pending'" size="small" type="primary" @click="openShipDialog(row)">发货</el-button>
          <el-button v-if="row.status === 'shipped'" size="small" type="success" @click="handleDone(row)">完成</el-button>
          <span v-if="row.status === 'done'" style="color:#999">已完成</span>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination :current-page="page" :page-size="size" :total="total" @current-change="p => { page = p; loadData() }" layout="total, prev, pager, next" />
    </div>
    </div>

    <!-- 发货弹窗 -->
    <el-dialog v-model="shipDialogVisible" title="发货" width="420px">
      <el-form label-width="80px">
        <el-form-item label="快递公司">
          <el-select v-model="shipForm.expressCompany" placeholder="选择快递公司" style="width: 100%;">
            <el-option v-for="c in expressCompanies" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="快递单号">
          <el-input v-model="shipForm.expressNo" placeholder="输入快递单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipLoading" @click="handleShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, inject, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const refreshPendingCounts = inject('refreshPendingCounts', () => {})

const list = ref([])
const status = ref('pending')
const keyword = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)

const expressCompanies = ['顺丰', '中通', '圆通', '韵达', '申通', 'EMS', '其他']

async function loadData() {
  try {
    const res = await adminApi.getOrders({ page: page.value, size: size.value, status: status.value || undefined, keyword: keyword.value || undefined })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

// ========== 发货弹窗 ==========
const shipDialogVisible = ref(false)
const shipLoading = ref(false)
const currentShipOrder = ref(null)
const shipForm = reactive({ expressCompany: '', expressNo: '' })

function openShipDialog(row) {
  currentShipOrder.value = row
  shipForm.expressCompany = ''
  shipForm.expressNo = ''
  shipDialogVisible.value = true
}

async function handleShip() {
  shipLoading.value = true
  try {
    await adminApi.shipOrder(currentShipOrder.value.id, {
      expressCompany: shipForm.expressCompany || undefined,
      expressNo: shipForm.expressNo || undefined,
    })
    ElMessage.success('已标记发货')
    shipDialogVisible.value = false
    loadData()
    refreshPendingCounts()
  } catch (e) { ElMessage.error(e.message || '操作失败') }
  finally { shipLoading.value = false }
}

async function handleDone(row) {
  try {
    await ElMessageBox.confirm('确认收货完成？', '提示')
    await adminApi.completeOrder(row.id)
    ElMessage.success('已完成')
    loadData()
    refreshPendingCounts()
  } catch (e) { if (e !== 'cancel' && e?.message) ElMessage.error(e.message) }
}

// ========== 导出 ==========
async function handleExport() {
  try {
    const res = await adminApi.exportDeliveryOrders(status.value || undefined)
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'delivery_orders.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) { ElMessage.error('导出失败') }
}

onMounted(loadData)
</script>

<style scoped>
.page-header h2 { margin: 0 0 20px; font-size: 20px; color: #1a1a1a; }
.card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.toolbar { margin-bottom: 16px; display: flex; align-items: center; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
