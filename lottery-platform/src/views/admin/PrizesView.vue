<template>
  <div>
    <!-- 抽奖配置 -->
    <div class="page-header"><h2>抽奖配置</h2></div>
    <div class="card config-card">
      <el-form :model="config" label-width="120px" inline>
        <el-form-item label="单次消耗代币">
          <el-input-number v-model="config.costTokens" :min="1" />
        </el-form-item>
        <el-form-item label="保底次数">
          <el-input-number v-model="config.jackpotThreshold" :min="1" />
        </el-form-item>
        <el-form-item label="大奖池奖品">
          <el-select v-model="config.jackpotPrizeIds" multiple placeholder="选择大奖奖品" style="width:300px">
            <el-option v-for="p in jackpotOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveConfig">保存配置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 奖品管理 -->
    <div class="page-header" style="display:flex;justify-content:space-between;align-items:center;margin-top:24px">
      <h2>奖品管理</h2>
      <el-button type="primary" @click="openForm()">新增奖品</el-button>
    </div>
    <div class="card">
      <el-table :data="list" stripe>
        <el-table-column type="index" label="序号" width="70" :index="i => (page - 1) * psize + i + 1" />
        <el-table-column label="图片" width="80">
          <template #default="{ row }">
            <el-image v-if="row.image" :src="row.image" :preview-src-list="[row.image]" preview-teleported style="width:40px;height:40px;border-radius:6px;cursor:pointer" fit="cover" />
            <span v-else style="color:#ccc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="value" label="市场价" width="100" />
        <el-table-column label="中奖率" width="90">
          <template #default="{ row }">{{ calcPercent(row.probability) }}</template>
        </el-table-column>
        <el-table-column prop="isJackpot" label="大奖" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.isJackpot" type="warning" size="small">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80">
          <template #default="{ row }">{{ row.stock === -1 ? '不限' : row.stock }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-switch :model-value="!!row.status" @change="val => toggleStatus(row, val)" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="openForm(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination :current-page="page" :page-size="psize" :total="total" @current-change="p => { page = p; loadPrizes() }" layout="total, prev, pager, next" />
      </div>
    </div>

    <!-- 奖品表单弹窗 -->
    <el-dialog v-model="dialog" :title="form.id ? '编辑奖品' : '新增奖品'" width="500">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="奖品图片">
          <div style="display:flex;align-items:flex-end;gap:10px">
            <el-image v-if="form.image" :src="form.image" :preview-src-list="[form.image]" preview-teleported class="prize-preview" fit="cover" />
            <el-upload
              class="prize-uploader"
              action="/api/upload/image"
              :headers="uploadHeaders"
              :show-file-list="false"
              accept="image/*"
              :on-success="onUploadSuccess"
              :before-upload="beforeUpload"
            >
              <el-button size="small" type="primary">{{ form.image ? '更换图片' : '上传图片' }}</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="市场价值"><el-input-number v-model="form.value" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="中奖权重">
          <div class="weight-control">
            <el-slider
              v-model="form.probability"
              :min="0"
              :max="weightMax"
              :step="1"
              :show-tooltip="false"
              class="weight-slider"
            />
            <el-input-number
              v-model="form.probability"
              :min="0"
              :step="10"
              :precision="0"
              size="small"
              class="weight-input"
            />
          </div>
          <div class="weight-info">
            <div class="weight-percent">
              中奖概率：<span class="percent-value">{{ calcPercentWithForm(form.probability) }}</span>
            </div>
            <div class="weight-hint">权重越大越容易抽中，系统自动按比例计算概率</div>
          </div>
        </el-form-item>
        <el-form-item label="是否大奖"><el-switch v-model="form.isJackpotBool" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="form.stock" :min="-1" />（-1不限）</el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="confirmForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { adminApi } from '@/api'
import { useUserStore } from '@/stores/userStore'

