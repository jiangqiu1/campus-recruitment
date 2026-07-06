#!/usr/bin/env python3
"""Write all correct student page files from the user-provided code."""
import os

BASE = r'C:\Users\yjq\.qclaw\workspace\校企项目\src\pages\student'

pages = {}

pages['home.vue'] = r"""<template>
	<view class="page-wrapper">
		<scroll-view class="content-scrollable" scroll-y refresher-enabled @refresherrefresh="onRefresh">
			<!-- 顶部导航 -->
			<view class="header-section">
				<view class="header-top">
					<view class="location" @click="goToCityPicker">
						<uni-icons type="location" size="16" color="rgba(255,255,255,0.9)" />
						<text class="location-text">广州</text>
						<uni-icons type="arrowdown" size="12" color="rgba(255,255,255,0.7)" />
					</view>
					<view class="header-actions">
						<view class="header-action-btn" @click="goToMessages">
							<uni-icons type="chat" size="20" color="#FFFFFF" />
							<text v-if="unreadCount" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</text>
						</view>
					</view>
				</view>
				<view class="search-box">
					<uni-icons type="search" size="16" color="#949599" />
					<input v-model="keyword" placeholder="搜索岗位、公司、关键词..." @confirm="handleSearch" />
				</view>
			</view>

			<!-- 快捷入口 -->
			<view class="quick-menu">
				<view class="quick-item" @click="goToAIMatches">
					<view class="quick-icon"><uni-icons type="star" size="24" color="#165DFF" /></view>
					<text>AI智能匹配</text>
				</view>
				<view class="quick-item" @click="goToJobSearch">
					<view class="quick-icon"><uni-icons type="list" size="24" color="#10B981" /></view>
					<text>热门岗位</text>
				</view>
				<view class="quick-item" @click="goToProfile">
					<view class="quick-icon"><uni-icons type="person" size="24" color="#E38330" /></view>
					<text>我的简历</text>
				</view>
				<view class="quick-item" @click="goToDeliveries">
					<view class="quick-icon"><uni-icons type="paperplane" size="24" color="#8B5CF6" /></view>
					<text>投递记录</text>
				</view>
			</view>

			<!-- 求职数据 -->
			<view class="data-section">
				<view class="section-header">
					<text class="section-title"><uni-icons type="list" size="16" color="#18181A" /> 求职数据</text>
				</view>
				<view class="stat-row">
					<view class="stat-box">
						<text class="stat-label">投递次数</text>
						<text class="stat-num">{{ stats.deliveries || 0 }}</text>
						<text class="stat-trend up">↑ 较上月</text>
					</view>
					<view class="stat-box">
						<text class="stat-label">被查看</text>
						<text class="stat-num success">{{ stats.viewed || 0 }}</text>
						<text class="stat-trend up">↑ 较上月</text>
					</view>
					<view class="stat-box">
						<text class="stat-label">面试邀请</text>
						<text class="stat-num">{{ stats.interviews || 0 }}</text>
						<text class="stat-trend">-- 较上月</text>
					</view>
					<view class="stat-box">
						<text class="stat-label">录用通知</text>
						<text class="stat-num accent">{{ stats.offers || 0 }}</text>
						<text class="stat-trend up">↑ 较上月</text>
					</view>
				</view>
			</view>

			<!-- AI推荐岗位 -->
			<view class="data-section">
				<view class="section-header">
					<text class="section-title"><uni-icons type="star" size="16" color="#0EA5E9" /> AI 推荐岗位</text>
					<text class="section-more" @click="loadMoreJobs">更多 ›</text>
				</view>
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
		</scroll-view>

		<TabBar current="home" />
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { request, jobAPI, deliveryAPI, statisticsAPI, favoriteAPI, mapJobData } from '@/utils/request'
import TabBar from '@/components/TabBar.vue'
import JobCard from '@/components/JobCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const keyword = ref('')
const unreadCount = ref(0)
const recommendJobs = ref([])
const stats = ref({})
const deliveredJobIds = ref(new Set())
const isRefreshing = ref(false)

const mockJobs = [
	{ id: 1, title: '前端开发实习生', salaryRange: '4K-6K', location: '广州', education: '大专及以上', companyName: '广州科技公司', matchScore: 92 },
	{ id: 2, title: 'Java开发助理', salaryRange: '5K-7K', location: '深圳', education: '大专及以上', companyName: '深圳信息科技', matchScore: 88 },
	{ id: 3, title: 'UI设计实习生', salaryRange: '3K-5K', location: '广州', education: '大专及以上', companyName: '数字创意公司', matchScore: 85 }
].map(mapJobData)

const mockStats = { deliveries: 12, viewed: 8, interviews: 3, offers: 1 }

onMounted(async () => {
	await loadData()
	await loadUserState()
})

const onRefresh = async () => {
	isRefreshing.value = true
	await loadData()
	isRefreshing.value = false
	uni.stopPullDownRefresh()
}

const loadData = async () => {
	try {
		const [jobsRes, statsRes] = await Promise.all([
			jobAPI.getRecommendJobs(),
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
	} catch (e) {
		console.log('API接口未就绪，使用模拟数据')
		recommendJobs.value = mockJobs
		stats.value = mockStats
	}
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
const goToDeliveries = () => uni.navigateTo({ url: '/pages/student/deliveries' })
const goToCollect = () => uni.navigateTo({ url: '/pages/student/collect' })
const goToProfile = () => uni.navigateTo({ url: '/pages/student/profile' })
const goToMessages = () => uni.navigateTo({ url: '/pages/student/messages' })
const goToAIMatches = () => uni.navigateTo({ url: '/pages/student/ai-matches' })
const goToJobSearch = () => uni.navigateTo({ url: '/pages/student/search-result' })
const goToCityPicker = () => uni.showToast({ title: '选择城市', icon: 'none' })
const loadMoreJobs = () => uni.showToast({ title: '加载更多...', icon: 'none' })
</script>

<style scoped>
.header-section {
	background: linear-gradient(135deg, #165DFF 0%, #2563EB 100%);
	color: white;
	padding: 16px;
	flex-shrink: 0;
}
.header-top {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
}
.location {
	flex-direction: row;
	align-items: center;
	gap: 4px;
}
.location-text {
	font-size: 14px;
	color: rgba(255,255,255,0.9);
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
	position: relative;
}
.badge {
	position: absolute;
	top: -2px;
	right: -2px;
	min-width: 16px;
	height: 16px;
	background: #E34D4F;
	border-radius: 50%;
	font-size: 10px;
	align-items: center;
	justify-content: center;
	border: 2px solid #165DFF;
	color: white;
	font-weight: 600;
	padding: 0 3px;
}
.search-box {
	position: relative;
	margin-top: 12px;
	flex-direction: row;
	align-items: center;
	gap: 8px;
}
.search-box input {
	flex: 1;
	height: 40px;
	border-radius: 8px;
	border: none;
	padding: 0 12px;
	font-size: 14px;
	background: rgba(255,255,255,0.95);
	color: #1D2129;
	outline: none;
}
.quick-menu {
	flex-direction: row;
	justify-content: space-around;
	padding: 16px;
	background: white;
	margin: 12px 0 0;
}
.quick-item {
	align-items: center;
	gap: 6px;
}
.quick-icon {
	width: 48px;
	height: 48px;
	border-radius: 12px;
	align-items: center;
	justify-content: center;
	background: #F8F9FC;
}
.quick-item text {
	font-size: 12px;
	color: #4E5969;
	font-weight: 500;
}
.data-section {
	padding: 0 16px;
	margin-top: 12px;
}
.section-header {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 12px;
}
.section-title {
	font-size: 16px;
	font-weight: 600;
	color: #18181A;
	flex-direction: row;
	align-items: center;
	gap: 6px;
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
	border-radius: 12px;
	padding: 16px;
}
.stat-label {
	font-size: 13px;
	color: #949599;
	margin-bottom: 4px;
}
.stat-num {
	font-size: 24px;
	font-weight: 700;
	color: #18181A;
}
.stat-num.success { color: #61C758; }
.stat-num.accent { color: #165DFF; }
.stat-trend {
	font-size: 11px;
	color: #949599;
	margin-top: 4px;
}
.stat-trend.up { color: #61C758; }
.card-list {
	padding: 0 16px;
	margin-bottom: 12px;
}
</style>"""

