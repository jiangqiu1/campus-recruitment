<template>
	<view class="page-wrapper">
		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="isRefreshing" @refresherrefresh="onRefresh">
			<!-- 1. 顶部渐变头部（对齐教师端结构，增加角色徽章） -->
			<view class="header-section" :style="{ paddingTop: (statusBarHeight + 16) + 'px' }">
				<view class="header-top">
					<view class="header-greeting">
						<text class="greeting">{{ greeting }}，{{ userInfo.realName || 'HR用户' }}</text>
						<text class="role-badge">企业招聘方</text>
					</view>
				</view>
				<!-- 今日任务句（角色人格：专业、直接、决策导向） -->
				<view class="header-task">
					<text class="header-task-text">今日任务：{{ formatNum(dashboard.todayInterviewCount) }} 场面试 · {{ formatNum(dashboard.pendingResumeCount) }} 份简历待筛选</text>
				</view>
				<!-- 招聘概览 -->
				<view class="header-stats">
					<view class="hs-item">
						<text class="hs-num">{{ formatNum(dashboard.resumeCount) }}</text>
						<text class="hs-label">收到投递</text>
					</view>
					<view class="hs-divider" />
					<view class="hs-item">
						<text class="hs-num">{{ formatNum(dashboard.todayNewCount) }}</text>
						<text class="hs-label">今日新增</text>
					</view>
					<view class="hs-divider" />
					<view class="hs-item">
						<text class="hs-num">{{ formatNum(dashboard.todayInterviewCount) }}</text>
						<text class="hs-label">待面试</text>
					</view>
				</view>
			</view>

			<!-- 2. 2×2 待办数据网格（对齐教师端 todo-grid 规范） -->
			<view class="todo-grid">
				<view class="todo-card" @click="goToDeliveries('pending')">
					<view class="todo-top">
						<text class="todo-num">{{ formatNum(dashboard.pendingResumeCount) }}</text>
						<view class="todo-icon todo-icon-yellow"><uni-icons type="paperplane" size="18" color="#FF7D00" /></view>
					</view>
					<text class="todo-label">待处理简历</text>
				</view>
				<view class="todo-card" @click="goToDeliveries('today')">
					<view class="todo-top">
						<text class="todo-num">{{ formatNum(dashboard.todayNewCount) }}</text>
						<view class="todo-icon todo-icon-blue"><uni-icons type="bars" size="18" color="#165DFF" /></view>
					</view>
					<text class="todo-label">今日新增</text>
				</view>
				<view class="todo-card" @click="goToInterviews">
					<view class="todo-top">
						<text class="todo-num">{{ formatNum(dashboard.todayInterviewCount) }}</text>
						<view class="todo-icon todo-icon-orange"><uni-icons type="calendar" size="18" color="#FF7D00" /></view>
					</view>
					<text class="todo-label">今日面试</text>
				</view>
				<view class="todo-card" @click="goToJobs">
					<view class="todo-top">
						<text class="todo-num">{{ formatNum(dashboard.activeJobCount) }}</text>
						<view class="todo-icon todo-icon-green"><uni-icons type="list" size="18" color="#00B42A" /></view>
					</view>
					<text class="todo-label">在招岗位</text>
				</view>
			</view>

			<!-- 3. 快捷工具栏 -->
			<view class="quick-actions">
				<view class="quick-action-item" @click="goToJobs">
					<uni-icons type="list" size="18" color="#165DFF" />
					<text class="quick-action-label">管理岗位</text>
				</view>
				<view class="quick-action-item" @click="goToStats">
					<uni-icons type="bars" size="18" color="#00B42A" />
					<text class="quick-action-label">数据统计</text>
				</view>
				<view class="quick-action-item" @click="goToMessages">
					<uni-icons type="chat" size="18" color="#FF7D00" />
					<text class="quick-action-label">消息通知</text>
				</view>
			</view>

			<!-- 4. 近期动态 -->
			<view class="list-section">
				<view class="section-header">
					<text class="section-title">近期动态</text>
				</view>
				<view v-for="(item, i) in recentList" :key="i" class="compact-item" @click="goToDetail(item.id)">
					<text class="compact-name">{{ item.studentName || '候选人' }}</text>
					<text class="compact-action">投递了</text>
					<text class="compact-job">{{ item.jobTitle || '' }}</text>
					<text class="status-tag" :class="'tag-' + item.status">{{ item.statusText || '' }}</text>
				</view>
				<EmptyState v-if="!recentList.length" icon="inbox" title="暂无动态" desc="有新的投递或操作会出现在这里" />
			</view>

			<view style="height: calc(60px + env(safe-area-inset-bottom))" />
		</scroll-view>
		<HrTabBar current="home" />
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@/utils/page-lifecycle'
import { hrAPI } from '@/utils/request'
import { checkRole } from '@/utils/auth'
import HrTabBar from '@/components/HrTabBar.vue'
import EmptyState from '@/components/EmptyState.vue'

