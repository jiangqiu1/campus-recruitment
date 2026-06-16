<template>
  <div class="message-notification">
    <div class="card">
      <h2>消息通知</h2>
      <div style="padding: 20px 0;">
        <div 
          v-for="msg in messageList" 
          :key="msg.id" 
          style="padding: 16px; border-bottom: 1px solid #F2F3F5; cursor: pointer;"
          @click="markAsRead(msg)"
        >
          <div style="font-weight: 500;">{{ msg.title }}</div>
          <div style="font-size: 12px; color: #86909C; margin-top: 4px;">
            {{ formatTime(msg.createTime) }}
          </div>
          <div v-if="!msg.read" style="width: 8px; height: 8px; background: #165DFF; border-radius: 50%; float: right; margin-top: -20px;"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const messageList = ref([
  { id: 1, title: '岗位审核通过：Java开发工程师已上线', createTime: '2025-04-18 10:23', read: false },
  { id: 2, title: '新简历投递：李明投递了Java开发工程师', createTime: '2025-04-18 09:15', read: false },
  { id: 3, title: '系统通知：2025届毕业生就业统计已更新', createTime: '2025-04-17 16:30', read: true }
])

const markAsRead = (msg) => {
  if (!msg.read) {
    msg.read = true
  }
}

const formatTime = (time) => {
  return time
}
</script>

<style scoped>
.message-notification {
  padding: 20px;
}

.card {
  background: white;
  border-radius: 16px;
  padding: 32px;
  margin-bottom: 28px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.06);
  position: relative;
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, #165DFF, #2563EB, transparent);
  border-radius: 16px 16px 0 0;
}

.card h2 {
  font-size: 19px;
  font-weight: 600;
  padding-bottom: 12px;
  border-bottom: 1px solid #F2F3F5;
  position: relative;
}

.card h2::after {
  content: '';
  width: 50px;
  height: 3px;
  background: #165DFF;
  border-radius: 3px;
  position: absolute;
  left: 0;
  bottom: -1px;
}
</style>
