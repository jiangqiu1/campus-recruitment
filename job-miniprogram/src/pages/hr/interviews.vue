<template>
  <view class="page-wrapper">
    <NavBar title="面试日程" :showBack="false" />
    <scroll-view class="content-scrollable" scroll-y refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
			<LoadingState type="skeleton" :rows="4" v-if="loading" />
			<view v-if="!loading">
      <!-- 统计栏 -->
      <view class="stats-bar">
        <view class="stats-item">
          <text class="stats-num">{{ todayCount }}</text>
          <text class="stats-label">今日面试</text>
        </view>
        <view class="stats-item">
          <text class="stats-num">{{ upcomingCount }}</text>
          <text class="stats-label">待面试</text>
        </view>
        <view class="stats-item">
          <text class="stats-num">{{ completedCount }}</text>
          <text class="stats-label">已完成</text>
        </view>
        <view class="stats-item">
          <text class="stats-num">{{ allInterviews.length }}</text>
          <text class="stats-label">总计</text>
        </view>
      </view>

      <!-- 日期快速切换 -->
      <scroll-view class="date-scroll" scroll-x show-scrollbar="false">
        <view class="date-scroll-inner">
          <text v-for="(d, i) in dateTabs" :key="i" class="date-tab" :class="{ active: selectedDate === d.value }" @click="selectedDate = d.value">
            <text class="date-tab-week">{{ d.week }}</text>
            <text class="date-tab-day">{{ d.day }}</text>
          </text>
        </view>
      </scroll-view>

      <view class="interview-list">
        <template v-if="filteredInterviews.length">
          <view v-for="item in filteredInterviews" :key="item.id" class="interview-card" @click="goToDetail(item.id)" @longpress="showActions(item)">
            <view class="card-left">
              <text class="card-date">{{ formatDate(item.interviewTime) }}</text>
              <text class="card-time">{{ formatTime(item.interviewTime) }}</text>
            </view>
            <view class="card-body">
              <view class="card-top">
                <text class="card-name">{{ item.studentName || '候选人' }}</text>
                <text class="status-tag" :class="'tag-' + item.status">{{ item.statusText }}</text>
              </view>
              <text class="card-job">{{ item.jobTitle || '岗位名称' }}</text>
              <text class="card-location">{{ item.interviewLocation || '线上面试' }}</text>
              <!-- 已完成面试显示反馈 -->
              <text v-if="item.feedback && (item.status === 'accepted' || item.status === 'rejected')" class="card-feedback">{{ item.feedback }}</text>
            </view>
            <view class="card-actions" v-if="item.status === 'interview'">
              <view class="action-btn action-btn--accept" @click.stop="confirmStatus(item, 3, '录用')">录用</view>
              <view class="action-btn action-btn--reject" @click.stop="confirmStatus(item, 4, '不合适')">不通过</view>
            </view>
            <uni-icons type="arrowright" size="16" color="#C9CDD4" v-else />
          </view>
        </template>
        <EmptyState v-else icon="calendar" title="暂无面试安排" desc="安排面试后会显示在这里" />
      </view>
</view>
      <view style="height: calc(60px + env(safe-area-inset-bottom))" />
    </scroll-view>
    <HrTabBar current="interviews" />
  </view>
</template>

<script setup>
import LoadingState from '@/components/LoadingState.vue'
import { ref, computed, onMounted } from 'vue'
import { hrAPI } from '@/utils/request'
import HrTabBar from '@/components/HrTabBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import NavBar from '@/components/NavBar.vue'

const allInterviews = ref([])
const selectedDate = ref('all')
const loading = ref(true)

const DELIVERY_STATUS = ['pending', 'viewed', 'interview', 'accepted', 'rejected']
const DELIVERY_STATUS_TEXT = ['待查看', '已查看', '面试中', '已录用', '未通过']
const WEEK_NAMES = ['日', '一', '二', '三', '四', '五', '六']

