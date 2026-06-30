<template>
	<view class="page-wrapper">
		<view class="header-simple" style="padding:12px 16px;flex-direction:row;align-items:center;gap:12px;">
			<text style="font-size:20px;" @click="goBack">‹</text>
			<text style="font-size:18px;font-weight:700;color:white;">{{ isEdit ? '编辑岗位' : '新建岗位' }}</text>
		</view>
		<scroll-view class="content-scrollable" scroll-y>
			<view class="form-container">
				<view class="input-group">
					<text class="input-label">岗位名称 <text style="color:#EF4444;">*</text></text>
					<input class="input-field" v-model="form.title" placeholder="请输入岗位名称" />
				</view>
				<view class="input-group">
					<text class="input-label">招聘企业</text>
					<view class="picker-box" @click="showCompanyPicker = true">
						<text class="picker-text" :class="{ placeholder: !form.companyId }">{{ form.companyName || '选择招聘企业' }}</text>
						<text class="picker-arrow">▼</text>
					</view>
				</view>
				<view class="input-group">
					<text class="input-label">工作地点 <text style="color:#EF4444;">*</text></text>
					<input class="input-field" v-model="form.location" placeholder="如：广州" />
				</view>
				<view class="row-inputs">
					<view class="input-group" style="flex:1;">
						<text class="input-label">最低学历</text>
						<view class="picker-box" @click="showEduPicker = true">
							<text class="picker-text" :class="{ placeholder: !form.education }">{{ form.education || '请选择' }}</text>
							<text class="picker-arrow">▼</text>
						</view>
					</view>
					<view class="input-group" style="flex:1;">
						<text class="input-label">经验要求</text>
						<view class="picker-box" @click="showExpPicker = true">
							<text class="picker-text" :class="{ placeholder: !form.experience }">{{ form.experience || '请选择' }}</text>
							<text class="picker-arrow">▼</text>
						</view>
					</view>
				</view>
				<view class="row-inputs">
					<view class="input-group" style="flex:1;">
						<text class="input-label">最低薪资 (K)</text>
						<input class="input-field" v-model="form.salaryMin" type="number" placeholder="如：4" />
					</view>
					<view class="input-group" style="flex:1;">
						<text class="input-label">最高薪资 (K)</text>
						<input class="input-field" v-model="form.salaryMax" type="number" placeholder="如：8" />
					</view>
				</view>
				<view class="input-group">
					<text class="input-label">岗位描述 <text style="color:#EF4444;">*</text></text>
					<textarea class="input-textarea" v-model="form.description" placeholder="请输入岗位描述、职责要求等" />
				</view>
				<view class="input-group">
					<text class="input-label">岗位要求</text>
					<textarea class="input-textarea" v-model="form.requirements" placeholder="请输入任职要求" />
				</view>
				<view class="form-actions">
					<button class="btn-cancel" @click="goBack">取消</button>
					<button class="btn-save" :loading="saving" @click="handleSave">
						<text v-if="!saving">{{ isEdit ? '保存修改' : '创建岗位' }}</text>
						<text v-else>保存中...</text>
					</button>
				</view>
			</view>
		</scroll-view>
	</view>

	<!-- 企业选择弹窗 -->
	<view v-if="showCompanyPicker" class="modal-overlay" @click="showCompanyPicker = false">
		<view class="modal-content" @click.stop>
			<view class="modal-header">
				<text class="modal-title">选择企业</text>
				<text class="modal-close" @click="showCompanyPicker = false">✕</text>
			</view>
			<scroll-view class="modal-list" scroll-y>
				<view v-for="(c, i) in companies" :key="i" class="modal-item" :class="{ selected: form.companyId === c.id }" @click="selectCompany(c)">
					<text class="modal-item-text">{{ c.name }}</text>
					<text class="modal-check" v-if="form.companyId === c.id">✓</text>
				</view>
				<view v-if="!companies.length" class="empty-state">
					<text>暂无企业数据</text>
				</view>
			</scroll-view>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { teacherAPI, jobAPI } from '@/utils/request'

const isEdit = ref(false)
const saving = ref(false)
const showCompanyPicker = ref(false)
const showEduPicker = ref(false)
const showExpPicker = ref(false)
const companies = ref([])

const form = ref({
	title: '',
	companyId: '',
	companyName: '',
	location: '',
	education: '',
	experience: '',
	salaryMin: '',
	salaryMax: '',
	description: '',
	requirements: ''
})

// 后端 Job ↔ 前端 form 字段映射
const jobToForm = (job) => {
	// 解析 salaryRange "4K-6K" → salaryMin/salaryMax
	let min = '', max = ''
	if (job.salaryRange) {
		const parts = job.salaryRange.match(/([\d.]+)/g)
		if (parts) {
			min = parts[0] || ''
			max = parts[1] || ''
		}
	}
	return {
		id: job.id,
		title: job.title || '',
		companyId: job.companyId || '',
		companyName: job.companyName || job._companyName || '',
		location: job.location || '',
		education: job.education || '',
		experience: job.experience || '',
		salaryMin: min,
		salaryMax: max,
		description: job.description || '',
		requirements: job.requirement || ''  // 后端字段为 requirement（单数）
	}
}

const formToJob = (form) => {
	const salaryText = form.salaryMin && form.salaryMax
		? form.salaryMin + 'K-' + form.salaryMax + 'K'
		: ''
	return {
		title: form.title,
		companyId: form.companyId,
		location: form.location,
		education: form.education,
		description: form.description,
		requirement: form.requirements,  // 前端 plural → 后端 singular
		salaryRange: salaryText,
	}
}

