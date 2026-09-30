<template>
	<view class="page-wrapper">
		<NavBar title="投递记录" :showBack="false" />
		<!-- 统计卡片 -->
		<view class="stats-row">
			<view class="stat-card" :class="{ active: currentTab === 'all' }" @click="currentTab = 'all'">
				<text class="stat-num">{{ stats.all }}</text>
				<text class="stat-label">全部</text>
			</view>
			<view class="stat-card" :class="{ active: currentTab === 'pending' }" @click="currentTab = 'pending'">
				<text class="stat-num">{{ stats.pending }}</text>
				<text class="stat-label">待查看</text>
			</view>
			<view class="stat-card" :class="{ active: currentTab === 'interview' }" @click="currentTab = 'interview'">
				<text class="stat-num">{{ stats.interview }}</text>
				<text class="stat-label">面试中</text>
			</view>
			<view class="stat-card" :class="{ active: currentTab === 'ended' }" @click="currentTab = 'ended'">
				<text class="stat-num">{{ stats.ended }}</text>
				<text class="stat-label">已结束</text>
			</view>
		</view>
		<view class="filter-tabs">
			<text 
				v-for="(tab, i) in tabs" 
				:key="i" 
				class="filter-tab" 
				:class="{ active: currentTab === tab.value }" 
				@click="currentTab = tab.value"
			>{{ tab.label }}</text>
		</view>
		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<LoadingState type="skeleton" :rows="4" v-if="loading" />
			<view v-if="!loading">
			<!-- 最近面试快捷入口 -->
			<view v-if="nextInterview" class="interview-banner" @click="currentTab = 'interview'">
				<uni-icons type="calendar-filled" size="20" color="#165DFF" />
				<text class="banner-text">最近面试：<text class="banner-count">{{ nextInterview.interviewTime }}</text></text>
				<text class="banner-link">{{ nextInterview.companyName }} ›</text>
			</view>
			<view class="delivery-list">
				<view 
					v-for="(item, i) in filteredList" 
					:key="i" 
					class="delivery-card" 
					@click="goToJobDetail(item.jobId)"
				>
					<view class="card-body">
						<!-- 第一层：标题 + 状态标签 -->
						<view class="card-row1">
							<text class="card-title" lines="1">{{ item.jobTitle }}</text>
							<text 
								class="status-tag" 
								:style="{ background: statusBg(item.status), color: statusColor(item.status) }"
							>{{ item.statusText }}</text>
						</view>
						<!-- 第二层：公司名 -->
						<text class="card-company">{{ item.companyName }}</text>
						<!-- 第三层：地点/薪资 + 投递时间 -->
						<view class="card-meta-row">
							<view class="card-meta-left">
								<text v-if="item.location" class="meta-text">{{ item.location }}</text>
								<text v-if="item.location && item.salaryText" class="meta-divider">|</text>
								<text v-if="item.salaryText" class="meta-text meta-salary">{{ item.salaryText }}</text>
							</view>
							<text class="meta-time">{{ item.createTime }}</text>
						</view>
						<!-- 第四层：投递进度时间轴 -->
						<view class="timeline">
							<view 
								v-for="(node, ni) in timelineNodes(item)" 
								:key="ni" 
								class="tl-node"
							>
								<view class="tl-col">
									<view 
										class="tl-dot" 
										:class="{ done: node.done, current: node.current, rejected: node.rejected }"
									>
										<uni-icons 
											v-if="node.done" type="checkmarkempty" size="10" color="#FFFFFF" 
										/>
										<uni-icons 
											v-else-if="node.rejected" type="closeempty" size="10" color="#FFFFFF" 
										/>
									</view>
									<view 
										v-if="ni < 3" 
										class="tl-line" 
										:class="{ done: isTimelineLineDone(item.status, ni) }" 
									/>
								</view>
								<text 
									class="tl-label" 
									:class="{ done: node.done, current: node.current, rejected: node.rejected }"
								>{{ node.label }}</text>
							</view>
						</view>
						<!-- 面试信息内嵌 -->
						<view v-if="item.status === 'interview' && (item.interviewTime || item.interviewLocation)" class="interview-info">
							<text v-if="item.interviewTime" class="interview-row">
								<uni-icons type="calendar" size="12" color="#165DFF" /> 面试时间：{{ item.interviewTime }}
							</text>
							<text v-if="item.interviewLocation" class="interview-row">
								<uni-icons type="location" size="12" color="#165DFF" /> 面试地点：{{ item.interviewLocation }}
							</text>
						</view>
						<!-- 操作按钮 -->
						<view class="card-actions">
							<button v-if="item.status === 'pending'" class="action-btn btn-cancel" @click.stop="confirmCancel(item.id)">取消投递</button>
							<button v-if="item.status === 'accepted' || item.status === 'rejected'" class="action-btn btn-view" @click.stop="showResult(item)">查看结果</button>
						</view>
					</view>
				</view>
				<EmptyState v-if="!filteredList.length" icon="inbox" title="暂无投递记录" desc="去首页看看有没有心仪的岗位吧" />
			</view>
			</view>
		</scroll-view>
		<PopupDrawer :show="resultPopup" :title="resultTitle" @update:show="resultPopup = $event">
			<view class="result-content">
				<view class="result-icon" :class="resultAccepted ? 'icon-accepted' : 'icon-rejected'">
					<uni-icons :type="resultAccepted ? 'checkmark-circle' : 'close-circle'" size="48" :color="resultAccepted ? '#00B42A' : '#F53F3F'" />
				</view>
				<text class="result-status" :style="{ color: resultAccepted ? '#00B42A' : '#F53F3F' }">
					{{ resultAccepted ? '已通过' : '未通过' }}
				</text>
				<text v-if="resultFeedback" class="result-feedback">{{ resultFeedback }}</text>
				<text v-else class="result-feedback-empty">
					{{ resultAccepted ? '企业已确认录用，等待后续安排' : '企业未提供具体反馈' }}
				</text>
			</view>
		</PopupDrawer>
		<TabBar current="deliveries" />
	</view>
