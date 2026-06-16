import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authAPI, userAPI } from '@/api'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', () => {
  // ==================== 状态 ====================
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const roles = ref(JSON.parse(localStorage.getItem('roles') || '[]'))
  const permissions = ref(JSON.parse(localStorage.getItem('permissions') || '[]'))

  // ==================== 计算属性 ====================
  const isLoggedIn = computed(() => !!token.value)
  const userId = computed(() => userInfo.value?.userId || null)
  const companyId = computed(() => userInfo.value?.companyId || null)
  const username = computed(() => userInfo.value?.username || '')
  const realName = computed(() => userInfo.value?.realName || '')
  const userRole = computed(() => userInfo.value?.role || '')
  const isAdmin = computed(() => userRole.value === 'admin')
  const isTeacher = computed(() => userRole.value === 'teacher')
  const isHR = computed(() => userRole.value === 'hr')
  const isStudent = computed(() => userRole.value === 'student')

  // ==================== 方法 ====================

  /**
   * 用户登录
   */
  async function login(username, password, role) {
    try {
      const params = { username, password }
      if (role !== undefined && role !== null) {
        params.role = role
      }
      const res = await authAPI.login(params)

      if (res.code === 200) {
        const data = res.data
        const newToken = data.token.startsWith('Bearer ') ? data.token.slice(7) : data.token
        const info = {
          userId: data.userId,
          username: data.username,
          realName: data.realName,
          role: data.role,
          avatarUrl: data.avatarUrl,
          companyId: data.companyId
        }

        // 保存token和用户信息
        setToken(newToken)
        setUserInfo(info)

        ElMessage.success('登录成功')
        return { success: true, data }
      } else {
        ElMessage.error(res.message || '登录失败')
        return { success: false, message: res.message }
      }
    } catch (error) {
      const message = error.response?.data?.message || '登录失败,请稍后重试'
      ElMessage.error(message)
      return { success: false, message }
    }
  }

  /**
   * 用户登出
   */
  async function logout() {
    try {
      if (token.value) {
        await authAPI.logout()
      }
    } catch (error) {
      console.error('登出接口调用失败:', error)
    } finally {
      clearToken()
      clearUserInfo()
      ElMessage.success('已登出')
      window.location.href = '/login'
    }
  }

  /**
   * 获取用户信息
   */
  async function getUserInfo() {
    try {
      const res = await userAPI.getUserInfo()
      if (res.code === 200) {
        const info = res.data
        setUserInfo(info)
        return { success: true, data: info }
      }
      return { success: false }
    } catch (error) {
      console.error('获取用户信息失败:', error)
      return { success: false }
    }
  }

  /**
   * 更新用户信息
   */
  async function updateUserInfo(data) {
    try {
      const res = await userAPI.updateUserInfo(data)
      if (res.code === 200) {
        const updatedInfo = { ...userInfo.value, ...data }
        setUserInfo(updatedInfo)
        ElMessage.success('更新成功')
        return { success: true }
      } else {
        ElMessage.error(res.message || '更新失败')
        return { success: false }
      }
    } catch (error) {
      const message = error.response?.data?.message || '更新失败'
      ElMessage.error(message)
      return { success: false }
    }
  }

  /**
   * 修改密码
   */
  async function changePassword(oldPassword, newPassword) {
    try {
      const res = await authAPI.changePassword({
        oldPassword,
        newPassword
      })
      if (res.code === 200) {
        ElMessage.success('密码修改成功，请重新登录')
        await logout()
        return { success: true }
      } else {
        ElMessage.error(res.message || '修改失败')
        return { success: false }
      }
    } catch (error) {
      const message = error.response?.data?.message || '修改失败'
      ElMessage.error(message)
      return { success: false }
    }
  }

  function hasPermission(permission) {
    return permissions.value.includes(permission)
  }

  function hasRole(role) {
    return roles.value.includes(role) || userRole.value === role
  }

  async function checkToken() {
    if (!token.value) return false
    try {
      const res = await authAPI.checkToken()
      return res.code === 200
    } catch (error) {
      if (error.response?.status === 401) {
        clearToken()
        clearUserInfo()
      }
      return false
    }
  }

  // ==================== 工具方法 ====================
  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function clearToken() {
    token.value = ''
    localStorage.removeItem('token')
  }

  function setUserInfo(info) {
    userInfo.value = info
    roles.value = info.roles || []
    permissions.value = info.permissions || []
    localStorage.setItem('userInfo', JSON.stringify(info))
    localStorage.setItem('roles', JSON.stringify(roles.value))
    localStorage.setItem('permissions', JSON.stringify(permissions.value))
  }

  function clearUserInfo() {
    userInfo.value = null
    roles.value = []
    permissions.value = []
    localStorage.removeItem('userInfo')
    localStorage.removeItem('roles')
    localStorage.removeItem('permissions')
  }

  return {
    token, userInfo, roles, permissions,
    isLoggedIn, userId, companyId, username, realName, userRole,
    isAdmin, isTeacher, isHR, isStudent,
    login, logout, getUserInfo, updateUserInfo, changePassword,
    hasPermission, hasRole, checkToken
  }
})