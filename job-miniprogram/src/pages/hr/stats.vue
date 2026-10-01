<template>
	<view class="page-wrapper">
		<NavBar title="数据统计" show-back />

		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<LoadingState v-if="loading" />

			<template v-if="!loading">
				<!-- 核心指标 -->
				<view class="stats-grid">
					<view class="stat-card">
						<text class="stat-num">{{ stats.activeJobCount || 0 }}</text>
						<text class="stat-label">在招岗位</text>
					</view>
					<view class="stat-card">
						<text class="stat-num">{{ stats.resumeCount || 0 }}</text>
						<text class="stat-label">总投递数</text>
					</view>
					<view class="stat-card">
						<text class="stat-num">{{ stats.pendingResumeCount || 0 }}</text>
						<text class="stat-label">待处理</text>
					</view>
					<view class="stat-card">
						<text class="stat-num">{{ stats.hiredCount || 0 }}</text>
						<text class="stat-label">已录用</text>
					</view>
				</view>

				<!-- 今日数据 -->
				<view class="section-card">
					<text class="section-title">今日数据</text>
					<view class="today-row">
						<view class="today-item">
							<text class="today-num">{{ stats.todayNewCount || 0 }}</text>
							<text class="today-label">今日新增投递</text>
						</view>
						<view class="today-divider" />
						<view class="today-item">
							<text class="today-num">{{ stats.todayInterviewCount || 0 }}</text>
							<text class="today-label">今日面试</text>
						</view>
					</view>
				</view>

				<!-- 转化漏斗 -->
				<view class="section-card">
					<text class="section-title">转化漏斗</text>
					<view class="funnel-list">
						<view class="funnel-row">
							<text class="funnel-label">投递简历</text>
							<view class="funnel-track">
								<view class="funnel-fill" :style="{ width: '100%' }" />
							</view>
							<text class="funnel-val">{{ stats.resumeCount || 0 }}</text>
						</view>
						<view class="funnel-row">
							<text class="funnel-label">进入面试</text>
							<view class="funnel-track">
								<view class="funnel-fill funnel-fill--blue" :style="{ width: interviewRate + '%' }" />
							</view>
							<text class="funnel-val">{{ stats.interviewCount || 0 }}</text>
						</view>
						<view class="funnel-row">
							<text class="funnel-label">成功录用</text>
							<view class="funnel-track">
								<view class="funnel-fill funnel-fill--green" :style="{ width: hireRate + '%' }" />
							</view>
							<text class="funnel-val">{{ stats.hiredCount || 0 }}</text>
						</view>
					</view>
					<view class="rate-row">
						<text class="rate-text">面试转化率: {{ interviewRate }}%</text>
						<text class="rate-text">录用率: {{ hireRate }}%</text>
					</view>
				</view>
			</template>

			<view style="height: 40px" />
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { hrAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import LoadingState from '@/components/LoadingState.vue'

const loading = ref(true)
const stats = ref({})

const interviewRate = computed(() => {
	const total = stats.value.resumeCount || 0
	if (total === 0) return 0
	return Math.round((stats.value.interviewCount || 0) / total * 100)
})

const hireRate = computed(() => {
	const total = stats.value.resumeCount || 0
	if (total === 0) return 0
	return Math.round((stats.value.hiredCount || 0) / total * 100)
})

onMounted(() => { loadData() })

const refreshing = ref(false)
const onRefresh = async () => {
	refreshing.value = true
	await loadData()
	refreshing.value = false
}

const loadData = async () => {
	loading.value = true
	try {
		const cId = getCompanyId()
		if (!cId) return
		const res = await hrAPI.getDashboard(cId)
		stats.value = res.data || {}
	} catch (e) {
		console.error('加载统计数据失败', e)
	} finally {
		loading.value = false
	}
}

const getCompanyId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.companyId || obj.id || null
	} catch (e) {
		console.error('获取公司ID失败', e)
		return null
	}
}
</script>

<style scoped lang="scss">
.stats-grid {
	flex-direction: row;
	flex-wrap: wrap;
	padding: 16px;
	gap: 12px;
}
.stat-card {
	width: calc(50% - 6px);
	background: $uni-bg-color;
	border-radius: 12px;
	padding: 20px 16px;
	box-shadow: $uni-shadow-card;
	align-items: center;
}
.stat-num {
	font-size: 28px;
	font-weight: 800;
	color: $uni-text-color-title;
}
.stat-label {
	font-size: 13px;
	color: $uni-text-color-secondary;
	margin-top: 4px;
}
.section-card {
	background: $uni-bg-color;
	border-radius: 12px;
	margin: 0 16px 12px;
	padding: 16px;
	box-shadow: $uni-shadow-card;
}
.section-title {
	font-size: 15px;
	font-weight: 700;
	color: $uni-text-color-title;
	margin-bottom: 12px;
	display: block;
}

/* 今日数据 */
.today-row {
	flex-direction: row;
	align-items: center;
}
.today-item {
	flex: 1;
	align-items: center;
	padding: 8px 0;
}
.today-num {
	font-size: 24px;
	font-weight: 800;
	color: $uni-color-primary;
}
.today-label {
	font-size: 12px;
	color: $uni-text-color-secondary;
	margin-top: 4px;
}
.today-divider {
	width: 1px;
	height: 40px;
	background: $uni-border-color-divider;
}

/* 漏斗图 */
.funnel-list { gap: 8px; }
.funnel-row {
	flex-direction: row;
	align-items: center;
	gap: 8px;
}
.funnel-label {
	width: 60px;
	font-size: 13px;
	color: $uni-text-color;
	flex-shrink: 0;
}
.funnel-track {
	flex: 1;
	height: 8px;
	background: $uni-border-color-divider;
	border-radius: 4px;
	overflow: hidden;
}
.funnel-fill {
	height: 100%;
	border-radius: 4px;
	background: $uni-color-primary;
}
.funnel-fill--blue { background: $uni-color-primary-hover; }
.funnel-fill--green { background: $uni-color-success; }
.funnel-val {
	width: 32px;
	font-size: 13px;
	font-weight: 600;
	color: $uni-text-color-title;
	text-align: right;
}
.rate-row {
	flex-direction: row;
	justify-content: space-around;
	margin-top: 12px;
	padding-top: 12px;
	border-top: 0.5px solid $uni-border-color-divider;
}
.rate-text {
	font-size: 13px;
	color: $uni-text-color-secondary;
}
</style>
