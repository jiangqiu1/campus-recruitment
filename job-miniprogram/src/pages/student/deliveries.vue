<template>
	<view class="page-wrapper">
		<view class="header-simple" style="padding:12px 16px;">
			<text style="font-size:18px;font-weight:700;color:white;">投递记录</text>
		</view>

		<!-- 状态筛选 -->
		<view class="filter-tabs">
			<text v-for="(tab, i) in tabs" :key="i" class="filter-tab" :class="{ active: currentTab === tab.value }" @click="currentTab = tab.value">{{ tab.label }}</text>
		</view>

		<scroll-view class="content-scrollable" scroll-y>
			<view class="timeline">
				<view v-for="(item, i) in filteredList" :key="i" class="timeline-item">
					<view class="timeline-dot" :class="item.dotClass">
						<text>{{ item.dotIcon }}</text>
					</view>
					<view class="timeline-content">
						<view class="timeline-header">
							<text class="timeline-title">{{ item.jobTitle }}</text>
							<text v-if="item.score !== null" class="score-badge" :class="item.scoreClass">{{ item.score }}分</text>
							<text v-else class="score-badge score-pending">待评分</text>
						</view>
						<text class="timeline-desc">{{ item.companyName }}</text>
						<view class="timeline-meta">
							<text class="tag-blue card-tag">{{ item.statusText }}</text>
							<text class="timeline-time">{{ item.createTime }}</text>
						</view>
						<view v-if="item.status === 'pending'" class="timeline-actions">
							<button class="btn-sm btn-outline" @click="cancelDelivery(item.id)">取消投递</button>
						</view>
						<text v-if="item.scoreDetail" class="score-detail" @click="showScoreDetail(item)">查看评分详情 →</text>
					</view>
				</view>
				<view v-if="!filteredList.length" class="empty-state">
					<text style="font-size:48px;margin-bottom:12px;">📭</text>
					<text class="empty-text">暂无投递记录</text>
				</view>
			</view>
		</scroll-view>

		<TabBar current="deliveries" />
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { deliveryAPI, scoreAPI } from '@/utils/request'
import TabBar from '@/components/TabBar.vue'

const tabs = [
	{ label: '全部', value: 'all' },
	{ label: '待查看', value: 'pending' },
	{ label: '已查看', value: 'viewed' },
	{ label: '面试', value: 'interview' },
	{ label: '已通过', value: 'accepted' },
	{ label: '未通过', value: 'rejected' }
]
const refreshing = ref(false)
const currentTab = ref('all')
const deliveries = ref([])

const loadData = async () => {
	const sid = getStudentId()
	if (!sid) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
	try {
		const res = await deliveryAPI.getDeliveriesByStudentId({ studentId: sid })
		deliveries.value = (res.data || []).map(mapDelivery)
		loadScores(deliveries.value)
	} catch (e) {
		console.error('加载投递记录失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

const onRefresh = async () => {
	refreshing.value = true
	await loadData()
	refreshing.value = false
}

// 后端 Integer(0-4) → 前端
const DELIVERY_STATUS = ['pending', 'viewed', 'interview', 'accepted', 'rejected']
const DELIVERY_STATUS_TEXT = ['待查看', '已查看', '面试', '已通过', '未通过']
const DOT_CONFIG = ['orange', '', 'green', 'green', 'gray']
const DOT_ICONS = ['📬', '👀', '📞', '✅', '❌']

const mapDelivery = (d) => ({
	id: d.id,
	jobId: d.jobId,
	jobTitle: d.jobTitle || '',
	companyName: d.companyName || '',
	status: DELIVERY_STATUS[d.status] || 'pending',
	statusText: DELIVERY_STATUS_TEXT[d.status] || '待查看',
	dotClass: DOT_CONFIG[d.status] || '',
	dotIcon: DOT_ICONS[d.status] || '📬',
	createTime: d.createTime ? d.createTime.substring(0, 10) : '',
	score: null,
	scoreDetail: '',
	scoreClass: ''
})

const filteredList = computed(() => {
	if (currentTab.value === 'all') return deliveries.value
	return deliveries.value.filter(d => d.status === currentTab.value)
})

/** 点击评分详情弹窗 */
const showScoreDetail = (item) => {
	uni.showModal({
		title: '简历评分 ' + item.score + '分',
		content: item.scoreDetail || '暂无详细评分数据',
		showCancel: false
	})
}

const getStudentId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		const sid = obj.id || obj.userId
		return sid ? Number(sid) : null
	} catch (e) { return null }
}

/** 获取评分颜色类名 */
const getScoreClass = (score) => {
	if (score >= 80) return 'score-green'
	if (score >= 60) return 'score-blue'
	if (score >= 40) return 'score-yellow'
	return 'score-red'
}

/** 批量加载简历评分 */
const loadScores = async (deliveries) => {
	if (!deliveries || deliveries.length === 0) return
	const results = await Promise.allSettled(
		deliveries.map(d => scoreAPI.getByDelivery(d.id).catch(() => null))
	)
	results.forEach((r, i) => {
		if (r.status === 'fulfilled' && r.value && r.value.data) {
			const s = r.value.data
			deliveries[i].score = s.score
			deliveries[i].scoreDetail = s.scoreDetail || ''
			deliveries[i].scoreClass = getScoreClass(s.score)
		}
	})
}

onMounted(loadData)

const cancelDelivery = async (id) => {
	try {
		await deliveryAPI.cancelDelivery(id)
		uni.showToast({ title: '已取消投递', icon: 'success' })
		deliveries.value = deliveries.value.filter(d => d.id !== id)
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'none' })
	}
}
</script>

