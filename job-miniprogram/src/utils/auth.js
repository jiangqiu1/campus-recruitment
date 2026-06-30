/**
 * 角色路由锁工具 — 每个页面 onLoad / setup 时调用
 * 检查 token 是否存在、角色是否匹配，不匹配则跳转登录
 */

/** 踢回登录页，清除缓存 */
function kickToLogin() {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
  uni.reLaunch({ url: '/pages/student/login' })
}

/**
 * 检查当前页面是否允许当前角色访问
 * @param {number|string} expectedRole - 期望的角色值 (0|1|2)
 * @returns {boolean} 是否允许通过
 */
export function checkRole(expectedRole) {
  const token = uni.getStorageSync('token')
  if (!token) {
    kickToLogin()
    return false
  }

  try {
    const raw = uni.getStorageSync('userInfo')
    if (!raw) {
      kickToLogin()
      return false
    }
    const user = JSON.parse(raw)
    const role = Number(user.role)
    if (role !== Number(expectedRole)) {
      console.warn(`[Auth] 角色不匹配: 期望=${expectedRole}, 实际=${role}`)
      // 跳转到正确角色的首页
      const homePages = ['/pages/student/home', '/pages/teacher/home', '/pages/hr/home']
      const target = homePages[role] || '/pages/student/login'
      uni.reLaunch({ url: target })
      return false
    }
    return true
  } catch (e) {
    kickToLogin()
    return false
  }
}

/**
 * 获取当前登录用户信息（安全解析）
 * @returns {object|null}
 */
export function getCurrentUser() {
  try {
    const raw = uni.getStorageSync('userInfo')
    if (!raw) return null
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
}
