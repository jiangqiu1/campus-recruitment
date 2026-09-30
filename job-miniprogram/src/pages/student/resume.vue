<template>
	<view class="page-wrapper">
		<NavBar title="我的简历" />
		<scroll-view class="content-scrollable" scroll-y :scroll-into-view="scrollTarget">
			<LoadingState v-if="loading" type="skeleton" :rows="5" />
			<template v-else-if="resume">
				<view class="resume-top-bar">
					<ScoreCircle :score="completeness" :size="56" />
					<view class="top-bar-right">
						<text class="completeness-title">简历完整度 {{ completeness }}%</text>
						<view class="ai-optimize" @click="goAIReview">
							<uni-icons type="compose" size="14" color="#7C3AED" />
							<text class="optimize-text optimize-text--purple">{{ reviewing ? 'AI 诊断中...' : 'AI 诊断' }}</text>
							<uni-icons v-if="!reviewing" type="arrowright" size="14" color="#7C3AED" />
						</view>
					</view>
				</view>
				<view class="resume-card">
					<view class="resume-header">
						<view>
							<text class="resume-name">{{ resume.name || resume.realName || userInfo.realName || '未设置姓名' }}</text>
							<text class="resume-contact">{{ resume.phone || userInfo.phone || '' }} {{ (resume.email || userInfo.email) ? '| ' + (resume.email || userInfo.email) : '' }}</text>
						</view>
						<text v-if="resume.isDefault" class="default-badge">默认</text>
					</view>
					<view class="section-block">
						<text class="section-label">求职意向</text>
						<text class="section-value">{{ resume.jobTarget || '未设置' }}</text>
					</view>
					<view class="section-block">
						<text class="section-label">教育经历</text>
						<view v-if="parsedEducation.length" class="exp-list">
							<view v-for="(edu, i) in parsedEducation" :key="i" class="exp-item">
								<text class="exp-title">{{ edu.school }}</text>
								<text class="exp-sub">{{ edu.major }} · {{ edu.degree }} · {{ edu.start }} ~ {{ edu.end }}</text>
							</view>
						</view>
						<text v-else class="value-empty">未填写</text>
					</view>
					<view class="section-block">
						<text class="section-label">实习经历</text>
						<view v-if="parsedInternship.length" class="exp-list">
							<view v-for="(job, i) in parsedInternship" :key="i" class="exp-item">
								<text class="exp-title">{{ job.company }}</text>
								<text class="exp-sub">{{ job.position }} · {{ job.duration }}</text>
							</view>
						</view>
						<text v-else class="value-empty">未填写</text>
					</view>
					<view class="section-block">
						<text class="section-label">项目经历</text>
					<view v-if="parsedProject.length" class="exp-list">
						<view v-for="(proj, i) in parsedProject" :key="i" class="exp-item">
							<text class="exp-title">{{ proj.name }}</text>
							<text class="exp-sub">{{ proj.role || '' }}{{ proj.duration ? ' · ' + proj.duration : '' }}</text>
							<text v-if="proj.description" class="section-value" style="margin-top:6px;font-size:13px;color:#4E5969;line-height:1.6;">{{ proj.description }}</text>
						</view>
					</view>
						<text v-else class="value-empty">未填写</text>
					</view>
					<view class="section-block">
						<text class="section-label">技能证书</text>
						<view v-if="resume.skills" class="skill-tags">
							<text v-for="(s, i) in resume.skills.split(/[,，]/).map(v => v.trim()).filter(Boolean)" :key="i" class="skill-tag">{{ s }}</text>
						</view>
						<text v-else class="value-empty">未填写</text>
					</view>
					<view class="section-block">
						<text class="section-label">自我评价</text>
						<text class="section-value">{{ resume.selfEvaluation || '未填写' }}</text>
					</view>
					<view class="resume-footer">
						<text class="update-time">最后更新：{{ formatTime(resume.updateTime) }}</text>
					</view>
				</view>

				<!-- AI 简历分析结果（由教师触发分析后保存，学生可查看） -->
				<view class="section-card" v-if="aiAnalysisResult" id="ai-analysis-section">
					<view class="section-title-row">
						<text class="section-title">AI 简历分析</text>
						<text class="ai-badge">{{ analysisBadge }}</text>
					</view>
					<view class="ai-result">
						<view class="ai-score-row">
							<text>综合评分</text>
							<text class="score-num">{{ aiAnalysisResult.overallScore || '--' }}分</text>
						</view>
						<view class="ai-section" v-if="aiAnalysisResult.strengths && aiAnalysisResult.strengths.length">
							<text class="ai-subtitle">优势</text>
							<text v-for="(s, i) in aiAnalysisResult.strengths" :key="i" class="ai-item ai-item--green">{{ s }}</text>
						</view>
						<view class="ai-section" v-if="aiAnalysisResult.weaknesses && aiAnalysisResult.weaknesses.length">
							<text class="ai-subtitle">不足</text>
							<text v-for="(w, i) in aiAnalysisResult.weaknesses" :key="i" class="ai-item ai-item--red">{{ w }}</text>
						</view>
						<view class="ai-section" v-if="aiAnalysisResult.suggestions && aiAnalysisResult.suggestions.length">
							<text class="ai-subtitle">改进建议</text>
							<text v-for="(sg, i) in aiAnalysisResult.suggestions" :key="i" class="ai-item ai-item--blue">{{ sg }}</text>
						</view>
						<view class="ai-section" v-if="aiAnalysisResult.missingFields && aiAnalysisResult.missingFields.length">
							<text class="ai-subtitle">缺失字段</text>
							<text class="ai-item ai-item--amber">{{ aiAnalysisResult.missingFields.join('、') }}</text>
						</view>
						<view class="ai-section" v-if="aiAnalysisResult.recommendedSkills && aiAnalysisResult.recommendedSkills.length">
							<text class="ai-subtitle">推荐补充技能</text>
							<view class="tag-container">
								<text v-for="(sk, i) in aiAnalysisResult.recommendedSkills" :key="i" class="skill-tag" style="background:rgba(139,92,246,0.1);color:#7C3AED;">{{ sk }}</text>
							</view>
						</view>
					</view>
				</view>

				<view class="action-buttons">
					<button class="btn-primary" @click="goEdit">编辑简历</button>
					<button class="btn-outline" @click="handlePreview">预览简历</button>
					<button class="btn-text-danger" @click="handleDelete">删除简历</button>
				</view>
			</template>
			<template v-else>
				<EmptyState icon="list" title="还没有创建简历" desc="点击下方按钮开始创建" btn-text="创建简历" @action="goCreate" />
			</template>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { resumeAPI, aiAssistantAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import ScoreCircle from '@/components/ScoreCircle.vue'

const loading = ref(true)
const resume = ref(null)
const completeness = ref(0)
const userInfo = ref({})
const aiAnalysisResult = ref(null)
const reviewing = ref(false)

const parsedEducation = computed(() => {
	if (!resume.value?.education) return []
	try {
		const arr = typeof resume.value.education === 'string' ? JSON.parse(resume.value.education) : resume.value.education
		return Array.isArray(arr) ? arr : []
	} catch { return [] }
})
const parsedInternship = computed(() => {
	if (!resume.value?.internship) return []
	try {
		const arr = typeof resume.value.internship === 'string' ? JSON.parse(resume.value.internship) : resume.value.internship
		return Array.isArray(arr) ? arr : []
	} catch { return []
	}
})
const parsedProject = computed(() => {
	if (!resume.value?.project) return []
	try {
		const arr = typeof resume.value.project === 'string' ? JSON.parse(resume.value.project) : resume.value.project
		return Array.isArray(arr) ? arr : []
	} catch { return [] }
})

onMounted(() => {
	try {
		const stored = uni.getStorageSync('userInfo')
		if (stored) userInfo.value = JSON.parse(stored)
	} catch (e) {}
	loadResume()
	uni.$on('resume-updated', loadResume)
})
onUnmounted(() => {
	uni.$off('resume-updated', loadResume)
})

const loadResume = async () => {
	try {
		const res = await resumeAPI.getResume()
		resume.value = res.data || null
		if (resume.value) {
			calcCompleteness()
			// 解析 AI 分析结果（由教师触发分析后保存在数据库）
			if (resume.value.aiAnalysis) {
				try {
					aiAnalysisResult.value = typeof resume.value.aiAnalysis === 'string'
						? JSON.parse(resume.value.aiAnalysis)
						: resume.value.aiAnalysis
				} catch (e) {
					aiAnalysisResult.value = null
				}
			} else {
				aiAnalysisResult.value = null
			}
		}
	} catch (e) { resume.value = null }
	finally { loading.value = false }
}

const calcCompleteness = () => {
	const r = resume.value
	if (!r) return
	// Resume 实体没有 name/phone/email 字段，从 userInfo（localStorage）获取
	const { realName, phone, email } = userInfo.value
	const fields = [
		realName || r.realName || r.name,
		phone,
		email,
		r.jobTarget,
		r.education,
		r.internship,
		r.project,
		r.skills,
		r.selfEvaluation
	]
	const filled = fields.filter(Boolean).length
	completeness.value = Math.round((filled / fields.length) * 100)
}

const goEdit = () => uni.navigateTo({ url: '/pages/student/resume-edit' })
const goCreate = () => uni.navigateTo({ url: '/pages/student/resume-edit' })
const scrollTarget = ref('')

// 分析卡徽标：显示最近一次分析时间（无时间戳的旧数据显示通用文案）
const analysisBadge = computed(() => {
	const t = aiAnalysisResult.value?.analyzedAt
	if (!t) return 'AI 评估'
	const d = new Date(t)
	if (isNaN(d.getTime())) return 'AI 评估'
	const hh = String(d.getHours()).padStart(2, '0')
	const mm = String(d.getMinutes()).padStart(2, '0')
	return `分析于 ${d.getMonth() + 1}-${String(d.getDate()).padStart(2, '0')} ${hh}:${mm}`
})

// AI 诊断：分析自己的简历（简历未变更时后端直接返回缓存结果）
const goAIReview = async () => {
	if (reviewing.value) return
	reviewing.value = true
	uni.showLoading({ title: 'AI 诊断中...', mask: true })
	try {
		const res = await aiAssistantAPI.resumeReview(false)
		await loadResume()
		uni.hideLoading()
		scrollTarget.value = 'ai-analysis-section'
		setTimeout(() => { scrollTarget.value = '' }, 500)
		uni.showToast({ title: res.message || '诊断完成', icon: 'none' })
	} catch (e) {
		uni.hideLoading()
		uni.showToast({ title: (e && e.message) || '诊断失败，请稍后重试', icon: 'none' })
	} finally {
		reviewing.value = false
	}
}
const handlePreview = () => uni.showToast({ title: '预览简历', icon: 'none' })

const handleDelete = () => {
	uni.showModal({
		title: '确认删除',
		content: '确定删除这份简历吗？删除后不可恢复。',
		success: async (res) => {
			if (res.confirm) {
				try {
					await resumeAPI.deleteResume(resume.value.id)
					uni.showToast({ title: '删除成功', icon: 'success' })
					resume.value = null
					uni.$emit('resume-updated')
				} catch (e) {
					uni.showToast({ title: '删除失败', icon: 'none' })
				}
			}
		}
	})
}

const formatTime = (time) => {
	if (!time) return ''
	const d = new Date(time)
	return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}
</script>

<style scoped>
.resume-top-bar {
	flex-direction: row;
	align-items: center;
	gap: 16px;
	padding: 16px;
	background: #FFFFFF;
	margin-bottom: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.top-bar-right {
	flex: 1;
	gap: 6px;
}
.completeness-title {
	font-size: 15px;
	font-weight: 600;
	color: #1D2129;
}
.ai-optimize {
	flex-direction: row;
	align-items: center;
	gap: 4px;
}
.optimize-text {
	font-size: 13px;
	color: #0EA5E9;
	font-weight: 500;
}
.optimize-text--purple {
	color: #7C3AED;
}
.resume-card {
	background: #FFFFFF;
	border-radius: 12px;
	padding: 20px;
	margin: 0 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.resume-header {
	flex-direction: row;
	align-items: flex-start;
	justify-content: space-between;
	margin-bottom: 16px;
	padding-bottom: 16px;
	border-bottom: 0.5px solid #F2F3F5;
}
.resume-name {
	font-size: 20px;
	font-weight: 700;
	color: #1D2129;
}
.resume-contact {
	font-size: 13px;
	color: #86909C;
	margin-top: 4px;
}
.default-badge {
	font-size: 11px;
	color: #165DFF;
	background: rgba(22,93,255,0.08);
	padding: 2px 10px;
	border-radius: 4px;
	font-weight: 500;
}
.section-block {
	margin-bottom: 16px;
	padding-bottom: 16px;
	border-bottom: 0.5px solid #F2F3F5;
}
.section-block:last-child { border-bottom: none; padding-bottom: 0; }
.section-label {
	font-size: 14px;
	font-weight: 600;
	color: #86909C;
	margin-bottom: 8px;
	display: block;
}
.section-value {
	font-size: 14px;
	color: #4E5969;
	line-height: 1.6;
}
.value-empty {
	font-size: 14px;
	color: #C9CDD4;
}
.exp-list { gap: 10px; }
.exp-item { gap: 2px; }
.exp-title { font-size: 15px; font-weight: 500; color: #1D2129; }
.exp-sub { font-size: 13px; color: #86909C; }
.skill-tags { flex-direction: row; flex-wrap: wrap; gap: 6px; }
.skill-tag {
	font-size: 13px;
	color: #165DFF;
	background: rgba(22,93,255,0.08);
	padding: 4px 10px;
	border-radius: 4px;
}
.resume-footer { padding-top: 12px; }
.update-time { font-size: 12px; color: #C9CDD4; }
.action-buttons { padding: 16px; gap: 10px; }
.btn-primary, .btn-outline, .btn-text-danger {
	width: 100%;
	height: 46px;
	border-radius: 12px;
	font-size: 15px;
	font-weight: 600;
	align-items: center;
	justify-content: center;
	border: none;
}
.btn-primary { background: #165DFF; color: #FFFFFF; }
.btn-outline { background: #FFFFFF; border: 1px solid #E5E6EB; color: #4E5969; }
.btn-text-danger { background: transparent; color: #F53F3F; height: 40px; }
.btn-primary:active { opacity: 0.85; }
.btn-outline:active { background: #F7F8FA; }

/* AI 简历分析结果卡片 */
.section-card {
	background: #FFFFFF;
	border-radius: 12px;
	padding: 16px;
	margin: 0 16px 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.section-title-row {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 12px;
}
.section-title {
	font-size: 15px;
	font-weight: 700;
	color: #1D2129;
}
.ai-badge {
	font-size: 11px;
	color: #8B5CF6;
	background: rgba(139,92,246,0.1);
	padding: 2px 8px;
	border-radius: 8px;
	font-weight: 500;
}
.ai-result { gap: 8px; }
.ai-score-row {
	flex-direction: row;
	justify-content: space-between;
	padding: 8px 0;
	border-bottom: 0.5px solid #F2F3F5;
}
.ai-score-row text:first-child { font-size: 13px; color: #86909C; }
.score-num { font-size: 16px; color: #165DFF; font-weight: 700; }
.ai-section { margin-top: 10px; }
.ai-subtitle { font-size: 13px; font-weight: 600; color: #4E5969; margin-bottom: 6px; display: block; }
.ai-item { display: block; font-size: 13px; padding: 6px 10px; border-radius: 6px; margin-bottom: 4px; line-height: 1.5; }
.ai-item--green { background: rgba(0,180,42,0.06); color: #00B42A; }
.ai-item--red { background: rgba(239,68,68,0.06); color: #EF4444; }
.ai-item--blue { background: rgba(22,93,255,0.06); color: #165DFF; }
.ai-item--amber { background: rgba(245,158,11,0.08); color: #D97706; }
.tag-container { flex-direction: row; flex-wrap: wrap; gap: 8px; }
</style>
