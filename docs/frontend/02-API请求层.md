# API 请求层

> 定位：`api/index.js`（264 行）。记录 axios 实例封装、拦截器逻辑与 17 个 API 模块。

## 1. axios 实例（4-7 行）

```js
const request = axios.create({
  baseURL: '/api',      // 由 vite 代理转发到后端
  timeout: 15000        // 15 秒
})
```

## 2. 请求拦截器（9-18 行）

自动从 localStorage 读 token，注入 `Authorization: Bearer {token}` 头。

## 3. 响应拦截器（20-51 行）

| 场景 | 处理 |
|------|------|
| blob 响应（文件下载） | 直接返回 `response.data`，不检查 code |
| `code !== 200` | ElMessage 错误 + `Promise.reject` |
| 401 | 若非登出流程（`window.__isLoggingOut`），清 localStorage + 跳 `/login` |
| 403 | ElMessage"没有权限访问" |
| 网络错误（无 response） | ElMessage"网络连接失败" |

**登出防抖**：`logout()` 前设置 `window.__isLoggingOut = true`，避免登出接口的 401 触发重复弹窗跳转。

## 4. 17 个 API 模块

| 模块 | 主要接口 | 对应后端 Controller |
|------|---------|-------------------|
| authAPI | login/register/logout/changePassword/getProfile/updateProfile | AuthController |
| userAPI | 用户 CRUD/批量删除/状态/重置密码 | UserController |
| companyAPI | 企业 CRUD/审核/合作等级 | CompanyController |
| companyAccountAPI | 企业子账号 CRUD | CompanyAccountController |
| jobAPI | 岗位 CRUD/发布/关闭/暂停/搜索/推荐/统计 | JobController |
| resumeAPI | 简历 CRUD/上传/按学生查 | ResumeController |
| deliveryAPI | 投递/状态/面试安排/统计 | DeliveryController |
| classAPI | 班级 CRUD/学生关联/批量 | ClassController |
| jobMatchAPI | 人岗匹配生成/推送/删除 | JobMatchController |
| resumeScoreAPI | 简历评分/批量/维度/分布 | ResumeScoreController |
| aiParseAPI | AI 解析日志/简历分析/岗位解析 | AiParseController |
| operationLogAPI | 操作日志查询/删除/清理 | OperationLogController |
| settingsAPI | 系统设置读写 | SettingsController |
| statisticsAPI | 各类 Dashboard 统计 | StatisticsController |
| jobChangeAPI | 岗位变更申请/审批 | JobChangeApplyController |
| dataExportAPI | 数据导出（blob） | DataExportController |
| userMessageAPI | 通用消息/未读/已读 | UserMessageController |

## 5. 特殊超时

`jobMatchAPI.batchGenerateMatches`（178 行）设置了 `timeout: 120000`（批量匹配需串行多次调 AI）。

## 6. 已知问题

1. ~~resumeAPI.uploadResume 忽略 studentId~~ ✅ 已修复（P1-7）：改调 `/resumes/upload-pdf` 并把 `studentId` 放进 `params`。
2. ~~接口不对齐~~ ✅ 已修复（P2-9）：删除 13 个无引用的死 API 方法（`checkToken`、`batchDeleteUsers`、`batchDeleteJobs`、`updateJobStatus`、`fetchHotJobs`、`fetchJobStatistics`、`getJobAnalysis`、`updateResume`、`batchDeleteResumes`、`updateResumeStatus`、`searchResumes`、`createDelivery`、`deleteParseLog`）与 `user.js` 的 `checkToken` 死链；真实缺口 `classAPI.batchDeleteClasses`（PC admin 班级管理在用）已由后端补 `POST /classes/batch-delete`。残留的 `views/miniprogram/` 页面引用不在 PC 路由中，暂不处理（该批页面未被挂载；已于 2026-09-30 整体删除）。
3. **code 约定冗余**：拦截器对 `code !== 200` 已 reject，因此各页面 `if (res.code === 200) ... else ...` 的 else 分支实际不可达。
