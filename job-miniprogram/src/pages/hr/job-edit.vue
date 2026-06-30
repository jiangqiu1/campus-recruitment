<template>
	<view class="page-wrapper">
		<view class="header-simple" style="padding:12px 16px;flex-direction:row;align-items:center;gap:12px;">
			<text style="font-size:20px;" @click="goBack">‹</text>
			<text style="font-size:18px;font-weight:700;color:white;">{{ isEdit ? '编辑岗位' : '发布岗位' }}</text>
		</view>

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
import { hrAPI, jobAPI } from '@/utils/request'

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
</script>

<style scoped>
.form-card {
	background: white;
	border-radius: 16px;
	margin: 16px;
	padding: 20px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.form-group {
	margin-bottom: 20px;
}
.form-label {
	font-size: 14px;
	font-weight: 600;
	color: #1D2129;
	margin-bottom: 8px;
	display: block;
}
.required {
	color: #EF4444;
}
.form-input {
	width: 100%;
	height: 48px;
	border: 2px solid #E2E8F0;
	border-radius: 12px;
	padding: 0 16px;
	font-size: 14px;
	background: #F8F9FC;
	color: #1D2129;
}
.form-input:focus {
	border-color: #0EA5E9;
	background: #fff;
}
.form-row {
	flex-direction: row;
	gap: 12px;
}
.form-picker {
	width: 100%;
	height: 48px;
	border: 2px solid #E2E8F0;
	border-radius: 12px;
	padding: 0 16px;
	font-size: 14px;
	background: #F8F9FC;
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
}
.picker-text {
	color: #1D2129;
	flex: 1;
}
.picker-arrow {
	color: #86909C;
	font-size: 12px;
}
.form-textarea {
	width: 100%;
	min-height: 120px;
	border: 2px solid #E2E8F0;
	border-radius: 12px;
	padding: 12px 16px;
	font-size: 14px;
	background: #F8F9FC;
	color: #1D2129;
	line-height: 1.6;
}
.form-textarea:focus {
	border-color: #0EA5E9;
	background: #fff;
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
	background: linear-gradient(135deg, #0EA5E9, #38BDF8);
	color: white;
	border: none;
	box-shadow: 0 4px 14px rgba(14,165,233,0.3);
}
.btn-outline {
	background: white;
	border: 2px solid #0EA5E9;
	color: #0EA5E9;
}
</style>
