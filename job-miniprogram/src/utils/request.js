// 优先读取自定义API地址（存储中修改），其次环境变量，默认localhost
const getBaseUrl = () => {
  try {
    const custom = uni.getStorageSync('api_base_url')
    if (custom) return custom
  } catch (e) { /* ignore */ }
  return 'http://localhost:8080/api'
}

const BASE_URL = getBaseUrl()

const getStudentId = () => {
  try {
    const raw = uni.getStorageSync('userInfo')
    if (!raw) return null
    const obj = JSON.parse(raw)
    const sid = obj.id || obj.userId
    return sid ? Number(sid) : null
  } catch (e) { return null }
}

/**
 * 通用请求封装，自动注入 token
 * 主要适配微信小程序（uni-app/WeChat Dev）的 HTTP 行为：
 * - GET请求：data自动转 query string（由 uni-app 底层处理）
 * - POST/PUT请求：需要 JSON.stringify(data) + Content-Type
 */
export const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    const method = (options.method || 'GET').toUpperCase()
    const header = { 'Authorization': token || '' }

    // 组装请求参数
    const reqOpts = {
      url: BASE_URL + options.url,
      method: method,
      header: header
    }

    if (options.data) {
      if (method === 'GET') {
        // GET: 手动拼接 query string（uni-app 有些环境不会自动转）
        const qs = Object.entries(options.data)
          .filter(([_, v]) => v != null)
          .map(([k, v]) => encodeURIComponent(k) + '=' + encodeURIComponent(v))
          .join('&')
        if (qs) reqOpts.url += (reqOpts.url.includes('?') ? '&' : '?') + qs
      } else {
        // POST/PUT: 手动 JSON.stringify，uni-app 底层默认不会自动序列化
        header['Content-Type'] = 'application/json;charset=utf-8'
        reqOpts.data = JSON.stringify(options.data)
        reqOpts.header = header
      }
    }

    uni.request({
      ...reqOpts,
      success: (res) => {
        // HTTP 401 — token过期，统一跳转登录
        if (res.statusCode === 401) {
          console.warn('[Token过期]', options.url)
          uni.removeStorageSync('token')
          uni.removeStorageSync('userInfo')
          uni.showModal({
            title: '登录已过期',
            content: '请重新登录',
            showCancel: false,
            success: () => {
              uni.reLaunch({ url: '/pages/student/login' })
            }
          })
          reject(res.data || { code: 401, message: '登录已过期' })
          return
        }

        if (res.data.code === 200) {
          resolve(res.data)
        } else {
          console.warn('[API]', options.url, res.data)
          reject(res.data)
        }
      },
      fail: (err) => {
        console.error('[API网络错误]', options.url, err)
        reject(err)
      }
    })
  })
}

/* ======================== 认证模块 ======================== */
export const authAPI = {
  login: (data) => request({ url: '/auth/login', method: 'POST', data }),
  register: (data) => request({ url: '/auth/register', method: 'POST', data }),
  getUserInfo: () => request({ url: '/auth/userinfo' }),
  updatePassword: (data) => request({ url: '/auth/update-password', method: 'PUT', data })
}

/* ======================== 岗位模块 ======================== */
export const jobAPI = {
  getRecommendJobs: (params) => request({ url: '/jobs/recommend', data: params }),
  getJobDetail: (id) => request({ url: '/jobs/' + id }),
  getJobs: (params) => request({ url: '/jobs', data: params }),
  searchJobs: (keyword) => request({ url: '/jobs/search', data: { keyword } }),
  getActiveJobs: () => request({ url: '/jobs/active' })
}

/* ======================== 投递模块 ======================== */
export const deliveryAPI = {
  createDelivery: (data) => {
    const { studentId, ...body } = data
    const qs = studentId ? '?studentId=' + studentId : ''
    return request({ url: '/deliveries/deliver' + qs, method: 'POST', data: { ...body, resumeVersion: 'latest' } })
  },
  getDeliveriesByStudentId: (params) => {
    if (params && params.studentId) {
      return request({ url: '/deliveries/by-student/' + params.studentId })
    }
    return request({ url: '/deliveries', data: params })
  },
  cancelDelivery: (id) => request({ url: '/deliveries/' + id, method: 'DELETE' }),
  getDeliveryDetail: (id) => request({ url: '/deliveries/' + id })
}

