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
						<text v-if="profileDescLine" class="profile-desc">{{ profileDescLine }}</text>
						<text v-if="classInfo" class="profile-class" @click="editProfile">{{ classInfo.name }}{{ classInfo.teacherName ? ' · ' + (classInfo.teacherName.endsWith('老师') ? classInfo.teacherName : classInfo.teacherName + '老师') : '' }}</text>
						<view class="profile-status-badge">
							<text class="profile-status-text">求职状态：{{ jobStatus }}</text>
						</view>
					</view>
				</view>

				<!-- 成长概览卡：完整度 + AI 诊断 + 继续完善 -->
				<view class="header-tools">
					<view class="grow-card">
						<view class="grow-item">
							<view class="grow-num-row">
								<text class="grow-num">{{ completeness }}</text>
								<text class="grow-unit">%</text>
							</view>
							<text class="grow-label">简历完整度</text>
						</view>
						<view class="grow-divider" />
						<view class="grow-item" @click="goToAIDiagnosis">
							<view class="grow-num-row">
								<text class="grow-num" :class="{ 'grow-num--empty': !aiScore }">{{ aiScore > 0 ? aiScore : '—' }}</text>
								<text v-if="aiScore > 0" class="grow-unit">分</text>
							</view>
							<text class="grow-label">AI 简历诊断</text>
						</view>
						<view class="grow-divider" />
						<view class="grow-item" @click="gotoFunc('/pages/student/resume-edit')">
							<view class="grow-action-icon">
								<uni-icons type="compose" size="20" color="#FFFFFF" />
							</view>
							<text class="grow-label">继续完善</text>
						</view>
					</view>
				</view>
			</view>

			<!-- 简历清单：告诉用户缺什么、下一步做什么 -->
			<view class="checklist-card">
				<view class="checklist-header">
					<text class="card-title">简历清单</text>
					<text class="checklist-edit" @click="gotoFunc('/pages/student/resume-edit')">{{ checklistDone }}/{{ fieldStatuses.length }} 已完善 · 去完善 ›</text>
				</view>
				<view class="checklist-grid">
					<view v-for="(item, i) in fieldStatuses" :key="i" class="checklist-item" :class="{ pending: !item.done }" @click="gotoFunc('/pages/student/resume-edit')">
						<text class="checklist-label">{{ item.label }}</text>
						<text v-if="item.done" class="checklist-ok">✓</text>
						<text v-else class="checklist-pending">待补充</text>
					</view>
				</view>
			</view>

			<!-- 常用功能（4合1） -->
			<view class="func-card">
				<text class="card-title">常用功能</text>
				<view class="func-grid">
					<view class="func-item" @click="gotoFunc('/pages/student/resume')">
						<view class="func-icon func-icon--blue">
							<uni-icons type="list" size="22" color="#165DFF" />
						</view>
						<text class="func-text">在线简历</text>
					</view>
					<view class="func-item" @click="gotoFunc('/pages/student/ai-matches')">
						<view class="func-icon func-icon--ai">
							<uni-icons type="star" size="22" color="#0EA5E9" />
						</view>
						<text class="func-text">AI人岗匹配</text>
					</view>
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
						<text class="func-text">浏览足迹</text>
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
							<uni-icons type="close" size="18" color="#F53F3F" />
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
import { buildResumeChecklist, resumeCompleteness } from '@/utils/resumeCheck'
import TabBar from '@/components/TabBar.vue'

const stats = ref({})
const refreshing = ref(false)
const userInfo = ref({})
const completeness = ref(0)
const aiScore = ref(0)
const fieldStatuses = ref([])
const classInfo = ref(null) // {id, name, major, grade, teacherName, studentCount}
const statusBarHeight = ref(0)

const avatarText = computed(() => (userInfo.value.realName || '学').charAt(0))

// 求职状态：按投递推进阶段生成
const jobStatus = computed(() => {
	const d = stats.value || {}
	if ((d.offersCount || 0) > 0) return '已获录用通知'
	if ((d.interviewCount || 0) > 0) return '面试推进中'
	if ((d.myDeliveries || 0) > 0) return '积极求职中'
	return '完善简历中'
})

const checklistDone = computed(() => fieldStatuses.value.filter(f => f.done).length)

