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
import JobOversight from '../views/pc/admin/JobOversight.vue'
import AIStats from '../views/pc/admin/AIStats.vue'

// 共享页面（所有角色通用）
import Profile from '../views/pc/Profile.vue'
import Password from '../views/pc/Password.vue'

// 教师端页面（PC 后台）
import TeacherDashboard from '../views/pc/teacher/Dashboard.vue'
import ClassManagement from '../views/pc/teacher/ClassManagement.vue'
import JobPosting from '../views/pc/teacher/JobPosting.vue'
import DeliveryBoard from '../views/pc/teacher/DeliveryBoard.vue'
import ResumeManage from '../views/pc/teacher/ResumeManage.vue'
import AIMatch from '../views/pc/teacher/AIMatch.vue'
import AIParse from '../views/pc/teacher/AIParse.vue'
import CompanyDetail from '../views/pc/teacher/CompanyDetail.vue'
import Approvals from '../views/pc/teacher/Approvals.vue'

// HR端页面（PC 后台）
import HRBatchResume from '../views/pc/hr/BatchResume.vue'
import HRDataStats from '../views/pc/hr/DataStats.vue'
import HRJobAnalysis from '../views/pc/hr/JobAnalysis.vue'
import HRAccountManagement from '../views/pc/hr/AccountManagement.vue'
import HRJobManage from '../views/pc/hr/JobManage.vue'
import HRCandidateManage from '../views/pc/hr/CandidateManage.vue'
import HRInterviewManage from '../views/pc/hr/InterviewManage.vue'
import HRCompanyProfile from '../views/pc/hr/CompanyProfile.vue'

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
      },
      {
        path: 'jobs',
        name: 'JobOversight',
        component: JobOversight,
        meta: { title: '岗位监管', role: 'admin' }
      },
      {
        path: 'ai-stats',
        name: 'AIStats',
        component: AIStats,
        meta: { title: 'AI使用统计', role: 'admin' }
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: Profile,
        meta: { title: '个人信息', role: 'admin' }
      },
      {
        path: 'password',
        name: 'AdminPassword',
        component: Password,
        meta: { title: '修改密码', role: 'admin' }
      },
      {
        path: 'messages',
        name: 'AdminMessages',
        component: () => import('@/views/pc/admin/MessageList.vue'),
        meta: { title: '消息通知', role: 'admin' }
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
      },
      {
        path: 'resumes',
        name: 'ResumeManage',
        component: ResumeManage,
        meta: { title: '简历管理', role: 'teacher' }
      },
      {
        path: 'ai-match',
        name: 'AIMatch',
        component: AIMatch,
        meta: { title: 'AI人岗匹配', role: 'teacher' }
      },
      {
        path: 'ai-parse',
        name: 'AIParse',
        component: AIParse,
        meta: { title: 'AI简历分析', role: 'teacher' }
      },
      {
        path: 'companies',
        name: 'TeacherCompanyDetail',
        component: CompanyDetail,
        meta: { title: '企业详情', role: 'teacher' }
      },
      {
        path: 'approvals',
        name: 'Approvals',
        component: Approvals,
        meta: { title: '审批管理', role: 'teacher' }
      },
      {
        path: 'profile',
        name: 'TeacherProfile',
        component: Profile,
        meta: { title: '个人信息', role: 'teacher' }
      },
      {
        path: 'password',
        name: 'TeacherPassword',
        component: Password,
        meta: { title: '修改密码', role: 'teacher' }
      },
      {
        path: 'messages',
        name: 'TeacherMessages',
        component: () => import('@/views/pc/admin/MessageList.vue'),
        meta: { title: '消息通知', role: 'teacher' }
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
      },
      {
        path: 'jobs',
        name: 'HRJobManage',
        component: HRJobManage,
        meta: { title: '岗位管理', role: 'hr' }
      },
      {
        path: 'candidates',
        name: 'HRCandidateManage',
        component: HRCandidateManage,
        meta: { title: '候选人管理', role: 'hr' }
      },
      {
        path: 'interviews',
        name: 'HRInterviewManage',
        component: HRInterviewManage,
        meta: { title: '面试管理', role: 'hr' }
      },
      {
        path: 'company',
        name: 'HRCompanyProfile',
        component: HRCompanyProfile,
        meta: { title: '企业信息', role: 'hr' }
      },
      {
        path: 'profile',
        name: 'HRProfile',
        component: Profile,
        meta: { title: '个人信息', role: 'hr' }
      },
      {
        path: 'password',
        name: 'HRPassword',
        component: Password,
        meta: { title: '修改密码', role: 'hr' }
      },
      {
        path: 'messages',
        name: 'HRMessages',
        component: () => import('@/views/pc/admin/MessageList.vue'),
        meta: { title: '消息通知', role: 'hr' }
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
