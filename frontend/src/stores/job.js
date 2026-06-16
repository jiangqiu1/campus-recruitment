import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { jobAPI } from '@/api'
import { ElMessage } from 'element-plus'

export const useJobStore = defineStore('job', () => {
  // ==================== 状态 ====================
  const jobList = ref([])
  const currentJob = ref(null)
  const hotJobs = ref([])
  const recommendJobs = ref([])
  const loading = ref(false)
  const total = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(10)

  // 筛选条件
  const filters = ref({
    keyword: '',
    companyId: null,
    industry: '',
    jobType: '',
    salaryRange: '',
    location: '',
    status: ''
  })

  // 统计信息
  const statistics = ref({})

  // ==================== 计算属性 ====================
  const hasJobs = computed(() => jobList.value.length > 0)
  const isLoading = computed(() => loading.value)
  const activeJobs = computed(() => jobList.value.filter(job => job.status === 1))
  const hotJobCount = computed(() => hotJobs.value.length)

  // ==================== 方法 ====================

  /**
   * 获取岗位列表
   */
  async function fetchJobList(params = {}) {
    loading.value = true
    try {
      const queryParams = {
        page: currentPage.value,
        size: pageSize.value,
        ...filters.value,
        ...params
      }

      const res = await jobAPI.getJobs(queryParams)
      
      if (res.code === 200) {
        const data = res.data
        jobList.value = data.records || []
        total.value = data.total || 0
        currentPage.value = data.current || 1
        pageSize.value = data.size || 10
        return { success: true, data }
      } else {
        ElMessage.error(res.message || '获取岗位列表失败')
        return { success: false }
      }
    } catch (error) {
      const message = error.response?.data?.message || '获取岗位列表失败'
      ElMessage.error(message)
      return { success: false }
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取岗位详情
   */
  async function fetchJobDetail(jobId) {
    loading.value = true
    try {
      const res = await jobAPI.getJobDetail(jobId)
      
      if (res.code === 200) {
        currentJob.value = res.data
        return { success: true, data: response.data.data }
      } else {
        ElMessage.error(response.data.message || '获取岗位详情失败')
        return { success: false }
      }
    } catch (error) {
      const message = error.response?.data?.message || '获取岗位详情失败'
      ElMessage.error(message)
      return { success: false }
    } finally {
      loading.value = false
    }
  }

  /**
   * 创建岗位
   */
  async function createJob(jobData) {
    loading.value = true
    try {
      const res = await jobAPI.createJob(jobData)
      
      if (res.code === 200) {
        ElMessage.success('岗位创建成功')
        await fetchJobList()
        return { success: true, data: res.data }
      } else {
        ElMessage.error(response.data.message || '创建岗位失败')
        return { success: false }
      }
    } catch (error) {
      const message = error.response?.data?.message || '创建岗位失败'
      ElMessage.error(message)
      return { success: false }
    } finally {
      loading.value = false
    }
  }

  /**
   * 更新岗位
   */
  async function updateJob(jobId, jobData) {
    loading.value = true
    try {
      const res = await jobAPI.updateJob(jobId, jobData)
      
      if (res.code === 200) {
        ElMessage.success('岗位更新成功')
        // 刷新详情
        if (currentJob.value?.id === jobId) {
          await fetchJobDetail(jobId)
        }
        return { success: true }
      } else {
        ElMessage.error(response.data.message || '更新岗位失败')
        return { success: false }
      }
    } catch (error) {
      const message = error.response?.data?.message || '更新岗位失败'
      ElMessage.error(message)
      return { success: false }
    } finally {
      loading.value = false
    }
  }

  /**
   * 删除岗位
   */
  async function deleteJob(jobId) {
    try {
      const res = await jobAPI.deleteJob(jobId)
      
      if (res.code === 200) {
        ElMessage.success('岗位删除成功')
        await fetchJobList()
        return { success: true }
      } else {
        ElMessage.error(response.data.message || '删除岗位失败')
        return { success: false }
      }
    } catch (error) {
      const message = error.response?.data?.message || '删除岗位失败'
      ElMessage.error(message)
      return { success: false }
    }
  }

  /**
   * 批量删除岗位
   */
  async function batchDeleteJobs(ids) {
    try {
      const res = await jobAPI.batchDeleteJobs(ids)
      
      if (res.code === 200) {
        ElMessage.success(`成功删除 ${ids.length} 个岗位`)
        await fetchJobList()
        return { success: true }
      } else {
        ElMessage.error(response.data.message || '批量删除失败')
        return { success: false }
      }
    } catch (error) {
      const message = error.response?.data?.message || '批量删除失败'
      ElMessage.error(message)
      return { success: false }
    }
  }

  /**
   * 更新岗位状态
   */
  async function updateJobStatus(jobId, status) {
    try {
      const res = await jobAPI.updateJobStatus(jobId, { status })
      
      if (res.code === 200) {
        ElMessage.success('状态更新成功')
        // 刷新详情
        if (currentJob.value?.id === jobId) {
          await fetchJobDetail(jobId)
        }
        return { success: true }
      } else {
        ElMessage.error(response.data.message || '状态更新失败')
        return { success: false }
      }
    } catch (error) {
      const message = error.response?.data?.message || '状态更新失败'
      ElMessage.error(message)
      return { success: false }
    }
  }

  /**
   * 搜索岗位
   */
  async function searchJobs(keyword) {
    loading.value = true
    try {
      const res = await jobAPI.searchJobs(keyword, { page: currentPage.value, size: pageSize.value })
      
      if (res.code === 200) {
        const data = res.data
        jobList.value = data.records || []
        total.value = data.total || 0
        return { success: true, data }
      } else {
        return { success: false }
      }
    } catch (error) {
      console.error('搜索岗位失败:', error)
      return { success: false }
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取热门岗位
   */
  async function fetchHotJobs(limit = 10) {
    try {
      const res = await jobAPI.fetchHotJobs(limit)
      
      if (res.code === 200) {
        hotJobs.value = res.data || []
        return { success: true, data: response.data.data }
      } else {
        return { success: false }
      }
    } catch (error) {
      console.error('获取热门岗位失败:', error)
      return { success: false }
    }
  }

  /**
   * 获取推荐岗位
   */
  async function fetchRecommendJobs(studentId, limit = 10) {
    try {
      const res = await jobAPI.fetchRecommendJobs(studentId, limit)
      
      if (res.code === 200) {
        recommendJobs.value = res.data || []
        return { success: true, data: response.data.data }
      } else {
        return { success: false }
      }
    } catch (error) {
      console.error('获取推荐岗位失败:', error)
      return { success: false }
    }
  }

  /**
   * 获取岗位统计信息
   */
  async function fetchJobStatistics() {
    try {
      const res = await jobAPI.fetchJobStatistics()
      
      if (res.code === 200) {
        statistics.value = res.data || {}
        return { success: true, data: response.data.data }
      } else {
        return { success: false }
      }
    } catch (error) {
      console.error('获取岗位统计失败:', error)
      return { success: false }
    }
  }

  /**
   * 设置筛选条件
   */
  function setFilters(newFilters) {
    filters.value = { ...filters.value, ...newFilters }
  }

  /**
   * 重置筛选条件
   */
  function resetFilters() {
    filters.value = {
      keyword: '',
      companyId: null,
      industry: '',
      jobType: '',
      salaryRange: '',
      location: '',
      status: ''
    }
  }

  /**
   * 设置分页
   */
  function setPagination(page, size) {
    currentPage.value = page
    pageSize.value = size
  }

  /**
   * 清空当前岗位
   */
  function clearCurrentJob() {
    currentJob.value = null
  }

  /**
   * 重置状态
   */
  function resetState() {
    jobList.value = []
    currentJob.value = null
    hotJobs.value = []
    recommendJobs.value = []
    loading.value = false
    total.value = 0
    currentPage.value = 1
    pageSize.value = 10
    resetFilters()
    statistics.value = {}
  }

  return {
    // 状态
    jobList,
    currentJob,
    hotJobs,
    recommendJobs,
    loading,
    total,
    currentPage,
    pageSize,
    filters,
    statistics,
    
    // 计算属性
    hasJobs,
    isLoading,
    activeJobs,
    hotJobCount,
    
    // 方法
    fetchJobList,
    fetchJobDetail,
    createJob,
    updateJob,
    deleteJob,
    batchDeleteJobs,
    updateJobStatus,
    searchJobs,
    fetchHotJobs,
    fetchRecommendJobs,
    fetchJobStatistics,
    setFilters,
    resetFilters,
    setPagination,
    clearCurrentJob,
    resetState
  }
})