</template>

<script setup>
const loading = ref(true)
import LoadingState from '@/components/LoadingState.vue'
import { ref, computed } from 'vue'
import { deliveryAPI, scoreAPI } from '@/utils/request'
import TabBar from '@/components/TabBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import PopupDrawer from '@/components/PopupDrawer.vue'

const tabs = [
	{ label: '全部', value: 'all' },
	{ label: '待查看', value: 'pending' },
	{ label: '进行中', value: 'viewed' },
	{ label: '面试', value: 'interview' },
	{ label: '已结束', value: 'ended' }
]
const currentTab = ref('all')
const deliveries = ref([])
const resultPopup = ref(false)
const resultAccepted = ref(false)
const resultFeedback = ref('')
const resultTitle = ref('')
const refreshing = ref(false)

const DELIVERY_STATUS = ['pending', 'viewed', 'interview', 'accepted', 'rejected']
const DELIVERY_STATUS_TEXT = ['待查看', '已查看', '面试', '已通过', '未通过']

const statusColor = (s) => {
	const map = { pending: '#FF7D00', viewed: '#165DFF', interview: '#165DFF', accepted: '#00B42A', rejected: '#F53F3F' }
	return map[s] || '#C9CDD4'
}
const statusBg = (s) => {
	const map = { 
		pending: 'rgba(255,125,0,0.08)', 
		viewed: 'rgba(22,93,255,0.08)', 
		interview: 'rgba(22,93,255,0.08)', 
		accepted: 'rgba(0,180,42,0.08)', 
		rejected: 'rgba(245,63,63,0.08)' 
	}
	return map[s] || '#F7F8FA'
}

const mapDelivery = (d) => ({
	id: d.id,
	jobId: d.jobId,
	jobTitle: d.jobTitle || '',
	companyName: d.companyName || '',
	salaryText: d.salaryText || '',
	location: d.location || '',
	status: DELIVERY_STATUS[d.status] || 'pending',
	statusText: DELIVERY_STATUS_TEXT[d.status] || '待查看',
	createTime: d.createTime ? d.createTime.substring(0, 10) : '',
	interviewTime: d.interviewTime ? formatInterviewTime(d.interviewTime) : '',
	interviewLocation: d.interviewLocation || '',
	feedback: d.feedback || '',
	score: null,
	scoreDetail: null
})

const formatInterviewTime = (t) => {
	if (!t) return ''
	// 后端返回的是 LocalDateTime 格式 YYYY-MM-DDTHH:mm:ss
	const str = t.replace('T', ' ')
	return str.length > 16 ? str.substring(0, 16) : str
}

const DELIVERY_NODES = [
	{ key: 'delivered', label: '投递成功' },
	{ key: 'screening', label: 'HR筛选' },
	{ key: 'interview', label: '面试' },
	{ key: 'result', label: '录用' }
]

const timelineNodes = (item) => {
	const status = item.status
	const activeLevel = { pending: 0, viewed: 1, interview: 2, accepted: 3, rejected: 3 }
	const level = activeLevel[status] ?? 0
	return DELIVERY_NODES.map((node, i) => ({
		...node,
		done: status !== 'rejected' ? i <= level : i < 3 && i <= level,
		current: !['accepted', 'rejected'].includes(status) && i === level,
		rejected: status === 'rejected' && i === 3
	}))
}

