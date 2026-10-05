<template>
	<view class="page-wrapper">
		<NavBar :title="studentName + ' 的简历'" show-back />
		<scroll-view class="content-scrollable" scroll-y>
			<LoadingState type="skeleton" :rows="5" v-if="loading" />
			<ErrorState v-else-if="loadFailed" error-msg="简历加载失败" @retry="loadData" />
			<template v-else>
			<!-- 头部信息卡：强化视觉层级 -->
			<view class="info-header-card">
				<view class="info-avatar">
					<text>{{ (studentName || '学').charAt(0) }}</text>
				</view>
				<view class="basic-info">
					<text class="name">{{ studentName || '未知' }}</text>
					<text class="desc" v-if="resumeData.basic">{{ resumeData.basic.major }} · {{ resumeData.basic.school }}</text>
					<view class="contact-row" v-if="resumeData.basic">
						<text><uni-icons type="phone" size="12" color="#86909C" /> {{ resumeData.basic.phone || '—' }}</text>
						<text><uni-icons type="email" size="12" color="#86909C" /> {{ resumeData.basic.email || '—' }}</text>
					</view>
				</view>
			</view>

			<!-- 标签信息 -->
			<view class="section-card" v-if="resumeData.basic">
				<view class="tag-row">
					<text class="tag-tag">{{ resumeData.basic.education || '大专' }}</text>
					<text v-if="resumeData.basic.gender && resumeData.basic.gender !== '未知'" class="tag-tag">{{ resumeData.basic.gender }}</text>
					<text class="tag-tag">{{ resumeData.basic.graduationYear || '待毕业' }}</text>
				</view>
			</view>

			<!-- 技能特长 -->
			<view class="section-card">
				<text class="section-title">技能特长</text>
				<view class="tag-container">
					<text v-for="(s, i) in resumeData.skills" :key="i" class="skill-tag">{{ s }}</text>
					<text v-if="!resumeData.skills || !resumeData.skills.length" class="section-empty">暂无技能</text>
				</view>
			</view>

			<!-- 教育经历 -->
			<view class="section-card">
				<text class="section-title">教育经历</text>
				<view v-for="(edu, i) in resumeData.education" :key="i" class="exp-item">
					<view class="exp-head">
						<text class="exp-school">{{ edu.school }}</text>
						<text class="exp-time">{{ edu.startDate || edu.start }} - {{ edu.endDate || edu.end || '至今' }}</text>
					</view>
					<text class="exp-sub">{{ edu.major }} · {{ edu.degree }}</text>
				</view>
				<text v-if="!resumeData.education || !resumeData.education.length" class="section-empty">暂无教育经历</text>
			</view>

			<!-- 实习经历 -->
			<view class="section-card">
				<text class="section-title">实习经历</text>
				<view v-for="(job, i) in resumeData.internship" :key="i" class="exp-item">
					<view class="exp-head">
						<text class="exp-school">{{ job.company || job.companyName }}</text>
						<text class="exp-time">{{ job.duration || job.start }} - {{ job.end || '至今' }}</text>
					</view>
					<text class="exp-sub">{{ job.position || job.jobTitle }}</text>
					<text class="exp-desc" v-if="job.description">{{ job.description }}</text>
				</view>
				<text v-if="!resumeData.internship || !resumeData.internship.length" class="section-empty">暂无实习经历</text>
			</view>

			<!-- 求职意向 + 自我评价 -->
			<view class="section-card">
				<text class="section-title">求职意向</text>
				<text class="section-value-strong">{{ resumeData.jobTarget || '未设置' }}</text>
			</view>
			<view class="section-card" v-if="resumeData.selfEvaluation">
				<text class="section-title">自我评价</text>
				<text class="section-value-block">{{ resumeData.selfEvaluation }}</text>
			</view>

			<!-- AI 简历分析 -->
			<view class="section-card">
				<view class="section-title-row">
					<text class="section-title">AI 简历分析</text>
					<text class="ai-analyze-btn" @click="handleAnalyze" v-if="!aiAnalyzing">
						<uni-icons type="star" size="14" color="#0EA5E9" />
						<text>分析简历</text>
					</text>
				</view>
				<view class="ai-result" v-if="aiResult">
					<view class="ai-score-row">
						<text>综合评分</text>
						<text class="score-num">{{ aiResult.overallScore || '--' }}分</text>
					</view>
					<view class="ai-section" v-if="aiResult.strengths && aiResult.strengths.length">
						<text class="ai-subtitle">优势</text>
						<text v-for="(s, i) in aiResult.strengths" :key="i" class="ai-item ai-item--green">{{ s }}</text>
					</view>
					<view class="ai-section" v-if="aiResult.weaknesses && aiResult.weaknesses.length">
						<text class="ai-subtitle">不足</text>
						<text v-for="(w, i) in aiResult.weaknesses" :key="i" class="ai-item ai-item--red">{{ w }}</text>
					</view>
					<view class="ai-section" v-if="aiResult.suggestions && aiResult.suggestions.length">
						<text class="ai-subtitle">改进建议</text>
						<text v-for="(sg, i) in aiResult.suggestions" :key="i" class="ai-item ai-item--blue">{{ sg }}</text>
					</view>
					<view class="ai-section" v-if="aiResult.missingFields && aiResult.missingFields.length">
						<text class="ai-subtitle">缺失字段</text>
						<text class="ai-item ai-item--amber">{{ aiResult.missingFields.join('、') }}</text>
					</view>
					<view class="ai-section" v-if="aiResult.recommendedSkills && aiResult.recommendedSkills.length">
						<text class="ai-subtitle">推荐补充技能</text>
						<view class="tag-container">
							<text v-for="(sk, i) in aiResult.recommendedSkills" :key="i" class="skill-tag skill-tag--ai">{{ sk }}</text>
						</view>
					</view>
				</view>
				<view v-else-if="aiAnalyzing" class="ai-loading">
					<text>AI 正在分析简历...</text>
				</view>
				<text v-else class="section-empty">点击「分析简历」获取优化建议</text>
			</view>

			<!-- 投递记录 -->
			<view class="section-card">
				<text class="section-title">投递记录</text>
				<view v-for="(d, i) in deliveries" :key="i" class="delivery-item">
					<view class="delivery-top">
						<view class="delivery-info">
							<text class="delivery-job">{{ d.jobTitle }}</text>
							<text class="delivery-company">{{ d.companyName }}</text>
						</view>
						<text class="status-tag" :class="'status-' + d.status">{{ d.statusText }}</text>
					</view>
					<text class="delivery-time">{{ d.createTime }}</text>
				</view>
				<text v-if="!deliveries.length" class="section-empty section-empty--pad">暂无投递记录</text>
			</view>

			<view style="height: 80px"></view>
			</template>
		</scroll-view>

		<!-- 底部操作栏 -->
		<view class="bottom-bar">
			<block v-if="deliveryId">
				<button class="btn-outline" @click="sendInterview">发送面试邀请</button>
				<button class="btn-primary" @click="markEmployed">标记录用</button>
			</block>
			<block v-else>
				<button class="btn-primary" style="flex:1;" @click="viewDeliveries">查看投递记录</button>
			</block>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import LoadingState from '@/components/LoadingState.vue'
