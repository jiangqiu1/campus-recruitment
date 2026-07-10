<template>
  <div class="candidate-manage fade-in">
    <div class="page-header">
      <h2>候选人管理</h2>
      <p>按岗位查看候选人 · 安排面试 · 批量处理</p>
    </div>

    <div class="content-card">
      <div class="content-card-header">
        <span class="content-card-title">筛选条件</span>
      </div>
      <el-form :inline="true" class="search-form">
        <el-form-item label="选择岗位">
          <el-select v-model="selectedJobId" placeholder="全部岗位" filterable clearable style="width:280px" @change="onJobChange">
            <el-option v-for="j in jobList" :key="j.id" :label="j.title" :value="j.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable style="width:130px" @change="filterCandidates">
            <el-option label="待查看" value="0" />
            <el-option label="已查看" value="1" />
            <el-option label="面试中" value="2" />
            <el-option label="已录用" value="3" />
            <el-option label="未通过" value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadCandidates">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="stat-grid" v-if="stats">
      <div class="stat-card" style="border-left-color:#165DFF">
        <h3>总投递</h3>
        <div class="num">{{ stats.total }}</div>
      </div>
      <div class="stat-card" style="border-left-color:#10B981">
        <h3>已录用</h3>
        <div class="num">{{ stats.hired }}</div>
      </div>
      <div class="stat-card" style="border-left-color:#F59E0B">
        <h3>面试中</h3>
        <div class="num">{{ stats.interviewing }}</div>
      </div>
      <div class="stat-card" style="border-left-color:#EF4444">
        <h3>未通过</h3>
        <div class="num">{{ stats.rejected }}</div>
      </div>
    </div>

    <div class="content-card">
      <div class="content-card-header">
        <span class="content-card-title">候选人列表</span>
        <div>
          <span v-if="selectedCandidates.length > 0" style="margin-right:8px;color:#86909C;font-size:13px;">
            已选 {{ selectedCandidates.length }} 人
          </span>
          <el-button v-if="selectedJobId" size="small" type="success" :loading="batchScoring" :disabled="selectedCandidates.length === 0" @click="batchScore">
            为选中候选人AI评分
          </el-button>
        </div>
      </div>
      <el-table v-loading="loading" :data="candidateList" style="width:100%" stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="45" />
        <el-table-column type="index" label="#" width="50" />
        <el-table-column label="学生姓名" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.displayName }}</template>
        </el-table-column>
        <el-table-column prop="jobTitle" label="投递岗位" min-width="160" show-overflow-tooltip />
        <el-table-column label="AI 评分" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.score" :type="scoreTag(row.score)" size="small">{{ row.score }}</el-tag>
            <span v-else class="no-data">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="投递时间" width="155">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewResume(row)">查看简历</el-button>
            <el-button v-if="row.status === 0" type="primary" link size="small" @click="markViewed(row)">标记查看</el-button>
            <el-button v-if="row.status <= 1" type="primary" link size="small" @click="arrangeInterview(row)">安排面试</el-button>
            <el-button v-if="row.status === 2" type="success" link size="small" @click="markHired(row)">录用</el-button>
            <el-button v-if="row.status <= 2" type="danger" link size="small" @click="markRejected(row)">不合适</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && candidateList.length === 0" class="empty-state">
        <el-empty :image-size="100" description="暂无候选人" />
      </div>
    </div>

    <!-- 安排面试对话框 -->
    <el-dialog v-model="interviewVisible" title="安排面试" width="500px">
      <el-form :model="interviewForm" label-width="80px">
        <el-form-item label="候选人">
          <span style="font-weight:500">{{ interviewForm.studentName }}</span>
        </el-form-item>
        <el-form-item label="面试时间" required>
          <el-date-picker v-model="interviewForm.time" type="datetime" placeholder="选择面试时间" style="width:100%"
            value-format="YYYY-MM-DD HH:mm" />
        </el-form-item>
        <el-form-item label="面试方式">
          <el-select v-model="interviewForm.method" style="width:100%">
            <el-option label="线下面试" value="线下" />
            <el-option label="视频面试" value="视频" />
            <el-option label="电话面试" value="电话" />
          </el-select>
        </el-form-item>
        <el-form-item label="面试地点">
          <el-input v-model="interviewForm.location" placeholder="如：公司会议室A" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="interviewForm.remark" type="textarea" :rows="3" placeholder="其他补充说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="interviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="arranging" @click="confirmInterview">确认安排</el-button>
      </template>
    </el-dialog>

    <!-- 简历详情 -->
    <ResumeDetailDialog
      v-model:visible="resumeVisible"
      :student-id="currentStudentId"
      mode="hr"
      :hr-score="currentHrScore"
      :hr-level="currentHrLevel"
      :hr-comment="currentHrComment"
      :hr-dims="currentHrDims"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { jobAPI, deliveryAPI, resumeScoreAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import ResumeDetailDialog from '@/components/ResumeDetailDialog.vue'
import { formatDate } from '@/utils/formatDate'

const userStore = useUserStore()
const loading = ref(false)
const batchScoring = ref(false)
const jobList = ref([])
const allCandidates = ref([])
const candidateList = ref([])
const stats = ref(null)
const selectedJobId = ref('')
const statusFilter = ref('')
const interviewVisible = ref(false)
const resumeVisible = ref(false)
const currentStudentId = ref('')
const currentHrScore = ref(null)
const currentHrLevel = ref('')
const currentHrComment = ref('')
const currentHrDims = ref([])
const arranging = ref(false)

const currentDeliveryId = ref(null)
const selectedCandidates = ref([])
const interviewForm = ref({
  studentName: '', time: '', method: '线下', location: '', remark: ''
})

onMounted(async () => {
  await loadJobs()
  await loadCandidates()
})

const loadJobs = async () => {
  try {
    const res = await jobAPI.getJobsByCompany(userStore.companyId, { page: 1, size: 200 })
    if (res.code === 200) {
      jobList.value = res.data?.records || res.data || []
    }
  } catch (e) { console.error('加载岗位失败', e) }
}

const onJobChange = () => {
  statusFilter.value = ''
  loadCandidates()
}

const filterCandidates = () => {
  let list = allCandidates.value
  if (statusFilter.value !== '') {
    list = list.filter(d => d.status === parseInt(statusFilter.value))
  }
  candidateList.value = list
}

const viewResume = async (row) => {
  currentStudentId.value = row.studentId
  currentHrScore.value = row.score || null
  currentHrLevel.value = ''
  currentHrComment.value = ''
  currentHrDims.value = []
  resumeVisible.value = true
  // 异步加载评分维度详情
  if (row.id) {
    try {
      const res = await resumeScoreAPI.getByDelivery(row.id)
      if (res.code === 200 && res.data) {
        const data = res.data
        if (data.score) currentHrScore.value = data.score
        if (data.scoreDetail) {
          try {
            const detail = typeof data.scoreDetail === 'string' ? JSON.parse(data.scoreDetail) : data.scoreDetail
            const dims = []
            const labelMap = { '技能得分': '技能匹配', '经验得分': '经验匹配', '教育得分': '学历匹配' }
            for (const [key, val] of Object.entries(detail)) {
              if (key === '评语') {
                currentHrComment.value = val
              } else {
                dims.push({
                  label: labelMap[key] || key,
                  score: typeof val === 'number' ? val : parseInt(val) || 0
                })
              }
            }
            if (dims.length) currentHrDims.value = dims
          } catch (e) { /* parse error */ }
        }
      }
    } catch (e) { /* no score log */ }
  }
}

const loadCandidates = async () => {
  loading.value = true
  try {
    const res = selectedJobId.value
      ? await deliveryAPI.getDeliveriesByJob(selectedJobId.value)
      : await deliveryAPI.getDeliveriesByCompany(userStore.companyId)
    if (res.code === 200) {
      allCandidates.value = (res.data || []).map(d => ({
        ...d,
        studentName: d.studentName || '',
        studentUsername: d.studentUsername || d.username || '',
        displayName: (d.studentName || '未知') + (d.studentUsername || d.username ? ' (' + (d.studentUsername || d.username) + ')' : ''),
        studentId: d.studentId || '',
        jobTitle: d.jobTitle || '',
        score: d.score ?? null,
        createTime: d.createTime || d.deliveryTime,
        status: d.status !== undefined ? d.status : 0
      }))
      computeStats()
      filterCandidates()
    }
  } catch (e) {
    console.error('加载候选人失败', e)
  } finally {
    loading.value = false
  }
}

const computeStats = () => {
  const list = allCandidates.value
  stats.value = {
    total: list.length,
    hired: list.filter(d => d.status === 3).length,
    interviewing: list.filter(d => d.status === 2).length,
    rejected: list.filter(d => d.status === 4 || d.status === 5).length
  }
}

const onSelectionChange = (selection) => {
  selectedCandidates.value = selection
}

const batchScore = async () => {
  if (selectedCandidates.value.length === 0) {
    ElMessage.warning('请先勾选需要评分的候选人')
    return
  }
  batchScoring.value = true
  let success = 0
  let fail = 0
  for (const c of selectedCandidates.value) {
    try {
      const res = await resumeScoreAPI.scoreResume({
        deliveryId: c.id,
        studentId: c.studentId,
        jobId: selectedJobId.value
      })
      if (res.code === 200) success++
      else fail++
    } catch (e) { fail++ }
  }
  ElMessage.success(`评分完成：${success}人成功${fail > 0 ? `，${fail}人失败` : ''}`)
  await loadCandidates()
  batchScoring.value = false
  selectedCandidates.value = []
}

const markViewed = async (row) => {
  try {
    const res = await deliveryAPI.updateDeliveryStatus(row.id, 1)
    if (res.code === 200) {
      row.status = 1
      computeStats()
      filterCandidates()
      ElMessage.success('已标记查看')
    }
  } catch (e) { ElMessage.error('操作失败') }
}

const arrangeInterview = (row) => {
  currentDeliveryId.value = row.id
  interviewForm.value = { studentName: row.studentName, time: '', method: '线下', location: '', remark: '' }
  interviewVisible.value = true
}

const confirmInterview = async () => {
  if (!interviewForm.value.time) {
    ElMessage.warning('请选择面试时间')
    return
  }
  arranging.value = true
  try {
    const data = {
      interviewTime: interviewForm.value.time,
      method: interviewForm.value.method,
      location: interviewForm.value.location,
      remark: interviewForm.value.remark
    }
    const res = await deliveryAPI.arrangeInterview(currentDeliveryId.value, data)
    if (res.code === 200) {
      ElMessage.success('面试已安排')
      interviewVisible.value = false
      await loadCandidates()
    }
  } catch (e) {
    ElMessage.error('安排失败')
  } finally {
    arranging.value = false
  }
}

const markHired = async (row) => {
  try {
    await ElMessageBox.confirm(`确认录用「${row.studentName}」？`)
    const res = await deliveryAPI.updateDeliveryStatus(row.id, 3)
    if (res.code === 200) {
      row.status = 3
      computeStats()
      filterCandidates()
      ElMessage.success('已录用')
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const markRejected = async (row) => {
  try {
    await ElMessageBox.confirm(`确认标记「${row.studentName}」为不合适？`)
    const res = await deliveryAPI.updateDeliveryStatus(row.id, 4)
    if (res.code === 200) {
      row.status = 4
      computeStats()
      filterCandidates()
      ElMessage.success('已标记')
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const statusTag = (s) => {
  const map = { 0: 'info', 1: 'primary', 2: 'warning', 3: 'success', 4: 'danger', 5: 'info' }
  return map[s] || 'info'
}
const statusLabel = (s) => {
  const map = { 0: '待查看', 1: '已查看', 2: '面试中', 3: '已录用', 4: '未通过', 5: '不合适' }
  return map[s] || '未知'
}
const scoreTag = (s) => {
  if (s >= 80) return 'success'
  if (s >= 60) return 'warning'
  return 'danger'
}
const formatTime = (t) => formatDate(t)
</script>

<style scoped>
.search-form { padding: 12px 0; }
.content-card { background: #fff; border-radius: 16px; padding: 24px; margin-bottom: 24px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); }
.content-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.content-card-title { font-size: 16px; font-weight: 600; color: #1D2129; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 28px; }
.stat-card { background: white; padding: 24px; border-radius: 16px; border-left: 6px solid; box-shadow: 0 6px 16px rgba(0,0,0,0.06); }
.stat-card h3 { margin: 0 0 8px; font-size: 14px; color: #86909C; font-weight: 500; }
.stat-card .num { font-size: 32px; font-weight: 700; color: #1D2129; }
.empty-state { padding: 40px 0; display: flex; justify-content: center; }
.no-data { padding: 20px; text-align: center; color: #86909C; font-size: 14px; }
</style>
