<template>
  <div class="page">
    <div class="top-section">
      <div class="page-header">
        <div class="page-title">我的仓库</div>
        <div v-if="!selectMode" class="header-btn" @click="enterSelect">管理</div>
        <div v-else class="header-actions">
          <span class="select-count">已选 {{ selectedIds.length }} 件</span>
          <div class="header-btn" @click="exitSelect">取消</div>
        </div>
      </div>

      <div class="tabs">
        <span v-for="t in tabs" :key="t.value" :class="['tab', { active: tab === t.value }]" @click="tab = t.value; exitSelect(); loadData()">{{ t.label }}</span>
      </div>

      <!-- 批量操作栏 -->
      <div v-if="selectMode && selectedIds.length" class="batch-bar">
        <van-button size="small" type="danger" plain round @click="batchDelete">批量删除</van-button>
      </div>
    </div>

    <div class="item-list">
      <div
        v-for="item in list" :key="item.id"
        class="item-card"
        :class="{ selected: selectedIds.includes(item.id) }"
        @click="selectMode && item.status === 'held' ? toggleSelect(item.id) : null"
      >
        <div class="item-top">
          <div class="item-left">
            <van-checkbox
              v-if="selectMode && item.status === 'held'"
              :model-value="selectedIds.includes(item.id)"
              @click.stop="toggleSelect(item.id)"
              icon-size="18"
            />
            <span class="item-name">{{ item.prize?.name }}</span>
          </div>
          <van-tag :type="item.status === 'held' ? 'success' : 'warning'" size="medium">
            {{ { held: '持有', selling: '售卖中', delivered: '已发货' }[item.status] }}
          </van-tag>
        </div>
        <div class="item-info">
          <span>市场价值 ¥{{ item.prize?.value || 0 }}</span>
        </div>
        <div v-if="!selectMode && item.status === 'held'" class="item-actions">
          <div class="action-btn delete" @click="handleDelete(item)">
            <span class="action-icon">🗑</span><span>删除</span>
          </div>
          <div class="action-btn sell" @click="openSell(item)">
            <span class="action-icon">💰</span><span>转售</span>
          </div>
          <div class="action-btn deliver" @click="handleDeliver(item)">
            <span class="action-icon">📦</span><span>发货</span>
          </div>
        </div>
        <div v-if="!selectMode && item.status === 'selling'" class="item-actions selling">
          <span class="sell-price">💰 挂售中 · {{ item.sellPrice }} 代币</span>
          <div class="action-btn unsell" @click="handleUnsell(item)">
            <span>取消挂售</span>
          </div>
        </div>
      </div>
      <van-empty v-if="!list.length" description="暂无物品" />
    </div>

    <!-- 底部全选栏 -->
    <div v-if="selectMode" class="select-footer">
      <van-checkbox :model-value="isAllSelected" @click="toggleAll" icon-size="18">全选</van-checkbox>
    </div>

    <van-dialog v-model:show="sellDialog" title="挂售到市场" show-cancel-button @confirm="confirmSell">
      <div style="padding:16px">
        <van-field v-model="sellPrice" type="number" label="售价(代币)" placeholder="请输入售价" />
      </div>
    </van-dialog>

    <van-dialog v-model:show="deliverDialog" title="申请发货" show-cancel-button @confirm="confirmDeliver">
      <div style="padding:16px;display:flex;flex-direction:column;gap:10px">
        <van-field v-model="deliverForm.receiverName" label="收件人" placeholder="请输入姓名" />
        <van-field v-model="deliverForm.receiverPhone" label="手机号" placeholder="请输入手机号" />
        <van-field v-model="deliverForm.receiverAddress" label="地址" type="textarea" placeholder="请输入详细地址" />
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { showToast, showConfirmDialog } from 'vant'
import { playerApi } from '@/api'
import { useUserStore } from '@/stores/userStore'

const userStore = useUserStore()

const list = ref([])
const tab = ref('')
const tabs = [
  { label: '全部', value: '' },
  { label: '持有', value: 'held' },
  { label: '售卖中', value: 'selling' },
]

// ========== 多选模式 ==========
const selectMode = ref(false)
const selectedIds = ref([])

const heldItems = computed(() => list.value.filter(i => i.status === 'held'))
const isAllSelected = computed(() => heldItems.value.length > 0 && heldItems.value.every(i => selectedIds.value.includes(i.id)))
function enterSelect() { selectMode.value = true; selectedIds.value = [] }
function exitSelect() { selectMode.value = false; selectedIds.value = [] }

function toggleSelect(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx > -1) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(id)
  }
}

function toggleAll() {
  if (isAllSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = heldItems.value.map(i => i.id)
  }
}

async function batchDelete() {
  if (!selectedIds.value.length) return
  try {
    await showConfirmDialog({ title: '批量删除', message: `确认删除 ${selectedIds.value.length} 件物品？删除后无法恢复` })
    for (const id of selectedIds.value) {
      await playerApi.deleteItem(id)
    }
    showToast(`已删除 ${selectedIds.value.length} 件`)
    exitSelect()
    loadData()
  } catch (e) { if (e !== 'cancel' && e?.message) showToast(e.message) }
}

