<template>
	<view class="page-wrapper">
		<NavBar title="消息通知" showBack @back="goBack" />
		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<template v-if="loading && !messages.length">
				<view style="padding:40px;align-items:center;">
					<uni-icons type="spinner" size="30" color="#C9CDD4" />
					<text style="font-size:13px;color:#C9CDD4;margin-top:8px;">加载中...</text>
				</view>
			</template>

			<view v-else class="msg-list">
				<view v-for="(msg, i) in messages" :key="i" class="msg-item" @click="handleRead(msg)">
					<view class="msg-icon" :style="{ background: msg.bgColor || '#F2F3F5' }">
						<uni-icons :type="msg.icon || 'chat'" :size="18" :color="msg.iconColor || '#86909C'" />
					</view>
					<view class="msg-content">
						<view class="msg-title-row">
							<view style="flex-direction:row;align-items:center;gap:6px;flex:1;">
								<text class="msg-title" :class="{ unread: !msg.isRead }">{{ msg.title || '消息' }}</text>
								<text v-if="!msg.isRead" class="msg-dot"></text>
							</view>
							<text class="msg-time">{{ formatTime(msg.createTime) }}</text>
						</view>
						<text class="msg-text">{{ msg.content || msg.message || '' }}</text>
					</view>
				</view>

				<view v-if="!messages.length && !loading" class="empty-state">
					<uni-icons type="chat" size="40" color="#C9CDD4" />
					<text class="empty-text">暂无消息</text>
					<text style="font-size:12px;color:#C9CDD4;margin-top:4px;">有新的投递或面试通知会出现在这里</text>
				</view>
			</view>

			<view style="height:40px;" />
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { hrAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'

const messages = ref([])
const loading = ref(false)

const getMessagesForHR = async () => {
	// 从所有投递中获取最近的消息动态
	const cId = getCompanyId()
	if (!cId) return []
	const jobsRes = await hrAPI.getHrJobs(cId)
	const jobs = jobsRes.data || []
	const all = []
	for (const job of jobs) {
		try {
			const dRes = await hrAPI.getCompanyDeliveries(job.id)
			;(dRes.data || []).forEach(d => {
				const statusMap = { 0: '投递了', 1: '已查看', 2: '安排了面试', 3: '已录用', 4: '未通过' }
				const statusIcon = { 0: 'paperplane', 1: 'eye', 2: 'calendar', 3: 'checkmark', 4: 'close' }
				const statusColor = { 0: '#F59E0B', 1: '#165DFF', 2: '#165DFF', 3: '#00B42A', 4: '#EF4444' }
				const bgColor = { 0: '#FEF3E8', 1: '#E6F1FB', 2: '#E6F1FB', 3: '#EAF3DE', 4: '#FCEBEB' }
				all.push({
					title: d.studentName || '候选人' + ' ' + (statusMap[d.status] || '投递了'),
					content: statusMap[d.status] || '投递了' + '「' + (job.title || '') + '」',
					createTime: d.createTime || d.updateTime,
					isRead: false,
					icon: statusIcon[d.status] || 'chat',
					iconColor: statusColor[d.status] || '#86909C',
					bgColor: bgColor[d.status] || '#F2F3F5'
				})
			})
		} catch (e) { console.error('获取消息数据失败', e) }
	}
	all.sort((a, b) => new Date(b.createTime || 0) - new Date(a.createTime || 0))
	return all.slice(0, 50)
}

const loadMessages = async () => {
	loading.value = true
	try {
		const data = await getMessagesForHR()
		messages.value = data
	} catch (e) {
		console.error('加载消息失败', e)
	} finally {
		loading.value = false
	}
}

const refreshing = ref(false)
const onRefresh = async () => {
	refreshing.value = true
	await loadMessages()
	refreshing.value = false
}

const handleRead = (msg) => {
	msg.isRead = true
}

const getCompanyId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.companyId || obj.id || null
	} catch (e) {
		console.error('获取公司ID失败', e)
		return null
	}
}

const formatTime = (t) => {
	if (!t) return ''
	if (t.length >= 16) {
		const dateStr = t.substring(5, 10)
		const timeStr = t.substring(11, 16)
		return dateStr + ' ' + timeStr
	}
	return t.substring(0, 10)
}

const goBack = () => uni.navigateBack()

onMounted(loadMessages)
</script>

<style scoped>
.msg-list { padding: 12px 16px; }
.msg-item {
	flex-direction: row;
	padding: 14px;
	background: #fff;
	border-radius: 12px;
	margin-bottom: 10px;
	gap: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.msg-item:active { background: #F7F8FA; }
.msg-icon {
	width: 40px; height: 40px; border-radius: 50%;
	align-items: center; justify-content: center;
	flex-shrink: 0;
}
.msg-content { flex: 1; }
.msg-title-row {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 4px;
}
.msg-title { font-size: 14px; font-weight: 500; color: #1D2129; }
.msg-title.unread { font-weight: 700; }
.msg-dot {
	width: 7px; height: 7px; border-radius: 50%;
	background: #EF4444; flex-shrink: 0;
}
.msg-time { font-size: 11px; color: #C9CDD4; flex-shrink: 0; }
.msg-text { font-size: 13px; color: #86909C; line-height: 1.5; }
.empty-state {
	padding: 60px 20px;
	align-items: center;
	justify-content: center;
	gap: 8px;
}
.empty-text { font-size: 14px; color: #86909C; }
</style>
