# 前端 API 层与状态管理文档

> 文件: `src/api/index.js` / `src/stores/*.js`
> 最后更新: 2026-06-08

---

## 一、API 层文档

### 1.1 原理

前端使用 **Axios** 封装统一的 HTTP 请求层，集中处理：
1. **请求拦截**：自动携带 JWT Token（从 localStorage 读取）
2. **响应拦截**：统一错误处理（401 自动跳转登录页）
3. **API 模块化**：按业务模块分组导出（auth/user/company/job/resume/delivery/...）

**架构**：
```
Vue Components / Pinia Stores
         │
         ▼
    API 模块 (api/index.js)
         │
    request 实例 (Axios)
         │
   ┌─────┴─────┐
   │ 请求拦截器   │
   │ 响应拦截器   │
   └─────┬─────┘
         │
   后端 API (/api/**)
```

### 1.2 Axios 实例配置

**文件**: `src/api/index.js` (第1-30行)

| 行号 | 配置项 | 值 | 说明 |
|------|--------|-----|------|
| 4 | `baseURL` | `/api` | 代理到 Spring Boot 后端 |
| 5 | `timeout` | 15000ms | 请求超时 |
| 8-16 | 请求拦截器 | 从 localStorage 取 token，添加到 Authorization header | 自动携带认证信息 |
| 19-38 | 响应拦截器 | 检查 res.code !== 200 时 ElMessage 报错 | 统一错误处理 |

### 1.3 API 模块完整清单

**文件**: `src/api/index.js` (第40-205行)

| 模块名 | 变量名 | 方法数 | 基础路径 |
|--------|--------|--------|----------|
| 认证 | `authAPI` | 5 | `/auth/**` |
| 用户 | `userAPI` | 8 | `/admin/users/**` + `/user/**` |
| 企业 | `companyAPI` | 9 | `/admin/companies/**` + `/companies/**` |
| 岗位 | `jobAPI` | 13 | `/jobs/**` + `/hr/job-analysis` |
| 简历 | `resumeAPI` | 11 | `/resumes/**` + `/files/upload` |
| 投递 | `deliveryAPI` | 5 | `/deliveries/**` |
| 班级 | `classAPI` | 5 | `/admin/classes/**` |
| 人岗匹配 | `jobMatchAPI` | 5 | `/job-matches/**` |
| 简历评分 | `resumeScoreAPI` | 7 | `/resume-scores/**` |
| AI解析 | `aiParseAPI` | 3 | `/ai-parse-logs/**` |
| 操作日志 | `operationLogAPI` | 3 | `/operation-logs/**` |
| 系统设置 | `settingsAPI` | 4 | `/settings/**` |

### 1.4 API 调用示例

```javascript
// 在 Vue 组件中
import { authAPI, jobAPI } from '@/api'

// 登录
const res = await authAPI.login({ username: 'admin', password: '123456', role: 3 })
// res = { code: 200, message: "成功", data: { token, userId, ... } }

// 获取岗位列表
const res = await jobAPI.getJobs({ page: 1, size: 10 })
// res.data = { records: [...], total: 100 }

// 创建岗位
const res = await jobAPI.createJob({ title: '前端工程师', companyId: 1, ... })
```

### 1.5 响应拦截器处理清单

| 响应状态 | 处理方式 | 用户提示 |
|----------|----------|----------|
| `res.code === 200` | 返回 `res` | — |
| `res.code !== 200` | reject + ElMessage | res.message |
| HTTP 401 | 清空 localStorage + 跳转 /login | "登录已过期，请重新登录" |
| HTTP 403 | ElMessage | "没有权限访问" |
| 网络错误 | ElMessage | "网络连接失败" |
| 其他错误 | ElMessage | error.response.data.message 或 "服务器错误" |

---

## 二、Pinia Store 文档

### 2.1 原理

