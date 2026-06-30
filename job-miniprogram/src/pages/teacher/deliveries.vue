<template>
	<view class="page-wrapper">
		<view class="header-simple" style="padding:12px 16px;flex-direction:row;align-items:center;gap:12px;">
			<text style="font-size:20px;" @click="goBack">‹</text>
			<text style="font-size:18px;font-weight:700;color:white;flex:1;">投递看板</text>
			<text class="export-btn" @click="exportCSV">📥 导出</text>
		</view>

		<!-- 岗位筛选 -->
		<view class="job-selector" @click="showJobPicker = true">
			<text class="selector-label">岗位：</text>
			<text class="selector-value" :class="{ placeholder: !selectedJobId }">{{ selectedJobTitle || '全部岗位' }}</text>
			<text class="selector-arrow">▼</text>
		</view>

		<!-- 状态分布可视化 -->
		<view class="chart-section">
			<text class="section-title">📊 就业状态分布</text>
			<view class="bar-chart">
				<view v-for="(item, i) in statusDistribution" :key="i" class="bar-item">
					<view class="bar-label">
						<text>{{ item.label }}</text>
						<text class="bar-count">{{ item.count }}</text>
					</view>
					<view class="bar-track">
						<view class="bar-fill" :style="{ width: item.percent + '%', background: item.color }"></view>
					</view>
				</view>
			</view>
		</view>

		<scroll-view class="content-scrollable" scroll-y>
			<!-- 投递列表 -->
			<view class="delivery-list">
				<text class="section-title" style="padding:0 16px 12px;font-size:16px;">📋 投递明细</text>
				<view v-for="(d, i) in deliveries" :key="i" class="delivery-card">
					<view class="delivery-top">
						<view class="delivery-avatar">
							<text>{{ (d.studentName || '学').charAt(0) }}</text>
						</view>
						<view class="delivery-info">
							<text class="delivery-name">{{ d.studentName }}</text>
							<text class="delivery-class">{{ d.className || d.jobTitle }}</text>
						</view>
						<text class="delivery-status" :class="'status-' + d.status">{{ d.statusText }}</text>
					</view>
					<view class="delivery-bottom">
						<text class="delivery-time">📅 {{ d.createTime }}</text>
						<text class="delivery-view" @click.stop="goToStudentResume(d)">查看简历 ›</text>
					</view>
				</view>
				<view v-if="!deliveries.length" class="empty-state">
					<text style="font-size:48px;margin-bottom:12px;">📭</text>
					<text class="empty-text">暂无投递记录</text>
				</view>
			</view>
			<view style="height:20px;"></view>
		</scroll-view>
	</view>

	<!-- 岗位选择弹窗 -->
	<view v-if="showJobPicker" class="modal-overlay" @click="showJobPicker = false">
		<view class="modal-content" @click.stop>
			<view class="modal-header">
				<text class="modal-title">选择岗位</text>
				<text class="modal-close" @click="showJobPicker = false">✕</text>
			</view>
			<scroll-view class="modal-list" scroll-y>
				<view class="modal-item" :class="{ selected: !selectedJobId }" @click="selectJob(null, '全部岗位')">
					<text class="modal-item-text">全部岗位</text>
					<text class="modal-check" v-if="!selectedJobId">✓</text>
				</view>
				<view v-for="(job, i) in jobOptions" :key="i" class="modal-item" :class="{ selected: selectedJobId === job.id }" @click="selectJob(job.id, job.title)">
					<text class="modal-item-text">{{ job.title }}</text>
					<text class="modal-check" v-if="selectedJobId === job.id">✓</text>
				</view>
			</scroll-view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { teacherAPI } from '@/utils/request'

const showJobPicker = ref(false)
const selectedJobId = ref(null)
const selectedJobTitle = ref('')
const deliveries = ref([])
const jobOptions = ref([])

// 状态映射（后端Integer → 前端展示）
const STATUS_STR = ['pending', 'viewed', 'interview', 'accepted', 'rejected']
const STATUS_TXT = ['待查看', '已查看', '面试', '已录用', '不合适']
const STATUS_COLORS = { pending: '#F59E0B', viewed: '#165DFF', interview: '#10B981', accepted: '#8B5CF6', rejected: '#EF4444' }

