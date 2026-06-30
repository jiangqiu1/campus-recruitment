<template>
	<view class="page-wrapper">
		<scroll-view class="content-scrollable" scroll-y>
			<!-- 头部 -->
			<view class="page-header">
				<view class="header-back" @click="goBack">
					<view class="back-btn"><text>‹</text></view>
					<text class="page-title">简历管理</text>
				</view>
			</view>

			<!-- 加载中 -->
			<view v-if="loading" class="loading-state">
				<text>加载中...</text>
			</view>

			<!-- 有简历 -->
			<view v-else-if="resume" class="resume-preview">
				<!-- 简历卡片 -->
				<view class="resume-card">
					<view class="resume-header">
						<text class="resume-job-target">{{ resume.jobTarget || '未设置求职意向' }}</text>
						<text v-if="resume.isDefault === 1" class="default-badge">默认</text>
					</view>

					<!-- 教育经历 -->
					<view class="resume-section">
						<text class="section-label">📖 教育经历</text>
						<view v-if="parsedEducation.length > 0" class="edu-list">
							<view v-for="(edu, i) in parsedEducation" :key="i" class="edu-item">
								<text class="edu-school">{{ edu.school }}</text>
								<text class="edu-detail">{{ edu.major }} · {{ edu.degree }} · {{ edu.start }} ~ {{ edu.end }}</text>
							</view>
						</view>
						<text v-else class="empty-field">未填写</text>
					</view>

					<!-- 实习经历 -->
					<view class="resume-section">
						<text class="section-label">💼 实习经历</text>
						<view v-if="parsedInternship.length > 0" class="intern-list">
							<view v-for="(job, i) in parsedInternship" :key="i" class="intern-item">
								<text class="intern-company">{{ job.company }}</text>
								<text class="intern-detail">{{ job.position }} · {{ job.duration }}</text>
							</view>
						</view>
						<text v-else class="empty-field">未填写</text>
					</view>

					<!-- 技能证书 -->
					<view class="resume-section">
						<text class="section-label">🛠 技能证书</text>
						<view v-if="resume.skills" class="skill-tags">
							<view v-for="(s, i) in resume.skills.split(',').map(v => v.trim()).filter(Boolean)" :key="i" class="skill-tag">
								<text>{{ s }}</text>
							</view>
						</view>
						<text v-else class="empty-field">未填写</text>
					</view>

					<!-- 自我评价 -->
					<view class="resume-section">
						<text class="section-label">📝 自我评价</text>
						<text class="self-eval-text">{{ resume.selfEvaluation || '未填写' }}</text>
					</view>

					<!-- 更新时间 -->
					<view class="resume-footer">
						<text class="update-time">最后更新：{{ formatTime(resume.updateTime) }}</text>
					</view>
				</view>

				<!-- 操作按钮 -->
				<view class="action-buttons">
					<button class="btn-primary" @click="goEdit">编辑简历</button>
					<button class="btn-danger" @click="handleDelete">删除简历</button>
				</view>
			</view>

			<!-- 没有简历 -->
			<view v-else class="empty-state">
				<text class="empty-icon">📄</text>
				<text class="empty-text">还没有创建简历</text>
				<text class="empty-desc">点击下方按钮开始创建</text>
				<button class="btn-primary" @click="goCreate">创建简历</button>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { resumeAPI } from '@/utils/request'

const getStudentId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.id || obj.userId || null
	} catch (e) { return null }
}

const loading = ref(true)
const resume = ref(null)

const parsedEducation = computed(() => {
	if (!resume.value || !resume.value.education) return []
	try {
		const arr = typeof resume.value.education === 'string'
			? JSON.parse(resume.value.education)
			: resume.value.education
		return Array.isArray(arr) ? arr : []
	} catch { return [] }
})

