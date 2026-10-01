<template>
	<view class="page-wrapper">
		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<!-- 顶部导航 -->
			<view class="header-section" :style="{ paddingTop: (statusBarHeight + 16) + 'px' }">
				<view class="header-top">
					<view class="greeting-wrap">
						<text class="greeting-text">您好，{{ userName }}</text>
						<text class="greeting-sub">今天继续完成你的求职计划</text>
					</view>
				</view>
				<view class="search-box">
					<uni-icons type="search" size="16" color="rgba(255,255,255,0.8)" />
					<input v-model="keyword" placeholder="搜索岗位、公司、关键词..." placeholder-style="color: rgba(255,255,255,0.65)" @confirm="handleSearch" />
				</view>
			</view>

			<!-- 求职进度漏斗 -->
			<view class="progress-card">
				<view class="progress-header">
					<text class="progress-title">求职进度</text>
					<text class="progress-more" @click="goToDeliveries">投递记录 ›</text>
				</view>
				<view class="funnel-row">
					<view
						v-for="(step, i) in funnelSteps"
						:key="i"
						class="funnel-step"
						:class="{ done: step.done, current: i === currentStep, success: step.done && i === funnelSteps.length - 1 }"
					>
						<view class="funnel-dot" />
						<text class="funnel-num">{{ step.num }}</text>
						<text class="funnel-label">{{ step.label }}</text>
					</view>
				</view>
			</view>

			<!-- 优先行动 -->
			<view class="action-card" v-if="actions.length">
				<view class="progress-header">
					<text class="progress-title">优先行动</text>
				</view>
				<view
					v-for="(a, i) in actions"
					:key="i"
					class="action-item"
					@click="handleAction(a)"
				>
					<view class="action-icon" :style="{ background: a.bg }">
						<uni-icons :type="a.icon" size="18" :color="a.color" />
					</view>
					<view class="action-texts">
						<text class="action-text">{{ a.text }}</text>
						<text v-if="a.sub" class="action-sub">{{ a.sub }}</text>
					</view>
					<uni-icons type="arrowright" size="14" color="#C9CDD4" />
				</view>
			</view>

			<!-- AI 智能匹配大入口 -->
			<view class="ai-entry" @click="goToAIMatches">
				<view class="ai-entry-icon">
					<uni-icons type="star-filled" size="24" color="#FFFFFF" />
				</view>
				<view class="ai-entry-texts">
					<text class="ai-entry-title">AI 智能匹配</text>
					<text class="ai-entry-sub">{{ matchCount > 0 ? '已为你匹配 ' + matchCount + ' 个岗位' : '完善简历后为你智能推荐岗位' }}</text>
				</view>
				<view class="ai-entry-score" v-if="topMatchScore > 0">
					<text class="ai-entry-score-num">{{ topMatchScore }}%</text>
					<text class="ai-entry-score-label">最高匹配</text>
				</view>
				<uni-icons v-else type="arrowright" size="16" color="#FFFFFF" />
			</view>

			<!-- 次级入口 -->
			<view class="sub-entries">
				<view class="sub-entry" @click="goToInterviewPractice">
					<view class="sub-entry-icon"><uni-icons type="chat" size="22" color="#0EA5E9" /></view>
					<text>模拟面试</text>
				</view>
				<view class="sub-entry" @click="goToHotJobs">
					<view class="sub-entry-icon"><uni-icons type="list" size="22" color="#165DFF" /></view>
					<text>热门岗位</text>
				</view>
				<view class="sub-entry" @click="goToResume">
					<view class="sub-entry-icon"><uni-icons type="compose" size="22" color="#165DFF" /></view>
					<text>简历管理</text>
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
import { ref, computed, onMounted } from 'vue'
import { request, jobAPI, deliveryAPI, statisticsAPI, favoriteAPI, mapJobData, matchAPI, resumeAPI } from '@/utils/request'
import TabBar from '@/components/TabBar.vue'
import JobCard from '@/components/JobCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const keyword = ref('')
const recommendJobs = ref([])
const stats = ref({})
const deliveredJobIds = ref(new Set())
const refreshing = ref(false)
const currentListTab = ref('recommend')
const statusBarHeight = ref(0)
const userName = ref('学生用户')
const resume = ref(null)
const matchCount = ref(0)
const topMatchScore = ref(0)

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

