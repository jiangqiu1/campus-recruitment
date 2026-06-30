<template>
	<view class="page-wrapper">
		<view class="header-simple" style="padding:12px 16px;flex-direction:row;align-items:center;gap:12px;">
			<text style="font-size:20px;" @click="goBack">‹</text>
			<text style="font-size:18px;font-weight:700;color:white;">{{ studentName }} 的简历</text>
		</view>
		<scroll-view class="content-scrollable" scroll-y>
			<!-- 基本信息 -->
			<view class="info-card">
				<view class="info-header">
					<view class="info-avatar">
						<text>{{ (studentName || '学').charAt(0) }}</text>
					</view>
					<view class="info-basic">
						<text class="info-name">{{ studentName || '未知' }}</text>
						<text class="info-detail" v-if="resumeData.basic">{{ resumeData.basic.major }} · {{ resumeData.basic.school }}</text>
					</view>
				</view>
				<view class="info-tags" v-if="resumeData.basic">
					<text class="tag-tag">{{ resumeData.basic.education || '大专' }}</text>
					<text class="tag-tag">{{ resumeData.basic.gender || '未知' }}</text>
					<text class="tag-tag">{{ resumeData.basic.graduationYear || '待毕业' }}</text>
				</view>
			</view>

			<!-- 技能 -->
			<view class="section-card">
				<text class="section-title">🛠️ 技能</text>
				<view class="tag-container">
					<text v-for="(s, i) in resumeData.skills" :key="i" class="skill-tag">{{ s }}</text>
					<text v-if="!resumeData.skills || !resumeData.skills.length" style="color:#86909C;font-size:13px;">暂无技能</text>
				</view>
			</view>

			<!-- 教育经历 -->
			<view class="section-card">
				<text class="section-title">🎓 教育经历</text>
				<view v-for="(edu, i) in resumeData.education" :key="i" class="exp-item">
					<text class="exp-title">{{ edu.school }}</text>
					<text class="exp-sub">{{ edu.major }} · {{ edu.degree }}</text>
					<text class="exp-time">{{ edu.startDate }} - {{ edu.endDate || '至今' }}</text>
				</view>
				<text v-if="!resumeData.education || !resumeData.education.length" style="color:#86909C;font-size:13px;">暂无教育经历</text>
			</view>

			<!-- 项目经验 -->
			<view class="section-card">
				<text class="section-title">📂 项目经验</text>
				<view v-for="(proj, i) in resumeData.projects" :key="i" class="exp-item">
					<text class="exp-title">{{ proj.name }}</text>
					<text class="exp-desc">{{ proj.description }}</text>
					<text class="exp-time">{{ proj.startDate }} - {{ proj.endDate || '至今' }}</text>
				</view>
				<text v-if="!resumeData.projects || !resumeData.projects.length" style="color:#86909C;font-size:13px;">暂无项目经验</text>
			</view>

			<!-- AI 解析结果 -->
			<view class="section-card">
				<text class="section-title">🤖 AI 简历解析</text>
				<view class="ai-result" v-if="aiResult">
					<view class="ai-row">
						<text class="ai-label">匹配度</text>
						<text class="ai-value">{{ aiResult.matchScore || '--' }}%</text>
					</view>
					<view class="ai-row">
						<text class="ai-label">推荐岗位方向</text>
						<text class="ai-value">{{ aiResult.recommendDirection || '--' }}</text>
					</view>
					<view class="ai-row">
						<text class="ai-label">技能短板</text>
						<text class="ai-value">{{ aiResult.weakness || '--' }}</text>
					</view>
					<view class="ai-suggestion" v-if="aiResult.suggestion">
						<text>{{ aiResult.suggestion }}</text>
					</view>
				</view>
				<text v-else style="color:#86909C;font-size:13px;">暂无 AI 解析数据</text>
			</view>

			<!-- 投递记录 -->
			<view class="section-card">
				<text class="section-title">📮 投递记录</text>
				<view v-for="(d, i) in deliveries" :key="i" class="delivery-item">
					<view class="delivery-top">
						<text class="delivery-job">{{ d.jobTitle }}</text>
						<text class="delivery-status" :class="'status-' + d.status">{{ d.statusText }}</text>
					</view>
					<text class="delivery-company">{{ d.companyName }}</text>
					<text class="delivery-time">{{ d.createTime }}</text>
				</view>
				<text v-if="!deliveries.length" style="color:#86909C;font-size:13px;display:block;padding:16px 0;">暂无投递记录</text>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { teacherAPI, jobAPI, resumeAPI } from '@/utils/request'

