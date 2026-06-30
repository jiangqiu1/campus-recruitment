<template>
	<view class="page-wrapper">
		<scroll-view class="content-scrollable" scroll-y>
			<!-- 顶部导航 -->
			<view class="header-simple">
				<view class="header-top-simple">
					<view class="location">
						<text>📍</text>
						<text>广州</text>
						<text>▼</text>
					</view>
					<view class="header-actions">
						<view class="header-action-btn" @click="goToCollect">
							<text>⭐</text>
						</view>
						<view class="header-action-btn" @click="goToMessages">
							<text>🔔</text>
							<text v-if="unreadCount" class="badge">{{ unreadCount }}</text>
						</view>
					</view>
				</view>
				<view class="search-box">
					<text class="search-icon">🔍</text>
					<input v-model="keyword" placeholder="搜索岗位、公司、关键词..." @confirm="handleSearch" />
				</view>
			</view>

			<!-- 快捷入口（原轮播图位，已移除） -->

			<!-- 快捷入口 -->
			<view class="quick-menu">
				<view class="quick-item" @click="goToDeliveries">
					<view class="quick-icon blue"><text>📋</text></view>
					<text>投递记录</text>
				</view>
				<view class="quick-item" @click="goToCollect">
					<view class="quick-icon green"><text>⭐</text></view>
					<text>我的收藏</text>
				</view>
				<view class="quick-item" @click="goToProfile">
					<view class="quick-icon orange"><text>👤</text></view>
					<text>个人中心</text>
				</view>
				<view class="quick-item" @click="goToMessages">
					<view class="quick-icon purple"><text>💬</text></view>
					<text>消息通知</text>
				</view>
			</view>

			<!-- 求职数据 -->
			<view class="data-section">
				<view class="section-header">
					<text class="section-title">求职数据</text>
				</view>
				<view class="stat-row">
					<view class="stat-box">
						<text class="stat-label">📮 投递次数</text>
						<text class="stat-num">{{ stats.deliveries || 0 }}</text>
					</view>
					<view class="stat-box green">
						<text class="stat-label">👀 被查看</text>
						<text class="stat-num">{{ stats.viewed || 0 }}</text>
					</view>
					<view class="stat-box orange">
						<text class="stat-label">📞 面试邀请</text>
						<text class="stat-num">{{ stats.interviews || 0 }}</text>
					</view>
					<view class="stat-box purple">
						<text class="stat-label">✅ 录用通知</text>
						<text class="stat-num">{{ stats.offers || 0 }}</text>
					</view>
				</view>
			</view>

			<!-- AI推荐岗位 -->
			<view class="data-section">
				<view class="section-header">
					<text class="section-title">🤖 AI 推荐岗位</text>
					<text class="section-more" @click="loadMoreJobs">更多 ›</text>
				</view>
			</view>
			<view class="card-list">
				<view v-for="(job, i) in recommendJobs" :key="i" class="card-item" :class="job.colorClass" @click="goToJobDetail(job.id)">
					<view class="card-header-row">
						<view>
							<text class="card-title">{{ job.title }}</text>
							<text class="card-sub">{{ job.companyName }}</text>
						</view>
						<text class="card-salary">{{ job.salaryText }}</text>
					</view>
					<view class="card-info">
						<text>{{ job.location }}</text>
						<text>{{ job.experience }}</text>
						<text>{{ job.education }}</text>
					</view>
					<view class="card-actions">
						<button class="btn-sm" :class="deliverBtnDisabled(job) ? 'btn-disabled' : 'btn-primary'" :disabled="deliverBtnDisabled(job)" @click.stop="handleDeliver(job)">{{ deliverBtnText(job) }}</button>
						<button class="btn-sm btn-outline" @click.stop="goToJobDetail(job.id)">查看详情</button>
					</view>
				</view>
				<view v-if="!recommendJobs.length" class="empty-state">
					<text>暂无推荐岗位</text>
				</view>
			</view>
		</scroll-view>

		<!-- 底部导航 -->
		<TabBar current="home" />
	</view>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { request, jobAPI, deliveryAPI, statisticsAPI, favoriteAPI, mapJobData } from '@/utils/request'
import TabBar from '@/components/TabBar.vue'

const keyword = ref('')
const unreadCount = ref(0)
const recommendJobs = ref([])
const stats = ref({})
const deliveredJobIds = ref(new Set())
const favoritedJobIds = ref(new Set())

const deliverBtnText = (job) => deliveredJobIds.value.has(job.id) ? '已投递' : '立即投递'
const deliverBtnDisabled = (job) => deliveredJobIds.value.has(job.id)

