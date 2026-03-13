<template>
  <div class="chat-page">
    <van-nav-bar :title="targetUser.nickname || '聊天'" left-arrow @click-left="$router.back()" />

    <div class="messages" ref="messagesRef" @scroll="handleScroll">
      <div v-if="loading" class="loading-tip">加载中...</div>
      <div v-if="noMore && messages.length" class="loading-tip">没有更多消息了</div>
      <div v-for="(msg, idx) in messages" :key="msg.id">
        <div v-if="shouldShowTime(msg, idx)" class="msg-time" @click="toggleTimeDetail(msg.id)">
          {{ expandedTimeIds.has(msg.id) ? formatMsgTimeFull(msg.createdAt) : formatMsgTime(msg.createdAt) }}
        </div>
        <div :class="['msg-row', msg.senderId == currentUserId ? 'mine' : 'other']">
          <div class="msg-avatar" v-if="msg.senderId != currentUserId" @click="showUserProfile">
            <img v-if="msg.senderAvatar" :src="msg.senderAvatar" />
            <span v-else class="avatar-text">{{ (msg.senderName || '?')[0] }}</span>
          </div>
          <div class="msg-bubble">{{ msg.content }}</div>
        </div>
      </div>
    </div>

    <!-- 用户资料弹窗 -->
    <van-popup v-model:show="showProfile" position="bottom" round :style="{ maxHeight: '70%' }">
      <div class="profile-popup">
        <div class="profile-header">
          <div class="profile-avatar">
            <img v-if="targetUser.avatar" :src="targetUser.avatar" />
            <span v-else class="profile-avatar-text">{{ (targetUser.nickname || '?')[0] }}</span>
          </div>
          <div class="profile-name">{{ targetUser.nickname }}</div>
        </div>
        <div class="profile-actions">
          <div class="profile-action" @click="viewTargetMarket">🛒 查看在售商品</div>
          <div v-if="!isFriend && !friendRequested" class="profile-action" @click="handleAddFriend(); showProfile = false">➕ 添加好友</div>
        </div>
        <!-- 在售商品列表 -->
        <div v-if="showTargetMarket" class="target-market">
          <div class="target-market-title">在售商品</div>
          <div v-if="targetMarketItems.length" class="target-market-list">
            <div v-for="item in targetMarketItems" :key="item.id" class="target-market-item">
              <div class="tm-info">
                <div class="tm-name">{{ item.prizeName }}</div>
                <div class="tm-price">{{ item.price }} 代币</div>
              </div>
              <button class="tm-buy-btn" @click="handleBuy(item)">购买</button>
            </div>
          </div>
          <div v-else class="target-market-empty">暂无在售商品</div>
        </div>
      </div>
    </van-popup>

    <div v-if="!canChat" class="not-friend-bar">
      <span v-if="friendRequested">好友申请已发送，等待对方确认</span>
      <template v-else>
        <span>还不是好友，</span>
        <span class="add-friend-link" @click="handleAddFriend">添加好友</span>
        <span>后才能聊天</span>
      </template>
    </div>
    <div v-else class="input-section">
      <div v-if="showMarketTip" class="market-tip">
        <template v-if="!canSend">已达上限，等待对方回复后可继续发送</template>
        <template v-else>对方回复前最多可发送{{ remainCount }}条消息</template>
      </div>
      <div class="input-bar">
        <input v-model="inputText" placeholder="输入消息..." class="msg-input" @keyup.enter="sendMessage" maxlength="500" :disabled="!canSend" />
        <button class="send-btn" @click="sendMessage" :disabled="!inputText.trim() || !canSend">发送</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { playerApi } from '@/api'
import { useWebSocket } from '@/composables/useWebSocket'
import { useUserStore } from '@/stores/userStore'
import { showToast, showConfirmDialog } from 'vant'

const route = useRoute()
const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.id)
const targetId = ref(Number(route.params.userId))
const fromMarket = route.query.from === 'market'

const targetUser = ref({ nickname: route.query.name || '' })
const messages = ref([])
const inputText = ref('')
const messagesRef = ref(null)
const page = ref(1)
const loading = ref(false)
const noMore = ref(false)
const isFriend = ref(true)
const isMarketConv = ref(false)
const hasSellerReplied = ref(false)
const friendRequested = ref(false)

// canChat: friends OR market conversation
const canChat = computed(() => isFriend.value || isMarketConv.value || fromMarket)

// Market limit: buyer can send 3 before seller replies; after reply, no limit
const MAX_CONSECUTIVE = 3
const serverConsecutiveCount = ref(0)
const showMarketTip = computed(() => !isFriend.value && (isMarketConv.value || fromMarket) && !hasSellerReplied.value)
const canSend = computed(() => {
  if (isFriend.value) return true
  if (isMarketConv.value || fromMarket) {
    if (hasSellerReplied.value) return true
    return serverConsecutiveCount.value < MAX_CONSECUTIVE
  }
  return false
})
const remainCount = computed(() => Math.max(0, MAX_CONSECUTIVE - serverConsecutiveCount.value))