const dateTabs = computed(() => {
  const tabs = [{ label: '全部', week: '全部', day: '', value: 'all' }]
  const today = new Date()
  for (let i = 0; i < 7; i++) {
    const d = new Date(today)
    d.setDate(today.getDate() + i)
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    const week = WEEK_NAMES[d.getDay()]
    tabs.push({
      label: mm + '-' + dd,
      week: i === 0 ? '今天' : week,
      day: mm + '/' + dd,
      value: d.getFullYear() + '-' + mm + '-' + dd
    })
  }
  return tabs
})

const filteredInterviews = computed(() => {
  if (selectedDate.value === 'all') return allInterviews.value
  return allInterviews.value.filter(item =>
    item.interviewTime && item.interviewTime.startsWith(selectedDate.value)
  )
})

// 统计
const todayStr = computed(() => {
  const d = new Date()
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
})
const todayCount = computed(() => allInterviews.value.filter(
  d => d.status === 'interview' && d.interviewTime && d.interviewTime.startsWith(todayStr.value)).length)
const upcomingCount = computed(() => allInterviews.value.filter(d => d.status === 'interview').length)
const completedCount = computed(() => allInterviews.value.filter(d => d.status === 'accepted' || d.status === 'rejected').length)

onMounted(() => { loadData() })

const refreshing = ref(false)
const onRefresh = async () => {
  refreshing.value = true
  await loadData()
  refreshing.value = false
}

const loadData = async () => {
  const cId = getCompanyId()
  if (!cId) return
  try {
    // 使用 by-company 一次查询所有投递，替代原本 N+1 的遍历方式
    const res = await hrAPI.getDeliveriesByCompany(cId)
    const data = res.data || []
    const all = data
      .filter(d => d.status >= 2) // 面试中(2)、已录用(3)、未通过(4)
      .map(d => ({
        id: d.id,
        studentId: d.studentId,
        studentName: d.studentName || '候选人',
        jobTitle: d.jobTitle || '',
        status: DELIVERY_STATUS[d.status] || 'pending',
        statusText: DELIVERY_STATUS_TEXT[d.status] || '',
        interviewTime: d.interviewTime ? d.interviewTime.replace('T', ' ') : '',
        interviewLocation: d.interviewLocation || '线上面试',
        feedback: d.feedback || ''
      }))
    all.sort((a, b) => {
      if (a.interviewTime < b.interviewTime) return -1
      if (a.interviewTime > b.interviewTime) return 1
      return 0
    })
    allInterviews.value = all
  } catch (e) {
    console.error('加载面试数据失败', e)
  }
  finally { loading.value = false }
}

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

const formatTime = (t) => {
	if (!t) return '待定'
	return t.length >= 16 ? t.substring(11, 16) : t
}

const formatDate = (t) => {
	if (!t) return '时间待定'
	if (t.length >= 10) return t.substring(5, 10)
	return t
}

// 长按操作菜单
const showActions = (item) => {
  const actions = ['查看详情']
  if (item.status === 'interview') {
    actions.push('修改面试', '标记已录用', '标记不合适')
  }
  uni.showActionSheet({
    itemList: actions,
    success: (res) => {
      const idx = res.tapIndex
      if (idx === 0) {
        goToDetail(item.id)
      } else if (item.status === 'interview') {
        if (idx === 1) {
          showModifyModal(item)
        } else if (idx === 2) {
          confirmStatus(item, 3, '录用')
        } else if (idx === 3) {
          confirmStatus(item, 4, '不合适')
        }
      }
    }
  })
}

const confirmStatus = (item, status, label) => {
  uni.showModal({
    title: '确认操作',
    content: '确定将「' + item.studentName + '」标记为' + label + '吗？',
    success: async (r) => {
      if (r.confirm) {
        try {
          await hrAPI.updateDeliveryStatus(item.id, { status })
          uni.showToast({ title: '已标记' + label, icon: 'success' })
          item.status = DELIVERY_STATUS[status]
          item.statusText = DELIVERY_STATUS_TEXT[status]
        } catch (e) {
          uni.showToast({ title: '操作失败', icon: 'none' })
        }
      }
    }
  })
}

