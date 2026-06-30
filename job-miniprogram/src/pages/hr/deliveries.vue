<template>
	<view class="page-wrapper">
		<view class="header-simple" style="padding:12px 16px;flex-direction:row;align-items:center;gap:12px;">
			<text style="font-size:18px;font-weight:700;color:white;">投递管理</text>
		</view>

		<!-- 顶部岗位选择下拉 -->
		<view class="job-select">
			<picker class="select-picker" :value="selectedJobIndex" :range="jobOptions" @change="onJobChange">
				<text class="select-text">{{ jobOptions[selectedJobIndex] }}</text>
				<text class="select-arrow">▼</text>
			</picker>
		</view>

		<scroll-view class="content-scrollable" scroll-y>
			<view class="candidate-list">
				<view v-for="(candidate, i) in candidates" :key="i" class="card-item" @click="goToDetail(candidate.id)">
					<view class="card-header-row">
						<view class="candidate-info">
							<view class="candidate-avatar">
								<text>{{ (candidate.studentName || '?').charAt(0) }}</text>
							</view>
							<view>
								<text class="card-title">{{ candidate.studentName || '候选人' }}</text>
								<text class="card-sub">{{ candidate.jobTitle || '岗位名称' }}</text>
							</view>
							<text v-if="candidate.score != null" class="score-badge" :class="scoreBadgeClass(candidate.score)">{{ candidate.score }}分</text>
						</view>
						<text class="status-tag" :class="'tag-' + candidate.status">{{ candidate.statusText || '' }}</text>
					</view>
					<view class="card-footer">
						<text class="card-time">{{ candidate.createTime || '' }}</text>
						<view class="card-actions">
							<button class="action-btn btn-ai" @click.stop="showScore(candidate)">🤖 AI评分</button>
							<button class="action-btn btn-blue" @click.stop="handleStatus(candidate, 'interview')">面试</button>
							<button class="action-btn btn-green" @click.stop="handleStatus(candidate, 'accepted')">录用</button>
							<button class="action-btn btn-gray" @click.stop="handleStatus(candidate, 'rejected')">拒绝</button>
						</view>
					</view>
				</view>
				<view v-if="!candidates.length" class="empty-state">
					<text style="font-size:48px;margin-bottom:12px;">📭</text>
					<text>暂无投递记录</text>
				</view>
			</view>
		</scroll-view>

		<HrTabBar current="deliveries" />

		<!-- AI评分详情弹窗 -->
		<view class="modal-overlay" v-if="showScoreModal" @click="closeScoreModal">
			<view class="modal-content" @click.stop>
				<view class="modal-header">
					<text class="modal-title">AI 简历评分</text>
					<text class="modal-close" @click="closeScoreModal">✕</text>
				</view>
				<view class="modal-body" v-if="scoreDetail">
					<view class="score-big-ring" :class="scoreRingClass(scoreTotal)">
						<text class="score-big-num">{{ scoreTotal }}</text>
						<text class="score-big-label">分</text>
					</view>
					<view class="score-dim-list">
						<view v-for="(dim, i) in scoreDims" :key="i" class="dim-row">
							<text class="dim-title">{{ dim.label }}</text>
							<view class="dim-track-wrap">
								<view class="dim-track-bg">
									<view class="dim-fill" :style="{ width: dim.percent + '%', background: dim.color }"></view>
								</view>
							</view>
							<text class="dim-val" :style="{ color: dim.color }">{{ dim.score }}</text>
						</view>
					</view>
					<view class="score-comment" v-if="scoreDetail.comment">
						<text class="comment-text">{{ scoreDetail.comment }}</text>
					</view>
				</view>
				<view class="modal-body" v-else style="padding:40px 20px;align-items:center;">
					<text style="font-size:40px;margin-bottom:12px;">🤖</text>
					<text style="color:#86909C;font-size:14px;">正在生成评分...</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { hrAPI, scoreAPI } from '@/utils/request'
import HrTabBar from '@/components/HrTabBar.vue'

const jobs = ref([])
const candidates = ref([])
const selectedJobIndex = ref(0)

const jobOptions = computed(() => {
	const opts = ['全部岗位']
	jobs.value.forEach(j => {
		opts.push(j.title || '岗位 #' + j.id)
	})
	return opts
})

// 后端 Delivery.status Integer(0-4) → 前端 string
const DELIVERY_STATUS = ['pending', 'viewed', 'interview', 'accepted', 'rejected']
const DELIVERY_STATUS_TEXT = ['待查看', '已查看', '面试中', '已录用', '未通过']

const mapDelivery = (d) => ({
	id: d.id,
	studentId: d.studentId,
	studentName: d.studentName || '候选人',
	jobTitle: d.jobTitle || '岗位名称',
	jobId: d.jobId,
	status: DELIVERY_STATUS[d.status] || 'pending',
	statusText: DELIVERY_STATUS_TEXT[d.status] || '待查看',
	createTime: d.createTime ? d.createTime.substring(0, 16).replace('T', ' ') : ''
})

