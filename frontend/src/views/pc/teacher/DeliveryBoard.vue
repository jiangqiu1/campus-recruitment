<template>
  <div class="delivery-board">
    <div class="card">
      <div class="card-header">
        <h2>投递追踪看板</h2>
      </div>

      <template v-if="deliveryStats.length > 0">
        <table class="data-table">
          <thead>
            <tr>
              <th>岗位名称</th>
              <th>查看/投递</th>
              <th>待处理</th>
              <th>已面试</th>
              <th>已录用</th>
              <th>不合适</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="stat in deliveryStats" :key="stat.id">
              <td>{{ stat.title }}</td>
              <td>{{ stat.views }} / {{ stat.totalDeliveries }}</td>
              <td><span style="color:#F59E0B;font-weight:600;">{{ stat.pending }}</span></td>
              <td><span style="color:#165DFF;font-weight:600;">{{ stat.interview }}</span></td>
              <td><span style="color:#10B981;font-weight:600;">{{ stat.hired }}</span></td>
              <td><span style="color:#EF4444;font-weight:600;">{{ stat.rejected }}</span></td>
              <td>
                <button class="btn btn-sm btn-outline" @click="viewJobDeliveries(stat)">查看学生</button>
              </td>
            </tr>
          </tbody>
        </table>
      </template>
      <div v-else class="empty-state">
        <i class="fa fa-inbox" style="font-size:48px;color:#C9CDD4;"></i>
        <p style="margin-top:16px;color:#86909C;">暂无投递记录</p>
      </div>
    </div>

    <!-- 投递明细对话框 -->
    <el-dialog v-model="detailDialogVisible" :title="selectedJobTitle + ' - 投递明细'" width="800px">
      <el-table :data="deliveryDetailList" border stripe style="width:100%;" v-if="deliveryDetailList.length > 0">
        <el-table-column label="学生姓名" width="120">
          <template #default="scope">{{ scope.row.studentName || '-' }}</template>
        </el-table-column>
        <el-table-column label="岗位" min-width="150">
          <template #default="scope">{{ scope.row.jobTitle || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <span :class="'status-badge status-' + (scope.row.status != null ? scope.row.status : 0)">{{ deliveryStatusLabel(scope.row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="投递时间" width="170">
          <template #default="scope">{{ scope.row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button link type="primary" @click="viewStudentResume(scope.row)">查看简历</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-else class="empty-state" style="padding:30px;">
        <p style="color:#86909C;">该岗位暂无投递记录</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { jobAPI, deliveryAPI, resumeAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const deliveryStats = ref([])
const detailDialogVisible = ref(false)
const selectedJobTitle = ref('')
const deliveryDetailList = ref([])

onMounted(async () => { await loadDeliveryStats() })

const loadDeliveryStats = async () => {
  if (!userStore.userId) return
  try {
    const jobRes = await jobAPI.getJobsByCreator(userStore.userId)
    if (jobRes.code === 200 && jobRes.data) {
      const jobs = jobRes.data
      const stats = await Promise.all(jobs.map(async (job) => {
        try {
          const dRes = await deliveryAPI.getDeliveriesByJob(job.id)
          const deliveries = (dRes.code === 200 && dRes.data) ? dRes.data : []
          return {
            id: job.id,
            title: job.title || '',
            views: job.viewCount || 0,
            totalDeliveries: deliveries.length,
            pending: deliveries.filter(d => d.status === 0 || d.status === 1).length,
            interview: deliveries.filter(d => d.status === 2).length,
            hired: deliveries.filter(d => d.status === 3).length,
            rejected: deliveries.filter(d => d.status === 4).length
          }
        } catch { return { id: job.id, title: job.title || '', views: 0, totalDeliveries: 0, pending: 0, interview: 0, hired: 0, rejected: 0 } }
      }))
      deliveryStats.value = stats
    }
  } catch (e) { console.error('加载投递统计失败', e) }
}

const deliveryStatusLabel = (s) => {
  const map = { 0: '已投递', 1: '企业已查看', 2: '待面试', 3: '已录用', 4: '不合适' }
  return map[s] || '未知'
}

const viewJobDeliveries = async (stat) => {
  selectedJobTitle.value = stat.title
  deliveryDetailList.value = []
  detailDialogVisible.value = true
  try {
    const dRes = await deliveryAPI.getDeliveriesByJob(stat.id)
    if (dRes.code === 200) deliveryDetailList.value = dRes.data || []
  } catch (e) { ElMessage.error('加载投递详情失败: ' + (e.message || '网络错误')) }
}

const viewStudentResume = async (delivery) => {
  if (!delivery.studentId) { ElMessage.warning('缺少学生信息'); return }
  try {
    const res = await resumeAPI.getResumeByStudent(delivery.studentId)
    if (res.code === 200 && res.data) {
      ElMessage.info('姓名: ' + (res.data.name || '-') + ' | 专业: ' + (res.data.major || '-'))
    } else {
      ElMessage.warning('该学生暂无简历')
    }
  } catch (e) { ElMessage.error('查看简历失败: ' + (e.message || '网络错误')) }
}
</script>

<style scoped>
.delivery-board { padding: 20px; }
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
.status-badge { display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.status-0 { background: #E8F5E9; color: #10B981; }
.status-1 { background: #DBEAFE; color: #3B82F6; }
.status-2 { background: #FEF3C7; color: #F59E0B; }
.status-3 { background: #D1FAE5; color: #059669; }
.status-4 { background: #FEE2E2; color: #EF4444; }
.empty-state { text-align: center; padding: 60px 20px; }
</style>
