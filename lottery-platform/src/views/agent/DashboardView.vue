<template>
  <div class="page">
    <div class="page-title">代理概览</div>

    <div class="stats-grid">
      <div class="stat-card" v-for="item in cards" :key="item.label">
        <div class="stat-value">{{ item.value }}</div>
        <div class="stat-label">{{ item.label }}</div>
      </div>
    </div>

    <!-- 邀请推广 -->
    <div class="section-title">邀请推广</div>
    <div class="invite-card">
      <div class="invite-row">
        <span class="invite-label">邀请码</span>
        <span class="invite-code">{{ data.inviteCode || '-' }}</span>
        <van-button size="small" type="primary" round @click="copyText(data.inviteCode)">复制</van-button>
      </div>
      <div class="invite-row">
        <span class="invite-label">邀请链接</span>
        <van-button size="small" type="primary" round block @click="copyText(inviteLink)">复制邀请链接</van-button>
      </div>
      <div class="invite-hint">玩家通过邀请链接注册或填写邀请码，即可绑定为您的下级</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { showSuccessToast, showFailToast } from 'vant'
import { agentApi } from '@/api'
import { useUserStore } from '@/stores/userStore'

const userStore = useUserStore()
const data = ref({})
const levelText = { 0: '无等级', 1: 'V1', 2: 'V2', 3: 'V3' }
const cards = computed(() => [
  { label: '团队人数', value: data.value.teamCount ?? 0 },
  { label: '团队充值', value: data.value.teamRecharge ?? 0 },
  { label: '累计佣金', value: '¥' + (data.value.totalCommission ?? 0) },
  { label: '代理等级', value: levelText[data.value.level] || '无' },
])

const inviteLink = computed(() => {
  return `${location.origin}/register?invite=${data.value.inviteCode || ''}`
})

function copyText(text) {
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text).then(() => {
      showSuccessToast('已复制')
    }).catch(() => {
      showFailToast('复制失败')
    })
  } else {
    // 非 HTTPS 环境降级方案
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    try {
      document.execCommand('copy')
      showSuccessToast('已复制')
    } catch {
      showFailToast('复制失败')
    }
    document.body.removeChild(textarea)
  }
}

onMounted(async () => {
  try {
    const res = await agentApi.getDashboard()
    data.value = res.data
  } catch (e) { showFailToast(e.message || '操作失败') }
})
</script>

<style scoped>
.page { padding: 16px; color: #fff; }
.page-title { font-size: 18px; font-weight: 600; margin-bottom: 16px; }

.stats-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; margin-bottom: 24px; }
.stat-card {
  background: #1e1e1e; border-radius: 12px; padding: 16px; text-align: center;
}
.stat-value { font-size: 24px; font-weight: 700; color: #f0c040; }
.stat-label { font-size: 12px; color: #888; margin-top: 6px; }

.section-title { font-size: 16px; font-weight: 600; margin-bottom: 12px; }

.invite-card {
  background: #1e1e1e; border-radius: 12px; padding: 16px;
  display: flex; flex-direction: column; gap: 14px;
}
.invite-row { display: flex; align-items: center; gap: 10px; }
.invite-label { font-size: 13px; color: #888; width: 55px; flex-shrink: 0; }
.invite-code { font-size: 18px; font-weight: 700; color: #f0c040; flex: 1; }
.invite-hint { font-size: 12px; color: #555; padding-top: 8px; border-top: 1px solid #2a2a2a; }
</style>
