<template>
  <div class="company-detail fade-in">
    <div class="page-header">
      <h2>企业详情</h2>
      <p>查看合作企业信息</p>
    </div>

    <div class="content-card">
      <div class="content-card-header">
        <span class="content-card-title">筛选企业</span>
      </div>
      <el-form :inline="true">
        <el-form-item label="企业名称">
          <el-input v-model="searchKeyword" placeholder="输入企业名称搜索" clearable style="width:260px" @keyup.enter="loadCompanies" />
        </el-form-item>
        <el-form-item label="合作等级">
          <el-select v-model="filterLevel" placeholder="全部" clearable style="width:140px" @change="loadCompanies">
            <el-option label="核心合作" value="核心" />
            <el-option label="一般合作" value="一般" />
            <el-option label="意向合作" value="意向" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadCompanies">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <el-table v-loading="loading" :data="companyList" style="width:100%" stripe @row-click="showDetail">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column label="企业名称" min-width="180">
          <template #default="{ row }">
            <div class="company-cell">
              <el-avatar :size="36" :src="row.logo" style="background:#E8F3FF;color:#165DFF;font-weight:bold">
                {{ (row.name || '企').charAt(0) }}
              </el-avatar>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="industry" label="行业" width="130" />
        <el-table-column label="合作等级" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.cooperationLevel)" size="small">
              {{ row.cooperationLevel || '未分级' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '已认证' : '待审核' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click.stop="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && companyList.length === 0" class="empty-state">
        <el-empty :image-size="100" description="暂无企业数据" />
      </div>
    </div>

    <!-- 企业详情对话框 -->
    <el-dialog v-model="detailVisible" :title="currentCompany?.name || '企业详情'" width="680px" top="5vh"
      append-to-body modal-class="company-overlay">
      <div v-if="currentCompany" class="detail-body">
        <div class="company-header">
          <el-avatar :size="72" :src="currentCompany.logo" style="background:#E8F3FF;color:#165DFF;font-size:32px;font-weight:bold">
            {{ (currentCompany.name || '企').charAt(0) }}
          </el-avatar>
          <div class="company-header-info">
            <h3>{{ currentCompany.name }}</h3>
            <p>
              <el-tag size="small">{{ currentCompany.industry || '未设置行业' }}</el-tag>
              <el-tag v-if="currentCompany.size" size="small" type="info">{{ currentCompany.size }}</el-tag>
              <el-tag :type="levelTag(currentCompany.cooperationLevel)" size="small">
                {{ currentCompany.cooperationLevel || '未分级' }}
              </el-tag>
            </p>
          </div>
        </div>
        <el-divider />
        <div class="company-sections">
          <div class="section">
            <h4>公司信息</h4>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="联系人">{{ currentCompany.contactPerson || '-' }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ currentCompany.contactPhone || '-' }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ currentCompany.contactEmail || '-' }}</el-descriptions-item>
              <el-descriptions-item label="所在城市">{{ currentCompany.city || '-' }}</el-descriptions-item>
              <el-descriptions-item label="详细地址">{{ currentCompany.address || '-' }}</el-descriptions-item>
              <el-descriptions-item label="企业规模">{{ currentCompany.size || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
          <div class="section">
            <h4>公司简介</h4>
            <p class="text-block">{{ currentCompany.description || '暂无简介' }}</p>
          </div>
          <div v-if="currentCompany.licenseUrl" class="section">
            <h4>资质认证</h4>
            <el-image style="max-width:300px;border-radius:8px;border:1px solid #E5E6EB" :src="currentCompany.licenseUrl" fit="contain" />
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { companyAPI } from '@/api'

const loading = ref(false)
const searchKeyword = ref('')
const filterLevel = ref('')
const companyList = ref([])
const detailVisible = ref(false)
const currentCompany = ref(null)

onMounted(() => { loadCompanies() })

const loadCompanies = async () => {
  loading.value = true
  try {
    const params = { page: 1, size: 200 }
    if (filterLevel.value) params.cooperationLevel = filterLevel.value
    if (searchKeyword.value) params.keyword = searchKeyword.value
    const res = await companyAPI.getCompanies(params)
    if (res.code === 200) {
      const data = res.data?.records || res.data || []
      companyList.value = data
    }
  } catch (e) {
    console.error('加载企业失败', e)
  } finally {
    loading.value = false
  }
}

const showDetail = (row) => {
  currentCompany.value = row
  detailVisible.value = true
}

const levelTag = (level) => {
  const map = { '核心': 'success', '一般': 'primary', '意向': 'warning' }
  return map[level] || 'info'
}
</script>

<style scoped>
.company-cell { display: flex; align-items: center; gap: 10px; }
.company-header { display: flex; align-items: center; gap: 24px; }
.company-header-info h3 { margin: 0 0 8px; font-size: 22px; }
.company-header-info p { margin: 0; display: flex; gap: 8px; flex-wrap: wrap; }
.company-sections .section { margin-bottom: 20px; }
.company-sections h4 { margin-bottom: 10px; color: #1D2129; font-size: 16px; border-left: 3px solid #165DFF; padding-left: 10px; }
.text-block { line-height: 1.8; color: #4E5969; white-space: pre-wrap; }
</style>

<style>
.company-overlay {
  position: fixed !important;
  top: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  left: 0 !important;
  background: rgba(0, 0, 0, 0.45) !important;
  z-index: 9999 !important;
}
</style>
