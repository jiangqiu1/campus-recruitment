<template>
	<view class="page-wrapper">
		<NavBar title="候选人" :showBack="false" />

		<!-- 岗位筛选：横向滚动标签 -->
		<scroll-view class="job-tags-scroll" scroll-x show-scrollbar="false">
			<view class="job-tags-inner">
				<text v-for="(job, i) in jobTags" :key="i" class="job-tag" :class="{ active: selectedJobId === job.id }" @click="selectJob(job.id)">
					{{ job.label }}
					<text v-if="job.count" class="job-tag-count">{{ job.count }}</text>
				</text>
			</view>
		</scroll-view>

		<!-- 状态筛选标签 -->
		<view class="status-tabs">
			<text v-for="(tab, i) in statusTabs" :key="i" class="status-tab" :class="{ active: currentStatus === tab.value }" @click="currentStatus = tab.value">
				{{ tab.label }}
			</text>
		</view>

		<!-- 候选人列表 -->
		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<view class="candidate-list">
				<view v-for="(c, i) in filteredCandidates" :key="i" class="candidate-card" :class="{ 'pending-card': c.status === 'pending' }" @click="goToDetail(c.id)">
					<view class="candidate-top">
						<view class="cand-avatar"><text>{{ (c.studentName || '?').charAt(0) }}</text></view>
						<view class="cand-info">
							<view class="cand-name-row">
								<text class="cand-name">{{ c.studentName || '候选人' }}</text>
								<text v-if="c.score != null" class="score-tag" :class="'score-' + scoreLevel(c.score)">{{ c.score }}分</text>
							</view>
							<text class="cand-job">{{ c.jobTitle || '岗位名称' }}</text>
						</view>
						<text class="status-tag" :class="'tag-' + c.status">{{ c.statusText }}</text>
					</view>
					<view class="cand-meta">
						<text class="cand-time">{{ c.createTime || '' }}</text>
					</view>
					<view class="cand-actions" v-if="c.status === 'pending' || c.status === 'viewed'">
						<text class="action-tag primary" @click.stop="handleInterview(c)">安排面试</text>
						<text class="action-tag danger" @click.stop="handleReject(c)">不合适</text>
					</view>
				</view>
				<EmptyState v-if="!filteredCandidates.length" icon="inbox" title="暂无候选人" desc="有学生投递岗位后会自动出现在这里" />
			</view>
			<view style="height: calc(60px + env(safe-area-inset-bottom))" />
		</scroll-view>

		<!-- 安排面试弹窗 -->
		<view class="modal-overlay" v-if="showInterviewModal" @click="showInterviewModal = false">
			<view class="modal-content" @click.stop>
				<view class="modal-header">
					<text class="modal-title">安排面试</text>
					<text class="modal-close" @click="showInterviewModal = false">✕</text>
				</view>
				<view class="modal-body">
					<text class="form-label">面试时间</text>
					<input class="form-input" v-model="interviewForm.time" type="text" placeholder="例：2026-07-04 14:00" />
					<text class="form-label" style="margin-top:12px;">面试地点</text>
					<input class="form-input" v-model="interviewForm.location" type="text" placeholder="例：线上/公司地址" />
					<text class="form-label" style="margin-top:12px;">备注（选填）</text>
					<input class="form-input" v-model="interviewForm.note" type="text" placeholder="备注信息" />
					<button class="submit-btn" @click="submitInterview">确认安排</button>
				</view>
			</view>
		</view>

		<HrTabBar current="deliveries" />
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@/utils/page-lifecycle'
import { hrAPI } from '@/utils/request'
import HrTabBar from '@/components/HrTabBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import NavBar from '@/components/NavBar.vue'

const jobs = ref([])
const candidates = ref([])
const selectedJobId = ref('all')
const currentStatus = ref('all')
const showInterviewModal = ref(false)

const interviewForm = ref({ time: '', location: '', note: '' })
const interviewCandidate = ref(null)

const DELIVERY_STATUS = ['pending', 'viewed', 'interview', 'accepted', 'rejected']
const DELIVERY_STATUS_TEXT = ['待查看', '已查看', '面试中', '已录用', '未通过']

const statusTabs = [
	{ label: '全部', value: 'all' },
	{ label: '待查看', value: 'pending' },
	{ label: '面试中', value: 'interview' },
	{ label: '已录用', value: 'accepted' },
	{ label: '未通过', value: 'rejected' }
]

const jobTags = computed(() => {
	const tags = [{ id: 'all', label: '全部岗位', count: candidates.value.length }]
	jobs.value.forEach(j => {
		const count = candidates.value.filter(c => c.jobId === j.id).length
		tags.push({ id: j.id, label: j.title || '岗位#' + j.id, count })
	})
	return tags
})

