<template>
	<view class="page-wrapper">
		<NavBar title="模拟面试" show-back />
		<scroll-view class="content-scrollable" scroll-y>
			<LoadingState v-if="pageLoading" type="skeleton" :rows="4" />

			<!-- 未开始：选择岗位 -->
			<template v-else-if="!currentJob && !finished">
				<view class="practice-banner">
					<view class="banner-icon">
						<uni-icons type="chat" size="32" color="#0EA5E9" />
					</view>
					<view class="banner-text">
						<text class="banner-title">AI 模拟面试</text>
						<text class="banner-desc">从你投递过的岗位中选一个，AI 根据岗位和简历出题，逐题点评</text>
					</view>
				</view>
				<view v-if="deliveries.length" class="job-section">
					<view class="section-header">
						<text class="section-title">选择一个投递过的岗位</text>
						<text class="section-count">{{ deliveries.length }}个</text>
					</view>
					<view class="job-list">
						<view v-for="(item, i) in deliveries" :key="i" class="job-card" @click="startPractice(item)">
							<view class="job-info">
								<text class="job-title">{{ item.jobTitle }}</text>
								<text class="job-company">{{ item.companyName || '未知公司' }}</text>
							</view>
							<uni-icons type="arrowright" size="16" color="#C9CDD4" />
						</view>
					</view>
				</view>
				<EmptyState v-else icon="inbox" title="暂无投递记录" desc="先去投递岗位，再回来练习面试吧" btn-text="去逛逛" @action="goHome" />
			</template>

			<!-- 生成题目中 -->
			<template v-else-if="genLoading">
				<view class="gen-loading">
					<LoadingState type="skeleton" :rows="4" />
					<text class="gen-loading-text">AI 正在根据岗位和你的简历出题，请稍候…</text>
				</view>
			</template>

			<!-- 答题中 -->
			<template v-else-if="questions.length && !finished">
				<view class="practice-job-bar">
					<view class="practice-job-info">
						<text class="practice-job-title">{{ currentJob.jobTitle }}</text>
					<text class="progress-text">第 {{ currentIdx + 1 }} / {{ questions.length }} 题</text>
				</view>
				<view class="q-progress-track">
					<view class="q-progress-fill" :style="{ width: (currentIdx / questions.length * 100) + '%' }" />
				</view>
					<text class="quit-link" @click="quitPractice">退出</text>
				</view>

				<view class="question-card">
					<view class="question-header">
						<text class="q-index">Q{{ currentIdx + 1 }}</text>
						<text class="q-type" :class="'q-type--' + typeClass(currentQuestion.type)">{{ currentQuestion.type || '面试题' }}</text>
					</view>
					<text class="q-text">{{ currentQuestion.question }}</text>

					<view class="answer-area">
						<text class="answer-label">你的回答</text>
						<textarea
							class="answer-textarea"
							v-model="answer"
							placeholder="像真实面试一样作答，建议 100 字以上…"
							:maxlength="1000"
							:disabled="!!result"
						/>
					</view>

					<!-- 点评结果 -->
					<view v-if="result" class="result-card">
						<view class="result-score-row">
							<text class="result-label">本题得分</text>
							<text class="result-score" :style="{ color: scoreColor(result.score) }">
								{{ result.score != null ? result.score + '分' : '--' }}
							</text>
						</view>
						<view class="result-block">
							<text class="result-subtitle">点评</text>
							<text class="result-text">{{ result.comment || '暂无' }}</text>
						</view>
						<view class="result-block" v-if="result.betterAnswer">
							<text class="result-subtitle">参考答案</text>
							<text class="result-text result-text--ref">{{ result.betterAnswer }}</text>
						</view>
					</view>

					<button v-if="!result" class="submit-btn" :disabled="evaluating || !answer.trim()" @click="submitAnswer">
						{{ evaluating ? 'AI 点评中...' : '提交点评' }}
					</button>
					<button v-else class="submit-btn" @click="nextQuestion">
						{{ currentIdx + 1 < questions.length ? '下一题' : '查看练习小结' }}
					</button>
				</view>
			</template>

			<!-- 练习小结 -->
			<template v-else-if="finished">
				<view class="summary-card">
					<view class="summary-icon">
						<uni-icons type="checkbox-filled" size="48" color="#00B42A" />
					</view>
					<text class="summary-title">本轮练习完成</text>
					<text class="summary-desc">{{ currentJob ? '「' + currentJob.jobTitle + '」· 共 ' + questions.length + ' 题' : '' }}</text>
					<view class="summary-score-row" v-if="avgScore != null">
						<text class="summary-score" :style="{ color: scoreColor(avgScore) }">{{ avgScore }}分</text>
						<text class="summary-score-label">平均得分</text>
					</view>
					<!-- 能力条：按题型平均分（技术/项目/行为） -->
					<view class="ability-bars" v-if="abilityBars.length">
						<view v-for="(b, i) in abilityBars" :key="i" class="ability-row">
							<text class="ability-label">{{ b.label }}</text>
							<view class="ability-track">
								<view class="ability-fill" :style="{ width: b.value + '%', background: scoreColor(b.value) }" />
							</view>
							<text class="ability-val">{{ b.value }}</text>
						</view>
					</view>
					<view class="advice-card">
						<text class="advice-title">提升建议</text>
						<view class="advice-row">
							<view class="advice-dot" />
							<text class="advice-text">回顾每题的参考答案，把没答好的知识点补进简历和项目里</text>
						</view>
					</view>
					<view class="summary-btn-row">
						<button class="summary-btn summary-btn--ghost" @click="practiceRecord">查看练习记录</button>
						<button class="summary-btn" @click="restartPractice">再练一次</button>
					</view>
				</view>
			</template>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { deliveryAPI, aiAssistantAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'

const pageLoading = ref(true)
const deliveries = ref([])
const currentJob = ref(null)
const genLoading = ref(false)
const questions = ref([])
const currentIdx = ref(0)
const answer = ref('')
const evaluating = ref(false)
const results = ref([])
const finished = ref(false)

const currentQuestion = computed(() => questions.value[currentIdx.value] || {})
const result = computed(() => results.value[currentIdx.value] || null)
const avgScore = computed(() => {
	const scores = results.value.filter(r => r && r.score != null).map(r => Number(r.score))
	if (!scores.length) return null
	return Math.round(scores.reduce((a, b) => a + b, 0) / scores.length)
})

// 能力条：按题型（技术/项目/行为）计算已答题平均分
const abilityBars = computed(() => {
	const defs = [['技术', '技术能力'], ['项目', '项目能力'], ['行为', '行为表达']]
	return defs.map(([type, label]) => {
		const scores = questions.value
			.map((q, i) => (q.type === type && results.value[i] && results.value[i].score != null)
				? Number(results.value[i].score) : null)
			.filter(s => s != null)
		if (!scores.length) return null
		return { label, value: Math.round(scores.reduce((a, b) => a + b, 0) / scores.length) }
	}).filter(Boolean)
})

const getStudentId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.id || obj.userId ? Number(obj.id || obj.userId) : null
	} catch (e) { return null }
}

