<template>
  <div class="hr-job-manage fade-in">
    <div class="page-header">
      <div class="page-header-left">
        <el-button text @click="$router.back()" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <div>
          <h2>岗位管理</h2>
          <p>发布 · 编辑 · 下架 · 暂停岗位</p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="content-card-header">
        <span class="content-card-title">岗位列表</span>
        <el-button type="primary" @click="showCreateDialog">
          <el-icon style="margin-right:4px"><Plus /></el-icon>
          发布新岗位
        </el-button>
      </div>

      <el-form :inline="true" class="search-form">
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable style="width:130px" @change="loadJobs">
            <el-option label="草稿" value="0" />
            <el-option label="招聘中" value="1" />
            <el-option label="已暂停" value="2" />
            <el-option label="已关闭" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="keyword" placeholder="岗位名称" clearable style="width:200px" @keyup.enter="loadJobs" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadJobs">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="jobList" style="width:100%" stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="title" label="岗位名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="type" label="岗位类型" width="100" />
        <el-table-column label="薪资" width="150">
          <template #default="{ row }">
            {{ row.salaryMin && row.salaryMax ? `${row.salaryMin}k-${row.salaryMax}k` : (row.salary || '面议') }}
          </template>
        </el-table-column>
        <el-table-column prop="deliveryCount" label="投递数" width="80" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="270" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button v-if="row.status === 0" type="primary" link size="small" @click="publishJob(row)">发布</el-button>
            <el-button v-if="row.status === 1" type="warning" link size="small" @click="pauseJob(row)">暂停</el-button>
            <el-button v-if="row.status === 2" type="primary" link size="small" @click="publishJob(row)">上架</el-button>
            <el-button v-if="row.status === 3" type="primary" link size="small" @click="publishJob(row)">恢复</el-button>
            <el-button v-if="row.status === 1 || row.status === 3" type="danger" link size="small" @click="closeJob(row)">下架</el-button>
            <el-popconfirm title="确认删除？" @confirm="deleteJob(row)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && jobList.length === 0" class="empty-state">
        <el-empty :image-size="100" description="暂无岗位" />
      </div>
    </div>

    <!-- 岗位编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑岗位' : '发布新岗位'" width="720px" top="5vh"
      append-to-body modal-class="job-overlay">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="岗位名称" prop="title">
          <el-input v-model="form.title" placeholder="请输入岗位名称" maxlength="100" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="岗位类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择" style="width:100%">
                <el-option label="全职" value="全职" />
                <el-option label="实习" value="实习" />
                <el-option label="兼职" value="兼职" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="招聘人数" prop="headcount">
              <el-input-number v-model="form.headcount" :min="1" :max="99" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="薪资范围" prop="salary">
          <el-row :gutter="10">
            <el-col :span="10">
              <el-input-number v-model="form.salaryMin" :min="0" :max="200" placeholder="最低" style="width:100%" /> 
            </el-col>
            <el-col :span="4" style="text-align:center;line-height:32px">~</el-col>
            <el-col :span="10">
              <el-input-number v-model="form.salaryMax" :min="0" :max="200" placeholder="最高" style="width:100%" />
            </el-col>
          </el-row>
        </el-form-item>
        <el-form-item label="工作城市">
          <el-input v-model="form.city" placeholder="如：广州" />
        </el-form-item>
        <el-form-item label="工作地址">
          <el-input v-model="form.address" placeholder="详细工作地址" />
        </el-form-item>
        <el-form-item label="学历要求" prop="education">
          <el-select v-model="form.education" placeholder="请选择" style="width:100%" :teleported="false">
            <el-option label="不限" value="不限" />
            <el-option label="大专" value="大专" />
            <el-option label="本科" value="本科" />
            <el-option label="硕士" value="硕士" />
            <el-option label="博士" value="博士" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="5" placeholder="岗位职责、任职要求等" />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="form.deadline" type="date" placeholder="选填" style="width:100%" value-format="YYYY-MM-DD" :teleported="false" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveJob">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, ArrowLeft } from '@element-plus/icons-vue'
