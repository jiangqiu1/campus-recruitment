<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-left">
      <!-- 环境光晕 -->
      <div class="glow-orb glow-orb--1"></div>
      <div class="glow-orb glow-orb--2"></div>
      <div class="glow-orb glow-orb--3"></div>

      <!-- 背景动态节点网络 (鼠标视差容器) -->
      <div class="network-bg" ref="networkBg">
        <svg class="lines" viewBox="0 0 800 600" preserveAspectRatio="xMidYMid meet">
          <path
            d="M 100 150 Q 300 50 500 200 T 700 100"
            stroke="rgba(147, 197, 253, 0.35)"
            fill="none"
            stroke-width="2"
          />
          <path
            d="M 50 300 Q 250 400 450 250 T 750 350"
            stroke="rgba(96, 165, 250, 0.25)"
            fill="none"
            stroke-width="1.5"
          />
          <path
            d="M 150 450 Q 350 300 550 450 T 700 500"
            stroke="rgba(59, 130, 246, 0.25)"
            fill="none"
            stroke-width="1.5"
          />
          <circle cx="200" cy="120" r="4" fill="#60A5FA" opacity="0.8">
            <animate attributeName="r" values="4;8;4" dur="3s" repeatCount="indefinite" />
            <animate attributeName="opacity" values="0.8;0.2;0.8" dur="3s" repeatCount="indefinite" />
          </circle>
          <circle cx="500" cy="200" r="6" fill="#93C5FD" opacity="0.9">
            <animate attributeName="r" values="6;12;6" dur="4s" repeatCount="indefinite" />
            <animate attributeName="opacity" values="0.9;0.3;0.9" dur="4s" repeatCount="indefinite" />
          </circle>
          <circle cx="700" cy="100" r="3" fill="#3B82F6" opacity="0.7">
            <animate attributeName="r" values="3;7;3" dur="2.5s" repeatCount="indefinite" />
            <animate attributeName="opacity" values="0.7;0.1;0.7" dur="2.5s" repeatCount="indefinite" />
          </circle>
        </svg>
      </div>

      <!-- 品牌文案 -->
      <div class="brand-content">
        <h1>
          <span class="highlight">连接</span>校园与未来
        </h1>
        <p>全校就业数据一张图 · 校企协同数字化</p>
        <div class="stats">
          <span class="stats-item"><strong>98%</strong> 就业率</span>
          <span class="stats-item"><strong>500+</strong> 合作企业</span>
          <span class="stats-item"><strong>10k</strong> 应届生</span>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="login-right">
      <div class="login-header">
        <div class="login-title">欢迎回来</div>
        <div class="login-sub">请选择身份并输入账号密码登录</div>
      </div>

      <!-- 角色选择标签页（支持键盘左右键） -->
      <div class="login-tabs" :class="{ disabled: loading }" role="tablist">
        <div
          v-for="(item, index) in roleList"
          :key="item.key"
          class="login-tab"
          :class="{ active: loginForm.role === item.key }"
          @click="selectRole(item.key)"
          @keydown.left.prevent="switchRole(index, -1)"
          @keydown.right.prevent="switchRole(index, 1)"
          tabindex="0"
          role="tab"
          :aria-selected="loginForm.role === item.key"
        >
          <i :class="item.icon" />
          <span>{{ item.label }}</span>
        </div>
      </div>

      <!-- 登录表单 -->
      <el-form
        :model="loginForm"
        :rules="rules"
        ref="loginFormRef"
        class="login-form"
        size="large"
      >
        <!-- 表单锁定遮罩（加载时禁用） -->
        <div class="form-disabled-overlay" v-if="loading"></div>

        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入工号 / 手机号"
            prefix-icon="User"
            @blur="validateField('username')"
            @input="onInputChange('username')"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            @blur="validateField('password')"
            @input="onInputChange('password')"
          />
        </el-form-item>

        <div class="btn-wrapper">
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            @click="handleLogin"
            :loading="loading"
          >
            <i class="fa fa-sign-in" style="margin-right: 8px;" />
            立即登录
          </el-button>
        </div>
      </el-form>

      <!-- 测试账号提示 -->
      <div class="login-hint">
        <span><strong>管理员</strong> admin / 123456</span>
        <span><strong>教师</strong> T001 / 123456</span>
        <span><strong>HR</strong> HR001 / 123456</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authAPI } from '@/api'

