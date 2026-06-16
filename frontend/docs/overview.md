# 校企招聘平台 — 前端总览文档

---

## 一、原理文档

### 1.1 技术栈总览

| 技术栈 | 版本 | 用途 |
|--------|------|------|
| Vue 3 | ^3.3.0 | 前端框架（Composition API） |
| Vite 5 | ^5.0.0 | 构建工具与开发服务器 |
| Element Plus | ^2.4.0 | UI 组件库 |
| Vue Router 4 | ^4.2.0 | 前端路由管理 |
| Pinia | ^2.1.0 | 状态管理 |
| Axios | ^1.6.0 | HTTP 请求库 |
| ECharts | ^5.6.0 | 数据可视化图表 |
| dayjs | ^1.11.10 | 日期处理工具 |
| js-cookie | ^3.0.5 | Cookie 操作 |
| xlsx + file-saver | — | 表格导出与文件下载 |
| sass | ^1.69.0 | CSS 预处理器（dev） |

### 1.2 技术选型理由

| 技术 | 选型理由 |
|------|----------|
| **Vue 3 Composition API** | 组合式 API 提供更好的逻辑复用能力（`<script setup>` + 组合函数），相较于 Vue 2 Options API 更适合中大型项目，类型推导更友好 |
| **Vite 5** | 原生 ES Module 开发服务器，冷启动毫秒级，HMR 极速热更新；基于 Rollup 4 的构建能力，开箱即用地支持路径别名、代理等配置 |
| **Element Plus** | Vue 3 生态中最成熟的中后台组件库，提供 Table、Form、Menu、Dialog、Select 等全套业务组件，支持按需引入和主题定制 |
| **Pinia** | Vue 3 官方推荐状态管理方案，TypeScript 支持好，无 mutations 概念，API 简洁，支持 DevTools |
| **Axios** | 功能完善的 HTTP 客户端，支持请求/响应拦截器、自动 JSON 序列化、超时控制、Token 携带 |
| **ECharts** | 国内使用最广的数据可视化库，图表类型丰富（柱状图、折线图、饼图、雷达图等），配置灵活，适合管理后台 BI 场景 |

### 1.3 Vue 3 Composition API 风格

本项目统一采用 `<script setup>` 语法（Vue 3.3+），特点如下：

**响应式状态：**
```js
import { ref, computed } from 'vue'
const count = ref(0)
const double = computed(() => count.value * 2)
```

**组合函数复用逻辑**（对比 Mixins 更清晰，无命名冲突）：
```js
// 将可复用逻辑提取为 useXxx 函数
export function usePagination(api) {
  const page = ref(1)
  const total = ref(0)
  async function fetch() { /* ... */ }
  return { page, total, fetch }
}
```

**优于 Options API 的方面：**
- 逻辑聚合：同一功能的逻辑集中在一起而非分散到 data/methods/computed
- 无 `this` 歧义：所有 ref 直接通过变量名访问
- 更好的 Tree-shaking：未使用的导出不会被打包
- 类型推导：无需单独声明接口即可获得类型提示

### 1.4 Element Plus 组件库集成

**全局注册**（`src/main.js`）：
```js
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

app.use(ElementPlus)

// 全局注册图标，模板中可直接使用 <el-icon><User /></el-icon>
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
```

**本项目使用的主要 Element Plus 组件：**

| 组件 | 用途 |
|------|------|
| `el-container` / `el-aside` / `el-header` / `el-main` | 页面布局骨架 |
| `el-menu` / `el-menu-item` | 侧边栏导航菜单 |
| `el-form` / `el-form-item` / `el-input` / `el-select` | 表单与搜索筛选 |
| `el-table` / `el-table-column` | 数据列表展示 |
| `el-pagination` | 分页控制 |
| `el-dialog` / `el-drawer` | 弹窗/抽屉编辑 |
| `el-button` / `el-button-group` | 操作按钮 |
| `el-message` / `el-message-box` | 全局消息提示与确认框 |
| `el-breadcrumb` | 面包屑导航 |
| `el-dropdown` | 用户下拉菜单 |
| `el-avatar` | 用户头像 |
| `el-tag` / `el-badge` | 状态标签和徽标 |
| `el-upload` | 文件上传 |
| `el-tabs` | 标签页切换 |
| `el-tree` | 树形结构展示 |
| `el-date-picker` | 日期选择 |

### 1.5 Vite 构建配置

