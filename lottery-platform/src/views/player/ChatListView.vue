<template>
  <div class="page">
    <div class="top-section">
      <div class="title-row">
        <div class="page-title">消息</div>
        <div class="friend-entry" @click="router.push('/player/friends')">
          好友
          <span v-if="friendPendingCount > 0" class="friend-badge">{{ friendPendingCount > 99 ? '99+' : friendPendingCount }}</span>
        </div>
      </div>
      <div class="search-bar">
        <input v-model="searchKeyword" placeholder="搜索用户发起聊天" class="search-input" @input="handleSearch" />
      </div>
    </div>

    <!-- Search results -->
    <div v-if="searchKeyword && searchResults.length" class="chat-list">
      <div v-for="user in searchResults" :key="user.id" class="chat-item">
        <div class="item-left" @click="user.friendStatus === 1 ? goChat(user.id) : null">
          <div class="avatar">
            <img v-if="user.avatar" :src="user.avatar" />
            <span v-else class="avatar-text">{{ (user.nickname || user.username || '?')[0] }}</span>
          </div>
          <div class="chat-info">
            <div class="chat-name">{{ user.nickname || user.username }}</div>
            <div class="chat-last">
              <template v-if="user.friendStatus === 1">点击发送消息</template>
              <template v-else-if="user.friendStatus === 0">已发送申请</template>
              <template v-else>未添加好友</template>
            </div>
          </div>
        </div>
        <div class="item-right">
          <button v-if="user.friendStatus === 1" class="action-btn msg-btn" @click="goChat(user.id)">发消息</button>
          <button v-else-if="user.friendStatus === 0" class="action-btn pending-btn" disabled>待确认</button>
          <button v-else class="action-btn add-btn" @click="addFriend(user)">加好友</button>
        </div>
      </div>
    </div>

    <div v-else-if="searchKeyword && !searchResults.length" class="empty-tip">未找到用户</div>

    <!-- Conversation list -->
    <div v-else class="chat-list">
      <van-swipe-cell v-for="conv in conversations" :key="conv.userId">
        <div class="chat-item" @click="goChat(conv.userId)">
          <div class="avatar">
            <img v-if="conv.avatar" :src="conv.avatar" />
            <span v-else class="avatar-text">{{ (conv.nickname || '?')[0] }}</span>
            <span v-if="conv.unreadCount > 0" class="badge">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
          </div>
          <div class="chat-info">
            <div class="chat-top">
              <span class="chat-name">{{ conv.nickname }}</span>
              <span class="chat-time">{{ formatTime(conv.lastTime) }}</span>
            </div>
            <div class="chat-last">{{ conv.lastMessage }}</div>
          </div>
        </div>
        <template #right>
          <van-button square type="danger" text="删除" class="swipe-del-btn" @click="deleteConv(conv)" />
        </template>
      </van-swipe-cell>
      <van-empty v-if="!conversations.length" description="暂无消息" />
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
const conversations = ref([])
const searchKeyword = ref('')
const searchResults = ref([])
const friendPendingCount = ref(0)
let searchTimer = null

const { onMessage, offMessage } = useWebSocket()

async function loadConversations() {
  try {
    const res = await playerApi.getConversations()
    conversations.value = res.data || []
  } catch (e) {
    showToast(e.message || '加载失败')
  }
}

function handleSearch() {
  clearTimeout(searchTimer)
  if (!searchKeyword.value.trim()) {
    searchResults.value = []
    return
  }
  searchTimer = setTimeout(async () => {
    try {
      const res = await playerApi.searchUsers(searchKeyword.value.trim())
      searchResults.value = res.data || []
    } catch (e) {
      searchResults.value = []
    }
  }, 300)
}

function goChat(userId) {
  router.push(`/player/chat/${userId}`)
}

