<template>
  <div class="hr-company-profile fade-in">
    <div class="page-header">
      <h2>企业信息</h2>
      <p>管理企业基本资料与资质认证</p>
    </div>

    <div class="profile-layout">
      <!-- ========== 左侧：编辑表单 ========== -->
      <div v-loading="loading" class="profile-main">
        <!-- 企业名片 -->
        <div class="content-card">
          <div class="company-card">
            <div class="company-avatar">
              <el-avatar :size="80" :src="form.logo" style="background:#E8F3FF;color:#165DFF;font-size:36px;font-weight:bold">
                {{ (form.name || '企').charAt(0) }}
              </el-avatar>
              <el-button size="small" @click="uploadLogo">更换Logo</el-button>
            </div>
            <div class="company-basic">
              <h2>{{ form.name || '企业名称' }}</h2>
              <p>
                <el-tag size="small">{{ form.industry || '未设置行业' }}</el-tag>
                <el-tag v-if="form.size" size="small" type="info">{{ form.size }}</el-tag>
              </p>
            </div>
          </div>
        </div>

        <!-- 公司信息 -->
        <div class="content-card">
          <div class="content-card-header">
            <span class="content-card-title">公司信息</span>
          </div>
          <el-form :model="form" label-width="100px" class="profile-form">
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="企业名称">
                  <el-input v-model="form.name" placeholder="请输入企业全称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="所属行业">
                  <el-select v-model="form.industry" placeholder="请选择行业" style="width:100%">
                    <el-option label="互联网/IT" value="互联网/IT" />
                    <el-option label="金融" value="金融" />
                    <el-option label="教育" value="教育" />
                    <el-option label="制造业" value="制造业" />
                    <el-option label="医疗" value="医疗" />
                    <el-option label="房地产" value="房地产" />
                    <el-option label="零售" value="零售" />
                    <el-option label="其他" value="其他" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="企业规模">
                  <el-select v-model="form.size" placeholder="请选择规模" style="width:100%">
                    <el-option label="少于50人" value="少于50人" />
                    <el-option label="50-150人" value="50-150人" />
                    <el-option label="150-500人" value="150-500人" />
                    <el-option label="500-2000人" value="500-2000人" />
                    <el-option label="2000人以上" value="2000人以上" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="所在城市">
                  <el-input v-model="form.city" placeholder="如：广州" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="详细地址">
              <el-input v-model="form.address" placeholder="企业办公地址" />
            </el-form-item>
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="联系人">
                  <el-input v-model="form.contactPerson" placeholder="联系人姓名" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="联系电话">
                  <el-input v-model="form.contactPhone" placeholder="联系人手机号" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="邮箱">
              <el-input v-model="form.contactEmail" placeholder="企业联系邮箱" />
            </el-form-item>
          </el-form>
        </div>

        <!-- 公司简介 -->
        <div class="content-card">
          <div class="content-card-header">
            <span class="content-card-title">公司简介</span>
          </div>
          <el-input v-model="form.description" type="textarea" :rows="6" placeholder="请输入公司简介、主营业务、发展历程等" />
        </div>

        <!-- 资质认证 -->
        <div class="content-card">
          <div class="content-card-header">
            <span class="content-card-title">资质认证</span>
          </div>
          <div v-if="form.licenseUrl" class="license-preview">
            <el-image :src="form.licenseUrl" style="max-width:300px;border-radius:8px;border:1px solid #E5E6EB" fit="contain" />
            <el-button size="small" type="danger" link @click="form.licenseUrl = ''">删除</el-button>
          </div>
          <div v-else class="license-upload">
            <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" @change="handleLicenseUpload">
              <el-button>上传营业执照</el-button>
              <template #tip><p class="tip-text">支持 JPG/PNG 格式</p></template>
            </el-upload>
          </div>
        </div>

        <!-- 保存按钮 -->
        <div class="save-bar">
          <el-button type="primary" size="large" :loading="saving" @click="saveCompany">
            保存信息
          </el-button>
        </div>
      </div>

      <!-- ========== 右侧：数据面板 ========== -->
      <div class="profile-sidebar">
        <!-- 公司数据概览 -->
        <div class="content-card sidebar-card">
          <div class="content-card-header">
            <span class="content-card-title">公司数据概览</span>
          </div>
          <div v-loading="statsLoading" class="stats-grid">
            <div class="stat-item">
              <div class="stat-icon" style="background:#E8F3FF;color:#165DFF">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="2" width="20" height="8" rx="2"/><rect x="2" y="14" width="20" height="8" rx="2"/></svg>
              </div>
              <div class="stat-info">
                <span class="stat-label">在招岗位</span>
                <span class="stat-value">{{ stats.activeJobCount ?? '-' }}</span>
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-icon" style="background:#FEF3E8;color:#F59E0B">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              </div>
              <div class="stat-info">
                <span class="stat-label">待处理简历</span>
                <span class="stat-value">{{ stats.pendingResumeCount ?? '-' }}</span>
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-icon" style="background:#E8FAF3;color:#10B981">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
              </div>
              <div class="stat-info">
                <span class="stat-label">总投递量</span>
                <span class="stat-value">{{ stats.resumeCount ?? '-' }}</span>
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-icon" style="background:#F0E8FF;color:#8B5CF6">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
              </div>
              <div class="stat-info">
                <span class="stat-label">面试总数</span>
                <span class="stat-value">{{ stats.interviewCount ?? '-' }}</span>
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-icon" style="background:#FDE8E8;color:#EF4444">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 12l2 2 4-4"/><path d="M22 12c0 5.523-4.477 10-10 10S2 17.523 2 12 6.477 2 12 2s10 4.477 10 10z"/></svg>
              </div>
              <div class="stat-info">
                <span class="stat-label">已录用</span>
                <span class="stat-value">{{ stats.hiredCount ?? '-' }}</span>
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-icon" style="background:#E8F6FF;color:#06B6D4">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
              </div>
              <div class="stat-info">
                <span class="stat-label">今日新增</span>
                <span class="stat-value">{{ stats.todayNewCount ?? '-' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 企业信息速览 -->
        <div class="content-card sidebar-card">
          <div class="content-card-header">
            <span class="content-card-title">企业信息速览</span>
          </div>
          <div class="info-list">
            <div class="info-row">
              <span class="info-label">联系人</span>
              <span class="info-value">{{ form.contactPerson || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">联系电话</span>
              <span class="info-value">{{ form.contactPhone || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">联系邮箱</span>
              <span class="info-value">{{ form.contactEmail || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">所在城市</span>
              <span class="info-value">{{ form.city || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">详细地址</span>
              <span class="info-value">{{ form.address || '未设置' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { companyAPI, statisticsAPI } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const companyId = ref(null)
const statsLoading = ref(false)
const stats = ref({})

const form = ref({
  name: '', industry: '', size: '', city: '', address: '',
  contactPerson: '', contactPhone: '', contactEmail: '',
  description: '', logo: '', licenseUrl: ''
})

onMounted(async () => {
  companyId.value = userStore.companyId
  if (companyId.value) {
    await Promise.all([loadCompany(), loadDashboardStats()])
  }
})

const loadCompany = async () => {
  loading.value = true
  try {
    const res = await companyAPI.getCompanyProfile(companyId.value)
    if (res.code === 200 && res.data) {
      const d = res.data
      form.value = {
        name: d.name || '',
        industry: d.industry || '',
        size: d.size || d.scale || '',
        city: d.city || d.location || '',
        address: d.address || '',
        contactPerson: d.contactPerson || d.contactInfo || '',
        contactPhone: d.contactPhone || '',
        contactEmail: d.contactEmail || '',
        description: d.description || '',
        logo: d.logo || '',
        licenseUrl: d.licenseUrl || ''
      }
    }
  } catch (e) {
    console.error('加载企业信息失败', e)
    ElMessage.error('加载企业信息失败')
  } finally {
    loading.value = false
  }
}

const loadDashboardStats = async () => {
  statsLoading.value = true
  try {
    const res = await statisticsAPI.getHrDashboard()
    if (res.code === 200 && res.data) {
      stats.value = res.data
    }
  } catch (e) {
    console.error('加载统计数据失败', e)
  } finally {
    statsLoading.value = false
  }
}

const uploadLogo = () => {
  ElMessage.info('Logo 上传功能可在企业设置中完成（文件上传接口）')
}

const handleLicenseUpload = (file) => {
  ElMessage.info('营业执照上传需要调用文件上传接口，暂未接入')
}

const saveCompany = async () => {
  if (!form.value.name) {
    ElMessage.warning('企业名称不能为空')
    return
  }
  saving.value = true
  try {
    const data = { ...form.value }
    const res = await companyAPI.updateCompany(companyId.value, data)
    if (res.code === 200) {
      ElMessage.success('保存成功')
      await Promise.all([loadCompany(), loadDashboardStats()])
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
/* ========== 双栏布局 ========== */
.profile-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}
.profile-main {
  flex: 7;
  min-width: 0;
}
.profile-sidebar {
  flex: 4;
  min-width: 320px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ========== 企业名片 ========== */
.company-card {
  display: flex;
  align-items: center;
  gap: 30px;
  padding: 10px 0;
}
.company-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}
.company-basic h2 {
  margin: 0 0 8px;
  font-size: 24px;
}
.company-basic p {
  margin: 0;
  display: flex;
  gap: 8px;
}
.profile-form {
  padding-top: 12px;
}

/* ========== 资质认证 ========== */
.license-preview {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}
.license-upload {
  padding: 20px 0;
}
.tip-text {
  color: #86909C;
  font-size: 12px;
  margin-top: 6px;
}

/* ========== 保存按钮 ========== */
.save-bar {
  margin-top: 30px;
  padding: 20px 0 40px;
}

/* ========== 右侧数据概览 ========== */
.stats-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 60px;
}
.stat-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 0;
  border-bottom: 1px solid #F2F3F5;
}
.stat-item:last-child {
  border-bottom: none;
}
.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}
.stat-label {
  font-size: 13px;
  color: #86909C;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #1D2129;
  line-height: 1.3;
}

/* ========== 右侧信息速览 ========== */
.info-list {
  display: flex;
  flex-direction: column;
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #F2F3F5;
}
.info-row:last-child {
  border-bottom: none;
}
.info-label {
  font-size: 13px;
  color: #86909C;
}
.info-value {
  font-size: 13px;
  color: #1D2129;
  font-weight: 500;
  text-align: right;
  max-width: 55%;
  word-break: break-word;
}

/* ========== Sidebar 卡片微调 ========== */
.sidebar-card .content-card-header {
  margin-bottom: 8px;
}
</style>