// 模拟数据（API不通时使用）
const mockJobs = [
	{ id: 1, title: '前端开发实习生', salaryRange: '4K-6K', location: '广州', education: '大专及以上', companyId: 1, colorClass: 'green', matchScore: 92 },
	{ id: 2, title: 'Java开发助理', salaryRange: '5K-7K', location: '深圳', education: '大专及以上', companyId: 1, colorClass: 'orange', matchScore: 88 },
	{ id: 3, title: 'UI设计实习生', salaryRange: '3K-5K', location: '广州', education: '大专及以上', companyId: 1, colorClass: 'ai', matchScore: 85 },
	{ id: 4, title: '测试工程师', salaryRange: '4K-6K', location: '珠海', education: '本科', companyId: 1, colorClass: 'red', matchScore: 80 },
	{ id: 5, title: '运维实习生', salaryRange: '3K-5K', location: '广州', education: '大专', companyId: 1, colorClass: '', matchScore: 78 }
].map(mapJobData)

const mockStats = { deliveries: 12, viewed: 8, interviews: 3, offers: 1 }

onMounted(async () => {
	await loadData()
	await loadUserState()
})

const loadData = async () => {
	try {
		const [jobsRes, statsRes] = await Promise.all([
			jobAPI.getRecommendJobs(),
			statisticsAPI.getStudentOverview()
		])
		// 后端岗位数据字段映射 + 补充公司名
		const rawJobs = jobsRes.data || []
		recommendJobs.value = await Promise.all(rawJobs.map(async (j) => {
			const mapped = mapJobData(j)
			// 补充公司名
			if (j.companyId && !mapped.companyName) {
				try {
					const cRes = await request({ url: '/companies/' + j.companyId })
					const c = cRes.data || {}
					mapped.companyName = c.name || c.shortName || ''
				} catch (ce) {}
			}
			return mapped
		}))
		const d = statsRes.data || {}
		stats.value = { deliveries: d.myDeliveries || 0, viewed: d.viewedDeliveries || 0, interviews: d.interviewCount || 0, offers: d.offersCount || 0 }
	} catch (e) {
		console.log('API接口未就绪，使用模拟数据')
		recommendJobs.value = mockJobs
		stats.value = mockStats
	}
}

const handleSearch = () => {
	if (keyword.value.trim()) {
		uni.navigateTo({ url: '/pages/student/job-detail?search=' + encodeURIComponent(keyword.value) })
	}
}

const goToJobDetail = (id) => {
	uni.navigateTo({ url: '/pages/student/job-detail?id=' + id })
}

const getStudentId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		const sid = obj.id || obj.userId
		return sid ? Number(sid) : null
	} catch (e) { return null }
}

const loadUserState = async () => {
	try {
		const sid = getStudentId()
		const [dRes, fRes] = await Promise.all([
			deliveryAPI.getDeliveriesByStudentId({ studentId: sid }),
			favoriteAPI.getFavorites({ studentId: sid })
		])
		deliveredJobIds.value = new Set((dRes.data || []).map(d => d.jobId))
		favoritedJobIds.value = new Set((fRes.data || []).map(d => d.jobId))
	} catch (e) {
		console.log('加载用户状态失败', e)
	}
}

const handleDeliver = async (job) => {
	if (deliveredJobIds.value.has(job.id)) return
	try {
		const sid = getStudentId()
		await deliveryAPI.createDelivery({ jobId: job.id, studentId: sid })
		deliveredJobIds.value.add(job.id)
		uni.showToast({ title: '投递成功', icon: 'success' })
	} catch (e) {
		if (e && e.message && e.message.includes('重复投递')) {
			uni.showToast({ title: '已投递过', icon: 'none' })
			deliveredJobIds.value.add(job.id)
		} else {
			uni.showToast({ title: '投递失败', icon: 'none' })
		}
	}
}

const loadMoreJobs = () => {
	uni.showToast({ title: '加载更多...', icon: 'none' })
}

const goToDeliveries = () => {
	uni.navigateTo({ url: '/pages/student/deliveries' })
}

const goToCollect = () => {
	uni.navigateTo({ url: '/pages/student/collect' })
}

const goToProfile = () => {
	uni.navigateTo({ url: '/pages/student/profile' })
}

const goToMessages = () => {
	uni.navigateTo({ url: '/pages/student/messages' })
}
</script>

