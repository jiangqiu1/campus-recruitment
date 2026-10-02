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

			<!-- AI 匹配结论（决策卡：回答"适不适合我、要不要投"） -->
			<view v-if="matchRecord" class="match-hero">
				<view class="match-hero-top">
					<view class="match-ring" :style="{ borderColor: matchVerdict.color, backgroundColor: matchVerdict.bg }">
						<text class="match-ring-num" :style="{ color: matchVerdict.color }">{{ matchScore }}%</text>
					</view>
					<view class="match-hero-texts">
						<text class="match-verdict" :style="{ color: matchVerdict.color }">{{ matchVerdict.text }}</text>
						<text class="match-reason">{{ matchRecord.matchReason || '基于你的简历与岗位要求综合评估' }}</text>
					</view>
				</view>
				<view class="match-dims" v-if="matchDims.length">
					<view v-for="(d, i) in matchDims" :key="i" class="match-dim-row">
						<text class="match-dim-label">{{ d.label }}</text>
						<view class="match-dim-track"><view class="match-dim-fill" :style="{ width: d.value + '%', background: d.color }" /></view>
						<text class="match-dim-val">{{ d.value }}%</text>
					</view>
				</view>
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
				<view v-if="descriptionLines.length > 1" class="section-list">
					<view v-for="(line, i) in descriptionLines" :key="i" class="section-li">
						<text class="section-li-dot">•</text>
						<text class="section-li-text">{{ line }}</text>
					</view>
				</view>
				<text v-else class="section-text">{{ job.description || '暂无描述' }}</text>
			</view>

			<!-- 任职要求 -->
			<view v-if="job.requirements" class="section-card">
				<text class="section-title">任职要求</text>
				<view v-if="requirementLines.length > 1" class="section-list">
					<view v-for="(line, i) in requirementLines" :key="i" class="section-li">
						<text class="section-li-dot">•</text>
						<text class="section-li-text">{{ line }}</text>
					</view>
				</view>
				<text v-else class="section-text">{{ job.requirements }}</text>
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
import { jobAPI, deliveryAPI, favoriteAPI, resumeAPI, hrAPI, matchAPI } from '@/utils/request'
import { formatSalary, formatTimeSemantic } from '@/utils/format'
import { addBrowseRecord } from '@/utils/browseHistory'
import NavBar from '@/components/NavBar.vue'

const job = ref({})
const isFavorited = ref(false)
const isDelivered = ref(false)
const matchRecord = ref(null)
const jobId = ref('')
const company = ref({})

// 匹配度：来自真实人岗匹配记录（教师推送/生成），无记录时决策卡整体隐藏
const matchScore = computed(() => {
	if (!matchRecord.value) return 0
	const v = Number(matchRecord.value.matchScore) || 0
	return v > 1 ? Math.round(v) : Math.round(v * 100)
})

const matchVerdict = computed(() => {
	const s = matchScore.value
	if (s >= 80) return { text: '非常适合投递', color: '#00B42A', bg: 'rgba(0,180,42,0.08)' }
	if (s >= 60) return { text: '比较匹配', color: '#165DFF', bg: 'rgba(22,93,255,0.08)' }
	if (s >= 40) return { text: '可以尝试', color: '#FF7D00', bg: 'rgba(255,125,0,0.1)' }
	return { text: '匹配度较低', color: '#F53F3F', bg: 'rgba(245,63,63,0.08)' }
})

const matchDims = computed(() => {
	const sd = (matchRecord.value && matchRecord.value.scoreDetail) || {}
	if (!Object.keys(sd).length) return []
	const defs = [['skillMatch', '技能匹配'], ['expMatch', '经验匹配'], ['eduMatch', '学历匹配']]
	return defs.map(([key, label]) => {
		const raw = Number(sd[key]) || 0
		const value = raw > 1 ? Math.round(raw) : Math.round(raw * 100)
		return { label, value, color: value >= 80 ? '#00B42A' : value >= 60 ? '#165DFF' : '#FF7D00' }
	})
})

