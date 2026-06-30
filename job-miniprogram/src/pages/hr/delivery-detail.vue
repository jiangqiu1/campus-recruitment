<template>
	<view class="page-wrapper">
		<view class="header-simple" style="padding:12px 16px;flex-direction:row;align-items:center;gap:12px;">
			<text style="font-size:20px;" @click="goBack">‹</text>
			<text style="font-size:18px;font-weight:700;color:white;">投递详情</text>
		</view>

		<scroll-view class="content-scrollable" scroll-y refresher-enabled refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<!-- 加载状态 -->
			<view v-if="loading" class="loading-state">
				<text class="loading-text">加载中...</text>
			</view>

			<template v-if="!loading && candidate.id">
				<!-- 候选人信息卡片 -->
				<view class="section-card">
					<view class="candidate-header">
						<view class="candidate-avatar">
							<text>{{ avatarChar }}</text>
						</view>
						<view class="candidate-meta">
							<text class="candidate-name">{{ candidate.studentName || '候选人' }}</text>
							<text class="candidate-pos">{{ candidate.jobTitle || '岗位名称' }}</text>
							<view class="candidate-tags">
								<text class="time-tag">{{ candidate.createTime || '--' }}</text>
							</view>
						</view>
						<view class="status-badge" :class="'sb-' + candidate.status">
							<text>{{ candidate.statusText }}</text>
						</view>
					</view>
				</view>

				<!-- AI 智能评分 -->
				<view v-if="aiScore !== null" class="section-card score-card">
					<view class="section-title-row">
						<text class="section-title">🤖 AI 智能评分</text>
						<text class="section-action" @click="refreshScore">刷新评分</text>
					</view>
					<view class="score-row">
						<view class="score-circle">
							<text class="score-num">{{ aiScore }}</text>
							<text class="score-label">分</text>
						</view>
						<view class="score-detail">
							<text class="score-level">{{ scoreLevel }}</text>
							<text class="score-desc">岗位匹配度评估</text>
						</view>
					</view>
					<view class="score-bar">
						<view class="score-bar-fill" :style="{ width: aiScore + '%' }"></view>
					</view>
				</view>
				<view v-else class="section-card score-card score-placeholder" @click="triggerScore">
					<view class="section-title-row">
						<text class="section-title">🤖 AI 智能评分</text>
						<text class="section-action">点击评分 ›</text>
					</view>
					<text class="score-hint">对该候选人进行岗位匹配度评估</text>
				</view>

				<!-- 简历信息 -->
				<view class="section-card">
					<view class="section-title-row">
						<text class="section-title">📄 简历信息</text>
						<text v-if="resume.name" class="section-action" @click="viewFullResume">查看简历 ›</text>
					</view>
					<view v-if="resume.name" class="info-grid">
						<view class="info-cell">
							<text class="info-label">姓名</text>
							<text class="info-value">{{ resume.name }}</text>
						</view>
						<view class="info-cell">
							<text class="info-label">手机</text>
							<text class="info-value">{{ resume.phone || '--' }}</text>
						</view>
						<view class="info-cell">
							<text class="info-label">学校</text>
							<text class="info-value">{{ resume.school || '--' }}</text>
						</view>
						<view class="info-cell">
							<text class="info-label">专业</text>
							<text class="info-value">{{ resume.major || '--' }}</text>
						</view>
						<view class="info-cell">
							<text class="info-label">学历</text>
							<text class="info-value">{{ resume.education || '--' }}</text>
						</view>
						<view class="info-cell">
							<text class="info-label">邮箱</text>
							<text class="info-value">{{ resume.email || '--' }}</text>
						</view>
					</view>
					<view v-else class="empty-resume">
						<text>暂未获取到简历数据</text>
					</view>
				</view>

				<!-- 操作区 -->
				<view class="section-card">
					<view class="section-title-row">
						<text class="section-title">📋 操作</text>
					</view>
					<view class="action-group">
						<view v-if="candidate.status === 'pending'" class="action-row-full">
							<button class="status-btn btn-primary" @click="markViewed">查看简历并标记</button>
						</view>
						<template v-if="candidate.status === 'pending' || candidate.status === 'viewed'">
							<button class="status-btn btn-blue" @click="showInterviewForm = !showInterviewForm">
								<text>📅 安排面试</text>
							</button>
							<button class="status-btn btn-green" @click="handleAccept">✅ 录用</button>
							<button class="status-btn btn-gray" @click="handleReject">❌ 不合适</button>
						</template>
						<template v-if="candidate.status === 'interview'">
							<button class="status-btn btn-green" @click="handleAccept">✅ 录用</button>
							<button class="status-btn btn-gray" @click="handleReject">❌ 未通过</button>
						</template>
					</view>
				</view>

				<!-- 面试安排表单 -->
				<view v-if="showInterviewForm" class="section-card">
					<view class="section-title-row">
						<text class="section-title">📅 面试安排</text>
					</view>
					<view class="form-group">
						<text class="form-label">面试时间</text>
						<picker mode="date" :value="interviewDate" @change="onDateChange">
							<view class="form-input-row">
								<text :class="interviewDate ? '' : 'color-muted'">{{ interviewDate || '请选择日期' }}</text>
								<text class="form-arrow">›</text>
							</view>
						</picker>
					</view>
					<view class="form-group">
						<text class="form-label">面试地点</text>
						<input class="form-input" v-model="interviewLocation" placeholder="请输入面试地点，如：会议室A" />
					</view>
					<view class="form-group">
						<text class="form-label">备注</text>
						<textarea class="form-textarea" v-model="interviewNote" placeholder="补充说明（可选）" />
					</view>
					<button class="submit-btn" @click="submitInterview">确认安排面试</button>
				</view>
			</template>

			<!-- 空状态 -->
			<view v-if="!loading && !candidate.id" class="empty-state">
				<text style="font-size:48px;margin-bottom:12px;">📋</text>
				<text>投递记录不存在</text>
			</view>

			<view style="height:40px;"></view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { deliveryAPI, hrAPI, resumeAPI, teacherAPI, scoreAPI } from '@/utils/request'