<style scoped>
/* Header */
.header-simple {
	background: linear-gradient(135deg, #165DFF 0%, #2563EB 100%);
	color: white;
	padding: 16px;
	position: relative;
	overflow: hidden;
	flex-shrink: 0;
}
.header-simple::before {
	content: '';
	position: absolute;
	top: -50%;
	right: -20%;
	width: 200px;
	height: 200px;
	background: rgba(255,255,255,0.08);
	border-radius: 50%;
}
.header-top-simple {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	position: relative;
	z-index: 1;
}
.location {
	flex-direction: row;
	align-items: center;
	gap: 6px;
	font-size: 14px;
	opacity: 0.9;
}
.header-actions {
	flex-direction: row;
	gap: 12px;
}
.header-action-btn {
	width: 36px;
	height: 36px;
	border-radius: 50%;
	background: rgba(255,255,255,0.15);
	align-items: center;
	justify-content: center;
	font-size: 18px;
	position: relative;
}
.badge {
	position: absolute;
	top: -2px;
	right: -2px;
	width: 18px;
	height: 18px;
	background: #EF4444;
	border-radius: 50%;
	font-size: 10px;
	align-items: center;
	justify-content: center;
	border: 2px solid #165DFF;
	color: white;
	font-weight: 600;
}
.search-box {
	position: relative;
	margin-top: 12px;
	z-index: 1;
	flex-direction: row;
	align-items: center;
}
.search-box input {
	flex: 1;
	height: 44px;
	border-radius: 12px;
	border: none;
	padding: 0 16px 0 42px;
	font-size: 14px;
	background: rgba(255,255,255,0.95);
	color: #1D2129;
	outline: none;
}
.search-icon {
	position: absolute;
	left: 14px;
	z-index: 1;
	font-size: 16px;
}

/* Quick Menu */
.quick-menu {
	flex-direction: row;
	justify-content: space-around;
	padding: 16px 16px;
	background: white;
	margin: 12px 0 0;
}
.quick-item {
	align-items: center;
	gap: 8px;
}
.quick-icon {
	width: 52px;
	height: 52px;
	border-radius: 16px;
	align-items: center;
	justify-content: center;
	font-size: 24px;
	box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.quick-icon.blue { background: linear-gradient(135deg, #165DFF, #60A5FA); }
.quick-icon.green { background: linear-gradient(135deg, #10B981, #34D399); }
.quick-icon.orange { background: linear-gradient(135deg, #F59E0B, #FBBF24); }
.quick-icon.purple { background: linear-gradient(135deg, #8B5CF6, #A78BFA); }
.quick-item text:last-child {
	font-size: 12px;
	color: #4E5969;
	font-weight: 500;
}

/* Data Section */
.data-section {
	padding: 0 16px;
	margin-top: 12px;
	margin-bottom: 12px;
}
.section-header {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 12px;
}
.section-title {
	font-size: 17px;
	font-weight: 700;
	color: #1D2129;
	flex-direction: row;
	align-items: center;
	gap: 8px;
}
.section-more {
	font-size: 13px;
	color: #165DFF;
	font-weight: 500;
}
.stat-row {
	flex-direction: row;
	flex-wrap: wrap;
	gap: 12px;
}
.stat-box {
	flex: 1;
	min-width: calc(50% - 6px);
	background: white;
	border-radius: 16px;
	padding: 20px;
	position: relative;
	overflow: hidden;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.stat-box::before {
	content: '';
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 3px;
	background: linear-gradient(90deg, #165DFF, transparent);
}
.stat-box.green::before { background: linear-gradient(90deg, #10B981, transparent); }
.stat-box.orange::before { background: linear-gradient(90deg, #F59E0B, transparent); }
.stat-box.purple::before { background: linear-gradient(90deg, #8B5CF6, transparent); }
.stat-label {
	font-size: 13px;
	color: #86909C;
	margin-bottom: 8px;
	flex-direction: row;
	align-items: center;
	gap: 6px;
}
.stat-num {
	font-size: 28px;
	font-weight: 800;
	color: #1D2129;
}

/* Card List */
.card-list {
	padding: 0 16px;
	margin-bottom: 12px;
}
.card-item {
	background: white;
	border-radius: 16px;
	padding: 16px;
	margin-bottom: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
	position: relative;
	overflow: hidden;
}
.card-item::before {
	content: '';
	position: absolute;
	top: 0;
	left: 0;
	width: 4px;
	height: 100%;
	background: #165DFF;
}
.card-item.green::before { background: #10B981; }
.card-item.orange::before { background: #F59E0B; }
.card-item.red::before { background: #EF4444; }
.card-item.ai::before { background: #0EA5E9; }
.card-header-row {
	flex-direction: row;
	justify-content: space-between;
	align-items: flex-start;
	margin-bottom: 12px;
}
.card-title {
	font-size: 16px;
	font-weight: 700;
	color: #1D2129;
	display: block;
	margin-bottom: 4px;
}
.card-sub {
	font-size: 13px;
	color: #86909C;
	display: block;
}
.card-salary {
	font-size: 18px;
	font-weight: 800;
	color: #165DFF;
}
.card-info {
	flex-direction: row;
	gap: 12px;
	font-size: 13px;
	color: #86909C;
	margin-bottom: 12px;
}
.card-actions {
	flex-direction: row;
	gap: 8px;
	padding-top: 12px;
	border-top: 1px solid #F2F3F5;
}
.btn-sm {
	flex: 1;
	padding: 10px;
	border-radius: 10px;
	border: none;
	font-size: 14px;
	font-weight: 600;
	text-align: center;
	align-items: center;
	justify-content: center;
}
.btn-primary {
	background: #165DFF;
	color: white;
}
.btn-outline {
	background: white;
	border: 1px solid #E2E8F0;
	color: #4E5969;
}
.btn-disabled {
	background: #E5E6EB;
	color: #A9AEB8;
	border: none;
}
.empty-state {
	padding: 40px;
	align-items: center;
	justify-content: center;
	color: #86909C;
	font-size: 14px;
}
</style>
