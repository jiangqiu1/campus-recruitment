<template>
	<view class="page-wrapper">
		<NavBar title="我的收藏" />
		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh" @scrolltolower="loadMore">
			<view v-if="editing" class="batch-bar">
				<button class="batch-btn" @click="batchRemove">批量取消收藏</button>
			</view>
			<view class="sort-bar">
				<text v-for="s in sorts" :key="s.value" class="sort-tab" :class="{ active: currentSort === s.value }" @click="currentSort = s.value">{{ s.label }}</text>
				<text class="edit-text" @click="editing = !editing">{{ editing ? '完成' : '编辑' }}</text>
			</view>
			<view class="card-list">
				<view v-for="(fav, i) in sortedList" :key="i" class="card-item" @click="!editing && goToDetail(fav.jobId)">
					<view v-if="editing" class="check-box" @click.stop="toggleSelect(fav.jobId)">
						<uni-icons :type="selectedIds.has(fav.jobId) ? 'checkbox-filled' : 'circle'" :size="20" :color="selectedIds.has(fav.jobId) ? '#165DFF' : '#C9CDD4'" />
					</view>
					<view class="card-body">
						<view class="card-header">
							<text class="card-title">{{ fav.title || '岗位 #' + fav.jobId }}</text>
							<text class="card-salary">{{ fav.salaryText || '' }}</text>
						</view>
						<text class="card-sub">{{ fav.companyName || '' }}</text>
						<view class="card-meta">
							<text class="meta-text">{{ fav.location || '' }}</text>
							<text class="meta-text">{{ fav.education || '' }}</text>
						</view>
						<view v-if="!editing" class="card-actions">
							<button class="action-btn btn-primary" :class="{ disabled: deliveredJobIds.has(fav.jobId) }" :disabled="deliveredJobIds.has(fav.jobId)" @click.stop="handleDeliver(fav)">{{ deliveredJobIds.has(fav.jobId) ? '已投递' : '投递' }}</button>
						</view>
					</view>
				</view>
				<EmptyState v-if="!favorites.length" icon="star" title="暂无收藏" desc="浏览岗位时点击收藏按钮即可添加" />
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { favoriteAPI, deliveryAPI, jobAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const refreshing = ref(false)
const favorites = ref([])
const deliveredJobIds = ref(new Set())
const editing = ref(false)
const selectedIds = ref(new Set())
const currentSort = ref('time')

const sorts = [
	{ label: '按时间', value: 'time' },
	{ label: '按薪资', value: 'salary' },
	{ label: '按匹配度', value: 'match' }
]

const sortedList = computed(() => {
	const list = [...favorites.value]
	if (currentSort.value === 'salary') {
		list.sort((a, b) => parseSalary(b.salaryText) - parseSalary(a.salaryText))
	}
	return list
})

const parseSalary = (s) => {
	if (!s) return 0
	const nums = s.match(/\d+/g)
	return nums ? parseInt(nums[0]) : 0
}

const getStudentId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.id || obj.userId ? Number(obj.id || obj.userId) : null
	} catch (e) { return null }
}

