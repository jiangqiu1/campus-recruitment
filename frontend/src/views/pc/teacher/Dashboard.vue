<template>
  <div class="teacher-dashboard fade-in">
    <div class="page-header">
      <h2>教师工作台</h2>
      <p>班级管理 · 岗位发布 · 投递跟踪</p>
    </div>

    <div class="stat-grid">
      <div class="stat-card" style="border-left-color: #165DFF;">
        <h3><span>👥</span> 管理班级</h3>
        <div class="num">{{ stats.classCount }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #10B981;">
        <h3><span>🎓</span> 学生总数</h3>
        <div class="num">{{ stats.studentCount }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #F59E0B;">
        <h3><span>💼</span> 发布岗位</h3>
        <div class="num">{{ stats.jobCount }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #EF4444;">
        <h3><span>📮</span> 投递总数</h3>
        <div class="num">{{ stats.deliveryCount }}</div>
      </div>
    </div>

    <div class="content-card">
      <div class="content-card-header">
        <span class="content-card-title">近期投递</span>
      </div>
      <el-table v-loading="loading" :data="deliveryData" style="width: 100%" stripe>
        <el-table-column prop="studentName" label="学生" width="120" />
        <el-table-column prop="jobTitle" label="岗位" min-width="160" />
        <el-table-column prop="companyName" label="企业" width="150" />
        <el-table-column prop="createTime" label="投递日期" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { statisticsAPI, deliveryAPI } from '@/api'

const stats = ref({ classCount: 0, studentCount: 0, jobCount: 0, deliveryCount: 0 })
const deliveryData = ref([])
const loading = ref(false)

onMounted(async () => {
  await Promise.all([loadStats(), loadDeliveries()])
})

const loadStats = async () => {
  try {
    const res = await statisticsAPI.getTeacherDashboard()
    if (res.code === 200) stats.value = res.data
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

const loadDeliveries = async () => {
  loading.value = true
  try {
    const res = await deliveryAPI.getDeliveries({ page: 1, size: 10 })
    if (res.code === 200 && res.data?.records) {
      deliveryData.value = res.data.records.map(d => ({
        ...d,
        studentName: d.studentName || ('学生 #' + d.studentId),
        jobTitle: d.jobTitle || ('岗位 #' + d.jobId),
        companyName: d.companyName || '',
        createTime: d.createTime ? d.createTime.replace('T', ' ') : '',
        status: d.status
      }))
    }
  } catch (error) {
    console.error('加载投递数据失败', error)
  } finally {
    loading.value = false
  }
}

const statusTag = (status) => {
  const map = { 0: 'info', 1: 'info', 2: 'primary', 3: 'warning', 4: 'success', 5: 'danger' }
  return map[status] || 'info'
}
const statusLabel = (status) => {
  const map = { 0: '待查看', 1: '待查看', 2: '已查看', 3: '面试中', 4: '已录用', 5: '未录用' }
  return map[status] || '未知'
}
</script>