async function deleteConv(conv) {
  try {
    await showConfirmDialog({ title: '确认', message: `删除与 ${conv.nickname} 的聊天记录？` })
    await playerApi.deleteConversation(conv.userId)
    conversations.value = conversations.value.filter(c => c.userId !== conv.userId)
    showToast('已删除')
  } catch (e) {
    if (e === 'cancel' || e?.message === 'cancel') return
    showToast(e.message || '删除失败')
  }
}

async function addFriend(user) {
  try {
    await playerApi.sendFriendRequest(user.id)
    showToast('好友申请已发送')
    user.friendStatus = 0
  } catch (e) {
    showToast(e.message || '发送失败')
  }
}

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return `${d.getMonth() + 1}/${d.getDate()}`
}

// Listen for new messages to update conversation list
function onNewMessage() {
  loadConversations()
}

async function loadPendingCount() {
  try {
    const res = await playerApi.getFriendPendingCount()
    friendPendingCount.value = res.data || 0
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  loadConversations()
  loadPendingCount()
  onMessage(onNewMessage)
})

onUnmounted(() => {
  offMessage(onNewMessage)
  clearTimeout(searchTimer)
})
</script>

<style scoped>
.page { color: #fff; display: flex; flex-direction: column; height: calc(100vh - 50px); height: calc(100dvh - 50px); }
.top-section { flex-shrink: 0; padding: 16px 16px 0; }
.title-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.page-title { font-size: 18px; font-weight: 600; }
.friend-entry {
  position: relative; font-size: 14px; color: #f0c040; font-weight: 600;
  padding: 6px 14px; background: rgba(240,192,64,0.1); border-radius: 16px; cursor: pointer;
}
.friend-badge {
  position: absolute; top: -6px; right: -6px;
  background: #f44; color: #fff; font-size: 10px;
  min-width: 16px; height: 16px; line-height: 16px;
  text-align: center; border-radius: 8px; padding: 0 3px;
}
.search-bar { margin-bottom: 16px; }
.search-input { width: 100%; background: #1e1e1e; border: 1px solid #2a2a2a; border-radius: 10px; padding: 10px 16px; color: #fff; font-size: 14px; outline: none; box-sizing: border-box; }
.search-input:focus { border-color: #f0c040; }
.search-input::placeholder { color: #444; }

.chat-list { flex: 1; overflow-y: auto; padding: 0 16px 16px; }
.chat-item { display: flex; align-items: center; gap: 12px; padding: 12px; background: #1e1e1e; border-radius: 10px; margin-bottom: 8px; cursor: pointer; }
.chat-item:active { background: #2a2a2a; }

.avatar { position: relative; width: 44px; height: 44px; flex-shrink: 0; }
.avatar img { width: 44px; height: 44px; border-radius: 50%; object-fit: cover; }
.avatar-text { display: flex; align-items: center; justify-content: center; width: 44px; height: 44px; border-radius: 50%; background: #f0c040; color: #000; font-weight: 600; font-size: 18px; }
.badge { position: absolute; top: -4px; right: -4px; background: #f44; color: #fff; font-size: 11px; min-width: 18px; height: 18px; line-height: 18px; text-align: center; border-radius: 9px; padding: 0 4px; }

.chat-info { flex: 1; min-width: 0; }
.chat-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.chat-name { font-size: 15px; font-weight: 500; }
.chat-time { font-size: 12px; color: #666; flex-shrink: 0; }
.chat-last { font-size: 13px; color: #888; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.empty-tip { text-align: center; color: #666; padding: 40px 0; }

.item-left { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; cursor: pointer; }
.item-right { flex-shrink: 0; }
.action-btn { border: none; border-radius: 16px; padding: 6px 14px; font-size: 13px; font-weight: 600; cursor: pointer; }
.add-btn { background: #f0c040; color: #000; }
.msg-btn { background: #f0c040; color: #000; }
.pending-btn { background: #2a2a2a; color: #666; cursor: not-allowed; }

.swipe-del-btn { height: 100% !important; }
</style>
