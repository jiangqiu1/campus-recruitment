<template>
	<view class="page-wrapper">
		<view class="header-simple" style="padding:12px 16px;flex-direction:row;align-items:center;gap:12px;">
			<text style="font-size:20px;" @click="goBack">‹</text>
			<text style="font-size:18px;font-weight:700;color:white;flex:1;">消息通知</text>
		</view>
		<scroll-view class="content-scrollable" scroll-y>
			<view class="msg-list">
				<view v-for="(msg, i) in messages" :key="i" class="msg-item" @click="handleRead(msg)">
					<view class="msg-icon" :class="msg.iconClass">
						<text>{{ msg.icon }}</text>
					</view>
					<view class="msg-content">
						<view class="msg-title-row">
							<view style="flex-direction:row;align-items:center;gap:6px;">
								<text class="msg-title">{{ msg.title }}</text>
								<text v-if="!msg.isRead" class="msg-dot"></text>
							</view>
							<text class="msg-time">{{ formatTime(msg.createTime) }}</text>
						</view>
						<text class="msg-text">{{ msg.content }}</text>
					</view>
				</view>
				<view v-if="!messages.length" class="empty-state">
					<text style="font-size:48px;margin-bottom:12px;">💬</text>
					<text class="empty-text">暂无消息</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { messageAPI } from '@/utils/request'

const messages = ref([])

const loadMessages = async () => {
	try {
		const res = await messageAPI.getMessages({ page: 1, size: 50 })
		messages.value = res.data?.records || res.data || []
	} catch (e) {
		console.error('加载消息失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

const handleRead = (msg) => {
	msg.isRead = true
}

const formatTime = (t) => {
	if (!t) return ''
	return t.length >= 16 ? t.substring(5, 16) : t.substring(0, 10)
}

const goBack = () => uni.navigateBack()

onMounted(loadMessages)
</script>

<style scoped>
.header-simple {
	background: linear-gradient(135deg, #0EA5E9 0%, #38BDF8 100%);
	color: white;
	flex-shrink: 0;
}
.msg-list { padding: 12px 16px; }
.msg-item {
	flex-direction: row;
	padding: 16px;
	background: #fff;
	border-radius: 14px;
	margin-bottom: 10px;
	gap: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.msg-icon {
	width: 40px;
	height: 40px;
	border-radius: 50%;
	align-items: center;
	justify-content: center;
	font-size: 20px;
	flex-shrink: 0;
}
.msg-content { flex: 1; }
.msg-title-row {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 4px;
}
.msg-title { font-size: 14px; font-weight: 600; color: #1D2129; }
.msg-dot {
	width: 8px;
	height: 8px;
	border-radius: 50%;
	background: #EF4444;
}
.msg-time { font-size: 12px; color: #C9CDD4; }
.msg-text { font-size: 13px; color: #86909C; line-height: 1.5; }
.empty-state {
	padding: 60px 20px;
	align-items: center;
	justify-content: center;
}
.empty-text { font-size: 14px; color: #86909C; }
</style>
