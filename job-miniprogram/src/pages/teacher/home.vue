<template>
	<view class="page-wrapper">
		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<!-- 蓝色渐变头部 -->
			<view class="header-section" :style="{ paddingTop: (statusBarHeight + 16) + 'px' }">
				<view class="header-top">
					<view class="header-greeting">
						<text class="greeting">您好，{{ userInfo.realName || '教师用户' }}</text>
						<text class="role-badge">就业指导老师</text>
					</view>
				</view>
				<!-- 本班概览 -->
				<view class="header-stats">
					<view class="hs-item">
						<text class="hs-num">{{ dashboard.pendingApprovalCount || 0 }}</text>
						<text class="hs-label">待审核简历</text>
					</view>
					<view class="hs-divider" />
					<view class="hs-item">
						<text class="hs-num">{{ dashboard.studentCount || 0 }}</text>
						<text class="hs-label">班级学生</text>
					</view>
					<view class="hs-divider" />
					<view class="hs-item">
						<text class="hs-num">{{ dashboard.todayDeliveryCount || 0 }}</text>
						<text class="hs-label">今日新增投递</text>
					</view>
				</view>
			</view>

			<!-- 待办快捷区（2×2卡片，替换原快捷入口） -->
			<view class="todo-grid">
				<view class="todo-card" @click="goToApprovals">
					<view class="todo-top">
						<text class="todo-num" :class="{ zero: !(dashboard.pendingApprovalCount || 0) }">{{ (dashboard.pendingApprovalCount || 0) > 99 ? '99+' : dashboard.pendingApprovalCount || 0 }}</text>
						<view class="todo-icon"><uni-icons type="auth" size="18" color="#FF7D00" /></view>
					</view>
					<text class="todo-label">待审批</text>
				</view>
				<view class="todo-card" @click="goToDeliveries">
					<view class="todo-top">
						<text class="todo-num" :class="{ zero: !(dashboard.todayDeliveryCount || 0) }">{{ (dashboard.todayDeliveryCount || 0) > 99 ? '99+' : dashboard.todayDeliveryCount || 0 }}</text>
						<view class="todo-icon"><uni-icons type="bars" size="18" color="#165DFF" /></view>
					</view>
					<text class="todo-label">今日新增投递</text>
				</view>
				<view class="todo-card" @click="goToResumes">
					<view class="todo-top">
						<text class="todo-num" :class="{ zero: !(dashboard.unreadResumeCount || 0) }">{{ (dashboard.unreadResumeCount || 0) > 99 ? '99+' : dashboard.unreadResumeCount || 0 }}</text>
						<view class="todo-icon"><uni-icons type="paperplane" size="18" color="#00B42A" /></view>
					</view>
					<text class="todo-label">未读简历</text>
				</view>
				<view class="todo-card" @click="goToUrgentJobs">
					<view class="todo-top">
						<text class="todo-num" :class="{ zero: !(dashboard.urgentJobCount || 0) }">{{ (dashboard.urgentJobCount || 0) > 99 ? '99+' : dashboard.urgentJobCount || 0 }}</text>
						<view class="todo-icon"><uni-icons type="star" size="18" color="#F53F3F" /></view>
					</view>
					<text class="todo-label">急招岗位</text>
				</view>
			</view>

			<!-- 近期动态 -->
			<view class="list-section">
				<view class="list-tabs">
					<text class="list-tab active">近期动态</text>
				</view>
				<view class="activity-list">
					<view v-for="(act, i) in activities" :key="i" class="activity-item" @click="goToActivityDetail(act)">
						<view class="activity-dot" :class="act.type"></view>
						<view class="activity-content">
							<text class="activity-text">{{ act.text }}</text>
							<text class="activity-time">{{ act.time }}</text>
						</view>
					</view>
					<EmptyState v-if="!activities.length" icon="inbox" title="暂无动态" />
				</view>
			</view>
		</scroll-view>
		<view style="height: calc(50px + env(safe-area-inset-bottom))"></view>
		<TabBar current="home" path-prefix="/pages/teacher/" :tab-list="teacherTabs" />
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { teacherAPI } from '@/utils/request'
import { checkRole } from '@/utils/auth'
import TabBar from '@/components/TabBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const teacherTabs = [
	{ page: 'home', icon: 'home', activeIcon: 'home-filled', label: '首页' },
	{ page: 'classes', icon: 'staff', activeIcon: 'staff-filled', label: '班级' },
	{ page: 'jobs', icon: 'list', activeIcon: 'list', label: '岗位' },
	{ page: 'profile', icon: 'person', activeIcon: 'person-filled', label: '我的' }
]

checkRole(1)

const userInfo = ref({})
const dashboard = ref({})
const activities = ref([])
const refreshing = ref(false)
const statusBarHeight = ref(0)

onMounted(() => {
	try {
		const winInfo = uni.getWindowInfo()
		statusBarHeight.value = winInfo.statusBarHeight || 0
	} catch (e) {
		try {
			const sysInfo = uni.getSystemInfoSync()
			statusBarHeight.value = sysInfo.statusBarHeight || 0
		} catch (e2) { console.error('获取状态栏高度失败', e2) }
	}
	try {
		const stored = uni.getStorageSync('userInfo')
		if (stored) userInfo.value = JSON.parse(stored)
	} catch (e) { console.error('获取用户信息失败', e) }
	loadDashboard()
})

