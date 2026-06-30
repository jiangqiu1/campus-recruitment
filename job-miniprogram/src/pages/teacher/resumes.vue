<template>
	<view class="page-wrapper">
		<view class="header-simple" style="padding:12px 16px;">
			<view class="header-top" style="flex-direction:row;align-items:center;gap:12px;margin-bottom:12px;">
				<text style="font-size:20px;font-weight:700;" @click="goBack">‹</text>
				<text style="font-size:18px;font-weight:700;color:white;flex:1;">简历管理</text>
				<text class="export-btn" @click="exportCSV">📥 导出</text>
			</view>
			<view class="search-box" style="position:relative;flex-direction:row;align-items:center;gap:8px;">
				<text style="position:absolute;left:14px;z-index:1;font-size:16px;">🔍</text>
				<input style="flex:1;height:44px;border-radius:12px;border:none;padding:0 16px 0 42px;font-size:14px;background:rgba(255,255,255,0.95);color:#1D2129;" v-model="keyword" placeholder="搜索学生姓名..." @confirm="handleSearch" />
			</view>
			<!-- 班级筛选 -->
			<view class="class-filter" style="flex-direction:row;gap:8px;margin-top:10px;">
				<text v-for="(cls, i) in classFilterOptions" :key="i" class="filter-tab" :class="{ active: currentClass === cls.value }" @click="currentClass = cls.value">{{ cls.label }}</text>
			</view>
		</view>
		<scroll-view class="content-scrollable" scroll-y>
			<view class="resume-list">
				<view v-for="(stu, i) in filteredStudents" :key="i" class="resume-card" @click="goToResume(stu)">
					<view class="resume-top">
						<view class="resume-avatar">
							<text>{{ (stu.realName || '学').charAt(0) }}</text>
						</view>
						<view class="resume-info">
							<text class="resume-name">{{ stu.realName || '未知' }}</text>
							<text class="resume-class">{{ stu.className || '未分配班级' }}</text>
						</view>
						<view class="resume-status-tag">
							<text style="font-size:12px;color:#10B981;">● 正常</text>
						</view>
					</view>
					<view class="resume-meta">
						<text>🆔 {{ stu.username || '—' }}</text>
						<text>📱 {{ stu.phone || '未绑定' }}</text>
					</view>
				</view>
				<view v-if="loading && !students.length" class="empty-state">
					<text style="font-size:48px;margin-bottom:12px;">⏳</text>
					<text class="empty-text">加载中...</text>
				</view>
				<view v-if="!loading && !filteredStudents.length" class="empty-state">
					<text style="font-size:48px;margin-bottom:12px;">📄</text>
					<text class="empty-text">暂无简历数据</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { teacherAPI } from '@/utils/request'

const keyword = ref('')
const currentClass = ref('all')
const students = ref([])
const loading = ref(true)

const classFilterOptions = ref([
	{ label: '全部', value: 'all' },
])

const filteredStudents = computed(() => {
	let list = students.value
	if (currentClass.value !== 'all') {
		list = list.filter(s => s.className === currentClass.value)
	}
	if (keyword.value.trim()) {
		const kw = keyword.value.toLowerCase()
		list = list.filter(s => (s.realName || '').toLowerCase().includes(kw))
	}
	return list
})

onMounted(async () => {
	await loadStudents()
	loading.value = false
})

const loadStudents = async () => {
	try {
		const classesRes = await teacherAPI.getClasses()
		const classes = classesRes.data || []
		if (!classes.length) {
			loading.value = false
			return
		}
		// Build class filter options
		const classSet = new Set()
		classSet.add('all')
		classes.forEach(c => classSet.add(c.name))
		classFilterOptions.value = Array.from(classSet).map(v => ({
			label: v === 'all' ? '全部' : v,
			value: v
		}))
		// Load students from each class
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

const handleSearch = () => {
	// computed handles real-time filtering
}

const goBack = () => {
	uni.navigateBack()
}

const exportCSV = () => {
	if (!students.value.length) {
		uni.showToast({ title: '暂无数据可导出', icon: 'none' })
		return
	}
	const BOM = '\uFEFF'
	const headers = '姓名,班级,学号,手机号\n'
	const rows = filteredStudents.value.map(s =>
		`${s.realName},${s.className || ''},${s.username || ''},${s.phone || ''}`
	).join('\n')
	const csv = BOM + headers + rows
	uni.setClipboardData({
		data: csv,
		success: () => {
			uni.showToast({ title: `已导出 ${filteredStudents.value.length} 条，粘贴到 Excel 即可`, icon: 'success', duration: 2500 })
		}
	})
}

const goToResume = (stu) => {
	uni.navigateTo({ url: '/pages/teacher/student-resume?studentId=' + stu.id + '&name=' + encodeURIComponent(stu.realName) })
}
</script>

<style scoped>
.header-simple {
	background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
	color: white;
	flex-shrink: 0;
}
.export-btn {
	font-size:13px;
	color:rgba(255,255,255,0.9);
	padding:6px 12px;
	border-radius:16px;
	background:rgba(255,255,255,0.2);
	font-weight:500;
}
.export-btn:active {
	background:rgba(255,255,255,0.35);
}
.filter-tab {
	padding: 6px 14px;
	border-radius: 20px;
	font-size: 12px;
	font-weight: 500;
	color: rgba(255,255,255,0.8);
	background: rgba(255,255,255,0.15);
	white-space: nowrap;
}
.filter-tab.active {
	background: white;
	color: #10B981;
}
.resume-list {
	padding: 16px;
}
.resume-card {
	background: white;
	border-radius: 16px;
	padding: 16px;
	margin-bottom: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
	position: relative;
	overflow: hidden;
}
.resume-card::before {
	content: '';
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 3px;
	background: linear-gradient(90deg, #10B981, transparent);
}
.resume-top {
	flex-direction: row;
	align-items: center;
	gap: 12px;
	margin-bottom: 12px;
}
.resume-avatar {
	width: 44px;
	height: 44px;
	border-radius: 50%;
	background: linear-gradient(135deg, #10B981, #34D399);
	align-items: center;
	justify-content: center;
	font-size: 18px;
	color: white;
	font-weight: 700;
	flex-shrink: 0;
}
.resume-info { flex: 1; }
.resume-name {
	font-size: 15px;
	font-weight: 700;
	color: #1D2129;
	display: block;
	margin-bottom: 2px;
}
.resume-class {
	font-size: 12px;
	color: #86909C;
	display: block;
}
.resume-status-tag {
	margin-left: 8px;
}
.resume-meta {
	flex-direction: row;
	gap: 18px;
	font-size: 12px;
	color: #86909C;
}
.empty-state {
	padding: 60px 20px;
	align-items: center;
	justify-content: center;
}
.empty-text {
	font-size: 14px;
	color: #86909C;
}
</style>
