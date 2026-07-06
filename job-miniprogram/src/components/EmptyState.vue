<template>
	<view class="empty-state">
		<uni-icons :type="icon" :size="64" color="#C9CDD4" />
		<text class="empty-title">{{ title }}</text>
		<text v-if="desc" class="empty-desc">{{ desc }}</text>
		<button v-if="btnText" class="empty-btn" @click="handleAction">{{ btnText }}</button>
	</view>
</template>

<script setup>
const props = defineProps({
	icon: { type: String, default: 'inbox' },
	title: { type: String, default: '暂无数据' },
	desc: { type: String, default: '' },
	btnText: { type: String, default: '' },
	btnAction: { type: String, default: '' }
})
const emit = defineEmits(['action'])
const handleAction = () => {
	emit('action', props.btnAction)
}
</script>

<style scoped>
.empty-state {
	flex: 1;
	align-items: center;
	justify-content: center;
	padding: 60px 32px;
	gap: 12px;
}
.empty-title {
	font-size: 15px;
	color: #4E5969;
	font-weight: 500;
	margin-top: 8px;
}
.empty-desc {
	font-size: 13px;
	color: #86909C;
	text-align: center;
	line-height: 20px;
}
.empty-btn {
	margin-top: 12px;
	padding: 10px 28px;
	background: #165DFF;
	color: #FFFFFF;
	border: none;
	border-radius: 8px;
	font-size: 14px;
	font-weight: 500;
	align-items: center;
	justify-content: center;
}
.empty-btn:active {
	opacity: 0.85;
}
</style>
