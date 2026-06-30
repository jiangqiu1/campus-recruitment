<template>
	<view class="page-wrapper">
		<view class="header-simple" style="padding:12px 16px;">
			<view class="header-top" style="flex-direction:row;align-items:center;gap:12px;margin-bottom:12px;">
				<text style="font-size:18px;font-weight:700;color:white;">企业资源</text>
			</view>
			<view class="search-box" style="position:relative;flex-direction:row;align-items:center;">
				<text style="position:absolute;left:14px;z-index:1;font-size:16px;">🔍</text>
				<input style="flex:1;height:44px;border-radius:12px;border:none;padding:0 16px 0 42px;font-size:14px;background:rgba(255,255,255,0.95);color:#1D2129;" v-model="keyword" placeholder="搜索企业名称..." @confirm="handleSearch" />
			</view>
		</view>
		<scroll-view class="content-scrollable" scroll-y>
			<view class="company-list">
				<view v-for="(c, i) in filteredCompanies" :key="i" class="company-card">
					<view class="company-top">
						<view class="company-logo">
							<text>{{ (c.name || '企').charAt(0) }}</text>
						</view>
						<view class="company-info">
							<text class="company-name">{{ c.name }}</text>
							<text class="company-industry">{{ c.industry || '未设置行业' }}</text>
						</view>
						<text class="company-arrow">›</text>
					</view>
					<view class="company-meta">
						<text>📍 {{ c.location || '未设置' }}</text>
						<text>👥 {{ c.scale || '未设置' }}</text>
					</view>
					<view class="company-tags" v-if="c.tags && c.tags.length">
						<text v-for="(t, ti) in c.tags.slice(0, 3)" :key="ti" class="tag-tag">{{ t }}</text>
					</view>
				</view>
				<view v-if="!filteredCompanies.length" class="empty-state">
					<text style="font-size:48px;margin-bottom:12px;">🏢</text>
					<text class="empty-text">暂无企业数据</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { teacherAPI } from '@/utils/request'

const keyword = ref('')
const companies = ref([])



const filteredCompanies = computed(() => {
	if (!keyword.value.trim()) return companies.value
	const kw = keyword.value.toLowerCase()
	return companies.value.filter(c => c.name.toLowerCase().includes(kw))
})

onMounted(async () => {
	try {
		const res = await teacherAPI.getCompanies()
		companies.value = res.data || []
	} catch (e) {
		console.error('加载企业列表失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
})

const handleSearch = () => {
	// 搜索由 computed 实时完成
}
</script>

<style scoped>
.header-simple {
	background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
	color: white;
	flex-shrink: 0;
}
.company-list {
	padding: 16px;
}
.company-card {
	background: white;
	border-radius: 16px;
	padding: 16px;
	margin-bottom: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
	position: relative;
	overflow: hidden;
}
.company-card::before {
	content: '';
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 3px;
	background: linear-gradient(90deg, #10B981, transparent);
}
.company-top {
	flex-direction: row;
	align-items: center;
	gap: 12px;
	margin-bottom: 12px;
}
.company-logo {
	width: 48px;
	height: 48px;
	border-radius: 12px;
	background: linear-gradient(135deg, #10B981, #34D399);
	align-items: center;
	justify-content: center;
	font-size: 22px;
	color: white;
	font-weight: 700;
}
.company-info { flex: 1; }
.company-name {
	font-size: 16px;
	font-weight: 700;
	color: #1D2129;
	display: block;
	margin-bottom: 2px;
}
.company-industry {
	font-size: 13px;
	color: #86909C;
	display: block;
}
.company-arrow { color: #C9CDD4; font-size: 20px; }
.company-meta {
	flex-direction: row;
	gap: 16px;
	font-size: 13px;
	color: #86909C;
	margin-bottom: 8px;
}
.company-tags {
	flex-direction: row;
	gap: 8px;
}
.tag-tag {
	padding: 4px 10px;
	border-radius: 8px;
	font-size: 12px;
	font-weight: 500;
	background: rgba(16,185,129,0.08);
	color: #10B981;
}
.empty-state {
	padding: 60px 20px;
	align-items: center;
	justify-content: center;
}
.empty-text { font-size: 14px; color: #86909C; }
</style>
