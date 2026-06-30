<template>
	<view class="page-wrapper">
		<view class="header-simple" style="padding:12px 16px;">
			<text style="font-size:18px;font-weight:700;color:white;">岗位管理</text>
		</view>
		<scroll-view class="content-scrollable" scroll-y>
			<view class="job-list">
				<view v-for="(job, i) in jobs" :key="i" class="job-card" @click="goToEdit(job)">
					<view class="job-card-top">
						<view class="job-info">
							<text class="job-title">{{ job.title }}</text>
							<text class="job-company">{{ job.companyName || '未设置公司' }}</text>
						</view>
						<text class="job-status" :class="'status-' + job.status">{{ statusText(job.status) }}</text>
					</view>
					<view class="job-meta">
						<text>{{ job.location || '未设置地点' }}</text>
						<text>{{ job.salaryText || '薪资面议' }}</text>
						<text>{{ job.deliveryCount || 0 }}人投递</text>
					</view>
					<view class="job-actions">
						<button v-if="job.status === 'draft'" class="btn-sm btn-green" @click.stop="handlePublish(job)">发布</button>
						<button v-if="job.status === 'active'" class="btn-sm btn-outline-orange" @click.stop="handleClose(job)">关闭</button>
						<button class="btn-sm btn-outline" @click.stop="goToEdit(job)">编辑</button>
						<button class="btn-sm btn-outline" @click.stop="goToDeliveries(job)">投递</button>
						<button class="btn-sm btn-ai" @click.stop="goToAiMatches(job.id)">🤖 匹配</button>
					</view>
				</view>
				<view v-if="!jobs.length" class="empty-state">
					<text style="font-size:48px;margin-bottom:12px;">📋</text>
					<text class="empty-text">暂无岗位，点击下方 + 新建</text>
				</view>
			</view>
			<view style="height:40px;"></view>
		</scroll-view>
		<!-- 浮动新建按钮 -->
		<view class="fab" @click="goToCreate">
			<text class="fab-icon">+</text>
		</view>
		<TeacherTabBar current="jobs" />
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { teacherAPI } from '@/utils/request'
import TeacherTabBar from '@/components/TeacherTabBar.vue'

const jobs = ref([])

// 后端Integer状态 → 前端字符串状态
const STATUS_MAP = { 0: 'draft', 1: 'active', 2: 'closed', 3: 'paused' }

const statusText = (status) => {
	const map = { draft: '草稿', active: '招聘中', closed: '已关闭', paused: '已暂停' }
	return map[status] || '未知'
}

// 后端Job → 前端卡片展示映射
const mapTeacherJob = (job) => ({
	id: job.id,
	title: job.title,
	companyName: job.companyName || '待加载',
	companyId: job.companyId,
	status: STATUS_MAP[job.status] || 'draft',
	location: job.location || '未设置地点',
	salaryText: job.salaryRange || '薪资面议',
	deliveryCount: job.deliveryCount || 0
})

// 公司名缓存（复用首页缓存方案）
const companyNameCache = {}
const loadCompanyName = async (job) => {
	if (job.companyName && job.companyName !== '待加载') return job.companyName
	if (!job.companyId) return '未设置公司'
	if (companyNameCache[job.companyId]) return companyNameCache[job.companyId]
	try {
		const res = await teacherAPI.getCompanyName(job.companyId)
		const name = res?.data?.name || '未设置公司'
		companyNameCache[job.companyId] = name
		return name
	} catch (e) {
		return '企业' + job.companyId
	}
}

const getTeacherId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.id || obj.userId
	} catch (e) { return null }
}

onMounted(async () => {
	await loadJobs()
})

const goToAiMatches = (jobId) => uni.navigateTo({ url: '/pages/teacher/ai-matches' })

