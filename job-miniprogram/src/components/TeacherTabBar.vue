<template>
	<view class="bottom-nav">
		<view
			v-for="(item, i) in tabs"
			:key="i"
			class="nav-item"
			:class="{ active: current === item.page }"
			@click="switchTab(item.page)"
		>
			<text class="nav-icon">{{ item.icon }}</text>
			<text class="nav-label">{{ item.label }}</text>
		</view>
	</view>
</template>

<script setup>
	const tabs = [
		{ page: 'home', icon: '🏫', label: '首页' },
		{ page: 'classes', icon: '👥', label: '班级' },
		{ page: 'jobs', icon: '📋', label: '岗位' },
		{ page: 'profile', icon: '👤', label: '我的' }
	]

	const props = defineProps({
		current: { type: String, default: 'home' }
	})

	const switchTab = (page) => {
		uni.reLaunch({
			url: '/pages/teacher/' + page
		})
	}
</script>

<style scoped>
.bottom-nav {
	height: 64px;
	background: rgba(255,255,255,0.98);
	display: flex;
	flex-direction: row;
	justify-content: space-around;
	align-items: center;
	box-shadow: 0 -2px 20px rgba(0,0,0,0.06);
	border-top: 1px solid rgba(0,0,0,0.04);
	flex-shrink: 0;
	padding-bottom: env(safe-area-inset-bottom);
}
.nav-item {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 2px;
	color: #86909C;
	font-size: 11px;
	font-weight: 500;
	padding: 8px 16px;
	transition: all 0.2s;
}
.nav-item.active {
	color: #10B981;
}
.nav-icon {
	font-size: 22px;
}
.nav-label {
	font-size: 11px;
}
</style>
