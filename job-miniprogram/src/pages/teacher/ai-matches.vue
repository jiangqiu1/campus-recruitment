<template>
	<view class="page-wrapper" style="background:#F5F7FA;min-height:100vh;">
		<view class="header-bar">
			<view class="header-left" @click="goBack"><text class="back-arrow">←</text></view>
			<text class="header-title">AI 人岗匹配</text>
			<view class="header-right"><text class="refresh-btn" @click="loadData">⟳</text></view>
		</view>

		<scroll-view class="content-scrollable" scroll-y>
			<!-- 岗位选择 -->
			<view class="job-select-bar">
				<text class="select-label">选择岗位</text>
				<text v-if="jobList.length === 0" style="flex:1;font-size:13px;color:#86909C;">加载中...</text>
				<picker v-else @change="onJobChange" :value="jobIndex" :range="jobList" range-key="title" class="job-picker">
					<view class="job-picker-btn">
						<text>{{ selectedJob ? selectedJob.title : '请选择岗位' }}</text>
						<text class="picker-arrow">▼</text>
					</view>
				</picker>
				<button class="batch-btn" :loading="batchLoading" @click="batchMatch" :disabled="!selectedJob">
					{{ batchLoading ? '匹配中...' : '🔄 批量匹配' }}
				</button>
			</view>

			<!-- 加载中状态 -->
			<view v-if="dataLoading" class="loading-hint"><text>正在加载匹配数据...</text></view>

			<!-- 统计数据 -->
			<view v-if="stats" class="stats-row">
				<view class="stat-card">
					<text class="stat-num">{{ formatScore(stats.avgScore) }}</text>
					<text class="stat-label">平均匹配度</text>
				</view>
				<view class="stat-card">
					<text class="stat-num">{{ formatPercent(stats.pushRate) }}</text>
					<text class="stat-label">推送率</text>
				</view>
				<view class="stat-card">
					<text class="stat-num">{{ formatPercent(stats.clickRate) }}</text>
					<text class="stat-label">点击率</text>
				</view>
				<view class="stat-card">
					<text class="stat-num">{{ matches.length }}</text>
					<text class="stat-label">总数</text>
				</view>
			</view>

			<!-- 匹配列表 -->
			<view v-if="matches.length > 0" class="section">
				<view class="section-header">
					<text class="section-title">匹配学生列表</text>
					<text class="section-count">按匹配度排序</text>
				</view>
				<view class="match-list">
					<view v-for="(item, i) in matches" :key="i" class="match-card">
						<view class="match-top">
							<view class="match-info">
								<text class="student-name">{{ item.studentName || ('学生#' + item.studentId) }}</text>
								<text class="student-detail">{{ item.studentInfo || '' }}</text>
							</view>
							<view class="match-score-box">
								<view class="score-badge" :style="{ background: scoreBg(item.matchScore) }">
									<text class="score-text">{{ formatScore(item.matchScore) }}</text>
								</view>
							</view>
						</view>
						<view class="match-reason" v-if="item.matchReason">
							<text>📌 {{ item.matchReason }}</text>
						</view>
						<view class="match-actions">
							<text class="action-tag" :class="{ active: item.isPushed == 1 }" @click="togglePush(item)">
								{{ item.isPushed == 1 ? '📤 已推送' : '📤 推送' }}
							</text>
							<text class="action-tag view-tag" @click="viewStudent(item)">
								👤 详情
							</text>
						</view>
					</view>
				</view>
			</view>

			<view v-if="!dataLoading && selectedJob && matches.length === 0" class="empty-state">
				<text class="empty-icon">🤖</text>
				<text class="empty-title">暂无匹配数据</text>
				<text class="empty-desc">选择一个岗位，点击"批量匹配"生成AI人岗匹配推荐</text>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { matchAPI, teacherAPI } from '@/utils/request'
import { getCurrentUser } from '@/utils/auth'

