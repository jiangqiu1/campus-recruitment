<template>
  <div class="enterprise-audit fade-in">
    <div class="page-header">
      <h2>企业审核</h2>
      <p>审核企业注册 · 管理企业资质</p>
    </div>
    
    <!-- 选项卡 -->
    <div class="tabs">
      <button 
        class="tab-item" 
        :class="{ active: activeTab === 'enterprise' }"
        @click="activeTab = 'enterprise'"
      >
        待审核企业 <span v-if="enterpriseList.length" class="badge">{{ enterpriseList.length }}</span>
      </button>
      <button 
        class="tab-item" 
        :class="{ active: activeTab === 'job' }"
        @click="activeTab = 'job'"
      >
        待审核岗位变更 <span v-if="jobChangeList.length" class="badge">{{ jobChangeList.length }}</span>
      </button>
    </div>

    <!-- ===== 企业审核列表 ===== -->
    <div v-if="activeTab === 'enterprise'" class="audit-content">
      <div class="card">
        <div class="card-header">
          <h2>企业注册审核</h2>
          <div class="header-actions">
            <el-button size="small" @click="loadPendingCompanies" :loading="loadingCompany">
              <el-icon style="margin-right:4px"><Refresh /></el-icon>刷新
            </el-button>
            <el-button size="small" type="primary" plain
              @click="batchApproveEnterprises" 
              :disabled="enterpriseList.length === 0">
              批量通过
            </el-button>
          </div>
        </div>
        
        <!-- 空状态 -->
        <el-empty v-if="!loadingCompany && enterpriseList.length === 0" description="暂无待审核企业" :image-size="120" />

        <el-table v-if="enterpriseList.length > 0" :data="enterpriseList" stripe style="width: 100%" table-layout="auto">
          <el-table-column type="selection" width="40" />
          <el-table-column prop="createTime" label="申请时间" min-width="170" show-overflow-tooltip>
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column prop="name" label="企业名称" min-width="130" show-overflow-tooltip />
          <el-table-column prop="industry" label="行业" min-width="110" show-overflow-tooltip />
          <el-table-column prop="contactPerson" label="联系人" min-width="100" show-overflow-tooltip />
          <el-table-column prop="contactPhone" label="联系电话" min-width="130" show-overflow-tooltip />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="openAuditModal(row)">审核</el-button>
              <el-button type="success" size="small" plain @click="approveEnterprise(row)">通过</el-button>
              <el-button type="danger" size="small" plain @click="rejectEnterprise(row)">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- ===== 岗位变更审核列表 ===== -->
    <div v-if="activeTab === 'job'" class="audit-content">
      <div class="card">
        <div class="card-header">
          <h2>岗位变更审核</h2>
          <el-button size="small" @click="loadPendingJobChanges" :loading="loadingJobChange">
            <el-icon style="margin-right:4px"><Refresh /></el-icon>刷新
          </el-button>
        </div>

        <!-- 空状态 -->
        <el-empty v-if="!loadingJobChange && jobChangeList.length === 0" description="暂无待审核岗位变更" :image-size="120" />
        
        <el-table v-if="jobChangeList.length > 0" :data="jobChangeList" stripe style="width: 100%" table-layout="auto">
          <el-table-column prop="applyTime" label="申请时间" min-width="170" show-overflow-tooltip />
          <el-table-column prop="companyName" label="企业名称" min-width="130" show-overflow-tooltip />
          <el-table-column prop="jobTitle" label="岗位名称" min-width="120" show-overflow-tooltip />
          <el-table-column prop="changeContent" label="变更内容" min-width="200" show-overflow-tooltip />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button type="success" size="small" @click="approveJobChange(row)">通过</el-button>
              <el-button type="danger" size="small" @click="rejectJobChange(row)">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- ===== 审核详情对话框（带拒绝理由） ===== -->
    <el-dialog v-model="auditDialogVisible" title="企业审核详情" width="640px" :append-to-body="true">
      <el-descriptions :column="2" border style="margin-bottom: 20px;">
        <el-descriptions-item label="企业名称" :span="2">{{ currentEnterprise.name }}</el-descriptions-item>
        <el-descriptions-item label="简称">{{ currentEnterprise.shortName || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="行业">{{ currentEnterprise.industry || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ currentEnterprise.contactPerson || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentEnterprise.contactPhone || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="合作等级" :span="2">
          <el-tag v-if="currentEnterprise.cooperationLevel !== undefined && currentEnterprise.cooperationLevel !== null" 
            :type="['info','success','warning','danger',''][currentEnterprise.cooperationLevel - 1] || 'info'">
            {{ ['','战略合作','核心伙伴','普通合作','试合作','已暂停'][currentEnterprise.cooperationLevel] || '未设置' }}
          </el-tag>
          <span v-else class="text-muted">未设置</span>
        </el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ currentEnterprise.address || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="申请时间" :span="2">{{ formatTime(currentEnterprise.createTime) || '未知' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 拒绝理由（弹窗内可填写，决定通过或拒绝） -->
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="3"
        placeholder="填写拒绝理由（可选，拒绝时发送给企业）"
        style="margin-bottom: 8px;"
      />
      
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="doReject(currentEnterprise)" :loading="submitting">
          拒绝
        </el-button>
        <el-button type="success" @click="doApprove(currentEnterprise)" :loading="submitting">
          通过审核
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { companyAPI, jobChangeAPI } from '@/api'
import { formatDate } from '@/utils/formatDate'

const activeTab = ref('enterprise')
const auditDialogVisible = ref(false)
const currentEnterprise = ref({})
const rejectReason = ref('')
const submitting = ref(false)
const loadingCompany = ref(false)
const loadingJobChange = ref(false)

const enterpriseList = ref([])
const jobChangeList = ref([])

onMounted(async () => {
  await loadPendingCompanies()
  await loadPendingJobChanges()
})

const formatTime = (t) => formatDate(t, { showSeconds: true })

// 加载待审核企业
const loadPendingCompanies = async () => {
  loadingCompany.value = true
  try {
    const res = await companyAPI.getCompanies({ status: 0 })
    if (res.code === 200) {
      enterpriseList.value = res.data || []
    }
  } catch (error) {
    console.error('加载待审核企业失败:', error)
  } finally {
    loadingCompany.value = false
  }
}

const openAuditModal = (enterprise) => {
  currentEnterprise.value = { ...enterprise }
  rejectReason.value = ''
  auditDialogVisible.value = true
}

const doApprove = async (enterprise) => {
  submitting.value = true
  try {
    const res = await companyAPI.approveCompany(enterprise.id)
    if (res.code === 200) {
      ElMessage.success('审核通过！' + (rejectReason.value ? '（理由已记录）' : ''))
      auditDialogVisible.value = false
      enterpriseList.value = enterpriseList.value.filter(e => e.id !== enterprise.id)
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

const doReject = async (enterprise) => {
  let reason = rejectReason.value.trim()
  if (!reason) {
    try {
      await ElMessageBox.confirm('未填写拒绝理由，确定直接拒绝吗？', '提示', { type: 'warning' })
    } catch {
      return
    }
  }
  submitting.value = true
  try {
    const res = await companyAPI.rejectCompany(enterprise.id)
    if (res.code === 200) {
      ElMessage.success('已拒绝' + (reason ? '，理由：' + reason : ''))
      auditDialogVisible.value = false
      enterpriseList.value = enterpriseList.value.filter(e => e.id !== enterprise.id)
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

const approveEnterprise = async (enterprise) => {
  try {
    await ElMessageBox.confirm('确定通过「' + enterprise.name + '」的审核吗？', '确认', { type: 'success' })
    const res = await companyAPI.approveCompany(enterprise.id)
    if (res.code === 200) {
      ElMessage.success('审核通过')
      enterpriseList.value = enterpriseList.value.filter(e => e.id !== enterprise.id)
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('操作失败')
  }
}

const rejectEnterprise = async (enterprise) => {
  try {
    await ElMessageBox.confirm('确定拒绝「' + enterprise.name + '」的注册申请吗？', '确认', { type: 'warning' })
    const res = await companyAPI.rejectCompany(enterprise.id)
    if (res.code === 200) {
      ElMessage.success('已拒绝')
      enterpriseList.value = enterpriseList.value.filter(e => e.id !== enterprise.id)
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('操作失败')
  }
}

const batchApproveEnterprises = async () => {
  try {
    await ElMessageBox.confirm('确定批量通过全部 ' + enterpriseList.value.length + ' 家待审核企业吗？', '确认', { type: 'success' })
    submitting.value = true
    let success = 0
    for (const e of enterpriseList.value) {
      try {
        const res = await companyAPI.approveCompany(e.id)
        if (res.code === 200) success++
      } catch { /* skip */ }
    }
    ElMessage.success('批量审核完成，成功：' + success + '/' + enterpriseList.value.length)
    await loadPendingCompanies()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('批量操作失败')
  } finally {
    submitting.value = false
  }
}

// 加载岗位变更
const loadPendingJobChanges = async () => {
  loadingJobChange.value = true
  try {
    const res = await jobChangeAPI.list({ status: 0 })
    if (res.code === 200) jobChangeList.value = res.data || []
  } catch (error) {
    console.error('加载待审核岗位变更失败:', error)
  } finally {
    loadingJobChange.value = false
  }
}

const approveJobChange = async (jobChange) => {
  try {
    await ElMessageBox.confirm('确定通过此岗位变更申请吗？', '确认', { type: 'success' })
    const res = await jobChangeAPI.approve(jobChange.id)
    if (res.code === 200) {
      ElMessage.success('审核通过')
      jobChangeList.value = jobChangeList.value.filter(j => j.id !== jobChange.id)
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('操作失败')
  }
}

const rejectJobChange = async (jobChange) => {
  try {
    await ElMessageBox.confirm('确定拒绝此岗位变更申请吗？', '确认', { type: 'warning' })
    const res = await jobChangeAPI.reject(jobChange.id)
    if (res.code === 200) {
      ElMessage.success('已拒绝')
      jobChangeList.value = jobChangeList.value.filter(j => j.id !== jobChange.id)
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('操作失败')
  }
}
</script>

<style scoped>
.enterprise-audit { padding: 20px; width: 100%; max-width: 100%; box-sizing: border-box; }
.enterprise-audit :deep(.el-table) { width: 100% !important; }

.tabs {
  display: flex;
  gap: 4px;
  background: #F2F3F5;
  padding: 4px;
  border-radius: 10px;
  margin-bottom: 24px;
  width: fit-content;
}

.tab-item {
  padding: 10px 24px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #4E5969;
  transition: all 0.25s;
  border: none;
  background: transparent;
  position: relative;
}

.tab-item.active {
  background: white;
  color: #165DFF;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.badge {
  display: inline-block;
  background: #F53F3F;
  color: white;
  font-size: 11px;
  line-height: 18px;
  padding: 0 6px;
  border-radius: 9px;
  margin-left: 6px;
  font-weight: 500;
  vertical-align: top;
}

.card {
  background: white;
  border-radius: 16px;
  padding: 32px;
  margin-bottom: 28px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.06);
  position: relative;
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, #165DFF, #2563EB, transparent);
  border-radius: 16px 16px 0 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.card h2 {
  font-size: 19px;
  font-weight: 600;
  padding-bottom: 12px;
  border-bottom: 1px solid #F2F3F5;
  position: relative;
}

.card h2::after {
  content: '';
  width: 50px;
  height: 3px;
  background: #165DFF;
  border-radius: 3px;
  position: absolute;
  left: 0;
  bottom: -1px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.text-muted {
  color: #86909C;
  font-size: 13px;
}
</style>
