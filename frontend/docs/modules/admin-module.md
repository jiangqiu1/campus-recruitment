# 前端管理员模块文档

> 视图: `src/views/pc/admin/` (8个页面)
> 布局: `src/layouts/AdminLayout.vue`
> 路由: `/admin/**`

---

## 一、原理文档

### 1. 模块职责
管理员后台是系统管理的核心控制台，提供用户、企业、班级、日志等全部管理功能。

### 2. 权限说明
- 所有管理员页面的路由 meta 均标记 `role: 'admin'`
- 路由守卫检测 `localStorage.getItem('userRole')` 必须为 `admin`
- 无权限访问时提示 `ElMessage.error('无权访问该页面')`

### 3. 页面关系

```
管理员后台
 ├── Dashboard.vue          ← 数据大屏（ECharts图表）
 ├── UserManage.vue         ← 用户CRUD + 禁用/启用/重置密码
 ├── CompanyManage.vue      ← 企业CRUD
 ├── ClassManage.vue        ← 班级CRUD
 ├── EnterpriseAudit.vue    ← 企业审核（待审核/通过/拒绝）
 ├── OperationLog.vue       ← 操作日志查询 + 清理
 ├── DataExport.vue         ← 数据导出（Blob/XHR下载）
 └── SystemSettings.vue     ← 系统设置（基础/安全/通知）
```

---

## 二、实现文档（函数级）

### 2.1 Dashboard.vue — 数据大屏

**文件**: `src/views/pc/admin/Dashboard.vue`

使用 ECharts 展示数据：
| 区域 | 内容 | 数据来源 |
|------|------|----------|
| 顶部统计卡片 | 用户数/企业数/岗位数/投递数 | `jobAPI.fetchJobStatistics()` |
| 折线图 | 近7天/30天投递趋势 | API 返回时间序列 |
| 饼图 | 投递状态分布 | API 返回状态统计数据 |
| 柱状图 | 热门岗位TOP10 | `jobAPI.fetchHotJobs(10)` |
| 表格 | 近期操作日志 | `operationLogAPI.getLogs({ recent: true })` |

关键逻辑：
```javascript
onMounted(() => {
  // 初始化图表
  initCharts()
  // 加载数据
  loadStatistics()
})

function initCharts() {
  // ECharts 实例化 + 配置项
}
```

### 2.2 UserManage.vue — 用户管理

**文件**: `src/views/pc/admin/UserManage.vue`

| 功能 | 触发方式 | 调用 API | 说明 |
|------|----------|----------|------|
| 查询用户列表 | 页面加载/搜索 | `userAPI.getUsers(params)` | 支持分页+关键字搜索 |
| 新增用户 | 对话框提交 | `userAPI.createUser(data)` | 表单验证 |
| 编辑用户 | 行内编辑/对话框 | `userAPI.updateUser(id, data)` | — |
| 删除用户 | 确认弹窗 | `userAPI.deleteUser(id)` | 软删除 (deleted=1) |
| 启用/禁用 | 开关组件 | `userAPI.updateUserStatus(id, status)` | 0=禁用, 1=启用 |
| 重置密码 | 对话框 | `userAPI.resetPassword(id, data)` | BCrypt加密 |

### 2.3 CompanyManage.vue — 企业管理

**文件**: `src/views/pc/admin/CompanyManage.vue`

| 功能 | 调用 API | 说明 |
|------|----------|------|
| 企业列表 | `companyAPI.getCompanies(params)` | 分页查询 |
| 新增企业 | `companyAPI.createCompany(data)` | — |
| 编辑企业 | `companyAPI.updateCompany(id, data)` | — |
| 删除企业 | `companyAPI.deleteCompany(id)` | 软删除 |
| 批量删除 | `companyAPI.batchDeleteCompanies(ids)` | 选择多条后批量操作 |

### 2.4 ClassManage.vue — 班级管理

**文件**: `src/views/pc/admin/ClassManage.vue`

| 功能 | 调用 API | 说明 |
|------|----------|------|
| 班级列表 | `classAPI.getClasses(params)` | 分页查询 |
| 新增班级 | `classAPI.createClass(data)` | 含教师选择 |
| 编辑班级 | `classAPI.updateClass(id, data)` | — |
| 删除班级 | `classAPI.deleteClass(id)` | — |
| 学生管理 | 弹窗式班级成员管理 | 查看/添加/移除学生 |

### 2.5 EnterpriseAudit.vue — 企业审核

**文件**: `src/views/pc/admin/EnterpriseAudit.vue`

| 功能 | 调用 API | 说明 |
|------|----------|------|
| 待审核列表 | `companyAPI.getCompanies({ status: 0 })` | 默认显示待审核 |
| 审核通过 | `companyAPI.approveCompany(id)` | status → 1 |
| 审核拒绝 | `companyAPI.rejectCompany(id)` | status → 2 |

### 2.6 OperationLog.vue — 操作日志

**文件**: `src/views/pc/admin/OperationLog.vue`

| 功能 | 调用 API | 说明 |
|------|----------|------|
| 日志列表 | `operationLogAPI.getLogs(params)` | 分页查询 |
| 按用户过滤 | `operationLogAPI.getLogs({ userId })` | — |
| 按类型过滤 | `operationLogAPI.getLogs({ operationType })` | — |
| 清理日志 | `operationLogAPI.cleanupLogs(beforeTime)` | 需确认弹窗 |
| 查看详情 | ElMessageBox 弹窗展示 | JSON格式化详情 |

### 2.7 DataExport.vue — 数据导出

**文件**: `src/views/pc/admin/DataExport.vue`

| 功能 | 说明 |
|------|------|
| 用户数据导出 | 使用 XHR/Blob 下载 CSV |
| 投递数据导出 | 按日期范围过滤后导出 |
| 岗位数据导出 | 导出岗位列表为 Excel |

注意：DataExport.vue 使用原生 XHR + Blob 下载，不走 Axios 拦截器。

### 2.8 SystemSettings.vue — 系统设置

**文件**: `src/views/pc/admin/SystemSettings.vue`

| Tab | 说明 | 调用 API |
|-----|------|----------|
| 基础设置 | 系统名称、Logo、联系方式 | `settingsAPI.saveBasic(data)` |
| 安全设置 | 密码策略、登录限制 | `settingsAPI.saveSecurity(data)` |
| 通知设置 | 邮件/短信通知开关 | `settingsAPI.saveNotification(data)` |

---

## 三、使用的 API 模块总览

| 页面 | 使用的 API 模块 |
|------|----------------|
| Dashboard | jobAPI, operationLogAPI |
| UserManage | userAPI |
| CompanyManage | companyAPI |
| ClassManage | classAPI |
| EnterpriseAudit | companyAPI |
| OperationLog | operationLogAPI |
| DataExport | 原生 XHR（不走 Axios）|
| SystemSettings | settingsAPI |

---

> **修改日志**: 首次全量梳理产出
