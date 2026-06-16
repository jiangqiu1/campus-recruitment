# 校企招聘与就业管理平台 - 项目文档

> 基于 Spring Boot 2.7.18 + Vue 3 + Element Plus 的全栈校园招聘系统

## 项目结构

```
校企项目/
├── backend/         # Spring Boot 后端（端口 8080，context-path: /api）
│   └── docs/        # 后端文档
├── frontend/        # Vue 3 前端（端口 5173，Vite 代理 /api → 8080）
│   └── docs/        # 前端文档
└── docs/            # 项目汇总文档（本目录）
```

## 📖 文档索引

### 后端文档（详见 `backend/docs/`）

| 文档 | 说明 |
|------|------|
| [总览](../backend/docs/overview.md) | 后端技术栈、目录结构、模块总览 |
| [数据库设计](../backend/docs/config/database.md) | 13张表完整结构 + ER关系 + MyBatis-Plus配置 |
| [安全配置](../backend/docs/config/security.md) | JWT / Redis / CORS / 文件上传 / 异常处理 |
| 用户管理 | [概览](../backend/docs/modules/auth-module.md) · [管理接口](../backend/docs/modules/user-module.md) |
| [企业管理](../backend/docs/modules/company-module.md) | 企业CRUD + 审核 |
| [岗位管理](../backend/docs/modules/job-module.md) | 岗位CRUD + 搜索 + 统计 |
| [班级管理](../backend/docs/modules/class-module.md) | 班级CRUD + 学生管理 |
| [简历管理](../backend/docs/modules/resume-module.md) | 简历CRUD + 上传 |
| [投递管理](../backend/docs/modules/delivery-module.md) | 状态流转：投递→查看→面试→录用 |
| [操作日志](../backend/docs/modules/operation-log-module.md) | 日志记录 + 清理 |
| [AI智能模块](../backend/docs/modules/ai-module.md) | 简历解析 / 智能评分 / 人岗匹配 |

### 前端文档（详见 `frontend/docs/`）

| 文档 | 说明 |
|------|------|
| [总览](../frontend/docs/overview.md) | 前端技术栈、目录结构 |
| [路由与布局](../frontend/docs/config/router.md) | 3个Layout + 路由守卫 + 权限控制 |
| [API与Store](../frontend/docs/config/api-and-store.md) | Axios封装 + Pinia状态管理 |
| [管理员模块](../frontend/docs/modules/admin-module.md) | 8个PC页面（仪表盘/用户/企业/班级/审核/日志/导出/设置） |
| [教师模块](../frontend/docs/modules/teacher-module.md) | 4个PC页面（工作台/班级/岗位/投递看板） |
| [HR模块](../frontend/docs/modules/hr-module.md) | 4个PC页面（简历/统计/分析/账号） |
| [小程序模块](../frontend/docs/modules/miniprogram-module.md) | 11个小程序页面 |

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
```

### 测试账号（密码均为 `123456`）
| 账号 | 角色 | role值 |
|------|------|--------|
| admin | 管理员 | 3 |
| T001 / T002 | 教师 | 1 |
| HR001 / HR002 | HR | 2 |
| S001 ~ S005 | 学生 | 0 |