const filteredCandidates = computed(() => {
	let list = candidates.value
	if (selectedJobId.value !== 'all') {
		list = list.filter(c => c.jobId === selectedJobId.value)
	}
	if (currentStatus.value !== 'all') {
		list = list.filter(c => c.status === currentStatus.value)
	}
	return list
})

const mapDelivery = (d) => ({
	id: d.id,
	studentId: d.studentId,
	studentName: d.studentName || '候选人',
	jobTitle: d.jobTitle || '岗位名称',
	jobId: d.jobId,
	status: DELIVERY_STATUS[d.status] || 'pending',
	statusText: DELIVERY_STATUS_TEXT[d.status] || '待查看',
	createTime: d.createTime ? d.createTime.substring(5, 16).replace('T', ' ') : '',
	score: d.score ?? d.matchScore ?? null
})

const scoreLevel = (s) => {
	if (s >= 80) return 'high'
	if (s >= 60) return 'mid'
	return 'low'
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

onMounted(async () => {
	const cId = getCompanyId()
	await loadData(cId)
})

onShow(() => {
	if (jobs.value.length) loadAllCandidates()
})

const refreshing = ref(false)
const onRefresh = async () => {
	refreshing.value = true
	const cId = getCompanyId()
	if (cId) await loadData(cId)
	refreshing.value = false
}

const loadData = async (cId) => {
	await loadJobs(cId)
	await loadAllCandidates()
}

const selectJob = (id) => {
	selectedJobId.value = id
}

const loadJobs = async (cId) => {
	try {
		const res = await hrAPI.getHrJobs(cId)
		jobs.value = res.data || []
	} catch (e) { console.error('加载岗位失败', e) }
}

const loadAllCandidates = async () => {
	try {
		const all = []
		for (const job of jobs.value) {
			try {
				const res = await hrAPI.getCompanyDeliveries(job.id)
				;(res.data || []).forEach(d => {
					all.push(mapDelivery({ ...d, jobTitle: job.title }))
				})
			} catch (e) { console.error('加载候选人列表失败', e) }
		}
		candidates.value = all
	} catch (e) {
		console.error('加载候选人失败', e)
	}
}

const handleInterview = (c) => {
	interviewCandidate.value = c
	interviewForm.value = { time: '', location: '', note: '' }
	showInterviewModal.value = true
}

const submitInterview = async () => {
	if (!interviewForm.value.time || !interviewForm.value.location) {
		uni.showToast({ title: '请填写面试时间和地点', icon: 'none' })
		return
	}
	try {
		await hrAPI.updateDeliveryStatus(interviewCandidate.value.id, { status: 2 })
		uni.showToast({ title: '已安排面试', icon: 'success' })
		showInterviewModal.value = false
		// 刷新状态
		const idx = candidates.value.findIndex(c => c.id === interviewCandidate.value.id)
		if (idx > -1) {
			candidates.value[idx].status = 'interview'
			candidates.value[idx].statusText = '面试中'
		}
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'none' })
	}
}

const handleReject = async (c) => {
	uni.showModal({
		title: '确认标记',
		content: '确定标记该候选人不合适吗？',
		success: async (res) => {
			if (!res.confirm) return
			try {
				await hrAPI.updateDeliveryStatus(c.id, { status: 4 })
				uni.showToast({ title: '已标记', icon: 'success' })
				const idx = candidates.value.findIndex(item => item.id === c.id)
				if (idx > -1) {
					candidates.value[idx].status = 'rejected'
					candidates.value[idx].statusText = '未通过'
				}
			} catch (e) {
				uni.showToast({ title: '操作失败', icon: 'none' })
			}
		}
	})
}

const goToDetail = (id) => {
	uni.navigateTo({ url: '/pages/hr/delivery-detail?id=' + id })
}
</script>

<style scoped>
/* ===== 岗位标签横向滚动 ===== */
.job-tags-scroll {
	white-space: nowrap;
	padding: 10px 16px;
	background: #FFFFFF;
}
.job-tags-inner {
	flex-direction: row;
	gap: 8px;
}
.job-tag {
	display: inline-block;
	padding: 6px 14px;
	border-radius: 20px;
	font-size: 13px;
	color: #4E5969;
	background: #F7F8FA;
	font-weight: 500;
}
.job-tag.active {
	background: rgba(22,93,255,0.1);
	color: #165DFF;
	font-weight: 600;
	box-shadow: 0 0 0 1px rgba(22,93,255,0.15);
}
.job-tag-count {
	font-size: 10px;
	color: #86909C;
	margin-left: 4px;
}

