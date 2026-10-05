<template>
  <div class="user-manage fade-in">
    <div class="page-header">
      <h2>用户管理</h2>
      <p>管理系统用户 · 分配角色权限</p>
    </div>
    
    <!-- 操作栏 -->
    <el-row class="operation-row">
      <el-col :span="12">
        <el-button type="primary" @click="showAddDialog">添加用户</el-button>
        <el-button type="danger" @click="batchDelete" :disabled="selectedIds.length === 0">批量删除</el-button>
      </el-col>
      <el-col :span="12" style="text-align: right;">
        <el-input v-model="searchKeyword" placeholder="搜索用户名/姓名" style="width: 300px;" clearable>
          <template #append>
            <el-button @click="loadUsers"><el-icon><Search /></el-icon></el-button>
          </template>
        </el-input>
      </el-col>
    </el-row>

    <!-- 用户表格 -->
    <el-table :data="userList" @selection-change="handleSelectionChange" stripe>
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="role" label="角色">
        <template #default="{ row }">
          <el-tag :type="roleTagType(row.role)">
            {{ roleText(row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="手机号" min-width="120">
        <template #default="{ row }">
          {{ row.phone && row.phone.includes('加密') ? '—' : row.phone }}
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="toggleStatus(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteUser(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      @current-change="loadUsers"
      layout="total, prev, pager, next, jumper"
      style="margin-top: 20px; text-align: center;"
    />

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" :append-to-body="true">
      <el-form :model="userForm" :rules="rules" ref="userFormRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="isEdit" />
        </el-form-item>
        
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="userForm.password" type="password" show-password />
        </el-form-item>
        
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="userForm.realName" />
        </el-form-item>
        
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色">
            <el-option label="学生" :value="0" />
            <el-option label="教师" :value="1" />
            <el-option label="HR" :value="2" />
            <el-option label="管理员" :value="3" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userAPI } from '@/api'

const userList = ref([])
const selectedIds = ref([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('添加用户')
const isEdit = ref(false)
const userFormRef = ref()

const userForm = ref({
  id: null,
  username: '',
  password: '',
  realName: '',
  role: 0,
  phone: '',
  email: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

onMounted(() => {
  loadUsers()
})

const loadUsers = async () => {
  try {
    const res = await userAPI.getUsers({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value
    })
    if (res.code === 200) {
      let data = res.data || []
      if (Array.isArray(data)) {
        // 客户端搜索过滤（后端未实现 keyword 参数）
        if (searchKeyword.value) {
          const kw = searchKeyword.value.toLowerCase()
          data = data.filter(u =>
            (u.username || '').toLowerCase().includes(kw) ||
            (u.realName || '').toLowerCase().includes(kw) ||
            (u.phone || '').toLowerCase().includes(kw)
          )
        }
        total.value = data.length
        const start = (currentPage.value - 1) * pageSize.value
        userList.value = data.slice(start, start + pageSize.value)
      } else if (data.records) {
        userList.value = data.records
        total.value = data.total
      }
    }
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  }
}

const showAddDialog = () => {
  isEdit.value = false
  dialogTitle.value = '添加用户'
  userForm.value = { id: null, username: '', password: '', realName: '', role: 0, phone: '', email: '' }
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  userForm.value = { ...row, password: '' }
  dialogVisible.value = true
}

const saveUser = async () => {
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          // 编辑时不要把空密码发过去，避免覆盖数据库中的加密密码
          const payload = { ...userForm.value }
          if (!payload.password) delete payload.password
          await userAPI.updateUser(userForm.value.id, payload)
          ElMessage.success('更新成功')
        } else {
          await userAPI.createUser(userForm.value)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        loadUsers()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '操作失败')
      }
    }
  })
}

const deleteUser = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该用户吗？', '提示', { type: 'warning' })
    const res = await userAPI.deleteUser(row.id)
    ElMessage.success(res.message || '删除成功')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

const batchDelete = async () => {
  if (selectedIds.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(`确定删除选中的${selectedIds.value.length} 个用户吗？`, '提示', { type: 'warning' })
    for (const id of selectedIds.value) {
      await userAPI.deleteUser(id)
    }
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '批量删除失败')
    }
  }
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

const toggleStatus = async (row) => {
  try {
    await userAPI.updateUserStatus(row.id, { status: row.status })
  } catch (error) {
    ElMessage.error('状态更新失败')
    row.status = row.status === 1 ? 0 : 1
  }
}

const roleTagType = (role) => {
  const types = ['info', 'success', 'warning', 'danger']
  return types[role] || 'info'
}

const roleText = (role) => {
  const texts = ['学生', '教师', 'HR', '管理员']
  return texts[role] || '未知'
}
</script>

<style scoped>
.user-manage {
  padding: 20px;
}

.operation-row {
  margin-bottom: 20px;
}
</style>