// 求职进度漏斗：建简历 → 投递 → 被查看 → 面试 → 录用
const funnelSteps = computed(() => [
	{ label: '建简历', num: resume.value ? '✓' : '0', done: !!resume.value },
	{ label: '投递', num: String(stats.value.deliveries || 0), done: (stats.value.deliveries || 0) > 0 },
	{ label: '被查看', num: String(stats.value.viewed || 0), done: (stats.value.viewed || 0) > 0 },
	{ label: '面试', num: String(stats.value.interviews || 0), done: (stats.value.interviews || 0) > 0 },
	{ label: '录用', num: String(stats.value.offers || 0), done: (stats.value.offers || 0) > 0 }
])
const currentStep = computed(() => funnelSteps.value.findIndex(s => !s.done))

// 简历缺失字段（字段级判断，给"优先行动"用）
const missingResumeFields = () => {
	const r = resume.value
	if (!r) return ['完整简历']
	const fields = [
		['求职意向', r.jobTarget], ['教育经历', r.education], ['实习经历', r.internship],
		['项目经历', r.project], ['技能', r.skills], ['自我评价', r.selfEvaluation]
	]
	return fields.filter(([, v]) => !v || !String(v).trim() || v === '[]').map(([label]) => label)
}

// 优先行动：按数据状态生成最多 3 条建议
const actions = computed(() => {
	const list = []
	if (!resume.value) {
		list.push({ icon: 'compose', color: '#165DFF', bg: 'rgba(22,93,255,0.08)', text: '创建你的第一份简历', sub: 'AI 匹配和投递都需要一份简历', url: '/pages/student/resume-edit' })
	} else {
		const missing = missingResumeFields()
		if (missing.length) {
			list.push({ icon: 'compose', color: '#165DFF', bg: 'rgba(22,93,255,0.08)', text: '完善简历：还缺' + missing.slice(0, 2).join('、'), sub: '完整度越高，AI 匹配越精准', url: '/pages/student/resume-edit' })
		}
	}
	if ((stats.value.interviews || 0) > 0) {
		list.push({ icon: 'chat', color: '#0EA5E9', bg: 'rgba(14,165,233,0.1)', text: '有面试在推进，先练几道模拟题', sub: 'AI 出题 + 逐题点评', url: '/pages/student/interview-practice' })
	}
	if (matchCount.value > 0) {
		list.push({ icon: 'star', color: '#0EA5E9', bg: 'rgba(14,165,233,0.1)', text: '查看 AI 为你匹配的 ' + matchCount.value + ' 个岗位', sub: '最高匹配 ' + topMatchScore.value + '%', url: '/pages/student/ai-matches' })
	}
	if (!list.length) {
		list.push({ icon: 'list', color: '#165DFF', bg: 'rgba(22,93,255,0.08)', text: '去热门岗位看看今天的机会', sub: '', url: '/pages/student/hot-jobs' })
	}
	return list.slice(0, 3)
})

const handleAction = (a) => uni.navigateTo({ url: a.url })

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
	await Promise.all([loadData(), loadUserState(), loadResume()])
})

const onRefresh = async () => {
	refreshing.value = true
	await Promise.all([loadData(), loadResume()])
	refreshing.value = false
}

