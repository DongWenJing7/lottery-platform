<template>
  <div class="page">
    <!-- 顶部状态 -->
    <div class="top-bar">
      <div class="balance-chip">
        <span class="coin-icon">&#x1FA99;</span>
        <span>{{ userStore.userInfo?.balance ?? 0 }}</span>
      </div>
      <div class="cost-chip">-{{ config.costTokens || 0 }} / 次</div>
    </div>

    <!-- 保底进度 -->
    <div class="progress-section">
      <div class="progress-header">
        <span>保底进度</span>
        <span class="progress-text">{{ consecutiveCount }} / {{ config.jackpotThreshold || 50 }}</span>
      </div>
      <div class="progress-track">
        <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
        <div class="progress-glow" :style="{ left: progressPercent + '%' }"></div>
      </div>
      <div class="progress-hint">再抽 {{ remainCount }} 次必得大奖</div>
    </div>

    <!-- 抽奖主区域 -->
    <div class="draw-stage">
      <div class="orbit orbit-1"><div class="orbit-dot"></div></div>
      <div class="orbit orbit-2"><div class="orbit-dot"></div></div>
      <div class="orbit orbit-3"><div class="orbit-dot"></div></div>
      <div class="draw-btn" :class="{ spinning: drawing }" @click="doDraw">
        <div class="draw-ring"></div>
        <div class="draw-ring ring-2"></div>
        <div class="draw-core">
          <div class="draw-text">{{ drawing ? '...' : 'GO' }}</div>
        </div>
      </div>
    </div>

    <!-- 抽奖结果弹窗 -->
    <van-overlay :show="!!result" @click="result = null" class="result-overlay">
      <div class="result-modal" @click.stop :class="{ jackpot: result?.prize?.isJackpot }">
        <div class="result-glow"></div>
        <div v-if="result?.prize?.image" class="result-img">
          <img :src="result.prize.image" alt="" />
        </div>
        <div class="result-badge" v-if="result?.prize?.isJackpot">JACKPOT</div>
        <div class="result-title">{{ result?.prize?.isJackpot ? '恭喜中大奖！' : '获得奖品' }}</div>
        <div class="result-name">{{ result?.prize?.name }}</div>
        <van-button round block color="#f0c040" class="result-btn" @click="result = null">
          <span style="color:#111;font-weight:600">继续抽奖</span>
        </van-button>
      </div>
    </van-overlay>

    <!-- 奖品列表 -->
    <div class="section-title">奖品一览</div>
    <div class="prize-grid">
      <div v-for="p in config.prizes" :key="p.id" class="prize-card" :class="{ 'is-jackpot': p.isJackpot }">
        <div v-if="p.image" class="prize-img">
          <img :src="p.image" alt="" />
        </div>
        <div v-else class="prize-img placeholder">?</div>
        <div class="prize-name">{{ p.name }}</div>
        <div class="prize-meta">
          <van-tag v-if="p.isJackpot" color="rgba(240,192,64,0.15)" text-color="#f0c040" size="small">大奖</van-tag>
          <span class="prize-recycle">{{ p.recycleTokens }} 币</span>
        </div>
      </div>
      <van-empty v-if="!config.prizes?.length" description="暂无奖品" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/userStore'
import { playerApi } from '@/api'

const userStore = useUserStore()
const config = ref({})
const result = ref(null)
const drawing = ref(false)
const consecutiveCount = ref(0)

const progressPercent = computed(() => {
  const threshold = config.value.jackpotThreshold || 50
  return Math.min((consecutiveCount.value / threshold) * 100, 100)
})

const remainCount = computed(() => {
  const threshold = config.value.jackpotThreshold || 50
  return Math.max(threshold - consecutiveCount.value, 0)
})

onMounted(async () => {
  try {
    const res = await playerApi.getLotteryConfig()
    config.value = res.data
    consecutiveCount.value = res.data.consecutiveCount || 0
  } catch (e) {
    showToast(e.message || '获取抽奖配置失败')
  }
})

async function doDraw() {
  if (drawing.value) return
  drawing.value = true
  try {
    const res = await playerApi.drawLottery()
    // 延迟一点显示结果，让动画跑一下
    await new Promise(r => setTimeout(r, 800))
    result.value = res.data
    consecutiveCount.value = res.data.consecutiveCount || 0
    const info = await playerApi.getInfo()
    userStore.updateBalance(info.data.balance)
  } catch (e) {
    showToast(e.message || '抽奖失败')
  } finally {
    drawing.value = false
  }
}
</script>