// ========== 单项操作 ==========
async function loadData() {
  try {
    const res = await playerApi.getWarehouse({ page: 1, size: 50, status: tab.value || undefined })
    list.value = (res.data.list || []).sort((a, b) => {
      const ja = a.prize?.isJackpot || 0, jb = b.prize?.isJackpot || 0
      if (jb !== ja) return jb - ja
      return (b.prize?.value || 0) - (a.prize?.value || 0)
    })
  } catch (e) { showToast(e.message || '操作失败') }
}

async function handleDelete(item) {
  try {
    await showConfirmDialog({ title: '确认删除？', message: '删除后无法恢复' })
    await playerApi.deleteItem(item.id)
    showToast('已删除')
    loadData()
  } catch (e) { if (e !== 'cancel' && e?.message) showToast(e.message) }
}

const sellDialog = ref(false)
const sellPrice = ref('')
const sellItem = ref(null)

function openSell(item) { sellItem.value = item; sellPrice.value = ''; sellDialog.value = true }

async function confirmSell() {
  if (!sellPrice.value || sellPrice.value <= 0) { showToast('请输入有效售价'); return }
  try {
    await playerApi.listForSale(sellItem.value.id, Number(sellPrice.value))
    showToast('挂售成功')
    loadData()
  } catch (e) { showToast(e.message || '操作失败') }
}

async function handleUnsell(item) {
  try {
    await playerApi.unsell(item.id)
    showToast('已取消挂售')
    loadData()
  } catch (e) { showToast(e.message || '操作失败') }
}

const deliverDialog = ref(false)
const deliverItem = ref(null)
const deliverForm = ref({ receiverName: '', receiverPhone: '', receiverAddress: '' })

function handleDeliver(item) {
  deliverItem.value = item
  const u = userStore.userInfo
  deliverForm.value = {
    receiverName: u?.nickname || '',
    receiverPhone: u?.username || '',
    receiverAddress: u?.address || '',
  }
  deliverDialog.value = true
}

async function confirmDeliver() {
  const { receiverName, receiverPhone, receiverAddress } = deliverForm.value
  if (!receiverName || !receiverPhone || !receiverAddress) { showToast('请填写完整收货信息'); return }
  try {
    await playerApi.requestDelivery(deliverItem.value.id, deliverForm.value)
    showToast('发货申请已提交')
    loadData()
  } catch (e) { showToast(e.message || '操作失败') }
}

onMounted(loadData)
</script>

<style scoped>
.page { color: #fff; display: flex; flex-direction: column; height: calc(100vh - 50px); }
.top-section { flex-shrink: 0; padding: 16px 16px 0; }

.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.page-title { font-size: 18px; font-weight: 600; }
.header-btn {
  font-size: 13px; color: #f0c040; cursor: pointer;
  padding: 4px 12px; border: 1px solid rgba(240,192,64,0.3); border-radius: 14px;
}
.header-actions { display: flex; align-items: center; gap: 10px; }
.select-count { font-size: 13px; color: #aaa; }

.tabs { display: flex; gap: 10px; margin-bottom: 16px; }
.tab { padding: 6px 16px; border-radius: 20px; background: #1e1e1e; font-size: 13px; color: #aaa; cursor: pointer; }
.tab.active { background: #f0c040; color: #111; font-weight: 600; }

.batch-bar {
  display: flex; gap: 10px; margin-bottom: 12px;
  padding: 10px 14px; background: #1a1a1a; border-radius: 10px;
}

.item-list { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; padding: 0 16px 16px; }
.item-card {
  background: #1e1e1e; border-radius: 10px; padding: 14px 16px;
  border: 2px solid transparent; transition: border-color 0.2s; flex-shrink: 0;
}
.item-card.selected { border-color: #f0c040; background: rgba(240,192,64,0.04); }

.item-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.item-left { display: flex; align-items: center; gap: 10px; }
.item-name { font-size: 15px; font-weight: 500; }
.item-info { font-size: 12px; color: #888; margin-bottom: 10px; }
.item-actions {
  display: flex; gap: 8px; align-items: center;
  padding-top: 10px; border-top: 1px solid #2a2a2a; margin-top: 4px;
}
.item-actions.selling { justify-content: space-between; }

.action-btn {
  display: flex; align-items: center; gap: 4px;
  padding: 7px 14px; border-radius: 20px; font-size: 12px; font-weight: 500;
  cursor: pointer; transition: all 0.2s; user-select: none;
}
.action-btn:active { transform: scale(0.95); opacity: 0.8; }
.action-icon { font-size: 13px; }

.action-btn.delete {
  background: rgba(255,75,75,0.1); color: #ff6b6b; border: 1px solid rgba(255,75,75,0.2);
}
.action-btn.sell {
  background: rgba(240,192,64,0.1); color: #f0c040; border: 1px solid rgba(240,192,64,0.2);
}
.action-btn.deliver {
  background: rgba(64,169,255,0.1); color: #40a9ff; border: 1px solid rgba(64,169,255,0.2);
}
.action-btn.unsell {
  background: rgba(255,75,75,0.1); color: #ff6b6b; border: 1px solid rgba(255,75,75,0.2);
  padding: 6px 14px;
}

.sell-price {
  font-size: 13px; color: #f0c040; font-weight: 500;
}

.select-footer {
  position: fixed; bottom: 60px; left: 0; right: 0;
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 20px; background: #1a1a1a;
  border-top: 1px solid #2a2a2a;
  z-index: 10;
}
.select-footer :deep(.van-checkbox__label) { color: #fff; font-size: 14px; }
</style>
