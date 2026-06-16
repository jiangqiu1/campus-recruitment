<template>
  <div class="job-match">
    <h2>人岗匹配</h2>
    
    <!-- 操作栏 -->
    <el-row class="operation-row">
      <el-col :span="12">
        <el-button type="primary" @click="showGenerateDialog">生成匹配</el-button>
        <el-button type="success" @click="batchGenerate">批量匹配</el-button>
      </el-col>
      <el-col :span="12" style="text-align: right;">
        <el-select v-model="filterJobId" placeholder="选择岗位" clearable style="width: 200px; margin-right: 10px;">
          <el-option v-for="job in jobList" :key="job.id" :label="job.title" :value="job.id" />
        </el-select>
        <el-button type="primary" @click="loadMatches">筛选</el-button>
      </el-col>
    </el-row>

    <!-- 匹配结果表格 -->
    <el-table :data="matchList" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="jobId" label="岗位ID" width="100" />
      <el-table-column prop="studentId" label="学生ID" width="100" />
      <el-table-column prop="matchScore" label="匹配度" width="150">
        <template #default="{ row }">
          <el-progress 
            :percentage="row.matchScore * 100" 
            :color="matchScoreColor(row.matchScore)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="isPushed" label="已推送" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isPushed ? 'success' : 'info'">
            {{ row.isPushed ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="isClicked" label="已点击" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isClicked ? 'success' : 'info'">
            {{ row.isClicked ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="pushTime" label="推送时间" width="180" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="pushToStudent(row)" :disabled="row.isPushed">推送</el-button>
          <el-button size="small" type="danger" @click="deleteMatch(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      @current-change="loadMatches"
      layout="total, prev, pager, next, jumper"
      style="margin-top: 20px; text-align: center;"
    />

    <!-- 生成匹配对话框 -->
    <el-dialog v-model="generateDialogVisible" title="生成人岗匹配" width="500px">
      <el-form :model="generateForm" label-width="100px">
        <el-form-item label="选择岗位" prop="jobId">
          <el-select v-model="generateForm.jobId" placeholder="请选择岗位" style="width: 100%;">
            <el-option v-for="job in jobList" :key="job.id" :label="job.title" :value="job.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="选择学生" prop="studentId">
          <el-select v-model="generateForm.studentId" placeholder="请选择学生" style="width: 100%;">
            <el-option v-for="student in studentList" :key="student.id" :label="student.realName" :value="student.id" />
          </el-select>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="generateMatch">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { jobAPI, userAPI, jobMatchAPI } from '@/api'

const matchList = ref([])
const jobList = ref([])
const studentList = ref([])
const filterJobId = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const generateDialogVisible = ref(false)
const generateForm = ref({
  jobId: null,
  studentId: null
})

onMounted(() => {
  loadMatches()
  loadJobs()
  loadStudents()
})

const loadMatches = async () => {
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    
    if (filterJobId.value) {
      params.jobId = filterJobId.value
    }
    
    const res = await jobMatchAPI.getMatches(params)
    if (res.code === 200) {
      matchList.value = res.data.records
      total.value = res.data.total
    }
  } catch (error) {
    ElMessage.error('加载匹配记录失败')
  }
}

const loadJobs = async () => {
  try {
    const res = await jobAPI.getJobs()
    if (res.code === 200) {
      jobList.value = res.data
    }
  } catch (error) {
    console.error('加载岗位列表失败', error)
  }
}

const loadStudents = async () => {
  try {
    const res = await userAPI.getUsers({ role: 0 })
    if (res.code === 200) {
      studentList.value = res.data.records
    }
  } catch (error) {
    console.error('加载学生列表失败', error)
  }
}

const showGenerateDialog = () => {
  generateForm.value = { jobId: null, studentId: null }
  generateDialogVisible.value = true
}

const generateMatch = async () => {
  if (!generateForm.value.jobId || !generateForm.value.studentId) {
    ElMessage.warning('请选择完整的匹配信息')
    return
  }

  try {
    await jobMatchAPI.generateMatch(generateForm.value)
    ElMessage.success('匹配生成成功')
    generateDialogVisible.value = false
    loadMatches()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '生成失败')
  }
}

const batchGenerate = async () => {
  if (!filterJobId.value) {
    ElMessage.warning('请先选择一个岗位')
    return
  }

  try {
    const res = await jobMatchAPI.batchGenerateMatches(filterJobId.value)
    if (res.code === 200) {
      ElMessage.success(`批量生成成功，共生成 ${res.data.generatedCount} 条匹配记录`)
      loadMatches()
    }
  } catch (error) {
    ElMessage.error('批量生成失败')
  }
}

const pushToStudent = async (row) => {
  try {
    await jobMatchAPI.pushMatch(row.id)
    ElMessage.success('推送成功')
    loadMatches()
  } catch (error) {
    ElMessage.error('推送失败')
  }
}

const deleteMatch = async (row) => {
  try {
    await jobMatchAPI.deleteMatch(row.id)
    ElMessage.success('删除成功')
    loadMatches()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const matchScoreColor = (score) => {
  if (score >= 0.8) return '#67C23A'
  if (score >= 0.6) return '#E6A23C'
  return '#F56C6C'
}
</script>

<style scoped>
.job-match {
  padding: 20px;
}

.operation-row {
  margin-bottom: 20px;
}
</style>
