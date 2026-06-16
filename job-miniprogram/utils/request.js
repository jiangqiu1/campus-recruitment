const BASE_URL = 'http://localhost:8080/api'

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
  cancelDelivery: (id) => request({ url: '/deliveries/' + id, method: 'DELETE' })
}

/* ======================== 简历模块 ======================== */
export const resumeAPI = {
  getResume: () => request({ url: '/resumes/my', data: { studentId: 0 } }),
  createOrUpdateResume: (studentId, data) => request({ url: '/resumes?studentId=' + studentId, method: 'POST', data }),
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
