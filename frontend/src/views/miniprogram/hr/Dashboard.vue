<template>
  <div class="dashboard">
    <h2>企业HR数据大屏</h2>
    
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <el-icon><Briefcase /></el-icon>
              <span>岗位数量</span>
            </div>
          </template>
          <div class="stat-value">{{ stats.jobCount }}</div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <el-icon><Document /></el-icon>
              <span>收到简历</span>
            </div>
          </template>
          <div class="stat-value">{{ stats.resumeCount }}</div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <el-icon><User /></el-icon>
              <span>面试邀请</span>
            </div>
          </template>
          <div class="stat-value">{{ stats.interviewCount }}</div>
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
            <span>简历评分分布</span>
          </template>
          <div ref="scoreDistributionChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Briefcase, Document, User } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { statisticsAPI } from '@/api'

const stats = ref({
  jobCount: 0,
  resumeCount: 0,
  interviewCount: 0
})

const deliveryTrendChart = ref(null)
const scoreDistributionChart = ref(null)

onMounted(async () => {
  await Promise.all([loadStats(), loadCharts()])
})

const loadStats = async () => {
  try {
    const res = await statisticsAPI.getHrDashboard()
    if (res.code === 200) stats.value = res.data
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

const loadCharts = async () => {
  try {
    const [trendRes, scoreRes] = await Promise.all([
      statisticsAPI.getDeliveryTrend(),
      statisticsAPI.getScoreDistribution()
    ])
    if (trendRes.code === 200) initDeliveryTrendChart(trendRes.data)
    if (scoreRes.code === 200) initScoreDistributionChart(scoreRes.data)
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
        type: 'line',
        smooth: true,
        lineStyle: { width: 3, color: '#409EFF' },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(64,158,255,0.3)' }, { offset: 1, color: 'rgba(64,158,255,0.05)' }] } },
        itemStyle: { color: '#409EFF' }
      }]
    })
  }, 50)
}

const initScoreDistributionChart = (data) => {
  const dom = scoreDistributionChart.value
  if (!dom || !data || !data.length) return
  setTimeout(() => {
    const chart = echarts.init(dom)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: data.map(d => d.name) },
      yAxis: { type: 'value', minInterval: 1 },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      series: [{
        data: data.map(d => d.value),
        type: 'bar',
        barMaxWidth: 40,
        itemStyle: {
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [
            { offset: 0, color: '#F56C6C' },
            { offset: 0.5, color: '#E6A23C' },
            { offset: 1, color: '#67C23A' }
          ]},
          borderRadius: [4, 4, 0, 0]
        }
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
