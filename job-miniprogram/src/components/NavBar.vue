<template>
	<view class="nav-bar" :style="{ paddingTop: (statusBarHeight + topExtra) + 'px' }">
		<view class="nav-content" :style="{ paddingRight: (12 + safeAreaRight) + 'px' }">
			<view class="nav-left" @click="handleBack">
				<uni-icons v-if="showBack" type="left" size="22" color="#1D2129" />
			</view>
			<text class="nav-title">{{ title }}</text>
			<view class="nav-right" @click="handleRightClick">
				<uni-icons v-if="rightIcon" :type="rightIcon" size="22" color="#1D2129" />
				<text v-if="rightText" class="nav-right-text">{{ rightText }}</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
const props = defineProps({
	title: { type: String, default: '' },
	showBack: { type: Boolean, default: true },
	rightText: { type: String, default: '' },
	rightIcon: { type: String, default: '' }
})
const emit = defineEmits(['back', 'rightClick'])
const statusBarHeight = ref(20)
const safeAreaRight = ref(0)
const topExtra = ref(4)
const getStatusBar = () => {
	try {
		const info = uni.getWindowInfo ? uni.getWindowInfo() : uni.getSystemInfoSync()
		statusBarHeight.value = info.statusBarHeight || 20
		safeAreaRight.value = info.safeAreaInsets?.right || 0
		// 非刘海屏状态栏高度通常为20，不需要额外下沉；刘海屏/灵动岛需要额外4px避免文字被圆角遮挡
		topExtra.value = (info.statusBarHeight || 20) > 20 ? 4 : 0
	} catch (e) {
		statusBarHeight.value = 20
		safeAreaRight.value = 0
		topExtra.value = 0
	}
}
getStatusBar()
const handleBack = () => {
	if (props.showBack) {
		emit('back')
		uni.navigateBack()
	}
}
const handleRightClick = () => {
	emit('rightClick')
}
</script>

<style scoped>
.nav-bar {
	background: #FFFFFF;
	flex-shrink: 0;
	border-bottom: 0.5px solid #F2F3F5;
	box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
	z-index: 100;
}
.nav-content {
	height: 44px;
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
	padding: 0 12px;
}
.nav-left {
	width: 48px;
	height: 44px;
	justify-content: center;
	align-items: flex-start;
}
.nav-title {
	flex: 1;
	text-align: center;
	font-size: 16px;
	font-weight: 600;
	color: #1D2129;
}
.nav-right {
	flex-direction: row;
	align-items: center;
	gap: 4px;
	min-width: 48px;
	justify-content: flex-end;
	height: 44px;
}
.nav-right-text {
	font-size: 14px;
	color: #1D2129;
}
.nav-left:active, .nav-right:active {
	opacity: 0.7;
}
</style>