import { jobAPI, companyAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { formatDate } from '@/utils/formatDate'

const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const jobList = ref([])
const statusFilter = ref('')
const keyword = ref('')
const currentCompanyId = ref(null)

const form = ref({
  title: '', type: '全职', headcount: 1,
  salaryMin: 5, salaryMax: 10,
  city: '', address: '',
  education: '不限', description: '',
  deadline: '', companyId: null
})

const rules = {
  title: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择岗位类型', trigger: 'change' }],
  education: [{ required: true, message: '请选择学历要求', trigger: 'change' }],
  description: [{ required: true, message: '请输入岗位描述', trigger: 'blur' }]
}

onMounted(async () => {
  // 获取HR所属企业
  if (userStore.companyId) {
    currentCompanyId.value = userStore.companyId
    form.value.companyId = userStore.companyId
  }
  await loadJobs()
})

const loadJobs = async () => {
  loading.value = true
  try {
    const params = { page: 1, size: 200 }
    if (statusFilter.value !== '') params.status = statusFilter.value
    if (keyword.value) params.keyword = keyword.value
    const res = await jobAPI.getJobsByCompany(currentCompanyId.value, params)
    if (res.code === 200) {
      let data = res.data?.records || res.data || []
      // 客户端过滤 keyword（后端未实现 keyword 查询）
      if (keyword.value && data.length) {
        const kw = keyword.value.toLowerCase()
        data = data.filter(j => (j.title || '').toLowerCase().includes(kw))
      }
      jobList.value = data
    }
  } catch (e) {
    console.error('加载岗位失败', e)
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  statusFilter.value = ''
  keyword.value = ''
  loadJobs()
}

const showCreateDialog = () => {
  isEdit.value = false
  form.value = {
    title: '', type: '全职', headcount: 1,
    salaryMin: 5, salaryMax: 10,
    city: '', address: '',
    education: '不限', description: '',
    deadline: '', companyId: currentCompanyId.value
  }
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  form.value = {
    title: row.title,
    type: row.type || '全职',
    headcount: row.headcount || 1,
    salaryMin: row.salaryMin || 0,
    salaryMax: row.salaryMax || 0,
    city: row.city || '',
    address: row.address || '',
    education: row.education || '不限',
    description: row.description || '',
    deadline: row.deadline || '',
    companyId: currentCompanyId.value
  }
  form.value._id = row.id
  dialogVisible.value = true
}

const saveJob = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const data = { ...form.value }
    delete data._id
    if (isEdit.value) {
      const res = await jobAPI.updateJob(form.value._id, data)
      if (res.code === 200) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        await loadJobs()
      }
    } else {
      data.status = 0 // 草稿
      const res = await jobAPI.createJob(data)
      if (res.code === 200) {
        ElMessage.success('创建成功')
        dialogVisible.value = false
        await loadJobs()
      }
    }
  } catch (e) {
    console.error('保存失败', e)
  } finally {
    saving.value = false
  }
}

const publishJob = async (row) => {
  try {
    const res = await jobAPI.publishJob(row.id)
    if (res.code === 200) {
      ElMessage.success('已发布')
      row.status = 1
    }
  } catch (e) { ElMessage.error('发布失败') }
}

const pauseJob = async (row) => {
  try {
    const res = await jobAPI.pauseJob(row.id)
    if (res.code === 200) {
      ElMessage.success('已暂停')
      row.status = 3
    }
  } catch (e) { ElMessage.error('暂停失败') }
}

const closeJob = async (row) => {
  try {
    const res = await jobAPI.closeJob(row.id)
    if (res.code === 200) {
      ElMessage.success('已关闭')
      row.status = 3
    }
  } catch (e) { ElMessage.error('关闭失败') }
}

const deleteJob = async (row) => {
  try {
    const res = await jobAPI.deleteJob(row.id)
    if (res.code === 200) {
      ElMessage.success('已删除')
      await loadJobs()
    }
  } catch (e) { ElMessage.error('删除失败') }
}

const statusTag = (s) => {
  const map = { 0: 'info', 1: 'success', 2: 'danger', 3: 'warning' }
  return map[s] || 'info'
}
const statusLabel = (s) => {
  const map = { 0: '草稿', 1: '招聘中', 2: '已关闭', 3: '已暂停' }
  return map[s] || '未知'
}
const formatTime = (t) => formatDate(t)
</script>

<style scoped>
.search-form { padding: 12px 0; }
.content-card { background: #fff; border-radius: 16px; padding: 24px; margin-bottom: 24px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); }
.content-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.content-card-title { font-size: 16px; font-weight: 600; color: #1D2129; }
.page-header-left { display: flex; align-items: center; gap: 16px; }
.back-btn { font-size: 14px; color: #4E5969; padding: 6px 12px; border-radius: 8px; }
.back-btn:hover { background: #F2F3F5; color: #1D2129; }
</style>

<style>
.job-overlay {
  position: fixed !important;
  top: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  left: 0 !important;
  background: rgba(0, 0, 0, 0.45) !important;
  z-index: 9999 !important;
}
</style>
