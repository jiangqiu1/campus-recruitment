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
			</view>

			<!-- 待办快捷区（2×2卡片，替换原快捷入口） -->
			<view class="todo-grid">
				<view class="todo-card" @click="goToApprovals">
					<view class="todo-top">
						<text class="todo-num">{{ (dashboard.pendingApprovalCount || 0) > 99 ? '99+' : dashboard.pendingApprovalCount || 0 }}</text>
						<view class="todo-icon"><uni-icons type="auth" size="18" color="#F59E0B" /></view>
					</view>
					<text class="todo-label">待审批</text>
				</view>
				<view class="todo-card" @click="goToDeliveries">
					<view class="todo-top">
						<text class="todo-num">{{ (dashboard.unreadResumeCount || 0) > 99 ? '99+' : dashboard.unreadResumeCount || 0 }}</text>
						<view class="todo-icon"><uni-icons type="bars" size="18" color="#165DFF" /></view>
					</view>
					<text class="todo-label">新增投递</text>
				</view>
				<view class="todo-card" @click="goToResumes">
					<view class="todo-top">
						<text class="todo-num">{{ (dashboard.unreadResumeCount || 0) > 99 ? '99+' : dashboard.unreadResumeCount || 0 }}</text>
						<view class="todo-icon"><uni-icons type="paperplane" size="18" color="#00B42A" /></view>
					</view>
					<text class="todo-label">未读简历</text>
				</view>
				<view class="todo-card" @click="goToUrgentJobs">
					<view class="todo-top">
						<text class="todo-num">{{ (dashboard.urgentJobCount || 0) > 99 ? '99+' : dashboard.urgentJobCount || 0 }}</text>
						<view class="todo-icon"><uni-icons type="star" size="18" color="#EF4444" /></view>
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

<style scoped>
/* ========== 顶部头部 ========== */
.header-section {
	background: linear-gradient(135deg, #165DFF 0%, #2563EB 100%);
	color: white;
	padding: 16px 16px 32px;
	flex-shrink: 0;
	position: relative;
	overflow: hidden;
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
	border-radius: 10px;
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
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
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
	color: #1D2129;
}
.todo-icon {
	width: 36px;
	height: 36px;
	border-radius: 10px;
	background: rgba(245,158,11,0.08);
	align-items: center;
	justify-content: center;
}
.todo-card:nth-child(2) .todo-icon { background: rgba(22,93,255,0.08); }
.todo-card:nth-child(3) .todo-icon { background: rgba(0,180,42,0.08); }
.todo-card:nth-child(4) .todo-icon { background: rgba(239,68,68,0.08); }
.todo-label {
	font-size: 13px;
	color: #4E5969;
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
	color: #86909C;
	font-weight: 500;
	padding-bottom: 4px;
	position: relative;
}
.list-tab.active {
	color: #1D2129;
	font-weight: 600;
}
.list-tab.active::after {
	content: '';
	position: absolute;
	bottom: 0;
	left: 0;
	width: 20px;
	height: 3px;
	background: #165DFF;
	border-radius: 2px;
}
.section-more {
	margin-left: auto;
	font-size: 13px;
	color: #165DFF;
	font-weight: 500;
}
.activity-list {
	background: white;
	border-radius: 12px;
	padding: 0 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.activity-item {
	flex-direction: row;
	gap: 12px;
	padding: 14px 0;
	border-bottom: 0.5px solid #F2F3F5;
}
.activity-item:last-child { border-bottom: none; }
.activity-dot {
	width: 8px;
	height: 8px;
	border-radius: 50%;
	margin-top: 6px;
	flex-shrink: 0;
}
.activity-dot.delivery { background: #165DFF; }
.activity-dot.register { background: #165DFF; }
.activity-dot.interview { background: #F59E0B; }
.activity-dot.employed { background: #8B5CF6; }
.activity-content { gap: 4px; flex: 1; }
.activity-text { font-size: 14px; color: #1D2129; }
.activity-time { font-size: 12px; color: #C9CDD4; }
</style>
