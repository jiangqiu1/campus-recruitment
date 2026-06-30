<template>
	<view class="page-wrapper">
		<scroll-view class="content-scrollable" scroll-y>
			<!-- 个人资料头部 -->
			<view class="profile-header">
				<view class="profile-avatar">
					<text>{{ avatarText }}</text>
				</view>
				<text class="profile-name">{{ userInfo.realName || 'HR用户' }}</text>
				<view class="role-badge">
					<text class="role-icon">🏢</text>
					<text class="role-text">企业招聘方</text>
				</view>
			</view>

			<!-- 统计卡片 -->
			<view class="profile-stats">
				<view class="profile-stat-box">
					<text class="profile-stat-num">{{ stats.jobCount || 0 }}</text>
					<text class="profile-stat-label">岗位数</text>
				</view>
				<view class="profile-stat-box">
					<text class="profile-stat-num">{{ stats.resumeProcessed || 0 }}</text>
					<text class="profile-stat-label">简历处理</text>
				</view>
				<view class="profile-stat-box">
					<text class="profile-stat-num">{{ stats.interviewCount || 0 }}</text>
					<text class="profile-stat-label">面试数</text>
				</view>
			</view>

			<!-- 菜单列表 -->
			<view class="menu-list">
				<view class="menu-item" @click="goToCompany">
					<view class="menu-icon blue"><text>🏢</text></view>
					<text class="menu-text">企业信息</text>
					<text class="menu-arrow">›</text>
				</view>
				<view class="menu-item" @click="goToSecurity">
					<view class="menu-icon green"><text>🔒</text></view>
					<text class="menu-text">账号安全</text>
					<text class="menu-arrow">›</text>
				</view>
				<view class="menu-item" @click="goToStats">
					<view class="menu-icon orange"><text>📊</text></view>
					<text class="menu-text">数据统计</text>
					<text class="menu-arrow">›</text>
				</view>
				<view class="menu-item" @click="goToAbout">
					<view class="menu-icon gray"><text>ℹ️</text></view>
					<text class="menu-text">关于平台</text>
					<text class="menu-arrow">›</text>
				</view>
			</view>

			<button class="logout-btn" @click="handleLogout">退出登录</button>
		</scroll-view>
		<HrTabBar current="profile" />
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { hrAPI } from '@/utils/request'
import HrTabBar from '@/components/HrTabBar.vue'

const userInfo = ref({})
const stats = ref({})

const avatarText = computed(() => {
	return (userInfo.value.realName || 'H').charAt(0)
})

onMounted(() => {
	try {
		const stored = uni.getStorageSync('userInfo')
		if (stored) userInfo.value = JSON.parse(stored)
	} catch (e) {}
	loadStats()
})

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

const goToSecurity = () => {
	uni.navigateTo({ url: '/pages/hr/security' })
}

const goToStats = () => {
	uni.navigateTo({ url: '/pages/hr/stats' })
}

const goToAbout = () => {
	uni.showToast({ title: '功能开发中', icon: 'none' })
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
	background: linear-gradient(135deg, #0EA5E9 0%, #38BDF8 100%);
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
.role-badge {
	flex-direction: row;
	align-items: center;
	gap: 6px;
	background: rgba(255,255,255,0.2);
	padding: 6px 16px;
	border-radius: 20px;
	position: relative;
	z-index: 1;
}
.role-icon { font-size: 14px; }
.role-text { font-size: 12px; font-weight: 500; }

/* 统计数据 */
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
	color: #0EA5E9;
	margin-bottom: 4px;
}
.profile-stat-label {
	font-size: 12px;
	color: #86909C;
}

/* 菜单列表 */
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
.menu-icon.blue { background: rgba(14,165,233,0.1); }
.menu-icon.green { background: rgba(16,185,129,0.1); }
.menu-icon.orange { background: rgba(245,158,11,0.1); }
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

/* 退出按钮 */
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
