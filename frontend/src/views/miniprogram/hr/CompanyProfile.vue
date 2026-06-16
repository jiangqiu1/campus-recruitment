<template>
  <div class="company-profile">
    <h2>企业信息管理</h2>
    
    <!-- 企业信息表单 -->
    <el-card shadow="hover">
      <template #header>
        <span>企业基本信息</span>
      </template>
      
      <el-form :model="companyForm" label-width="120px" :disabled="!isEditing">
        <el-form-item label="企业名称" prop="name">
          <el-input v-model="companyForm.name" />
        </el-form-item>
        
        <el-form-item label="行业领域" prop="industry">
          <el-input v-model="companyForm.industry" />
        </el-form-item>
        
        <el-form-item label="合作等级" prop="cooperationLevel">
          <el-rate v-model="companyForm.cooperationLevel" disabled show-score />
        </el-form-item>
        
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="companyForm.contactPerson" />
        </el-form-item>
        
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="companyForm.contactPhone" />
        </el-form-item>
        
        <el-form-item label="企业地址" prop="address">
          <el-input v-model="companyForm.address" type="textarea" :rows="3" />
        </el-form-item>
        
        <el-form-item label="企业简介" prop="description">
          <el-input v-model="companyForm.description" type="textarea" :rows="5" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button v-if="!isEditing" type="primary" @click="startEdit">编辑信息</el-button>
        <template v-else>
          <el-button @click="cancelEdit">取消</el-button>
          <el-button type="primary" @click="saveCompany">保存</el-button>
        </template>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { companyAPI } from '@/api/index.js'

const companyForm = ref({
  id: null,
  name: '',
  industry: '',
  cooperationLevel: 3,
  contactPerson: '',
  contactPhone: '',
  address: '',
  description: ''
})

const isEditing = ref(false)
const originalData = ref(null)

onMounted(() => {
  loadCompanyInfo()
})

const loadCompanyInfo = async () => {
  try {
    // 获取当前HR所在的企业ID（假设从登录信息中获取）
    const companyId = localStorage.getItem('companyId') || 1 // 模拟数据
    
    const res = await companyAPI.getCompanyProfile(companyId)
    if (res.code === 200) {
      companyForm.value = res.data
    }
  } catch (error) {
    ElMessage.error('加载企业信息失败')
  }
}

const startEdit = () => {
  originalData.value = { ...companyForm.value }
  isEditing.value = true
}

const cancelEdit = () => {
  companyForm.value = { ...originalData.value }
  isEditing.value = false
}

const saveCompany = async () => {
  try {
    const res = await companyAPI.updateCompany(companyForm.value.id, companyForm.value)
    if (res.code === 200) {
      ElMessage.success('保存成功')
      isEditing.value = false
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  }
}
</script>

<style scoped>
.company-profile {
  padding: 20px;
}

.el-form {
  max-width: 800px;
  margin: 0 auto;
}
</style>
