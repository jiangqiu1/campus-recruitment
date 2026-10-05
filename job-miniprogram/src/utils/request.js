// 优先读取自定义API地址（存储中修改），其次环境变量，默认localhost
import { formatSalary, formatTimeSemantic } from './format'

const getBaseUrl = () => {
  try {
    const custom = uni.getStorageSync('api_base_url')
    if (custom) return custom
  } catch (e) { /* ignore */ }
  return 'http://localhost:8080/api'
}

export const BASE_URL = getBaseUrl()

export const getStudentId = () => {
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

        // HTTP 403 — 无权限，统一提示
        if (res.statusCode === 403) {
          console.warn('[无权限]', options.url, res.data)
          uni.showToast({ title: '无权限访问', icon: 'none' })
          reject(res.data || { code: 403, message: '无权限访问' })
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
  updatePassword: (data) => request({ url: '/auth/update-password', method: 'PUT', data }),
  updateProfile: (data) => request({ url: '/auth/profile', method: 'PUT', data })
}

/* ======================== 岗位模块 ======================== */
export const jobAPI = {
  getRecommendJobs: (params) => request({ url: '/jobs/recommend', data: params }),
  getJobDetail: (id) => request({ url: '/jobs/' + id }),
  getJobs: (params) => request({ url: '/jobs', data: params }),
	searchJobs: (params) => request({ url: '/jobs/search', data: params }),
  getActiveJobs: () => request({ url: '/jobs/active' })
}

/* ======================== 投递模块 ======================== */
export const deliveryAPI = {
  createDelivery: (data) => {
    // studentId 由后端从JWT解析，前端不再手动传参
    return request({ url: '/deliveries/deliver', method: 'POST', data: { ...data, resumeVersion: 'latest' } })
  },
  getDeliveriesByStudentId: (params) => {
    if (params && params.studentId) {
      return request({ url: '/deliveries/by-student/' + params.studentId })
    }
    // 不传 studentId 时，后端从 JWT 自动取
    return request({ url: '/deliveries' })
  },
  cancelDelivery: (id) => request({ url: '/deliveries/' + id, method: 'DELETE' }),
  getDeliveryDetail: (id) => request({ url: '/deliveries/' + id })
}

/* ======================== 简历模块 ======================== */
export const resumeAPI = {
  getResume: () => {
    // studentId 由后端从JWT解析
    return request({ url: '/resumes/my' })
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
        formData: {},  // studentId由后端从JWT解析
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
  readMessage: (id) => request({ url: '/messages/' + id + '/read', method: 'PUT' }),
  getUnreadCount: (studentId) => request({ url: '/messages/unread-count', data: { studentId } })
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
  getStudentOverview: () => request({ url: '/statistics/student/overview' })
}

/* ======================== 班级模块 ======================== */
export const classAPI = {
  getMyClass: () => request({ url: '/classes/student/my-class' })
}

/* ======================== 数据字典模块 ======================== */
export const dictAPI = {
  getSchools: (keyword) => request({ url: '/dict/schools', data: { keyword } })
}

/* ======================== 教师端模块 ======================== */
export const teacherAPI = {
  getDashboard: () => request({ url: '/statistics/teacher/dashboard' }),
  getAttentionStudents: () => request({ url: '/statistics/teacher/attention-students' }),
  getDeliveryTrend: (userId) => request({ url: '/statistics/delivery-trend', data: { userId } }),
  getClasses: () => request({ url: '/classes' }),
  createClass: (data) => request({ url: '/classes', method: 'POST', data }),
  deleteClass: (id) => request({ url: '/classes/' + id, method: 'DELETE' }),
  getStudentsByClass: (classId) => request({ url: '/classes/' + classId + '/students' }),
  getAllStudents: () => request({ url: '/classes/students' }),
  getStudentResume: (studentId) => request({ url: '/resumes/student/' + studentId }),
  getStudentInfo: (studentId) => request({ url: '/resumes/student/' + studentId + '/info' }),
  getStudentDeliveries: (studentId) => request({ url: '/deliveries/by-student/' + studentId }),
  updateDeliveryStatus: (id, data) => request({ url: '/deliveries/' + id + '/status', method: 'PUT', data }),
  getTeacherJobs: () => request({ url: '/jobs/teacher' }),
  createJob: (data) => request({ url: '/jobs', method: 'POST', data }),
  updateJob: (id, data) => request({ url: '/jobs/' + id, method: 'PUT', data }),
  publishJob: (id) => request({ url: '/jobs/' + id + '/publish', method: 'PUT' }),
  closeJob: (id) => request({ url: '/jobs/' + id + '/close', method: 'PUT' }),
  getCompanyName: (id) => request({ url: '/companies/' + id }),
  getCompanies: (params) => request({ url: '/companies', data: params }),
  createCompany: (data) => request({ url: '/companies', method: 'POST', data }),
  getAllDeliveries: (jobId) => request({ url: '/deliveries/by-job/' + jobId }),
  getApprovals: (status) => request({ url: '/job-changes', data: { status } }),
  approveJobChange: (id) => request({ url: '/job-changes/' + id + '/approve', method: 'PUT' }),
  rejectJobChange: (id, reason) => request({ url: '/job-changes/' + id + '/reject', method: 'PUT', data: { reason } }),
}

/* ======================== 企业端（HR）模块 ======================== */
export const hrAPI = {
  getDashboard: (companyId) => request({ url: '/statistics/hr/dashboard', data: { companyId } }),
  getDeliveryTrend: (userId) => request({ url: '/statistics/delivery-trend', data: { userId } }),
  getHrJobs: (companyId) => request({ url: '/jobs/by-company/' + companyId }),
  createJob: (data) => request({ url: '/jobs', method: 'POST', data }),
  updateJob: (id, data) => request({ url: '/jobs/' + id, method: 'PUT', data }),
  publishJob: (id) => request({ url: '/jobs/' + id + '/publish', method: 'PUT' }),
  closeJob: (id) => request({ url: '/jobs/' + id + '/close', method: 'PUT' }),
  deleteJob: (id) => request({ url: '/jobs/' + id, method: 'DELETE' }),
  getCompanyDeliveries: (jobId) => request({ url: '/deliveries/by-job/' + jobId }),
  getDeliveriesByCompany: (companyId) => request({ url: '/deliveries/by-company/' + companyId }),
  updateDeliveryStatus: (id, data) => request({ url: '/deliveries/' + id + '/status', method: 'PUT', data }),
  arrangeInterview: (id, data) => request({ url: '/deliveries/' + id + '/arrange-interview', method: 'PUT', data }),
  getCompanyProfile: (id) => request({ url: '/companies/' + id }),
  updateCompany: (id, data) => request({ url: '/companies/' + id, method: 'PUT', data }),
  getCompanyName: (companyId) => request({ url: '/companies/' + companyId }),
  getJobStats: (jobId) => request({ url: '/jobs/' + jobId + '/statistics' }),
  getRangeStats: (days) => request({ url: '/statistics/hr/range-stats', data: { days } }),
}

/* ======================== AI人岗匹配模块 ======================== */
export const matchAPI = {
  getByStudent: (studentId) => request({ url: '/job-matches/by-student/' + studentId }),
  getByJob: (jobId) => request({ url: '/job-matches/by-job/' + jobId }),
  generate: (jobId, studentId) => request({ url: '/job-matches/generate', method: 'POST', data: { jobId, studentId } }),
  batchGenerate: (jobId, classId) => request({ url: '/job-matches/batch-generate/' + jobId + (classId ? '?classId=' + classId : ''), method: 'POST' }),
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
  parseJob: (rawMessage) => request({ url: '/ai-parse/parse-job', method: 'POST', data: { rawMessage } }),
  analyzeResume: (studentId) => request({ url: '/ai-parse/analyze-resume', method: 'POST', data: { studentId } }),
}

/* ======================== AI助手模块（学生向：简历诊断 / 模拟面试） ======================== */
export const aiAssistantAPI = {
  // 诊断当前登录学生自己的简历；简历未变更时后端返回缓存结果，force=true 强制重新分析
  resumeReview: (force) => request({ url: '/ai-assistant/resume-review', method: 'POST', data: { force: !!force } }),
  genInterviewQuestions: (jobId) => request({ url: '/ai-assistant/interview/questions', method: 'POST', data: { jobId } }),
  evaluateAnswer: (jobId, question, answer) => request({ url: '/ai-assistant/interview/evaluate', method: 'POST', data: { jobId, question, answer } }),
}

/* ======================== 字段映射工具 ======================== */
export const mapJobData = (raw) => {
	if (!raw) return {}
	return {
		...raw,
		salaryText: formatSalary(raw.salaryText || raw.salaryRange || ''),
		requirements: raw.requirements || raw.requirement || '',
		companyName: raw.companyName || '',
		companyDesc: raw.companyDesc || '',
		experience: raw.experience || '',
		jobType: raw.jobType || ''
	}
}

/* ======================== 教师端通用数据映射 ======================== */
// 投递状态映射（色值对齐 uni.scss 设计 token，见 docs/frontend/08-小程序UI设计规范.md）
// light = 同色系浅底，供状态标签使用
export const DELIVERY_STATUS = {
	PENDING: { value: 0, label: '待查看', color: '#FF7D00', light: 'rgba(255,125,0,0.08)', class: 'pending' },
	VIEWED: { value: 1, label: '已查看', color: '#165DFF', light: 'rgba(22,93,255,0.08)', class: 'viewed' },
	INTERVIEW: { value: 2, label: '面试中', color: '#165DFF', light: 'rgba(22,93,255,0.08)', class: 'interview' },
	ACCEPTED: { value: 3, label: '已录用', color: '#00B42A', light: 'rgba(0,180,42,0.08)', class: 'accepted' },
	REJECTED: { value: 4, label: '不合适', color: '#F53F3F', light: 'rgba(245,63,63,0.08)', class: 'rejected' }
}

// 岗位状态映射
export const JOB_STATUS = {
	DRAFT: { value: 0, label: '草稿', color: '#FF7D00', class: 'draft' },
	ACTIVE: { value: 1, label: '招聘中', color: '#165DFF', class: 'active' },
	CLOSED: { value: 2, label: '已关闭', color: '#86909C', class: 'closed' },
	PAUSED: { value: 3, label: '已暂停', color: '#86909C', class: 'paused' }
}

// 投递数据统一格式化
export function mapDeliveryItem(d) {
	const statusKey = Object.keys(DELIVERY_STATUS).find(k => DELIVERY_STATUS[k].value === d.status) || 'PENDING'
	const status = DELIVERY_STATUS[statusKey]
	return {
		id: d.id,
		studentId: d.studentId,
		studentName: d.studentName || '未知学生',
		className: d.className || '',
		jobId: d.jobId,
		jobTitle: d.jobTitle || '未知岗位',
		status: status.class,
		statusText: status.label,
		statusColor: status.color,
		createTime: d.createTime ? d.createTime.substring(0, 10) : '',
		createTimeText: d.createTime ? formatTimeSemantic(d.createTime) : ''
	}
}

// 教师端岗位数据统一格式化
export function mapTeacherJob(job) {
	const statusKey = Object.keys(JOB_STATUS).find(k => JOB_STATUS[k].value === job.status) || 'DRAFT'
	const status = JOB_STATUS[statusKey]
	return {
		id: job.id,
		title: job.title,
		companyId: job.companyId,
		companyName: job.companyName || '待设置',
		location: job.location || '未设置',
		salaryText: formatSalary(job.salaryRange) || '薪资面议',
		status: status.class,
		statusText: status.label,
		deliveryCount: job.deliveryCount || 0
	}
}
