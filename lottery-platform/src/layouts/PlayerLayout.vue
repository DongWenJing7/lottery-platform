<template>
  <div class="player-layout" v-if="isMobile">
    <router-view />
    <van-tabbar route v-show="route.name !== 'Chat'">
      <van-tabbar-item icon="home-o" to="/player/home">首页</van-tabbar-item>
      <van-tabbar-item icon="gift-o" to="/player/lottery">抽奖</van-tabbar-item>
      <van-tabbar-item icon="exchange" to="/player/market">市场</van-tabbar-item>
      <van-tabbar-item icon="chat-o" to="/player/chat" :badge="unreadCount > 0 ? (unreadCount > 99 ? '99+' : unreadCount) : ''">消息</van-tabbar-item>
      <van-tabbar-item icon="user-o" to="/player/profile">我的</van-tabbar-item>
    </van-tabbar>

    <!-- 新消息提醒弹框 -->
    <transition name="notify-slide">
      <div v-if="notification" class="msg-notify" @click="goToChat(notification.senderId)">
        <div class="notify-avatar">
          <img v-if="notification.senderAvatar" :src="notification.senderAvatar" />
          <span v-else class="notify-avatar-text">{{ (notification.senderName || '?')[0] }}</span>
        </div>
        <div class="notify-body">
          <div class="notify-name">{{ notification.senderName }}</div>
          <div class="notify-content">{{ notification.content }}</div>
        </div>
        <div class="notify-close" @click.stop="notification = null">&times;</div>
      </div>
    </transition>
  </div>
  <DesktopSidebar v-else title="玩家中心" :menu-groups="menuGroups">
    <router-view />
    <!-- 桌面端新消息提醒 -->
    <transition name="notify-slide">
      <div v-if="notification" class="msg-notify desktop-notify" @click="goToChat(notification.senderId)">
        <div class="notify-avatar">
          <img v-if="notification.senderAvatar" :src="notification.senderAvatar" />
          <span v-else class="notify-avatar-text">{{ (notification.senderName || '?')[0] }}</span>
        </div>
        <div class="notify-body">
          <div class="notify-name">{{ notification.senderName }}</div>
          <div class="notify-content">{{ notification.content }}</div>
        </div>
        <div class="notify-close" @click.stop="notification = null">&times;</div>
      </div>
    </transition>
  </DesktopSidebar>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { playerApi } from '@/api'
import { showToast } from 'vant'
import { useIsMobile } from '@/composables/useIsMobile'
import { useWebSocket } from '@/composables/useWebSocket'
import DesktopSidebar from '@/components/DesktopSidebar.vue'
import { HomeFilled, Present, Box, Shop, Tickets, List, User, ChatDotSquare, Avatar } from '@element-plus/icons-vue'

const { isMobile } = useIsMobile()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const unreadCount = ref(0)
const notification = ref(null)
let unreadTimer = null
let notifyTimer = null

const { connect, onMessage, offMessage } = useWebSocket()

// Generate a short notification WAV as base64 data URI
function createNotifyWav() {
  const sampleRate = 8000, duration = 0.15, freq1 = 880, freq2 = 660
  const samples = sampleRate * duration * 2 // two tones
  const buffer = new ArrayBuffer(44 + samples * 2)
  const view = new DataView(buffer)
  const writeStr = (o, s) => { for (let i = 0; i < s.length; i++) view.setUint8(o + i, s.charCodeAt(i)) }
  writeStr(0, 'RIFF'); view.setUint32(4, 36 + samples * 2, true); writeStr(8, 'WAVE')
  writeStr(12, 'fmt '); view.setUint32(16, 16, true); view.setUint16(20, 1, true)
  view.setUint16(22, 1, true); view.setUint32(24, sampleRate, true)
  view.setUint32(28, sampleRate * 2, true); view.setUint16(32, 2, true); view.setUint16(34, 16, true)
  writeStr(36, 'data'); view.setUint32(40, samples * 2, true)
  for (let i = 0; i < samples; i++) {
    const t = i / sampleRate
    const freq = t < duration ? freq1 : freq2
    const vol = Math.max(0, 1 - t / (duration * 2)) * 0.4
    view.setInt16(44 + i * 2, Math.sin(2 * Math.PI * freq * t) * vol * 32767, true)
  }
  const blob = new Blob([buffer], { type: 'audio/wav' })
  return URL.createObjectURL(blob)
}
const notifySoundUrl = createNotifyWav()
let audioUnlocked = false
// Unlock audio on first user interaction (iOS requirement)
function unlockAudio() {
  if (audioUnlocked) return
  const a = new Audio(notifySoundUrl)
  a.volume = 0
  a.play().then(() => { a.pause(); audioUnlocked = true }).catch(() => {})
}
if (typeof window !== 'undefined') {
  ['touchstart', 'click'].forEach(e => window.addEventListener(e, unlockAudio, { once: false, passive: true }))
}

