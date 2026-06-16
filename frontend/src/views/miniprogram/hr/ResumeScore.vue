<template>
  <div class="resume-score">
    <h2>简历智能评分</h2>
    
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <el-icon><Document /></el-icon>
              <span>已评分简历</span>
            </div>
          </template>
          <div class="stat-value">{{ stats.scoredCount }}</div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <el-icon><Histogram /></el-icon>
              <span>平均分</span>
            </div>
          </template>
          <div class="stat-value">{{ stats.averageScore }}</div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <template #header>
            <div class="card-header">
              <el-icon><TrendCharts /></el-icon>
              <span>最高分</span>
            </div>
          </template>
          <div class="stat-value">{{ stats.topScore }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作栏 -->
    <el-row class="operation-row">
      <el-col :span="12">
        <el-button type="primary" @click="showScoreDialog">AI评分</el-button>
        <el-button type="success" @click="batchScore">批量评分</el-button>
      </el-col>
      <el-col :span="12" style="text-align: right;">
        <el-select v-model="filterJobId" placeholder="选择岗位" clearable style="width: 200px; margin-right: 10px;">
          <el-option v-for="job in jobList" :key="job.id" :label="job.title" :value="job.id" />
        </el-select>
        <el-button type="primary" @click="loadScores">筛选</el-button>
      </el-col>
    </el-row>

    <!-- 评分结果表格 -->
    <el-table :data="scoreList" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="jobId" label="岗位ID" width="100" />
      <el-table-column prop="deliveryId" label="投递ID" width="100" />
      <el-table-column prop="score" label="评分" width="150">
        <template #default="{ row }">
          <el-progress 
            :percentage="row.score" 
            :color="scoreColor(row.score)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="评分时间" width="180" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="viewScoreDetail(row)">详情</el-button>
          <el-button size="small" type="danger" @click="deleteScore(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      @current-change="loadScores"
      layout="total, prev, pager, next, jumper"
      style="margin-top: 20px; text-align: center;"
    />

    <!-- AI评分对话框 -->
    <el-dialog v-model="scoreDialogVisible" title="简历智能评分" width="500px">
      <el-form :model="scoreForm" label-width="100px">
        <el-form-item label="选择岗位" prop="jobId">
          <el-select v-model="scoreForm.jobId" placeholder="请选择岗位" style="width: 100%;" @change="loadDeliveries">
            <el-option v-for="job in jobList" :key="job.id" :label="job.title" :value="job.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="选择投递" prop="deliveryId">
          <el-select v-model="scoreForm.deliveryId" placeholder="请选择投递记录" style="width: 100%;">
            <el-option v-for="delivery in deliveryList" :key="delivery.id" :label="`投递#${delivery.id}`" :value="delivery.id" />
          </el-select>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="scoreDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="startScoring" :loading="scoring">
          {{ scoring ? '评分中...' : '开始评分' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 评分详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="评分详情" width="800px">
      <div v-if="currentScore">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="评分ID">{{ currentScore.id }}</el-descriptions-item>
          <el-descriptions-item label="岗位ID">{{ currentScore.jobId }}</el-descriptions-item>
          <el-descriptions-item label="投递ID">{{ currentScore.deliveryId }}</el-descriptions-item>
          <el-descriptions-item label="评分">
            <el-progress :percentage="currentScore.score" :color="scoreColor(currentScore.score)" />
          </el-descriptions-item>
          <el-descriptions-item label="评分时间" :span="2">{{ currentScore.createTime }}</el-descriptions-item>
          <el-descriptions-item label="详细分析" :span="2">
            {{ currentScore.analysis || '无详细分析' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Document, Histogram, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { resumeScoreAPI, jobAPI, deliveryAPI } from '@/api/index.js'

const scoreList = ref([])
const jobList = ref([])
const deliveryList = ref([])
const filterJobId = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const stats = ref({
  scoredCount: 0,
  averageScore: 0,
  topScore: 0
})

const scoreDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const scoring = ref(false)
const currentScore = ref(null)

const scoreForm = ref({
  jobId: null,
  deliveryId: null
})

onMounted(() => {
  loadScores()
  loadStats()
  loadJobs()
})

const loadScores = async () => {
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    
    if (filterJobId.value) {
      params.jobId = filterJobId.value
    }
    
    const res = await resumeScoreAPI.getScores(params)
    if (res.code === 200) {
      scoreList.value = res.data.records
      total.value = res.data.total
    }
  } catch (error) {
    ElMessage.error('加载评分列表失败')
  }
}

const loadStats = async () => {
  try {
    // 模拟数据
    stats.value = {
      scoredCount: 86,
      averageScore: 76.5,
      topScore: 95
    }
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

const loadJobs = async () => {
  try {
    const companyId = localStorage.getItem('companyId') || 1
    const res = await jobAPI.getJobsByCompany(companyId, {})
    if (res.code === 200) {
      jobList.value = res.data
    }
  } catch (error) {
    console.error('加载岗位列表失败', error)
  }
}

const showScoreDialog = () => {
  scoreForm.value = { jobId: null, deliveryId: null }
  scoreDialogVisible.value = true
  
  // 加载该岗位的投递记录
  if (scoreForm.value.jobId) {
    loadDeliveries(scoreForm.value.jobId)
  }
}

const loadDeliveries = async (jobId) => {
  try {
    const res = await deliveryAPI.getDeliveriesByJob(jobId)
    if (res.code === 200) {
      deliveryList.value = res.data
    }
  } catch (error) {
    console.error('加载投递记录失败', error)
  }
}

const startScoring = async () => {
  if (!scoreForm.value.jobId || !scoreForm.value.deliveryId) {
    ElMessage.warning('请选择完整的评分信息')
    return
  }

  scoring.value = true
  
  try {
    const res = await resumeScoreAPI.scoreResume({
      jobId: scoreForm.value.jobId,
      deliveryId: scoreForm.value.deliveryId
    })
    
    if (res.code === 200) {
      ElMessage.success('评分完成')
      scoreDialogVisible.value = false
      loadScores()
      loadStats()
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '评分失败')
  } finally {
    scoring.value = false
  }
}

const batchScore = async () => {
  if (!filterJobId.value) {
    ElMessage.warning('请先选择一个岗位')
    return
  }

  try {
    const res = await resumeScoreAPI.batchScoreResumes(filterJobId.value)
    if (res.code === 200) {
      ElMessage.success(`批量评分完成，共评分 ${res.data.scoredCount} 份简历`)
      loadScores()
      loadStats()
    }
  } catch (error) {
    ElMessage.error('批量评分失败')
  }
}

const viewScoreDetail = async (row) => {
  try {
    const res = await resumeScoreAPI.getScoreDetail(row.id)
    if (res.code === 200) {
      currentScore.value = res.data
      detailDialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('加载评分详情失败')
  }
}

const deleteScore = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该评分记录吗？', '提示', { type: 'warning' })
    const res = await resumeScoreAPI.deleteScore(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadScores()
      loadStats()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const scoreColor = (score) => {
  if (score >= 90) return '#67C23A'
  if (score >= 75) return '#E6A23C'
  return '#F56C6C'
}
</script>

<style scoped>
.resume-score {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-value {
  font-size: 36px;
  font-weight: bold;
  color: #409EFF;
}

.operation-row {
  margin-bottom: 20px;
}
</style>
