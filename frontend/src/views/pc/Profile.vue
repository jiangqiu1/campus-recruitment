<template>
  <div class="profile-page fade-in">
    <div class="page-header">
      <h2>个人信息</h2>
      <p>查看和编辑您的个人资料</p>
    </div>

    <div class="profile-layout">
      <!-- 左侧：头像 + 基本信息卡片 -->
      <div class="profile-sidebar">
        <div class="avatar-card">
          <div class="avatar-wrap">
            <el-avatar :size="120" :src="form.avatarUrl || undefined">
              {{ form.realName?.charAt(0) || '?' }}
            </el-avatar>
            <div class="avatar-overlay" @click="$refs.avatarInput.click()">
              <el-icon><Camera /></el-icon>
              <span>更换头像</span>
            </div>
          </div>
          <input ref="avatarInput" type="file" accept="image/*" style="display:none" @change="handleAvatarUpload" />
          <h3>{{ form.realName || '未设置姓名' }}</h3>
          <p class="role-tag">{{ roleLabel }}</p>
          <p class="account-info">账号：{{ form.username }}</p>
        </div>
      </div>

      <!-- 右侧：编辑表单 -->
      <div class="profile-main">
        <div class="content-card">
          <div class="content-card-header">
            <span class="content-card-title">基本信息</span>
          </div>

          <el-form :model="form" label-width="100px" class="profile-form">
            <el-form-item label="真实姓名">
              <el-input v-model="form.realName" placeholder="请输入真实姓名" />
            </el-form-item>

            <el-form-item label="手机号码">
              <el-input v-model="form.phone" placeholder="请输入手机号码" maxlength="11" />
            </el-form-item>

            <el-form-item label="电子邮箱">
              <el-input v-model="form.email" placeholder="请输入电子邮箱" />
            </el-form-item>

            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio :value="0">未知</el-radio>
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="角色">
              <el-tag>{{ roleLabel }}</el-tag>
            </el-form-item>

            <el-form-item v-if="form.companyId" label="所属企业">
              <span class="text-secondary">{{ form.companyName || '--' }}</span>
            </el-form-item>

            <el-form-item label="注册时间">
              <span class="text-secondary">{{ (form.createTime || '').replace('T', ' ') || '--' }}</span>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSave">
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { authAPI } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'

const userStore = useUserStore()
const saving = ref(false)

const form = reactive({
  realName: '',
  phone: '',
  email: '',
  gender: 0,
  avatarUrl: '',
  username: '',
  companyId: null,
  companyName: '',
  createTime: ''
})

const roleLabel = computed(() => {
  const map = { student: '学生', teacher: '教师', hr: '企业 HR', admin: '管理员' }
  return map[userStore.userRole] || '未知'
})

async function loadProfile() {
  try {
    const res = await authAPI.getProfile()
    if (res.code === 200) {
      const data = res.data
      form.realName = data.realName || ''
      form.phone = data.phone || ''
      form.email = data.email || ''
      form.gender = data.gender ?? 0
      form.avatarUrl = data.avatarUrl || ''
      form.username = data.username || ''
      form.companyId = data.companyId || null
      form.createTime = data.createTime || ''
    }
  } catch (e) {
    ElMessage.error('加载个人信息失败')
  }
}

async function handleSave() {
  saving.value = true
  try {
    const res = await authAPI.updateProfile({
      realName: form.realName,
      phone: form.phone,
      email: form.email,
      gender: form.gender,
      avatarUrl: form.avatarUrl
    })
    if (res.code === 200) {
      ElMessage.success('保存成功')
      // 同步更新 store 中的 realName 和 avatarUrl
      if (res.data) {
        try {
          userStore.setUserInfo({
            ...(userStore.userInfo || {}),
            realName: res.data.realName || form.realName,
            avatarUrl: res.data.avatarUrl || form.avatarUrl
          })
        } catch (e) { /* store sync error ignored */ }
      }
    }
  } catch (e) {
    // 拦截器已显示错误消息，不做重复提示
    console.error('保存个人信息失败', e)
  } finally {
    saving.value = false
  }
}

async function handleAvatarUpload(e) {
  const file = e.target.files?.[0]
  if (!file) return
  // 简单头像上传 - 用 FormData 上传到文件接口
  const fd = new FormData()
  fd.append('file', file)
  fd.append('type', 'avatar')
  try {
    const { default: axios } = await import('axios')
    const uploadRes = await axios.post('/api/files/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      params: { token: localStorage.getItem('token') }
    })
    if (uploadRes.data?.code === 200) {
      form.avatarUrl = uploadRes.data.data
      ElMessage.success('头像已上传，请保存个人信息')
    } else {
      ElMessage.error(uploadRes.data?.message || '上传失败')
    }
  } catch (e) {
    ElMessage.error('上传失败')
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page {
  max-width: 1100px;
  margin: 0 auto;
}

.profile-layout {
  display: flex;
  gap: 28px;
  align-items: flex-start;
}

.profile-sidebar {
  flex-shrink: 0;
  width: 280px;
}

.avatar-card {
  background: white;
  border-radius: var(--radius-lg);
  padding: 40px 24px;
  text-align: center;
  box-shadow: var(--card-shadow);
}

.avatar-wrap {
  position: relative;
  display: inline-block;
  margin-bottom: 16px;
}
.avatar-wrap :deep(.el-avatar) {
  background: #E8F3FF;
  color: #165DFF;
  font-size: 40px;
  font-weight: 600;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0,0,0,0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: white;
  font-size: 12px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.25s;
}

.avatar-wrap:hover .avatar-overlay {
  opacity: 1;
}

.avatar-card h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}

.role-tag {
  display: inline-block;
  padding: 4px 14px;
  background: var(--primary-bg);
  color: var(--primary);
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 12px;
}

.account-info {
  font-size: 13px;
  color: var(--text-muted);
}

.profile-main {
  flex: 1;
  min-width: 0;
}

.profile-form {
  max-width: 520px;
}

.text-secondary {
  color: var(--text-secondary);
  font-size: 14px;
}
</style>