checkRole(2)

const userInfo = ref({})
const isRefreshing = ref(false)
const statusBarHeight = ref(0)

const dashboard = ref({
	pendingResumeCount: 0,
	todayNewCount: 0,
	todayInterviewCount: 0,
	activeJobCount: 0
})
const recentList = ref([])

const greeting = computed(() => {
	const h = new Date().getHours()
	if (h < 9) return '早上好'
	if (h < 12) return '上午好'
	if (h < 14) return '中午好'
	if (h < 18) return '下午好'
	return '晚上好'
})

onMounted(() => {
	try {
		const sysInfo = uni.getWindowInfo()
		statusBarHeight.value = sysInfo.statusBarHeight || 0
	} catch (e) {
		// 降级：部分旧版环境可能不支持 getWindowInfo
		try {
			const sysInfo = uni.getSystemInfoSync()
			statusBarHeight.value = sysInfo.statusBarHeight || 0
		} catch (e2) {}
	}
	try {
		const stored = uni.getStorageSync('userInfo')
		if (stored) userInfo.value = JSON.parse(stored)
	} catch (e) {}
})

onShow(() => { loadAllData() })

const onRefresh = async () => {
	isRefreshing.value = true
	await loadAllData()
	isRefreshing.value = false
}

const loadAllData = async () => {
	const cId = getCompanyId()
	if (!cId) return

	try {
		const dashRes = await hrAPI.getDashboard(cId)
		dashboard.value = dashRes.data || {}
		// 加载近期动态（最近的投递记录）
		const all = await fetchAllDeliveries(cId)
		all.sort((a, b) => new Date(b.createTime || 0) - new Date(a.createTime || 0))
		recentList.value = all.slice(0, 5)
	} catch (e) {
		console.error('首页数据加载失败', e)
	}
}

const fetchAllDeliveries = async (companyId) => {
	const jobsRes = await hrAPI.getHrJobs(companyId)
	const jobs = jobsRes.data || []
	const all = []
	for (const job of jobs) {
		try {
			const dRes = await hrAPI.getCompanyDeliveries(job.id)
			const list = (dRes.data || []).map(item => ({ ...item, jobTitle: job.title }))
			all.push(...list)
		} catch (e) { console.error('获取投递列表失败', e) }
	}
	return all
}

const goToDeliveries = (filter) => {
	const url = filter ? '/pages/hr/deliveries?filter=' + filter : '/pages/hr/deliveries'
	uni.navigateTo({ url })
}
const goToDetail = (id) => uni.navigateTo({ url: '/pages/hr/delivery-detail?id=' + id })
const goToInterviews = () => uni.reLaunch({ url: '/pages/hr/interviews' })
const goToJobs = () => uni.reLaunch({ url: '/pages/hr/jobs' })
const goToMessages = () => uni.navigateTo({ url: '/pages/hr/messages' })
const goToStats = () => uni.navigateTo({ url: '/pages/hr/stats' })

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

const formatNum = (num) => {
	if (!num && num !== 0) return 0
	return num > 99 ? '99+' : num
}

const formatTime = (time, type) => {
	if (!time) return '--'
	if (type === 'time') return time.substring(11, 16)
	return time.substring(5, 16)
}
</script>

<style scoped lang="scss">
/* ===== 头部（完全对齐教师端） ===== */
.header-section {
	position: relative;
	overflow: hidden;
	background: $uni-gradient-hero;
	padding: 16px 16px 32px;
	flex-shrink: 0;
}
.header-section::after {
	content: '';
	position: absolute;
	top: -30px;
	right: -30px;
	width: 160px;
	height: 160px;
	border-radius: 50%;
	background: rgba(255, 255, 255, 0.1);
	pointer-events: none;
}
.header-top {
	position: relative;
	z-index: 1;
	flex-direction: row;
	justify-content: space-between;
	align-items: flex-start;
}
.header-greeting { flex: 1; }
.greeting {
	font-size: 20px;
	font-weight: 700;
	color: $uni-text-color-inverse;
	margin-bottom: 8px;
}
.role-badge {
	display: inline-block;
	font-size: 12px;
	color: rgba(255,255,255,0.9);
	background: rgba(255,255,255,0.15);
	padding: 4px 12px;
	border-radius: 999px;
	font-weight: 500;
}

