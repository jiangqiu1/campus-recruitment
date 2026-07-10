<template>
  <div class="password-page fade-in">
    <div class="page-header">
      <h2>修改密码</h2>
      <p>定期更改密码有助于保障账号安全</p>
    </div>

    <div class="password-card">
      <div class="content-card">
        <div class="content-card-header">
          <span class="content-card-title">密码设置</span>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="120px"
          class="password-form"
        >
          <el-form-item label="当前密码" prop="oldPassword">
            <el-input
              v-model="form.oldPassword"
              type="password"
              show-password
              placeholder="请输入当前密码"
            />
          </el-form-item>

          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="form.newPassword"
              type="password"
              show-password
              placeholder="请输入新密码（至少6位）"
            />
          </el-form-item>

          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              show-password
              placeholder="请再次输入新密码"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              确认修改
            </el-button>
          </el-form-item>
        </el-form>

        <el-divider />

        <div class="security-tips">
          <h4>密码安全提示</h4>
          <ul>
            <li>密码长度至少 6 位字符</li>
            <li>建议包含字母、数字和特殊字符的组合</li>
            <li>请勿与其他网站使用相同的密码</li>
            <li>修改密码后需要重新登录</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const result = await userStore.changePassword(form.oldPassword, form.newPassword)
    if (result.success) {
      // changePassword 内部会调用 logout，跳转到登录页
    }
  } catch (e) {
    // 错误已在 store 中处理
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.password-page {
  max-width: 700px;
  margin: 0 auto;
}

.password-form {
  max-width: 480px;
}

.security-tips {
  background: var(--primary-bg);
  border-radius: var(--radius-md);
  padding: 20px 24px;
}

.security-tips h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--primary);
  margin-bottom: 10px;
}

.security-tips ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.security-tips li {
  font-size: 13px;
  color: var(--text-secondary);
  padding: 4px 0;
  padding-left: 16px;
  position: relative;
}

.security-tips li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 6px;
  height: 6px;
  background: var(--primary);
  border-radius: 50%;
  opacity: 0.4;
}
</style>