const studentId = ref('')
const studentName = ref('')
const resumeData = ref({})
const aiResult = ref(null)
const deliveries = ref([])

// 后端Delivery.status Integer → 前端展示状态
const STATUS_MAP = { 0: 'pending', 1: 'viewed', 2: 'interview', 3: 'accepted', 4: 'rejected' }
const STATUS_TEXT_MAP = { 0: '待查看', 1: '已查看', 2: '面试', 3: '已录用', 4: '不合适' }

// 后端Resume扁平字段 → 前端展示结构
const mapResume = (resume) => {
	if (!resume) return {}
	// 解析JSON字段（education为JSON字符串）
	let eduArray = []
	try { eduArray = JSON.parse(resume.education || '[]') } catch (e) { }
	// 解析skills（可能是JSON数组或逗号分隔）
	let skillsArray = []
	if (resume.skills) {
		try { skillsArray = JSON.parse(resume.skills) } catch (e) {
			skillsArray = resume.skills.split(/[,\n]/).map(s => s.trim()).filter(Boolean)
		}
	}
	return {
		basic: {
			realName: resume.realName || '',
			major: eduArray[0]?.major || '',
			school: eduArray[0]?.school || '',
			education: eduArray[0]?.degree || '大专',
			gender: eduArray[0]?.gender || '未知',
			graduationYear: eduArray[0]?.endDate ? eduArray[0].endDate.substring(0, 4) + '届' : '待毕业'
		},
		skills: skillsArray,
		education: eduArray,
		projects: [] // 后端暂无project字段
	}
}

// 后端DeliveryVO → 前端展示映射
const mapDelivery = (d) => ({
	id: d.id,
	jobTitle: d.jobTitle || '未知岗位',
	companyName: '加载中...',
	status: STATUS_MAP[d.status] || 'pending',
	statusText: STATUS_TEXT_MAP[d.status] || '待查看',
	createTime: d.createTime ? d.createTime.substring(0, 10) : ''
})

// 异步加载各投递记录的公司名
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
			} catch (e) { }
		}))
		deliveries.value = deliveries.value.map(d => {
			const cached = jobCache[rawDeliveries.find(r => r.jobTitle === d.jobTitle)?.jobId]
			if (cached) {
				d.companyName = cached.companyName
				d.jobTitle = cached.jobTitle || d.jobTitle
			}
			return d
		})
	} catch (e) { }
}

onMounted(() => {
	const pages = getCurrentPages()
	const currentPage = pages[pages.length - 1]
	if (currentPage.options) {
		studentId.value = currentPage.options.studentId || ''
		studentName.value = decodeURIComponent(currentPage.options.name || '学生')
	}
	loadData()
})