const isTimelineLineDone = (status, nodeIndex) => {
	const activeLevel = { pending: 0, viewed: 1, interview: 2, accepted: 3, rejected: 3 }
	const level = activeLevel[status] ?? 0
	if (status === 'rejected') return nodeIndex < level && nodeIndex < 3
	return nodeIndex < level
}

const filteredList = computed(() => {
	if (currentTab.value === 'all') return deliveries.value
	if (currentTab.value === 'ended') return deliveries.value.filter(d => ['accepted', 'rejected'].includes(d.status))
	return deliveries.value.filter(d => d.status === currentTab.value)
})

const nextInterview = computed(() => {
	const interviews = deliveries.value
		.filter(d => d.status === 'interview' && d.interviewTime)
		.sort((a, b) => {
			if (!a.interviewTime || !b.interviewTime) return 0
			return a.interviewTime.localeCompare(b.interviewTime)
		})
	return interviews.length > 0 ? interviews[0] : null
})

const stats = computed(() => {
	const list = deliveries.value
	return {
		all: list.length,
		pending: list.filter(d => d.status === 'pending').length,
		interview: list.filter(d => d.status === 'interview').length,
		ended: list.filter(d => ['accepted', 'rejected'].includes(d.status)).length
	}
})

function getStudentId() {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.id || obj.userId ? Number(obj.id || obj.userId) : null
	} catch (e) { return null }
}

loadData()
async function loadData() {
	const sid = getStudentId()
	if (!sid) { loading.value = false; return }
	try {
		const res = await deliveryAPI.getDeliveriesByStudentId({ studentId: sid })
		deliveries.value = (res.data || []).map(mapDelivery)
		loadScores()
	} catch (e) { console.log('加载投递记录失败', e) }
	finally { loading.value = false }
}

const onRefresh = async () => {
	refreshing.value = true
	await loadData()
	refreshing.value = false
}

const loadScores = async () => {
	if (!deliveries.value.length) return
	const results = await Promise.allSettled(deliveries.value.map(d => scoreAPI.getByDelivery(d.id).catch(() => null)))
	results.forEach((r, i) => {
		if (r.status === 'fulfilled' && r.value && r.value.data) {
			deliveries.value[i].score = r.value.data.score
			deliveries.value[i].scoreDetail = r.value.data.scoreDetail || ''
		}
	})
}

const showResult = (item) => {
	resultAccepted.value = item.status === 'accepted'
	resultFeedback.value = item.feedback || ''
	resultTitle.value = resultAccepted.value ? '录用通知' : '投递结果'
	resultPopup.value = true
}

const confirmCancel = (id) => {
	uni.showModal({
		title: '确认取消',
		content: '确定要取消本次投递吗？',
		success: (r) => { if (r.confirm) cancelDelivery(id) }
	})
}

const cancelDelivery = async (id) => {
	try {
		await deliveryAPI.cancelDelivery(id)
		uni.showToast({ title: '已取消投递', icon: 'success' })
		deliveries.value = deliveries.value.filter(d => d.id !== id)
	} catch (e) { uni.showToast({ title: '操作失败', icon: 'none' }) }
}

const goToJobDetail = (jobId) => jobId && uni.navigateTo({ url: '/pages/student/job-detail?id=' + jobId })
</script>