onMounted(async () => {
	const pages = getCurrentPages()
	const currentPage = pages[pages.length - 1]
	const jobId = currentPage.options ? currentPage.options.id : null
	if (jobId) {
		isEdit.value = true
		loadJob(jobId)
	}
	loadCompanies()
})

const loadJob = async (jobId) => {
	try {
		const res = await jobAPI.getJobDetail(jobId)
		const job = res.data
		if (job) {
			const mapped = jobToForm(job)
			// 异步加载公司名
			if (job.companyId) {
				try {
					const cRes = await teacherAPI.getCompanyName(job.companyId)
					mapped.companyName = cRes?.data?.name || '企业' + job.companyId
				} catch (e) { }
			}
			form.value = mapped
		}
	} catch (e) {
		console.error('加载岗位详情失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

const loadCompanies = async () => {
	try {
		const res = await teacherAPI.getCompanies()
		companies.value = res.data || []
	} catch (e) {
		console.error('加载企业列表失败', e)
		uni.showToast({ title: '加载企业列表失败', icon: 'none' })
	}
}

const selectCompany = (c) => {
	form.value.companyId = c.id
	form.value.companyName = c.name
	showCompanyPicker.value = false
}

const handleSave = async () => {
	if (!form.value.title) {
		uni.showToast({ title: '请输入岗位名称', icon: 'none' })
		return
	}
	saving.value = true
	try {
		const payload = formToJob(form.value)
		if (isEdit.value) {
			await teacherAPI.updateJob(form.value.id, payload)
			uni.showToast({ title: '保存成功', icon: 'success' })
		} else {
			await teacherAPI.createJob(payload)
			uni.showToast({ title: '创建成功', icon: 'success' })
		}
		setTimeout(() => { uni.navigateBack() }, 500)
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'none' })
	} finally {
		saving.value = false
	}
}

const goBack = () => { uni.navigateBack() }
</script>

<style scoped>
.header-simple {
	background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
	color: white;
	flex-shrink: 0;
}
.form-container {
	padding: 16px;
}
.input-group {
	margin-bottom: 16px;
}
.input-label {
	font-size: 14px;
	font-weight: 600;
	color: #1D2129;
	margin-bottom: 8px;
	display: block;
}
.input-field {
	width: 100%;
	height: 48px;
	border: 2px solid #E2E8F0;
	border-radius: 12px;
	padding: 0 16px;
	font-size: 15px;
	background: #F8F9FC;
	box-sizing: border-box;
}
.input-field:focus {
	border-color: #10B981;
	background: #fff;
}
.input-textarea {
	width: 100%;
	min-height: 120px;
	border: 2px solid #E2E8F0;
	border-radius: 12px;
	padding: 12px 16px;
	font-size: 15px;
	background: #F8F9FC;
	box-sizing: border-box;
	line-height: 1.6;
}
.input-textarea:focus {
	border-color: #10B981;
	background: #fff;
}
.row-inputs {
	flex-direction: row;
	gap: 12px;
}
.picker-box {
	flex-direction: row;
	align-items: center;
	height: 48px;
	border: 2px solid #E2E8F0;
	border-radius: 12px;
	padding: 0 16px;
	background: #F8F9FC;
}
.picker-text {
	flex: 1;
	font-size: 15px;
	color: #1D2129;
}
.picker-text.placeholder { color: #C9CDD4; }
.picker-arrow { color: #C9CDD4; font-size: 12px; }
.form-actions {
	flex-direction: row;
	gap: 12px;
	margin-top: 24px;
}
.btn-cancel {
	flex: 1;
	padding: 14px;
	border-radius: 12px;
	border: 1px solid #E2E8F0;
	background: white;
	color: #4E5969;
	font-size: 15px;
	font-weight: 600;
	align-items: center;
	justify-content: center;
}
.btn-save {
	flex: 2;
	padding: 14px;
	border-radius: 12px;
	border: none;
	background: linear-gradient(135deg, #10B981, #34D399);
	color: white;
	font-size: 15px;
	font-weight: 700;
	align-items: center;
	justify-content: center;
	box-shadow: 0 4px 12px rgba(16,185,129,0.3);
}
/* Modal */
.modal-overlay {
	position: fixed;
	top: 0; left: 0; right: 0; bottom: 0;
	background: rgba(0,0,0,0.4);
	justify-content: center;
	align-items: center;
	z-index: 999;
}
.modal-content {
	width: 85%;
	max-height: 70%;
	background: white;
	border-radius: 20px;
	overflow: hidden;
}
.modal-header {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	padding: 16px 20px;
	border-bottom: 1px solid #F2F3F5;
}
.modal-title {
	font-size: 17px;
	font-weight: 700;
	color: #1D2129;
}
.modal-close {
	font-size: 20px;
	color: #86909C;
	padding: 4px;
}
.modal-list {
	max-height: 400px;
}
.modal-item {
	flex-direction: row;
	align-items: center;
	padding: 16px 20px;
	border-bottom: 1px solid #F2F3F5;
}
.modal-item.selected { background: rgba(16,185,129,0.05); }
.modal-item-text { flex: 1; font-size: 15px; color: #1D2129; }
.modal-check { color: #10B981; font-size: 18px; font-weight: 700; }
.empty-state {
	padding: 40px;
	align-items: center;
	justify-content: center;
	color: #86909C;
	font-size: 14px;
}
</style>
