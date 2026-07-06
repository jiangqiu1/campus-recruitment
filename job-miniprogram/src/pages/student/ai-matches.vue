<template>
	<view class="page-wrapper">
		<NavBar title="AI 岗位匹配" show-back />
		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<view class="match-banner">
				<view class="banner-icon">
					<uni-icons type="star-filled" size="32" color="#165DFF" />
				</view>
				<view class="banner-text">
					<text class="banner-title">AI 智能匹配</text>
					<text class="banner-desc">基于你的简历和技能，智能推荐最合适的岗位</text>
				</view>
			</view>
			<view class="sort-bar">
				<view v-for="opt in sortOptions" :key="opt.value" class="sort-item" :class="{ active: currentSort === opt.value }" @click="currentSort = opt.value">
					<text>{{ opt.label }}</text>
				</view>
			</view>
			<view class="action-bar">
				<text class="action-hint">由教师根据你的简历生成匹配推荐</text>
			</view>
			<view v-if="matches.length > 0" class="section">
				<view class="section-header">
					<text class="section-title">推荐岗位（按匹配度排序）</text>
					<text class="section-count">{{ matches.length }}个</text>
				</view>
				<view class="match-list">
					<view v-for="(item, i) in sortedMatches" :key="i" class="match-card" @click="showMatchDetail(item)">
						<view class="match-top">
							<view class="match-info">
								<text class="match-job-title">{{ item.jobTitle || '岗位#' + item.jobId }}</text>
								<text class="match-company">{{ item.companyName || '' }}</text>
							</view>
							<view class="match-score-box">
								<view class="score-circle" :style="{ borderColor: scoreColor(item.matchScore), backgroundColor: scoreBg(item.matchScore) }">
									<text class="score-text" :style="{ color: scoreColor(item.matchScore) }">{{ formatScore(item.matchScore) }}</text>
								</view>
								<text class="score-label">匹配度</text>
							</view>
						</view>
						<view class="match-reason" v-if="item.matchReason">
							<uni-icons type="info" size="12" color="#165DFF" />
							<text>{{ item.matchReason }}</text>
						</view>
						<view class="match-meta">
							<text class="meta-tag" v-if="item.isPushed == 1">已推送</text>
							<text class="meta-tag" v-if="item.isClicked == 1">已查看</text>
							<text class="meta-date">{{ formatTime(item.createTime) }}</text>
						</view>
					</view>
				</view>
			</view>
			<EmptyState v-else-if="!loading" icon="search" title="暂无匹配推荐" desc="点击上方按钮，AI将根据你的简历自动匹配岗位" />
		</scroll-view>

		<PopupDrawer :show="showDetail" @update:show="showDetail = $event" title="匹配详情">
			<view class="detail-body" v-if="detailItem">
				<view class="detail-job-section">
					<text class="detail-job-title">{{ detailItem.jobTitle }}</text>
					<text class="detail-company">{{ detailItem.companyName }}</text>
				</view>
				<view class="detail-score-section">
					<text class="detail-section-label">匹配评分</text>
					<view class="detail-score-ring" :style="{ borderColor: scoreColor(detailItem.matchScore), backgroundColor: scoreBg(detailItem.matchScore) }">
						<text :style="{ color: scoreColor(detailItem.matchScore) }">{{ formatScore(detailItem.matchScore) }}</text>
					</view>
				</view>
				<view class="detail-reason-section">
					<text class="detail-section-label">匹配理由</text>
					<text class="detail-reason-text">{{ detailItem.matchReason || '暂无' }}</text>
				</view>
				<view class="detail-dims-section">
					<text class="detail-section-label">维度分析</text>
					<view class="dim-bar">
						<view class="dim-row">
							<text class="dim-label">技能匹配</text>
							<view class="dim-track"><view class="dim-fill" :style="{ width: dimPercent('skill'), background: dimColor('skill') }"></view></view>
							<text class="dim-val">{{ formatDim(detailItem.skillScore) }}</text>
						</view>
						<view class="dim-row">
							<text class="dim-label">经验匹配</text>
							<view class="dim-track"><view class="dim-fill" :style="{ width: dimPercent('exp'), background: dimColor('exp') }"></view></view>
							<text class="dim-val">{{ formatDim(detailItem.expScore) }}</text>
						</view>
						<view class="dim-row">
							<text class="dim-label">学历匹配</text>
							<view class="dim-track"><view class="dim-fill" :style="{ width: dimPercent('edu'), background: dimColor('edu') }"></view></view>
							<text class="dim-val">{{ formatDim(detailItem.eduScore) }}</text>
						</view>
					</view>
				</view>
				<button class="detail-action-btn" @click="goToJob(detailItem)">查看岗位详情</button>
			</view>
		</PopupDrawer>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { matchAPI, jobAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import PopupDrawer from '@/components/PopupDrawer.vue'

const refreshing = ref(false)
const matches = ref([])
const loading = ref(false)
const showDetail = ref(false)
const detailItem = ref(null)
const currentSort = ref('score')

const sortOptions = [
	{ label: '匹配度', value: 'score' },
	{ label: '最新', value: 'time' },
	{ label: '薪资', value: 'salary' }
]

const getStudentId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.id || obj.userId ? Number(obj.id || obj.userId) : null
	} catch (e) { return null }
}

