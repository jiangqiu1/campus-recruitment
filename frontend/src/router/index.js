import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import Login from '../views/Login.vue'

// 布局组件
import AdminLayout from '../layouts/AdminLayout.vue'
import TeacherLayout from '../layouts/TeacherLayout.vue'
import HRLayout from '../layouts/HRLayout.vue'

// 管理员端页面（PC 后台）
import AdminDashboard from '../views/pc/admin/Dashboard.vue'
import UserManage from '../views/pc/admin/UserManage.vue'
import CompanyManage from '../views/pc/admin/CompanyManage.vue'
import ClassManage from '../views/pc/admin/ClassManage.vue'
import EnterpriseAudit from '../views/pc/admin/EnterpriseAudit.vue'
import OperationLog from '../views/pc/admin/OperationLog.vue'
import DataExport from '../views/pc/admin/DataExport.vue'
import SystemSettings from '../views/pc/admin/SystemSettings.vue'

// 教师端页面（PC 后台）
import TeacherDashboard from '../views/pc/teacher/Dashboard.vue'
import ClassManagement from '../views/pc/teacher/ClassManagement.vue'
import JobPosting from '../views/pc/teacher/JobPosting.vue'
import DeliveryBoard from '../views/pc/teacher/DeliveryBoard.vue'

// HR端页面（PC 后台）
import HRBatchResume from '../views/pc/hr/BatchResume.vue'
import HRDataStats from '../views/pc/hr/DataStats.vue'
import HRJobAnalysis from '../views/pc/hr/JobAnalysis.vue'
import HRAccountManagement from '../views/pc/hr/AccountManagement.vue'

const routes = [
  // 登录页
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  
  // 管理员后台（PC）
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: AdminDashboard,
        meta: { title: '数据大屏', role: 'admin' }
      },
      {
        path: 'users',
        name: 'UserManage',
        component: UserManage,
        meta: { title: '用户管理', role: 'admin' }
      },
      {
        path: 'companies',
        name: 'CompanyManage',
        component: CompanyManage,
        meta: { title: '企业管理', role: 'admin' }
      },
      {
        path: 'classes',
        name: 'ClassManage',
        component: ClassManage,
        meta: { title: '班级管理', role: 'admin' }
      },
      {
        path: 'audit',
        name: 'EnterpriseAudit',
        component: EnterpriseAudit,
        meta: { title: '企业审核', role: 'admin' }
      },
      {
        path: 'logs',
        name: 'OperationLog',
        component: OperationLog,
        meta: { title: '操作日志', role: 'admin' }
      },
      {
        path: 'export',
        name: 'DataExport',
        component: DataExport,
        meta: { title: '数据导出', role: 'admin' }
      },
      {
        path: 'settings',
        name: 'SystemSettings',
        component: SystemSettings,
        meta: { title: '系统设置', role: 'admin' }
      }
    ]
  },
  
  // 教师端后台（PC）
  {
    path: '/teacher',
    component: TeacherLayout,
    redirect: '/teacher/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'TeacherDashboard',
        component: TeacherDashboard,
        meta: { title: '工作台', role: 'teacher' }
      },
      {
        path: 'classes',
        name: 'ClassManagement',
        component: ClassManagement,
        meta: { title: '班级管理', role: 'teacher' }
      },
      {
        path: 'jobs',
        name: 'JobPosting',
        component: JobPosting,
        meta: { title: '岗位发布', role: 'teacher' }
      },
      {
        path: 'deliveries',
        name: 'DeliveryBoard',
        component: DeliveryBoard,
        meta: { title: '投递看板', role: 'teacher' }
      }
    ]
  },
  
  // HR端后台（PC）
  {
    path: '/hr',
    component: HRLayout,
    redirect: '/hr/resumes',
    children: [
      {
        path: 'resumes',
        name: 'HRBatchResume',
        component: HRBatchResume,
        meta: { title: '批量简历处理', role: 'hr' }
      },
      {
        path: 'stats',
        name: 'HRDataStats',
        component: HRDataStats,
        meta: { title: '数据统计', role: 'hr' }
      },
      {
        path: 'analysis',
        name: 'HRJobAnalysis',
        component: HRJobAnalysis,
        meta: { title: '岗位分析', role: 'hr' }
      },
      {
        path: 'accounts',
        name: 'HRAccountManagement',
        component: HRAccountManagement,
        meta: { title: '账号管理', role: 'hr' }
      }
    ]
  },
  
  // 默认重定向到登录页
  {
    path: '/',
    redirect: '/login'
  },
  
  // 404页面
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局路由守卫 - 权限控制
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userRole = localStorage.getItem('userRole')
  
  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title + ' - 招聘就业管理平台'
  }
  
  // 如果是登录页，已登录则跳转到对应后台
  if (to.path === '/login') {
    if (token) {
      if (userRole === 'admin') {
        next('/admin/dashboard')
      } else if (userRole === 'teacher') {
        next('/teacher/dashboard')
      } else if (userRole === 'hr') {
        next('/hr/resumes')
      } else {
        next()
      }
    } else {
      next()
    }
  } else {
    // 非登录页需要验证token
    if (!token) {
      next('/login')
    } else {
      // 检查角色权限
      if (to.meta.role && to.meta.role !== userRole) {
        ElMessage.error('无权访问该页面')
        next(false)
      } else {
        next()
      }
    }
  }
})

export default router
