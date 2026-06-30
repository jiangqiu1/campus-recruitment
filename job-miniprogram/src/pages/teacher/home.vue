<template>
	<view class="page-wrapper">
		<scroll-view class="content-scrollable" scroll-y>
			<!-- 绿色渐变头部 -->
			<view class="teacher-header">
				<view class="header-top">
					<view class="header-greeting">
						<text class="greeting">您好, {{ userInfo.realName || '教师用户' }}</text>
						<text class="role-badge">教师端</text>
					</view>
					<view class="header-actions">
						<view class="header-action-btn" @click="goToSecurity">
							<text>🔒</text>
						</view>
					</view>
				</view>
				<text class="header-sub">就业管理后台 · {{ currentDate }}</text>
			</view>

			<!-- 统计卡片 -->
			<view class="stats-grid">
				<view class="stat-card">
					<view class="stat-icon blue"><text>👥</text></view>
					<text class="stat-num">{{ dashboard.classCount || 0 }}</text>
					<text class="stat-label">班级数</text>
				</view>
				<view class="stat-card">
					<view class="stat-icon green"><text>👨‍🎓</text></view>
					<text class="stat-num">{{ dashboard.studentCount || 0 }}</text>
					<text class="stat-label">学生数</text>
				</view>
				<view class="stat-card">
					<view class="stat-icon orange"><text>📋</text></view>
					<text class="stat-num">{{ dashboard.jobCount || 0 }}</text>
					<text class="stat-label">岗位数</text>
				</view>
				<view class="stat-card">
					<view class="stat-icon purple"><text>📮</text></view>
					<text class="stat-num">{{ dashboard.deliveryCount || 0 }}</text>
					<text class="stat-label">投递数</text>
				</view>
			</view>

			<!-- 快捷入口 -->
			<view class="quick-menu">
				<view class="quick-item" @click="goToClasses">
					<view class="quick-icon blue"><text>👥</text></view>
					<text>班级管理</text>
				</view>
				<view class="quick-item" @click="goToResumes">
					<view class="quick-icon green"><text>📄</text></view>
					<text>简历管理</text>
				</view>
				<view class="quick-item" @click="goToJobEdit">
					<view class="quick-icon orange"><text>📋</text></view>
					<text>岗位发布</text>
				</view>
				<view class="quick-item" @click="goToDeliveries">
					<view class="quick-icon purple"><text>📊</text></view>
					<text>投递看板</text>
				</view>
				<view class="quick-item" @click="goToAiMatches">
					<view class="quick-icon"><text>🤖</text></view>
					<text>人岗匹配</text>
				</view>
				<view class="quick-item" @click="goToAiParse">
					<view class="quick-icon"><text>📄</text></view>
					<text>简历解析</text>
				</view>
				<view class="quick-item" @click="goToApprovals">
					<view class="quick-icon" style="background:linear-gradient(135deg,#EF4444,#F87171);box-shadow:0 4px 12px rgba(239,68,68,0.2);"><text>📝</text></view>
					<text>审批管理</text>
				</view>
			</view>

			<!-- 近期动态 -->
			<view class="section">
				<view class="section-header">
					<text class="section-title">📌 近期动态</text>
				</view>
				<view class="activity-list">
					<view v-for="(act, i) in activities" :key="i" class="activity-item">
						<view class="activity-dot" :class="act.type"></view>
						<view class="activity-content">
							<text class="activity-text">{{ act.text }}</text>
							<text class="activity-time">{{ act.time }}</text>
						</view>
					</view>
					<view v-if="!activities.length" class="empty-state">
						<text style="font-size:42px;margin-bottom:8px;">📭</text>
						<text>暂无动态</text>
					</view>
				</view>
			</view>
		</scroll-view>
		<TeacherTabBar current="home" />
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { teacherAPI } from '@/utils/request'
import { checkRole } from '@/utils/auth'
import TeacherTabBar from '@/components/TeacherTabBar.vue'

// 角色路由锁 — 仅教师(1)可访问
checkRole(1)

const userInfo = ref({})
const dashboard = ref({})
const activities = ref([])

const goToAiMatches = () => uni.navigateTo({ url: '/pages/teacher/ai-matches' })
const goToAiParse = () => uni.navigateTo({ url: '/pages/teacher/ai-parse' })

const currentDate = ref('')



