# 变更日志

## 2026-09-30

### 新增：AI 双模型接入（DeepSeek + 智谱 GLM）+ 学生 AI 助手

**后端**：
- `AiService` 双 provider 改造：新增 `config/AiProperties`（`ai.default-provider` + `ai.providers.{deepseek,glm}`），旧键 `ai.api.*` 废弃；某通道 key 缺失自动降级 mock
- 每次 AI 调用写一条 `ai_parse_log`（provider/task_name/latency_ms/user_id/mock_flag），作为毕设多模型对比实验数据源；迁移脚本 `backend/sql/migration-20260930-ai-provider.sql`（**需执行**）
- 新增 `AiService.generateInterviewQuestions` / `evaluateAnswer`（模拟面试）
- 新增 `AiAssistantController`（`/ai-assistant`，学生向）：`POST /resume-review`（简历诊断）、`POST /interview/questions`、`POST /interview/evaluate`；`BaseController` 新增 `requireStudent()`
- 简历诊断防重复缓存：`ResumeService.analyzeWithCache`，aiAnalysis 记录 `analyzedAt`，简历未变更直接返回缓存（`cached:true`），`force=true` 强制重跑；教师 `analyze-resume` 同步接入

**小程序**：
- `request.js` 新增 `aiAssistantAPI`
- `resume.vue` 顶栏新增「AI 诊断」按钮（命中缓存秒回）
- 新页面 `interview-practice.vue`（选投递岗位 → AI 出 5 题 → 逐题点评 → 小结），`pages.json` 注册 + home 快捷入口

**PC 端**：
- `ResumeDetailDialog` 教师模式新增「AI 诊断/重新诊断」+「强制重新分析」（带 force）
- `aiParseAPI.analyzeResume(studentId, force)` 透传 force

**待办**：执行 SQL 迁移；智谱 key 到位后填 `providers.glm.key` 并把 `default-provider` 改为 `glm`；建议作废已泄露到 GitHub 的 DeepSeek key。

详见 `docs/backend/02-AI模块/双模型与学生AI助手-20260930.md`。

## 2026-06-08

### 修复：前端登录失败 + 权限校验错误

**问题现象：** 三种角色登录均提示"无权访问"，HR登录后不跳转。

**根因分析：**

1. **登录数据解析不匹配** — 后端返回扁平结构 `{ token, userId, username, realName, role, avatarUrl }`，但前端 Login.vue 使用 `const { token, user } = res.data` 解构，`user` 为 `undefined`，导致后续取 `user.role` 时报错。

2. **路由守卫角色类型不匹配** — `localStorage.setItem('userRole', data.role)` 存的是数字（3/1/2），但路由 `meta.role` 是字符串（'admin'/'teacher'/'hr'），守卫比较 `to.meta.role !== userRole` 永远成立。

3. **userStore 变量名错误** — `response.data.message` 中的 `response` 是未定义变量（应为 `res`），涉及 login/updateUserInfo/changePassword 三处。

**修复文件：**

| 文件 | 修复内容 |
|------|----------|
| `frontend/src/views/Login.vue` | 扁平结构取数；数字角色映射为字符串（`{1:'teacher',2:'hr',3:'admin'}`） |
| `frontend/src/stores/user.js` | 扁平字段组装为 userInfo；login 增加 role 参数；`response`→`res` 变量名修正 |

### 待修复问题
- [ ] 教师端 9 个 Vue 文件仍有 `axios` 直接调用（未走 API 模块封装）
- [ ] 教师端路由合并（`/teacher` 与旧目录）
- [ ] Mock 数据清理
- [ ] ECharts 图表组件化
- [ ] 数据导出功能

## 2026-06-09 ~ 2026-06-15

### 新增：三个 Dashboard 接真实统计 API

| Dashboard | 功能 |
|-----------|------|
| PC 管理员 → 数据大屏 | 概览统计卡片（用户数/企业数/岗位数/投递数）+ 近7日投递趋势图 + 热门岗位 Top5 + 最近动态 |
| 小程序教师端 → 工作台 | 统计卡片（班级数/学生数/岗位数/投递数）+ 投递趋势柱状图 + 就业分布饼图 |
| 小程序 HR 端 → 工作台 | 统计卡片（岗位数/简历数/面试数）+ 投递趋势折线图 + 评分分布柱状图 |

**后端新增 Controller：**
- `StatisticsController.java` — 6 个统计端点（总览/教师/HR/投递趋势/就业分布/评分分布/热门岗位）
- `DataExportController.java` — 6 种导出 CSV（学生/简历/投递/企业/岗位/操作日志）
- `JobChangeApplyController.java` — 岗位变更申请 CRUD 及审核

### 修复：企业审核页面全链路打通
- 后端 `CompanyController` 新增 `GET /companies?status=X` + `PUT /{id}/approve` + `PUT /{id}/reject`
- 前端 `EnterpriseAudit.vue` 去掉 mock 数据，对接真实 API
- 数据库 `company` 表新增 `status` 字段（0=待审核/1=通过/2=拒绝）

### 修复：用户管理列表 & 班级删除
- `UserManage.vue` — 修复 `el-switch` @change 误触导致全部账号被禁用
- `ClassController.java` — 去除 UTF-8 BOM 编码 + `deleteById()`→`removeById()` 修复逻辑删除

### 修复：UserManage.vue 中文乱码
- 文件含 UTF-8 BOM 导致 Vite 编译报错，去掉 BOM 后发现中文已损坏
- 整个文件重写（修复全部中文 label、message、表单验证规则）
- `toggleStatus` 函数补充缺失的 `userAPI.updateStatus()` API 调用

### 修复：退出登录红色警告
- 前端 `authAPI.logout()` 未传 body，后端 `@RequestBody Map` 默认 required=true 报错
- 修复：后端改为 `@RequestBody(required = false)`，前端传空 body `{}`

### 当前功能清单（PC 管理后台）
- ✅ 数据大屏（真实 ECharts 图表）
- ✅ 用户管理（CRUD + 状态开关）
- ✅ 企业管理（CRUD）
- ✅ 班级管理（CRUD）
- ✅ 企业审核（通过/拒绝 + 岗位变更审批）
- ✅ 操作日志（查看）
- ✅ 数据导出（CSV 下载）
- ✅ 系统设置（基本配置）
- ✅ 登录/退出/角色鉴权
