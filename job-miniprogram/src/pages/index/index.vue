<template>
	<view class="page-wrapper" style="align-items:center;justify-content:center;">
		<text style="font-size:14px;color:#86909C;">加载中...</text>
	</view>
</template>

<script setup>
// 自动重定向到对应角色的页面
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
</script>
