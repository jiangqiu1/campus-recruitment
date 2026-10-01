<template>
	<view class="page-wrapper" style="background:#F7F8FA;min-height:100vh;">
		<NavBar title="AI 人岗匹配" show-back />
		<scroll-view class="content-scrollable" scroll-y>
			<!-- 模式切换 -->
			<view class="mode-tabs">
				<view class="mode-tab" :class="{ active: matchMode === 'job' }" @click="switchToJobMode">按岗位匹配</view>
				<view class="mode-tab" :class="{ active: matchMode === 'student' }" @click="switchToStudentMode">按学生匹配</view>
			</view>

			<!-- 按岗位匹配 -->
			<view v-if="matchMode === 'job'" class="job-select-bar">
				<view class="select-row">
					<text class="select-label">选择岗位</text>
					<picker v-if="jobList.length" @change="onJobChange" :value="jobIndex" :range="jobList" range-key="title" class="job-picker">
						<view class="job-picker-btn">
							<text>{{ selectedJob ? selectedJob.title : '请选择岗位' }}</text>
							<uni-icons type="arrowdown" size="12" color="#C9CDD4" />
						</view>
					</picker>
					<text v-else style="flex:1;font-size:13px;color:#86909C;">加载中...</text>
				</view>
				<view class="select-row">
					<text class="select-label">筛选班级</text>
					<picker v-if="classList.length" @change="onClassChange" :value="classIndex" :range="classList" range-key="name" class="job-picker">
						<view class="job-picker-btn">
							<text>{{ selectedClass ? selectedClass.name : '全部班级' }}</text>
							<uni-icons type="arrowdown" size="12" color="#C9CDD4" />
						</view>
					</picker>
					<text v-else style="flex:1;font-size:13px;color:#86909C;">加载中...</text>
				</view>
				<button class="batch-btn" :loading="batchLoading" @click="batchMatchByJob" :disabled="!selectedJob">
					{{ batchLoading ? '匹配中...' : '批量匹配' }}
				</button>
			</view>

			<!-- 按学生匹配 -->
			<view v-if="matchMode === 'student'" class="job-select-bar">
				<view class="select-row">
					<text class="select-label">选择班级</text>
					<picker v-if="classList.length" @change="onStudentClassChange" :value="scIndex" :range="classList" range-key="name" class="job-picker">
						<view class="job-picker-btn">
							<text>{{ selectedStudentClass ? selectedStudentClass.name : '请选择班级' }}</text>
							<uni-icons type="arrowdown" size="12" color="#C9CDD4" />
						</view>
					</picker>
					<text v-else style="flex:1;font-size:13px;color:#86909C;">加载中...</text>
				</view>
				<view class="select-row">
					<text class="select-label">选择学生</text>
					<picker v-if="studentList.length" @change="onStudentChange" :value="studentIndex" :range="studentList" range-key="realName" class="job-picker">
						<view class="job-picker-btn">
							<text>{{ selectedStudent ? selectedStudent.realName : '请选择学生' }}</text>
							<uni-icons type="arrowdown" size="12" color="#C9CDD4" />
						</view>
					</picker>
					<text v-else style="flex:1;font-size:13px;color:#86909C;">先选择班级</text>
				</view>
				<button class="batch-btn" :loading="batchLoading" @click="batchMatchByStudent" :disabled="!selectedStudent">
					{{ batchLoading ? '匹配中...' : '为该生匹配岗位' }}
				</button>
			</view>

			<view v-if="dataLoading" class="loading-hint"><text>正在加载匹配数据...</text></view>
			<view v-if="stats && matchMode === 'job'" class="stats-row">
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
			<view v-if="matches.length > 0" class="section">
				<view class="section-header">
					<text class="section-title">匹配{{ matchMode === 'job' ? '学生' : '岗位' }}列表</text>
					<text class="section-count">按匹配度排序</text>
				</view>
				<view class="match-list">
					<view v-for="item in matches" :key="item.id" class="match-card">
						<view class="match-top">
							<view class="match-info">
								<text class="student-name">{{ matchMode === 'job' ? item.studentName : item.jobTitle }}</text>
								<text class="student-detail">{{ matchMode === 'job' ? (item.studentInfo || '') : (item.companyName || '') }}</text>
							</view>
							<view class="match-score-box">
								<view class="score-badge" :style="{ background: scoreBg(item.matchScore) }">
									<text class="score-text" :style="{ color: scoreTx(item.matchScore) }">{{ formatScore(item.matchScore) }}</text>
								</view>
								<text class="score-label">匹配度</text>
							</view>
						</view>
						<text class="match-reason">{{ item.matchReason || '暂无匹配理由' }}</text>
						<view class="match-actions">
							<button class="action-btn push-btn" :class="{ pushed: item.isPushed }" @click="togglePush(item)">{{ item.isPushed ? '已推送' : '推送' }}</button>
							<button class="action-btn view-btn" @click="viewDetail(item)">查看详情</button>
						</view>
					</view>
				</view>
			</view>
			<EmptyState v-else-if="!dataLoading && ((matchMode==='job' && selectedJob) || (matchMode==='student' && selectedStudent)) && matches.length === 0" icon="star" title="暂无匹配数据" :desc="matchMode === 'job' ? '点击「批量匹配」生成AI人岗匹配推荐' : '点击「为该生匹配岗位」生成AI推荐'" />
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { checkRole } from '@/utils/auth'
import { matchAPI, teacherAPI, jobAPI, request } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

