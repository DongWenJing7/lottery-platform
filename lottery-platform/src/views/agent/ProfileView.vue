<template>
  <div class="page">
    <div class="profile-header">
      <div class="avatar-wrap" @click="triggerUpload">
        <img v-if="info.avatar" :src="info.avatar" class="avatar-img" />
        <div v-else class="avatar-text">{{ (info.nickname || 'A').charAt(0) }}</div>
        <div class="avatar-edit">换</div>
        <input ref="fileInput" type="file" accept="image/*" hidden @change="onFileChange" />
      </div>
      <div class="profile-info">
        <div class="nickname">{{ info.nickname }}</div>
        <div class="phone">{{ maskedPhone }}</div>
      </div>
      <div class="edit-btn" @click="showEdit = true">编辑资料</div>
    </div>

    <div class="menu-list">
      <div class="menu-item" @click="$router.push('/agent/dashboard')">
        <span>数据概览</span><span class="arrow">›</span>
      </div>
      <div class="menu-item" @click="$router.push('/agent/players')">
        <span>我的团队</span><span class="arrow">›</span>
      </div>
      <div class="menu-item" @click="$router.push('/agent/commission')">
        <span>佣金统计</span><span class="arrow">›</span>
      </div>
    </div>

    <van-button type="danger" block round plain class="logout-btn" @click="handleLogout">退出登录</van-button>

    <!-- 头像裁剪弹窗 -->
    <div v-if="showCropper" class="cropper-overlay">
      <div class="cropper-header">
        <span class="cropper-cancel" @click="closeCropper">取消</span>
        <span class="cropper-title">裁剪头像</span>
        <span class="cropper-confirm" @click="confirmCrop">确定</span>
      </div>
      <div class="cropper-body">
        <img ref="cropImg" :src="cropSrc" />
      </div>
    </div>

    <!-- 编辑资料弹窗 -->
    <van-popup v-model:show="showEdit" position="bottom" round :style="{ padding: '24px', background: '#1e1e1e' }">
      <div class="edit-title">编辑资料</div>
      <div class="edit-form">
        <div class="edit-group">
          <label class="edit-label">昵称</label>
          <input v-model="editForm.nickname" class="edit-input" placeholder="请输入昵称" />
        </div>
      </div>
      <van-button type="primary" block round @click="saveProfile" :loading="saving" style="margin-top:20px">保存</van-button>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { agentApi } from '@/api'
import request from '@/api/request'
import { showToast } from 'vant'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'

const router = useRouter()
const userStore = useUserStore()
const info = ref({})
const fileInput = ref(null)
const showEdit = ref(false)
const saving = ref(false)
const editForm = ref({ nickname: '' })

// 裁剪相关
const showCropper = ref(false)
const cropSrc = ref('')
const cropImg = ref(null)
let cropperInstance = null
let cropperReady = false

const maskedPhone = computed(() => {
  const phone = info.value.username || ''
  if (phone.length === 11) {
    return phone.slice(0, 3) + '****' + phone.slice(7)
  }
  return phone
})

onMounted(async () => {
  try {
    const res = await agentApi.getInfo()
    info.value = res.data
    userStore.updateUserInfo(res.data)
  } catch (e) {
    showToast(e.message || '获取信息失败')
  }
})

function triggerUpload() {
  fileInput.value?.click()
}

function onFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  e.target.value = ''
  const reader = new FileReader()
  reader.onload = (ev) => {
    cropSrc.value = ev.target.result
    showCropper.value = true
  }
  reader.readAsDataURL(file)
}

watch(showCropper, (val) => {
  if (val) {
    nextTick(() => {
      setTimeout(() => {
        const img = cropImg.value
        if (!img) return
        if (cropperInstance) { cropperInstance.destroy() }
        cropperReady = false
        cropperInstance = new Cropper(img, {
          aspectRatio: 1,
          viewMode: 1,
          dragMode: 'move',
          background: false,
          autoCropArea: 0.8,
          ready() { cropperReady = true }
        })
      }, 300)
    })
  }
})

