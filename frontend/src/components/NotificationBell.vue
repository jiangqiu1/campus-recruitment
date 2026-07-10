<template>
  <div class="notification-bell" @click.stop>
    <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="bell-badge">
      <el-button circle size="small" class="bell-btn" @click="toggleDropdown">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>
      </el-button>
    </el-badge>

    <Transition name="bell-dropdown">
      <div v-if="showDropdown" class="bell-dropdown" @click.stop>
        <div class="bell-dropdown-header">
          <span>消息通知</span>
          <el-button v-if="unreadCount > 0" link type="primary" size="small" @click="markAllRead">
            全部已读
          </el-button>
        </div>
        <div class="bell-dropdown-body">
          <div v-if="loading" class="bell-loading">加载中...</div>
          <div v-else-if="messages.length === 0" class="bell-empty">
            <p>暂无消息</p>
          </div>
          <div v-else class="bell-list">
            <div
              v-for="msg in messages.slice(0, 5)"
              :key="msg.id"
              class="bell-item"
              :class="{ 'bell-item--unread': !msg.isRead }"
              @click="goToMessage(msg)"
            >
              <div class="bell-item-dot" v-if="!msg.isRead" />
              <div class="bell-item-content">
                <div class="bell-item-title">{{ msg.title }}</div>
                <div class="bell-item-time">{{ formatTime(msg.createTime) }}</div>
              </div>
            </div>
          </div>
        </div>
        <div class="bell-dropdown-footer">
          <el-button link type="primary" size="small" @click="viewAll">查看全部消息</el-button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { userMessageAPI } from '@/api'

const router = useRouter()
const showDropdown = ref(false)
const unreadCount = ref(0)
const messages = ref([])
const loading = ref(false)
let pollTimer = null

const hasToken = () => !!localStorage.getItem('token')

const loadData = async () => {
  if (!hasToken()) return
  try {
    const [countRes, msgRes] = await Promise.all([
      userMessageAPI.getUnreadCount(),
      userMessageAPI.getMessages()
    ])
    unreadCount.value = countRes?.data?.count || 0
    messages.value = msgRes?.data || []
  } catch (e) { /* ignore */ }
  loading.value = false
}

const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value
  if (showDropdown.value) {
    loading.value = true
    loadData()
  }
}

const closeDropdown = () => {
  showDropdown.value = false
}

const markAllRead = async () => {
  try {
    await userMessageAPI.markAllRead()
    unreadCount.value = 0
    messages.value.forEach(m => { m.isRead = 1 })
  } catch (e) { /* ignore */ }
}

const goToMessage = async (msg) => {
  if (!msg.isRead) {
    try {
      await userMessageAPI.markAsRead(msg.id)
      msg.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (e) { /* ignore */ }
  }
  showDropdown.value = false
}

const viewAll = () => {
  showDropdown.value = false
  const role = localStorage.getItem('userRole')
  const base = ({ admin: '/admin', teacher: '/teacher', hr: '/hr' })[role] || '/admin'
  router.push(base + '/messages')
}

const formatTime = (t) => {
  if (!t) return ''
  const date = new Date(t)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 172800000) return '昨天'
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

onMounted(() => {
  loadData()
  // 点击外部关闭下拉
  document.addEventListener('click', closeDropdown)
  // 每 5 秒轮询未读数，确保红点实时更新
  pollTimer = setInterval(() => {
    if (!hasToken() || window.__isLoggingOut) return
    userMessageAPI.getUnreadCount().then(res => {
      unreadCount.value = res?.data?.count || 0
    }).catch(() => {})
  }, 5000)
  // 切换回页面时立即刷新
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  document.removeEventListener('click', closeDropdown)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})

const onVisibilityChange = () => {
  if (!document.hidden && hasToken()) {
    userMessageAPI.getUnreadCount().then(res => {
      unreadCount.value = res?.data?.count || 0
    }).catch(() => {})
  }
}
</script>

<style scoped>
.notification-bell {
  position: relative;
  display: inline-flex;
  align-items: center;
  margin-right: 8px;
}
.bell-btn {
  border: none !important;
  background: transparent !important;
  color: #4E5969 !important;
  font-size: 18px;
  padding: 4px !important;
  width: 32px !important;
  height: 32px !important;
  border-radius: 50% !important;
  transition: background 0.2s;
}
.bell-btn:hover {
  background: rgba(0,0,0,0.06) !important;
}

.bell-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 340px;
  max-height: 420px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.15);
  z-index: 10000;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.bell-dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-bottom: 1px solid #F2F3F5;
  font-size: 14px;
  font-weight: 600;
  color: #1D2129;
}
.bell-dropdown-body {
  flex: 1;
  overflow-y: auto;
  min-height: 80px;
}
.bell-loading, .bell-empty {
  text-align: center;
  padding: 28px 0;
  color: #C9CDD4;
  font-size: 13px;
}
.bell-list { display: flex; flex-direction: column; }
.bell-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 18px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid #F7F8FA;
}
.bell-item:hover { background: #F5F7FA; }
.bell-item--unread { background: rgba(22,93,255,0.03); }
.bell-item-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #F53F3F;
  flex-shrink: 0;
  margin-top: 5px;
}
.bell-item-content { flex: 1; min-width: 0; }
.bell-item-title {
  font-size: 13px;
  color: #1D2129;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.bell-item-time {
  font-size: 11px;
  color: #C9CDD4;
  margin-top: 3px;
}
.bell-dropdown-footer {
  padding: 10px 18px;
  border-top: 1px solid #F2F3F5;
  text-align: center;
}

.bell-dropdown-enter-active, .bell-dropdown-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.bell-dropdown-enter-from, .bell-dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
