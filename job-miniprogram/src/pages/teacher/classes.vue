<template>
	<view class="page-wrapper">
		<NavBar title="班级管理" :showBack="false" />
		<!-- 搜索栏 -->
		<view class="search-box">
			<uni-icons type="search" size="16" color="#86909C" />
			<input v-model="keyword" placeholder="搜索班级名称..." />
			<text class="all-resume-link" @click="goToResumes">全量简历 ›</text>
		</view>

		<scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<LoadingState type="skeleton" :rows="4" v-if="loading" />
			<view v-if="!loading" class="class-list">
				<view v-for="(cls, i) in filteredClasses" :key="i" class="class-card" @click="goToStudents(cls.id, cls.name)">
					<view class="class-card-top">
						<view class="class-icon">
							<uni-icons type="staff" size="28" color="#165DFF" />
						</view>
						<view class="class-info">
							<text class="class-name">{{ cls.name }}</text>
							<text class="class-major">{{ cls.major || '未设置专业' }} · {{ cls.grade || '' }}级</text>
						</view>
						<view class="more-btn" @click.stop="showMoreActions(cls)">
							<uni-icons type="more" size="16" color="#86909C" />
						</view>
					</view>
					<view class="stat-row">
						<view class="stat-item">
							<text class="stat-num">{{ cls.studentCount || 0 }}</text>
							<text class="stat-label">学生数</text>
						</view>
						<view class="stat-item">
							<text class="stat-num">{{ cls.employmentRate ?? 0 }}%</text>
							<text class="stat-label">就业率</text>
						</view>
						<view class="stat-item">
							<text class="stat-num">{{ cls.deliveryCount || 0 }}</text>
							<text class="stat-label">投递数</text>
						</view>
					</view>
				</view>
				<EmptyState v-if="!filteredClasses.length" icon="staff" title="暂无班级" desc="点击右下角+号创建班级" />
			</view>
			<view style="height: calc(60px + env(safe-area-inset-bottom))" />
		</scroll-view>

		<!-- 底部浮动添加按钮 -->
		<view class="fab-btn" @click="showAddModal = true">
			<text class="fab-icon">+</text>
		</view>

		<!-- 添加班级弹出层 -->
		<PopupDrawer :show="showAddModal" @update:show="showAddModal = $event" title="添加班级">
			<view class="add-form">
				<input class="form-input" v-model="newClassName" placeholder="班级名称" />
				<input class="form-input" v-model="newClassMajor" placeholder="专业" />
				<input class="form-input" v-model="newClassGrade" placeholder="年级" />
				<button class="submit-btn" @click="handleAddClass">确认添加</button>
			</view>
		</PopupDrawer>

		<TabBar current="classes" path-prefix="/pages/teacher/" :tab-list="teacherTabs" />
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@/utils/page-lifecycle'
import { teacherAPI } from '@/utils/request'
import NavBar from '@/components/NavBar.vue'
import TabBar from '@/components/TabBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import PopupDrawer from '@/components/PopupDrawer.vue'
import LoadingState from '@/components/LoadingState.vue'
import { checkRole } from '@/utils/auth'

checkRole(1)

const loading = ref(true)

const teacherTabs = [
	{ page: 'home', icon: 'home', activeIcon: 'home-filled', label: '首页' },
	{ page: 'classes', icon: 'staff', activeIcon: 'staff-filled', label: '班级' },
	{ page: 'jobs', icon: 'list', activeIcon: 'list', label: '岗位' },
	{ page: 'profile', icon: 'person', activeIcon: 'person-filled', label: '我的' }
]

const keyword = ref('')
const classes = ref([])
const showAddModal = ref(false)
const newClassName = ref('')
const newClassMajor = ref('')
const newClassGrade = ref('')
const refreshing = ref(false)

const filteredClasses = computed(() => {
	if (!keyword.value.trim()) return classes.value
	const kw = keyword.value.toLowerCase()
	return classes.value.filter(c => (c.name || '').toLowerCase().includes(kw))
})

onShow(() => { loading.value = true; loadClasses() })

const onRefresh = async () => {
	refreshing.value = true
	await loadClasses()
	refreshing.value = false
}

