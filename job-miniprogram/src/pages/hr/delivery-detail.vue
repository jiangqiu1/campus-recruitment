<template>
	<view class="page-wrapper">
		<NavBar title="候选人详情" showBack @back="goBack" />

		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<LoadingState v-if="loading" />

			<template v-if="!loading && candidate.id">
				<!-- 1. 候选人信息卡 -->
				<view class="profile-card">
					<view class="profile-top">
						<view class="profile-avatar"><text>{{ avatarChar }}</text></view>
						<view class="profile-meta">
							<text class="profile-name">{{ candidate.studentName || '候选人' }}</text>
							<text class="profile-job">{{ candidate.jobTitle || '岗位名称' }}</text>
							<text class="profile-time">投递于 {{ candidate.createTime || '--' }}</text>
						</view>
						<text class="status-badge" :class="'sb-' + candidate.status">{{ candidate.statusText }}</text>
					</view>
				</view>

				<!-- 2. AI 评分模块（默认折叠维度详情） -->
				<view class="section-card score-card">
					<view class="section-title-row" @click="scoreExpanded = !scoreExpanded">
						<text class="section-title">AI 简历评分</text>
						<view class="section-right">
							<text v-if="aiScore !== null" class="section-action" @click.stop="triggerScore">刷新</text>
							<text v-else class="section-action" @click.stop="triggerScore">点击评分 ›</text>
							<uni-icons :type="scoreExpanded ? 'arrowdown' : 'arrowright'" size="14" color="#C9CDD4" />
						</view>
					</view>
					<template v-if="aiScore !== null">
						<view class="score-hero">
							<view class="score-circle">
								<text class="score-big">{{ aiScore }}</text>
							</view>
							<view class="score-hero-info">
								<text class="score-level">{{ scoreLevel }}</text>
								<text class="score-desc">岗位匹配度评估</text>
								<text v-if="scoreComment" class="score-comment">{{ scoreComment }}</text>
							</view>
						</view>
						<view class="score-bar-wrap">
							<view class="score-bar"><view class="score-fill" :style="{ width: aiScore + '%' }" /></view>
						</view>
						<view v-if="scoreExpanded && scoreDims.length" class="score-dims">
							<view v-for="(dim, i) in scoreDims" :key="i" class="dim-row">
								<text class="dim-label">{{ dim.label }}</text>
								<view class="dim-track"><view class="dim-fill" :style="{ width: dim.percent + '%' }" /></view>
								<text class="dim-val">{{ dim.score }}</text>
							</view>
						</view>
					</template>
					<text v-else-if="!aiScore && !loading" class="score-hint" @click="triggerScore">点击启用 AI 评分，评估候选人与岗位匹配度</text>
				</view>

				<!-- 3. 简历信息（展开全部字段） -->
				<view class="section-card">
					<text class="section-title">简历信息</text>
					<view v-if="resume.name" class="resume-full">
						<!-- 基本信息 -->
						<view class="resume-block">
							<text class="block-label">基本信息</text>
							<view class="block-grid">
								<view><text class="grid-label">姓名</text><text class="grid-value">{{ resume.name }}</text></view>
								<view><text class="grid-label">电话</text><text class="grid-value">{{ resume.phone || '--' }}</text></view>
							</view>
						</view>
						<!-- 教育经历 -->
						<view class="resume-block">
							<text class="block-label">教育经历</text>
							<view class="block-grid">
								<view><text class="grid-label">学校</text><text class="grid-value">{{ resume.school || '--' }}</text></view>
								<view><text class="grid-label">专业</text><text class="grid-value">{{ resume.major || '--' }}</text></view>
								<view><text class="grid-label">学历</text><text class="grid-value">{{ resume.education || '--' }}</text></view>
							</view>
						</view>
						<!-- 技能证书 -->
						<view v-if="resume.skills" class="resume-block">
							<text class="block-label">技能证书</text>
							<view class="tag-container">
								<text v-for="(s, i) in resume.skills.split(/[,，]/).map(v => v.trim()).filter(Boolean)" :key="i" class="skill-chip">{{ s }}</text>
							</view>
						</view>
						<!-- 实习经历 -->
						<view v-if="resume.internships && resume.internships.length" class="resume-block">
							<text class="block-label">实习经历</text>
							<view v-for="(exp, i) in resume.internships" :key="i" class="exp-item">
								<text class="exp-title">{{ exp.company || exp.companyName }} · {{ exp.position || exp.jobTitle }}</text>
								<text class="exp-duration">{{ exp.duration || exp.start }} - {{ exp.end || '至今' }}</text>
								<text v-if="exp.description" class="exp-desc">{{ exp.description }}</text>
							</view>
						</view>
						<!-- 求职意向 -->
						<view v-if="resume.jobTarget" class="resume-block">
							<text class="block-label">求职意向</text>
							<text class="block-text">{{ resume.jobTarget }}</text>
						</view>
						<!-- 自我评价 -->
						<view v-if="resume.selfEvaluation" class="resume-block">
							<text class="block-label">自我评价</text>
							<text class="block-text">{{ resume.selfEvaluation }}</text>
						</view>
					</view>
					<view v-else class="empty-hint">暂未获取到简历数据</view>
				</view>

				<!-- 4. 操作区（根据状态展示） -->
				<view class="section-card">
					<view class="section-title-row">
						<text class="section-title">操作</text>
					</view>
					<view class="action-group">
						<!-- 初筛阶段：只提供面试与淘汰，录用留给面试后（高风险动作按阶段出现） -->
						<template v-if="candidate.status === 'pending' || candidate.status === 'viewed'">
							<text class="action-btn primary" @click="showInterviewPopup = true">安排面试</text>
							<text class="action-btn danger" @click="handleReject">不合适</text>
						</template>
						<template v-if="candidate.status === 'interview'">
							<text class="action-btn primary" @click="showInterviewPopup = true">调整面试</text>
							<text class="action-btn success" @click="handleAccept">录用</text>
							<text class="action-btn danger" @click="handleReject">未通过</text>
						</template>
						<template v-if="candidate.status === 'accepted' || candidate.status === 'rejected'">
							<text class="action-btn disabled">该候选人已{{ candidate.statusText }}</text>
						</template>
					</view>
				</view>
			</template>

			<view v-if="!loading && !candidate.id" class="empty-block">
				<text style="color:#86909C;font-size:14px;">投递记录不存在</text>
			</view>

			<view style="height:40px;" />
		</scroll-view>

		<!-- 面试安排弹窗 -->
		<view class="modal-overlay" v-if="showInterviewPopup" @click="showInterviewPopup = false">
			<view class="modal-content" @click.stop>
				<view class="modal-header">
					<text class="modal-title">安排面试</text>
					<text class="modal-close" @click="showInterviewPopup = false">✕</text>
				</view>
				<view class="modal-body">
					<text class="form-label">面试日期</text>
					<picker mode="date" :value="interviewDate" @change="onInterviewDateChange" fields="day">
						<view class="picker-input" :class="{ 'picker-placeholder': !interviewDate }">
							{{ interviewDate || '点击选择日期' }}
						</view>
					</picker>
					<text class="form-label" style="margin-top:12px;">面试时间</text>
					<picker mode="time" :value="interviewTime" @change="onInterviewTimeChange">
						<view class="picker-input" :class="{ 'picker-placeholder': !interviewTime }">
							{{ interviewTime || '点击选择时间' }}
						</view>
					</picker>
					<text class="form-label" style="margin-top:12px;">面试地点</text>
					<input class="form-input" v-model="interviewForm.location" type="text" placeholder="线上/公司地址" />
					<text class="form-label" style="margin-top:12px;">备注（选填）</text>
					<input class="form-input" v-model="interviewForm.note" type="text" placeholder="可选备注信息" />
					<button class="submit-btn" @click="submitInterview">确认安排</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { deliveryAPI, hrAPI, teacherAPI, scoreAPI } from '@/utils/request'
