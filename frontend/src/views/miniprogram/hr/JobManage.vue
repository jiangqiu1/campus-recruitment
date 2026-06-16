<template>
  <div class="job-manage">
    <h2>岗位管理</h2>
    
    <!-- 操作栏 -->
    <el-row class="operation-row">
      <el-col :span="12">
        <el-button type="primary" @click="showAddDialog">发布岗位</el-button>
        <el-button type="danger" @click="batchDelete" :disabled="selectedIds.length === 0">批量删除</el-button>
      </el-col>
      <el-col :span="12" style="text-align: right;">
        <el-select v-model="statusFilter" placeholder="岗位状态" clearable style="width: 150px; margin-right: 10px;">
          <el-option label="草稿" :value="0" />
          <el-option label="已发布" :value="1" />
          <el-option label="已关闭" :value="2" />
          <el-option label="已暂停" :value="3" />
        </el-select>
        <el-button type="primary" @click="loadJobs">筛选</el-button>
      </el-col>
    </el-row>

    <!-- 岗位表格 -->
    <el-table :data="jobList" @selection-change="handleSelectionChange" stripe>
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="岗位名称" />
      <el-table-column prop="salaryRange" label="薪资范围" width="150" />
      <el-table-column prop="location" label="工作地点" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览量" width="100" />
      <el-table-column prop="deliveryCount" label="投递数" width="100" />
      <el-table-column prop="endDate" label="截止日期" width="120" />
      <el-table-column label="操作" width="250">
        <template #default="{ row }">
          <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteJob(row)">删除</el-button>
          <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '暂停' : '发布' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      @current-change="loadJobs"
      layout="total, prev, pager, next, jumper"
      style="margin-top: 20px; text-align: center;"
    />

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form :model="jobForm" :rules="rules" ref="jobFormRef" label-width="100px">
        <el-form-item label="岗位名称" prop="title">
          <el-input v-model="jobForm.title" />
        </el-form-item>
        
        <el-form-item label="薪资范围" prop="salaryRange">
          <el-input v-model="jobForm.salaryRange" placeholder="如：8K-15K" />
        </el-form-item>
        
        <el-form-item label="工作地点" prop="location">
          <el-input v-model="jobForm.location" />
        </el-form-item>
        
        <el-form-item label="岗位要求" prop="requirements">
          <el-input v-model="jobForm.requirements" type="textarea" :rows="4" />
        </el-form-item>
        
        <el-form-item label="岗位职责" prop="responsibilities">
          <el-input v-model="jobForm.responsibilities" type="textarea" :rows="4" />
        </el-form-item>
        
        <el-form-item label="截止日期" prop="endDate">
          <el-date-picker v-model="jobForm.endDate" type="date" placeholder="选择日期" />
        </el-form-item>
        
        <el-form-item label="岗位状态" prop="status">
          <el-radio-group v-model="jobForm.status">
            <el-radio :label="0">草稿</el-radio>
            <el-radio :label="1">发布</el-radio>
            <el-radio :label="2">关闭</el-radio>
            <el-radio :label="3">暂停</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveJob">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { jobAPI } from '@/api/index.js'

const jobList = ref([])
const selectedIds = ref([])
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('发布岗位')
const isEdit = ref(false)
const jobFormRef = ref()

const jobForm = ref({
  id: null,
  title: '',
  salaryRange: '',
  location: '',
  requirements: '',
  responsibilities: '',
  endDate: '',
  status: 0
})

const rules = {
  title: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  salaryRange: [{ required: true, message: '请输入薪资范围', trigger: 'blur' }],
  location: [{ required: true, message: '请输入工作地点', trigger: 'blur' }],
  requirements: [{ required: true, message: '请输入岗位要求', trigger: 'blur' }],
  responsibilities: [{ required: true, message: '请输入岗位职责', trigger: 'blur' }],
  endDate: [{ required: true, message: '请选择截止日期', trigger: 'change' }]
}

onMounted(() => {
  loadJobs()
})

const loadJobs = async () => {
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    
    if (statusFilter.value !== '') {
      params.status = statusFilter.value
    }
    
    // 获取当前HR所在企业的岗位
    const companyId = localStorage.getItem('companyId') || 1 // 模拟数据
    const res = await jobAPI.getJobsByCompany(companyId, params)
    
    if (res.code === 200) {
      jobList.value = res.data.records
      total.value = res.data.total
    }
  } catch (error) {
    ElMessage.error('加载岗位列表失败')
  }
}

const showAddDialog = () => {
  isEdit.value = false
  dialogTitle.value = '发布岗位'
  jobForm.value = { id: null, title: '', salaryRange: '', location: '', requirements: '', responsibilities: '', endDate: '', status: 0 }
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑岗位'
  jobForm.value = { ...row }
  dialogVisible.value = true
}

const saveJob = async () => {
  await jobFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const companyId = localStorage.getItem('companyId') || 1
        jobForm.value.companyId = companyId
        
        if (isEdit.value) {
          const res = await jobAPI.updateJob(jobForm.value.id, jobForm.value)
          if (res.code === 200) {
            ElMessage.success('更新成功')
          }
        } else {
          const res = await jobAPI.createJob(jobForm.value)
          if (res.code === 200) {
            ElMessage.success('发布成功')
          }
        }
        dialogVisible.value = false
        loadJobs()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '操作失败')
      }
    }
  })
}

const deleteJob = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该岗位吗？', '提示', { type: 'warning' })
    const res = await jobAPI.deleteJob(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadJobs()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const batchDelete = async () => {
  if (selectedIds.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个岗位吗？`, '提示', { type: 'warning' })
    const res = await jobAPI.batchDeleteJobs(selectedIds.value)
    if (res.code === 200) {
      ElMessage.success('批量删除成功')
      loadJobs()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

const toggleStatus = async (row) => {
  try {
    const newStatus = row.status === 1 ? 3 : 1
    const res = await jobAPI.updateJobStatus(row.id, newStatus)
    if (res.code === 200) {
      ElMessage.success('状态更新成功')
      loadJobs()
    }
  } catch (error) {
    ElMessage.error('状态更新失败')
  }
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

const statusTagType = (status) => {
  const types = ['info', 'success', 'danger', 'warning']
  return types[status] || 'info'
}

const statusText = (status) => {
  const texts = ['草稿', '已发布', '已关闭', '已暂停']
  return texts[status] || '未知'
}
</script>

<style scoped>
.job-manage {
  padding: 20px;
}

.operation-row {
  margin-bottom: 20px;
}
</style>
