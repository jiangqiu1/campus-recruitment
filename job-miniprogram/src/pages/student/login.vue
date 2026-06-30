<template>
	<view class="page-wrapper">
		<view class="login-container">
			<view class="login-header">
				<text class="login-logo">🎓</text>
				<text class="login-title">职业院校招聘就业平台</text>
				<text class="login-subtitle">校企协同 · AI 精准就业</text>
			</view>

			<!-- 身份切换 -->
			<view class="role-switcher">
				<view class="role-tab" :class="{ active: role === 0 }" @click="role = 0">
					<text class="role-icon">👨‍🎓</text>
					<text class="role-name">学生</text>
				</view>
				<view class="role-tab" :class="{ active: role === 1 }" @click="role = 1">
					<text class="role-icon">👨‍🏫</text>
					<text class="role-name">教师</text>
				</view>
				<view class="role-tab" :class="{ active: role === 2 }" @click="role = 2">
					<text class="role-icon">👔</text>
					<text class="role-name">HR</text>
				</view>
			</view>

			<view class="login-form">
				<view class="input-group">
					<text class="input-label">{{ roleLabel }}账号</text>
					<input class="input-field" v-model="username" :placeholder="'请输入' + roleLabel + '账号'" />
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
				<text class="footer-text">实训平台 · {{ roleName }}端 v2.0</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { authAPI } from '@/utils/request'

const username = ref('')
const password = ref('')
const loading = ref(false)
const role = ref(0)

const roleLabel = computed(() => {
	return ['学号', '工号', '企业账号'][role.value] || '账号'
})

const roleName = computed(() => {
	return ['学生', '教师', '企业HR'][role.value] || ''
})

const rolePrefix = computed(() => {
	return ['student', 'teacher', 'hr'][role.value] || 'student'
})

const handleLogin = async () => {
	if (!username.value || !password.value) {
		uni.showToast({ title: '请输入' + roleLabel.value + '和密码', icon: 'none' })
		return
	}
	loading.value = true
	try {
		const res = await authAPI.login({
			username: username.value,
			password: password.value,
			role: role.value
		})
		const body = res.data || res
		const token = body.token
		if (!token) {
			uni.showToast({ title: res.message || '登录失败', icon: 'none' })
			return
		}
		uni.setStorageSync('token', token)

		// 提取用户信息
		const ui = body.userInfo || body
		const userInfo = {
			id: ui.userId || ui.id || body.userId || body.id,
			username: ui.username || body.username || username.value,
			realName: ui.realName || body.realName,
			role: ui.role ?? body.role ?? role.value,
			avatarUrl: ui.avatarUrl || body.avatarUrl || '',
			companyId: ui.companyId || body.companyId
		}
		uni.setStorageSync('userInfo', JSON.stringify(userInfo))
		uni.showToast({ title: '登录成功', icon: 'success' })

		// 按角色跳转
		const homePages = ['/pages/student/home', '/pages/teacher/home', '/pages/hr/home']
		setTimeout(() => {
			uni.reLaunch({ url: homePages[role.value] || homePages[0] })
		}, 500)
	} catch (e) {
		console.error('登录失败', e)
		// If the interceptor already showed error, don't double-toast
	} finally {
		loading.value = false
	}
}
</script>

<style scoped>
.page-wrapper {
	min-height: 100vh;
	background: linear-gradient(180deg, #165DFF 0%, #2563EB 35%, #F5F7FA 35%);
}
.login-container {
	display: flex;
	flex-direction: column;
	justify-content: center;
	padding: 40px 32px;
	min-height: 100vh;
}
.login-header {
	align-items: center;
	margin-bottom: 28px;
}
.login-logo {
	font-size: 56px;
	margin-bottom: 12px;
}
.login-title {
	font-size: 22px;
	font-weight: 700;
	color: #fff;
	margin-bottom: 4px;
}
.login-subtitle {
	font-size: 14px;
	color: rgba(255,255,255,0.8);
}
/* 身份切换 */
.role-switcher {
	flex-direction: row;
	background: rgba(255,255,255,0.15);
	border-radius: 14px;
	padding: 4px;
	margin-bottom: 24px;
	gap: 4px;
}
.role-tab {
	flex: 1;
	align-items: center;
	padding: 10px 0;
	border-radius: 12px;
	transition: all 0.2s;
}
.role-tab.active {
	background: rgba(255,255,255,0.95);
	box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.role-icon {
	font-size: 24px;
	margin-bottom: 2px;
}
.role-name {
	font-size: 13px;
	font-weight: 600;
	color: #165DFF;
}
.role-tab.active .role-name {
	color: #1D2129;
}
/* 表单 */
.login-form {
	background: #fff;
	border-radius: 20px;
	padding: 28px 24px;
	box-shadow: 0 8px 30px rgba(0,0,0,0.08);
}
.input-group {
	margin-bottom: 18px;
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
	box-sizing: border-box;
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
	margin-top: 10px;
	align-items: center;
	justify-content: center;
	box-shadow: 0 4px 14px rgba(22,93,255,0.4);
}
.login-btn:active {
	opacity: 0.9;
}
.login-footer {
	align-items: center;
	margin-top: 28px;
}
.footer-text {
	font-size: 12px;
	color: rgba(255,255,255,0.6);
}
</style>
