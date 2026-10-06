<template>
  <div class="class-management fade-in">
    <div class="page-header">
      <h2>班级管理</h2>
      <p>管理班级 · 管理学生关联</p>
    </div>
    <div class="card">
      <div class="card-header">
        <div class="card-header-left">
          <el-input v-model="searchKeyword" placeholder="搜索班级名称..." clearable style="width: 240px;" size="default" />
        </div>
        <button class="btn" @click="showCreateDialog"><i class="fa fa-plus"></i> 添加班级</button>
      </div>

      <table class="data-table" v-if="classList.length > 0">
        <caption v-if="searchKeyword" style="caption-side:top;text-align:left;padding:4px 0;color:#86909C;font-size:13px;">搜索 "{{ searchKeyword }}" 结果</caption>
        <thead>
          <tr>
            <th>班级名称</th>
            <th>专业</th>
            <th>年级</th>
            <th>学生数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="cls in classList" :key="cls.id"
            v-show="!searchKeyword || cls.name.toLowerCase().includes(searchKeyword.toLowerCase())">
            <td>{{ cls.name }}</td>
            <td>{{ cls.major || '-' }}</td>
            <td>{{ cls.grade || '-' }}</td>
            <td><el-tag size="small" :type="(cls.studentCount || 0) > 0 ? 'primary' : 'info'">{{ cls.studentCount || 0 }}人</el-tag></td>
            <td>
              <button class="btn btn-sm btn-outline" style="margin-right:6px;" @click="viewStudents(cls)">管理学生</button>
              <button class="btn btn-sm btn-outline" style="color:#EF4444;border-color:#EF4444;" @click="deleteClass(cls.id, cls.name)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else class="empty-state">
        <i class="fa fa-users" style="font-size:48px;color:#C9CDD4;"></i>
        <p style="margin-top:16px;color:#86909C;">暂无班级，点击上方按钮添加班级</p>
      </div>
    </div>

    <!-- 添加班级对话框 -->
    <el-dialog v-model="createDialogVisible" title="添加班级" width="480px" :close-on-click-modal="false"
      append-to-body modal-class="class-create-overlay">
      <el-form :model="createForm" label-width="80px" size="default">
        <el-form-item label="班级名称" required>
          <el-input v-model="createForm.name" placeholder="如: 软件工程2301班" />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="createForm.major" placeholder="如: 软件工程" />
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="createForm.grade" placeholder="请选择" style="width:100%;">
            <el-option label="2022级" value="2022级" />
            <el-option label="2023级" value="2023级" />
            <el-option label="2024级" value="2024级" />
            <el-option label="2025级" value="2025级" />
            <el-option label="2026级" value="2026级" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveClass" :loading="creating">保存</el-button>
      </template>
    </el-dialog>

    <!-- 学生列表对话框 -->
    <el-dialog v-model="studentDialogVisible" :title="selectedClassName + ' - 学生列表'" width="800px"
      append-to-body modal-class="class-student-overlay">
      <div class="student-dialog-header">
        <span class="student-count">共 <strong>{{ studentList.length }}</strong> 名学生</span>
        <div class="student-dialog-actions">
          <el-input v-model="studentSearchKey" placeholder="搜索学生姓名/学号..." clearable style="width:200px;margin-right:8px;" size="small" />
          <el-button type="primary" size="small" @click="showAddStudentDialog" :disabled="!currentClassId">
            <i class="fa fa-plus"></i> 添加学生
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
      <div v-if="studentList.length === 0" class="empty-state" style="padding:30px;">
        <p style="color:#86909C;">暂无学生，点击上方"添加学生"按钮将学生加入班级</p>
      </div>
    </el-dialog>

    <!-- 添加学生对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加学生到班级" width="650px"
      append-to-body modal-class="class-student-overlay">
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
import { classAPI, userAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import ResumeDetailDialog from '@/components/ResumeDetailDialog.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const classList = ref([])
const currentClassId = ref(null)
const searchKeyword = ref('')

// ----- 创建班级 -----
const createDialogVisible = ref(false)
const creating = ref(false)
const createForm = ref({ name: '', major: '', grade: '' })

// ----- 学生列表 -----
const studentDialogVisible = ref(false)
const selectedClassName = ref('')
const studentList = ref([])
const studentSearchKey = ref('')

// ----- 添加学生 -----
const addDialogVisible = ref(false)
const addSearchKey = ref('')
const allStudents = ref([])
const selectedAddStudents = ref([])
const addingStudents = ref(false)

// ----- 简历 -----
const detailVisible = ref(false)
const detailStudentId = ref(null)

// 过滤后的学生列表（搜索）
const filteredStudentList = computed(() => {
  const key = studentSearchKey.value.trim().toLowerCase()
  if (!key) return studentList.value
  return studentList.value.filter(s =>
    (s.realName || '').toLowerCase().includes(key) ||
    (s.username || '').toLowerCase().includes(key)
  )
})

// 待选学生（不在本班 + 搜索过滤）
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

onMounted(async () => { await loadClasses() })

const loadClasses = async () => {
  if (!userStore.userId) return
  try {
    const res = await classAPI.getClassesByTeacher(userStore.userId)
    if (res.code === 200) classList.value = res.data || []
  } catch (e) { console.error('加载班级列表失败', e) }
}

const showCreateDialog = () => {
  createForm.value = { name: '', major: '', grade: '' }
  createDialogVisible.value = true
}

const saveClass = async () => {
  if (!createForm.value.name.trim()) { ElMessage.warning('请输入班级名称'); return }
  creating.value = true
  try {
    const payload = { ...createForm.value, teacherId: userStore.userId }
    const res = await classAPI.createClass(payload)
    if (res.code === 200) {
      ElMessage.success('班级创建成功')
      createDialogVisible.value = false
      await loadClasses()
    } else { ElMessage.error(res.message || '创建失败') }
  } catch (e) { ElMessage.error('创建失败: ' + (e.message || '网络错误')) }
  finally { creating.value = false }
}

const deleteClass = async (id, name) => {
  try {
    await ElMessageBox.confirm('确定要删除"' + name + '"吗？', '确认删除', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning', zIndex: 3000 })
    const res = await classAPI.deleteClass(id)
    if (res.code === 200) { ElMessage.success('删除成功'); await loadClasses() }
    else { ElMessage.error(res.message || '删除失败') }
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败: ' + (e.message || '网络错误')) }
}

// ----- 学生管理 -----
const viewStudents = async (cls) => {
  currentClassId.value = cls.id
  selectedClassName.value = cls.name
  studentList.value = []
  studentDialogVisible.value = true
  studentSearchKey.value = ''
  try {
    const res = await classAPI.getClassStudents(cls.id)
    if (res.code === 200) studentList.value = (res.data || []).sort((a, b) => (a.username || '').localeCompare(b.username || ''))
  } catch (e) { ElMessage.error('加载学生列表失败: ' + (e.message || '网络错误')) }
}

const showAddStudentDialog = async () => {
  addSearchKey.value = ''
  selectedAddStudents.value = []
  addDialogVisible.value = true
  // 加载全校学生列表
  try {
    const res = await classAPI.getStudentDirectory()
    allStudents.value = (res.data || []).sort((a, b) => (a.username || '').localeCompare(b.username || ''))
  } catch (e) {
    ElMessage.error('加载学生列表失败')
    allStudents.value = []
  }
}

const onAddSelectionChange = (rows) => {
  // 只选择未加入的学生
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
      // 刷新学生列表
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
.class-management { padding: 20px; }
.card { background: #fff; border-radius: 16px; padding: 32px; margin-bottom: 28px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); position: relative; }
.card::before { content: ""; position: absolute; top: 0; left: 0; width: 100%; height: 3px; background: linear-gradient(90deg,#165DFF,#2563EB,transparent); border-radius: 16px 16px 0 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.card-header-left { display: flex; align-items: center; gap: 8px; }
.data-table { width: 100%; border-collapse: collapse; margin-top: 16px; }
.data-table th { text-align: left; padding: 14px 12px; background: #F8F9FC; color: #4E5969; font-weight: 600; font-size: 14px; border-bottom: 2px solid #E2E8F0; }
.data-table td { padding: 14px 12px; border-bottom: 1px solid #F2F3F5; font-size: 14px; color: #1D2129; }
.data-table tr:hover td { background: rgba(22,93,255,0.03); }
.btn { padding: 10px 20px; background: #165DFF; color: #fff; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; transition: all 0.25s ease; box-shadow: 0 3px 8px rgba(22,93,255,0.2); font-weight: 500; display: inline-flex; align-items: center; gap: 6px; }
.btn:hover { background: #0E42C1; box-shadow: 0 5px 15px rgba(22,93,255,0.3); transform: translateY(-2px); }
.btn-sm { padding: 6px 12px; font-size: 13px; }
.btn-outline { background: #fff; border: 1px solid #DCDFE6; color: #4E5969; box-shadow: none; }
.btn-outline:hover { background: #F5F7FA; border-color: #165DFF; color: #165DFF; transform: translateY(-2px); }
.empty-state { text-align: center; padding: 60px 20px; }

/* 学生对话框 */
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

<style>
.class-create-overlay, .class-student-overlay {
  position: fixed !important;
  top: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  left: 0 !important;
  background: rgba(0, 0, 0, 0.45) !important;
}
</style>
