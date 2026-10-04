<template>
	<view class="page-wrapper">
		<NavBar title="审批管理" show-back />
		<view class="filter-tabs">
			<text v-for="(tab, i) in tabs" :key="i" class="filter-tab"
				:class="{ active: currentTab === tab.value }"
				@click="switchTab(tab.value)">
				{{ tab.label }}
				<text v-if="tab.badge > 0" class="tab-badge">{{ tab.badge }}</text>
			</text>
		</view>

		<scroll-view class="content-scrollable" scroll-y @scrolltolower="loadMore">
			<view v-if="loading" class="loading-wrap">
				<LoadingState type="skeleton" :rows="4" />
			</view>
			<view v-else-if="!list.length" class="empty-state">
				<EmptyState icon="checkmark-circle" title="暂无申请" :desc="'暂无' + (currentTab === 0 ? '待审核' : currentTab === 1 ? '已通过' : '已拒绝') + '的申请'" />
			</view>
			<view v-else class="approval-list">
				<view v-for="(item, i) in list" :key="item.id" class="approval-card" :class="{ 'pending-card': item.status === 0 }">
					<view class="card-header">
						<view class="job-info">
							<text class="job-title">{{ item.jobTitle || '未知岗位' }}</text>
							<text class="company-name">{{ item.companyName || '' }}</text>
						</view>
						<text class="status-tag" :class="statusClass(item.status)">
							{{ statusLabel(item.status) }}
						</text>
					</view>
				<view class="card-meta">
					<text><uni-icons type="person" size="12" color="#C9CDD4" /> 申请人：{{ item.applyUser || '未知' }}</text>
					<text><uni-icons type="calendar" size="12" color="#C9CDD4" /> {{ item.applyTime || '—' }}</text>
					<text><uni-icons type="info" size="12" color="#C9CDD4" /> 申请 #{{ item.id }}</text>
				</view>
				<view v-if="item.status === 2 && item.rejectReason" class="reject-reason-row">
						<text class="reject-reason-label">拒绝原因</text>
						<text class="reject-reason-text">{{ item.rejectReason }}</text>
					</view>
				<view v-if="item.changeContent" class="change-content">
					<text class="change-label">变更内容</text>
					<view class="change-rows">
						<view v-for="(row, idx) in parseChangeContent(item.changeContent)" :key="idx" class="change-row">
							<text class="change-key">{{ row.label }}</text>
							<text class="change-value">{{ row.value }}</text>
						</view>
					</view>
				</view>
					<view v-if="item.status === 0" class="card-actions">
						<button class="action-btn reject" @click="handleReject(item)">拒绝</button>
						<button class="action-btn approve" @click="handleApprove(item)">通过</button>
					</view>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@/utils/page-lifecycle'
import { teacherAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import { checkRole } from '@/utils/auth'

checkRole(1)

const currentTab = ref(0)
const list = ref([])
const loading = ref(true)

const pendingCount = ref(0)

const tabs = computed(() => [
	{ label: '待审核', value: 0, badge: pendingCount.value },
	{ label: '已通过', value: 1, badge: 0 },
	{ label: '已拒绝', value: 2, badge: 0 }
])

const statusClass = (s) => {
	const map = { 0: 'pending', 1: 'approved', 2: 'rejected' }
	return map[s] || ''
}
const statusLabel = (s) => {
	const map = { 0: '待审核', 1: '已通过', 2: '已拒绝' }
	return map[s] || '未知'
}

const fieldLabelMap = {
	title: '岗位名称',
	salaryRange: '薪资范围',
	salary: '薪资',
	salaryMin: '最低薪资',
	salaryMax: '最高薪资',
	type: '岗位类型',
	location: '工作地点',
	city: '工作城市',
	address: '详细地址',
	education: '学历要求',
	headcount: '招聘人数',
	description: '岗位描述',
	requirements: '任职要求',
	duties: '岗位职责',
	deadline: '截止日期'
}

const parseChangeContent = (content) => {
	if (!content || typeof content !== 'string') return []
	// 处理乱码 / 非法字符占位
	const trimmed = content.trim()
	if (!trimmed || /^[?？]+$/.test(trimmed)) return []
	let parsed = null
	try {
		parsed = JSON.parse(trimmed)
	} catch (e) {
		// 可能后端做了 JSON.stringify 的 JSON，或中文引号问题，尝试二次处理
		return [{ label: '原始内容', value: trimmed }]
	}
	if (parsed && typeof parsed === 'object') {
		return Object.entries(parsed)
			.filter(([, value]) => value !== '' && value !== null && value !== undefined)
			.map(([key, value]) => ({
				label: fieldLabelMap[key] || key,
				value: String(value)
			}))
	}
	return [{ label: '原始内容', value: String(parsed) }]
}

{
	const pages = getCurrentPages()
	const cp = pages[pages.length - 1]
	if (cp.options?.tab) currentTab.value = Number(cp.options.tab)
}

// 页面显示时刷新数据
onShow(() => {
	loadList()
	loading.value = false
})

const loadList = async () => {
	try {
		const res = await teacherAPI.getApprovals(currentTab.value)
		const data = res.data || []
		list.value = data.map(item => ({
			...item,
			applyTime: item.applyTime ? item.applyTime.replace('T', ' ').substring(0, 19) : '—'
		}))
		if (currentTab.value === 0) {
			pendingCount.value = data.filter(item => item.status === 0).length
		}
	} catch (e) {
		console.error('加载审批列表失败:', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

const switchTab = (val) => {
	currentTab.value = val
	loading.value = true
	loadList().finally(() => { loading.value = false })
}

const handleApprove = (item) => {
	uni.showModal({
		title: '通过变更申请',
		content: '确定通过该岗位的变更申请吗？通过后立即生效。',
		success: async (r) => {
			if (!r.confirm) return
			try {
				await teacherAPI.approveJobChange(item.id)
				uni.showToast({ title: '已通过', icon: 'success' })
				list.value = list.value.filter(x => x.id !== item.id)
				pendingCount.value = list.value.filter(x => x.status === 0).length
			} catch (e) {
				console.error('审批通过失败:', e)
				uni.showToast({ title: '操作失败', icon: 'none' })
			}
		}
	})
}

const handleReject = (item) => {
	uni.showModal({
		title: '拒绝变更申请',
		content: '确定拒绝该岗位的变更申请吗？拒绝后将通知企业HR。',
		success: async (r) => {
			if (!r.confirm) return
			try {
				await teacherAPI.rejectJobChange(item.id)
				uni.showToast({ title: '已拒绝', icon: 'success' })
				list.value = list.value.filter(x => x.id !== item.id)
				pendingCount.value = list.value.filter(x => x.status === 0).length
			} catch (e) {
				console.error('拒绝失败:', e)
				uni.showToast({ title: '操作失败', icon: 'none' })
			}
		}
	})
}

const loadMore = () => {}
</script>

<style scoped lang="scss">
.filter-tabs {
	flex-direction: row;
	padding: 0 16px;
	background: $uni-bg-color;
	gap: 20px;
	border-bottom: 0.5px solid $uni-border-color-divider;
	height: 44px;
	align-items: center;
}
.filter-tab {
	font-size: 14px;
	color: $uni-text-color-secondary;
	font-weight: 500;
	padding-bottom: 4px;
	position: relative;
}
.filter-tab.active { color: $uni-text-color-title; font-weight: 600; }
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
.tab-badge {
	position: absolute;
	top: -4px;
	right: -12px;
	background: $uni-color-error;
	color: white;
	font-size: 10px;
	min-width: 16px;
	height: 16px;
	border-radius: 8px;
	text-align: center;
	line-height: 16px;
	padding: 0 4px;
}

.loading-wrap { padding: 16px; }

.approval-list { padding: 16px; }
.approval-card {
	background: white;
	border-radius: 12px;
	padding: 16px 16px 16px 20px;
	margin-bottom: 12px;
	box-shadow: $uni-shadow-card;
	position: relative;
	overflow: hidden;
}
.pending-card::before {
	content: '';
	position: absolute;
	top: 8px;
	left: 0;
	width: 3px;
	height: calc(100% - 16px);
	background: $uni-color-warning;
	border-radius: 0 2px 2px 0;
}
.card-header { flex-direction: row; justify-content: space-between; align-items: flex-start; margin-bottom: 10px; }
.job-info { flex: 1; }
.job-title { font-size: 16px; font-weight: 700; color: $uni-text-color-title; display: block; margin-bottom: 2px; }
.company-name { font-size: 12px; color: $uni-text-color-secondary; display: block; }
.status-tag { font-size: 12px; font-weight: 600; padding: 4px 10px; border-radius: 8px; white-space: nowrap; margin-left: 8px; }
.status-tag.pending { background: $uni-color-warning-light; color: $uni-color-warning; }
.status-tag.approved { background: $uni-color-success-light; color: $uni-color-success; }
.reject-reason-row {
	background: $uni-color-error-light;
	border-radius: 8px;
	padding: 8px 12px;
	margin-top: 8px;
}
.reject-reason-label { font-size: 12px; color: $uni-color-error; font-weight: 600; display: block; margin-bottom: 2px; }
.reject-reason-text { font-size: 13px; color: $uni-text-color; line-height: 1.5; }
.status-tag.rejected { background: $uni-color-error-light; color: $uni-color-error; }

.card-meta { flex-direction: row; flex-wrap: wrap; gap: 12px 16px; margin-bottom: 12px; font-size: 12px; color: $uni-text-color-secondary; }
.change-content { background: $uni-bg-color-page; border-radius: 8px; padding: 12px; margin-bottom: 14px; }
.change-label { font-size: 12px; font-weight: 600; color: $uni-text-color; display: block; margin-bottom: 8px; }
.change-rows { display: flex; flex-direction: column; gap: 6px; }
.change-row { flex-direction: row; align-items: flex-start; gap: 8px; }
.change-key { font-size: 12px; color: $uni-text-color-secondary; flex-shrink: 0; min-width: 70px; }
.change-value { font-size: 13px; color: $uni-text-color-title; line-height: 1.5; flex: 1; word-break: break-word; }

.card-actions { flex-direction: row; gap: 12px; }
.action-btn {
	flex: 1;
	height: 40px;
	border-radius: 8px;
	font-size: 14px;
	font-weight: 600;
	border: none;
	align-items: center;
	justify-content: center;
}
.action-btn.approve { background: $uni-gradient-primary; color: white; }
.action-btn.reject { background: white; border: 1px solid $uni-border-color; color: $uni-color-error; }
.action-btn:active { opacity: 0.85; }

.empty-state { padding: 40px; }
</style>
