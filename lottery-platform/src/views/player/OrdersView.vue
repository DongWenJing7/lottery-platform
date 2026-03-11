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
        <!-- 物流信息 -->
        <div v-if="order.expressCompany || order.expressNo" class="express-info">
          <span>{{ order.expressCompany }}</span>
          <span v-if="order.expressNo" class="express-no" @click="copyExpressNo(order.expressNo)">
            {{ order.expressNo }}
            <van-icon name="notes-o" size="14" />
          </span>
        </div>
        <!-- 售后状态 -->
        <div v-if="order.afterSaleStatus" class="after-sale-tag">
          <van-tag :type="afterSaleStatusType(order.afterSaleStatus)" size="medium">
            售后：{{ afterSaleStatusText(order.afterSaleStatus) }}
          </van-tag>
        </div>
        <div class="order-time">{{ order.createdAt }}</div>
        <!-- 申请售后按钮 -->
        <van-button
          v-if="(order.status === 'shipped' || order.status === 'done') && !order.afterSaleStatus"
          type="default"
          size="small"
          round
          class="after-sale-btn"
          @click="openAfterSaleDialog(order)"
        >
          申请售后
        </van-button>
      </div>
      <van-empty v-if="!list.length" description="暂无订单" />
    </div>

    <!-- 售后申请弹窗 -->
    <van-dialog
      v-model:show="afterSaleDialogVisible"
      title="申请售后"
      :show-confirm-button="false"
      close-on-click-overlay
    >
      <div class="as-dialog-body">
        <div class="as-type-row">
          <div :class="['as-type', asForm.type === 'refund' && 'active']" @click="asForm.type = 'refund'">退款</div>
          <div :class="['as-type', asForm.type === 'exchange' && 'active']" @click="asForm.type = 'exchange'">换货</div>
        </div>
        <van-field
          v-model="asForm.reason"
          type="textarea"
          rows="3"
          placeholder="请输入申请原因"
          class="as-reason-input"
        />
        <van-button
          type="warning"
          block
          round
          :loading="asSubmitting"
          :disabled="!asForm.reason"
          style="margin-top: 12px;"
          @click="handleSubmitAfterSale"
        >
          提交申请
        </van-button>
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { showToast, showSuccessToast, showFailToast } from 'vant'
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

function afterSaleStatusType(s) { return { pending: 'warning', approved: 'primary', rejected: 'danger', done: 'success' }[s] || 'default' }
function afterSaleStatusText(s) { return { pending: '处理中', approved: '已同意', rejected: '已驳回', done: '已完成' }[s] || s }

function copyExpressNo(no) {
  navigator.clipboard.writeText(no).then(() => {
    showToast('已复制快递单号')
  }).catch(() => {
    showToast('复制失败')
  })
}

async function loadData() {
  try {
    const [ordersRes, afterSalesRes] = await Promise.all([
      playerApi.getOrders({ page: 1, size: 50, status: tab.value || undefined }),
      playerApi.getAfterSales({ page: 1, size: 100 }),
    ])
    const afterSaleMap = {}
    ;(afterSalesRes.data.list || []).forEach(as => {
      // Use delivery order no to match; but we have deliveryOrderId in afterSale.
      // We'll match by the relationship: afterSale has deliveryOrderId
      // But our orders list uses order.id as the delivery order id
      // Store by delivery order ID (which we don't have directly in afterSale response)
      // Actually the afterSale response has deliveryOrderNo, not id. Let's match by orderNo.
      // Hmm, we need the delivery order id. Let's just track the most recent afterSale status per deliveryOrderNo.
    })

    // Build a map from delivery order ID (use orderNo matching)
    const orders = ordersRes.data.list || []
    const afterSales = afterSalesRes.data.list || []

    // afterSales has deliveryOrderNo, orders has orderNo
    const asMap = {}
    afterSales.forEach(as => {
      if (as.deliveryOrderNo && (!asMap[as.deliveryOrderNo] || as.id > asMap[as.deliveryOrderNo].id)) {
        asMap[as.deliveryOrderNo] = as
      }
    })

    orders.forEach(order => {
      const as = asMap[order.orderNo]
      if (as) {
        order.afterSaleStatus = as.status
      }
    })

    list.value = orders
  } catch (e) { showToast(e.message || '操作失败') }
}

// ========== 售后申请 ==========
const afterSaleDialogVisible = ref(false)
const asSubmitting = ref(false)
const currentOrder = ref(null)
const asForm = reactive({ type: 'refund', reason: '' })

function openAfterSaleDialog(order) {
  currentOrder.value = order
  asForm.type = 'refund'
  asForm.reason = ''
  afterSaleDialogVisible.value = true
}

async function handleSubmitAfterSale() {
  if (!asForm.reason) return
  asSubmitting.value = true
  try {
    await playerApi.submitAfterSale({
      deliveryOrderId: currentOrder.value.id,
      type: asForm.type,
      reason: asForm.reason,
    })
    showSuccessToast('售后申请已提交')
    afterSaleDialogVisible.value = false
    loadData()
  } catch (e) {
    showFailToast(e.message || '提交失败')
  } finally {
    asSubmitting.value = false
  }
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

.express-info {
  display: flex; gap: 8px; align-items: center; margin-top: 8px;
  font-size: 13px; color: #f0c040; background: rgba(240,192,64,0.08);
  padding: 6px 10px; border-radius: 6px;
}
.express-no { cursor: pointer; display: flex; align-items: center; gap: 4px; text-decoration: underline; }
.after-sale-tag { margin-top: 8px; }
.after-sale-btn { margin-top: 10px; }

/* 售后弹窗 */
.as-dialog-body { padding: 16px; color: #333; }
.as-type-row { display: flex; gap: 10px; margin-bottom: 12px; }
.as-type {
  flex: 1; text-align: center; padding: 10px; border: 2px solid #eee; border-radius: 8px;
  cursor: pointer; font-size: 14px;
}
.as-type.active { border-color: #f0c040; background: rgba(240,192,64,0.08); color: #f0c040; font-weight: 600; }
.as-reason-input { border: 1px solid #eee; border-radius: 8px; }
</style>
