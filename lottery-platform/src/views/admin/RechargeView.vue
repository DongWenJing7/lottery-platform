<template>
  <div>
    <div class="page-header"><h2>充值管理</h2></div>

    <!-- 支付配置 -->
    <div class="card" style="margin-bottom: 16px;">
      <div class="section-title">支付配置</div>
      <el-form :inline="true" size="default">
        <el-form-item label="淘宝链接">
          <el-input v-model="payConfig.taobaoLink" placeholder="淘宝商品链接" style="width: 360px;" />
        </el-form-item>
        <el-form-item label="注意事项">
          <el-input v-model="payConfig.paymentNotice" placeholder="付款注意事项" style="width: 360px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="savePayConfig">保存配置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
    <div class="toolbar">
      <el-radio-group v-model="status" @change="loadData">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="unpaid">待支付</el-radio-button>
        <el-radio-button label="pending">待审核</el-radio-button>
        <el-radio-button label="done">已确认</el-radio-button>
        <el-radio-button label="rejected">已驳回</el-radio-button>
      </el-radio-group>
      <el-input v-model="keyword" placeholder="搜索订单号/用户/凭证" clearable style="width: 220px; margin-left: 12px;" @keyup.enter="loadData" @clear="loadData">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="success" @click="handleExport" style="margin-left: 12px;">导出Excel</el-button>
    </div>
    <el-table :data="list" stripe>
      <el-table-column type="index" label="序号" width="70" :index="i => (page - 1) * size + i + 1" />
      <el-table-column prop="orderNo" label="订单号" width="200" />
      <el-table-column label="用户" width="120">
        <template #default="{ row }">{{ row.user?.nickname || '-' }}</template>
      </el-table-column>
      <el-table-column prop="tokens" label="代币" width="80" />
      <el-table-column prop="amount" label="金额" width="90">
        <template #default="{ row }">¥{{ row.amount }}</template>
      </el-table-column>
      <el-table-column prop="paymentMethod" label="支付方式" width="100">
        <template #default="{ row }">{{ payMethodText(row.paymentMethod) }}</template>
      </el-table-column>
      <el-table-column prop="paymentProof" label="支付凭证" width="150" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTypeMap[row.status]" size="small">
            {{ statusTextMap[row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="170">
        <template #default="{ row }">{{ row.createdAt?.replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" @click="showDetail(row)">详情</el-button>
          <template v-if="row.status === 'pending'">
            <el-button size="small" type="success" @click="handleConfirm(row)">确认</el-button>
            <el-button size="small" type="danger" @click="handleReject(row)">驳回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination :current-page="page" :page-size="size" :total="total" @current-change="p => { page = p; loadData() }" layout="total, prev, pager, next" />
    </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="充值订单详情" width="520px">
      <el-descriptions :column="1" border size="default">
        <el-descriptions-item label="订单号">{{ detailRow.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ detailRow.user?.nickname || '-' }}（{{ detailRow.user?.username || '-' }}）</el-descriptions-item>
        <el-descriptions-item label="代币">{{ detailRow.tokens }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ detailRow.amount }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ payMethodText(detailRow.paymentMethod) }}</el-descriptions-item>
        <el-descriptions-item label="支付凭证">{{ detailRow.paymentProof || '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付截图">
          <el-image
            v-if="detailRow.paymentImage"
            :src="detailRow.paymentImage"
            :preview-src-list="[detailRow.paymentImage]"
            fit="contain"
            style="max-width: 200px; max-height: 200px; cursor: pointer; border-radius: 4px;"
          />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ detailRow.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTypeMap[detailRow.status]" size="small">{{ statusTextMap[detailRow.status] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="detailRow.rejectReason" label="驳回原因">{{ detailRow.rejectReason }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ detailRow.createdAt?.replace('T', ' ') }}</el-descriptions-item>
      </el-descriptions>
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

const statusTypeMap = { unpaid: 'info', pending: 'warning', done: 'success', rejected: 'danger' }
const statusTextMap = { unpaid: '待支付', pending: '待审核', done: '已确认', rejected: '已驳回' }

function payMethodText(m) {
  if (!m) return '-'
  return { taobao: '淘宝', manual: '手动转账' }[m] || m
}

async function loadData() {
  try {
    const res = await adminApi.getRechargeOrders({ page: page.value, size: size.value, status: status.value || undefined, keyword: keyword.value || undefined })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

async function handleConfirm(row) {
  try {
    await ElMessageBox.confirm(`确认充值 ${row.tokens} 代币给 ${row.user?.nickname}？`, '确认充值')
    await adminApi.confirmRecharge(row.id)
    ElMessage.success('已确认')
    loadData()
    refreshPendingCounts()
  } catch (e) { if (e !== 'cancel' && e?.message) ElMessage.error(e.message) }
}

async function handleReject(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回', { inputPlaceholder: '驳回原因' })
    await adminApi.rejectRecharge(row.id, value)
    ElMessage.success('已驳回')
    loadData()
    refreshPendingCounts()
  } catch (e) { if (e !== 'cancel' && e?.message) ElMessage.error(e.message) }
}

// ========== 详情弹窗 ==========
const detailVisible = ref(false)
const detailRow = ref({})

function showDetail(row) {
  detailRow.value = row
  detailVisible.value = true
}

// ========== 支付配置 ==========
const payConfig = reactive({ taobaoLink: '', paymentNotice: '' })

async function loadPayConfig() {
  try {
    const res = await adminApi.getPaymentConfig()
    payConfig.taobaoLink = res.data.taobaoLink || ''
    payConfig.paymentNotice = res.data.paymentNotice || ''
  } catch (e) { /* ignore */ }
}

async function savePayConfig() {
  try {
    await adminApi.savePaymentConfig({
      taobaoLink: payConfig.taobaoLink,
      paymentNotice: payConfig.paymentNotice,
    })
    ElMessage.success('配置已保存')
  } catch (e) { ElMessage.error(e.message || '保存失败') }
}

// ========== 导出 ==========
async function handleExport() {
  try {
    const res = await adminApi.exportRechargeOrders(status.value || undefined)
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'recharge_orders.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) { ElMessage.error('导出失败') }
}

onMounted(() => {
  loadData()
  loadPayConfig()
})
</script>

<style scoped>
.page-header h2 { margin: 0 0 20px; font-size: 20px; color: #1a1a1a; }
.card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.section-title { font-size: 15px; font-weight: 600; color: #333; margin-bottom: 12px; }
.toolbar { margin-bottom: 16px; display: flex; align-items: center; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