const loadJobs = async () => {
	try {
		const tid = getTeacherId()
		const res = await teacherAPI.getTeacherJobs(tid)
		const rawList = res.data || []
		const mapped = await Promise.all(rawList.map(async (j) => {
			const item = mapTeacherJob(j)
			item.companyName = await loadCompanyName(j)
			return item
		}))
		jobs.value = mapped
	} catch (e) {
		console.error('加载岗位列表失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

const handlePublish = async (job) => {
	try {
		await teacherAPI.publishJob(job.id)
		uni.showToast({ title: '发布成功', icon: 'success' })
		job.status = 'active'
	} catch (e) {
		uni.showToast({ title: '发布失败', icon: 'none' })
	}
}

const handleClose = async (job) => {
	try {
		await teacherAPI.closeJob(job.id)
		uni.showToast({ title: '已关闭', icon: 'success' })
		job.status = 'closed'
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'none' })
	}
}

const goToEdit = (job) => {
	uni.navigateTo({ url: '/pages/teacher/job-edit?id=' + job.id })
}

const goToCreate = () => {
	uni.navigateTo({ url: '/pages/teacher/job-edit' })
}

const goToDeliveries = (job) => {
	uni.navigateTo({ url: '/pages/teacher/deliveries?jobId=' + job.id + '&jobTitle=' + encodeURIComponent(job.title) })
}
</script>

<style scoped>
.header-simple {
	background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
	color: white;
	flex-shrink: 0;
}
.job-list {
	padding: 16px;
}
.job-card {
	background: white;
	border-radius: 16px;
	padding: 16px;
	margin-bottom: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
	position: relative;
	overflow: hidden;
}
.job-card::before {
	content: '';
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 3px;
	background: linear-gradient(90deg, #10B981, transparent);
}
.job-card-top {
	flex-direction: row;
	justify-content: space-between;
	align-items: flex-start;
	margin-bottom: 8px;
}
.job-info { flex: 1; }
.job-title {
	font-size: 16px;
	font-weight: 700;
	color: #1D2129;
	display: block;
	margin-bottom: 4px;
}
.job-company {
	font-size: 13px;
	color: #86909C;
	display: block;
}
.job-status {
	font-size: 12px;
	padding: 3px 10px;
	border-radius: 8px;
	font-weight: 600;
	flex-shrink: 0;
}
.status-active { background: rgba(16,185,129,0.1); color: #10B981; }
.status-draft { background: rgba(245,158,11,0.1); color: #F59E0B; }
.status-closed { background: #F2F3F5; color: #86909C; }
.job-meta {
	flex-direction: row;
	gap: 12px;
	font-size: 13px;
	color: #86909C;
	margin-bottom: 12px;
}
.job-actions {
	flex-direction: row;
	gap: 8px;
	padding-top: 12px;
	border-top: 1px solid #F2F3F5;
}
.btn-sm {
	flex: 1;
	padding: 8px;
	border-radius: 8px;
	font-size: 13px;
	font-weight: 600;
	align-items: center;
	justify-content: center;
}
.btn-green {
	background: #10B981;
	color: white;
	border: none;
}
.btn-outline {
	background: white;
	border: 1px solid #E2E8F0;
	color: #4E5969;
}
.btn-outline-orange {
	background: white;
	border: 1px solid #F59E0B;
	color: #F59E0B;
}
.btn-ai {
	background: linear-gradient(135deg, #8B5CF6, #A78BFA);
	color: white;
	border: none;
}
.fab {
	position: fixed;
	bottom: calc(64px + env(safe-area-inset-bottom) + 16px);
	right: 20px;
	width: 56px;
	height: 56px;
	border-radius: 50%;
	background: #10B981;
	align-items: center;
	justify-content: center;
	box-shadow: 0 4px 16px rgba(16,185,129,0.4);
	z-index: 100;
}
.fab-icon {
	font-size: 28px;
	color: white;
	font-weight: 300;
	line-height: 56px;
	text-align: center;
}
.empty-state {
	padding: 60px 20px;
	align-items: center;
	justify-content: center;
}
.empty-text {
	font-size: 14px;
	color: #86909C;
}
</style>
