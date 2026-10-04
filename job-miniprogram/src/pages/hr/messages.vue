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
				<view class="msg-group" v-for="group in groupedMessages" :key="group.label">
					<text class="msg-group-title">{{ group.label }}</text>
					<view v-for="(msg, i) in group.items" :key="group.label + i" class="msg-item" @click="handleRead(msg)">
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
				</view>

				<EmptyState v-if="!messages.length && !loading" icon="chat" title="暂无消息" desc="有新的投递或面试通知会出现在这里" />
			</view>

			<view style="height:40px;" />
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { hrAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import LoadingState from '@/components/LoadingState.vue'
import EmptyState from '@/components/EmptyState.vue'

const messages = ref([])
const loading = ref(false)

// 日期分组：今天 / 昨天 / M月D日 / 跨年带年份（渲染层）
const groupedMessages = computed(() => {
	const today = new Date()
	const startOf = (x) => new Date(x.getFullYear(), x.getMonth(), x.getDate()).getTime()
	const buckets = []
	const map = {}
	for (const m of messages.value) {
		const key = (m.createTime || '').substring(0, 10)
		if (!key) continue
		if (!map[key]) {
			const dd = new Date(key + 'T00:00')
			const diff = Math.round((startOf(dd) - startOf(today)) / 86400000)
			let label
			if (diff === 0) label = '今天'
			else if (diff === -1) label = '昨天'
			else if (diff < 0 && diff >= -6) label = ['周日','周一','周二','周三','周四','周五','周六'][dd.getDay()]
			else label = dd.getFullYear() === today.getFullYear() ? (dd.getMonth() + 1) + '月' + dd.getDate() + '日' : key
			map[key] = { label, items: [] }
			buckets.push(map[key])
		}
		map[key].items.push(m)
	}
	return buckets
})

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
				const statusColor = { 0: '#FF7D00', 1: '#165DFF', 2: '#165DFF', 3: '#00B42A', 4: '#F53F3F' }
				const bgColor = { 0: 'rgba(255,125,0,0.08)', 1: 'rgba(22,93,255,0.08)', 2: 'rgba(22,93,255,0.08)', 3: 'rgba(0,180,42,0.08)', 4: 'rgba(245,63,63,0.08)' }
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

<style scoped lang="scss">
.msg-group-title {
	font-size: 12px;
	font-weight: 500;
	color: #86909C;
	padding: 12px 4px 4px;
	display: block;
}
.msg-list { padding: 12px 16px; }
.msg-item {
	flex-direction: row;
	padding: 14px;
	background: $uni-bg-color;
	border-radius: 12px;
	margin-bottom: 10px;
	gap: 12px;
	box-shadow: $uni-shadow-card;
}
.msg-item:active { background: $uni-bg-color-page; }
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
.msg-title { font-size: 14px; font-weight: 500; color: $uni-text-color-title; }
.msg-title.unread { font-weight: 700; }
.msg-dot {
	width: 7px; height: 7px; border-radius: 50%;
	background: $uni-color-error; flex-shrink: 0;
}
.msg-time { font-size: 12px; color: $uni-text-color-placeholder; flex-shrink: 0; }
.msg-text { font-size: 13px; color: $uni-text-color-secondary; line-height: 1.5; }
.empty-state {
	padding: 60px 20px;
	align-items: center;
	justify-content: center;
	gap: 8px;
}
.empty-text { font-size: 14px; color: $uni-text-color-secondary; }
</style>
