import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    if (error.response) {
      if (error.response.status === 401) {
        ElMessage.error('登录已过期，请重新登录')
        localStorage.clear()
        window.location.href = '/login'
      } else if (error.response.status === 403) {
        ElMessage.error('没有权限访问')
      } else {
        ElMessage.error(error.response.data?.message || '服务器错误')
      }
    } else {
      ElMessage.error('网络连接失败')
    }
    return Promise.reject(error)
  }
)

// ==================== 认证模块 ====================
export const authAPI = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
  logout: () => request.post('/auth/logout', {}),
  changePassword: (data) => request.post('/auth/change-password', data),
  checkToken: () => request.get('/auth/check')
}

// ==================== 用户管理模块 ====================
export const userAPI = {
  getUsers: (params) => request.get('/admin/users', { params }),
  createUser: (data) => request.post('/admin/users', data),
  updateUser: (id, data) => request.put('/admin/users/' + id, data),
  deleteUser: (id) => request.delete('/admin/users/' + id),
  batchDeleteUsers: (ids) => request.post('/admin/users/batch-delete', ids),
  updateUserStatus: (id, status) => request.put('/admin/users/' + id + '/status', status),
  getUserInfo: () => request.get('/user/info'),
  updateUserInfo: (data) => request.put('/user/info', data)
}

// ==================== 企业管理模块 ====================
export const companyAPI = {
  getCompanies: (params) => request.get('/companies', { params }),
  getAllCompanies: (params) => request.get('/companies', { params }),
  createCompany: (data) => request.post('/companies', data),
  updateCompany: (id, data) => request.put('/companies/' + id, data),
  deleteCompany: (id) => request.delete('/companies/' + id),
  approveCompany: (id) => request.put('/companies/' + id + '/approve'),
  rejectCompany: (id) => request.put('/companies/' + id + '/reject'),
  getCompanyProfile: (id) => request.get('/companies/' + id)
}

export const companyAccountAPI = {
  listAccounts: (companyId) => request.get('/companies/' + companyId + '/accounts'),
  createAccount: (companyId, data) => request.post('/companies/' + companyId + '/accounts', data),
  updateAccount: (companyId, userId, data) => request.put('/companies/' + companyId + '/accounts/' + userId, data),
  deleteAccount: (companyId, userId) => request.delete('/companies/' + companyId + '/accounts/' + userId)
}

// ==================== 岗位管理模块 ====================
export const jobAPI = {
  getJobs: (params) => request.get('/jobs', { params }),
  getJobDetail: (id) => request.get('/jobs/' + id),
  createJob: (data) => request.post('/jobs', data),
  updateJob: (id, data) => request.put('/jobs/' + id, data),
  deleteJob: (id) => request.delete('/jobs/' + id),
  batchDeleteJobs: (ids) => request.post('/jobs/batch-delete', ids),
  updateJobStatus: (id, status) => request.put('/jobs/' + id + '/status', status),
  getJobsByCompany: (companyId, params) => request.get('/jobs/by-company/' + companyId, { params }),
  searchJobs: (keyword, params) => request.get('/jobs/search', { params: { ...params, keyword } }),
  fetchHotJobs: (limit) => request.get('/jobs/hot', { params: { limit } }),
  fetchRecommendJobs: (studentId, limit) => request.get('/jobs/recommend', { params: { studentId, limit } }),
  fetchJobStatistics: () => request.get('/jobs/statistics'),
  getJobAnalysis: () => request.get('/hr/job-analysis'),
  getJobsByCreator: (creatorId) => request.get('/jobs/by-creator/' + creatorId),
  publishJob: (id) => request.put('/jobs/' + id + '/publish'),
  closeJob: (id) => request.put('/jobs/' + id + '/close'),
  pauseJob: (id) => request.put('/jobs/' + id + '/pause')
}