const loading = ref(true)
const refreshing = ref(false)
const candidate = ref({})
const resume = ref({})
const aiScore = ref(null)
const showInterviewForm = ref(false)
const interviewDate = ref('')
const interviewLocation = ref('')
const interviewNote = ref('')

const DELIVERY_STATUS = ['pending', 'viewed', 'interview', 'accepted', 'rejected']
const DELIVERY_STATUS_TEXT = ['待查看', '已查看', '面试中', '已录用', '未通过']
const STATUS_TO_INT = { pending: 0, viewed: 1, interview: 2, accepted: 3, rejected: 4 }

const avatarChar = computed(() => (candidate.value.studentName || '?').charAt(0).toUpperCase())

const scoreLevel = computed(() => {
	if (aiScore.value >= 85) return '非常匹配'
	if (aiScore.value >= 70) return '比较匹配'
	if (aiScore.value >= 60) return '一般匹配'
	return '匹配度较低'
})

const mapDelivery = (d) => ({
	id: d.id,
	studentId: d.studentId,
	studentName: d.studentName || '候选人',
	jobTitle: d.jobTitle || '岗位名称',
	status: DELIVERY_STATUS[d.status] !== undefined ? DELIVERY_STATUS[d.status] : 'pending',
	statusText: DELIVERY_STATUS_TEXT[d.status] !== undefined ? DELIVERY_STATUS_TEXT[d.status] : '待查看',
	createTime: d.createTime ? d.createTime.substring(0, 16).replace('T', ' ') : '--'
})

const mapResume = (data) => {
	if (!data) return {}
	let eduArray = []
	try { eduArray = JSON.parse(data.education || '[]') } catch (e) { }
	return {
		name: data.name || '',
		phone: data.phone || '',
		email: data.email || '',
		school: eduArray[0]?.school || '',
		major: eduArray[0]?.major || '',
		education: eduArray[0]?.degree || ''
	}
}

const getParamId = () => {
	const pages = getCurrentPages()
	const currentPage = pages[pages.length - 1]
	return currentPage.options ? Number(currentPage.options.id) : null
}

onMounted(async () => {
	const id = getParamId()
	if (id) await loadData(id)
	else loading.value = false
})

const onRefresh = async () => {
	refreshing.value = true
	const id = getParamId()
	if (id) await loadData(id)
	refreshing.value = false
}

