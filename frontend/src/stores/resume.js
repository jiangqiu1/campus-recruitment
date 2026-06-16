import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { resumeAPI } from '@/api'
import { ElMessage } from 'element-plus'

export const useResumeStore = defineStore('resume', () => {
  // ==================== 状态 ====================
  const resumeList = ref([])
  const currentResume = ref(null)
  const loading = ref(false)
  const total = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(10)

  // 筛选条件
  const filters = ref({
    studentName: '',
    major: '',
    status: '',
    classId: null
  })

  // ==================== 计算属性 ====================
  const hasResumes = computed(() => resumeList.value.length > 0)
  const isLoading = computed(() => loading.value)

  // ==================== 方法 ====================

  /**
   * 获取简历列表
   */
  async function fetchResumeList(params = {}) {
    loading.value = true
    try {
      const queryParams = {
        page: currentPage.value,
        size: pageSize.value,
        ...filters.value,
        ...params
      }

      const res = await resumeAPI.getResumes(queryParams)
      
      if (res.code === 200) {
        const data = res.data
        resumeList.value = data.records || []
        total.value = data.total || 0
        currentPage.value = data.current || 1
        pageSize.value = data.size || 10
        return { success: true, data }
      } else {
        ElMessage.error(response.data.message || '获取简历列表失败')
        return { success: false }
      }
    } catch (error) {
      const message = error.response?.data?.message || '获取简历列表失败'
      ElMessage.error(message)
      return { success: false }
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取简历详情
   */
  async function fetchResumeDetail(resumeId) {
    loading.value = true
    try {
      const res = await resumeAPI.getResumeDetail(resumeId)
      
      if (res.code === 200) {
        currentResume.value = res.data
        return { success: true, data: response.data.data }
      } else {
        ElMessage.error(response.data.message || '获取简历详情失败')
        return { success: false }
      }
    } catch (error) {
      const message = error.response?.data?.message || '获取简历详情失败'
      ElMessage.error(message)
      return { success: false }
    } finally {
      loading.value = false
    }
  }

  /**
   * 创建简历
   */
  async function createResume(resumeData) {
    loading.value = true
    try {
      const res = await resumeAPI.createResume(resumeData)
      
      if (res.code === 200) {
        ElMessage.success('简历创建成功')
        // 刷新列表
        await fetchResumeList()
        return { success: true, data: response.data.data }
      } else {
        ElMessage.error(response.data.message || '创建简历失败')
        return { success: false }
      }
    } catch (error) {
      const message = error.response?.data?.message || '创建简历失败'
      ElMessage.error(message)
      return { success: false }
    } finally {
      loading.value = false
    }
  }

  /**
   * 更新简历
   */
  async function updateResume(resumeId, resumeData) {
    loading.value = true
    try {
      const res = await resumeAPI.updateResume(resumeId, resumeData)
      
      if (res.code === 200) {
        ElMessage.success('简历更新成功')
        // 刷新详情
        if (currentResume.value?.id === resumeId) {
          await fetchResumeDetail(resumeId)
        }
        return { success: true }
      } else {
        ElMessage.error(response.data.message || '更新简历失败')
        return { success: false }
      }
    } catch (error) {
      const message = error.response?.data?.message || '更新简历失败'
      ElMessage.error(message)
      return { success: false }
    } finally {
      loading.value = false
    }
  }

  /**
   * 删除简历
   */
  async function deleteResume(resumeId) {
    try {
      const res = await resumeAPI.deleteResume(resumeId)
      
      if (res.code === 200) {
        ElMessage.success('简历删除成功')
        // 刷新列表
        await fetchResumeList()
        return { success: true }
      } else {
        ElMessage.error(response.data.message || '删除简历失败')
        return { success: false }
      }
    } catch (error) {
      const message = error.response?.data?.message || '删除简历失败'
      ElMessage.error(message)
      return { success: false }
    }
  }

  /**
   * 批量删除简历
   */
  async function batchDeleteResumes(ids) {
    try {
      const res = await resumeAPI.batchDeleteResumes(ids)
      
      if (res.code === 200) {
        ElMessage.success(`成功删除 ${ids.length} 条简历`)
        await fetchResumeList()
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
   * 更新简历状态（审核）
   */
  async function updateResumeStatus(resumeId, status) {
    try {
      const res = await resumeAPI.updateResumeStatus(resumeId, { status })
      
      if (res.code === 200) {
        ElMessage.success('状态更新成功')
        // 刷新详情
        if (currentResume.value?.id === resumeId) {
          await fetchResumeDetail(resumeId)
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
   * 搜索简历
   */
  async function searchResumes(keyword) {
    loading.value = true
    try {
      const res = await resumeAPI.searchResumes(keyword, { page: currentPage.value, size: pageSize.value })
      
      if (res.code === 200) {
        const data = res.data
        resumeList.value = data.records || []
        total.value = data.total || 0
        return { success: true, data }
      } else {
        return { success: false }
      }
    } catch (error) {
      console.error('搜索简历失败:', error)
      return { success: false }
    } finally {
      loading.value = false
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
      studentName: '',
      major: '',
      status: '',
      classId: null
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
   * 清空当前简历
   */
  function clearCurrentResume() {
    currentResume.value = null
  }

  /**
   * 重置状态
   */
  function resetState() {
    resumeList.value = []
    currentResume.value = null
    loading.value = false
    total.value = 0
    currentPage.value = 1
    pageSize.value = 10
    resetFilters()
  }

  return {
    // 状态
    resumeList,
    currentResume,
    loading,
    total,
    currentPage,
    pageSize,
    filters,
    
    // 计算属性
    hasResumes,
    isLoading,
    
    // 方法
    fetchResumeList,
    fetchResumeDetail,
    createResume,
    updateResume,
    deleteResume,
    batchDeleteResumes,
    updateResumeStatus,
    searchResumes,
    setFilters,
    resetFilters,
    setPagination,
    clearCurrentResume,
    resetState
  }
})
