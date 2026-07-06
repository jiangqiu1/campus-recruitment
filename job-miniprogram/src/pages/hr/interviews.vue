<template>
	<view class="page-wrapper">
		<NavBar title="面试日程" :showBack="false" />
		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<!-- 日期快速切换 -->
			<scroll-view class="date-scroll" scroll-x show-scrollbar="false">
				<view class="date-scroll-inner">
					<text v-for="(d, i) in dateTabs" :key="i" class="date-tab" :class="{ active: selectedDate === d.value }" @click="selectedDate = d.value">
						<text class="date-tab-week">{{ d.week }}</text>
						<text class="date-tab-day">{{ d.day }}</text>
					</text>
				</view>
			</scroll-view>

			<view class="interview-list">
				<template v-if="filteredInterviews.length">
					<view v-for="item in filteredInterviews" :key="item.id" class="interview-card" @click="goToDetail(item.id)">
						<view class="interview-left">
							<text class="interview-date-text">{{ formatDate(item.interviewTime) }}</text>
							<text class="interview-time-text">{{ formatTime(item.interviewTime) }}</text>
						</view>
						<view class="interview-body">
							<view class="interview-top">
								<text class="interview-name">{{ item.studentName || '候选人' }}</text>
								<text class="status-tag" :class="'tag-' + item.status">{{ item.statusText }}</text>
							</view>
							<text class="interview-job">{{ item.jobTitle || '岗位名称' }}</text>
							<text class="interview-location">{{ item.interviewLocation || '线上面试' }}</text>
						</view>
						<uni-icons type="arrowright" size="16" color="#C9CDD4" />
					</view>
				</template>
				<EmptyState v-else icon="calendar" title="暂无面试安排" desc="安排面试后会显示在这里" />
			</view>
			<view style="height: calc(60px + env(safe-area-inset-bottom))" />
		</scroll-view>
		<HrTabBar current="interviews" />
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { hrAPI } from '@/utils/request'
import HrTabBar from '@/components/HrTabBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import NavBar from '@/components/NavBar.vue'

const allInterviews = ref([])
const selectedDate = ref('all')

const DELIVERY_STATUS = ['pending', 'viewed', 'interview', 'accepted', 'rejected']
const DELIVERY_STATUS_TEXT = ['待查看', '已查看', '面试中', '已录用', '未通过']
const WEEK_NAMES = ['日', '一', '二', '三', '四', '五', '六']

const dateTabs = computed(() => {
	const tabs = [{ label: '全部', week: '全部', day: '', value: 'all' }]
	const today = new Date()
	for (let i = 0; i < 7; i++) {
		const d = new Date(today)
		d.setDate(today.getDate() + i)
		const mm = String(d.getMonth() + 1).padStart(2, '0')
		const dd = String(d.getDate()).padStart(2, '0')
		const week = WEEK_NAMES[d.getDay()]
		tabs.push({
			label: mm + '-' + dd,
			week: i === 0 ? '今天' : week,
			day: mm + '/' + dd,
			value: d.getFullYear() + '-' + mm + '-' + dd
		})
	}
	return tabs
})

const filteredInterviews = computed(() => {
	if (selectedDate.value === 'all') return allInterviews.value
	return allInterviews.value.filter(item =>
		item.interviewTime && item.interviewTime.startsWith(selectedDate.value)
	)
})

onMounted(() => { loadData() })

const refreshing = ref(false)
const onRefresh = async () => {
	refreshing.value = true
	await loadData()
	refreshing.value = false
}

const loadData = async () => {
	const cId = getCompanyId()
	if (!cId) return
	try {
		const jobsRes = await hrAPI.getHrJobs(cId)
		const jobs = jobsRes.data || []
		const all = []
		for (const job of jobs) {
			try {
				const dRes = await hrAPI.getCompanyDeliveries(job.id)
				;(dRes.data || []).forEach(d => {
					if (d.status === 2) {
						all.push({
							id: d.id,
							studentId: d.studentId,
							studentName: d.studentName || '候选人',
							jobTitle: job.title,
							status: DELIVERY_STATUS[d.status] || 'pending',
							statusText: DELIVERY_STATUS_TEXT[d.status] || '待查看',
							interviewTime: d.interviewTime || '',
							interviewLocation: d.interviewLocation || '线上面试'
						})
					}
				})
			} catch (e) { console.error('加载面试数据失败', e) }
		}
		all.sort((a, b) => {
			if (a.interviewTime < b.interviewTime) return -1
			if (a.interviewTime > b.interviewTime) return 1
			return 0
		})
		allInterviews.value = all
	} catch (e) {
		console.error('加载面试数据失败', e)
	}
}

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

const formatTime = (t) => {
	if (!t) return '--:--'
	return t.length >= 16 ? t.substring(11, 16) : t
}

const formatDate = (t) => {
	if (!t) return '--'
	// 格式: MM/DD，如 "07/06"
	if (t.length >= 10) {
		return t.substring(5, 10)
	}
	return t
}

const goToDetail = (id) => uni.navigateTo({ url: '/pages/hr/delivery-detail?id=' + id })
</script>

<style scoped>
/* 日期滚动条 */
.date-scroll { background: #FFFFFF; border-bottom: 0.5px solid #F2F3F5; }
.date-scroll-inner { flex-direction: row; padding: 10px 12px; gap: 8px; }
.date-tab {
	flex-direction: column;
	align-items: center;
	padding: 8px 14px;
	border-radius: 10px;
	background: #F7F8FA;
	min-width: 52px;
}
.date-tab.active { background: #165DFF; }
.date-tab.active .date-tab-week { color: #FFFFFF; }
.date-tab.active .date-tab-day { color: rgba(255,255,255,0.8); }
.date-tab-week { font-size: 12px; color: #4E5969; font-weight: 500; }
.date-tab-day { font-size: 11px; color: #86909C; margin-top: 2px; }

/* 面试卡片 */
.interview-list { padding: 12px 16px; }
.interview-card {
	flex-direction: row;
	align-items: center;
	gap: 12px;
	background: #FFFFFF;
	border-radius: 12px;
	padding: 14px;
	margin-bottom: 10px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.interview-card:active { background: #F7F8FA; }
.interview-left {
	width: 60px;
	align-items: center;
	flex-direction: column;
}
.interview-date-text {
	font-size: 12px;
	color: #165DFF;
	margin-bottom: 2px;
}
.interview-time-text {
	font-size: 14px;
	font-weight: 700;
	color: #165DFF;
}
.interview-body { flex: 1; gap: 4px; }
.interview-top {
	flex-direction: row;
	align-items: center;
	gap: 8px;
}
.interview-name { font-size: 15px; font-weight: 700; color: #1D2129; }
.interview-job { font-size: 12px; color: #86909C; display: block; margin-top: 2px; }
.interview-location { font-size: 12px; color: #C9CDD4; display: block; margin-top: 2px; }
.status-tag { font-size: 11px; padding: 2px 8px; border-radius: 6px; font-weight: 600; flex-shrink: 0; }
.tag-pending { background: rgba(245,158,11,0.1); color: #F59E0B; }
.tag-viewed { background: rgba(22,93,255,0.1); color: #165DFF; }
.tag-interview { background: rgba(22,93,255,0.1); color: #165DFF; }
.tag-accepted { background: rgba(0,180,42,0.1); color: #00B42A; }
.tag-rejected { background: rgba(239,68,68,0.1); color: #EF4444; }
</style>
