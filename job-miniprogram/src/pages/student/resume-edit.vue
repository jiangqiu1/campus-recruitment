<template>
	<view class="page-wrapper">
		<!-- 固定头部 -->
		<view class="fixed-header">
			<view class="header-back" @click="goBack">
				<view class="back-btn"><text>‹</text></view>
				<text class="header-title">{{ resumeId ? '编辑简历' : '新建简历' }}</text>
			</view>
		</view>

		<!-- 可滚动内容区 -->
		<scroll-view class="scroll-area" scroll-y>
			<!-- 求职意向 -->
			<view class="form-section">
				<text class="form-section-title">🎯 求职意向</text>
				<view class="form-group">
					<input class="form-input" v-model="form.jobTarget" placeholder="例：Java开发工程师" />
				</view>
			</view>

			<!-- 教育经历 -->
			<view class="form-section">
				<view class="form-section-header">
					<text class="form-section-title">📖 教育经历</text>
					<text class="form-section-action" @click="addEducation">+ 添加</text>
				</view>
				<view v-for="(edu, i) in form.education" :key="i" class="json-item">
					<view class="json-item-header">
						<text class="json-item-title">教育经历 {{ i + 1 }}</text>
						<text class="json-item-remove" @click="form.education.splice(i, 1)">删除</text>
					</view>
					<input class="form-input mb-8" v-model="edu.school" placeholder="学校名称" />
					<view class="form-row">
						<input class="form-input half" v-model="edu.major" placeholder="专业" />
						<input class="form-input half" v-model="edu.degree" placeholder="学历" />
					</view>
					<view class="form-row">
						<input class="form-input half" v-model="edu.start" placeholder="入学时间 (2020-09)" />
						<input class="form-input half" v-model="edu.end" placeholder="毕业时间 (2024-06)" />
					</view>
				</view>
				<text v-if="form.education.length === 0" class="empty-hint">点击上方「+ 添加」添加教育经历</text>
			</view>

			<!-- 实习经历 -->
			<view class="form-section">
				<view class="form-section-header">
					<text class="form-section-title">💼 实习经历</text>
					<text class="form-section-action" @click="addInternship">+ 添加</text>
				</view>
				<view v-for="(job, i) in form.internship" :key="i" class="json-item">
					<view class="json-item-header">
						<text class="json-item-title">实习经历 {{ i + 1 }}</text>
						<text class="json-item-remove" @click="form.internship.splice(i, 1)">删除</text>
					</view>
					<input class="form-input mb-8" v-model="job.company" placeholder="公司名称" />
					<input class="form-input mb-8" v-model="job.position" placeholder="职位" />
					<input class="form-input" v-model="job.duration" placeholder="时间段 (2023-07至2023-12)" />
				</view>
				<text v-if="form.internship.length === 0" class="empty-hint">点击上方「+ 添加」添加实习经历</text>
			</view>

			<!-- 技能证书 -->
			<view class="form-section">
				<text class="form-section-title">🛠 技能证书</text>
				<textarea class="form-textarea" v-model="form.skills" placeholder="用逗号分隔多个技能，如：Java, Spring Boot, MySQL, Redis" />
			</view>

			<!-- 自我评价 -->
			<view class="form-section">
				<text class="form-section-title">📝 自我评价</text>
				<textarea class="form-textarea" v-model="form.selfEvaluation" placeholder="简要描述你的优势和求职意向，建议100-300字" />
			</view>

			<!-- 占位，防键盘遮挡 -->
			<view style="height: 100px;"></view>
		</scroll-view>

		<!-- 固定底部保存 -->
		<view class="bottom-bar">
			<button class="save-btn" :loading="saving" @click="handleSave">保存</button>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { resumeAPI } from '@/utils/request'

const getStudentId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.id || obj.userId || null
	} catch (e) { return null }
}

const resumeId = ref(null)
const saving = ref(false)

const form = ref({
	jobTarget: '',
	education: [],
	internship: [],
	skills: '',
	selfEvaluation: ''
})

onMounted(() => {
	const pages = getCurrentPages()
	const currentPage = pages[pages.length - 1]
	const options = currentPage?.$page?.options || currentPage?.options || {}

	// Also try to load existing resume data upfront
	loadExisting()
})

