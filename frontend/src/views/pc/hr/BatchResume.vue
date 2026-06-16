<template>
  <div class="hr-resume-manage">
    <div class="card">
      <div class="card-header">
        <h2>简历管理</h2>
      </div>

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
          <el-button type="success" @click="batchScore" :loading="batchScoring">批量AI评分</el-button>
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
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="投递时间" width="170">
          <template #default="{ row }">{{ row.createTime ? row.createTime.substring(0,19).replace('T', ' ') : '-' }}</template>
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
    <el-dialog v-model="viewDialogVisible" title="简历详情" width="800px" :close-on-click-modal="false">
      <div v-if="currentResume">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="姓名">{{ currentResume.name || currentResume.realName }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ currentResume.phone }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentResume.email }}</el-descriptions-item>
          <el-descriptions-item label="学历">{{ currentResume.education }}</el-descriptions-item>
          <el-descriptions-item label="求职意向" :span="2">{{ currentResume.jobTarget }}</el-descriptions-item>
          <el-descriptions-item label="技能标签" :span="2">
            <el-tag v-for="tag in resumeTags" :key="tag" style="margin-right: 5px; margin-bottom: 3px;">{{ tag }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="自我评价" :span="2">{{ currentResume.selfEvaluation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="工作经历" :span="2">{{ currentResume.workExperience || '-' }}</el-descriptions-item>
          <el-descriptions-item label="项目经历" :span="2">{{ currentResume.projectExperience || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 评分对话框 -->
    <el-dialog v-model="scoreDialogVisible" title="简历评分" width="500px">
      <el-form :model="scoreForm" label-width="100px">
        <el-form-item label="岗位">
          <el-input :value="scoreForm.jobTitle" disabled />
        </el-form-item>
        <el-form-item label="AI评分">
          <el-slider v-model="scoreForm.score" :min="0" :max="100" show-input />
        </el-form-item>
        <el-form-item label="评价">
          <el-input v-model="scoreForm.analysis" type="textarea" :rows="4" placeholder="AI自动生成评价或手动输入" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scoreDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitScore">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user.js'
import { deliveryAPI, resumeAPI, resumeScoreAPI, jobAPI } from '@/api/index.js'
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
const batchScoring = ref(false)

const viewDialogVisible = ref(false)
const scoreDialogVisible = ref(false)
const currentResume = ref(null)
const currentDelivery = ref(null)

const scoreForm = ref({ jobId: null, jobTitle: '', score: 0, analysis: '' })

const resumeTags = computed(() => {
  if (!currentResume.value) return []
  const tags = currentResume.value.skillTags || currentResume.value.skills || ''
  return tags.split(/[,，\/]/).map(t => t.trim()).filter(Boolean)
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
  try {
    const res = await resumeAPI.getResumeByStudent(row.studentId)
    if (res.code === 200) {
      currentResume.value = res.data
      viewDialogVisible.value = true
    } else {
      ElMessage.info('该学生暂无简历')
    }
  } catch (e) { ElMessage.error('加载简历失败: ' + (e.message || '网络错误')) }
}

const showScoreDialog = (row) => {
  scoreForm.value = { jobId: row.jobId, jobTitle: row.jobTitle, score: row.score || 60, analysis: row.analysis || '' }
  currentDelivery.value = row
  scoreDialogVisible.value = true
}

const submitScore = async () => {
  try {
    const res = await resumeScoreAPI.scoreResume({
      jobId: scoreForm.value.jobId,
      deliveryId: currentDelivery.value.id,
      score: scoreForm.value.score,
      analysis: scoreForm.value.analysis
    })
    if (res.code === 200) {
      ElMessage.success('评分成功')
      scoreDialogVisible.value = false
      loadDeliveries()
    } else { ElMessage.error(res.message || '评分失败') }
  } catch (e) { ElMessage.error('评分失败: ' + (e.message || '网络错误')) }
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

const batchScore = async () => {
  if (!companyId.value) { ElMessage.warning('未关联企业信息'); return }
  batchScoring.value = true
  try {
    const res = await resumeScoreAPI.batchScoreByCompany(companyId.value)
    if (res.code === 200) {
      ElMessage.success('批量AI评分完成')
      loadDeliveries()
    } else { ElMessage.error(res.message || '批量评分失败') }
  } catch (e) {
    // Fallback: score per job
    try {
      const jobsRes = await jobAPI.getJobsByCompany(companyId.value, {})
      if (jobsRes.code === 200 && Array.isArray(jobsRes.data)) {
        for (const job of jobsRes.data) {
          await resumeScoreAPI.batchScoreResumes(job.id)
        }
        ElMessage.success('批量评分完成')
        loadDeliveries()
      }
    } catch (e2) { ElMessage.error('批量评分失败: ' + (e2.message || '网络错误')) }
  } finally { batchScoring.value = false }
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
</style>