/* 招聘概览（头部内半透明指标行，对齐教师端） */
.header-stats {
	flex-direction: row;
	align-items: center;
	margin-top: 16px;
	background: rgba(255,255,255,0.14);
	border: 1px solid rgba(255,255,255,0.2);
	border-radius: 12px;
	padding: 12px 0;
	position: relative;
	z-index: 1;
}
.header-task {
	margin-top: 12px;
	position: relative;
	z-index: 1;
}
.header-task-text {
	font-size: 13px;
	color: rgba(255,255,255,0.9);
}
.hs-item {
	flex: 1;
	align-items: center;
	gap: 2px;
}
.hs-num {
	font-size: 20px;
	font-weight: 800;
	color: $uni-text-color-inverse;
}
.hs-label {
	font-size: 11px;
	color: rgba(255,255,255,0.8);
}
.hs-divider {
	width: 0.5px;
	height: 28px;
	background: rgba(255,255,255,0.25);
}
.header-action-btn {
	width: 36px;
	height: 36px;
	border-radius: 50%;
	background: rgba(255,255,255,0.15);
	align-items: center;
	justify-content: center;
	position: relative;
}
.badge {
	position: absolute;
	top: -2px;
	right: -2px;
	width: 18px;
	height: 18px;
	background: $uni-color-error;
	border-radius: 50%;
	font-size: 10px;
	align-items: center;
	justify-content: center;
	border: 2px solid $uni-color-primary;
	color: white;
	font-weight: 600;
}

/* ===== 2×2 待办网格（对齐教师端 todo-grid，卡片上移进入蓝色区域） ===== */
.todo-grid {
	flex-direction: row;
	flex-wrap: wrap;
	padding: 0 16px;
	gap: 12px;
	margin-top: -16px;
	position: relative;
	z-index: 10;
}
.todo-card {
	width: calc(50% - 6px);
	background: $uni-bg-color;
	border-radius: 12px;
	padding: 16px;
	box-shadow: $uni-shadow-card;
}
.todo-card:active { background: $uni-bg-color-page; }
.todo-top {
	flex-direction: row;
	justify-content: space-between;
	align-items: flex-start;
	margin-bottom: 8px;
}
.todo-num {
	font-size: 24px;
	font-weight: 800;
	color: $uni-text-color-title;
}
.todo-icon {
	width: 36px;
	height: 36px;
	border-radius: 12px;
	align-items: center;
	justify-content: center;
}
.todo-icon-yellow { background: $uni-color-warning-light; }
.todo-icon-blue { background: $uni-color-primary-light; }
.todo-icon-orange { background: $uni-color-warning-light; }
.todo-icon-green { background: $uni-color-success-light; }
.todo-label {
	font-size: 13px;
	color: $uni-text-color;
	font-weight: 500;
}

/* ===== 快捷工具栏 ===== */
.quick-actions {
	flex-direction: row;
	padding: 0 16px;
	margin-top: 16px;
	margin-bottom: 12px;
}
.quick-action-item {
	flex: 1;
	flex-direction: row;
	align-items: center;
	justify-content: center;
	gap: 6px;
	height: 44px;
	background: $uni-bg-color;
	border-radius: 12px;
	box-shadow: $uni-shadow-card;
}
.quick-action-item + .quick-action-item {
	margin-left: 12px;
}
.quick-action-item:active { background: $uni-bg-color-page; }
.quick-action-label {
	font-size: 13px;
	color: $uni-text-color;
	font-weight: 500;
}

/* ===== 模块标题（对齐教师端） ===== */
.list-section {
	padding: 0 16px;
	margin-bottom: 12px;
}
.list-section-secondary {
	padding: 0 16px;
	margin-bottom: 4px;
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
	color: $uni-text-color-title;
	letter-spacing: 0.02em;
}
.section-title-secondary {
	font-size: 15px;
	font-weight: 600;
	color: $uni-text-color;
}
.section-more {
	font-size: 13px;
	color: $uni-color-primary;
	font-weight: 500;
}

