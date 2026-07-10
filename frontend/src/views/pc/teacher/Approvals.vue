<template>
  <div class="approvals fade-in">
    <div class="page-header">
      <h2>审批管理</h2>
      <p>审批 HR 提交的岗位变更申请</p>
    </div>

    <div class="content-card">
      <div class="content-card-header">
        <span class="content-card-title">审批列表</span>
        <div class="filter-tabs">
          <el-radio-group v-model="statusFilter" @change="loadApprovals">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="0">待审批</el-radio-button>
            <el-radio-button value="1">已通过</el-radio-button>
            <el-radio-button value="2">已拒绝</el-radio-button>
          </el-radio-group>
        </div>
      </div>
      <el-table v-loading="loading" :data="approvalList" style="width:100%" stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="jobTitle" label="岗位名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="companyName" label="所属企业" width="160" />
        <el-table-column prop="applyUser" label="申请人" width="100" />
        <el-table-column label="变更类型" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.changeType || '信息修改' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="success" size="small" @click="handleApprove(row)">
              通过
            </el-button>
            <el-button v-if="row.status === 0" type="danger" size="small" @click="handleReject(row)">
              拒绝
            </el-button>
            <el-button v-if="row.status !== 0" type="primary" link size="small" @click="viewDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && approvalList.length === 0" class="empty-state">
        <el-empty :image-size="100" description="暂无审批记录" />
      </div>
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="变更详情" width="600px"
      append-to-body modal-class="approval-overlay">
      <div v-if="currentItem" class="detail-body">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="岗位名称">{{ currentItem.jobTitle || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所属企业">{{ currentItem.companyName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ currentItem.applyUser || '-' }}</el-descriptions-item>
          <el-descriptions-item label="变更类型">{{ currentItem.changeType || '信息修改' }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatTime(currentItem.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(currentItem.status)">{{ statusLabel(currentItem.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="currentItem.reason" label="备注说明">{{ currentItem.reason }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="currentItem.content" class="change-content">
          <h4>变更内容</h4>
          <pre class="text-block">{{ currentItem.content }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { jobChangeAPI } from '@/api'

const loading = ref(false)
const statusFilter = ref('')
const approvalList = ref([])
const detailVisible = ref(false)
const currentItem = ref(null)

onMounted(() => { loadApprovals() })

const loadApprovals = async () => {
  loading.value = true
  try {
    const params = {}
    if (statusFilter.value !== '') params.status = parseInt(statusFilter.value)
    const res = await jobChangeAPI.list(params)
    if (res.code === 200) {
      const data = res.data || []
      approvalList.value = data.map(item => ({
        ...item,
        jobTitle: item.jobTitle || ('岗位 #' + item.jobId),
        companyName: item.companyName || '',
        applyUser: item.applyUser || ('HR #' + item.hrId),
        changeType: item.changeType || '信息修改',
        createTime: item.applyTime || item.createTime || ''
      }))
    }
  } catch (e) {
    console.error('加载审批列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm(`确认通过「${row.jobTitle}」的变更申请？`, '提示')
    const res = await jobChangeAPI.approve(row.id)
    if (res.code === 200) {
      ElMessage.success('已通过')
      row.status = 1
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const handleReject = async (row) => {
  try {
    await ElMessageBox.confirm(`确认拒绝「${row.jobTitle}」的变更申请？`, '提示')
    const res = await jobChangeAPI.reject(row.id)
    if (res.code === 200) {
      ElMessage.success('已拒绝')
      row.status = 2
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const viewDetail = (row) => {
  currentItem.value = row
  detailVisible.value = true
}

const statusTag = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'danger' }
  return map[status] || 'info'
}
const statusLabel = (status) => {
  const map = { 0: '待审批', 1: '已通过', 2: '已拒绝' }
  return map[status] || '未知'
}
const formatTime = (t) => t ? t.substring(0, 19) : '-'
</script>

<style scoped>
.filter-tabs { margin: 0; }
.change-content { margin-top: 20px; }
.change-content h4 { margin-bottom: 10px; color: #1D2129; }
.text-block { background: #F7F8FA; padding: 16px; border-radius: 6px; line-height: 1.6; white-space: pre-wrap; font-size: 13px; max-height: 300px; overflow-y: auto; }
</style>

<style>
.approval-overlay {
  position: fixed !important;
  top: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  left: 0 !important;
  background: rgba(0, 0, 0, 0.45) !important;
  z-index: 9999 !important;
}
</style>
