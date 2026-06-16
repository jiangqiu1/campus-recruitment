<template>
  <div class="resume-manage">
    <h2>简历管理</h2>
    
    <!-- 操作栏 -->
    <el-row class="operation-row">
      <el-col :span="12">
        <el-button type="primary" @click="showUploadDialog">上传简历</el-button>
        <el-button type="danger" @click="batchDelete" :disabled="selectedIds.length === 0">批量删除</el-button>
        <el-button type="success" @click="exportToExcel">导出Excel</el-button>
      </el-col>
      <el-col :span="12" style="text-align: right;">
        <el-input v-model="searchKeyword" placeholder="搜索学生姓名/技能" style="width: 300px;" clearable>
          <template #append>
            <el-button @click="loadResumes"><el-icon><Search /></el-icon></el-button>
          </template>
        </el-input>
      </el-col>
    </el-row>

    <!-- 简历表格 -->
    <el-table :data="resumeList" @selection-change="handleSelectionChange" stripe>
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="studentId" label="学生ID" width="100" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="jobTarget" label="求职意向" />
      <el-table-column prop="education" label="学历" width="100" />
      <el-table-column prop="skillTags" label="技能标签" />
      <el-table-column prop="isDefault" label="默认简历" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isDefault ? 'success' : 'info'">
            {{ row.isDefault ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="showViewDialog(row)">查看</el-button>
          <el-button size="small" type="danger" @click="deleteResume(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      @current-change="loadResumes"
      layout="total, prev, pager, next, jumper"
      style="margin-top: 20px; text-align: center;"
    />

    <!-- 上传对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="上传简历" width="500px">
      <el-form :model="uploadForm" label-width="100px">
        <el-form-item label="学生ID" prop="studentId">
          <el-input v-model="uploadForm.studentId" type="number" />
        </el-form-item>
        
        <el-form-item label="简历文件" prop="file">
          <el-upload
            class="upload-demo"
            action="#"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
          >
            <template #trigger>
              <el-button type="primary">选择文件</el-button>
            </template>
            <template #tip>
              <div class="el-upload__tip">
                支持 PDF、Word 格式
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="uploadResume">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看对话框 -->
    <el-dialog v-model="viewDialogVisible" title="简历详情" width="800px">
      <div v-if="currentResume">
        <h3>{{ currentResume.name }} 的简历</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="姓名">{{ currentResume.name }}</el-descriptions-item>
          <el-descriptions-item label="求职意向">{{ currentResume.jobTarget }}</el-descriptions-item>
          <el-descriptions-item label="学历">{{ currentResume.education }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ currentResume.phone }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentResume.email }}</el-descriptions-item>
          <el-descriptions-item label="技能标签">{{ currentResume.skillTags }}</el-descriptions-item>
          <el-descriptions-item label="自我评价" :span="2">{{ currentResume.selfEvaluation }}</el-descriptions-item>
          <el-descriptions-item label="工作经历" :span="2">{{ currentResume.workExperience }}</el-descriptions-item>
          <el-descriptions-item label="项目经历" :span="2">{{ currentResume.projectExperience }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { resumeAPI } from '@/api'
import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'

const resumeList = ref([])
const selectedIds = ref([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const uploadDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const currentResume = ref(null)
const uploadForm = ref({
  studentId: null,
  file: null
})

onMounted(() => {
  loadResumes()
})

const loadResumes = async () => {
  try {
    const res = await resumeAPI.getResumes({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value
    })
    if (res.code === 200) {
      resumeList.value = res.data.records
      total.value = res.data.total
    }
  } catch (error) {
    ElMessage.error('加载简历列表失败')
  }
}

const showUploadDialog = () => {
  uploadForm.value = { studentId: null, file: null }
  uploadDialogVisible.value = true
}

const handleFileChange = (file) => {
  uploadForm.value.file = file.raw
}

const uploadResume = async () => {
  if (!uploadForm.value.studentId || !uploadForm.value.file) {
    ElMessage.warning('请填写完整信息')
    return
  }

  try {
    const res = await resumeAPI.uploadResume(uploadForm.value.studentId, uploadForm.value.file)
    if (res.code === 200) {
      ElMessage.success('上传成功')
      uploadDialogVisible.value = false
      loadResumes()
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '上传失败')
  }
}

const showViewDialog = async (row) => {
  try {
    const res = await resumeAPI.getResumeDetail(row.id)
    if (res.code === 200) {
      currentResume.value = res.data
      viewDialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('加载简历详情失败')
  }
}

const deleteResume = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该简历吗？', '提示', { type: 'warning' })
    await resumeAPI.deleteResume(row.id)
    ElMessage.success('删除成功')
    loadResumes()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const batchDelete = async () => {
  if (selectedIds.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 份简历吗？`, '提示', { type: 'warning' })
    await resumeAPI.batchDeleteResumes(selectedIds.value)
    ElMessage.success('批量删除成功')
    loadResumes()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

const exportToExcel = () => {
  const exportData = resumeList.value.map(resume => ({
    'ID': resume.id,
    '学生ID': resume.studentId,
    '姓名': resume.name,
    '求职意向': resume.jobTarget,
    '学历': resume.education,
    '技能标签': resume.skillTags,
    '默认简历': resume.isDefault ? '是' : '否'
  }))

  const ws = XLSX.utils.json_to_sheet(exportData)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '简历列表')
  const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
  const blob = new Blob([wbout], { type: 'application/octet-stream' })
  saveAs(blob, '简历列表.xlsx')
  ElMessage.success('导出成功')
}
</script>

<style scoped>
.resume-manage {
  padding: 20px;
}

.operation-row {
  margin-bottom: 20px;
}
</style>
