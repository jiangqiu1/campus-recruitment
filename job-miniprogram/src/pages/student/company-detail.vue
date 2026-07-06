<template>
	<view class="page-wrapper">
		<NavBar title="企业详情" showBack @back="goBack" />
		<scroll-view class="content-scrollable" scroll-y>
			<!-- 顶部背景 -->
			<view class="header-bg" />

			<!-- 悬浮企业名片 -->
			<view class="company-card" v-if="company">
				<view class="company-logo"><text>{{ logoText }}</text></view>
				<text class="company-name">{{ company.name || '企业名称' }}</text>
				<text class="company-short" v-if="company.shortName">{{ company.shortName }}</text>
				<view class="company-tags">
					<text class="company-tag" v-if="company.industry">{{ company.industry }}</text>
					<text class="company-tag" v-if="company.size">{{ company.size }}</text>
					<text class="company-tag" v-if="company.city">{{ company.city }}</text>
				</view>
			</view>

			<!-- 1. 公司信息 -->
			<view class="info-card" v-if="company">
				<view class="info-title-row">
					<view class="info-title-left">
						<view class="title-dot" />
						<text class="info-title">公司信息</text>
					</view>
				</view>
				<view class="info-list">
					<view class="info-item">
						<text class="info-label">行业领域</text>
						<view class="info-right">
							<text class="info-value" :class="{ empty: !company.industry }">{{ company.industry || '待填写' }}</text>
						</view>
					</view>
					<view class="info-item">
						<text class="info-label">企业规模</text>
						<view class="info-right">
							<text class="info-value" :class="{ empty: !company.size }">{{ company.size || '待填写' }}</text>
						</view>
					</view>
					<view class="info-item">
						<text class="info-label">所在城市</text>
						<view class="info-right">
							<text class="info-value" :class="{ empty: !company.city }">{{ company.city || '待填写' }}</text>
						</view>
					</view>
					<view class="info-item">
						<text class="info-label">详细地址</text>
						<view class="info-right">
							<text class="info-value" :class="{ empty: !company.address }">{{ company.address || '待填写' }}</text>
						</view>
					</view>
				</view>
			</view>

			<!-- 2. 公司简介 -->
			<view class="info-card" v-if="company">
				<view class="info-title-row">
					<view class="info-title-left">
						<view class="title-dot" />
						<text class="info-title">公司简介</text>
					</view>
				</view>
				<text class="desc-text" :class="{ empty: !company.description }">
					{{ company.description || '暂无公司简介' }}
				</text>
			</view>

			<!-- 3. 资质认证 -->
			<view class="info-card" v-if="company">
				<view class="info-title-row">
					<view class="info-title-left">
						<view class="title-dot" />
						<text class="info-title">资质认证</text>
					</view>
				</view>
				<view class="info-list">
					<view class="info-item">
						<text class="info-label">合作等级</text>
						<view class="info-right">
							<text class="coop-tag" :class="'coop-' + (company.cooperationLevel || 0)">
								{{ coopText }}
							</text>
						</view>
					</view>
					<view class="info-item">
						<text class="info-label">营业执照</text>
						<view class="info-right">
							<text class="info-value" :class="{ empty: !company.licenseUrl }">
								{{ company.licenseUrl ? '已上传' : '未上传' }}
							</text>
						</view>
					</view>
				</view>
			</view>

			<!-- 4. 在招岗位 -->
			<view class="info-card" v-if="jobs.length">
				<view class="info-title-row">
					<view class="info-title-left">
						<view class="title-dot" />
						<text class="info-title">在招岗位 ({{ jobs.length }})</text>
					</view>
				</view>
				<view v-for="job in jobs" :key="job.id" class="job-card" @click="goToJob(job.id)">
					<view class="job-top">
						<text class="job-title">{{ job.title }}</text>
						<text class="job-salary">{{ job.salaryRange || job.salaryText || '面议' }}</text>
					</view>
					<text class="job-location">{{ job.location || '' }}</text>
				</view>
				<EmptyState v-if="!jobs.length" icon="inbox" title="暂无在招岗位" desc="该公司暂无在招岗位" />
			</view>

			<EmptyState v-if="!company" icon="shop" title="暂无数据" desc="企业信息加载失败" />

			<view style="height: calc(40px + env(safe-area-inset-bottom))" />
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { hrAPI, jobAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const company = ref(null)
const jobs = ref([])

const logoText = computed(() => (company.value?.name || '企').charAt(0))

const coopText = computed(() => {
	const level = company.value?.cooperationLevel
	if (level === 3) return '战略合作'
	if (level === 2) return '深度合作'
	if (level === 1) return '合作企业'
	return '未评级'
})

onMounted(async () => {
	try {
		const pages = getCurrentPages()
		const currentPage = pages[pages.length - 1]
		const id = currentPage.$page.options?.id || currentPage.options?.id
		if (!id) return
		const [cRes, jRes] = await Promise.all([
			hrAPI.getCompanyProfile(id),
			jobAPI.getActiveJobs().catch(() => ({ data: [] }))
		])
		company.value = cRes.data || null
		if (jRes.data && jRes.data.length) {
			jobs.value = jRes.data.filter(j => String(j.companyId) === String(id))
		}
	} catch (e) {
		console.error('加载企业详情失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
})

const goToJob = (id) => uni.navigateTo({ url: '/pages/student/job-detail?id=' + id })
const goBack = () => uni.navigateBack()
</script>

<style scoped>
/* ===== 顶部背景 ===== */
.header-bg {
	height: 110px;
	background: linear-gradient(135deg, #165DFF 0%, #2563EB 100%);
}

/* ===== 悬浮企业名片 ===== */
.company-card {
	background: #FFFFFF;
	border-radius: 16px;
	margin: -50px 16px 0;
	padding: 24px 16px 20px;
	align-items: center;
	box-shadow: 0 4px 16px rgba(0,0,0,0.08);
	position: relative;
	z-index: 2;
}
.company-logo {
	width: 72px; height: 72px; border-radius: 20px;
	background: linear-gradient(135deg, #165DFF 0%, #2563EB 100%);
	align-items: center; justify-content: center;
	font-size: 32px; color: white; font-weight: 700;
	box-shadow: 0 4px 12px rgba(22,93,255,0.3);
	margin-bottom: 12px;
}
.company-name { font-size: 19px; font-weight: 700; color: #1D2129; }
.company-short { font-size: 13px; color: #86909C; margin-top: 4px; }
.company-tags {
	flex-direction: row; flex-wrap: wrap;
	gap: 6px; margin-top: 10px;
	justify-content: center;
}
.company-tag {
	font-size: 11px; padding: 3px 10px; border-radius: 12px;
	background: rgba(22,93,255,0.08); color: #165DFF;
}

/* ===== 信息卡片 ===== */
.info-card {
	background: #FFFFFF; border-radius: 12px;
	margin: 12px 16px 0; padding: 18px 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.info-title-row {
	flex-direction: row; justify-content: space-between;
	align-items: center; margin-bottom: 12px;
	padding-bottom: 12px; border-bottom: 0.5px solid #F2F3F5;
}
.info-title-left { flex-direction: row; align-items: center; gap: 8px; }
.title-dot { width: 4px; height: 16px; border-radius: 2px; background: #165DFF; }
.info-title { font-size: 16px; font-weight: 700; color: #1D2129; }

/* ===== 字段行 ===== */
.info-list { gap: 0; }
.info-item {
	flex-direction: row; padding: 12px 0;
	border-bottom: 0.5px solid #F2F3F5;
	align-items: center; justify-content: space-between;
}
.info-item:last-child { border-bottom: none; }
.info-label {
	width: 72px; font-size: 14px; color: #86909C;
	flex-shrink: 0; font-weight: 500;
}
.info-right { flex: 1; flex-direction: row; justify-content: flex-end; }
.info-value {
	font-size: 14px; color: #1D2129;
	font-weight: 500; text-align: right;
}
.info-value.empty { color: #C9CDD4; }

/* ===== 公司简介 ===== */
.desc-text {
	font-size: 13px; color: #4E5969; line-height: 1.8;
	display: block;
}
.desc-text.empty { color: #C9CDD4; font-style: normal; }

/* ===== 合作等级标签 ===== */
.coop-tag {
	font-size: 12px; padding: 3px 12px; border-radius: 10px;
	font-weight: 500;
}
.coop-0 { background: #F2F3F5; color: #86909C; }
.coop-1 { background: rgba(22,93,255,0.08); color: #165DFF; }
.coop-2 { background: rgba(0,180,42,0.08); color: #00B42A; }
.coop-3 { background: rgba(139,92,246,0.1); color: #7C3AED; }

/* ===== 岗位卡片 ===== */
.job-card {
	padding: 14px 0;
	border-bottom: 0.5px solid #F2F3F5;
}
.job-card:last-child { border-bottom: none; }
.job-card:active { background: #F7F8FA; margin: 0 -16px; padding: 14px 16px; }
.job-top { flex-direction: row; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.job-title { font-size: 14px; font-weight: 600; color: #1D2129; flex: 1; }
.job-salary { font-size: 13px; color: #F53F3F; font-weight: 600; }
.job-location { font-size: 12px; color: #86909C; }
</style>
