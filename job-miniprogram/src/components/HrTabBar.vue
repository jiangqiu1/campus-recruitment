<template>
	<view class="page-wrapper">
		<view v-for="(item, i) in tabs" :key="i" class="tab-item" :class="{ active: current === item.page }" @click="switchTab(item.page)">
			<uni-icons
				:type="current === item.page ? item.activeIcon : item.icon"
				:size="24"
				:color="current === item.page ? '#165DFF' : '#86909C'"
			/>
			<text class="tab-label">{{ item.label }}</text>
		</view>
	</view>
</template>

<script setup>
import { defineProps } from 'vue'

const tabs = [
	{ page: 'home', icon: 'home', activeIcon: 'home-filled', label: '首页' },
	{ page: 'deliveries', icon: 'paperplane', activeIcon: 'paperplane-filled', label: '候选人' },
	{ page: 'interviews', icon: 'calendar', activeIcon: 'calendar-filled', label: '面试日程' },
	{ page: 'profile', icon: 'person', activeIcon: 'person-filled', label: '企业中心' }
]

const props = defineProps({
	current: { type: String, default: 'home' }
})

const switchTab = (page) => {
	uni.reLaunch({ url: '/pages/hr/' + page })
}
</script>

<style scoped>
.page-wrapper {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	height: 50px;
	flex-direction: row;
	background: rgba(255, 255, 255, 0.92);
	backdrop-filter: blur(20px);
	-webkit-backdrop-filter: blur(20px);
	align-items: center;
	justify-content: space-around;
	box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.04);
	border-top: 0.5px solid #F2F3F5;
	z-index: 999;
	padding-bottom: constant(safe-area-inset-bottom);
	padding-bottom: env(safe-area-inset-bottom);
	box-sizing: content-box;
}

.tab-item {
	flex: 1;
	height: 100%;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 3px;
	color: #86909C;
	font-size: 11px;
	font-weight: 500;
	transition: all 0.2s ease;
}

.tab-item:active {
	transform: scale(0.92);
}

.tab-item.active {
	color: #165DFF;
	font-weight: 600;
	transform: scale(1.05);
}

.tab-label {
	font-size: 11px;
	line-height: 1;
}
</style>