<style scoped lang="scss">
/* 统计卡片 */
.stats-row {
	flex-direction: row;
	padding: 12px 16px 0;
	gap: 8px;
	background: $uni-bg-color;
}
.stat-card {
	flex: 1;
	align-items: center;
	padding: 10px 4px;
	border-radius: 12px;
	background: $uni-bg-color-page;
	gap: 2px;
}
.stat-card.active {
	background: $uni-color-primary;
}
.stat-card:active {
	opacity: 0.85;
}
.stat-num {
	font-size: 20px;
	font-weight: 700;
	color: $uni-text-color-title;
	line-height: 1.3;
}
.stat-card.active .stat-num {
	color: $uni-text-color-inverse;
}
.stat-label {
	font-size: 12px;
	color: $uni-text-color-secondary;
	line-height: 1.3;
}
.stat-card.active .stat-label {
	color: rgba(255,255,255,0.85);
}
.filter-tabs {
	flex-direction: row;
	padding: 8px 16px 0;
	background: $uni-bg-color;
	gap: 24px;
	border-bottom: 0.5px solid $uni-border-color-divider;
	height: 44px;
	align-items: center;
}
.filter-tab {
	font-size: 14px;
	color: $uni-text-color-secondary;
	font-weight: 500;
	position: relative;
	padding-bottom: 4px;
}
.filter-tab.active {
	color: $uni-text-color-title;
	font-weight: 600;
}
.filter-tab.active::after {
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
.delivery-list {
	padding: 12px 16px;
	gap: 12px;
}
.delivery-card {
	background: $uni-bg-color;
	border-radius: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.delivery-card:active { background: $uni-bg-color-page; }
.card-body {
	padding: 16px;
	gap: 8px;
}
/* 第一行：标题 + 状态标签 */
.card-row1 {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	gap: 8px;
}
.card-title {
	font-size: 16px;
	font-weight: 600;
	color: $uni-text-color-title;
	flex: 1;
	lines: 1;
}
.status-tag {
	font-size: 12px;
	font-weight: 500;
	padding: 3px 10px;
	border-radius: 4px;
	flex-shrink: 0;
}
/* 第二行：公司名 */
.card-company {
	font-size: 14px;
	color: $uni-text-color;
}
/* 第三行：地点/薪资 + 投递时间 */
.card-meta-row {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
}
.card-meta-left {
	flex-direction: row;
	align-items: center;
	gap: 4px;
}
.meta-text {
	font-size: 13px;
	color: $uni-text-color-secondary;
}
.meta-salary {
	color: $uni-color-primary;
	font-weight: 500;
}
.meta-divider {
	font-size: 12px;
	color: $uni-border-color;
}
.meta-time {
	font-size: 12px;
	color: $uni-text-color-placeholder;
}
/* 时间轴 */
.timeline {
	flex-direction: row;
	padding: 12px 0 4px;
	gap: 0;
}
.tl-node {
	flex: 1;
	align-items: center;
	gap: 4px;
}
.tl-col {
	flex-direction: row;
	align-items: center;
	width: 100%;
	position: relative;
	justify-content: center;
	height: 20px;
}
.tl-dot {
	width: 18px;
	height: 18px;
	border-radius: 50%;
	background: $uni-border-color;
	align-items: center;
	justify-content: center;
	z-index: 1;
	flex-shrink: 0;
}
.tl-dot.done {
	background: $uni-color-primary;
}
.tl-dot.current {
	background: $uni-color-primary;
	width: 20px;
	height: 20px;
	box-shadow: 0 0 0 4px $uni-color-primary-light;
}
.tl-dot.rejected {
	background: $uni-color-error;
}
.tl-line {
	position: absolute;
	left: calc(50% + 9px);
	width: calc(100% - 18px);
	height: 2px;
	background: $uni-border-color;
	flex-shrink: 1;
}
.tl-line.done {
	background: $uni-color-primary;
}
.tl-label {
	font-size: 12px;
	color: $uni-text-color-placeholder;
	line-height: 1.3;
}
.tl-label.done {
	color: $uni-color-primary;
	font-weight: 500;
}
.tl-label.current {
	color: $uni-color-primary;
	font-weight: 600;
}
.tl-label.rejected {
	color: $uni-color-error;
}
/* 面试信息内嵌展示 */
.interview-info {
	background: $uni-color-primary-light;
	border-radius: 8px;
	padding: 10px 12px;
	margin-top: 4px;
	gap: 6px;
}
.interview-row {
	flex-direction: row;
	align-items: center;
	gap: 4px;
	font-size: 13px;
	color: $uni-text-color-title;
	line-height: 1.5;
}
/* 操作按钮 */
.card-actions {
	flex-direction: row;
	justify-content: flex-end;
	gap: 8px;
	margin-top: 4px;
}
.action-btn {
	padding: 6px 14px;
	border-radius: 8px;
	font-size: 12px;
	font-weight: 500;
	border: none;
	height: 32px;
}
.btn-cancel {
	background: $uni-bg-color-page;
	color: $uni-text-color-secondary;
}
.btn-view {
	background: $uni-color-primary-light;
	color: $uni-color-primary;
}
/* 投递结果弹窗 */
.result-content {
	align-items: center;
	gap: 16px;
	padding: 20px 0;
}
.result-icon { margin-bottom: 4px; }
.result-status {
	font-size: 24px;
	font-weight: 700;
}
.result-feedback {
	font-size: 14px;
	color: $uni-text-color;
	line-height: 1.6;
	text-align: center;
	background: $uni-bg-color-page;
	border-radius: 8px;
	padding: 14px 16px;
	width: 100%;
	box-sizing: border-box;
}
.result-feedback-empty {
	font-size: 14px;
	color: $uni-text-color-placeholder;
	text-align: center;
}
/* 面试日程快捷入口 */
.interview-banner {
	flex-direction: row;
	align-items: center;
	background: $uni-color-primary-light;
	border: 1px solid $uni-color-primary-light;
	border-radius: 12px;
	padding: 12px 16px;
	margin: 12px 16px 0;
	gap: 8px;
}
.interview-banner:active { background: $uni-color-primary-light; }
.banner-text { flex: 1; font-size: 14px; color: $uni-text-color-title; }
.banner-count { font-size: 16px; font-weight: 700; color: $uni-color-primary; }
.banner-link { font-size: 13px; color: $uni-color-primary; font-weight: 500; }
</style>
