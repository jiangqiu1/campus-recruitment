<template>
	<view class="page-wrapper">
		<NavBar title="我的岗位" :showBack="false" />

		<!-- AI 智能写岗位入口 -->
		<view class="ai-job-banner" @click="showAiModal = true">
			<view class="ai-job-banner__left">
				<view class="ai-job-banner__icon">
					<uni-icons type="star" size="20" color="#8B5CF6" />
				</view>
				<view>
					<text class="ai-job-banner__title">AI 智能写岗位</text>
					<text class="ai-job-banner__desc">粘贴岗位描述，AI 自动提取信息并填充表单</text>
				</view>
			</view>
			<uni-icons type="arrowright" size="16" color="#8B5CF6" />
		</view>

		<!-- 顶部筛选标签 -->
		<view class="filter-tabs">
			<text v-for="(tab, i) in tabs" :key="i" class="filter-tab" :class="{ active: currentTab === tab.value }" @click="currentTab = tab.value">{{ tab.label }}</text>
		</view>

		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<view class="job-list">
				<view v-for="(job, i) in filteredList" :key="i" class="job-card" @click="goToEdit(job)">
					<view class="job-top">
						<view class="job-title-row">
							<text class="job-title">{{ job.title }}</text>
							<text class="status-tag" :class="'tag-' + job.status">{{ job.statusText }}</text>
						</view>
						<view class="job-info-row">
							<text class="job-salary">{{ job.salaryText }}</text>
							<text class="job-divider">|</text>
							<text class="job-delivery">{{ job.deliveryCount }}人投递</text>
						</view>
					</view>
					<view class="job-actions">
						<text v-if="job.status === 'draft'" class="action-tag primary" @click.stop="handlePublish(job.id)">发布</text>
						<text v-if="job.status === 'active'" class="action-tag warning" @click.stop="handleClose(job.id)">下架</text>
						<text v-if="job.status === 'closed'" class="action-tag primary" @click.stop="handlePublish(job.id)">上架</text>
						<text class="action-tag outline" @click.stop="goToEdit(job)">编辑</text>
					</view>
				</view>
				<EmptyState v-if="!filteredList.length" icon="inbox" title="暂无岗位" desc="点击右下角+号新建" />
			</view>
			<view style="height: calc(60px + env(safe-area-inset-bottom))" />
		</scroll-view>

		<!-- 底部浮动新建按钮 -->
		<view class="fab-btn" @click="goToCreate">
			<text class="fab-icon">+</text>
		</view>

		<!-- AI 智能写岗位弹窗 -->
		<PopupDrawer :show="showAiModal" @update:show="showAiModal = $event" title="AI 智能写岗位">
			<view class="ai-modal-body">
				<text class="ai-modal-desc">粘贴岗位描述文本，AI 将自动提取岗位名称、薪资、地点、学历等信息</text>
				<textarea v-model="aiJobText" class="ai-modal-input" placeholder="在此粘贴完整的岗位描述..." :maxlength="3000" />
				<text class="ai-modal-count">{{ aiJobText.length }}/3000</text>
				<view class="ai-modal-btn" :class="{ disabled: !aiJobText.trim() }" @click="handleAiParse">
					<uni-icons v-if="!aiParsing" type="star" size="16" color="#FFFFFF" />
					<text>{{ aiParsing ? '解析中...' : 'AI 解析并跳转' }}</text>
				</view>
			</view>
		</PopupDrawer>

		<HrTabBar current="jobs" />
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@/utils/page-lifecycle'
import { hrAPI, aiParseAPI } from '@/utils/request'
import HrTabBar from '@/components/HrTabBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import NavBar from '@/components/NavBar.vue'
import PopupDrawer from '@/components/PopupDrawer.vue'

const showAiModal = ref(false)
const aiJobText = ref('')
const aiParsing = ref(false)

const tabs = [
	{ label: '全部', value: 'all' },
	{ label: '已发布', value: 'active' },
	{ label: '草稿', value: 'draft' },
	{ label: '已关闭', value: 'closed' }
]
const refreshing = ref(false)
const currentTab = ref('all')
const jobs = ref([])

const STATUS_MAP = ['draft', 'active', 'closed', 'paused']
const STATUS_TEXT_MAP = ['草稿', '已发布', '已关闭', '已暂停']

