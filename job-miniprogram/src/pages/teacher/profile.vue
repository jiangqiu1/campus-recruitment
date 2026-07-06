<template>
	<view class="page-wrapper">
		<scroll-view 
			class="content-scrollable" 
			scroll-y 
			refresher-enabled 
			:refresher-triggered="refreshing"
			@refresherrefresh="onRefresh"
			@scroll="onScroll"
		>
			<!-- 个人资料头部：对齐登录页渐变风格 + 横向紧凑布局 -->
			<view class="profile-header" :class="{ 'profile-header--shrink': isHeaderShrink }" :style="{ paddingTop: (statusBarHeight + 16) + 'px' }">
				<view class="profile-header__left">
					<view class="profile-avatar">
						<text>{{ avatarText }}</text>
					</view>
					<view class="profile-user">
						<text class="profile-name">{{ userInfo.realName || '教师用户' }}</text>
						<text class="profile-info">{{ userInfo.department || '就业指导老师' }}</text>
					</view>
				</view>
			</view>

			<!-- 功能快捷入口：对齐登录页卡片质感 -->
			<view class="func-grid">
				<view class="func-item" @click="gotoFunc('/pages/teacher/deliveries')">
					<view class="func-icon func-icon--blue">
						<uni-icons type="bars" size="22" color="#165DFF" />
					</view>
					<text class="func-label">投递看板</text>
				</view>
				<view class="func-item" @click="gotoFunc('/pages/teacher/companies')">
					<view class="func-icon func-icon--amber">
						<uni-icons type="shop" size="22" color="#D97706" />
					</view>
					<text class="func-label">企业资源</text>
				</view>
				<view class="func-item" @click="gotoFunc('/pages/teacher/ai-matches')">
					<view class="func-icon func-icon--purple">
						<uni-icons type="star" size="22" color="#7C3AED" />
					</view>
					<text class="func-label">AI人岗匹配</text>
				</view>
				<view class="func-item" @click="gotoFunc('/pages/teacher/resumes')">
					<view class="func-icon func-icon--emerald">
						<uni-icons type="scan" size="22" color="#059669" />
					</view>
					<text class="func-label">简历分析</text>
				</view>
				<view class="func-item" @click="gotoFunc('/pages/teacher/approvals')">
					<view class="func-icon func-icon--red">
						<uni-icons type="auth" size="22" color="#DC2626" />
					</view>
					<text class="func-label">审批管理</text>
				</view>
			</view>

			<!-- 管理数据统计 -->
			<view class="section-header">
				<text class="section-title">管理数据</text>
			</view>
			
			<!-- 核心指标大卡片：沿用登录页渐变按钮的品牌渐变 -->
			<view class="stat-card stat-card--primary" @click="gotoFunc('/pages/teacher/deliveries')">
				<view class="stat-card__main">
					<text class="stat-num--large">{{ dashboard.deliveryCount || 0 }}</text>
					<text class="stat-label">总投递数</text>
				</view>
				<view class="stat-card__side">
					<text class="stat-trend">较上周 +12%</text>
					<uni-icons type="arrowup" size="14" color="#10B981" />
				</view>
			</view>

			<!-- 次要指标三列网格 -->
			<view class="stat-grid">
				<view class="stat-box" @click="gotoFunc('/pages/teacher/classes')">
					<text class="stat-num">{{ dashboard.classCount || 0 }}</text>
					<text class="stat-label">班级数</text>
				</view>
				<view class="stat-box" @click="gotoFunc('/pages/teacher/resumes')">
					<text class="stat-num">{{ dashboard.studentCount || 0 }}</text>
					<text class="stat-label">学生数</text>
				</view>
				<view class="stat-box" @click="gotoFunc('/pages/teacher/jobs')">
					<text class="stat-num">{{ dashboard.jobCount || 0 }}</text>
					<text class="stat-label">岗位数</text>
				</view>
			</view>

			<!-- 设置项列表 -->
			<view class="section-header">
				<text class="section-title">账号与设置</text>
			</view>
			<view class="settings-card">
				<view class="settings-item" @click="gotoFunc('/pages/teacher/security')">
					<view class="settings-left">
						<view class="settings-icon">
							<uni-icons type="locked" size="18" color="#165DFF" />
						</view>
						<text class="settings-label">账号安全</text>
					</view>
					<uni-icons type="arrowright" size="16" color="#C9CDD4" />
				</view>
				<view class="settings-divider"></view>
				<view class="settings-item" @click="gotoFunc('/pages/common/about')">
					<view class="settings-left">
						<view class="settings-icon">
							<uni-icons type="flag" size="18" color="#165DFF" />
						</view>
						<text class="settings-label">关于我们</text>
					</view>
					<uni-icons type="arrowright" size="16" color="#C9CDD4" />
				</view>
				<view class="settings-divider"></view>
				<view class="settings-item" @click="handleLogout">
					<view class="settings-left">
						<view class="settings-icon settings-icon--danger">
							<uni-icons type="close" size="18" color="#EF4444" />
						</view>
						<text class="settings-label settings-label--danger">退出登录</text>
					</view>
				</view>
			</view>

			<view class="bottom-placeholder" />
		</scroll-view>

		<!-- 通用 TabBar -->
		<TabBar current="profile" path-prefix="/pages/teacher/" :tab-list="teacherTabs" />
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { teacherAPI } from '@/utils/request'
import TabBar from '@/components/TabBar.vue'
import { checkRole } from '@/utils/auth'