checkRole(1)

const matchMode = ref('job')

const jobList = ref([])
const jobIndex = ref(-1)
const selectedJob = ref(null)

const classList = ref([])
const classIndex = ref(-1)
const selectedClass = ref(null)

const scIndex = ref(-1)
const selectedStudentClass = ref(null)
const studentList = ref([])
const studentIndex = ref(-1)
const selectedStudent = ref(null)

const matches = ref([])
const stats = ref(null)
const batchLoading = ref(false)
const dataLoading = ref(false)

onMounted(async () => {
	try {
		// 使用 /jobs 接口获取所有岗位（含分页包装，字段名为 list）
		const res = await jobAPI.getJobs({ page: 1, size: 50 })
		const jobsData = Array.isArray(res.data) ? res.data
			: (res.data && res.data.list ? res.data.list : [])
		jobList.value = jobsData.filter(j => j.status === 1 || j.status === '1')
		const classRes = await teacherAPI.getClasses()
		classList.value = Array.isArray(classRes.data) ? classRes.data : []
	} catch (e) {
		console.error('加载数据失败:', e)
	}

	const pages = getCurrentPages()
	const currentPage = pages[pages.length - 1]
	const jobId = currentPage.options?.jobId
	if (jobId) {
		const idx = jobList.value.findIndex(j => j.id == jobId)
		if (idx >= 0) {
			jobIndex.value = idx
			selectedJob.value = jobList.value[idx]
			await loadData()
		}
	}
})

// ====== 按岗位匹配 ======
const onJobChange = async (e) => {
	jobIndex.value = e.detail.value
	selectedJob.value = jobList.value[jobIndex.value]
	await loadData()
}

const onClassChange = (e) => {
	classIndex.value = e.detail.value
	selectedClass.value = classList.value[classIndex.value] || null
}

