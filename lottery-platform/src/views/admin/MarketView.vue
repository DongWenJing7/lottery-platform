<template>
  <div>
    <div class="page-header"><h2>转售市场管理</h2></div>
    <div class="card">
    <div class="toolbar">
      <el-radio-group v-model="status" @change="loadData">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="on">在售</el-radio-button>
        <el-radio-button label="sold">已售</el-radio-button>
        <el-radio-button label="off">已下架</el-radio-button>
      </el-radio-group>
      <el-input v-model="keyword" placeholder="搜索卖家/买家" clearable style="width: 220px; margin-left: 12px;" @clear="loadData">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" @click="loadData" style="margin-left: 10px;">搜索</el-button>
    </div>
    <el-table :data="list" stripe>
      <el-table-column type="index" label="序号" width="70" :index="i => (page - 1) * size + i + 1" />
      <el-table-column label="奖品" width="150">
        <template #default="{ row }">{{ row.prize?.name || '-' }}</template>
      </el-table-column>
      <el-table-column label="卖家" width="120">
        <template #default="{ row }">{{ row.seller?.nickname || '-' }}</template>
      </el-table-column>
      <el-table-column label="买家" width="120">
        <template #default="{ row }">{{ row.buyer?.nickname || '-' }}</template>
      </el-table-column>
      <el-table-column prop="price" label="价格(代币)" width="110" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="{ on: 'success', sold: 'info', off: 'danger' }[row.status]" size="small">
            {{ { on: '在售', sold: '已售', off: '已下架' }[row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="上架时间" width="170">
        <template #default="{ row }">{{ row.createdAt?.replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button v-if="row.status === 'on'" size="small" type="danger" @click="handleRemove(row)">下架</el-button>
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
const status = ref('on')
const keyword = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)

async function loadData() {
  try {
    const res = await adminApi.getMarket({ page: page.value, size: size.value, status: status.value || undefined, keyword: keyword.value || undefined })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

async function handleRemove(row) {
  try {
    await ElMessageBox.confirm('确认下架该商品？', '提示', { type: 'warning' })
    await adminApi.removeMarketItem(row.id)
    ElMessage.success('已下架')
    loadData()
  } catch (e) { if (e !== 'cancel' && e?.message) ElMessage.error(e.message) }
}

onMounted(loadData)
</script>

<style scoped>
.page-header h2 { margin: 0 0 20px; font-size: 20px; color: #1a1a1a; }
.card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.toolbar { margin-bottom: 16px; display: flex; align-items: center; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