const parsedInternship = computed(() => {
	if (!resume.value || !resume.value.internship) return []
	try {
		const arr = typeof resume.value.internship === 'string'
			? JSON.parse(resume.value.internship)
			: resume.value.internship
		return Array.isArray(arr) ? arr : []
	} catch { return [] }
})

onMounted(() => { loadResume() })

const loadResume = async () => {
	try {
		const res = await resumeAPI.getResume()
		resume.value = res.data || null
	} catch (e) {
		resume.value = null
	} finally {
		loading.value = false
	}
}

const goBack = () => { uni.navigateBack() }
const goEdit = () => { uni.navigateTo({ url: '/pages/student/resume-edit' }) }
const goCreate = () => { uni.navigateTo({ url: '/pages/student/resume-edit' }) }

const handleDelete = () => {
	if (!resume.value) return
	uni.showModal({
		title: '确认删除',
		content: '确定删除这份简历吗？删除后不可恢复。',
		success: async (res) => {
			if (res.confirm) {
				try {
					await resumeAPI.deleteResume(resume.value.id)
					uni.showToast({ title: '删除成功', icon: 'success' })
					resume.value = null
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
.page-header {
	background: white;
	border-bottom: 1px solid #F2F3F5;
	padding-top: 44px;
	flex-shrink: 0;
}
.header-back {
	flex-direction: row;
	align-items: center;
	padding: 12px 16px;
}
.back-btn {
	width: 36px; height: 36px;
	border-radius: 50%;
	background: #F2F3F5;
	align-items: center; justify-content: center;
	font-size: 20px; font-weight: 700; color: #165DFF;
	margin-right: 12px;
}
.page-title { font-size: 18px; font-weight: 700; color: #1D2129; flex: 1; }
.loading-state, .empty-state {
	align-items: center; padding: 80px 16px;
}
.empty-icon { font-size: 48px; margin-bottom: 16px; }
.empty-text { font-size: 16px; color: #4E5969; margin-bottom: 8px; }
.empty-desc { font-size: 13px; color: #86909C; margin-bottom: 24px; }

.resume-preview { padding: 16px; }
.resume-card { background: white; border-radius: 16px; padding: 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.resume-header { flex-direction: row; align-items: center; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #F2F3F5; }
.resume-job-target { flex: 1; font-size: 20px; font-weight: 700; color: #1D2129; }
.default-badge { font-size: 11px; color: #165DFF; background: rgba(22,93,255,0.1); padding: 2px 10px; border-radius: 10px; }

.resume-section { margin-bottom: 16px; padding-bottom: 16px; border-bottom: 1px solid #F2F3F5; }
.resume-section:last-child { border-bottom: none; }
.section-label { font-size: 14px; font-weight: 600; color: #86909C; margin-bottom: 8px; display: block; }
.empty-field { font-size: 14px; color: #C9CDD4; }

.edu-item, .intern-item { margin-bottom: 8px; }
.edu-school, .intern-company { font-size: 15px; font-weight: 500; color: #1D2129; }
.edu-detail, .intern-detail { font-size: 13px; color: #4E5969; margin-top: 2px; }

.skill-tags { flex-direction: row; flex-wrap: wrap; gap: 8px; }
.skill-tag { background: rgba(22,93,255,0.08); padding: 4px 12px; border-radius: 14px; font-size: 13px; color: #165DFF; }
.self-eval-text { font-size: 14px; color: #4E5969; line-height: 1.6; }

.resume-footer { padding-top: 12px; }
.update-time { font-size: 12px; color: #86909C; }

.action-buttons { padding: 12px 0 24px; gap: 12px; }
.btn-primary {
	width: 100%; height: 48px; border-radius: 12px;
	background: #165DFF; color: white; font-size: 16px; font-weight: 700;
	align-items: center; justify-content: center; border: none;
}
.btn-danger {
	width: 100%; height: 48px; border-radius: 12px;
	background: white; color: #EF4444; font-size: 16px; font-weight: 600;
	align-items: center; justify-content: center;
	border: 1px solid #EF4444;
}
</style>
