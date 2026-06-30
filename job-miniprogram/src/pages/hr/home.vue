<template>
	<view class="page-wrapper">
		<scroll-view class="content-scrollable" scroll-y>
			<!-- 顶部蓝色渐变头 -->
			<view class="header-simple">
				<view class="header-top-simple">
					<text class="header-title">企业招聘管理</text>
					<view class="header-actions">
						<view class="header-action-btn" @click="goToMessages">
							<text>🔔</text>
							<text v-if="unreadCount" class="badge">{{ unreadCount }}</text>
						</view>
					</view>
				</view>
				<text class="company-name">{{ userInfo.realName || '企业用户' }}</text>
			</view>

			<!-- 4个统计卡片 -->
			<view class="stat-row">
				<view v-for="(s, i) in statCards" :key="i" class="stat-box" :class="s.color">
					<text class="stat-icon">{{ s.icon }}</text>
					<view class="stat-info">
						<text class="stat-num">{{ dashboardData[s.key] || 0 }}</text>
						<text class="stat-label">{{ s.label }}</text>
					</view>
				</view>
			</view>

			<!-- 近期投递列表 -->
			<view class="section">
				<view class="section-header">
					<text class="section-title">📮 近期投递</text>
					<text class="section-more" @click="goToDeliveries">查看全部 ›</text>
				</view>
				<view v-for="(item, i) in recentDeliveries" :key="i" class="card-item" @click="goToDeliveryDetail(item.id)">
					<view class="card-header-row">
						<view>
							<text class="card-title">{{ item.studentName || '候选人' }}</text>
							<text class="card-sub">{{ item.jobTitle || '岗位名称' }}</text>
						</view>
						<text class="status-tag" :class="'tag-' + item.status">{{ item.statusText || '' }}</text>
					</view>
					<text class="card-time">{{ item.createTime || '' }}</text>
				</view>
				<view v-if="!recentDeliveries.length" class="empty-state">
					<text style="font-size:40px;margin-bottom:8px;">📭</text>
					<text>暂无投递记录</text>
				</view>
			</view>

			<!-- 待处理事项 -->
			<view class="section">
				<view class="section-header">
					<text class="section-title">📋 待处理事项</text>
				</view>
				<view v-for="(task, i) in pendingTasks" :key="i" class="task-item" @click="handleTask(task)">
					<view class="task-dot" :class="task.color"></view>
					<view class="task-content">
						<text class="task-title">{{ task.title }}</text>
						<text class="task-desc">{{ task.desc }}</text>
					</view>
					<text class="task-count" :class="task.color">{{ task.count }}</text>
				</view>
				<view v-if="!pendingTasks.length" class="empty-state">
					<text style="font-size:40px;margin-bottom:8px;">✅</text>
					<text>暂无待处理事项</text>
				</view>
			</view>
		</scroll-view>
		<HrTabBar current="home" />
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { hrAPI } from '@/utils/request'
import { checkRole } from '@/utils/auth'
import HrTabBar from '@/components/HrTabBar.vue'

// 角色路由锁 — 仅HR(2)可访问
checkRole(2)

const userInfo = ref({})
const unreadCount = ref(0)
const dashboardData = ref({})
const recentDeliveries = ref([])
const pendingTasks = ref([])

const statCards = [
	{ key: 'jobCount', label: '岗位数', icon: '📋', color: 'blue' },
	{ key: 'resumeCount', label: '简历数', icon: '📄', color: 'green' },
	{ key: 'interviewCount', label: '面试数', icon: '📞', color: 'orange' },
	{ key: 'offerCount', label: '录用数', icon: '✅', color: 'purple' }
]



const getCompanyId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.companyId || obj.id || null
	} catch (e) { return null }
}

onMounted(async () => {
	try {
		const stored = uni.getStorageSync('userInfo')
		if (stored) userInfo.value = JSON.parse(stored)
	} catch (e) {}
	await loadDashboard()
	await loadDeliveries()
	loadTasks()
})

const loadDashboard = async () => {
	try {
		const cId = getCompanyId()
		const res = await hrAPI.getDashboard(cId)
		dashboardData.value = res.data || {}
	} catch (e) {
		console.error('加载dashboard失败', e)
	}
}

const loadDeliveries = async () => {
	try {
		const cId = getCompanyId()
		// getCompanyDeliveries takes jobId, not companyId — iterate through jobs
		const jobsRes = await hrAPI.getHrJobs(cId)
		const jobs = jobsRes.data || []
		const all = []
		for (const job of jobs) {
			try {
				const dRes = await hrAPI.getCompanyDeliveries(job.id)
				all.push(...(dRes.data || []))
			} catch (e) { }
		}
		recentDeliveries.value = all.slice(0, 5)
	} catch (e) {
		console.error('加载投递列表失败', e)
	}
}

const loadTasks = () => {
	// 从后台统计中衍生待办事项
	const d = dashboardData.value
	const tasks = []
	if (d.resumeCount > 0) tasks.push({ title: '待查看简历', desc: '新增简历等待查看处理', count: d.resumeCount, color: 'blue' })
	if (d.interviewCount > 0) tasks.push({ title: '待安排面试', desc: '需安排面试时间', count: d.interviewCount, color: 'orange' })
	pendingTasks.value = tasks
}

