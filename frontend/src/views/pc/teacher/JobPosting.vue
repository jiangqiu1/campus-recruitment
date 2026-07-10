<template>
  <div class="job-posting fade-in">
    <div class="page-header">
      <h2>AI岗位发布</h2>
      <p>AI 智能解析 · 一键发布岗位</p>
    </div>
    <div class="card">
      <div class="card-header">
        <button class="btn" @click="showAIDialog"><i class="fa fa-magic"></i> AI解析发布</button>
      </div>

      <table class="data-table" v-if="jobList.length > 0">
        <thead>
          <tr>
            <th>岗位名称</th>
            <th>招聘企业</th>
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
            <td>{{ job.companyName || '-' }}</td>
            <td>{{ job.salaryRange || '-' }}</td>
            <td>{{ job.location || '-' }}</td>
            <td>{{ job.education || '-' }}</td>
            <td>{{ job.deadline || '-' }}</td>
            <td>{{ job.viewCount || 0 }}</td>
            <td><span :class="'status-badge status-' + (job.status != null ? job.status : 0)">{{ statusLabel(job.status) }}</span></td>
            <td class="action-cell">
              <button class="btn btn-sm btn-outline" style="margin-right:6px;" @click="editJob(job)">编辑</button>
              <button v-if="job.status === 0 || job.status === 2 || job.status === 3" class="btn btn-sm btn-outline" style="margin-right:6px;color:#10B981;border-color:#10B981;" @click="publishJob(job.id)">发布</button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑岗位' : 'AI解析发布'" width="680px" :close-on-click-modal="false"
      append-to-body modal-class="job-overlay">
      <!-- AI解析输入区（仅新建时显示） -->
      <div v-if="!isEdit" class="ai-parse-section">
        <div class="ai-parse-header">
          <el-icon style="color:#165DFF;margin-right:6px"><MagicStick /></el-icon>
          <span>粘贴岗位描述，AI自动解析填充</span>
        </div>
        <el-input v-model="aiRawText" type="textarea" :rows="4" placeholder="请粘贴完整的岗位描述文本（JD），包含岗位名称、薪资、职责、要求等信息..." />
        <el-button size="small" type="primary" :loading="aiParsing" style="margin-top:8px" @click="aiParseJob" :disabled="!aiRawText.trim()">
          <el-icon style="margin-right:4px"><MagicStick /></el-icon>
          AI 解析
        </el-button>
        <el-divider />
      </div>
      <el-form :model="form" label-width="100px" size="default">
        <el-form-item label="招聘企业" required>
          <el-select v-model="form.companyId" placeholder="请选择企业" style="width:100%;" :teleported="false" @change="onCompanyChange">
            <el-option v-for="c in companies" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位名称" required>
          <el-input v-model="form.title" placeholder="请输入岗位名称" />
        </el-form-item>
        <el-form-item label="薪资范围">
          <el-input v-model="form.salaryRange" placeholder="如: 8k-12k" />
        </el-form-item>
        <el-form-item label="学历要求">
          <el-select v-model="form.education" placeholder="请选择" style="width:100%;" :teleported="false">
            <el-option label="不限" value="不限" />
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
          <el-date-picker v-model="form.deadline" type="date" placeholder="选择日期" style="width:100%;" format="YYYY-MM-DD" value-format="YYYY-MM-DD" :teleported="false" />
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
import { jobAPI, aiParseAPI, companyAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'

const userStore = useUserStore()
const jobList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const aiParsing = ref(false)
const aiRawText = ref('')
const companies = ref([])
const form = ref({
  id: null, title: '', companyId: null, companyName: '', salaryRange: '', education: '', location: '',
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
  form.value = { id: null, title: '', companyId: null, companyName: '', salaryRange: '', education: '', location: '', deadline: '', description: '', requirement: '', requiredSkills: '' }
  isEdit.value = false
  aiRawText.value = ''
}

const showAIDialog = async () => { resetForm(); await loadCompanies(); dialogVisible.value = true }

const editJob = async (job) => {
  form.value = { id: job.id, title: job.title || '', companyId: job.companyId || null, companyName: job.companyName || '', salaryRange: job.salaryRange || '', education: job.education || '', location: job.location || '', deadline: job.deadline || '', description: job.description || '', requirement: job.requirement || '', requiredSkills: job.requiredSkills || '' }
  isEdit.value = true
  await loadCompanies()
  dialogVisible.value = true
}

const loadCompanies = async () => {
  try {
    const res = await companyAPI.getCompanies()
    if (res.code === 200) companies.value = res.data || []
  } catch (e) { console.error('加载企业列表失败', e) }
}

const onCompanyChange = (id) => {
  const c = companies.value.find(c => c.id === id)
  form.value.companyName = c ? c.name : ''
}

const saveJob = async () => {
  if (!form.value.title.trim()) { ElMessage.warning('请输入岗位名称'); return }
  if (!form.value.companyId) { ElMessage.warning('请选择招聘企业'); return }
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

const aiParseJob = async () => {
  if (!aiRawText.value.trim()) return
  aiParsing.value = true
  try {
    // 优先调用后端 AI 接口（DeepSeek）解析，更准确
    let parsed = null
    try {
      const res = await aiParseAPI.parseJob(aiRawText.value)
      if (res.code === 200 && res.data) {
        parsed = res.data
      }
    } catch (e) { /* 后端 AI 失败，降级到本地正则 */ }

    if (parsed) {
      // 后端 AI 解析结果填充
      if (parsed.title) form.value.title = parsed.title
      if (parsed.salaryRange) form.value.salaryRange = parsed.salaryRange
      if (parsed.education) form.value.education = parsed.education
      if (parsed.location) form.value.location = parsed.location
      if (parsed.deadline) form.value.deadline = parsed.deadline
      if (parsed.description) form.value.description = parsed.description
      if (parsed.requirements) form.value.requirement = parsed.requirements
      if (parsed.skills && Array.isArray(parsed.skills)) {
        form.value.requiredSkills = parsed.skills.join(',')
      }
      // 从原始文本中自动匹配企业（后端 AI 不返回 companyName）
      if (companies.value.length > 0 && !form.value.companyId) {
        const text = aiRawText.value
        const matchedCompany = companies.value.find(c => text.includes(c.name))
        if (matchedCompany) {
          form.value.companyId = matchedCompany.id
          form.value.companyName = matchedCompany.name
        }
      }
      ElMessage.success('AI解析完成，请核对信息')
    } else {
      // 前端本地关键词解析（降级方案）
      const text = aiRawText.value
      const lines = text.split('\n').filter(l => l.trim())
      
      // 解析岗位名称
      const titleMatch = text.match(/(?:岗位|职位|招聘|急招)[：:]\s*(.+)/) || text.match(/^(.{2,20}（?[实习全职兼职]?）?)$/m)
      if (titleMatch) form.value.title = titleMatch[1].trim()
      
      // 解析薪资
      const salaryMatch = text.match(/(\d+)\s*[kK]?\s*[～\-~]\s*(\d+)\s*[kK]?/) || text.match(/[薪工][资酬][：:]\s*(\S+)/)
      if (salaryMatch) {
        if (salaryMatch[2]) form.value.salaryRange = salaryMatch[1] + 'k-' + salaryMatch[2] + 'k'
        else form.value.salaryRange = salaryMatch[1] + 'k'
      }
      
      // 解析学历
      if (/博士/i.test(text)) form.value.education = '博士'
      else if (/硕士/i.test(text)) form.value.education = '硕士'
      else if (/本科/i.test(text)) form.value.education = '本科'
      else if (/大专/i.test(text)) form.value.education = '大专'
      else form.value.education = '不限'
      
      // 解析地点（支持"地点："格式）
      const locMatch = text.match(/(?:工作[地点]|地点|[城地]市|上班)[：: ]\s*(\S{2,10})/)
      if (locMatch) form.value.location = locMatch[1]
      
      // 解析截止日期
      const deadlineMatch = text.match(/(?:截止[日期]?|截至)[：: ]\s*(.+)/)
      if (deadlineMatch) {
        let dl = deadlineMatch[1].trim()
        const chineseDate = dl.match(/(\d{1,2})月(\d{1,2})[号日]/)
        if (chineseDate) {
          const now = new Date()
          const year = now.getFullYear()
          form.value.deadline = year + '-' + String(chineseDate[1]).padStart(2, '0') + '-' + String(chineseDate[2]).padStart(2, '0')
        } else {
          const stdDate = dl.match(/(\d{4})[年\/\-.](\d{1,2})[月\/\-.]?(\d{1,2})/)
          if (stdDate) {
            form.value.deadline = stdDate[1] + '-' + String(stdDate[2]).padStart(2, '0') + '-' + String(stdDate[3]).padStart(2, '0')
          }
        }
      }
      
      // 解析描述和要求
      const descParts = text.split(/(?:岗位职责|工作内容|任职要求|职位要求|岗位要求)[：:]/i)
      if (descParts.length >= 2) form.value.description = descParts[1].trim().substring(0, 500)
      if (descParts.length >= 3) form.value.requirement = descParts[2].trim().substring(0, 500)
      
      // 解析技能
      const skillKeywords = ['Java','Spring','Vue','React','Python','Go','MySQL','Redis','Docker','K8s','Linux','JavaScript','TypeScript','HTML','CSS','Node.js','PHP','C++','Flutter','小程序','微服务','分布式']
      const foundSkills = skillKeywords.filter(s => text.includes(s))
      if (foundSkills.length > 0) form.value.requiredSkills = foundSkills.join(',')
      
      // 本地模式：尝试从文本中匹配企业名称
      if (companies.value.length > 0) {
        const matchedCompany = companies.value.find(c => text.includes(c.name))
        if (matchedCompany && !form.value.companyId) {
          form.value.companyId = matchedCompany.id
          form.value.companyName = matchedCompany.name
        }
      }
      
      ElMessage.success('解析完成（本地模式），请核对并补充信息')
    }
  } catch (e) {
    console.error('AI解析失败', e)
    ElMessage.error('AI解析失败')
  } finally {
    aiParsing.value = false
  }
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
.status-badge { display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; white-space: nowrap; }
.status-0 { background: #F2F3F5; color: #86909C; }
.status-1 { background: #E8F5E9; color: #10B981; }
.status-2 { background: #FEE2E2; color: #EF4444; }
.status-3 { background: #FEF3C7; color: #F59E0B; }
.empty-state { text-align: center; padding: 60px 20px; }
.ai-parse-section { background: #F8F9FC; border-radius: 10px; padding: 16px; margin-bottom: 16px; border: 1px solid #E2E8F0; }
.ai-parse-header { display: flex; align-items: center; font-weight: 500; color: #1D2129; margin-bottom: 10px; }
</style>

<style>
.job-overlay {
  position: fixed !important;
  top: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  left: 0 !important;
  background: rgba(0, 0, 0, 0.45) !important;
  z-index: 9999 !important;
}
</style>
