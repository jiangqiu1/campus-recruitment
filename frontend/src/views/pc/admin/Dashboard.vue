<template>
  <div class="dashboard fade-in" v-loading="loading">
    <div class="page-header">
      <h2>数据大屏</h2>
      <p>全校就业数据全景总览</p>
    </div>

    <div class="stat-grid">
      <div class="stat-card" style="border-left-color: #165DFF;">
        <h3>注册用户</h3>
        <div class="num">{{ overview.totalUsers || 0 }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #10B981;">
        <h3>入驻企业</h3>
        <div class="num">{{ overview.totalCompanies || 0 }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #F59E0B;">
        <h3>在招岗位</h3>
        <div class="num">{{ overview.activeJobs || 0 }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #EF4444;">
        <h3>投递简历</h3>
        <div class="num">{{ overview.totalDeliveries || 0 }}</div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="chart-box">
        <div class="chart-title">岗位投递趋势（近7日）</div>
        <div ref="trendChart" class="chart-container" style="height: 260px;"></div>
        <div v-if="!loading && !hasTrendData" class="chart-empty">近7日暂无投递数据</div>
      </div>
      <div class="chart-box">
        <div class="chart-title">热门岗位 Top 10</div>
        <div ref="hotChart" class="chart-container" style="height: 260px;"></div>
        <div v-if="!loading && hotBarData.length === 0" class="chart-empty">暂无热门岗位数据</div>
      </div>
    </div>

    <div class="content-card">
      <div class="content-card-header">
        <span class="content-card-title">最近动态</span>
      </div>
      <el-table :data="recentActivities" style="width: 100%" stripe>
        <el-table-column prop="time" label="时间" width="180" />
        <el-table-column prop="user" label="用户" width="120" />
        <el-table-column prop="action" label="操作" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '成功' ? 'success' : 'warning'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && recentActivities.length === 0" class="empty-state">
        <el-empty :image-size="100" description="暂无最近动态" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { statisticsAPI } from '@/api'
import echarts from '@/utils/echarts'

const loading = ref(false)
const overview = ref({})
const recentActivities = ref([])
// 近7日全为 0 时视为无数据，避免渲染一张"空图"
const hasTrendData = computed(() => trendData.value.some(d => (d.value || 0) > 0))
const trendData = ref([])
const hotBarData = ref([])
const trendChart = ref(null)
const hotChart = ref(null)
let trendInstance = null
let hotInstance = null

onMounted(async () => {
  loading.value = true
  await Promise.all([loadOverview(), loadDeliveryTrend(), loadHotJobs(), loadRecentActivities()])
  await nextTick()
  renderCharts()
  loading.value = false
})

onUnmounted(() => {
  trendInstance?.dispose()
  hotInstance?.dispose()
})

const loadOverview = async () => {
  try {
    const res = await statisticsAPI.getOverview()
    if (res.code === 200) overview.value = res.data
  } catch (e) { console.error('加载统计概览失败', e) }
}

const loadDeliveryTrend = async () => {
  try {
    const res = await statisticsAPI.getDeliveryTrend()
    if (res.code === 200 && Array.isArray(res.data)) {
      trendData.value = res.data.map(d => ({ label: d.label || d.date, value: d.value || d.count }))
    }
  } catch (e) { console.error('加载投递趋势失败', e) }
}

const loadHotJobs = async () => {
  try {
    const res = await statisticsAPI.getHotJobs()
    if (res.code === 200 && Array.isArray(res.data)) {
      // 取 top 10，按数量升序排列（让柱状图从上往下由高到低）
      const sorted = res.data.slice(0, 10).sort((a, b) => (a.count || 0) - (b.count || 0))
      hotBarData.value = sorted
    }
  } catch (e) { console.error('加载热门岗位失败', e) }
}

const loadRecentActivities = async () => {
  try {
    const res = await statisticsAPI.getRecentActivities()
    if (res.code === 200) recentActivities.value = (res.data || []).map(a => ({
      ...a,
      action: (a.action || '').replace(/^OPERATION:/, '')
    }))
  } catch (e) { console.error('加载最近动态失败', e) }
}

const renderCharts = () => {
  // 投递趋势折线图（全 0 时不出图，显示空态）
  if (trendChart.value && hasTrendData.value) {
    trendInstance = echarts.init(trendChart.value)
    trendInstance.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 50, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: trendData.value.map(d => d.label), axisLabel: { fontSize: 12, color: '#86909C' } },
      yAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 12, color: '#86909C' } },
      series: [{
        type: 'line', data: trendData.value.map(d => d.value),
        smooth: true, lineStyle: { color: '#165DFF', width: 3 },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(22,93,255,0.3)' }, { offset: 1, color: 'rgba(22,93,255,0.02)' }
        ]) },
        itemStyle: { color: '#165DFF' }
      }]
    })
  }

  // 热门岗位横向柱状图
  if (hotChart.value && hotBarData.value.length > 0) {
    hotInstance = echarts.init(hotChart.value)
    hotInstance.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 8, right: 30, top: 10, bottom: 20, containLabel: true },
      xAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 12, color: '#86909C' } },
      yAxis: {
        type: 'category', data: hotBarData.value.map(d => d.name),
        axisLabel: { fontSize: 12, color: '#4E5969', width: 105, overflow: 'truncate' }
      },
      series: [{
        type: 'bar', data: hotBarData.value.map(d => ({
          value: d.count || 0,
          itemStyle: { color: '#165DFF' }
        })),
        barWidth: 20, borderRadius: [0, 4, 4, 0]
      }]
    })
  }
}
</script>

<style scoped>
.dashboard { min-height: 400px; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 28px; }
.stat-card { background: white; padding: 24px; border-radius: 16px; border-left: 6px solid; box-shadow: 0 6px 16px rgba(0,0,0,0.06); }
.stat-card h3 { margin: 0 0 8px; font-size: 14px; color: #86909C; font-weight: 500; }
.stat-card .num { font-size: 32px; font-weight: 700; color: #1D2129; }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-bottom: 24px; }
.empty-state { padding: 40px 0; display: flex; justify-content: center; }
.chart-box { background: white; border-radius: 16px; padding: 24px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); position: relative; }
.chart-title { font-size: 15px; font-weight: 600; color: #1D2129; margin-bottom: 12px; }
.chart-container { width: 100%; }
.chart-empty { text-align: center; color: #C9CDD4; font-size: 13px; padding: 60px 0; }
.content-card { background: #fff; border-radius: 16px; padding: 24px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); }
.content-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.content-card-title { font-size: 16px; font-weight: 600; color: #1D2129; }
</style>
