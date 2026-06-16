<template>
  <div class="dashboard fade-in">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>数据大屏</h2>
      <p>全校就业数据全景总览</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div class="stat-card" style="border-left-color: #165DFF;">
        <h3><span>👤</span> 注册用户</h3>
        <div class="num">{{ overview.totalUsers || 0 }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #10B981;">
        <h3><span>🏢</span> 入驻企业</h3>
        <div class="num">{{ overview.totalCompanies || 0 }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #F59E0B;">
        <h3><span>💼</span> 在招岗位</h3>
        <div class="num">{{ overview.activeJobs || 0 }}</div>
      </div>
      <div class="stat-card" style="border-left-color: #EF4444;">
        <h3><span>📮</span> 投递简历</h3>
        <div class="num">{{ overview.totalDeliveries || 0 }}</div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="chart-grid">
      <div class="chart-box">
        <div class="chart-title">岗位投递趋势（近7日）</div>
        <div class="chart-bar-wrap" v-if="barData.length">
          <div class="chart-bar-item" v-for="item in barData" :key="item.label">
            <div class="chart-bar" :style="{ height: item.height + '%' }">
              <span class="chart-bar-value">{{ item.value }}</span>
            </div>
            <span class="chart-bar-label">{{ item.label }}</span>
          </div>
        </div>
        <div v-else class="empty-chart">暂无投递数据</div>
      </div>
      <div class="chart-box">
        <div class="chart-title">热门岗位</div>
        <div class="word-cloud" v-if="hotJobs.length">
          <span
            v-for="(item, i) in hotJobs"
            :key="i"
            :class="['word-item', wordWeightClass(item.count || 0)]"
          >{{ item.name }}</span>
        </div>
        <div v-else class="empty-chart">暂无热门岗位数据</div>
      </div>
    </div>

    <!-- 最近动态 -->
    <div class="content-card">
      <div class="content-card-header">
        <span class="content-card-title">最近动态</span>
      </div>
      <el-table :data="recentActivities" style="width: 100%" stripe>
        <el-table-column prop="time" label="时间" width="180" />
        <el-table-column prop="user" label="用户" width="120" />
        <el-table-column prop="action" label="操作" min-width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '成功' ? 'success' : 'warning'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { statisticsAPI } from '@/api'

const overview = ref({})
const barData = ref([])
const hotJobs = ref([])
const recentActivities = ref([])

onMounted(async () => {
  await loadOverview()
  await loadDeliveryTrend()
  await loadHotJobs()
  await loadRecentActivities()
})

const loadOverview = async () => {
  try {
    const res = await statisticsAPI.getOverview()
    if (res.code === 200) overview.value = res.data
  } catch (e) {
    console.error('加载统计概览失败', e)
  }
}

const loadDeliveryTrend = async () => {
  try {
    const res = await statisticsAPI.getDeliveryTrend()
    if (res.code === 200) barData.value = res.data
  } catch (e) {
    console.error('加载投递趋势失败', e)
  }
}

const loadHotJobs = async () => {
  try {
    const res = await statisticsAPI.getHotJobs()
    if (res.code === 200) hotJobs.value = res.data
  } catch (e) {
    console.error('加载热门岗位失败', e)
  }
}

const loadRecentActivities = async () => {
  try {
    const res = await statisticsAPI.getRecentActivities()
    if (res.code === 200) recentActivities.value = res.data
  } catch (e) {
    console.error('加载最近动态失败', e)
  }
}

const wordWeightClass = (count) => {
  if (count >= 8) return 'w1'
  if (count >= 5) return 'w2'
  if (count >= 3) return 'w3'
  return 'w4'
}
</script>

<style scoped>
.chart-bar-wrap {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  gap: 12px;
  height: 280px;
  padding: 0 10px 40px 10px;
  border-bottom: 1px solid var(--border-color);
  position: relative;
  margin-bottom: 20px;
}
.chart-bar-item {
  flex: 1;
  max-width: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  height: 100%;
  position: relative;
}
.chart-bar {
  width: 100%;
  background: linear-gradient(to top, #165DFF, #60A5FA);
  border-radius: 6px 6px 0 0;
  transition: all 0.5s ease;
  position: relative;
  min-height: 20px;
}
.chart-bar:hover { filter: brightness(1.1); }
.chart-bar-value {
  position: absolute;
  top: -24px;
  font-size: 12px;
  font-weight: 600;
  color: var(--primary);
  white-space: nowrap;
}
.chart-bar-label {
  position: absolute;
  bottom: -28px;
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  max-width: 100%;
}
.word-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 20px;
  align-items: center;
  justify-content: center;
  min-height: 120px;
}
.word-item {
  color: var(--primary);
  opacity: 0.85;
  transition: all 0.3s;
  cursor: default;
}
.word-item:hover { opacity: 1; transform: scale(1.1); }
.empty-chart {
  text-align: center;
  color: var(--text-muted);
  padding: 60px 0;
  font-size: 14px;
}
</style>
