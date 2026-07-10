<template>
  <div class="hr-resume-manage fade-in">
    <div class="page-header">
      <h2>简历管理</h2>
      <p>查看学生简历 · AI 评分分析</p>
    </div>
    <div class="card">
      <!-- 操作栏 -->
      <el-row class="operation-row" :gutter="12">
        <el-col :span="10">
          <el-select v-model="filterJobId" placeholder="按岗位筛选" clearable style="width: 220px; margin-right: 10px;">
            <el-option v-for="job in jobOptions" :key="job.id" :label="job.title" :value="job.id" />
          </el-select>
          <el-select v-model="filterStatus" placeholder="投递状态" clearable style="width: 140px; margin-right: 10px;">
            <el-option label="已投递" :value="0" />
            <el-option label="企业已查看" :value="1" />
            <el-option label="待面试" :value="2" />
            <el-option label="已录用" :value="3" />
            <el-option label="不合适" :value="4" />
          </el-select>
          <el-button type="primary" @click="loadDeliveries">查询</el-button>
        </el-col>
        <el-col :span="14" style="text-align: right;">
          <el-button type="primary" @click="exportToExcel">导出CSV</el-button>
        </el-col>
      </el-row>

      <!-- 表格 -->
      <el-table :data="deliveryList" stripe v-loading="loading" style="width: 100%;">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="studentName" label="学生姓名" width="110" />
        <el-table-column prop="jobTitle" label="应聘岗位" min-width="140" />
        <el-table-column label="AI评分" width="150">
          <template #default="{ row }">
            <el-progress v-if="row.score" :percentage="Math.round(row.score)" :color="scoreColor(row.score)" />
            <el-tag v-else type="info">未评分</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="投递时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime, { showSeconds: true }) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button size="small" @click="viewResume(row)">简历</el-button>
            <el-button size="small" type="primary" @click="showScoreDialog(row)">评分</el-button>
            <el-button size="small" type="success" v-if="row.status === 0 || row.status === 1" @click="updateStatus(row, 2)">邀约</el-button>
            <el-button size="small" type="danger" v-if="row.status !== 4 && row.status !== 3" @click="updateStatus(row, 4)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div style="margin-top: 20px; text-align: center;">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" @current-change="loadDeliveries" layout="total, prev, pager, next, jumper" />
      </div>
    </div>

    <!-- 简历详情对话框 -->
    <ResumeDetailDialog
      v-model:visible="viewDialogVisible"
      :student-id="viewStudentId"
      mode="hr"
      :hr-score="currentHrScore"
      :hr-level="currentHrLevel"
      :hr-comment="currentHrComment"
      :hr-dims="currentHrDims"
    />

    <!-- 评分对话框 -->
    <el-dialog v-model="scoreDialogVisible" title="简历评分" width="520px"
      append-to-body modal-class="batch-score-overlay">
      <div v-if="scoreData.loading" style="text-align:center;padding:40px 0;color:#86909C;">
        <el-icon class="is-loading" style="font-size:28px;margin-bottom:12px;"><i class="el-icon-loading" /></el-icon>
        <p style="margin:0;font-size:14px;">AI 分析中，请稍候...</p>
      </div>
      <div v-else>
        <!-- 岗位信息 -->
        <div style="margin-bottom:18px;">
          <span style="font-size:13px;color:#86909C;">岗位：</span>
          <span style="font-weight:600;font-size:14px;color:#1D2129;">{{ scoreData.jobTitle }}</span>
        </div>

        <!-- AI 综合评分 -->
        <div class="score-summary-card">
          <div style="display:flex;align-items:center;gap:16px;">
            <div class="score-big-circle" :style="{ borderColor: scoreColor(scoreData.total) }">
              <span class="score-big-num" :style="{ color: scoreColor(scoreData.total) }">{{ scoreData.total }}</span>
            </div>
            <div style="flex:1;">
              <div style="font-size:15px;font-weight:700;color:#1D2129;margin-bottom:4px;">
                AI 综合评分
                <span v-if="scoreData.isAiGenerated" style="font-size:11px;color:#86909C;font-weight:400;margin-left:6px;">(AI 生成)</span>
              </div>
              <el-progress :percentage="scoreData.total" :stroke-width="10"
                :color="scoreColor(scoreData.total)" :show-text="false" />
            </div>
          </div>
        </div>

        <!-- 三维度评分 -->
        <div class="dims-section">
          <div class="dim-row" v-for="dim in scoreData.dims" :key="dim.key">
            <div class="dim-label">{{ dim.label }}</div>
            <el-progress :percentage="dim.score" :stroke-width="8"
              :color="scoreColor(dim.score)" :show-text="false" />
            <span class="dim-val" :style="{ color: scoreColor(dim.score) }">{{ dim.score }}</span>
          </div>
        </div>

        <!-- 评语 -->
        <div v-if="scoreData.comment" class="comment-box">
          <div style="font-size:13px;font-weight:600;color:#4E5969;margin-bottom:6px;">评语</div>
          <p style="margin:0;font-size:13px;color:#86909C;line-height:1.6;">{{ scoreData.comment }}</p>
        </div>
        <div v-else class="comment-box comment-box--empty">
          <p style="margin:0;font-size:13px;color:#C9CDD4;">暂无评语</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="scoreDialogVisible = false">关闭</el-button>
        <el-button v-if="!scoreData.loading" type="primary" :loading="scoreData.scoring" @click="handleRescore">
          重新 AI 评分
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user.js'
import { deliveryAPI, resumeScoreAPI, jobAPI } from '@/api/index.js'
import ResumeDetailDialog from '@/components/ResumeDetailDialog.vue'
import { formatDate } from '@/utils/formatDate'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const companyId = computed(() => userStore.companyId)