// 学校/专业都为空时整行隐藏，不展示「未设置」类半成品文案
const profileDescLine = computed(() => {
	const s = userInfo.value.school || ''
	const m = userInfo.value.major || ''
	const g = classInfo.value && classInfo.value.grade ? classInfo.value.grade + '级' : ''
	return [s, m, g].filter(Boolean).join(' · ')
})

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
			// 字段级清单（统一口径见 utils/resumeCheck.js）
			const raw = uni.getStorageSync('userInfo')
			let ui = {}
			try { if (raw) ui = JSON.parse(raw) } catch (e) {}
			fieldStatuses.value = buildResumeChecklist(r, ui)
			completeness.value = resumeCompleteness(fieldStatuses.value)
			// AI 诊断分数：读真实诊断结果（教师评估或学生自诊写入 aiAnalysis）
			if (r.aiAnalysis) {
				try {
					const analysis = typeof r.aiAnalysis === 'string' ? JSON.parse(r.aiAnalysis) : r.aiAnalysis
					aiScore.value = Number(analysis.overallScore) || 0
				} catch (e) { aiScore.value = 0 }
			}
		} else {
			fieldStatuses.value = []
			completeness.value = 0
			aiScore.value = 0
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
const goToAIDiagnosis = () => uni.navigateTo({ url: '/pages/student/resume' })

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

/* ========== 头部区域：深色 Hero（对齐三端工作台） ========== */
.profile-header {
	background: $uni-gradient-hero;
	color: $uni-text-color-inverse;
	padding: 0 0 24px;
	position: relative;
	overflow: hidden;
}
.profile-header::after {
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
/* 仅顶部用户信息行避让右侧胶囊 */
.profile-main--safe {
	position: relative;
	z-index: 1;
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
.profile-status-badge {
	margin-top: 8px;
	align-self: flex-start;
	background: rgba(255,255,255,0.18);
	border: 1px solid rgba(255,255,255,0.25);
	border-radius: 999px;
	padding: 3px 10px;
}
.profile-status-text {
	font-size: 11px;
	color: $uni-text-color-inverse;
	font-weight: 500;
}

/* 简历清单 */
.checklist-card {
	background: $uni-bg-color;
	border-radius: $uni-border-radius-xl;
	margin: $uni-spacing-lg $uni-spacing-lg 0;
	padding: $uni-spacing-lg;
	box-shadow: $uni-shadow-card;
}
.checklist-header {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: $uni-spacing-base;
}
.checklist-edit {
	font-size: 12px;
	color: $uni-color-primary;
	font-weight: 500;
}
.checklist-grid {
	flex-direction: row;
	flex-wrap: wrap;
	gap: 8px;
}
.checklist-item {
	width: calc(50% - 4px);
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
	background: $uni-bg-color-page;
	border-radius: $uni-border-radius-base;
	padding: 10px 12px;
	box-sizing: border-box;
}
.checklist-label {
	font-size: 13px;
	color: $uni-text-color;
}
.checklist-ok {
	font-size: 14px;
	font-weight: 700;
	color: $uni-color-success;
}
.checklist-pending {
	font-size: 11px;
	color: $uni-color-warning;
	font-weight: 500;
}
.checklist-item.pending {
	background: $uni-color-warning-light;
}

/* 成长概览卡：完整度 + AI 诊断 + 继续完善（玻璃拟态，对齐三端概览行） */
.header-tools {
	width: auto;
	margin: 0 $uni-spacing-lg;
	display: flex;
	flex-direction: column;
	gap: $uni-spacing-sm;
	position: relative;
	z-index: 1;
}
.grow-card {
	background: rgba(255,255,255,0.14);
	border: 1px solid rgba(255,255,255,0.22);
	border-radius: 12px;
	padding: 14px 0;
	display: flex;
	flex-direction: row;
	align-items: center;
}
.grow-item {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 4px;
}
.grow-item:active { opacity: 0.75; }
.grow-num-row {
	display: flex;
	flex-direction: row;
	align-items: baseline;
	gap: 1px;
}
.grow-num {
	font-size: 24px;
	font-weight: 800;
	color: $uni-text-color-inverse;
	line-height: 1.1;
}
.grow-num--empty {
	color: rgba(255,255,255,0.5);
}
.grow-unit {
	font-size: 12px;
	color: rgba(255,255,255,0.85);
}
.grow-label {
	font-size: 11px;
	color: rgba(255,255,255,0.85);
}
.grow-divider {
	width: 0.5px;
	height: 30px;
	background: rgba(255,255,255,0.25);
}
.grow-action-icon {
	width: 36px;
	height: 36px;
	border-radius: 50%;
	background: rgba(255,255,255,0.2);
	display: flex;
	align-items: center;
	justify-content: center;
}

/* ========== 功能卡片 ========== */
.func-card {
	background: $uni-bg-color;
	border-radius: $uni-border-radius-xl;
	margin: $uni-spacing-lg $uni-spacing-lg 0;
	padding: $uni-spacing-lg $uni-spacing-lg $uni-spacing-xs;
	box-shadow: $uni-shadow-card;
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
.func-icon--ai { background: $uni-color-ai-light; }
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
	box-shadow: $uni-shadow-card;
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