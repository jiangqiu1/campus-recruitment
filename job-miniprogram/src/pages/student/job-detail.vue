<template>
	<view class="page-wrapper">
		<scroll-view class="content-scrollable" scroll-y>
			<!-- 返回按钮 -->
			<view class="detail-back" @click="goBack">
				<text><text style="font-size:24px;font-weight:700">‹</text> 返回</text>
			</view>

			<!-- 岗位信息 -->
			<view class="job-detail-header">
				<view class="job-detail-company">
					<view class="company-logo-large">{{ companyLogo }}</view>
					<view>
						<text class="job-detail-title">{{ job.title }}</text>
						<text class="job-detail-salary">{{ job.salaryText }}</text>
					</view>
				</view>
				<view class="job-detail-tags">
					<text class="job-detail-tag">{{ job.location }}</text>
					<text class="job-detail-tag">{{ job.experience }}</text>
					<text class="job-detail-tag">{{ job.education }}</text>
					<text v-if="job.jobType" class="job-detail-tag">{{ job.jobType }}</text>
				</view>
			</view>

			<!-- 公司信息 -->
			<view class="job-detail-section">
				<text class="job-detail-section-title">公司信息</text>
				<text class="job-detail-text">{{ job.companyName }}</text>
				<text v-if="job.companyDesc" class="job-detail-text" style="margin-top:8px;">{{ job.companyDesc }}</text>
			</view>

			<!-- 职位描述 -->
			<view class="job-detail-section">
				<text class="job-detail-section-title">职位描述</text>
				<text class="job-detail-text">{{ job.description || '暂无描述' }}</text>
			</view>

			<!-- 任职要求 -->
			<view v-if="job.requirements" class="job-detail-section">
				<text class="job-detail-section-title">任职要求</text>
				<text class="job-detail-text">{{ job.requirements }}</text>
			</view>

			<!-- 匹配度（模拟） -->
			<view class="job-detail-section">
				<text class="job-detail-section-title">AI 匹配度</text>
				<view class="match-bar">
					<view class="match-fill" :style="{ width: matchScore + '%' }"></view>
				</view>
				<text class="match-text">{{ matchScore }}% 匹配 · 基于你的技能和简历分析</text>
			</view>
		</scroll-view>

		<!-- 底部操作栏 -->
		<view class="job-detail-bottom">
			<view class="bottom-action favorite-btn" @click="toggleFavorite">
				<text>{{ isFavorited ? '❤️' : '🤍' }}</text>
				<text class="action-label">{{ isFavorited ? '已收藏' : '收藏' }}</text>
			</view>
			<button class="btn-large" :class="isDelivered ? 'btn-disabled' : 'btn-primary'" :disabled="isDelivered" @click="handleDeliver">{{ isDelivered ? '已投递' : '立即投递' }}</button>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { jobAPI, deliveryAPI, favoriteAPI, resumeAPI } from '@/utils/request'

const job = ref({})
const isFavorited = ref(false)
const isDelivered = ref(false)
const matchScore = ref(85)

onMounted(async () => {
	const pages = getCurrentPages()
	const currentPage = pages[pages.length - 1]
	// uni-app 标准参数获取方式：options.id
	const jobId = (currentPage.$page && currentPage.$page.options && currentPage.$page.options.id)
		|| (currentPage.options && currentPage.options.id)
		|| ''
	if (!jobId) {
		uni.showToast({ title: '参数错误', icon: 'none' })
		return
	}
	await loadJobDetail(jobId)
	await loadUserState()
})

const companyLogo = computed(() => {
	const name = job.value.companyName || ''
	return name.charAt(0) || '企'
})

