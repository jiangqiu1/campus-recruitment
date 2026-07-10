<template>
  <div class="message-list-page fade-in" :style="themeVars">
    <div class="page-header">
      <h2>{{ pageTheme.title }}</h2>
      <p>{{ pageTheme.desc }}</p>
    </div>
    <div class="card">
      <div class="card-toolbar">
        <span class="card-toolbar-title">共 {{ messages.length }} 条消息</span>
        <el-button v-if="unreadCount > 0" size="small" type="primary" plain @click="markAllRead">
          全部标为已读
        </el-button>
      </div>

      <div v-if="loading" style="text-align:center;padding:60px 0;color:#C9CDD4;">加载中...</div>

      <div v-else-if="messages.length === 0" style="text-align:center;padding:60px 0;">
        <el-empty :image-size="80" description="暂无消息" />
      </div>

      <div v-else class="message-list">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-item"
          :class="{ 'message-item--unread': !msg.isRead }"
          @click="markRead(msg)"
        >
          <div class="msg-left">
            <div class="msg-dot" v-if="!msg.isRead" />
          </div>
          <div class="msg-content">
            <div class="msg-title">{{ msg.title }}</div>
            <div class="msg-body" v-if="msg.content">{{ msg.content }}</div>
            <div class="msg-type">
              <el-tag size="small" :type="typeTag(msg.type)">{{ typeLabel(msg.type) }}</el-tag>
            </div>
          </div>
          <div class="msg-time">{{ formatTime(msg.createTime) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { userMessageAPI } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()

const pageTheme = computed(() => {
  const themes = {
    admin:   { accent: '#165DFF', title: '系统消息', desc: '审核通知 · 系统公告' },
    teacher: { accent: '#00B42A', title: '我的消息', desc: '简历动态 · 投递反馈' },
    hr:      { accent: '#F59E0B', title: '消息中心', desc: '候选人动态 · 投递提醒' }
  }
  return themes[route.meta?.role] || themes.admin
})

const themeVars = computed(() => ({
  '--msg-accent': pageTheme.value.accent,
  '--msg-accent-bg': pageTheme.value.accent + '08'
}))

const messages = ref([])
const unreadCount = ref(0)
const loading = ref(true)

const loadMessages = async () => {
  loading.value = true
  try {
    const [msgRes, countRes] = await Promise.all([
      userMessageAPI.getMessages(),
      userMessageAPI.getUnreadCount()
    ])
    messages.value = msgRes?.data || []
    unreadCount.value = countRes?.data?.count || 0
  } catch (e) {
    ElMessage.error('加载消息失败')
  } finally {
    loading.value = false
  }
}

const markRead = async (msg) => {
  if (msg.isRead) return
  try {
    await userMessageAPI.markAsRead(msg.id)
    msg.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (e) { /* ignore */ }
}

const markAllRead = async () => {
  try {
    await userMessageAPI.markAllRead()
    messages.value.forEach(m => { m.isRead = 1 })
    unreadCount.value = 0
    ElMessage.success('已全部标为已读')
  } catch (e) { /* ignore */ }
}

const typeTag = (t) => {
  const map = { system: '', audit: 'warning', delivery: 'primary', interview: 'success' }
  return map[t] || 'info'
}

const typeLabel = (t) => {
  const map = { system: '系统通知', audit: '审核消息', delivery: '投递消息', interview: '面试消息' }
  return map[t] || '其他'
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const pad = (n) => String(n).padStart(2, '0')
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
      + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}

onMounted(loadMessages)
</script>

<style scoped>
.message-list-page { padding: 20px; }
.card { background: white; border-radius: 16px; padding: 24px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); }
.card-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #F2F3F5;
}
.card-toolbar-title { font-size: 14px; color: #86909C; }

.message-item {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid #F7F8FA;
}
.message-item:hover { background: #F5F7FA; }
.message-item:last-child { border-bottom: none; }
.message-item--unread { background: var(--msg-accent-bg, rgba(22,93,255,0.03)); }

.msg-left { width: 12px; flex-shrink: 0; padding-top: 4px; }
.msg-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #F53F3F;
}

.msg-content { flex: 1; min-width: 0; }
.msg-title {
  font-size: 14px;
  font-weight: 600;
  color: #1D2129;
  margin-bottom: 4px;
}
.msg-body {
  font-size: 13px;
  color: #86909C;
  line-height: 1.5;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.msg-type { margin-top: 4px; }

.msg-time {
  font-size: 12px;
  color: #C9CDD4;
  white-space: nowrap;
  flex-shrink: 0;
  padding-top: 2px;
}
</style>
