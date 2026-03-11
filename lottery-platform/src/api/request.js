import axios from 'axios'
import { useUserStore } from '@/stores/userStore'
import router from '@/router'

const request = axios.create({ baseURL: '/api', timeout: 10000 })

request.interceptors.request.use(config => {
  const userStore = useUserStore()
  if (userStore.token) config.headers.Authorization = `Bearer ${userStore.token}`
  return config
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200 && res.code !== 0) return Promise.reject(new Error(res.message || '请求失败'))
    return res
  },
  error => {
    if (error.response?.status === 401) {
      useUserStore().logout()
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default request
