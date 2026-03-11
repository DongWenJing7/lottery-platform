<template>
  <div class="page">
    <div class="top-section">
      <div class="page-title">转售市场</div>
      <div class="search-bar">
        <input v-model="keyword" placeholder="搜索奖品名称" class="search-input" @keyup.enter="loadData" />
      </div>
    </div>
    <div class="market-list">
      <div v-for="item in list" :key="item.id" class="market-card">
        <div class="market-top">
          <span class="market-name">{{ item.prize?.name }}</span>
          <span class="market-price">{{ item.price }} 代币</span>
        </div>
        <div class="market-seller">卖家：{{ item.seller?.nickname }}</div>
        <van-button size="small" type="warning" round block @click="handleBuy(item)">购买</van-button>
      </div>
      <van-empty v-if="!list.length" description="暂无商品" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showToast, showConfirmDialog } from 'vant'
import { playerApi } from '@/api'
import { useUserStore } from '@/stores/userStore'

const userStore = useUserStore()
const list = ref([])
const keyword = ref('')
const buying = ref(false)

async function loadData() {
  try {
    const res = await playerApi.getMarket({ page: 1, size: 50, keyword: keyword.value || undefined })
    list.value = res.data.list
  } catch (e) {
    showToast(e.message || '加载失败')
  }
}

async function handleBuy(item) {
  if (buying.value) return
  try {
    await showConfirmDialog({ title: '确认购买？', message: `花费 ${item.price} 代币购买 ${item.prize?.name}` })
    buying.value = true
    const res = await playerApi.buyItem(item.id)
    showToast('购买成功')
    userStore.updateBalance(res.data.balanceAfter)
    loadData()
  } catch (e) {
    if (e !== 'cancel' && e?.message) {
      showToast(e.message)
    }
  } finally {
    buying.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page { color: #fff; display: flex; flex-direction: column; height: calc(100vh - 50px); }
.top-section { flex-shrink: 0; padding: 16px 16px 0; }
.page-title { font-size: 18px; font-weight: 600; margin-bottom: 12px; }
.search-bar { margin-bottom: 16px; }
.search-input { width: 100%; background: #1e1e1e; border: 1px solid #2a2a2a; border-radius: 10px; padding: 10px 16px; color: #fff; font-size: 14px; outline: none; box-sizing: border-box; }
.search-input:focus { border-color: #f0c040; }
.search-input::placeholder { color: #444; }
.market-list { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; padding: 0 16px 16px; }
.market-card { background: #1e1e1e; border-radius: 10px; padding: 14px 16px; flex-shrink: 0; }
.market-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.market-name { font-size: 15px; font-weight: 500; }
.market-price { font-size: 16px; font-weight: 700; color: #f0c040; }
.market-seller { font-size: 12px; color: #888; margin-bottom: 10px; }
</style>