// ----- 常量定义 -----
const STORAGE_KEYS = {
  TOKEN: 'token',
  USER_ROLE: 'userRole',
  USER_ID: 'userId',
  USERNAME: 'username',
  USER_INFO: 'userInfo',
}

const ROLE_MAP = {
  admin: 3,
  teacher: 1,
  hr: 2,
}

const ROLE_NAME_MAP = { 0: 'student', 1: 'teacher', 2: 'hr', 3: 'admin' }

const roleList = [
  { key: 'admin', label: '管理员', icon: 'fa fa-user' },
  { key: 'teacher', label: '普通教师', icon: 'fa fa-user-circle' },
  { key: 'hr', label: '企业HR', icon: 'fa fa-building' },
]

const router = useRouter()
const loginFormRef = ref()
const loading = ref(false)
const networkBg = ref(null)

const loginForm = reactive({
  username: '',
  password: '',
  role: '', // 默认为空
})

// 表单校验规则
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

// ----- 角色选择与键盘切换 -----
const selectRole = (role) => {
  if (loading.value) return
  loginForm.role = role
  clearFieldStatus()
}

const switchRole = (currentIndex, direction) => {
  if (loading.value) return
  const newIndex = (currentIndex + direction + roleList.length) % roleList.length
  selectRole(roleList[newIndex].key)
}

// ----- 字段校验（实时反馈）-----
const fieldStatus = reactive({
  username: { valid: false, touched: false },
  password: { valid: false, touched: false },
})

const validateField = (field) => {
  const value = loginForm[field]
  const touched = fieldStatus[field]
  touched.touched = true
  touched.valid = value && value.trim().length > 0
}

const onInputChange = (field) => {
  const value = loginForm[field]
  const touched = fieldStatus[field]
  if (value && value.trim().length > 0) {
    touched.valid = true
    touched.touched = true
  } else {
    touched.valid = false
    touched.touched = false
  }
}

const clearFieldStatus = () => {
  fieldStatus.username = { valid: false, touched: false }
  fieldStatus.password = { valid: false, touched: false }
}

// ----- 登录逻辑（完整保留原始接口）-----
const handleLogin = async () => {
  // 手动校验角色
  if (!loginForm.role) {
    ElMessage.warning('请先选择登录身份')
    return
  }

  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const requestData = {
          username: loginForm.username,
          password: loginForm.password,
          role: ROLE_MAP[loginForm.role],
        }
        const res = await authAPI.login(requestData)
        if (res.code === 200) {
          const data = res.data
          const roleName = ROLE_NAME_MAP[data.role] || data.role
          // 剥离 "Bearer " 前缀
          const rawToken = data.token.startsWith('Bearer ')
            ? data.token.slice(7)
            : data.token
          localStorage.setItem(STORAGE_KEYS.TOKEN, rawToken)
          localStorage.setItem(STORAGE_KEYS.USER_ROLE, roleName)
          localStorage.setItem(STORAGE_KEYS.USER_ID, data.userId)
          localStorage.setItem(STORAGE_KEYS.USERNAME, data.username)
          localStorage.setItem(
            STORAGE_KEYS.USER_INFO,
            JSON.stringify({
              userId: data.userId,
              username: data.username,
              realName: data.realName,
              role: roleName,
              avatarUrl: data.avatarUrl,
              companyId: data.companyId,
            })
          )

          ElMessage.success('登录成功')

          // 路由跳转
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
        // 细化错误提示
        let msg = error.response?.data?.message || '登录失败'
        if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
          msg = '网络超时，请检查连接后重试'
        } else if (!navigator.onLine) {
          msg = '网络已断开，请连接网络后重试'
        }
        ElMessage.error(msg)
        // 输入框抖动反馈
        const wrappers = document.querySelectorAll('.login-form .el-input__wrapper')
        wrappers.forEach(el => {
          el.classList.add('shake-error')
          setTimeout(() => el.classList.remove('shake-error'), 500)
        })
      } finally {
        loading.value = false
      }
    }
  })
}

