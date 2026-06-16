<template>
  <div class="ai-parse">
    <h2>AI简历解析</h2>
    
    <!-- 上传区域 -->
    <el-card shadow="hover" class="upload-card">
      <template #header>
        <span>上传简历文件</span>
      </template>
      
      <el-upload
        class="upload-demo"
        action="#"
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="1"
        accept=".pdf,.doc,.docx"
      >
        <template #trigger>
          <el-button type="primary">选择文件</el-button>
        </template>
        <template #tip>
          <div class="el-upload__tip">
            支持 PDF、Word 格式，单个文件不超过 10MB
          </div>
        </template>
      </el-upload>
      
      <div style="margin-top: 20px;">
        <el-button type="success" @click="startParse" :loading="parsing" :disabled="!selectedFile">
          {{ parsing ? '解析中...' : '开始解析' }}
        </el-button>
      </div>
    </el-card>

    <!-- 解析结果 -->
    <el-card shadow="hover" class="result-card" v-if="parseResult">
      <template #header>
        <span>解析结果</span>
      </template>
      
      <el-descriptions :column="2" border>
        <el-descriptions-item label="姓名">{{ parseResult.name }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ parseResult.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ parseResult.email }}</el-descriptions-item>
        <el-descriptions-item label="学历">{{ parseResult.education }}</el-descriptions-item>
        <el-descriptions-item label="求职意向" :span="2">{{ parseResult.jobTarget }}</el-descriptions-item>
        <el-descriptions-item label="技能标签" :span="2">
          <el-tag v-for="tag in parseTags" :key="tag" style="margin-right: 5px;">
            {{ tag }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="自我评价" :span="2">{{ parseResult.selfEvaluation }}</el-descriptions-item>
        <el-descriptions-item label="工作经历" :span="2">{{ parseResult.workExperience }}</el-descriptions-item>
        <el-descriptions-item label="项目经历" :span="2">{{ parseResult.projectExperience }}</el-descriptions-item>
      </el-descriptions>
      
      <div style="margin-top: 20px; text-align: center;">
        <el-button type="primary" @click="saveResume">保存到简历库</el-button>
        <el-button @click="parseResult = null">重新解析</el-button>
      </div>
    </el-card>

    <!-- 历史解析记录 -->
    <el-card shadow="hover" class="history-card" style="margin-top: 20px;">
      <template #header>
        <span>历史解析记录</span>
      </template>
      
      <el-table :data="historyList" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="fileName" label="文件名" />
        <el-table-column prop="confidenceScore" label="置信度" width="100">
          <template #default="{ row }">
            <el-progress 
              type="circle" 
              :percentage="row.confidenceScore * 100" 
              :width="50"
            />
          </template>
        </el-table-column>
        <el-table-column prop="isManualCorrected" label="人工修正" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isManualCorrected ? 'success' : 'warning'">
              {{ row.isManualCorrected ? '已修正' : '未修正' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="解析时间" width="180" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="viewParseDetail(row)">查看</el-button>
            <el-button size="small" type="danger" @click="deleteParseLog(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { resumeAPI, aiParseAPI } from '@/api'

const selectedFile = ref(null)
const parsing = ref(false)
const parseResult = ref(null)
const historyList = ref([])

const parseTags = computed(() => {
  if (!parseResult.value || !parseResult.value.skillTags) return []
  return parseResult.value.skillTags.split(',').map(tag => tag.trim())
})

onMounted(() => {
  loadHistory()
})

const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

const startParse = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  parsing.value = true
  
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  formData.append('teacherId', localStorage.getItem('userId'))

  try {
    // 模拟AI解析
    setTimeout(() => {
      parseResult.value = {
        name: '张三',
        phone: '13800138000',
        email: 'zhangsan@example.com',
        education: '本科',
        jobTarget: 'Java开发工程师',
        skillTags: 'Java,Spring Boot,MySQL,Redis',
        selfEvaluation: '热爱编程，学习能力强',
        workExperience: '无',
        projectExperience: '校园项目经验'
      }
      parsing.value = false
      ElMessage.success('解析完成')
      loadHistory()
    }, 2000)
  } catch (error) {
    ElMessage.error('解析失败')
    parsing.value = false
  }
}

const saveResume = async () => {
  try {
    await resumeAPI.createResume(parseResult.value)
    ElMessage.success('保存成功')
    parseResult.value = null
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const loadHistory = async () => {
  try {
    const res = await aiParseAPI.getParseLogs({
      teacherId: localStorage.getItem('userId')
    })
    if (res.code === 200) {
      historyList.value = res.data
    }
  } catch (error) {
    console.error('加载历史记录失败', error)
  }
}

const viewParseDetail = (row) => {
  // 查看解析详情
  console.log('查看详情', row)
}

const deleteParseLog = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该解析记录吗？', '提示', { type: 'warning' })
    await aiParseAPI.deleteParseLog(row.id)
    ElMessage.success('删除成功')
    loadHistory()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}
</script>

<style scoped>
.ai-parse {
  padding: 20px;
}

.upload-card, .result-card, .history-card {
  margin-bottom: 20px;
}
</style>
