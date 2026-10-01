<template>
  <div class="ai-stats fade-in">
    <div class="page-header">
      <h2>AI使用统计</h2>
      <p>全局 AI 功能使用情况 · 多模型对比实验数据</p>
    </div>

    <!-- 多模型对比看板（毕设实验数据，来源 ai_parse_log） -->
    <div class="content-card compare-card">
      <div class="content-card-header">
        <span class="content-card-title">多模型对比看板</span>
        <span class="compare-sub">每次 AI 调用自动记录提供方与耗时，智谱 GLM 接入后自动纳入对比</span>
      </div>
      <el-table v-if="modelStats.providers.length" :data="modelStats.providers" stripe>
        <el-table-column label="AI 提供方" width="160">
          <template #default="{ row }">
            <el-tag size="small" :type="row.provider === 'glm' ? 'success' : row.provider === 'deepseek' ? 'primary' : 'info'">{{ providerLabel(row.provider) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="调用次数" width="110" align="center">
          <template #default="{ row }">{{ row.calls }}</template>
        </el-table-column>
        <el-table-column label="平均耗时" width="130" align="center">
          <template #default="{ row }">{{ row.avgLatency }} ms</template>
        </el-table-column>
        <el-table-column label="降级（mock）次数" width="150" align="center">
          <template #default="{ row }">{{ row.mockCount }}</template>
        </el-table-column>
        <el-table-column label="降级率" min-width="180">
          <template #default="{ row }">
            <el-progress :percentage="row.calls ? Math.round(row.mockCount / row.calls * 100) : 0" :stroke-width="10" />
          </template>
        </el-table-column>
      </el-table>
      <div v-else class="empty-state">
        <el-empty :image-size="80" description="暂无 AI 调用记录" />
      </div>
      <div v-if="modelStats.providers.length" ref="compareChart" style="height:260px;margin-top:20px"></div>
    </div>

    <div class="stat-grid">
      <div class="stat-card" style="border-left-color:#165DFF">
        <h3>简历解析</h3>
        <div class="num">{{ stats.parseCount }}</div>
      </div>
      <div class="stat-card" style="border-left-color:#10B981">
        <h3>人岗匹配</h3>
        <div class="num">{{ stats.matchCount }}</div>
      </div>
      <div class="stat-card" style="border-left-color:#F59E0B">
        <h3>简历评分</h3>
        <div class="num">{{ stats.scoreCount }}</div>
      </div>
      <div class="stat-card" style="border-left-color:#8B5CF6">
        <h3>操作总数</h3>
        <div class="num">{{ stats.parseCount + stats.matchCount + stats.scoreCount }}</div>
      </div>
    </div>

    <div class="content-card">
      <div class="content-card-header">
        <span class="content-card-title">教师 AI 使用排行</span>
      </div>
      <el-table v-loading="loading" :data="teacherStats" style="width:100%" stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column label="教师姓名" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.name || row.username || '未知' }}<text v-if="row.username" class="table-sub-text"> ({{ row.username }})</text></template>
        </el-table-column>
        <el-table-column label="简历解析次数" width="130" align="center">
          <template #default="{ row }">{{ row.parseCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="人岗匹配次数" width="130" align="center">
          <template #default="{ row }">{{ row.matchCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="简历评分次数" width="130" align="center">
          <template #default="{ row }">{{ row.scoreCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="最后使用时间" width="160">
          <template #default="{ row }">{{ (row.lastUseTime || '').replace('T', ' ') || '-' }}</template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && teacherStats.length === 0" class="empty-state">
        <el-empty :image-size="100" description="暂无AI使用数据">
          <p style="color:#86909C;font-size:13px;margin-top:8px;">
            教师使用 AI 简历解析、人岗匹配、简历评分功能后，数据会自动出现在这里。
          </p>
        </el-empty>
      </div>
    </div>

    <div class="chart-grid">
      <div class="chart-box">
        <div class="chart-title">每日AI使用趋势（近7天）</div>
        <div ref="trendChart" style="height:260px"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">功能使用分布</div>
        <div ref="pieChart" style="height:260px"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { aiParseAPI, jobMatchAPI, resumeScoreAPI, userAPI } from '@/api'
import echarts from '@/utils/echarts'

const loading = ref(false)
const stats = ref({ parseCount: 0, matchCount: 0, scoreCount: 0, teacherCount: 0 })
const teacherStats = ref([])
const parseLogs = ref([])
const matchRecords = ref([])
const scoreRecords = ref([])
const trendChart = ref(null)
const pieChart = ref(null)
const compareChart = ref(null)
let trendInstance = null
let pieInstance = null
let compareInstance = null

