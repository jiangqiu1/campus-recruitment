<template>
	<view class="page-wrapper">
		<NavBar title="消息中心" :showBack="false" />
		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<LoadingState type="skeleton" :rows="4" v-if="loading" />
			<view v-if="!loading">
			<view class="msg-tabs">
				<text v-for="cat in categories" :key="cat.value" class="msg-tab" :class="{ active: currentCat === cat.value }" @click="currentCat = cat.value">{{ cat.label }}</text>
			</view>
			<!-- 待办摘要：把"下一步做什么"顶到消息列表上方 -->
			<view v-if="todoInterview > 0 || todoMatch > 0" class="todo-strip">
				<view v-if="todoInterview > 0" class="todo-strip-item" @click="goDeliveries">
					<uni-icons type="calendar-filled" size="16" color="#0EA5E9" />
					<text class="todo-strip-text">有 <text class="todo-strip-num">{{ todoInterview }}</text> 场面试待确认</text>
					<uni-icons type="arrowright" size="12" color="#C9CDD4" />
				</view>
				<view v-if="todoMatch > 0" class="todo-strip-item" :class="{ divided: todoInterview > 0 }" @click="goMatches">
					<uni-icons type="star-filled" size="16" color="#165DFF" />
					<text class="todo-strip-text">AI 新匹配 <text class="todo-strip-num">{{ todoMatch }}</text> 个岗位</text>
					<uni-icons type="arrowright" size="12" color="#C9CDD4" />
				</view>
			</view>
			<view class="msg-group" v-for="group in groupedMessages" :key="group.label">
				<text class="msg-group-title">{{ group.label }}</text>
				<view v-for="(msg, i) in group.items" :key="group.label + i" class="msg-item" :class="{ unread: !msg.isRead, 'msg-today': isToday(msg.createTime || msg.time) }" @click="handleRead(msg)">
					<view class="msg-icon" :class="'msg-icon--' + msg.type">
						<uni-icons :type="msgIcon(msg.type)" :size="20" :color="msgIconColor(msg.type)" />
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
			</view>
			<EmptyState v-if="!filteredList.length" icon="chat" title="暂无消息" desc="有新的投递反馈或面试通知会出现在这里" />
			</view>
			<view style="height: calc(60px + env(safe-area-inset-bottom))" />
		</scroll-view>
		<TabBar current="messages" />
	</view>
</template>

<script setup>
import LoadingState from '@/components/LoadingState.vue'
import { ref, computed } from 'vue'
import { messageAPI, deliveryAPI, matchAPI } from '@/utils/request'
import TabBar from '@/components/TabBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import NavBar from '@/components/NavBar.vue'

const messages = ref([])
const currentCat = ref('all')
const todoInterview = ref(0)
const todoMatch = ref(0)

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
// 类型上色：AI 青 / 系统 蓝 / 企业 橙
const msgIconColor = (type) => {
	const map = { system: '#165DFF', company: '#FF7D00', ai: '#0EA5E9' }
	return map[type] || '#86909C'
}

const filteredList = computed(() => {
	let base = currentCat.value === 'all' ? messages.value : messages.value.filter(m => m.type === currentCat.value)
	// 聚合重复的轻量通知：同类「投递成功通知」超过 2 条时合并为一条汇总
	const AGG = '投递成功通知'
	const hits = base.filter(m => m.title === AGG)
	if (hits.length > 2) {
		const newest = hits.reduce((a, b) => ((a.time || '') > (b.time || '') ? a : b))
		const aggregated = {
			...newest,
			content: '你已成功投递 ' + hits.length + ' 个岗位，可在「投递」页查看进展与反馈',
			__aggIds: hits.map(h => h.id)
		}
		base = [aggregated, ...base.filter(m => m.title !== AGG)]
	}
	return base
})

