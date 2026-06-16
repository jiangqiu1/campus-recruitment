# 前端小程序模块文档

> 视图: `src/views/miniprogram/` (11个页面)
> 说明: 小程序页面不参与 PC 端路由，独立的小程序界面

---

## 一、原理文档

### 1. 模块职责
小程序端提供移动化的轻量操作界面，面向教师和 HR 移动办公场景。

### 2. 架构说明
- 小程序页面与 PC 后台**共享同一个 Vue 项目**，通过不同的路由入口区分
- 小程序页面设计为**移动端自适应**布局，使用 Element Plus 响应式组件
- 不参与 PC 端路由守护，通过不同的导航入口访问

### 3. 页面清单

```
miniprogram/teacher/          (7个页面)
 ├── Dashboard.vue            ← 教师工作台（移动版）
 ├── AIParse.vue              ← AI简历解析
 ├── JobMatch.vue             ← 人岗匹配
 ├── ResumeManage.vue         ← 简历管理
 ├── CompanyResource.vue      ← 企业资源
 ├── MessageNotification.vue  ← 消息通知
 └── PersonalSettings.vue     ← 个人设置

miniprogram/hr/               (4个页面)
 ├── Dashboard.vue            ← HR工作台（移动版）
 ├── CompanyProfile.vue       ← 企业信息
 ├── JobManage.vue            ← 岗位管理
 └── ResumeScore.vue          ← 简历评分
```

---

## 二、实现文档（函数级）

### 2.1 教师小程序页面

**Dashboard.vue** — `miniprogram/teacher/Dashboard.vue`
- 移动端适配的概览页面
- 展示待办事项、近期动态
- 包含快捷入口（AI解析、简历评分等）

**AIParse.vue** — `miniprogram/teacher/AIParse.vue`
- 调用 `aiParseAPI.parseResume(resumeId)` 进行AI解析
- 展示解析结果（结构化信息）
- 支持提交反馈到 `aiParseAPI.submitFeedback(id, data)`

**JobMatch.vue** — `miniprogram/teacher/JobMatch.vue`
- 调用 `jobMatchAPI.getMatches(params)` 查看匹配列表
- 调用 `jobMatchAPI.generateMatch(data)` 触发新匹配
- 展示匹配分数和详情

**ResumeManage.vue** — `miniprogram/teacher/ResumeManage.vue`
- 调用 `resumeAPI.getResumes(params)` 查看学生简历
- 调用 `resumeAPI.getResumeDetail(id)` 查看详情
- 支持 PDF 预览

**CompanyResource.vue** — `miniprogram/teacher/CompanyResource.vue`
- 调用 `companyAPI.getCompanies(params)` 查看企业资源
- 查看企业详情和岗位需求

**MessageNotification.vue** — `miniprogram/teacher/MessageNotification.vue`
- 查看通知列表（可结合 operationLogAPI 或独立通知接口）
- 标记已读/未读

**PersonalSettings.vue** — `miniprogram/teacher/PersonalSettings.vue`
- 个人资料修改
- 调用 `userAPI.updateUserInfo(data)` 更新信息
- 调用 `authAPI.changePassword(data)` 修改密码

### 2.2 HR 小程序页面

**Dashboard.vue** — `miniprogram/hr/Dashboard.vue`
- HR 移动端工作台
- 投递统计概览
- 快捷操作入口

**CompanyProfile.vue** — `miniprogram/hr/CompanyProfile.vue`
- 调用 `companyAPI.getCompanyProfile(companyId)` 查看企业信息
- 调用 `companyAPI.updateCompany(id, data)` 编辑企业信息

**JobManage.vue** — `miniprogram/hr/JobManage.vue`
- 调用 `jobAPI.getJobsByCompany(companyId)` 查看岗位列表
- 调用 `jobAPI.createJob(data)` 新增岗位
- 调用 `jobAPI.updateJobStatus(id, status)` 开关岗位

**ResumeScore.vue** — `miniprogram/hr/ResumeScore.vue`
- 调用 `resumeScoreAPI.getScores(params)` 查看评分记录
- 调用 `resumeScoreAPI.scoreResume(data)` 单份评分
- 调用 `resumeScoreAPI.batchScoreResumes(jobId)` 批量评分

---

## 三、与 PC 端共享资源

| 资源类型 | 小程序 | PC端 | 说明 |
|----------|--------|------|------|
| API 模块 | 共享 `api/index.js` | 共享 | 同一套 API 封装 |
| Pinia Store | 共享 | 共享 | 同一套状态管理 |
| Layout | 不适用 | 使用 | 小程序无侧边栏布局 |
| Router | 独立路由 | 独立 | 不同入口 |
| 认证 | 共享 localStorage | 共享 | 同一 Token |

---

> **修改日志**: 首次全量梳理产出