import LoadingState from '@/components/LoadingState.vue'
import NavBar from '@/components/NavBar.vue'

const loading = ref(true)
const candidate = ref({})
const resume = ref({})
const aiScore = ref(null)
const scoreComment = ref('')
const scoreDims = ref([])
const scoreExpanded = ref(false)
const showInterviewPopup = ref(false)
const interviewDate = ref('')
const interviewTime = ref('')
const interviewForm = ref({ location: '', note: '' })

const DELIVERY_STATUS = ['pending', 'viewed', 'interview', 'accepted', 'rejected']
const DELIVERY_STATUS_TEXT = ['待查看', '已查看', '面试中', '已录用', '未通过']
const STATUS_TO_INT = { pending: 0, viewed: 1, interview: 2, accepted: 3, rejected: 4 }

const onInterviewDateChange = (e) => { interviewDate.value = e.detail.value }
const onInterviewTimeChange = (e) => { interviewTime.value = e.detail.value }

const avatarChar = computed(() => (candidate.value.studentName || '?').charAt(0))

const scoreLevel = computed(() => {
	if (aiScore.value >= 85) return '非常匹配'
	if (aiScore.value >= 70) return '比较匹配'
	if (aiScore.value >= 60) return '一般匹配'
	return '匹配度较低'
})

