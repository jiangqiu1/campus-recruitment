<template>
	<view class="page-wrapper">
		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<!-- 头部：左对齐布局，避让微信胶囊 -->
			<view class="profile-header" :style="{ paddingTop: (statusBarHeight + 20) + 'px' }">
				<view class="profile-main profile-main--safe">
					<view class="profile-avatar" @click="goEditProfile">
						<text>{{ avatarText }}</text>
					</view>
					<view class="profile-info-wrap">
						<text class="profile-name" @click="goEditProfile">{{ userInfo.realName || 'HR用户' }}</text>
						<view class="role-badge">
							<text class="role-text">企业招聘方</text>
						</view>
					</view>
				</view>
			</view>

			<!-- 数据统计卡片：悬浮式布局，对齐全站规范 -->
			<view class="stats-wrap">
				<view class="stat-box" v-for="(item, index) in statList" :key="index">
					<text class="stat-num">{{ item.value || 0 }}</text>
					<text class="stat-label">{{ item.label }}</text>
				</view>
			</view>

			<!-- 菜单列表 -->
			<view class="settings-card">
				<!-- 企业管理 -->
				<view class="settings-group-label">企业管理</view>
				<view class="settings-item" @click="goToJobs">
					<view class="settings-left">
						<view class="settings-icon">
							<uni-icons type="list" size="18" color="#165DFF" />
						</view>
						<text class="settings-label">岗位管理</text>
						<text class="settings-hint">发布/编辑岗位</text>
					</view>
					<uni-icons type="arrowright" size="16" color="#C9CDD4" />
				</view>
				<view class="settings-divider"></view>
				<view class="settings-item" @click="goToStats">
					<view class="settings-left">
						<view class="settings-icon">
							<uni-icons type="bars" size="18" color="#00B42A" />
						</view>
						<text class="settings-label">数据统计</text>
						<text class="settings-hint">关键指标查看</text>
					</view>
					<uni-icons type="arrowright" size="16" color="#C9CDD4" />
				</view>

				<view class="settings-section-divider" />

				<!-- 设置 -->
				<view class="settings-group-label">设置</view>
				<view class="settings-item" @click="goToCompany">
					<view class="settings-left">
						<view class="settings-icon">
							<uni-icons type="home" size="18" color="#165DFF" />
						</view>
						<text class="settings-label">企业信息</text>
					</view>
					<uni-icons type="arrowright" size="16" color="#C9CDD4" />
				</view>
				<view class="settings-divider"></view>
				<view class="settings-item" @click="goToSecurity">
					<view class="settings-left">
						<view class="settings-icon">
							<uni-icons type="locked" size="18" color="#165DFF" />
						</view>
						<text class="settings-label">账号安全</text>
					</view>
					<uni-icons type="arrowright" size="16" color="#C9CDD4" />
				</view>
				<view class="settings-divider"></view>
				<view class="settings-item" @click="goToAbout">
					<view class="settings-left">
						<view class="settings-icon settings-icon--gray">
							<uni-icons type="info" size="18" color="#86909C" />
						</view>
						<text class="settings-label">关于平台</text>
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
		<HrTabBar current="profile" />
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@/utils/page-lifecycle'
import { hrAPI } from '@/utils/request'
import HrTabBar from '@/components/HrTabBar.vue'

const userInfo = ref({})
const stats = ref({})
const statusBarHeight = ref(0)

const avatarText = computed(() => {
	return (userInfo.value.realName || 'H').charAt(0)
})

// 统计项配置
const statList = computed(() => [
	{ value: stats.value.activeJobCount || 0, label: '岗位数' },
	{ value: stats.value.resumeCount || 0, label: '简历处理' },
	{ value: stats.value.interviewCount || 0, label: '面试数' }
])

onMounted(() => {
	// 状态栏高度适配
	try {
		const winInfo = uni.getWindowInfo()
		statusBarHeight.value = winInfo.statusBarHeight || 0
	} catch (e) {
		try {
			const sysInfo = uni.getSystemInfoSync()
			statusBarHeight.value = sysInfo.statusBarHeight || 0
		} catch (e2) {}
	}

	loadUserInfo()
	loadStats()
})

onShow(() => { loadUserInfo() })

const loadUserInfo = () => {
	try {
		const stored = uni.getStorageSync('userInfo')
		if (stored) userInfo.value = JSON.parse(stored)
	} catch (e) {}
}

const refreshing = ref(false)
const onRefresh = async () => {
	refreshing.value = true
	await loadStats()
	refreshing.value = false
}

const loadStats = async () => {
	try {
		const cId = getCompanyId()
		const res = await hrAPI.getDashboard(cId)
		stats.value = res.data || {}
	} catch (e) {
		console.error('加载统计失败', e)
	}
}

const getCompanyId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.companyId || obj.id || null
	} catch (e) { return null }
}

const goToCompany = () => {
	uni.navigateTo({ url: '/pages/hr/company' })
}

const goToJobs = () => {
	uni.navigateTo({ url: '/pages/hr/jobs' })
}