const mapJob = (job) => ({
	id: job.id,
	title: job.title || '',
	status: STATUS_MAP[job.status] || 'draft',
	statusText: STATUS_TEXT_MAP[job.status] || '草稿',
	deliveryCount: job._deliveryCount || 0,
	salaryText: job.salaryRange || '面议'
})

const filteredList = computed(() => {
	if (currentTab.value === 'all') return jobs.value
	return jobs.value.filter(j => j.status === currentTab.value)
})

const getCompanyId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.companyId || obj.id || null
	} catch (e) {
		console.error('获取公司ID失败', e)
		return null
	}
}

onMounted(async () => {
	const cId = getCompanyId()
	if (cId) await loadJobs(cId)
})

onShow(() => {
	const cId = getCompanyId()
	if (cId) loadJobs(cId)
})

const onRefresh = async () => {
	refreshing.value = true
	const cId = getCompanyId()
	if (cId) await loadJobs(cId)
	refreshing.value = false
}

const loadJobs = async (cId) => {
	try {
		const res = await hrAPI.getHrJobs(cId)
		const raw = res.data || []
		jobs.value = raw.map(mapJob)
		raw.forEach(j => {
			hrAPI.getJobStats(j.id).then(sRes => {
				const stats = sRes.data
				if (stats) {
					const idx = jobs.value.findIndex(jj => jj.id === j.id)
					if (idx > -1) jobs.value[idx].deliveryCount = stats.deliveryCount || 0
				}
			}).catch(e => console.error('加载岗位统计失败', e))
		})
	} catch (e) {
		console.error('加载岗位列表失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

const handlePublish = async (id) => {
	try {
		await hrAPI.publishJob(id)
		uni.showToast({ title: '发布成功', icon: 'success' })
		const idx = jobs.value.findIndex(j => j.id === id)
		if (idx > -1) { jobs.value[idx].status = 'active'; jobs.value[idx].statusText = '已发布' }
	} catch (e) { uni.showToast({ title: '操作失败', icon: 'none' }) }
}

const handleClose = async (id) => {
	try {
		await hrAPI.closeJob(id)
		uni.showToast({ title: '已下架', icon: 'success' })
		const idx = jobs.value.findIndex(j => j.id === id)
		if (idx > -1) { jobs.value[idx].status = 'closed'; jobs.value[idx].statusText = '已关闭' }
	} catch (e) { uni.showToast({ title: '操作失败', icon: 'none' }) }
}

const goToEdit = (job) => {
	uni.navigateTo({ url: '/pages/hr/job-edit?id=' + job.id })
}

const goToCreate = () => {
	uni.navigateTo({ url: '/pages/hr/job-edit' })
}

// AI 智能写岗位
const handleAiParse = async () => {
	if (aiParsing.value || !aiJobText.value.trim()) return
	aiParsing.value = true
	try {
		const res = await aiParseAPI.parseJob(aiJobText.value)
		const data = res.data || {}
		uni.setStorageSync('ai_parsed_job', data)
		showAiModal.value = false
		aiJobText.value = ''
		uni.navigateTo({ url: '/pages/hr/job-edit?aiParsed=1' })
	} catch (e) {
		console.error('AI解析失败', e)
		uni.showToast({ title: 'AI 解析失败', icon: 'none' })
	} finally {
		aiParsing.value = false
	}
}
</script>

<style scoped>
/* ===== 筛选标签 ===== */
.filter-tabs {
	flex-direction: row;
	gap: 8px;
	padding: 12px 16px;
	background: white;
	overflow-x: auto;
	flex-shrink: 0;
}
.filter-tab {
	padding: 8px 16px;
	border-radius: 20px;
	font-size: 13px;
	font-weight: 500;
	color: #4E5969;
	background: #F2F3F5;
	white-space: nowrap;
}
.filter-tab.active {
	background: rgba(22,93,255,0.1);
	color: #165DFF;
	font-weight: 600;
}

/* ===== 岗位卡片 ===== */
.job-list { padding: 12px 16px; }
.job-card {
	background: #FFFFFF;
	border-radius: 12px;
	padding: 16px;
	margin-bottom: 10px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.job-card:active { background: #F7F8FA; }
.job-top { margin-bottom: 12px; }
.job-title-row {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 6px;
}
.job-title {
	font-size: 16px;
	font-weight: 700;
	color: #1D2129;
	flex: 1;
	margin-right: 8px;
}
.job-info-row { flex-direction: row; align-items: center; gap: 8px; }
.job-salary {
	font-size: 15px;
	color: #F53F3F;
	font-weight: 700;
	background: rgba(245,63,63,0.06);
	padding: 2px 8px;
	border-radius: 4px;
}
.job-divider { font-size: 12px; color: #E5E6EB; }
.job-delivery { font-size: 12px; color: #86909C; }

/* ===== 状态标签 ===== */
.status-tag {
	padding: 3px 10px;
	border-radius: 6px;
	font-size: 12px;
	font-weight: 600;
	flex-shrink: 0;
}
.tag-active { background: rgba(0,180,42,0.1); color: #00B42A; }
.tag-draft { background: rgba(245,158,11,0.1); color: #F59E0B; }
.tag-closed { background: rgba(201,205,212,0.3); color: #86909C; }

/* ===== 操作标签 ===== */
.job-actions { flex-direction: row; gap: 10px; }
.action-tag {
	flex: 1;
	text-align: center;
	padding: 8px 0;
	border-radius: 8px;
	font-size: 13px;
	font-weight: 600;
}
.action-tag.primary { background: #165DFF; color: #FFFFFF; }
.action-tag.warning { background: #F59E0B; color: #FFFFFF; }
.action-tag.outline { background: #FFFFFF; border: 1px solid #E5E6EB; color: #4E5969; }

/* ===== 浮动新建按钮 ===== */
.fab-btn {
	position: fixed;
	right: 24px;
	bottom: 90px;
	width: 56px;
	height: 56px;
	border-radius: 50%;
	background: linear-gradient(135deg, #165DFF, #2563EB);
	align-items: center;
	justify-content: center;
	box-shadow: 0 4px 16px rgba(22,93,255,0.4);
	z-index: 100;
}
.fab-icon {
	font-size: 32px;
	color: white;
	font-weight: 300;
	margin-top: -2px;
}

/* ===== AI 智能写岗位 ===== */
.ai-job-banner {
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
	background: rgba(139,92,246,0.06);
	border: 1px solid rgba(139,92,246,0.15);
	border-radius: 12px;
	margin: 12px 16px;
	padding: 12px 16px;
}
.ai-job-banner:active { opacity: 0.8; }
.ai-job-banner__left {
	flex-direction: row;
	align-items: center;
	gap: 10px;
	flex: 1;
}
.ai-job-banner__icon {
	width: 36px;
	height: 36px;
	border-radius: 10px;
	background: rgba(139,92,246,0.12);
	align-items: center;
	justify-content: center;
}
.ai-job-banner__title {
	font-size: 14px;
	font-weight: 600;
	color: #7C3AED;
}
.ai-job-banner__desc {
	font-size: 11px;
	color: #A78BFA;
	margin-top: 1px;
}

/* AI 写岗位弹窗 */
.ai-modal-body { padding: 0; width: 100%; overflow-x: hidden; }
.ai-modal-desc { font-size: 13px; color: #86909C; margin-bottom: 12px; display: block; }
.ai-modal-input {
	width: 100%;
	max-width: 100%;
	height: 100px;
	border: 1px solid #E5E6EB;
	border-radius: 8px;
	padding: 12px;
	font-size: 14px;
	line-height: 1.6;
	background: #F7F8FA;
	box-sizing: border-box;
}
.ai-modal-count { font-size: 11px; color: #C9CDD4; text-align: right; margin-top: 4px; display: block; }
.ai-modal-btn {
	flex-direction: row;
	align-items: center;
	justify-content: center;
	gap: 6px;
	width: 100%;
	max-width: 100%;
	height: 44px;
	border-radius: 8px;
	background: linear-gradient(135deg, #8B5CF6, #7C3AED);
	color: #fff;
	font-size: 15px;
	font-weight: 600;
	margin-top: 12px;
	margin-bottom: 12px;
	flex-shrink: 0;
	overflow: hidden;
}
.ai-modal-btn:active { opacity: 0.85; }
.ai-modal-btn.disabled { background: #E5E6EB !important; color: #A9AEB8 !important; }
</style>