import ErrorState from '@/components/ErrorState.vue'
import { teacherAPI, jobAPI, aiParseAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import { checkRole } from '@/utils/auth'

checkRole(1)

const studentId = ref('')
const studentName = ref('')
const deliveryId = ref('')
const resumeData = ref({})
const aiResult = ref(null)
const aiAnalyzing = ref(false)
const deliveries = ref([])

const STATUS_MAP = { 0: 'pending', 1: 'viewed', 2: 'interview', 3: 'accepted', 4: 'rejected' }
const STATUS_TEXT_MAP = { 0: '待查看', 1: '已查看', 2: '面试', 3: '已录用', 4: '不合适' }

const mapResume = (resume) => {
	if (!resume) return {}
	let eduArray = []
	try { eduArray = JSON.parse(resume.education || '[]') } catch (e) {}
	let skillsArray = []
	if (resume.skills) {
		try { skillsArray = JSON.parse(resume.skills) } catch (e) {
			skillsArray = resume.skills.split(/[,\n]/).map(s => s.trim()).filter(Boolean)
		}
	}
	return {
		basic: {
			realName: resume.realName || '',
			phone: resume.phone || '',
			email: resume.email || '',
			major: eduArray[0]?.major || '',
			school: eduArray[0]?.school || '',
			education: eduArray[0]?.degree || '大专',
			gender: resume.gender || eduArray[0]?.gender || '未知',
			graduationYear: eduArray[0]?.endDate ? eduArray[0].endDate.substring(0, 4) + '届' : '待毕业'
		},
		skills: skillsArray,
		education: eduArray,
		internship: (() => {
			try { return JSON.parse(resume.internship || '[]') } catch (e) { return [] }
		})(),
		selfEvaluation: resume.selfEvaluation || '',
		jobTarget: resume.jobTarget || '',
		pdfUrl: resume.pdfUrl || ''
	}
}

const mapDelivery = (d) => ({
	id: d.id,
	jobTitle: d.jobTitle || '未知岗位',
	companyName: '加载中...',
	status: STATUS_MAP[d.status] || 'pending',
	statusText: STATUS_TEXT_MAP[d.status] || '待查看',
	createTime: d.createTime ? d.createTime.substring(0, 10) : ''
})

const loadDeliveryCompanyNames = async (rawDeliveries) => {
	const jobIds = [...new Set(rawDeliveries.filter(d => d.jobId).map(d => d.jobId))]
	if (!jobIds.length) return
	try {
		const jobCache = {}
		await Promise.all(jobIds.map(async (jid) => {
			try {
				const res = await jobAPI.getJobDetail(jid)
				const job = res?.data
				if (job) {
					let companyName = '未知企业'
					if (job.companyName) { companyName = job.companyName }
					else if (job.companyId) {
						try {
							const cRes = await teacherAPI.getCompanyName(job.companyId)
							companyName = cRes?.data?.name || '企业' + job.companyId
						} catch (e) { companyName = '企业' + job.companyId }
					}
					jobCache[jid] = { companyName, jobTitle: job.title }
				}
			} catch (e) {}
		}))
		deliveries.value = deliveries.value.map(d => {
			const cached = jobCache[rawDeliveries.find(r => r.jobTitle === d.jobTitle)?.jobId]
			if (cached) {
				d.companyName = cached.companyName
				d.jobTitle = cached.jobTitle || d.jobTitle
			}
			return d
		})
	} catch (e) {}
}

onMounted(() => {
	const pages = getCurrentPages()
	const currentPage = pages[pages.length - 1]
	if (currentPage.options) {
		studentId.value = currentPage.options.studentId || ''
		studentName.value = decodeURIComponent(currentPage.options.name || '学生')
		deliveryId.value = currentPage.options.deliveryId || ''
	}
	loadData()
})

const loading = ref(true)
const loadFailed = ref(false)

const loadData = async () => {
	loading.value = true
	loadFailed.value = false
	try {
		const calls = [
			teacherAPI.getStudentResume(studentId.value),
			teacherAPI.getStudentDeliveries(studentId.value)
		]
		if (!studentName.value || studentName.value === '学生') {
			calls.push(teacherAPI.getStudentInfo(studentId.value).catch(() => ({ data: { realName: '' } })))
		}
		const [resumeRes, deliveryRes, infoRes] = await Promise.all(calls)

		const rawResume = resumeRes.data
		resumeData.value = mapResume(rawResume)

		if (infoRes?.data?.realName) {
			studentName.value = infoRes.data.realName
		}

		// 尝试从简历中加载已保存的 AI 分析结果
		if (rawResume) {
			const savedAnalysis = rawResume.aiAnalysis || null
			if (savedAnalysis) {
				try {
					aiResult.value = typeof savedAnalysis === 'string' ? JSON.parse(savedAnalysis) : savedAnalysis
				} catch (e) {
					aiResult.value = null
				}
			} else {
				aiResult.value = null
			}
		} else {
			aiResult.value = null
		}
		const rawDeliveries = deliveryRes.data || []
		deliveries.value = rawDeliveries.map(mapDelivery)
		if (rawDeliveries.length > 0) loadDeliveryCompanyNames(rawDeliveries)
	} catch (e) {
		console.error('加载学生详情失败', e)
		loadFailed.value = true
	} finally {
		loading.value = false
	}
}

const sendInterview = async () => {
	if (!deliveryId.value) return
	try {
		await teacherAPI.updateDeliveryStatus(deliveryId.value, { status: 2, feedback: '邀请参加面试' })
		uni.showToast({ title: '已发送面试邀请', icon: 'success' })
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'none' })
	}
}

