<template>
	<view class="page-wrapper" style="align-items:center;justify-content:center;">
		<text style="font-size:14px;color:#86909C;">加载中...</text>
	</view>
</template>

<script setup>
// 自动重定向到登录页
const token = uni.getStorageSync('token')
if (token) {
	uni.reLaunch({ url: '/pages/student/home' })
} else {
	uni.reLaunch({ url: '/pages/student/login' })
}
</script>