// 多模型对比统计（后端聚合 ai_parse_log）
const modelStats = ref({ providers: [], tasks: [], total: 0, mockCount: 0 })

const providerLabel = (p) => ({ deepseek: 'DeepSeek', glm: '智谱 GLM', unknown: '未记录' }[p] || p)

onMounted(async () => {
  await loadData()
  await nextTick()
  renderCharts()
})

onUnmounted(() => {
  trendInstance?.dispose()
  pieInstance?.dispose()
  compareInstance?.dispose()
})

const loadData = async () => {
  loading.value = true
  try {
    // 并发获取各种数据
    const [parseRes, allUsersRes, matchRes, scoreRes, compareRes] = await Promise.all([
      aiParseAPI.getParseLogs({ page: 1, size: 500 }),
      userAPI.getUsers({ page: 1, size: 500 }),
      jobMatchAPI.getMatches({ page: 1, size: 500 }),
      resumeScoreAPI.getScores({ page: 1, size: 500 }),
      aiParseAPI.getModelComparison().catch(() => null)
    ])

    // 0. 多模型对比统计
    if (compareRes && compareRes.code === 200 && compareRes.data) {
      modelStats.value = { providers: [], tasks: [], total: 0, mockCount: 0, ...compareRes.data }
    }

    // 1. 解析AI解析日志
    if (parseRes.code === 200) {
      parseLogs.value = parseRes.data?.list || parseRes.data?.records || parseRes.data || []
    }

    // 2. 获取人岗匹配记录
    if (matchRes.code === 200) {
      matchRecords.value = matchRes.data?.list || matchRes.data?.records || matchRes.data || []
    }

    // 3. 获取简历评分记录
    if (scoreRes.code === 200) {
      scoreRecords.value = scoreRes.data?.list || scoreRes.data?.records || scoreRes.data || []
    }

    // 筛选出教师角色（role=1）的用户
    let allUsers = []
    if (allUsersRes.code === 200) {
      allUsers = allUsersRes.data?.list || allUsersRes.data?.records || allUsersRes.data || []
    }
    const teachers = (Array.isArray(allUsers) ? allUsers : []).filter(u => u.role === 1 || u.role === '1')

    // 按教师统计
    const teacherMap = {}

    // 统计解析日志
    for (const log of parseLogs.value) {
      const teacherId = log.teacherId || log.createdBy || log.userId
      if (!teacherId) continue
      if (!teacherMap[teacherId]) {
        const teacher = teachers.find(t => t.id === teacherId)
        teacherMap[teacherId] = { id: teacherId, name: teacher?.realName || teacher?.username || '未知', username: teacher?.username || '', parseCount: 0, matchCount: 0, scoreCount: 0, lastUseTime: '' }
      }
      teacherMap[teacherId].parseCount++
      const t = log.createTime || log.updateTime
      if (t && (!teacherMap[teacherId].lastUseTime || t > teacherMap[teacherId].lastUseTime)) {
        teacherMap[teacherId].lastUseTime = t
      }
    }

    // 统计匹配记录
    for (const m of matchRecords.value) {
      const teacherId = m.teacherId || m.createdBy
      if (!teacherId) continue
      if (!teacherMap[teacherId]) {
        const teacher = teachers.find(t => t.id === teacherId)
        teacherMap[teacherId] = { id: teacherId, name: teacher?.realName || teacher?.username || '未知', username: teacher?.username || '', parseCount: 0, matchCount: 0, scoreCount: 0, lastUseTime: '' }
      }
      teacherMap[teacherId].matchCount++
      const t = m.createTime
      if (t && (!teacherMap[teacherId].lastUseTime || t > teacherMap[teacherId].lastUseTime)) {
        teacherMap[teacherId].lastUseTime = t
      }
    }

    // 统计评分记录
    for (const s of scoreRecords.value) {
      const teacherId = s.teacherId || s.createdBy || s.scorerId
      if (!teacherId) continue
      if (!teacherMap[teacherId]) {
        const teacher = teachers.find(t => t.id === teacherId)
        teacherMap[teacherId] = { id: teacherId, name: teacher?.realName || teacher?.username || '未知', username: teacher?.username || '', parseCount: 0, matchCount: 0, scoreCount: 0, lastUseTime: '' }
      }
      teacherMap[teacherId].scoreCount++
      const t = s.createTime
      if (t && (!teacherMap[teacherId].lastUseTime || t > teacherMap[teacherId].lastUseTime)) {
        teacherMap[teacherId].lastUseTime = t
      }
    }

    teacherStats.value = Object.values(teacherMap).sort((a, b) => (b.parseCount + b.matchCount + b.scoreCount) - (a.parseCount + a.matchCount + a.scoreCount))

    // 更新统计
    const allCount = parseLogs.value.length + matchRecords.value.length + scoreRecords.value.length
    stats.value = {
      parseCount: parseLogs.value.length,
      matchCount: matchRecords.value.length,
      scoreCount: scoreRecords.value.length,
      teacherCount: Object.keys(teacherMap).length
    }
  } catch (e) {
    console.error('加载AI统计数据失败', e)
  } finally {
    loading.value = false
  }
}