checkRole(1)

const teacherTabs = [
	{ page: 'home', icon: 'home', activeIcon: 'home-filled', label: '首页' },
	{ page: 'classes', icon: 'staff', activeIcon: 'staff-filled', label: '班级' },
	{ page: 'jobs', icon: 'list', activeIcon: 'list', label: '岗位' },
	{ page: 'profile', icon: 'person', activeIcon: 'person-filled', label: '我的' }
]

const userInfo = ref({})
const dashboard = ref({})
const refreshing = ref(false)
const statusBarHeight = ref(0)
const isHeaderShrink = ref(false)

const avatarText = computed(() => (userInfo.value.realName || '教').charAt(0))

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

// 监听滚动，控制头部收缩
const onScroll = (e) => {
	const scrollTop = e.detail.scrollTop
	isHeaderShrink.value = scrollTop > 40
}

const onRefresh = async () => {
	refreshing.value = true
	try {
		const stored = uni.getStorageSync('userInfo')
		if (stored) userInfo.value = JSON.parse(stored)
	} catch (e) {}
	await loadDashboard()
	refreshing.value = false
}

const loadDashboard = async () => {
	try {
		const res = await teacherAPI.getDashboard()
		const d = res.data || {}
		dashboard.value = d
	} catch (e) {
		console.error('加载dashboard失败', e)
	}
}

const gotoFunc = (path) => {
	if (path) {
		uni.navigateTo({ url: path })
	} else {
		uni.showToast({ title: '功能开发中', icon: 'none' })
	}
}

const handleLogout = () => {
	uni.showModal({
		title: '提示',
		content: '确定退出登录吗？',
		success: (res) => {
			if (res.confirm) {
				uni.removeStorageSync('token')
				uni.removeStorageSync('userInfo')
				uni.reLaunch({ url: '/pages/student/login' })
			}
		}
	})
}
</script>

<style scoped>
/* ========== 全局基础：与登录页底色体系对齐 ========== */
.page-wrapper {
	width: 100%;
	height: 100vh;
	background-color: #F7F8FA;
	display: flex;
	flex-direction: column;
}
.content-scrollable {
	flex: 1;
	overflow: hidden;
}