const loadJobDetail = async (id) => {
	try {
		const res = await jobAPI.getJobDetail(id)
		job.value = res.data || {}
	} catch (e) {
		console.log('加载岗位详情失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

const getStudentId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		// 兼容 id 和 userId 两种字段名
		const sid = obj.id || obj.userId
		return sid ? Number(sid) : null
	} catch (e) { return null }
}

const loadUserState = async () => {
	const sid = getStudentId()
	const theId = job.value && job.value.id
	if (!sid || !theId) {
		console.log('跳过：studentId或jobId为空')
		return
	}
	try {
		const [dRes, fRes] = await Promise.all([
			deliveryAPI.getDeliveriesByStudentId({ studentId: sid }),
			favoriteAPI.getFavorites({ studentId: sid })
		])
		isDelivered.value = (dRes.data || []).some(d => Number(d.jobId) === Number(theId))
		isFavorited.value = (fRes.data || []).some(f => Number(f.jobId) === Number(theId))
	} catch (e) {
		console.log('加载用户状态失败', e)
	}
}

const handleDeliver = async () => {
	if (isDelivered.value) return
	const sid = getStudentId()
	const theId = job.value && job.value.id
	if (!sid || !theId) {
		uni.showToast({ title: '请先登录', icon: 'none' })
		return
	}
	// 检查是否有简历
	try {
		const resumeRes = await resumeAPI.getResume()
		if (!resumeRes || !resumeRes.data) {
			uni.showModal({
				title: '无简历',
				content: '投递前请先创建简历，是否前往创建？',
				success: (r) => {
					if (r.confirm) {
						uni.navigateTo({ url: '/pages/student/resume-edit' })
					}
				}
			})
			return
		}
	} catch (e) {
		uni.showToast({ title: '检查简历失败', icon: 'none' })
		return
	}
	try {
		await deliveryAPI.createDelivery({ jobId: theId, studentId: sid })
		isDelivered.value = true
		uni.showToast({ title: '投递成功', icon: 'success' })
	} catch (e) {
		if (e && e.message && e.message.includes('重复投递')) {
			isDelivered.value = true
			uni.showToast({ title: '已投递过', icon: 'none' })
		} else {
			uni.showToast({ title: '投递失败', icon: 'none' })
		}
	}
}

const toggleFavorite = async () => {
	const sid = getStudentId()
	const theId = job.value && job.value.id
	if (!sid || !theId) {
		uni.showToast({ title: '请先登录', icon: 'none' })
		return
	}
	try {
		if (isFavorited.value) {
			await favoriteAPI.removeFavorite(theId, sid)
			isFavorited.value = false
			uni.showToast({ title: '已取消收藏', icon: 'none' })
		} else {
			await favoriteAPI.addFavorite({ jobId: theId, studentId: sid })
			isFavorited.value = true
			uni.showToast({ title: '已收藏', icon: 'success' })
		}
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'none' })
	}
}

const goBack = () => {
	try {
		uni.navigateBack()
	} catch (e) {
		uni.reLaunch({ url: '/pages/student/home' })
	}
}
</script>

<style scoped>
.detail-back {
	padding: 12px 16px;
	font-size: 16px;
	color: #165DFF;
	font-weight: 600;
	background: white;
	flex-shrink: 0;
}
.job-detail-header {
	background: white;
	padding: 20px 16px;
	margin-bottom: 12px;
}
.job-detail-company {
	flex-direction: row;
	align-items: center;
	gap: 12px;
	margin-bottom: 16px;
}
.company-logo-large {
	width: 64px;
	height: 64px;
	border-radius: 12px;
	background: linear-gradient(135deg, #165DFF, #60A5FA);
	align-items: center;
	justify-content: center;
	color: white;
	font-size: 28px;
	font-weight: bold;
}
.job-detail-title {
	font-size: 20px;
	font-weight: 700;
	color: #1D2129;
	display: block;
	margin-bottom: 8px;
}
.job-detail-salary {
	font-size: 18px;
	color: #165DFF;
	font-weight: 700;
}
.job-detail-tags {
	flex-direction: row;
	gap: 8px;
	flex-wrap: wrap;
}
.job-detail-tag {
	padding: 6px 12px;
	background: #F2F3F5;
	border-radius: 6px;
	font-size: 13px;
	color: #4E5969;
}
.job-detail-section {
	background: white;
	margin-bottom: 12px;
	padding: 16px;
}
.job-detail-section-title {
	font-size: 16px;
	font-weight: 700;
	color: #1D2129;
	margin-bottom: 12px;
	padding-left: 12px;
	border-left: 4px solid #165DFF;
	display: block;
}
.job-detail-text {
	font-size: 14px;
	color: #4E5969;
	line-height: 1.8;
	display: block;
	white-space: pre-line;
}
.match-bar {
	height: 8px;
	background: #F2F3F5;
	border-radius: 4px;
	overflow: hidden;
	margin-bottom: 8px;
}
.match-fill {
	height: 100%;
	background: linear-gradient(90deg, #165DFF, #60A5FA);
	border-radius: 4px;
}
.match-text {
	font-size: 13px;
	color: #86909C;
}
.job-detail-bottom {
	background: white;
	padding: 12px 16px 24px;
	border-top: 1px solid #F2F3F5;
	flex-direction: row;
	gap: 12px;
	flex-shrink: 0;
	align-items: center;
}
.bottom-action {
	align-items: center;
	gap: 4px;
	padding: 8px;
}
.favorite-btn text:first-child {
	font-size: 24px;
}
.action-label {
	font-size: 12px;
	color: #86909C;
}
.btn-large {
	flex: 1;
	padding: 14px;
	border-radius: 12px;
	font-size: 16px;
	font-weight: 700;
	align-items: center;
	justify-content: center;
}
.btn-primary {
	background: #165DFF;
	color: white;
	box-shadow: 0 4px 14px rgba(22,93,255,0.4);
}
</style>
