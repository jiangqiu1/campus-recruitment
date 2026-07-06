<template>
	<view class="page-wrapper">
		<NavBar title="企业信息" showBack @back="goBack" />
		<scroll-view class="content-scrollable" scroll-y>
			<!-- 顶部背景 -->
			<view class="header-bg" />

			<!-- 悬浮企业名片 -->
			<view class="company-card">
				<view class="company-logo"><text>{{ logoText }}</text></view>
				<text class="company-name">{{ company.name || '企业名称' }}</text>
				<text class="company-short">{{ company.shortName || '' }}</text>
				<view class="company-tags" v-if="!isEditing">
					<text class="company-tag" v-if="company.industry">{{ company.industry }}</text>
					<text class="company-tag" v-if="company.size">{{ company.size }}</text>
					<text class="company-tag" v-if="company.city">{{ company.city }}</text>
				</view>
			</view>

			<!-- 1. 公司信息 -->
			<view class="info-card">
				<view class="info-title-row">
					<view class="info-title-left">
						<view class="title-dot" />
						<text class="info-title">公司信息</text>
					</view>
					<text class="info-edit" @click="toggleEdit">
						{{ isEditing ? '取消' : '编辑' }}
					</text>
				</view>
				<view class="info-list">
					<view class="info-item">
						<text class="info-label">行业领域</text>
						<view class="info-right">
							<input v-if="isEditing" class="info-input" v-model="form.industry" placeholder="如：互联网/IT" />
							<text v-else class="info-value" :class="{ empty: !company.industry }">{{ company.industry || '待填写' }}</text>
						</view>
					</view>
					<view class="info-item">
						<text class="info-label">企业规模</text>
						<view class="info-right">
							<input v-if="isEditing" class="info-input" v-model="form.size" placeholder="如：50-200人" />
							<text v-else class="info-value" :class="{ empty: !company.size }">{{ company.size || '待填写' }}</text>
						</view>
					</view>
					<view class="info-item">
						<text class="info-label">所在城市</text>
						<view class="info-right">
							<input v-if="isEditing" class="info-input" v-model="form.city" placeholder="如：广州" />
							<text v-else class="info-value" :class="{ empty: !company.city }">{{ company.city || '待填写' }}</text>
						</view>
					</view>
					<view class="info-item">
						<text class="info-label">详细地址</text>
						<view class="info-right">
							<input v-if="isEditing" class="info-input" v-model="form.address" placeholder="如：天河区XX大厦" />
							<text v-else class="info-value" :class="{ empty: !company.address }">{{ company.address || '待填写' }}</text>
						</view>
					</view>
				</view>
			</view>

			<!-- 2. 公司简介 -->
			<view class="info-card">
				<view class="info-title-row">
					<view class="info-title-left">
						<view class="title-dot" />
						<text class="info-title">公司简介</text>
					</view>
				</view>
				<view v-if="isEditing">
					<textarea class="info-textarea" v-model="form.description" placeholder="介绍一下公司的主营业务、发展历程、团队文化等，学生投递时会看到" />
				</view>
				<text v-else class="desc-text" :class="{ empty: !company.description }">
					{{ company.description || '暂无公司简介' }}
				</text>
			</view>

			<!-- 3. 资质认证 -->
			<view class="info-card">
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

			<!-- 4. 招聘设置 -->
			<view class="info-card">
				<view class="info-title-row">
					<view class="info-title-left">
						<view class="title-dot" />
						<text class="info-title">招聘设置</text>
					</view>
				</view>
				<view class="info-list">
					<view class="info-item">
						<text class="info-label">联系人</text>
						<view class="info-right">
							<input v-if="isEditing" class="info-input" v-model="form.contactInfo" placeholder="姓名" />
							<text v-else class="info-value" :class="{ empty: !company.contactPerson && !company.contactPhone }">
								{{ company.contactPerson || company.contactPhone || '待填写' }}
							</text>
						</view>
					</view>
					<view class="info-item">
						<text class="info-label">联系电话</text>
						<view class="info-right">
							<text class="info-value" :class="{ empty: !company.contactPhone }">
								{{ company.contactPhone || '待填写' }}
							</text>
						</view>
					</view>
				</view>
			</view>

			<!-- 保存按钮 -->
			<view v-if="isEditing" class="save-bar">
				<button class="save-btn" @click="saveCompany">保存修改</button>
			</view>

			<view style="height: calc(80px + env(safe-area-inset-bottom));" />
		</scroll-view>
		<HrTabBar current="profile" />
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { hrAPI } from '@/utils/request'
import HrTabBar from '@/components/HrTabBar.vue'
import NavBar from '@/components/NavBar.vue'