const markEmployed = async () => {
	if (!deliveryId.value) return
	try {
		await teacherAPI.updateDeliveryStatus(deliveryId.value, { status: 3, feedback: '已录用' })
		uni.showToast({ title: '已标记录用', icon: 'success' })
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'none' })
	}
}

const viewDeliveries = () => {
	uni.navigateTo({ url: '/pages/teacher/deliveries' })
}

// AI 简历分析
const handleAnalyze = async () => {
	if (!studentId.value) return
	aiAnalyzing.value = true
	aiResult.value = null
	try {
		const res = await aiParseAPI.analyzeResume(studentId.value)
		aiResult.value = res.data || {}
	} catch (e) {
		console.error('简历分析失败', e)
		uni.showToast({ title: '分析失败', icon: 'none' })
	} finally {
		aiAnalyzing.value = false
	}
}
</script>

<style scoped lang="scss">
/* 头部信息卡 */
.info-header-card {
	margin: 16px 16px 0;
	background: white;
	border-radius: 12px;
	padding: 20px;
	box-shadow: $uni-shadow-card;
	flex-direction: row;
	align-items: center;
	gap: 12px;
}
.info-avatar {
	width: 56px;
	height: 56px;
	border-radius: 50%;
	background: $uni-gradient-primary;
	align-items: center;
	justify-content: center;
	font-size: 24px;
	color: white;
	font-weight: 700;
	flex-shrink: 0;
}
.basic-info { flex: 1; }
.basic-info .name { font-size: 18px; font-weight: 700; color: $uni-text-color-title; display: block; margin-bottom: 4px; }
.basic-info .desc { font-size: 13px; color: $uni-text-color-secondary; display: block; margin-bottom: 6px; }
.contact-row { flex-direction: row; flex-wrap: wrap; gap: 12px; }
.contact-row text { font-size: 12px; color: $uni-text-color-secondary; flex-direction: row; align-items: center; gap: 4px; word-break: break-all; }

