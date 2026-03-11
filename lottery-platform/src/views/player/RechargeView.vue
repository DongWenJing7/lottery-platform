<template>
  <div class="page">
    <van-tabs v-model:active="activeTab" shrink sticky class="dark-tabs" @change="onTabChange">
      <van-tab title="充值">
        <div class="tab-body">
          <div class="form-item">
            <label>充值代币数量</label>
            <div class="token-options">
              <div
                v-for="opt in tokenOptions"
                :key="opt.tokens"
                :class="['token-opt', form.tokens === opt.tokens && 'active']"
                @click="selectOption(opt)"
              >
                <div class="opt-tokens">{{ opt.tokens }}</div>
                <div class="opt-price">¥{{ opt.price }}</div>
              </div>
            </div>
          </div>
          <div class="form-item">
            <label>自定义数量</label>
            <van-field
              v-model="customTokens"
              type="digit"
              placeholder="输入代币数量"
              class="custom-input"
              @update:model-value="onCustomInput"
            />
            <div v-if="form.tokens && form.amount" class="calc-hint">
              {{ form.tokens }} 代币 = ¥{{ form.amount }}
            </div>
          </div>
          <div class="form-item">
            <label>付款备注（选填）</label>
            <van-field v-model="form.remark" placeholder="转账时的备注信息" class="custom-input" />
          </div>
          <van-button
            type="primary"
            block
            round
            :loading="submitting"
            :disabled="!form.tokens"
            class="submit-btn"
            @click="handleSubmit"
          >
            提交充值申请（¥{{ form.amount || 0 }}）
          </van-button>
          <div class="tips">
            <p>1. 选择或输入充值数量后提交申请</p>
            <p>2. 按页面提示完成转账付款</p>
            <p>3. 管理员确认后代币自动到账</p>
          </div>
        </div>
      </van-tab>

      <van-tab title="记录" :badge="pendingCount || ''">
        <div class="tab-body">
          <van-pull-refresh v-model="refreshing" @refresh="loadData">
            <div class="order-list">
              <div v-for="order in list" :key="order.id" class="order-card">
                <div class="order-top">
                  <span class="order-no">{{ order.orderNo }}</span>
                  <van-tag :type="statusType(order.status)">{{ statusText(order.status) }}</van-tag>
                </div>
                <div class="order-detail">
                  <span>{{ order.tokens }} 代币</span>
                  <span class="order-amount">¥{{ order.amount }}</span>
                </div>
                <div v-if="order.paymentMethod" class="order-remark">支付方式：{{ paymentMethodText(order.paymentMethod) }}</div>
                <div v-if="order.paymentProof" class="order-remark">支付凭证：{{ order.paymentProof }}</div>
                <div v-if="order.remark" class="order-remark">备注：{{ order.remark }}</div>
                <div v-if="order.rejectReason" class="order-remark red">驳回原因：{{ order.rejectReason }}</div>
                <div class="order-time">{{ order.createdAt }}</div>
                <!-- 待支付订单显示去支付按钮 -->
                <van-button
                  v-if="order.status === 'unpaid'"
                  type="warning"
                  size="small"
                  round
                  class="pay-btn"
                  @click="openPayDialog(order)"
                >
                  去支付
                </van-button>
              </div>
              <van-empty v-if="!list.length && !refreshing" description="暂无充值记录" />
            </div>
          </van-pull-refresh>
        </div>
      </van-tab>
    </van-tabs>

    <!-- 支付引导弹窗 -->
    <van-dialog
      v-model:show="payDialogVisible"
      title="确认付款"
      :show-confirm-button="false"
      close-on-click-overlay
    >
      <div class="pay-dialog-body">
        <div v-if="paymentConfig.paymentNotice" class="pay-notice">{{ paymentConfig.paymentNotice }}</div>

        <div class="pay-methods">
          <div
            :class="['pay-method', payForm.paymentMethod === 'taobao' && 'active']"
            @click="payForm.paymentMethod = 'taobao'"
          >
            淘宝支付
          </div>
          <div
            :class="['pay-method', payForm.paymentMethod === 'manual' && 'active']"
            @click="payForm.paymentMethod = 'manual'"
          >
            手动转账
          </div>
        </div>

        <div v-if="payForm.paymentMethod === 'taobao' && paymentConfig.taobaoLink" style="margin: 12px 0;">
          <van-button type="primary" size="small" block @click="openTaobao">打开淘宝商品链接</van-button>
        </div>

        <van-field
          v-model="payForm.paymentProof"
          :placeholder="payForm.paymentMethod === 'taobao' ? '请输入淘宝订单号' : '请输入转账凭证/备注'"
          class="proof-input"
        />

        <!-- 上传支付截图 -->
        <div class="upload-section">
          <div class="upload-label">上传支付截图（选填）</div>
          <van-uploader
            v-model="payForm.fileList"
            :max-count="1"
            :after-read="onUploadRead"
            accept="image/*"
            :preview-size="80"
          />
        </div>

        <van-button
          type="warning"
          block
          round
          :loading="paySubmitting"
          :disabled="!payForm.paymentProof && !payForm.proofImage"
          style="margin-top: 12px;"
          @click="handleConfirmPay"
        >
          我已付款
        </van-button>
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { showSuccessToast, showFailToast } from 'vant'
import { playerApi } from '@/api'
import request from '@/api/request'

