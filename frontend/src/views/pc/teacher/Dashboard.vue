<template>
  <div class="teacher-dashboard fade-in">
    <div class="page-header">
      <h2>教师工作台</h2>
      <p>班级管理 · 岗位发布 · 投递跟踪</p>
    </div>

    <div class="stat-grid">
      <div class="stat-card" style="border-left-color: #165DFF;">
        <h3>管理班级</h3>
        <div class="num">{{ stats.classCount || 0 }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #10B981;">
        <h3>学生总数</h3>
        <div class="num">{{ stats.studentCount || 0 }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #F59E0B;">
        <h3>发布岗位</h3>
        <div class="num">{{ stats.jobCount || 0 }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #EF4444;">
        <h3>投递总数</h3>
        <div class="num">{{ stats.deliveryCount || 0 }}</div>
      </div>
    </div>

        <AIInsightCard />

    <div class="chart-grid">
      <div class="chart-box">
        <div class="chart-title">投递趋势（近7日）</div>
        <div ref="trendChartRef" style="height: 260px"></div>
        <div v-if="!hasTrendData" class="chart-empty">近7日暂无投递数据</div>
      </div>
      <div class="chart-box">
        <div class="chart-title">投递状态分布</div>
        <div ref="pieChartRef" style="height: 260px"></div>
      </div>
    </div>

    <div class="content-card">
      <div class="content-card-header">
        <span class="content-card-title">近期投递</span>
      </div>
      <el-table v-loading="loading" :data="deliveryData" style="width: 100%" stripe>
        <el-table-column label="动态" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">{{ row.text || row.studentName }}</template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDate(row.time || row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && deliveryData.length === 0" class="empty-state">
        <el-empty :image-size="100" description="暂无近期投递记录" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { statisticsAPI } from '@/api'
import { formatDate } from '@/utils/formatDate'
import echarts from '@/utils/echarts'
import AIInsightCard from '@/components/AIInsightCard.vue'

const stats = ref({ classCount: 0, studentCount: 0, jobCount: 0, deliveryCount: 0 })
const deliveryData = ref([])
const loading = ref(false)

const trendChartRef = ref(null)
const hasTrendData = ref(false)
const pieChartRef = ref(null)
let trendChart = null
let pieChart = null

onMounted(async () => {
  await Promise.all([loadStats(), loadTrendData()])
  nextTick(initCharts)
})

onBeforeUnmount(() => {
  trendChart?.dispose()
  pieChart?.dispose()
})

const loadStats = async () => {
  try {
    const res = await statisticsAPI.getTeacherDashboard()
    if (res.code === 200) {
      stats.value = res.data
      // 从 dashboard 接口获取近期投递（已含学生姓名和岗位名）
      if (res.data.recentActivities && Array.isArray(res.data.recentActivities)) {
        deliveryData.value = res.data.recentActivities.map(a => ({
          studentName: a.text || '未知',
          jobTitle: '',
          companyName: '',
          createTime: a.time || '',
          status: 0
        }))
      }
    }
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

// 加载全部数据（用于图表）
const loadTrendData = async () => {
  try {
    const [trendRes, distRes] = await Promise.all([
      statisticsAPI.getDeliveryTrend({}),
      statisticsAPI.getEmploymentDistribution()
    ])
    // 存储趋势数据（按日期的投递量）
    if (trendRes.code === 200 && Array.isArray(trendRes.data)) {
      window._trendData = trendRes.data
    }
    // 存储就业分布数据（用于饼图）
    if (distRes.code === 200 && Array.isArray(distRes.data)) {
      window._distData = distRes.data
    }
  } catch (e) { /* silent */ }
}

function initCharts() {
  // ----- 投递趋势折线图（全 0 时不出图，显示空态） -----
  const trendData = window._trendData || []
  const trendHasData = trendData.some(d => (d.value || 0) > 0)
  hasTrendData.value = trendHasData
  if (trendChartRef.value && trendHasData) {
    trendChart = echarts.init(trendChartRef.value)
    const days = trendData.map(d => typeof d.label === 'string' ? d.label : '')
    const counts = trendData.map(d => d.value || 0)
    // 如果后端没返回数据，用前端计算
    if (days.length === 0) {
      const now = new Date()
      for (let i = 6; i >= 0; i--) {
        const d = new Date(now)
        d.setDate(d.getDate() - i)
        days.push((d.getMonth() + 1) + '/' + d.getDate())
      }
    }
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 50, right: 20, top: 30, bottom: 30 },
      xAxis: { type: 'category', data: days, axisLine: { lineStyle: { color: '#E2E8F0' } } },
      yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#F2F3F5' } } },
      series: [{
        type: 'line',
        data: counts,
        smooth: true,
        lineStyle: { color: '#165DFF', width: 3 },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(22,93,255,0.3)' }, { offset: 1, color: 'rgba(22,93,255,0.02)' }]) },
        symbol: 'circle',
        symbolSize: 8
      }]
    })
  }

  // ----- 投递状态饼图 -----
  if (pieChartRef.value) {
    pieChart = echarts.init(pieChartRef.value)
    const distData = window._distData || []
    const colorMap = { '已录用': '#10B981', '面试中': '#409EFF', '待查看': '#F59E0B', '未录用': '#909399' }
    let pieData
    if (distData.length > 0) {
      // 使用后端就业分布数据（已按教师学生正确过滤）
      pieData = distData.map((d, i) => ({
        name: d.name || '未知',
        value: d.value || 0,
        itemStyle: { color: colorMap[d.name] || '#909399' }
      })).filter(d => d.value > 0)
    }
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        data: pieData.length ? pieData : [{ name: '暂无数据', value: 1, itemStyle: { color: '#E2E8F0' } }],
        label: { show: true, formatter: '{b}' },
        emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.2)' } }
      }]
    })
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

<style scoped>
.chart-empty { position: absolute; inset: 40px 0 0; display: flex; align-items: center; justify-content: center; color: #86909C; font-size: 14px; background: white; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 24px; }
.stat-card { background: white; padding: 24px; border-radius: 16px; border-left: 6px solid; box-shadow: 0 6px 16px rgba(0,0,0,0.06); }
.stat-card h3 { margin: 0 0 8px; font-size: 14px; color: #86909C; font-weight: 500; }
.stat-card .num { font-size: 32px; font-weight: 700; color: #1D2129; }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-bottom: 24px; }
.chart-box { background: white; border-radius: 16px; padding: 24px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); position: relative; }
.chart-title { font-size: 15px; font-weight: 600; color: #1D2129; margin-bottom: 12px; }
.content-card { background: #fff; border-radius: 16px; padding: 24px; margin-bottom: 24px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); }
.content-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.content-card-title { font-size: 16px; font-weight: 600; color: #1D2129; }
</style>
