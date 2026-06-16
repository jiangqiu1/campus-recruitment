<template>
  <div class="hr-data-stats">
    <h2>数据统计</h2>
    
    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div class="stat-card" style="border-left-color: #165DFF;">
        <h3><i class="fa fa-briefcase"></i> 发布岗位</h3>
        <div class="num">{{ stats.jobCount || 0 }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #10B981;">
        <h3><i class="fa fa-paper-plane"></i> 总投递量</h3>
        <div class="num">{{ stats.totalDeliveries || stats.resumeCount || 0 }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #F59E0B;">
        <h3><i class="fa fa-calendar-check"></i> 待面试</h3>
        <div class="num">{{ stats.pending || stats.interviewCount || 0 }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #8B5CF6;">
        <h3><i class="fa fa-star"></i> 已录用</h3>
        <div class="num">{{ stats.hired || stats.scoreCount || 0 }}</div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="chart-grid">
      <div class="chart-box">
        <div class="chart-title">近7天投递趋势</div>
        <div ref="trendChart" class="chart-container" style="height: 260px;"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">各岗位投递量分布</div>
        <div ref="barChart" class="chart-container" style="height: 260px;"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useUserStore } from '@/stores/user.js'
import { statisticsAPI, jobAPI, deliveryAPI } from '@/api/index.js'
import * as echarts from 'echarts'

const userStore = useUserStore()
const userId = userStore.userId

const stats = ref({})
const trendChart = ref(null)
const barChart = ref(null)
const trendData = ref([])
const jobDistribution = ref([])
let trendInstance = null
let barInstance = null

onMounted(async () => {
  await loadDashboard()
  await loadTrend()
  await loadJobDistribution()
  await nextTick()
  renderCharts()
})

onUnmounted(() => {
  trendInstance?.dispose()
  barInstance?.dispose()
})

const loadDashboard = async () => {
  try {
    const res = await statisticsAPI.getHrDashboard()
    if (res.code === 200) {
      stats.value = res.data || {}
    }
  } catch (e) {
    console.error('加载统计失败', e)
  }
}

const loadTrend = async () => {
  try {
    const res = await statisticsAPI.getDeliveryTrend({ userId: userId })
    if (res.code === 200 && Array.isArray(res.data)) {
      trendData.value = res.data.map(d => ({ label: d.label || d.date, value: d.value || d.count }))
    }
  } catch (e) {
    console.error('加载趋势失败', e)
  }
}

const loadJobDistribution = async () => {
  if (!userStore.companyId) return
  try {
    const res = await jobAPI.getJobsByCompany(userStore.companyId, {})
    if (res.code === 200 && Array.isArray(res.data)) {
      const promises = res.data.map(async (job) => {
        try {
          const dRes = await deliveryAPI.getDeliveriesByJob(job.id)
          return { name: job.title, value: (dRes.code === 200 && Array.isArray(dRes.data)) ? dRes.data.length : 0 }
        } catch {
          return { name: job.title, value: 0 }
        }
      })
      jobDistribution.value = await Promise.all(promises)
    }
  } catch (e) {
    console.error('加载岗位分布失败', e)
  }
}

const renderCharts = () => {
  if (trendChart.value) {
    trendInstance = echarts.init(trendChart.value)
    trendInstance.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 50, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: trendData.value.map(d => d.label), axisLabel: { fontSize: 12, color: '#86909C' } },
      yAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 12, color: '#86909C' } },
      series: [{
        type: 'line', data: trendData.value.map(d => d.value),
        smooth: true, lineStyle: { color: '#165DFF', width: 3 },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(22,93,255,0.3)' }, { offset: 1, color: 'rgba(22,93,255,0.02)' }]) },
        itemStyle: { color: '#165DFF' }
      }]
    })
  }

  if (barChart.value) {
    barInstance = echarts.init(barChart.value)
    barInstance.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 50, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: jobDistribution.value.map(d => d.name), axisLabel: { fontSize: 12, color: '#86909C', rotate: 15 } },
      yAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 12, color: '#86909C' } },
      series: [{
        type: 'bar',
        data: jobDistribution.value.map((d, i) => ({ value: d.value, itemStyle: { color: ['#165DFF','#10B981','#F59E0B','#8B5CF6','#EF4444','#06B6D4'][i % 6] } })),
        barWidth: 36, borderRadius: [4, 4, 0, 0]
      }]
    })
  }
}
</script>

<style scoped>
.hr-data-stats { padding: 20px; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px; margin-bottom: 28px; }
.stat-card { background: white; padding: 28px; border-radius: 16px; border-left: 6px solid #165DFF; box-shadow: 0 6px 16px rgba(0,0,0,0.06); transition: transform 0.25s ease, box-shadow 0.25s ease; position: relative; overflow: hidden; }
.stat-card:hover { transform: translateY(-5px); box-shadow: 0 12px 25px rgba(0,0,0,0.1); }
.stat-card h3 { font-size: 14px; color: #86909C; display: flex; align-items: center; gap: 6px; }
.stat-card .num { font-size: 32px; font-weight: bold; margin-top: 10px; background: linear-gradient(90deg,#1E293B,#334155); -webkit-background-clip: text; color: transparent; }
.chart-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 24px; }
.chart-box { background: white; border-radius: 16px; padding: 28px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); position: relative; overflow: hidden; }
.chart-box::before { content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 3px; background: linear-gradient(90deg,#165DFF,#2563EB,transparent); }
.chart-title { font-size: 16px; font-weight: 600; margin-bottom: 16px; color: #1D2129; }
.chart-container { width: 100%; }
</style>
