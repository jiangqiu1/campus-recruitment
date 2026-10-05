<template>
  <div class="hr-data-stats fade-in">
    <div class="page-header">
      <h2>数据统计</h2>
      <p>招聘数据概览 · 实时统计</p>
    </div>
    
    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div class="stat-card" style="border-left-color: #165DFF;">
        <h3>在招岗位</h3>
        <div class="num">{{ stats.activeJobCount || 0 }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #10B981;">
        <h3>总投递量</h3>
        <div class="num">{{ stats.resumeCount || 0 }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #F59E0B;">
        <h3>待处理简历</h3>
        <div class="num">{{ stats.pendingResumeCount || 0 }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #06B6D4;">
        <h3>今日新增投递</h3>
        <div class="num">{{ stats.todayNewCount || 0 }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #8B5CF6;">
        <h3>今日面试</h3>
        <div class="num">{{ stats.todayInterviewCount || 0 }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #EF4444;">
        <h3>已录用</h3>
        <div class="num">{{ stats.hiredCount || 0 }}</div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div v-loading="loading" class="chart-grid">
      <div class="chart-box">
        <div class="chart-title">近7天投递趋势</div>
        <div ref="trendChart" class="chart-container" style="height: 260px;"></div>
        <div v-if="!loading && trendData.length === 0" class="chart-empty">暂无投递数据</div>
      </div>
      <div class="chart-box">
        <div class="chart-title">各岗位投递量分布</div>
        <div ref="barChart" class="chart-container" style="height: 260px;"></div>
        <div v-if="!loading && jobDistribution.length === 0" class="chart-empty">暂无岗位数据</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useUserStore } from '@/stores/user.js'
import { statisticsAPI, jobAPI, deliveryAPI } from '@/api/index.js'
import echarts from '@/utils/echarts'

const userStore = useUserStore()
const userId = userStore.userId

const stats = ref({})
const loading = ref(false)
const trendChart = ref(null)
const barChart = ref(null)
const trendData = ref([])
const jobDistribution = ref([])
let trendInstance = null
let barInstance = null

onMounted(async () => {
  loading.value = true
  // 三个数据源并行加载，缩短首屏等待
  await Promise.all([loadDashboard(), loadTrend(), loadJobDistribution()])
  await nextTick()
  renderCharts()
  loading.value = false
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
.stat-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 20px; margin-bottom: 28px; }
.stat-card { background: white; padding: 24px; border-radius: 16px; border-left: 6px solid #165DFF; box-shadow: 0 6px 16px rgba(0,0,0,0.06); transition: transform 0.25s ease, box-shadow 0.25s ease; position: relative; overflow: hidden; }
.stat-card:hover { transform: translateY(-5px); box-shadow: 0 12px 25px rgba(0,0,0,0.1); }
.stat-card h3 { margin: 0 0 8px 0; font-size: 14px; color: #86909C; font-weight: 500; }
.stat-card .num { font-size: 32px; font-weight: 700; color: #1D2129; margin-top: 8px; }
.chart-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 24px; }
.chart-box { background: white; border-radius: 16px; padding: 28px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); position: relative; overflow: hidden; }
.chart-box::before { content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 3px; background: linear-gradient(90deg,#165DFF,#2563EB,transparent); }
.chart-title { font-size: 16px; font-weight: 600; margin-bottom: 16px; color: #1D2129; }
.chart-container { width: 100%; }
.chart-empty { text-align: center; color: #C9CDD4; font-size: 13px; padding: 40px 0; }
</style>
