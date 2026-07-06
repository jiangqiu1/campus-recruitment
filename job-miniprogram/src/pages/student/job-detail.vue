<template>
	<view class="page-wrapper">
		<NavBar title="岗位详情" show-back @back="goBack" />
		<scroll-view class="content-scrollable" scroll-y>
			<!-- 岗位核心信息 -->
			<view class="job-header">
				<view class="job-title-row">
					<text class="job-title">{{ job.title }}</text>
					<text v-if="job.urgent" class="tag-urgent">急</text>
					<text v-if="job.campus" class="tag-campus">校招</text>
				</view>
				<text class="job-salary">{{ job.salaryText || '面议' }}</text>
				<view class="job-base-tags">
					<text class="base-tag">{{ job.location || '待定' }}</text>
					<text class="base-tag">{{ job.experience || '经验不限' }}</text>
					<text class="base-tag">{{ job.education || '学历不限' }}</text>
					<text v-if="job.recruitCount" class="base-tag">招{{ job.recruitCount }}人</text>
					<text v-if="job.canConvert" class="base-tag">可转正</text>
				</view>
				<view class="job-update-time">更新于 {{ formatDate(job.updateTime) }}</view>
			</view>

			<!-- 公司信息入口 -->
			<view class="company-info" @click="goToCompany">
				<view class="company-icon">{{ companyLogo }}</view>
				<view class="company-meta">
					<text class="company-name">{{ company.name || job.companyName || '企业名称' }}</text>
					<text class="company-sub">{{ company.industry || job.industry || '' }} {{ company.size || job.staffSize ? '· ' + (company.size || job.staffSize) : '' }}</text>
				</view>
				<uni-icons type="arrowright" size="16" color="#C9CDD4" />
			</view>

			<!-- 职位亮点 -->
			<view v-if="job.highlights && job.highlights.length" class="section-card">
				<text class="section-title">职位亮点</text>
				<view class="highlight-tags">
					<text v-for="(item, i) in job.highlights" :key="i" class="highlight-tag">{{ item }}</text>
				</view>
			</view>

			<!-- 岗位职责 -->
			<view class="section-card">
				<text class="section-title">岗位职责</text>
				<text class="section-text">{{ job.description || '暂无描述' }}</text>
			</view>

			<!-- 任职要求 -->
			<view v-if="job.requirements" class="section-card">
				<text class="section-title">任职要求</text>
				<text class="section-text">{{ job.requirements }}</text>
			</view>

			<!-- 投递须知 -->
			<view class="section-card">
				<text class="section-title">投递须知</text>
				<view class="info-row">
					<text class="info-label">简历要求</text>
					<text class="info-value">{{ job.resumeRequire || '中文简历' }}</text>
				</view>
				<view class="info-row">
					<text class="info-label">截止日期</text>
					<text class="info-value">{{ job.deadline || '招满即止' }}</text>
				</view>
			</view>

			<!-- AI 匹配度 -->
			<view class="section-card">
				<text class="section-title">AI 匹配度</text>
				<view class="match-bar">
					<view class="match-fill" :style="{ width: matchScore + '%' }"></view>
				</view>
				<text class="match-text">{{ matchScore }}% 匹配 · 基于你的技能和简历分析</text>
			</view>

			<!-- 工作地点 -->
			<view class="section-card">
				<text class="section-title">工作地点</text>
				<view class="address-box">
					<uni-icons type="location" size="16" color="#165DFF" />
					<text class="address-text">{{ job.address || job.location || '待定' }}</text>
				</view>
			</view>

			<!-- 安全提示 -->
			<view class="safety-tip">
				<uni-icons type="info-filled" size="14" color="#FF7D00" />
				<text class="tip-text">求职过程中如遇虚假宣传、收取财物等情况，请立即举报</text>
			</view>

			<view style="height: calc(80px + env(safe-area-inset-bottom))" />
		</scroll-view>

		<!-- 底部操作栏 -->
		<view class="bottom-bar">
			<view class="action-item" @click="handleShare">
				<uni-icons type="redo" size="22" color="#86909C" />
				<text class="action-label">分享</text>
			</view>
			<view class="action-item" @click="toggleFavorite">
				<uni-icons :type="isFavorited ? 'star-filled' : 'star'" size="22" :color="isFavorited ? '#FF7D00' : '#86909C'" />
				<text class="action-label">{{ isFavorited ? '已收藏' : '收藏' }}</text>
			</view>
			<button class="deliver-btn" :class="isDelivered ? 'btn-disabled' : 'btn-primary'" :disabled="isDelivered" @click="handleDeliver">
				{{ isDelivered ? '已投递' : '立即投递' }}
			</button>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { jobAPI, deliveryAPI, favoriteAPI, resumeAPI, hrAPI } from '@/utils/request'
