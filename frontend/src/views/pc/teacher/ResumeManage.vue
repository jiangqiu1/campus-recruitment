<template>
  <div class="resume-manage fade-in">
    <div class="page-header">
      <h2>简历管理</h2>
      <p>查看学生简历 · AI 分析评分</p>
    </div>

    <div class="content-card">
      <div class="content-card-header">
        <span class="content-card-title">筛选条件</span>
      </div>
      <el-form :inline="true" class="search-form">
        <el-form-item label="班级">
          <el-select v-model="filters.classId" placeholder="选择班级" clearable @change="loadStudents" style="width:200px">
            <el-option v-for="c in classList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="简历状态">
          <el-select v-model="filters.resumeStatus" placeholder="全部" clearable style="width:140px">
            <el-option label="有简历" value="has" />
            <el-option label="无简历" value="none" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadStudents">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <el-table v-loading="loading" :data="studentList" style="width:100%" stripe @row-click="viewDetail">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="displayName" label="学生姓名" min-width="160" show-overflow-tooltip />
        <el-table-column prop="className" label="班级" width="150" />
        <el-table-column label="简历状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.hasResume ? 'success' : 'info'" size="small">
              {{ row.hasResume ? '有简历' : '无简历' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="completeness" label="简历完整度" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.completeness || 0" :stroke-width="12" />
          </template>
        </el-table-column>
        <el-table-column prop="deliveryCount" label="投递数" width="80" align="center" />
        <el-table-column label="AI 评分" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.aiScore" :type="scoreTag(row.aiScore)" size="small">{{ row.aiScore }}分</el-tag>
            <span v-else class="no-data">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click.stop="viewDetail(row)">查看简历</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && studentList.length === 0" class="empty-state">
        <el-empty :image-size="100" description="暂无学生数据" />
      </div>
    </div>

    <ResumeDetailDialog
      v-model:visible="detailVisible"
      :student-id="currentStudentId"
      :class-name="currentClassName"
      mode="teacher"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { classAPI, resumeAPI, statisticsAPI } from '@/api'
import ResumeDetailDialog from '@/components/ResumeDetailDialog.vue'

const loading = ref(false)
const classList = ref([])
const studentList = ref([])
const detailVisible = ref(false)
const currentStudentId = ref('')
const currentClassName = ref('')

const filters = ref({ classId: '', resumeStatus: '' })

onMounted(async () => {
  await loadClasses()
  // 默认加载所有学生
  await loadStudents()
})

const loadClasses = async () => {
  try {
    const res = await classAPI.getClasses({ page: 1, size: 200 })
    if (res.code === 200) {
      const data = res.data?.records || res.data || []
      classList.value = data
    }
  } catch (e) { console.error('加载班级失败', e) }
}

const loadStudents = async () => {
  loading.value = true
  try {
    let allStudents = []
    if (filters.value.classId) {
      // 按班级筛选
      const res = await classAPI.getClassStudents(filters.value.classId)
      if (res.code === 200) allStudents = res.data || []
    } else {
      // 加载所有班级的学生
      for (const cls of classList.value) {
        if (cls.deleted) continue
        try {
          const res = await classAPI.getClassStudents(cls.id)
          if (res.code === 200 && res.data) {
            allStudents = allStudents.concat(res.data.map(s => ({ ...s, className: cls.name })))
          }
        } catch (e) { /* 单个班级加载失败不影响其他 */ }
      }
    }
    let list = (allStudents || []).map(s => ({
      ...s,
      name: s.realName || s.name || '未知',
      displayName: (s.realName || s.name || '未知') + (s.username ? ' (' + s.username + ')' : ''),
      className: s.className || '',
      hasResume: (s.resumeComplete || 0) > 0 || s.resumeId ? true : false,
      completeness: s.resumeComplete || s.completeness || 0,
      deliveryCount: s.deliveryCount || 0,
      aiScore: s.aiScore || 0
    }))
    if (filters.value.resumeStatus === 'has') {
      list = list.filter(s => s.hasResume)
    } else if (filters.value.resumeStatus === 'none') {
      list = list.filter(s => !s.hasResume)
    }
    studentList.value = list
  } catch (e) {
    console.error('加载学生失败', e)
    studentList.value = []
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.value = { classId: '', resumeStatus: '' }
  studentList.value = []
}

const viewDetail = async (row) => {
  currentStudentId.value = row.id || row.studentId || ''
  currentClassName.value = row.className || ''
  detailVisible.value = true
}

const scoreTag = (score) => {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}
const scoreColor = (score) => {
  if (score >= 80) return '#10B981'
  if (score >= 60) return '#F59E0B'
  return '#EF4444'
}
</script>

<style scoped>
.search-form { padding: 12px 0; }
.content-card { background: #fff; border-radius: 16px; padding: 24px; margin-bottom: 24px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); }
.content-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.content-card-title { font-size: 16px; font-weight: 600; color: #1D2129; }
</style>
