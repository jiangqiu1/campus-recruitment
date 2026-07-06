<template>
	<view class="tab-bar">
		<view
			v-for="(item, index) in tabList"
			:key="index"
			class="tab-item"
			:class="{ active: current === item.page }"
			@click="handleSwitch(item.page)"
		>
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
import { ref } from 'vue'

const props = defineProps({
	current: {
		type: String,
		default: 'home'
	},
	pathPrefix: {
		type: String,
		default: '/pages/student/'
	},
	tabList: {
		type: Array,
		default: () => [
			{ page: 'home', icon: 'home', activeIcon: 'home-filled', label: '首页' },
			{ page: 'deliveries', icon: 'paperplane', activeIcon: 'paperplane-filled', label: '投递' },
			{ page: 'messages', icon: 'chat', activeIcon: 'chat-filled', label: '消息' },
			{ page: 'profile', icon: 'person', activeIcon: 'person-filled', label: '我的' }
		]
	}
})

const emit = defineEmits(['change'])

const handleSwitch = (page) => {
	if (page === props.current) return
	emit('change', page)
	uni.reLaunch({
		url: props.pathPrefix + page
	})
}
</script>

<style scoped>
.tab-bar {
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
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 3px;
	color: #86909C;
	font-size: 11px;
	font-weight: 500;
	transition: all 0.2s ease;
	cursor: pointer;
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
