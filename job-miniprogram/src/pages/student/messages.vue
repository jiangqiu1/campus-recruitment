<template>
	<view class="page-wrapper">
		<NavBar title="消息中心" :showBack="false" />
		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<view class="msg-tabs">
				<text v-for="cat in categories" :key="cat.value" class="msg-tab" :class="{ active: currentCat === cat.value }" @click="currentCat = cat.value">{{ cat.label }}</text>
			</view>
			<view class="msg-list">
				<view v-for="(msg, i) in filteredList" :key="i" class="msg-item" :class="{ unread: !msg.isRead, 'msg-today': isToday(msg.createTime || msg.time) }" @click="handleRead(msg)">
					<view class="msg-icon">
						<uni-icons :type="msgIcon(msg.type)" :size="20" color="#86909C" />
					</view>
					<view class="msg-content">
						<view class="msg-top">
							<text class="msg-title">{{ msg.title }}</text>
							<text class="msg-time">{{ msg.time }}</text>
						</view>
						<text class="msg-text">{{ msg.content }}</text>
					</view>
					<view v-if="!msg.isRead" class="msg-red-dot" />
				</view>
				<EmptyState v-if="!filteredList.length" icon="chat" title="暂无消息" desc="有新的投递反馈或面试通知会出现在这里" />
			</view>
		</scroll-view>
		<TabBar current="messages" />
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { messageAPI } from '@/utils/request'
import TabBar from '@/components/TabBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import NavBar from '@/components/NavBar.vue'

const messages = ref([])
const currentCat = ref('all')

const categories = [
	{ label: '全部', value: 'all' },
	{ label: '系统', value: 'system' },
	{ label: '企业', value: 'company' },
	{ label: 'AI', value: 'ai' }
]

const msgIcon = (type) => {
	const map = { system: 'gear', company: 'shop', ai: 'star' }
	return map[type] || 'chat'
}

const filteredList = computed(() => {
	if (currentCat.value === 'all') return messages.value
	return messages.value.filter(m => m.type === currentCat.value)
})

function getStudentId() {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.id || obj.userId ? Number(obj.id || obj.userId) : null
	} catch (e) { return null }
}

const TYPE_MAP = { 0: 'system', 1: 'company', 2: 'ai' }

loadData()
async function loadData() {
	const sid = getStudentId()
	if (!sid) return
	try {
		const res = await messageAPI.getMessages({ studentId: sid })
		messages.value = (res.data || []).map(m => ({
			...m,
			type: TYPE_MAP[m.type] || 'system',
			time: m.createTime ? m.createTime.replace('T', ' ').substring(0, 16) : ''
		}))
	} catch (e) { console.log('加载消息失败', e) }
}

const refreshing = ref(false)

const onRefresh = async () => {
	refreshing.value = true
	await loadData()
	refreshing.value = false
}

const handleRead = async (msg) => {
	if (!msg.isRead) {
		try { await messageAPI.readMessage(msg.id); msg.isRead = true } catch (e) {}
	}
}

const isToday = (t) => {
	if (!t) return false
	const today = new Date().toISOString().substring(0, 10)
	return t.substring(0, 10) === today
}
</script>

<style scoped>
.msg-tabs {
	flex-direction: row;
	padding: 0 16px;
	background: #FFFFFF;
	gap: 24px;
	border-bottom: 0.5px solid #F2F3F5;
	height: 44px;
	align-items: center;
}
.msg-tab {
	font-size: 14px;
	color: #86909C;
	font-weight: 500;
	position: relative;
	padding-bottom: 4px;
}
.msg-tab.active {
	color: #1D2129;
	font-weight: 600;
}
.msg-tab.active::after {
	content: '';
	position: absolute;
	bottom: 0;
	left: 50%;
	transform: translateX(-50%);
	width: 20px;
	height: 3px;
	background: #165DFF;
	border-radius: 2px;
}
.msg-tabs:active { opacity: 0.7; }
.msg-list {
	padding: 8px 16px;
	gap: 0;
}
.msg-item {
	flex-direction: row;
	background: #FFFFFF;
	padding: 14px 14px 14px 0;
	gap: 12px;
	align-items: flex-start;
	position: relative;
	border-bottom: 0.5px solid #F2F3F5;
}
.msg-item:last-child {
	border-bottom: none;
}
.msg-icon {
	width: 40px;
	height: 40px;
	border-radius: 10px;
	background: #F7F8FA;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}
.msg-content {
	flex: 1;
	gap: 4px;
}
.msg-top {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
}
.msg-title {
	font-size: 15px;
	font-weight: 500;
	color: #1D2129;
}
.msg-item.unread .msg-title {
	font-weight: 700;
}
.msg-today .msg-title {
	font-weight: 700;
}
.msg-time {
	font-size: 12px;
	color: #C9CDD4;
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
.msg-red-dot {
	position: absolute;
	top: 16px;
	right: 4px;
	width: 8px;
	height: 8px;
	background: #F53F3F;
	border-radius: 50%;
}
.msg-tab:active { opacity: 0.7; }
.msg-item:active { background: #F7F8FA; }
</style>