// 日期分组：今天 / 昨天 / M月D日 / 跨年带年份
const groupedMessages = computed(() => {
	const today = new Date()
	const startOf = (x) => new Date(x.getFullYear(), x.getMonth(), x.getDate()).getTime()
	const buckets = []
	const map = {}
	for (const m of filteredList.value) {
		const key = (m.time || '').substring(0, 10)
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
	if (!sid) { loading.value = false; return }
	try {
		const res = await messageAPI.getMessages({ studentId: sid })
		messages.value = (res.data || []).map(m => ({
			...m,
			type: TYPE_MAP[m.type] || 'system',
			time: m.createTime ? m.createTime.replace('T', ' ').substring(0, 16) : ''
		}))
	} catch (e) { console.log('加载消息失败', e) }
	finally { loading.value = false }
	loadTodos()
}

// 待办摘要：从投递与匹配数据拼出"下一步做什么"
const loadTodos = async () => {
	const sid = getStudentId()
	if (!sid) return
	try {
		const [dRes, mRes] = await Promise.all([
			deliveryAPI.getDeliveriesByStudentId({ studentId: sid }).catch(() => null),
			matchAPI.getByStudent(sid).catch(() => null)
		])
		todoInterview.value = (dRes && dRes.data || []).filter(d => Number(d.status) === 2).length
		todoMatch.value = (mRes && mRes.data || []).length
	} catch (e) { /* 待办数据失败不影响消息列表 */ }
}

const goDeliveries = () => uni.reLaunch({ url: '/pages/student/deliveries' })
const goMatches = () => uni.navigateTo({ url: '/pages/student/ai-matches' })

const refreshing = ref(false)

const onRefresh = async () => {
	refreshing.value = true
	await loadData()
	refreshing.value = false
}

const handleRead = async (msg) => {
	if (msg.__aggIds) {
		// 聚合消息：点击一次性标记所有成员已读
		try { await Promise.all(msg.__aggIds.map(id => messageAPI.readMessage(id))) } catch (e) {}
		msg.isRead = true
		return
	}
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

<style scoped lang="scss">
.msg-tabs {
	flex-direction: row;
	padding: 0 16px;
	background: $uni-bg-color;
	gap: 24px;
	border-bottom: 0.5px solid $uni-border-color-divider;
	height: 44px;
	align-items: center;
}
.msg-tab {
	font-size: 14px;
	color: $uni-text-color-secondary;
	font-weight: 500;
	position: relative;
	padding-bottom: 4px;
}
.msg-tab.active {
	color: $uni-text-color-title;
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
	background: $uni-color-primary;
	border-radius: 4px;
}
.msg-tabs:active { opacity: 0.7; }
.msg-list {
	padding: 8px 16px;
	gap: 0;
}
.msg-group-title {
	font-size: 12px;
	font-weight: 500;
	color: $uni-text-color-secondary;
	padding: 12px 4px 4px;
	display: block;
}
.msg-item {
	flex-direction: row;
	background: $uni-bg-color;
	padding: 14px 14px 14px 0;
	gap: 12px;
	align-items: flex-start;
	position: relative;
	border-bottom: 0.5px solid $uni-border-color-divider;
}
.msg-item:last-child {
	border-bottom: none;
}
.msg-icon {
	width: 40px;
	height: 40px;
	border-radius: 12px;
	background: $uni-bg-color-page;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}
.msg-icon--ai { background: $uni-color-ai-light; }
.msg-icon--system { background: $uni-color-primary-light; }
.msg-icon--company { background: $uni-color-warning-light; }
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
	color: $uni-text-color-title;
}
.msg-item.unread .msg-title {
	font-weight: 600;
}
.msg-today .msg-title {
	font-weight: 700;
}
.msg-time {
	font-size: 12px;
	color: $uni-text-color-placeholder;
}
.msg-text {
	font-size: 13px;
	color: $uni-text-color-secondary;
	line-height: 1.5;
	overflow: hidden;
	text-overflow: ellipsis;
	display: -webkit-box;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
}
.msg-red-dot {
	position: absolute;
	top: 17px;
	right: 5px;
	width: 6px;
	height: 6px;
	background: $uni-color-error;
	border-radius: 50%;
}
.msg-tab:active { opacity: 0.7; }
.msg-item:active { background: $uni-bg-color-page; }

/* 待办摘要条 */
.todo-strip {
	background: $uni-bg-color;
	border-radius: 12px;
	margin: 12px 16px 0;
	padding: 4px 14px;
	box-shadow: $uni-shadow-sm;
}
.todo-strip-item {
	flex-direction: row;
	align-items: center;
	gap: 8px;
	padding: 11px 0;
}
.todo-strip-item.divided {
	border-top: 0.5px solid $uni-border-color-divider;
}
.todo-strip-item:active { opacity: 0.7; }
.todo-strip-text {
	flex: 1;
	font-size: 13px;
	color: $uni-text-color-title;
}
.todo-strip-num {
	font-size: 15px;
	font-weight: 700;
	color: $uni-color-primary;
}
</style>
