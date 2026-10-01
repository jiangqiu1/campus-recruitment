<template>
	<view class="error-state">
		<uni-icons type="error" size="64" color="#F53F3F" />
		<text class="error-msg">{{ errorMsg }}</text>
		<button class="error-retry" @click="$emit('retry')">重新加载</button>
	</view>
</template>

<script setup>
defineProps({
	errorMsg: { type: String, default: '加载失败，请稍后重试' }
})
defineEmits(['retry'])
</script>

<style scoped lang="scss">
.error-state {
	flex: 1;
	align-items: center;
	justify-content: center;
	padding: 60px 32px;
	gap: 12px;
}
.error-msg {
	font-size: 15px;
	color: $uni-text-color-secondary;
	text-align: center;
}
.error-retry {
	margin-top: 8px;
	padding: 10px 24px;
	background: $uni-color-error;
	color: $uni-text-color-inverse;
	border: none;
	border-radius: 8px;
	font-size: 14px;
	font-weight: 500;
	align-items: center;
	justify-content: center;
}
</style>