// JD 结构化：按换行拆成条目，去掉"1."类序号
const splitLines = (text) => (text || '').split(/\n+/).map(s => s.trim().replace(/^\d+[\.、]\s*/, '')).filter(Boolean)
const descriptionLines = computed(() => splitLines(job.value.description))
const requirementLines = computed(() => splitLines(job.value.requirements))

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
		// 薪资格式统一（5K–8K），与岗位卡片一致
		job.value.salaryText = formatSalary(job.value.salaryText || job.value.salaryRange)
		addBrowseRecord(id)
		loadUserState()
		loadCompany()
		loadMatch()
	} catch (e) {
		console.log('加载岗位详情失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

// 拉当前学生在该岗位的人岗匹配记录
const loadMatch = async () => {
	const sid = getStudentId()
	if (!sid || !job.value.id) return
	try {
		const res = await matchAPI.getByStudent(sid)
		matchRecord.value = (res.data || []).find(m => Number(m.jobId) === Number(job.value.id)) || null
	} catch (e) {
		matchRecord.value = null
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
	// 更新时间语义化（今天/昨天/周X/M月D日），见 utils/format.js
	return formatTimeSemantic(time) || '刚刚'
}
</script>

<style scoped lang="scss">
.job-header { background: $uni-bg-color; padding: 20px 16px; margin-bottom: 12px; }
.job-title-row { flex-direction: row; align-items: center; gap: 8px; margin-bottom: 8px; }
.job-title { font-size: 20px; font-weight: 700; color: $uni-text-color-title; flex: 1; }
.tag-urgent { font-size: 12px; color: $uni-color-error; background: $uni-color-error-light; padding: 2px 8px; border-radius: 4px; font-weight: 600; flex-shrink: 0; }
.tag-campus { font-size: 12px; color: $uni-color-primary; background: $uni-color-primary-light; padding: 2px 8px; border-radius: 4px; font-weight: 500; flex-shrink: 0; }
.job-salary { font-size: 20px; color: $uni-color-error; font-weight: 700; display: block; margin-bottom: 12px; }
.job-base-tags { flex-direction: row; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.base-tag { font-size: 13px; color: $uni-text-color; background: $uni-bg-color-page; padding: 4px 10px; border-radius: 4px; }
.job-update-time { font-size: 12px; color: $uni-text-color-placeholder; }
.company-info { flex-direction: row; align-items: center; padding: 12px 16px; background: $uni-bg-color; margin-top: 12px; gap: 12px; }
.company-icon { width: 48px; height: 48px; border-radius: 8px; background: linear-gradient(135deg, $uni-color-primary, $uni-color-primary-lighter); align-items: center; justify-content: center; color: white; font-size: 20px; font-weight: bold; flex-shrink: 0; }
.company-meta { flex: 1; flex-direction: column; gap: 2px; margin-left: 10px; }
.company-name { font-size: 14px; font-weight: 600; color: $uni-text-color-title; }
.company-sub { font-size: 12px; color: $uni-text-color-secondary; }
.company-info:active { background: $uni-bg-color-page; }
.section-card { background: $uni-bg-color; margin-bottom: 12px; padding: 20px 16px; }
.section-title { font-size: 16px; font-weight: 700; color: $uni-text-color-title; margin-bottom: 12px; padding-left: 12px; border-left: 4px solid $uni-color-primary; display: block; }
.section-text { font-size: 14px; color: $uni-text-color; line-height: 1.8; display: block; white-space: pre-line; }
.highlight-tags { flex-direction: row; flex-wrap: wrap; gap: 8px; }
.highlight-tag { font-size: 13px; color: $uni-color-primary; background: $uni-color-primary-light; padding: 6px 12px; border-radius: 8px; font-weight: 500; }
.info-row { flex-direction: row; align-items: center; padding: 8px 0; }
.info-label { font-size: 14px; color: $uni-text-color-secondary; width: 80px; flex-shrink: 0; }
.info-value { font-size: 14px; color: $uni-text-color-title; flex: 1; }
/* AI 匹配决策卡 */
.match-hero { background: $uni-bg-color; padding: 16px; margin-bottom: 12px; }
.match-hero-top { flex-direction: row; align-items: center; gap: 14px; }
.match-ring { width: 60px; height: 60px; border-radius: 50%; border-width: 3px; border-style: solid; align-items: center; justify-content: center; flex-shrink: 0; }
.match-ring-num { font-size: 15px; font-weight: 800; }
.match-hero-texts { flex: 1; }
.match-verdict { font-size: 16px; font-weight: 700; display: block; }
.match-reason { font-size: 12px; color: $uni-text-color-secondary; margin-top: 4px; line-height: 1.5; display: block; }
.match-dims { margin-top: 14px; gap: 8px; }
.match-dim-row { flex-direction: row; align-items: center; gap: 8px; }
.match-dim-label { width: 56px; font-size: 12px; color: $uni-text-color; }
.match-dim-track { flex: 1; height: 6px; background: $uni-border-color-divider; border-radius: 3px; overflow: hidden; }
.match-dim-fill { height: 100%; border-radius: 3px; }
.match-dim-val { width: 36px; font-size: 12px; font-weight: 600; color: $uni-text-color; text-align: right; }
.section-li { flex-direction: row; gap: 8px; margin-bottom: 6px; }
.section-li:last-child { margin-bottom: 0; }
.section-li-dot { color: $uni-color-primary; font-size: 14px; line-height: 1.7; }
.section-li-text { flex: 1; font-size: 14px; color: $uni-text-color; line-height: 1.7; }
.address-box { flex-direction: row; align-items: center; gap: 8px; padding: 12px; background: $uni-bg-color-page; border-radius: 8px; }
.address-text { font-size: 14px; color: $uni-text-color; flex: 1; }
.safety-tip { margin: 0 16px 16px; padding: 12px 14px; background: $uni-color-warning-light; border-radius: 8px; flex-direction: row; align-items: flex-start; gap: 8px; }
.tip-text { font-size: 12px; color: $uni-color-warning; line-height: 1.6; flex: 1; }
.bottom-bar { position: fixed; bottom: 0; left: 0; right: 0; background: $uni-bg-color; padding: 10px 16px; padding-bottom: calc(10px + env(safe-area-inset-bottom)); border-top: 0.5px solid $uni-border-color-divider; flex-direction: row; align-items: center; gap: 16px; box-shadow: 0 -2px 12px rgba(0,0,0,0.04); z-index: 99; }
.action-item { align-items: center; justify-content: center; gap: 2px; width: 56px; flex-shrink: 0; }
.action-label { font-size: 12px; color: $uni-text-color-secondary; line-height: 1; margin-top: 2px; }
.deliver-btn { flex: 1; padding: 0 16px; border-radius: 999px; height: 48px; font-size: 16px; font-weight: 600; align-items: center; justify-content: center; border: none; }
.btn-primary { background: $uni-color-primary; color: white; }
.btn-disabled { background: $uni-border-color; color: $uni-text-color-placeholder; }
.btn-primary:active { opacity: 0.85; }
</style>
