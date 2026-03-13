import { ref, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/userStore'

let ws = null
let reconnectTimer = null
const listeners = new Set()
const connected = ref(false)

export function useWebSocket() {
  const userStore = useUserStore()

  function connect() {
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) return

    const token = userStore.token
    if (!token) return

    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = location.host
    ws = new WebSocket(`${protocol}//${host}/ws/chat?token=${token}`)

    ws.onopen = () => {
      connected.value = true
      clearTimeout(reconnectTimer)
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        listeners.forEach(fn => fn(data))
      } catch (e) {
        // ignore parse errors
      }
    }

    ws.onclose = () => {
      connected.value = false
      ws = null
      // Auto reconnect after 3s
      reconnectTimer = setTimeout(() => {
        if (userStore.token) connect()
      }, 3000)
    }

    ws.onerror = () => {
      ws?.close()
    }
  }

  function disconnect() {
    clearTimeout(reconnectTimer)
    if (ws) {
      ws.onclose = null
      ws.close()
      ws = null
    }
    connected.value = false
  }

  function send(data) {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(data))
    }
  }

  function onMessage(fn) {
    listeners.add(fn)
  }

  function offMessage(fn) {
    listeners.delete(fn)
  }

  onUnmounted(() => {
    // Don't disconnect on unmount - keep connection alive across views
  })

  return { connected, connect, disconnect, send, onMessage, offMessage }
}