// ----- 鼠标视差效果 -----
const handleMouseMove = (e) => {
  const x = (e.clientX / window.innerWidth - 0.5) * 15
  const y = (e.clientY / window.innerHeight - 0.5) * 15
  const bg = networkBg.value
  if (bg) {
    bg.style.transition = 'transform 0.8s cubic-bezier(0.2, 0.9, 0.4, 1)'
    bg.style.transform = `translate(${x}px, ${y}px)`
  }
}

// ----- 生命周期 -----
onMounted(() => {
  // 生成背景节点
  nextTick(() => {
    const container = networkBg.value
    if (!container) return
    for (let i = 0; i < 30; i++) {
      const node = document.createElement('div')
      node.className = 'node'
      const size = Math.random() * 8 + 3
      node.style.width = size + 'px'
      node.style.height = size + 'px'
      node.style.top = Math.random() * 100 + '%'
      node.style.left = Math.random() * 100 + '%'
      node.style.setProperty('--duration', (Math.random() * 5 + 3) + 's')
      node.style.setProperty('--delay', (Math.random() * 6) + 's')
      container.appendChild(node)
    }
  })

  // 鼠标视差
  window.addEventListener('mousemove', handleMouseMove)
})

onUnmounted(() => {
  window.removeEventListener('mousemove', handleMouseMove)
})
</script>

<style scoped>
/* ===== 字体导入 ===== */
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@400;600;700;800&family=Inter:wght@400;500;600&display=swap');

/* ===== CSS 变量 ===== */
:root {
  --primary-deep: #0F172A;
  --primary-blue: #165DFF;
  --primary-light: #3B82F6;
  --shadow-heavy: 0 25px 50px -12px rgba(22, 93, 255, 0.35);
  --radius-md: 14px;
}

/* ===== 页面容器 ===== */
.login-page {
  position: fixed;
  inset: 0;
  display: flex;
  z-index: 2000;
  background: #EEF3FB;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

/* ===== 左侧品牌区 ===== */
.login-left {
  flex: 1;
  background: linear-gradient(135deg, #0B132B 0%, #165DFF 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 60px;
  position: relative;
  overflow: hidden;
}

/* 环境光晕 */
.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  pointer-events: none;
  z-index: 0;
  animation: floatGlow 12s ease-in-out infinite alternate;
  will-change: transform;
}
.glow-orb--1 {
  width: 400px;
  height: 400px;
  background: #3B82F6;
  top: -10%;
  right: -10%;
  animation-delay: 0s;
}
.glow-orb--2 {
  width: 300px;
  height: 300px;
  background: #60A5FA;
  bottom: -10%;
  left: -10%;
  animation-delay: 4s;
  opacity: 0.4;
}
.glow-orb--3 {
  width: 200px;
  height: 200px;
  background: #93C5FD;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: 8s;
  opacity: 0.3;
}

@keyframes floatGlow {
  0% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -40px) scale(1.2); }
  66% { transform: translate(-20px, 30px) scale(0.9); }
  100% { transform: translate(10px, -20px) scale(1.1); }
}

/* 背景节点网络 */
.network-bg {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  transition: transform 0.3s ease-out;
}

