<template>
	<view v-if="type === 'skeleton'" class="loading-skeleton">
		<view v-for="i in rows" :key="i" class="skeleton-row" :style="{ width: (80 - i * 5) + '%' }">
			<view class="skeleton-block skeleton-line"></view>
		</view>
	</view>
	<view v-else class="loading-spinner">
		<uni-icons type="spinner-cycle" size="32" color="#165DFF" class="spin-icon" />
		<text class="loading-text">{{ text }}</text>
	</view>
</template>

<script setup>
defineProps({
	type: { type: String, default: 'spinner' },
	rows: { type: Number, default: 4 },
	text: { type: String, default: '加载中...' }
})
</script>

<style scoped>
.loading-skeleton {
	padding: 16px;
	gap: 16px;
}
.skeleton-row {
	height: 16px;
}
.skeleton-block {
	width: 100%;
	height: 100%;
	border-radius: 4px;
	background: linear-gradient(90deg, #F2F3F5 25%, #E8EAED 50%, #F2F3F5 75%);
	background-size: 200% 100%;
	animation: shimmer 1.5s infinite;
}
@keyframes shimmer {
	0% { background-position: -200% 0; }
	100% { background-position: 200% 0; }
}
.loading-spinner {
	flex: 1;
	align-items: center;
	justify-content: center;
	padding: 40px;
	gap: 12px;
}
.spin-icon {
	animation: spin 1s linear infinite;
}
@keyframes spin {
	0% { transform: rotate(0deg); }
	100% { transform: rotate(360deg); }
}
.loading-text {
	font-size: 14px;
	color: #949599;
}
</style>
