<template>
	<view class="page-wrapper">
		<NavBar title="搜索结果" show-back />
		<!-- 顶部搜索栏 -->
		<view class="search-bar">
				<view class="search-input" :class="{ 'search-input--focus': searchFocused }">
					<uni-icons type="search" size="16" color="#86909C" />
					<input
						v-model="keyword"
						placeholder="搜索岗位、公司名称"
						:focus="true"
						@confirm="handleSearch"
						@input="handleInput"
						@focus="searchFocused = true"
						@blur="searchFocused = false"
					/>
				<text v-if="keyword" class="clear-btn" @click="clearKeyword">
					<uni-icons type="clear" size="16" color="#C9CDD4" />
				</text>
			</view>
		</view>

		<!-- 筛选Tab -->
		<view class="filter-tabs">
			<text 
				v-for="tab in tabs" 
				:key="tab.value" 
				class="filter-tab" 
				:class="{ active: currentTab === tab.value }" 
				@click="switchTab(tab.value)"
			>{{ tab.label }}</text>
		</view>

		<!-- 结果列表 -->
		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<view v-if="loading" class="loading-wrap">
				<LoadingState type="skeleton" :rows="4" />
			</view>
			<view v-else class="job-list">
				<JobCard 
					v-for="job in list" 
					:key="job.id" 
					:job="job" 
					:show-deliver="true"
					:delivered="deliveredIds.has(job.id)"
					@click="goToDetail(job.id)"
					@deliver="handleDeliver"
				/>
				<EmptyState 
					v-if="!list.length && keyword" 
					icon="search" 
					title="未找到相关岗位" 
					desc="换个关键词或调整筛选条件试试"
				/>
				<EmptyState 
					v-if="!list.length && !keyword" 
					icon="search" 
					title="输入关键词开始搜索" 
					desc="支持搜索岗位名称、公司名称"
				/>
			</view>
			<view style="height: 24px;"></view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { jobAPI, deliveryAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import JobCard from '@/components/JobCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'

const refreshing = ref(false)
const keyword = ref('')
const list = ref([])
const loading = ref(false)
const currentTab = ref('default')
const searchFocused = ref(false)
const deliveredIds = ref(new Set())

const tabs = [
	{ label: '综合', value: 'default' },
	{ label: '最新发布', value: 'latest' },
	{ label: '薪资最高', value: 'salary' }
]

onMounted(() => {
	const pages = getCurrentPages()
	const query = pages[pages.length - 1].options
	if (query?.q) {
		keyword.value = decodeURIComponent(query.q)
		handleSearch()
	}
	loadDeliveredStatus()
})

const loadDeliveredStatus = async () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return
		const obj = JSON.parse(raw)
		const sid = obj.id || obj.userId
		if (!sid) return
		const res = await deliveryAPI.getDeliveriesByStudentId({ studentId: Number(sid) })
		deliveredIds.value = new Set((res.data || []).map(d => d.jobId))
	} catch (e) {}
}

const handleInput = () => {}

const clearKeyword = () => {
	keyword.value = ''
	list.value = []
}

const switchTab = (val) => {
	if (currentTab.value === val) return
	currentTab.value = val
	if (keyword.value.trim()) handleSearch()
}

const handleSearch = async () => {
	const kw = keyword.value.trim()
	if (!kw) return
	loading.value = true
	try {
		const res = await jobAPI.searchJobs({
			keyword: kw,
			sort: currentTab.value
		})
		list.value = res.data || []
	} catch (e) {
		list.value = []
	} finally {
		loading.value = false
	}
}

const onRefresh = async () => {
	refreshing.value = true
	if (keyword.value.trim()) await handleSearch()
	refreshing.value = false
}

const goToDetail = (id) => {
	uni.navigateTo({ url: '/pages/student/job-detail?id=' + id })
}

const handleDeliver = async (job) => {
	if (deliveredIds.value.has(job.id)) return
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return
		const obj = JSON.parse(raw)
		const sid = obj.id || obj.userId
		await deliveryAPI.createDelivery({ jobId: job.id, studentId: sid })
		deliveredIds.value.add(job.id)
		uni.showToast({ title: '投递成功', icon: 'success' })
	} catch (e) {
		uni.showToast({ title: '投递失败', icon: 'none' })
	}
}
</script>

<style scoped lang="scss">
.search-bar {
	padding: 8px 16px;
	background: $uni-bg-color;
	border-bottom: 0.5px solid $uni-border-color-divider;
}
.search-input {
	flex-direction: row;
	align-items: center;
	gap: 8px;
	background: $uni-bg-color-page;
	border: 1px solid transparent;
	border-radius: 999px;
	padding: 0 16px;
	height: 36px;
	transition: border-color 0.15s ease;
}
.search-input--focus {
	border-color: $uni-color-primary;
}
.search-input input {
	flex: 1;
	font-size: 14px;
	background: transparent;
	border: none;
	color: $uni-text-color-title;
}
.clear-btn {
	line-height: 1;
}
.filter-tabs {
	flex-direction: row;
	padding: 0 16px;
	background: $uni-bg-color;
	gap: 24px;
	border-bottom: 0.5px solid $uni-border-color-divider;
	height: 44px;
	align-items: center;
}
.filter-tab {
	font-size: 14px;
	color: $uni-text-color-secondary;
	font-weight: 500;
	position: relative;
	padding-bottom: 4px;
}
.filter-tab.active {
	color: $uni-text-color-title;
	font-weight: 600;
}
.filter-tab.active::after {
	content: '';
	position: absolute;
	bottom: 0;
	left: 50%;
	transform: translateX(-50%);
	width: 20px;
	height: 3px;
	background: $uni-color-primary;
	border-radius: 4px;
}
.content-scrollable {
	flex: 1;
	height: 0;
}
.loading-wrap {
	padding: 0 16px;
}
.job-list {
	padding: 12px 16px;
}
</style>
