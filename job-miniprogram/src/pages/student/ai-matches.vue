<template>
	<view class="page-wrapper">
		<!-- 顶部导航 -->
		<view class="header-bar">
			<view class="header-left" @click="goBack">
				<text class="back-arrow">←</text>
			</view>
			<text class="header-title">AI 岗位匹配</text>
			<view class="header-right" @click="loadMatches">
				<text class="refresh-btn">⟳</text>
			</view>
		</view>

		<scroll-view class="content-scrollable" scroll-y>
			<!-- 匹配概览 -->
			<view class="match-banner">
				<view class="banner-icon">🤖</view>
				<view class="banner-text">
					<text class="banner-title">AI 智能匹配</text>
					<text class="banner-desc">基于你的简历和技能，智能推荐最合适的岗位</text>
				</view>
			</view>

			<!-- 操作按钮 -->
			<view class="action-bar">
				<button class="action-btn primary" :loading="loading" @click="generateAllMatch">
					{{ loading ? '匹配中...' : '🔄 刷新匹配结果' }}
				</button>
			</view>

			<!-- 匹配结果 -->
			<view v-if="matches.length > 0" class="section">
				<view class="section-header">
					<text class="section-title">推荐岗位（按匹配度排序）</text>
					<text class="section-count">{{ matches.length }}个</text>
				</view>

				<view class="match-list">
					<view v-for="(item, i) in matches" :key="i" class="match-card" @click="goToJob(item)">
						<view class="match-top">
							<view class="match-info">
								<text class="match-job-title">{{ item.jobTitle || '岗位#' + item.jobId }}</text>
								<text class="match-company">{{ item.companyName || '' }}</text>
							</view>
							<view class="match-score-box">
								<view class="score-circle" :style="{ borderColor: scoreColor(item.matchScore) }">
									<text class="score-text" :style="{ color: scoreColor(item.matchScore) }">{{ formatScore(item.matchScore) }}</text>
								</view>
								<text class="score-label">匹配度</text>
							</view>
						</view>
						<view class="match-reason" v-if="item.matchReason" @click.stop="showMatchDetail(item)">
							<text>📌 {{ item.matchReason }}</text>
							<text class="reason-more">详情 ›</text>
						</view>
						<view class="match-meta">
							<text class="meta-tag" v-if="item.isPushed == 1">📤 已推送</text>
							<text class="meta-tag" v-if="item.isClicked == 1">👁️ 已查看</text>
							<text class="meta-date">{{ formatTime(item.createTime) }}</text>
						</view>
					</view>
				</view>
			</view>

			<!-- 空状态 -->
			<view v-else-if="!loading" class="empty-state">
				<text class="empty-icon">🤖</text>
				<text class="empty-title">暂无匹配推荐</text>
				<text class="empty-desc">点击上方按钮，AI将根据你的简历自动匹配岗位</text>
			</view>
		</scroll-view>

		<!-- 匹配详情弹窗 -->
		<view class="modal-overlay" v-if="showDetail" @click="closeDetail">
			<view class="modal-content" @click.stop>
				<view class="modal-header">
					<text class="modal-title">匹配详情</text>
					<text class="modal-close" @click="closeDetail">✕</text>
				</view>
				<view class="modal-body" v-if="detailItem">
					<view class="detail-job-section">
						<text class="detail-job-title">{{ detailItem.jobTitle }}</text>
						<text class="detail-company">{{ detailItem.companyName }}</text>
					</view>

					<view class="detail-score-section">
						<text class="detail-section-label">匹配评分</text>
						<view class="detail-score-ring" :style="{ borderColor: scoreColor(detailItem.matchScore) }">
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
								<view class="dim-track">
									<view class="dim-fill" :style="{ width: dimPercent('skill'), background: dimColor('skill') }"></view>
								</view>
								<text class="dim-val">{{ formatDim(detailItem.skillScore) }}</text>
							</view>
							<view class="dim-row">
								<text class="dim-label">经验匹配</text>
								<view class="dim-track">
									<view class="dim-fill" :style="{ width: dimPercent('exp'), background: dimColor('exp') }"></view>
								</view>
								<text class="dim-val">{{ formatDim(detailItem.expScore) }}</text>
							</view>
							<view class="dim-row">
								<text class="dim-label">学历匹配</text>
								<view class="dim-track">
									<view class="dim-fill" :style="{ width: dimPercent('edu'), background: dimColor('edu') }"></view>
								</view>
								<text class="dim-val">{{ formatDim(detailItem.eduScore) }}</text>
							</view>
						</view>
					</view>

					<button class="detail-action-btn" @click="goToJob(detailItem)">查看岗位详情</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { matchAPI, jobAPI } from '@/utils/request'
