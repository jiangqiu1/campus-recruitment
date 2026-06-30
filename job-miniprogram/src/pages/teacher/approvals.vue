<template>
	<view class="page-wrapper">
		<view class="header-simple">
			<view class="header-top">
				<text class="back-btn" @click="goBack">‹</text>
				<text class="header-title">审批管理</text>
			</view>
			<view class="tab-bar">
				<text v-for="(tab, i) in tabs" :key="i"
					class="tab-item" :class="{ active: currentTab === tab.value }"
					@click="switchTab(tab.value)">
					<text>{{ tab.label }}</text>
					<text v-if="tab.badge > 0" class="tab-badge">{{ tab.badge }}</text>
				</text>
			</view>
		</view>
		<scroll-view class="content-scrollable" scroll-y @scrolltolower="loadMore">
			<view v-if="loading" class="empty-state">
				<text style="font-size:48px;margin-bottom:12px;">⏳</text>
				<text class="empty-text">加载中...</text>
			</view>
			<view v-else-if="!list.length" class="empty-state">
				<text style="font-size:48px;margin-bottom:12px;">✅</text>
				<text class="empty-text">暂无{{ currentTab === 0 ? '待审核' : currentTab === 1 ? '已通过' : '已拒绝' }}的申请</text>
			</view>
			<view v-else class="approval-list">
				<view v-for="(item, i) in list" :key="item.id" class="approval-card">
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
						<text class="meta-item">🆔 申请 #{{ item.id }}</text>
						<text class="meta-item">📅 {{ item.applyTime || '—' }}</text>
					</view>
					<view v-if="item.changeContent" class="change-content">
						<text class="change-label">变更内容：</text>
						<text class="change-text">{{ item.changeContent }}</text>
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
import { ref, onMounted, computed } from 'vue'
import { teacherAPI } from '@/utils/request'

const currentTab = ref(0)
const list = ref([])
const loading = ref(true)
const page = ref(1)

const tabs = computed(() => [
	{ label: '待审核', value: 0, badge: pendingCount.value },
	{ label: '已通过', value: 1, badge: 0 },
	{ label: '已拒绝', value: 2, badge: 0 },
])
const pendingCount = ref(0)

const statusClass = (s) => {
	const map = { 0: 'pending', 1: 'approved', 2: 'rejected' }
	return map[s] || ''
}
const statusLabel = (s) => {
	const map = { 0: '⏳ 待审核', 1: '✅ 已通过', 2: '❌ 已拒绝' }
	return map[s] || '未知'
}

onMounted(async () => {
	await loadList()
	loading.value = false
})

const loadList = async () => {
	try {
		const res = await teacherAPI.getApprovals(currentTab.value)
		list.value = (res.data || []).map(item => ({
			...item,
			applyTime: item.applyTime ? item.applyTime.replace('T', ' ').substring(0, 19) : '—'
		}))
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

const handleApprove = async (item) => {
	try {
		await teacherAPI.approveJobChange(item.id)
		uni.showToast({ title: '已通过', icon: 'success' })
		list.value = list.value.filter(x => x.id !== item.id)
	} catch (e) {
		console.error('审批通过失败:', e)
		uni.showToast({ title: '操作失败', icon: 'none' })
	}
}

const handleReject = async (item) => {
	try {
		await teacherAPI.rejectJobChange(item.id)
		uni.showToast({ title: '已拒绝', icon: 'success' })
		list.value = list.value.filter(x => x.id !== item.id)
	} catch (e) {
		console.error('拒绝失败:', e)
		uni.showToast({ title: '操作失败', icon: 'none' })
	}
}

const goBack = () => {
	uni.navigateBack()
}

const loadMore = () => {
	// 后端暂不分页，无更多逻辑
}
</script>

<style scoped>
.header-simple {
	background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
	color: white;
	padding: 12px 16px;
	flex-shrink: 0;
}
.header-top {
	flex-direction: row;
	align-items: center;
	gap: 12px;
	margin-bottom: 16px;
}
.back-btn {
	font-size: 24px;
	font-weight: 700;
	width: 36px;
	height: 36px;
	border-radius: 50%;
	background: rgba(255,255,255,0.15);
	text-align: center;
	line-height: 36px;
}
.header-title {
	font-size: 18px;
	font-weight: 700;
}
.tab-bar {
	flex-direction: row;
	gap: 8px;
}
.tab-item {
	padding: 8px 18px;
	border-radius: 20px;
	font-size: 13px;
	font-weight: 500;
	color: rgba(255,255,255,0.8);
	background: rgba(255,255,255,0.15);
	position: relative;
}
.tab-item.active {
	background: white;
	color: #10B981;
	font-weight: 600;
}
.tab-badge {
	position: absolute;
	top: -4px;
	right: -4px;
	background: #EF4444;
	color: white;
	font-size: 10px;
	min-width: 16px;
	height: 16px;
	border-radius: 8px;
	text-align: center;
	line-height: 16px;
	padding: 0 4px;
}
.approval-list {
	padding: 16px;
}
.approval-card {
	background: white;
	border-radius: 16px;
	padding: 16px;
	margin-bottom: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
	position: relative;
	overflow: hidden;
}
.approval-card::before {
	content: '';
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 3px;
}
.approval-card::before { background: linear-gradient(90deg, #10B981, transparent); }
.card-header {
	flex-direction: row;
	justify-content: space-between;
	align-items: flex-start;
	margin-bottom: 10px;
}
.job-info { flex: 1; }
.job-title {
	font-size: 16px;
	font-weight: 700;
	color: #1D2129;
	display: block;
	margin-bottom: 2px;
}
.company-name {
	font-size: 12px;
	color: #86909C;
	display: block;
}
.status-tag {
	font-size: 12px;
	font-weight: 600;
	padding: 4px 10px;
	border-radius: 8px;
	white-space: nowrap;
	margin-left: 8px;
}
.status-tag.pending { background: #FEF3C7; color: #D97706; }
.status-tag.approved { background: #D1FAE5; color: #059669; }
.status-tag.rejected { background: #FEE2E2; color: #DC2626; }
.card-meta {
	flex-direction: row;
	gap: 16px;
	margin-bottom: 12px;
}
.meta-item {
	font-size: 12px;
	color: #86909C;
}
.change-content {
	background: #F8F9FC;
	border-radius: 10px;
	padding: 12px;
	margin-bottom: 14px;
}
.change-label {
	font-size: 12px;
	font-weight: 600;
	color: #4E5969;
	display: block;
	margin-bottom: 4px;
}
.change-text {
	font-size: 13px;
	color: #1D2129;
	line-height: 1.5;
}
.card-actions {
	flex-direction: row;
	gap: 12px;
}
.action-btn {
	flex: 1;
	height: 40px;
	border-radius: 10px;
	font-size: 14px;
	font-weight: 600;
	border: none;
	align-items: center;
	justify-content: center;
}
.action-btn.approve {
	background: linear-gradient(135deg, #10B981, #34D399);
	color: white;
}
.action-btn.reject {
	background: #FEE2E2;
	color: #DC2626;
}
.action-btn:active {
	opacity: 0.85;
	transform: scale(0.97);
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
