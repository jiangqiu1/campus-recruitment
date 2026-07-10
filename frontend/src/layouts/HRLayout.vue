<template>
  <div class="hr-layout" style="display: flex; min-height: 100vh;">
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
        <el-menu-item index="/hr/resumes">
          <el-icon><Document /></el-icon>
          <template #title>简历处理</template>
        </el-menu-item>
        <el-menu-item index="/hr/candidates">
          <el-icon><UserFilled /></el-icon>
          <template #title>候选人管理</template>
        </el-menu-item>
        <el-menu-item index="/hr/interviews">
          <el-icon><Timer /></el-icon>
          <template #title>面试管理</template>
        </el-menu-item>
        <el-menu-item index="/hr/jobs">
          <el-icon><Briefcase /></el-icon>
          <template #title>岗位管理</template>
        </el-menu-item>
        <el-menu-item index="/hr/analysis">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>岗位分析</template>
        </el-menu-item>
        <el-menu-item index="/hr/stats">
          <el-icon><TrendCharts /></el-icon>
          <template #title>数据统计</template>
        </el-menu-item>
        <el-menu-item index="/hr/company">
          <el-icon><OfficeBuilding /></el-icon>
          <template #title>企业信息</template>
        </el-menu-item>
        <el-menu-item index="/hr/accounts">
          <el-icon><User /></el-icon>
          <template #title>账号管理</template>
        </el-menu-item>
      </el-menu>
    </aside>

    <div class="wrapper" :style="{ marginLeft: isCollapse ? '64px' : 'var(--sidebar-width)', width: isCollapse ? 'calc(100% - 64px)' : 'calc(100% - var(--sidebar-width))' }">
      <header class="navbar">
        <div class="navbar-left">
          <el-icon class="collapse-btn" @click="toggleSidebar" :size="20">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <Breadcrumb />
        </div>
        <div class="navbar-right">
          <NotificationBell />
          <el-dropdown @command="handleCommand">
            <span class="user-badge">
              <el-avatar :size="28" style="background: #165DFF; color: white; flex-shrink: 0;">
                {{ (userStore.realName || 'HR').charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ userStore.realName || 'HR' }}</span>
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

      <main class="container"><router-view /></main>

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
import NotificationBell from '@/components/NotificationBell.vue'
import { Fold, Expand, Document, TrendCharts, DataAnalysis, User, Briefcase, UserFilled, OfficeBuilding, Timer } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)

function toggleSidebar() { isCollapse.value = !isCollapse.value }
function handleCommand(command) {
  switch (command) {
    case 'profile':
      router.push('/hr/profile')
      break
    case 'password':
      router.push('/hr/password')
      break
    case 'logout': userStore.logout(); break
  }
}
</script>

<style scoped>
.sidebar { position: fixed; top: 0; left: 0; height: 100vh; background: var(--sidebar-bg); z-index: 999; transition: width 0.3s ease; overflow: hidden; box-shadow: 2px 0 20px rgba(0,0,0,0.15); }
.sidebar-logo { padding: 28px 20px; font-size: 16px; font-weight: bold; text-align: center; border-bottom: 1px solid var(--sidebar-border); margin-bottom: 24px; background: linear-gradient(90deg, #165DFF, #2563EB); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; letter-spacing: 0.5px; }
.wrapper { flex: 1; min-height: 100vh; display: flex; flex-direction: column; transition: margin-left 0.3s ease, width 0.3s ease; }
.navbar { background: rgba(255,255,255,0.92); backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px); padding: 18px 35px; box-shadow: 0 3px 15px rgba(0,0,0,0.06); display: flex; justify-content: space-between; align-items: center; position: sticky; top: 0; z-index: 100; }
.navbar-left { display: flex; align-items: center; gap: 12px; }
.collapse-btn { cursor: pointer; color: var(--text-secondary); transition: color 0.2s; }
.collapse-btn:hover { color: var(--primary); }
.navbar-right { display: flex; align-items: center; }
.user-badge { display: inline-flex; align-items: center; gap: 8px; cursor: pointer; padding: 8px 16px; background: var(--primary-bg); border-radius: var(--radius-md); border: 1px solid rgba(22,93,255,0.1); font-weight: 500; font-size: 14px; color: var(--text-secondary); }
.user-badge:hover { background: rgba(22,93,255,0.12); border-color: var(--primary); }
.user-name { max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.container { max-width: 1300px; margin: 35px auto; padding: 0 35px; width: 100%; flex: 1; }
</style>
