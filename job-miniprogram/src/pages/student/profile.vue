<template>
	<view class="page-wrapper">
		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<!-- 个人资料头部：分层避让胶囊 -->
			<view class="profile-header" :style="{ paddingTop: (statusBarHeight + 20) + 'px' }">
				<!-- 仅这一行避让右侧胶囊 -->
				<view class="profile-main profile-main--safe">
					<view class="profile-avatar" @click="editProfile">
						<text>{{ avatarText }}</text>
					</view>
					<view class="profile-info-wrap">
						<text class="profile-name" @click="editProfile">{{ userInfo.realName || '学生用户' }}</text>
						<text class="profile-desc">{{ userInfo.school || '职业院校' }} · {{ userInfo.major || '未设置专业' }}</text>
						<text v-if="classInfo" class="profile-class" @click="editProfile">{{ classInfo.name }}{{ classInfo.teacherName ? ' · ' + classInfo.teacherName + '老师' : '' }}</text>
					</view>
				</view>

				<!-- 工具区：全宽显示，不避让胶囊（位置在胶囊下方，不会被挡） -->
				<view class="header-tools">
					<view class="tool-card">
						<view class="tool-top">
							<text class="tool-label">简历完整度</text>
							<text class="tool-val">{{ completeness }}%</text>
						</view>
						<view class="progress-bar">
							<view class="progress-fill" :style="{ width: completeness + '%' }" />
						</view>
					</view>
					<view class="tool-card tool-card--ai" @click="goToAIDiagnosis">
						<view class="ai-icon">
							<uni-icons type="star" size="16" color="#0EA5E9" />
						</view>
						<view class="ai-content">
							<text class="tool-label">AI简历诊断</text>
							<text class="tool-val">{{ aiScore }}分</text>
						</view>
						<uni-icons type="arrowright" size="14" color="rgba(255,255,255,0.7)" />
					</view>
				</view>
			</view>

			<!-- 功能服务 -->
			<view class="func-card">
				<text class="card-title">功能服务</text>
				<view class="func-grid">
					<view class="func-item" @click="gotoFunc('/pages/student/resume')">
						<view class="func-icon func-icon--blue">
							<uni-icons type="list" size="22" color="#165DFF" />
						</view>
						<text class="func-text">在线简历</text>
					</view>
					<view class="func-item" @click="gotoFunc('/pages/student/resume-edit')">
						<view class="func-icon func-icon--blue">
							<uni-icons type="compose" size="22" color="#165DFF" />
						</view>
						<text class="func-text">编辑简历</text>
					</view>
					<view class="func-item" @click="gotoFunc('/pages/student/ai-matches')">
						<view class="func-icon func-icon--purple">
							<uni-icons type="star" size="22" color="#975FE4" />
						</view>
						<text class="func-text">AI人岗匹配</text>
					</view>
				</view>
			</view>

			<!-- 我的记录 -->
			<view class="func-card">
				<text class="card-title">我的记录</text>
				<view class="func-grid func-grid--2col">
					<view class="func-item" @click="gotoFunc('/pages/student/collect')">
						<view class="func-icon func-icon--orange">
							<uni-icons type="star-filled" size="22" color="#FF7D00" />
						</view>
						<text class="func-text">我的收藏</text>
					</view>
					<view class="func-item" @click="gotoFunc('/pages/student/footprint')">
						<view class="func-icon func-icon--blue">
							<uni-icons type="eye" size="22" color="#165DFF" />
						</view>
						<text class="func-text">浏览记录</text>
					</view>
				</view>
			</view>

			<!-- 设置列表 -->
			<view class="settings-card">
				<view class="settings-item" @click="gotoFunc('/pages/student/security')">
					<view class="settings-left">
						<view class="settings-icon">
							<uni-icons type="locked" size="18" color="#165DFF" />
						</view>
						<text class="settings-label">账号安全</text>
					</view>
					<uni-icons type="arrowright" size="16" color="#C9CDD4" />
				</view>
				<view class="settings-divider"></view>
				<view class="settings-item" @click="gotoFunc('')">
					<view class="settings-left">
						<view class="settings-icon">
							<uni-icons type="flag" size="18" color="#86909C" />
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
		<TabBar current="profile" />
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@/utils/page-lifecycle'
import { statisticsAPI, resumeAPI, classAPI } from '@/utils/request'
import TabBar from '@/components/TabBar.vue'