const getCompanyId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.companyId || obj.id || null
	} catch (e) { return null }
}

onMounted(async () => {
	const cId = getCompanyId()
	await loadJobs(cId)
	await loadAllCandidates()
})

const onJobChange = (e) => {
	selectedJobIndex.value = e.detail.value
	const title = jobOptions.value[selectedJobIndex.value]
	if (title === '全部岗位') {
		loadAllCandidates()
	} else {
		const job = jobs.value.find(j => j.title === title)
		if (job) {
			loadCandidatesByJob(job.id)
		}
	}
}

const loadJobs = async (cId) => {
	try {
		const res = await hrAPI.getHrJobs(cId)
		jobs.value = res.data || []
	} catch (e) {
		console.error('加载岗位列表失败', e)
	}
}

const loadAllCandidates = async () => {
	try {
		const all = []
		for (const job of jobs.value) {
			try {
				const res = await hrAPI.getCompanyDeliveries(job.id)
				all.push(...(res.data || []))
			} catch (e) { /* skip job */ }
		}
		candidates.value = all.map(mapDelivery)
	} catch (e) {
		console.error('加载投递列表失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

const loadCandidatesByJob = async (jobId) => {
	try {
		const res = await hrAPI.getCompanyDeliveries(jobId)
		candidates.value = (res.data || []).map(mapDelivery)
	} catch (e) {
		console.error('加载岗位投递列表失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

const handleStatus = async (candidate, status) => {
	// 字符串 → 后端 Integer 映射
	const statusToInt = { pending: 0, viewed: 1, interview: 2, accepted: 3, rejected: 4 }
	const statusInt = statusToInt[status]
	if (statusInt === undefined) return

	try {
		await hrAPI.updateDeliveryStatus(candidate.id, { status: statusInt })
		candidate.status = status
		candidate.statusText = DELIVERY_STATUS_TEXT[statusInt]
		uni.showToast({ title: '状态已更新', icon: 'success' })
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'none' })
	}
}

const goToDetail = (id) => {
	uni.navigateTo({ url: '/pages/hr/delivery-detail?id=' + id })
}

// AI 简历评分
const showScoreModal = ref(false)
const scoreDetail = ref(null)
const scoreTotal = ref(0)
const scoreDims = ref([])

const scoreBadgeClass = (score) => {
	if (score >= 80) return 'score-green'
	if (score >= 60) return 'score-blue'
	if (score >= 40) return 'score-yellow'
	return 'score-red'
}

const scoreRingClass = (score) => {
	if (score >= 80) return 'ring-green'
	if (score >= 60) return 'ring-blue'
	if (score >= 40) return 'ring-yellow'
	return 'ring-red'
}

const showScore = async (candidate) => {
	showScoreModal.value = true
	scoreDetail.value = null
	scoreTotal.value = 0
	scoreDims.value = []
	try {
		const res = await hrAPI.getDeliveryScore?.(candidate.id) ?? await scoreAPI.getByStudentAndJob?.(candidate.studentId, candidate.jobId)
		const data = res.data || res
		scoreDetail.value = data
		scoreTotal.value = data.totalScore || data.score || 0
		scoreDims.value = [
			{ label: '技能匹配', score: data.skillScore || 0, percent: (data.skillScore || 0) * 100 / 100, color: scoreTotal.value >= 80 ? '#10B981' : scoreTotal.value >= 60 ? '#165DFF' : '#F59E0B' },
			{ label: '经验匹配', score: data.expScore || 0, percent: (data.expScore || 0) * 100 / 100, color: (data.expScore || 0) >= 80 ? '#10B981' : (data.expScore || 0) >= 60 ? '#165DFF' : '#F59E0B' },
			{ label: '学历匹配', score: data.eduScore || 0, percent: (data.eduScore || 0) * 100 / 100, color: (data.eduScore || 0) >= 80 ? '#10B981' : (data.eduScore || 0) >= 60 ? '#165DFF' : '#F59E0B' }
		]
	} catch (e) {
		console.error('评分加载失败', e)
		scoreDetail.value = { comment: '评分加载失败，请稍后重试' }
		scoreTotal.value = 0
		scoreDims.value = []
	}
}

const closeScoreModal = () => {
	showScoreModal.value = false
}
</script>

<style scoped>
.job-select {
	padding: 12px 16px;
	background: white;
	flex-shrink: 0;
}
.select-picker {
	width: 100%;
	height: 44px;
	border: 2px solid #E2E8F0;
	border-radius: 12px;
	padding: 0 16px;
	font-size: 14px;
	background: #F8F9FC;
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
}
.select-text {
	color: #1D2129;
	font-weight: 500;
}
.select-arrow {
	color: #86909C;
	font-size: 12px;
}
.candidate-list {
	padding: 16px;
}
.card-item {
	background: white;
	border-radius: 16px;
	padding: 16px;
	margin-bottom: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
	position: relative;
	overflow: hidden;
}
.card-item::before {
	content: '';
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 3px;
	background: linear-gradient(90deg, #0EA5E9, transparent);
}
.card-header-row {
	flex-direction: row;
	justify-content: space-between;
	align-items: flex-start;
	margin-bottom: 8px;
}
.candidate-info {
	flex-direction: row;
	align-items: center;
	gap: 12px;
}
.candidate-avatar {
	width: 40px;
	height: 40px;
	border-radius: 50%;
	background: linear-gradient(135deg, #0EA5E9, #38BDF8);
	align-items: center;
	justify-content: center;
	color: white;
	font-size: 16px;
	font-weight: 600;
}
.card-title {
	font-size: 15px;
	font-weight: 600;
	color: #1D2129;
	display: block;
	margin-bottom: 2px;
}
.card-sub {
	font-size: 13px;
	color: #86909C;
	display: block;
}
.status-tag {
	padding: 4px 10px;
	border-radius: 6px;
	font-size: 12px;
	font-weight: 600;
}
.tag-pending { background: rgba(245,158,11,0.1); color: #F59E0B; }
.tag-viewed { background: rgba(22,93,255,0.1); color: #165DFF; }
.tag-interview { background: rgba(14,165,233,0.1); color: #0EA5E9; }
.tag-accepted { background: rgba(16,185,129,0.1); color: #10B981; }
.tag-rejected { background: rgba(239,68,68,0.1); color: #EF4444; }
.card-footer {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
}
.card-time {
	font-size: 12px;
	color: #C9CDD4;
}
.card-actions {
	flex-direction: row;
	gap: 6px;
}
.action-btn {
	padding: 6px 14px;
	border-radius: 8px;
	font-size: 12px;
	font-weight: 600;
	align-items: center;
	justify-content: center;
}
.btn-blue { background: rgba(14,165,233,0.1); color: #0EA5E9; border: none; }
.btn-green { background: rgba(16,185,129,0.1); color: #10B981; border: none; }
.btn-gray { background: #F2F3F5; color: #86909C; border: none; }
.btn-ai {
	background: linear-gradient(135deg, rgba(99,102,241,0.1), rgba(139,92,246,0.1));
	color: #7C3AED;
	border: none;
	font-size: 11px;
	padding: 4px 10px;
}

/* 评分徽章 */
.score-badge {
	font-size: 11px;
	font-weight: 700;
	padding: 2px 8px;
	border-radius: 10px;
	margin-left: auto;
}
.score-green { background: #ECFDF5; color: #10B981; }
.score-blue { background: #EFF6FF; color: #165DFF; }
.score-yellow { background: #FFFBEB; color: #F59E0B; }
.score-red { background: #FEF2F2; color: #EF4444; }

/* 评分弹窗 */
.modal-overlay {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(0,0,0,0.5);
	z-index: 999;
	align-items: center;
	justify-content: center;
	padding: 40px 20px;
}
.modal-content {
	width: 100%;
	max-width: 340px;
	background: #fff;
	border-radius: 20px;
	overflow: hidden;
}
.modal-header {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	padding: 20px 20px 0;
}
.modal-title { font-size: 18px; font-weight: 700; color: #1D2129; }
.modal-close { font-size: 20px; color: #86909C; padding: 4px; }
.modal-body { padding: 16px 20px 20px; }

.score-big-ring {
	width: 100px;
	height: 100px;
	border-radius: 50%;
	border: 5px solid #10B981;
	align-items: center;
	justify-content: center;
	align-self: center;
	margin-bottom: 16px;
}
.score-big-ring.ring-green { border-color: #10B981; }
.score-big-ring.ring-blue { border-color: #165DFF; }
.score-big-ring.ring-yellow { border-color: #F59E0B; }
.score-big-ring.ring-red { border-color: #EF4444; }
.score-big-num { font-size: 32px; font-weight: 800; color: #1D2129; }
.score-big-label { font-size: 14px; color: #86909C; }

.score-dim-list { gap: 12px; margin-bottom: 16px; }
.dim-row { flex-direction: row; align-items: center; gap: 8px; }
.dim-title { width: 56px; font-size: 13px; color: #4E5969; }
.dim-track-wrap { flex: 1; }
.dim-track-bg {
	height: 10px;
	background: #F2F3F5;
	border-radius: 5px;
	overflow: hidden;
}
.dim-fill { height: 100%; border-radius: 5px; transition: width 0.5s; }
.dim-val { width: 30px; font-size: 13px; font-weight: 700; text-align: right; }

.score-comment {
	padding: 12px 16px;
	background: #F8F9FC;
	border-radius: 10px;
}
.comment-text { font-size: 13px; color: #4E5969; line-height: 1.6; }

.empty-state {
	padding: 60px 20px;
	align-items: center;
	color: #86909C;
	font-size: 14px;
}
</style>
