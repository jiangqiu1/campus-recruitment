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

			<!-- Banner -->
			<view class="banner">
				<view class="banner-content">
					<text class="banner-title">AI 智能匹配</text>
					<text class="banner-desc">基于你的技能和简历，精准推荐适合岗位</text>
				</view>
				<view class="banner-dots">
					<view class="dot active"></view>
					<view class="dot"></view>
					<view class="dot"></view>
				</view>
			</view>

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
import { jobAPI, deliveryAPI, statisticsAPI, favoriteAPI } from '@/utils/request'
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
	{ id: 1, title: '前端开发实习生', companyName: '广州科技有限公司', location: '广州', experience: '经验不限', education: '大专及以上', salaryText: '4K-6K', colorClass: 'green', matchScore: 92 },
	{ id: 2, title: 'Java开发助理', companyName: '深圳信息技术公司', location: '深圳', experience: '应届', education: '大专及以上', salaryText: '5K-7K', colorClass: 'orange', matchScore: 88 },
	{ id: 3, title: 'UI设计实习生', companyName: '广州创意设计工作室', location: '广州', experience: '经验不限', education: '大专及以上', salaryText: '3K-5K', colorClass: 'ai', matchScore: 85 },
	{ id: 4, title: '测试工程师', companyName: '珠海软件股份', location: '珠海', experience: '1年以下', education: '本科', salaryText: '4K-6K', colorClass: 'red', matchScore: 80 },
	{ id: 5, title: '运维实习生', companyName: '广州网络科技', location: '广州', experience: '经验不限', education: '大专', salaryText: '3K-5K', colorClass: '', matchScore: 78 }
]

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
		recommendJobs.value = jobsRes.data || []
		// 后端返回字段名映射（myDeliveries → deliveries, viewedDeliveries → viewed）
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

/* Banner */
.banner {
	margin: 12px 16px;
	border-radius: 16px;
	overflow: hidden;
	height: 130px;
	background: linear-gradient(135deg, #165DFF, #60A5FA);
	padding: 24px;
	color: white;
	position: relative;
	justify-content: center;
}
.banner-content {
	gap: 8px;
}
.banner-title {
	font-size: 20px;
	font-weight: 700;
}
.banner-desc {
	font-size: 14px;
	opacity: 0.9;
}
.banner-dots {
	flex-direction: row;
	position: absolute;
	bottom: 12px;
	gap: 6px;
}
.dot {
	width: 6px;
	height: 6px;
	border-radius: 50%;
	background: rgba(255,255,255,0.4);
}
.dot.active {
	background: white;
	width: 18px;
	border-radius: 3px;
}

/* Quick Menu */
.quick-menu {
	flex-direction: row;
	justify-content: space-around;
	padding: 20px 16px;
	background: white;
	margin-bottom: 12px;
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
