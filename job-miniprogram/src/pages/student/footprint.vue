<template>
	<view class="page-wrapper">
		<NavBar title="浏览记录" />
		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh" @scrolltolower="loadMore">
			<view v-if="editing" class="batch-bar">
				<button class="batch-btn" @click="batchRemove">批量删除</button>
			</view>
			<view class="sort-bar">
				<text class="sort-tab active">按时间</text>
				<text class="edit-text" @click="editing = !editing">{{ editing ? '完成' : '编辑' }}</text>
			</view>
			<view class="card-list">
				<view v-for="(item, i) in list" :key="i" class="card-item" @click="!editing && goToDetail(item.jobId)">
					<view v-if="editing" class="check-box" @click.stop="toggleSelect(item.jobId)">
						<uni-icons :type="selectedIds.has(item.jobId) ? 'checkbox-filled' : 'circle'" :size="20" :color="selectedIds.has(item.jobId) ? '#165DFF' : '#C9CDD4'" />
					</view>
					<view class="card-body">
						<view class="card-header">
							<text class="card-title">{{ item.title || '岗位 #' + item.jobId }}</text>
							<text class="card-salary">{{ item.salaryText || '' }}</text>
						</view>
						<text class="card-sub">{{ item.companyName || '' }}</text>
						<view class="card-meta">
							<text class="meta-text">{{ item.location || '' }}</text>
							<text class="meta-text">{{ item.education || '' }}</text>
							<text class="meta-date">{{ item.viewTime }}</text>
						</view>
					</view>
				</view>
				<EmptyState v-if="!list.length && !loading" icon="eye" title="暂无浏览记录" desc="浏览过的岗位会出现在这里" />
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { jobAPI } from '@/utils/request'
import { getBrowseHistory, clearBrowseHistory } from '@/utils/browseHistory'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const list = ref([])
const loading = ref(false)
const refreshing = ref(false)
const editing = ref(false)
const selectedIds = ref(new Set())

onMounted(() => {
	loadData()
})

async function loadData() {
	loading.value = true
	try {
		const history = await getBrowseHistory()
		if (!history.length) { list.value = []; return }

		const enriched = []
		for (const item of history) {
			try {
				const res = await jobAPI.getJobDetail(item.jobId)
				const job = res.data || {}
				enriched.push({
					jobId: item.jobId,
					viewTime: item.viewTime,
					title: job.title || job.jobTitle || '岗位 #' + item.jobId,
					companyName: job.companyName || '',
					location: job.location || '',
					education: job.education || '',
					salaryText: job.salaryText || job.salaryRange || ''
				})
			} catch (e) {
				enriched.push({ jobId: item.jobId, viewTime: item.viewTime, title: '岗位 #' + item.jobId })
			}
		}
		list.value = enriched
	} catch (e) {
		list.value = []
	} finally {
		loading.value = false
	}
}

const onRefresh = async () => {
	refreshing.value = true
	await loadData()
	refreshing.value = false
}

const loadMore = () => {}

const toggleSelect = (jobId) => {
	if (!jobId) return
	const next = new Set(selectedIds.value)
	if (next.has(jobId)) next.delete(jobId)
	else next.add(jobId)
	selectedIds.value = next
}

const batchRemove = async () => {
	const ids = [...selectedIds.value]
	if (!ids.length) {
		uni.showToast({ title: '请选择要删除的记录', icon: 'none' })
		return
	}
	uni.showModal({
		title: '批量删除',
		content: '确定删除选中的 ' + ids.length + ' 条记录吗？',
		success: (r) => {
			if (!r.confirm) return
			clearBrowseHistory(ids)
			list.value = list.value.filter(item => !ids.includes(item.jobId))
			selectedIds.value = new Set()
			editing.value = false
			uni.showToast({ title: '删除成功', icon: 'success' })
		}
	})
}

const goToDetail = (id) => {
	if (!id) return
	uni.navigateTo({ url: '/pages/student/job-detail?id=' + id })
}
</script>

<style scoped>
.batch-bar {
	padding: 12px 16px;
	background: #FFFFFF;
	border-bottom: 0.5px solid #F2F3F5;
}
.batch-btn {
	padding: 8px 16px;
	background: rgba(245, 63, 63, 0.08);
	color: #F53F3F;
	border: none;
	border-radius: 6px;
	font-size: 13px;
	font-weight: 500;
}
.sort-bar {
	flex-direction: row;
	align-items: center;
	padding: 0 16px;
	background: #FFFFFF;
	gap: 20px;
	border-bottom: 0.5px solid #F2F3F5;
	height: 44px;
}
.sort-tab {
	font-size: 14px;
	color: #1D2129;
	font-weight: 600;
	position: relative;
	padding-bottom: 4px;
}
.sort-tab::after {
	content: '';
	position: absolute;
	bottom: 0;
	left: 50%;
	transform: translateX(-50%);
	width: 20px;
	height: 3px;
	background: #165DFF;
	border-radius: 2px;
}
.edit-text {
	margin-left: auto;
	font-size: 14px;
	color: #165DFF;
	font-weight: 500;
}
.card-list {
	padding: 12px 16px;
	gap: 12px;
}
.card-item {
	flex-direction: row;
	background: #FFFFFF;
	border-radius: 12px;
	padding: 14px;
	gap: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.check-box {
	justify-content: center;
	align-items: center;
}
.card-body {
	flex: 1;
	gap: 6px;
}
.card-header {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
}
.card-title {
	font-size: 15px;
	font-weight: 600;
	color: #1D2129;
	flex: 1;
}
.card-salary {
	font-size: 15px;
	font-weight: 600;
	color: #165DFF;
}
.card-sub {
	font-size: 13px;
	color: #86909C;
}
.card-meta {
	flex-direction: row;
	gap: 8px;
	align-items: center;
}
.meta-text {
	font-size: 12px;
	color: #C9CDD4;
}
.meta-date {
	font-size: 11px;
	color: #C9CDD4;
	margin-left: auto;
}
.card-item:active { background: #F7F8FA; }
</style>