const loadData = async (id) => {
	loading.value = true
	try {
		// 1. 直接用 API 获取投递详情（一条请求，不遍历）
		const res = await deliveryAPI.getDeliveryDetail(id)
		const d = res.data
		if (d) {
			candidate.value = mapDelivery(d)
			// 2. 加载简历
			await loadResume(d.studentId)
			// 3. 加载 AI 评分
			await loadScore(id)
		}
	} catch (e) {
		console.error('加载投递详情失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	} finally {
		loading.value = false
	}
}

const loadResume = async (studentId) => {
	if (!studentId) return
	try {
		const res = await teacherAPI.getStudentResume(studentId)
		resume.value = mapResume(res.data)
	} catch (e) {
		resume.value = { name: candidate.value.studentName || '' }
	}
}

const loadScore = async (deliveryId) => {
	try {
		const res = await scoreAPI.getByDelivery(deliveryId)
		if (res.data && res.data.score !== undefined) {
			aiScore.value = Math.round(res.data.score)
		}
	} catch (e) {
		// 没有评分数据，保持 null
	}
}

const triggerScore = async () => {
	const id = getParamId()
	if (!id || !candidate.value.id) {
		uni.showToast({ title: '暂无数据', icon: 'none' })
		return
	}
	uni.showToast({ title: '评分计算中...', icon: 'loading' })
	try {
		// 通过 jobId 和 deliveryId 触发评分
		const jobId = candidate.value.jobId
		await scoreAPI.score(jobId, id)
		await loadScore(id)
		uni.showToast({ title: '评分完成', icon: 'success' })
	} catch (e) {
		uni.showToast({ title: '评分失败', icon: 'none' })
	}
}

const refreshScore = triggerScore

const updateStatus = async (newStatus, toastText) => {
	const statusInt = STATUS_TO_INT[newStatus]
	if (statusInt === undefined) return
	try {
		await hrAPI.updateDeliveryStatus(candidate.value.id, { status: statusInt })
		candidate.value.status = newStatus
		candidate.value.statusText = DELIVERY_STATUS_TEXT[statusInt]
		uni.showToast({ title: toastText || '成功', icon: 'success' })
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'none' })
	}
}

const markViewed = () => updateStatus('viewed', '标记成功')

const handleAccept = () => {
	uni.showModal({
		title: '确认录用',
		content: '确定录用该候选人吗？',
		success: (r) => { if (r.confirm) updateStatus('accepted', '录用成功') }
	})
}

const handleReject = () => {
	const msg = candidate.value.status === 'interview' ? '确定该候选人面试未通过吗？' : '确定标记为不合适吗？'
	uni.showModal({
		title: '确认',
		content: msg,
		success: (r) => { if (r.confirm) updateStatus('rejected', '已标记') }
	})
}

const onDateChange = (e) => { interviewDate.value = e.detail.value }

const submitInterview = async () => {
	if (!interviewDate.value || !interviewLocation.value) {
		uni.showToast({ title: '请填写完整信息', icon: 'none' })
		return
	}
	uni.showToast({ title: '安排中...', icon: 'loading' })
	try {
		await hrAPI.updateDeliveryStatus(candidate.value.id, { status: 2 })
		candidate.value.status = 'interview'
		candidate.value.statusText = '面试中'
		uni.showToast({ title: '面试安排已发送', icon: 'success' })
		showInterviewForm.value = false
		interviewDate.value = ''
		interviewLocation.value = ''
		interviewNote.value = ''
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'none' })
	}
}

const viewFullResume = () => {
	uni.showToast({ title: '简历详情页开发中', icon: 'none' })
}

const goBack = () => { uni.navigateBack() }
</script>

<style scoped>
/* ========== 基础卡片 ========== */
.section-card {
	background: white;
	border-radius: 16px;
	margin: 12px 16px;
	padding: 20px;
	box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}
.section-title-row {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 14px;
}
.section-title {
	font-size: 16px;
	font-weight: 700;
	color: #1D2129;
}
.section-action {
	font-size: 13px;
	color: #0EA5E9;
	font-weight: 500;
	padding: 4px 0;
}

