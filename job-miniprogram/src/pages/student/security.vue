<template>
	<view class="page-wrapper">
		<NavBar title="账号安全" />
		<scroll-view class="content-scrollable" scroll-y>
			<view class="menu-list">
				<view class="menu-item" @click="handleChangePwd">
					<view class="menu-icon blue"><uni-icons type="locked" size="20" color="#165DFF" /></view>
					<text class="menu-text">修改密码</text>
					<text class="menu-value">••••••</text>
					<uni-icons type="arrowright" size="16" color="#C9CDD4" />
				</view>
				<view class="menu-item" @click="handleBindPhone">
					<view class="menu-icon green"><uni-icons type="phone" size="20" color="#00B42A" /></view>
					<text class="menu-text">绑定手机</text>
					<text class="menu-value">{{ userInfo.phone || '未绑定' }}</text>
					<uni-icons type="arrowright" size="16" color="#C9CDD4" />
				</view>
				<view class="menu-item" @click="handleBindEmail">
					<view class="menu-icon orange"><uni-icons type="email" size="20" color="#FF7D00" /></view>
					<text class="menu-text">绑定邮箱</text>
					<text class="menu-value">{{ userInfo.email || '未绑定' }}</text>
					<uni-icons type="arrowright" size="16" color="#C9CDD4" />
				</view>
				<view class="menu-item" style="border:none;" @click="handleDeleteAccount">
					<view class="menu-icon red"><uni-icons type="trash" size="20" color="#F53F3F" /></view>
					<text class="menu-text">账号注销</text>
					<uni-icons type="arrowright" size="16" color="#C9CDD4" />
				</view>
			</view>
			<view class="info-card">
				<text class="info-title">安全建议</text>
				<text class="info-text">· 定期修改密码，避免使用简单密码</text>
				<text class="info-text">· 绑定手机号和邮箱，方便找回密码</text>
				<text class="info-text">· 不要在公共场所保存登录状态</text>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { authAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'

const userInfo = ref({})

onMounted(() => {
	try {
		const stored = uni.getStorageSync('userInfo')
		if (stored) userInfo.value = JSON.parse(stored)
	} catch (e) {}
})

const handleChangePwd = () => {
	uni.showModal({
		title: '修改密码',
		content: '请输入当前密码',
		editable: true,
		placeholderText: '当前密码',
		success: (res) => {
			if (!res.confirm) return
			uni.showModal({
				title: '设置新密码',
				content: '请输入新密码（至少6位）',
				editable: true,
				placeholderText: '新密码',
				success: async (res2) => {
					if (!res2.confirm) return
					try {
						const token = uni.getStorageSync('token')
						await authAPI.updatePassword({ oldPassword: res.content, newPassword: res2.content, token: token })
						uni.showToast({ title: '密码修改成功', icon: 'success' })
					} catch (e) {
						uni.showToast({ title: '修改失败，请检查旧密码', icon: 'none' })
					}
				}
			})
		}
	})
}
const handleBindPhone = () => uni.showToast({ title: '功能开发中', icon: 'none' })
const handleBindEmail = () => uni.showToast({ title: '功能开发中', icon: 'none' })
const handleDeleteAccount = () => {
	uni.showModal({
		title: '警告',
		content: '确定要注销账号吗？此操作不可恢复！',
		success: (res) => {
			if (res.confirm) uni.showToast({ title: '请联系管理员注销', icon: 'none' })
		}
	})
}
</script>

<style scoped lang="scss">
.menu-list {
	background: white;
	border-radius: 12px;
	margin: 16px;
	overflow: hidden;
	box-shadow: $uni-shadow-card;
}
.menu-item {
	flex-direction: row;
	align-items: center;
	padding: 16px 16px 16px 60px;
	position: relative;
	min-height: 56px;
}
.menu-item::after {
	content: '';
	position: absolute;
	left: 60px;
	right: 0;
	bottom: 0;
	height: 0.5px;
	background: $uni-border-color-divider;
}
.menu-item:last-child::after { display: none; }
.menu-icon {
	position: absolute;
	left: 12px;
	width: 36px;
	height: 36px;
	border-radius: 8px;
	align-items: center;
	justify-content: center;
}
.menu-icon.blue { background: $uni-color-primary-light; }
.menu-icon.green { background: $uni-color-success-light; }
.menu-icon.orange { background: $uni-color-warning-light; }
.menu-icon.red { background: $uni-color-error-light; }
.menu-text { flex: 1; font-size: 15px; color: $uni-text-color-title; font-weight: 500; }
.menu-value { font-size: 14px; color: $uni-text-color-secondary; margin-right: 8px; }
.menu-item:active { background: $uni-bg-color-page; }
.info-card {
	background: white;
	border-radius: 12px;
	margin: 0 16px 16px;
	padding: 16px;
	box-shadow: $uni-shadow-card;
}
.info-title { font-size: 15px; font-weight: 600; color: $uni-text-color-title; margin-bottom: 12px; display: block; }
.info-text { font-size: 13px; color: $uni-text-color-secondary; line-height: 1.8; display: block; }
</style>
