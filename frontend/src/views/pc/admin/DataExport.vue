<template>
  <div class="data-export">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据导出</span>
        </div>
      </template>
      
      <el-form :model="exportForm" label-width="120px">
        <el-form-item label="导出类型">
          <el-select v-model="exportForm.type" placeholder="请选择导出类型">
            <el-option label="学生信息" value="student" />
            <el-option label="简历信息" value="resume" />
            <el-option label="投递记录" value="delivery" />
            <el-option label="企业信息" value="company" />
            <el-option label="岗位信息" value="job" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="筛选条件">
          <el-input v-model="exportForm.filter" placeholder="可选：输入筛选条件" />
        </el-form-item>
        
        <el-form-item label="导出格式">
          <el-radio-group v-model="exportForm.format">
            <el-radio label="xlsx">Excel (.xlsx)</el-radio>
            <el-radio label="csv">CSV (.csv)</el-radio>
            <el-radio label="pdf">PDF (.pdf)</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleExport" :loading="loading">
            导出数据
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { dataExportAPI } from '@/api'

const loading = ref(false)
const exportForm = ref({
  type: 'student',
  filter: '',
  format: 'xlsx'
})

async function handleExport() {
  loading.value = true
  try {
    const response = await dataExportAPI.export(exportForm.value)
    
    // 创建下载链接
    const blob = new Blob([response])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `export_${exportForm.value.type}_${Date.now()}.${exportForm.value.format}`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
    
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请检查后端服务')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.data-export {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
