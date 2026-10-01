<template>
	<view class="tab-bar">
		<view
			v-for="(item, index) in tabList"
			:key="index"
			class="tab-item"
			:class="{ active: current === item.page }"
			@click="handleSwitch(item.page)"
		>
			<view class="tab-icon-wrap">
				<uni-icons
					:type="current === item.page ? item.activeIcon : item.icon"
					:size="24"
					:color="current === item.page ? '#165DFF' : '#86909C'"
				/>
				<view v-if="item.page === 'messages' && unreadCount > 0" class="tab-badge">
					{{ unreadCount > 99 ? '99+' : unreadCount }}
				</view>
			</view>
			<text class="tab-label">{{ item.label }}</text>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { messageAPI } from '@/utils/request'

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

// 学生端：消息未读数角标（未读接口按学生维度统计）
const unreadCount = ref(0)

const getStudentId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.id || obj.userId ? Number(obj.id || obj.userId) : null
	} catch (e) { return null }
}

onMounted(async () => {
	if (props.pathPrefix !== '/pages/student/') return
	const sid = getStudentId()
	if (!sid) return
	try {
		const res = await messageAPI.getUnreadCount(sid)
		unreadCount.value = res.data || 0
	} catch (e) { /* 未读数获取失败不影响导航 */ }
})

const handleSwitch = (page) => {
	if (page === props.current) return
	emit('change', page)
	uni.reLaunch({
		url: props.pathPrefix + page
	})
}
</script>

<style scoped lang="scss">
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
	border-top: 0.5px solid $uni-border-color-divider;
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
	color: $uni-text-color-secondary;
	font-size: 11px;
	font-weight: 500;
	transition: all 0.2s ease;
	cursor: pointer;
}

.tab-item:active {
	transform: scale(0.92);
}

.tab-item.active {
	color: $uni-color-primary;
	font-weight: 600;
	transform: scale(1.05);
}

.tab-icon-wrap {
	position: relative;
}

.tab-badge {
	position: absolute;
	top: -4px;
	right: -10px;
	min-width: 16px;
	height: 16px;
	padding: 0 4px;
	background: $uni-color-error;
	color: $uni-text-color-inverse;
	font-size: 10px;
	font-weight: 600;
	border-radius: 8px;
	text-align: center;
	line-height: 16px;
	box-sizing: border-box;
}

.tab-label {
	font-size: 11px;
	line-height: 1;
}
</style>
