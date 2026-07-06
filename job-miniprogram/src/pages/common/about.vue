<template>
	<view class="page-wrapper">
		<NavBar title="关于我们" show-back />

		<view class="about-content">
			<view class="app-logo">
				<uni-icons type="star" size="64" color="#165DFF" />
			</view>
			<text class="app-name">校企招聘平台</text>
			<text class="app-version">v1.0.0</text>

			<view class="app-desc">
				<text class="desc-text">
					校企招聘平台致力于为学校和企业搭建高效的人才对接桥梁，为毕业生提供精准的就业机会，助力校企合作共赢。
				</text>
			</view>

			<view class="link-card">
				<view class="link-item" @click="showToast('隐私政策')">
					<text class="link-label">隐私政策</text>
					<uni-icons type="arrowright" size="16" color="#C9CDD4" />
				</view>
				<view class="link-divider"></view>
				<view class="link-item" @click="showToast('用户协议')">
					<text class="link-label">用户协议</text>
					<uni-icons type="arrowright" size="16" color="#C9CDD4" />
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import NavBar from '@/components/NavBar.vue'

const showToast = (title) => {
	uni.showToast({ title: '功能开发中', icon: 'none' })
}
</script>

<style scoped>
.about-content {
	padding: 40px 32px;
	align-items: center;
}
.app-logo {
	width: 96px;
	height: 96px;
	border-radius: 24px;
	background: rgba(22,93,255,0.08);
	align-items: center;
	justify-content: center;
	margin-bottom: 16px;
}
.app-name {
	font-size: 22px;
	font-weight: 700;
	color: #1D2129;
	margin-bottom: 6px;
}
.app-version {
	font-size: 13px;
	color: #86909C;
	margin-bottom: 24px;
}
.app-desc {
	padding: 0 8px;
	margin-bottom: 32px;
}
.desc-text {
	font-size: 14px;
	color: #4E5969;
	line-height: 1.6;
	text-align: center;
}
.link-card {
	background: #FFFFFF;
	border-radius: 12px;
	width: 100%;
	padding: 4px 0;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.link-item {
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
	padding: 16px;
}
.link-label {
	font-size: 15px;
	color: #1D2129;
	font-weight: 500;
}
.link-divider {
	height: 0.5px;
	background: #F2F3F5;
	margin-left: 16px;
}
.link-item:active { background: #F7F8FA; }
</style>