const loadResume = async () => {
	try {
		const res = await resumeAPI.getResume()
		resume.value = res.data || null
	} catch (e) { resume.value = null }
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
				const matches = matchRes.data || []
				matchCount.value = matches.length
				const normalize = (v) => (typeof v === 'number' && v > 1 ? Math.round(v) : Math.round((v || 0) * 100))
				topMatchScore.value = matches.reduce((max, m) => Math.max(max, normalize(m.matchScore)), 0)
				const matchMap = {}
				matches.forEach(m => { matchMap[m.jobId] = m.matchScore })
				recommendJobs.value = recommendJobs.value.map(j => {
					const dbScore = matchMap[j.id]
					if (dbScore) {
						const score = typeof dbScore === 'number' && dbScore > 1
							? Math.round(dbScore)      // 已经是百分数(如 88)
							: Math.round(dbScore * 100)  // 小数(如 0.88)
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
		matchCount.value = mockJobs.length
		topMatchScore.value = 92
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
const goToProfile = () => uni.navigateTo({ url: '/pages/student/profile' })
const goToAIMatches = () => uni.navigateTo({ url: '/pages/student/ai-matches' })
const goToInterviewPractice = () => uni.navigateTo({ url: '/pages/student/interview-practice' })
const goToHotJobs = () => uni.navigateTo({ url: '/pages/student/hot-jobs' })
const goToResume = () => uni.navigateTo({ url: '/pages/student/resume-edit' })
const goToDeliveries = () => uni.reLaunch({ url: '/pages/student/deliveries' })
const loadMoreJobs = () => uni.showToast({ title: '加载更多...', icon: 'none' })
</script>

<style scoped lang="scss">
/* Header：深色 Hero + 光斑 */
.header-section {
	position: relative;
	overflow: hidden;
	background: $uni-gradient-hero;
	color: white;
	padding: 16px 16px 24px;
	flex-shrink: 0;
}
.header-section::after {
	content: '';
	position: absolute;
	top: -30px;
	right: -30px;
	width: 160px;
	height: 160px;
	border-radius: 50%;
	background: rgba(255, 255, 255, 0.1);
	pointer-events: none;
}
.header-top {
	flex-direction: row;
	justify-content: flex-start;
	align-items: center;
	margin-bottom: 12px;
	position: relative;
	z-index: 1;
}
.greeting-wrap { flex: 1; }
.greeting-text {
	font-size: 20px;
	font-weight: 700;
	color: $uni-text-color-inverse;
	display: block;
}
.greeting-sub {
	font-size: 12px;
	color: rgba(255, 255, 255, 0.85);
	margin-top: 4px;
}

/* 搜索框：玻璃拟态 */
.search-box {
	flex-direction: row;
	align-items: center;
	gap: 8px;
	background: rgba(255, 255, 255, 0.16);
	border: 1px solid rgba(255, 255, 255, 0.3);
	border-radius: 999px;
	padding: 0 16px;
	height: 40px;
	position: relative;
	z-index: 1;
}
.search-box input {
	flex: 1;
	height: 100%;
	border: none;
	padding: 0;
	font-size: 14px;
	background: transparent;
	color: $uni-text-color-inverse;
	outline: none;
}

/* 求职进度漏斗 */
.progress-card {
	background: $uni-bg-color;
	border-radius: 12px;
	margin: -12px 16px 0;
	padding: 16px;
	box-shadow: $uni-shadow-sm;
	position: relative;
	z-index: 10;
}
.progress-header {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
}
.progress-title {
	font-size: 15px;
	font-weight: 600;
	color: $uni-text-color-title;
}
.progress-more {
	font-size: 12px;
	color: $uni-text-color-secondary;
}
.funnel-row {
	flex-direction: row;
	align-items: flex-start;
	margin-top: 16px;
}
.funnel-step {
	flex: 1;
	align-items: center;
	position: relative;
}
/* 节点间连接线 */
.funnel-step::after {
	content: '';
	position: absolute;
	top: 5px;
	left: calc(50% + 12px);
	width: calc(100% - 24px);
	height: 2px;
	background: $uni-border-color-divider;
}
.funnel-step:last-child::after {
	display: none;
}
.funnel-step.done::after {
	background: $uni-color-primary;
}
.funnel-dot {
	width: 12px;
	height: 12px;
	border-radius: 50%;
	background: $uni-border-color-divider;
	margin-bottom: 8px;
	z-index: 1;
}
.funnel-step.done .funnel-dot {
	background: $uni-color-primary;
}
.funnel-step.current .funnel-dot {
	background: $uni-color-primary;
	box-shadow: 0 0 0 4px $uni-color-primary-light;
}
/* 录用节点：成功绿收尾 */
.funnel-step.success .funnel-dot {
	background: $uni-color-success;
}
.funnel-step.success .funnel-num {
	color: $uni-color-success;
}
.funnel-num {
	font-size: 16px;
	font-weight: 700;
	color: $uni-text-color-title;
}
.funnel-step.current .funnel-num {
	color: $uni-color-primary;
}
.funnel-label {
	font-size: 12px;
	color: $uni-text-color-secondary;
	margin-top: 2px;
}

/* 优先行动 */
.action-card {
	background: $uni-bg-color;
	border-radius: 12px;
	margin: 12px 16px 0;
	padding: 16px;
	box-shadow: $uni-shadow-sm;
}
.action-item {
	flex-direction: row;
	align-items: center;
	gap: 12px;
	padding: 12px 0;
	border-bottom: 0.5px solid $uni-border-color-divider;
}
.action-item:last-child {
	border-bottom: none;
	padding-bottom: 0;
}
.action-icon {
	width: 40px;
	height: 40px;
	border-radius: 10px;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}
.action-texts { flex: 1; }
.action-text {
	font-size: 14px;
	font-weight: 600;
	color: $uni-text-color-title;
	display: block;
}
.action-sub {
	font-size: 12px;
	color: $uni-text-color-secondary;
	margin-top: 2px;
	display: block;
}

/* AI 智能匹配大入口 */
.ai-entry {
	flex-direction: row;
	align-items: center;
	gap: 12px;
	background: $uni-gradient-primary;
	border-radius: 12px;
	margin: 12px 16px 0;
	padding: 16px;
	box-shadow: 0 4px 12px rgba(22, 93, 255, 0.25);
}
.ai-entry:active { opacity: 0.9; }
.ai-entry-icon {
	width: 44px;
	height: 44px;
	border-radius: 12px;
	background: rgba(255, 255, 255, 0.2);
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}
.ai-entry-texts { flex: 1; }
.ai-entry-title {
	font-size: 16px;
	font-weight: 700;
	color: $uni-text-color-inverse;
	display: block;
}
.ai-entry-sub {
	font-size: 12px;
	color: rgba(255, 255, 255, 0.85);
	margin-top: 2px;
	display: block;
}
.ai-entry-score {
	align-items: center;
}
.ai-entry-score-num {
	font-size: 22px;
	font-weight: 800;
	color: $uni-text-color-inverse;
}
.ai-entry-score-label {
	font-size: 10px;
	color: rgba(255, 255, 255, 0.85);
	margin-top: 2px;
}

/* 次级入口 */
.sub-entries {
	flex-direction: row;
	background: $uni-bg-color;
	border-radius: 12px;
	margin: 12px 16px 0;
	padding: 14px 0;
	box-shadow: $uni-shadow-sm;
}
.sub-entry {
	flex: 1;
	align-items: center;
	gap: 6px;
}
.sub-entry:active { opacity: 0.7; }
.sub-entry-icon {
	width: 40px;
	height: 40px;
	border-radius: 10px;
	background: $uni-bg-color-page;
	align-items: center;
	justify-content: center;
}
.sub-entry text {
	font-size: 12px;
	color: $uni-text-color;
	font-weight: 500;
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
	color: $uni-text-color-secondary;
	font-weight: 500;
	padding-bottom: 4px;
	position: relative;
}
.list-tab.active {
	color: $uni-text-color-title;
	font-weight: 600;
}
.list-tab.active::after {
	content: '';
	position: absolute;
	bottom: 0;
	left: 0;
	width: 20px;
	height: 3px;
	background: $uni-color-primary;
	border-radius: 4px;
}
.section-more {
	margin-left: auto;
	font-size: 13px;
	color: $uni-color-primary;
	font-weight: 500;
}
.card-list {
	margin-bottom: 12px;
}
</style>