/* ===== 状态筛选 ===== */
.status-tabs {
	flex-direction: row;
	padding: 0 16px 8px;
	gap: 16px;
	background: #FFFFFF;
	border-bottom: 0.5px solid #F2F3F5;
}
.status-tab {
	font-size: 14px;
	color: #86909C;
	font-weight: 500;
	padding-bottom: 6px;
	position: relative;
}
.status-tab.active {
	color: #1D2129;
	font-weight: 600;
}
.status-tab.active::after {
	content: '';
	position: absolute;
	bottom: 0;
	left: 0;
	width: 20px;
	height: 3px;
	background: #165DFF;
	border-radius: 2px;
}

/* ===== 候选人卡片 ===== */
.candidate-list { padding: 12px 16px; }
.candidate-card {
	background: #FFFFFF;
	border-radius: 12px;
	padding: 14px 14px 14px 18px;
	margin-bottom: 10px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
	position: relative;
	overflow: hidden;
}
/* Signature：待查看卡片左侧橙色指示条 */
.candidate-card.pending-card::before {
	content: '';
	position: absolute;
	top: 8px;
	left: 0;
	width: 3px;
	height: calc(100% - 16px);
	background: #F59E0B;
	border-radius: 0 2px 2px 0;
}
.candidate-card:active { background: #F7F8FA; }
.candidate-top {
	flex-direction: row;
	align-items: center;
	gap: 10px;
	margin-bottom: 6px;
}
.cand-avatar {
	width: 40px;
	height: 40px;
	border-radius: 50%;
	background: linear-gradient(135deg, #165DFF, #2563EB);
	align-items: center;
	justify-content: center;
	color: #FFFFFF;
	font-size: 16px;
	font-weight: 700;
	flex-shrink: 0;
}
.cand-info { flex: 1; }
.cand-name-row {
	flex-direction: row;
	align-items: center;
	gap: 8px;
	margin-bottom: 2px;
}
.cand-name { font-size: 16px; font-weight: 700; color: #1D2129; letter-spacing: 0.01em; }
.score-tag {
	font-size: 11px;
	padding: 2px 6px;
	border-radius: 4px;
	font-weight: 600;
}
.score-high { background: rgba(0,180,42,0.1); color: #00B42A; }
.score-mid { background: rgba(22,93,255,0.1); color: #165DFF; }
.score-low { background: rgba(245,158,11,0.1); color: #F59E0B; }
.cand-job { font-size: 12px; color: #86909C; }
.cand-meta {
	margin-left: 50px;
	margin-bottom: 8px;
}
.cand-time { font-size: 11px; color: #C9CDD4; }

/* ===== 状态标签 ===== */
.status-tag {
	padding: 4px 10px;
	border-radius: 6px;
	font-size: 12px;
	font-weight: 600;
	flex-shrink: 0;
}
.tag-pending { background: rgba(245,158,11,0.1); color: #F59E0B; }
.tag-viewed { background: rgba(22,93,255,0.1); color: #165DFF; }
.tag-interview { background: rgba(22,93,255,0.1); color: #165DFF; }
.tag-accepted { background: rgba(0,180,42,0.1); color: #00B42A; }
.tag-rejected { background: rgba(239,68,68,0.1); color: #EF4444; }

/* ===== 操作标签 ===== */
.cand-actions {
	flex-direction: row;
	gap: 10px;
	margin-left: 50px;
}
.action-tag {
	flex: 1;
	text-align: center;
	padding: 7px 0;
	border-radius: 8px;
	font-size: 12px;
	font-weight: 600;
}
.action-tag.primary { background: #165DFF; color: #FFFFFF; box-shadow: 0 2px 6px rgba(22,93,255,0.25); }
.action-tag.danger {
	background: #FFFFFF;
	border: 1px solid #EF4444;
	color: #EF4444;
}

/* ===== 面试弹窗 ===== */
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
.form-label { font-size: 13px; color: #4E5969; font-weight: 500; margin-bottom: 6px; display: block; }
.form-input {
	width: 100%;
	height: 44px;
	border: 1px solid #E5E6EB;
	border-radius: 8px;
	padding: 0 12px;
	font-size: 14px;
	color: #1D2129;
	background: #F7F8FA;
	box-sizing: border-box;
}
.submit-btn {
	width: 100%;
	height: 44px;
	border-radius: 8px;
	background: #165DFF;
	color: white;
	font-size: 15px;
	font-weight: 600;
	border: none;
	margin-top: 20px;
}

.empty-state { padding: 60px 20px; align-items: center; color: #86909C; font-size: 14px; }
</style>
