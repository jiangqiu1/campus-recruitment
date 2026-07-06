<template>
	<view class="page-wrapper">
		<NavBar :title="isEdit ? '编辑岗位' : '新建岗位'" show-back />
		<scroll-view class="content-scrollable" scroll-y>
			<view class="form-container">
				<view class="form-section">
					<view class="input-group">
						<text class="input-label">岗位名称 <text style="color:#EF4444;">*</text></text>
						<input class="input-field" v-model="form.title" placeholder="请输入岗位名称" />
					</view>
					<view class="input-group">
						<text class="input-label">招聘企业</text>
						<view class="picker-box" @click="showCompanyPicker = true">
							<text class="picker-text" :class="{ placeholder: !form.companyId }">{{ form.companyName || '选择招聘企业' }}</text>
							<uni-icons type="arrowdown" size="12" color="#C9CDD4" />
						</view>
					</view>
					<view class="input-group">
						<text class="input-label">工作地点 <text style="color:#EF4444;">*</text></text>
						<input class="input-field" v-model="form.location" placeholder="如：广州" />
					</view>
					<view class="row-inputs">
						<view class="input-group half">
							<text class="input-label">最低学历</text>
							<picker @change="onEduChange" :range="eduOptions">
								<view class="picker-box">
									<text class="picker-text" :class="{ placeholder: !form.education }">{{ form.education || '请选择' }}</text>
									<uni-icons type="arrowdown" size="12" color="#C9CDD4" />
								</view>
							</picker>
						</view>
						<view class="input-group half">
							<text class="input-label">经验要求</text>
							<picker @change="onExpChange" :range="expOptions">
								<view class="picker-box">
									<text class="picker-text" :class="{ placeholder: !form.experience }">{{ form.experience || '请选择' }}</text>
									<uni-icons type="arrowdown" size="12" color="#C9CDD4" />
								</view>
							</picker>
						</view>
					</view>
					<view class="row-inputs">
						<view class="input-group half">
							<text class="input-label">最低薪资 (K)</text>
							<input class="input-field" v-model="form.salaryMin" type="number" placeholder="如：4" />
						</view>
						<view class="input-group half">
							<text class="input-label">最高薪资 (K)</text>
							<input class="input-field" v-model="form.salaryMax" type="number" placeholder="如：8" />
						</view>
					</view>
					<view class="input-group">
						<text class="input-label">岗位描述 <text style="color:#EF4444;">*</text></text>
						<textarea class="input-textarea" v-model="form.description" placeholder="在此输入完整的岗位描述，点击下方「AI 智能填写」可自动提取标题、薪资、地点等信息" />
					</view>
					<button class="ai-btn" :loading="aiParsing" @click="handleAiParseJob" :disabled="!form.description.trim()">
						<uni-icons type="star" size="16" color="#FFFFFF" />
						<text>AI 智能填写</text>
					</button>
					<view class="input-group">
						<text class="input-label">岗位要求</text>
						<textarea class="input-textarea" v-model="form.requirements" placeholder="请输入任职要求" />
					</view>
				</view>

				<view class="form-actions">
					<button class="btn-cancel" @click="goBack">取消</button>
					<button class="btn-save" :loading="saving" @click="handleSave">
						{{ saving ? '保存中...' : (isEdit ? '保存修改' : '创建岗位') }}
					</button>
				</view>
			</view>
		</scroll-view>

		<!-- 企业选择弹窗 -->
		<PopupDrawer :show="showCompanyPicker" title="选择企业" @update:show="showCompanyPicker = $event">
			<view v-for="(c, i) in companies" :key="i" class="modal-item" :class="{ selected: form.companyId === c.id }" @click="selectCompany(c)">
				<text class="modal-item-text">{{ c.name }}</text>
				<uni-icons v-if="form.companyId === c.id" type="checkmark-filled" size="18" color="#165DFF" />
			</view>
			<EmptyState v-if="!companies.length" icon="shop" title="暂无企业" desc="暂无企业，可点击下方添加" />
			<!-- 快速添加企业 -->
			<view class="modal-item-sep" />
			<view class="add-company-section">
				<text class="add-company-toggle" @click="showAddCompany = !showAddCompany">
					<uni-icons :type="showAddCompany ? 'minus' : 'plus'" size="14" color="#165DFF" />
					<text>{{ showAddCompany ? '收起' : '添加新企业' }}</text>
				</text>
				<view v-if="showAddCompany" class="add-company-form">
					<input class="add-input" v-model="newCompany.name" placeholder="企业名称（必填）" />
					<view class="add-input-row">
						<input class="add-input half" v-model="newCompany.contact" placeholder="联系人" />
						<input class="add-input half" v-model="newCompany.phone" placeholder="联系电话" />
					</view>
					<button class="add-company-btn" @click="handleAddCompany" :disabled="!newCompany.name.trim()">确认添加</button>
				</view>
			</view>
		</PopupDrawer>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { teacherAPI, jobAPI, aiParseAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import PopupDrawer from '@/components/PopupDrawer.vue'
import EmptyState from '@/components/EmptyState.vue'
import { checkRole } from '@/utils/auth'

checkRole(1)

const isEdit = ref(false)
const saving = ref(false)
const aiParsing = ref(false)
const showCompanyPicker = ref(false)
const showAddCompany = ref(false)
const newCompany = ref({ name: '', contact: '', phone: '' })
const companies = ref([])

const eduOptions = ['不限', '高中', '中专', '大专', '本科', '硕士', '博士']
const expOptions = ['不限', '应届生', '1年以下', '1-3年', '3-5年', '5-10年', '10年以上']

const form = ref({
	id: null,
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

const jobToForm = (job) => {
	let min = '', max = ''
	if (job.salaryRange) {
		const parts = job.salaryRange.match(/([\d.]+)/g)
		if (parts) { min = parts[0] || ''; max = parts[1] || '' }
	}
	return {
		id: job.id,
		title: job.title || '',
		companyId: job.companyId || '',
		companyName: job.companyName || '',
		location: job.location || '',
		education: job.education || '',
		experience: job.experience || '',
		salaryMin: min,
		salaryMax: max,
		description: job.description || '',
		requirements: job.requirement || ''
	}
}

const formToJob = (form) => {
	const salaryText = form.salaryMin && form.salaryMax ? form.salaryMin + 'K-' + form.salaryMax + 'K' : ''
	return {
		title: form.title,
		companyId: form.companyId,
		location: form.location,
		education: form.education,
		description: form.description,
		requirement: form.requirements,
		salaryRange: salaryText
	}
}

onMounted(async () => {
	const pages = getCurrentPages()
	const currentPage = pages[pages.length - 1]
	const opts = currentPage.options || {}
	const jobId = opts.id ? Number(opts.id) : null
	if (jobId) {
		isEdit.value = true
		await loadJob(jobId)
	}
	// 从 AI 解析结果填充表单
	if (opts.aiParsed) {
		try {
			const parsed = uni.getStorageSync('ai_parsed_job')
			if (parsed) {
				const data = typeof parsed === 'string' ? JSON.parse(parsed) : parsed
				fillFromAiParse(data)
			}
			uni.removeStorageSync('ai_parsed_job')
		} catch (e) {}
	}
	await loadCompanies()
})

const loadJob = async (jobId) => {
	try {
		const res = await jobAPI.getJobDetail(jobId)
		const job = res.data
		if (job) {
			const mapped = jobToForm(job)
			if (job.companyId) {
				try {
					const cRes = await teacherAPI.getCompanyName(job.companyId)
					mapped.companyName = cRes?.data?.name || '企业' + job.companyId
				} catch (e) {}
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
	}
}

const selectCompany = (c) => {
	form.value.companyId = c.id
	form.value.companyName = c.name
	showCompanyPicker.value = false
}

// 快速添加企业
const handleAddCompany = async () => {
	if (!newCompany.value.name.trim()) return
	try {
		const payload = { name: newCompany.value.name.trim() }
		if (newCompany.value.contact.trim()) payload.contactPerson = newCompany.value.contact.trim()
		if (newCompany.value.phone.trim()) payload.contactPhone = newCompany.value.phone.trim()
		await teacherAPI.createCompany(payload)
		uni.showToast({ title: '添加成功', icon: 'success' })
		newCompany.value = { name: '', contact: '', phone: '' }
		showAddCompany.value = false
		// 重新加载企业列表并自动选中新企业
		await loadCompanies()
		const last = companies.value[companies.value.length - 1]
		if (last) selectCompany(last)
	} catch (e) {
		uni.showToast({ title: '添加失败', icon: 'none' })
	}
}

const onEduChange = (e) => { form.value.education = eduOptions[e.detail.value] }
const onExpChange = (e) => { form.value.experience = expOptions[e.detail.value] }

const handleSave = async () => {
	if (!form.value.title) { uni.showToast({ title: '请输入岗位名称', icon: 'none' }); return }
	if (!form.value.companyId) { uni.showToast({ title: '请选择招聘企业', icon: 'none' }); return }
	if (!form.value.location) { uni.showToast({ title: '请输入工作地点', icon: 'none' }); return }
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
		const msg = e?.message || e?.errMsg || '操作失败'
		uni.showToast({ title: msg, icon: 'none' })
	} finally {
		saving.value = false
	}
}

const goBack = () => { uni.navigateBack() }

// 从 AI 解析结果填充表单
const fillFromAiParse = (data) => {
	if (!data) return
	if (data.title) form.value.title = data.title
	if (data.salaryRange) {
		const parts = data.salaryRange.match(/([\d.]+)/g)
		if (parts) {
			form.value.salaryMin = parts[0] || ''
			form.value.salaryMax = parts[1] || parts[0] || ''
		}
	}
	if (data.location) form.value.location = data.location
	if (data.education) form.value.education = data.education
	if (data.experience) form.value.experience = data.experience
	if (data.description) form.value.description = data.description
	if (data.requirements) form.value.requirements = data.requirements
	uni.showToast({ title: 'AI 填写完成，请核对', icon: 'success' })
}

// AI 智能填写：从岗位描述中提取结构化信息
const handleAiParseJob = async () => {
	if (!form.value.description.trim()) return
	aiParsing.value = true
	try {
		const res = await aiParseAPI.parseJob(form.value.description)
		const data = res.data || {}
		fillFromAiParse(data)
	} catch (e) {
		console.error('AI解析失败', e)
		uni.showToast({ title: 'AI 解析失败', icon: 'none' })
	} finally {
		aiParsing.value = false
	}
}
</script>

<style scoped>
.form-container { padding: 16px; }
.form-section {
	background: white;
	border-radius: 12px;
	padding: 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.input-group { margin-bottom: 16px; }
.input-label { font-size: 14px; font-weight: 600; color: #1D2129; margin-bottom: 8px; display: block; }
.input-field {
	width: 100%;
	height: 44px;
	border: 1px solid #E5E6EB;
	border-radius: 8px;
	padding: 0 12px;
	font-size: 15px;
	background: #F7F8FA;
	box-sizing: border-box;
}
.input-field:focus { border-color: #165DFF; background: #FFFFFF; }
.input-textarea {
	width: 100%;
	min-height: 120px;
	border: 1px solid #E5E6EB;
	border-radius: 8px;
	padding: 12px;
	font-size: 15px;
	background: #F7F8FA;
	box-sizing: border-box;
	line-height: 1.6;
}
.input-textarea:focus { border-color: #165DFF; background: #FFFFFF; }

/* AI 智能填写按钮 */
.ai-btn {
	flex-direction: row;
	align-items: center;
	justify-content: center;
	gap: 6px;
	height: 42px;
	border-radius: 8px;
	background: linear-gradient(135deg, #8B5CF6, #7C3AED);
	color: #fff;
	font-size: 15px;
	font-weight: 600;
	border: none;
	margin-bottom: 16px;
}
.ai-btn[disabled] { background: #E5E6EB !important; color: #A9AEB8 !important; }
.ai-btn:active { opacity: 0.85; }

/* 企业选择弹窗 - 快速添加 */
.modal-item-sep { height: 1px; background: #F2F3F5; margin: 12px 0; }
.add-company-section { padding-bottom: 4px; }
.add-company-toggle {
	flex-direction: row;
	align-items: center;
	gap: 6px;
	font-size: 14px;
	color: #165DFF;
	font-weight: 500;
	padding: 8px 0;
}
.add-company-form { gap: 10px; margin-top: 10px; }
.add-input {
	width: 100%;
	height: 40px;
	border: 1px solid #E5E6EB;
	border-radius: 8px;
	padding: 0 12px;
	font-size: 14px;
	background: #F7F8FA;
	color: #1D2129;
	box-sizing: border-box;
}
.add-input.half { width: calc(50% - 5px); }
.add-input-row { flex-direction: row; gap: 10px; }
.add-company-btn {
	width: 100%;
	height: 40px;
	border-radius: 8px;
	background: #165DFF;
	color: white;
	font-size: 14px;
	font-weight: 600;
	border: none;
	align-items: center;
	justify-content: center;
}
.add-company-btn[disabled] { background: #E5E6EB !important; color: #A9AEB8 !important; }

.row-inputs { flex-direction: row; gap: 12px; }
.input-group.half { flex: 1; }
.picker-box {
	flex-direction: row;
	align-items: center;
	height: 44px;
	border: 1px solid #E5E6EB;
	border-radius: 8px;
	padding: 0 12px;
	background: #F7F8FA;
}
.picker-text { flex: 1; font-size: 15px; color: #1D2129; }
.picker-text.placeholder { color: #C9CDD4; }

.form-actions {
	flex-direction: row;
	gap: 12px;
	margin-top: 16px;
}
.btn-cancel {
	flex: 1;
	padding: 12px;
	border-radius: 8px;
	border: 1px solid #E5E6EB;
	background: white;
	color: #4E5969;
	font-size: 15px;
	font-weight: 600;
	align-items: center;
	justify-content: center;
}
.btn-save {
	flex: 2;
	padding: 12px;
	border-radius: 8px;
	border: none;
	background: linear-gradient(135deg, #165DFF, #2563EB);
	color: white;
	font-size: 15px;
	font-weight: 700;
	align-items: center;
	justify-content: center;
}

.modal-item {
	flex-direction: row;
	align-items: center;
	padding: 14px 16px;
}
.modal-item-text { flex: 1; font-size: 15px; color: #1D2129; }
.modal-item.selected { background: rgba(22,93,255,0.05); }
</style>
