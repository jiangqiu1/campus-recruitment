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
					<text class="banner-desc">基于你的简历、技能与岗位要求生成 · 教师推送后自动更新</text>
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
								<text class="score-label" :style="{ color: matchVerdict(item.matchScore).color }">{{ matchVerdict(item.matchScore).text }}</text>
							</view>
						</view>
						<view class="match-reason" v-if="item.matchReason">
							<uni-icons type="info" size="12" color="#165DFF" />
							<text>{{ item.matchReason }}</text>
						</view>
						<view class="match-improve" v-if="improveOf(item)">
							<text class="match-improve-mark">△</text>
							<text>待提升：{{ improveOf(item) }}</text>
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
					<text class="detail-verdict" :style="{ color: matchVerdict(detailItem.matchScore).color }">{{ matchVerdict(detailItem.matchScore).text }}</text>
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
				<view class="detail-improve-section">
					<text class="detail-section-label">待提升</text>
					<view v-for="(w, i) in weakDims" :key="i" class="improve-item">
						<text class="improve-mark">△</text>
						<text class="improve-text">{{ w }}</text>
					</view>
					<view v-if="!weakDims.length" class="improve-item">
						<text class="improve-mark improve-mark--ok">✓</text>
						<text class="improve-text">各维度均衡，可放心投递</text>
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
import { formatTimeSemantic } from '@/utils/format'
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
		const enriched = (await Promise.all(raw.map(async (m) => {
			try {
				const jRes = await jobAPI.getJobDetail(m.jobId)
				const j = jRes.data || {}
				// 岗位已被删除/不可见时不再显示"岗位#N"裸标题，直接过滤
				if (!j.id && !m.jobTitle) return null
				return { ...m, jobTitle: j.title || j.jobTitle || m.jobTitle, companyName: j.companyName || m.companyName }
			} catch (e) {
				return m.jobTitle ? { ...m } : null
			}
		}))).filter(Boolean)
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
// 列表卡「待提升」：从该条匹配的三维分取最弱项（<70% 视为待提升）
const IMPROVE_ADVICE = {
	skill: '补充岗位要求的技术栈',
	exp: '补充相关实习或项目经历',
	edu: '在简历中突出相关课程与自学成果'
}
const improveOf = (item) => {
	if (!item || !item.scoreDetail) return ''
	const defs = [['skill', '技能'], ['exp', '经验'], ['edu', '学历']]
	for (const [key, label] of defs) {
		const raw = Number(item.scoreDetail[key])
		if (!isNaN(raw)) {
			const p = raw > 1 ? Math.round(raw) : Math.round(raw * 100)
			if (p < 70) return label + '匹配 ' + p + '%，' + IMPROVE_ADVICE[key]
		}
	}
	return ''
}

// 匹配区间语义：让 AI 分数直接支撑投递决策
const matchVerdict = (score) => {
	const num = typeof score === 'number' ? score : parseFloat(score) || 0
	const p = num > 1 ? Math.round(num) : Math.round(num * 100)
	if (p >= 90) return { text: '非常匹配', color: '#00B42A' }
	if (p >= 80) return { text: '值得投递', color: '#00B42A' }
	if (p >= 70) return { text: '部分匹配', color: '#165DFF' }
	return { text: '建议谨慎', color: '#FF7D00' }
}