// 后端 DeliveryVO → 前端展示映射
const mapDelivery = (d) => ({
	id: d.id,
	studentId: d.studentId,
	studentName: d.studentName || '未知',
	className: d.className || d.jobTitle || '',
	jobTitle: d.jobTitle || '',
	jobId: d.jobId,
	status: STATUS_STR[d.status] || 'pending',
	statusText: STATUS_TXT[d.status] || '待查看',
	createTime: d.createTime ? d.createTime.substring(0, 10) : ''
})

const statusDistribution = computed(() => {
	const all = currentDeliveries.value
	const total = all.length || 1
	const countByStatus = (s) => all.filter(d => d.status === s).length
	return [
		{ label: '待查看', count: countByStatus('pending'), percent: (countByStatus('pending') / total) * 100, color: '#F59E0B' },
		{ label: '已查看', count: countByStatus('viewed'), percent: (countByStatus('viewed') / total) * 100, color: '#165DFF' },
		{ label: '面试中', count: countByStatus('interview'), percent: (countByStatus('interview') / total) * 100, color: '#10B981' },
		{ label: '已录用', count: countByStatus('accepted'), percent: (countByStatus('accepted') / total) * 100, color: '#8B5CF6' },
		{ label: '未通过', count: countByStatus('rejected'), percent: (countByStatus('rejected') / total) * 100, color: '#EF4444' },
	]
})

const currentDeliveries = computed(() => {
	if (!selectedJobId.value) return deliveries.value
	return deliveries.value.filter(d => d.jobId === selectedJobId.value)
})

const getTeacherId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.id || obj.userId
	} catch (e) { return null }
}

onMounted(async () => {
	const pages = getCurrentPages()
	const currentPage = pages[pages.length - 1]
	if (currentPage.options) {
		if (currentPage.options.jobId) {
			selectedJobId.value = Number(currentPage.options.jobId)
			selectedJobTitle.value = decodeURIComponent(currentPage.options.jobTitle || '')
		}
	}
	await Promise.all([loadJobs(), loadDeliveries()])
})

