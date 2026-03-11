import { ref, onMounted, onUnmounted } from 'vue'

export function useIsMobile(breakpoint = 768) {
  const mql = window.matchMedia(`(max-width: ${breakpoint - 1}px)`)
  const isMobile = ref(mql.matches)

  function update(e) {
    isMobile.value = e.matches
  }

  onMounted(() => mql.addEventListener('change', update))
  onUnmounted(() => mql.removeEventListener('change', update))

  return { isMobile }
}