const showModifyModal = (item) => {
  let timeText = item.interviewTime || ''
  if (timeText.length > 16) timeText = timeText.substring(0, 16)
  // 用弹窗让用户输入新的面试时间和地点
  uni.showModal({
    title: '修改面试',
    content: '修改功能已跳转到详情页',
    success: () => goToDetail(item.id)
  })
}

const goToDetail = (id) => uni.navigateTo({ url: '/pages/hr/delivery-detail?id=' + id })
</script>

<style scoped lang="scss">
/* 统计栏 */
.stats-bar {
  flex-direction: row;
  background: $uni-bg-color;
  padding: 16px 16px 12px;
  gap: 0;
}
.stats-item {
  flex: 1;
  align-items: center;
  gap: 4px;
}
.stats-num {
  font-size: 22px;
  font-weight: 800;
  color: $uni-text-color-title;
}
.stats-label {
  font-size: 12px;
  color: $uni-text-color-secondary;
}

/* 日期滚动条 */
.date-scroll { background: $uni-bg-color; border-bottom: 0.5px solid $uni-border-color-divider; }
.date-scroll-inner { flex-direction: row; padding: 10px 12px; gap: 8px; }
.date-tab {
  flex-direction: column;
  align-items: center;
  padding: 8px 14px;
  border-radius: 12px;
  background: $uni-bg-color-page;
  min-width: 52px;
}
.date-tab.active { background: $uni-color-primary; }
.date-tab.active .date-tab-week { color: $uni-text-color-inverse; }
.date-tab.active .date-tab-day { color: rgba(255,255,255,0.8); }
.date-tab-week { font-size: 12px; color: $uni-text-color; font-weight: 500; }
.date-tab-day { font-size: 12px; color: $uni-text-color-secondary; margin-top: 2px; }

/* 面试卡片 */
.interview-list { padding: 12px 16px; }
.interview-card {
  flex-direction: row;
  align-items: center;
  gap: 12px;
  background: $uni-bg-color;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 10px;
  box-shadow: $uni-shadow-card;
}
.interview-card:active { background: $uni-bg-color-page; }
.card-left {
  width: 60px;
  align-items: center;
  flex-direction: column;
}
.card-date { font-size: 12px; color: $uni-text-color-secondary; margin-bottom: 2px; }
.card-time { font-size: 15px; font-weight: 700; color: $uni-text-color-title; }
.card-body { flex: 1; gap: 3px; }
.card-top {
  flex-direction: row;
  align-items: center;
  gap: 8px;
}
.card-name { font-size: 15px; font-weight: 700; color: $uni-text-color-title; }
.card-job { font-size: 12px; color: $uni-text-color-secondary; margin-top: 2px; }
.card-location { font-size: 12px; color: $uni-text-color-placeholder; margin-top: 2px; }
.card-feedback { font-size: 12px; color: $uni-text-color; margin-top: 4px; background: $uni-bg-color-page; border-radius: 4px; padding: 4px 8px; }

.status-tag { font-size: 12px; padding: 2px 8px; border-radius: 8px; font-weight: 600; flex-shrink: 0; }
.tag-pending { background: rgba(245,158,11,0.1); color: $uni-color-warning; }
.tag-viewed { background: $uni-color-primary-light; color: $uni-color-primary; }
.tag-interview { background: $uni-color-primary-light; color: $uni-color-primary; }
.tag-accepted { background: $uni-color-success-light; color: $uni-color-success; }
.tag-rejected { background: rgba(239,68,68,0.1); color: $uni-color-error; }

/* 操作按钮 */
.card-actions { flex-direction: column; gap: 6px; flex-shrink: 0; }
.action-btn {
	padding: 4px 12px;
	border-radius: 8px;
	font-size: 12px;
	font-weight: 600;
	text-align: center;
	min-width: 48px;
}
.action-btn--accept { background: $uni-color-success-light; color: $uni-color-success; }
.action-btn--reject { background: rgba(239,68,68,0.1); color: $uni-color-error; }
.action-btn:active { opacity: 0.7; }
</style>