.node {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  animation: pulse var(--duration, 4s) ease-in-out infinite alternate;
  animation-delay: var(--delay, 0s);
  z-index: 1;
  will-change: transform, opacity;
}

@keyframes pulse {
  0% { opacity: 0.15; transform: scale(0.8); }
  100% { opacity: 0.7; transform: scale(1.3); }
}

.lines {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0.5;
  z-index: 1;
  pointer-events: none;
}

/* 品牌文案 */
.brand-content {
  position: relative;
  z-index: 3;
  max-width: 600px;
  color: white;
}

.brand-content h1 {
  font-family: 'Outfit', sans-serif;
  font-size: 48px;
  font-weight: 800;
  line-height: 1.1;
  margin-bottom: 16px;
  letter-spacing: -0.02em;
}

.brand-content .highlight {
  background: linear-gradient(135deg, #93C5FD, #E0F2FE);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-content p {
  font-size: 18px;
  opacity: 0.85;
  font-weight: 400;
  margin-bottom: 32px;
  letter-spacing: 0.3px;
}

.stats {
  display: flex;
  gap: 40px;
  font-size: 14px;
  opacity: 0.8;
}

.stats-item {
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  cursor: default;
}
.stats-item:hover {
  transform: translateY(-6px);
}

.stats strong {
  font-family: 'Outfit', sans-serif;
  font-size: 26px;
  font-weight: 700;
  display: block;
  color: white;
  opacity: 1;
  margin-bottom: 4px;
  letter-spacing: -0.02em;
}

/* ===== 右侧登录表单 ===== */
.login-right {
  width: 480px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px) saturate(1.8);
  -webkit-backdrop-filter: blur(20px) saturate(1.8);
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 60px 48px;
  box-shadow: var(--shadow-heavy);
  border-left: 1px solid rgba(255, 255, 255, 0.3);
  position: relative;
}

.login-header {
  margin-bottom: 32px;
}

.login-title {
  font-family: 'Outfit', sans-serif;
  font-size: 28px;
  font-weight: 700;
  color: var(--primary-deep);
  letter-spacing: -0.02em;
}

.login-sub {
  color: #64748B;
  font-size: 14px;
  margin-top: 4px;
  font-weight: 400;
}

/* 角色标签页 */
.login-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 32px;
  background: #EFF3FA;
  padding: 6px;
  border-radius: 16px;
  transition: opacity 0.3s;
}
.login-tabs.disabled {
  opacity: 0.5;
  pointer-events: none;
}

.login-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 0;
  border-radius: 12px;
  cursor: pointer;
  font-weight: 500;
  font-size: 14px;
  color: #475569;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: transparent;
  border: none;
  font-family: 'Inter', sans-serif;
  position: relative;
  outline: none;
}
.login-tab:focus-visible {
  outline: 2px solid var(--primary-blue);
  outline-offset: 2px;
}
.login-tab.active {
  background: white;
  color: var(--primary-blue);
  box-shadow: 0 4px 14px rgba(22, 93, 255, 0.15);
  font-weight: 600;
}
.login-tab:not(.active):hover {
  color: var(--primary-blue);
  background: rgba(255, 255, 255, 0.5);
}

/* 表单覆盖 el-form 默认样式 */
.login-form {
  position: relative;
}
.login-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 4px 16px;
  border: 2px solid #E2E8F0;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
  box-shadow: none !important;
  background: white;
}
.login-form :deep(.el-input__wrapper:hover) {
  border-color: #94A3B8;
}
.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary-blue);
  box-shadow: 0 0 0 4px rgba(22, 93, 255, 0.08) !important;
}
.login-form :deep(.el-input__inner) {
  height: 50px;
  font-size: 15px;
}
.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}
/* 校验状态覆盖 */
.login-form :deep(.el-form-item.is-error .el-input__wrapper) {
  border-color: #EF4444;
  background: #FEF2F2;
}
.login-form :deep(.el-form-item.is-success .el-input__wrapper) {
  border-color: #22C55E;
  background: #F0FDF4;
}
/* 密码显隐图标位置调整 */
.login-form :deep(.el-input-group .el-input__wrapper) {
  padding-right: 44px;
}
.login-form :deep(.el-input__suffix) {
  right: 12px;
}