/* ======================== 简历模块 ======================== */
export const resumeAPI = {
  getResume: () => {
    const sid = getStudentId() || 0
    return request({ url: '/resumes/my', data: { studentId: sid } })
  },
  createOrUpdateResume: (studentId, data) => request({ url: '/resumes?studentId=' + studentId, method: 'POST', data }),
  setDefault: (resumeId, studentId) => request({ url: '/resumes/' + resumeId + '/set-default', method: 'PUT', data: { studentId } }),
  deleteResume: (resumeId) => request({ url: '/resumes/' + resumeId, method: 'DELETE' }),
  getResumeById: (id) => request({ url: '/resumes/' + id }),
  uploadResumeFile: (filePath) => {
    return new Promise((resolve, reject) => {
      const token = uni.getStorageSync('token')
      uni.uploadFile({
        url: BASE_URL + '/resumes/upload-pdf',
        filePath: filePath,
        name: 'file',
        formData: { studentId: uni.getStorageSync('userInfo') ? JSON.parse(uni.getStorageSync('userInfo')).id || 0 : 0 },
        header: { 'Authorization': token },
        success: (res) => {
          try {
            const data = JSON.parse(res.data)
            if (data.code === 200) resolve(data)
            else reject(data)
          } catch (e) { reject(e) }
        },
        fail: reject
      })
    })
  }
}

/* ======================== 消息模块 ======================== */
export const messageAPI = {
  getMessages: (params) => request({ url: '/messages', data: params }),
  readMessage: (id) => request({ url: '/messages/' + id + '/read', method: 'PUT' })
}

/* ======================== 收藏模块 ======================== */
export const favoriteAPI = {
  getFavorites: (params) => request({ url: '/favorites', data: params }),
  addFavorite: (data) => request({ url: '/favorites', method: 'POST', data }),
  removeFavorite: (jobId, studentId) => {
    const qs = studentId ? '?studentId=' + studentId : ''
    return request({ url: '/favorites/' + jobId + qs, method: 'DELETE' })
  }
}

/* ======================== 统计模块 ======================== */
export const statisticsAPI = {
  getStudentOverview: (studentId) => request({ url: '/statistics/student/overview' })
}

/* ======================== 教师端模块 ======================== */
export const teacherAPI = {
  getDashboard: () => request({ url: '/statistics/teacher/dashboard' }),
  getDeliveryTrend: (userId) => request({ url: '/statistics/delivery-trend', data: { userId } }),
  getClasses: () => request({ url: '/classes' }),
  createClass: (data) => request({ url: '/classes', method: 'POST', data }),
  deleteClass: (id) => request({ url: '/classes/' + id, method: 'DELETE' }),
  getStudentsByClass: (classId) => request({ url: '/classes/' + classId + '/students' }),
  getStudentResume: (studentId) => request({ url: '/resumes/student/' + studentId }),
  getStudentInfo: (studentId) => request({ url: '/resumes/student/' + studentId + '/info' }),
  getStudentDeliveries: (studentId) => request({ url: '/deliveries/by-student/' + studentId }),
  getTeacherJobs: (createdBy) => request({ url: '/jobs/by-creator/' + createdBy }),
  createJob: (data) => request({ url: '/jobs', method: 'POST', data }),
  updateJob: (id, data) => request({ url: '/jobs/' + id, method: 'PUT', data }),
  publishJob: (id) => request({ url: '/jobs/' + id + '/publish', method: 'PUT' }),
  closeJob: (id) => request({ url: '/jobs/' + id + '/close', method: 'PUT' }),
  getCompanyName: (id) => request({ url: '/companies/' + id }),
  getCompanies: (params) => request({ url: '/companies', data: params }),
  getAllDeliveries: (jobId) => request({ url: '/deliveries/by-job/' + jobId }),
  getApprovals: (status) => request({ url: '/job-changes', data: { status } }),
  approveJobChange: (id) => request({ url: '/job-changes/' + id + '/approve', method: 'PUT' }),
  rejectJobChange: (id) => request({ url: '/job-changes/' + id + '/reject', method: 'PUT' }),
}

