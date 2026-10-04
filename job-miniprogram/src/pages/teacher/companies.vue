<template>
	<view class="page-wrapper">
		<NavBar title="企业资源" show-back />
		<view class="search-bar">
			<view class="search-input">
				<uni-icons type="search" size="16" color="#86909C" />
				<input v-model="keyword" placeholder="搜索企业名称..." @confirm="handleSearch" />
				<text v-if="keyword" class="clear-btn" @click="clearKeyword">
					<uni-icons type="clear" size="16" color="#C9CDD4" />
				</text>
			</view>
		</view>

		<scroll-view class="content-scrollable" scroll-y>
			<LoadingState type="skeleton" :rows="5" v-if="loading" />
			<view class="company-list" v-else>
				<view v-for="(c, i) in filteredCompanies" :key="i" class="company-card" @click="goToCompany(c.id)">
					<view class="company-top">
						<view class="company-logo">
							<text>{{ (c.name || '企').charAt(0) }}</text>
						</view>
						<view class="company-info">
							<text class="company-name">{{ c.name }}</text>
							<text class="company-industry" v-if="c.industry">{{ c.industry }}</text>
						</view>
						<uni-icons type="arrowright" size="16" color="#C9CDD4" />
					</view>
					<view class="company-meta">
						<text v-if="c.location"><uni-icons type="location" size="12" color="#C9CDD4" /> {{ c.location }}</text>
						<text v-if="c.scale"><uni-icons type="person" size="12" color="#C9CDD4" /> {{ c.scale }}</text>
					</view>
					<view class="company-tags" v-if="c.tags && c.tags.length">
						<text v-for="(t, ti) in c.tags.slice(0, 3)" :key="ti" class="tag-tag">{{ t }}</text>
					</view>
				</view>
				<EmptyState v-if="!filteredCompanies.length" icon="shop" title="暂无企业" desc="暂无企业数据" />
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@/utils/page-lifecycle'
import { teacherAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import { checkRole } from '@/utils/auth'

checkRole(1)

const keyword = ref('')
const companies = ref([])

const filteredCompanies = computed(() => {
	if (!keyword.value.trim()) return companies.value
	const kw = keyword.value.toLowerCase()
	return companies.value.filter(c => c.name.toLowerCase().includes(kw))
})

const clearKeyword = () => { keyword.value = '' }

const loading = ref(true)

const loadCompanies = async () => {
	loading.value = true
	try {
		const res = await teacherAPI.getCompanies()
		companies.value = res.data || []
		loading.value = false
	} catch (e) {
		console.error('加载企业列表失败', e)
		loading.value = false
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

// 页面显示时刷新数据（含首次加载 + 返回刷新）
onShow(() => { loadCompanies() })

const handleSearch = () => {}

const goToCompany = (id) => {
	uni.navigateTo({ url: '/pages/teacher/company-detail?id=' + id })
}
</script>

<style scoped lang="scss">
.search-bar { padding: 8px 16px; background: $uni-bg-color; }
.search-input {
	flex-direction: row;
	align-items: center;
	gap: 8px;
	background: $uni-bg-color-page;
	border-radius: 999px;
	padding: 0 16px;
	height: 36px;
}
.search-input input { flex: 1; font-size: 14px; background: transparent; border: none; color: $uni-text-color-title; }
.clear-btn { line-height: 1; }

.company-list { padding: 16px; }
.company-card {
	background: white;
	border-radius: 12px;
	padding: 16px;
	margin-bottom: 12px;
	box-shadow: $uni-shadow-card;
}
.company-top { flex-direction: row; align-items: center; gap: 12px; margin-bottom: 12px; }
.company-logo {
	width: 48px;
	height: 48px;
	border-radius: 12px;
	background: $uni-gradient-primary;
	align-items: center;
	justify-content: center;
	font-size: 22px;
	color: white;
	font-weight: 700;
}
.company-info { flex: 1; }
.company-name { font-size: 16px; font-weight: 700; color: $uni-text-color-title; display: block; margin-bottom: 2px; }
.company-industry { font-size: 13px; color: $uni-text-color-secondary; display: block; }
.company-meta { flex-direction: row; gap: 16px; font-size: 13px; color: $uni-text-color-secondary; margin-bottom: 8px; }
.company-tags { flex-direction: row; gap: 8px; }
.tag-tag { padding: 4px 10px; border-radius: 8px; font-size: 12px; font-weight: 500; background: $uni-color-primary-light; color: $uni-color-primary; }
.company-card:active { background: $uni-bg-color-page; }
</style>