/* ========== 头部区域：对齐登录页蓝色渐变 + 玻璃质感 ========== */
.profile-header {
	position: relative;
	display: flex;
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
	padding: 16px 20px 28px;
	background: linear-gradient(170deg, #165DFF 0%, #3B7AFF 100%);
	color: #FFFFFF;
	transition: padding 0.25s ease;
}
.profile-header--shrink {
	padding-top: 12px !important;
	padding-bottom: 16px;
}
.profile-header__left {
	display: flex;
	flex-direction: row;
	align-items: center;
	gap: 12px;
}
.profile-avatar {
	width: 52px;
	height: 52px;
	border-radius: 50%;
	background: rgba(255,255,255,0.2);
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 22px;
	font-weight: 600;
	border: 1.5px solid rgba(255,255,255,0.4);
	flex-shrink: 0;
	backdrop-filter: blur(4px);
}
.profile-user {
	display: flex;
	flex-direction: column;
	gap: 2px;
}
.profile-name {
	font-size: 17px;
	font-weight: 700;
	line-height: 1.3;
	letter-spacing: 0.02em;
}
.profile-info {
	font-size: 13px;
	opacity: 0.85;
}

/* ========== 功能快捷入口：与登录页卡片阴影/圆角统一 ========== */
.func-grid {
	display: flex;
	flex-direction: row;
	flex-wrap: nowrap;
	background: #FFFFFF;
	border-radius: 16px;
	margin: -14px 16px 0;
	padding: 18px 8px;
	box-shadow: 0 4px 24px rgba(22,93,255,0.08);
	position: relative;
	z-index: 2;
}
.func-item {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 8px;
	transition: transform 0.2s ease;
}
.func-item:active {
	transform: scale(0.97);
}
.func-icon {
	width: 44px;
	height: 44px;
	border-radius: 12px;
	display: flex;
	align-items: center;
	justify-content: center;
}
.func-icon--blue { background: rgba(22, 93, 255, 0.1); }
.func-icon--amber { background: rgba(217, 119, 6, 0.1); }
.func-icon--purple { background: rgba(124, 58, 237, 0.1); }
.func-icon--emerald { background: rgba(5, 150, 105, 0.1); }
.func-icon--red { background: rgba(220, 38, 38, 0.1); }
.func-label {
	font-size: 12px;
	color: #4E5969;
	font-weight: 500;
}

/* ========== 通用区块标题：与登录页文字色阶对齐 ========== */
.section-header {
	padding: 0 16px;
	margin-top: 24px;
	margin-bottom: 12px;
}
.section-title {
	font-size: 16px;
	font-weight: 600;
	color: #1D2129;
}

/* ========== 数据卡片：沿用登录页卡片规范 ========== */
.stat-card {
	background: #FFFFFF;
	border-radius: 16px;
	margin: 0 16px;
	padding: 18px 20px;
	display: flex;
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
	box-shadow: 0 4px 24px rgba(22,93,255,0.08);
	transition: transform 0.2s ease, opacity 0.2s ease;
}
.stat-card:active {
	transform: scale(0.98);
	opacity: 0.9;
}
.stat-card--primary {
	background: linear-gradient(135deg, #EFF4FF 0%, #FFFFFF 100%);
	border: 1px solid rgba(22, 93, 255, 0.12);
}
.stat-card__main {
	display: flex;
	flex-direction: column;
	gap: 4px;
}
.stat-num--large {
	font-size: 28px;
	font-weight: 700;
	color: #1D2129;
	font-variant-numeric: tabular-nums;
	line-height: 1.1;
}
.stat-card__side {
	display: flex;
	flex-direction: row;
	align-items: center;
	gap: 4px;
}
.stat-trend {
	font-size: 13px;
	color: #10B981;
	font-weight: 500;
}
.stat-label {
	font-size: 12px;
	color: #86909C;
}

/* 次要指标网格 */
.stat-grid {
	display: flex;
	flex-direction: row;
	gap: 12px;
	padding: 12px 16px 0;
}
.stat-box {
	flex: 1;
	background: #FFFFFF;
	border-radius: 12px;
	padding: 14px 12px;
	display: flex;
	flex-direction: column;
	gap: 4px;
	box-shadow: 0 2px 12px rgba(22,93,255,0.06);
	transition: transform 0.2s ease, opacity 0.2s ease;
}
.stat-box:active {
	transform: scale(0.97);
	opacity: 0.85;
}
.stat-num {
	font-size: 20px;
	font-weight: 700;
	color: #1D2129;
	font-variant-numeric: tabular-nums;
	line-height: 1.2;
}

/* ========== 设置列表：与登录页输入框边框/分割线体系对齐 ========== */
.settings-card {
	background: #FFFFFF;
	border-radius: 16px;
	margin: 0 16px;
	padding: 4px 0;
	box-shadow: 0 4px 24px rgba(22,93,255,0.08);
}
.settings-item {
	display: flex;
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
	padding: 14px 16px;
	transition: background-color 0.2s ease;
}
.settings-item:active {
	background: #F7F8FA;
}
.settings-left {
	display: flex;
	flex-direction: row;
	align-items: center;
	gap: 12px;
}
.settings-icon {
	width: 34px;
	height: 34px;
	border-radius: 10px;
	background: rgba(22, 93, 255, 0.1);
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}
.settings-icon--danger {
	background: rgba(239, 68, 68, 0.1);
}
.settings-label {
	font-size: 15px;
	color: #1D2129;
	font-weight: 500;
}
.settings-label--danger {
	color: #EF4444;
}
.settings-divider {
	height: 1px;
	background: #E5E6EB;
	margin-left: 62px;
}

/* 底部占位 */
.bottom-placeholder {
	height: calc(60px + env(safe-area-inset-bottom));
}

/* 减弱动效适配 */
@media (prefers-reduced-motion: reduce) {
	.profile-header,
	.func-item,
	.stat-card,
	.stat-box,
	.settings-item {
		transition: none;
	}
}
</style>