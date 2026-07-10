<template>
  <div class="company-manage fade-in">
    <div class="page-header">
      <h2>企业管理</h2>
      <p>查看和管理注册企业信息</p>
    </div>
    
    <!-- 操作栏 -->
    <el-row class="operation-row">
      <el-col :span="12">
        <el-button type="primary" @click="showAddDialog">添加企业</el-button>
        <el-button type="danger" @click="batchDelete" :disabled="selectedIds.length === 0">批量删除</el-button>
      </el-col>
      <el-col :span="12" style="text-align: right;">
        <el-input v-model="searchKeyword" placeholder="搜索企业名称" style="width: 300px;" clearable @keyup.enter="loadCompanies">
          <template #append>
            <el-button @click="loadCompanies"><el-icon><Search /></el-icon></el-button>
          </template>
        </el-input>
      </el-col>
    </el-row>

    <!-- 企业表格 -->
    <el-table :data="companyList" @selection-change="handleSelectionChange" stripe>
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="企业名称" />
      <el-table-column prop="industry" label="行业" />
      <el-table-column prop="cooperationLevel" label="合作等级">
        <template #default="{ row }">
          <el-rate :model-value="row.cooperationLevel" disabled show-score />
        </template>
      </el-table-column>
      <el-table-column prop="contactPerson" label="联系人" />
      <el-table-column prop="contactPhone" label="联系电话" />
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteCompany(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      @current-change="loadCompanies"
      layout="total, prev, pager, next, jumper"
      style="margin-top: 20px; text-align: center;"
    />

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" :append-to-body="true">
      <el-form :model="companyForm" :rules="rules" ref="companyFormRef" label-width="120px">
        <el-form-item label="企业名称" prop="name">
          <el-input v-model="companyForm.name" />
        </el-form-item>
        
        <el-form-item label="行业" prop="industry">
          <el-input v-model="companyForm.industry" />
        </el-form-item>
        
        <el-form-item label="合作等级" prop="cooperationLevel">
          <el-rate v-model="companyForm.cooperationLevel" show-score />
        </el-form-item>
        
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="companyForm.contactPerson" />
        </el-form-item>
        
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="companyForm.contactPhone" />
        </el-form-item>
        
        <el-form-item label="地址" prop="address">
          <el-input v-model="companyForm.address" type="textarea" :rows="3" />
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-switch v-model="companyForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCompany">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { companyAPI } from '@/api'

const companyList = ref([])
const selectedIds = ref([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('添加企业')
const isEdit = ref(false)
const companyFormRef = ref()

const companyForm = ref({
  id: null,
  name: '',
  industry: '',
  cooperationLevel: 3,
  contactPerson: '',
  contactPhone: '',
  address: '',
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  industry: [{ required: true, message: '请输入行业', trigger: 'blur' }],
  contactPerson: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }]
}

onMounted(() => {
  loadCompanies()
})

const loadCompanies = async () => {
  try {
    const res = await companyAPI.getCompanies({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value
    })
    if (res.code === 200) {
      let data = res.data || []
      if (Array.isArray(data)) {
        // 客户端过滤（后端未实现 keyword）
        if (searchKeyword.value) {
          const kw = searchKeyword.value.toLowerCase()
          data = data.filter(c =>
            (c.name || '').toLowerCase().includes(kw) ||
            (c.shortName || '').toLowerCase().includes(kw)
          )
        }
        total.value = data.length
        const start = (currentPage.value - 1) * pageSize.value
        companyList.value = data.slice(start, start + pageSize.value)
      } else if (data.records) {
        companyList.value = data.records
        total.value = data.total
      }
    }
  } catch (error) {
    ElMessage.error('加载企业列表失败')
  }
}

const showAddDialog = () => {
  isEdit.value = false
  dialogTitle.value = '添加企业'
  companyForm.value = { id: null, name: '', industry: '', cooperationLevel: 3, contactPerson: '', contactPhone: '', address: '', status: 1 }
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑企业'
  companyForm.value = { ...row }
  dialogVisible.value = true
}

const saveCompany = async () => {
  await companyFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          await companyAPI.updateCompany(companyForm.value.id, companyForm.value)
          ElMessage.success('更新成功')
        } else {
          await companyAPI.createCompany(companyForm.value)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        loadCompanies()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '操作失败')
      }
    }
  })
}

const deleteCompany = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该企业吗？', '提示', { type: 'warning' })
    await companyAPI.deleteCompany(row.id)
    ElMessage.success('删除成功')
    loadCompanies()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const batchDelete = async () => {
  if (selectedIds.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个企业吗？`, '提示', { type: 'warning' })
    for (const id of selectedIds.value) {
      await companyAPI.deleteCompany(id)
    }
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadCompanies()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}
</script>

<style scoped>
.company-manage {
  padding: 20px;
}

.operation-row {
  margin-bottom: 20px;
}
</style>