/* 模块卡片全局统一样式 */
.section-card {
	margin: 0 16px 12px;
	background: white;
	border-radius: 12px;
	padding: 16px;
	box-shadow: $uni-shadow-card;
}
.section-title { font-size: 15px; font-weight: 700; color: $uni-text-color-title; margin-bottom: 12px; display: block; }
.section-empty { font-size: 13px; color: $uni-text-color-secondary; }
.section-empty--pad { display: block; padding: 16px 0; }
.section-value-strong { font-size: 14px; color: $uni-text-color-title; }
.section-value-block { font-size: 14px; color: $uni-text-color; line-height: 1.7; display: block; }

.tag-row { flex-direction: row; gap: 8px; }
.tag-tag { padding: 4px 10px; border-radius: 8px; font-size: 12px; font-weight: 600; background: $uni-border-color-divider; color: $uni-text-color; }

.tag-container { flex-direction: row; flex-wrap: wrap; gap: 8px; }
.skill-tag { padding: 6px 14px; border-radius: 999px; font-size: 13px; font-weight: 500; background: $uni-color-primary-light; color: $uni-color-primary; }
.skill-tag--ai { background: $uni-color-ai-light; color: $uni-color-ai; }

.exp-item { padding: 12px 0; border-bottom: 0.5px solid $uni-border-color-divider; }
.exp-item:last-child { border-bottom: none; }
.exp-head { flex-direction: row; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.exp-school { font-size: 14px; font-weight: 600; color: $uni-text-color-title; }
.exp-time { font-size: 12px; color: $uni-text-color-placeholder; }
.exp-sub { font-size: 13px; color: $uni-text-color; display: block; }
.exp-desc { font-size: 13px; color: $uni-text-color-secondary; display: block; margin-top: 4px; line-height: 1.5; }

/* AI分析 */
.ai-result { gap: 8px; }
.ai-score-row { flex-direction: row; justify-content: space-between; padding: 8px 0; border-bottom: 0.5px solid $uni-border-color-divider; }
.ai-score-row text:first-child { font-size: 13px; color: $uni-text-color-secondary; }
.score-num { font-size: 16px; color: $uni-color-primary; font-weight: 700; }
.score-val { font-size: 13px; color: $uni-text-color-title; font-weight: 600; }
.ai-suggestion { margin-top: 8px; padding: 12px; background: $uni-color-primary-light; border-radius: 12px; }
.ai-suggestion text { font-size: 13px; color: $uni-text-color; line-height: 1.6; }

/* AI 分析新样式 */
.section-title-row {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 12px;
}
.ai-analyze-btn {
	flex-direction: row;
	align-items: center;
	gap: 4px;
	padding: 4px 12px;
	background: $uni-color-ai-light;
	border-radius: 14px;
	font-size: 12px;
	color: $uni-color-ai;
	font-weight: 600;
}
.ai-analyze-btn:active { opacity: 0.7; }
.ai-loading { padding: 20px 0; align-items: center; }
.ai-loading text { font-size: 13px; color: $uni-color-ai; }
.ai-section { margin-top: 10px; }
.ai-subtitle { font-size: 13px; font-weight: 600; color: $uni-text-color; margin-bottom: 6px; display: block; }
.ai-item { display: block; font-size: 13px; padding: 6px 10px; border-radius: 8px; margin-bottom: 4px; line-height: 1.5; }
.ai-item--green { background: $uni-color-success-light; color: $uni-color-success; }
.ai-item--red { background: $uni-color-error-light; color: $uni-color-error; }
.ai-item--blue { background: $uni-color-primary-light; color: $uni-color-primary; }
.ai-item--amber { background: $uni-color-warning-light; color: $uni-color-warning; }

/* 投递记录 */
.delivery-item { padding: 12px 0; border-bottom: 0.5px solid $uni-border-color-divider; }
.delivery-item:last-child { border-bottom: none; }
.delivery-top { flex-direction: row; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.delivery-info { flex: 1; }
.delivery-job { font-size: 14px; font-weight: 600; color: $uni-text-color-title; display: block; margin-bottom: 2px; }
.delivery-company { font-size: 12px; color: $uni-text-color-secondary; display: block; }
.delivery-time { font-size: 12px; color: $uni-text-color-placeholder; display: block; }
.status-tag { font-size: 12px; padding: 2px 8px; border-radius: 8px; font-weight: 600; flex-shrink: 0; }
.status-pending { background: $uni-color-warning-light; color: $uni-color-warning; }
.status-viewed { background: $uni-color-primary-light; color: $uni-color-primary; }
.status-interview { background: $uni-color-primary-light; color: $uni-color-primary; }
.status-accepted { background: $uni-color-success-light; color: $uni-color-success; }
.status-rejected { background: $uni-color-error-light; color: $uni-color-error; }

/* 底部操作栏 */
.bottom-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	flex-direction: row;
	padding: 12px 16px;
	padding-bottom: calc(12px + env(safe-area-inset-bottom));
	background: white;
	border-top: 0.5px solid $uni-border-color-divider;
	gap: 12px;
}
.btn-outline, .btn-primary {
	flex: 1;
	height: 44px;
	border-radius: 12px;
	font-size: 15px;
	font-weight: 600;
	align-items: center;
	justify-content: center;
	border: none;
}
.btn-outline { background: white; border: 1px solid $uni-border-color; color: $uni-text-color; }
.btn-primary { background: $uni-color-primary; color: white; }
</style>
