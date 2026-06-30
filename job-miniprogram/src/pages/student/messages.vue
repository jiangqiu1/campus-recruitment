<template>
	<view class="page-wrapper">
		<view class="header-simple" style="padding:12px 16px;">
			<text style="font-size:18px;font-weight:700;color:white;">消息通知</text>
		</view>
		<scroll-view class="content-scrollable" scroll-y>
			<view class="msg-list">
				<view v-for="(msg, i) in messages" :key="i" class="msg-item" @click="handleRead(msg)">
					<view class="msg-icon" :class="msg.iconClass">
						<text>{{ msg.icon }}</text>
					</view>
					<view class="msg-content">
						<view class="msg-title">
							<view style="flex-direction:row;align-items:center;gap:6px;">
								<text>{{ msg.title }}</text>
								<text v-if="!msg.isRead" class="msg-dot"></text>
							</view>
							<text class="msg-time">{{ msg.time }}</text>
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
		<TabBar current="messages" />
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { messageAPI } from '@/utils/request'
import TabBar from '@/components/TabBar.vue'

const messages = ref([])



const getStudentId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		const sid = obj.id || obj.userId
		return sid ? Number(sid) : null
	} catch (e) { return null }
}

onMounted(async () => {
	const sid = getStudentId()
	if (!sid) return
	try {
		const res = await messageAPI.getMessages({ studentId: sid })
		messages.value = res.data || []
	} catch (e) {
		console.error('加载消息失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
})

const handleRead = async (msg) => {
	if (!msg.isRead) {
		try {
			await messageAPI.readMessage(msg.id)
			msg.isRead = true
		} catch (e) {}
	}
}
</script>

<style scoped>
.msg-list {
	padding: 16px;
}
.msg-item {
	background: white;
	border-radius: 16px;
	padding: 16px;
	margin-bottom: 12px;
	flex-direction: row;
	gap: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.msg-icon {
	width: 48px;
	height: 48px;
	border-radius: 12px;
	background: linear-gradient(135deg, #165DFF, #60A5FA);
	align-items: center;
	justify-content: center;
	color: white;
	font-size: 20px;
	flex-shrink: 0;
}
.msg-icon.green { background: linear-gradient(135deg, #10B981, #34D399); }
.msg-icon.orange { background: linear-gradient(135deg, #F59E0B, #FBBF24); }
.msg-icon.ai { background: linear-gradient(135deg, #0EA5E9, #38BDF8); }
.msg-content {
	flex: 1;
	min-width: 0;
}
.msg-title {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 4px;
}
.msg-title text:first-child {
	font-size: 15px;
	font-weight: 600;
	color: #1D2129;
}
.msg-time {
	font-size: 12px;
	color: #86909C;
}
.msg-text {
	font-size: 13px;
	color: #86909C;
	line-height: 1.5;
	overflow: hidden;
	text-overflow: ellipsis;
	display: -webkit-box;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
}
.msg-dot {
	width: 8px;
	height: 8px;
	background: #EF4444;
	border-radius: 50%;
	flex-shrink: 0;
}
.empty-state {
	padding: 60px 20px;
	align-items: center;
}
.empty-text {
	font-size: 14px;
	color: #86909C;
}
</style>
