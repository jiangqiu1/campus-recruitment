<template>
	<view class="page-wrapper">
		<scroll-view class="content-scrollable" scroll-y>
			<!-- 企业信息头部 -->
			<view class="company-header">
				<view class="company-logo">
					<text>{{ logoText }}</text>
				</view>
				<text class="company-name">{{ company.name || '企业名称' }}</text>
				<text class="company-intro">{{ company.intro || '暂无简介' }}</text>
			</view>

			<!-- 基本信息 -->
			<view class="section-card">
				<view class="section-title-row">
					<text class="section-title">📋 基本信息</text>
					<text v-if="!isEditing" class="section-action" @click="isEditing = true">编辑</text>
					<text v-else class="section-action" @click="saveCompany">保存</text>
				</view>

				<view class="info-list">
					<view class="info-item">
						<text class="info-label">行业领域</text>
						<input v-if="isEditing" class="info-input" v-model="form.industry" placeholder="如：互联网/IT" />
						<text v-else class="info-value">{{ company.industry || '--' }}</text>
					</view>
					<view class="info-item">
						<text class="info-label">企业规模</text>
						<input v-if="isEditing" class="info-input" v-model="form.size" placeholder="如：50-200人" />
						<text v-else class="info-value">{{ company.size || '--' }}</text>
					</view>
					<view class="info-item">
						<text class="info-label">所在地址</text>
						<input v-if="isEditing" class="info-input" v-model="form.address" placeholder="如：广州市天河区" />
						<text v-else class="info-value">{{ company.address || '--' }}</text>
					</view>
					<view class="info-item" style="border:none;">
						<text class="info-label">官方网站</text>
						<input v-if="isEditing" class="info-input" v-model="form.website" placeholder="如：https://www.example.com" />
						<text v-else class="info-value">{{ company.website || '--' }}</text>
					</view>
				</view>
			</view>

			<!-- 企业相册 -->
			<view class="section-card">
				<view class="section-title-row">
					<text class="section-title">🖼️ 企业相册</text>
					<text class="section-action" @click="addPhoto">添加照片</text>
				</view>
				<view class="photo-grid">
					<view v-for="(photo, i) in photos" :key="i" class="photo-item">
						<text class="photo-placeholder">{{ photo.icon }}</text>
					</view>
					<view class="photo-item photo-add" @click="addPhoto">
						<text class="photo-add-icon">+</text>
					</view>
				</view>
			</view>

			<!-- 联系人信息 -->
			<view class="section-card">
				<view class="section-title-row">
					<text class="section-title">📞 联系人信息</text>
				</view>
				<view class="info-list">
					<view class="info-item">
						<text class="info-label">联系人</text>
						<text class="info-value">{{ company.contactName || userInfo.realName || '--' }}</text>
					</view>
					<view class="info-item">
						<text class="info-label">联系电话</text>
						<text class="info-value">{{ company.contactPhone || '--' }}</text>
					</view>
					<view class="info-item" style="border:none;">
						<text class="info-label">联系邮箱</text>
						<text class="info-value">{{ company.contactEmail || '--' }}</text>
					</view>
				</view>
			</view>
		</scroll-view>

		<HrTabBar current="company" />
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { hrAPI } from '@/utils/request'
import HrTabBar from '@/components/HrTabBar.vue'

const userInfo = ref({})
const company = ref({})
const isEditing = ref(false)
const form = ref({ industry: '', size: '', address: '', website: '' })
const photos = ref([])

const logoText = computed(() => {
	return (company.value.name || '企').charAt(0)
})



const getCompanyId = () => {
	try {
		const raw = uni.getStorageSync('userInfo')
		if (!raw) return null
		const obj = JSON.parse(raw)
		return obj.companyId || obj.id || null
	} catch (e) { return null }
}

onMounted(async () => {
	try {
		const stored = uni.getStorageSync('userInfo')
		if (stored) userInfo.value = JSON.parse(stored)
	} catch (e) {}

	try {
		const cId = getCompanyId()
		if (cId) {
			const res = await hrAPI.getCompanyProfile(cId)
			company.value = res.data || {}
		}
		form.value = {
			industry: company.value.industry || '',
			size: company.value.size || '',
			address: company.value.address || '',
			website: company.value.website || ''
		}
	} catch (e) {
		console.error('加载企业信息失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
})

const saveCompany = async () => {
	try {
		const cId = getCompanyId()
		await hrAPI.updateCompany(cId, form.value)
		company.value = { ...company.value, ...form.value }
		isEditing.value = false
		uni.showToast({ title: '保存成功', icon: 'success' })
	} catch (e) {
		uni.showToast({ title: '保存失败', icon: 'none' })
	}
}

const addPhoto = () => {
	uni.showToast({ title: '功能开发中', icon: 'none' })
}
</script>

<style scoped>
.company-header {
	background: linear-gradient(135deg, #0EA5E9 0%, #38BDF8 100%);
	color: white;
	padding: 40px 16px 30px;
	align-items: center;
	text-align: center;
	position: relative;
	overflow: hidden;
}
.company-header::before {
	content: '';
	position: absolute;
	top: -50%;
	right: -20%;
	width: 200px;
	height: 200px;
	background: rgba(255,255,255,0.08);
	border-radius: 50%;
}
.company-logo {
	width: 80px;
	height: 80px;
	border-radius: 20px;
	background: rgba(255,255,255,0.2);
	align-items: center;
	justify-content: center;
	font-size: 36px;
	border: 3px solid rgba(255,255,255,0.3);
	margin-bottom: 16px;
	position: relative;
	z-index: 1;
}
.company-name {
	font-size: 22px;
	font-weight: 700;
	margin-bottom: 8px;
	position: relative;
	z-index: 1;
}
.company-intro {
	font-size: 13px;
	opacity: 0.85;
	text-align: center;
	padding: 0 20px;
	line-height: 1.6;
	position: relative;
	z-index: 1;
}

/* 卡片样式 */
.section-card {
	background: white;
	border-radius: 16px;
	margin: 12px 16px;
	padding: 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.section-title-row {
	flex-direction: row;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 12px;
}
.section-title {
	font-size: 16px;
	font-weight: 700;
	color: #1D2129;
}
.section-action {
	font-size: 13px;
	color: #0EA5E9;
	font-weight: 500;
}
.info-list {
	gap: 0;
}
.info-item {
	flex-direction: row;
	padding: 12px 0;
	border-bottom: 1px solid #F2F3F5;
	align-items: center;
}
.info-item:last-child { border-bottom: none; }
.info-label {
	width: 80px;
	font-size: 14px;
	color: #86909C;
	flex-shrink: 0;
}
.info-value {
	flex: 1;
	font-size: 14px;
	color: #1D2129;
}
.info-input {
	flex: 1;
	height: 40px;
	border: 2px solid #E2E8F0;
	border-radius: 8px;
	padding: 0 12px;
	font-size: 14px;
	background: #F8F9FC;
	color: #1D2129;
}

/* 相册 */
.photo-grid {
	flex-direction: row;
	flex-wrap: wrap;
	gap: 10px;
}
.photo-item {
	width: calc(33.33% - 7px);
	aspect-ratio: 1;
	border-radius: 12px;
	background: #F2F3F5;
	align-items: center;
	justify-content: center;
	font-size: 32px;
	overflow: hidden;
}
.photo-placeholder { font-size: 32px; }
.photo-add {
	border: 2px dashed #D2D5DA;
	background: transparent;
}
.photo-add-icon {
	font-size: 28px;
	color: #86909C;
}
</style>