/* ======================== 企业端（HR）模块 ======================== */
export const hrAPI = {
  getDashboard: (companyId) => request({ url: '/statistics/hr/dashboard' }),
  getDeliveryTrend: (userId) => request({ url: '/statistics/delivery-trend', data: { userId } }),
  getHrJobs: (companyId) => request({ url: '/jobs/by-company/' + companyId }),
  createJob: (data) => request({ url: '/jobs', method: 'POST', data }),
  updateJob: (id, data) => request({ url: '/jobs/' + id, method: 'PUT', data }),
  publishJob: (id) => request({ url: '/jobs/' + id + '/publish', method: 'PUT' }),
  closeJob: (id) => request({ url: '/jobs/' + id + '/close', method: 'PUT' }),
  deleteJob: (id) => request({ url: '/jobs/' + id, method: 'DELETE' }),
  getCompanyDeliveries: (jobId) => request({ url: '/deliveries/by-job/' + jobId }),
  updateDeliveryStatus: (id, data) => request({ url: '/deliveries/' + id + '/status', method: 'PUT', data }),
  getCompanyProfile: (id) => request({ url: '/companies/' + id }),
  updateCompany: (id, data) => request({ url: '/companies/' + id, method: 'PUT', data }),
  getCompanyName: (companyId) => request({ url: '/companies/' + companyId }),
  getJobStats: (jobId) => request({ url: '/jobs/' + jobId + '/statistics' }),
}

/* ======================== AI人岗匹配模块 ======================== */
export const matchAPI = {
  getByStudent: (studentId) => request({ url: '/job-matches/by-student/' + studentId }),
  getByJob: (jobId) => request({ url: '/job-matches/by-job/' + jobId }),
  generate: (jobId, studentId) => request({ url: '/job-matches/generate', method: 'POST', data: { jobId, studentId } }),
  batchGenerate: (jobId) => request({ url: '/job-matches/batch-generate/' + jobId, method: 'POST' }),
  getPushed: (jobId) => request({ url: '/job-matches/pushed/by-job/' + jobId }),
  push: (id) => request({ url: '/job-matches/' + id + '/push', method: 'PUT' }),
  click: (id) => request({ url: '/job-matches/' + id + '/click', method: 'PUT' }),
  getPushRate: (jobId) => request({ url: '/job-matches/statistics/push-rate/' + jobId }),
  getClickRate: (jobId) => request({ url: '/job-matches/statistics/click-rate/' + jobId }),
  getAvgScore: (jobId) => request({ url: '/job-matches/statistics/average-match-score/' + jobId }),
  delete: (id) => request({ url: '/job-matches/' + id, method: 'DELETE' }),
}

/* ======================== 简历评分模块 ======================== */
export const scoreAPI = {
  getByDelivery: (deliveryId) => request({ url: '/resume-scores/by-delivery/' + deliveryId }),
  getByJob: (jobId) => request({ url: '/resume-scores/by-job/' + jobId }),
  score: (jobId, deliveryId) => request({ url: '/resume-scores/score', method: 'POST', data: { jobId, deliveryId } }),
  batchScore: (jobId) => request({ url: '/resume-scores/batch-score/' + jobId, method: 'POST' }),
  rescore: (scoreId) => request({ url: '/resume-scores/' + scoreId + '/rescore', method: 'PUT' }),
  getAvgScore: (jobId) => request({ url: '/resume-scores/statistics/average-score/' + jobId }),
  getDistribution: (jobId) => request({ url: '/resume-scores/statistics/score-distribution/' + jobId }),
  getTopScore: (jobId) => request({ url: '/resume-scores/top-score/' + jobId }),
  delete: (id) => request({ url: '/resume-scores/' + id, method: 'DELETE' }),
}

/* ======================== AI解析模块 ======================== */
export const aiParseAPI = {
  parse: (data) => request({ url: '/ai-parse/parse', method: 'POST', data }),
  getLogs: (teacherId) => request({ url: '/ai-parse/logs/by-teacher/' + teacherId }),
  getLogById: (id) => request({ url: '/ai-parse/logs/' + id }),
  getUncorrected: () => request({ url: '/ai-parse/logs/uncorrected' }),
  correct: (id, correctedResult) => request({ url: '/ai-parse/logs/' + id + '/correct', method: 'PUT', data: { correctedResult } }),
  countByTeacher: (teacherId) => request({ url: '/ai-parse/statistics/count-by-teacher/' + teacherId }),
}

/* ======================== 字段映射工具 ======================== */
export const mapJobData = (raw) => {
  if (!raw) return {}
  return {
    ...raw,
    salaryText: raw.salaryText || raw.salaryRange || '',
    requirements: raw.requirements || raw.requirement || '',
    companyName: raw.companyName || '',
    companyDesc: raw.companyDesc || '',
    experience: raw.experience || '',
    jobType: raw.jobType || ''
  }
}
