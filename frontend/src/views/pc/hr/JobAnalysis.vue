<template>
  <div class="hr-job-analysis fade-in">
    <div class="page-header">
      <h2>岗位分析</h2>
      <p>能力维度匹配 · 岗位评分概况</p>
    </div>
    
    <div class="chart-grid">
      <!-- 雷达图 -->
      <div class="chart-box">
        <div class="chart-title">能力维度匹配</div>
        <div ref="radarChart" style="width: 100%; height: 320px;"></div>
      </div>
      <!-- 各岗位匹配度排名 -->
      <div class="chart-box">
        <div class="chart-title">各岗位平均匹配度</div>
        <div ref="rankBarChart" style="width: 100%; height: 320px;"></div>
      </div>
    </div>

    <!-- 评分分布表格 -->
    <div class="chart-box full-width">
      <div class="chart-title">各岗位评分概况</div>
      <el-table :data="jobScores" stripe style="width: 100%;">
        <el-table-column prop="title" label="岗位名称" />
        <el-table-column prop="total" label="浏览次数" width="100" />
        <el-table-column prop="scored" label="已评分" width="100" />
        <el-table-column prop="avgScore" label="平均分" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.avgScore || 0" :color="scoreColor(row.avgScore)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="viewJobDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 岗位详情对话框 -->
    <el-dialog v-model="detailVisible" :title="detailTitle" width="800px" destroy-on-close>
      <div v-loading="detailLoading">
        <!-- 概览统计 -->
        <div class="detail-stats" v-if="detailData.length > 0">
          <div class="detail-stat-item">
            <span class="stat-label">投递人数</span>
            <span class="stat-value">{{ detailData.length }}</span>
          </div>
          <div class="detail-stat-item">
            <span class="stat-label">最高分</span>
            <span class="stat-value" :style="{ color: scoreColor(detailMax) }">{{ detailMax }}</span>
          </div>
          <div class="detail-stat-item">
            <span class="stat-label">最低分</span>
            <span class="stat-value" :style="{ color: scoreColor(detailMin) }">{{ detailMin }}</span>
          </div>
          <div class="detail-stat-item">
            <span class="stat-label">平均分</span>
            <span class="stat-value" :style="{ color: scoreColor(detailAvg) }">{{ detailAvg }}</span>
          </div>
        </div>

        <!-- 评分详情表格 -->
        <el-table :data="detailData" stripe style="width: 100%;" empty-text="暂无评分数据">
          <el-table-column label="序号" type="index" width="60" />
          <el-table-column prop="studentName" label="学生" width="100" />
          <el-table-column prop="score" label="综合评分" width="100">
            <template #default="{ row }">
              <el-tag :type="scoreTag(row.score)" effect="plain">{{ row.score }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="skillScore" label="专业技能" width="90" align="center" />
          <el-table-column prop="expScore" label="项目经验" width="90" align="center" />
          <el-table-column prop="eduScore" label="学历匹配" width="90" align="center" />
          <el-table-column prop="salaryScore" label="薪资匹配" width="90" align="center" />
          <el-table-column label="评分时间" width="100">
            <template #default="{ row }">
              <span class="time-text">{{ row.createTime ? row.createTime.slice(5, 16) : '--' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useUserStore } from '@/stores/user.js'
import { jobAPI, resumeScoreAPI } from '@/api/index.js'
import { ElMessage } from 'element-plus'
import echarts from '@/utils/echarts'
import { scoreColor, scoreTag, parseScoreDetail } from '@/utils/score'

const userStore = useUserStore()

const radarChart = ref(null)
const rankBarChart = ref(null)
let radarInstance = null
let rankBarInstance = null

const jobScores = ref([])
const radarData = ref({
  indicator: ['专业技能', '项目经验', '学历匹配', '期望薪资', '稳定性', '综合素质'],
  value: [0, 0, 0, 0, 0, 0]
})

// 详情对话框
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailTitle = ref('')
const detailData = ref([])
const detailMax = computed(() => Math.max(...detailData.value.map(d => d.score || 0), 0))
const detailMin = computed(() => Math.min(...detailData.value.map(d => d.score || 100), 100))
const detailAvg = computed(() => {
  const scores = detailData.value.map(d => d.score || 0)
  return scores.length ? Math.round(scores.reduce((a, b) => a + b, 0) / scores.length) : 0
})

onMounted(async () => {
  await loadJobScores()
  await nextTick()
  renderCharts()
})

onUnmounted(() => {
  radarInstance?.dispose()
  rankBarInstance?.dispose()
})

const loadJobScores = async () => {
  if (!userStore.companyId) return
  try {
    const res = await jobAPI.getJobsByCompany(userStore.companyId, {})
    if (res.code !== 200 || !Array.isArray(res.data)) return

    // 各岗位的评分/分布并行请求，替代串行循环
    let dimensionAvg = null
    const dimJobId = res.data.find(j => j.id)?.id

    const [dimRes, ...jobResults] = await Promise.all([
      dimJobId ? resumeScoreAPI.getDimensionScores(dimJobId).catch(() => null) : Promise.resolve(null),
      ...res.data.map(async (job) => {
        try {
          const [avgRes, distRes] = await Promise.all([
            resumeScoreAPI.getAverageScoreByJobId(job.id),
            resumeScoreAPI.getMatchDistribution(job.id)
          ])
          const avgScore = avgRes.code === 200 ? Math.round(avgRes.data.averageScore) : 0
          const dist = distRes.code === 200 ? distRes.data : {}
          let scored = 0
          if (dist) {
            Object.values(dist).forEach(v => {
              if (v > 0) scored += v
            })
          }
          return { id: job.id, title: job.title, total: job.viewCount || 0, scored: scored, avgScore: avgScore }
        } catch {
          return { id: job.id, title: job.title, total: 0, scored: 0, avgScore: 0 }
        }
      })
    ])

    if (dimRes && dimRes.code === 200 && dimRes.data && dimRes.data.overall > 0) {
      dimensionAvg = dimRes.data
    }

    const data = jobResults
    jobScores.value = data

    if (dimensionAvg) {
      radarData.value.value = [
        dimensionAvg.skills || 0,
        dimensionAvg.experience || 0,
        dimensionAvg.education || 0,
        dimensionAvg.salary || 0,
        dimensionAvg.stability || 0,
        dimensionAvg.overall || 0
      ]
    } else {
      // 无真实维度数据时清空雷达图，不伪造数据
      radarData.value.value = []
    }
  } catch (e) {
    console.error('加载评分数据失败', e)
  }
}

const viewJobDetail = async (row) => {
  detailTitle.value = `${row.title} - 评分详情`
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = []
  try {
    const res = await resumeScoreAPI.getScoresByJobId(row.id)
    if (res.code === 200 && Array.isArray(res.data)) {
      detailData.value = res.data.map(log => {
        const parsed = parseScoreDetail(log.scoreDetail)
        const dimMap = {}
        parsed.dims.forEach(d => { dimMap[d.key] = d.score })
        return {
          studentName: log.studentName || '学生 #' + (log.deliveryId || log.id),
          score: log.score,
          skillScore: dimMap.skill ?? '-',
          expScore: dimMap.exp ?? '-',
          eduScore: dimMap.edu ?? '-',
          salaryScore: dimMap.salary ?? '-',
          createTime: log.createTime
        }
      })
    }
  } catch (e) {
    ElMessage.error('加载评分详情失败')
  } finally {
    detailLoading.value = false
  }
}

const renderCharts = () => {
  if (radarChart.value) {
    radarInstance = echarts.init(radarChart.value)
    radarInstance.setOption({
      tooltip: {},
      radar: {
        indicator: radarData.value.indicator.map(name => ({ name, max: 100 })),
        radius: '65%',
        axisName: { color: '#4E5969', fontSize: 12 }
      },
      series: [{
        type: 'radar',
        data: [{ value: radarData.value.value, name: '整体匹配度' }],
        areaStyle: { color: 'rgba(22,93,255,0.2)' },
        lineStyle: { color: '#165DFF', width: 2 },
        itemStyle: { color: '#165DFF' }
      }]
    })
  }

  if (rankBarChart.value) {
    rankBarInstance = echarts.init(rankBarChart.value)
    const sorted = [...jobScores.value].sort((a, b) => b.avgScore - a.avgScore)
    rankBarInstance.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 100, right: 40, top: 20, bottom: 20 },
      xAxis: { type: 'value', max: 100, axisLabel: { fontSize: 12, color: '#86909C' } },
      yAxis: {
        type: 'category',
        data: sorted.map(d => d.title).reverse(),
        axisLabel: { fontSize: 12, color: '#4E5969' }
      },
      series: [{
        type: 'bar',
        data: sorted.map(d => ({
          value: d.avgScore,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#165DFF' },
              { offset: 1, color: '#60A5FA' }
            ])
          }
        })).reverse(),
        barWidth: 20,
        label: { show: true, position: 'right', fontSize: 12, color: '#4E5969' }
      }]
    })
  }
}
</script>

<style scoped>
.hr-job-analysis { padding: 20px; }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-bottom: 24px; }
.chart-box { background: white; border-radius: 16px; padding: 28px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); position: relative; overflow: hidden; }
.chart-box::before { content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 3px; background: linear-gradient(90deg,#165DFF,#2563EB,transparent); }
.chart-title { font-size: 16px; font-weight: 600; margin-bottom: 16px; color: #1D2129; }
.full-width { grid-column: 1 / -1; }

.detail-stats { display: flex; gap: 24px; margin-bottom: 20px; }
.detail-stat-item { flex: 1; background: #F8F9FC; border-radius: 10px; padding: 16px; text-align: center; }
.detail-stat-item .stat-label { display: block; font-size: 13px; color: #86909C; margin-bottom: 6px; }
.detail-stat-item .stat-value { font-size: 24px; font-weight: 700; color: #1D2129; }
.time-text { font-size: 13px; color: #86909C; }
</style>
