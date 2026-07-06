<template>
	<view class="page-wrapper">
		<NavBar title="岗位管理" :showBack="false" />

		<!-- 状态筛选 Tab -->
		<view class="list-tabs">
			<text v-for="tab in statusTabs" :key="tab.value" class="list-tab"
				:class="{ active: currentStatus === tab.value }" @click="switchStatus(tab.value)">
				{{ tab.label }}<text v-if="tab.count" class="tab-badge">{{ tab.count }}</text>
			</text>
		</view>

		<!-- AI 智能写岗位入口（对齐学生端简历页的 "AI 优化建议" 样式） -->
		<view class="ai-job-banner" @click="showAiJobModal = true">
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

		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<view class="job-list">
				<view v-for="(job, i) in filteredJobs" :key="i" class="job-card" @click="goToEdit(job)">
					<view class="job-card-top">
						<view class="job-info">
							<text class="job-title">{{ job.title }}</text>
							<text class="job-company">{{ job.companyName || '未设置公司' }}</text>
						</view>
						<text class="job-salary">{{ job.salaryText }}</text>
					</view>
					<view class="job-meta">
						<text>{{ job.location || '未设置地点' }}</text>
						<text>{{ job.deliveryCount || 0 }}人投递</text>
					</view>
					<!-- 操作按钮统一为标签样式 -->
					<view class="job-actions">
						<text v-if="job.status === 'draft'" class="action-tag primary" @click.stop="handlePublish(job)">发布</text>
						<text v-if="job.status === 'active'" class="action-tag warning" @click.stop="handleClose(job)">关闭</text>
						<text v-if="job.status === 'closed'" class="action-tag primary" @click.stop="handlePublish(job)">上架</text>
						<text class="action-tag" @click.stop="goToDeliveries(job)">投递明细</text>
						<text v-if="job.status !== 'closed'" class="action-tag ai" @click.stop="goToAiMatches(job.id)">AI匹配</text>
					</view>
				</view>
				<EmptyState v-if="!filteredJobs.length" icon="list" title="暂无岗位" desc="点击右下角 + 按钮新建" />
			</view>
			<view style="height:40px;"></view>
		</scroll-view>
		<!-- 浮动新建按钮 -->
		<view class="fab" @click="goToCreate">
			<text class="fab-icon">+</text>
		</view>

		<!-- AI 智能写岗位弹窗 -->
		<PopupDrawer :show="showAiJobModal" @update:show="showAiJobModal = $event" title="AI 智能写岗位">
			<view class="ai-modal-body">
				<text class="ai-modal-desc">粘贴岗位描述文本，AI 将自动提取岗位名称、薪资、地点、学历等信息</text>
				<textarea v-model="aiJobText" class="ai-modal-input" placeholder="在此粘贴完整的岗位描述..." :maxlength="3000" />
				<text class="ai-modal-count">{{ aiJobText.length }}/3000</text>
				<view class="ai-modal-btn" :class="{ disabled: !aiJobText.trim() }" @click="handleAiParseJob">
					<uni-icons v-if="!aiParsing" type="star" size="16" color="#FFFFFF" />
					<text>{{ aiParsing ? '解析中...' : 'AI 解析并跳转' }}</text>
				</view>
			</view>
		</PopupDrawer>
		<TabBar current="jobs" path-prefix="/pages/teacher/" :tab-list="teacherTabs" />
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@/utils/page-lifecycle'
import { teacherAPI, jobAPI, aiParseAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import TabBar from '@/components/TabBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import PopupDrawer from '@/components/PopupDrawer.vue'
import { checkRole } from '@/utils/auth'

checkRole(1)

const teacherTabs = [
	{ page: 'home', icon: 'home', activeIcon: 'home-filled', label: '首页' },
	{ page: 'classes', icon: 'staff', activeIcon: 'staff-filled', label: '班级' },
	{ page: 'jobs', icon: 'list', activeIcon: 'list', label: '岗位' },
	{ page: 'profile', icon: 'person', activeIcon: 'person-filled', label: '我的' }
]

const jobs = ref([])
const currentStatus = ref('all')
const urgentFilter = ref(false) // 首页待办「急招岗位」筛选
const refreshing = ref(false)
const showAiJobModal = ref(false)
const aiJobText = ref('')
const aiParsing = ref(false)

const statusTabs = computed(() => [
	{ label: '全部', value: 'all', count: jobs.value.length },
	{ label: '招聘中', value: 'active', count: jobs.value.filter(j => j.status === 'active').length },
	{ label: '草稿', value: 'draft', count: jobs.value.filter(j => j.status === 'draft').length },
	{ label: '已关闭', value: 'closed', count: jobs.value.filter(j => j.status === 'closed').length }
])

const filteredJobs = computed(() => {
	let list = jobs.value
	if (currentStatus.value !== 'all') {
		list = list.filter(j => j.status === currentStatus.value)
	}
	// 首页待办「急招岗位」筛选 — 仅展示标记为急招的招聘中岗位
	if (urgentFilter.value) {
		list = list.filter(j => j.urgent === true)
	}
	return list
})

const STATUS_MAP = { 0: 'draft', 1: 'active', 2: 'closed', 3: 'paused' }
const switchStatus = (val) => {
	currentStatus.value = val
	uni.setStorageSync('teacher_jobs_status', val)
}

const mapTeacherJob = (job) => ({
	id: job.id,
	title: job.title,
	companyName: job.companyName || '待加载',
	companyId: job.companyId,
	status: STATUS_MAP[job.status] || 'draft',
	location: job.location || '未设置地点',
	salaryText: job.salaryRange || '薪资面议',
	deliveryCount: job.deliveryCount || 0,
	urgent: job.urgent || false
})

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

// 初始化：URL参数解析 + 筛选记忆恢复（onShow 之前执行一次）
{
	const pages = getCurrentPages()
	const cp = pages[pages.length - 1]
	if (cp.options?.filter === 'urgent') {
		urgentFilter.value = true
		currentStatus.value = 'active'
	}
	const savedStatus = uni.getStorageSync('teacher_jobs_status')
	if (savedStatus && !urgentFilter.value) currentStatus.value = savedStatus
}

// 页面显示时刷新数据（含首次加载 + 返回刷新）
onShow(() => { loadJobs() })

const onRefresh = async () => {
	refreshing.value = true
	await loadJobs()
	refreshing.value = false
}

const goToAiMatches = (jobId) => uni.navigateTo({ url: '/pages/teacher/ai-matches?jobId=' + jobId })

const loadJobs = async () => {
	try {
		// 调用教师专用接口（返回所有状态的岗位，不分页）
		const res = await teacherAPI.getTeacherJobs()
		// 返回结构: Result<List<Job>> → res.data 为数组
		const rawList = Array.isArray(res.data) ? res.data : []
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
		await loadJobs()
	} catch (e) { uni.showToast({ title: '发布失败', icon: 'none' }) }
}

const handleClose = async (job) => {
	try {
		await teacherAPI.closeJob(job.id)
		uni.showToast({ title: '已关闭', icon: 'success' })
		await loadJobs()
	} catch (e) { uni.showToast({ title: '操作失败', icon: 'none' }) }
}

const goToEdit = (job) => { uni.navigateTo({ url: '/pages/teacher/job-edit?id=' + job.id }) }
const goToCreate = () => { uni.navigateTo({ url: '/pages/teacher/job-edit' }) }

// AI 智能写岗位
const handleAiParseJob = async () => {
	if (aiParsing.value || !aiJobText.value.trim()) return
	aiParsing.value = true
	try {
		const res = await aiParseAPI.parseJob(aiJobText.value)
		const data = res.data || {}
		// 把解析结果存到缓存，跳转到编辑页后读取
		uni.setStorageSync('ai_parsed_job', data)
		showAiJobModal.value = false
		aiJobText.value = ''
		uni.navigateTo({ url: '/pages/teacher/job-edit?aiParsed=1' })
	} catch (e) {
		console.error('AI解析失败', e)
		uni.showToast({ title: 'AI 解析失败', icon: 'none' })
	} finally {
		aiParsing.value = false
	}
}
const goToDeliveries = (job) => {
	uni.navigateTo({ url: '/pages/teacher/deliveries?jobId=' + job.id + '&jobTitle=' + encodeURIComponent(job.title) })
}
</script>

<style scoped>
/* 筛选 Tab 统一样式 */
.list-tabs {
	flex-direction: row;
	padding: 0 16px;
	gap: 20px;
	margin-bottom: 4px;
	background: #FFFFFF;
	height: 44px;
	align-items: center;
	overflow-x: auto;
}
.list-tab {
	font-size: 15px;
	color: #86909C;
	font-weight: 500;
	padding-bottom: 4px;
	position: relative;
	white-space: nowrap;
}
.list-tab.active {
	color: #1D2129;
	font-weight: 600;
}
.list-tab.active::after {
	content: '';
	position: absolute;
	bottom: 0;
	left: 0;
	width: 20px;
	height: 3px;
	background: #165DFF;
	border-radius: 2px;
}
.tab-badge {
	font-size: 11px;
	color: #86909C;
	margin-left: 2px;
	font-weight: 400;
}

/* AI 智能写岗位横幅 */
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
.ai-job-banner:active { opacity: 0.8; }

/* AI 写岗位弹窗 */
.ai-modal-body { padding: 0; width: 100%; overflow-x: hidden; }
.ai-modal-desc { font-size: 13px; color: #86909C; margin-bottom: 12px; display: block; max-width: 100%; word-wrap: break-word; }
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
.ai-modal-count { font-size: 11px; color: #C9CDD4; text-align: right; margin-top: 4px; display: block; max-width: 100%; }
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
.ai-modal-btn[disabled] { background: #E5E6EB !important; color: #A9AEB8 !important; }

.job-list { padding: 16px; }
.job-card {
	background: white;
	border-radius: 12px;
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
	background: linear-gradient(90deg, #165DFF, transparent);
}
.job-card-top {
	flex-direction: row;
	justify-content: space-between;
	align-items: flex-start;
	margin-bottom: 8px;
}
.job-info { flex: 1; }
.job-title { font-size: 16px; font-weight: 700; color: #1D2129; display: block; margin-bottom: 4px; }
.job-company { font-size: 13px; color: #86909C; display: block; }
.job-salary {
	font-size: 15px;
	font-weight: 700;
	color: #F53F3F;
	background: rgba(245,63,63,0.06);
	padding: 2px 8px;
	border-radius: 4px;
	flex-shrink: 0;
}
.job-meta {
	flex-direction: row;
	gap: 12px;
	font-size: 13px;
	color: #86909C;
	margin-bottom: 12px;
}
.job-actions {
	flex-direction: row;
	gap: 10px;
	padding-top: 14px;
	border-top: 1px solid #F2F3F5;
	flex-wrap: wrap;
}
/* 操作按钮 — 胶囊风格，更饱满 */
.action-tag {
	flex: 1;
	text-align: center;
	padding: 10px 0;
	border-radius: 10px;
	font-size: 14px;
	font-weight: 600;
	background: #F2F3F5;
	color: #4E5969;
	min-width: 60px;
}
.action-tag:active { opacity: 0.75; }
.action-tag.primary { background: #165DFF; color: #FFFFFF; }
.action-tag.warning { background: #F59E0B; color: #FFFFFF; }
.action-tag.ai { background: #8B5CF6; color: #FFFFFF; }

.fab {
	position: fixed;
	bottom: calc(50px + env(safe-area-inset-bottom) + 16px);
	right: 20px;
	width: 56px;
	height: 56px;
	border-radius: 50%;
	background: #165DFF;
	align-items: center;
	justify-content: center;
	box-shadow: 0 4px 16px rgba(22,93,255,0.4);
	z-index: 100;
}
.fab-icon { font-size: 28px; color: white; font-weight: 300; }
</style>
