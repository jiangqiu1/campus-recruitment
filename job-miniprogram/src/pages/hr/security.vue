<template>
	<view class="page-wrapper">
		<NavBar title="账号安全" showBack @back="goBack" />
		<scroll-view class="content-scrollable" scroll-y>
			<view class="menu-list">
				<view class="menu-item" @click="handleChangePwd">
					<view class="menu-icon blue"><text>🔑</text></view>
					<text class="menu-text">修改密码</text>
					<text class="menu-value">••••••</text>
					<text class="menu-arrow">›</text>
				</view>
				<view class="menu-item" @click="handleBindPhone">
					<view class="menu-icon green"><text>📱</text></view>
					<text class="menu-text">绑定手机</text>
					<text class="menu-value">{{ phoneDisplay }}</text>
					<text class="menu-arrow">›</text>
				</view>
				<view class="menu-item" @click="handleBindEmail">
					<view class="menu-icon orange"><text>📧</text></view>
					<text class="menu-text">绑定邮箱</text>
					<text class="menu-value">{{ emailDisplay }}</text>
					<text class="menu-arrow">›</text>
				</view>
				<view class="menu-item" style="border:none;" @click="handleDeleteAccount">
					<view class="menu-icon red"><text>🚫</text></view>
					<text class="menu-text">账号注销</text>
					<text class="menu-arrow">›</text>
				</view>
			</view>

			<view class="info-card">
				<text class="info-title">安全建议</text>
				<text class="info-text">• 定期修改密码，避免使用简单密码</text>
				<text class="info-text">• 绑定手机号和邮箱，方便找回密码</text>
				<text class="info-text">• 不要在公共场所保存登录状态</text>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { authAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'

const phoneDisplay = ref('未绑定')
const emailDisplay = ref('未绑定')

onMounted(() => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (raw) {
			const info = JSON.parse(raw)
			if (info.phone) phoneDisplay.value = info.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
			if (info.email) emailDisplay.value = info.email
		}
	} catch (e) {}
})

const handleChangePwd = () => {
	uni.showModal({
		title: '修改密码',
		content: '请输入旧密码和新密码',
		editable: true,
		placeholderText: '旧密码',
		success: (res) => {
			if (!res.confirm) return
			uni.showModal({
				title: '新密码',
				content: '请输入新密码（至少6位）',
				editable: true,
				placeholderText: '新密码',
				success: async (res2) => {
					if (!res2.confirm) return
					try {
						await authAPI.updatePassword({ oldPassword: res.content, newPassword: res2.content })
						uni.showToast({ title: '密码修改成功', icon: 'success' })
					} catch (e) {
						uni.showToast({ title: '修改失败，请检查旧密码', icon: 'none' })
					}
				}
			})
		}
	})
}

const handleBindPhone = () => {
	uni.showToast({ title: '功能开发中', icon: 'none' })
}

const handleBindEmail = () => {
	uni.showToast({ title: '功能开发中', icon: 'none' })
}

const handleDeleteAccount = () => {
	uni.showModal({
		title: '警告',
		content: '确定要注销账号吗？此操作不可恢复！',
		success: (res) => {
			if (res.confirm) {
				uni.showToast({ title: '请联系管理员注销', icon: 'none' })
			}
		}
	})
}

const goBack = () => {
	uni.navigateBack()
}
</script>

<style scoped>
.menu-list {
	background: white;
	border-radius: 16px;
	margin: 16px;
	overflow: hidden;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.menu-item {
	flex-direction: row;
	align-items: center;
	padding: 16px;
	border-bottom: 1px solid #F2F3F5;
}
.menu-icon {
	width: 36px;
	height: 36px;
	border-radius: 10px;
	align-items: center;
	justify-content: center;
	margin-right: 12px;
	font-size: 18px;
}
.menu-icon.blue { background: rgba(22,93,255,0.1); }
.menu-icon.green { background: rgba(16,185,129,0.1); }
.menu-icon.orange { background: rgba(245,158,11,0.1); }
.menu-icon.red { background: rgba(239,68,68,0.1); }
.menu-text {
	flex: 1;
	font-size: 15px;
	color: #1D2129;
	font-weight: 500;
}
.menu-value {
	font-size: 14px;
	color: #86909C;
	margin-right: 8px;
}
.menu-arrow { color: #C9CDD4; font-size: 18px; }
.info-card {
	background: white;
	border-radius: 16px;
	margin: 0 16px 16px;
	padding: 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.info-title {
	font-size: 16px;
	font-weight: 700;
	color: #1D2129;
	margin-bottom: 12px;
	display: block;
}
.info-text {
	font-size: 13px;
	color: #86909C;
	line-height: 1.8;
	display: block;
}
</style>
