<template>
  <div class="class-manage">
    <h2>班级管理</h2>
    
    <!-- 操作栏 -->
    <el-row class="operation-row">
      <el-col :span="12">
        <el-button type="primary" @click="showAddDialog">添加班级</el-button>
        <el-button type="danger" @click="batchDelete" :disabled="selectedIds.length === 0">批量删除</el-button>
      </el-col>
      <el-col :span="12" style="text-align: right;">
        <el-input v-model="searchKeyword" placeholder="搜索班级名称" style="width: 300px;" clearable>
          <template #append>
            <el-button @click="loadClasses"><el-icon><Search /></el-icon></el-button>
          </template>
        </el-input>
      </el-col>
    </el-row>

    <!-- 班级表格 -->
    <el-table :data="classList" @selection-change="handleSelectionChange" stripe>
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="班级名称" />
      <el-table-column prop="major" label="专业" />
      <el-table-column prop="grade" label="年级" width="100" />
      <el-table-column prop="teacherId" label="班主任ID" width="120" />
      <el-table-column prop="studentCount" label="学生数" width="100" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteClass(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      @current-change="loadClasses"
      layout="total, prev, pager, next, jumper"
      style="margin-top: 20px; text-align: center;"
    />

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="classForm" :rules="rules" ref="classFormRef" label-width="100px">
        <el-form-item label="班级名称" prop="name">
          <el-input v-model="classForm.name" />
        </el-form-item>
        <el-form-item label="专业" prop="major">
          <el-input v-model="classForm.major" />
        </el-form-item>
        <el-form-item label="年级" prop="grade">
          <el-select v-model="classForm.grade" placeholder="请选择年级">
            <el-option label="2021级" value="2021" />
            <el-option label="2022级" value="2022" />
            <el-option label="2023级" value="2023" />
            <el-option label="2024级" value="2024" />
          </el-select>
        </el-form-item>
        <el-form-item label="班主任ID" prop="teacherId">
          <el-input v-model.number="classForm.teacherId" type="number" />
        </el-form-item>
        <el-form-item label="学生数">
          <el-input v-model.number="classForm.studentCount" type="number" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveClass">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { classAPI } from '@/api'

const classList = ref([])
const selectedIds = ref([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('添加班级')
const isEdit = ref(false)
const classFormRef = ref()

const classForm = ref({
  id: null,
  name: '',
  major: '',
  grade: '',
  teacherId: null,
  studentCount: 0
})

const rules = {
  name: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }],
  grade: [{ required: true, message: '请选择年级', trigger: 'change' }],
  teacherId: [{ required: true, message: '请输入班主任ID', trigger: 'blur' }]
}

onMounted(() => {
  loadClasses()
})

const loadClasses = async () => {
  try {
    const res = await classAPI.getClasses({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value
    })
    if (res.code === 200) {
      let data = res.data || []
      if (Array.isArray(data)) {
        total.value = data.length
        const start = (currentPage.value - 1) * pageSize.value
        classList.value = data.slice(start, start + pageSize.value)
      } else if (data.records) {
        classList.value = data.records
        total.value = data.total
      }
    }
  } catch (error) {
    ElMessage.error('加载班级列表失败')
  }
}

const showAddDialog = () => {
  isEdit.value = false
  dialogTitle.value = '添加班级'
  classForm.value = { id: null, name: '', major: '', grade: '', teacherId: null, studentCount: 0 }
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑班级'
  classForm.value = { ...row }
  dialogVisible.value = true
}

const saveClass = async () => {
  await classFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          await classAPI.updateClass(classForm.value.id, classForm.value)
          ElMessage.success('更新成功')
        } else {
          await classAPI.createClass(classForm.value)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        loadClasses()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '操作失败')
      }
    }
  })
}

const deleteClass = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该班级吗？', '提示', { type: 'warning' })
    await classAPI.deleteClass(row.id)
    ElMessage.success('删除成功')
    loadClasses()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const batchDelete = async () => {
  if (selectedIds.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个班级吗？`, '提示', { type: 'warning' })
    await classAPI.batchDeleteClasses(selectedIds.value)
    ElMessage.success('批量删除成功')
    loadClasses()
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
.class-manage {
  padding: 20px;
}

.operation-row {
  margin-bottom: 20px;
}
</style>
