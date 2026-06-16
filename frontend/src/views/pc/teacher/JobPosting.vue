<template>
  <div class="job-posting">
    <div class="card">
      <div class="card-header">
        <h2>AI岗位发布</h2>
        <button class="btn" @click="showAIDialog"><i class="fa fa-magic"></i> AI解析发布</button>
      </div>

      <table class="data-table" v-if="jobList.length > 0">
        <thead>
          <tr>
            <th>岗位名称</th>
            <th>薪资</th>
            <th>地点</th>
            <th>学历要求</th>
            <th>截止日期</th>
            <th>浏览次数</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="job in jobList" :key="job.id">
            <td>{{ job.title }}</td>
            <td>{{ job.salaryRange || '-' }}</td>
            <td>{{ job.location || '-' }}</td>
            <td>{{ job.education || '-' }}</td>
            <td>{{ job.deadline || '-' }}</td>
            <td>{{ job.viewCount || 0 }}</td>
            <td><span :class="'status-badge status-' + (job.status != null ? job.status : 0)">{{ statusLabel(job.status) }}</span></td>
            <td class="action-cell">
              <button class="btn btn-sm btn-outline" style="margin-right:6px;" @click="editJob(job)">编辑</button>
              <button v-if="job.status === 0 || job.status === 3" class="btn btn-sm btn-outline" style="margin-right:6px;color:#10B981;border-color:#10B981;" @click="publishJob(job.id)">发布</button>
              <button v-if="job.status === 1" class="btn btn-sm btn-outline" style="margin-right:6px;color:#F59E0B;border-color:#F59E0B;" @click="pauseJob(job.id)">暂停</button>
              <button v-if="job.status === 1 || job.status === 3" class="btn btn-sm btn-outline" style="margin-right:6px;color:#EF4444;border-color:#EF4444;" @click="closeJob(job.id)">关闭</button>
              <button class="btn btn-sm btn-outline" style="color:#EF4444;border-color:#EF4444;" @click="deleteJob(job.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else class="empty-state">
        <i class="fa fa-briefcase" style="font-size:48px;color:#C9CDD4;"></i>
        <p style="margin-top:16px;color:#86909C;">暂无岗位，点击上方按钮发布第一个岗位</p>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑岗位' : 'AI解析发布'" width="680px" :close-on-click-modal="false">
      <el-form :model="form" label-width="100px" size="default">
        <el-form-item label="岗位名称" required>
          <el-input v-model="form.title" placeholder="请输入岗位名称" />
        </el-form-item>
        <el-form-item label="薪资范围">
          <el-input v-model="form.salaryRange" placeholder="如: 8k-12k" />
        </el-form-item>
        <el-form-item label="学历要求">
          <el-select v-model="form.education" placeholder="请选择" style="width:100%;">
            <el-option label="不限" value="" />
            <el-option label="大专" value="大专" />
            <el-option label="本科" value="本科" />
            <el-option label="硕士" value="硕士" />
            <el-option label="博士" value="博士" />
          </el-select>
        </el-form-item>
        <el-form-item label="工作地点">
          <el-input v-model="form.location" placeholder="如: 北京市海淀区" />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="form.deadline" type="date" placeholder="选择日期" style="width:100%;" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="岗位描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="岗位职责描述" />
        </el-form-item>
        <el-form-item label="任职要求">
          <el-input v-model="form.requirement" type="textarea" :rows="3" placeholder="任职要求" />
        </el-form-item>
        <el-form-item label="技能标签">
          <el-input v-model="form.requiredSkills" placeholder="用逗号分隔，如: Java,Spring,Vue" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveJob" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { jobAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const jobList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const form = ref({
  id: null, title: '', salaryRange: '', education: '', location: '',
  deadline: '', description: '', requirement: '', requiredSkills: ''
})

onMounted(async () => { await loadJobs() })

const loadJobs = async () => {
  if (!userStore.userId) return
  try {
    const res = await jobAPI.getJobsByCreator(userStore.userId)
    if (res.code === 200) jobList.value = res.data || []
  } catch (e) { console.error('加载岗位列表失败', e) }
}

const statusLabel = (s) => ({ 0: '草稿', 1: '已发布', 2: '已关闭', 3: '已暂停' })[s] || '未知'

const resetForm = () => {
  form.value = { id: null, title: '', salaryRange: '', education: '', location: '', deadline: '', description: '', requirement: '', requiredSkills: '' }
  isEdit.value = false
}

const showAIDialog = () => { resetForm(); dialogVisible.value = true }

