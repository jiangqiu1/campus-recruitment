<template>
  <div class="class-manage fade-in">
    <div class="page-header">
      <h2>班级管理</h2>
      <p>管理班级信息 · 管理学生关联</p>
    </div>

    <div class="stats-row">
      <div class="stat-box">
        <span class="stat-num">{{ classList.length }}</span>
        <span class="stat-label">班级总数</span>
      </div>
      <div class="stat-box">
        <span class="stat-num stat-num--blue">{{ totalStudents }}</span>
        <span class="stat-label">学生总数</span>
      </div>
      <div class="stat-box">
        <span class="stat-num stat-num--green">{{ avgSize }}</span>
        <span class="stat-label">平均班额</span>
      </div>
      <div class="stat-box stat-box--tip">
        <span class="stat-tip">点击「管理学生」可查看班级名单、添加或移除学生</span>
      </div>
    </div>
    
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
      <el-table-column prop="studentCount" label="学生数" width="80" />
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="viewStudents(row)">管理学生</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" :append-to-body="true">
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

    <!-- 学生列表对话框 -->
    <el-dialog v-model="studentDialogVisible" :title="selectedClassName + ' - 学生列表'" width="800px" append-to-body>
      <div class="student-dialog-header">
        <span class="student-count">共 <strong>{{ studentList.length }}</strong> 名学生</span>
        <div class="student-dialog-actions">
          <el-input v-model="studentSearchKey" placeholder="搜索学生姓名/学号..." clearable style="width:200px;margin-right:8px;" size="small" />
          <el-button type="primary" size="small" @click="showAddStudentDialog" :disabled="!currentClassId">
            <el-icon><Plus /></el-icon> 添加学生
          </el-button>
        </div>
      </div>
      <el-table :data="filteredStudentList" border stripe style="width:100%;" max-height="400">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="username" label="学号" min-width="100" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column label="操作" width="140">
          <template #default="scope">
            <el-button link type="primary" size="small" style="margin-right:6px;" @click="viewResume(scope.row)" v-if="scope.row.id">查看简历</el-button>
            <span v-else style="color:#C9CDD4;margin-right:6px;">无</span>
            <el-button type="danger" link size="small" @click="removeStudent(scope.row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="studentList.length === 0" style="text-align:center;padding:30px;color:#C9CDD4;">
        <p>暂无学生，点击上方"添加学生"按钮将学生加入班级</p>
      </div>
    </el-dialog>

    <!-- 添加学生对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加学生到班级" width="650px" append-to-body>
      <div style="margin-bottom:12px;display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
        <el-input v-model="addSearchKey" placeholder="搜索学生姓名或学号..." clearable style="width:220px;" size="small" />
        <el-tag type="info" size="small">已选 {{ selectedAddStudents.length }} 人</el-tag>
      </div>
      <el-table :data="candidateStudents" border stripe style="width:100%;" max-height="350"
        @selection-change="onAddSelectionChange">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="username" label="学号" width="120" />
        <el-table-column prop="realName" label="姓名" width="130" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag v-if="scope.row._inClass" type="warning" size="small">已加入</el-tag>
            <el-tag v-else type="success" size="small">未加入</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="candidateStudents.length === 0" style="text-align:center;padding:20px;color:#C9CDD4;">
        <p>无匹配学生</p>
      </div>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddStudents" :loading="addingStudents"
          :disabled="selectedAddStudents.length === 0">
          添加 {{ selectedAddStudents.length > 0 ? '(' + selectedAddStudents.length + '人)' : '' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 简历详情 -->
    <ResumeDetailDialog
      v-model:visible="detailVisible"
      :student-id="detailStudentId"
      mode="teacher"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { classAPI, userAPI } from '@/api'
import ResumeDetailDialog from '@/components/ResumeDetailDialog.vue'

const classList = ref([])
// 学生数优先取后端返回的 studentCount，取不到按列表渲染（分页时仅当前页）
const totalStudents = computed(() => classList.value.reduce((sum, c) => sum + (Number(c.studentCount) || 0), 0))
const avgSize = computed(() => classList.value.length ? Math.round(totalStudents.value / classList.value.length) : 0)
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

// ----- 学生管理 -----
const currentClassId = ref(null)
const selectedClassName = ref('')
const studentDialogVisible = ref(false)
const studentList = ref([])
const studentSearchKey = ref('')
const addDialogVisible = ref(false)
const addSearchKey = ref('')
const allStudents = ref([])
const selectedAddStudents = ref([])
const addingStudents = ref(false)
const detailVisible = ref(false)
const detailStudentId = ref(null)

const filteredStudentList = computed(() => {
  const key = studentSearchKey.value.trim().toLowerCase()
  if (!key) return studentList.value
  return studentList.value.filter(s =>
    (s.realName || '').toLowerCase().includes(key) ||
    (s.username || '').toLowerCase().includes(key)
  )
})

const candidateStudents = computed(() => {
  const inClassIds = new Set(studentList.value.map(s => s.id))
  const key = addSearchKey.value.trim().toLowerCase()
  let list = allStudents.value.map(s => ({
    ...s,
    _inClass: inClassIds.has(s.id)
  }))
  if (key) {
    list = list.filter(s =>
      (s.realName || '').toLowerCase().includes(key) ||
      (s.username || '').toLowerCase().includes(key)
    )
  }
  return list
})

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
        if (searchKeyword.value) {
          const kw = searchKeyword.value.toLowerCase()
          data = data.filter(c => (c.name || '').toLowerCase().includes(kw))
        }
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
    await ElMessageBox.confirm('确定删除该班级吗？', '提示', { type: 'warning', zIndex: 3000 })
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
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个班级吗？`, '提示', { type: 'warning', zIndex: 3000 })
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

// ----- 学生关联管理 -----
const viewStudents = async (cls) => {
  currentClassId.value = cls.id
  selectedClassName.value = cls.name
  studentList.value = []
  studentDialogVisible.value = true
  studentSearchKey.value = ''
  try {
    const res = await classAPI.getClassStudents(cls.id)
    if (res.code === 200) studentList.value = (res.data || []).sort((a, b) => (a.username || '').localeCompare(b.username || ''))
  } catch (e) { ElMessage.error('加载学生列表失败') }
}

const showAddStudentDialog = async () => {
  addSearchKey.value = ''
  selectedAddStudents.value = []
  addDialogVisible.value = true
  try {
    const res = await userAPI.getUsersByRole(0)
    allStudents.value = (res.data || []).sort((a, b) => (a.username || '').localeCompare(b.username || ''))
  } catch (e) {
    ElMessage.error('加载学生列表失败')
    allStudents.value = []
  }
}

const onAddSelectionChange = (rows) => {
  selectedAddStudents.value = rows.filter(r => !r._inClass)
}

const confirmAddStudents = async () => {
  const toAdd = selectedAddStudents.value
  if (toAdd.length === 0) { ElMessage.warning('请选择未加入班级的学生'); return }
  addingStudents.value = true
  try {
    const ids = toAdd.map(s => s.id)
    const res = await classAPI.batchAddStudentsToClass(currentClassId.value, ids)
    if (res.code === 200) {
      ElMessage.success('成功添加 ' + (res.data?.successCount || ids.length) + ' 名学生')
      addDialogVisible.value = false
      await viewStudents({ id: currentClassId.value, name: selectedClassName.value })
      await loadClasses()
    } else {
      ElMessage.error(res.message || '添加失败')
    }
  } catch (e) {
    ElMessage.error('添加失败: ' + (e.message || '网络错误'))
  } finally {
    addingStudents.value = false
  }
}

const removeStudent = async (student) => {
  try {
    await ElMessageBox.confirm(
      '确定将 "' + (student.realName || student.username) + '" 从班级移出吗？',
      '确认移除',
      { confirmButtonText: '移除', cancelButtonText: '取消', type: 'warning', zIndex: 9999 }
    )
    const res = await classAPI.removeStudentFromClass(currentClassId.value, student.id)
    if (res.code === 200) {
      ElMessage.success('已移除')
      studentList.value = studentList.value.filter(s => s.id !== student.id)
      await loadClasses()
    } else {
      ElMessage.error(res.message || '移除失败')
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('移除失败')
  }
}

const viewResume = (student) => {
  detailStudentId.value = student.id
  detailVisible.value = true
}
</script>

<style scoped>
.class-manage {
  padding: 20px;
}
.operation-row {
  margin-bottom: 20px;
}
.student-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}
.student-count { font-size: 14px; color: #86909C; }
.student-dialog-actions { display: flex; align-items: center; }
</style>
<style scoped>
.stats-row { display: flex; gap: 16px; margin-bottom: 20px; }
.stat-box { background: white; border-radius: 12px; padding: 18px 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); display: flex; flex-direction: column; gap: 4px; min-width: 120px; }
.stat-num { font-size: 26px; font-weight: 700; color: #1D2129; }
.stat-num--blue { color: #165DFF; }
.stat-num--green { color: #10B981; }
.stat-label { font-size: 13px; color: #86909C; }
.stat-box--tip { flex: 1; justify-content: center; background: #F7F8FA; box-shadow: none; }
.stat-tip { font-size: 13px; color: #86909C; line-height: 1.6; }
</style>
