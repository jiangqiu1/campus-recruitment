<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :style="{ width: isCollapse ? '64px' : 'var(--sidebar-width)' }">
      <div class="sidebar-logo">
        <span v-if="!isCollapse">招聘就业管理平台</span>
        <span v-else>招</span>
      </div>
      
      <el-menu
        :default-active="route.path"
        :collapse="isCollapse"
        router
        background-color="#0F172A"
        text-color="#CBD5E1"
        active-text-color="#FFFFFF"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataLine /></el-icon>
          <template #title>数据大屏</template>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/companies">
          <el-icon><OfficeBuilding /></el-icon>
          <template #title>企业管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/classes">
          <el-icon><Reading /></el-icon>
          <template #title>班级管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/audit">
          <el-icon><Checked /></el-icon>
          <template #title>企业审核</template>
        </el-menu-item>
        <el-menu-item index="/admin/logs">
          <el-icon><Document /></el-icon>
          <template #title>操作日志</template>
        </el-menu-item>
        <el-menu-item index="/admin/export">
          <el-icon><Download /></el-icon>
          <template #title>数据导出</template>
        </el-menu-item>
        <el-menu-item index="/admin/settings">
          <el-icon><Setting /></el-icon>
          <template #title>系统设置</template>
        </el-menu-item>
      </el-menu>
    </aside>
    
    <!-- 主体区域 -->
    <div class="wrapper" :style="{ marginLeft: isCollapse ? '64px' : 'var(--sidebar-width)', width: isCollapse ? 'calc(100% - 64px)' : 'calc(100% - var(--sidebar-width))' }">
      <!-- 顶部导航栏 -->
      <header class="navbar">
        <div class="navbar-left">
          <el-icon class="collapse-btn" @click="toggleSidebar" :size="20">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <Breadcrumb />
        </div>
        <div class="navbar-right">
          <el-dropdown @command="handleCommand">
            <span class="user-badge">
              <el-avatar :size="28" style="background: #165DFF; color: white; flex-shrink: 0;">
                {{ (userStore.realName || '管理员').charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ userStore.realName || '管理员' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      
      <!-- 主内容 -->
      <main class="container">
        <router-view />
      </main>
      
      <!-- 页脚 -->
      <footer class="footer-area">
        <p>&copy; 2026 招聘就业管理平台 · 全校就业数据一张图 校企协同数字化 高效就业管理</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import Breadcrumb from '@/components/Breadcrumb.vue'
import { Fold, Expand, DataLine, User, OfficeBuilding, Reading, Checked, Document, Download, Setting } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)

function toggleSidebar() {
  isCollapse.value = !isCollapse.value
}

function handleCommand(command) {
  switch (command) {
    case 'profile': break
    case 'password': break
    case 'logout':
      userStore.logout()
      break
  }
}
</script>

<style scoped>
.admin-layout { display: flex; min-height: 100vh; }

.sidebar {
  position: fixed; top: 0; left: 0; height: 100vh;
  background: var(--sidebar-bg);
  z-index: 999;
  transition: width 0.3s ease;
  overflow: hidden;
  box-shadow: 2px 0 15px rgba(0,0,0,0.12);
}

.sidebar-logo {
  padding: 28px 20px;
  font-size: 16px;
  font-weight: bold;
  text-align: center;
  border-bottom: 1px solid var(--sidebar-border);
  margin-bottom: 20px;
  background: linear-gradient(90deg, #165DFF, #2563EB);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.5px;
}

.sidebar .el-menu {
  border-right: none;
}

.wrapper {
  flex: 1;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  transition: margin-left 0.3s ease, width 0.3s ease;
}

.navbar {
  background: var(--header-bg);
  backdrop-filter: blur(12px);
  padding: 18px 35px;
  box-shadow: 0 3px 15px rgba(0,0,0,0.06);
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 100;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn {
  cursor: pointer;
  color: var(--text-secondary);
  transition: color 0.2s;
}
.collapse-btn:hover { color: var(--primary); }

.navbar-right { display: flex; align-items: center; }

.user-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 16px;
  background: var(--primary-bg);
  border-radius: var(--radius-md);
  border: 1px solid rgba(22,93,255,0.1);
  font-weight: 500;
  font-size: 14px;
  color: var(--text-secondary);
  transition: all 0.2s;
}
.user-badge:hover {
  background: rgba(22,93,255,0.12);
  border-color: var(--primary);
}

.user-name { max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.container {
  max-width: 1300px;
  margin: 35px auto;
  padding: 0 35px;
  width: 100%;
  flex: 1;
}

:deep(.el-menu) { border-right: none; }
</style>
