<template>
  <div>
    <div class="page-header" style="display:flex;justify-content:space-between;align-items:center">
      <h2>代理管理</h2>
      <el-button type="primary" @click="openCreate">新增代理</el-button>
    </div>
    <div class="card">
    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="level" label="等级" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.level" type="warning" size="small">V{{ row.level }}</el-tag>
          <span v-else style="color:#999">无</span>
        </template>
      </el-table-column>
      <el-table-column prop="commissionRate" label="佣金比例" width="100">
        <template #default="{ row }">{{ row.commissionRate }}%</template>
      </el-table-column>
      <el-table-column prop="teamCount" label="团队人数" width="100" />
      <el-table-column prop="teamRecharge" label="团队充值" width="100" />
      <el-table-column prop="totalCommission" label="累计佣金" width="120">
        <template #default="{ row }">¥{{ row.totalCommission }}</template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination :current-page="page" :page-size="size" :total="total" @current-change="p => { page = p; loadData() }" layout="total, prev, pager, next" />
    </div>
    </div>

    <!-- 新增代理弹窗 -->
    <el-dialog v-model="createDialog" title="新增代理" width="420" destroy-on-close>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'

const list = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)

async function loadData() {
  try {
    const res = await adminApi.getAgents({ page: page.value, size: size.value })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

// ========== 新增代理 ==========
const createDialog = ref(false)
const createForm = ref({ username: '', password: '', nickname: '' })

function openCreate() {
  createForm.value = { username: '', password: '', nickname: '' }
  createDialog.value = true
}

async function confirmCreate() {
  if (!createForm.value.username || !createForm.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  try {
    await adminApi.createUser({ ...createForm.value, role: 'agent' })
    ElMessage.success('创建成功')
    createDialog.value = false
    loadData()
  } catch (e) { ElMessage.error(e.message || '创建失败') }
}

onMounted(loadData)
</script>

<style scoped>
.page-header h2 { margin: 0 0 20px; font-size: 20px; color: #1a1a1a; }
.card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.toolbar { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