const mapDelivery = (d) => ({
	id: d.id, studentId: d.studentId, studentName: d.studentName || '候选人',
	jobTitle: d.jobTitle || '岗位名称', jobId: d.jobId,
	status: DELIVERY_STATUS[d.status] !== undefined ? DELIVERY_STATUS[d.status] : 'pending',
	statusText: DELIVERY_STATUS_TEXT[d.status] !== undefined ? DELIVERY_STATUS_TEXT[d.status] : '待查看',
	createTime: d.createTime ? d.createTime.substring(5, 16).replace('T', ' ') : '--'
})

const mapResume = (data) => {
	if (!data) return {}
	let edu = []
	try { edu = JSON.parse(data.education || '[]') } catch (e) {}
	let interships = []
	try { interships = JSON.parse(data.internship || '[]') } catch (e) {}
	return {
		name: data.realName || candidate.value.studentName || '',
		phone: data.phone || '',
		school: edu[0]?.school || '',
		major: edu[0]?.major || '',
		education: edu[0]?.degree || '',
		skills: data.skills || '',
		selfEvaluation: data.selfEvaluation || '',
		jobTarget: data.jobTarget || '',
		internships: interships
	}
}

const getParamId = () => {
	const pages = getCurrentPages()
	return pages[pages.length - 1]?.options?.id ? Number(pages[pages.length - 1].options.id) : null
}

onMounted(async () => {
	const id = getParamId()
	if (id) await loadData(id)
	else loading.value = false
})

const refreshing = ref(false)
const onRefresh = async () => {
	refreshing.value = true
	const id = getParamId()
	if (id) await loadData(id)
	refreshing.value = false
}