完整的 `vite.config.js`：

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],                           // Vue SFC 编译插件
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')          // 路径别名，import xxx from '@/api'
    }
  },
  server: {
    port: 5173,                               // 开发服务器端口
    proxy: {
      '/api': {                               // API 代理：开发时解决跨域
        target: 'http://localhost:8080',       // 代理到后端服务
        changeOrigin: true                     // 修改请求 Host 头
      }
    }
  }
})
```

**构建脚本（`package.json`）：**
```json
{
  "scripts": {
    "dev": "vite",          // 启动开发服务器（热更新）
    "build": "vite build",  // 生产构建
    "preview": "vite preview" // 预览构建产物
  }
}
```

**关键构建特性：**
- `@` 路径别名避免深层相对路径（如 `@/api` → `../../api`）
- 开发环境 `/api` 自动代理到 `http://localhost:8080`，解决跨域
- 生产部署时 Nginx 反向代理 `/api` 到后端服务

---

## 二、项目结构说明

### 2.1 整体目录结构

```
frontend/
├── docs/                           # 项目文档
├── node_modules/                   # 依赖包
├── src/                            # 源码目录
│   ├── api/
│   │   └── index.js                # Axios 实例 + 所有 API 接口封装
│   ├── assets/
│   │   ├── logo.svg                # 完整 Logo（展开状态）
│   │   └── logo-mini.svg           # Mini Logo（折叠状态）
│   ├── components/
│   │   └── Breadcrumb.vue          # 通用面包屑导航组件
│   ├── layouts/
│   │   ├── AdminLayout.vue         # 管理员布局（8个侧边栏菜单项）
│   │   ├── TeacherLayout.vue       # 教师布局（4个菜单项）
│   │   └── HRLayout.vue            # HR布局（4个菜单项）
│   ├── router/
│   │   └── index.js                # 路由配置 + 全局路由守卫
│   ├── stores/
│   │   ├── user.js                 # Pinia：用户状态（登录/登出/权限）
│   │   ├── app.js                  # Pinia：应用状态（侧边栏/主题/标签栏/面包屑）
│   │   ├── resume.js               # Pinia：简历状态（列表/分页/筛选/CRUD）
│   │   └── job.js                  # Pinia：岗位状态（列表/搜索/推荐/统计）
│   ├── views/
│   │   ├── Login.vue               # 登录页
│   │   ├── pc/                     # PC 后台页面
│   │   │   ├── admin/              # 管理员 PC 后台（8个页面）
│   │   │   ├── teacher/            # 教师 PC 后台（4个页面）
│   │   │   └── hr/                 # HR PC 后台（4个页面）
│   │   └── miniprogram/            # 小程序页面（11个，不参与 PC 路由）
│   ├── App.vue                     # 根组件（仅 router-view + 全局样式）
│   └── main.js                     # 入口文件（挂载应用）
├── index.html                      # HTML 入口
├── package.json                    # 项目依赖与脚本
├── vite.config.js                  # Vite 构建配置
└── create-views.ps1                # 视图创建辅助脚本
```

### 2.2 各目录/文件职责

#### `src/main.js` — 入口文件

应用启动的唯一入口，按顺序执行：
1. 创建 Vue 应用实例
2. 设置 Axios 全局 `baseURL: '/api'`
3. 全局注册 Element Plus 组件库及所有图标
4. 安装 Vue Router、Pinia 插件
5. 挂载应用到 `#app` 元素

```js
const app = createApp(App)
// 注册图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.use(ElementPlus).use(router).use(createPinia())
app.mount('#app')
```

#### `src/App.vue` — 根组件

仅包含 `<router-view />` 和全局 CSS 重置：
- 统一字体栈（PingFang SC → Microsoft YaHei → Arial）
- 消除默认 margin/padding
- 设置全局背景色 `#f5f7fa`

#### `src/api/index.js` — API 接口层

核心职责：
1. **创建 Axios 实例** — 配置 `baseURL: '/api'` 和 `timeout: 15000`
2. **请求拦截器** — 自动从 `localStorage` 读取 Token 并添加到请求头 `Authorization: Bearer <token>`
3. **响应拦截器** — 统一处理响应：
   - 业务成功（`res.code === 200`）：返回 `res.data`
   - 业务失败：`ElMessage.error(res.message)`
   - 401（登录过期）：清除本地存储 + 跳转登录页
   - 403（无权限）：`ElMessage.error('没有权限访问')`
   - 网络错误：`ElMessage.error('网络连接失败')`

**模块划分（12个 API 模块）：**