async function loadData() {
	const studentId = getStudentId()
	if (!studentId) return
	loading.value = true
	try {
		const res = await matchAPI.getByStudent(studentId)
		const raw = res.data || []
		const enriched = await Promise.all(raw.map(async (m) => {
			try {
				const jRes = await jobAPI.getJobDetail(m.jobId)
				const j = jRes.data || {}
				return { ...m, jobTitle: j.title || j.jobTitle || m.jobTitle, companyName: j.companyName || m.companyName }
			} catch (e) {
				return { ...m, jobTitle: m.jobTitle || ('岗位#' + m.jobId) }
			}
		}))
		matches.value = enriched
	} catch (e) {
		console.error('加载匹配结果失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	} finally {
		loading.value = false
	}
}
loadData()

const onRefresh = async () => {
	refreshing.value = true
	await loadData()
	refreshing.value = false
}

const sortedMatches = computed(() => {
	const arr = [...matches.value]
	switch (currentSort.value) {
		case 'score': return arr.sort((a, b) => (b.matchScore || 0) - (a.matchScore || 0))
		case 'time': return arr.sort((a, b) => (b.createTime || '').localeCompare(a.createTime || ''))
		case 'salary': return arr.sort((a, b) => (b.maxSalary || 0) - (a.maxSalary || 0))
		default: return arr
	}
})

const formatScore = (score) => {
	if (score == null) return '--'
	const num = typeof score === 'string' ? parseFloat(score) : score
	return Math.round(num * 100) + '%'
}
const scoreColor = (score) => {
	if (score == null) return '#86909C'
	const num = typeof score === 'string' ? parseFloat(score) : score
	if (num >= 0.8) return '#00B42A'
	if (num >= 0.6) return '#165DFF'
	if (num >= 0.4) return '#FF7D00'
	return '#F53F3F'
}
const scoreBg = (score) => {
	if (score == null) return 'rgba(134,144,156,0.06)'
	const num = typeof score === 'string' ? parseFloat(score) : score
	if (num >= 0.8) return 'rgba(0,180,42,0.06)'
	if (num >= 0.6) return 'rgba(22,93,255,0.06)'
	if (num >= 0.4) return 'rgba(255,125,0,0.06)'
	return 'rgba(245,63,63,0.06)'
}
const formatTime = (t) => {
	if (!t) return ''
	return t.substring(0, 10)
}
const showMatchDetail = (item) => {
	detailItem.value = item
	showDetail.value = true
}
const dimPercent = (dim) => {
	const item = detailItem.value
	if (!item) return '0%'
	const key = dim === 'skill' ? 'skillScore' : dim === 'exp' ? 'expScore' : 'eduScore'
	const val = item[key]
	if (val == null) return '50%'
	const num = typeof val === 'string' ? parseFloat(val) : val
	return Math.round(num * 100) + '%'
}
const dimColor = (dim) => {
	const item = detailItem.value; if (!item) return '#86909C'
	const key = dim === 'skill' ? 'skillScore' : dim === 'exp' ? 'expScore' : 'eduScore'
	const val = item[key]; if (val == null) return '#86909C'
	const num = typeof val === 'string' ? parseFloat(val) : val
	if (num >= 0.8) return '#00B42A'; if (num >= 0.6) return '#165DFF'; if (num >= 0.4) return '#FF7D00'; return '#F53F3F'
}
const formatDim = (val) => {
	if (val == null) return '--'
	const num = typeof val === 'string' ? parseFloat(val) : val
	return Math.round(num * 100) + '%'
}
const goToJob = (item) => {
	showDetail.value = false
	uni.navigateTo({ url: '/pages/student/job-detail?id=' + item.jobId })
}
</script>

<style scoped>
.match-banner { flex-direction: row; align-items: center; margin: 16px; padding: 20px; background: rgba(22,93,255,0.06); border-radius: 12px; gap: 16px; }
.banner-icon { width: 48px; height: 48px; border-radius: 12px; background: #FFFFFF; align-items: center; justify-content: center; }
.banner-text { flex: 1; }
.banner-title { font-size: 18px; font-weight: 700; color: #1D2129; display: block; margin-bottom: 4px; }
.banner-desc { font-size: 13px; color: #4E5969; }
.sort-bar { flex-direction: row; padding: 0 16px 12px; gap: 8px; }
.sort-item { padding: 6px 16px; border-radius: 20px; background: #F7F8FA; font-size: 13px; color: #4E5969; }
.sort-item.active { background: #165DFF; color: #fff; font-weight: 500; }
.action-bar { padding: 0 16px 12px; }
.action-hint { font-size: 12px; color: #86909C; text-align: center; display: block; }
.action-btn[disabled] { background: #E5E6EB !important; color: #A9AEB8 !important; border: none !important; }
.section { padding: 0 16px; }
.section-header { flex-direction: row; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-title { font-size: 16px; font-weight: 700; color: #1D2129; }
.section-count { font-size: 13px; color: #86909C; }
.match-list { flex-direction: column; gap: 12px; }
.match-card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.match-top { flex-direction: row; justify-content: space-between; align-items: flex-start; }
.match-info { flex: 1; }
.match-job-title { font-size: 16px; font-weight: 600; color: #1D2129; display: block; }
.match-company { font-size: 13px; color: #86909C; margin-top: 2px; display: block; }
.match-score-box { align-items: center; margin-left: 12px; }
.score-circle { width: 56px; height: 56px; border-radius: 50%; border-width: 3px; border-style: solid; align-items: center; justify-content: center; }
.score-text { font-size: 16px; font-weight: 700; }
.score-label { font-size: 11px; color: #86909C; margin-top: 2px; }
.match-reason { flex-direction: row; align-items: center; gap: 6px; margin-top: 10px; padding: 8px 12px; background: #F7F8FA; border-radius: 8px; font-size: 13px; color: #4E5969; }
.match-meta { flex-direction: row; align-items: center; gap: 8px; margin-top: 10px; }
.meta-tag { font-size: 11px; padding: 2px 8px; border-radius: 4px; background: rgba(22,93,255,0.08); color: #165DFF; font-weight: 500; }
.meta-date { font-size: 11px; color: #C9CDD4; margin-left: auto; }
.detail-body { padding: 0; }
.detail-job-section { margin-bottom: 16px; }
.detail-job-title { font-size: 17px; font-weight: 700; color: #1D2129; display: block; margin-bottom: 2px; }
.detail-company { font-size: 13px; color: #86909C; }
.detail-score-section { align-items: center; margin-bottom: 16px; }
.detail-section-label { font-size: 13px; color: #86909C; margin-bottom: 8px; align-self: flex-start; }
.detail-score-ring { width: 80px; height: 80px; border-radius: 50%; border-width: 4px; border-style: solid; align-items: center; justify-content: center; font-size: 24px; font-weight: 800; }
.detail-reason-section { margin-bottom: 16px; }
.detail-reason-text { font-size: 14px; color: #4E5969; line-height: 1.6; padding: 10px 14px; background: #F7F8FA; border-radius: 8px; }
.detail-dims-section { margin-bottom: 20px; }
.dim-bar { gap: 10px; }
.dim-row { flex-direction: row; align-items: center; gap: 8px; }
.dim-label { width: 56px; font-size: 12px; color: #4E5969; }
.dim-track { flex: 1; height: 8px; background: #F2F3F5; border-radius: 4px; overflow: hidden; }
.dim-fill { height: 100%; border-radius: 4px; transition: width 0.5s; }
.dim-val { width: 36px; font-size: 12px; font-weight: 600; color: #4E5969; text-align: right; }
.detail-action-btn { width: 100%; height: 44px; border-radius: 12px; background: #165DFF; color: #fff; font-size: 15px; font-weight: 600; align-items: center; justify-content: center; border: none; margin-top: 16px; flex-shrink: 0; }
.action-btn:active { opacity: 0.85; }
</style>
