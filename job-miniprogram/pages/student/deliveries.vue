<template>
	<view class="page-wrapper">
		<view class="header-simple" style="padding:12px 16px;">
			<text style="font-size:18px;font-weight:700;color:white;">投递记录</text>
		</view>

		<!-- 状态筛选 -->
		<view class="filter-tabs">
			<text v-for="(tab, i) in tabs" :key="i" class="filter-tab" :class="{ active: currentTab === tab.value }" @click="currentTab = tab.value">{{ tab.label }}</text>
		</view>

		<scroll-view class="content-scrollable" scroll-y>
			<view class="timeline">
				<view v-for="(item, i) in filteredList" :key="i" class="timeline-item">
					<view class="timeline-dot" :class="item.dotClass">
						<text>{{ item.dotIcon }}</text>
					</view>
					<view class="timeline-content">
						<text class="timeline-title">{{ item.jobTitle }}</text>
						<text class="timeline-desc">{{ item.companyName }}</text>
						<view class="timeline-meta">
							<text class="tag-blue card-tag">{{ item.statusText }}</text>
							<text class="timeline-time">{{ item.createTime }}</text>
						</view>
						<view v-if="item.status === 'pending'" class="timeline-actions">
							<button class="btn-sm btn-outline" @click="cancelDelivery(item.id)">取消投递</button>
						</view>
					</view>
				</view>
				<view v-if="!filteredList.length" class="empty-state">
					<text style="font-size:48px;margin-bottom:12px;">📭</text>
					<text class="empty-text">暂无投递记录</text>
				</view>
			</view>
		</scroll-view>

		<TabBar current="deliveries" />
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { deliveryAPI } from '@/utils/request'
import TabBar from '@/components/TabBar.vue'

const tabs = [
	{ label: '全部', value: 'all' },
	{ label: '待查看', value: 'pending' },
	{ label: '已查看', value: 'viewed' },
	{ label: '面试', value: 'interview' },
	{ label: '已通过', value: 'accepted' },
	{ label: '未通过', value: 'rejected' }
]
const currentTab = ref('all')
const deliveries = ref([])

const mockDeliveries = [
	{ id: 1, jobTitle: '前端开发实习生', companyName: '广州科技有限公司', status: 'pending', statusText: '待查看', createTime: '2026-06-15', dotClass: 'orange', dotIcon: '📬' },
	{ id: 2, jobTitle: 'Java开发助理', companyName: '深圳信息技术公司', status: 'viewed', statusText: '已查看', createTime: '2026-06-14', dotClass: '', dotIcon: '👀' },
	{ id: 3, jobTitle: 'UI设计实习生', companyName: '广州创意设计工作室', status: 'interview', statusText: '面试', createTime: '2026-06-13', dotClass: 'green', dotIcon: '📞' },
	{ id: 4, jobTitle: '运维实习生', companyName: '广州网络科技', status: 'rejected', statusText: '未通过', createTime: '2026-06-12', dotClass: 'gray', dotIcon: '❌' },
	{ id: 5, jobTitle: '测试工程师', companyName: '珠海软件股份', status: 'accepted', statusText: '已通过', createTime: '2026-06-11', dotClass: 'green', dotIcon: '✅' }
]

const filteredList = computed(() => {
	if (currentTab.value === 'all') return deliveries.value
	return deliveries.value.filter(d => d.status === currentTab.value)
})

const getStudentId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		const sid = obj.id || obj.userId
		return sid ? Number(sid) : null
	} catch (e) { return null }
}

onMounted(async () => {
	try {
		const sid = getStudentId()
		const res = await deliveryAPI.getDeliveriesByStudentId({ studentId: sid })
		deliveries.value = res.data || []
	} catch (e) {
		deliveries.value = mockDeliveries
	}
})

const cancelDelivery = async (id) => {
	try {
		await deliveryAPI.cancelDelivery(id)
		uni.showToast({ title: '已取消投递', icon: 'success' })
		deliveries.value = deliveries.value.filter(d => d.id !== id)
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'none' })
	}
}
</script>

<style scoped>
.filter-tabs {
	flex-direction: row;
	gap: 8px;
	padding: 12px 16px;
	background: white;
	overflow-x: auto;
	flex-shrink: 0;
}
.filter-tab {
	padding: 8px 16px;
	border-radius: 20px;
	font-size: 13px;
	font-weight: 500;
	color: #4E5969;
	background: #F2F3F5;
	white-space: nowrap;
}
.filter-tab.active {
	background: #165DFF;
	color: white;
}
.timeline {
	padding: 16px;
}
.timeline-item {
	flex-direction: row;
	gap: 12px;
	padding-bottom: 16px;
	position: relative;
}
.timeline-item::before {
	content: '';
	position: absolute;
	left: 15px;
	top: 32px;
	bottom: 0;
	width: 2px;
	background: #E2E8F0;
}
.timeline-item:last-child::before { display: none; }
.timeline-dot {
	width: 32px;
	height: 32px;
	border-radius: 50%;
	background: #165DFF;
	align-items: center;
	justify-content: center;
	color: white;
	font-size: 14px;
	flex-shrink: 0;
	z-index: 1;
}
.timeline-dot.green { background: #10B981; }
.timeline-dot.orange { background: #F59E0B; }
.timeline-dot.gray { background: #C9CDD4; }
.timeline-content {
	flex: 1;
	background: white;
	border-radius: 12px;
	padding: 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.timeline-title {
	font-size: 15px;
	font-weight: 600;
	color: #1D2129;
	display: block;
	margin-bottom: 4px;
}
.timeline-desc {
	font-size: 13px;
	color: #86909C;
	display: block;
	margin-bottom: 8px;
}
.timeline-meta {
	flex-direction: row;
	align-items: center;
	gap: 8px;
}
.card-tag {
	padding: 4px 10px;
	border-radius: 6px;
	font-size: 12px;
	font-weight: 600;
}
.tag-blue { background: rgba(22,93,255,0.1); color: #165DFF; }
.timeline-time {
	font-size: 12px;
	color: #C9CDD4;
}
.timeline-actions {
	margin-top: 8px;
}
.btn-sm {
	padding: 8px 16px;
	border-radius: 8px;
	font-size: 13px;
	font-weight: 600;
	align-items: center;
	justify-content: center;
}
.btn-outline {
	background: white;
	border: 1px solid #E2E8F0;
	color: #86909C;
}
.empty-state {
	padding: 60px 20px;
	align-items: center;
	justify-content: center;
}
.empty-text {
	font-size: 14px;
	color: #86909C;
}
</style>
