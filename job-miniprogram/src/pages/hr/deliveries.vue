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
			<text v-for="(tab, i) in statusTabs" :key="i" class="status-tab" :class="{ active: currentStatus === tab.value }" @click="switchStatus(tab.value)">
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
					<text class="form-label">面试日期</text>
					<picker mode="date" :value="interviewDate" @change="onDateChange" fields="day">
						<view class="picker-input" :class="{ 'picker-placeholder': !interviewDate }">
							{{ interviewDate || '点击选择日期' }}
						</view>
					</picker>
					<text class="form-label" style="margin-top:12px;">面试时间</text>
					<picker mode="time" :value="interviewTime" @change="onTimeChange">
						<view class="picker-input" :class="{ 'picker-placeholder': !interviewTime }">
							{{ interviewTime || '点击选择时间' }}
						</view>
					</picker>
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

<script>
import { hrAPI } from '@/utils/request'
import HrTabBar from '@/components/HrTabBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import NavBar from '@/components/NavBar.vue'

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
	createTime: d.createTime ? d.createTime.substring(5, 16).replace('T', ' ') : '',
	score: d.score ?? d.matchScore ?? null
})

export default {
	components: { HrTabBar, EmptyState, NavBar },
	onLoad(options) {
		// 支持从岗位管理「查看候选人」直达：预选岗位
		if (options && options.jobId) this.selectedJobId = Number(options.jobId)
	},
	data() {
		return {
			jobs: [],
			candidates: [],
			selectedJobId: 'all',
			currentStatus: 'all',
			showInterviewModal: false,
			interviewDate: '',
			interviewTime: '',
			interviewForm: { location: '', note: '' },
			interviewCandidate: null,
			refreshing: false,
			firstLoad: true
		}
	},
	computed: {
		statusTabs() {
			return [
				{ label: '全部', value: 'all' },
				{ label: '待查看', value: 'pending' },
				{ label: '面试中', value: 'interview' },
				{ label: '已录用', value: 'accepted' },
				{ label: '未通过', value: 'rejected' }
			]
		},
		jobTags() {
			const tags = [{ id: 'all', label: '全部岗位', count: this.candidates.length }]
			this.jobs.forEach(j => {
				const count = this.candidates.filter(c => c.jobId === j.id).length
				tags.push({ id: j.id, label: j.title || '岗位#' + j.id, count })
			})
			return tags
		},
		filteredCandidates() {
			let list = this.candidates
			if (this.selectedJobId !== 'all') {
				list = list.filter(c => c.jobId === this.selectedJobId)
			}
			if (this.currentStatus !== 'all') {
				list = list.filter(c => c.status === this.currentStatus)
			}
			return list
		}
	},
	mounted() {
		const cId = this.getCompanyId()
		this.loadData(cId)
	},
	methods: {
		getCompanyId() {
			try {
				const raw = uni.getStorageSync('userInfo')
				if (!raw) return null
				const obj = JSON.parse(raw)
				return obj.companyId || obj.id || null
			} catch (e) {
				console.error('获取公司ID失败', e)
				return null
			}
		},
		scoreLevel(s) {
			if (s >= 80) return 'high'
			if (s >= 60) return 'mid'
			return 'low'
		},
		async loadData(cId) {
			await this.loadJobs(cId)
			await this.loadAllCandidates()
		},
		async loadJobs(cId) {
			try {
				const res = await hrAPI.getHrJobs(cId)
				this.jobs = res.data || []
			} catch (e) { console.error('加载岗位失败', e) }
		},
		async loadAllCandidates() {
			try {
				const cId = this.getCompanyId()
				if (!cId) return
				const res = await hrAPI.getDeliveriesByCompany(cId)
				const data = res.data || []
				const jobMap = {}
				this.jobs.forEach(j => { jobMap[j.id] = j.title })
				this.candidates = data.map(d => mapDelivery({ ...d, jobTitle: jobMap[d.jobId] || '未知岗位' }))
			} catch (e) {
				console.error('加载候选人失败', e)
			}
		},
		selectJob(id) {
			this.selectedJobId = id
		},
		switchStatus(val) {
			this.currentStatus = val
		},
		onRefresh() {
			this.refreshing = true
			const cId = this.getCompanyId()
			if (cId) this.loadData(cId).then(() => { this.refreshing = false })
		},
		handleInterview(c) {
			this.interviewCandidate = c
			this.interviewDate = ''
			this.interviewTime = ''
			this.interviewForm = { location: '', note: '' }
			this.showInterviewModal = true
		},
		onDateChange(e) { this.interviewDate = e.detail.value },
		onTimeChange(e) { this.interviewTime = e.detail.value },
		async submitInterview() {
			const dateStr = this.interviewDate
			const timeStr = this.interviewTime
			if (!dateStr || !timeStr) {
				uni.showToast({ title: '请选择面试日期和时间', icon: 'none' })
				return
			}
			if (!this.interviewForm.location || !this.interviewForm.location.trim()) {
				uni.showToast({ title: '请填写面试地点', icon: 'none' })
				return
			}
			try {
				const dateTime = dateStr + 'T' + timeStr + ':00'
				await hrAPI.arrangeInterview(this.interviewCandidate.id, {
					interviewTime: dateTime,
					interviewLocation: this.interviewForm.location.trim()
				})
				uni.showToast({ title: '已安排面试', icon: 'success' })
				this.showInterviewModal = false
				// Options API 直接赋值 — uni-app 原生响应式，100% 可渲染
				const fresh = JSON.parse(JSON.stringify(this.candidates))
				const idx = fresh.findIndex(c => c.id === this.interviewCandidate.id)
				if (idx > -1) {
					fresh[idx].status = 'interview'
					fresh[idx].statusText = '面试中'
				}
				this.candidates = fresh
				this.switchStatus('interview')
			} catch (e) {
				console.error('安排面试失败', e)
				uni.showToast({ title: '操作失败', icon: 'none' })
			}
		},
		async handleReject(c) {
			const res = await new Promise(resolve => {
				uni.showModal({
					title: '确认标记',
					content: '确定标记该候选人不合适吗？',
					success: (r) => resolve(r.confirm)
				})
			})
			if (!res) return
			try {
				await hrAPI.updateDeliveryStatus(c.id, { status: 4 })
				uni.showToast({ title: '已标记', icon: 'success' })
				this.candidates = this.candidates.map(item =>
					item.id === c.id ? { ...item, status: 'rejected', statusText: '未通过' } : item
				)
			} catch (e) {
				uni.showToast({ title: '操作失败', icon: 'none' })
			}
		},
		goToDetail(id) {
			uni.navigateTo({ url: '/pages/hr/delivery-detail?id=' + id })
		}
	}
}
</script>