onMounted(() => {
	try {
		const stored = uni.getStorageSync('userInfo')
		if (stored) userInfo.value = JSON.parse(stored)
	} catch (e) {}
	const now = new Date()
	currentDate.value = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`
	loadDashboard()
})

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

const goToClasses = () => uni.navigateTo({ url: '/pages/teacher/classes' })
const goToResumes = () => uni.navigateTo({ url: '/pages/teacher/resumes' })
const goToJobEdit = () => uni.navigateTo({ url: '/pages/teacher/job-edit' })
const goToDeliveries = () => uni.navigateTo({ url: '/pages/teacher/deliveries' })
const goToSecurity = () => uni.navigateTo({ url: '/pages/teacher/security' })
const goToApprovals = () => uni.navigateTo({ url: '/pages/teacher/approvals' })
</script>

<style scoped>
.teacher-header {
	background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
	color: white;
	padding: 24px 16px;
	position: relative;
	overflow: hidden;
	flex-shrink: 0;
}
.teacher-header::before {
	content: '';
	position: absolute;
	top: -50%;
	right: -20%;
	width: 200px;
	height: 200px;
	background: rgba(255,255,255,0.08);
	border-radius: 50%;
}
.header-top {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	position: relative;
	z-index: 1;
}
.header-greeting {
	gap: 8px;
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
	align-self: flex-start;
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
}
.header-sub {
	font-size: 13px;
	opacity: 0.8;
	margin-top: 8px;
	position: relative;
	z-index: 1;
}
.stats-grid {
	flex-direction: row;
	flex-wrap: wrap;
	gap: 12px;
	padding: 16px;
}
.stat-card {
	flex: 1;
	min-width: calc(50% - 6px);
	background: white;
	border-radius: 16px;
	padding: 20px;
	position: relative;
	overflow: hidden;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.stat-card::before {
	content: '';
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 3px;
	background: linear-gradient(90deg, #10B981, transparent);
}
.stat-card.blue::before { background: linear-gradient(90deg, #165DFF, transparent); }
.stat-card.green::before { background: linear-gradient(90deg, #10B981, transparent); }
.stat-card.orange::before { background: linear-gradient(90deg, #F59E0B, transparent); }
.stat-card.purple::before { background: linear-gradient(90deg, #8B5CF6, transparent); }
.stat-icon {
	width: 36px;
	height: 36px;
	border-radius: 10px;
	align-items: center;
	justify-content: center;
	font-size: 18px;
	margin-bottom: 8px;
}
.stat-icon.blue { background: rgba(22,93,255,0.1); }
.stat-icon.green { background: rgba(16,185,129,0.1); }
.stat-icon.orange { background: rgba(245,158,11,0.1); }
.stat-icon.purple { background: rgba(139,92,246,0.1); }
.stat-num {
	font-size: 28px;
	font-weight: 800;
	color: #1D2129;
	margin-bottom: 4px;
}
.stat-label {
	font-size: 13px;
	color: #86909C;
}
.quick-menu {
	flex-direction: row;
	justify-content: space-around;
	padding: 16px;
	background: white;
	margin: 0 16px 12px;
	border-radius: 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.quick-item {
	align-items: center;
	gap: 8px;
}
.quick-icon {
	width: 52px;
	height: 52px;
	border-radius: 16px;
	align-items: center;
	justify-content: center;
	font-size: 24px;
	box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.quick-icon.blue { background: linear-gradient(135deg, #165DFF, #60A5FA); }
.quick-icon.green { background: linear-gradient(135deg, #10B981, #34D399); }
.quick-icon.orange { background: linear-gradient(135deg, #F59E0B, #FBBF24); }
.quick-icon.purple { background: linear-gradient(135deg, #8B5CF6, #A78BFA); }
.quick-item text:last-child {
	font-size: 12px;
	color: #4E5969;
	font-weight: 500;
}
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
.activity-list {
	background: white;
	border-radius: 16px;
	padding: 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.activity-item {
	flex-direction: row;
	gap: 12px;
	padding: 12px 0;
	border-bottom: 1px solid #F2F3F5;
}
.activity-item:last-child { border-bottom: none; }
.activity-dot {
	width: 8px;
	height: 8px;
	border-radius: 50%;
	margin-top: 6px;
	flex-shrink: 0;
}
.activity-dot.delivery { background: #10B981; }
.activity-dot.register { background: #165DFF; }
.activity-dot.interview { background: #F59E0B; }
.activity-dot.employed { background: #8B5CF6; }
.activity-content { gap: 4px; }
.activity-text {
	font-size: 14px;
	color: #1D2129;
}
.activity-time {
	font-size: 12px;
	color: #C9CDD4;
}
.empty-state {
	padding: 40px;
	align-items: center;
	justify-content: center;
	color: #86909C;
	font-size: 14px;
}
</style>
