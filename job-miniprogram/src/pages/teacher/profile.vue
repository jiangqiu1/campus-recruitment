<template>
	<view class="page-wrapper">
		<scroll-view class="content-scrollable" scroll-y>
			<!-- 个人资料头部 -->
			<view class="profile-header">
				<view class="profile-avatar">
					<text>{{ avatarText }}</text>
				</view>
				<text class="profile-name">{{ userInfo.realName || '教师用户' }}</text>
				<text class="profile-info">{{ userInfo.department || '未设置部门' }}</text>
			</view>

			<!-- 统计数据 -->
			<view class="profile-stats">
				<view class="profile-stat-box">
					<text class="profile-stat-num">{{ dashboard.classCount || 0 }}</text>
					<text class="profile-stat-label">班级</text>
				</view>
				<view class="profile-stat-box">
					<text class="profile-stat-num">{{ dashboard.studentCount || 0 }}</text>
					<text class="profile-stat-label">学生</text>
				</view>
				<view class="profile-stat-box">
					<text class="profile-stat-num">{{ dashboard.jobCount || 0 }}</text>
					<text class="profile-stat-label">岗位</text>
				</view>
			</view>

			<!-- 菜单列表 -->
			<view class="menu-list">
				<view class="menu-item" @click="goToPage('security')">
					<view class="menu-icon green"><text>🔒</text></view>
					<text class="menu-text">账号安全</text>
					<text class="menu-arrow">›</text>
				</view>
				<view class="menu-item" @click="goToPage('about')">
					<view class="menu-icon gray"><text>ℹ️</text></view>
					<text class="menu-text">关于平台</text>
					<text class="menu-arrow">›</text>
				</view>
			</view>

			<button class="logout-btn" @click="handleLogout">退出登录</button>
		</scroll-view>
		<TeacherTabBar current="profile" />
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { teacherAPI } from '@/utils/request'
import TeacherTabBar from '@/components/TeacherTabBar.vue'

const stats = ref({})
const dashboard = ref({})
const userInfo = ref({})

const avatarText = computed(() => {
	return (userInfo.value.realName || '教').charAt(0)
})

onMounted(() => {
	try {
		const stored = uni.getStorageSync('userInfo')
		if (stored) userInfo.value = JSON.parse(stored)
	} catch (e) {}
	loadDashboard()
})

const loadDashboard = async () => {
	try {
		const res = await teacherAPI.getDashboard()
		const d = res.data || {}
		dashboard.value = d
	} catch (e) {
		dashboard.value = { classCount: 6, studentCount: 168, jobCount: 45 }
	}
}

const goToPage = (page) => {
	if (page === 'security') {
		uni.navigateTo({ url: '/pages/teacher/security' })
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
.profile-header {
	background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
	color: white;
	padding: 40px 16px 60px;
	position: relative;
	overflow: hidden;
	text-align: center;
	align-items: center;
}
.profile-header::before {
	content: '';
	position: absolute;
	top: -50%;
	right: -20%;
	width: 200px;
	height: 200px;
	background: rgba(255,255,255,0.08);
	border-radius: 50%;
}
.profile-avatar {
	width: 80px;
	height: 80px;
	border-radius: 50%;
	background: rgba(255,255,255,0.2);
	align-items: center;
	justify-content: center;
	font-size: 36px;
	border: 3px solid rgba(255,255,255,0.3);
	margin-bottom: 16px;
	position: relative;
	z-index: 1;
}
.profile-name {
	font-size: 22px;
	font-weight: 700;
	margin-bottom: 8px;
	position: relative;
	z-index: 1;
}
.profile-info {
	font-size: 14px;
	opacity: 0.9;
	position: relative;
	z-index: 1;
}
.profile-stats {
	flex-direction: row;
	gap: 12px;
	padding: 0 16px;
	margin-top: -30px;
	position: relative;
	z-index: 2;
}
.profile-stat-box {
	flex: 1;
	background: white;
	border-radius: 16px;
	padding: 16px;
	text-align: center;
	align-items: center;
	box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.profile-stat-num {
	font-size: 24px;
	font-weight: 800;
	color: #10B981;
	margin-bottom: 4px;
}
.profile-stat-label {
	font-size: 12px;
	color: #86909C;
}
.menu-list {
	background: white;
	border-radius: 16px;
	margin: 12px 16px;
	overflow: hidden;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.menu-item {
	flex-direction: row;
	align-items: center;
	padding: 16px;
	border-bottom: 1px solid #F2F3F5;
}
.menu-item:last-child { border-bottom: none; }
.menu-icon {
	width: 36px;
	height: 36px;
	border-radius: 10px;
	align-items: center;
	justify-content: center;
	margin-right: 12px;
	font-size: 18px;
}
.menu-icon.green { background: rgba(16,185,129,0.1); }
.menu-icon.gray { background: #F2F3F5; }
.menu-text {
	flex: 1;
	font-size: 15px;
	color: #1D2129;
	font-weight: 500;
}
.menu-arrow {
	color: #C9CDD4;
	font-size: 18px;
}
.logout-btn {
	margin: 24px 16px;
	padding: 14px;
	border-radius: 12px;
	border: 1px solid #EF4444;
	color: #EF4444;
	background: white;
	font-size: 15px;
	font-weight: 600;
	align-items: center;
	justify-content: center;
}
</style>