<style scoped lang="scss">
/* ===== 岗位标签横向滚动 ===== */
.job-tags-scroll {
	white-space: nowrap;
	padding: 10px 16px;
	background: $uni-bg-color;
}
.job-tags-inner {
	flex-direction: row;
	gap: 8px;
}
.job-tag {
	display: inline-block;
	padding: 6px 14px;
	border-radius: 999px;
	font-size: 13px;
	color: $uni-text-color;
	background: $uni-bg-color-page;
	font-weight: 500;
}
.job-tag.active {
	background: $uni-color-primary-light;
	color: $uni-color-primary;
	font-weight: 600;
	box-shadow: 0 0 0 1px $uni-color-primary-light;
}
.job-tag-count {
	font-size: 10px;
	color: $uni-text-color-secondary;
	margin-left: 4px;
}

/* ===== 状态筛选 ===== */
.status-tabs {
	flex-direction: row;
	padding: 0 16px 8px;
	gap: 16px;
	background: $uni-bg-color;
	border-bottom: 0.5px solid $uni-border-color-divider;
}
.status-tab {
	font-size: 14px;
	color: $uni-text-color-secondary;
	font-weight: 500;
	padding-bottom: 6px;
	position: relative;
}
.status-tab.active {
	color: $uni-text-color-title;
	font-weight: 600;
}
.status-tab.active::after {
	content: '';
	position: absolute;
	bottom: 0;
	left: 0;
	width: 20px;
	height: 3px;
	background: $uni-color-primary;
	border-radius: 4px;
}

