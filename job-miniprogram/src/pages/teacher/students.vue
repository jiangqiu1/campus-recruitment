<template>
	<view class="page-wrapper">
		<NavBar :title="className" show-back />
		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<LoadingState type="skeleton" :rows="4" v-if="loading" />
			<view v-if="!loading">
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
						<view class="tags-row">
							<text class="tag">完成度 {{ stu.resumeComplete || '—' }}%</text>
							<text class="tag">最后活跃: {{ stu.lastActive || '—' }}</text>
						</view>
					</view>
					<uni-icons type="arrowright" size="16" color="#C9CDD4" />
				</view>
				<EmptyState v-if="!students.length" icon="person" title="暂无学生" desc="该班级暂无学生数据" />
			</view>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import LoadingState from '@/components/LoadingState.vue'
import { ref, onMounted } from 'vue'
import { teacherAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import { checkRole } from '@/utils/auth'

checkRole(1)

const refreshing = ref(false)
const className = ref('')
const classId = ref('')
const students = ref([])

const onRefresh = async () => {
	refreshing.value = true
	await loadStudents()
	refreshing.value = false
}

const mapStudent = (stu) => ({
	id: stu.id,
	realName: stu.realName || '未知',
	username: stu.username || '',
	phone: stu.phone || '',
	resumeComplete: stu.resumeComplete || 0,
	deliveryCount: stu.deliveryCount || 0,
	lastActive: stu.lastActive || ''
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
		students.value = (res.data || []).map(mapStudent)
	} catch (e) {
		console.error('加载学生列表失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
	finally { loading.value = false }
}

const goToResume = (studentId, realName) => {
	uni.navigateTo({ url: '/pages/teacher/student-resume?studentId=' + studentId + '&name=' + encodeURIComponent(realName) })
}
</script>

<style scoped>
.student-list { padding: 16px; }
.student-card {
	flex-direction: row;
	align-items: center;
	background: white;
	border-radius: 12px;
	padding: 16px;
	margin-bottom: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.student-avatar {
	width: 48px;
	height: 48px;
	border-radius: 50%;
	background: linear-gradient(135deg, #165DFF, #2563EB);
	align-items: center;
	justify-content: center;
	font-size: 20px;
	color: white;
	font-weight: 700;
	margin-right: 12px;
	flex-shrink: 0;
}
.student-info { flex: 1; }
.student-name { font-size: 16px; font-weight: 700; color: #1D2129; display: block; margin-bottom: 2px; }
.student-id { font-size: 13px; color: #86909C; display: block; margin-bottom: 6px; }
.student-tags { flex-direction: row; gap: 8px; }
.tag-green { padding: 3px 8px; border-radius: 6px; font-size: 12px; font-weight: 600; background: rgba(22,93,255,0.08); color: #165DFF; }
.tag-blue { padding: 3px 8px; border-radius: 6px; font-size: 12px; font-weight: 600; background: rgba(22,93,255,0.1); color: #165DFF; }
.student-card:active { background: #F7F8FA; }
.tags-row { flex-direction: row; gap: 8px; margin-top: 6px; }
.tag { padding: 2px 8px; border-radius: 6px; font-size: 11px; font-weight: 500; background: rgba(22,93,255,0.08); color: #165DFF; }
</style>
