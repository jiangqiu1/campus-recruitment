<template>
	<view class="page-wrapper">
		<NavBar title="投递看板" show-back />

		<!-- 岗位选择器：统一全局下拉样式 -->
		<view class="selector-bar" @click="showJobPicker = true">
			<text class="label">筛选岗位</text>
			<text class="value" :class="{ placeholder: !selectedJobId }">{{ selectedJobTitle || '全部岗位' }}</text>
			<uni-icons type="arrowdown" size="12" color="#C9CDD4" />
		</view>

		<!-- 关键指标（点击即筛选） -->
		<view class="key-stats">
			<view class="key-stat" @click="filterByStatus(null)">
				<text class="ks-num">{{ deliveries.length }}</text>
				<text class="ks-label">总投递</text>
			</view>
			<view class="ks-divider" />
			<view class="key-stat" @click="filterByStatus('pending')">
				<text class="ks-num" :class="{ 'ks-warn': pendingCount > 0 }">{{ pendingCount }}</text>
				<text class="ks-label">待查看</text>
			</view>
			<view class="ks-divider" />
			<view class="key-stat" @click="filterByStatus('accepted')">
				<text class="ks-num" :class="{ 'ks-ok': acceptedCount > 0 }">{{ acceptedCount }}</text>
				<text class="ks-label">已录用</text>
			</view>
		</view>

		<!-- 状态分布卡片 -->
		<view class="chart-card">
			<text class="section-title">就业状态分布</text>
			<view class="bar-list">
				<view v-for="item in statusDistribution" :key="item.status" class="bar-item" :class="{ 'bar-selected': selectedStatus === item.status }" @click="filterByStatus(item.status)">
					<view class="bar-label">
						<text>{{ item.label }}</text>
						<text class="bar-count">{{ item.percent.toFixed(0) }}% · {{ item.count }}人</text>
					</view>
					<view class="bar-track" :class="{ 'bar-track-selected': selectedStatus === item.status }">
						<view class="bar-fill" :style="{ width: item.percent + '%', background: item.color }"></view>
					</view>
				</view>
			</view>
		</view>

		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<view class="delivery-list">
				<text class="section-title">投递明细</text>
				<view v-for="d in filteredDeliveries" :key="d.id" class="delivery-card" :class="{ 'is-pending': d.status === 'pending' }">
					<view class="delivery-top">
						<view class="delivery-avatar">
							<text>{{ (d.studentName || '学').charAt(0) }}</text>
						</view>
						<view class="delivery-info">
							<text class="delivery-name">{{ d.studentName }}</text>
							<text class="delivery-class">{{ d.className || d.jobTitle }}</text>
						</view>
						<text class="status-tag" :class="'status-' + d.status">{{ d.statusText }}</text>
					</view>
					<view class="delivery-bottom">
						<text class="delivery-time"><uni-icons type="calendar" size="12" color="#C9CDD4" /> {{ d.createTime }}</text>
						<text class="delivery-link" @click.stop="goToStudentResume(d)">查看简历 ›</text>
					</view>
				</view>
				<EmptyState v-if="!loading && !filteredDeliveries.length" icon="inbox" title="暂无投递记录" />
			</view>
			<view style="height: calc(60px + env(safe-area-inset-bottom))" />
		</scroll-view>

		<PopupDrawer :show="showJobPicker" title="选择岗位" @update:show="showJobPicker = $event">
			<view class="modal-item" :class="{ selected: !selectedJobId }" @click="selectJob(null, '全部岗位')">
				<text class="modal-item-text">全部岗位</text>
				<uni-icons v-if="!selectedJobId" type="checkmark-filled" size="18" color="#165DFF" />
			</view>
			<view v-for="job in jobOptions" :key="job.id" class="modal-item" :class="{ selected: selectedJobId === job.id }" @click="selectJob(job.id, job.title)">
				<text class="modal-item-text">{{ job.title }}</text>
				<uni-icons v-if="selectedJobId === job.id" type="checkmark-filled" size="18" color="#165DFF" />
			</view>
		</PopupDrawer>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@/utils/page-lifecycle'
import { checkRole } from '@/utils/auth'
import { teacherAPI, mapDeliveryItem } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import PopupDrawer from '@/components/PopupDrawer.vue'

checkRole(1)


const showJobPicker = ref(false)
const selectedJobId = ref(null)
const selectedJobTitle = ref('')
const deliveries = ref([])
const jobOptions = ref([])
const loading = ref(true)
const refreshing = ref(false)
const todayFilter = ref(false)
const selectedStatus = ref(null)