function closeCropper() {
  if (cropperInstance) { cropperInstance.destroy(); cropperInstance = null }
  cropperReady = false
  showCropper.value = false
}

async function confirmCrop() {
  if (!cropperInstance || !cropperReady) {
    showToast('裁剪器加载中，请稍候')
    return
  }
  const canvas = cropperInstance.getCroppedCanvas({ width: 400, height: 400 })
  canvas.toBlob(async (blob) => {
    const formData = new FormData()
    formData.append('file', blob, 'avatar.jpg')
    try {
      const res = await request.post('/upload/image', formData)
      const url = res.data.url
      await agentApi.updateProfile({ avatar: url })
      info.value.avatar = url
      userStore.updateUserInfo({ avatar: url })
      showToast('头像已更新')
    } catch (err) {
      showToast(err.message || '上传失败')
    }
    closeCropper()
  }, 'image/jpeg', 0.9)
}

watch(showEdit, (v) => {
  if (v) {
    editForm.value.nickname = info.value.nickname || ''
  }
})

async function saveProfile() {
  saving.value = true
  try {
    await agentApi.updateProfile({ nickname: editForm.value.nickname })
    info.value.nickname = editForm.value.nickname
    userStore.updateUserInfo({ nickname: editForm.value.nickname })
    showEdit.value = false
    showToast('保存成功')
  } catch (err) {
    showToast(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.page { padding: 16px; color: #fff; }

.profile-header { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; }
.avatar-wrap { position: relative; width: 60px; height: 60px; border-radius: 50%; overflow: hidden; cursor: pointer; flex-shrink: 0; }
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-text { width: 100%; height: 100%; background: linear-gradient(135deg, #f0c040, #d4a017); display: flex; align-items: center; justify-content: center; font-size: 24px; font-weight: 700; color: #111; }
.avatar-edit { position: absolute; bottom: 0; left: 0; right: 0; background: rgba(0,0,0,0.55); color: #fff; font-size: 11px; text-align: center; padding: 2px 0; }
.profile-info { flex: 1; min-width: 0; }
.nickname { font-size: 18px; font-weight: 600; }
.phone { font-size: 13px; color: #888; margin-top: 2px; }
.edit-btn { padding: 6px 14px; background: #2a2a2a; border-radius: 16px; font-size: 13px; color: #f0c040; cursor: pointer; white-space: nowrap; }

.menu-list {
  background: #1e1e1e; border-radius: 12px; overflow: hidden; margin-bottom: 30px;
}
.menu-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; border-bottom: 1px solid #2a2a2a; cursor: pointer; font-size: 15px;
}
.menu-item:last-child { border-bottom: none; }
.menu-item:active { background: rgba(255,255,255,0.03); }
.arrow { color: #444; font-size: 20px; }

.logout-btn { margin-top: 10px; }

.edit-title { font-size: 18px; font-weight: 600; color: #fff; margin-bottom: 20px; text-align: center; }
.edit-form { display: flex; flex-direction: column; gap: 16px; }
.edit-group { display: flex; flex-direction: column; gap: 6px; }
.edit-label { font-size: 13px; color: #888; }
.edit-input { background: #2a2a2a; border: 1px solid #333; border-radius: 8px; padding: 12px; color: #fff; font-size: 15px; outline: none; }
.edit-input:focus { border-color: #f0c040; }

/* 裁剪弹窗 */
.cropper-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; z-index: 9999; background: #000; display: flex; flex-direction: column; }
.cropper-header { display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; flex-shrink: 0; }
.cropper-cancel { color: #aaa; font-size: 15px; cursor: pointer; }
.cropper-title { color: #fff; font-size: 16px; font-weight: 600; }
.cropper-confirm { color: #f0c040; font-size: 15px; font-weight: 600; cursor: pointer; }
.cropper-body { flex: 1; overflow: hidden; }
.cropper-body img { display: block; max-width: 100%; }
</style>
