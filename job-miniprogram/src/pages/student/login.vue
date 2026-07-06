<template>
	<view class="page-wrapper">
		<view class="login-container">
			<!-- 头部：品牌区 -->
			<view class="login-header">
				<view class="login-badge">
					<text class="badge-letter">聘</text>
				</view>
				<text class="login-title">职业院校招聘就业平台</text>
				<text class="login-subtitle">校企协同 · AI 精准匹配</text>
			</view>

			<!-- Signature：身份选择器（三个门户） -->
			<view class="role-switcher">
				<view class="role-card" :class="{ active: role === 0 }" @click="role = 0">
					<view class="role-icon-wrap">
						<uni-icons type="person" size="22" :color="role === 0 ? '#FFFFFF' : '#4E5969'" />
					</view>
					<text class="role-name" :class="{ active: role === 0 }">学生</text>
				</view>
				<view class="role-card" :class="{ active: role === 1 }" @click="role = 1">
					<view class="role-icon-wrap">
						<uni-icons type="staff" size="22" :color="role === 1 ? '#FFFFFF' : '#4E5969'" />
					</view>
					<text class="role-name" :class="{ active: role === 1 }">教师</text>
				</view>
				<view class="role-card" :class="{ active: role === 2 }" @click="role = 2">
					<view class="role-icon-wrap">
						<uni-icons type="person" size="22" :color="role === 2 ? '#FFFFFF' : '#4E5969'" />
					</view>
					<text class="role-name" :class="{ active: role === 2 }">企业</text>
				</view>
			</view>

			<!-- 表单卡片 -->
			<view class="form-card">
				<view class="input-wrap">
					<uni-icons type="person" size="16" color="#C9CDD4" />
					<input class="input-field" v-model="username" :placeholder="'请输入' + roleLabel" @confirm="handleLogin" />
				</view>
				<view class="input-wrap" :class="{ 'pwd-visible': showPwd }" @click="showPwd = !showPwd">
					<uni-icons type="locked" size="16" color="#C9CDD4" />
					<input class="input-field" v-model="password" :password="!showPwd" placeholder="点击切换明文/密文" @confirm="handleLogin" />
				</view>
				<view class="form-options">
					<view class="agree-row" @click="agree = !agree">
						<uni-icons :type="agree ? 'checkbox-filled' : 'circle'" size="16" :color="agree ? '#165DFF' : '#C9CDD4'" />
						<text class="agree-text">登录即代表同意</text>
						<text class="agree-link" @click.stop="goToAgreement">《用户协议》</text>
						<text class="agree-text">和</text>
						<text class="agree-link" @click.stop="goToPrivacy">《隐私政策》</text>
					</view>
				</view>
				<button class="login-btn" :disabled="loading || !agree" @click="handleLogin">
					<text v-if="!loading">登 录</text>
					<text v-else>登录中...</text>
				</button>
				<view class="forgot-row">
					<text class="link-text" @click="goToForgetPwd">忘记密码？</text>
				</view>
			</view>

			<!-- 微信登录 -->
			<view class="divider-row">
				<view class="divider-line" />
				<text class="divider-text">其他方式</text>
				<view class="divider-line" />
			</view>
			<view class="wechat-btn" @click="handleWechatLogin">
				<uni-icons type="chat" size="18" color="#07C160" />
				<text class="wechat-text">微信一键登录</text>
			</view>

			<view class="login-footer">
				<text class="footer-text">{{ roleName }}端 v2.0</text>
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
const agree = ref(false)
const showPwd = ref(false)

const roleLabel = computed(() => ['学号', '工号', '企业账号'][role.value] || '账号')
const roleName = computed(() => ['学生', '教师', '企业HR'][role.value] || '')

const handleLogin = async () => {
	if (!agree.value) {
		uni.showToast({ title: '请先同意用户协议和隐私政策', icon: 'none' })
		return
	}
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
			const msg = body.message || '登录失败'
			if (msg.includes('密码')) uni.showToast({ title: '密码错误，请重试', icon: 'none' })
			else if (msg.includes('不存在') || msg.includes('未找到')) uni.showToast({ title: '账号不存在', icon: 'none' })
			else if (msg.includes('冻结') || msg.includes('禁用')) uni.showToast({ title: '账号已被冻结，请联系管理员', icon: 'none' })
			else uni.showToast({ title: msg, icon: 'none' })
			return
		}
		uni.setStorageSync('token', token)
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
		const homePages = ['/pages/student/home', '/pages/teacher/home', '/pages/hr/home']
		setTimeout(() => uni.reLaunch({ url: homePages[role.value] || homePages[0] }), 500)
	} catch (e) {
		console.error('登录失败', e)
		const msg = e?.message || e?.data?.message || '网络错误，请稍后重试'
		if (msg.includes('密码')) uni.showToast({ title: '密码错误，请重试', icon: 'none' })
		else if (msg.includes('不存在') || msg.includes('未找到')) uni.showToast({ title: '账号不存在', icon: 'none' })
		else if (msg.includes('冻结') || msg.includes('禁用')) uni.showToast({ title: '账号已被冻结，请联系管理员', icon: 'none' })
		else uni.showToast({ title: msg, icon: 'none' })
	} finally {
		loading.value = false
	}
}

const handleWechatLogin = () => uni.showToast({ title: '微信登录开发中', icon: 'none' })
const goToForgetPwd = () => uni.navigateTo({ url: '/pages/common/forget-pwd' })
const goToAgreement = () => uni.navigateTo({ url: '/pages/common/agreement' })
const goToPrivacy = () => uni.navigateTo({ url: '/pages/common/privacy' })
</script>

