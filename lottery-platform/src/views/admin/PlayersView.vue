<template>
  <div>
    <div class="page-header" style="display:flex;justify-content:space-between;align-items:center">
      <h2>玩家管理</h2>
      <el-button type="primary" @click="openCreate">新增用户</el-button>
    </div>
    <div class="card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索昵称/用户名" style="width:260px" @keyup.enter="loadData" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <el-table :data="list" stripe>
        <el-table-column type="index" label="序号" width="70" :index="i => (page - 1) * size + i + 1" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="balance" label="余额" width="90" />
        <el-table-column prop="agentNickname" label="所属代理" width="110">
          <template #default="{ row }">{{ row.agentNickname || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '封禁' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openTokens(row)">调整代币</el-button>
            <el-button size="small" type="warning" link @click="openWarehouse(row)">管理仓库</el-button>
            <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" link @click="toggleStatus(row)">
              {{ row.status === 1 ? '封禁' : '解封' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination :current-page="page" :page-size="size" :total="total" @current-change="p => { page = p; loadData() }" layout="total, prev, pager, next" background small />
      </div>
    </div>

    <!-- 新增用户弹窗 -->
    <el-dialog v-model="createDialog" title="新增用户" width="420" destroy-on-close>
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="createForm.username" placeholder="登录用户名" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="createForm.password" placeholder="登录密码" show-password /></el-form-item>
        <el-form-item label="昵称"><el-input v-model="createForm.nickname" placeholder="显示昵称（选填）" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 调整代币弹窗 -->
    <el-dialog v-model="tokenDialog" title="调整代币" width="420" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="数量"><el-input-number v-model="tokenForm.amount" :min="-99999" :max="99999" style="width:100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="tokenForm.remark" placeholder="如：淘宝充值到账" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tokenDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmTokens">确认</el-button>
      </template>
    </el-dialog>

    <!-- 管理仓库弹窗 -->
    <el-dialog v-model="warehouseDialog" :title="`管理仓库 - ${warehouseUser?.nickname || ''}`" width="600" destroy-on-close>
      <div style="margin-bottom:16px;display:flex;gap:10px;align-items:center">
        <el-select v-model="addPrizeId" placeholder="选择奖品" style="flex:1" filterable>
          <el-option v-for="p in prizeOptions" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-button type="primary" @click="handleAddItem" :disabled="!addPrizeId">添加奖品</el-button>
      </div>
      <el-table :data="warehouseItems" stripe size="small">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column label="奖品">
          <template #default="{ row }">{{ row.prize?.name || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="{ held: 'success', selling: 'warning', delivered: 'info' }[row.status]" size="small">
              {{ { held: '持有', selling: '售卖中', delivered: '已发货' }[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button size="small" type="danger" link @click="handleRemoveItem(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'

const list = ref([])
const keyword = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)

async function loadData() {
  try {
    const res = await adminApi.getPlayers({ page: page.value, size: size.value, keyword: keyword.value || undefined, role: 'player' })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

// ========== 新增用户 ==========
const createDialog = ref(false)
const createForm = ref({ username: '', password: '', nickname: '', role: 'player' })

function openCreate() {
  createForm.value = { username: '', password: '', nickname: '', role: 'player' }
  createDialog.value = true
}

async function confirmCreate() {
  if (!createForm.value.username || !createForm.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  try {
    await adminApi.createUser(createForm.value)
    ElMessage.success('创建成功')
    createDialog.value = false
    loadData()
  } catch (e) { ElMessage.error(e.message || '创建失败') }
}

// ========== 调整代币 ==========
const tokenDialog = ref(false)
const tokenRow = ref(null)
const tokenForm = ref({ amount: 0, remark: '' })

function openTokens(row) {
  tokenRow.value = row
  tokenForm.value = { amount: 0, remark: '' }
  tokenDialog.value = true
}

async function confirmTokens() {
  try {
    await adminApi.updateTokens(tokenRow.value.id, tokenForm.value)
    ElMessage.success('操作成功')
    tokenDialog.value = false
    loadData()
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

// ========== 管理仓库 ==========
const warehouseDialog = ref(false)
const warehouseUser = ref(null)
const warehouseItems = ref([])
const prizeOptions = ref([])
const addPrizeId = ref(null)

async function openWarehouse(row) {
  warehouseUser.value = row
  addPrizeId.value = null
  warehouseDialog.value = true
  await loadWarehouse()
  // 加载奖品选项
  try {
    const res = await adminApi.getPrizes({ page: 1, size: 200 })
    prizeOptions.value = res.data.list
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

async function loadWarehouse() {
  try {
    const res = await adminApi.getWarehouseItems(warehouseUser.value.id)
    warehouseItems.value = res.data
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

async function handleAddItem() {
  if (!addPrizeId.value) return
  try {
    await adminApi.addWarehouseItem(warehouseUser.value.id, addPrizeId.value)
    ElMessage.success('已添加')
    addPrizeId.value = null
    loadWarehouse()
  } catch (e) { ElMessage.error(e.message || '添加失败') }
}

async function handleRemoveItem(row) {
  try {
    await adminApi.removeWarehouseItem(warehouseUser.value.id, row.id)
    ElMessage.success('已删除')
    loadWarehouse()
  } catch (e) { ElMessage.error(e.message || '删除失败') }
}

async function toggleStatus(row) {
  try {
    await adminApi.setStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success('操作成功')
    loadData()
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

onMounted(loadData)
</script>

<style scoped>
.page-header h2 { margin: 0 0 20px; font-size: 20px; color: #1a1a1a; }
.card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.toolbar { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