const loadExisting = async () => {
	try {
		const res = await resumeAPI.getResume()
		const data = res.data
		if (data) {
			resumeId.value = data.id
			form.value = {
				jobTarget: data.jobTarget || '',
				education: parseJsonArray(data.education),
				internship: parseJsonArray(data.internship),
				skills: data.skills || '',
				selfEvaluation: data.selfEvaluation || ''
			}
		}
	} catch (e) {
		// No existing resume, that's OK - creating new
	}
}

const parseJsonArray = (val) => {
	if (!val) return []
	try {
		const arr = typeof val === 'string' ? JSON.parse(val) : val
		return Array.isArray(arr) ? arr : []
	} catch { return [] }
}

const addEducation = () => {
	form.value.education.push({ school: '', major: '', degree: '', start: '', end: '' })
}

const addInternship = () => {
	form.value.internship.push({ company: '', position: '', duration: '' })
}

const goBack = () => { uni.navigateBack() }

const handleSave = async () => {
	if (!form.value.jobTarget.trim()) {
		uni.showToast({ title: '请填写求职意向', icon: 'none' })
		return
	}

	saving.value = true
	try {
		const sid = getStudentId()
		const payload = {
			jobTarget: form.value.jobTarget,
			education: JSON.stringify(form.value.education),
			internship: JSON.stringify(form.value.internship),
			skills: form.value.skills,
			selfEvaluation: form.value.selfEvaluation
		}
		if (resumeId.value) {
			payload.id = resumeId.value
		}
		await resumeAPI.createOrUpdateResume(sid, payload)
		uni.showToast({ title: '保存成功', icon: 'success' })
		setTimeout(() => { uni.navigateBack() }, 500)
	} catch (e) {
		uni.showToast({ title: '保存失败，请重试', icon: 'none' })
	} finally {
		saving.value = false
	}
}
</script>

<style scoped>
.fixed-header {
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
.header-title { font-size: 18px; font-weight: 700; color: #1D2129; flex: 1; }
.scroll-area { flex: 1; overflow-y: auto; padding-bottom: 80px; }
.form-section {
	background: white; border-radius: 12px;
	margin: 12px 16px; padding: 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.form-section-header {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 12px;
}
.form-section-title { font-size: 15px; font-weight: 600; color: #1D2129; }
.form-section-action { font-size: 14px; color: #165DFF; font-weight: 500; padding: 4px; }
.form-group { margin-bottom: 14px; }
.form-input {
	width: 100%; height: 44px;
	border: 1px solid #E2E8F0; border-radius: 8px;
	padding: 0 12px; font-size: 15px; background: #F8F9FC;
	box-sizing: border-box;
}
.form-textarea {
	width: 100%; min-height: 100px;
	border: 1px solid #E2E8F0; border-radius: 8px;
	padding: 12px; font-size: 15px; background: #F8F9FC;
	box-sizing: border-box;
}
.form-row { flex-direction: row; gap: 12px; }
.form-input.half { flex: 1; }
.mb-8 { margin-bottom: 8px; }
.empty-hint { font-size: 13px; color: #C9CDD4; }

.json-item {
	background: #F8F9FC; border-radius: 8px;
	padding: 12px; margin-bottom: 12px;
}
.json-item-header {
	flex-direction: row; justify-content: space-between;
	align-items: center; margin-bottom: 8px;
}
.json-item-title { font-size: 14px; font-weight: 500; color: #4E5969; }
.json-item-remove { font-size: 13px; color: #EF4444; padding: 4px; }

.bottom-bar {
	position: fixed; bottom: 0; left: 0; right: 0;
	background: white; padding: 12px 16px 24px;
	border-top: 1px solid #F2F3F5;
	box-shadow: 0 -2px 10px rgba(0,0,0,0.06);
}
.save-btn {
	width: 100%; height: 48px; border-radius: 12px;
	background: #165DFF; color: white; font-size: 16px; font-weight: 700;
	align-items: center; justify-content: center; border: none;
}
.save-btn:active { opacity: 0.9; }
</style>
