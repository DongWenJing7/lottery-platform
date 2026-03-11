import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const role = ref(localStorage.getItem('role') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const isLoggedIn = computed(() => !!token.value)

  function setUser(data) {
    token.value = data.token
    role.value = data.role
    userInfo.value = data.userInfo
    localStorage.setItem('token', data.token)
    localStorage.setItem('role', data.role)
    localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
  }

  function logout() {
    token.value = ''; role.value = ''; userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('userInfo')
  }

  function updateBalance(balance) {
    if (userInfo.value) {
      userInfo.value.balance = balance
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    }
  }

  function updateUserInfo(fields) {
    if (userInfo.value) {
      Object.assign(userInfo.value, fields)
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    }
  }

  return { token, role, userInfo, isLoggedIn, setUser, logout, updateBalance, updateUserInfo }
})