| 模块 | 导出名 | 核心接口 |
|------|--------|----------|
| 认证 | `authAPI` | login, register, logout, changePassword, checkToken |
| 用户 | `userAPI` | getUsers, createUser, updateUser, deleteUser, batchDelete, 状态更新, getUserInfo |
| 企业 | `companyAPI` | getCompanies, CRUD, 审核通过/拒绝, getCompanyProfile |
| 岗位 | `jobAPI` | getJobs, CRUD, 搜索, 热门推荐, 统计, 分析 |
| 简历 | `resumeAPI` | getResumes, CRUD, 搜索, 状态更新, PDF上传 |
| 投递 | `deliveryAPI` | getDeliveries, create, 状态更新, 按岗位查询 |
| 班级 | `classAPI` | getClasses, CRUD, 批量删除 |
| 人岗匹配 | `jobMatchAPI` | getMatches, generate, batchGenerate, push, delete |
| 评分 | `resumeScoreAPI` | getScores, score, batchScore, 详情, 删除 |
| AI解析 | `aiParseAPI` | getParseLogs, delete |
| 操作日志 | `operationLogAPI` | getLogs, delete, cleanup |
| 系统设置 | `settingsAPI` | getSettings, saveBasic, saveSecurity, saveNotification |

#### `src/layouts/` — 布局组件

三个布局组件均采用「侧边栏固定 + 顶部导航 + 主内容区」的经典后台结构：

```
┌──────────────────────────────────┐
│ Logo          │ 面包屑  用户信息  │
│───────────    ├──────────────────│
│ 导航菜单      │                  │
│               │    <router-view> │
│               │      (子页面)    │
│               │                  │
└───────────────┴──────────────────┘
```

**共性设计：**
- `el-aside` 支持折叠（64px ↔ 210px），通过 `isCollapse` 双向切换
- 顶栏右侧用户下拉菜单：个人信息、修改密码、退出登录
- 顶栏左侧：折叠按钮 + Breadcrumb 面包屑
- 菜单通过 `router` 属性启用路由模式，点击自动导航
- 默认激活项绑定 `route.path`，刷新保持高亮

**三个布局的角色划分：**

| 布局 | 角色 | 侧边栏菜单项数 | 主题色 |
|------|------|----------------|--------|
| AdminLayout | 系统管理员 | 8个 | 深蓝 #304156 |
| TeacherLayout | 教师 | 4个 | 深蓝 #304156 |
| HRLayout | 企业 HR | 4个 | 深蓝 #304156 |

#### `src/components/Breadcrumb.vue` — 面包屑组件

自动根据当前路由的 `matched` 数组生成面包屑导航。通过 `watch(route.path)` 监听路由变化并即时刷新，利用 `route.matched` 中每个路由记录的 `meta.title` 字段生成展示文字。

### 2.3 路由设计

#### 路由表结构

采用 **嵌套路由** 设计，三种后台共享同一层次结构：

| 路径 | 视图/布局 | 说明 |
|------|-----------|------|
| `/login` | Login.vue | 登录页 |
| `/admin` | AdminLayout | 管理员后台（8个子路由） |
| `/admin/dashboard` | Dashboard | 数据大屏（ECharts） |
| `/admin/users` | UserManage | 用户 CRUD 管理 |
| `/admin/companies` | CompanyManage | 企业管理 |
| `/admin/classes` | ClassManage | 班级管理 |
| `/admin/audit` | EnterpriseAudit | 企业入驻审核 |
| `/admin/logs` | OperationLog | 操作日志查询 |
| `/admin/export` | DataExport | 数据导出 |
| `/admin/settings` | SystemSettings | 系统配置 |
| `/teacher` | TeacherLayout | 教师后台（4个子路由） |
| `/teacher/dashboard` | Dashboard | 工作台概览 |
| `/teacher/classes` | ClassManagement | 班级管理 |
| `/teacher/jobs` | JobPosting | 岗位发布管理 |
| `/teacher/deliveries` | DeliveryBoard | 投递进度看板 |
| `/hr` | HRLayout | HR后台（4个子路由） |
| `/hr/resumes` | BatchResume | 批量简历审阅 |
| `/hr/stats` | DataStats | 数据统计 |
| `/hr/analysis` | JobAnalysis | 岗位分析 |
| `/hr/accounts` | AccountManagement | 企业子账号管理 |
| `/` | — | 重定向 → `/login` |
| `/:pathMatch(.*)*` | — | 404通配 → `/login` |