const jobList = ref([])
const jobIndex = ref(-1)
const selectedJob = ref(null)
const matches = ref([])
const stats = ref(null)
const batchLoading = ref(false)
const dataLoading = ref(false)

onMounted(async () => {
  // 先加载岗位列表，再处理外部传入的jobId
  try {
    const user = getCurrentUser()
    const res = await teacherAPI.getTeacherJobs(user ? user.id : '')
    jobList.value = (res.data || []).filter(j => j.status === 1 || j.status === '1')
  } catch (e) {
    console.error('加载岗位列表失败', e)
  }

  // 支持从外部传入jobId（在岗位列表中自动选中）
  const cp = getCurrentPages()
  const page = cp[cp.length - 1]
  if (page && page.$page && page.$page.options && page.$page.options.jobId) {
    const jid = parseInt(page.$page.options.jobId)
    const idx = jobList.value.findIndex(j => j.id == jid)
    if (idx >= 0) {
      jobIndex.value = idx
      selectedJob.value = jobList.value[idx]
      await loadData()
    }
  }
})

const onJobChange = async (e) => {
  jobIndex.value = e.detail.value
  selectedJob.value = jobList.value[jobIndex.value]
  await loadData()
}

const loadData = async () => {
  if (!selectedJob.value) return
  dataLoading.value = true
  const jobId = selectedJob.value.id
  try {
    const [mRes, avgRes, pushRes, clickRes] = await Promise.all([
      matchAPI.getByJob(jobId).catch(() => ({ data: [] })),
      matchAPI.getAvgScore(jobId).catch(() => ({ data: { averageMatchScore: null } })),
      matchAPI.getPushRate(jobId).catch(() => ({ data: { pushRate: null } })),
      matchAPI.getClickRate(jobId).catch(() => ({ data: { clickRate: null } }))
    ])
    matches.value = (mRes.data || []).sort((a, b) => (b.matchScore || 0) - (a.matchScore || 0))
    stats.value = {
      avgScore: avgRes.data?.averageMatchScore,
      pushRate: pushRes.data?.pushRate,
      clickRate: clickRes.data?.clickRate
    }
  } catch (e) {
    console.error('加载匹配数据失败', e)
    uni.showToast({ title: '加载匹配数据失败', icon: 'none' })
  } finally {
    dataLoading.value = false
  }
}

const batchMatch = async () => {
  if (!selectedJob.value) return
  batchLoading.value = true
  uni.showLoading({ title: '批量匹配中...' })
  try {
    const res = await matchAPI.batchGenerate(selectedJob.value.id)
    const count = res.data?.generatedCount || res.data?.data?.generatedCount || 0
    uni.hideLoading()
    uni.showToast({ title: '匹配完成，共 ' + count + ' 人', icon: 'success' })
    await loadData()
  } catch (e) {
    uni.hideLoading()
    console.error('批量匹配失败', e)
    uni.showToast({ title: '匹配失败', icon: 'none' })
  } finally {
    batchLoading.value = false
  }
}

const togglePush = async (item) => {
  try {
    await matchAPI.push(item.id)
    item.isPushed = 1
    uni.showToast({ title: '推送成功', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: '推送失败', icon: 'none' })
  }
}

const viewStudent = (item) => {
  const name = item.studentName ? encodeURIComponent(item.studentName) : ''
  uni.navigateTo({ url: '/pages/teacher/student-resume?studentId=' + item.studentId + '&name=' + name })
}

