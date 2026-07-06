<template>
	<view class="page-wrapper">
		<NavBar title="编辑简历" />
		<scroll-view class="scroll-area" scroll-y>
			<!-- 导入PDF简历 -->
			<view class="import-section" @click="handleImportPdf">
				<uni-icons type="cloud-upload" size="22" color="#165DFF" />
				<view class="import-texts">
					<text class="import-title">导入PDF简历</text>
					<text class="import-desc">上传PDF自动解析，快速填写简历</text>
				</view>
				<uni-icons type="arrowright" size="16" color="#C9CDD4" />
			</view>

			<view class="form-section">
				<text class="form-section-title">基本信息</text>
				<view class="form-group">
					<text class="form-label">姓名</text>
					<input class="form-input" v-model="form.name" placeholder="请输入姓名" />
				</view>
				<view class="form-group">
					<text class="form-label">性别</text>
					<view class="radio-group">
						<text class="radio-item" :class="{ active: form.gender === '男' }" @click="form.gender = '男'">男</text>
						<text class="radio-item" :class="{ active: form.gender === '女' }" @click="form.gender = '女'">女</text>
					</view>
				</view>
				<view class="form-group">
					<text class="form-label">手机号</text>
					<input class="form-input" v-model="form.phone" placeholder="请输入手机号" type="number" maxlength="11" />
				</view>
				<view class="form-group">
					<text class="form-label">邮箱</text>
					<input class="form-input" v-model="form.email" placeholder="请输入邮箱" type="email" />
				</view>
			</view>

			<view class="form-section">
				<text class="form-section-title">求职意向</text>
				<view class="form-group">
					<input class="form-input" v-model="form.jobTarget" placeholder="例：Java开发工程师" />
				</view>
			</view>

			<view class="form-section">
				<view class="form-section-header">
					<text class="form-section-title">教育经历</text>
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
						<input class="form-input half" v-model="edu.start" placeholder="入学时间" />
						<input class="form-input half" v-model="edu.end" placeholder="毕业时间" />
					</view>
				</view>
				<text v-if="form.education.length === 0" class="empty-hint">点击上方「+ 添加」添加教育经历</text>
			</view>

			<view class="form-section">
				<view class="form-section-header">
					<text class="form-section-title">实习经历</text>
					<text class="form-section-action" @click="addInternship">+ 添加</text>
				</view>
				<view v-for="(job, i) in form.internship" :key="i" class="json-item">
					<view class="json-item-header">
						<text class="json-item-title">实习经历 {{ i + 1 }}</text>
						<text class="json-item-remove" @click="form.internship.splice(i, 1)">删除</text>
					</view>
					<input class="form-input mb-8" v-model="job.company" placeholder="公司名称" />
					<input class="form-input mb-8" v-model="job.position" placeholder="职位" />
					<input class="form-input" v-model="job.duration" placeholder="时间段" />
				</view>
				<text v-if="form.internship.length === 0" class="empty-hint">点击上方「+ 添加」添加实习经历</text>
			</view>

			<view class="form-section">
				<text class="form-section-title">技能证书</text>
				<TagInput v-model="form.skillsList" placeholder="输入技能后按回车添加" />
			</view>

			<view class="form-section">
				<text class="form-section-title">自我评价</text>
				<textarea class="form-textarea" v-model="form.selfEvaluation" placeholder="简要描述你的优势和求职意向" />
			</view>
			<view style="height: 100px;"></view>
		</scroll-view>
		<view class="bottom-bar">
			<button class="save-btn" :disabled="saving" @click="handleSave">{{ saving ? '保存中...' : '保存' }}</button>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { resumeAPI, authAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import TagInput from '@/components/TagInput.vue'

const resumeId = ref(null)
const saving = ref(false)
const importing = ref(false)
const form = ref({
	name: '',
	gender: '男',
	phone: '',
	email: '',
	jobTarget: '',
	education: [],
	internship: [],
	skillsList: [],
	skills: '',
	selfEvaluation: ''
})

const getStudentId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.id || obj.userId || null
	} catch (e) { return null }
}

onMounted(async () => {
	await loadExisting()
})