/* ========== 候选人头部 ========== */
.candidate-header {
	flex-direction: row;
	align-items: center;
	gap: 14px;
}
.candidate-avatar {
	width: 56px;
	height: 56px;
	border-radius: 50%;
	background: linear-gradient(135deg, #0EA5E9, #38BDF8);
	align-items: center;
	justify-content: center;
	color: white;
	font-size: 24px;
	font-weight: 600;
	flex-shrink: 0;
	box-shadow: 0 4px 12px rgba(14,165,233,0.25);
}
.candidate-meta {
	flex: 1;
}
.candidate-name {
	font-size: 18px;
	font-weight: 700;
	color: #1D2129;
	display: block;
	margin-bottom: 2px;
}
.candidate-pos {
	font-size: 13px;
	color: #86909C;
	display: block;
	margin-bottom: 6px;
}
.candidate-tags {
	flex-direction: row;
	gap: 8px;
}
.time-tag {
	font-size: 11px;
	color: #C9CDD4;
	background: #F2F3F5;
	padding: 2px 8px;
	border-radius: 4px;
}

/* ========== 状态徽章 ========== */
.status-badge {
	padding: 6px 14px;
	border-radius: 20px;
	font-size: 13px;
	font-weight: 700;
	white-space: nowrap;
}
.sb-pending { background: rgba(245,158,11,0.12); color: #D97706; }
.sb-viewed { background: rgba(22,93,255,0.1); color: #165DFF; }
.sb-interview { background: rgba(14,165,233,0.12); color: #0284C7; }
.sb-accepted { background: rgba(16,185,129,0.12); color: #059669; }
.sb-rejected { background: rgba(239,68,68,0.1); color: #DC2626; }

/* ========== AI 评分 ========== */
.score-card {
	background: linear-gradient(135deg, #EEF2FF 0%, #E0F2FE 100%);
	border: 1px solid rgba(14,165,233,0.2);
}
.score-placeholder { background: #F8F9FC; border: 1px dashed #D0D5DD; }
.score-row {
	flex-direction: row;
	align-items: center;
	gap: 16px;
	margin-bottom: 12px;
}
.score-circle {
	width: 64px;
	height: 64px;
	border-radius: 50%;
	background: linear-gradient(135deg, #0EA5E9, #38BDF8);
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	box-shadow: 0 4px 12px rgba(14,165,233,0.3);
}
.score-num {
	font-size: 28px;
	font-weight: 800;
	color: white;
	line-height: 1;
}
.score-label {
	font-size: 11px;
	color: rgba(255,255,255,0.8);
}
.score-detail { flex: 1; }
.score-level {
	font-size: 18px;
	font-weight: 700;
	color: #0EA5E9;
	display: block;
	margin-bottom: 2px;
}
.score-desc {
	font-size: 12px;
	color: #6B7280;
}
.score-hint {
	font-size: 14px;
	color: #86909C;
	margin-top: 4px;
}
.score-bar {
	width: 100%;
	height: 6px;
	border-radius: 3px;
	background: rgba(14,165,233,0.15);
	overflow: hidden;
}
.score-bar-fill {
	height: 100%;
	border-radius: 3px;
	background: linear-gradient(90deg, #0EA5E9, #38BDF8);
	transition: width 0.3s;
}

/* ========== 简历信息网格 ========== */
.info-grid {
	flex-direction: row;
	flex-wrap: wrap;
	gap: 0;
}
.info-cell {
	width: 50%;
	padding: 12px 0;
	border-bottom: 1px solid #F2F3F5;
}
.info-cell:nth-child(odd) { padding-right: 12px; }
.info-cell:nth-last-child(-n+2) { border-bottom: none; }
.info-label {
	font-size: 12px;
	color: #86909C;
	display: block;
	margin-bottom: 4px;
}
.info-value {
	font-size: 14px;
	font-weight: 500;
	color: #1D2129;
}
.empty-resume {
	padding: 24px 0;
	align-items: center;
	color: #86909C;
	font-size: 14px;
}

/* ========== 操作按钮 ========== */
.action-group {
	gap: 10px;
}
.action-row-full {
	width: 100%;
}
.status-btn {
	flex-direction: row;
	align-items: center;
	justify-content: center;
	padding: 14px;
	border-radius: 12px;
	font-size: 14px;
	font-weight: 600;
	border: none;
	width: 100%;
	margin-bottom: 0;
}
.btn-primary {
	background: linear-gradient(135deg, #0EA5E9, #38BDF8);
	color: white;
	box-shadow: 0 4px 12px rgba(14,165,233,0.3);
}
.btn-blue { background: rgba(14,165,233,0.1); color: #0EA5E9; }
.btn-green { background: rgba(16,185,129,0.1); color: #10B981; }
.btn-gray { background: #F2F3F5; color: #86909C; }

/* ========== 面试表单 ========== */
.form-group { margin-bottom: 16px; }
.form-label {
	font-size: 14px;
	font-weight: 600;
	color: #1D2129;
	margin-bottom: 8px;
	display: block;
}
.form-input-row {
	width: 100%;
	height: 48px;
	border: 2px solid #E2E8F0;
	border-radius: 12px;
	padding: 0 16px;
	background: #F8F9FC;
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
}
.form-input {
	width: 100%;
	height: 48px;
	border: 2px solid #E2E8F0;
	border-radius: 12px;
	padding: 0 16px;
	font-size: 14px;
	background: #F8F9FC;
	color: #1D2129;
}
.form-textarea {
	width: 100%;
	min-height: 80px;
	border: 2px solid #E2E8F0;
	border-radius: 12px;
	padding: 12px 16px;
	font-size: 14px;
	background: #F8F9FC;
	color: #1D2129;
	line-height: 1.6;
}
.form-arrow { color: #C9CDD4; font-size: 16px; }
.color-muted { color: #C9CDD4; }
.submit-btn {
	width: 100%;
	padding: 16px;
	border-radius: 12px;
	background: linear-gradient(135deg, #0EA5E9, #38BDF8);
	color: white;
	font-size: 15px;
	font-weight: 700;
	align-items: center;
	justify-content: center;
	margin-top: 8px;
	box-shadow: 0 4px 12px rgba(14,165,233,0.3);
	border: none;
}

/* ========== 状态 ========== */
.loading-state { padding: 60px 20px; align-items: center; }
.loading-text { color: #86909C; font-size: 14px; }
.empty-state { padding: 60px 20px; align-items: center; color: #86909C; font-size: 14px; }
</style>
