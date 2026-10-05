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
					<view class="info-item" v-if="company.industry">
						<text class="info-label">行业领域</text>
						<view class="info-right">
							<text class="info-value">{{ company.industry }}</text>
						</view>
					</view>
					<view class="info-item" v-if="company.size">
						<text class="info-label">企业规模</text>
						<view class="info-right">
							<text class="info-value">{{ company.size }}</text>
						</view>
					</view>
					<view class="info-item" v-if="company.city">
						<text class="info-label">所在城市</text>
						<view class="info-right">
							<text class="info-value">{{ company.city }}</text>
						</view>
					</view>
					<view class="info-item" v-if="company.address">
						<text class="info-label">详细地址</text>
						<view class="info-right">
							<text class="info-value">{{ company.address }}</text>
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
				<text class="desc-text" v-if="company.description">{{ company.description }}</text>
				<text class="desc-text empty" v-else>企业暂未完善简介</text>
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

<style scoped lang="scss">
/* ===== 顶部背景 ===== */
.header-bg {
	height: 110px;
	background: $uni-gradient-primary;
}

/* ===== 悬浮企业名片 ===== */
.company-card {
	background: $uni-bg-color;
	border-radius: 16px;
	margin: -50px 16px 0;
	padding: 24px 16px 20px;
	align-items: center;
	box-shadow: 0 4px 16px rgba(0,0,0,0.08);
	position: relative;
	z-index: 2;
}
.company-logo {
	width: 72px; height: 72px; border-radius: 999px;
	background: $uni-gradient-primary;
	align-items: center; justify-content: center;
	font-size: 32px; color: white; font-weight: 700;
	box-shadow: 0 4px 12px $uni-color-primary-light;
	margin-bottom: 12px;
}
.company-name { font-size: 19px; font-weight: 700; color: $uni-text-color-title; }
.company-short { font-size: 13px; color: $uni-text-color-secondary; margin-top: 4px; }
.company-tags {
	flex-direction: row; flex-wrap: wrap;
	gap: 6px; margin-top: 10px;
	justify-content: center;
}
.company-tag {
	font-size: 12px; padding: 3px 10px; border-radius: 12px;
	background: $uni-color-primary-light; color: $uni-color-primary;
}

/* ===== 信息卡片 ===== */
.info-card {
	background: $uni-bg-color; border-radius: 12px;
	margin: 12px 16px 0; padding: 18px 16px;
	box-shadow: $uni-shadow-card;
}
.info-title-row {
	flex-direction: row; justify-content: space-between;
	align-items: center; margin-bottom: 12px;
	padding-bottom: 12px; border-bottom: 0.5px solid $uni-border-color-divider;
}
.info-title-left { flex-direction: row; align-items: center; gap: 8px; }
.title-dot { width: 4px; height: 16px; border-radius: 4px; background: $uni-color-primary; }
.info-title { font-size: 16px; font-weight: 700; color: $uni-text-color-title; }

/* ===== 字段行 ===== */
.info-list { gap: 0; }
.info-item {
	flex-direction: row; padding: 12px 0;
	border-bottom: 0.5px solid $uni-border-color-divider;
	align-items: center; justify-content: space-between;
}
.info-item:last-child { border-bottom: none; }
.info-label {
	width: 72px; font-size: 14px; color: $uni-text-color-secondary;
	flex-shrink: 0; font-weight: 500;
}
.info-right { flex: 1; flex-direction: row; justify-content: flex-end; }
.info-value {
	font-size: 14px; color: $uni-text-color-title;
	font-weight: 500; text-align: right;
}
.info-value.empty { color: $uni-text-color-placeholder; }

/* ===== 公司简介 ===== */
.desc-text {
	font-size: 13px; color: $uni-text-color; line-height: 1.8;
	display: block;
}
.desc-text.empty { color: $uni-text-color-placeholder; font-style: normal; }

/* ===== 合作等级标签 ===== */
.coop-tag {
	font-size: 12px; padding: 3px 12px; border-radius: 12px;
	font-weight: 500;
}
.coop-0 { background: $uni-border-color-divider; color: $uni-text-color-secondary; }
.coop-1 { background: $uni-color-primary-light; color: $uni-color-primary; }
.coop-2 { background: $uni-color-success-light; color: $uni-color-success; }
.coop-3 { background: $uni-color-warning-light; color: $uni-color-warning; }

/* ===== 岗位卡片 ===== */
.job-card {
	padding: 14px 0;
	border-bottom: 0.5px solid $uni-border-color-divider;
}
.job-card:last-child { border-bottom: none; }
.job-card:active { background: $uni-bg-color-page; margin: 0 -16px; padding: 14px 16px; }
.job-top { flex-direction: row; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.job-title { font-size: 14px; font-weight: 600; color: $uni-text-color-title; flex: 1; }
.job-salary { font-size: 13px; color: $uni-color-error; font-weight: 600; }
.job-location { font-size: 12px; color: $uni-text-color-secondary; }
</style>