pages['collect.vue'] = r"""<template>
	<view class="page-wrapper">
		<NavBar title="我的收藏" />
		<scroll-view class="content-scrollable" scroll-y refresher-enabled @refresherrefresh="onRefresh" @scrolltolower="loadMore">
			<view v-if="editing" class="batch-bar">
				<button class="batch-btn" @click="batchRemove">批量取消收藏</button>
			</view>
			<view class="sort-bar">
				<text v-for="s in sorts" :key="s.value" class="sort-tab" :class="{ active: currentSort === s.value }" @click="currentSort = s.value">{{ s.label }}</text>
				<text class="edit-text" @click="editing = !editing">{{ editing ? '完成' : '编辑' }}</text>
			</view>
			<view class="card-list">
				<view v-for="(fav, i) in sortedList" :key="i" class="card-item" @click="!editing && goToDetail(fav.jobId)">
					<view v-if="editing" class="check-box" @click.stop="toggleSelect(fav.jobId)">
						<uni-icons :type="selectedIds.has(fav.jobId) ? 'checkbox-filled' : 'circle'" :size="20" :color="selectedIds.has(fav.jobId) ? '#165DFF' : '#C9CDD4'" />
					</view>
					<view class="card-body">
						<view class="card-header">
							<text class="card-title">{{ fav.title || '岗位 #' + fav.jobId }}</text>
							<text class="card-salary">{{ fav.salaryText || '' }}</text>
						</view>
						<text class="card-sub">{{ fav.companyName || '' }}</text>
						<view class="card-meta">
							<text class="card-location">{{ fav.location || '' }}</text>
							<text class="card-edu">{{ fav.education || '' }}</text>
						</view>
						<view v-if="!editing" class="card-actions">
							<button class="action-btn btn-primary" :class="{ disabled: deliveredJobIds.has(fav.jobId) }" :disabled="deliveredJobIds.has(fav.jobId)" @click.stop="handleDeliver(fav)">{{ deliveredJobIds.has(fav.jobId) ? '已投递' : '投递' }}</button>
						</view>
					</view>
				</view>
				<EmptyState v-if="!favorites.length" icon="star" title="暂无收藏" desc="浏览岗位时点击收藏按钮即可添加" />
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { favoriteAPI, deliveryAPI, jobAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const refreshing = ref(false)
const favorites = ref([])
const deliveredJobIds = ref(new Set())
const editing = ref(false)
const selectedIds = ref(new Set())
const currentSort = ref('time')

const sorts = [
	{ label: '按时间', value: 'time' },
	{ label: '按薪资', value: 'salary' },
	{ label: '按匹配度', value: 'match' }
]

const sortedList = computed(() => {
	const list = [...favorites.value]
	if (currentSort.value === 'salary') {
		list.sort((a, b) => parseSalary(b.salaryText) - parseSalary(a.salaryText))
	}
	return list
})

const parseSalary = (s) => {
	if (!s) return 0
	const nums = s.match(/\d+/g)
	return nums ? parseInt(nums[0]) : 0
}

const getStudentId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.id || obj.userId ? Number(obj.id || obj.userId) : null
	} catch (e) { return null }
}

loadData()

async function loadData() {
	const sid = getStudentId()
	if (!sid) { favorites.value = []; return }
	try {
		const fRes = await favoriteAPI.getFavorites({ studentId: sid })
		const rawList = fRes.data || []
		if (!Array.isArray(rawList) || !rawList.length) { favorites.value = []; return }
		const enriched = []
		for (const fav of rawList) {
			const jobId = fav.jobId || fav.id
			if (!jobId) continue
			try {
				const jRes = await jobAPI.getJobDetail(jobId)
				const job = jRes.data || {}
				enriched.push({
					...fav, jobId,
					title: job.title || job.jobTitle || '岗位 #' + jobId,
					companyName: job.companyName || '',
					location: job.location || '',
					education: job.education || '',
					salaryText: job.salaryText || job.salaryRange || ''
				})
			} catch (e) {
				enriched.push({ ...fav, jobId, title: '岗位 #' + jobId, companyName: '', location: '', education: '', salaryText: '' })
			}
		}
		favorites.value = enriched
		try {
			const dRes = await deliveryAPI.getDeliveriesByStudentId({ studentId: sid })
			deliveredJobIds.value = new Set((dRes.data || []).map(d => d.jobId))
		} catch (e) { deliveredJobIds.value = new Set() }
	} catch (e) {
		console.error('加载收藏失败', e)
		favorites.value = []
	}
}

const onRefresh = async () => {
	refreshing.value = true
	await loadData()
	refreshing.value = false
}

const loadMore = () => {}

const toggleSelect = (jobId) => {
	if (!jobId) return
	const next = new Set(selectedIds.value)
	if (next.has(jobId)) next.delete(jobId); else next.add(jobId)
	selectedIds.value = next
}

const batchRemove = async () => {
	const ids = [...selectedIds.value]
	if (!ids.length) { uni.showToast({ title: '请选择要取消收藏的岗位', icon: 'none' }); return }
	uni.showModal({
		title: '批量取消收藏',
		content: '确定取消选中的 ' + ids.length + ' 个收藏吗？',
		success: async (r) => {
			if (!r.confirm) return
			const sid = getStudentId()
			try {
				await Promise.all(ids.map(jid => favoriteAPI.removeFavorite(jid, sid)))
				favorites.value = favorites.value.filter(f => !ids.includes(f.jobId || f.id))
				selectedIds.value = new Set()
				editing.value = false
				uni.showToast({ title: '已取消收藏', icon: 'success' })
			} catch (e) { uni.showToast({ title: '操作失败，请重试', icon: 'none' }) }
		}
	})
}

const goToDetail = (id) => {
	if (!id) return
	uni.navigateTo({ url: '/pages/student/job-detail?id=' + id })
}

const handleDeliver = async (job) => {
	const jid = job.jobId || job.id
	if (!jid) return
	if (deliveredJobIds.value.has(jid)) return
	try {
		await deliveryAPI.createDelivery({ jobId: jid, studentId: getStudentId() })
		deliveredJobIds.value.add(jid)
		uni.showToast({ title: '投递成功', icon: 'success' })
	} catch (e) {
		if (e?.message?.includes('重复投递')) { deliveredJobIds.value.add(jid); uni.showToast({ title: '已投递过', icon: 'none' }) }
		else { uni.showToast({ title: '投递失败', icon: 'none' }) }
	}
}
</script>

<style scoped>
.batch-bar { padding: 12px 16px; background: #FFFFFF; border-bottom: 1px solid #F2F3F5; }
.batch-btn { padding: 8px 16px; background: rgba(227, 77, 79, 0.1); color: #E34D4F; border: none; border-radius: 6px; font-size: 13px; font-weight: 500; }
.sort-bar { flex-direction: row; align-items: center; padding: 10px 16px; background: #FFFFFF; gap: 12px; border-bottom: 1px solid #F2F3F5; }
.sort-tab { font-size: 13px; color: #949599; font-weight: 500; }
.sort-tab.active { color: #165DFF; font-weight: 600; }
.edit-text { margin-left: auto; font-size: 13px; color: #165DFF; font-weight: 500; }
.card-list { padding: 12px 16px; gap: 12px; }
.card-item { flex-direction: row; background: #FFFFFF; border-radius: 12px; padding: 14px; gap: 12px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.check-box { justify-content: center; align-items: center; }
.card-body { flex: 1; gap: 6px; }
.card-header { flex-direction: row; justify-content: space-between; align-items: center; }
.card-title { font-size: 15px; font-weight: 600; color: #18181A; flex: 1; }
.card-salary { font-size: 14px; font-weight: 600; color: #165DFF; }
.card-sub { font-size: 13px; color: #626366; }
.card-meta { flex-direction: row; gap: 8px; }
.card-location, .card-edu { font-size: 12px; color: #949599; }
.card-actions { flex-direction: row; gap: 8px; margin-top: 4px; }
.action-btn { flex: 1; padding: 8px; border-radius: 6px; font-size: 13px; font-weight: 500; align-items: center; justify-content: center; border: none; }
.btn-primary { background: #165DFF; color: #FFFFFF; }
.btn-primary.disabled { background: #E5E6EB; color: #A9AEB8; }
</style>"""

# Write all files
for filename, content in pages.items():
    path = os.path.join(BASE, filename)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f'Written: {path}')
