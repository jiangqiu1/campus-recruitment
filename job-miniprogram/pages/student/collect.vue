<template>
	<view class="page-wrapper">
		<view class="header-simple" style="padding:12px 16px;flex-direction:row;align-items:center;gap:12px;">
			<text style="font-size:20px;" @click="goBack">‹</text>
			<text style="font-size:18px;font-weight:700;color:white;">我的收藏</text>
		</view>
		<scroll-view class="content-scrollable" scroll-y>
			<view class="card-list">
				<view v-for="(fav, i) in favorites" :key="i" class="card-item" @click="goToDetail(fav.jobId)">
					<view class="card-header-row">
						<view>
							<text class="card-title">{{ fav.title || '岗位 #' + fav.jobId }}</text>
							<text class="card-sub">{{ fav.companyName || '' }}</text>
						</view>
						<text class="card-salary">{{ fav.salaryText || '' }}</text>
					</view>
					<view class="card-info">
						<text>{{ fav.location || '' }}</text>
						<text>{{ fav.experience || '' }}</text>
					</view>
					<view class="card-actions">
						<button class="btn-sm" :class="deliveredJobIds.has(fav.jobId) ? 'btn-disabled' : 'btn-primary'" :disabled="deliveredJobIds.has(fav.jobId)" @click.stop="handleDeliver(fav)">{{ deliveredJobIds.has(fav.jobId) ? '已投递' : '投递' }}</button>
						<button class="btn-sm btn-outline" @click.stop="removeFavorite(fav.jobId)">取消收藏</button>
					</view>
				</view>
				<view v-if="!favorites.length" class="empty-state">
					<text style="font-size:48px;margin-bottom:12px;">⭐</text>
					<text class="empty-text">暂无收藏岗位</text>
					<text style="font-size:13px;color:#C9CDD4;margin-top:8px;">浏览岗位时点击收藏按钮即可添加</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { favoriteAPI, deliveryAPI, jobAPI } from '@/utils/request'

const favorites = ref([])
const deliveredJobIds = ref(new Set())
const mockFavorites = [
	{ id: 1, title: '前端开发实习生', companyName: '广州科技有限公司', location: '广州', experience: '经验不限', salaryText: '4K-6K' },
	{ id: 2, title: 'UI设计实习生', companyName: '广州创意设计工作室', location: '广州', experience: '经验不限', salaryText: '3K-5K' }
]

const getStudentId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		const sid = obj.id || obj.userId
		return sid ? Number(sid) : null
	} catch (e) { return null }
}

onMounted(async () => {
	try {
		const sid = getStudentId()
		const [fRes, dRes] = await Promise.all([
			favoriteAPI.getFavorites({ studentId: sid }),
			deliveryAPI.getDeliveriesByStudentId({ studentId: sid })
		])
		const rawFavs = fRes.data || []
		const jobIds = rawFavs.map(f => f.jobId).filter(Boolean)
		// 获取岗位详情用于显示
		let jobMap = {}
		if (jobIds.length > 0) {
			try {
				const jobRes = await jobAPI.getActiveJobs()
				const allJobs = jobRes.data || []
				jobIds.forEach(jid => {
					const found = allJobs.find(j => j.id == jid)
					if (found) jobMap[jid] = found
				})
			} catch (e) { /* fallback to default labels */ }
		}
		favorites.value = rawFavs.map(f => ({
			...f,
			jobId: f.jobId,
			title: jobMap[f.jobId]?.title || '岗位 #' + f.jobId,
			companyName: jobMap[f.jobId]?.companyName || '',
			location: jobMap[f.jobId]?.location || '',
			experience: jobMap[f.jobId]?.experience || '',
			salaryText: jobMap[f.jobId]?.salaryText || ''
		}))
		deliveredJobIds.value = new Set((dRes.data || []).map(d => d.jobId))
	} catch (e) {
		console.log('API未就绪，使用模拟数据', e)
		favorites.value = mockFavorites
	}
})

const goToDetail = (id) => {
	uni.navigateTo({ url: '/pages/student/job-detail?id=' + id })
}

const handleDeliver = async (job) => {
	const jobId = job.jobId || job.id
	if (deliveredJobIds.value.has(jobId)) {
		uni.showToast({ title: '已投递过', icon: 'none' })
		return
	}
	try {
		await deliveryAPI.createDelivery({ jobId: jobId, studentId: getStudentId() })
		deliveredJobIds.value.add(jobId)
		uni.showToast({ title: '投递成功', icon: 'success' })
	} catch (e) {
		if (e && e.message && e.message.includes('重复投递')) {
			deliveredJobIds.value.add(jobId)
			uni.showToast({ title: '已投递过', icon: 'none' })
		} else {
			uni.showToast({ title: '投递失败', icon: 'none' })
		}
	}
}

const removeFavorite = async (jobId) => {
	try {
		const sid = getStudentId()
		await favoriteAPI.removeFavorite(jobId, sid)
		favorites.value = favorites.value.filter(f => f.jobId !== jobId)
		uni.showToast({ title: '已取消收藏', icon: 'none' })
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'none' })
	}
}

const goBack = () => {
	uni.navigateBack()
}
</script>

<style scoped>
.card-list {
	padding: 16px;
}
.card-item {
	background: white;
	border-radius: 16px;
	padding: 16px;
	margin-bottom: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.card-header-row {
	flex-direction: row;
	justify-content: space-between;
	align-items: flex-start;
	margin-bottom: 8px;
}
.card-title {
	font-size: 16px;
	font-weight: 700;
	color: #1D2129;
	display: block;
	margin-bottom: 4px;
}
.card-sub {
	font-size: 13px;
	color: #86909C;
}
.card-salary {
	font-size: 18px;
	font-weight: 800;
	color: #165DFF;
}
.card-info {
	flex-direction: row;
	gap: 12px;
	font-size: 13px;
	color: #86909C;
	margin-bottom: 12px;
}
.card-actions {
	flex-direction: row;
	gap: 8px;
	padding-top: 12px;
	border-top: 1px solid #F2F3F5;
}
.btn-sm {
	flex: 1;
	padding: 10px;
	border-radius: 10px;
	font-size: 14px;
	font-weight: 600;
	align-items: center;
	justify-content: center;
}
.btn-primary { background: #165DFF; color: white; }
.btn-outline { background: white; border: 1px solid #E2E8F0; color: #4E5969; }
.empty-state {
	padding: 60px 20px;
	align-items: center;
}
.empty-text { font-size: 14px; color: #86909C; }
</style>