/* ===== 候选人卡片 ===== */
.candidate-list { padding: 12px 16px; }
.candidate-card {
	background: $uni-bg-color;
	border-radius: 12px;
	padding: 14px 14px 14px 18px;
	margin-bottom: 10px;
	box-shadow: $uni-shadow-card;
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
	background: $uni-color-warning;
	border-radius: 0 2px 2px 0;
}
.candidate-card:active { background: $uni-bg-color-page; }
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
	background: $uni-gradient-primary;
	align-items: center;
	justify-content: center;
	color: $uni-text-color-inverse;
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
.cand-name { font-size: 16px; font-weight: 700; color: $uni-text-color-title; letter-spacing: 0.01em; }
.score-tag {
	font-size: 12px;
	padding: 2px 6px;
	border-radius: 4px;
	font-weight: 600;
}
.score-high { background: $uni-color-success-light; color: $uni-color-success; }
.score-mid { background: $uni-color-primary-light; color: $uni-color-primary; }
.score-low { background: rgba(245,158,11,0.1); color: $uni-color-warning; }
.cand-job { font-size: 12px; color: $uni-text-color-secondary; }
.cand-meta {
	margin-left: 50px;
	margin-bottom: 8px;
}
.cand-time { font-size: 12px; color: $uni-text-color-placeholder; }

/* ===== 状态标签 ===== */
.status-tag {
	padding: 4px 10px;
	border-radius: 8px;
	font-size: 12px;
	font-weight: 600;
	flex-shrink: 0;
}
.tag-pending { background: rgba(245,158,11,0.1); color: $uni-color-warning; }
.tag-viewed { background: $uni-color-primary-light; color: $uni-color-primary; }
.tag-interview { background: $uni-color-primary-light; color: $uni-color-primary; }
.tag-accepted { background: $uni-color-success-light; color: $uni-color-success; }
.tag-rejected { background: rgba(239,68,68,0.1); color: $uni-color-error; }

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
.action-tag.primary { background: $uni-color-primary; color: $uni-text-color-inverse; box-shadow: 0 2px 6px $uni-color-primary-light; }
.action-tag.danger {
	background: $uni-bg-color;
	border: 1px solid $uni-color-error;
	color: $uni-color-error;
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
	background: $uni-bg-color;
	border-radius: 999px;
	overflow: hidden;
}
.modal-header {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	padding: 20px 20px 0;
}
.modal-title { font-size: 18px; font-weight: 700; color: $uni-text-color-title; }
.modal-close { font-size: 20px; color: $uni-text-color-secondary; padding: 4px; }
.modal-body { padding: 16px 20px 20px; }
.form-label { font-size: 13px; color: $uni-text-color; font-weight: 500; margin-bottom: 6px; display: block; }
.picker-input {
	width: 100%;
	height: 44px;
	border: 1px solid $uni-border-color;
	border-radius: 8px;
	padding: 0 12px;
	font-size: 14px;
	color: $uni-text-color-title;
	background: $uni-bg-color-page;
	box-sizing: border-box;
	align-items: center;
	line-height: 44px;
}
.picker-placeholder { color: $uni-text-color-placeholder; }
.form-input {
	width: 100%;
	height: 44px;
	border: 1px solid $uni-border-color;
	border-radius: 8px;
	padding: 0 12px;
	font-size: 14px;
	color: $uni-text-color-title;
	background: $uni-bg-color-page;
	box-sizing: border-box;
}
.submit-btn {
	width: 100%;
	height: 44px;
	border-radius: 8px;
	background: $uni-color-primary;
	color: white;
	font-size: 15px;
	font-weight: 600;
	border: none;
	margin-top: 20px;
}

.empty-state { padding: 60px 20px; align-items: center; color: $uni-text-color-secondary; font-size: 14px; }
</style>
