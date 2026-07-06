<template>
	<view class="page-wrapper">
		<NavBar title="简历管理" show-back />

		<!-- 搜索框：复用学生端首页胶囊样式 -->
		<view class="search-box">
			<uni-icons type="search" size="16" color="#86909C" />
			<input v-model="keyword" placeholder="搜索学生姓名..." @confirm="handleSearch" />
			<text v-if="keyword" class="clear-btn" @click="clearKeyword">
				<uni-icons type="clear" size="16" color="#C9CDD4" />
			</text>
		</view>

		<!-- 筛选条件：简历完成度 + 近期活跃 -->
		<view class="filter-row">
			<picker @change="onCompleteChange" :value="completeIndex" :range="completeOptions">
				<view class="filter-picker">
					<text>{{ completeOptions[completeIndex] }}</text>
					<uni-icons type="arrowdown" size="12" color="#C9CDD4" />
				</view>
			</picker>
			<picker @change="onActiveChange" :value="activeIndex" :range="activeOptions">
				<view class="filter-picker">
					<text>{{ activeOptions[activeIndex] }}</text>
					<uni-icons type="arrowdown" size="12" color="#C9CDD4" />
				</view>
			</picker>
		</view>

		<!-- 筛选 Tab：复用首页 list-tab 统一规范 -->
		<view class="list-tabs">
			<text v-for="(cls, i) in classFilterOptions" :key="i" class="list-tab"
				:class="{ active: currentClass === cls.value }" @click="currentClass = cls.value">
				{{ cls.label }}
			</text>
		</view>

		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<view class="resume-list">
				<view v-for="(stu, i) in filteredStudents" :key="i" class="resume-card" @click="goToResume(stu)">
					<view class="resume-top">
						<view class="resume-avatar">
							<text>{{ (stu.realName || '学').charAt(0) }}</text>
						</view>
						<view class="resume-info">
							<text class="resume-name">{{ stu.realName || '未知' }}</text>
							<text class="resume-class">{{ stu.className || '未分配班级' }} · 学号{{ stu.username || '' }}</text>
							<view class="resume-tags">
								<text class="tag">完成度 {{ stu.resumeComplete || 0 }}%</text>
								<text class="tag">投递{{ stu.deliveryCount || 0 }}次</text>
								<text class="tag">{{ stu.lastActive || '—' }}</text>
							</view>
						</view>
						<uni-icons type="arrowright" size="16" color="#C9CDD4" />
					</view>
				</view>
				<EmptyState v-if="!loading && !filteredStudents.length" icon="file" title="暂无简历" desc="请添加学生或检查筛选条件" />
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { onShow } from '@/utils/page-lifecycle'
import { teacherAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import { checkRole } from '@/utils/auth'

checkRole(1)

const keyword = ref('')
const currentClass = ref('all')
const unreadFilter = ref(false) // 首页待办「未读简历」筛选
const students = ref([])
const loading = ref(true)
const refreshing = ref(false)

const completeOptions = ['简历完成度', '<40%', '40-70%', '>70%']
const completeIndex = ref(0)
const activeOptions = ['近期活跃', '近7天', '近30天']
const activeIndex = ref(0)
const onCompleteChange = (e) => {
	completeIndex.value = e.detail.value
	uni.setStorageSync('teacher_resumes_complete', e.detail.value)
}
const onActiveChange = (e) => {
	activeIndex.value = e.detail.value
	uni.setStorageSync('teacher_resumes_active', e.detail.value)
}

const classFilterOptions = ref([{ label: '全部', value: 'all' }])

const filteredStudents = computed(() => {
	let list = students.value
	// 首页待办「未读简历」筛选 — 筛选有投递记录的学生
	if (unreadFilter.value) {
		list = list.filter(s => (s.deliveryCount || 0) > 0)
	}
	if (currentClass.value !== 'all') {
		list = list.filter(s => s.className === currentClass.value)
	}
	if (keyword.value.trim()) {
		const kw = keyword.value.toLowerCase()
		list = list.filter(s => (s.realName || '').toLowerCase().includes(kw))
	}
	// 简历完成度筛选
	if (completeIndex.value === 1) {
		list = list.filter(s => (s.resumeComplete || 0) < 40)
	} else if (completeIndex.value === 2) {
		list = list.filter(s => (s.resumeComplete || 0) >= 40 && (s.resumeComplete || 0) <= 70)
	} else if (completeIndex.value === 3) {
		list = list.filter(s => (s.resumeComplete || 0) > 70)
	}
	// 近期活跃筛选
	if (activeIndex.value === 1 || activeIndex.value === 2) {
		const now = new Date()
		const days = activeIndex.value === 1 ? 7 : 30
		list = list.filter(s => {
			if (!s.lastActive) return false
			const t = new Date(s.lastActive)
			return (now - t) / (1000 * 60 * 60 * 24) <= days
		})
	}
	return list
})

// 初始化：URL参数解析 + 筛选记忆恢复（onShow 之前执行一次）
{
	const pages = getCurrentPages()
	const cp = pages[pages.length - 1]
	if (cp.options?.filter === 'unread') {
		unreadFilter.value = true
		if (cp.options.className) currentClass.value = cp.options.className
	}
	const savedClass = uni.getStorageSync('teacher_resumes_class')
	if (savedClass && currentClass.value === 'all') currentClass.value = savedClass
	const savedComplete = uni.getStorageSync('teacher_resumes_complete')
	if (savedComplete !== undefined) completeIndex.value = Number(savedComplete)
	const savedActive = uni.getStorageSync('teacher_resumes_active')
	if (savedActive !== undefined) activeIndex.value = Number(savedActive)
}

// 页面显示时刷新数据（含首次加载 + 返回刷新）
onShow(async () => {
	await loadStudents()
	loading.value = false
})

watch(currentClass, (val) => {
	uni.setStorageSync('teacher_resumes_class', val)
})

const onRefresh = async () => {
	refreshing.value = true
	await loadStudents()
	refreshing.value = false
}

const clearKeyword = () => {
	keyword.value = ''
}

const loadStudents = async () => {
	try {
		const classesRes = await teacherAPI.getClasses()
		const classes = classesRes.data || []
		if (!classes.length) { loading.value = false; return }
		const classSet = new Set()
		classSet.add('all')
		classes.forEach(c => classSet.add(c.name))
		classFilterOptions.value = Array.from(classSet).map(v => ({
			label: v === 'all' ? '全部' : v,
			value: v
		}))
		let allStudents = []
		for (const cls of classes) {
			try {
				const res = await teacherAPI.getStudentsByClass(cls.id)
				const list = (res.data || []).map(s => ({ ...s, className: cls.name }))
				allStudents = allStudents.concat(list)
			} catch (e) { /* skip class */ }
		}
		students.value = allStudents
	} catch (e) {
		console.error('加载学生失败:', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

const handleSearch = () => {}
const goToResume = (stu) => {
	uni.navigateTo({ url: '/pages/teacher/student-resume?studentId=' + stu.id + '&name=' + encodeURIComponent(stu.realName) })
}
</script>

<style scoped>
/* 搜索框和学生端完全一致 */
.search-box {
	flex-direction: row;
	align-items: center;
	gap: 8px;
	background: #FFFFFF;
	border-radius: 24px;
	padding: 0 16px;
	height: 40px;
	margin: 12px 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.search-box input { flex: 1; font-size: 14px; background: transparent; border: none; color: #1D2129; height: 100%; }
.clear-btn { line-height: 1; }

.filter-row { flex-direction: row; padding: 0 16px; gap: 12px; margin-bottom: 12px; }
.filter-picker { flex-direction: row; align-items: center; gap: 4px; padding: 6px 12px; background: #F7F8FA; border-radius: 8px; }
.filter-picker text { font-size: 13px; color: #4E5969; }

/* Tab 复用首页样式规范 */
.list-tabs {
	flex-direction: row;
	padding: 0 16px;
	gap: 20px;
	margin-bottom: 12px;
	overflow-x: auto;
}
.list-tab {
	font-size: 15px;
	color: #86909C;
	font-weight: 500;
	padding-bottom: 4px;
	position: relative;
	white-space: nowrap;
}
.list-tab.active {
	color: #1D2129;
	font-weight: 600;
}
.list-tab.active::after {
	content: '';
	position: absolute;
	bottom: 0;
	left: 0;
	width: 20px;
	height: 3px;
	background: #165DFF;
	border-radius: 2px;
}

.resume-list { padding: 0 16px; }
.resume-card {
	flex-direction: row;
	background: white;
	border-radius: 12px;
	padding: 16px;
	margin-bottom: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
	align-items: center;
}
.resume-top {
	flex-direction: row;
	align-items: center;
	gap: 12px;
	flex: 1;
}
.resume-avatar {
	width: 44px;
	height: 44px;
	border-radius: 50%;
	background: linear-gradient(135deg, #165DFF, #2563EB);
	align-items: center;
	justify-content: center;
	font-size: 18px;
	color: white;
	font-weight: 700;
	flex-shrink: 0;
}
.resume-info { flex: 1; }
.resume-name { font-size: 15px; font-weight: 700; color: #1D2129; display: block; margin-bottom: 2px; }
.resume-class { font-size: 12px; color: #86909C; display: block; margin-bottom: 6px; }
.resume-tags { flex-direction: row; gap: 8px; }
.resume-tags .tag {
	padding: 2px 8px;
	border-radius: 6px;
	font-size: 11px;
	font-weight: 500;
	background: rgba(22,93,255,0.08);
	color: #165DFF;
}
.resume-card:active { background: #F7F8FA; }
</style>