const formatScore = (s) => s != null ? Math.round(Number(s) * 100) + '%' : '--'
const formatPercent = (s) => s != null ? Math.round(Number(s) * 100) + '%' : '--'
const scoreBg = (s) => {
  if (s == null) return '#F0F0F0'
  const n = typeof s === 'string' ? parseFloat(s) : s
  if (n >= 0.8) return '#10B981'
  if (n >= 0.6) return '#165DFF'
  if (n >= 0.4) return '#F59E0B'
  return '#F56C6C'
}
const goBack = () => {
  const cp = getCurrentPages()
  if (cp.length > 1) uni.navigateBack()
  else uni.reLaunch({ url: '/pages/teacher/home' })
}
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
	top: 0; z-index: 10;
}
.header-left, .header-right { width: 40px; }
.header-title { font-size: 17px; font-weight: 700; color: #1D2129; }
.back-arrow { font-size: 22px; color: #10B981; }
.refresh-btn { font-size: 22px; color: #10B981; text-align: right; }

.job-select-bar {
	display: flex;
	flex-direction: row;
	align-items: center;
	padding: 16px;
	gap: 10px;
	background: #fff;
	margin: 12px 16px;
	border-radius: 14px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.select-label { font-size: 14px; font-weight: 600; color: #1D2129; white-space: nowrap; }
.job-picker { flex: 1; }
.job-picker-btn {
	display: flex;
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
	padding: 10px 14px;
	background: #F5F7FA;
	border-radius: 10px;
	font-size: 14px;
	color: #1D2129;
}
.picker-arrow { font-size: 10px; color: #86909C; margin-left: 8px; }
.batch-btn {
	background: #10B981;
	color: #fff;
	border: none;
	border-radius: 10px;
	padding: 0 16px;
	height: 40px;
	font-size: 13px;
	font-weight: 600;
	white-space: nowrap;
	align-items: center;
	justify-content: center;
}

.content-scrollable { flex:1; }
.loading-hint { text-align: center; padding: 40px 16px; font-size: 14px; color: #86909C; }
.stats-row {
	display: flex;
	flex-direction: row;
	padding: 0 16px;
	gap: 10px;
	margin-bottom: 12px;
}
.stat-card {
	flex: 1;
	background: #fff;
	border-radius: 12px;
	padding: 14px 8px;
	align-items: center;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.stat-num { font-size: 18px; font-weight: 700; color: #10B981; text-overflow: ellipsis; overflow: hidden; white-space: nowrap; max-width: 100%; display: block; text-align: center; }
.stat-label { font-size: 11px; color: #86909C; margin-top: 2px; }

.section { padding: 0 16px; }
.section-header {
	display: flex;
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 12px;
}
.section-title { font-size: 16px; font-weight: 700; color: #1D2129; }
.section-count { font-size: 12px; color: #86909C; }

.match-list { display: flex; flex-direction: column; gap: 10px; padding-bottom: 20px; }
.match-card {
	background: #fff;
	border-radius: 14px;
	padding: 14px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.match-top {
	display: flex;
	flex-direction: row;
	justify-content: space-between;
	align-items: flex-start;
}
.match-info { flex: 1; }
.student-name { font-size: 15px; font-weight: 600; color: #1D2129; display: block; }
.student-detail { font-size: 12px; color: #86909C; margin-top: 2px; display: block; }
.score-badge {
	padding: 6px 12px;
	border-radius: 20px;
	margin-left: 8px;
}
.score-text { font-size: 14px; font-weight: 700; color: #fff; }
.match-reason {
	margin-top: 8px;
	padding: 6px 10px;
	background: #F8F9FC;
	border-radius: 6px;
	font-size: 12px;
	color: #4E5969;
}
.match-actions {
	display: flex;
	flex-direction: row;
	gap: 8px;
	margin-top: 10px;
}
.action-tag {
	padding: 4px 12px;
	border-radius: 6px;
	font-size: 12px;
	background: #F0F5FF;
	color: #165DFF;
}
.action-tag.active { background: #F6FFED; color: #10B981; }
.view-tag { background: #F5F7FA; color: #4E5969; }

.empty-state { align-items: center; padding: 60px 20px; }
.empty-icon { font-size: 64px; margin-bottom: 16px; }
.empty-title { font-size: 17px; font-weight: 600; color: #1D2129; margin-bottom: 8px; }
.empty-desc { font-size: 14px; color: #86909C; text-align: center; line-height: 1.5; }
</style>
