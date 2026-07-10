<template>
  <div class="ai-match fade-in">
    <div class="page-header">
      <h2>AI 人岗匹配</h2>
      <p>智能匹配学生与岗位 · 批量推送</p>
    </div>

    <!-- 统计看板（仅岗位模式） -->
    <div class="stat-grid" v-if="matchMode === 'job'">
      <div class="stat-card" style="border-left-color:#165DFF">
        <h3>已匹配</h3>
        <div class="num">{{ matchStats.matched }}</div>
      </div>
      <div class="stat-card" style="border-left-color:#10B981">
        <h3>已推送</h3>
        <div class="num">{{ matchStats.pushed }}</div>
      </div>
      <div class="stat-card" style="border-left-color:#F59E0B">
        <h3>已查看</h3>
        <div class="num">{{ matchStats.viewed }}</div>
      </div>
      <div class="stat-card" style="border-left-color:#7F77DD">
        <h3>平均匹配度</h3>
        <div class="num">{{ matchStats.avgScore }}%</div>
      </div>
    </div>

    <!-- 分数分布（仅岗位模式） -->
    <div class="content-card" v-if="matchMode === 'job' && matchStats.matched > 0 && matchStats.distribution.total > 0">
      <div class="content-card-header">
        <span class="content-card-title">分数分布</span>
      </div>
      <div class="dist-bar-wrapper">
        <div class="dist-bar">
          <div class="dist-segment dist-high" :style="{ width: distPct('high') + '%' }" :title="'高分(80+): ' + matchStats.distribution.high + '人'">
            <span v-if="distPct('high') > 8">{{ matchStats.distribution.high }}人</span>
          </div>
          <div class="dist-segment dist-mid" :style="{ width: distPct('mid') + '%' }" :title="'中等(60-79): ' + matchStats.distribution.mid + '人'">
            <span v-if="distPct('mid') > 8">{{ matchStats.distribution.mid }}人</span>
          </div>
          <div class="dist-segment dist-low" :style="{ width: distPct('low') + '%' }" :title="'低分(60以下): ' + matchStats.distribution.low + '人'">
            <span v-if="distPct('low') > 8">{{ matchStats.distribution.low }}人</span>
          </div>
        </div>
        <div class="dist-legend">
          <span><span class="dot dot-high"></span> 高分 80+</span>
          <span><span class="dot dot-mid"></span> 中等 60-79</span>
          <span><span class="dot dot-low"></span> 低分 &lt;60</span>
        </div>
      </div>
    </div>

    <!-- 模式切换 -->
    <div class="mode-tabs">
      <div class="mode-tab" :class="{ active: matchMode === 'job' }" @click="switchMode('job')">按岗位匹配</div>
      <div class="mode-tab" :class="{ active: matchMode === 'student' }" @click="switchMode('student')">按学生匹配</div>
    </div>

    <!-- 岗位匹配模式 -->
    <div class="content-card" v-if="matchMode === 'job'">
      <div class="content-card-header">
        <span class="content-card-title">匹配配置</span>
      </div>
      <el-form :inline="true" class="match-form">
        <el-form-item label="选择岗位">
          <el-select v-model="selectedJobId" placeholder="请选择岗位" filterable clearable style="width:320px">
            <el-option v-for="j in jobList" :key="j.id" :label="j.title + (j.companyName ? ' - ' + j.companyName : '')" :value="j.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择班级">
          <el-select v-model="selectedClassId" placeholder="全部学生" clearable style="width:200px">
            <el-option v-for="c in classList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="generateMatch" :loading="generating" :disabled="!selectedJobId">
            <el-icon style="margin-right:4px"><MagicStick /></el-icon>
            批量匹配
          </el-button>
          <el-button @click="showCreateDialog" :disabled="!selectedJobId">单条生成</el-button>
          <el-button @click="loadResults" :disabled="!selectedJobId">刷新结果</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 学生匹配模式 -->
    <div class="content-card" v-if="matchMode === 'student'">
      <div class="content-card-header">
        <span class="content-card-title">选择学生</span>
      </div>
      <el-form :inline="true" class="match-form">
        <el-form-item label="选择班级">
          <el-select v-model="selectedStudentClassId" placeholder="请选择班级" clearable style="width:200px" @change="onStudentClassChange">
            <el-option v-for="c in classList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择学生">
          <el-select v-model="selectedStudentId" placeholder="请选择学生" filterable clearable style="width:300px" :disabled="!selectedStudentClassId">
            <el-option v-for="s in studentModeStudents" :key="s.id" :label="(s.realName || s.username) + (s.username ? ' (' + s.username + ')' : '')" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="batchMatchByStudent" :loading="generating" :disabled="!selectedStudentId">
            <el-icon style="margin-right:4px"><MagicStick /></el-icon>
            为该生匹配岗位
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <div class="content-card-header">
        <span class="content-card-title">{{ matchMode === 'job' ? '匹配结果' : '岗位匹配排名' }}</span>
        <div v-if="matchMode === 'job'">
          <el-button v-if="matchList.length > 0" size="small" type="success" :loading="pushingAll" @click="pushAll" style="margin-right:8px">
            批量推送
          </el-button>
          <el-button v-if="matchList.length > 0" size="small" @click="showCompare" :disabled="selectedCompare.length < 2 || selectedCompare.length > 4">
            对比选中 ({{ selectedCompare.length }})
          </el-button>
        </div>
      </div>

      <!-- 岗位模式表格 -->
      <el-table v-if="matchMode === 'job'" v-loading="loading" :data="matchList" style="width:100%" stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="40" />
        <el-table-column label="学生" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.displayStudent }}</template>
        </el-table-column>
        <el-table-column prop="className" label="班级" width="130" />
        <el-table-column label="匹配度" width="220" align="center">
          <template #default="{ row }">
            <el-popover placement="bottom" :width="210" trigger="hover" :disabled="!row.scoreDetail || Object.keys(row.scoreDetail).length === 0">
              <template #reference>
                <div style="cursor:pointer; padding:4px 0;">
                  <el-progress :percentage="matchPercent(row.matchScore)" :color="scoreColor(row.matchScore)" :stroke-width="10" />
                </div>
              </template>
              <div class="dimension-popover">
                <div class="dimension-title">子维度匹配详情</div>
                <div v-for="(val, key) in row.scoreDetail" :key="key" class="dimension-row">
                  <span class="dimension-label">{{ dimLabel(key) }}</span>
                  <el-progress :percentage="val" :stroke-width="8" :color="scoreColor(val / 100)" style="width:110px" />
                </div>
              </div>
            </el-popover>
          </template>
        </el-table-column>
        <el-table-column label="推送状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.pushed ? 'success' : 'info'" size="small">
              {{ row.pushed ? '已推送' : '未推送' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="推送时间" width="170">
          <template #default="{ row }">{{ formatDate(row.pushTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button v-if="!row.pushed" type="primary" link size="small" @click="pushSingle(row)">推送</el-button>
            <span v-else style="color:#C9CDD4;font-size:12px;">已推送</span>
            <el-button type="danger" link size="small" @click="deleteMatch(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 学生模式表格 -->
      <el-table v-if="matchMode === 'student'" v-loading="loading" :data="matchList" style="width:100%" stripe>
        <el-table-column label="岗位" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.jobTitle || '岗位#' + row.jobId }}</template>
        </el-table-column>
        <el-table-column label="公司" width="160">
          <template #default="{ row }">{{ row.companyName || '-' }}</template>
        </el-table-column>
        <el-table-column label="匹配度" width="220" align="center">
          <template #default="{ row }">
            <el-popover placement="bottom" :width="210" trigger="hover" :disabled="!row.scoreDetail || Object.keys(row.scoreDetail).length === 0">
              <template #reference>
                <div style="cursor:pointer; padding:4px 0;">
                  <el-progress :percentage="matchPercent(row.matchScore)" :color="scoreColor(row.matchScore)" :stroke-width="10" />
                </div>
              </template>
              <div class="dimension-popover">
                <div class="dimension-title">子维度匹配详情</div>
                <div v-for="(val, key) in row.scoreDetail" :key="key" class="dimension-row">
                  <span class="dimension-label">{{ dimLabel(key) }}</span>
                  <el-progress :percentage="val" :stroke-width="8" :color="scoreColor(val / 100)" style="width:110px" />
                </div>
              </div>
            </el-popover>
          </template>
        </el-table-column>
        <el-table-column label="匹配理由" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.matchReason || '-' }}</template>
        </el-table-column>
        <el-table-column label="推送状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.pushed ? 'success' : 'info'" size="small">
              {{ row.pushed ? '已推送' : '未推送' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="!row.pushed" type="primary" link size="small" @click="pushSingle(row)">推送</el-button>
            <span v-else style="color:#C9CDD4;font-size:12px;">已推送</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && matchList.length === 0" class="empty-state">
        <el-empty :image-size="100" :description="matchMode === 'job' ? '请选择岗位并点击批量匹配' : '请选择学生并点击为该生匹配岗位'" />
      </div>

      <div v-if="total > pageSize" style="margin-top:20px;text-align:center;">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" @current-change="onPageChange" layout="total, prev, pager, next, jumper" />
      </div>
    </div>

    <!-- 单条生成匹配对话框 -->
    <el-dialog v-model="createDialogVisible" title="生成人岗匹配" width="500px"
      append-to-body modal-class="match-create-overlay">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="选择岗位">
          <el-select v-model="createForm.jobId" placeholder="请选择岗位" style="width:100%;" :teleported="false">
            <el-option v-for="job in jobList" :key="job.id" :label="job.title + (job.companyName ? ' - ' + job.companyName : '')" :value="job.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择学生">
          <el-select v-model="createForm.studentId" placeholder="请选择学生" filterable style="width:100%;" :teleported="false">
            <el-option v-for="s in studentList" :key="s.id" :label="(s.realName || s.username) + (s.username ? ' (' + s.username + ')' : '')" :value="s.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createMatch" :loading="creating">确定</el-button>
      </template>
    </el-dialog>

    <!-- 横向对比对话框 -->
    <el-dialog v-model="compareDialogVisible" title="学生横向对比" width="700px" top="5vh"
      append-to-body modal-class="match-create-overlay" @opened="renderCompareChart" @closed="destroyCompareChart">
      <div v-if="compareData.length > 0" class="compare-body">
        <div ref="compareChartRef" class="compare-chart"></div>
        <el-divider />
        <div class="compare-table-wrapper">
          <table class="compare-table">
            <thead>
              <tr>
                <th>维度</th>
                <th v-for="s in compareData" :key="s.id">{{ s.name }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="dim in compareDims" :key="dim.key">
                <td>{{ dim.label }}</td>
                <td v-for="s in compareData" :key="s.id" :class="cellClass(s, dim)">{{ s[dim.key] }}</td>
              </tr>
              <tr>
                <td>总分</td>
                <td v-for="s in compareData" :key="s.id">{{ s.total }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <template #footer>
        <el-button @click="compareDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import { jobAPI, classAPI, jobMatchAPI, userAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { formatDate } from '@/utils/formatDate'
import * as echarts from 'echarts'

const userStore = useUserStore()

const loading = ref(false)
const generating = ref(false)
const creating = ref(false)
const pushingAll = ref(false)
const jobList = ref([])
const classList = ref([])
const studentList = ref([])
const matchList = ref([])
const matchStats = ref({ matched: 0, pushed: 0, viewed: 0 })
const selectedJobId = ref('')
const selectedClassId = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const createDialogVisible = ref(false)
const createForm = ref({ jobId: null, studentId: null })

// 模式切换
const matchMode = ref('job')
const selectedStudentId = ref('')
const selectedStudentObj = ref(null)
const studentModeStudents = ref([])
const selectedStudentClassId = ref('')

// 横向对比
const compareDialogVisible = ref(false)
const compareChartRef = ref(null)
const compareData = ref([])
const selectedCompare = ref([])
let compareChartInstance = null

const compareDims = [
  { key: 'skillMatch', label: '技能匹配' },
  { key: 'eduMatch', label: '学历匹配' },
  { key: 'expMatch', label: '经验匹配' },
  { key: 'majorFit', label: '专业契合' }
]

onMounted(async () => {
  await Promise.all([loadJobs(), loadClasses(), loadStudents()])
})

// 切换岗位时重新加载匹配结果（仅岗位模式）
watch(selectedJobId, () => {
  if (matchMode.value !== 'job') return
  currentPage.value = 1
  if (selectedJobId.value) loadResults()
})

const loadJobs = async () => {
  try {
    // 加载所有已发布岗位（和小程序一致）
    const res = await jobAPI.getJobs({ page: 1, size: 200, status: 1 })
    if (res.code === 200) {
      jobList.value = res.data?.list || res.data?.records || res.data || []
    }
  } catch (e) { console.error('加载岗位失败', e) }
}

const loadClasses = async () => {
  try {
    const res = await classAPI.getClasses({ page: 1, size: 200 })
    if (res.code === 200) {
      classList.value = res.data?.records || res.data || []
    }
  } catch (e) { console.error('加载班级失败', e) }
}

const loadStudents = async () => {
  try {
    const res = await userAPI.getUsersByRole(0)
    if (res.code === 200) {
      studentList.value = res.data || []
    }
  } catch (e) { console.error('加载学生列表失败', e) }
}

const generateMatch = async () => {
  if (!selectedJobId.value) { ElMessage.warning('请先选择岗位'); return }

  // 计算待匹配学生数量
  let studentCount = 0
  let classHint = ''
  if (selectedClassId.value) {
    const cls = classList.value.find(c => c.id === Number(selectedClassId.value))
    if (cls) {
      studentCount = cls.studentCount || 0
      classHint = '（' + cls.name + '）'
    }
  } else {
    // 未选班级 = 匹配所有有简历的学生
    studentCount = '全部'
  }

  // 确认对话框
  try {
    const msg = selectedClassId.value
      ? '即将为 ' + classHint + ' 的 ' + studentCount + ' 名学生生成人岗匹配\n每名学生约需 1-2 秒，预计耗时约 ' + Math.ceil(studentCount) + ' 秒，是否继续？'
      : '未选择班级，将匹配所有有简历的学生（数量可能较多）\n是否继续？'
    await ElMessageBox.confirm(msg, '批量匹配确认', {
      confirmButtonText: '开始匹配',
      cancelButtonText: '取消',
      type: 'info'
    })
  } catch (e) {
    return // 用户取消
  }

  // 如果已有匹配记录，询问是否覆盖
  if (total.value > 0) {
    try {
      await ElMessageBox.confirm(
        '当前岗位已有 ' + total.value + ' 条匹配记录，确定删除旧数据并重新匹配吗？',
        '重新匹配',
        { confirmButtonText: '确定覆盖', cancelButtonText: '取消', type: 'warning' }
      )
      await jobMatchAPI.deleteMatchesByJob(selectedJobId.value)
    } catch (e) {
      if (e !== 'cancel') ElMessage.error('删除旧记录失败')
      return
    }
  }
  generating.value = true
  try {
    const classParam = selectedClassId.value || undefined
    const res = await jobMatchAPI.batchGenerateMatches(selectedJobId.value, classParam)
    if (res.code === 200) {
      ElMessage.success('批量匹配成功，共生成 ' + (res.data?.generatedCount || '') + ' 条')
    } else {
      ElMessage.warning(res.message || '匹配生成完成，请刷新查看')
    }
    await loadResults()
  } catch (e) {
    ElMessage.warning('匹配生成可能已完成，请点击刷新结果查看')
    await loadResults()
  } finally {
    generating.value = false
  }
}

const showCreateDialog = () => {
  createForm.value = { jobId: selectedJobId.value, studentId: null }
  createDialogVisible.value = true
}

const createMatch = async () => {
  if (!createForm.value.jobId || !createForm.value.studentId) {
    ElMessage.warning('请选择岗位和学生'); return
  }
  creating.value = true
  try {
    const res = await jobMatchAPI.generateMatch(createForm.value)
    if (res.code === 200) {
      ElMessage.success('匹配生成成功')
      createDialogVisible.value = false
      await loadResults()
    } else {
      ElMessage.error(res.message || '生成失败')
    }
  } catch (e) {
    ElMessage.error('生成失败: ' + (e.message || '网络错误'))
  } finally {
    creating.value = false
  }
}

const loadResults = async () => {
  if (!selectedJobId.value) return
  loading.value = true
  try {
    // 使用 by-job 端点按岗位筛选并填充学生姓名，分页由前端处理
    const res = await jobMatchAPI.getMatchesByJob(selectedJobId.value)
    if (res.code === 200) {
      const allData = res.data || []
      const list = Array.isArray(allData) ? allData : []
      total.value = list.length
      // 用全量数据计算统计
      const fullList = list.map(m => ({
        ...m,
        studentName: m.studentName || m.student?.realName || m.student?.name || '',
        displayStudent: (m.studentName || m.student?.realName || m.student?.name || '未知') + (m.student?.username ? ' (' + m.student.username + ')' : ''),
        matchScore: typeof m.matchScore === 'number' ? m.matchScore : parseFloat(m.matchScore || 0),
        pushed: m.pushed || m.isPushed === 1 || m.isPushed === true,
        pushTime: m.pushTime || '',
        className: m.className || ''
      }))
      updateStats(fullList)
      // 前端分页
      const start = (currentPage.value - 1) * pageSize.value
      const paged = fullList.slice(start, start + pageSize.value)
      matchList.value = paged
    } else {
      matchList.value = []
      total.value = 0
    }
  } catch (e) {
    console.error('加载匹配结果失败', e)
    matchList.value = []
  } finally {
    loading.value = false
  }
}

// ===== 模式切换 =====
const switchMode = (mode) => {
  matchMode.value = mode
  matchList.value = []
  total.value = 0
  if (mode === 'student') {
    // 学生模式不显示分数分布
    matchStats.value = { matched: 0, pushed: 0, viewed: 0, avgScore: 0, distribution: { high: 0, mid: 0, low: 0, total: 0 } }
  } else if (selectedJobId.value) {
    loadResults()
  }
}

// ===== 学生模式 =====
const onStudentClassChange = async () => {
  selectedStudentId.value = ''
  selectedStudentObj.value = null
  studentModeStudents.value = []
  matchList.value = []
  total.value = 0
  if (!selectedStudentClassId.value) return
  try {
    const res = await classAPI.getClassStudents(selectedStudentClassId.value)
    if (res.code === 200) {
      const raw = res.data?.records || res.data || []
      studentModeStudents.value = Array.isArray(raw) ? raw : []
    }
  } catch (e) {
    console.error('加载班级学生失败', e)
  }
}

const batchMatchByStudent = async () => {
  if (!selectedStudentId.value) { ElMessage.warning('请先选择学生'); return }
  generating.value = true
  try {
    // 加载所有已发布岗位
    const jRes = await jobAPI.getJobs({ page: 1, size: 200, status: 1 })
    const jobs = jRes.data?.list || jRes.data?.records || jRes.data || []
    let count = 0
    for (const job of jobs) {
      try {
        await jobMatchAPI.generateMatch({ jobId: job.id, studentId: selectedStudentId.value })
        count++
      } catch (e) { /* 单个匹配失败跳过 */ }
    }
    ElMessage.success('匹配完成 ' + count + '/' + jobs.length + ' 个岗位')
    await loadStudentMatches(selectedStudentId.value)
  } catch (e) {
    ElMessage.error('匹配失败: ' + (e.message || '网络错误'))
  } finally {
    generating.value = false
  }
}

const loadStudentMatches = async (studentId) => {
  loading.value = true
  try {
    const mRes = await jobMatchAPI.getMatchesByStudent(studentId)
    if (mRes.code === 200) {
      const raw = mRes.data || []
      // 补充岗位标题和公司名
          const enriched = await Promise.all((Array.isArray(raw) ? raw : []).map(async (m) => {
            try {
              const jRes = await jobAPI.getJobDetail(m.jobId)
              const j = jRes.data || {}
              return {
                ...m,
                jobTitle: j.title || '岗位#' + m.jobId,
                companyName: j.companyName || '',
                matchScore: typeof m.matchScore === 'number' ? m.matchScore : parseFloat(m.matchScore || 0),
                pushed: m.pushed || m.isPushed === 1 || m.isPushed === true
              }
        } catch (e) {
          return { ...m, jobTitle: '岗位#' + m.jobId, companyName: '' }
        }
      }))
      // 按匹配度降序
      enriched.sort((a, b) => b.matchScore - a.matchScore)
      total.value = enriched.length
      const start = (currentPage.value - 1) * pageSize.value
      matchList.value = enriched.slice(start, start + pageSize.value)
      updateStats(enriched)
    }
  } catch (e) {
    console.error('加载学生匹配结果失败', e)
    matchList.value = []
  } finally {
    loading.value = false
  }
}

const onPageChange = () => {
  if (matchMode.value === 'job') {
    loadResults()
  } else {
    loadStudentMatches(selectedStudentId.value)
  }
}

const updateStats = (list) => {
  list = list || matchList.value
  const matched = list.length
  const pushed = list.filter(m => m.pushed).length
  const viewed = list.filter(m => m.viewed || m.isClicked).length
  // 平均匹配度
  let avgScore = 0
  if (matched > 0) {
    const sum = list.reduce((s, m) => s + matchPercent(m.matchScore), 0)
    avgScore = Math.round(sum / matched)
  }
  // 分数分布
  const high = list.filter(m => matchPercent(m.matchScore) >= 80).length
  const mid = list.filter(m => {
    const p = matchPercent(m.matchScore)
    return p >= 60 && p < 80
  }).length
  const low = list.filter(m => matchPercent(m.matchScore) < 60).length
  matchStats.value = {
    matched,
    pushed,
    viewed,
    avgScore,
    distribution: { high, mid, low, total: matched }
  }
}

const pushSingle = async (row) => {
  try {
    const res = await jobMatchAPI.pushMatch(row.id)
    if (res.code === 200) {
      row.pushed = true
      row.isPushed = 1
      updateStats()
      ElMessage.success('推送成功')
    }
  } catch (e) {
    ElMessage.error('推送失败')
  }
}

const pushAll = async () => {
  const unPushed = matchList.value.filter(m => !m.pushed)
  if (unPushed.length === 0) { ElMessage.info('没有未推送的匹配'); return }
  try {
    await ElMessageBox.confirm('确认批量推送 ' + unPushed.length + ' 条匹配给对应学生？', '提示')
    pushingAll.value = true
    for (const m of unPushed) { await pushSingle(m) }
    ElMessage.success('已成功推送 ' + unPushed.length + ' 条')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('批量推送失败')
  } finally {
    pushingAll.value = false
  }
}

const deleteMatch = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该条匹配记录？', '确认删除', { type: 'warning' })
    await jobMatchAPI.deleteMatch(row.id)
    ElMessage.success('删除成功')
    await loadResults()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// ===== 横向对比 =====
const onSelectionChange = (rows) => {
  selectedCompare.value = rows
}

const showCompare = () => {
  if (selectedCompare.value.length < 2) {
    ElMessage.warning('请至少选择 2 名学生进行对比')
    return
  }
  if (selectedCompare.value.length > 4) {
    ElMessage.warning('最多选择 4 名学生进行对比')
    return
  }
  compareData.value = selectedCompare.value.map(m => {
    const sd = m.scoreDetail || {}
    return {
      id: m.id,
      name: m.displayStudent || m.studentName || '未知',
      skillMatch: sd.skillMatch ?? '-',
      eduMatch: sd.eduMatch ?? '-',
      expMatch: sd.expMatch ?? '-',
      majorFit: sd.majorFit ?? '-',
      total: matchPercent(m.matchScore)
    }
  })
  compareDialogVisible.value = true
}

const renderCompareChart = () => {
  nextTick(() => {
    if (!compareChartRef.value || compareData.value.length < 2) return
    if (compareChartInstance) compareChartInstance.dispose()
    compareChartInstance = echarts.init(compareChartRef.value)
    const colors = ['#378ADD', '#10B981', '#F59E0B', '#7F77DD']
    const option = {
      tooltip: { trigger: 'item' },
      radar: {
        indicator: compareDims.map(d => ({ name: d.label, max: 100 })),
        center: ['50%', '55%'],
        radius: '65%',
        axisName: { color: '#4E5969', fontSize: 12 }
      },
      series: [{
        type: 'radar',
        data: compareData.value.map((s, i) => ({
          value: compareDims.map(d => s[d.key] === '-' ? 0 : s[d.key]),
          name: s.name,
          itemStyle: { color: colors[i % colors.length] },
          areaStyle: { color: colors[i % colors.length], opacity: 0.15 }
        }))
      }],
      legend: {
        data: compareData.value.map(s => s.name),
        bottom: 0,
        textStyle: { fontSize: 12, color: '#4E5969' }
      }
    }
    compareChartInstance.setOption(option)
    window.addEventListener('resize', resizeCompareChart)
  })
}

const resizeCompareChart = () => {
  if (compareChartInstance) compareChartInstance.resize()
}

const destroyCompareChart = () => {
  if (compareChartInstance) {
    compareChartInstance.dispose()
    compareChartInstance = null
  }
  window.removeEventListener('resize', resizeCompareChart)
}

const cellClass = (s, dim) => {
  const val = s[dim.key]
  if (val === '-') return 'dim-na'
  if (val >= 80) return 'dim-high'
  if (val >= 60) return 'dim-mid'
  return 'dim-low'
}

const matchPercent = (score) => Math.round((typeof score === 'number' ? score : parseFloat(score || 0)) * 100)

const scoreColor = (score) => {
  const p = matchPercent(score)
  if (p >= 80) return '#10B981'
  if (p >= 60) return '#F59E0B'
  return '#EF4444'
}

const dimLabel = (key) => {
  const labels = { skillMatch: '技能匹配', eduMatch: '学历匹配', expMatch: '经验匹配', majorFit: '专业契合' }
  return labels[key] || key
}

const distPct = (tier) => {
  const total = matchStats.value.distribution?.total || 0
  if (total === 0) return 0
  const count = matchStats.value.distribution[tier] || 0
  return Math.max(count / total * 100, count > 0 ? 5 : 0)
}
</script>

<style scoped>
.match-form { padding: 12px 0; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 24px; }
.stat-card { background: white; padding: 24px; border-radius: 16px; border-left: 6px solid; box-shadow: 0 6px 16px rgba(0,0,0,0.06); }
.stat-card h3 { margin: 0 0 8px; font-size: 14px; color: #86909C; font-weight: 500; }
.stat-card .num { font-size: 32px; font-weight: 700; color: #1D2129; }
.content-card { background: #fff; border-radius: 16px; padding: 24px; margin-bottom: 24px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); }
.content-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.content-card-title { font-size: 16px; font-weight: 600; color: #1D2129; }
.empty-state { text-align: center; padding: 40px 0; }
.mode-tabs { display: flex; background: #fff; border-radius: 12px; overflow: hidden; margin-bottom: 24px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); }
.mode-tab { flex: 1; text-align: center; padding: 14px 0; font-size: 14px; font-weight: 500; color: #86909C; cursor: pointer; transition: all 0.2s; }
.mode-tab.active { color: #165DFF; background: rgba(22,93,255,0.06); font-weight: 600; }
.mode-tab:hover:not(.active) { background: #F7F8FA; }
.dist-bar-wrapper { margin-top: 4px; }
.dist-bar { display: flex; height: 28px; border-radius: 6px; overflow: hidden; }
.dist-segment { display: flex; align-items: center; justify-content: center; font-size: 12px; color: #fff; font-weight: 500; transition: width 0.3s; }
.dist-high { background: #10B981; }
.dist-mid { background: #F59E0B; }
.dist-low { background: #EF4444; }
.dist-legend { display: flex; gap: 20px; margin-top: 8px; font-size: 12px; color: #86909C; }
.dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; vertical-align: middle; margin-right: 4px; }
.dot-high { background: #10B981; }
.dot-mid { background: #F59E0B; }
.dot-low { background: #EF4444; }
.compare-body { min-height: 300px; }
.compare-chart { width: 100%; height: 340px; }
.compare-table-wrapper { overflow-x: auto; }
.compare-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.compare-table th,
.compare-table td { padding: 8px 12px; text-align: center; border-bottom: 1px solid #E5E6EB; }
.compare-table th { background: #F7F8FA; color: #4E5969; font-weight: 500; }
.compare-table td.dim-high { color: #10B981; font-weight: 500; }
.compare-table td.dim-mid { color: #F59E0B; font-weight: 500; }
.compare-table td.dim-low { color: #EF4444; font-weight: 500; }
.compare-table td.dim-na { color: #C9CDD4; }
.compare-table tbody tr:hover { background: #F7F8FA; }
</style>

<style>
.match-create-overlay {
  position: fixed !important;
  top: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  left: 0 !important;
  background: rgba(0, 0, 0, 0.45) !important;
  z-index: 9999 !important;
}
.dimension-popover { padding: 4px 0; }
.dimension-title { font-size: 13px; font-weight: 500; color: #1D2129; margin-bottom: 10px; }
.dimension-row { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.dimension-row:last-child { margin-bottom: 0; }
.dimension-label { font-size: 12px; color: #4E5969; width: 60px; flex-shrink: 0; text-align: right; }
</style>