<style scoped>
/* ===== 页面背景：平滑蓝色渐变，无硬分割 ===== */
.page-wrapper {
	min-height: 100vh;
	background: linear-gradient(170deg, #165DFF 0%, #3B7AFF 55%, #E8EDFF 80%, #F7F8FA 100%);
}
.login-container {
	padding: 60px 32px 32px;
	min-height: 100vh;
	box-sizing: border-box;
	padding-bottom: calc(32px + env(safe-area-inset-bottom));
}

/* ===== 品牌头部 ===== */
.login-header {
	align-items: center;
	margin-bottom: 32px;
	gap: 6px;
}
.login-badge {
	width: 56px;
	height: 56px;
	border-radius: 16px;
	background: rgba(255,255,255,0.85);
	align-items: center;
	justify-content: center;
	margin-bottom: 8px;
	backdrop-filter: blur(4px);
	box-shadow: 0 4px 16px rgba(22,93,255,0.15);
}
.badge-letter {
	font-size: 28px;
	font-weight: 700;
	color: #165DFF;
	line-height: 1;
}
.login-title {
	font-size: 22px;
	font-weight: 700;
	color: #FFFFFF;
	letter-spacing: 0.05em;
}
.login-subtitle {
	font-size: 13px;
	color: rgba(255,255,255,0.7);
	letter-spacing: 0.02em;
}

/* ===== Signature：三角色卡片式选择器 ===== */
.role-switcher {
	flex-direction: row;
	gap: 10px;
	margin-bottom: 24px;
}
.role-card {
	flex: 1;
	align-items: center;
	padding: 14px 0;
	border-radius: 12px;
	background: rgba(255,255,255,0.15);
	border: 1px solid rgba(255,255,255,0.2);
	transition: all 0.2s ease;
	gap: 6px;
}
.role-card.active {
	background: #FFFFFF;
	border-color: transparent;
	box-shadow: 0 4px 16px rgba(22,93,255,0.15);
	transform: scale(1.03);
}
.role-card:active { transform: scale(0.97); }
.role-icon-wrap {
	width: 36px;
	height: 36px;
	border-radius: 50%;
	align-items: center;
	justify-content: center;
}
.role-card.active .role-icon-wrap {
	background: #165DFF;
}
.role-name {
	font-size: 13px;
	font-weight: 500;
	color: rgba(255,255,255,0.8);
}
.role-name.active {
	color: #1D2129;
	font-weight: 600;
}

/* ===== 表单卡片 ===== */
.form-card {
	background: #FFFFFF;
	border-radius: 16px;
	padding: 24px 20px;
	box-shadow: 0 4px 24px rgba(22,93,255,0.08);
	margin-bottom: 24px;
}
.input-wrap {
	flex-direction: row;
	align-items: center;
	gap: 10px;
	height: 48px;
	border: 1px solid #E5E6EB;
	border-radius: 10px;
	padding: 0 14px;
	background: #F7F8FA;
	margin-bottom: 14px;
	transition: all 0.2s;
}
.input-wrap:focus-within {
	border-color: #165DFF;
	background: #FFFFFF;
	box-shadow: 0 0 0 3px rgba(22,93,255,0.08);
}
.input-field {
	flex: 1;
	height: 100%;
	font-size: 15px;
	color: #1D2129;
	border: none;
	background: transparent;
	padding: 0;
}
.input-wrap.pwd-visible {
	border-color: #165DFF;
	background: #FFFFFF;
}

/* 协议行 */
.form-options { margin-top: 4px; }
.agree-row {
	flex-direction: row;
	align-items: center;
	gap: 4px;
	flex-wrap: wrap;
}
.agree-text { font-size: 12px; color: #86909C; }
.agree-link { font-size: 12px; color: #165DFF; }

/* 登录按钮 */
.login-btn {
	width: 100%;
	height: 48px;
	border-radius: 12px;
	background: linear-gradient(135deg, #165DFF, #3B7AFF);
	color: #FFFFFF;
	font-size: 16px;
	font-weight: 700;
	border: none;
	margin-top: 20px;
	margin-left: 0;
	margin-right: 0;
	padding: 0;
	align-items: center;
	justify-content: center;
	line-height: 48px;
	box-shadow: 0 4px 14px rgba(22,93,255,0.3);
	letter-spacing: 0.1em;
}
.login-btn[disabled] {
	background: #E5E6EB !important;
	color: #A9AEB8 !important;
	box-shadow: none !important;
}
.login-btn:active { transform: scale(0.98); opacity: 0.9; }

.forgot-row {
	align-items: center;
	margin-top: 14px;
}
.link-text { font-size: 13px; color: #C9CDD4; }

/* ===== 微信区 ===== */
.divider-row {
	flex-direction: row;
	align-items: center;
	gap: 12px;
	margin-bottom: 16px;
}
.divider-line {
	flex: 1;
	height: 1px;
	background: rgba(255,255,255,0.35);
}
.divider-text {
	font-size: 12px;
	color: rgba(255,255,255,0.6);
	flex-shrink: 0;
}
.wechat-btn {
	flex-direction: row;
	align-items: center;
	justify-content: center;
	gap: 8px;
	padding: 14px;
	background: rgba(255,255,255,0.9);
	border-radius: 12px;
	backdrop-filter: blur(4px);
}
.wechat-text { font-size: 15px; color: #1D2129; font-weight: 500; }
.wechat-btn:active { transform: scale(0.98); }

/* ===== 底部 ===== */
.login-footer {
	align-items: center;
	margin-top: 24px;
}
.footer-text {
	font-size: 11px;
	color: rgba(255,255,255,0.4);
}
</style>
