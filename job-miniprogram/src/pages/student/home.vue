<template>
	<view class="page-wrapper">
		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<!-- 顶部导航 -->
			<view class="header-section" :style="{ paddingTop: (statusBarHeight + 16) + 'px' }">
				<view class="header-top">
					<text class="greeting-text">您好，{{ userName }}</text>
				</view>
				<view class="search-box">
					<uni-icons type="search" size="16" color="#86909C" />
					<input v-model="keyword" placeholder="搜索岗位、公司、关键词..." @confirm="handleSearch" />
				</view>
			</view>

			<!-- 快捷入口（4个，投递记录已移至TabBar） -->
			<view class="quick-menu">
				<view class="quick-item" @click="goToAIMatches">
					<view class="quick-icon"><uni-icons type="star" size="24" color="#165DFF" /></view>
					<text>AI智能匹配</text>
				</view>
				<view class="quick-item" @click="goToInterviewPractice">
					<view class="quick-icon"><uni-icons type="chat" size="24" color="#7C3AED" /></view>
					<text>模拟面试</text>
				</view>
				<view class="quick-item" @click="goToHotJobs">
					<view class="quick-icon"><uni-icons type="list" size="24" color="#165DFF" /></view>
					<text>热门岗位</text>
				</view>
				<view class="quick-item" @click="goToResume">
					<view class="quick-icon"><uni-icons type="compose" size="24" color="#165DFF" /></view>
					<text>简历管理</text>
				</view>
			</view>

			<!-- 求职数据 -->
			<view class="data-section">
				<view class="section-header">
					<text class="section-title">求职数据</text>
				</view>
				<view class="stat-row">
					<view class="stat-box">
						<text class="stat-num">{{ stats.deliveries || 0 }}</text>
						<text class="stat-label">投递次数</text>
					</view>
					<view class="stat-box">
						<text class="stat-num">{{ stats.viewed || 0 }}</text>
						<text class="stat-label">被查看</text>
					</view>
					<view class="stat-box">
						<text class="stat-num">{{ stats.interviews || 0 }}</text>
						<text class="stat-label">面试邀请</text>
					</view>
					<view class="stat-box">
						<text class="stat-num">{{ stats.offers || 0 }}</text>
						<text class="stat-label">录用通知</text>
					</view>
				</view>
			</view>

				<!-- 列表Tab + 岗位列表 -->
			<view class="list-section">
				<view class="list-tabs">
					<text class="list-tab" :class="{ active: currentListTab === 'recommend' }" @click="switchListTab('recommend')">推荐</text>
					<text class="list-tab" :class="{ active: currentListTab === 'latest' }" @click="switchListTab('latest')">最新</text>
					<text class="section-more" @click="loadMoreJobs">更多 ›</text>
				</view>
				<view class="card-list">
					<JobCard
						v-for="job in recommendJobs"
						:key="job.id"
						:job="job"
						:show-match="true"
						:match-score="job.matchScore"
						:delivered="deliveredJobIds.has(job.id)"
						:show-deliver="true"
						@click="goToJobDetail(job.id)"
						@deliver="handleDeliver"
					/>
					<EmptyState v-if="!recommendJobs.length" icon="inbox" title="暂无推荐岗位" desc="完善简历后系统会为你推荐匹配岗位" btn-text="完善简历" @action="goToProfile" />
				</view>
			</view>
		</scroll-view>
		<!-- 底部安全区 -->
		<view style="height: calc(50px + env(safe-area-inset-bottom))" />
		<TabBar current="home" />
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { request, jobAPI, deliveryAPI, statisticsAPI, favoriteAPI, mapJobData, matchAPI } from '@/utils/request'
import TabBar from '@/components/TabBar.vue'
import JobCard from '@/components/JobCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const keyword = ref('')
const unreadCount = ref(0)
const recommendJobs = ref([])
const stats = ref({})
const deliveredJobIds = ref(new Set())
const refreshing = ref(false)
const currentListTab = ref('recommend')
const statusBarHeight = ref(0)
const userName = ref('学生用户')