<style scoped>
.page { padding: 16px 16px 30px; color: #fff; }

/* 顶部 */
.top-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.balance-chip {
  display: flex; align-items: center; gap: 6px;
  background: rgba(240,192,64,0.1); border: 1px solid rgba(240,192,64,0.25);
  border-radius: 20px; padding: 6px 14px; font-size: 15px; font-weight: 600; color: #f0c040;
}
.coin-icon { font-size: 16px; }
.cost-chip {
  background: rgba(255,255,255,0.06); border-radius: 20px;
  padding: 6px 14px; font-size: 13px; color: #888;
}

/* 保底进度 */
.progress-section {
  background: #1a1a1a; border-radius: 12px; padding: 14px 16px; margin-bottom: 24px;
}
.progress-header { display: flex; justify-content: space-between; font-size: 13px; color: #aaa; margin-bottom: 10px; }
.progress-text { color: #f0c040; font-weight: 600; }
.progress-track {
  position: relative; height: 6px; background: #2a2a2a; border-radius: 3px; overflow: visible;
}
.progress-fill {
  height: 100%; border-radius: 3px; transition: width 0.5s ease;
  background: linear-gradient(90deg, #f0c040, #ff6b35);
}
.progress-glow {
  position: absolute; top: -4px; width: 14px; height: 14px; border-radius: 50%;
  background: #f0c040; box-shadow: 0 0 12px #f0c040, 0 0 24px rgba(240,192,64,0.4);
  transform: translateX(-50%); transition: left 0.5s ease;
}
.progress-hint { margin-top: 8px; font-size: 12px; color: #666; text-align: center; }

/* 抽奖舞台 */
.draw-stage {
  position: relative; display: flex; justify-content: center; align-items: center;
  height: 240px; margin-bottom: 30px;
}

/* 轨道动画 */
.orbit {
  position: absolute; border: 1px solid rgba(240,192,64,0.08); border-radius: 50%;
}
.orbit-1 { width: 280px; height: 280px; animation: spin 12s linear infinite; }
.orbit-2 { width: 220px; height: 220px; animation: spin 8s linear infinite reverse; }
.orbit-3 { width: 340px; height: 340px; animation: spin 20s linear infinite; }
.orbit-dot {
  position: absolute; top: -3px; left: 50%; width: 6px; height: 6px;
  background: #f0c040; border-radius: 50%; transform: translateX(-50%);
  box-shadow: 0 0 8px rgba(240,192,64,0.6);
}
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

/* 抽奖按钮 */
.draw-btn {
  position: relative; width: 140px; height: 140px; cursor: pointer; z-index: 1;
}
.draw-ring {
  position: absolute; inset: 0; border-radius: 50%;
  border: 3px solid transparent;
  border-top-color: #f0c040; border-bottom-color: #f0c040;
  animation: ring-spin 3s linear infinite;
}
.ring-2 {
  inset: 10px;
  border-top-color: transparent; border-bottom-color: transparent;
  border-left-color: rgba(240,192,64,0.4); border-right-color: rgba(240,192,64,0.4);
  animation: ring-spin 2s linear infinite reverse;
}
@keyframes ring-spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.draw-core {
  position: absolute; inset: 20px; border-radius: 50%;
  background: linear-gradient(135deg, #f0c040, #d4a017);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 0 40px rgba(240,192,64,0.35), inset 0 2px 8px rgba(255,255,255,0.25);
  transition: transform 0.15s;
}
.draw-btn:active .draw-core { transform: scale(0.92); }
.draw-btn.spinning .draw-core {
  animation: core-pulse 0.4s ease-in-out infinite alternate;
}
.draw-btn.spinning .draw-ring { animation-duration: 0.4s; }
.draw-btn.spinning .ring-2 { animation-duration: 0.3s; }
@keyframes core-pulse { from { box-shadow: 0 0 40px rgba(240,192,64,0.35); } to { box-shadow: 0 0 60px rgba(240,192,64,0.7), 0 0 100px rgba(240,192,64,0.3); } }

.draw-text {
  color: #111; font-size: 32px; font-weight: 900; letter-spacing: 2px;
}

/* 结果弹窗 */
.result-overlay {
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0.75); backdrop-filter: blur(8px);
}
.result-modal {
  position: relative; width: 300px;
  background: #1e1e1e; border-radius: 20px; padding: 40px 24px 28px;
  text-align: center; overflow: hidden;
}
.result-modal.jackpot {
  border: 2px solid #f0c040;
}
.result-glow {
  position: absolute; top: -60px; left: 50%; transform: translateX(-50%);
  width: 200px; height: 120px; border-radius: 50%;
  background: radial-gradient(circle, rgba(240,192,64,0.25), transparent 70%);
}
.result-modal.jackpot .result-glow {
  background: radial-gradient(circle, rgba(240,192,64,0.5), transparent 70%);
  animation: glow-pulse 1.5s ease-in-out infinite alternate;
}
@keyframes glow-pulse { from { opacity: 0.6; } to { opacity: 1; } }

.result-img { margin-bottom: 16px; }
.result-img img { width: 100px; height: 100px; object-fit: contain; border-radius: 12px; }
.result-badge {
  display: inline-block; background: linear-gradient(90deg, #f0c040, #ff6b35);
  color: #111; font-size: 12px; font-weight: 800; letter-spacing: 2px;
  padding: 4px 16px; border-radius: 12px; margin-bottom: 12px;
}
.result-title { font-size: 14px; color: #888; margin-bottom: 6px; }
.result-name { font-size: 26px; font-weight: 700; color: #f0c040; margin-bottom: 24px; }
.result-btn { margin-top: 4px; }

/* 奖品列表 */
.section-title { font-size: 16px; font-weight: 600; margin-bottom: 14px; }
.prize-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px;
}
.prize-card {
  background: #1a1a1a; border-radius: 10px; padding: 12px 8px; text-align: center;
  border: 1px solid transparent; transition: border-color 0.2s;
}
.prize-card.is-jackpot {
  border-color: rgba(240,192,64,0.25);
  background: linear-gradient(180deg, rgba(240,192,64,0.06), #1a1a1a);
}
.prize-img { width: 52px; height: 52px; margin: 0 auto 8px; border-radius: 8px; overflow: hidden; }
.prize-img img { width: 100%; height: 100%; object-fit: cover; }
.prize-img.placeholder {
  background: #2a2a2a; display: flex; align-items: center; justify-content: center;
  font-size: 20px; color: #444;
}
.prize-name { font-size: 12px; font-weight: 500; margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.prize-meta { display: flex; align-items: center; justify-content: center; gap: 4px; }
.prize-recycle { font-size: 11px; color: #666; }
</style>
