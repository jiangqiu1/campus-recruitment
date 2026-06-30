<template>
	<view class="page-wrapper">
		<view class="header-simple" style="padding:12px 16px;flex-direction:row;align-items:center;gap:12px;">
			<text style="font-size:18px;font-weight:700;color:white;">岗位管理</text>
		</view>

		<!-- 顶部筛选标签 -->
		<view class="filter-tabs">
			<text v-for="(tab, i) in tabs" :key="i" class="filter-tab" :class="{ active: currentTab === tab.value }" @click="currentTab = tab.value">{{ tab.label }}</text>
		</view>

		<scroll-view class="content-scrollable" scroll-y refresher-enabled="true" :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<view class="job-list">
				<view v-for="(job, i) in filteredList" :key="i" class="card-item">
					<view class="card-header-row">
						<view>
							<text class="card-title">{{ job.title }}</text>
							<text class="card-sub">发布时间：{{ job.createTime || '--' }}</text>
						</view>
						<text class="status-tag" :class="'tag-' + job.status">{{ job.statusText || '' }}</text>
					</view>
					<view class="card-info">
						<text>📮 {{ job.deliveryCount || 0 }}人投递</text>
						<text>💰 {{ job.salaryText || '面议' }}</text>
					</view>
					<view class="card-actions">
						<button v-if="job.status === 'draft'" class="action-btn btn-blue" @click="handlePublish(job.id)">发布</button>
						<button v-if="job.status === 'active'" class="action-btn btn-orange" @click="handleClose(job.id)">下架</button>
						<button v-if="job.status === 'closed'" class="action-btn btn-blue" @click="handlePublish(job.id)">上架</button>
						<button class="action-btn btn-outline" @click="goToEdit(job)">编辑</button>
					</view>
				</view>
				<view v-if="!filteredList.length" class="empty-state">
					<text style="font-size:48px;margin-bottom:12px;">📋</text>
					<text>暂无岗位数据</text>
				</view>
			</view>
		</scroll-view>

		<!-- 底部浮动新建按钮 -->
		<view class="fab-btn" @click="goToCreate">
			<text class="fab-icon">+</text>
		</view>

		<HrTabBar current="jobs" />
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { hrAPI } from '@/utils/request'
import HrTabBar from '@/components/HrTabBar.vue'

const tabs = [
	{ label: '全部', value: 'all' },
	{ label: '已发布', value: 'active' },
	{ label: '草稿', value: 'draft' },
	{ label: '已关闭', value: 'closed' }
]
const refreshing = ref(false)
const currentTab = ref('all')
const jobs = ref([])

const onRefresh = async () => {
	refreshing.value = true
	const cId = getCompanyId()
	if (cId) await loadJobs(cId)
	refreshing.value = false
}

// 后端 Job.status Integer(0-3) → 前端 string
const STATUS_MAP = ['draft', 'active', 'closed', 'paused']
const STATUS_TEXT_MAP = ['草稿', '已发布', '已关闭', '已暂停']

// 后端 Job → 前端展示映射
const mapJob = (job) => ({
	id: job.id,
	title: job.title || '',
	createTime: job.createTime ? job.createTime.substring(0, 10) : '--',
	status: STATUS_MAP[job.status] || 'draft',
	statusText: STATUS_TEXT_MAP[job.status] || '草稿',
	deliveryCount: job._deliveryCount || 0,
	salaryText: job.salaryRange || '面议'
})

const filteredList = computed(() => {
	if (currentTab.value === 'all') return jobs.value
	return jobs.value.filter(j => j.status === currentTab.value)
})

const getCompanyId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.companyId || obj.id || null
	} catch (e) { return null }
}

const loadJobs = async (cId) => {
	try {
		const res = await hrAPI.getHrJobs(cId)
		const raw = res.data || []
		jobs.value = raw.map(mapJob)
		// 异步加载各岗位投递数
		raw.forEach(j => {
			hrAPI.getJobStats(j.id).then(sRes => {
				const stats = sRes.data
				if (stats) {
					const idx = jobs.value.findIndex(jj => jj.id === j.id)
					if (idx > -1) jobs.value[idx].deliveryCount = stats.deliveryCount || 0
				}
			}).catch(() => {})
		})
	} catch (e) {
		console.error('加载岗位列表失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

onMounted(async () => {
	const cId = getCompanyId()
	await loadJobs(cId)
})

const handlePublish = async (id) => {
	try {
		await hrAPI.publishJob(id)
		uni.showToast({ title: '发布成功', icon: 'success' })
		const idx = jobs.value.findIndex(j => j.id === id)
		if (idx > -1) {
			jobs.value[idx].status = 'active'
			jobs.value[idx].statusText = '已发布'
		}
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'none' })
	}
}

const handleClose = async (id) => {
	try {
		await hrAPI.closeJob(id)
		uni.showToast({ title: '已关闭', icon: 'success' })
		const idx = jobs.value.findIndex(j => j.id === id)
		if (idx > -1) {
			jobs.value[idx].status = 'closed'
			jobs.value[idx].statusText = '已关闭'
		}
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'none' })
	}
}

const goToEdit = (job) => {
	uni.navigateTo({ url: '/pages/hr/job-edit?id=' + job.id })
}

const goToCreate = () => {
	uni.navigateTo({ url: '/pages/hr/job-edit' })
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
	background: #0EA5E9;
	color: white;
}
.job-list {
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
.card-title {
	font-size: 16px;
	font-weight: 700;
	color: #1D2129;
	display: block;
	margin-bottom: 4px;
}
.card-sub {
	font-size: 12px;
	color: #86909C;
	display: block;
}
.status-tag {
	padding: 4px 10px;
	border-radius: 6px;
	font-size: 12px;
	font-weight: 600;
}
.tag-active { background: rgba(16,185,129,0.1); color: #10B981; }
.tag-draft { background: rgba(245,158,11,0.1); color: #F59E0B; }
.tag-closed { background: rgba(201,205,212,0.3); color: #86909C; }
.card-info {
	flex-direction: row;
	gap: 16px;
	font-size: 13px;
	color: #86909C;
	margin-bottom: 12px;
}
.card-actions {
	flex-direction: row;
	gap: 8px;
	padding-top: 12px;
	border-top: 1px solid #F2F3F5;
}
.action-btn {
	flex: 1;
	padding: 10px;
	border-radius: 10px;
	font-size: 14px;
	font-weight: 600;
	align-items: center;
	justify-content: center;
}
.btn-blue { background: #0EA5E9; color: white; }
.btn-orange { background: #F59E0B; color: white; }
.btn-outline { background: white; border: 1px solid #E2E8F0; color: #4E5969; }
.empty-state {
	padding: 60px 20px;
	align-items: center;
	color: #86909C;
	font-size: 14px;
}

/* 浮动按钮 */
.fab-btn {
	position: fixed;
	right: 24px;
	bottom: 90px;
	width: 56px;
	height: 56px;
	border-radius: 50%;
	background: linear-gradient(135deg, #0EA5E9, #38BDF8);
	align-items: center;
	justify-content: center;
	box-shadow: 0 4px 16px rgba(14,165,233,0.4);
	z-index: 100;
}
.fab-icon {
	font-size: 32px;
	color: white;
	font-weight: 300;
	margin-top: -2px;
}
</style>
