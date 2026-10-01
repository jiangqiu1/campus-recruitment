<template>
	<view class="page-wrapper">
		<NavBar :title="isEdit ? '编辑岗位' : '发布岗位'" showBack @back="goBack" />

		<scroll-view class="content-scrollable" scroll-y>
			<view class="form-card">
				<view class="form-group">
					<text class="form-label">岗位标题 <text class="required">*</text></text>
					<input class="form-input" v-model="form.title" placeholder="请输入岗位名称，如：前端开发实习生" />
				</view>

				<view class="form-row">
					<view class="form-group" style="flex:1;">
						<text class="form-label">薪资范围 <text class="required">*</text></text>
						<input class="form-input" v-model="form.salaryText" placeholder="如：4K-6K" />
					</view>
					<view class="form-group" style="flex:1;">
						<text class="form-label">工作地点 <text class="required">*</text></text>
						<input class="form-input" v-model="form.location" placeholder="如：广州" />
					</view>
				</view>

				<view class="form-row">
					<view class="form-group" style="flex:1;">
						<text class="form-label">经验要求</text>
						<picker class="form-picker" :value="expIndex" :range="expOptions" @change="onExpChange">
							<text class="picker-text">{{ expOptions[expIndex] }}</text>
							<text class="picker-arrow">▼</text>
						</picker>
					</view>
					<view class="form-group" style="flex:1;">
						<text class="form-label">学历要求</text>
						<picker class="form-picker" :value="eduIndex" :range="eduOptions" @change="onEduChange">
							<text class="picker-text">{{ eduOptions[eduIndex] }}</text>
							<text class="picker-arrow">▼</text>
						</picker>
					</view>
				</view>

				<view class="form-group">
					<text class="form-label">职位描述 <text class="required">*</text></text>
					<textarea class="form-textarea" v-model="form.description" placeholder="请详细描述岗位职责" />
				</view>

				<view class="form-group">
					<text class="form-label">任职要求 <text class="required">*</text></text>
					<textarea class="form-textarea" v-model="form.requirements" placeholder="请描述任职要求，如：熟练使用Vue.js" />
				</view>
			</view>

			<view class="form-actions">
				<button class="submit-btn btn-primary" @click="handleSave(true)">发布岗位</button>
				<button class="submit-btn btn-outline" @click="handleSave(false)">存为草稿</button>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { hrAPI, jobAPI, aiParseAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'

const isEdit = ref(false)
const jobId = ref(null)

const form = ref({
	title: '',
	salaryText: '',
	location: '',
	experience: '',
	education: '',
	description: '',
	requirements: ''
})

const expOptions = ['经验不限', '应届', '1年以下', '1-3年', '3-5年', '5年以上']
const eduOptions = ['学历不限', '大专及以上', '本科及以上', '硕士及以上']
const expIndex = ref(0)
const eduIndex = ref(0)

// 后端 Job → 前端 form 映射
const jobToForm = (job) => ({
	title: job.title || '',
	salaryText: job.salaryRange || '',
	location: job.location || '',
	experience: job.experience || '',
	education: job.education || '',
	description: job.description || '',
	requirements: job.requirement || ''  // 后端字段为 requirement（单数）
})

const onExpChange = (e) => {
	expIndex.value = e.detail.value
	form.value.experience = expOptions[expIndex.value]
}

const onEduChange = (e) => {
	eduIndex.value = e.detail.value
	form.value.education = eduOptions[eduIndex.value]
}

onMounted(async () => {
	const pages = getCurrentPages()
	const currentPage = pages[pages.length - 1]
	const opts = currentPage.options
	if (opts && opts.id) {
		isEdit.value = true
		jobId.value = Number(opts.id)
		await loadJobDetail(jobId.value)
	}
	// 从 AI 解析结果填充表单
	if (opts && opts.aiParsed) {
		try {
			const parsed = uni.getStorageSync('ai_parsed_job')
			if (parsed) {
				const data = typeof parsed === 'string' ? JSON.parse(parsed) : parsed
				fillFromAiParse(data)
			}
			uni.removeStorageSync('ai_parsed_job')
		} catch (e) {}
	}
})

const loadJobDetail = async (id) => {
	try {
		const res = await jobAPI.getJobDetail(id)
		const job = res.data
		if (job) {
			form.value = jobToForm(job)
			const ei = expOptions.indexOf(job.experience)
			if (ei > -1) expIndex.value = ei
			const edi = eduOptions.indexOf(job.education)
			if (edi > -1) eduIndex.value = edi
		}
	} catch (e) {
		console.error('加载岗位详情失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

const handleSave = async (publish) => {
	if (!form.value.title || !form.value.salaryText || !form.value.location || !form.value.description || !form.value.requirements) {
		uni.showToast({ title: '请填写完整信息', icon: 'none' })
		return
	}

	const payload = {
		title: form.value.title,
		salaryRange: form.value.salaryText,
		location: form.value.location,
		education: form.value.education,
		description: form.value.description,
		requirement: form.value.requirements,  // 前端 plural → 后端 singular
		status: publish ? 1 : 0  // 1=已发布, 0=草稿
	}

	try {
		if (isEdit.value && jobId.value) {
			await hrAPI.updateJob(jobId.value, payload)
		} else {
			const cId = getCompanyId()
			await hrAPI.createJob({ ...payload, companyId: cId })
		}
		uni.showToast({ title: publish ? '发布成功' : '已保存草稿', icon: 'success' })
		setTimeout(() => {
			uni.navigateBack()
		}, 500)
	} catch (e) {
		uni.showToast({ title: '保存失败', icon: 'none' })
	}
}

const getCompanyId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.companyId || obj.id || null
	} catch (e) { return null }
}

const goBack = () => {
	uni.navigateBack()
}

// 从 AI 解析结果填充表单
const fillFromAiParse = (data) => {
	if (!data) return
	if (data.title) form.value.title = data.title
	if (data.salaryRange) form.value.salaryText = data.salaryRange
	if (data.location) form.value.location = data.location
	if (data.education) {
		form.value.education = data.education
		const edi = eduOptions.indexOf(data.education)
		if (edi > -1) eduIndex.value = edi
	}
	if (data.experience) {
		form.value.experience = data.experience
		const ei = expOptions.indexOf(data.experience)
		if (ei > -1) expIndex.value = ei
	}
	if (data.description) form.value.description = data.description
	if (data.requirements) form.value.requirements = data.requirements
	uni.showToast({ title: 'AI 填写完成，请核对', icon: 'success' })
}
</script>

<style scoped lang="scss">
.page-wrapper {
	min-height: 100vh;
	background: $uni-bg-color-page;
	display: flex;
	flex-direction: column;
}
.content-scrollable {
	flex: 1;
	height: 0;
}
.form-card {
	background: white;
	border-radius: 16px;
	margin: 16px;
	padding: 20px;
	box-shadow: $uni-shadow-card;
	overflow: hidden;
}
.form-group {
	margin-bottom: 20px;
}
.form-label {
	font-size: 14px;
	font-weight: 600;
	color: $uni-text-color-title;
	margin-bottom: 8px;
	display: block;
	padding-left: 10px;
	border-left: 3px solid $uni-color-primary;
}
.required {
	color: $uni-color-error;
}
.form-input {
	width: 100%;
	height: 48px;
	border: 2px solid $uni-border-color;
	border-radius: 12px;
	padding: 0 16px;
	font-size: 14px;
	background: $uni-bg-color-hover;
	color: $uni-text-color-title;
	box-sizing: border-box;
}
.form-input:focus {
	border-color: $uni-color-primary;
	background: $uni-bg-color;
}
.form-row {
	flex-direction: row;
	gap: 12px;
}
.form-picker {
	width: 100%;
	height: 48px;
	border: 2px solid $uni-border-color;
	border-radius: 12px;
	padding: 0 16px;
	font-size: 14px;
	background: $uni-bg-color-hover;
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
	box-sizing: border-box;
}
.picker-text {
	color: $uni-text-color-title;
	flex: 1;
}
.picker-arrow {
	color: $uni-text-color-secondary;
	font-size: 12px;
}
.form-textarea {
	width: 100%;
	min-height: 120px;
	border: 2px solid $uni-border-color;
	border-radius: 12px;
	padding: 12px 16px;
	font-size: 14px;
	background: $uni-bg-color-hover;
	color: $uni-text-color-title;
	line-height: 1.6;
	box-sizing: border-box;
}
.form-textarea:focus {
	border-color: $uni-color-primary;
	background: $uni-bg-color;
}
.form-actions {
	padding: 0 16px 24px;
	gap: 12px;
}
.submit-btn {
	width: 100%;
	padding: 14px;
	border-radius: 12px;
	font-size: 16px;
	font-weight: 700;
	align-items: center;
	justify-content: center;
}
.btn-primary {
	background: $uni-gradient-primary;
	color: white;
	border: none;
	box-shadow: 0 4px 14px $uni-color-primary-light;
}
.btn-outline {
	background: $uni-bg-color-page;
	border: none;
	color: $uni-text-color-secondary;
	font-weight: 500;
}
</style>
