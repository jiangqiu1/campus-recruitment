<template>
  <div class="operation-log fade-in">
    <div class="page-header">
      <h2>操作日志</h2>
      <p>查看系统操作记录</p>
    </div>
    
    <!-- 筛选栏 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters" label-width="auto" size="default">
        <el-form-item label="操作类型">
          <el-select v-model="filters.operationType" placeholder="全部类型" clearable style="width: 180px;">
            <el-option v-for="opt in operationTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="filters.userId" placeholder="用户ID" clearable style="width: 120px;" />
        </el-form-item>
        <el-form-item label="目标ID">
          <el-input v-model="filters.targetId" placeholder="目标ID" clearable style="width: 120px;" />
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input v-model="filters.ipAddress" placeholder="搜索IP..." clearable style="width: 150px;" />
        </el-form-item>
        <el-form-item label="时间段">
          <el-date-picker
            v-model="timeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px;"
            value-format="YYYY-MM-DD HH:mm:ss"
            :default-value="[new Date(Date.now() - 7 * 86400000), new Date()]"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchLogs">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 日志表格 -->
    <el-card shadow="never" class="table-card">
      <div class="table-header">
        <span class="table-title">日志列表</span>
        <span class="table-count">共 {{ total }} 条</span>
      </div>

      <el-table 
        :data="logList" 
        stripe 
        style="width: 100%" 
        table-layout="auto"
        @sort-change="onSortChange"
        empty-text="暂无操作日志"
      >
        <el-table-column prop="id" label="ID" width="70" sortable="custom" />
        <el-table-column prop="userId" label="用户ID" min-width="90" show-overflow-tooltip sortable="custom" />
        <el-table-column prop="operationType" label="操作类型" width="200" sortable="custom">
          <template #default="{ row }">
            <el-tag :type="operationTypeTag(row.operationType)" size="small" effect="plain">
              {{ (row.operationType || '').includes(':') ? row.operationType.split(':')[1] : row.operationType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetId" label="目标ID/对象" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createTime" label="操作时间" min-width="170" show-overflow-tooltip sortable="custom">
          <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ') || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" link @click="deleteLog(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="loadLogs"
          @size-change="onPageSizeChange"
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { operationLogAPI } from '@/api'

const filters = reactive({
  operationType: '',
  userId: '',
  targetId: '',
  ipAddress: ''
})
const timeRange = ref([])

const logList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const sortField = ref('')
const sortOrder = ref('')

const operationTypeOptions = [
  { label: '登录', value: '登录' },
  { label: '创建', value: '创建' },
  { label: '更新', value: '更新' },
  { label: '删除', value: '删除' },
  { label: '审核', value: '审核' },
  { label: '安排面试', value: '面试' }
]

onMounted(() => {
  loadLogs()
})

const buildParams = () => {
  const params = {
    page: currentPage.value,
    size: pageSize.value
  }
  if (filters.operationType) params.operationType = filters.operationType
  if (filters.userId) params.userId = filters.userId
  if (filters.targetId) params.targetId = filters.targetId
  if (filters.ipAddress) params.ipAddress = filters.ipAddress
  if (timeRange.value && timeRange.value.length === 2) {
    params.startTime = timeRange.value[0]
    params.endTime = timeRange.value[1]
  }
  if (sortField.value && sortOrder.value) {
    params.sortField = sortField.value
    params.sortOrder = sortOrder.value
  }
  return params
}

const loadLogs = async () => {
  try {
    const params = buildParams()
    const res = await operationLogAPI.getLogs(params)
    if (res.code === 200) {
      let data = res.data || []
      if (Array.isArray(data)) {
        // 客户端过滤（后端未实现查询参数）
        if (filters.operationType) {
          data = data.filter(r => (r.operationType || '').includes(filters.operationType))
        }
        if (filters.userId) {
          data = data.filter(r => String(r.userId) === String(filters.userId))
        }
        if (filters.ipAddress) {
          const ip = filters.ipAddress.toLowerCase()
          data = data.filter(r => (r.ipAddress || '').toLowerCase().includes(ip))
        }
        if (timeRange.value && timeRange.value.length === 2) {
          const start = new Date(timeRange.value[0]).getTime()
          const end = new Date(timeRange.value[1]).getTime()
          data = data.filter(r => {
            const t = new Date(r.createTime).getTime()
            return t >= start && t <= end
          })
        }
        // 排序
        if (sortField.value && sortOrder.value) {
          data.sort((a, b) => {
            const va = a[sortField.value] || ''
            const vb = b[sortField.value] || ''
            const cmp = typeof va === 'number' ? va - vb : String(va).localeCompare(String(vb))
            return sortOrder.value === 'asc' ? cmp : -cmp
          })
        }
        total.value = data.length
        // 分页
        const start = (currentPage.value - 1) * pageSize.value
        logList.value = data.slice(start, start + pageSize.value)
      } else if (data.records) {
        logList.value = data.records
        total.value = data.total || data.records.length
      } else {
        logList.value = []
        total.value = 0
      }
    }
  } catch (error) {
    ElMessage.error('加载日志列表失败')
  }
}

const searchLogs = () => {
  currentPage.value = 1
  loadLogs()
}

const resetFilters = () => {
  filters.operationType = ''
  filters.userId = ''
  filters.targetId = ''
  filters.ipAddress = ''
  timeRange.value = []
  currentPage.value = 1
  sortField.value = ''
  sortOrder.value = ''
  loadLogs()
}

const onSortChange = ({ prop, order }) => {
  sortField.value = prop || ''
  sortOrder.value = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : ''
  loadLogs()
}

const onPageSizeChange = () => {
  currentPage.value = 1
  loadLogs()
}

const deleteLog = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该日志（ID: ' + row.id + '）吗？', '提示', { type: 'warning' })
    const res = await operationLogAPI.deleteLog(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadLogs()
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const operationTypeTag = (type) => {
  const desc = (type || '').includes(':') ? type.split(':')[1] : type
  if (desc.includes('登录')) return 'info'
  if (desc.includes('创建')) return 'success'
  if (desc.includes('更新')) return 'warning'
  if (desc.includes('删除')) return 'danger'
  if (desc.includes('审核')) return 'warning'
  if (desc.includes('面试')) return 'primary'
  return 'info'
}

const operationTypeText = (type) => {
  const desc = (type || '').includes(':') ? type.split(':')[1] : type
  return desc || '未知'
}
</script>

<style scoped>
.operation-log { padding: 20px; width: 100%; max-width: 100%; box-sizing: border-box; }
.operation-log :deep(.el-table) { width: 100% !important; }

.filter-card {
  margin-bottom: 16px;
  border-radius: 12px;
}
.filter-card :deep(.el-card__body) {
  padding: 20px 24px;
}
.filter-card .el-form {
  flex-wrap: wrap;
  gap: 0;
}

.table-card {
  border-radius: 12px;
}
.table-card :deep(.el-card__body) {
  padding: 20px 24px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #F2F3F5;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
  color: #1D2129;
}

.table-count {
  font-size: 13px;
  color: #86909C;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>

