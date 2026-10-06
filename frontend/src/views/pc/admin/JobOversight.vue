<template>
  <div class="job-oversight fade-in">
    <div class="page-header">
      <h2>岗位监管</h2>
      <p>查看全平台所有岗位 · 下架 · 删除</p>
    </div>

    <div class="stat-grid">
      <div class="stat-card" style="border-left-color:#165DFF">
        <h3>全部岗位</h3>
        <div class="num">{{ totalJobs }}</div>
      </div>
      <div class="stat-card" style="border-left-color:#10B981">
        <h3>招聘中</h3>
        <div class="num">{{ activeJobs }}</div>
      </div>
      <div class="stat-card" style="border-left-color:#F59E0B">
        <h3>已暂停</h3>
        <div class="num">{{ pausedJobs }}</div>
      </div>
      <div class="stat-card" style="border-left-color:#EF4444">
        <h3>已关闭</h3>
        <div class="num">{{ closedJobs }}</div>
      </div>
    </div>

    <div class="content-card">
      <div class="content-card-header">
        <span class="content-card-title">岗位列表</span>
      </div>
      <el-form :inline="true" class="search-form">
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable style="width:130px" @change="loadJobs">
            <el-option label="草稿" value="0" />
            <el-option label="招聘中" value="1" />
            <el-option label="已关闭" value="2" />
            <el-option label="已暂停" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="keyword" placeholder="岗位名称/企业名称" clearable style="width:220px" @keyup.enter="loadJobs" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadJobs">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="jobList" style="width:100%" stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="title" label="岗位名称" min-width="160" />
        <el-table-column prop="companyName" label="所属企业" width="160" />
        <el-table-column prop="createdByName" label="发布者" width="100" />
        <el-table-column prop="education" label="学历" width="90" />
        <el-table-column label="薪资" width="120">
          <template #default="{ row }">
            {{ row.salaryRange || '面议' }}
          </template>
        </el-table-column>
        <el-table-column prop="deliveryCount" label="投递" width="60" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" type="warning" link size="small" @click="closeJob(row)">下架</el-button>
            <el-button v-if="row.status === 2 || row.status === 3" type="primary" link size="small" @click="publishJob(row)">上架</el-button>
            <el-popconfirm title="确认删除此岗位？" @confirm="deleteJob(row)">
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
      <el-pagination v-if="total > pageSize" background layout="prev,pager,next" :total="total" :page-size="pageSize" :current-page="currentPage" @current-change="onPageChange" style="margin-top:20px;justify-content:center" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { jobAPI, companyAPI, userAPI } from '@/api'
import { formatDate } from '@/utils/formatDate'

const loading = ref(false)
const jobList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const statusFilter = ref('')
const keyword = ref('')
const totalJobs = ref(0)
const activeJobs = ref(0)
const pausedJobs = ref(0)
const closedJobs = ref(0)

onMounted(() => { loadJobs() })

const loadJobs = async () => {
  loading.value = true
  try {
    const params = { page: 1, size: 500 }
    if (statusFilter.value !== '') params.status = parseInt(statusFilter.value)
    // 不传 keyword 到后端，统一在前端做客户端过滤（后端只按岗位名称查，查不到企业名）
    const res = await jobAPI.getJobs(params)
    if (res.code === 200) {
      const data = res.data?.list || res.data?.records || res.data || []
      const jobArr = Array.isArray(data) ? data : []
      
      // 批量获取企业名称和发布者姓名（避免N+1查询）
      const companyIds = [...new Set(jobArr.filter(j => j.companyId).map(j => j.companyId))]
      const userIds = [...new Set(jobArr.filter(j => j.createdBy).map(j => j.createdBy))]
      
      const [companiesRes, usersRes] = await Promise.all([
        companyIds.length > 0 ? companyAPI.getCompanies({ page: 1, size: 500 }).catch(() => null) : null,
        userIds.length > 0 ? userAPI.getUsers({ page: 1, size: 500 }).catch(() => null) : null
      ])
      
      const companyMap = {}
      if (companiesRes?.code === 200) {
        const clist = companiesRes.data?.records || companiesRes.data || []
        ;(Array.isArray(clist) ? clist : []).forEach(c => {
          companyMap[c.id] = { name: c.name, shortName: c.shortName }
        })
      }
      const userMap = {}
      if (usersRes?.code === 200) {
        const ulist = usersRes.data?.records || usersRes.data || []
        ;(Array.isArray(ulist) ? ulist : []).forEach(u => { userMap[u.id] = u.realName || u.username || '' })
      }
      
      jobList.value = jobArr.map(job => ({
        ...job,
        companyName: companyMap[job.companyId]?.name || '',
        companyShortName: companyMap[job.companyId]?.shortName || '',
        createdByName: userMap[job.createdBy] || ''
      }))
      // 关键字匹配岗位名称、企业全称和企业简称
      if (keyword.value.trim()) {
        const kw = keyword.value.trim().toLowerCase()
        jobList.value = jobList.value.filter(j =>
          (j.title || '').toLowerCase().includes(kw) ||
          (j.companyName || '').toLowerCase().includes(kw) ||
          (j.companyShortName || '').toLowerCase().includes(kw)
        )
      }
      total.value = jobList.value.length
      updateStats(jobArr)
    }
  } catch (e) {
    console.error('加载岗位失败', e)
  } finally {
    loading.value = false
  }
}

const updateStats = (list) => {
  totalJobs.value = list.length
  activeJobs.value = list.filter(j => j.status === 1).length
  pausedJobs.value = list.filter(j => j.status === 3).length
  closedJobs.value = list.filter(j => j.status === 2).length
}

const resetFilters = () => {
  statusFilter.value = ''
  keyword.value = ''
  currentPage.value = 1
  loadJobs()
}

const onPageChange = (page) => {
  currentPage.value = page
  loadJobs()
}

const closeJob = async (row) => {
  try {
    const res = await jobAPI.closeJob(row.id)
    if (res.code === 200) {
      ElMessage.success('已下架')
      row.status = 2
      await loadJobs()
    }
  } catch (e) { ElMessage.error('下架失败') }
}

const publishJob = async (row) => {
  try {
    const res = await jobAPI.publishJob(row.id)
    if (res.code === 200) {
      ElMessage.success('已上架')
      row.status = 1
      await loadJobs()
    }
  } catch (e) { ElMessage.error('上架失败') }
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
</style>