const deliveryList = ref([])
const loading = ref(false)
const filterJobId = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const jobOptions = ref([])

const viewDialogVisible = ref(false)
const scoreDialogVisible = ref(false)
const viewStudentId = ref(null)
const currentDelivery = ref(null)

const currentHrScore = ref(null)
const currentHrLevel = ref('')
const currentHrComment = ref('')
const currentHrDims = ref([])

// 评分弹窗数据
const scoreData = ref({
  jobTitle: '',
  total: 0,
  dims: [],
  comment: '',
  deliveryId: null,
  jobId: null,
  scoreLogId: null,
  isAiGenerated: false,
  loading: false,
  scoring: false
})

onMounted(() => {
  loadJobOptions()
  loadDeliveries()
})

const loadJobOptions = async () => {
  if (!companyId.value) return
  try {
    const res = await jobAPI.getJobsByCompany(companyId.value, {})
    if (res.code === 200) jobOptions.value = res.data || []
  } catch (e) { console.error('加载岗位列表失败', e) }
}

const loadDeliveries = async () => {
  if (!companyId.value) { ElMessage.warning('未关联企业信息'); return }
  loading.value = true
  try {
    // Try primary path: get all jobs for this company, then aggregate deliveries
    const jobsRes = await jobAPI.getJobsByCompany(companyId.value, {})
    if (jobsRes.code === 200 && Array.isArray(jobsRes.data)) {
      let allDeliveries = []
      for (const job of jobsRes.data) {
        const dRes = await deliveryAPI.getDeliveriesByJob(job.id)
        if (dRes.code === 200 && Array.isArray(dRes.data)) {
          allDeliveries = allDeliveries.concat(dRes.data.map(d => ({
            ...d,
            jobTitle: job.title,
            companyId: job.companyId
          })))
        }
      }
      // Apply filters
      if (filterJobId.value) {
        allDeliveries = allDeliveries.filter(d => d.jobId === filterJobId.value)
      }
      if (filterStatus.value !== '' && filterStatus.value !== null) {
        allDeliveries = allDeliveries.filter(d => d.status === filterStatus.value)
      }
      // Paginate
      const start = (currentPage.value - 1) * pageSize.value
      deliveryList.value = allDeliveries.slice(start, start + pageSize.value)
      total.value = allDeliveries.length
    }
  } catch (e) {
    ElMessage.error('加载投递列表失败: ' + (e.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

const viewResume = async (row) => {
  if (!row.studentId) { ElMessage.warning('缺少学生信息'); return }
  viewStudentId.value = row.studentId
  // 重置 HR 评分数据
  currentHrScore.value = null
  currentHrLevel.value = ''
  currentHrComment.value = ''
  currentHrDims.value = []
  // 尝试加载评分维度
  if (row.id) {
    try {
      const sr = await resumeScoreAPI.getByDelivery(row.id)
      if (sr.code === 200 && sr.data) {
        fillHrScoreData(sr.data)
      }
    } catch (e) {
      // 没有评分 → 自动触发单条 AI 评分（1 token）
      if (row.jobId && row.id) {
        try {
          const scoreRes = await resumeScoreAPI.scoreResume({ jobId: row.jobId, deliveryId: row.id })
          if (scoreRes.code === 200) {
            // 评分成功后重新加载
            const sr2 = await resumeScoreAPI.getByDelivery(row.id)
            if (sr2.code === 200 && sr2.data) {
              fillHrScoreData(sr2.data)
            }
          }
        } catch (e2) { /* auto score failed */ }
      }
    }
  }
  // 投递记录行中已有的 score 作为兜底
  if (currentHrScore.value === null && row.score) {
    currentHrScore.value = Math.round(row.score)
  }
  viewDialogVisible.value = true
}

// 填充 HR 评分数据到 dialog 状态和维度数据
const fillHrScoreData = (data) => {
  let sd = data.scoreDetail || data
  if (typeof sd === 'string') {
    try { sd = JSON.parse(sd) } catch (e) { sd = {} }
  }
  if (sd && typeof sd === 'object') {
    currentHrScore.value = data.score || sd.总分 || sd.totalScore || null
    currentHrComment.value = sd.评语 || sd.comment || data.analysis || ''
    const dims = []
    const dimMap = { 技能得分: '技能匹配', 经验得分: '经验匹配', 教育得分: '学历匹配' }
    for (const key of Object.keys(dimMap)) {
      if (sd[key] !== undefined && sd[key] !== null) {
        dims.push({ label: dimMap[key], score: Math.round(sd[key]) })
      }
    }
    if (dims.length) currentHrDims.value = dims
  }
}

const showScoreDialog = async (row) => {
  currentDelivery.value = row
  // 显示 loading，准备数据
  scoreData.value = {
    jobTitle: row.jobTitle || '',
    total: 0,
    dims: [],
    comment: '',
    deliveryId: row.id,
    jobId: row.jobId,
    scoreLogId: null,
    isAiGenerated: false,
    loading: true,
    scoring: false
  }
  scoreDialogVisible.value = true
  // 加载现有评分
  if (row.id) {
    try {
      const sr = await resumeScoreAPI.getByDelivery(row.id)
      if (sr.code === 200 && sr.data) {
        const data = sr.data
        let sd = data.scoreDetail || data
        if (typeof sd === 'string') {
          try { sd = JSON.parse(sd) } catch (e) { sd = {} }
        }
        const dims = []
        const dimMap = { 技能得分: '技能匹配', 经验得分: '经验匹配', 教育得分: '学历匹配' }
        for (const key of Object.keys(dimMap)) {
          if (sd[key] !== undefined && sd[key] !== null) {
            dims.push({ key, label: dimMap[key], score: Math.round(sd[key]) })
          }
        }
        scoreData.value.total = data.score || 0
        scoreData.value.dims = dims
        scoreData.value.comment = sd.评语 || sd.comment || data.analysis || ''
        scoreData.value.scoreLogId = data.id || null
        scoreData.value.isAiGenerated = true
      }
    } catch (e) { /* no existing score */ }
  }
  scoreData.value.loading = false
}

const handleRescore = async () => {
  if (!scoreData.value.scoreLogId) {
    // 还没有评分记录 → 直接调用评分
    if (!scoreData.value.jobId || !scoreData.value.deliveryId) {
      ElMessage.warning('缺少评分信息')
      return
    }
    scoreData.value.scoring = true
    try {
      const res = await resumeScoreAPI.scoreResume({
        jobId: scoreData.value.jobId,
        deliveryId: scoreData.value.deliveryId
      })
      if (res.code === 200) {
        ElMessage.success('AI 评分完成')
        // 重新加载数据
        await showScoreDialog(currentDelivery.value)
        // 刷新表格
        loadDeliveries()
      } else {
        ElMessage.error(res.message || '评分失败')
      }
    } catch (e) {
      ElMessage.error('评分失败: ' + (e.message || '网络错误'))
    } finally {
      scoreData.value.scoring = false
    }
    return
  }
  // 已有评分记录 → 重新评分
  scoreData.value.scoring = true
  try {
    const res = await resumeScoreAPI.rescoreResume(scoreData.value.scoreLogId)
    if (res.code === 200) {
      ElMessage.success('重新评分完成')
      // 重新加载数据
      await showScoreDialog(currentDelivery.value)
      // 刷新表格
      loadDeliveries()
    } else {
      ElMessage.error(res.message || '重新评分失败')
    }
  } catch (e) {
    ElMessage.error('重新评分失败: ' + (e.message || '网络错误'))
  } finally {
    scoreData.value.scoring = false
  }
}

const updateStatus = async (row, status) => {
  try {
    const res = await deliveryAPI.updateDeliveryStatus(row.id, status)
    if (res.code === 200) {
      ElMessage.success('状态更新成功')
      loadDeliveries()
    } else { ElMessage.error(res.message || '状态更新失败') }
  } catch (e) { ElMessage.error('状态更新失败: ' + (e.message || '网络错误')) }
}

const exportToExcel = () => {
  if (!deliveryList.value.length) { ElMessage.warning('没有数据可导出'); return }
  const header = '学生姓名,应聘岗位,AI评分,状态,投递时间\n'
  const rows = deliveryList.value.map(r =>
    (r.studentName || '') + ',' + (r.jobTitle || '') + ',' + (r.score || 0) + ',' + statusText(r.status) + ',' + (r.createTime || '')
  ).join('\n')
  const BOM = '\uFEFF'
  const blob = new Blob([BOM + header + rows], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = '投递记录_' + new Date().toISOString().slice(0, 10) + '.csv'
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

const statusText = (s) => ({ 0: '已投递', 1: '企业已查看', 2: '待面试', 3: '已录用', 4: '不合适' })[s] || '未知'
const statusTagType = (s) => ({ 0: 'info', 1: '', 2: 'warning', 3: 'success', 4: 'danger' })[s] || 'info'
const scoreColor = (s) => s >= 90 ? '#67C23A' : (s >= 75 ? '#E6A23C' : '#F56C6C')
</script>

<style scoped>
.hr-resume-manage { padding: 20px; }
.card { background: white; border-radius: 16px; padding: 28px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); position: relative; }
.card::before { content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 3px; background: linear-gradient(90deg,#165DFF,#2563EB,transparent); border-radius: 16px 16px 0 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.card h2 { font-size: 19px; font-weight: 600; padding-bottom: 12px; border-bottom: 1px solid #F2F3F5; position: relative; }
.card h2::after { content: ''; width: 50px; height: 3px; background: #165DFF; border-radius: 3px; position: absolute; left: 0; bottom: -1px; }
.operation-row { margin-bottom: 20px; }

/* 评分弹窗样式 */
.score-summary-card {
  background: #F9FAFB;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
}
.score-big-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: 3px solid;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.score-big-num {
  font-size: 24px;
  font-weight: 800;
  line-height: 1;
}
.dims-section {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
  border: 1px solid #F2F3F5;
}
.dims-section .dim-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.dims-section .dim-row:last-child { margin-bottom: 0; }
.dims-section .dim-label {
  font-size: 13px;
  color: #4E5969;
  width: 72px;
  flex-shrink: 0;
}
.dims-section .el-progress { flex: 1; }
.dims-section .dim-val {
  font-size: 13px;
  font-weight: 700;
  width: 28px;
  text-align: right;
  flex-shrink: 0;
}
.comment-box {
  background: #F9FAFB;
  border-radius: 10px;
  padding: 14px 18px;
  margin-bottom: 8px;
}
.comment-box--empty { border: 1px dashed #E5E6EB; }
</style>

<style>
.batch-score-overlay {
  position: fixed !important;
  top: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  left: 0 !important;
  background: rgba(0, 0, 0, 0.45) !important;
  z-index: 9999 !important;
}
</style>