const formatTime = (t) => formatTimeSemantic(t)
const showMatchDetail = (item) => {
	// 从 scoreDetail 中提取维度分数
	const sd = item.scoreDetail || {}
	detailItem.value = {
		...item,
		skillScore: sd.skillMatch != null ? sd.skillMatch / 100 : null,
		expScore: sd.expMatch != null ? sd.expMatch / 100 : null,
		eduScore: sd.eduMatch != null ? sd.eduMatch / 100 : null
	}
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
// 待提升：从弱项维度生成可解释的改进建议（<70% 视为待提升）
const DIM_ADVICE = {
	skill: '补充岗位要求的技术栈，并在项目经历中体现实际应用',
	exp: '补充与岗位方向相关的实习或项目经历',
	edu: '在简历中突出与岗位相关的课程、证书与自学成果'
}
const weakDims = computed(() => {
	const item = detailItem.value
	if (!item) return []
	const defs = [['skill', '技能匹配'], ['exp', '经验匹配'], ['edu', '学历匹配']]
	const list = []
	defs.forEach(([key, label]) => {
		const val = item[key + 'Score']
		if (val == null) return
		const num = typeof val === 'string' ? parseFloat(val) : val
		if (num < 0.7) list.push(label + ' ' + Math.round(num * 100) + '%：' + DIM_ADVICE[key])
	})
	return list
})

const goToJob = (item) => {
	showDetail.value = false
	uni.navigateTo({ url: '/pages/student/job-detail?id=' + item.jobId })
}
</script>

<style scoped lang="scss">
.match-banner { flex-direction: row; align-items: center; margin: 16px; padding: 20px; background: $uni-color-primary-light; border-radius: 12px; gap: 16px; }
.banner-icon { width: 48px; height: 48px; border-radius: 12px; background: $uni-bg-color; align-items: center; justify-content: center; }
.banner-text { flex: 1; }
.banner-title { font-size: 18px; font-weight: 700; color: $uni-text-color-title; display: block; margin-bottom: 4px; }
.banner-desc { font-size: 13px; color: $uni-text-color; }
.sort-bar { flex-direction: row; padding: 0 16px 12px; gap: 8px; }
.sort-item { padding: 6px 16px; border-radius: 999px; background: $uni-bg-color-page; font-size: 13px; color: $uni-text-color; }
.sort-item.active { background: $uni-color-primary; color: $uni-text-color-inverse; font-weight: 500; }
.action-bar { padding: 0 16px 12px; }
.action-hint { font-size: 12px; color: $uni-text-color-secondary; text-align: center; display: block; }
.action-btn[disabled] { background: $uni-border-color !important; color: $uni-text-color-placeholder !important; border: none !important; }
.section { padding: 0 16px; }
.section-header { flex-direction: row; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-title { font-size: 16px; font-weight: 700; color: $uni-text-color-title; }
.section-count { font-size: 13px; color: $uni-text-color-secondary; }
.match-list { flex-direction: column; gap: 12px; }
.match-card { background: $uni-bg-color; border-radius: 12px; padding: 16px; box-shadow: $uni-shadow-card; }
.match-top { flex-direction: row; justify-content: space-between; align-items: flex-start; }
.match-info { flex: 1; }
.match-job-title { font-size: 16px; font-weight: 600; color: $uni-text-color-title; display: block; }
.match-company { font-size: 13px; color: $uni-text-color-secondary; margin-top: 2px; display: block; }
.match-score-box { align-items: center; margin-left: 12px; }
.score-circle { width: 56px; height: 56px; border-radius: 50%; border-width: 3px; border-style: solid; align-items: center; justify-content: center; }
.score-text { font-size: 16px; font-weight: 700; }
.score-label { font-size: 12px; color: $uni-text-color-secondary; margin-top: 2px; }
.match-reason { flex-direction: row; align-items: center; gap: 6px; margin-top: 10px; padding: 8px 12px; background: $uni-bg-color-page; border-radius: 8px; font-size: 13px; color: $uni-text-color; }
.match-improve {
	flex-direction: row;
	align-items: flex-start;
	gap: 4px;
	margin-top: 6px;
	padding: 6px 10px;
	background: rgba(255, 125, 0, 0.06);
	border-radius: 6px;
}
.match-improve-mark { color: $uni-color-warning; font-size: 12px; line-height: 1.5; }
.match-improve text {
	flex: 1;
	font-size: 12px;
	color: $uni-color-warning;
	line-height: 1.5;
}
.match-meta { flex-direction: row; align-items: center; gap: 8px; margin-top: 10px; }
.meta-tag { font-size: 12px; padding: 2px 8px; border-radius: 4px; background: $uni-color-primary-light; color: $uni-color-primary; font-weight: 500; }
.meta-date { font-size: 12px; color: $uni-text-color-placeholder; margin-left: auto; }
.detail-body { padding: 0; }
.detail-job-section { margin-bottom: 16px; }
.detail-job-title { font-size: 17px; font-weight: 700; color: $uni-text-color-title; display: block; margin-bottom: 2px; }
.detail-company { font-size: 13px; color: $uni-text-color-secondary; }
.detail-score-section { align-items: center; margin-bottom: 16px; }
.detail-section-label { font-size: 13px; color: $uni-text-color-secondary; margin-bottom: 8px; align-self: flex-start; }
.detail-verdict { font-size: 13px; font-weight: 600; color: $uni-text-color-secondary; margin-top: 8px; }
.detail-score-ring { width: 80px; height: 80px; border-radius: 50%; border-width: 4px; border-style: solid; align-items: center; justify-content: center; font-size: 24px; font-weight: 800; }
.detail-reason-section { margin-bottom: 16px; }
.detail-reason-text { font-size: 14px; color: $uni-text-color; line-height: 1.6; padding: 10px 14px; background: $uni-bg-color-page; border-radius: 8px; }
.detail-dims-section { margin-bottom: 20px; }
.dim-bar { gap: 10px; }
.dim-row { flex-direction: row; align-items: center; gap: 8px; }
.dim-label { width: 56px; font-size: 12px; color: $uni-text-color; }
.dim-track { flex: 1; height: 8px; background: $uni-border-color-divider; border-radius: 4px; overflow: hidden; }
.dim-fill { height: 100%; border-radius: 4px; transition: width 0.5s; }
.dim-val { width: 36px; font-size: 12px; font-weight: 600; color: $uni-text-color; text-align: right; }
.detail-improve-section { margin-bottom: 20px; }
.improve-item { flex-direction: row; gap: 8px; margin-bottom: 6px; }
.improve-item:last-child { margin-bottom: 0; }
.improve-mark { color: $uni-color-warning; font-size: 13px; line-height: 1.6; }
.improve-mark--ok { color: $uni-color-success; }
.improve-text { flex: 1; font-size: 13px; color: $uni-text-color; line-height: 1.6; }
.detail-action-btn { width: 100%; height: 44px; border-radius: 12px; background: $uni-color-primary; color: $uni-text-color-inverse; font-size: 15px; font-weight: 600; align-items: center; justify-content: center; border: none; margin-top: 16px; flex-shrink: 0; }
.action-btn:active { opacity: 0.85; }
</style>
