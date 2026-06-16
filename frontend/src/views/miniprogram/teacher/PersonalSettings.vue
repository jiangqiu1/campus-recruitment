<template>
  <div class="personal-settings">
    <div class="card">
      <h2>个人设置</h2>
      
      <el-form :model="settingsForm" label-width="100px" style="max-width: 500px; margin-top: 24px;">
        <el-form-item label="姓名">
          <el-input v-model="settingsForm.name" placeholder="请输入姓名" />
        </el-form-item>
        
        <el-form-item label="手机号">
          <el-input v-model="settingsForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        
        <el-form-item label="修改密码">
          <el-input 
            v-model="settingsForm.newPassword" 
            type="password" 
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="确认密码">
          <el-input 
            v-model="settingsForm.confirmPassword" 
            type="password" 
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="saveSettings">保存设置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'

const settingsForm = reactive({
  name: '陈老师',
  phone: '13800138000',
  newPassword: '',
  confirmPassword: ''
})

const saveSettings = async () => {
  if (settingsForm.newPassword && 
      settingsForm.newPassword !== settingsForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  
  try {
    // 实际应该调用API保存设置
    // const res = await axios.put('/api/teacher/settings', settingsForm)
    ElMessage.success('设置保存成功')
    
    // 清空密码字段
    settingsForm.newPassword = ''
    settingsForm.confirmPassword = ''
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

onMounted(async () => {
  // 实际应该加载当前用户信息
  // await loadUserInfo()
})
</script>

<style scoped>
.personal-settings {
  padding: 20px;
}

.card {
  background: white;
  border-radius: 16px;
  padding: 32px;
  margin-bottom: 28px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.06);
  position: relative;
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, #165DFF, #2563EB, transparent);
  border-radius: 16px 16px 0 0;
}

.card h2 {
  font-size: 19px;
  font-weight: 600;
  padding-bottom: 12px;
  border-bottom: 1px solid #F2F3F5;
  position: relative;
}

.card h2::after {
  content: '';
  width: 50px;
  height: 3px;
  background: #165DFF;
  border-radius: 3px;
  position: absolute;
  left: 0;
  bottom: -1px;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  outline: none;
  transition: all 0.25s;
  font-size: 14px;
}

.form-input:focus {
  border-color: #165DFF;
  box-shadow: 0 0 0 4px rgba(22, 93, 255, 0.12);
}

.btn {
  padding: 10px 20px;
  background: #165DFF;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.25s ease;
  box-shadow: 0 3px 8px rgba(22, 93, 255, 0.2);
  position: relative;
  overflow: hidden;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn:hover {
  background: #0E42C1;
  box-shadow: 0 5px 15px rgba(22, 93, 255, 0.3);
  transform: translateY(-2px);
}
</style>