const loadData = async () => {
	try {
		const calls = [
			teacherAPI.getStudentResume(studentId.value),
			teacherAPI.getStudentDeliveries(studentId.value)
		]
		// 如果URL没传name，从后端兜底获取
		if (!studentName.value || studentName.value === '学生') {
			calls.push(
				teacherAPI.getStudentInfo(studentId.value).catch(() => ({ data: { realName: '' } }))
			)
		}
		const [resumeRes, deliveryRes, infoRes] = await Promise.all(calls)

		const rawResume = resumeRes.data
		resumeData.value = mapResume(rawResume)

		// 用后端返回的真实姓名覆盖URL参数
		if (infoRes?.data?.realName) {
			studentName.value = infoRes.data.realName
		}

		aiResult.value = rawResume ? (rawResume.aiAnalysis || rawResume.aiResult || null) : null
		const rawDeliveries = deliveryRes.data || []
		deliveries.value = rawDeliveries.map(mapDelivery)
		if (rawDeliveries.length > 0) loadDeliveryCompanyNames(rawDeliveries)
	} catch (e) {
		console.error('加载学生详情失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

const goBack = () => {
	uni.navigateBack()
}
</script>

<style scoped>
.header-simple {
	background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
	color: white;
	flex-shrink: 0;
}
.info-card {
	margin: 16px;
	background: white;
	border-radius: 16px;
	padding: 20px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
	position: relative;
	overflow: hidden;
}
.info-card::before {
	content: '';
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 3px;
	background: linear-gradient(90deg, #10B981, transparent);
}
.info-header {
	flex-direction: row;
	align-items: center;
	gap: 12px;
	margin-bottom: 12px;
}
.info-avatar {
	width: 56px;
	height: 56px;
	border-radius: 50%;
	background: linear-gradient(135deg, #10B981, #34D399);
	align-items: center;
	justify-content: center;
	font-size: 24px;
	color: white;
	font-weight: 700;
}
.info-basic { flex: 1; }
.info-name {
	font-size: 18px;
	font-weight: 700;
	color: #1D2129;
	display: block;
	margin-bottom: 4px;
}
.info-detail {
	font-size: 13px;
	color: #86909C;
	display: block;
}
.info-tags {
	flex-direction: row;
	gap: 8px;
}
.tag-tag {
	padding: 4px 10px;
	border-radius: 8px;
	font-size: 12px;
	font-weight: 600;
	background: #F2F3F5;
	color: #4E5969;
}
.section-card {
	margin: 0 16px 12px;
	background: white;
	border-radius: 16px;
	padding: 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.section-title {
	font-size: 15px;
	font-weight: 700;
	color: #1D2129;
	margin-bottom: 12px;
	display: block;
}
.tag-container {
	flex-direction: row;
	flex-wrap: wrap;
	gap: 8px;
}
.skill-tag {
	padding: 6px 14px;
	border-radius: 20px;
	font-size: 13px;
	font-weight: 500;
	background: rgba(16,185,129,0.1);
	color: #10B981;
}
.exp-item {
	padding: 12px 0;
	border-bottom: 1px solid #F2F3F5;
}
.exp-item:last-child { border-bottom: none; }
.exp-title {
	font-size: 14px;
	font-weight: 600;
	color: #1D2129;
	display: block;
	margin-bottom: 4px;
}
.exp-sub {
	font-size: 13px;
	color: #4E5969;
	display: block;
	margin-bottom: 2px;
}
.exp-desc {
	font-size: 13px;
	color: #86909C;
	display: block;
	margin-bottom: 2px;
	line-height: 1.5;
}
.exp-time {
	font-size: 12px;
	color: #C9CDD4;
	display: block;
}
.ai-result {
	gap: 8px;
}
.ai-row {
	flex-direction: row;
	justify-content: space-between;
	padding: 8px 0;
	border-bottom: 1px solid #F2F3F5;
}
.ai-label {
	font-size: 13px;
	color: #86909C;
}
.ai-value {
	font-size: 13px;
	color: #1D2129;
	font-weight: 600;
}
.ai-suggestion {
	margin-top: 8px;
	padding: 12px;
	background: rgba(16,185,129,0.06);
	border-radius: 10px;
}
.ai-suggestion text {
	font-size: 13px;
	color: #4E5969;
	line-height: 1.6;
}
.delivery-item {
	padding: 12px 0;
	border-bottom: 1px solid #F2F3F5;
}
.delivery-item:last-child { border-bottom: none; }
.delivery-top {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 4px;
}
.delivery-job {
	font-size: 14px;
	font-weight: 600;
	color: #1D2129;
}
.delivery-status {
	font-size: 12px;
	padding: 2px 8px;
	border-radius: 6px;
	font-weight: 600;
}
.status-pending { background: rgba(245,158,11,0.1); color: #F59E0B; }
.status-viewed { background: rgba(22,93,255,0.1); color: #165DFF; }
.status-interview { background: rgba(16,185,129,0.1); color: #10B981; }
.status-accepted { background: rgba(16,185,129,0.1); color: #10B981; }
.status-rejected { background: rgba(239,68,68,0.1); color: #EF4444; }
.delivery-company {
	font-size: 13px;
	color: #86909C;
	display: block;
	margin-bottom: 2px;
}
.delivery-time {
	font-size: 12px;
	color: #C9CDD4;
	display: block;
}
</style>