// ==================== 简历管理模块 ====================
export const resumeAPI = {
  getResumes: (params) => request.get('/resumes', { params }),
  getResumeDetail: (id) => request.get('/resumes/' + id),
  getResumeByStudent: (studentId) => request.get('/resumes/student/' + studentId),
  createResume: (data) => request.post('/resumes', data),
  updateResume: (id, data) => request.put('/resumes/' + id, data),
  deleteResume: (id) => request.delete('/resumes/' + id),
  batchDeleteResumes: (ids) => request.post('/resumes/batch-delete', ids),
  updateResumeStatus: (id, status) => request.put('/resumes/' + id + '/status', { status }),
  searchResumes: (keyword, params) => request.get('/resumes/search', { params: { ...params, keyword } }),
  uploadResume: (studentId, file) => {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('type', 'resume')
    return request.post('/files/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

// ==================== 投递管理模块 ====================
export const deliveryAPI = {
  getDeliveries: (params) => request.get('/deliveries', { params }),
  createDelivery: (data) => request.post('/deliveries', data),
  updateDeliveryStatus: (id, status) => request.put('/deliveries/' + id + '/status', { status }),
  deleteDelivery: (id) => request.delete('/deliveries/' + id),
  getDeliveriesByJob: (jobId) => request.get('/deliveries/by-job/' + jobId),
  getDeliveryStatsByJob: (jobId) => request.get('/deliveries/statistics/by-job/' + jobId),
  getDeliveriesByStudent: (studentId) => request.get('/deliveries/by-student/' + studentId),
  arrangeInterview: (id, data) => request.put('/deliveries/' + id + '/arrange-interview', data)
}

// ==================== 班级管理模块 ====================
export const classAPI = {
  getClasses: (params) => request.get('/classes', { params }),
  createClass: (data) => request.post('/classes', data),
  updateClass: (id, data) => request.put('/classes/' + id, data),
  deleteClass: (id) => request.delete('/classes/' + id),
  batchDeleteClasses: (ids) => request.post('/classes/batch-delete', ids),
  getClassStudents: (classId) => request.get('/classes/' + classId + '/students'),
  getClassesByTeacher: (teacherId) => request.get('/classes/by-teacher/' + teacherId),
  getAllClasses: (params) => request.get('/classes', { params })
}

// ==================== 人岗匹配模块 ====================
export const jobMatchAPI = {
  getMatches: (params) => request.get('/job-matches', { params }),
  generateMatch: (data) => request.post('/job-matches/generate', data),
  batchGenerateMatches: (jobId) => request.post('/job-matches/batch-generate/' + jobId),
  pushMatch: (id) => request.put('/job-matches/' + id + '/push'),
  deleteMatch: (id) => request.delete('/job-matches/' + id)
}

// ==================== 简历智能评分模块 ====================
export const resumeScoreAPI = {
  getScores: (params) => request.get('/resume-scores', { params }),
  scoreResume: (data) => request.post('/resume-scores/score', data),
  batchScoreResumes: (jobId) => request.post('/resume-scores/batch-score/' + jobId),
  batchScoreByCompany: (companyId) => request.post('/resume-scores/batch-score-by-company/' + companyId),
  getScoreDetail: (id) => request.get('/resume-scores/' + id),
  deleteScore: (id) => request.delete('/resume-scores/' + id),
  getMatchDistribution: (jobId) => request.get('/resume-scores/statistics/match-distribution/' + jobId),
  getAverageScoreByJobId: (jobId) => request.get('/resume-scores/statistics/average-score/' + jobId)
}

// ==================== AI 解析日志模块 ====================
export const aiParseAPI = {
  getParseLogs: (params) => request.get('/ai-parse-logs', { params }),
  deleteParseLog: (id) => request.delete('/ai-parse-logs/' + id)
}

// ==================== 操作日志模块 ====================
export const operationLogAPI = {
  getLogs: (params) => request.get('/operation-logs', { params }),
  deleteLog: (id) => request.delete('/operation-logs/' + id),
  cleanupLogs: (beforeTime) => request.delete('/operation-logs/cleanup', { params: { beforeTime } })
}

// ==================== 系统设置模块 ====================
export const settingsAPI = {
  getSettings: () => request.get('/settings'),
  saveBasic: (data) => request.post('/settings/basic', data),
  saveSecurity: (data) => request.post('/settings/security', data),
  saveNotification: (data) => request.post('/settings/notification', data)
}

// ==================== 数据统计模块 ====================
export const statisticsAPI = {
  getOverview: () => request.get('/statistics/overview'),
  getDeliveryTrend: (params) => request.get('/statistics/delivery-trend', { params }),
  getHotJobs: () => request.get('/statistics/hot-jobs'),
  getRecentActivities: () => request.get('/statistics/recent-activities'),
  getTeacherDashboard: () => request.get('/statistics/teacher/dashboard'),
  getEmploymentDistribution: () => request.get('/statistics/teacher/employment-distribution'),
  getHrDashboard: () => request.get('/statistics/hr/dashboard'),
  getScoreDistribution: (params) => request.get('/statistics/hr/score-distribution', { params })
}

// ==================== 岗位变更申请模块 ====================
export const jobChangeAPI = {
  list: (params) => request.get('/job-changes', { params }),
  submit: (data) => request.post('/job-changes', data),
  approve: (id) => request.put('/job-changes/' + id + '/approve'),
  reject: (id) => request.put('/job-changes/' + id + '/reject')
}

// ==================== 数据导出模块 ====================
export const dataExportAPI = {
  export: (data) => request.post('/export', data, { responseType: 'blob' })
}

export default {
  auth: authAPI,
  user: userAPI,
  company: companyAPI,
  companyAccount: companyAccountAPI,
  job: jobAPI,
  resume: resumeAPI,
  delivery: deliveryAPI,
  class: classAPI,
  jobMatch: jobMatchAPI,
  resumeScore: resumeScoreAPI,
  aiParse: aiParseAPI,
  operationLog: operationLogAPI,
  settings: settingsAPI,
  statistics: statisticsAPI,
  jobChange: jobChangeAPI,
  dataExport: dataExportAPI
}