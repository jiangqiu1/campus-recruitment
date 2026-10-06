<template>
  <div class="data-export fade-in">
    <div class="page-header">
      <h2>数据导出</h2>
      <p>导出系统数据 · 下载报表</p>
    </div>
    <el-card>
      
      <el-form :model="exportForm" label-width="120px">
        <el-form-item label="导出类型">
          <el-select v-model="exportForm.type" placeholder="请选择导出类型">
            <el-option label="学生信息 — 注册学生名单与基本信息" value="student" />
            <el-option label="简历信息 — 学生简历明细（含完整度）" value="resume" />
            <el-option label="投递记录 — 投递流水与当前状态" value="delivery" />
            <el-option label="企业信息 — 入驻企业与资质概要" value="company" />
            <el-option label="岗位信息 — 全部岗位及在招状态" value="job" />
            <el-option label="AI调用记录（多模型对比）— 供方/任务/耗时/降级明细" value="ai" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="筛选条件">
          <el-input v-model="exportForm.filter" :placeholder="filterPlaceholder" clearable />
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
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { dataExportAPI } from '@/api'

const loading = ref(false)
const exportForm = ref({
  type: 'student',
  filter: '',
  format: 'xlsx'
})

const filterPlaceholder = computed(() => {
  const tips = {
    student: '按姓名 / 用户名 / 手机号筛选',
    company: '按企业名称 / 行业 / 联系人筛选',
    job: '按岗位名称 / 学历要求 / 工作地点筛选',
    delivery: '按学生ID / 岗位ID筛选',
    resume: '按学生ID / 学历 / 求职意向筛选',
    ai: 'AI调用记录导出最近5000条（含模型、耗时、降级标记），筛选条件不适用'
  }
  return tips[exportForm.value.type] || '输入筛选条件（可选）'
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