const mockJobs = [
	{ id: 1, title: '前端开发实习生', salaryRange: '4K-6K', location: '广州', education: '大专及以上', companyName: '广州科技公司', matchScore: 92 },
	{ id: 2, title: 'Java开发助理', salaryRange: '5K-7K', location: '深圳', education: '大专及以上', companyName: '深圳信息科技', matchScore: 88 },
	{ id: 3, title: 'UI设计实习生', salaryRange: '3K-5K', location: '广州', education: '大专及以上', companyName: '数字创意公司', matchScore: 85 }
].map(mapJobData)
const mockStats = { deliveries: 12, viewed: 8, interviews: 3, offers: 1 }

const switchListTab = (tab) => {
	if (currentListTab.value === tab) return
	currentListTab.value = tab
	loadData()
}

onMounted(async () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (raw) {
			const ui = JSON.parse(raw)
			userName.value = ui.realName || '学生用户'
		}
	} catch (e) { console.error('获取用户信息失败', e) }
	try {
		const winInfo = uni.getWindowInfo()
		statusBarHeight.value = winInfo.statusBarHeight || 0
	} catch (e) {
		try {
			const sysInfo = uni.getSystemInfoSync()
			statusBarHeight.value = sysInfo.statusBarHeight || 0
		} catch (e2) { console.error('获取状态栏高度失败', e2) }
	}
	await loadData()
	await loadUserState()
})

const onRefresh = async () => {
	refreshing.value = true
	await loadData()
	refreshing.value = false
}

const loadData = async () => {
	try {
		const [jobsRes, statsRes] = await Promise.all([
			jobAPI.getRecommendJobs({ sort: currentListTab.value }),
			statisticsAPI.getStudentOverview()
		])
		const rawJobs = jobsRes.data || []
		recommendJobs.value = await Promise.all(rawJobs.map(async (j) => {
			const mapped = mapJobData(j)
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

		// 加载匹配分数并合并到岗位数据中
		try {
			const studentId = getStudentId()
			if (studentId) {
				const matchRes = await matchAPI.getByStudent(studentId)
				const matchMap = {}
				;(matchRes.data || []).forEach(m => { matchMap[m.jobId] = m.matchScore })
				recommendJobs.value = recommendJobs.value.map(j => {
					const dbScore = matchMap[j.id]
					if (dbScore) {
						const score = typeof dbScore === 'number' && dbScore > 1
							? Math.round(dbScore)      // 已经是百分�?如 88)
							: Math.round(dbScore * 100)  // 小�?如 0.88)
						return { ...j, matchScore: score }
					}
					// 退化：从 mock 数据取默认匹配度
					const mock = mockJobs.find(m => m.id === j.id)
					return { ...j, matchScore: mock ? mock.matchScore : 0 }
				})
			}
		} catch (me) {
			console.log('加载匹配度失败', me)
		}
	} catch (e) {
		console.log('API接口未就绪，使用模拟数据')
		recommendJobs.value = currentListTab.value === 'latest' ? [...mockJobs].reverse() : mockJobs
		stats.value = mockStats
	}
}

const getStudentId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.id || obj.userId ? Number(obj.id || obj.userId) : null
	} catch (e) { return null }
}

const loadUserState = async () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return
		const obj = JSON.parse(raw)
		const sid = obj.id || obj.userId
		if (!sid) return
		const [dRes] = await Promise.all([
			deliveryAPI.getDeliveriesByStudentId({ studentId: Number(sid) })
		])
		deliveredJobIds.value = new Set((dRes.data || []).map(d => d.jobId))
	} catch (e) {
		console.log('加载用户状态失败', e)
	}
}

const handleSearch = () => {
	if (keyword.value.trim()) {
		uni.navigateTo({ url: '/pages/student/search-result?q=' + encodeURIComponent(keyword.value) })
	}
}