const loadClasses = async () => {
	try {
		const res = await teacherAPI.getClasses()
		classes.value = res.data || []
	} catch (e) { console.log('加载班级失败', e) }
	finally { loading.value = false }
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
	} catch (e) { uni.showToast({ title: '添加失败', icon: 'none' }) }
}

const showMoreActions = (cls) => {
	uni.showActionSheet({
		itemList: ['编辑班级', '删除班级'],
		success: (res) => {
			if (res.tapIndex === 0) {
				uni.showToast({ title: '编辑班级：' + cls.name, icon: 'none' })
			} else if (res.tapIndex === 1) {
				uni.showModal({
					title: '提示',
					content: '确定删除班级「' + cls.name + '」吗？',
					success: (r) => {
						if (r.confirm) {
							teacherAPI.deleteClass(cls.id).then(() => {
								uni.showToast({ title: '删除成功', icon: 'success' })
								loadClasses()
							}).catch(() => {
								uni.showToast({ title: '删除失败', icon: 'none' })
							})
						}
					}
				})
			}
		}
	})
}

const goToStudents = (classId, className) => {
	uni.navigateTo({ url: '/pages/teacher/students?classId=' + classId + '&className=' + encodeURIComponent(className) })
}
const goToResumes = () => uni.navigateTo({ url: '/pages/teacher/resumes' })
</script>

<style scoped lang="scss">
/* 搜索框和学生端完全一致 */
.search-box {
	flex-direction: row;
	align-items: center;
	gap: 8px;
	background: $uni-bg-color;
	border-radius: 24px;
	padding: 0 16px;
	height: 40px;
	margin: 12px 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.search-box input { flex: 1; font-size: 14px; background: transparent; border: none; color: $uni-text-color-title; height: 100%; }
.all-resume-link { font-size: 13px; color: $uni-color-primary; font-weight: 500; flex-shrink: 0; }

.class-list { padding: 0 16px; }
.class-card {
	background: white;
	border-radius: 12px;
	padding: 16px;
	margin-bottom: 12px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.04);
	position: relative;
}
.class-card-top {
	flex-direction: row;
	align-items: center;
	gap: 12px;
	margin-bottom: 16px;
}
.class-icon {
	width: 48px;
	height: 48px;
	background: $uni-color-primary-light;
	border-radius: 12px;
	align-items: center;
	justify-content: center;
}
.class-info { flex: 1; }
.class-name { font-size: 16px; font-weight: 700; color: $uni-text-color-title; display: block; margin-bottom: 4px; }
.class-major { font-size: 13px; color: $uni-text-color-secondary; display: block; }
.more-btn {
	width: 32px;
	height: 32px;
	align-items: center;
	justify-content: center;
}
.stat-row {
	flex-direction: row;
	justify-content: space-around;
	padding-top: 12px;
	border-top: 0.5px solid $uni-border-color-divider;
}
.stat-item { align-items: center; gap: 4px; }
.stat-num { font-size: 20px; font-weight: 800; color: $uni-text-color-title; }
.stat-label { font-size: 12px; color: $uni-text-color-secondary; }

.add-form { padding: 0; gap: 12px; }
.form-input {
	width: 100%;
	height: 44px;
	border: 1px solid $uni-border-color;
	border-radius: 8px;
	padding: 0 12px;
	font-size: 15px;
	background: $uni-bg-color-page;
	box-sizing: border-box;
	color: $uni-text-color-title;
}
.form-input:focus {
	border-color: $uni-color-primary;
	background: $uni-bg-color;
}
.submit-btn {
	width: 100%;
	height: 44px;
	border-radius: 12px;
	background: $uni-color-primary;
	color: white;
	font-size: 15px;
	font-weight: 600;
	border: none;
	margin-top: 8px;
}

/* ===== 浮动添加按钮 ===== */
.fab-btn {
	position: fixed;
	right: 24px;
	bottom: 90px;
	width: 56px;
	height: 56px;
	border-radius: 50%;
	background: $uni-gradient-primary;
	align-items: center;
	justify-content: center;
	box-shadow: 0 4px 16px $uni-color-primary-light;
	z-index: 100;
}
.fab-icon {
	font-size: 32px;
	color: white;
	font-weight: 300;
	margin-top: -2px;
}
</style>