const batchMatchByJob = async () => {
	if (!selectedJob.value) return
	batchLoading.value = true
	uni.showLoading({ title: '批量匹配中...' })
	try {
		const classId = selectedClass.value ? selectedClass.value.id : null
		const res = await matchAPI.batchGenerate(selectedJob.value.id, classId)
		const count = res.data?.generatedCount || 0
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

// ====== 按学生匹配 ======
const switchToJobMode = () => {
	matchMode.value = 'job'
	matches.value = []
	stats.value = null
	if (selectedJob.value) loadData()
}

const switchToStudentMode = () => {
	matchMode.value = 'student'
	studentList.value = []
	selectedStudent.value = null
	studentIndex.value = -1
	scIndex.value = -1
	selectedStudentClass.value = null
	matches.value = []
	stats.value = null
}

const onStudentClassChange = async (e) => {
	scIndex.value = e.detail.value
	selectedStudentClass.value = classList.value[scIndex.value] || null
	if (selectedStudentClass.value) {
		try {
			const res = await request({ url: '/classes/' + selectedStudentClass.value.id + '/students' })
			studentList.value = res.data || []
		} catch (err) {
			studentList.value = []
		}
	}
	selectedStudent.value = null
	studentIndex.value = -1
	matches.value = []
}

const onStudentChange = (e) => {
	studentIndex.value = e.detail.value
	selectedStudent.value = studentList.value[studentIndex.value] || null
	loadStudentMatches(selectedStudent.value.id)
}

const batchMatchByStudent = async () => {
	if (!selectedStudent.value) return
	batchLoading.value = true
	uni.showLoading({ title: 'AI匹配中...' })
	const studentId = selectedStudent.value.id
	try {
		const jRes = await jobAPI.getRecommendJobs()
		const jobs = Array.isArray(jRes.data) ? jRes.data : []
		let count = 0
		for (const job of jobs) {
			try { await matchAPI.generate(job.id, studentId); count++ } catch (e) {}
		}
		uni.hideLoading()
		uni.showToast({ title: '匹配完成 ' + count + '/' + jobs.length + ' 个岗位', icon: 'success' })
		await loadStudentMatches(studentId)
	} catch (e) {
		uni.hideLoading()
		console.error('匹配失败', e)
		uni.showToast({ title: '匹配失败', icon: 'none' })
	} finally {
		batchLoading.value = false
	}
}

const loadStudentMatches = async (studentId) => {
	dataLoading.value = true
	try {
		const mRes = await matchAPI.getByStudent(studentId)
		const raw = mRes.data || []
		const enriched = await Promise.all(raw.map(async (m) => {
			try {
				const jRes = await jobAPI.getJobDetail(m.jobId)
				const j = jRes.data || {}
				return { ...m, jobTitle: j.title || '岗位#' + m.jobId, companyName: j.companyName || '' }
			} catch (e) {
				return { ...m, jobTitle: '岗位#' + m.jobId }
			}
		}))
		matches.value = enriched.sort((a, b) => (b.matchScore || 0) - (a.matchScore || 0))
	} catch (e) {
		console.error('加载匹配数据失败', e)
	} finally {
		dataLoading.value = false
	}
}

// ====== 加载岗位模式数据 ======
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
		const raw = mRes.data || []
		matches.value = await enrichStudentNames(raw)
		stats.value = {
			avgScore: avgRes.data?.averageMatchScore,
			pushRate: pushRes.data?.pushRate,
			clickRate: clickRes.data?.clickRate
		}
	} catch (e) {
		console.error('加载匹配数据失败', e)
	} finally {
		dataLoading.value = false
	}
}

const enrichStudentNames = async (records) => {
	if (!records || !records.length) return records
	const ids = [...new Set(records.map(r => r.studentId).filter(Boolean))]
	const nameMap = {}
	await Promise.all(ids.map(async (sid) => {
		try {
			const uRes = await request({ url: '/auth/user-basic/' + sid })
			const u = uRes.data || {}
			nameMap[sid] = u.realName || ''
		} catch (e) { nameMap[sid] = '' }
	}))
	return records.map(r => ({ ...r, studentName: nameMap[r.studentId] || '学生#' + r.studentId }))
}

const togglePush = async (item) => {
	try {
		await matchAPI.push(item.id)
		item.isPushed = 1
		uni.showToast({ title: '已推送', icon: 'success' })
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'none' })
	}
}

const viewDetail = (item) => {
	if (matchMode.value === 'student' && item.jobId) {
		uni.navigateTo({ url: '/pages/student/job-detail?id=' + item.jobId })
	}
}