onLoad(() => { loadDeliveries() })

// 拉投递记录作为可选岗位（同一岗位多次投递只保留一个）
const loadDeliveries = async () => {
	const studentId = getStudentId()
	if (!studentId) { pageLoading.value = false; return }
	try {
		const res = await deliveryAPI.getDeliveriesByStudentId({ studentId })
		const seen = new Set()
		deliveries.value = (res.data || []).filter(d => {
			if (!d.jobId || seen.has(d.jobId)) return false
			seen.add(d.jobId)
			return true
		}).map(d => ({
			jobId: d.jobId,
			jobTitle: d.jobTitle || '岗位#' + d.jobId,
			companyName: d.companyName || ''
		}))
	} catch (e) {
		console.error('加载投递记录失败', e)
		uni.showToast({ title: '加载投递记录失败', icon: 'none' })
	} finally {
		pageLoading.value = false
	}
}

// 开始练习：选定岗位 → AI 生成题目
const startPractice = async (item) => {
	if (genLoading.value) return
	currentJob.value = item
	genLoading.value = true
	try {
		const res = await aiAssistantAPI.genInterviewQuestions(item.jobId)
		const qs = (res.data && res.data.questions) || []
		if (!Array.isArray(qs) || !qs.length) {
			uni.showToast({ title: '题目生成失败，请重试', icon: 'none' })
			currentJob.value = null
			return
		}
		questions.value = qs
		results.value = qs.map(() => null)
		currentIdx.value = 0
		answer.value = ''
	} catch (e) {
		currentJob.value = null
		uni.showToast({ title: (e && e.message) || '题目生成失败', icon: 'none' })
	} finally {
		genLoading.value = false
	}
}

// 提交当前题的作答，获取 AI 点评
const submitAnswer = async () => {
	if (evaluating.value || !answer.value.trim()) return
	evaluating.value = true
	uni.showLoading({ title: 'AI 点评中...', mask: true })
	try {
		const res = await aiAssistantAPI.evaluateAnswer(
			currentJob.value.jobId, currentQuestion.value.question || '', answer.value)
		results.value[currentIdx.value] = res.data || {}
	} catch (e) {
		uni.showToast({ title: (e && e.message) || '点评失败，请重试', icon: 'none' })
	} finally {
		evaluating.value = false
		uni.hideLoading()
	}
}

