<template>
	<view class="page-wrapper">
		<view class="login-container">
			<view class="login-header">
				<text class="login-logo">🎓</text>
				<text class="login-title">职业院校招聘就业平台</text>
				<text class="login-subtitle">学生端 - AI 增强求职</text>
			</view>
			<view class="login-form">
				<view class="input-group">
					<text class="input-label">学号</text>
					<input class="input-field" v-model="username" placeholder="请输入学号" />
				</view>
				<view class="input-group">
					<text class="input-label">密码</text>
					<input class="input-field" v-model="password" type="password" placeholder="请输入密码" @confirm="handleLogin" />
				</view>
				<button class="login-btn" :loading="loading" @click="handleLogin">
					<text v-if="!loading">登 录</text>
					<text v-else>登录中...</text>
				</button>
			</view>
			<view class="login-footer">
				<text class="footer-text">实训平台 · 学生端 v1.0</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { authAPI } from '@/utils/request'

const username = ref('')
const password = ref('')
const loading = ref(false)

const handleLogin = async () => {
	if (!username.value || !password.value) {
		uni.showToast({ title: '请输入学号和密码', icon: 'none' })
		return
	}
	loading.value = true
	try {
		const res = await authAPI.login({
			username: username.value,
			password: password.value,
			role: 0
		})
		const data = res.data || res
		const token = data.token
		uni.setStorageSync('token', token)
		// data = { token, userId, realName, ... }
		// Also handle data.userInfo = { userId, ... } format
		const ui = data.userInfo || data
		const userInfo = {
			id: ui.userId || ui.id || data.userId || data.id,
			username: ui.username || data.username,
			realName: ui.realName || data.realName,
			role: ui.role || data.role,
			avatarUrl: ui.avatarUrl || data.avatarUrl
		}
		uni.setStorageSync('userInfo', JSON.stringify(userInfo))
		uni.showToast({ title: '登录成功', icon: 'success' })
		setTimeout(() => {
			uni.reLaunch({ url: '/pages/student/home' })
		}, 500)
	} catch (e) {
		console.error('登录失败', e)
	} finally {
		loading.value = false
	}
}
</script>

<style scoped>
.login-container {
	flex: 1;
	display: flex;
	flex-direction: column;
	justify-content: center;
	padding: 40px 32px;
	background: linear-gradient(180deg, #165DFF 0%, #2563EB 40%, #F5F7FA 40%);
}
.login-header {
	align-items: center;
	margin-bottom: 40px;
}
.login-logo {
	font-size: 64px;
	margin-bottom: 16px;
}
.login-title {
	font-size: 22px;
	font-weight: 700;
	color: #fff;
	margin-bottom: 8px;
}
.login-subtitle {
	font-size: 14px;
	color: rgba(255,255,255,0.8);
}
.login-form {
	background: #fff;
	border-radius: 20px;
	padding: 32px 24px;
	box-shadow: 0 8px 30px rgba(0,0,0,0.08);
}
.input-group {
	margin-bottom: 20px;
}
.input-label {
	font-size: 14px;
	font-weight: 600;
	color: #1D2129;
	margin-bottom: 8px;
	display: block;
}
.input-field {
	width: 100%;
	height: 48px;
	border: 2px solid #E2E8F0;
	border-radius: 12px;
	padding: 0 16px;
	font-size: 16px;
	background: #F8F9FC;
	transition: border-color 0.2s;
}
.input-field:focus {
	border-color: #165DFF;
	background: #fff;
}
.login-btn {
	width: 100%;
	height: 50px;
	border-radius: 12px;
	background: linear-gradient(135deg, #165DFF, #2563EB);
	color: white;
	font-size: 17px;
	font-weight: 700;
	border: none;
	margin-top: 12px;
	align-items: center;
	justify-content: center;
	box-shadow: 0 4px 14px rgba(22,93,255,0.4);
}
.login-btn:active {
	opacity: 0.9;
	transform: scale(0.98);
}
.login-footer {
	align-items: center;
	margin-top: 32px;
}
.footer-text {
	font-size: 12px;
	color: #86909C;
}
</style>
