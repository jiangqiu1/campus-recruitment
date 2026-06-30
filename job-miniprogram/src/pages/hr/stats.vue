<template>
	<view class="page-wrapper">
		<view class="header-simple" style="padding:12px 16px;flex-direction:row;align-items:center;gap:12px;">
			<text style="font-size:20px;" @click="goBack">‹</text>
			<text style="font-size:18px;font-weight:700;color:white;">数据统计</text>
		</view>

		<scroll-view class="content-scrollable" scroll-y>
			<!-- 投递趋势（7天柱状图，纯CSS） -->
			<view class="section-card">
				<view class="section-title-row">
					<text class="section-title">📈 投递趋势（近7天）</text>
				</view>
				<view class="chart-container">
					<view class="chart-y-axis">
						<text v-for="(label, i) in yLabels" :key="i" class="y-label">{{ label }}</text>
					</view>
					<view class="chart-bars">
						<view v-for="(day, i) in trendData" :key="i" class="chart-bar-col">
							<view class="chart-bar-wrapper">
								<view class="chart-bar" :style="{ height: calcBarHeight(day.count) + 'px' }">
									<text class="bar-value">{{ day.count }}</text>
								</view>
							</view>
							<text class="bar-label">{{ day.label }}</text>
						</view>
					</view>
				</view>
			</view>

			<!-- 岗位分类统计 -->
			<view class="section-card">
				<view class="section-title-row">
					<text class="section-title">📊 岗位分类统计</text>
				</view>
				<view class="job-category-list">
					<view v-for="(cat, i) in jobCategories" :key="i" class="category-item">
						<view class="category-header">
							<text class="category-name">{{ cat.name }}</text>
							<text class="category-count">{{ cat.count }}个</text>
						</view>
						<view class="progress-track">
							<view class="progress-fill" :style="{ width: cat.percent + '%', background: cat.color }"></view>
						</view>
					</view>
				</view>
			</view>

			<!-- 简历评分分布 -->
			<view class="section-card">
				<view class="section-title-row">
					<text class="section-title">⭐ 简历评分分布</text>
				</view>
				<view class="score-distribution">
					<view v-for="(score, i) in scoreDistribution" :key="i" class="score-item">
						<text class="score-range">{{ score.range }}</text>
						<view class="score-track">
							<view class="score-fill" :style="{ width: score.percent + '%', background: score.color }"></view>
						</view>
						<text class="score-count">{{ score.count }}人</text>
					</view>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { hrAPI, statisticsAPI } from '@/utils/request'

const trendData = ref([])
const jobCategories = ref([])
const scoreDistribution = ref([])

const yLabels = ['20', '15', '10', '5', '0']
const maxCount = 20



const calcBarHeight = (count) => {
	return Math.max(4, (count / maxCount) * 120)
}

onMounted(async () => {
	try {
		const userId = getUserId()
		const res = await hrAPI.getDeliveryTrend(userId)
		trendData.value = res.data || []
	} catch (e) {
		console.error('加载投递趋势失败', e)
	}

	try {
		const cId = getCompanyId()
		const res = await hrAPI.getDashboard(cId)
		const data = res.data || {}
		// 岗位分类 — 从 jobs 数据派生
		loadJobCategories(cId)
	} catch (e) {
		console.error('加载dashboard失败', e)
	}

	loadScoreDistribution()
})

const loadJobCategories = async (cId) => {
	try {
		const res = await hrAPI.getHrJobs(cId)
		const jobs = res.data || []
		// 按类型分（简单分组：用 title 关键词判断）
		const groups = { '技术研发': 0, '设计创意': 0, '运营市场': 0, '其他': 0 }
		jobs.forEach(j => {
			const t = (j.title || '').toLowerCase()
			if (t.match(/前端|后端|java|测试|运维|算法|开发/)) groups['技术研发']++
			else if (t.match(/设计|ui|创意/)) groups['设计创意']++
			else if (t.match(/运营|市场|销售/)) groups['运营市场']++
			else groups['其他']++
		})
		const total = jobs.length || 1
		const colors = ['#0EA5E9', '#8B5CF6', '#10B981', '#F59E0B']
		jobCategories.value = Object.entries(groups).filter(([,c]) => c > 0).map(([name, count], i) => ({
			name, count, percent: (count / total) * 100, color: colors[i] || '#C9CDD4'
		}))
	} catch (e) {
		console.error('加载岗位分类失败', e)
	}
}

const loadScoreDistribution = async () => {
	// 评分分布 — statisticsAPI 拉取
	scoreDistribution.value = []
}

const getUserId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.id || obj.userId || null
	} catch (e) { return null }
}

const getCompanyId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.companyId || obj.id || null
	} catch (e) { return null }
}

const goBack = () => {
	uni.navigateBack()
}
</script>

<style scoped>
.section-card {
	background: white;
	border-radius: 16px;
	margin: 12px 16px;
	padding: 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.section-title-row {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 16px;
}
.section-title {
	font-size: 16px;
	font-weight: 700;
	color: #1D2129;
}

/* 柱状图 */
.chart-container {
	flex-direction: row;
	gap: 8px;
}
.chart-y-axis {
	width: 28px;
	justify-content: space-between;
	padding-bottom: 24px;
}
.y-label {
	font-size: 10px;
	color: #86909C;
	text-align: right;
}
.chart-bars {
	flex: 1;
	flex-direction: row;
	align-items: flex-end;
	justify-content: space-around;
	padding-bottom: 0;
	height: 160px;
}
.chart-bar-col {
	flex: 1;
	align-items: center;
	height: 100%;
	justify-content: flex-end;
}
.chart-bar-wrapper {
	width: 100%;
	align-items: center;
	justify-content: flex-end;
	height: 130px;
}
.chart-bar {
	width: 60%;
	max-width: 32px;
	min-height: 4px;
	background: linear-gradient(180deg, #0EA5E9, #38BDF8);
	border-radius: 6px 6px 2px 2px;
	align-items: center;
	justify-content: flex-start;
	padding-top: 4px;
	position: relative;
}
.bar-value {
	font-size: 10px;
	color: white;
	font-weight: 700;
	margin-top: -14px;
	position: absolute;
	top: 0;
}
.bar-label {
	font-size: 10px;
	color: #86909C;
	margin-top: 6px;
}

/* 岗位分类 */
.job-category-list {
	gap: 14px;
}
.category-item {
	gap: 6px;
}
.category-header {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
}
.category-name {
	font-size: 14px;
	color: #1D2129;
	font-weight: 500;
}
.category-count {
	font-size: 13px;
	color: #86909C;
}
.progress-track {
	height: 8px;
	background: #F2F3F5;
	border-radius: 4px;
	overflow: hidden;
}
.progress-fill {
	height: 100%;
	border-radius: 4px;
}

/* 评分分布 */
.score-distribution {
	gap: 12px;
}
.score-item {
	flex-direction: row;
	align-items: center;
	gap: 8px;
}
.score-range {
	width: 60px;
	font-size: 13px;
	color: #4E5969;
	flex-shrink: 0;
}
.score-track {
	flex: 1;
	height: 10px;
	background: #F2F3F5;
	border-radius: 5px;
	overflow: hidden;
}
.score-fill {
	height: 100%;
	border-radius: 5px;
}
.score-count {
	width: 36px;
	font-size: 12px;
	color: #86909C;
	text-align: right;
	flex-shrink: 0;
}
</style>