const userStore = useUserStore()
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${userStore.token}` }))

function beforeUpload(file) {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) { ElMessage.error('只能上传图片'); return false }
  if (!isLt2M) { ElMessage.error('图片不能超过 2MB'); return false }
  return true
}

function onUploadSuccess(res) {
  if (res.code === 200) {
    form.value.image = res.data.url
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

// ========== 抽奖配置 ==========
const config = ref({ costTokens: 10, jackpotThreshold: 50, jackpotPrizeIds: [] })

async function loadConfig() {
  try {
    const res = await adminApi.getLotteryConfig()
    const d = res.data
    config.value = {
      costTokens: d.costTokens ?? 10,
      jackpotThreshold: d.jackpotThreshold ?? 50,
      jackpotPrizeIds: d.jackpotPrizeIds ? d.jackpotPrizeIds.split(',').map(Number).filter(Boolean) : [],
    }
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

async function saveConfig() {
  try {
    await adminApi.updateLotteryConfig({
      costTokens: config.value.costTokens,
      jackpotThreshold: config.value.jackpotThreshold,
      jackpotPrizeIds: config.value.jackpotPrizeIds.join(','),
    })
    ElMessage.success('配置已保存')
  } catch (e) { ElMessage.error(e.message || '保存失败') }
}

// ========== 概率计算 ==========
const totalWeight = computed(() => list.value.reduce((sum, p) => sum + (p.probability || 0), 0))
const weightMax = computed(() => Math.max(totalWeight.value * 2, 10000))

function calcPercent(weight) {
  const total = totalWeight.value
  if (!total || !weight) return '0%'
  const pct = (weight / total) * 100
  return pct >= 1 ? pct.toFixed(1) + '%' : pct.toFixed(2) + '%'
}

function calcPercentWithForm(weight) {
  // 编辑时：用当前列表总权重 - 原来的权重 + 新权重
  const origWeight = form.value.id ? (list.value.find(p => p.id === form.value.id)?.probability || 0) : 0
  const total = totalWeight.value - origWeight + (weight || 0)
  if (!total || !weight) return '0%'
  const pct = (weight / total) * 100
  return pct >= 1 ? pct.toFixed(1) + '%' : pct.toFixed(2) + '%'
}

// 大奖池下拉选项：只显示标记为大奖的奖品
const jackpotOptions = computed(() => list.value.filter(p => p.isJackpot))

// ========== 奖品管理 ==========
const list = ref([])
const page = ref(1)
const psize = ref(20)
const total = ref(0)
const dialog = ref(false)
const form = ref({})

async function loadPrizes() {
  try {
    const res = await adminApi.getPrizes({ page: page.value, size: psize.value })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

function openForm(row) {
  if (row) {
    form.value = { ...row, isJackpotBool: !!row.isJackpot }
  } else {
    form.value = { name: '', image: '', value: 0, probability: 0, isJackpotBool: false, recycleTokens: 0, stock: -1 }
  }
  dialog.value = true
}

async function confirmForm() {
  const data = { ...form.value, isJackpot: form.value.isJackpotBool ? 1 : 0 }
  if (!data.id) data.status = 1
  delete data.isJackpotBool
  try {
    if (data.id) {
      await adminApi.updatePrize(data.id, data)
    } else {
      await adminApi.createPrize(data)
    }
    ElMessage.success('保存成功')
    dialog.value = false
    loadPrizes()
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

async function toggleStatus(row, val) {
  try {
    await adminApi.updatePrize(row.id, { ...row, status: val ? 1 : 0 })
    ElMessage.success(val ? '已启用' : '已禁用')
    loadPrizes()
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除 ${row.name}？`, '提示', { type: 'warning' })
    await adminApi.deletePrize(row.id)
    ElMessage.success('已删除')
    loadPrizes()
  } catch (e) { if (e !== 'cancel' && e?.message) ElMessage.error(e.message) }
}

onMounted(() => {
  loadPrizes()
  loadConfig()
})
</script>

<style scoped>
.page-header h2 { margin: 0 0 20px; font-size: 20px; color: #1a1a1a; }
.card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.config-card :deep(.el-form-item) { margin-bottom: 12px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }

.prize-uploader :deep(.el-upload) {
  width: 100px; height: 100px; border: 1px dashed #dcdfe6; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; cursor: pointer;
  overflow: hidden; transition: border-color 0.2s;
}
.prize-uploader :deep(.el-upload:hover) { border-color: #409eff; }
.prize-preview { width: 100px; height: 100px; object-fit: cover; }
.prize-upload-icon { font-size: 28px; color: #8c939d; }

.weight-control { display: flex; align-items: center; gap: 12px; width: 100%; }
.weight-slider { flex: 1; }
.weight-input { width: 120px; }
.weight-info { margin-top: 8px; }
.weight-percent { font-size: 13px; color: #606266; }
.percent-value { font-size: 15px; font-weight: 600; color: #e6a23c; }
.weight-hint { font-size: 12px; color: #999; margin-top: 4px; }
</style>
