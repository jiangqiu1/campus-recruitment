<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-left">
      <h1><i class="fa fa-graduation-cap" style="margin-right: 12px;"></i>招聘就业管理平台</h1>
      <p>全校就业数据一张图 · 校企协同数字化 · 高效就业管理</p>
    </div>
    
    <!-- 右侧登录表单 -->
    <div class="login-right">
      <div class="login-title">欢迎登录</div>
      <div class="login-sub">请选择身份并输入账号密码</div>
      
      <!-- 角色选择标签页（默认无高亮） -->
      <div class="login-tabs">
        <div 
          class="login-tab" 
          :class="{ active: loginForm.role === 'admin' }"
          @click="loginForm.role = 'admin'"
        >
          <i class="fa fa-user" style="margin-right: 6px;"></i>管理员
        </div>
        <div 
          class="login-tab" 
          :class="{ active: loginForm.role === 'teacher' }"
          @click="loginForm.role = 'teacher'"
        >
          <i class="fa fa-user-circle" style="margin-right: 6px;"></i>普通教师
        </div>
        <div 
          class="login-tab" 
          :class="{ active: loginForm.role === 'hr' }"
          @click="loginForm.role = 'hr'"
        >
          <i class="fa fa-building" style="margin-right: 6px;"></i>企业HR
        </div>
      </div>

      <!-- 登录表单 -->
      <el-form :model="loginForm" :rules="rules" ref="loginFormRef">
        <el-form-item prop="username">
          <el-input 
            v-model="loginForm.username" 
            placeholder="请输入工号 / 手机号"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        
        <el-button 
          type="primary" 
          size="large" 
          style="width: 100%; margin-top: 8px;"
          @click="handleLogin"
          :loading="loading"
        >
          <i class="fa fa-sign-in" style="margin-right: 6px;"></i>立即登录
        </el-button>
      </el-form>
      
      <div class="login-hint">
        管理员：admin / 123456 | 普通教师：T001 / 123456 | 企业HR：HR001 / 123456
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authAPI } from '@/api'

const router = useRouter()
const loginFormRef = ref()
const loading = ref(false)

// 角色映射：前端显示 -> 后端数字（0=学生,1=教师,2=HR,3=管理员）
const roleMap = {
  admin: 3,
  teacher: 1,
  hr: 2
}

const loginForm = reactive({
  username: '',
  password: '',
  role: ''        // 默认为空，无选中角色
})

// 校验规则：增加角色必选
const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择登录身份', trigger: 'change' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  // 手动校验角色是否已选（因为 role 字段没有绑定表单输入组件，需要单独检查）
  if (!loginForm.role) {
    ElMessage.warning('请先选择登录身份')
    return
  }
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const requestData = {
          username: loginForm.username,
          password: loginForm.password,
          role: roleMap[loginForm.role]
        }
        const res = await authAPI.login(requestData)
        if (res.code === 200) {
          const data = res.data
          const roleNameMap = { 0: 'student', 1: 'teacher', 2: 'hr', 3: 'admin' }
          const roleName = roleNameMap[data.role] || data.role
          // 剥离 "Bearer " 前缀再存储，避免 axios 拦截器重复添加
          const rawToken = data.token.startsWith('Bearer ') ? data.token.slice(7) : data.token
          localStorage.setItem('token', rawToken)
          localStorage.setItem('userRole', roleName)
          localStorage.setItem('userId', data.userId)
          localStorage.setItem('username', data.username)
          // 同步写入 userInfo，确保 Pinia userStore 初始化时能取到 userId
          localStorage.setItem('userInfo', JSON.stringify({
            userId: data.userId,
            username: data.username,
            realName: data.realName,
            role: roleName,
            avatarUrl: data.avatarUrl,
            companyId: data.companyId
          }))
          
          ElMessage.success('登录成功')
          
          if (data.role === 3) {
            router.push('/admin/dashboard')
          } else if (data.role === 1) {
            router.push('/teacher/dashboard')
          } else if (data.role === 2) {
            router.push('/hr/resumes')
          } else {
            router.push('/')
          }
        }
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
/* 样式完全保留，无改动 */
.login-page {
  position: fixed;
  inset: 0;
  display: flex;
  z-index: 2000;
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #0F172A, #165DFF);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: white;
  padding: 60px;
  position: relative;
  overflow: hidden;
}

.login-left::before {
  content: '';
  position: absolute;
  top: -20%;
  right: -10%;
  width: 500px;
  height: 500px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 50%;
}

.login-left::after {
  content: '';
  position: absolute;
  bottom: -15%;
  left: -5%;
  width: 400px;
  height: 400px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 50%;
}

.login-left h1 {
  font-size: 36px;
  margin-bottom: 16px;
  position: relative;
  z-index: 2;
}

.login-left p {
  font-size: 16px;
  opacity: 0.85;
  position: relative;
  z-index: 2;
}

.login-right {
  width: 480px;
  background: white;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 60px;
  box-shadow: -10px 0 30px rgba(0, 0, 0, 0.05);
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 8px;
}

.login-sub {
  color: #86909C;
  margin-bottom: 32px;
  font-size: 14px;
}

.login-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.login-tab {
  flex: 1;
  padding: 12px;
  text-align: center;
  border: 2px solid #E2E8F0;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.25s;
  font-size: 14px;
}

.login-tab.active {
  border-color: #165DFF;
  color: #165DFF;
  background: rgba(22, 93, 255, 0.05);
}

.login-hint {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: #86909C;
}
</style>