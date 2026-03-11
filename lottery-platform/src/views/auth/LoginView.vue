<template>
  <div class="login-page">
    <div class="login-container">
      <div class="logo-area">
        <div class="logo-icon">🎰</div>
        <h1 class="logo-title">抽奖平台</h1>
        <p class="logo-sub">好运，从这里开始</p>
      </div>
      <div class="form-area">
        <div class="input-group">
          <span class="input-icon">📱</span>
          <input v-model="form.username" type="text" placeholder="请输入手机号/用户名" class="input-field" @keyup.enter="handleLogin" />
        </div>
        <div class="input-group">
          <span class="input-icon">🔒</span>
          <input v-model="form.password" type="password" placeholder="请输入密码" class="input-field" @keyup.enter="handleLogin" />
        </div>
        <button class="login-btn" @click="handleLogin" :disabled="loading">
          <span v-if="!loading">登 录</span>
          <span v-else>登录中...</span>
        </button>
        <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>
      </div>
      <div class="footer">没有账号？<router-link to="/register" class="link">立即注册</router-link></div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import request from '@/api/request'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const errorMsg = ref('')
const form = ref({ username: '', password: '' })

async function handleLogin() {
  if (!form.value.username || !form.value.password) { errorMsg.value = '请输入手机号/用户名和密码'; return }
  loading.value = true; errorMsg.value = ''
  try {
    const res = await request.post('/auth/login', form.value)
    userStore.setUser(res.data)
    const map = { player: '/player/home', agent: '/agent/dashboard', admin: '/admin/dashboard' }
    router.push(map[res.data.role] || '/login')
  } catch (e) {
    errorMsg.value = e.message || '手机号或密码错误'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
* { box-sizing: border-box; }
.login-page { min-height: 100vh; background: #0f0f0f; display: flex; align-items: center; justify-content: center; padding: 24px; }
.login-container { width: 100%; max-width: 360px; }
.logo-area { text-align: center; margin-bottom: 48px; }
.logo-icon { font-size: 56px; margin-bottom: 12px; }
.logo-title { font-size: 26px; font-weight: 700; color: #f0c040; margin: 0 0 6px; letter-spacing: 2px; }
.logo-sub { font-size: 13px; color: #666; margin: 0; }
.form-area { display: flex; flex-direction: column; gap: 16px; }
.input-group { display: flex; align-items: center; background: #1e1e1e; border: 1px solid #2a2a2a; border-radius: 10px; padding: 0 16px; transition: border-color 0.2s; }
.input-group:focus-within { border-color: #f0c040; }
.input-icon { font-size: 16px; margin-right: 10px; opacity: 0.6; }
.input-field { flex: 1; background: transparent; border: none; outline: none; color: #fff; font-size: 15px; padding: 14px 0; }
.input-field::placeholder { color: #444; }
.login-btn { margin-top: 8px; width: 100%; padding: 15px; background: linear-gradient(135deg, #f0c040, #d4a017); border: none; border-radius: 10px; color: #0f0f0f; font-size: 16px; font-weight: 700; letter-spacing: 4px; cursor: pointer; transition: opacity 0.2s; }
.login-btn:hover { opacity: 0.9; }
.login-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.error-msg { text-align: center; color: #e55; font-size: 13px; }
.footer { text-align: center; margin-top: 32px; color: #444; font-size: 13px; }
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
