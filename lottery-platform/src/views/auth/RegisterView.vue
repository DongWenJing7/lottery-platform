<template>
  <div class="login-page">
    <div class="login-container">
      <div class="logo-area">
        <div class="logo-icon">🎰</div>
        <h1 class="logo-title">注册账号</h1>
      </div>
      <div class="form-area">
        <div class="input-group">
          <span class="input-icon">📱</span>
          <input v-model="form.username" type="tel" placeholder="请输入手机号" class="input-field" />
        </div>
        <div class="input-group">
          <span class="input-icon">😊</span>
          <input v-model="form.nickname" type="text" placeholder="请输入昵称（选填）" class="input-field" />
        </div>
        <div class="input-group">
          <span class="input-icon">🔒</span>
          <input v-model="form.password" type="password" placeholder="请输入密码" class="input-field" />
        </div>
        <div class="input-group">
          <span class="input-icon">🔒</span>
          <input v-model="form.password2" type="password" placeholder="请确认密码" class="input-field" @keyup.enter="handleRegister" />
        </div>
        <div class="input-group">
          <span class="input-icon">🎫</span>
          <input v-model="form.inviteCode" type="text" placeholder="邀请码（选填）" class="input-field" />
        </div>
        <button class="login-btn" @click="handleRegister" :disabled="loading">
          <span v-if="!loading">注 册</span>
          <span v-else>注册中...</span>
        </button>
        <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>
      </div>
      <div class="footer">已有账号？<router-link to="/login" class="link">立即登录</router-link></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { authApi } from '@/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const errorMsg = ref('')
const form = ref({ username: '', password: '', password2: '', nickname: '', inviteCode: '' })

// 从URL参数自动填入邀请码
onMounted(() => {
  if (route.query.invite) {
    form.value.inviteCode = route.query.invite
  }
})

async function handleRegister() {
  const { username, password, password2, nickname, inviteCode } = form.value
  if (!username || !password) { errorMsg.value = '请输入手机号和密码'; return }
  if (!/^1\d{10}$/.test(username)) { errorMsg.value = '请输入正确的11位手机号'; return }
  if (password !== password2) { errorMsg.value = '两次密码不一致'; return }
  loading.value = true; errorMsg.value = ''
  try {
    const res = await authApi.register({ username, password, nickname, inviteCode })
    userStore.setUser(res.data)
    router.push('/player/home')
  } catch (e) {
    errorMsg.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
* { box-sizing: border-box; }
.login-page { min-height: 100vh; background: #0f0f0f; display: flex; align-items: center; justify-content: center; padding: 24px; }
.login-container { width: 100%; max-width: 360px; }
.logo-area { text-align: center; margin-bottom: 36px; }
.logo-icon { font-size: 48px; margin-bottom: 12px; }
.logo-title { font-size: 24px; font-weight: 700; color: #f0c040; margin: 0; letter-spacing: 2px; }
.form-area { display: flex; flex-direction: column; gap: 14px; }
.input-group { display: flex; align-items: center; background: #1e1e1e; border: 1px solid #2a2a2a; border-radius: 10px; padding: 0 16px; transition: border-color 0.2s; }
.input-group:focus-within { border-color: #f0c040; }
.input-icon { font-size: 16px; margin-right: 10px; opacity: 0.6; }
.input-field { flex: 1; background: transparent; border: none; outline: none; color: #fff; font-size: 15px; padding: 14px 0; }
.input-field::placeholder { color: #444; }
.login-btn { margin-top: 8px; width: 100%; padding: 15px; background: linear-gradient(135deg, #f0c040, #d4a017); border: none; border-radius: 10px; color: #0f0f0f; font-size: 16px; font-weight: 700; letter-spacing: 4px; cursor: pointer; transition: opacity 0.2s; }
.login-btn:hover { opacity: 0.9; }
.login-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.error-msg { text-align: center; color: #e55; font-size: 13px; }
.footer { text-align: center; margin-top: 24px; color: #444; font-size: 13px; }
.link { color: #f0c040; text-decoration: none; }

@media (min-width: 768px) {
  .login-container {
    max-width: 420px;
    background: #1a1a1a;
    border: 1px solid #2a2a2a;
    border-radius: 16px;
    padding: 48px 40px;
    box-shadow: 0 8px 32px rgba(0,0,0,0.4);
  }
}
</style>
