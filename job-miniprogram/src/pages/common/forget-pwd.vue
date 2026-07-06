<template>
	<view class="page-wrapper">
		<NavBar title="忘记密码" show-back />
		<view class="form-container">
			<view class="form-card">
				<view class="input-group">
					<text class="input-label">账号</text>
					<input class="input-field" v-model="username" placeholder="请输入账号/手机号" type="number" maxlength="11" />
				</view>
				<view class="input-group">
					<text class="input-label">验证码</text>
					<view class="code-row">
						<input class="input-field code-input" v-model="code" placeholder="请输入验证码" type="number" maxlength="6" />
						<button class="code-btn" :disabled="countdown > 0" @click="sendCode">{{ countdown > 0 ? countdown + 's后重发' : '获取验证码' }}</button>
					</view>
				</view>
				<view class="input-group">
					<text class="input-label">新密码</text>
					<input class="input-field" v-model="newPassword" type="password" placeholder="请输入新密码（6-20位）" />
				</view>
				<view class="input-group">
					<text class="input-label">确认密码</text>
					<input class="input-field" v-model="confirmPassword" type="password" placeholder="请再次输入新密码" />
				</view>
				<button class="submit-btn" :disabled="loading" @click="handleReset">{{ loading ? '重置中...' : '重置密码' }}</button>
				<text class="tip-text">密码重置成功后，请使用新密码登录</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import NavBar from '@/components/NavBar.vue'

const username = ref('')
const code = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const countdown = ref(0)
let timer = null

const sendCode = () => {
	if (!username.value) { uni.showToast({ title: '请先输入账号', icon: 'none' }); return }
	if (!/^1\d{10}$/.test(username.value)) { uni.showToast({ title: '请输入正确的手机号', icon: 'none' }); return }
	countdown.value = 60
	timer = setInterval(() => {
		countdown.value--
		if (countdown.value <= 0) { clearInterval(timer); timer = null }
	}, 1000)
	uni.showToast({ title: '验证码已发送', icon: 'success' })
}

const handleReset = async () => {
	if (!username.value) { uni.showToast({ title: '请输入账号', icon: 'none' }); return }
	if (!code.value) { uni.showToast({ title: '请输入验证码', icon: 'none' }); return }
	if (!newPassword.value || newPassword.value.length < 6) { uni.showToast({ title: '密码至少6位', icon: 'none' }); return }
	if (newPassword.value !== confirmPassword.value) { uni.showToast({ title: '两次密码不一致', icon: 'none' }); return }
	loading.value = true
	try {
		setTimeout(() => {
			uni.showToast({ title: '密码重置成功', icon: 'success' })
			setTimeout(() => { uni.navigateBack() }, 1500)
		}, 800)
	} catch (e) { uni.showToast({ title: '重置失败，请重试', icon: 'none' }) }
	finally { loading.value = false }
}
</script>

<style scoped>
.page-wrapper { min-height: 100vh; background: #F7F8FA; }
.form-container { padding: 24px 16px; }
.form-card { background: #FFFFFF; border-radius: 12px; padding: 24px 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.04); }
.input-group { margin-bottom: 20px; }
.input-label { font-size: 14px; color: #4E5969; margin-bottom: 8px; display: block; font-weight: 500; }
.input-field { width: 100%; height: 48px; border: 1px solid #E5E6EB; border-radius: 8px; padding: 0 14px; font-size: 15px; color: #1D2129; background: #F7F8FA; box-sizing: border-box; }
.input-field:focus { border-color: #165DFF; background: #FFFFFF; }
.code-row { flex-direction: row; gap: 12px; align-items: center; }
.code-input { flex: 1; }
.code-btn { width: 110px; height: 48px; background: rgba(22,93,255,0.08); color: #165DFF; font-size: 13px; font-weight: 500; border-radius: 8px; border: none; flex-shrink: 0; }
.code-btn[disabled] { background: #F2F3F5; color: #C9CDD4; }
.submit-btn { width: 100%; height: 48px; border-radius: 12px; background: #165DFF; color: #FFFFFF; font-size: 16px; font-weight: 600; border: none; margin-top: 8px; }
.submit-btn[disabled] { background: #E5E6EB !important; color: #A9AEB8 !important; }
.tip-text { display: block; text-align: center; font-size: 12px; color: #C9CDD4; margin-top: 16px; }
</style>