const userInfo = ref({})
const company = ref({})
const isEditing = ref(false)
const form = ref({ industry: '', size: '', city: '', address: '', description: '', contactInfo: '' })

const logoText = computed(() => (company.value.name || '企').charAt(0))

const coopText = computed(() => {
	const level = company.value.cooperationLevel
	if (level === 3) return '战略合作'
	if (level === 2) return '深度合作'
	if (level === 1) return '合作企业'
	return '未评级'
})

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

const toggleEdit = () => {
	if (isEditing.value) {
		// 取消编辑，重置表单
		resetForm()
	}
	isEditing.value = !isEditing.value
}

const resetForm = () => {
	form.value = {
		industry: company.value.industry || '',
		size: company.value.size || '',
		city: company.value.city || '',
		address: company.value.address || '',
		description: company.value.description || '',
		contactInfo: company.value.contactPerson || company.value.contactPhone || ''
	}
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
			resetForm()
		}
	} catch (e) {
		console.error('加载企业信息失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
})

const saveCompany = async () => {
	try {
		const cId = getCompanyId()
		const saveData = {
			industry: form.value.industry,
			size: form.value.size,
			city: form.value.city,
			address: form.value.address,
			description: form.value.description,
			contactPerson: form.value.contactInfo
		}
		await hrAPI.updateCompany(cId, saveData)
		company.value = { ...company.value, ...saveData, contactInfo: form.value.contactInfo, contactPerson: form.value.contactInfo }
		isEditing.value = false
		uni.showToast({ title: '保存成功', icon: 'success' })
	} catch (e) {
		uni.showToast({ title: '保存失败', icon: 'none' })
	}
}

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
.info-edit {
	font-size: 13px; color: #165DFF; font-weight: 500;
	padding: 6px 14px; border-radius: 8px; background: rgba(22,93,255,0.06);
}
.info-edit:active { background: rgba(22,93,255,0.12); }

/* ===== 字段行 ===== */
.info-list { gap: 0; }
.info-item {
	flex-direction: row; padding: 12px 0;
	border-bottom: 0.5px solid #F2F3F5;
	align-items: center; justify-content: space-between;
}
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
.info-input {
	flex: 1; height: 40px; border: 1px solid #E5E6EB;
	border-radius: 8px; padding: 0 12px;
	font-size: 14px; background: #F7F8FA; color: #1D2129;
	text-align: right;
}
.info-input:focus { border-color: #165DFF; background: #FFFFFF; }

/* ===== 公司简介 ===== */
.desc-text {
	font-size: 13px; color: #4E5969; line-height: 1.8;
	display: block;
}
.desc-text.empty { color: #C9CDD4; font-style: normal; }
.info-textarea {
	width: 100%; min-height: 120px;
	border: 1px solid #E5E6EB; border-radius: 8px;
	padding: 12px; font-size: 13px; color: #1D2129;
	background: #F7F8FA; line-height: 1.8;
}
.info-textarea:focus { border-color: #165DFF; background: #FFFFFF; }

/* ===== 合作等级标签 ===== */
.coop-tag {
	font-size: 12px; padding: 3px 12px; border-radius: 10px;
	font-weight: 500;
}
.coop-0 { background: #F2F3F5; color: #86909C; }
.coop-1 { background: rgba(22,93,255,0.08); color: #165DFF; }
.coop-2 { background: rgba(0,180,42,0.08); color: #00B42A; }
.coop-3 { background: rgba(139,92,246,0.1); color: #7C3AED; }

/* ===== 保存按钮 ===== */
.save-bar { padding: 16px 16px 0; }
.save-btn {
	width: 100%; height: 48px; border-radius: 12px;
	background: linear-gradient(135deg, #165DFF 0%, #2563EB 100%);
	color: white; font-size: 16px; font-weight: 600;
	border: none; box-shadow: 0 4px 12px rgba(22,93,255,0.3);
}
.save-btn:active { opacity: 0.9; }
</style>