const loadJobs = async () => {
	try {
		const tid = getTeacherId()
		const res = await teacherAPI.getTeacherJobs(tid)
		jobOptions.value = res.data || []
	} catch (e) {
		console.error('加载岗位列表失败', e)
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
			for (const job of jobs) {
				try {
					const res = await teacherAPI.getAllDeliveries(job.id)
					const list = (res.data || [])
					rawList = rawList.concat(list)
				} catch (e) { /* skip job */ }
			}
		}
		deliveries.value = rawList.map(mapDelivery)
	} catch (e) {
		console.error('加载投递记录失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

const selectJob = (id, title) => {
	selectedJobId.value = id
	selectedJobTitle.value = title
	showJobPicker.value = false
	loadDeliveries()
}

const goToStudentResume = (d) => {
	if (d.studentId) {
		uni.navigateTo({ url: '/pages/teacher/student-resume?studentId=' + d.studentId + '&name=' + encodeURIComponent(d.studentName) })
	}
}

const goBack = () => { uni.navigateBack() }

const exportCSV = () => {
	const list = currentDeliveries.value
	if (!list.length) {
		uni.showToast({ title: '暂无数据可导出', icon: 'none' })
		return
	}
	// BOM 头 for Excel 中文兼容
	const BOM = '\uFEFF'
	const headers = '姓名,班级,岗位,投递状态,投递日期\n'
	const rows = list.map(d =>
		`${d.studentName},${d.className},${d.jobTitle},${d.statusText},${d.createTime}`
	).join('\n')
	const csv = BOM + headers + rows
	uni.setClipboardData({
		data: csv,
		success: () => {
			uni.showToast({ title: `已导出 ${list.length} 条，粘贴到 Excel 即可`, icon: 'success', duration: 2500 })
		}
	})
}
</script>

<style scoped>
.header-simple {
	background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
	color: white;
}
.export-btn {
	font-size:13px;
	color:rgba(255,255,255,0.9);
	padding:6px 12px;
	border-radius:16px;
	background:rgba(255,255,255,0.2);
	font-weight:500;
}
.export-btn:active {
	background:rgba(255,255,255,0.35);
}
.job-selector {
	flex-direction: row;
	align-items: center;
	padding: 12px 16px;
	background: white;
	border-bottom: 1px solid #F2F3F5;
}
.selector-label {
	font-size: 14px;
	color: #86909C;
}
.selector-value {
	flex: 1;
	font-size: 14px;
	color: #1D2129;
	font-weight: 600;
}
.selector-value.placeholder { color: #C9CDD4; font-weight: 400; }
.selector-arrow { color: #C9CDD4; font-size: 12px; }
.chart-section {
	background: white;
	margin: 12px 16px;
	border-radius: 16px;
	padding: 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.section-title {
	font-size: 15px;
	font-weight: 700;
	color: #1D2129;
	margin-bottom: 16px;
	display: block;
}
.bar-chart { gap: 10px; }
.bar-item { gap: 6px; }
.bar-label {
	flex-direction: row;
	justify-content: space-between;
	font-size: 13px;
	color: #4E5969;
}
.bar-count {
	font-size: 13px;
	color: #86909C;
}
.bar-track {
	height: 8px;
	background: #F2F3F5;
	border-radius: 4px;
	overflow: hidden;
}
.bar-fill {
	height: 100%;
	border-radius: 4px;
	transition: width 0.3s;
}
.delivery-list {
	padding: 0 16px;
}
.delivery-card {
	background: white;
	border-radius: 16px;
	padding: 16px;
	margin-bottom: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
	position: relative;
	overflow: hidden;
}
.delivery-card::before {
	content: '';
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 3px;
	background: linear-gradient(90deg, #10B981, transparent);
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
	background: linear-gradient(135deg, #10B981, #34D399);
	align-items: center;
	justify-content: center;
	font-size: 16px;
	color: white;
	font-weight: 700;
	flex-shrink: 0;
}
.delivery-info { flex: 1; }
.delivery-name {
	font-size: 15px;
	font-weight: 700;
	color: #1D2129;
	display: block;
	margin-bottom: 2px;
}
.delivery-class {
	font-size: 12px;
	color: #86909C;
	display: block;
}
.delivery-status {
	font-size: 12px;
	padding: 3px 10px;
	border-radius: 8px;
	font-weight: 600;
	flex-shrink: 0;
}
.status-pending { background: rgba(245,158,11,0.1); color: #F59E0B; }
.status-viewed { background: rgba(22,93,255,0.1); color: #165DFF; }
.status-interview { background: rgba(16,185,129,0.1); color: #10B981; }
.status-accepted { background: rgba(139,92,246,0.1); color: #8B5CF6; }
.status-rejected { background: rgba(239,68,68,0.1); color: #EF4444; }
.delivery-bottom {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
}
.delivery-time {
	font-size: 12px;
	color: #C9CDD4;
}
.delivery-view {
	font-size: 13px;
	color: #10B981;
	font-weight: 600;
}
/* Modal */
.modal-overlay {
	position: fixed;
	top: 0; left: 0; right: 0; bottom: 0;
	background: rgba(0,0,0,0.4);
	justify-content: center;
	align-items: center;
	z-index: 999;
}
.modal-content {
	width: 85%;
	max-height: 70%;
	background: white;
	border-radius: 20px;
	overflow: hidden;
}
.modal-header {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	padding: 16px 20px;
	border-bottom: 1px solid #F2F3F5;
}
.modal-title {
	font-size: 17px;
	font-weight: 700;
	color: #1D2129;
}
.modal-close {
	font-size: 20px;
	color: #86909C;
	padding: 4px;
}
.modal-list { max-height: 400px; }
.modal-item {
	flex-direction: row;
	align-items: center;
	padding: 16px 20px;
	border-bottom: 1px solid #F2F3F5;
}
.modal-item.selected { background: rgba(16,185,129,0.05); }
.modal-item-text { flex: 1; font-size: 15px; color: #1D2129; }
.modal-check { color: #10B981; font-size: 18px; font-weight: 700; }
.empty-state {
	padding: 60px 20px;
	align-items: center;
	justify-content: center;
}
.empty-text { font-size: 14px; color: #86909C; }
</style>