<style scoped>
.filter-tabs {
	flex-direction: row;
	gap: 8px;
	padding: 12px 16px;
	background: white;
	overflow-x: auto;
	flex-shrink: 0;
}
.filter-tab {
	padding: 8px 16px;
	border-radius: 20px;
	font-size: 13px;
	font-weight: 500;
	color: #4E5969;
	background: #F2F3F5;
	white-space: nowrap;
}
.filter-tab.active {
	background: #165DFF;
	color: white;
}
.timeline {
	padding: 16px;
}
.timeline-item {
	flex-direction: row;
	gap: 12px;
	padding-bottom: 16px;
	position: relative;
}
.timeline-item::before {
	content: '';
	position: absolute;
	left: 15px;
	top: 32px;
	bottom: 0;
	width: 2px;
	background: #E2E8F0;
}
.timeline-item:last-child::before { display: none; }
.timeline-dot {
	width: 32px;
	height: 32px;
	border-radius: 50%;
	background: #165DFF;
	align-items: center;
	justify-content: center;
	color: white;
	font-size: 14px;
	flex-shrink: 0;
	z-index: 1;
}
.timeline-dot.green { background: #10B981; }
.timeline-dot.orange { background: #F59E0B; }
.timeline-dot.gray { background: #C9CDD4; }
.timeline-content {
	flex: 1;
	background: white;
	border-radius: 12px;
	padding: 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.timeline-title {
	font-size: 15px;
	font-weight: 600;
	color: #1D2129;
}
.timeline-desc {
	font-size: 13px;
	color: #86909C;
	display: block;
	margin-bottom: 8px;
}
.timeline-meta {
	flex-direction: row;
	align-items: center;
	gap: 8px;
}
.card-tag {
	padding: 4px 10px;
	border-radius: 6px;
	font-size: 12px;
	font-weight: 600;
}
.tag-blue { background: rgba(22,93,255,0.1); color: #165DFF; }
.timeline-header {
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 4px;
}
.timeline-title {
	flex: 1;
	font-size: 15px;
	font-weight: 600;
	color: #1D2129;
	margin-bottom: 0;
}
.score-badge {
	padding: 2px 10px;
	border-radius: 12px;
	font-size: 12px;
	font-weight: 700;
	white-space: nowrap;
	margin-left: 8px;
	flex-shrink: 0;
}
.score-green { background: #ECFDF5; color: #059669; }
.score-blue { background: #EFF6FF; color: #2563EB; }
.score-yellow { background: #FFFBEB; color: #D97706; }
.score-red { background: #FEF2F2; color: #DC2626; }
.score-pending { background: #F3F4F6; color: #9CA3AF; }
.score-detail {
	margin-top: 8px;
	font-size: 12px;
	color: #165DFF;
	text-decoration: underline;
}
.timeline-time {
	font-size: 12px;
	color: #C9CDD4;
}
.timeline-actions {
	margin-top: 8px;
}
.btn-sm {
	padding: 8px 16px;
	border-radius: 8px;
	font-size: 13px;
	font-weight: 600;
	align-items: center;
	justify-content: center;
}
.btn-outline {
	background: white;
	border: 1px solid #E2E8F0;
	color: #86909C;
}
.empty-state {
	padding: 60px 20px;
	align-items: center;
	justify-content: center;
}
.empty-text {
	font-size: 14px;
	color: #86909C;
}
</style>