async function loadChatStatus() {
  try {
    const res = await playerApi.getChatStatus(targetId.value)
    const d = res.data || {}
    isFriend.value = d.isFriend
    isMarketConv.value = d.isMarket
    hasSellerReplied.value = d.hasSellerReplied
    serverConsecutiveCount.value = d.consecutiveCount || 0
  } catch (_) {}
}

const showProfile = ref(false)
const showTargetMarket = ref(false)
const targetMarketItems = ref([])

function showUserProfile() {
  showProfile.value = true
  showTargetMarket.value = false
}

async function viewTargetMarket() {
  showTargetMarket.value = true
  targetMarketItems.value = []
  try {
    const res = await playerApi.getMarket({ page: 1, size: 50 })
    const items = (res.data?.list || res.data || []).filter(m => m.seller?.id == targetId.value)
    targetMarketItems.value = items.map(m => ({ id: m.id, prizeName: m.prize?.name || '未知商品', price: m.price }))
  } catch (_) {}
}

async function handleBuy(item) {
  try {
    await showConfirmDialog({ title: '确认购买', message: `确定花费 ${item.price} 代币购买「${item.prizeName}」？` })
    await playerApi.buyItem(item.id)
    showToast('购买成功')
    showProfile.value = false
  } catch (e) {
    if (e === 'cancel' || e?.message === 'cancel') return
    showToast(e.message || '购买失败')
  }
}

const expandedTimeIds = ref(new Set())
const { send, onMessage, offMessage } = useWebSocket()

async function loadMessages(isLoadMore = false) {
  if (loading.value || noMore.value) return
  loading.value = true
  try {
    const res = await playerApi.getChatMessages({ userId: targetId.value, page: page.value, size: 20 })
    const data = res.data || []
    if (data.length < 20) noMore.value = true

    if (isLoadMore) {
      // Prepend older messages, keep scroll position
      const el = messagesRef.value
      const prevHeight = el?.scrollHeight || 0
      messages.value = [...data.reverse(), ...messages.value]
      await nextTick()
      if (el) el.scrollTop = el.scrollHeight - prevHeight
    } else {
      messages.value = data.reverse()
      await nextTick()
      scrollToBottom()
    }
  } catch (e) {
    showToast(e.message || '加载消息失败')
  } finally {
    loading.value = false
  }
}