async function loadExisting() {
	// 先从后端加载最新用户信息（确保清缓存后也能拿到）
	let userName = '', userPhone = '', userEmail = '', userGender = '男'
	try {
		const userRes = await authAPI.getUserInfo()
		if (userRes.data) {
			const u = userRes.data
			userName = u.realName || ''
			userPhone = u.phone || ''
			userEmail = u.email || ''
			userGender = u.gender === 1 ? '男' : u.gender === 2 ? '女' : '男'
			// 同步更新 localStorage
			try {
				const raw = uni.getStorageSync('userInfo')
				const ui = raw ? JSON.parse(raw) : {}
				ui.realName = userName
				ui.phone = userPhone
				ui.email = userEmail
				ui.gender = u.gender || 0
				uni.setStorageSync('userInfo', JSON.stringify(ui))
			} catch (e) {}
		}
	} catch (e) { /* 用户信息接口不可用，降级到 localStorage */ }

	// 如果上面没拿到，从 localStorage 读取
	if (!userName) {
		try {
			const raw = uni.getStorageSync('userInfo')
			if (raw) {
				const ui = JSON.parse(raw)
				userName = ui.realName || ''
				userPhone = ui.phone || ''
				userEmail = ui.email || ''
				userGender = ui.gender === 1 ? '男' : ui.gender === 2 ? '女' : '男'
			}
		} catch (e) {}
	}

	// 加载简历数据
	try {
		const res = await resumeAPI.getResume()
		const data = res.data
		if (data) {
			resumeId.value = data.id
			form.value = {
				name: data.name || data.realName || userName,
				gender: userGender,
				phone: data.phone || userPhone,
				email: data.email || userEmail,
				jobTarget: data.jobTarget || '',
				education: parseJsonArray(data.education),
				internship: parseJsonArray(data.internship),
				skillsList: data.skills ? data.skills.split(',').map(s => s.trim()).filter(Boolean) : [],
				skills: data.skills || '',
				selfEvaluation: data.selfEvaluation || ''
			}
		} else {
			// 无简历数据时，至少填充用户信息
			form.value.name = userName
			form.value.phone = userPhone
			form.value.email = userEmail
		}
	} catch (e) {}
}