/* 抖动动画 */
@keyframes shakeError {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-6px); }
  75% { transform: translateX(6px); }
}
.login-form :deep(.el-input__wrapper.shake-error) {
  animation: shakeError 0.4s ease;
}

/* 按钮 */
.btn-wrapper {
  position: relative;
  width: 100%;
  margin-top: 8px;
  border-radius: var(--radius-md);
  overflow: hidden;
}
.login-btn {
  width: 100%;
  height: 54px;
  border-radius: var(--radius-md);
  font-weight: 600;
  font-size: 16px;
  letter-spacing: 0.5px;
  transition: all 0.3s ease;
  border: none;
  background: var(--primary-blue);
  color: white;
}
.login-btn:hover:not(:disabled) {
  background: #1A4FD4;
  transform: translateY(-2px);
  box-shadow: 0 12px 28px -8px rgba(22, 93, 255, 0.50);
}
.login-btn:active:not(:disabled) {
  transform: translateY(0);
}
.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none !important;
}
/* 覆盖 Element Plus 按钮默认样式 */
.login-form :deep(.el-button--primary) {
  --el-button-bg-color: var(--primary-blue);
  --el-button-border-color: var(--primary-blue);
  --el-button-hover-bg-color: #1A4FD4;
  --el-button-hover-border-color: #1A4FD4;
}

/* 表单锁定遮罩 */
.form-disabled-overlay {
  position: absolute;
  inset: -10px -10px -10px -10px;
  z-index: 5;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(2px);
  border-radius: var(--radius-md);
  pointer-events: none;
}

/* 测试账号提示 */
.login-hint {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 28px;
  padding: 12px 16px;
  background: #F2F6FF;
  border-radius: 12px;
  font-size: 12px;
  color: #475569;
  border: 1px dashed #BCD1F5;
  flex-wrap: wrap;
}
.login-hint span {
  white-space: nowrap;
}
.login-hint span strong {
  color: var(--primary-blue);
}

/* ===== 响应式适配 ===== */
@media (max-width: 1024px) {
  .login-right { width: 420px; padding: 48px 36px; }
  .brand-content h1 { font-size: 36px; }
}
@media (max-width: 768px) {
  .login-page { flex-direction: column; position: relative; height: 100vh; overflow-y: auto; }
  .login-left { flex: 0 0 280px; padding: 40px 24px; }
  .brand-content h1 { font-size: 28px; }
  .brand-content p { font-size: 15px; }
  .stats { gap: 20px; }
  .stats strong { font-size: 20px; }

  .login-right {
    width: 100%; flex: 1; padding: 32px 24px;
    box-shadow: 0 -10px 30px rgba(22, 93, 255, 0.08);
    border-radius: 28px 28px 0 0;
    margin-top: -20px; position: relative; z-index: 3;
    backdrop-filter: blur(16px);
  }
  .login-title { font-size: 24px; }
  .login-tabs { gap: 4px; padding: 4px; }
  .login-tab { font-size: 12px; padding: 8px 0; }
  .login-tab i { font-size: 14px; }
  .login-hint { gap: 8px; font-size: 11px; }
}
@media (max-width: 480px) {
  .login-left { flex: 0 0 200px; padding: 20px 16px; }
  .brand-content h1 { font-size: 22px; }
  .brand-content p { font-size: 12px; margin-bottom: 16px; }
  .stats { gap: 12px; }
  .stats strong { font-size: 16px; }
  .stats span { font-size: 10px; }
  .login-right { padding: 24px 16px; }
  .login-title { font-size: 20px; }
}
</style>