<template>
	<view class="page-wrapper">
		<view class="header-simple" style="padding:12px 16px;flex-direction:row;align-items:center;gap:12px;">
			<text style="font-size:18px;font-weight:700;color:white;flex:1;">班级管理</text>
			<text style="font-size:26px;" @click="showAddModal = true">＋</text>
		</view>
		<scroll-view class="content-scrollable" scroll-y>
			<view class="class-list">
				<view v-for="(cls, i) in classes" :key="i" class="class-card" @click="goToStudents(cls.id, cls.name)">
					<view class="class-card-top">
						<text class="class-icon">👥</text>
						<view class="class-info">
							<text class="class-name">{{ cls.name }}</text>
							<text class="class-major">{{ cls.major || '未设置专业' }}</text>
						</view>
						<text class="class-arrow">›</text>
					</view>
					<view class="class-stats">
						<view class="class-stat-item">
							<text class="class-stat-num">{{ cls.studentCount || 0 }}</text>
							<text class="class-stat-label">学生</text>
						</view>
						<view class="class-stat-item">
							<text class="class-stat-num" style="color:#10B981;">{{ cls.employmentRate ?? 0 }}</text>
							<text class="class-stat-label">就业率</text>
						</view>
						<view class="class-stat-item">
							<text class="class-stat-num" style="color:#165DFF;">{{ cls.deliveryCount || 0 }}</text>
							<text class="class-stat-label">投递</text>
						</view>
					</view>
					<!-- 删除按钮 -->
					<view class="delete-btn" @click.stop="handleDeleteClass(cls)">
						<text style="font-size:18px;color:#EF4444;">🗑</text>
					</view>
				</view>
				<view v-if="!classes.length" class="empty-state">
					<text style="font-size:48px;margin-bottom:12px;">👥</text>
					<text class="empty-text">暂无班级数据</text>
				</view>
			</view>
		</scroll-view>

		<!-- 添加班级弹窗 -->
		<view v-if="showAddModal" class="modal-overlay" @click="showAddModal = false">
			<view class="modal-content" @click.stop>
				<text style="font-size:18px;font-weight:700;margin-bottom:16px;">添加班级</text>
				<input class="modal-input" v-model="newClassName" placeholder="班级名称" />
				<input class="modal-input" v-model="newClassMajor" placeholder="专业名称" />
				<input class="modal-input" v-model="newClassGrade" placeholder="年级（如2023级）" />
				<view class="modal-actions">
					<text class="modal-btn modal-btn-cancel" @click="showAddModal = false">取消</text>
					<text class="modal-btn modal-btn-confirm" @click="handleAddClass">确定</text>
				</view>
			</view>
		</view>

		<TeacherTabBar current="classes" />
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { teacherAPI } from '@/utils/request'
import TeacherTabBar from '@/components/TeacherTabBar.vue'

const classes = ref([])
const showAddModal = ref(false)
const newClassName = ref('')
const newClassMajor = ref('')
const newClassGrade = ref('')

onMounted(async () => {
	await loadClasses()
})

const loadClasses = async () => {
	try {
		const res = await teacherAPI.getClasses()
		classes.value = res.data || []
	} catch (e) {
		console.log('加载班级失败', e)
	}
}

const handleAddClass = async () => {
	if (!newClassName.value.trim()) {
		uni.showToast({ title: '请输入班级名称', icon: 'none' })
		return
	}
	try {
		const userInfo = uni.getStorageSync('userInfo') || {}
		const data = {
			name: newClassName.value.trim(),
			major: newClassMajor.value.trim(),
			grade: newClassGrade.value.trim(),
			teacherId: userInfo.userId || userInfo.id || ''
		}
		await teacherAPI.createClass(data)
		uni.showToast({ title: '添加成功', icon: 'success' })
		showAddModal.value = false
		newClassName.value = ''
		newClassMajor.value = ''
		newClassGrade.value = ''
		await loadClasses()
	} catch (e) {
		uni.showToast({ title: '添加失败', icon: 'none' })
	}
}

const handleDeleteClass = (cls) => {
	uni.showModal({
		title: '确认删除',
		content: '确定删除班级「' + cls.name + '」吗？',
		success: async (res) => {
			if (res.confirm) {
				try {
					await teacherAPI.deleteClass(cls.id)
					uni.showToast({ title: '删除成功', icon: 'success' })
					await loadClasses()
				} catch (e) {
					uni.showToast({ title: '删除失败', icon: 'none' })
				}
			}
		}
	})
}

const goToStudents = (classId, className) => {
	uni.navigateTo({ url: '/pages/teacher/students?classId=' + classId + '&className=' + encodeURIComponent(className) })
}
</script>

<style scoped>
.header-simple {
	background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
	color: white;
	flex-shrink: 0;
}
.class-list {
	padding: 16px;
}
.class-card {
	background: white;
	border-radius: 16px;
	padding: 16px;
	margin-bottom: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
	position: relative;
	overflow: hidden;
}
.class-card::before {
	content: '';
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 3px;
	background: linear-gradient(90deg, #10B981, transparent);
}
.class-card-top {
	flex-direction: row;
	align-items: center;
	gap: 12px;
	margin-bottom: 16px;
}
.class-icon {
	font-size: 32px;
	width: 48px;
	height: 48px;
	background: rgba(16,185,129,0.1);
	border-radius: 12px;
	align-items: center;
	justify-content: center;
	text-align: center;
	line-height: 48px;
}
.class-info { flex: 1; }
.class-name {
	font-size: 16px;
	font-weight: 700;
	color: #1D2129;
	display: block;
	margin-bottom: 4px;
}
.class-major {
	font-size: 13px;
	color: #86909C;
	display: block;
}
.class-arrow { color: #C9CDD4; font-size: 20px; }
.class-stats {
	flex-direction: row;
	justify-content: space-around;
	padding-top: 12px;
	border-top: 1px solid #F2F3F5;
}
.class-stat-item { align-items: center; gap: 4px; }
.class-stat-num {
	font-size: 20px;
	font-weight: 800;
	color: #1D2129;
}
.class-stat-label {
	font-size: 12px;
	color: #86909C;
}
.delete-btn {
	position: absolute;
	bottom: 12px;
	right: 12px;
	width: 36px;
	height: 36px;
	align-items: center;
	justify-content: center;
}

/* Modal */
.modal-overlay {
	position: fixed;
	top: 0; left: 0; right: 0; bottom: 0;
	background: rgba(0,0,0,0.5);
	align-items: center;
	justify-content: center;
	z-index: 1000;
}
.modal-content {
	background: white;
	border-radius: 20px;
	padding: 28px;
	width: 80%;
	max-width: 340px;
	gap: 12px;
}
.modal-input {
	border: 1px solid #E2E8F0;
	border-radius: 10px;
	padding: 12px 14px;
	font-size: 14px;
	color: #1D2129;
	background: #F7F8FA;
}
.modal-actions {
	flex-direction: row;
	gap: 12px;
	margin-top: 8px;
}
.modal-btn {
	flex: 1;
	padding: 12px;
	border-radius: 10px;
	text-align: center;
	font-size: 15px;
	font-weight: 600;
}
.modal-btn-cancel {
	background: #F2F3F5;
	color: #4E5969;
}
.modal-btn-confirm {
	background: #165DFF;
	color: white;
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