function parseJsonArray(val) {
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

const handleImportPdf = () => {
	uni.chooseImage({
		count: 1,
		success: (res) => {
			const filePath = res.tempFilePaths[0]
			uploadAndParse(filePath)
		}
	})
}

const uploadAndParse = async (filePath) => {
	importing.value = true
	uni.showToast({ title: '解析中...', icon: 'loading' })
	try {
		const studentId = getStudentId()
		const token = uni.getStorageSync('token')
		// uni-app 中上传文件
		const uploadRes = await new Promise((resolve, reject) => {
			uni.uploadFile({
				url: getApiBaseUrl() + '/resumes/upload-and-parse',
				filePath: filePath,
				name: 'file',
				formData: { studentId: studentId },
				header: { Authorization: token },
				success: (r) => {
					try { resolve(JSON.parse(r.data)) } catch (e) { reject(e) }
				},
				fail: reject
			})
		})
		if (uploadRes.code === 200) {
			const data = uploadRes.data.parsedData
			// 自动填充表单
			if (data.name) form.value.name = data.name
			if (data.phone) form.value.phone = data.phone
			if (data.email) form.value.email = data.email
			// 教育经历
			if (data.school || data.major || data.education) {
				form.value.education = [{
					school: data.school || '',
					major: data.major || '',
					degree: data.education || '',
					start: data.educationStart || '',
					end: data.educationEnd || ''
				}]
			}
			// 技能
			if (data.skills && Array.isArray(data.skills)) {
				form.value.skillsList = data.skills
				form.value.skills = data.skills.join(', ')
			}
			// 实习经历
			if (data.internshipCompany || data.internshipPosition) {
				form.value.internship = [{
					company: data.internshipCompany || '',
					position: data.internshipPosition || '',
					duration: data.internshipDuration || '',
					description: data.internshipDesc || ''
				}]
			}
			// 自我评价
			if (data.selfEvaluation) {
				form.value.selfEvaluation = data.selfEvaluation
			}
			uni.showToast({ title: '解析成功，请核对信息', icon: 'success' })
		} else {
			uni.showToast({ title: uploadRes.message || '解析失败', icon: 'none' })
		}
	} catch (e) {
		uni.showToast({ title: '导入失败', icon: 'none' })
	} finally {
		importing.value = false
	}
}

const getApiBaseUrl = () => {
	// 从已有请求配置中获取 baseURL
	try {
		const app = getApp()
		if (app?.globalData?.baseUrl) return app.globalData.baseUrl
	} catch (e) {}
	return 'http://localhost:8080/api'
}

const handleSave = async () => {
	if (!form.value.jobTarget.trim()) {
		uni.showToast({ title: '请填写求职意向', icon: 'none' })
		return
	}
	const studentId = getStudentId()
	if (!studentId) {
		uni.showToast({ title: '请先登录', icon: 'none' })
		return
	}
	saving.value = true
	try {
		const payload = {
			name: form.value.name,
			gender: form.value.gender === '男' ? 1 : form.value.gender === '女' ? 2 : 0,
			phone: form.value.phone,
			email: form.value.email,
			jobTarget: form.value.jobTarget,
			education: JSON.stringify(form.value.education),
			internship: JSON.stringify(form.value.internship),
			skills: form.value.skillsList.join(','),
			selfEvaluation: form.value.selfEvaluation
		}
		if (resumeId.value) payload.id = resumeId.value
		await resumeAPI.createOrUpdateResume(studentId, payload)
		// 同步更新本地用户信息
		try {
			const raw = uni.getStorageSync('userInfo')
			if (raw) {
				const ui = JSON.parse(raw)
				ui.realName = form.value.name || ui.realName
				ui.phone = form.value.phone || ui.phone
				ui.email = form.value.email || ui.email
				ui.gender = form.value.gender === '男' ? 1 : form.value.gender === '女' ? 2 : 0
				uni.setStorageSync('userInfo', JSON.stringify(ui))
			}
		} catch (e) {}
		uni.showToast({ title: '保存成功', icon: 'success' })
		uni.$emit('resume-updated')
		setTimeout(() => { uni.navigateBack() }, 500)
	} catch (e) {
		uni.showToast({ title: '保存失败，请重试', icon: 'none' })
	} finally {
		saving.value = false
	}
}
</script>

<style scoped>
.scroll-area { flex: 1; overflow-y: auto; padding-bottom: 80px; }
.form-section {
	background: white;
	border-radius: 12px;
	margin: 12px 16px;
	padding: 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.form-section-header {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 12px;
}
.form-section-title {
	font-size: 15px;
	font-weight: 600;
	color: #1D2129;
	margin-bottom: 12px;
}
.form-section-header .form-section-title { margin-bottom: 0; }
.form-section-action {
	font-size: 14px;
	color: #165DFF;
	font-weight: 500;
	padding: 4px;
}
.form-group { margin-bottom: 14px; }
.form-label {
	font-size: 13px;
	color: #4E5969;
	margin-bottom: 6px;
	display: block;
}
.form-input {
	width: 100%;
	height: 44px;
	border: 1px solid #E5E6EB;
	border-radius: 8px;
	padding: 0 12px;
	font-size: 15px;
	background: #F7F8FA;
	box-sizing: border-box;
	color: #1D2129;
}
.form-input:focus {
	border-color: #165DFF;
	background: #FFFFFF;
}
.form-textarea {
	width: 100%;
	min-height: 100px;
	border: 1px solid #E5E6EB;
	border-radius: 8px;
	padding: 12px;
	font-size: 15px;
	background: #F7F8FA;
	box-sizing: border-box;
	color: #1D2129;
	line-height: 1.6;
}
.form-textarea:focus {
	border-color: #165DFF;
	background: #FFFFFF;
}
.form-row { flex-direction: row; gap: 12px; }
.form-input.half { flex: 1; }
.mb-8 { margin-bottom: 8px; }
.empty-hint { font-size: 13px; color: #C9CDD4; }
.radio-group {
	flex-direction: row;
	gap: 12px;
}
.radio-item {
	padding: 10px 24px;
	border: 1px solid #E5E6EB;
	border-radius: 8px;
	font-size: 14px;
	color: #4E5969;
	background: #F7F8FA;
}
.radio-item.active {
	border-color: #165DFF;
	color: #165DFF;
	background: rgba(22,93,255,0.06);
	font-weight: 500;
}
.json-item {
	background: #F7F8FA;
	border-radius: 8px;
	padding: 12px;
	margin-bottom: 12px;
}
.json-item-header {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 8px;
}
.json-item-title { font-size: 14px; font-weight: 500; color: #4E5969; }
.json-item-remove { font-size: 13px; color: #F53F3F; padding: 4px; }
.bottom-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	background: rgba(255,255,255,0.92);
	backdrop-filter: blur(20px);
	-webkit-backdrop-filter: blur(20px);
	padding: 12px 16px 24px;
	border-top: 0.5px solid #F2F3F5;
	box-shadow: 0 -2px 12px rgba(0,0,0,0.04);
	padding-bottom: calc(12px + env(safe-area-inset-bottom));
}
.save-btn {
	width: 100%;
	height: 48px;
	border-radius: 12px;
	background: #165DFF;
	color: white;
	font-size: 16px;
	font-weight: 700;
	align-items: center;
	justify-content: center;
	border: none;
}
.save-btn[disabled] {
	background: #E5E6EB !important;
	color: #A9AEB8 !important;
	border: none !important;
}
.save-btn:active { opacity: 0.9; }

/* ===== 导入PDF简历 ===== */
.import-section {
	flex-direction: row;
	align-items: center;
	gap: 12px;
	background: rgba(22,93,255,0.04);
	border: 1px dashed rgba(22,93,255,0.3);
	border-radius: 12px;
	padding: 14px 16px;
	margin: 12px 16px;
}
.import-section:active {
	background: rgba(22,93,255,0.08);
}
.import-texts { flex: 1; }
.import-title {
	font-size: 14px;
	font-weight: 600;
	color: #165DFF;
}
.import-desc {
	font-size: 12px;
	color: #86909C;
	margin-top: 2px;
}
</style>