/* ===== 待处理简历卡片（Signature 元素：左侧蓝色指示条） ===== */
.resume-card {
	background: $uni-bg-color;
	border-radius: 12px;
	padding: 14px 14px 14px 18px;
	margin-bottom: 10px;
	box-shadow: 0 2px 12px $uni-color-primary-light;
	position: relative;
	overflow: hidden;
}
.resume-card::before {
	content: '';
	position: absolute;
	top: 8px;
	left: 0;
	width: 3px;
	height: calc(100% - 16px);
	background: $uni-color-primary;
	border-radius: 0 2px 2px 0;
}
.resume-card:active { background: $uni-bg-color-page; }
.card-top {
	flex-direction: row;
	align-items: center;
	gap: 10px;
	margin-bottom: 12px;
}
.avatar {
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
.card-info { flex: 1; }
.info-row {
	flex-direction: row;
	align-items: center;
	gap: 8px;
	margin-bottom: 2px;
}
.name { font-size: 15px; font-weight: 700; color: $uni-text-color-title; letter-spacing: 0.01em; }
.score-tag {
	font-size: 12px;
	padding: 2px 6px;
	border-radius: 4px;
	background: $uni-color-ai-light;
	color: $uni-color-ai;
	font-weight: 600;
}
.sub-info { font-size: 12px; color: $uni-text-color-secondary; }
.time { font-size: 12px; color: $uni-text-color-placeholder; flex-shrink: 0; }

/* 操作：标签式轻量化按钮 */
.card-actions { flex-direction: row; gap: 10px; }
.action-btn {
	flex: 1;
	text-align: center;
	padding: 7px 0;
	border-radius: 8px;
	font-size: 12px;
	font-weight: 600;
}
.action-btn.primary { background: $uni-color-primary; color: $uni-text-color-inverse; box-shadow: 0 2px 6px $uni-color-primary-light; }
.action-btn.danger {
	background: $uni-bg-color;
	border: 1px solid $uni-color-error;
	color: $uni-color-error;
}

/* ===== 面试卡片 ===== */
.interview-card {
	background: $uni-bg-color;
	border-radius: 12px;
	padding: 14px;
	margin-bottom: 8px;
	flex-direction: row;
	align-items: center;
	gap: 12px;
	box-shadow: $uni-shadow-card;
}
.interview-card:active { background: $uni-bg-color-page; }
.interview-icon {
	width: 36px;
	height: 36px;
	border-radius: 12px;
	background: $uni-color-ai-light;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}
.interview-info { flex: 1; }
.interview-name {
	font-size: 14px;
	font-weight: 600;
	color: $uni-text-color-title;
	display: block;
	margin-bottom: 2px;
}
.interview-job { font-size: 12px; color: $uni-text-color-secondary; }
.interview-time {
	font-size: 14px;
	font-weight: 600;
	color: $uni-color-primary;
	flex-shrink: 0;
}

/* ===== 近期投递（紧凑降权重） ===== */
.compact-item {
	background: $uni-bg-color;
	border-radius: 12px;
	padding: 12px 14px;
	margin-bottom: 6px;
	flex-direction: row;
	align-items: center;
	box-shadow: 0 1px 4px rgba(0,0,0,0.03);
}
.compact-item:active { background: $uni-bg-color-page; }
.compact-name {
	font-size: 14px;
	font-weight: 500;
	color: $uni-text-color-title;
	flex-shrink: 0;
}
.compact-action {
	font-size: 12px;
	color: $uni-text-color;
	margin-left: 4px;
	margin-right: 4px;
	flex-shrink: 0;
}
.compact-job {
	font-size: 14px;
	color: $uni-text-color;
	flex: 1;
	margin-right: 8px;
}
.status-tag {
	padding: 3px 8px;
	border-radius: 8px;
	font-size: 12px;
	font-weight: 600;
	flex-shrink: 0;
}
.tag-pending { background: $uni-color-warning-light; color: $uni-color-warning; }
.tag-viewed { background: $uni-color-primary-light; color: $uni-color-primary; }
.tag-interview { background: $uni-color-primary-light; color: $uni-color-primary; }
.tag-accepted { background: $uni-color-success-light; color: $uni-color-success; }
.tag-rejected { background: $uni-color-error-light; color: $uni-color-error; }
</style>
