<template>
	<view class="page-wrapper">
		<NavBar title="关于平台" />
		<scroll-view class="content-scrollable" scroll-y>
			<view class="about-header">
				<view class="logo-placeholder">
					<uni-icons type="flag" size="40" color="#165DFF" />
				</view>
				<text class="app-name">校企招聘平台</text>
				<text class="app-version">v1.0.0</text>
			</view>

			<view class="about-card">
				<text class="about-title">平台简介</text>
				<text class="about-desc">
					校企招聘平台是一个连接学校、学生与企业的智能化招聘管理系统。平台基于 AI 技术，为学生提供智能岗位匹配、简历诊断等服务，帮助教师更高效地管理学生就业，助力企业精准招揽人才。
				</text>
			</view>

			<view class="about-card">
				<text class="about-title">联系我们</text>
				<view class="contact-row">
					<text class="contact-label">客服邮箱</text>
					<text class="contact-value">support@xiaozhao.com</text>
				</view>
				<view class="contact-row" style="border:none;">
					<text class="contact-label">服务时间</text>
					<text class="contact-value">工作日 9:00 - 18:00</text>
				</view>
			</view>

			<view class="footer-text">
				<text>Copyright 2026 校企招聘平台</text>
				<text>All Rights Reserved</text>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import NavBar from '@/components/NavBar.vue'
</script>

<style scoped>
.about-header {
	align-items: center;
	padding: 40px 0 24px;
}
.logo-placeholder {
	width: 72px;
	height: 72px;
	border-radius: 20px;
	background: rgba(22,93,255,0.08);
	align-items: center;
	justify-content: center;
	margin-bottom: 12px;
}
.app-name {
	font-size: 20px;
	font-weight: 700;
	color: #1D2129;
}
.app-version {
	font-size: 13px;
	color: #86909C;
	margin-top: 4px;
}
.about-card {
	background: #FFFFFF;
	border-radius: 12px;
	margin: 0 16px 12px;
	padding: 20px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.about-title {
	font-size: 15px;
	font-weight: 600;
	color: #1D2129;
	margin-bottom: 10px;
	display: block;
}
.about-desc {
	font-size: 14px;
	color: #4E5969;
	line-height: 1.7;
}
.contact-row {
	flex-direction: row;
	align-items: center;
	padding: 12px 0;
	border-bottom: 0.5px solid #F2F3F5;
}
.contact-label {
	font-size: 14px;
	color: #86909C;
	width: 80px;
}
.contact-value {
	font-size: 14px;
	color: #1D2129;
	flex: 1;
}
.footer-text {
	align-items: center;
	padding: 24px 0 40px;
}
.footer-text text {
	font-size: 12px;
	color: #C9CDD4;
	display: block;
}
</style>