// ====== 工具函数 ======
const formatScore = (score) => {
	if (score == null) return '--'
	const num = typeof score === 'string' ? parseFloat(score) : score
	return Math.round(num * 100) + '%'
}
const formatPercent = (val) => {
	if (val == null) return '--'
	return Math.round(val * 100) + '%'
}
const scoreBg = (score) => {
	if (!score) return 'rgba(134,144,156,0.1)'
	const s = typeof score === 'string' ? parseFloat(score) : score
	if (s >= 0.8) return 'rgba(0,180,42,0.12)'
	if (s >= 0.6) return 'rgba(22,93,255,0.12)'
	if (s >= 0.4) return 'rgba(255,125,0,0.12)'
	return 'rgba(134,144,156,0.1)'
}
const scoreTx = (score) => {
	if (!score) return '#86909C'
	const s = typeof score === 'string' ? parseFloat(score) : score
	if (s >= 0.8) return '#00B42A'
	if (s >= 0.4) return '#FF7D00'
	return '#86909C'
}
</script>

<style scoped lang="scss">
.mode-tabs {
	flex-direction: row; background: $uni-bg-color; margin: 12px 16px; border-radius: 12px; overflow: hidden;
}
.mode-tab {
	flex: 1; text-align: center; padding: 12px 0; font-size: 14px; font-weight: 500; color: $uni-text-color-secondary;
}
.mode-tab.active { color: $uni-color-primary; background: $uni-color-primary-light; font-weight: 600; }
.job-select-bar { background: white; padding: 14px 16px; margin: 0 16px 12px; border-radius: 12px; gap: 10px; box-shadow: $uni-shadow-card; }
.select-row { flex-direction: row; align-items: center; gap: 10px; }
.select-label { font-size: 14px; font-weight: 600; color: $uni-text-color-title; white-space: nowrap; }
.job-picker { flex: 1; }
.job-picker-btn { flex-direction: row; align-items: center; justify-content: space-between; padding: 8px 12px; background: $uni-bg-color-page; border-radius: 8px; }
.job-picker-btn text { font-size: 13px; color: $uni-text-color; }
.batch-btn { width: 100%; height: 42px; border-radius: 12px; background: $uni-color-primary; color: $uni-text-color-inverse; font-size: 15px; font-weight: 600; align-items: center; justify-content: center; border: none; margin-top: 4px; }
.batch-btn[disabled] { background: $uni-border-color !important; color: $uni-text-color-placeholder !important; }
.loading-hint { padding: 40px 16px; text-align: center; font-size: 13px; color: $uni-text-color-secondary; }
.stats-row { flex-direction: row; flex-wrap: wrap; padding: 0 16px; gap: 8px; }
.stat-card { flex: 1; min-width: 70px; background: white; border-radius: 12px; padding: 12px; align-items: center; box-shadow: $uni-shadow-card; }
.stat-num { font-size: 20px; font-weight: 700; color: $uni-text-color-title; }
.stat-label { font-size: 12px; color: $uni-text-color-secondary; margin-top: 4px; }
.section { padding: 0 16px; }
.section-header { flex-direction: row; justify-content: space-between; align-items: center; padding: 12px 0; }
.section-title { font-size: 16px; font-weight: 600; color: $uni-text-color-title; }
.section-count { font-size: 12px; color: $uni-text-color-secondary; }
.match-list { gap: 12px; }
.match-card { background: white; border-radius: 12px; padding: 14px; gap: 10px; box-shadow: $uni-shadow-card; }
.match-top { flex-direction: row; justify-content: space-between; align-items: center; }
.match-info { flex: 1; gap: 4px; }
.student-name { font-size: 15px; font-weight: 600; color: $uni-text-color-title; }
.student-detail { font-size: 12px; color: $uni-text-color-secondary; }
.match-score-box { align-items: center; gap: 2px; }
.score-badge { padding: 4px 10px; border-radius: 8px; }
.score-text { font-size: 15px; font-weight: 700; }
.score-label { font-size: 10px; color: $uni-text-color-placeholder; }
.match-reason { font-size: 13px; color: $uni-text-color; line-height: 1.6; }
.match-actions { flex-direction: row; gap: 10px; }
.action-btn { flex: 1; height: 36px; border-radius: 8px; font-size: 13px; font-weight: 500; align-items: center; justify-content: center; border: none; }
.push-btn { background: $uni-color-primary-light; color: $uni-color-primary; }
.push-btn.pushed { background: $uni-border-color; color: $uni-text-color-placeholder; }
.view-btn { background: $uni-bg-color-page; color: $uni-text-color; }
</style>