const statusDistribution = computed(() => {
	const list = deliveries.value
	const total = list.length || 1
	const statusList = [
		{ status: 'pending', label: '待查看', color: '#FF7D00' },
		{ status: 'viewed', label: '已查看', color: '#165DFF' },
		{ status: 'interview', label: '面试中', color: '#165DFF' },
		{ status: 'accepted', label: '已录用', color: '#00B42A' },
		{ status: 'rejected', label: '未通过', color: '#F53F3F' }
	]
	return statusList.map(item => {
		const count = list.filter(d => d.status === item.status).length
		return { ...item, count, percent: (count / total) * 100 }
	})
})

const filteredDeliveries = computed(() => {
	let list = deliveries.value
	if (selectedStatus.value) {
		list = list.filter(d => d.status === selectedStatus.value)
	}
	if (todayFilter.value) {
		const today = new Date()
		const todayStr = today.getFullYear() + '-' + String(today.getMonth() + 1).padStart(2, '0') + '-' + String(today.getDate()).padStart(2, '0')
		list = list.filter(d => d.createTime && d.createTime.startsWith(todayStr))
	}
	return list
})

const getTeacherId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.id || obj.userId
	} catch (e) { return null }
}

// 初始化：URL参数解析 + 筛选记忆恢复（onShow 之前执行一次）
{
	const pages = getCurrentPages()
	const currentPage = pages[pages.length - 1]
	if (currentPage.options?.jobId) {
		selectedJobId.value = Number(currentPage.options.jobId)
		selectedJobTitle.value = decodeURIComponent(currentPage.options.jobTitle || '')
	}
	if (currentPage.options?.filter === 'today') {
		todayFilter.value = true
	}
	const savedStatus = uni.getStorageSync('teacher_deliveries_status')
	if (savedStatus) selectedStatus.value = savedStatus
}

// 页面显示时刷新数据（含首次加载 + 返回刷新）
onShow(async () => {
	await loadJobs()
	await loadDeliveries()
	loading.value = false
	const savedJob = uni.getStorageSync('teacher_deliveries_job')
	if (savedJob && !selectedJobId.value) {
		selectedJobId.value = Number(savedJob)
		const job = jobOptions.value.find(j => j.id === selectedJobId.value)
		if (job) selectedJobTitle.value = job.title
	}
})

const onRefresh = async () => {
	refreshing.value = true
	await loadDeliveries()
	refreshing.value = false
}

const loadJobs = async () => {
	try {
		const tid = getTeacherId()
		const res = await teacherAPI.getTeacherJobs(tid)
		jobOptions.value = res.data || []
	} catch (e) {
		console.error('加载岗位列表失败', e)
		uni.showToast({ title: '加载岗位失败', icon: 'none' })
	}
}