const handleDeliver = async (job) => {
	if (deliveredJobIds.value.has(job.id)) return
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return
		const obj = JSON.parse(raw)
		const sid = obj.id || obj.userId
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

const goToJobDetail = (id) => uni.navigateTo({ url: '/pages/student/job-detail?id=' + id })
const goToCollect = () => uni.navigateTo({ url: '/pages/student/collect' })
const goToProfile = () => uni.navigateTo({ url: '/pages/student/profile' })
const goToMessages = () => uni.navigateTo({ url: '/pages/student/messages' })
const goToAIMatches = () => uni.navigateTo({ url: '/pages/student/ai-matches' })
const goToInterviewPractice = () => uni.navigateTo({ url: '/pages/student/interview-practice' })
const goToHotJobs = () => uni.navigateTo({ url: '/pages/student/hot-jobs' })
const goToResume = () => uni.navigateTo({ url: '/pages/student/resume-edit' })
const goToCityPicker = () => uni.showToast({ title: '选择城市', icon: 'none' })
const loadMoreJobs = () => uni.showToast({ title: '加载更多...', icon: 'none' })
</script>

<style scoped>
/* Header */
.header-section {
	background: linear-gradient(135deg, #165DFF 0%, #2563EB 100%);
	color: white;
	padding: 16px 16px 24px;
	flex-shrink: 0;
}
.header-top {
	flex-direction: row;
	justify-content: flex-start;
	align-items: center;
	margin-bottom: 16px;
}
.greeting-text {
	font-size: 20px;
	font-weight: 700;
	color: #FFFFFF;
}

/* 搜索框 */
.search-box {
	flex-direction: row;
	align-items: center;
	gap: 8px;
	background: #FFFFFF;
	border-radius: 24px;
	padding: 0 16px;
	height: 40px;
	box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.search-box input {
	flex: 1;
	height: 100%;
	border: none;
	padding: 0;
	font-size: 14px;
	background: transparent;
	color: #1D2129;
	outline: none;
}
/* 快捷入口 */
.quick-menu {
	flex-direction: row;
	justify-content: space-around;
	padding: 20px 16px;
	background: white;
	margin: -12px 16px 0;
	border-radius: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
	position: relative;
	z-index: 10;
}
.quick-item {
	align-items: center;
	gap: 8px;
}
.quick-icon {
	width: 48px;
	height: 48px;
	border-radius: 12px;
	background: rgba(22,93,255,0.08);
	align-items: center;
	justify-content: center;
}
.quick-item text {
	font-size: 12px;
	color: #4E5969;
	font-weight: 500;
}
.quick-item:active {
	transform: scale(0.95);
}
/* Data Section */
.data-section {
	padding: 0 16px;
	margin-top: 20px;
}
.section-header {
	margin-bottom: 12px;
}
.section-title {
	font-size: 16px;
	font-weight: 600;
	color: #1D2129;
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
	border-radius: 12px;
	padding: 16px;
	align-items: flex-start;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.stat-num {
	font-size: 22px;
	font-weight: 700;
	color: #1D2129;
	margin-bottom: 4px;
}
.stat-label {
	font-size: 12px;
	color: #86909C;
}
/* 列表区 */
.list-section {
	padding: 0 16px;
	margin-top: 20px;
}
.list-tabs {
	flex-direction: row;
	align-items: center;
	margin-bottom: 12px;
	gap: 20px;
}
.list-tab {
	font-size: 15px;
	color: #86909C;
	font-weight: 500;
	padding-bottom: 4px;
	position: relative;
}
.list-tab.active {
	color: #1D2129;
	font-weight: 600;
}
.list-tab.active::after {
	content: '';
	position: absolute;
	bottom: 0;
	left: 0;
	width: 20px;
	height: 3px;
	background: #165DFF;
	border-radius: 2px;
}
.section-more {
	margin-left: auto;
	font-size: 13px;
	color: #165DFF;
	font-weight: 500;
}
.card-list {
	margin-bottom: 12px;
}
</style>