const stats = ref({})
const refreshing = ref(false)
const userInfo = ref({})
const completeness = ref(0)
const aiScore = ref(0)
const classInfo = ref(null) // {id, name, major, grade, teacherName, studentCount}
const statusBarHeight = ref(0)

const avatarText = computed(() => (userInfo.value.realName || '学').charAt(0))

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

	loadUserInfo()
	loadStats()
	loadMyClass()
})

// 每次页面显示时重新加载用户信息（编辑资料返回后刷新数据）
onShow(() => {
	loadUserInfo()
})

const loadUserInfo = () => {
	try {
		const stored = uni.getStorageSync('userInfo')
		if (stored) userInfo.value = JSON.parse(stored)
	} catch (e) { console.error('获取用户信息失败', e) }
}

const onRefresh = async () => {
	refreshing.value = true
	try {
		const stored = uni.getStorageSync('userInfo')
		if (stored) userInfo.value = JSON.parse(stored)
	} catch (e) { console.error('获取用户信息失败', e) }
	await Promise.all([loadStats(), loadMyClass()])
	refreshing.value = false
}

const loadStats = async () => {
	try {
		const [statsRes, resumeRes] = await Promise.all([
			statisticsAPI.getStudentOverview(),
			resumeAPI.getResume().catch(() => ({ data: null }))
		])
		stats.value = statsRes.data || {}
		const r = resumeRes.data
		if (r) {
			const resumeFields = [r.jobTarget, r.education, r.internship, r.skills, r.selfEvaluation]
			let filled = resumeFields.filter(Boolean).length
			let total = resumeFields.length
			try {
				const raw = uni.getStorageSync('userInfo')
				if (raw) {
					const ui = JSON.parse(raw)
					if (ui.realName) filled++
					if (ui.phone) filled++
					if (ui.email) filled++
				}
			} catch (e) {}
			total += 3
			completeness.value = Math.round((filled / total) * 100)
			aiScore.value = Math.min(completeness.value + 5, 95)
		}
	} catch (e) {
		stats.value = { myDeliveries: 0, interviewCount: 0, offersCount: 0 }
	}
}

const loadMyClass = async () => {
	try {
		const res = await classAPI.getMyClass()
		if (res.code === 200 && res.data) {
			classInfo.value = res.data
		} else {
			classInfo.value = null
		}
	} catch (e) {
		classInfo.value = null
	}
}

const gotoFunc = (path) => {
	if (path) {
		uni.navigateTo({ url: path })
	} else {
		uni.navigateTo({ url: '/pages/student/about' })
	}
}

const editProfile = () => uni.navigateTo({ url: '/pages/student/edit-profile' })
const goToAIDiagnosis = () => uni.navigateTo({ url: '/pages/student/ai-matches' })

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
@use 'sass:color';
/* ========== 全局基础 ========== */
.page-wrapper {
	width: 100%;
	height: 100vh;
	background-color: $uni-bg-color-page;
}
.content-scrollable {
	flex: 1;
	overflow: hidden;
	background: $uni-bg-color-page;
}

