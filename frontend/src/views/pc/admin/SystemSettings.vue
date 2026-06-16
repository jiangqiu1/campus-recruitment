<template>
  <div class="system-settings">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>系统设置</span>
        </div>
      </template>
      
      <el-tabs v-model="activeTab">
        <!-- 基础设置 -->
        <el-tab-pane label="基础设置" name="basic">
          <el-form :model="basicForm" label-width="120px">
            <el-form-item label="系统名称">
              <el-input v-model="basicForm.systemName" />
            </el-form-item>
            <el-form-item label="Logo">
              <el-upload
                class="avatar-uploader"
                action="/api/files/upload"
                :show-file-list="false"
                :on-success="handleLogoSuccess"
              >
                <img v-if="basicForm.logo" :src="basicForm.logo" class="avatar" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>
            <el-form-item label="每页显示条数">
              <el-input-number v-model="basicForm.pageSize" :min="10" :max="100" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveBasicSettings">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <!-- 安全设置 -->
        <el-tab-pane label="安全设置" name="security">
          <el-form :model="securityForm" label-width="120px">
            <el-form-item label="JWT密钥">
              <el-input v-model="securityForm.jwtSecret" type="password" show-password />
            </el-form-item>
            <el-form-item label="Token有效期">
              <el-input-number v-model="securityForm.jwtExpiration" :min="3600" :max="604800" />
              <span style="margin-left: 8px;">秒</span>
            </el-form-item>
            <el-form-item label="AES密钥">
              <el-input v-model="securityForm.aesKey" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveSecuritySettings">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <!-- 通知设置 -->
        <el-tab-pane label="通知设置" name="notification">
          <el-form :model="notificationForm" label-width="120px">
            <el-form-item label="启用邮件通知">
              <el-switch v-model="notificationForm.emailEnabled" />
            </el-form-item>
            <el-form-item label="SMTP服务器" v-if="notificationForm.emailEnabled">
              <el-input v-model="notificationForm.smtpHost" placeholder="smtp.example.com" />
            </el-form-item>
            <el-form-item label="SMTP端口" v-if="notificationForm.emailEnabled">
              <el-input-number v-model="notificationForm.smtpPort" :min="1" :max="65535" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveNotificationSettings">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { settingsAPI } from '@/api'

const activeTab = ref('basic')

const basicForm = ref({
  systemName: '职业院校校企招聘与就业管理平台',
  logo: '',
  pageSize: 20
})

const securityForm = ref({
  jwtSecret: '',
  jwtExpiration: 604800,
  aesKey: ''
})

const notificationForm = ref({
  emailEnabled: false,
  smtpHost: '',
  smtpPort: 465
})

onMounted(async () => {
  await loadSettings()
})

async function loadSettings() {
  try {
    const res = await settingsAPI.getSettings()
    if (res.code === 200) {
      const data = res.data
      basicForm.value = data.basic || basicForm.value
      securityForm.value = data.security || securityForm.value
      notificationForm.value = data.notification || notificationForm.value
    }
  } catch (error) {
    console.error('加载设置失败:', error)
  }
}

async function saveBasicSettings() {
  try {
    const res = await settingsAPI.saveBasic(basicForm.value)
    if (res.code === 200) {
      ElMessage.success('基础设置保存成功')
    }
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

async function saveSecuritySettings() {
  try {
    const res = await settingsAPI.saveSecurity(securityForm.value)
    if (res.code === 200) {
      ElMessage.success('安全设置保存成功')
    }
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

async function saveNotificationSettings() {
  try {
    const res = await settingsAPI.saveNotification(notificationForm.value)
    if (res.code === 200) {
      ElMessage.success('通知设置保存成功')
    }
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

function handleLogoSuccess(response) {
  if (response.code === 200) {
    basicForm.value.logo = response.data.url
    ElMessage.success('Logo上传成功')
  } else {
    ElMessage.error('上传失败')
  }
}
</script>

<style scoped>
.system-settings {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  width: 178px;
  height: 178px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-uploader:hover {
  border-color: #409eff;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}

.avatar {
  width: 178px;
  height: 178px;
  display: block;
}
</style>
