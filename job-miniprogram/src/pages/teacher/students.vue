<template>
	<view class="page-wrapper">
		<view class="header-simple" style="padding:12px 16px;flex-direction:row;align-items:center;gap:12px;">
			<text style="font-size:20px;" @click="goBack">‹</text>
			<text style="font-size:18px;font-weight:700;color:white;flex:1;">{{ className }}</text>
			<text class="export-btn" @click="exportCSV">📥 导出</text>
		</view>
		<scroll-view class="content-scrollable" scroll-y refresher-enabled="true" :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<view class="student-list">
				<view v-for="(stu, i) in students" :key="i" class="student-card" @click="goToResume(stu.id, stu.realName)">
					<view class="student-avatar">
						<text>{{ (stu.realName || '学').charAt(0) }}</text>
					</view>
					<view class="student-info">
						<text class="student-name">{{ stu.realName || '未知' }}</text>
						<text class="student-id">学号: {{ stu.username || '' }}</text>
						<view class="student-tags">
							<text class="tag-green" v-if="stu.resumeComplete">简历: {{ stu.resumeComplete }}%</text>
							<text class="tag-blue">投递: {{ stu.deliveryCount || 0 }}</text>
						</view>
					</view>
					<text class="student-arrow">›</text>
				</view>
				<view v-if="!students.length" class="empty-state">
					<text style="font-size:48px;margin-bottom:12px;">👨‍🎓</text>
					<text class="empty-text">暂无学生数据</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { teacherAPI } from '@/utils/request'

const refreshing = ref(false)
const className = ref('')
const classId = ref('')
const students = ref([])

const onRefresh = async () => {
	refreshing.value = true
	await loadStudents()
	refreshing.value = false
}

// 后端SysUser → 前排展示映射
const mapStudent = (stu) => ({
	id: stu.id,
	realName: stu.realName || '未知',
	username: stu.username || '',
	phone: stu.phone || '',
	resumeComplete: stu.resumeComplete || 0,
	deliveryCount: stu.deliveryCount || 0
})

onMounted(() => {
	const pages = getCurrentPages()
	const currentPage = pages[pages.length - 1]
	if (currentPage.options) {
		classId.value = currentPage.options.classId || ''
		className.value = decodeURIComponent(currentPage.options.className || '班级')
	}
	loadStudents()
})

const loadStudents = async () => {
	try {
		const res = await teacherAPI.getStudentsByClass(classId.value)
		const rawList = res.data || []
		students.value = rawList.map(mapStudent)
	} catch (e) {
		console.error('加载学生列表失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

const goToResume = (studentId, realName) => {
	uni.navigateTo({ url: '/pages/teacher/student-resume?studentId=' + studentId + '&name=' + encodeURIComponent(realName) })
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
	const headers = '姓名,学号,手机号,简历完整度,投递数\n'
	const rows = students.value.map(s =>
		`${s.realName},${s.username || ''},${s.phone || ''},${s.resumeComplete || 0},${s.deliveryCount || 0}`
	).join('\n')
	const csv = BOM + headers + rows
	uni.setClipboardData({
		data: csv,
		success: () => {
			uni.showToast({ title: `已导出 ${students.value.length} 条，粘贴到 Excel 即可`, icon: 'success', duration: 2500 })
		}
	})
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
.student-list {
	padding: 16px;
}
.student-card {
	flex-direction: row;
	align-items: center;
	background: white;
	border-radius: 16px;
	padding: 16px;
	margin-bottom: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
	position: relative;
	overflow: hidden;
}
.student-card::before {
	content: '';
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 3px;
	background: linear-gradient(90deg, #10B981, transparent);
}
.student-avatar {
	width: 48px;
	height: 48px;
	border-radius: 50%;
	background: linear-gradient(135deg, #10B981, #34D399);
	align-items: center;
	justify-content: center;
	font-size: 20px;
	color: white;
	font-weight: 700;
	margin-right: 12px;
	flex-shrink: 0;
}
.student-info { flex: 1; }
.student-name {
	font-size: 16px;
	font-weight: 700;
	color: #1D2129;
	display: block;
	margin-bottom: 2px;
}
.student-id {
	font-size: 13px;
	color: #86909C;
	display: block;
	margin-bottom: 6px;
}
.student-tags {
	flex-direction: row;
	gap: 8px;
}
.tag-green {
	padding: 3px 8px;
	border-radius: 6px;
	font-size: 12px;
	font-weight: 600;
	background: rgba(16,185,129,0.1);
	color: #10B981;
}
.tag-blue {
	padding: 3px 8px;
	border-radius: 6px;
	font-size: 12px;
	font-weight: 600;
	background: rgba(22,93,255,0.1);
	color: #165DFF;
}
.student-arrow {
	color: #C9CDD4;
	font-size: 20px;
	margin-left: 8px;
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