/* ========== 头部区域：分层避让 ========== */
.profile-header {
	background: linear-gradient(170deg, $uni-color-primary 0%, $uni-color-primary-hover 100%);
	color: $uni-text-color-inverse;
	padding: 0 0 24px;
}
/* 仅顶部用户信息行避让右侧胶囊 */
.profile-main--safe {
	padding: 0 $uni-spacing-lg;
	padding-right: 106px;
}
.profile-main {
	display: flex;
	flex-direction: row;
	align-items: center;
	gap: $uni-spacing-base;
	margin-bottom: $uni-spacing-lg;
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
}
.profile-info-wrap {
	display: flex;
	flex-direction: column;
	gap: 3px;
}
.profile-name {
	font-size: $uni-font-size-h2;
	font-weight: $uni-font-weight-bold;
	line-height: 1.3;
}
.profile-desc {
	font-size: $uni-font-size-sm;
	opacity: 0.85;
}
.profile-class {
	font-size: 12px;
	opacity: 0.7;
	margin-top: 2px;
}

/* 工具区：左右边距和下方卡片对齐，不再整体右缩 */
.header-tools {
	width: auto;
	margin: 0 $uni-spacing-lg;
	display: flex;
	flex-direction: column;
	gap: $uni-spacing-sm;
}
.tool-card {
	background: rgba(255,255,255,0.18);
	border-radius: $uni-border-radius-base;
	padding: 10px 12px;
	border: 1px solid rgba(255,255,255,0.2);
}
.tool-top {
	display: flex;
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 6px;
}
.tool-label {
	font-size: $uni-font-size-sm;
	color: rgba(255,255,255,0.9);
}
.tool-val {
	font-size: $uni-font-size-sm;
	font-weight: $uni-font-weight-semibold;
	color: $uni-text-color-inverse;
}
.progress-bar {
	width: 100%;
	height: 4px;
	background: rgba(255,255,255,0.2);
	border-radius: 4px;
	overflow: hidden;
}
.progress-fill {
	height: 100%;
	background: linear-gradient(90deg, $uni-color-success 0%, color.adjust($uni-color-success, $lightness: 8%) 100%);
	border-radius: 4px;
	transition: width 0.3s ease;
}

/* AI诊断卡片 */
.tool-card--ai {
	display: flex;
	flex-direction: row;
	align-items: center;
	gap: 10px;
	transition: opacity 0.2s ease;
}
.tool-card--ai:active {
	opacity: 0.8;
}
.ai-icon {
	width: 32px;
	height: 32px;
	border-radius: $uni-border-radius-sm;
	background: $uni-color-ai-light;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}
.ai-content {
	flex: 1;
	display: flex;
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
}

/* ========== 功能卡片 ========== */
.func-card {
	background: $uni-bg-color;
	border-radius: $uni-border-radius-xl;
	margin: $uni-spacing-lg $uni-spacing-lg 0;
	padding: $uni-spacing-lg $uni-spacing-lg $uni-spacing-xs;
	box-shadow: $uni-shadow-base;
}
.card-title {
	font-size: $uni-font-size-h3;
	font-weight: $uni-font-weight-semibold;
	color: $uni-text-color-title;
	margin-bottom: $uni-spacing-lg;
}
.func-grid {
	display: flex;
	flex-direction: row;
	flex-wrap: nowrap;
}
.func-grid--2col .func-item {
	flex: 1;
	width: auto;
}
.func-item {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: $uni-spacing-sm;
	margin-bottom: $uni-spacing-lg;
	transition: transform 0.2s ease;
}
.func-item:active {
	transform: scale(0.97);
}
.func-icon {
	width: 44px;
	height: 44px;
	border-radius: $uni-border-radius-lg;
	display: flex;
	align-items: center;
	justify-content: center;
}
.func-icon--blue { background: $uni-color-primary-light; }
.func-icon--purple { background: rgba(151, 95, 228, 0.08); }
.func-icon--orange { background: $uni-color-warning-light; }
.func-text {
	font-size: $uni-font-size-sm;
	color: $uni-text-color;
	font-weight: $uni-font-weight-medium;
}

/* ========== 设置列表 ========== */
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

/* 底部占位 */
.bottom-placeholder {
	height: calc($tab-bar-height + env(safe-area-inset-bottom));
}
</style>