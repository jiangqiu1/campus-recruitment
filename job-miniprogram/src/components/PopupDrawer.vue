<template>
	<view class="popup-mask" :class="{ visible: show }" @click="handleMaskClick">
		<view class="popup-drawer" :class="{ visible: show }" @click.stop>
			<view class="popup-handle" />
			<view class="popup-header">
				<text class="popup-title">{{ title }}</text>
				<view class="popup-close" @click="handleClose">
					<text>✕</text>
				</view>
			</view>
			<view class="popup-body">
				<view class="popup-body-inner">
					<slot />
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
const props = defineProps({
	show: { type: Boolean, default: false },
	title: { type: String, default: '' },
	maskClosable: { type: Boolean, default: true }
})

const emit = defineEmits(['update:show'])

const handleClose = () => emit('update:show', false)

const handleMaskClick = () => {
	if (props.maskClosable) {
		setTimeout(() => emit('update:show', false), 200)
	}
}
</script>

<style scoped>
.popup-mask {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(0, 0, 0, 0.6);
	z-index: 1000;
	opacity: 0;
	pointer-events: none;
	transition: opacity 0.25s ease;
}
.popup-mask.visible {
	opacity: 1;
	pointer-events: auto;
}
.popup-drawer {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	background: #FFFFFF;
	border-radius: 16px 16px 0 0;
	z-index: 1001;
	transform: translateY(100%);
	transition: transform 0.3s cubic-bezier(0.32, 0.72, 0, 1);
	max-height: 85vh;
	overflow: hidden;
}
.popup-drawer.visible {
	transform: translateY(0);
}
.popup-handle {
	width: 36px;
	height: 4px;
	background: #E8EAED;
	border-radius: 2px;
	align-self: center;
	margin-top: 8px;
	flex-shrink: 0;
}
.popup-header {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	padding: 16px 20px 12px;
	border-bottom: 1px solid #F2F3F5;
	flex-shrink: 0;
}
.popup-title {
	font-size: 16px;
	font-weight: 600;
	color: #18181A;
}
.popup-close {
	width: 32px;
	height: 32px;
	align-items: center;
	justify-content: center;
	font-size: 18px;
	color: #949599;
}
.popup-body {
	flex: 1;
	overflow-y: auto;
	overflow-x: hidden;
	max-height: calc(85vh - 80px);
}
.popup-body-inner {
	width: 100%;
	padding: 16px 20px;
	padding-bottom: calc(20px + env(safe-area-inset-bottom));
	box-sizing: border-box;
}
</style>
