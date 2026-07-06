<template>
	<view class="page-wrapper">
		<NavBar title="投递记录" :showBack="false" />
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
			<view class="delivery-list">
				<view 
					v-for="(item, i) in filteredList" 
					:key="i" 
					class="delivery-card" 
					@click="goToJobDetail(item.jobId)"
				>
					<view class="card-left-stripe" :style="{ background: statusColor(item.status) }" />
					<view class="card-body">
						<view class="card-top">
							<text class="card-title">{{ item.jobTitle }}</text>
							<text class="card-salary">{{ item.salaryText || '' }}</text>
						</view>
						<text class="card-company">{{ item.companyName }}</text>
						<view class="card-meta">
							<text 
								class="status-tag" 
								:style="{ background: statusBg(item.status), color: statusColor(item.status) }"
							>{{ item.statusText }}</text>
							<text class="card-time">{{ item.createTime }}</text>
						</view>
						<view class="card-actions">
							<button v-if="item.status === 'pending'" class="action-btn btn-cancel" @click.stop="confirmCancel(item.id)">取消投递</button>
							<button v-if="item.status === 'interview'" class="action-btn btn-view" @click.stop="viewInterview">查看面试详情</button>
							<button v-if="item.status === 'accepted' || item.status === 'rejected'" class="action-btn btn-view" @click.stop="showScoreDetail(item)">查看结果</button>
						</view>
					</view>
				</view>
				<EmptyState v-if="!filteredList.length" icon="inbox" title="暂无投递记录" desc="去首页看看有没有心仪的岗位吧" />
			</view>
		</scroll-view>
		<PopupDrawer :show="scorePopup" :title="'简历评分'" @update:show="scorePopup = $event">
			<view class="score-content">
				<text class="score-big">{{ currentScore.score }}分</text>
				<view class="score-dims">
					<view v-for="(dim, i) in scoreDims" :key="i" class="score-dim">
						<text class="dim-name">{{ dim.name }}</text>
						<view class="dim-bar"><view class="dim-fill" :style="{ width: dim.score + '%' }" /></view>
					</view>
				</view>
				<text class="score-detail-text">{{ currentScore.scoreDetail || '暂无详细评分数据' }}</text>
			</view>
		</PopupDrawer>
		<TabBar current="deliveries" />
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { deliveryAPI, scoreAPI } from '@/utils/request'
import TabBar from '@/components/TabBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import PopupDrawer from '@/components/PopupDrawer.vue'

const tabs = [
	{ label: '全部', value: 'all' },
	{ label: '待查看', value: 'pending' },
	{ label: '进行中', value: 'viewed' },
	{ label: '已结束', value: 'ended' }
]
const currentTab = ref('all')
const deliveries = ref([])
const scorePopup = ref(false)
const currentScore = ref({})
const scoreDims = ref([])
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
	status: DELIVERY_STATUS[d.status] || 'pending',
	statusText: DELIVERY_STATUS_TEXT[d.status] || '待查看',
	createTime: d.createTime ? d.createTime.substring(0, 10) : '',
	score: null,
	scoreDetail: null
})

const filteredList = computed(() => {
	if (currentTab.value === 'all') return deliveries.value
	if (currentTab.value === 'ended') return deliveries.value.filter(d => ['accepted', 'rejected'].includes(d.status))
	return deliveries.value.filter(d => d.status === currentTab.value)
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
	if (!sid) return
	try {
		const res = await deliveryAPI.getDeliveriesByStudentId({ studentId: sid })
		deliveries.value = (res.data || []).map(mapDelivery)
		loadScores()
	} catch (e) { console.log('加载投递记录失败', e) }
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

const showScoreDetail = (item) => {
	currentScore.value = item
	scoreDims.value = [
		{ name: '技能', score: Math.min((item.score || 0) + 5, 100) },
		{ name: '经验', score: Math.max((item.score || 0) - 10, 0) },
		{ name: '学历', score: item.score || 0 }
	]
	scorePopup.value = true
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

const viewInterview = () => uni.showToast({ title: '面试详情', icon: 'none' })
const goToJobDetail = (jobId) => jobId && uni.navigateTo({ url: '/pages/student/job-detail?id=' + jobId })
</script>

<style scoped>
.filter-tabs {
	flex-direction: row;
	padding: 0 16px;
	background: #FFFFFF;
	gap: 24px;
	border-bottom: 0.5px solid #F2F3F5;
	height: 44px;
	align-items: center;
}
.filter-tab {
	font-size: 14px;
	color: #86909C;
	font-weight: 500;
	position: relative;
	padding-bottom: 4px;
}
.filter-tab.active {
	color: #1D2129;
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
	background: #165DFF;
	border-radius: 2px;
}
.delivery-list {
	padding: 12px 16px;
	gap: 12px;
}
.delivery-card {
	flex-direction: row;
	background: #FFFFFF;
	border-radius: 12px;
	overflow: hidden;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.card-left-stripe {
	width: 3px;
	flex-shrink: 0;
}
.card-body {
	flex: 1;
	padding: 14px 16px;
	gap: 6px;
}
.card-top {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
}
.card-title {
	font-size: 15px;
	font-weight: 600;
	color: #1D2129;
	flex: 1;
}
.card-salary {
	font-size: 15px;
	font-weight: 600;
	color: #165DFF;
}
.card-company {
	font-size: 13px;
	color: #86909C;
}
.card-meta {
	flex-direction: row;
	align-items: center;
	gap: 8px;
}
.status-tag {
	font-size: 12px;
	font-weight: 500;
	padding: 2px 10px;
	border-radius: 4px;
}
.card-time {
	font-size: 12px;
	color: #C9CDD4;
	margin-left: auto;
}
.card-actions {
	flex-direction: row;
	gap: 8px;
	margin-top: 8px;
}
.action-btn {
	padding: 6px 14px;
	border-radius: 6px;
	font-size: 12px;
	font-weight: 500;
	border: none;
	height: 32px;
}
.btn-cancel {
	background: #F7F8FA;
	color: #86909C;
}
.btn-view {
	background: rgba(22,93,255,0.08);
	color: #165DFF;
}
.score-content {
	align-items: center;
	gap: 16px;
}
.score-big {
	font-size: 40px;
	font-weight: 700;
	color: #165DFF;
}
.score-dims {
	width: 100%;
	gap: 12px;
}
.score-dim {
	flex-direction: row;
	align-items: center;
	gap: 10px;
}
.dim-name {
	font-size: 13px;
	color: #86909C;
	width: 40px;
}
.dim-bar {
	flex: 1;
	height: 6px;
	background: #F2F3F5;
	border-radius: 3px;
	overflow: hidden;
}
.dim-fill {
	height: 100%;
	background: #165DFF;
	border-radius: 3px;
}
.score-detail-text {
	font-size: 13px;
	color: #C9CDD4;
	line-height: 1.6;
	text-align: center;
}
.delivery-card:active { background: #F7F8FA; }
.action-btn:active { opacity: 0.85; }
</style>
