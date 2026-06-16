<template>
  <div class="class-management">
    <div class="card">
      <div class="card-header">
        <h2>我带的班级</h2>
        <button class="btn" @click="showCreateDialog"><i class="fa fa-plus"></i> 添加班级</button>
      </div>

      <table class="data-table" v-if="classList.length > 0">
        <thead>
          <tr>
            <th>班级名称</th>
            <th>专业</th>
            <th>年级</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="cls in classList" :key="cls.id">
            <td>{{ cls.name }}</td>
            <td>{{ cls.major || '-' }}</td>
            <td>{{ cls.grade || '-' }}</td>
            <td>
              <button class="btn btn-sm btn-outline" style="margin-right:6px;" @click="viewStudents(cls)">查看学生</button>
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
    <el-dialog v-model="createDialogVisible" title="添加班级" width="480px" :close-on-click-modal="false">
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
    <el-dialog v-model="studentDialogVisible" :title="selectedClassName + ' - 学生列表'" width="700px">
      <el-table :data="studentList" border stripe style="width:100%;" v-if="studentList.length > 0">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="学号" width="120" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="简历">
          <template #default="scope">
            <el-button link type="primary" @click="viewResume(scope.row)" v-if="scope.row.id">查看简历</el-button>
            <span v-else style="color:#C9CDD4;">无简历</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-else class="empty-state" style="padding:30px;">
        <p style="color:#86909C;">暂无学生数据</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { classAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const classList = ref([])
const createDialogVisible = ref(false)
const creating = ref(false)
const createForm = ref({ name: '', major: '', grade: '' })
const studentDialogVisible = ref(false)
const selectedClassName = ref('')
const studentList = ref([])

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
    await ElMessageBox.confirm('确定要删除"' + name + '"吗？', '确认删除', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    const res = await classAPI.deleteClass(id)
    if (res.code === 200) { ElMessage.success('删除成功'); await loadClasses() }
    else { ElMessage.error(res.message || '删除失败') }
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败: ' + (e.message || '网络错误')) }
}

const viewStudents = async (cls) => {
  selectedClassName.value = cls.name
  studentList.value = []
  studentDialogVisible.value = true
  try {
    const res = await classAPI.getClassStudents(cls.id)
    if (res.code === 200) studentList.value = res.data || []
  } catch (e) { ElMessage.error('加载学生列表失败: ' + (e.message || '网络错误')) }
}

const viewResume = (student) => {
  ElMessage.info('查看' + (student.realName || student.username) + '的简历功能待对接')
}
</script>

<style scoped>
.class-management { padding: 20px; }
.card { background: #fff; border-radius: 16px; padding: 32px; margin-bottom: 28px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); position: relative; }
.card::before { content: ""; position: absolute; top: 0; left: 0; width: 100%; height: 3px; background: linear-gradient(90deg,#165DFF,#2563EB,transparent); border-radius: 16px 16px 0 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.card h2 { font-size: 19px; font-weight: 600; padding-bottom: 12px; border-bottom: 1px solid #F2F3F5; position: relative; }
.card h2::after { content: ""; width: 50px; height: 3px; background: #165DFF; border-radius: 3px; position: absolute; left: 0; bottom: -1px; }
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
</style>
