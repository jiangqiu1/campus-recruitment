<template>
	<view class="page-wrapper">
		<view class="brand-badge">
			<text class="badge-letter">聘</text>
		</view>
		<text class="brand-name">校企招聘平台</text>
		<text class="brand-sub">校企协同 × AI 精准匹配</text>
		<text class="loading-text">正在进入工作台…</text>
	</view>
</template>

<script setup>
import { onMounted } from 'vue'

onMounted(() => {
	// 延迟跳转，避免页面初始化阶段 reLaunch 导致的竞态问题
	setTimeout(() => {
		const token = uni.getStorageSync('token')
		if (token) {
			try {
				const raw = uni.getStorageSync('userInfo')
				if (raw) {
					const user = JSON.parse(raw)
					if (Number(user.role) === 1) {
						uni.reLaunch({ url: '/pages/teacher/home' })
					} else if (Number(user.role) === 2) {
						uni.reLaunch({ url: '/pages/hr/home' })
					} else {
						uni.reLaunch({ url: '/pages/student/home' })
					}
				} else {
					uni.reLaunch({ url: '/pages/student/login' })
				}
			} catch (e) {
				uni.reLaunch({ url: '/pages/student/login' })
			}
		} else {
			uni.reLaunch({ url: '/pages/student/login' })
		}
	}, 100)
})
</script>

<style scoped lang="scss">
.page-wrapper {
	min-height: 100vh;
	background: $uni-gradient-hero;
	align-items: center;
	justify-content: center;
}
.brand-badge {
	width: 64px;
	height: 64px;
	border-radius: 18px;
	background: rgba(255,255,255,0.9);
	align-items: center;
	justify-content: center;
	margin-bottom: 16px;
	box-shadow: 0 4px 16px rgba(11, 62, 194, 0.3);
	animation: badgeUp 0.6s ease both;
}
.badge-letter {
	font-size: 32px;
	font-weight: 700;
	color: $uni-color-primary;
	line-height: 1;
}
.brand-name {
	font-size: 20px;
	font-weight: 700;
	color: $uni-text-color-inverse;
	letter-spacing: 0.04em;
}
.brand-sub {
	font-size: 12px;
	color: rgba(255,255,255,0.75);
	margin-top: 6px;
	letter-spacing: 0.06em;
}
.loading-text {
	font-size: 12px;
	color: rgba(255,255,255,0.55);
	margin-top: 48px;
}
@keyframes badgeUp {
	from { opacity: 0; transform: translateY(14px); }
	to { opacity: 1; transform: translateY(0); }
}
</style>