#### 路由守卫 — 权限控制

全局 `router.beforeEach` 实现了三层校验：

```
用户访问任意路由
       │
       ▼
┌──────────────────┐
│ 是否登录页？      │
│ (to.path===/login)│
└──────┬───────────┘
       │
   ┌───┴───┐
   │ 是    │ 否
   ▼       ▼
 ┌──────┐ ┌──────────────────────┐
 │有token│ │ 有 token 吗？         │
 └──┬───┘ └──┬───────────────────┘
    │        │
 ┌──┴──┐  ┌──┴──┐
 │是   │  │否   │ 是           否
 ▼    ▼  ▼    ▼              ▼
跳转  放行 检查  → /login   检查角色
后台      角色          ┌─────┴─────┐
          ┌────┴───┐  匹配     不匹配
          │admin   │           │
          │teacher │          └── ElMessage.error
          │hr      │              ('无权访问')
          └──→ 放行               next(false)
```

**具体逻辑：**

1. **登录页特殊处理** — 已登录用户访问 `/login` 时自动跳转对应后台：admin → `/admin/dashboard`，teacher → `/teacher/dashboard`，hr → `/hr/resumes`

2. **Token 校验** — 非登录页必须有 Token（`localStorage.getItem('token')`），否则重定向到 `/login`

3. **角色权限校验** — 每个路由的 `meta.role` 定义了允许的角色，与 `localStorage.getItem('userRole')` 比对，不匹配则弹出「无权访问该页面」并阻止导航

4. **页面标题** — 自动设置 `document.title = `${meta.title} - 招聘就业管理平台``

### 2.4 状态管理（Pinia）

项目划分 4 个 Store，职责清晰不重叠：

#### `stores/user.js` — 用户状态（核心 Store）

**状态：**
| 字段 | 类型 | 持久化 | 说明 |
|------|------|--------|------|
| token | string | localStorage | JWT 令牌 |
| userInfo | object | localStorage | 用户信息（userId, username, realName, role） |
| roles | array | localStorage | 角色列表 |
| permissions | array | localStorage | 权限列表 |

**计算属性：** isLoggedIn, userId, username, realName, userRole, isAdmin, isTeacher, isHR, isStudent

**方法：**
| 方法 | 说明 |
|------|------|
| login() | 调用 `authAPI.login` → 保存 token + userInfo |
| logout() | 调用登出接口 → 清除本地状态 → 跳转 `/login` |
| getUserInfo() | 获取并更新用户信息 |
| updateUserInfo() | 更新用户信息（本地同步） |
| changePassword() | 修改密码 → 强制登出 |
| hasPermission() | 检查权限字符串 |
| hasRole() | 检查角色 |
| checkToken() | 向后端验证 token 是否有效 |

**持久化方式：** 手动同步 `localStorage`，而非使用 `pinia-plugin-persistedstate`（保持轻量）。

#### `stores/app.js` — 应用状态

**状态：**
| 字段 | 类型 | 持久化 | 说明 |
|------|------|--------|------|
| sidebarCollapsed | boolean | localStorage | 侧边栏折叠状态 |
| theme | string | Cookie（365天） | 亮色/暗色主题 |
| language | string | Cookie（365天） | 语言偏好 |
| loading | boolean | — | 全局加载状态 |
| keepAlive | array | — | 需要缓存的组件名列表 |
| breadcrumb | array | — | 面包屑导航 |
| visitedViews | array | localStorage | 已访问页面标签（类 IDE 标签栏） |

**核心方法：** toggleSidebar, toggleTheme/setTheme, setLanguage, addVisitedView/delVisitedView, setBreadcrumb, addKeepAlive/removeKeepAlive

**暗色主题切换：** 通过 `document.documentElement.setAttribute('data-theme', theme)` + CSS class `dark` 实现双主题。

#### `stores/resume.js` — 简历管理

**职责：** 简历列表的 CRUD、分页、筛选、搜索

**状态：** resumeList, currentResume, loading, total, currentPage (default 1), pageSize (default 10), filters (studentName, major, status, classId)

**关键方法：** fetchResumeList, fetchResumeDetail, createResume, updateResume, deleteResume, batchDeleteResumes, updateResumeStatus, searchResumes, setFilters/resetFilters, setPagination, resetState

**设计模式：** 筛选 → 分页 → 请求 的链式调用，每次 CRUD 操作后自动刷新列表。

#### `stores/job.js` — 岗位管理

