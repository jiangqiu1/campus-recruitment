<template>
	<view class="page-wrapper">
		<NavBar title="热门岗位" showBack @back="goBack" />
		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh" @scrolltolower="loadMore">
			<view class="sub-title">
				<uni-icons type="fire" size="16" color="#F53F3F" />
				<text>精选热门岗位，等你来投</text>
			</view>
			<view class="job-list">
				<JobCard
					v-for="job in jobs"
					:key="job.id"
					:job="job"
					:show-match="false"
					:delivered="deliveredJobIds.has(job.id)"
					:show-deliver="true"
					:show-actions="true"
					@click="goToJobDetail(job.id)"
					@deliver="handleDeliver"
				/>
				<view v-if="loadingMore" class="loading-more">
					<uni-icons type="spinner-cycle" size="16" color="#86909C" />
					<text>加载中...</text>
				</view>
				<view v-if="!loadingMore && !hasMore && jobs.length" class="no-more">
					<text>— 没有更多了 —</text>
				</view>
				<EmptyState v-if="!jobs.length && !loadingMore" icon="inbox" title="暂无热门岗位" desc="热门岗位更新中，请稍后再来看看" />
			</view>
			<view style="height: calc(30px + env(safe-area-inset-bottom))" />
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { jobAPI, deliveryAPI } from '@/utils/request'
import { mapJobData } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import JobCard from '@/components/JobCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const getStudentId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.id || obj.userId ? Number(obj.id || obj.userId) : null
	} catch (e) { return null }
}

const jobs = ref([])
const refreshing = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const page = ref(1)
const PAGE_SIZE = 10
const deliveredJobIds = ref(new Set())

onMounted(async () => {
	await loadJobs()
	await loadUserState()
})

const loadJobs = async () => {
	try {
		const res = await jobAPI.getRecommendJobs({ sort: 'latest', page: 1, pageSize: PAGE_SIZE })
		const raw = res.data || []
		jobs.value = raw.map(mapJobData)
		hasMore.value = raw.length >= PAGE_SIZE
		page.value = 1
	} catch (e) {
		console.log('加载热门岗位失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

const loadMore = async () => {
	if (loadingMore.value || !hasMore.value) return
	loadingMore.value = true
	try {
		const nextPage = page.value + 1
		const res = await jobAPI.getRecommendJobs({ sort: 'latest', page: nextPage, pageSize: PAGE_SIZE })
		const raw = res.data || []
		if (raw.length) {
			jobs.value = [...jobs.value, ...raw.map(mapJobData)]
			page.value = nextPage
			hasMore.value = raw.length >= PAGE_SIZE
		} else {
			hasMore.value = false
		}
	} catch (e) {
		console.log('加载更多失败', e)
	} finally {
		loadingMore.value = false
	}
}

const onRefresh = async () => {
	refreshing.value = true
	page.value = 1
	hasMore.value = true
	await loadJobs()
	await loadUserState()
	refreshing.value = false
}

const loadUserState = async () => {
	const sid = getStudentId()
	if (!sid) return
	try {
		const dRes = await deliveryAPI.getDeliveriesByStudentId({ studentId: sid })
		deliveredJobIds.value = new Set((dRes.data || []).map(d => d.jobId))
	} catch (e) {
		console.log('加载用户状态失败', e)
	}
}

const handleDeliver = async (job) => {
	if (deliveredJobIds.value.has(job.id)) return
	const sid = getStudentId()
	if (!sid) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
	try {
		await deliveryAPI.createDelivery({ jobId: job.id, studentId: sid })
		deliveredJobIds.value.add(job.id)
		uni.showToast({ title: '投递成功', icon: 'success' })
	} catch (e) {
		if (e && e.message && e.message.includes('重复投递')) {
			uni.showToast({ title: '已投递过', icon: 'none' })
			deliveredJobIds.value.add(job.id)
		} else {
			uni.showToast({ title: '投递失败', icon: 'none' })
		}
	}
}

const goToJobDetail = (id) => uni.navigateTo({ url: '/pages/student/job-detail?id=' + id })
const goBack = () => uni.navigateBack()
</script>

<style scoped lang="scss">
.sub-title {
	flex-direction: row;
	align-items: center;
	gap: 6px;
	padding: 16px 16px 8px;
	font-size: 13px;
	color: $uni-text-color-secondary;
}
.job-list {
	padding: 8px 16px;
}
.loading-more {
	flex-direction: row;
	align-items: center;
	justify-content: center;
	gap: 8px;
	padding: 16px;
	font-size: 13px;
	color: $uni-text-color-secondary;
}
.no-more {
	align-items: center;
	padding: 16px;
	font-size: 13px;
	color: $uni-text-color-placeholder;
}
</style>