const onRefresh = async () => {
	refreshing.value = true
	await loadDashboard()
	refreshing.value = false
}

const loadDashboard = async () => {
	try {
		const res = await teacherAPI.getDashboard()
		const d = res.data || {}
		dashboard.value = d
		activities.value = d.recentActivities || []
	} catch (e) {
		console.error('加载dashboard失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

const goToDeliveries = () => uni.navigateTo({ url: '/pages/teacher/deliveries?filter=today' })
const goToApprovals = () => uni.navigateTo({ url: '/pages/teacher/approvals?tab=0' })
const goToResumes = () => uni.navigateTo({ url: '/pages/teacher/resumes?filter=unread' })
const goToUrgentJobs = () => uni.navigateTo({ url: '/pages/teacher/jobs?filter=urgent' })
const goToActivityDetail = (act) => {
  if (act.id) {
    uni.navigateTo({ url: '/pages/teacher/deliveries?activityId=' + act.id })
  } else {
    uni.showToast({ title: act.text || '查看详情', icon: 'none' })
  }
}
</script>

<style scoped lang="scss">
/* ========== 顶部头部 ========== */
.header-section {
	background: $uni-gradient-hero;
	color: white;
	padding: 16px 16px 32px;
	flex-shrink: 0;
	position: relative;
	overflow: hidden;
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
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 4px;
	position: relative;
	z-index: 1;
}
.header-greeting {
	flex-direction: row;
	align-items: center;
	gap: 10px;
}
.greeting {
	font-size: 20px;
	font-weight: 700;
}
.role-badge {
	font-size: 12px;
	background: rgba(255,255,255,0.2);
	padding: 3px 10px;
	border-radius: 12px;
}

/* 本班概览（头部内半透明指标行） */
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

/* ========== 待办快捷区（2×2卡片） ========== */
.todo-grid {
	flex-direction: row;
	flex-wrap: wrap;
	gap: 12px;
	padding: 0 16px;
	margin-top: -16px;
	position: relative;
	z-index: 10;
}
.todo-card {
	width: calc(50% - 6px);
	background: white;
	border-radius: 12px;
	padding: 16px;
	box-shadow: $uni-shadow-card;
}
.todo-top {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 8px;
}
.todo-num {
	font-size: 28px;
	font-weight: 800;
	color: $uni-text-color-title;
}
/* 待办数为 0 时弱化显示，>0 才有"待办感" */
.todo-num.zero {
	color: $uni-text-color-placeholder;
}
.todo-icon {
	width: 36px;
	height: 36px;
	border-radius: 12px;
	background: $uni-color-warning-light;
	align-items: center;
	justify-content: center;
}
.todo-card:nth-child(2) .todo-icon { background: $uni-color-primary-light; }
.todo-card:nth-child(3) .todo-icon { background: $uni-color-success-light; }
.todo-card:nth-child(4) .todo-icon { background: $uni-color-error-light; }
.todo-label {
	font-size: 13px;
	color: $uni-text-color;
	font-weight: 500;
}
.todo-card:active { transform: scale(0.97); }

/* ========== 近期动态列表 ========== */
.list-section {
	padding: 0 16px;
	margin-top: 20px;
	margin-bottom: 16px;
}
.list-tabs {
	flex-direction: row;
	align-items: center;
	margin-bottom: 12px;
	gap: 20px;
}
.list-tab {
	font-size: 15px;
	color: $uni-text-color-secondary;
	font-weight: 500;
	padding-bottom: 4px;
	position: relative;
}
.list-tab.active {
	color: $uni-text-color-title;
	font-weight: 600;
}
.list-tab.active::after {
	content: '';
	position: absolute;
	bottom: 0;
	left: 0;
	width: 20px;
	height: 3px;
	background: $uni-color-primary;
	border-radius: 4px;
}
.section-more {
	margin-left: auto;
	font-size: 13px;
	color: $uni-color-primary;
	font-weight: 500;
}
.activity-list {
	background: white;
	border-radius: 12px;
	padding: 0 16px;
	box-shadow: $uni-shadow-card;
}
.activity-item {
	flex-direction: row;
	gap: 12px;
	padding: 14px 0;
	border-bottom: 0.5px solid $uni-border-color-divider;
}
.activity-item:last-child { border-bottom: none; }
.activity-dot {
	width: 8px;
	height: 8px;
	border-radius: 50%;
	margin-top: 6px;
	flex-shrink: 0;
}
.activity-dot.delivery { background: $uni-color-primary; }
.activity-dot.register { background: $uni-color-primary; }
.activity-dot.interview { background: $uni-color-warning; }
.activity-dot.employed { background: $uni-color-ai; }
.activity-content { gap: 4px; flex: 1; }
.activity-text { font-size: 14px; color: $uni-text-color-title; }
.activity-time { font-size: 12px; color: $uni-text-color-placeholder; }
</style>