import { addBrowseRecord } from '@/utils/browseHistory'
import NavBar from '@/components/NavBar.vue'

const job = ref({})
const isFavorited = ref(false)
const isDelivered = ref(false)
const matchScore = ref(85)
const jobId = ref('')
const company = ref({})

const companyLogo = computed(() => {
	const name = company.value.name || job.value.companyName || ''
	return name.charAt(0) || '企'
})

onLoad((options) => {
	if (options?.id) {
		jobId.value = options.id
		loadJobDetail(jobId.value)
	} else {
		uni.showToast({ title: '参数错误', icon: 'none' })
	}
})

const loadJobDetail = async (id) => {
	try {
		const res = await jobAPI.getJobDetail(id)
		job.value = res.data || {}
		addBrowseRecord(id)
		loadUserState()
		loadCompany()
	} catch (e) {
		console.log('加载岗位详情失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

const loadCompany = async () => {
	const cid = job.value.companyId
	if (!cid) return
	try {
		const res = await hrAPI.getCompanyProfile(cid)
		if (res.data) company.value = res.data
	} catch (e) {
		console.log('加载企业信息失败', e)
	}
}

const getStudentId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		const sid = obj.id || obj.userId
		return sid ? Number(sid) : null
	} catch (e) { return null }
}

const loadUserState = async () => {
	const sid = getStudentId()
	const theId = job.value && job.value.id
	if (!sid || !theId) return
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
	try {
		const resumeRes = await resumeAPI.getResume()
		if (!resumeRes || !resumeRes.data) {
			uni.showModal({
				title: '无简历',
				content: '投递前请先创建简历，是否前往创建？',
				success: (r) => {
					if (r.confirm) { uni.navigateTo({ url: '/pages/student/resume-edit' }) }
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
	if (!sid || !theId) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
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
	} catch (e) { uni.showToast({ title: '操作失败', icon: 'none' }) }
}

const handleShare = () => { uni.showToast({ title: '分享功能开发中', icon: 'none' }) }
const goToCompany = () => {
	const cid = job.value.companyId
	if (cid) {
		uni.navigateTo({ url: '/pages/student/company-detail?id=' + cid })
	} else {
		uni.showToast({ title: '暂无企业信息', icon: 'none' })
	}
}
const goCompanyDetail = goToCompany

const goBack = () => {
	try { uni.navigateBack() } catch (e) { uni.reLaunch({ url: '/pages/student/home' }) }
}

const formatDate = (time) => {
	if (!time) return '刚刚'
	const d = new Date(time)
	return `${String(d.getMonth() + 1).padStart(2, '0')}月${String(d.getDate()).padStart(2, '0')}日`
}
</script>

<style scoped>
.job-header { background: #FFFFFF; padding: 20px 16px; margin-bottom: 12px; }
.job-title-row { flex-direction: row; align-items: center; gap: 8px; margin-bottom: 8px; }
.job-title { font-size: 22px; font-weight: 700; color: #1D2129; flex: 1; }
.tag-urgent { font-size: 12px; color: #F53F3F; background: rgba(245,63,63,0.08); padding: 2px 8px; border-radius: 4px; font-weight: 600; flex-shrink: 0; }
.tag-campus { font-size: 12px; color: #165DFF; background: rgba(22,93,255,0.08); padding: 2px 8px; border-radius: 4px; font-weight: 500; flex-shrink: 0; }
.job-salary { font-size: 20px; color: #F53F3F; font-weight: 700; display: block; margin-bottom: 12px; }
.job-base-tags { flex-direction: row; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.base-tag { font-size: 13px; color: #4E5969; background: #F7F8FA; padding: 4px 10px; border-radius: 4px; }
.job-update-time { font-size: 12px; color: #C9CDD4; }
.company-info { flex-direction: row; align-items: center; padding: 12px 16px; background: #FFFFFF; margin-top: 12px; gap: 12px; }
.company-icon { width: 48px; height: 48px; border-radius: 8px; background: linear-gradient(135deg, #165DFF, #60A5FA); align-items: center; justify-content: center; color: white; font-size: 20px; font-weight: bold; flex-shrink: 0; }
.company-meta { flex: 1; flex-direction: column; gap: 2px; margin-left: 10px; }
.company-name { font-size: 14px; font-weight: 600; color: #1D2129; }
.company-sub { font-size: 12px; color: #86909C; }
.company-info:active { background: #F7F8FA; }
.section-card { background: #FFFFFF; margin-bottom: 12px; padding: 20px 16px; }
.section-title { font-size: 16px; font-weight: 700; color: #1D2129; margin-bottom: 12px; padding-left: 12px; border-left: 4px solid #165DFF; display: block; }
.section-text { font-size: 14px; color: #4E5969; line-height: 1.8; display: block; white-space: pre-line; }
.highlight-tags { flex-direction: row; flex-wrap: wrap; gap: 8px; }
.highlight-tag { font-size: 13px; color: #165DFF; background: rgba(22,93,255,0.08); padding: 6px 12px; border-radius: 6px; font-weight: 500; }
.info-row { flex-direction: row; align-items: center; padding: 8px 0; }
.info-label { font-size: 14px; color: #86909C; width: 80px; flex-shrink: 0; }
.info-value { font-size: 14px; color: #1D2129; flex: 1; }
.match-bar { height: 8px; background: #F2F3F5; border-radius: 4px; overflow: hidden; margin-bottom: 8px; }
.match-fill { height: 100%; background: linear-gradient(90deg, #165DFF, #60A5FA); border-radius: 4px; }
.match-text { font-size: 13px; color: #86909C; }
.address-box { flex-direction: row; align-items: center; gap: 8px; padding: 12px; background: #F7F8FA; border-radius: 8px; }
.address-text { font-size: 14px; color: #4E5969; flex: 1; }
.safety-tip { margin: 0 16px 16px; padding: 12px 14px; background: rgba(255,125,0,0.06); border-radius: 8px; flex-direction: row; align-items: flex-start; gap: 8px; }
.tip-text { font-size: 12px; color: #FF7D00; line-height: 1.6; flex: 1; }
.bottom-bar { position: fixed; bottom: 0; left: 0; right: 0; background: #FFFFFF; padding: 10px 16px; padding-bottom: calc(10px + env(safe-area-inset-bottom)); border-top: 0.5px solid #F2F3F5; flex-direction: row; align-items: center; gap: 16px; box-shadow: 0 -2px 12px rgba(0,0,0,0.04); z-index: 99; }
.action-item { align-items: center; justify-content: center; gap: 2px; width: 56px; flex-shrink: 0; }
.action-label { font-size: 11px; color: #86909C; line-height: 1; margin-top: 2px; }
.deliver-btn { flex: 1; padding: 0 16px; border-radius: 24px; height: 48px; font-size: 16px; font-weight: 600; align-items: center; justify-content: center; border: none; }
.btn-primary { background: #165DFF; color: white; }
.btn-disabled { background: #E5E6EB; color: #A9AEB8; }
.btn-primary:active { opacity: 0.85; }
</style>
