<template>
  <div class="account-management fade-in">
    <div class="page-header">
      <h2>子账号管理</h2>
      <p>管理本企业下的 HR 操作账号</p>
    </div>

    <div class="stats-row">
      <div class="stat-box">
        <span class="stat-num">{{ accountList.length }}</span>
        <span class="stat-label">子账号总数</span>
      </div>
      <div class="stat-box">
        <span class="stat-num stat-num--ok">{{ activeCount }}</span>
        <span class="stat-label">启用中</span>
      </div>
      <div class="stat-box">
        <span class="stat-num stat-num--off">{{ disabledCount }}</span>
        <span class="stat-label">已禁用</span>
      </div>
      <div class="stat-box stat-box--tip">
        <span class="stat-tip">子账号可独立登录处理简历与面试；建议按招聘小组分配，一人一号</span>
      </div>
    </div>

    <div class="operation-row">
      <el-button type="primary" @click="showCreateDialog">+ 新建子账号</el-button>
    </div>

    <el-table :data="accountList" stripe v-loading="loading" style="width: 100%" table-layout="auto">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
      <el-table-column prop="realName" label="姓名" min-width="100" show-overflow-tooltip />
      <el-table-column prop="phone" label="手机号" min-width="130" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          {{ formatTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-button
            size="small"
            :type="row.status === 1 ? 'warning' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-popconfirm title="确认删除该子账号？" @confirm="deleteAccount(row)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="!loading && accountList.length === 0" class="empty-state">
      <el-empty :image-size="100" description="暂无子账号，请点击上方新建" />
    </div>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑子账号' : '新建子账号'"
      width="500px"
      :append-to-body="true"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="登录用的账号" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="手机号（可选）" />
        </el-form-item>
        <el-form-item label="密码" :prop="isEdit ? '' : 'password'">
          <el-input v-model="form.password" type="password" show-password
            :placeholder="isEdit ? '留空则不修改密码' : '请输入密码'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user.js'
import { companyAccountAPI } from '@/api/index.js'
import { formatDate } from '@/utils/formatDate'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const companyId = computed(() => userStore.companyId)


const accountList = ref([])
const activeCount = computed(() => accountList.value.filter(a => a.status === 1).length)
const disabledCount = computed(() => accountList.value.filter(a => a.status !== 1).length)
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const form = ref({
  id: null,
  username: '',
  realName: '',
  phone: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

onMounted(() => {
  loadAccounts()
})

const loadAccounts = async () => {
  if (!companyId.value) {
    ElMessage.warning('未关联企业信息，无法加载子账号')
    return
  }
  loading.value = true
  try {
    const res = await companyAccountAPI.listAccounts(companyId.value)
    if (res.code === 200) {
      accountList.value = res.data || []
    }
  } catch (e) {
    ElMessage.error('加载账号列表失败')
  } finally {
    loading.value = false
  }
}

const showCreateDialog = () => {
  isEdit.value = false
  form.value = { id: null, username: '', realName: '', phone: '', password: '' }
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  form.value = { id: row.id, username: row.username, realName: row.realName, phone: row.phone, password: '' }
  dialogVisible.value = true
}

const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    let res
    if (isEdit.value) {
      res = await companyAccountAPI.updateAccount(companyId.value, form.value.id, {
        realName: form.value.realName,
        phone: form.value.phone,
        password: form.value.password || undefined
      })
    } else {
      res = await companyAccountAPI.createAccount(companyId.value, {
        username: form.value.username,
        realName: form.value.realName,
        phone: form.value.phone,
        password: form.value.password
      })
    }
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadAccounts()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

const toggleStatus = async (row) => {
  try {
    const res = await companyAccountAPI.updateAccount(companyId.value, row.id, {
      status: row.status === 1 ? 0 : 1
    })
    if (res.code === 200) {
      ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
      loadAccounts()
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const deleteAccount = async (row) => {
  try {
    const res = await companyAccountAPI.deleteAccount(companyId.value, row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadAccounts()
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const formatTime = (t) => formatDate(t, { showSeconds: true })
</script>

<style scoped>
.account-management { padding: 20px; width: 100%; max-width: 100%; box-sizing: border-box; }
.account-management :deep(.el-table) { width: 100% !important; }
.operation-row { margin-bottom: 20px; display: flex; align-items: center; }
.empty-state { padding: 40px 0; display: flex; justify-content: center; }


</style>
<style scoped>
.stats-row { display: flex; gap: 16px; margin-bottom: 20px; }
.stat-box { background: white; border-radius: 12px; padding: 18px 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); display: flex; flex-direction: column; gap: 4px; min-width: 120px; }
.stat-num { font-size: 26px; font-weight: 700; color: #1D2129; }
.stat-num--ok { color: #10B981; }
.stat-num--off { color: #F53F3F; }
.stat-label { font-size: 13px; color: #86909C; }
.stat-box--tip { flex: 1; justify-content: center; background: #F7F8FA; box-shadow: none; }
.stat-tip { font-size: 13px; color: #86909C; line-height: 1.6; }
</style>
