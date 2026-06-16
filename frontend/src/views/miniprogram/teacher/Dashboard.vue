<template>
  <div class="dashboard">
    <h2>教师数据大屏</h2>
    
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <el-icon><User /></el-icon>
              <span>班级学生</span>
            </div>
          </template>
          <div class="stat-value">{{ stats.studentCount }}</div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <el-icon><Document /></el-icon>
              <span>发布岗位</span>
            </div>
          </template>
          <div class="stat-value">{{ stats.jobCount }}</div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <el-icon><Briefcase /></el-icon>
              <span>投递次数</span>
            </div>
          </template>
          <div class="stat-value">{{ stats.deliveryCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>简历投递趋势（近7日）</span>
          </template>
          <div ref="deliveryTrendChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>学生就业分布</span>
          </template>
          <div ref="employmentChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { User, Document, Briefcase } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { statisticsAPI } from '@/api'
import { getToken } from '@/utils/auth'

const stats = ref({
  studentCount: 0,
  jobCount: 0,
  deliveryCount: 0
})

const deliveryTrendChart = ref(null)
const employmentChart = ref(null)

onMounted(async () => {
  await Promise.all([loadStats(), loadCharts()])
})

const loadStats = async () => {
  try {
    const res = await statisticsAPI.getTeacherDashboard()
    if (res.code === 200) stats.value = res.data
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

const loadCharts = async () => {
  try {
    const [trendRes, employRes] = await Promise.all([
      statisticsAPI.getDeliveryTrend(),
      statisticsAPI.getEmploymentDistribution()
    ])
    if (trendRes.code === 200) initDeliveryTrendChart(trendRes.data)
    if (employRes.code === 200) initEmploymentChart(employRes.data)
  } catch (error) {
    console.error('加载图表数据失败', error)
  }
}

const initDeliveryTrendChart = (data) => {
  const dom = deliveryTrendChart.value
  if (!dom || !data || !data.length) return
  setTimeout(() => {
    const chart = echarts.init(dom)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: data.map(d => d.label) },
      yAxis: { type: 'value', minInterval: 1 },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      series: [{
        data: data.map(d => d.value),
        type: 'bar',
        barMaxWidth: 40,
        itemStyle: { color: '#409EFF', borderRadius: [4, 4, 0, 0] }
      }]
    })
  }, 50)
}

const initEmploymentChart = (data) => {
  const dom = employmentChart.value
  if (!dom || !data || !data.length) return
  setTimeout(() => {
    const chart = echarts.init(dom)
    chart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        data: data,
        label: { formatter: '{b}\n{d}%' },
        emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } }
      }]
    })
  }, 50)
}
</script>

<style scoped>
.dashboard { padding: 20px; }
.stats-row { margin-bottom: 20px; }
.stat-card { text-align: center; }
.card-header { display: flex; align-items: center; gap: 8px; }
.stat-value { font-size: 36px; font-weight: bold; color: #409EFF; }
.charts-row { margin-top: 20px; }
</style>
