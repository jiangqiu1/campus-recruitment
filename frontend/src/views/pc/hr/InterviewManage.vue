<template>
  <div class="interview-manage fade-in">
    <div class="page-header">
      <h2>面试管理</h2>
      <p>统一管理所有面试安排</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div class="stat-card" style="border-left-color:#165DFF">
        <h3>今日面试</h3>
        <div class="num">{{ stats.today }}</div>
      </div>
      <div class="stat-card" style="border-left-color:#10B981">
        <h3>待面试</h3>
        <div class="num">{{ stats.pending }}</div>
      </div>
      <div class="stat-card" style="border-left-color:#F59E0B">
        <h3>已完成</h3>
        <div class="num">{{ stats.completed }}</div>
      </div>
      <div class="stat-card" style="border-left-color:#7F77DD">
        <h3>总计</h3>
        <div class="num">{{ stats.total }}</div>
      </div>
    </div>

    <div class="content-card">
      <div class="content-card-header">
        <span class="content-card-title">面试列表</span>
        <el-radio-group v-model="dateFilter" @change="loadInterviews">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="today">今天</el-radio-button>
          <el-radio-button value="week">未来7天</el-radio-button>
          <el-radio-button value="past">已过期</el-radio-button>
        </el-radio-group>
      </div>

      <el-table v-loading="loading" :data="interviewList" style="width:100%" stripe>
        <el-table-column label="候选人" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.studentName || '学生#' + row.studentId }}</template>
        </el-table-column>
        <el-table-column label="岗位" min-width="160" show-overflow-tooltip>  <template #default="{ row }">{{ row.jobTitle || '岗位#' + row.jobId }}</template>
        </el-table-column>
        <el-table-column label="面试时间" width="170">
          <template #default="{ row }">
            <span :style="{ color: isExpired(row.interviewTime) ? '#EF4444' : '#1D2129' }">
              {{ formatTime(row.interviewTime) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="面试地点" width="150">
          <template #default="{ row }">{{ row.interviewLocation || row.location || '-' }}</template>
        </el-table-column>
        <el-table-column label="面试方式" width="100" align="center">
          <template #default="{ row }">{{ guessMethod(row.interviewLocation || row.location) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="editInterview(row)">修改</el-button>
            <el-button v-if="row.status === 2" type="success" link size="small" @click="updateStatus(row, 3)">录用</el-button>
            <el-button v-if="row.status === 2" type="danger" link size="small" @click="updateStatus(row, 4)">不合适</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && interviewList.length === 0" class="empty-state">
        <el-empty :image-size="100" description="暂无面试安排" />
      </div>
    </div>

    <!-- 修改面试弹窗 -->
    <el-dialog v-model="dialogVisible" title="修改面试安排" width="520px" append-to-body>
      <el-form :model="form" label-width="90px">
        <el-form-item label="候选人">
          <span style="font-weight:500">{{ form.studentName }}</span>
        </el-form-item>
        <el-form-item label="面试时间" required>
          <el-date-picker v-model="form.interviewTime" type="datetime" placeholder="选择面试时间"
            value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="面试方式">
          <el-select v-model="form.method" style="width:100%">
            <el-option label="线下面试" value="线下" />
            <el-option label="视频面试" value="视频" />
            <el-option label="电话面试" value="电话" />
          </el-select>
        </el-form-item>
        <el-form-item label="面试地点" required>
          <el-input v-model="form.location" placeholder="如：公司会议室A" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="其他补充说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirmSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deliveryAPI, jobAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { formatDate } from '@/utils/formatDate'

const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const dateFilter = ref('all')
const interviewList = ref([])
const dialogVisible = ref(false)
const currentId = ref(null)
const form = ref({ studentName: '', interviewTime: '', method: '线下', location: '', remark: '' })
const stats = ref({ today: 0, pending: 0, completed: 0, total: 0 })

onMounted(() => {
  loadInterviews()
})

const loadInterviews = async () => {
  loading.value = true
  try {
    // 获取所有投递记录（HR的）
    const res = await deliveryAPI.getDeliveriesByCompany(userStore.companyId)
    const all = (res.data?.list || res.data?.records || res.data || []).filter(d => d.status >= 2)
    // 补充岗位标题
    const enriched = await Promise.all(all.map(async (d) => {
      if (!d.jobTitle) {
        try {
          const jRes = await jobAPI.getJobDetail(d.jobId)
          d.jobTitle = jRes.data?.title || '岗位#' + d.jobId
        } catch (e) { d.jobTitle = '岗位#' + d.jobId }
      }
      return d
    }))
    // 按面试时间排序
    enriched.sort((a, b) => {
      const ta = a.interviewTime || ''
      const tb = b.interviewTime || ''
      return ta < tb ? -1 : ta > tb ? 1 : 0
    })
    // 日期筛选
    const now = new Date()
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const weekEnd = new Date(todayStart.getTime() + 7 * 24 * 60 * 60 * 1000)
    let filtered = enriched
    if (dateFilter.value === 'today') {
      filtered = enriched.filter(d => d.interviewTime && new Date(d.interviewTime) >= todayStart && new Date(d.interviewTime) < new Date(todayStart.getTime() + 24 * 60 * 60 * 1000))
    } else if (dateFilter.value === 'week') {
      filtered = enriched.filter(d => d.interviewTime && new Date(d.interviewTime) >= todayStart && new Date(d.interviewTime) < weekEnd)
    } else if (dateFilter.value === 'past') {
      filtered = enriched.filter(d => d.interviewTime && new Date(d.interviewTime) < todayStart)
    }
    interviewList.value = filtered
    // 统计
    const todayCount = enriched.filter(d => d.interviewTime && new Date(d.interviewTime) >= todayStart && new Date(d.interviewTime) < new Date(todayStart.getTime() + 24 * 60 * 60 * 1000)).length
    const pendingCount = enriched.filter(d => d.status === 2).length
    const completedCount = enriched.filter(d => d.status === 3 || d.status === 4).length
    stats.value = { today: todayCount, pending: pendingCount, completed: completedCount, total: enriched.length }
  } catch (e) {
    console.error('加载面试列表失败', e)
    interviewList.value = []
  } finally {
    loading.value = false
  }
}

const editInterview = (row) => {
  currentId.value = row.id
  form.value = {
    studentName: row.studentName || '学生#' + row.studentId,
    interviewTime: row.interviewTime || '',
    method: row.method || '线下',
    location: row.interviewLocation || row.location || '',
    remark: ''
  }
  dialogVisible.value = true
}

const confirmSave = async () => {
  if (!form.value.interviewTime) { ElMessage.warning('请选择面试时间'); return }
  if (!form.value.location || !form.value.location.trim()) { ElMessage.warning('请填写面试地点'); return }
  saving.value = true
  try {
    const data = {
      interviewTime: form.value.interviewTime,
      interviewLocation: form.value.location,
      method: form.value.method,
      remark: form.value.remark
    }
    const res = await deliveryAPI.arrangeInterview(currentId.value, data)
    if (res.code === 200) {
      ElMessage.success('修改成功')
      dialogVisible.value = false
      await loadInterviews()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const updateStatus = async (row, status) => {
  const labels = { 3: '录用', 4: '不合适' }
  try {
    await ElMessageBox.confirm(`确定标记该候选人为「${labels[status]}」？`, '确认操作')
    const res = await deliveryAPI.updateDeliveryStatus(row.id, status)
    if (res.code === 200) {
      ElMessage.success('状态已更新')
      await loadInterviews()
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const formatTime = (t) => formatDate(t)

const guessMethod = (location) => {
  if (!location) return '线下'
  const kw = ['线上', '视频', '腾讯会议', '会议', 'zoom', '飞书', '钉钉', 'skype']
  if (kw.some(k => location.includes(k))) return '线上'
  if (location.includes('电话')) return '电话'
  return '线下'
}

const isExpired = (t) => {
  if (!t) return false
  return new Date(t) < new Date()
}

const statusType = (s) => {
  if (s === 2) return 'warning'
  if (s === 3) return 'success'
  if (s === 4) return 'danger'
  return 'info'
}

const statusLabel = (s) => {
  if (s === 2) return '面试中'
  if (s === 3) return '已录用'
  if (s === 4) return '不合适'
  return '未知'
}
</script>

<style scoped>
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 24px; }
.stat-card { background: white; padding: 24px; border-radius: 16px; border-left: 6px solid; box-shadow: 0 6px 16px rgba(0,0,0,0.06); }
.stat-card h3 { margin: 0 0 8px; font-size: 14px; color: #86909C; font-weight: 500; }
.stat-card .num { font-size: 32px; font-weight: 700; color: #1D2129; }
.content-card { background: #fff; border-radius: 16px; padding: 24px; margin-bottom: 24px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); }
.content-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.content-card-title { font-size: 16px; font-weight: 600; color: #1D2129; }
.empty-state { text-align: center; padding: 40px 0; }
</style>