**职责：** 岗位列表的 CRUD、搜索、热门/推荐、统计

**状态：** jobList, currentJob, hotJobs, recommendJobs, loading, total, currentPage (default 1), pageSize (default 10), filters (keyword, companyId, industry, jobType, salaryRange, location, status), statistics

**计算属性：** hasJobs, isLoading, activeJobs（过滤 `status === 1` 的活跃岗位）, hotJobCount

**相较于 resume Store 的额外方法：** fetchHotJobs, fetchRecommendJobs, fetchJobStatistics — 用于首页数据大屏展示；searchJobs — 支持关键词搜索分页

**统一的分页模式（resume 和 job 共用）：**

```js
// 通用分页请求范式
async function fetchList(params) {
  loading.value = true
  try {
    const res = await api.getList({
      page: currentPage.value,
      size: pageSize.value,
      ...filters.value,
      ...params
    })
    // 成功 → 更新 list + total + currentPage + pageSize
  } finally {
    loading.value = false
  }
}
```

### 2.5 页面清单

#### PC 后台页面（16个）

| 角色 | 页面 | 路由路径 | 说明 |
|------|------|----------|------|
| **登录** | Login | `/login` | 统一登录入口 |
| **管理员** | Dashboard | `/admin/dashboard` | 数据大屏（ECharts 图表） |
| | UserManage | `/admin/users` | 用户 CRUD、批量删除、状态管理 |
| | CompanyManage | `/admin/companies` | 企业管理、审核状态 |
| | ClassManage | `/admin/classes` | 班级 CRUD |
| | EnterpriseAudit | `/admin/audit` | 企业入驻审核 |
| | OperationLog | `/admin/logs` | 操作日志检索、清理 |
| | DataExport | `/admin/export` | 数据导出（xlsx） |
| | SystemSettings | `/admin/settings` | 系统配置（基础/安全/通知） |
| **教师** | Dashboard | `/teacher/dashboard` | 工作台概览 |
| | ClassManagement | `/teacher/classes` | 所带班级管理 |
| | JobPosting | `/teacher/jobs` | 发布/编辑/下架岗位 |
| | DeliveryBoard | `/teacher/deliveries` | 查看学生投递进度 |
| **HR** | BatchResume | `/hr/resumes` | 批量审阅简历 |
| | DataStats | `/hr/stats` | 招聘数据统计 |
| | JobAnalysis | `/hr/analysis` | 岗位匹配分析 |
| | AccountManagement | `/hr/accounts` | 子账号管理 |

#### 小程序页面（11个，不参与 PC 路由）

| 角色 | 页面 |
|------|------|
| **HR** | Dashboard（工作台）、JobManage（岗位管理）、CompanyProfile（企业资料）、ResumeScore（简历评分） |
| **教师** | Dashboard（工作台）、ResumeManage（简历管理）、JobMatch（人岗匹配）、AIParse（AI 解析）、CompanyResource（企业资源）、MessageNotification（消息通知）、PersonalSettings（个人设置） |
| **学生** | 学生端页面（未在现有文件结构中体现，可能在后续开发中） |

### 2.6 依赖包清单

```json
{
  "dependencies": {
    "vue": "^3.3.0",
    "vue-router": "^4.2.0",
    "pinia": "^2.1.0",
    "element-plus": "^2.4.0",
    "@element-plus/icons-vue": "^2.3.0",
    "axios": "^1.6.0",
    "echarts": "^5.6.0",
    "dayjs": "^1.11.10",
    "js-cookie": "^3.0.5",
    "file-saver": "^2.0.5",
    "xlsx": "^0.18.5"
  }
}
```

---

## 三、开发指南

### 启动开发服务器

```bash
cd frontend
npm install
npm run dev   # 默认 http://localhost:5173
```

### 构建生产包

```bash
npm run build   # 输出到 dist/
npm run preview # 预览构建结果
```

### 规范与约定

**编码规范：**
- 文件名：PascalCase（组件/页面），kebab-case（路由路径）
- API 模块按业务领域分组（auth / user / company / job / resume ...）
- Store 统一 `useXxxStore` 命名模式
- 路由 `meta` 属性固定包含 `title` 和 `role`

**后续扩展建议：**
1. 引入 `pinia-plugin-persistedstate` 自动管理持久化
2. 引入 TypeScript 增强大型代码库的类型安全
3. 考虑 `unplugin-auto-import` 自动导入 Element Plus API
4. 小程序页面可作为独立 Vite 入口构建
