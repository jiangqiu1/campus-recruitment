import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import Cookies from 'js-cookie'

export const useAppStore = defineStore('app', () => {
  // ==================== 状态 ====================
  const sidebarCollapsed = ref(false)
  const theme = ref(Cookies.get('theme') || 'light')
  const language = ref(Cookies.get('language') || 'zh-CN')
  const loading = ref(false)
  const keepAlive = ref([])
  
  // 面包屑导航
  const breadcrumb = ref([])
  
  // 标签栏（访问过的页面）
  const visitedViews = ref(JSON.parse(localStorage.getItem('visitedViews') || '[]'))

  // ==================== 计算属性 ====================
  const isDark = computed(() => theme.value === 'dark')
  const sidebarWidth = computed(() => sidebarCollapsed.value ? 64 : 210)

  // ==================== 方法 ====================

  /**
   * 切换侧边栏
   */
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
    localStorage.setItem('sidebarCollapsed', sidebarCollapsed.value)
  }

  /**
   * 设置侧边栏状态
   */
  function setSidebar(collapsed) {
    sidebarCollapsed.value = collapsed
    localStorage.setItem('sidebarCollapsed', collapsed)
  }

  /**
   * 切换主题
   */
  function toggleTheme() {
    const newTheme = theme.value === 'light' ? 'dark' : 'light'
    setTheme(newTheme)
  }

  /**
   * 设置主题
   */
  function setTheme(newTheme) {
    theme.value = newTheme
    Cookies.set('theme', newTheme, { expires: 365 })
    document.documentElement.setAttribute('data-theme', newTheme)
    
    // 切换 Element Plus 主题
    if (newTheme === 'dark') {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
  }

  /**
   * 设置语言
   */
  function setLanguage(lang) {
    language.value = lang
    Cookies.set('language', lang, { expires: 365 })
  }

  /**
   * 设置全局加载状态
   */
  function setLoading(status) {
    loading.value = status
  }

  /**
   * 添加缓存组件
   */
  function addKeepAlive(componentName) {
    if (!keepAlive.value.includes(componentName)) {
      keepAlive.value.push(componentName)
    }
  }

  /**
   * 移除缓存组件
   */
  function removeKeepAlive(componentName) {
    const index = keepAlive.value.indexOf(componentName)
    if (index > -1) {
      keepAlive.value.splice(index, 1)
    }
  }

  /**
   * 设置面包屑
   */
  function setBreadcrumb(breadcrumbList) {
    breadcrumb.value = breadcrumbList
  }

  /**
   * 添加访问过的页面
   */
  function addVisitedView(view) {
    // 避免重复添加
    const existView = visitedViews.value.find(v => v.path === view.path)
    if (existView) {
      // 更新标题
      Object.assign(existView, view)
      return
    }
    
    visitedViews.value.push(view)
    saveVisitedViews()
  }

  /**
   * 删除访问过的页面
   */
  function delVisitedView(path) {
    const index = visitedViews.value.findIndex(v => v.path === path)
    if (index > -1) {
      visitedViews.value.splice(index, 1)
      saveVisitedViews()
    }
  }

  /**
   * 删除其他访问过的页面
   */
  function delOtherVisitedViews(path) {
    visitedViews.value = visitedViews.value.filter(v => {
      return v.path === path || v.meta?.affix
    })
    saveVisitedViews()
  }

  /**
   * 删除所有访问过的页面
   */
  function delAllVisitedViews() {
    visitedViews.value = visitedViews.value.filter(v => v.meta?.affix)
    saveVisitedViews()
  }

  /**
   * 保存访问过的页面到本地
   */
  function saveVisitedViews() {
    localStorage.setItem('visitedViews', JSON.stringify(visitedViews.value))
  }

  // ==================== 初始化 ====================
  
  // 从本地存储恢复侧边栏状态
  const savedSidebar = localStorage.getItem('sidebarCollapsed')
  if (savedSidebar !== null) {
    sidebarCollapsed.value = savedSidebar === 'true'
  }

  // 初始化主题
  setTheme(theme.value)

  return {
    // 状态
    sidebarCollapsed,
    theme,
    language,
    loading,
    keepAlive,
    breadcrumb,
    visitedViews,
    
    // 计算属性
    isDark,
    sidebarWidth,
    
    // 方法
    toggleSidebar,
    setSidebar,
    toggleTheme,
    setTheme,
    setLanguage,
    setLoading,
    addKeepAlive,
    removeKeepAlive,
    setBreadcrumb,
    addVisitedView,
    delVisitedView,
    delOtherVisitedViews,
    delAllVisitedViews
  }
})