const loadDeliveries = async () => {
	try {
		let rawList = []
		if (selectedJobId.value) {
			const res = await teacherAPI.getAllDeliveries(selectedJobId.value)
			rawList = res.data || []
		} else {
			const jobs = jobOptions.value
			const requests = jobs.map(job => teacherAPI.getAllDeliveries(job.id).catch(() => ({ data: [] })))
			const results = await Promise.all(requests)
			rawList = results.flatMap(res => res.data || [])
		}
		deliveries.value = rawList.map(mapDeliveryItem)
	} catch (e) {
		console.error('加载投递记录失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

const selectJob = (id, title) => {
	selectedJobId.value = id
	selectedJobTitle.value = title
	showJobPicker.value = false
	uni.setStorageSync('teacher_deliveries_job', id)
	loading.value = true
	loadDeliveries().finally(() => loading.value = false)
}

// 关键指标
const pendingCount = computed(() => deliveries.value.filter(d => d.status === 'pending').length)
const acceptedCount = computed(() => deliveries.value.filter(d => d.status === 'accepted').length)

const filterByStatus = (status) => {
	if (selectedStatus.value === status) {
		selectedStatus.value = null
	} else {
		selectedStatus.value = status
	}
	uni.setStorageSync('teacher_deliveries_status', selectedStatus.value || '')
}

const goToStudentResume = (d) => {
	if (!d.studentId) return
	uni.navigateTo({
		url: '/pages/teacher/student-resume?studentId=' + d.studentId + '&name=' + encodeURIComponent(d.studentName) + '&deliveryId=' + d.id
	})
}
</script>

<style scoped lang="scss">
/* 岗位选择器 */
.selector-bar {
	flex-direction: row;
	align-items: center;
	padding: 12px 16px;
	background: white;
	gap: 8px;
}
.selector-bar .label { font-size: 14px; color: $uni-text-color-secondary; }
.selector-bar .value { flex: 1; font-size: 14px; color: $uni-text-color-title; font-weight: 600; }
.selector-bar .value.placeholder { color: $uni-text-color-placeholder; font-weight: 400; }

/* 状态分布卡片 */
.chart-card {
	background: white;
	margin: 0 16px 12px;
	border-radius: 12px;
	padding: 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.section-title {
	font-size: 15px;
	font-weight: 700;
	color: $uni-text-color-title;
	margin-bottom: 16px;
	display: block;
}
.bar-list { gap: 10px; }
.bar-item { gap: 6px; }
.bar-label { flex-direction: row; justify-content: space-between; font-size: 13px; color: $uni-text-color; }
.bar-label .count { font-size: 13px; color: $uni-text-color-secondary; }
.bar-track { height: 8px; background: $uni-border-color-divider; border-radius: 4px; overflow: hidden; }
.bar-track-selected { background: $uni-color-primary-light; border: 1px solid $uni-color-primary; }
.bar-fill { height: 100%; border-radius: 4px; transition: width 0.3s; }
.bar-count { font-size: 13px; color: $uni-color-primary; font-weight: 600; }

/* 投递卡片 */
.delivery-list { padding: 0 16px; }
.delivery-card {
	background: white;
	border-radius: 12px;
	padding: 16px;
	margin-bottom: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
/* 待查看 = 教师的待办，左侧橙色标识条 */
.delivery-card.is-pending {
	border-left: 3px solid $uni-color-warning;
}

/* 关键指标行 */
.key-stats {
	flex-direction: row;
	align-items: center;
	background: white;
	border-radius: 12px;
	margin: 12px 16px 0;
	padding: 12px 0;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.key-stat {
	flex: 1;
	align-items: center;
	gap: 2px;
}
.key-stat:active { opacity: 0.7; }
.ks-num {
	font-size: 20px;
	font-weight: 800;
	color: $uni-text-color-title;
}
.ks-num.ks-warn { color: $uni-color-warning; }
.ks-num.ks-ok { color: $uni-color-success; }
.ks-label {
	font-size: 12px;
	color: $uni-text-color-secondary;
}
.ks-divider {
	width: 0.5px;
	height: 26px;
	background: $uni-border-color-divider;
}
.delivery-top {
	flex-direction: row;
	align-items: center;
	gap: 12px;
	margin-bottom: 8px;
}
.delivery-avatar {
	width: 40px;
	height: 40px;
	border-radius: 50%;
	background: $uni-gradient-primary;
	align-items: center;
	justify-content: center;
	font-size: 16px;
	color: white;
	font-weight: 700;
	flex-shrink: 0;
}
.delivery-info { flex: 1; }
.delivery-name { font-size: 15px; font-weight: 700; color: $uni-text-color-title; display: block; margin-bottom: 2px; }
.delivery-class { font-size: 12px; color: $uni-text-color-secondary; display: block; }
.status-tag { font-size: 12px; padding: 3px 10px; border-radius: 8px; font-weight: 600; flex-shrink: 0; }
.status-pending { background: rgba(245,158,11,0.1); color: $uni-color-warning; }
.status-viewed { background: $uni-color-primary-light; color: $uni-color-primary; }
.status-interview { background: $uni-color-primary-light; color: $uni-color-primary; }
.status-accepted { background: $uni-color-success-light; color: $uni-color-success; }
.status-rejected { background: rgba(239,68,68,0.1); color: $uni-color-error; }

.delivery-bottom { flex-direction: row; justify-content: space-between; align-items: center; }
.delivery-time { font-size: 12px; color: $uni-text-color-placeholder; flex-direction: row; align-items: center; gap: 4px; }
.delivery-link { font-size: 13px; color: $uni-color-primary; font-weight: 600; }

.modal-item {
	flex-direction: row;
	align-items: center;
	padding: 14px 16px;
}
.modal-item-text { flex: 1; font-size: 15px; color: $uni-text-color-title; }
.modal-item.selected { background: $uni-color-primary-light; }
.delivery-card:active { background: $uni-bg-color-page; }
</style>