const activeTab = ref(0)

// ========== 充值表单 ==========
const RATE = 10
const tokenOptions = [
  { tokens: 100, price: 10 },
  { tokens: 500, price: 50 },
  { tokens: 1000, price: 100 },
  { tokens: 5000, price: 500 },
]

const form = ref({ tokens: 0, amount: 0, remark: '' })
const customTokens = ref('')
const submitting = ref(false)

function selectOption(opt) {
  form.value.tokens = opt.tokens
  form.value.amount = opt.price
  customTokens.value = ''
}

function onCustomInput(val) {
  const n = parseInt(val) || 0
  form.value.tokens = n
  form.value.amount = +(n / RATE).toFixed(2)
}

async function handleSubmit() {
  if (!form.value.tokens || form.value.tokens <= 0) return
  submitting.value = true
  try {
    const res = await playerApi.submitRecharge({
      tokens: form.value.tokens,
      amount: form.value.amount,
      remark: form.value.remark || undefined,
    })
    showSuccessToast('申请已提交')
    form.value = { tokens: 0, amount: 0, remark: '' }
    customTokens.value = ''
    // 切到记录tab并刷新
    activeTab.value = 1
    await loadData()
    // 自动弹出支付弹窗
    const newOrder = list.value.find(o => o.id === res.data.orderId)
    if (newOrder) openPayDialog(newOrder)
  } catch (e) {
    showFailToast(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

// ========== 充值记录 ==========
const list = ref([])
const refreshing = ref(false)

const pendingCount = computed(() => list.value.filter(o => o.status === 'unpaid' || o.status === 'pending').length)

function statusType(s) { return { unpaid: 'default', pending: 'warning', done: 'success', rejected: 'danger' }[s] || 'default' }
function statusText(s) { return { unpaid: '待支付', pending: '待确认', done: '已到账', rejected: '已驳回' }[s] || s }
function paymentMethodText(m) { return { taobao: '淘宝支付', manual: '手动转账' }[m] || m }

function onTabChange(index) {
  if (index === 1) loadData()
}

async function loadData() {
  try {
    const res = await playerApi.getRechargeOrders({ page: 1, size: 50 })
    list.value = res.data.list
  } catch (e) {
    showFailToast(e.message || '加载失败')
  } finally { refreshing.value = false }
}

// ========== 支付引导 ==========
const payDialogVisible = ref(false)
const paySubmitting = ref(false)
const currentPayOrder = ref(null)
const payForm = reactive({ paymentMethod: 'taobao', paymentProof: '', proofImage: '', fileList: [] })
const paymentConfig = reactive({ taobaoLink: '', paymentNotice: '' })

async function loadPaymentConfig() {
  try {
    const res = await playerApi.getPaymentConfig()
    paymentConfig.taobaoLink = res.data.taobaoLink || ''
    paymentConfig.paymentNotice = res.data.paymentNotice || ''
  } catch (e) { /* ignore */ }
}

function openPayDialog(order) {
  currentPayOrder.value = order
  payForm.paymentMethod = 'taobao'
  payForm.paymentProof = ''
  payForm.proofImage = ''
  payForm.fileList = []
  payDialogVisible.value = true
}

async function onUploadRead(file) {
  file.status = 'uploading'
  file.message = '上传中...'
  try {
    const formData = new FormData()
    formData.append('file', file.file)
    const res = await request.post('/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    payForm.proofImage = res.data.url
    file.status = 'done'
    file.message = ''
  } catch (e) {
    file.status = 'failed'
    file.message = '上传失败'
    showFailToast('图片上传失败')
  }
}

function openTaobao() {
  if (paymentConfig.taobaoLink) {
    window.open(paymentConfig.taobaoLink, '_blank')
  }
}

async function handleConfirmPay() {
  if (!payForm.paymentProof && !payForm.proofImage) return
  paySubmitting.value = true
  try {
    await playerApi.confirmPayment(currentPayOrder.value.id, {
      paymentMethod: payForm.paymentMethod,
      paymentProof: payForm.paymentProof || undefined,
      paymentImage: payForm.proofImage || undefined,
    })
    showSuccessToast('已提交付款确认')
    payDialogVisible.value = false
    loadData()
  } catch (e) {
    showFailToast(e.message || '操作失败')
  } finally {
    paySubmitting.value = false
  }
}

onMounted(() => {
  loadData()
  loadPaymentConfig()
})
</script>

<style scoped>
.page { color: #fff; }

.dark-tabs :deep(.van-tabs__nav) {
  background: transparent;
}
.dark-tabs :deep(.van-tab) {
  color: rgba(255,255,255,0.5);
  font-size: 16px;
  font-weight: 600;
}
.dark-tabs :deep(.van-tab--active) {
  color: #fff;
}
.dark-tabs :deep(.van-tabs__line) {
  background: #f0c040;
  width: 20px !important;
  height: 3px;
  border-radius: 3px;
}
.dark-tabs :deep(.van-tab__text--ellipsis) {
  overflow: visible;
}

.tab-body {
  padding: 16px;
}

.form-item {
  margin-bottom: 16px;
}
.form-item label {
  display: block;
  font-size: 13px;
  color: #aaa;
  margin-bottom: 8px;
}

.token-options {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
.token-opt {
  background: #1e1e1e;
  border: 2px solid transparent;
  border-radius: 10px;
  padding: 14px 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}
.token-opt:active { opacity: 0.7; }
.token-opt.active {
  border-color: #f0c040;
  background: rgba(240, 192, 64, 0.1);
}
.opt-tokens {
  font-size: 20px;
  font-weight: 700;
  color: #f0c040;
}
.opt-price {
  font-size: 12px;
  color: #888;
  margin-top: 4px;
}

.custom-input {
  background: #1e1e1e;
  border-radius: 8px;
  overflow: hidden;
}
.custom-input :deep(.van-field__control) {
  color: #fff;
}
.calc-hint {
  font-size: 12px;
  color: #f0c040;
  margin-top: 6px;
}

.submit-btn {
  margin-top: 8px;
  --van-button-primary-background: #f0c040;
  --van-button-primary-border-color: #f0c040;
  --van-button-primary-color: #111;
  font-weight: 600;
}

.tips {
  margin-top: 16px;
  padding: 12px;
  background: rgba(240, 192, 64, 0.06);
  border-radius: 8px;
}
.tips p {
  font-size: 12px;
  color: #888;
  line-height: 1.8;
  margin: 0;
}

.order-list { display: flex; flex-direction: column; gap: 10px; }
.order-card { background: #1e1e1e; border-radius: 10px; padding: 14px 16px; }
.order-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.order-no { font-size: 12px; color: #666; }
.order-detail { display: flex; justify-content: space-between; font-size: 15px; font-weight: 500; }
.order-amount { color: #f0c040; }
.order-remark { font-size: 12px; color: #888; margin-top: 6px; }
.order-remark.red { color: #e55; }
.order-time { font-size: 12px; color: #444; margin-top: 6px; }
.pay-btn { margin-top: 10px; }

/* 支付弹窗 */
.pay-dialog-body { padding: 16px; color: #333; }
.pay-notice { font-size: 13px; color: #e88; background: #fff5f5; padding: 10px; border-radius: 6px; margin-bottom: 12px; }
.pay-methods { display: flex; gap: 10px; margin-bottom: 12px; }
.pay-method {
  flex: 1; text-align: center; padding: 10px; border: 2px solid #eee; border-radius: 8px; cursor: pointer; font-size: 14px;
}
.pay-method.active { border-color: #f0c040; background: rgba(240,192,64,0.08); color: #f0c040; font-weight: 600; }
.proof-input { border: 1px solid #eee; border-radius: 8px; }
.upload-section { margin-top: 12px; }
.upload-label { font-size: 13px; color: #888; margin-bottom: 6px; }
</style>
