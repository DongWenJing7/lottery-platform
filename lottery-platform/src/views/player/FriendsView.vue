<template>
  <div class="page">
    <div class="top-section">
      <div class="page-title">好友</div>
      <div class="tabs">
        <div :class="['tab', activeTab === 'friends' && 'active']" @click="activeTab = 'friends'">我的好友</div>
        <div :class="['tab', activeTab === 'requests' && 'active']" @click="activeTab = 'requests'">
          好友申请
          <span v-if="pendingCount > 0" class="tab-badge">{{ pendingCount > 99 ? '99+' : pendingCount }}</span>
        </div>
      </div>
    </div>

    <!-- 好友列表 -->
    <div v-if="activeTab === 'friends'" class="list">
      <div v-for="friend in friends" :key="friend.id" class="list-item">
        <div class="item-main" @click="showFriendActions(friend)">
          <div class="avatar">
            <img v-if="friend.avatar" :src="friend.avatar" />
            <span v-else class="avatar-text">{{ (friend.nickname || '?')[0] }}</span>
          </div>
          <div class="item-info">
            <div class="item-name">{{ friend.nickname || friend.username }}</div>
          </div>
        </div>
        <div class="item-actions">
          <button class="btn-delete" @click="handleDelete(friend)">删除</button>
        </div>
      </div>
      <van-empty v-if="!friends.length" description="暂无好友" />
    </div>

    <!-- 好友在售商品弹窗 -->
    <van-action-sheet v-model:show="showActions" :title="selectedFriend?.nickname">
      <div class="action-list">
        <div class="action-item" @click="goChat(selectedFriend?.id)">发消息</div>
        <div class="action-item" @click="viewMarketItems(selectedFriend)">查看在售商品</div>
      </div>
    </van-action-sheet>

    <!-- 在售商品列表弹窗 -->
    <van-popup v-model:show="showMarket" position="bottom" round :style="{ maxHeight: '70%' }">
      <div class="market-popup">
        <div class="popup-title">{{ selectedFriend?.nickname }} 的在售商品</div>
        <div v-if="friendMarketItems.length" class="popup-list">
          <div v-for="item in friendMarketItems" :key="item.id" class="popup-item">
            <div class="popup-item-info">
              <div class="popup-name">{{ item.prizeName }}</div>
              <div class="popup-price">{{ item.price }} 代币</div>
            </div>
            <div class="popup-item-actions">
              <button class="popup-btn chat-btn" @click="goMarketChat(item)">💬 私聊</button>
              <button class="popup-btn buy-btn" @click="goBuy(item)">🛒 购买</button>
            </div>
          </div>
        </div>
        <van-empty v-else description="暂无在售商品" />
      </div>
    </van-popup>

    <!-- 好友申请列表 -->
    <div v-if="activeTab === 'requests'" class="list">
      <div v-for="req in requests" :key="req.id" class="list-item">
        <div class="item-main">
          <div class="avatar">
            <img v-if="req.avatar" :src="req.avatar" />
            <span v-else class="avatar-text">{{ (req.nickname || '?')[0] }}</span>
          </div>
          <div class="item-info">
            <div class="item-name">{{ req.nickname }}</div>
            <div class="item-time">{{ formatTime(req.createdAt) }}</div>
          </div>
        </div>
        <div class="item-actions">
          <button class="btn-accept" @click="handleAccept(req)">同意</button>
          <button class="btn-reject" @click="handleReject(req)">拒绝</button>
        </div>
      </div>
      <van-empty v-if="!requests.length" description="暂无好友申请" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { playerApi } from '@/api'
import { useWebSocket } from '@/composables/useWebSocket'
import { showToast, showConfirmDialog } from 'vant'

const router = useRouter()
const activeTab = ref('friends')
const friends = ref([])
const requests = ref([])
const pendingCount = ref(0)
const showActions = ref(false)
const selectedFriend = ref(null)
const showMarket = ref(false)
const friendMarketItems = ref([])

const { onMessage, offMessage } = useWebSocket()

async function loadFriends() {
  try {
    const res = await playerApi.getFriendList()
    friends.value = res.data || []
  } catch (e) { /* ignore */ }
}

async function loadRequests() {
  try {
    const res = await playerApi.getFriendRequests()
    requests.value = res.data || []
  } catch (e) { /* ignore */ }
}

async function loadPendingCount() {
  try {
    const res = await playerApi.getFriendPendingCount()
    pendingCount.value = res.data || 0
  } catch (e) { /* ignore */ }
}

function showFriendActions(friend) {
  selectedFriend.value = friend
  showActions.value = true
}

function goChat(userId) {
  showActions.value = false
  router.push(`/player/chat/${userId}`)
}

async function viewMarketItems(friend) {
  showActions.value = false
  friendMarketItems.value = []
  showMarket.value = true
  try {
    const res = await playerApi.getMarket({ page: 1, size: 50 })
    const allItems = res.data?.list || res.data || []
    const items = allItems.filter(m => m.seller?.id == friend.id)
    friendMarketItems.value = items.map(m => ({
      id: m.id, prizeName: m.prize?.name || '未知商品', price: m.price,
      sellerId: m.seller?.id, sellerName: m.seller?.nickname
    }))
  } catch (e) { /* ignore */ }
}

function goMarketChat(item) {
  showMarket.value = false
  router.push(`/player/chat/${item.sellerId}?from=market&name=${encodeURIComponent(item.sellerName)}&product=${encodeURIComponent(item.prizeName)}&price=${item.price}`)
}