const renderCharts = () => {
  // 近7天趋势（从已有记录中计算）
  if (trendChart.value) {
    trendInstance = echarts.init(trendChart.value)
    // 收集所有 AI 操作的时间
    const allTimes = []
    parseLogs.value.forEach(l => { if (l.createTime) allTimes.push(l.createTime) })
    matchRecords.value.forEach(m => { if (m.createTime) allTimes.push(m.createTime) })
    scoreRecords.value.forEach(s => { if (s.createTime) allTimes.push(s.createTime) })
    // 统计最近7天
    const days = []
    const dayData = []
    for (let i = 6; i >= 0; i--) {
      const d = new Date()
      d.setDate(d.getDate() - i)
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
      const label = key.substring(5) // MM-DD
      days.push(label)
      const count = allTimes.filter(t => {
        const tStr = typeof t === 'string' ? t.substring(0, 10) : ''
        return tStr === key
      }).length
      dayData.push(count)
    }
    trendInstance.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 45, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: days, axisLabel: { fontSize: 12, color: '#86909C' } },
      yAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 12, color: '#86909C' } },
      series: [{
        type: 'line', data: dayData,
        smooth: true, lineStyle: { color: '#8B5CF6', width: 3 },
        areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(139,92,246,0.3)'},{offset:1,color:'rgba(139,92,246,0.02)'}]) },
        itemStyle: { color: '#8B5CF6' }
      }]
    })
  }

  // 功能使用分布
  if (pieChart.value) {
    pieInstance = echarts.init(pieChart.value)
    pieInstance.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['40%','70%'],
        center: ['50%','55%'],
        data: [
          { name: '简历解析', value: stats.value.parseCount, itemStyle: { color: '#165DFF' } },
          { name: '人岗匹配', value: stats.value.matchCount || 0, itemStyle: { color: '#10B981' } },
          { name: '简历评分', value: stats.value.scoreCount || 0, itemStyle: { color: '#F59E0B' } }
        ],
        label: { fontSize: 13, color: '#4E5969' },
        emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.2)' } }
      }]
    })
  }

  // 多模型对比：调用量柱状图 + 平均耗时折线（双轴）
  if (compareChart.value && modelStats.value.providers.length) {
    compareInstance = echarts.init(compareChart.value)
    const names = modelStats.value.providers.map(p => providerLabel(p.provider))
    compareInstance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['调用次数', '平均耗时(ms)'], bottom: 0 },
      grid: { left: 55, right: 60, top: 30, bottom: 45 },
      xAxis: { type: 'category', data: names, axisLabel: { fontSize: 12, color: '#86909C' } },
      yAxis: [
        { type: 'value', name: '调用次数', minInterval: 1, axisLabel: { fontSize: 12, color: '#86909C' } },
        { type: 'value', name: '耗时(ms)', axisLabel: { fontSize: 12, color: '#86909C' } }
      ],
      series: [
        {
          name: '调用次数', type: 'bar', barWidth: 40,
          data: modelStats.value.providers.map(p => p.calls),
          itemStyle: { color: '#8B5CF6', borderRadius: [4, 4, 0, 0] }
        },
        {
          name: '平均耗时(ms)', type: 'line', yAxisIndex: 1, smooth: true,
          data: modelStats.value.providers.map(p => p.avgLatency),
          lineStyle: { color: '#165DFF', width: 3 },
          itemStyle: { color: '#165DFF' }
        }
      ]
    })
  }
}
</script>

<style scoped>
.compare-card { margin-bottom: 28px; }
.compare-sub { font-size: 12px; color: #86909C; }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-top: 28px; }
.chart-box { background: white; border-radius: 16px; padding: 28px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); position: relative; overflow: hidden; }
.chart-box::before { content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 3px; background: linear-gradient(90deg,#8B5CF6,#A78BFA,transparent); border-radius: 16px 16px 0 0; }
.table-sub-text { font-size: 12px; color: #C9CDD4; }
.chart-title { font-size: 16px; font-weight: 600; margin-bottom: 16px; color: #1D2129; }
</style>