loadData()
async function loadData() {
	const sid = getStudentId()
	if (!sid) { favorites.value = []; return }
	try {
		const fRes = await favoriteAPI.getFavorites({ studentId: sid })
		const rawList = fRes.data || []
		if (!Array.isArray(rawList) || !rawList.length) { favorites.value = []; return }
		const enriched = []
		for (const fav of rawList) {
			const jobId = fav.jobId || fav.id
			if (!jobId) continue
			try {
				const jRes = await jobAPI.getJobDetail(jobId)
				const job = jRes.data || {}
				enriched.push({ ...fav, jobId, title: job.title || job.jobTitle || '岗位 #' + jobId, companyName: job.companyName || '', location: job.location || '', education: job.education || '', salaryText: job.salaryText || job.salaryRange || '' })
			} catch (e) {
				enriched.push({ ...fav, jobId, title: '岗位 #' + jobId, companyName: '', location: '', education: '', salaryText: '' })
			}
		}
		favorites.value = enriched
		try {
			const dRes = await deliveryAPI.getDeliveriesByStudentId({ studentId: sid })
			deliveredJobIds.value = new Set((dRes.data || []).map(d => d.jobId))
		} catch (e) { deliveredJobIds.value = new Set() }
	} catch (e) {
		console.error('加载收藏失败', e)
		favorites.value = []
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
	if (next.has(jobId)) next.delete(jobId); else next.add(jobId)
	selectedIds.value = next
}

const batchRemove = async () => {
	const ids = [...selectedIds.value]
	if (!ids.length) { uni.showToast({ title: '请选择要取消收藏的岗位', icon: 'none' }); return }
	uni.showModal({
		title: '批量取消收藏',
		content: '确定取消选中的 ' + ids.length + ' 个收藏吗？',
		success: async (r) => {
			if (!r.confirm) return
			const sid = getStudentId()
			try {
				await Promise.all(ids.map(jid => favoriteAPI.removeFavorite(jid, sid)))
				favorites.value = favorites.value.filter(f => !ids.includes(f.jobId || f.id))
				selectedIds.value = new Set()
				editing.value = false
				uni.showToast({ title: '已取消收藏', icon: 'success' })
			} catch (e) { uni.showToast({ title: '操作失败，请重试', icon: 'none' }) }
		}
	})
}

const goToDetail = (id) => {
	if (!id) return
	uni.navigateTo({ url: '/pages/student/job-detail?id=' + id })
}

const handleDeliver = async (job) => {
	const jid = job.jobId || job.id
	if (!jid) return
	if (deliveredJobIds.value.has(jid)) return
	try {
		await deliveryAPI.createDelivery({ jobId: jid, studentId: getStudentId() })
		deliveredJobIds.value.add(jid)
		uni.showToast({ title: '投递成功', icon: 'success' })
	} catch (e) {
		if (e?.message?.includes('重复投递')) { deliveredJobIds.value.add(jid); uni.showToast({ title: '已投递过', icon: 'none' }) }
		else { uni.showToast({ title: '投递失败', icon: 'none' }) }
	}
}
</script>

<style scoped lang="scss">
.batch-bar {
	padding: 12px 16px;
	background: $uni-bg-color;
	border-bottom: 0.5px solid $uni-border-color-divider;
}
.batch-btn {
	padding: 8px 16px;
	background: $uni-color-error-light;
	color: $uni-color-error;
	border: none;
	border-radius: 8px;
	font-size: 13px;
	font-weight: 500;
}
.sort-bar {
	flex-direction: row;
	align-items: center;
	padding: 0 16px;
	background: $uni-bg-color;
	gap: 20px;
	border-bottom: 0.5px solid $uni-border-color-divider;
	height: 44px;
}
.sort-tab {
	font-size: 14px;
	color: $uni-text-color-secondary;
	font-weight: 500;
	position: relative;
	padding-bottom: 4px;
}
.sort-tab.active {
	color: $uni-text-color-title;
	font-weight: 600;
}
.sort-tab.active::after {
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
.edit-text {
	margin-left: auto;
	font-size: 14px;
	color: $uni-color-primary;
	font-weight: 500;
}
.card-list {
	padding: 12px 16px;
	gap: 12px;
}
.card-item {
	flex-direction: row;
	background: $uni-bg-color;
	border-radius: 12px;
	padding: 14px;
	gap: 12px;
	box-shadow: $uni-shadow-card;
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
	color: $uni-text-color-title;
	flex: 1;
}
.card-salary {
	font-size: 15px;
	font-weight: 600;
	color: $uni-color-primary;
}
.card-sub {
	font-size: 13px;
	color: $uni-text-color-secondary;
}
.card-meta {
	flex-direction: row;
	gap: 8px;
}
.meta-text {
	font-size: 12px;
	color: $uni-text-color-placeholder;
}
.card-actions {
	flex-direction: row;
	gap: 8px;
	margin-top: 4px;
}
.action-btn {
	flex: 1;
	height: 32px;
	border-radius: 8px;
	font-size: 13px;
	font-weight: 500;
	align-items: center;
	justify-content: center;
	border: none;
}
.btn-primary { background: $uni-color-primary; color: $uni-text-color-inverse; }
.btn-primary.disabled { background: $uni-border-color; color: $uni-text-color-placeholder; }
.card-item:active { background: $uni-bg-color-page; }
.sort-tab:active { opacity: 0.7; }
</style>