function playNotifySound() {
  try {
    const a = new Audio(notifySoundUrl)
    a.volume = 0.5
    a.play().catch(() => {})
  } catch (e) { /* ignore */ }
}

const menuGroups = [
  {
    label: '游戏',
    items: [
      { path: '/player/home', title: '首页', icon: HomeFilled },
      { path: '/player/lottery', title: '抽奖', icon: Present },
    ]
  },
  {
    label: '资产',
    items: [
      { path: '/player/warehouse', title: '仓库', icon: Box },
      { path: '/player/market', title: '市场', icon: Shop },
    ]
  },
  {
    label: '社交',
    items: [
      { path: '/player/friends', title: '好友', icon: Avatar },
      { path: '/player/chat', title: '消息', icon: ChatDotSquare },
    ]
  },
  {
    label: '账户',
    items: [
      { path: '/player/recharge', title: '充值记录', icon: Tickets },
      { path: '/player/orders', title: '发货订单', icon: List },
      { path: '/player/profile', title: '我的', icon: User },
    ]
  }
]

async function fetchUnreadCount() {
  try {
    const res = await playerApi.getUnreadCount()
    unreadCount.value = res.data || 0
  } catch (e) { /* ignore */ }
}

function onNewMessage(data) {
  // Handle friend system notifications
  if (data.type === 'friend_request') {
    playNotifySound()
    clearTimeout(notifyTimer)
    notification.value = {
      senderName: data.nickname,
      senderAvatar: data.avatar,
      senderId: data.userId,
      content: '请求添加你为好友',
      _isFriendRequest: true
    }
    notifyTimer = setTimeout(() => { notification.value = null }, 4000)
    return
  }
  if (data.type === 'friend_accepted') {
    playNotifySound()
    showToast(`${data.nickname} 已同意你的好友申请`)
    return
  }
  if (data.type === 'error') return

  fetchUnreadCount()

  // 如果是自己发的消息，不弹提醒
  const myId = userStore.userInfo?.id
  if (data.senderId == myId) return

  // 如果正在和发送者聊天，不弹提醒
  const currentChatUserId = route.params.userId
  if (route.name === 'Chat' && currentChatUserId == data.senderId) return

  // 弹出提醒
  playNotifySound()
  clearTimeout(notifyTimer)
  notification.value = data
  notifyTimer = setTimeout(() => {
    notification.value = null
  }, 4000)
}

function goToChat(userId) {
  const isFriendRequest = notification.value?._isFriendRequest
  notification.value = null
  if (isFriendRequest) {
    router.push('/player/friends')
  } else {
    router.push(`/player/chat/${userId}`)
  }
}

onMounted(async () => {
  try {
    const res = await playerApi.getInfo()
    userStore.updateUserInfo(res.data)
  } catch (e) {
    showToast(e.message || '获取用户信息失败')
  }
  connect()
  fetchUnreadCount()
  onMessage(onNewMessage)
  unreadTimer = setInterval(fetchUnreadCount, 30000)
})

onUnmounted(() => {
  offMessage(onNewMessage)
  clearInterval(unreadTimer)
  clearTimeout(notifyTimer)
})
</script>

<style scoped>
.player-layout { min-height: 100vh; padding-bottom: 50px; background: #111; }

/* 消息提醒弹框 */
.msg-notify {
  position: fixed;
  top: 16px;
  left: 16px;
  right: 16px;
  z-index: 9999;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: #1e1e1e;
  border: 1px solid #f0c040;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(240, 192, 64, 0.15), 0 2px 8px rgba(0,0,0,0.5);
  cursor: pointer;
}
.msg-notify.desktop-notify {
  left: auto;
  right: 24px;
  top: 20px;
  max-width: 360px;
}
.notify-avatar { flex-shrink: 0; }
.notify-avatar img { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; }
.notify-avatar-text {
  display: flex; align-items: center; justify-content: center;
  width: 40px; height: 40px; border-radius: 50%;
  background: #f0c040; color: #000; font-weight: 600; font-size: 16px;
}
.notify-body { flex: 1; min-width: 0; }
.notify-name { font-size: 14px; font-weight: 600; color: #f0c040; margin-bottom: 2px; }
.notify-content { font-size: 13px; color: #ccc; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notify-close {
  flex-shrink: 0; width: 24px; height: 24px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; color: #666; border-radius: 50%;
}
.notify-close:hover { background: rgba(255,255,255,0.1); color: #fff; }

/* 动画 */
.notify-slide-enter-active { transition: all 0.3s ease-out; }
.notify-slide-leave-active { transition: all 0.25s ease-in; }
.notify-slide-enter-from { transform: translateY(-100%); opacity: 0; }
.notify-slide-leave-to { transform: translateY(-100%); opacity: 0; }
</style>