const editJob = (job) => {
  form.value = { id: job.id, title: job.title || '', salaryRange: job.salaryRange || '', education: job.education || '', location: job.location || '', deadline: job.deadline || '', description: job.description || '', requirement: job.requirement || '', requiredSkills: job.requiredSkills || '' }
  isEdit.value = true; dialogVisible.value = true
}

const saveJob = async () => {
  if (!form.value.title.trim()) { ElMessage.warning('请输入岗位名称'); return }
  saving.value = true
  try {
    const payload = { ...form.value }; delete payload.id
    let res
    if (isEdit.value) {
      res = await jobAPI.updateJob(form.value.id, payload)
    } else {
      payload.createdBy = userStore.userId; payload.status = 0
      res = await jobAPI.createJob(payload)
    }
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '岗位更新成功' : '岗位创建成功')
      dialogVisible.value = false; await loadJobs()
    } else { ElMessage.error(res.message || '保存失败') }
  } catch (e) { ElMessage.error('保存失败: ' + (e.message || '网络错误')) }
  finally { saving.value = false }
}

const publishJob = async (id) => {
  try {
    const res = await jobAPI.publishJob(id)
    if (res.code === 200) { ElMessage.success('岗位已发布'); await loadJobs() }
    else { ElMessage.error(res.message || '发布失败') }
  } catch (e) { ElMessage.error('发布失败: ' + (e.message || '网络错误')) }
}

const pauseJob = async (id) => {
  try {
    const res = await jobAPI.pauseJob(id)
    if (res.code === 200) { ElMessage.success('岗位已暂停'); await loadJobs() }
    else { ElMessage.error(res.message || '暂停失败') }
  } catch (e) { ElMessage.error('暂停失败: ' + (e.message || '网络错误')) }
}

const closeJob = async (id) => {
  try {
    const res = await jobAPI.closeJob(id)
    if (res.code === 200) { ElMessage.success('岗位已关闭'); await loadJobs() }
    else { ElMessage.error(res.message || '关闭失败') }
  } catch (e) { ElMessage.error('关闭失败: ' + (e.message || '网络错误')) }
}

const deleteJob = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该岗位吗？', '确认删除', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    const res = await jobAPI.deleteJob(id)
    if (res.code === 200) { ElMessage.success('删除成功'); await loadJobs() }
    else { ElMessage.error(res.message || '删除失败') }
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败: ' + (e.message || '网络错误')) }
}
</script>

<style scoped>
.job-posting { padding: 20px; }
.card { background: #fff; border-radius: 16px; padding: 32px; margin-bottom: 28px; box-shadow: 0 6px 16px rgba(0,0,0,0.06); position: relative; }
.card::before { content: ""; position: absolute; top: 0; left: 0; width: 100%; height: 3px; background: linear-gradient(90deg,#165DFF,#2563EB,transparent); border-radius: 16px 16px 0 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.card h2 { font-size: 19px; font-weight: 600; padding-bottom: 12px; border-bottom: 1px solid #F2F3F5; position: relative; }
.card h2::after { content: ""; width: 50px; height: 3px; background: #165DFF; border-radius: 3px; position: absolute; left: 0; bottom: -1px; }
.data-table { width: 100%; border-collapse: collapse; margin-top: 16px; }
.data-table th { text-align: left; padding: 14px 12px; background: #F8F9FC; color: #4E5969; font-weight: 600; font-size: 14px; border-bottom: 2px solid #E2E8F0; }
.data-table td { padding: 14px 12px; border-bottom: 1px solid #F2F3F5; font-size: 14px; color: #1D2129; }
.data-table tr:hover td { background: rgba(22,93,255,0.03); }
.action-cell { white-space: nowrap; }
.btn { padding: 10px 20px; background: #165DFF; color: #fff; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; transition: all 0.25s ease; box-shadow: 0 3px 8px rgba(22,93,255,0.2); font-weight: 500; display: inline-flex; align-items: center; gap: 6px; }
.btn:hover { background: #0E42C1; box-shadow: 0 5px 15px rgba(22,93,255,0.3); transform: translateY(-2px); }
.btn-sm { padding: 6px 12px; font-size: 13px; }
.btn-outline { background: #fff; border: 1px solid #DCDFE6; color: #4E5969; box-shadow: none; }
.btn-outline:hover { background: #F5F7FA; border-color: #165DFF; color: #165DFF; transform: translateY(-2px); }
.status-badge { display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.status-0 { background: #F2F3F5; color: #86909C; }
.status-1 { background: #E8F5E9; color: #10B981; }
.status-2 { background: #FEE2E2; color: #EF4444; }
.status-3 { background: #FEF3C7; color: #F59E0B; }
.empty-state { text-align: center; padding: 60px 20px; }
</style>