const nextQuestion = () => {
	if (currentIdx.value + 1 < questions.value.length) {
		currentIdx.value++
		answer.value = ''
	} else {
		finished.value = true
	}
}

const quitPractice = () => {
	uni.showModal({
		title: '退出练习',
		content: '退出后本轮作答不会保存，确定退出吗？',
		success: (res) => { if (res.confirm) restartPractice() }
	})
}

const restartPractice = () => {
	finished.value = false
	currentJob.value = null
	questions.value = []
	results.value = []
	currentIdx.value = 0
	answer.value = ''
}

const goHome = () => uni.reLaunch({ url: '/pages/student/home' })
const practiceRecord = () => uni.showToast({ title: '练习记录功能即将上线', icon: 'none' })

// 题目类型 → 样式类
const typeClass = (type) => {
	if (type === '项目') return 'proj'
	if (type === '行为') return 'beh'
	return 'tech'
}

// 0-100 分配色（与全站阈值一致：优秀绿/合格蓝/待提升橙红）
const scoreColor = (score) => {
	if (score == null) return '#86909C'
	const num = Number(score)
	if (num >= 80) return '#00B42A'
	if (num >= 60) return '#165DFF'
	if (num >= 40) return '#FF7D00'
	return '#F53F3F'
}
</script>

<style scoped lang="scss">
.practice-banner {
	flex-direction: row;
	align-items: center;
	margin: 16px;
	padding: 20px;
	background: $uni-color-ai-light;
	border-radius: 12px;
	gap: 16px;
}
.banner-icon {
	width: 48px;
	height: 48px;
	border-radius: 12px;
	background: $uni-bg-color;
	align-items: center;
	justify-content: center;
}
.banner-text { flex: 1; }
.banner-title { font-size: 18px; font-weight: 700; color: $uni-text-color-title; display: block; margin-bottom: 4px; }
.banner-desc { font-size: 13px; color: $uni-text-color; line-height: 1.5; }

