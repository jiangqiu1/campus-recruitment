<template>
	<view class="page-wrapper">
		<NavBar title="编辑资料" />
		<scroll-view class="content-scrollable" scroll-y>
			<view class="form-card">
				<view class="form-item">
					<text class="form-label">姓名</text>
					<input v-model="form.realName" class="form-input" placeholder="请输入真实姓名" maxlength="20" />
				</view>
				<view class="form-item">
					<text class="form-label">学校</text>
					<input v-model="form.school" class="form-input" placeholder="请输入学校名称" maxlength="50" />
				</view>
				<view class="form-item">
					<text class="form-label">专业</text>
					<input v-model="form.major" class="form-input" placeholder="请输入专业名称" maxlength="50" />
				</view>
				<view class="form-item">
					<text class="form-label">手机号</text>
					<input v-model="form.phone" class="form-input" placeholder="请输入手机号" maxlength="11" type="number" />
				</view>
				<view class="form-item">
					<text class="form-label">邮箱</text>
					<input v-model="form.email" class="form-input" placeholder="请输入邮箱" maxlength="50" type="text" />
				</view>
				<view class="form-item" style="border:none;">
					<text class="form-label">性别</text>
					<view class="gender-group">
						<text class="gender-option" :class="{ active: form.gender === 1 }" @click="form.gender = 1">男</text>
						<text class="gender-option" :class="{ active: form.gender === 2 }" @click="form.gender = 2">女</text>
						<text class="gender-option" :class="{ active: form.gender === 0 }" @click="form.gender = 0">保密</text>
					</view>
				</view>
			</view>

			<button class="save-btn" :disabled="saving" @click="handleSave">
				{{ saving ? '保存中...' : '保存' }}
			</button>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { authAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'

const saving = ref(false)
const form = ref({
	realName: '',
	school: '',
	major: '',
	phone: '',
	email: '',
	gender: 0
})

onMounted(async () => {
	// 优先从 API 获取最新用户信息（手机号/邮箱等仅在服务端有完整数据）
	try {
		const userRes = await authAPI.getUserInfo()
		if (userRes.data) {
			const u = userRes.data
			form.value.realName = u.realName || ''
			form.value.school = u.school || ''
			form.value.major = u.major || ''
			form.value.phone = u.phone || ''
			form.value.email = u.email || ''
			form.value.gender = u.gender != null ? u.gender : 0
			// 同步到 localStorage
			const raw = uni.getStorageSync('userInfo')
			if (raw) {
				const ui = { ...JSON.parse(raw), ...u }
				uni.setStorageSync('userInfo', JSON.stringify(ui))
			}
			return
		}
	} catch (e) { /* API 不可用时降级到 localStorage */ }

	// 降级：从 localStorage 读取
	try {
		const stored = uni.getStorageSync('userInfo')
		if (stored) {
			const ui = JSON.parse(stored)
			form.value.realName = ui.realName || ''
			form.value.school = ui.school || ''
			form.value.major = ui.major || ''
			form.value.phone = ui.phone || ''
			form.value.email = ui.email || ''
			form.value.gender = ui.gender != null ? ui.gender : 0
		}
	} catch (e) {}
})

const handleSave = async () => {
	if (!form.value.realName.trim()) {
		uni.showToast({ title: '请输入姓名', icon: 'none' })
		return
	}
	saving.value = true
	try {
		const res = await authAPI.updateProfile({
			realName: form.value.realName.trim(),
			school: form.value.school,
			major: form.value.major,
			phone: form.value.phone,
			email: form.value.email,
			gender: form.value.gender
		})
		if (res.code === 200) {
			// 更新本地存储的 userInfo
			const stored = uni.getStorageSync('userInfo')
			if (stored) {
				const ui = JSON.parse(stored)
				const updated = { ...ui, ...res.data }
				uni.setStorageSync('userInfo', JSON.stringify(updated))
			}
			uni.showToast({ title: '保存成功', icon: 'success' })
			setTimeout(() => uni.navigateBack(), 1500)
		} else {
			uni.showToast({ title: res.message || '保存失败', icon: 'none' })
		}
	} catch (e) {
		uni.showToast({ title: '保存失败', icon: 'none' })
	} finally {
		saving.value = false
	}
}
</script>

<style scoped>
.form-card {
	background: #FFFFFF;
	border-radius: 12px;
	margin: 16px;
	overflow: hidden;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.form-item {
	flex-direction: row;
	align-items: center;
	padding: 16px;
	border-bottom: 0.5px solid #F2F3F5;
	min-height: 52px;
}
.form-label {
	width: 60px;
	font-size: 15px;
	color: #1D2129;
	font-weight: 500;
	flex-shrink: 0;
}
.form-input {
	flex: 1;
	font-size: 15px;
	color: #1D2129;
	padding: 0;
	height: 36px;
}
.form-input::placeholder { color: #C9CDD4; }
.gender-group { flex-direction: row; gap: 10px; }
.gender-option {
	padding: 6px 20px;
	border-radius: 20px;
	font-size: 14px;
	color: #86909C;
	background: #F7F8FA;
	border: 1px solid #E5E6EB;
}
.gender-option.active {
	color: #165DFF;
	background: rgba(22,93,255,0.06);
	border-color: #165DFF;
}
.save-btn {
	width: calc(100% - 32px);
	margin: 0 16px 20px;
	height: 46px;
	border-radius: 12px;
	background: #165DFF;
	color: #FFFFFF;
	font-size: 16px;
	font-weight: 600;
	border: none;
	align-items: center;
	justify-content: center;
}
.save-btn:active { opacity: 0.85; }
.save-btn[disabled] { background: #A9AEB8; opacity: 0.6; }
</style>
