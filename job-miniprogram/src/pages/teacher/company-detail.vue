<template>
	<view class="page-wrapper">
		<NavBar title="企业详情" show-back />
		<scroll-view class="content-scrollable" scroll-y>
			<view class="header-bg" />
			<view class="company-card" v-if="company">
				<view class="company-logo">{{ (company.name || '企').charAt(0) }}</view>
				<text class="company-name">{{ company.name }}</text>
				<text class="company-short">{{ company.shortName || '' }}</text>
				<view class="company-tags" v-if="company.industry || company.size || company.city">
					<text class="company-tag" v-if="company.industry">{{ company.industry }}</text>
					<text class="company-tag" v-if="company.size">{{ company.size }}</text>
					<text class="company-tag" v-if="company.city">{{ company.city }}</text>
				</view>
			</view>
			<EmptyState v-else icon="shop" title="暂无数据" desc="企业信息加载失败" />

			<view class="info-card" v-if="company">
				<view class="info-title-row"><text class="info-title">公司信息</text></view>
				<view class="info-list">
					<view class="info-item"><text class="info-label">行业领域</text><text class="info-value">{{ company.industry || '—' }}</text></view>
					<view class="info-item"><text class="info-label">企业规模</text><text class="info-value">{{ company.size || '—' }}</text></view>
					<view class="info-item"><text class="info-label">所在城市</text><text class="info-value">{{ company.city || '—' }}</text></view>
					<view class="info-item"><text class="info-label">详细地址</text><text class="info-value">{{ company.address || '—' }}</text></view>
					<view class="info-item"><text class="info-label">联系人</text><text class="info-value">{{ company.contactPerson || '—' }}</text></view>
					<view class="info-item"><text class="info-label">联系电话</text><text class="info-value">{{ company.contactPhone || '—' }}</text></view>
				</view>
			</view>

			<view class="info-card" v-if="company && company.description">
				<view class="info-title-row"><text class="info-title">公司简介</text></view>
				<text class="desc-text">{{ company.description }}</text>
			</view>

			<view class="info-card" v-if="company">
				<view class="info-title-row"><text class="info-title">资质认证</text></view>
				<view class="info-list">
					<view class="info-item"><text class="info-label">合作等级</text><text class="coop-tag" :class="'coop-' + (company.cooperationLevel ?? 0)">{{ coopText }}</text></view>
					<view class="info-item"><text class="info-label">营业执照</text><text class="info-value">{{ company.licenseUrl ? '已上传' : '未上传' }}</text></view>
				</view>
			</view>

			<view style="height:40px" />
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { request } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const company = ref(null)

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
		const res = await request({ url: '/companies/' + id })
		company.value = res.data || null
	} catch (e) {
		console.error('加载企业详情失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
})
</script>

<style scoped>
.page-wrapper {
	min-height: 100vh;
	background: #F7F8FA;
}
.content-scrollable {
	height: 100vh;
}

/* ===== 蓝色渐变头部背景 ===== */
.header-bg {
	height: 160px;
	background: linear-gradient(180deg, #165DFF 0%, #3B7AFF 60%, #E8EDFF 100%);
}

/* ===== 浮动白色公司卡片 ===== */
.company-card {
	margin: -80px 16px 12px;
	background: #FFFFFF;
	border-radius: 16px;
	padding: 24px 20px 20px;
	align-items: center;
	box-shadow: 0 4px 24px rgba(22,93,255,0.10);
	position: relative;
	z-index: 1;
}
.company-logo {
	width: 64px;
	height: 64px;
	border-radius: 16px;
	background: linear-gradient(135deg, #165DFF, #60A5FA);
	align-items: center;
	justify-content: center;
	font-size: 28px;
	color: #FFFFFF;
	font-weight: 700;
	margin-bottom: 12px;
	box-shadow: 0 4px 12px rgba(22,93,255,0.25);
}
.company-name {
	font-size: 20px;
	font-weight: 700;
	color: #1D2129;
	margin-bottom: 4px;
}
.company-short {
	font-size: 13px;
	color: #86909C;
	margin-bottom: 12px;
}
.company-tags {
	flex-direction: row;
	flex-wrap: wrap;
	gap: 8px;
	justify-content: center;
}
.company-tag {
	font-size: 12px;
	color: #165DFF;
	background: rgba(22,93,255,0.08);
	padding: 4px 12px;
	border-radius: 20px;
	font-weight: 500;
}

/* ===== 信息卡片 ===== */
.info-card {
	background: #FFFFFF;
	margin: 0 16px 12px;
	border-radius: 12px;
	padding: 20px 16px;
	box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.info-title-row {
	margin-bottom: 16px;
}
.info-title {
	font-size: 16px;
	font-weight: 700;
	color: #1D2129;
	padding-left: 12px;
	border-left: 4px solid #165DFF;
	display: block;
	line-height: 1.4;
}
.info-list {
	gap: 0;
}
.info-item {
	flex-direction: row;
	align-items: center;
	padding: 12px 0;
	border-bottom: 0.5px solid #F2F3F5;
}
.info-item:last-child {
	border-bottom: none;
}
.info-label {
	font-size: 14px;
	color: #86909C;
	width: 80px;
	flex-shrink: 0;
}
.info-value {
	font-size: 14px;
	color: #1D2129;
	flex: 1;
	text-align: right;
}

/* ===== 公司简介描述 ===== */
.desc-text {
	font-size: 14px;
	color: #4E5969;
	line-height: 1.8;
	display: block;
	white-space: pre-line;
}

/* ===== 合作等级标签 ===== */
.coop-tag {
	font-size: 13px;
	font-weight: 600;
	padding: 2px 12px;
	border-radius: 12px;
	flex-shrink: 0;
}
.coop-0 {
	color: #86909C;
	background: #F2F3F5;
}
.coop-1 {
	color: #165DFF;
	background: rgba(22,93,255,0.08);
}
.coop-2 {
	color: #722ED1;
	background: rgba(114,46,209,0.08);
}
.coop-3 {
	color: #F77234;
	background: rgba(247,114,52,0.08);
}

/* ===== 加载失败占位兼容 ===== */
.company-card:has(+ EmptyState) {
	display: block;
}
</style>