const goToStats = () => {
	uni.navigateTo({ url: '/pages/hr/stats' })
}

const goToSecurity = () => {
	uni.navigateTo({ url: '/pages/hr/security' })
}

const goToAbout = () => {
	uni.navigateTo({ url: '/pages/common/about' })
}

const goEditProfile = () => {
	uni.navigateTo({ url: '/pages/hr/edit-profile' })
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

<style lang="scss" scoped>
/* ========== 全局基础：复用全站规范 ========== */
.page-wrapper {
	width: 100%;
	height: 100vh;
	background-color: $uni-bg-color-page;
	display: flex;
	flex-direction: column;
}
.content-scrollable {
	flex: 1;
	overflow: hidden;
	background: $uni-bg-color-page;
}

/* ========== 头部：对齐全站渐变 + 分层避让胶囊 ========== */
.profile-header {
	background: linear-gradient(170deg, $uni-color-primary 0%, $uni-color-primary-hover 100%);
	color: $uni-text-color-inverse;
	padding: 0 0 36px;
}
.profile-main--safe {
	padding: 0 $uni-spacing-lg;
	padding-right: 106px; /* 仅顶部行避让微信胶囊 */
}
.profile-main {
	display: flex;
	flex-direction: row;
	align-items: center;
	gap: $uni-spacing-base;
}
.profile-avatar {
	width: 56px;
	height: 56px;
	border-radius: $uni-border-radius-circle;
	background: rgba(255,255,255,0.25);
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 24px;
	font-weight: $uni-font-weight-semibold;
	border: 1.5px solid rgba(255,255,255,0.4);
	flex-shrink: 0;
	backdrop-filter: blur(4px);
}
.profile-info-wrap {
	display: flex;
	flex-direction: column;
	gap: 6px;
}
.profile-name {
	font-size: $uni-font-size-h2;
	font-weight: $uni-font-weight-bold;
	line-height: 1.3;
}
.role-badge {
	display: inline-flex;
	align-items: center;
	background: rgba(255,255,255,0.2);
	padding: 4px 10px;
	border-radius: 20px;
	align-self: flex-start;
}
.role-text {
	font-size: 12px;
	font-weight: $uni-font-weight-medium;
	color: rgba(255,255,255,0.9);
}

/* ========== 统计卡片：悬浮式 + 专业质感 ========== */
.stats-wrap {
	display: flex;
	flex-direction: row;
	gap: 12px;
	padding: 0 $uni-spacing-lg;
	margin-top: -24px;
	position: relative;
	z-index: 2;
}
.stat-box {
	flex: 1;
	background: $uni-bg-color;
	border-radius: $uni-border-radius-xl;
	padding: 16px 8px;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 4px;
	box-shadow: $uni-shadow-base;
	transition: transform 0.2s ease, opacity 0.2s ease;
}
.stat-box:active {
	transform: scale(0.97);
	opacity: 0.9;
}
.stat-num {
	font-size: 22px;
	font-weight: $uni-font-weight-bold;
	color: $uni-text-color-title;
	font-variant-numeric: tabular-nums;
	line-height: 1.2;
}
.stat-label {
	font-size: $uni-font-size-sm;
	color: $uni-text-color-secondary;
}

/* ===== 菜单分组 ===== */
.settings-group-label {
	font-size: 12px;
	color: #86909C;
	padding: 12px 16px 4px;
	font-weight: 500;
}
.settings-section-divider {
	height: 8px;
	background: #F7F8FA;
	margin: 4px 0;
}
.settings-hint {
	font-size: 11px;
	color: #C9CDD4;
	margin-left: 6px;
}

/* ========== 菜单列表：与学生/教师端像素级对齐 ========== */
.settings-card {
	background: $uni-bg-color;
	border-radius: $uni-border-radius-xl;
	margin: $uni-spacing-lg $uni-spacing-lg 0;
	padding: 4px 0;
	box-shadow: $uni-shadow-base;
}
.settings-item {
	display: flex;
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
	padding: 14px $uni-spacing-lg;
	transition: background-color 0.2s ease;
}
.settings-item:active {
	background: $uni-bg-color-hover;
}
.settings-left {
	display: flex;
	flex-direction: row;
	align-items: center;
	gap: $uni-spacing-base;
}
.settings-icon {
	width: 34px;
	height: 34px;
	border-radius: $uni-border-radius-base;
	background: $uni-color-primary-light;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}
.settings-icon--gray {
	background: #F2F3F5;
}
.settings-icon--danger {
	background: $uni-color-error-light;
}
.settings-label {
	font-size: $uni-font-size-base;
	color: $uni-text-color-title;
	font-weight: $uni-font-weight-medium;
}
.settings-label--danger {
	color: $uni-color-error;
}
.settings-divider {
	height: 1px;
	background: $uni-border-color-divider;
	margin-left: 62px;
}

/* 底部占位，避让TabBar */
.bottom-placeholder {
	height: calc($tab-bar-height + env(safe-area-inset-bottom));
}
</style>