.job-section { padding: 0 16px; }
.section-header { flex-direction: row; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-title { font-size: 16px; font-weight: 700; color: $uni-text-color-title; }
.section-count { font-size: 13px; color: $uni-text-color-secondary; }
.job-list { gap: 10px; }
.job-card {
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
	background: $uni-bg-color;
	border-radius: 12px;
	padding: 16px;
	box-shadow: $uni-shadow-card;
}
.job-info { flex: 1; }
.job-title { font-size: 16px; font-weight: 600; color: $uni-text-color-title; display: block; }
.job-company { font-size: 13px; color: $uni-text-color-secondary; margin-top: 2px; display: block; }
.job-card:active { opacity: 0.85; }

.gen-loading { padding: 24px 16px; }
.gen-loading-text { font-size: 13px; color: $uni-text-color-secondary; text-align: center; margin-top: 12px; display: block; }

.practice-job-bar {
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
	margin: 16px 16px 12px;
	padding: 12px 16px;
	background: $uni-bg-color;
	border-radius: 12px;
	box-shadow: $uni-shadow-card;
}
.practice-job-info { flex: 1; }
.practice-job-title { font-size: 15px; font-weight: 600; color: $uni-text-color-title; display: block; }
.progress-text { font-size: 12px; color: $uni-text-color-secondary; margin-top: 2px; display: block; }
.q-progress-track {
	height: 4px;
	background: $uni-border-color-divider;
	border-radius: 2px;
	overflow: hidden;
	margin-top: 10px;
}
.q-progress-fill {
	height: 100%;
	background: $uni-color-ai;
	border-radius: 2px;
	transition: width 0.3s;
}
.quit-link { font-size: 13px; color: $uni-color-error; padding: 4px 0 4px 12px; }

.question-card {
	background: $uni-bg-color;
	border-radius: 12px;
	padding: 20px 16px;
	margin: 0 16px;
	box-shadow: $uni-shadow-card;
}
.question-header { flex-direction: row; align-items: center; gap: 8px; margin-bottom: 10px; }
.q-index {
	font-size: 14px;
	font-weight: 700;
	color: $uni-color-ai;
	background: $uni-color-ai-light;
	padding: 2px 10px;
	border-radius: 8px;
}
.q-type { font-size: 12px; padding: 2px 8px; border-radius: 4px; font-weight: 500; }
.q-type--tech { color: $uni-color-primary; background: $uni-color-primary-light; }
.q-type--proj { color: $uni-color-ai; background: $uni-color-ai-light; }
.q-type--beh { color: $uni-color-success; background: $uni-color-success-light; }
.q-text { font-size: 16px; font-weight: 600; color: $uni-text-color-title; line-height: 1.6; }

.answer-area { margin-top: 16px; }
.answer-label { font-size: 13px; font-weight: 600; color: $uni-text-color; margin-bottom: 8px; display: block; }
.answer-textarea {
	width: 100%;
	box-sizing: border-box;
	min-height: 140px;
	background: $uni-bg-color-page;
	border-radius: 8px;
	padding: 12px;
	font-size: 14px;
	line-height: 1.6;
	color: $uni-text-color-title;
}

.result-card {
	margin-top: 16px;
	background: $uni-bg-color-hover;
	border: 0.5px solid $uni-border-color-divider;
	border-radius: 12px;
	padding: 14px;
}
.result-score-row { flex-direction: row; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.result-label { font-size: 13px; color: $uni-text-color-secondary; }
.result-score { font-size: 20px; font-weight: 800; }
.result-block { margin-top: 10px; }
.result-subtitle { font-size: 13px; font-weight: 600; color: $uni-text-color; margin-bottom: 4px; display: block; }
.result-text { font-size: 13px; color: $uni-text-color; line-height: 1.6; }
.result-text--ref {
	color: $uni-color-primary;
	background: $uni-color-primary-light;
	padding: 8px 10px;
	border-radius: 8px;
}

.submit-btn {
	margin-top: 20px;
	width: 100%;
	height: 46px;
	border-radius: 12px;
	background: $uni-color-ai;
	color: $uni-text-color-inverse;
	font-size: 15px;
	font-weight: 600;
	align-items: center;
	justify-content: center;
	border: none;
}
.submit-btn[disabled] { background: $uni-border-color; color: $uni-text-color-placeholder; }
.submit-btn:active { opacity: 0.85; }

.summary-card {
	background: $uni-bg-color;
	border-radius: 12px;
	margin: 16px;
	padding: 32px 20px;
	align-items: center;
	box-shadow: $uni-shadow-card;
}
.summary-icon { margin-bottom: 12px; }
.summary-title { font-size: 20px; font-weight: 700; color: $uni-text-color-title; }
.summary-desc { font-size: 13px; color: $uni-text-color-secondary; margin-top: 6px; }
.summary-score-row { align-items: center; margin-top: 20px; }
.summary-score { font-size: 40px; font-weight: 800; animation: scoreUp 0.4s ease both; }
@keyframes scoreUp {
	from { opacity: 0; transform: translateY(8px); }
	to { opacity: 1; transform: translateY(0); }
}
.summary-btn-row {
	align-self: stretch;
	flex-direction: row;
	gap: 12px;
	margin-top: 24px;
}
.summary-btn {
	flex: 1;
	height: 44px;
	border-radius: 12px;
	background: $uni-color-ai;
	color: #FFFFFF;
	font-size: 14px;
	font-weight: 600;
	align-items: center;
	justify-content: center;
	border: none;
}
.summary-btn:active { transform: scale(0.97); }
.summary-btn--ghost {
	background: $uni-bg-color;
	border: 1px solid $uni-border-color;
	color: $uni-text-color;
}
.summary-score-label { font-size: 12px; color: $uni-text-color-secondary; margin-top: 2px; }
.ability-bars {
	align-self: stretch;
	margin: 20px 0 4px;
	gap: 10px;
}
.ability-row {
	flex-direction: row;
	align-items: center;
	gap: 10px;
}
.ability-label {
	width: 64px;
	font-size: 12px;
	color: $uni-text-color;
	text-align: right;
	flex-shrink: 0;
}
.ability-track {
	flex: 1;
	height: 8px;
	background: $uni-border-color-divider;
	border-radius: 4px;
	overflow: hidden;
}
.ability-fill {
	height: 100%;
	border-radius: 4px;
	transition: width 0.5s;
}
.ability-val {
	width: 32px;
	font-size: 12px;
	font-weight: 700;
	color: $uni-text-color;
	text-align: right;
}
.advice-card {
	align-self: stretch;
	background: #F7F8FA;
	border-radius: 10px;
	padding: 14px;
	margin: 20px 0 8px;
}
.advice-title { font-size: 13px; font-weight: 600; color: $uni-text-color-title; margin-bottom: 8px; display: block; }
.advice-row { flex-direction: row; align-items: flex-start; gap: 8px; }
.advice-dot {
	width: 6px;
	height: 6px;
	border-radius: 50%;
	background: $uni-color-ai;
	margin-top: 6px;
	flex-shrink: 0;
}
.advice-text { flex: 1; font-size: 13px; color: $uni-text-color; line-height: 1.6; }
.summary-tips { margin: 16px 0 8px; }
.summary-tip {
	font-size: 12px;
	color: $uni-text-color;
	background: $uni-bg-color-page;
	padding: 8px 12px;
	border-radius: 8px;
	line-height: 1.6;
}
</style>