const handleTask = (task) => {
	if (task.title.includes('简历')) {
		uni.switchTab({ url: '/pages/hr/deliveries' })
	} else if (task.title.includes('面试')) {
		uni.switchTab({ url: '/pages/hr/deliveries' })
	} else {
		uni.switchTab({ url: '/pages/hr/deliveries' })
	}
}

const goToDeliveries = () => {
	uni.switchTab({ url: '/pages/hr/deliveries' })
}

const goToDeliveryDetail = (id) => {
	uni.navigateTo({ url: '/pages/hr/delivery-detail?id=' + id })
}

const goToMessages = () => {
	uni.navigateTo({ url: '/pages/hr/messages' })
}
</script>

<style scoped>
.header-simple {
	background: linear-gradient(135deg, #0EA5E9 0%, #38BDF8 100%);
	color: white;
	padding: 20px 16px 24px;
	position: relative;
	overflow: hidden;
	flex-shrink: 0;
}
.header-simple::before {
	content: '';
	position: absolute;
	top: -50%;
	right: -20%;
	width: 200px;
	height: 200px;
	background: rgba(255,255,255,0.08);
	border-radius: 50%;
}
.header-top-simple {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	position: relative;
	z-index: 1;
	margin-bottom: 12px;
}
.header-title {
	font-size: 20px;
	font-weight: 700;
}
.header-actions {
	flex-direction: row;
	gap: 12px;
}
.header-action-btn {
	width: 36px;
	height: 36px;
	border-radius: 50%;
	background: rgba(255,255,255,0.15);
	align-items: center;
	justify-content: center;
	font-size: 18px;
	position: relative;
}
.badge {
	position: absolute;
	top: -2px;
	right: -2px;
	width: 18px;
	height: 18px;
	background: #EF4444;
	border-radius: 50%;
	font-size: 10px;
	align-items: center;
	justify-content: center;
	border: 2px solid #0EA5E9;
	color: white;
	font-weight: 600;
}
.company-name {
	font-size: 15px;
	opacity: 0.9;
	position: relative;
	z-index: 1;
}

/* 统计卡片 */
.stat-row {
	flex-direction: row;
	flex-wrap: wrap;
	gap: 12px;
	padding: 12px 16px;
}
.stat-box {
	flex: 1;
	min-width: calc(50% - 6px);
	background: white;
	border-radius: 16px;
	padding: 16px;
	flex-direction: row;
	align-items: center;
	gap: 12px;
	position: relative;
	overflow: hidden;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.stat-box::before {
	content: '';
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 3px;
	background: linear-gradient(90deg, #0EA5E9, transparent);
}
.stat-box.green::before { background: linear-gradient(90deg, #10B981, transparent); }
.stat-box.orange::before { background: linear-gradient(90deg, #F59E0B, transparent); }
.stat-box.purple::before { background: linear-gradient(90deg, #8B5CF6, transparent); }
.stat-icon {
	font-size: 32px;
}
.stat-info {
	flex: 1;
}
.stat-num {
	font-size: 24px;
	font-weight: 800;
	color: #1D2129;
}
.stat-label {
	font-size: 12px;
	color: #86909C;
	margin-top: 2px;
}

/* 通用section */
.section {
	padding: 0 16px;
	margin-bottom: 12px;
}
.section-header {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 12px;
}
.section-title {
	font-size: 17px;
	font-weight: 700;
	color: #1D2129;
}
.section-more {
	font-size: 13px;
	color: #0EA5E9;
	font-weight: 500;
}

/* 近期投递卡片 */
.card-item {
	background: white;
	border-radius: 16px;
	padding: 16px;
	margin-bottom: 10px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.card-header-row {
	flex-direction: row;
	justify-content: space-between;
	align-items: flex-start;
	margin-bottom: 6px;
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
.card-time {
	font-size: 12px;
	color: #C9CDD4;
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

/* 待处理事项 */
.task-item {
	background: white;
	border-radius: 16px;
	padding: 16px;
	margin-bottom: 10px;
	flex-direction: row;
	align-items: center;
	gap: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.task-dot {
	width: 8px;
	height: 8px;
	border-radius: 50%;
	flex-shrink: 0;
}
.task-dot.blue { background: #0EA5E9; }
.task-dot.green { background: #10B981; }
.task-dot.orange { background: #F59E0B; }
.task-content {
	flex: 1;
}
.task-title {
	font-size: 14px;
	font-weight: 600;
	color: #1D2129;
	margin-bottom: 2px;
}
.task-desc {
	font-size: 12px;
	color: #86909C;
}
.task-count {
	font-size: 18px;
	font-weight: 800;
}
.task-count.blue { color: #0EA5E9; }
.task-count.green { color: #10B981; }
.task-count.orange { color: #F59E0B; }

.empty-state {
	padding: 30px;
	align-items: center;
	color: #86909C;
	font-size: 14px;
}
</style>