function scrollToBottom() {
  nextTick(() => {
    const el = messagesRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function handleScroll() {
  const el = messagesRef.value
  if (el && el.scrollTop < 50 && !loading.value && !noMore.value) {
    page.value++
    loadMessages(true)
  }
}

function shouldShowTime(msg, idx) {
  if (idx === 0) return true
  const prev = messages.value[idx - 1]
  if (!prev?.createdAt || !msg.createdAt) return false
  const prevTime = new Date(prev.createdAt).getTime()
  const curTime = new Date(msg.createdAt).getTime()
  return curTime - prevTime > 5 * 60 * 1000 // 超过5分钟显示时间
}

function toggleTimeDetail(msgId) {
  const s = new Set(expandedTimeIds.value)
  if (s.has(msgId)) s.delete(msgId)
  else s.add(msgId)
  expandedTimeIds.value = s
}

const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

function formatMsgTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  const timeStr = `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) return timeStr
  const yesterday = new Date(now); yesterday.setDate(now.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) return `昨天 ${timeStr}`
  return `${d.getMonth() + 1}/${d.getDate()} ${timeStr}`
}

function formatMsgTimeFull(time) {
  if (!time) return ''
  const d = new Date(time)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${weekDays[d.getDay()]} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

async function handleAddFriend() {
  try {
    await playerApi.sendFriendRequest(targetId.value)
    friendRequested.value = true
    showToast('好友申请已发送')
  } catch (e) {
    showToast(e.message || '发送失败')
  }
}

function sendMessage() {
  const text = inputText.value.trim()
  if (!text) return
  const msg = { receiverId: targetId.value, content: text }
  if (fromMarket && !isMarketConv.value) msg.isMarket = true
  send(msg)
  inputText.value = ''
}

function handleIncoming(data) {
  // Handle error messages from server
  if (data.type === 'error') {
    showToast(data.message)
    return
  }
  // Handle friend accepted - re-enable chat
  if (data.type === 'friend_accepted' && data.userId == targetId.value) {
    isFriend.value = true
    return
  }
  if (data.type === 'friend_request' || data.type === 'friend_accepted') return

  // Only show messages from/to the current chat target
  if (data.senderId == targetId.value || data.senderId == currentUserId.value) {
    // Avoid duplicates
    if (!messages.value.find(m => m.id === data.id)) {
      messages.value.push(data)
      scrollToBottom()
      loadChatStatus()
    }
  }
  // Mark as read if from target
  if (data.senderId == targetId.value) {
    playerApi.markAsRead(targetId.value)
  }
}

async function loadTargetUser() {
  // Try conversations first (always has nickname)
  try {
    const res = await playerApi.getConversations()
    const conv = (res.data || []).find(c => c.userId == targetId.value)
    if (conv) {
      targetUser.value = { nickname: conv.nickname, avatar: conv.avatar }
    }
  } catch (_) {}
}

onMounted(async () => {
  loadTargetUser()
  await loadMessages()
  await loadChatStatus()
  playerApi.markAsRead(targetId.value)
  onMessage(handleIncoming)

  // Auto-send product inquiry from market
  if (fromMarket && route.query.product && messages.value.length === 0 && canSend.value) {
    const msg = `你好，我对你的商品「${route.query.product}」（${route.query.price}代币）感兴趣`
    send({ receiverId: targetId.value, content: msg, isMarket: true })
  }
})

onUnmounted(() => {
  offMessage(handleIncoming)
})
</script>

<style scoped>
.chat-page { display: flex; flex-direction: column; height: 100vh; height: 100dvh; background: #111; }
:deep(.van-nav-bar) { background: #1e1e1e; flex-shrink: 0; }
:deep(.van-nav-bar__title) { color: #fff; }
:deep(.van-nav-bar__arrow) { color: #fff; }

.messages { flex: 1; overflow-y: auto; padding: 12px 16px; display: flex; flex-direction: column; gap: 8px; }
.loading-tip { text-align: center; color: #666; font-size: 12px; padding: 8px 0; }

.msg-time { text-align: center; color: #666; font-size: 11px; padding: 6px 0; cursor: pointer; user-select: none; }
.msg-time:active { color: #999; }

.msg-row { display: flex; align-items: flex-start; gap: 8px; }
.msg-row.mine { flex-direction: row-reverse; }

.msg-avatar { flex-shrink: 0; cursor: pointer; }
.msg-avatar img { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; }
.msg-avatar .avatar-text { display: flex; align-items: center; justify-content: center; width: 36px; height: 36px; border-radius: 50%; background: #f0c040; color: #000; font-weight: 600; font-size: 14px; }

.msg-bubble { max-width: 70%; padding: 10px 14px; border-radius: 12px; font-size: 14px; line-height: 1.5; word-break: break-word; }
.other .msg-bubble { background: #1e1e1e; color: #fff; border-bottom-left-radius: 4px; }
.mine .msg-bubble { background: #f0c040; color: #000; border-bottom-right-radius: 4px; }

.input-bar { flex-shrink: 0; display: flex; gap: 8px; padding: 10px 16px; }
.msg-input { flex: 1; background: #2a2a2a; border: none; border-radius: 20px; padding: 10px 16px; color: #fff; font-size: 14px; outline: none; }
.msg-input::placeholder { color: #666; }
.send-btn { flex-shrink: 0; background: #f0c040; color: #000; border: none; border-radius: 20px; padding: 0 20px; font-size: 14px; font-weight: 600; cursor: pointer; }
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.not-friend-bar {
  flex-shrink: 0; text-align: center; padding: 14px 16px;
  background: #1a1a1a; border-top: 1px solid #2a2a2a;
  color: #888; font-size: 14px;
}
.add-friend-link { color: #f0c040; font-weight: 600; cursor: pointer; }

.input-section { flex-shrink: 0; background: #1a1a1a; border-top: 1px solid #2a2a2a; }
.market-tip { text-align: center; font-size: 12px; color: #888; padding: 6px 16px 0; }

.profile-popup { padding: 20px 16px; color: #333; }
.profile-header { display: flex; flex-direction: column; align-items: center; padding-bottom: 16px; border-bottom: 1px solid #eee; }
.profile-avatar { width: 60px; height: 60px; margin-bottom: 8px; }
.profile-avatar img { width: 60px; height: 60px; border-radius: 50%; object-fit: cover; }
.profile-avatar-text { display: flex; align-items: center; justify-content: center; width: 60px; height: 60px; border-radius: 50%; background: #f0c040; color: #000; font-weight: 700; font-size: 24px; }
.profile-name { font-size: 17px; font-weight: 600; color: #222; }
.profile-actions { padding: 12px 0; }
.profile-action { text-align: center; padding: 13px 0; font-size: 15px; color: #333; cursor: pointer; border-bottom: 1px solid #f0f0f0; }
.profile-action:active { background: #f5f5f5; }
.profile-action:last-child { border-bottom: none; }

.target-market { padding-top: 8px; border-top: 1px solid #eee; }
.target-market-title { font-size: 14px; font-weight: 600; color: #999; padding: 8px 0; }
.target-market-list { max-height: 200px; overflow-y: auto; }
.target-market-item { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; margin-bottom: 6px; background: #f9f9f9; border-radius: 10px; }
.tm-info { flex: 1; min-width: 0; }
.tm-name { font-size: 14px; color: #222; font-weight: 500; }
.tm-price { font-size: 12px; color: #e8a820; font-weight: 600; }
.tm-buy-btn { border: none; border-radius: 16px; padding: 6px 14px; font-size: 12px; font-weight: 600; background: linear-gradient(135deg, #f0c040, #e8a820); color: #fff; cursor: pointer; flex-shrink: 0; }
.tm-buy-btn:active { opacity: 0.7; }
.target-market-empty { text-align: center; color: #999; font-size: 13px; padding: 16px 0; }
</style>