使用 **Pinia**（Vue 3 官方状态管理）管理全局状态:
- **user.js** — 用户登录状态 + Token 管理
- **app.js** — 应用 UI 状态（侧边栏折叠）
- **resume.js** — 简历管理状态（分页查询）
- **job.js** — 岗位搜索状态（搜索条件 + 分页）

### 2.2 user.js — 用户 Store

**文件**: `src/stores/user.js`

| 行号 | State | 类型 | 说明 |
|------|-------|------|------|
| ~8 | `token` | String | JWT Token |
| ~9 | `userInfo` | Object | 用户信息 (id, username, realName, role, avatarUrl) |
| ~10 | `role` | String | 用户角色 |
| ~11 | `isLoggedIn` | Boolean | 是否已登录 |

| 行号 | Action | 功能 |
|------|--------|------|
| ~20 | `login(credentials)` | 调用 authAPI.login → 存储 Token + 用户信息到 localStorage |
| ~35 | `logout()` | 清除 localStorage + 重置状态 |
| ~45 | `fetchUserInfo()` | 通过 Token 获取当前用户信息 |
| ~55 | `updateUserInfo(data)` | 更新用户信息 |

### 2.3 app.js — 应用 Store

**文件**: `src/stores/app.js`

| 行号 | State | 类型 | 说明 |
|------|-------|------|------|
| ~6 | `sidebarCollapsed` | Boolean | 侧边栏是否折叠 |

| 行号 | Action | 功能 |
|------|--------|------|
| ~12 | `toggleSidebar()` | 切换侧边栏折叠状态 |

### 2.4 resume.js — 简历 Store

**文件**: `src/stores/resume.js`

| 行号 | State | 类型 | 说明 |
|------|-------|------|------|
| ~8 | `resumeList` | Array | 简历列表数据 |
| ~9 | `total` | Number | 总记录数 |
| ~10 | `currentPage` | Number | 当前页码 |
| ~11 | `pageSize` | Number | 每页条数 |
| ~12 | `searchKeyword` | String | 搜索关键词 |
| ~13 | `filter` | Object | 筛选条件 |

| 行号 | Action | 功能 |
|------|--------|------|
| ~25 | `fetchResumes()` | 调用 resumeAPI.getResumes(params) |
| ~35 | `deleteResume(id)` | 调用 resumeAPI.deleteResume(id) |
| ~40 | `search(keyword)` | 设置关键词 + 重新查询 |

### 2.5 job.js — 岗位 Store

**文件**: `src/stores/job.js`

| 行号 | State | 类型 | 说明 |
|------|-------|------|------|
| ~8 | `jobList` | Array | 岗位列表数据 |
| ~9 | `total` | Number | 总记录数 |
| ~10 | `searchParams` | Object | 搜索条件 (keyword, location, type, salaryRange) |
| ~11 | `currentPage` | Number | 当前页码 |

| 行号 | Action | 功能 |
|------|--------|------|
| ~22 | `fetchJobs()` | 调用 jobAPI.getJobs(params) |
| ~32 | `searchJobs(params)` | 搜索岗位 |
| ~38 | `clearSearch()` | 清空搜索条件 |

---

## 三、关键数据流

### 3.1 登录数据流

```
Login.vue
  → userStore.login({ username, password, role })
    → authAPI.login(data)
      → request.post('/auth/login', data)
        → 后端验证 → 返回 { token, userId, ... }
      ← 存储到 localStorage: token, userRole, userId, realName
    → router.push('/admin/dashboard') 或相应角色首页
```

### 3.2 请求鉴权数据流

```
Component / Store 调用 API
  → request(config) — Axios 实例
    → 请求拦截器: config.headers.Authorization = `Bearer ${token}`
    → 后端 JwtInterceptor 校验
    → 响应拦截器: 401 → 清空登录状态 + 跳转 /login
  → Component/Store 收到 data 或 error
```

---

> **修改日志**: 首次全量梳理产出