import { getCurrentUser } from '@/utils/auth'

const matches = ref([])
const loading = ref(false)

const getStudentId = () => {
  const user = getCurrentUser()
  return user ? user.id : null
}

const loadMatches = async () => {
  const studentId = getStudentId()
  if (!studentId) return
  loading.value = true
  try {
    const res = await matchAPI.getByStudent(studentId)
    const raw = res.data || []
    // 异步加载岗位详情补充标题/公司名
    const enriched = await Promise.all(raw.map(async (m) => {
      try {
        const jRes = await jobAPI.getJobDetail(m.jobId)
        const j = jRes.data || {}
        return { ...m, jobTitle: j.title || j.jobTitle || m.jobTitle, companyName: j.companyName || m.companyName }
      } catch (e) {
        return { ...m, jobTitle: m.jobTitle || ('岗位#' + m.jobId) }
      }
    }))
    matches.value = enriched.sort((a, b) => (b.matchScore || 0) - (a.matchScore || 0))
  } catch (e) {
    console.error('加载匹配结果失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const generateAllMatch = async () => {
  loading.value = true
  uni.showLoading({ title: 'AI匹配中...' })
  try {
    const studentId = getStudentId()
    if (!studentId) throw new Error('未登录')
    // 获取活跃岗位列表，逐一生成匹配
    const jRes = await jobAPI.getActiveJobs()
    const jobs = jRes.data || []
    let count = 0
    for (const job of jobs) {
      try {
        await matchAPI.generate(job.id, studentId)
        count++
      } catch (e) { /* skip if already matched */ }
    }
    uni.hideLoading()
    uni.showToast({ title: '匹配完成 ' + count + '/' + jobs.length + ' 个', icon: 'success' })
    await loadMatches()
  } catch (e) {
    uni.hideLoading()
    console.error('匹配失败', e)
    uni.showToast({ title: e.errMsg || '匹配失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const formatScore = (score) => {
  if (score == null) return '--'
  const num = typeof score === 'string' ? parseFloat(score) : score
  return Math.round(num * 100) + '%'
}

const scoreColor = (score) => {
  if (score == null) return '#86909C'
  const num = typeof score === 'string' ? parseFloat(score) : score
  if (num >= 0.8) return '#10B981'
  if (num >= 0.6) return '#165DFF'
  if (num >= 0.4) return '#F59E0B'
  return '#F56C6C'
}

const formatTime = (t) => {
  if (!t) return ''
  return t.substring(0, 10)
}

// 匹配详情弹窗
const showDetail = ref(false)
const detailItem = ref(null)

const showMatchDetail = (item) => {
  detailItem.value = item
  showDetail.value = true
}

const closeDetail = () => {
  showDetail.value = false
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
  const item = detailItem.value
  if (!item) return '#86909C'
  const key = dim === 'skill' ? 'skillScore' : dim === 'exp' ? 'expScore' : 'eduScore'
  const val = item[key]
  if (val == null) return '#86909C'
  const num = typeof val === 'string' ? parseFloat(val) : val
  if (num >= 0.8) return '#10B981'
  if (num >= 0.6) return '#165DFF'
  if (num >= 0.4) return '#F59E0B'
  return '#F56C6C'
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

const goBack = () => uni.navigateBack()
</script>

<style scoped>
.header-bar {
	display: flex;
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
	padding: 12px 16px;
	background: #fff;
	border-bottom: 1px solid #F0F2F5;
	position: sticky;
	top: 0;
	z-index: 10;
}
.header-left, .header-right { width: 40px; }
.header-title { font-size: 17px; font-weight: 700; color: #1D2129; }
.back-arrow { font-size: 22px; color: #165DFF; }
.refresh-btn { font-size: 22px; color: #165DFF; text-align: right; }

.match-banner {
	display: flex;
	flex-direction: row;
	align-items: center;
	margin: 16px;
	padding: 20px;
	background: linear-gradient(135deg, #EEF2FF, #E0E7FF);
	border-radius: 16px;
	gap: 16px;
}
.banner-icon { font-size: 48px; }
.banner-text { flex: 1; }
.banner-title { font-size: 18px; font-weight: 700; color: #1D2129; display: block; margin-bottom: 4px; }
.banner-desc { font-size: 13px; color: #4E5969; }

.action-bar { padding: 0 16px 12px; }
.action-btn {
	width: 100%;
	height: 44px;
	border-radius: 12px;
	font-size: 15px;
	font-weight: 600;
	align-items: center;
	justify-content: center;
	border: none;
}
.action-btn.primary { background: #165DFF; color: #fff; }

.section { padding: 0 16px; }
.section-header {
	display: flex;
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 12px;
}
.section-title { font-size: 16px; font-weight: 700; color: #1D2129; }
.section-count { font-size: 13px; color: #86909C; }

.match-list { display: flex; flex-direction: column; gap: 12px; }
.match-card {
	background: #fff;
	border-radius: 14px;
	padding: 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.match-top {
	display: flex;
	flex-direction: row;
	justify-content: space-between;
	align-items: flex-start;
}
.match-info { flex: 1; }
.match-job-title { font-size: 16px; font-weight: 600; color: #1D2129; display: block; }
.match-company { font-size: 13px; color: #86909C; margin-top: 2px; display: block; }
.match-score-box { align-items: center; margin-left: 12px; }
.score-circle {
	width: 56px;
	height: 56px;
	border-radius: 50%;
	border: 3px solid #10B981;
	align-items: center;
	justify-content: center;
	background: #F6FFED;
}
.score-text { font-size: 16px; font-weight: 700; }
.score-label { font-size: 11px; color: #86909C; margin-top: 2px; }

.match-reason {
	margin-top: 10px;
	padding: 8px 12px;
	background: #F8F9FC;
	border-radius: 8px;
	font-size: 13px;
	color: #4E5969;
}

.match-meta {
	display: flex;
	flex-direction: row;
	align-items: center;
	gap: 8px;
	margin-top: 10px;
}
.meta-tag {
	font-size: 11px;
	padding: 2px 8px;
	border-radius: 4px;
	background: #F0F5FF;
	color: #165DFF;
}
.meta-date { font-size: 11px; color: #C9CDD4; margin-left: auto; }

/* 匹配详情弹窗 */
.modal-overlay {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(0,0,0,0.5);
	z-index: 999;
	align-items: center;
	justify-content: center;
	padding: 40px 20px;
}
.modal-content {
	width: 100%;
	max-width: 360px;
	background: #fff;
	border-radius: 20px;
	overflow: hidden;
}
.modal-header {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	padding: 20px 20px 0;
}
.modal-title { font-size: 18px; font-weight: 700; color: #1D2129; }
.modal-close { font-size: 20px; color: #86909C; padding: 4px; }
.modal-body { padding: 16px 20px 20px; }

.detail-job-section { margin-bottom: 16px; }
.detail-job-title { font-size: 17px; font-weight: 700; color: #1D2129; display: block; margin-bottom: 2px; }
.detail-company { font-size: 13px; color: #86909C; }

.detail-score-section {
	align-items: center;
	margin-bottom: 16px;
}
.detail-section-label {
	font-size: 13px;
	color: #86909C;
	margin-bottom: 8px;
	align-self: flex-start;
}
.detail-score-ring {
	width: 80px;
	height: 80px;
	border-radius: 50%;
	border: 4px solid #10B981;
	align-items: center;
	justify-content: center;
	background: #F6FFED;
	font-size: 24px;
	font-weight: 800;
}

.detail-reason-section { margin-bottom: 16px; }
.detail-reason-text {
	font-size: 14px;
	color: #4E5969;
	line-height: 1.6;
	padding: 10px 14px;
	background: #F8F9FC;
	border-radius: 8px;
}

.detail-dims-section { margin-bottom: 20px; }
.dim-bar { gap: 10px; }
.dim-row {
	flex-direction: row;
	align-items: center;
	gap: 8px;
}
.dim-label {
	width: 56px;
	font-size: 12px;
	color: #4E5969;
}
.dim-track {
	flex: 1;
	height: 8px;
	background: #F2F3F5;
	border-radius: 4px;
	overflow: hidden;
}
.dim-fill {
	height: 100%;
	border-radius: 4px;
	transition: width 0.5s;
}
.dim-val {
	width: 36px;
	font-size: 12px;
	font-weight: 600;
	color: #4E5969;
	text-align: right;
}

.reason-more {
	font-size: 12px;
	color: #165DFF;
	margin-left: auto;
}

.detail-action-btn {
	width: 100%;
	height: 44px;
	border-radius: 12px;
	background: #165DFF;
	color: #fff;
	font-size: 15px;
	font-weight: 600;
	align-items: center;
	justify-content: center;
	border: none;
}

.empty-state {
	align-items: center;
	padding: 60px 20px;
}
.empty-icon { font-size: 64px; margin-bottom: 16px; }
.empty-title { font-size: 17px; font-weight: 600; color: #1D2129; margin-bottom: 8px; }
.empty-desc { font-size: 14px; color: #86909C; text-align: center; line-height: 1.5; }
</style>
