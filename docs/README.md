# 校企招聘与就业管理平台 - 项目文档

> 基于 Spring Boot 2.7.18 + Vue 3 + Element Plus 的全栈校园招聘系统
> 本目录是项目唯一的文档基线（2026-09-30 收敛：原 `backend/docs/`、`frontend/docs/` 旧树已并入）

## 项目结构

```
校企项目/
├── backend/         # Spring Boot 后端（端口 8080，context-path: /api）
├── frontend/        # Vue 3 前端（端口 5173，Vite 代理 /api → 8080）
├── job-miniprogram/ # uni-app 微信小程序（学生/教师/HR 三端）
└── docs/            # 项目文档（本目录）
```

## 📖 文档索引

### 后端文档（docs/backend/）

| 文档 | 说明 |
|------|------|
| [00-后端总览](backend/00-后端总览.md) | 技术栈、目录结构、模块总览 |
| [01-认证模块](backend/01-认证模块/) | JWT 认证 · 原理/实现/认证流程详解 |
| [02-AI模块](backend/02-AI模块/) | DeepSeek + 智谱双模型 · Prompt 规范 · 学生 AI 助手 |
| [03-用户管理](backend/03-用户管理/) | 用户 CRUD 与角色 |
| [04-企业管理](backend/04-企业管理/) | 企业 CRUD 与审核 |
| [05-岗位管理](backend/05-岗位管理/) | 岗位 CRUD · 三层权限详解 |
| [06-简历管理](backend/06-简历管理/) | 简历 CRUD · 简历与 AI 解析详解 |
| [07-投递管理](backend/07-投递管理/) | 状态流转 · 状态机与消息联动 |
| [08-班级管理](backend/08-班级管理/) | 班级 CRUD · 权限链路详解 |
| [09-消息通知](backend/09-消息通知/) | 站内消息 |
| [10-统计看板](backend/10-统计看板/) | 三角色 Dashboard · 看板详解 |
| [11-系统支撑](backend/11-系统支撑/) | 文件上传、数据导出、操作日志、异常处理 |
| [12-数据库设计](backend/12-数据库设计.md) | 13 张表完整结构 + ER 关系（自旧文档树迁入） |
| [13-安全配置](backend/13-安全配置.md) | JWT/Redis/CORS/文件上传/异常（自旧文档树迁入） |

### 前端文档（docs/frontend/）

| 文档 | 说明 |
|------|------|
| [00-前端总览](frontend/00-前端总览.md) | 技术栈、目录结构 |
| [01-路由与权限守卫](frontend/01-路由与权限守卫.md) | 3 个 Layout + 路由守卫 + 权限控制 |
| [02-API请求层](frontend/02-API请求层.md) | Axios 封装 |
| [03-状态管理](frontend/03-状态管理.md) | Pinia |
| [04-布局与组件](frontend/04-布局与组件.md) | 布局与通用组件 |
| [05-业务页面](frontend/05-业务页面.md) | 三角色业务页面 |
| [06-Dashboard数据大屏详解](frontend/06-Dashboard数据大屏详解.md) | 图表页面实现 |
| [07-AI相关页面详解](frontend/07-AI相关页面详解.md) | AI 功能页面实现 |

### 其他

- [CHANGELOG](CHANGELOG.md) — 变更日志
- [系统体检报告](系统体检报告.md) — 全栈代码体检与修复记录
- [AI功能扩展方案-智谱接入](AI功能扩展方案-智谱接入.md) — 双模型接入方案（已实施，见 CHANGELOG 2026-09-30）

## 🚀 启动说明

### 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0
- Redis
- Node.js 18+

### 启动步骤
```bash
# 1. 启动 MySQL 和 Redis

# 2. 启动后端
cd backend
mvn spring-boot:run
# → http://localhost:8080/api

# 3. 启动前端
cd frontend
npm run dev
# → http://localhost:5173

# 4. 小程序：HBuilderX 打开 job-miniprogram/ 运行到微信开发者工具
```

### 测试账号（密码均为 `123456`）
| 账号 | 角色 | role值 |
|------|------|--------|
| admin | 管理员 | 3 |
| T001 / T002 | 教师 | 1 |
| HR001 / HR002 | HR | 2 |
| S001 ~ S005 | 学生 | 0 |