const loadData = async (id) => {
	loading.value = true
	try {
		const res = await deliveryAPI.getDeliveryDetail(id)
		const d = res.data
		if (d) {
			candidate.value = mapDelivery(d)
			await loadResume(d.studentId)
			await loadScore(id)
		}
	} catch (e) {
		console.error('加载失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	} finally { loading.value = false }
}

const loadResume = async (studentId) => {
	if (!studentId) return
	try {
		const res = await teacherAPI.getStudentResume(studentId)
		resume.value = mapResume(res.data)
	} catch (e) { resume.value = { name: candidate.value.studentName || '' } }
}

const loadScore = async (deliveryId) => {
	try {
		const res = await scoreAPI.getByDelivery(deliveryId)
		if (res.data?.score !== undefined) {
			aiScore.value = Math.round(res.data.score)
			// 从 scoreDetail JSON 中解析评语和维度分
			const dims = []
			let comment = ''
			if (res.data.scoreDetail) {
				try {
					const detail = typeof res.data.scoreDetail === 'string'
						? JSON.parse(res.data.scoreDetail)
						: res.data.scoreDetail
					// 兼容新旧格式
					const s = detail.skillScore ?? detail['技能得分'] ?? 0
					const e = detail.expScore ?? detail['经验得分'] ?? 0
					const ed = detail.eduScore ?? detail['教育得分'] ?? 0
					comment = detail.comment ?? detail['评语'] ?? ''
					if (s || e || ed) {
						dims.push({ label: '技能匹配', score: s, percent: s })
						dims.push({ label: '经验匹配', score: e, percent: e })
						dims.push({ label: '学历匹配', score: ed, percent: ed })
					}
				} catch (_) {}
			}
			scoreComment.value = comment
			scoreDims.value = dims
		}
	} catch (e) { /* no score */ }
}

const triggerScore = async () => {
	const id = getParamId()
	if (!id || !candidate.value.id) { uni.showToast({ title: '暂无数据', icon: 'none' }); return }
	uni.showToast({ title: '评分计算中...', icon: 'loading' })
	try {
		await scoreAPI.score(candidate.value.jobId, id)
		await loadScore(id)
		uni.showToast({ title: '评分完成', icon: 'success' })
	} catch (e) {
		uni.showToast({ title: '评分失败', icon: 'none' })
	}
}

const updateStatus = async (newStatus, toastText) => {
	const int = STATUS_TO_INT[newStatus]
	if (int === undefined) return
	try {
		await hrAPI.updateDeliveryStatus(candidate.value.id, { status: int })
		candidate.value.status = newStatus
		candidate.value.statusText = DELIVERY_STATUS_TEXT[int]
		uni.showToast({ title: toastText || '成功', icon: 'success' })
	} catch (e) { uni.showToast({ title: '操作失败', icon: 'none' }) }
}

const handleAccept = () => {
	uni.showModal({
		title: '确认录用', content: '确定录用该候选人吗？',
		success: (r) => { if (r.confirm) updateStatus('accepted', '录用成功') }
	})
}

const handleReject = () => {
	const msg = candidate.value.status === 'interview' ? '确定面试未通过吗？' : '确定标记为不合适吗？'
	uni.showModal({
		title: '确认', content: msg,
		success: (r) => { if (r.confirm) updateStatus('rejected', '已标记') }
	})
}

const submitInterview = async () => {
	const dateStr = interviewDate.value
	const timeStr = interviewTime.value
	if (!dateStr || !timeStr) {
		uni.showToast({ title: '请选择面试日期和时间', icon: 'none' }); return
	}
	if (!interviewForm.value.location || !interviewForm.value.location.trim()) {
		uni.showToast({ title: '请填写面试地点', icon: 'none' }); return
	}
	try {
		const dateTime = dateStr + 'T' + timeStr + ':00'
		await hrAPI.arrangeInterview(candidate.value.id, {
			interviewTime: dateTime,
			interviewLocation: interviewForm.value.location.trim()
		})
		candidate.value.status = 'interview'
		candidate.value.statusText = '面试中'
		uni.showToast({ title: '面试已安排', icon: 'success' })
		showInterviewPopup.value = false
		interviewDate.value = ''
		interviewTime.value = ''
		interviewForm.value = { location: '', note: '' }
	} catch (e) { uni.showToast({ title: '操作失败', icon: 'none' }) }
}

const goBack = () => uni.navigateBack()
</script>

<style scoped lang="scss">
/* ===== 候选人信息卡 ===== */
.profile-card {
	background: $uni-bg-color;
	border-radius: 12px;
	margin: 12px 16px;
	padding: 16px;
	box-shadow: $uni-shadow-card;
}
.profile-top {
	flex-direction: row;
	align-items: center;
	gap: 12px;
}
.profile-avatar {
	width: 52px; height: 52px; border-radius: 50%;
	background: $uni-gradient-primary;
	align-items: center; justify-content: center;
	color: $uni-text-color-inverse; font-size: 22px; font-weight: 700; flex-shrink: 0;
}
.profile-meta { flex: 1; }
.profile-name { font-size: 18px; font-weight: 700; color: $uni-text-color-title; display: block; }
.profile-job { font-size: 13px; color: $uni-text-color-secondary; margin-top: 2px; display: block; }
.profile-time { font-size: 12px; color: $uni-text-color-placeholder; margin-top: 4px; display: block; }

/* ===== 状态徽章 ===== */
.status-badge {
	padding: 5px 12px; border-radius: 999px; font-size: 12px; font-weight: 600; flex-shrink: 0;
}
.sb-pending { background: rgba(245,158,11,0.1); color: $uni-color-warning; }
.sb-viewed { background: $uni-color-primary-light; color: $uni-color-primary; }
.sb-interview { background: $uni-color-primary-light; color: $uni-color-primary; }
.sb-accepted { background: $uni-color-success-light; color: $uni-color-success; }
.sb-rejected { background: rgba(239,68,68,0.1); color: $uni-color-error; }

/* ===== 通用卡片 ===== */
.section-card {
	background: $uni-bg-color; border-radius: 12px;
	margin: 0 16px 10px; padding: 16px;
	box-shadow: $uni-shadow-card;
}
.section-title-row {
	flex-direction: row; justify-content: space-between; align-items: center;
}
.section-title { font-size: 15px; font-weight: 700; color: $uni-text-color-title; }
.section-right { flex-direction: row; align-items: center; gap: 6px; }
.section-action { font-size: 13px; color: $uni-color-primary; font-weight: 500; }
.score-num-text { font-size: 16px; font-weight: 700; color: $uni-color-primary; }

/* ===== AI 评分 Signature ===== */
.score-card {
	background: linear-gradient(135deg, #EEF2FF 0%, #E0F2FE 100%);
	border: 1px solid $uni-color-primary-light;
}
.score-hero {
	flex-direction: row;
	align-items: center;
	gap: 16px;
	margin: 16px 0 12px;
}
.score-circle {
	width: 56px;
	height: 56px;
	border-radius: 50%;
	background: linear-gradient(135deg, $uni-color-primary, $uni-color-primary-hover);
	align-items: center;
	justify-content: center;
	box-shadow: 0 4px 14px $uni-color-primary-light;
	flex-shrink: 0;
}
.score-big {
	font-size: 26px;
	font-weight: 800;
	color: $uni-text-color-inverse;
	line-height: 1;
}
.score-hero-info { flex: 1; }
.score-level {
	font-size: 16px;
	font-weight: 700;
	color: $uni-color-primary;
	display: block;
	margin-bottom: 2px;
}
.score-desc {
	font-size: 12px;
	color: $uni-text-color;
}
.score-comment {
	font-size: 13px;
	color: $uni-text-color;
	margin-top: 4px;
	line-height: 1.5;
}
.score-bar-wrap { margin-top: 0; margin-bottom: 12px; }
.score-bar { flex: 1; height: 6px; background: $uni-color-primary-light; border-radius: 4px; overflow: hidden; }
.score-fill { height: 100%; border-radius: 4px; background: linear-gradient(90deg, $uni-color-primary, $uni-color-primary-hover); }
.score-hint { font-size: 13px; color: $uni-text-color-secondary; margin-top: 10px; }
.score-dims { margin-top: 12px; gap: 8px; }
.dim-row { flex-direction: row; align-items: center; gap: 8px; }
.dim-label { width: 36px; font-size: 12px; color: $uni-text-color; }
.dim-track { flex: 1; height: 6px; background: $uni-border-color-divider; border-radius: 4px; overflow: hidden; }
.dim-fill { height: 100%; border-radius: 4px; background: $uni-color-primary; }
.dim-val { width: 24px; font-size: 12px; font-weight: 600; color: $uni-color-primary; text-align: right; }

/* ===== 简历全字段展示 ===== */
.resume-full { margin-top: 12px; }
.resume-block { margin-bottom: 16px; padding-bottom: 12px; border-bottom: 0.5px solid $uni-border-color-divider; }
.resume-block:last-child { border-bottom: none; margin-bottom: 0; padding-bottom: 0; }
.block-label { font-size: 13px; font-weight: 600; color: $uni-text-color; margin-bottom: 8px; display: block; }
.block-grid { flex-direction: row; flex-wrap: wrap; gap: 4px; }
.block-grid view { width: 50%; padding: 4px 0; }
.block-text { font-size: 14px; color: $uni-text-color; line-height: 1.6; }
.grid-label { font-size: 12px; color: $uni-text-color-secondary; display: block; margin-bottom: 2px; }
.grid-value { font-size: 14px; font-weight: 500; color: $uni-text-color-title; word-break: break-all; }
.tag-container { flex-direction: row; flex-wrap: wrap; gap: 6px; }
.skill-chip { padding: 4px 10px; border-radius: 8px; font-size: 12px; background: $uni-color-primary-light; color: $uni-color-primary; }
.exp-item { margin-bottom: 8px; padding: 8px; background: $uni-bg-color-page; border-radius: 8px; }
.exp-title { font-size: 13px; font-weight: 600; color: $uni-text-color-title; display: block; }
.exp-duration { font-size: 12px; color: $uni-text-color-placeholder; display: block; margin-top: 2px; }
.exp-desc { font-size: 12px; color: $uni-text-color; display: block; margin-top: 4px; line-height: 1.5; }
.empty-hint { padding: 16px 0; text-align: center; color: $uni-text-color-secondary; font-size: 13px; }

/* ===== 操作区 ===== */
.action-group { margin-top: 12px; gap: 8px; }
.action-btn {
	display: block; text-align: center; padding: 12px 0;
	border-radius: 8px; font-size: 14px; font-weight: 600;
}
.action-btn.primary { background: $uni-color-primary; color: $uni-text-color-inverse; }
.action-btn.success { background: $uni-color-success-light; color: $uni-color-success; }
.action-btn.danger { background: rgba(239,68,68,0.1); color: $uni-color-error; }
.action-btn.disabled { background: $uni-border-color-divider; color: $uni-text-color-placeholder; }

/* ===== 面试弹窗 ===== */
.modal-overlay {
	position: fixed; top: 0; left: 0; right: 0; bottom: 0;
	background: rgba(0,0,0,0.5); z-index: 999;
	align-items: center; justify-content: center; padding: 40px 20px;
}
.modal-content {
	width: 100%; max-width: 340px; background: $uni-bg-color; border-radius: 999px; overflow: hidden;
}
.modal-header {
	flex-direction: row; justify-content: space-between; align-items: center; padding: 20px 20px 0;
}
.modal-title { font-size: 18px; font-weight: 700; color: $uni-text-color-title; }
.modal-close { font-size: 20px; color: $uni-text-color-secondary; padding: 4px; }
.modal-body { padding: 16px 20px 20px; }
.form-label { font-size: 13px; color: $uni-text-color; font-weight: 500; margin-bottom: 6px; display: block; }
.picker-input {
	width: 100%; height: 44px; border: 1px solid $uni-border-color;
	border-radius: 8px; padding: 0 12px; font-size: 14px;
	color: $uni-text-color-title; background: $uni-bg-color-page; box-sizing: border-box;
	align-items: center; justify-content: center;
	line-height: 44px;
}
.picker-placeholder { color: $uni-text-color-placeholder; }
.form-input {
	width: 100%; height: 44px; border: 1px solid $uni-border-color;
	border-radius: 8px; padding: 0 12px; font-size: 14px;
	color: $uni-text-color-title; background: $uni-bg-color-page; box-sizing: border-box;
}
.submit-btn {
	width: 100%; height: 44px; border-radius: 8px;
	background: $uni-color-primary; color: white; font-size: 15px;
	font-weight: 600; border: none; margin-top: 20px;
}

.empty-block { padding: 60px 20px; align-items: center; }
</style>
