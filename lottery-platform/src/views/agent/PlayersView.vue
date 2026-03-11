<template>
  <div class="page">
    <div class="top-section">
      <div class="page-title">我的团队</div>
      <van-search v-model="keyword" placeholder="搜索昵称/用户名" shape="round" @search="loadData" />
    </div>
    <van-pull-refresh v-model="refreshing" @refresh="loadData" class="scroll-area">
      <div class="player-list">
        <div v-for="p in list" :key="p.id" class="player-card">
          <div class="player-top">
            <span class="player-name">{{ p.nickname }}</span>
            <span class="player-recharge">充值 {{ p.totalRecharge }} 币</span>
          </div>
          <div class="player-bottom">
            <span>余额 {{ p.balance }}</span>
            <span>{{ formatTime(p.createdAt) }}</span>
          </div>
        </div>
        <van-empty v-if="!list.length && !refreshing" description="暂无团队成员" />
      </div>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showToast } from 'vant'
import { agentApi } from '@/api'

const list = ref([])
const keyword = ref('')
const refreshing = ref(false)

function formatTime(t) {
  if (!t) return ''
  return t.substring(0, 10)
}

async function loadData() {
  try {
    const res = await agentApi.getPlayers({ page: 1, size: 100, keyword: keyword.value || undefined })
    list.value = res.data.list
  } catch (e) { showToast(e.message || '操作失败') } finally { refreshing.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.page { color: #fff; display: flex; flex-direction: column; height: calc(100vh - 50px); }
.top-section { flex-shrink: 0; padding: 16px 16px 0; }
.page-title { font-size: 18px; font-weight: 600; margin-bottom: 12px; }

:deep(.van-search) { background: transparent; padding: 0 0 12px; }
:deep(.van-search__content) { background: #1e1e1e; }
:deep(.van-field__control) { color: #fff; }

.scroll-area { flex: 1; overflow-y: auto; }
.player-list { display: flex; flex-direction: column; gap: 10px; padding: 0 16px 16px; }
.player-card { background: #1e1e1e; border-radius: 10px; padding: 14px 16px; flex-shrink: 0; }
.player-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.player-name { font-size: 15px; font-weight: 500; }
.player-recharge { font-size: 13px; color: #f0c040; font-weight: 600; }
.player-bottom { display: flex; justify-content: space-between; font-size: 12px; color: #666; }
</style>