async function goBuy(item) {
  try {
    await showConfirmDialog({ title: '确认购买', message: `确定花费 ${item.price} 代币购买「${item.prizeName}」？` })
    await playerApi.buyItem(item.id)
    showToast('购买成功')
    showMarket.value = false
  } catch (e) {
    if (e === 'cancel' || e?.message === 'cancel') return
    showToast(e.message || '购买失败')
  }
}

async function handleAccept(req) {
  try {
    await playerApi.acceptFriend(req.id)
    showToast('已同意')
    loadRequests()
    loadFriends()
    loadPendingCount()
  } catch (e) {
    showToast(e.message || '操作失败')
  }
}

async function handleReject(req) {
  try {
    await playerApi.rejectFriend(req.id)
    showToast('已拒绝')
    loadRequests()
    loadPendingCount()
  } catch (e) {
    showToast(e.message || '操作失败')
  }
}

async function handleDelete(friend) {
  try {
    await showConfirmDialog({ title: '确认', message: `确定删除好友 ${friend.nickname || friend.username}？` })
    await playerApi.deleteFriend(friend.id)
    showToast('已删除')
    loadFriends()
  } catch (e) {
    if (e === 'cancel' || e?.message === 'cancel') return
    showToast(e.message || '操作失败')
  }
}

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return `${d.getMonth() + 1}/${d.getDate()}`
}

function onWsMessage(data) {
  if (data.type === 'friend_request') {
    loadRequests()
    loadPendingCount()
  } else if (data.type === 'friend_accepted') {
    loadFriends()
  }
}

onMounted(() => {
  loadFriends()
  loadRequests()
  loadPendingCount()
  onMessage(onWsMessage)
})

onUnmounted(() => {
  offMessage(onWsMessage)
})
</script>

<style scoped>
.page { color: #fff; display: flex; flex-direction: column; height: calc(100vh - 50px); height: calc(100dvh - 50px); }
.top-section { flex-shrink: 0; padding: 16px 16px 0; }
.page-title { font-size: 18px; font-weight: 600; margin-bottom: 12px; }

.tabs { display: flex; gap: 0; margin-bottom: 16px; background: #1e1e1e; border-radius: 10px; overflow: hidden; }
.tab {
  flex: 1; text-align: center; padding: 10px 0; font-size: 14px; color: #888;
  cursor: pointer; position: relative; transition: all 0.2s;
}
.tab.active { color: #f0c040; background: rgba(240, 192, 64, 0.1); font-weight: 600; }
.tab-badge {
  position: absolute; top: 4px; right: 16px;
  background: #f44; color: #fff; font-size: 11px;
  min-width: 18px; height: 18px; line-height: 18px;
  text-align: center; border-radius: 9px; padding: 0 4px;
}

.list { flex: 1; overflow-y: auto; padding: 0 16px 16px; }
.list-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px; background: #1e1e1e; border-radius: 10px; margin-bottom: 8px;
}
.item-main { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; cursor: pointer; }

.avatar { width: 44px; height: 44px; flex-shrink: 0; }
.avatar img { width: 44px; height: 44px; border-radius: 50%; object-fit: cover; }
.avatar-text {
  display: flex; align-items: center; justify-content: center;
  width: 44px; height: 44px; border-radius: 50%;
  background: #f0c040; color: #000; font-weight: 600; font-size: 18px;
}

.item-info { flex: 1; min-width: 0; }
.item-name { font-size: 15px; font-weight: 500; }
.item-time { font-size: 12px; color: #666; margin-top: 2px; }

.item-actions { display: flex; gap: 8px; flex-shrink: 0; }
.btn-accept {
  background: #f0c040; color: #000; border: none; border-radius: 16px;
  padding: 6px 14px; font-size: 13px; font-weight: 600; cursor: pointer;
}
.btn-reject {
  background: #2a2a2a; color: #888; border: none; border-radius: 16px;
  padding: 6px 14px; font-size: 13px; cursor: pointer;
}
.btn-delete {
  background: transparent; color: #f44; border: 1px solid #f44; border-radius: 16px;
  padding: 5px 12px; font-size: 12px; cursor: pointer;
}

.action-list { padding: 8px 0 20px; }
.action-item {
  text-align: center; padding: 14px 0; font-size: 16px; color: #333;
  cursor: pointer; border-bottom: 1px solid #eee;
}
.action-item:last-child { border-bottom: none; }
.action-item:active { background: #f5f5f5; }

.market-popup { padding: 20px 16px; color: #333; }
.popup-title { font-size: 17px; font-weight: 700; text-align: center; padding-bottom: 14px; margin-bottom: 14px; color: #222; border-bottom: 1px solid #eee; }
.popup-list { max-height: 300px; overflow-y: auto; }
.popup-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 12px; margin-bottom: 8px;
  background: #f9f9f9; border-radius: 12px; gap: 12px;
}
.popup-item-info { flex: 1; min-width: 0; }
.popup-name { font-size: 15px; color: #222; font-weight: 500; margin-bottom: 2px; }
.popup-price { font-size: 13px; color: #e8a820; font-weight: 600; }
.popup-item-actions { display: flex; gap: 8px; flex-shrink: 0; }
.popup-btn {
  border: none; border-radius: 18px; padding: 7px 14px;
  font-size: 13px; font-weight: 600; cursor: pointer;
  transition: opacity 0.2s;
}
.popup-btn:active { opacity: 0.7; }
.chat-btn { background: #fff3d0; color: #b8860b; }
.buy-btn { background: linear-gradient(135deg, #f0c040, #e8a820); color: #fff; box-shadow: 0 2px 6px rgba(240,192,64,0.3); }
</style>
