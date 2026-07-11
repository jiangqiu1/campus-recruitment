# 职业院校校企招聘与就业管理平台

> 基于 Spring Boot + Vue 3 + uni-app 的全栈应用，支持管理员、教师、企业HR、学生四种身份  
> 实训项目成果 · JavaWeb 开发框架课程

## 项目结构

```
campus-recruitment/
├── backend/               # 后端（Spring Boot 2.7 + MyBatis-Plus）
│   ├── src/
│   │   ├── main/java/com/recruit/
│   │   │   ├── controller/    # 14 个控制器
│   │   │   ├── service/       # 业务逻辑层
│   │   │   ├── entity/        # 13 个实体类
│   │   │   ├── mapper/        # 数据访问层
│   │   │   ├── config/        # 配置类
│   │   │   ├── interceptor/   # JWT 拦截器
│   │   │   └── utils/         # 工具类（JWT、AES、AI等）
│   │   ├── main/resources/
│   │   │   ├── application.yml       # ⚠️ 本地配置（已 gitignore，参考 application-example.yml）
│   │   │   ├── application-example.yml # 配置模板
│   │   │   └── mapper/               # MyBatis XML
│   │   └── sql/              # 数据库迁移脚本
│   ├── pom.xml
│   └── uploads/              # 文件上传目录
├── frontend/                # PC 管理后台（Vue 3 + Vite + Element Plus）
│   ├── src/
│   │   ├── views/pc/
│   │   │   ├── admin/       # 管理员端（11 个页面）
│   │   │   ├── teacher/     # 教师端（9 个页面）
│   │   │   └── hr/          # HR 端（8 个页面）
│   │   ├── api/             # Axios 接口封装
│   │   ├── router/          # 路由配置 + 权限守卫
│   │   ├── stores/          # Pinia 状态管理
│   │   └── components/      # 通用组件
│   ├── package.json
│   └── vite.config.js
├── job-miniprogram/         # 微信小程序（uni-app）
│   └── src/
│       ├── pages/student/   # 学生端
│       ├── pages/teacher/   # 教师端
│       └── pages/hr/        # HR 端
└── docs/                    # 文档
```

## 技术栈

### 后端
| 技术 | 说明 |
|------|------|
| Spring Boot 2.7.18 | 开发框架 |
| MyBatis-Plus 3.5.5 | ORM 持久层 |
| MySQL 8.0 | 关系型数据库 |
| Redis（Lettuce） | 缓存 + JWT 黑名单 |
| JWT（JJWT 0.11.5） | 无状态认证 |
| BCrypt | 密码加密 |
| AES | 敏感字段加密（手机号、身份证） |
| DeepSeek API | AI 简历评分 / 人岗匹配 / JD 解析 |

### 前端
| 技术 | 说明 |
|------|------|
| Vue 3（Composition API） | 前端框架 |
| Vite 5 | 构建工具 |
| Element Plus 2.4 | UI 组件库 |
| Pinia 2.1 | 状态管理 |
| Vue Router 4 | 路由管理 |
| Axios | HTTP 客户端 |
| ECharts 5 | 数据可视化 |
| uni-app | 微信小程序跨端框架 |

## 快速启动

### 环境要求
- JDK 17+、Maven 3.6+
- MySQL 8.0+、Redis 6+
- Node.js 18+、npm 9+

### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE campus_recruitment DEFAULT CHARSET utf8mb4"

# 导入初始表结构
mysql -u root -p campus_recruitment < backend/sql/init-db.sql

# 按时间顺序执行迁移脚本
mysql -u root -p campus_recruitment < backend/sql/migration_20260705_ai_analysis.sql
mysql -u root -p campus_recruitment < backend/sql/migration_20260706_v2.sql
mysql -u root -p campus_recruitment < backend/sql/migration_20260708_settings.sql
mysql -u root -p campus_recruitment < backend/sql/migration_20260708_usermessage.sql
mysql -u root -p campus_recruitment < backend/sql/migration-20260709-add-school-major.sql
mysql -u root -p campus_recruitment < backend/sql/migration-add-project-field.sql
```

### 2. 后端启动

复制 `application-example.yml` 为 `application.yml`，填写数据库密码和 AI API Key：

```bash
cp backend/src/main/resources/application-example.yml backend/src/main/resources/application.yml
# 编辑 application.yml，填入实际的 DB 密码、JWT 密钥、DeepSeek API Key
```

启动方式：

```bash
# Maven 直接启动
cd backend
mvn spring-boot:run

# 或打包后启动
mvn clean package -DskipTests
java -jar target/recruit-0.0.1-SNAPSHOT.jar
```

后端运行在 `http://localhost:8080/api`

### 3. 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`

### 4. 小程序启动

用 HBuilderX 打开 `job-miniprogram/` 目录，运行到微信开发者工具。

### 默认测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 123456 |
| 教师 | teacher | 123456 |
| HR | hr | 123456 |
| 学生 | S001 | 123456 |

## 功能概览

### 管理员端
- **数据大屏**: 用户统计、岗位统计、投递趋势、企业合作榜
- **用户管理**: 增删改查、角色分配、状态管理
- **企业管理**: 企业审核、合作等级管理
- **班级管理**: 班级创建、学生分配
- **岗位监管**: 全平台岗位监控、强制关闭
- **操作日志**: 审计日志查询
- **系统设置**: 平台参数配置

### 教师端
- **AI 岗位发布**: 粘贴 JD → AI 自动解析 → 关联企业 → 一键发布
- **AI 人岗匹配**: 选择岗位 + 班级 → AI 多维度匹配（技能/学历/经验/专业）→ 批量推送
- **AI 简历分析**: 对学生简历智能评分 + 改进建议
- **班级管理**: 班级成员管理、学生关联
- **投递看板**: 全班学生投递进度追踪
- **审批管理**: 审核 HR 岗位变更申请
- **企业资源库**: 合作企业信息查看

### HR 端
- **企业信息管理**: 企业资料维护
- **岗位管理**: 岗位发布、编辑、暂停、关闭、变更申请
- **简历评分**: AI 自动评分 + 批量筛选
- **简历管理**: 候选人列表、投递管理、面试安排
- **数据统计**: 投递漏斗、评分分布、岗位分析
- **账号管理**: 子账号管理

### 学生端（小程序）
- **岗位浏览**: 岗位列表、分类筛选、AI 推荐
- **投递管理**: 一键投递、进度追踪、面试通知
- **简历管理**: 在线编辑、项目经历、完整度检测
- **AI 匹配**: 查看与各岗位的匹配度详情
- **消息通知**: 投递反馈、面试邀请

## 技术亮点

### 1. JWT + Redis 黑名单
无状态 Token 认证 + Redis 黑名单实现退出即失效。前端 axios 拦截器统一处理 401 过期。

### 2. AI 集成（DeepSeek）
- **AI 人岗匹配**: 四维度评估（技能/学历/经验/专业契合），结构化 JSON 返回
- **AI 简历评分**: 自动打分 + 改进建议
- **AI JD 解析**: 粘贴岗位描述自动提取标题、薪资、地点、技能标签等

### 3. 安全加固
- 密码 BCrypt 不可逆加密
- 手机号、身份证 AES 加密存储
- 逻辑删除（@TableLogic）防止数据丢失

### 4. 三角色权限控制
- 路由级权限守卫
- 接口级角色校验
- 数据级隔离（HR 只看本企业数据）

## 数据库设计

13 张表，覆盖完整业务链路：

| 表名 | 说明 |
|------|------|
| sys_user | 系统用户（含角色：0学生/1教师/2HR/3管理员） |
| company | 企业信息（含审核状态、合作等级） |
| job | 岗位信息（含状态流转、AI关键词） |
| resume | 学生简历（含AI分析结果） |
| delivery | 投递记录（含状态机：待查看→已邀约→面试→录用/拒绝） |
| class | 班级信息 |
| student_class | 学生-班级关联 |
| job_match_record | 人岗匹配记录（含多维度分数） |
| resume_score_log | 简历评分记录 |
| ai_parse_log | AI 解析日志 |
| job_change_apply | 岗位变更申请 |
| user_message | 用户消息通知 |
| operation_log | 操作审计日志 |
| sys_setting | 系统设置 |

## 配置文件说明

**⚠️ 敏感配置不提交 Git**

`backend/src/main/resources/application.yml` 已被 `.gitignore` 排除，包含：
- 数据库密码
- JWT 签名密钥
- DeepSeek API Key

部署时请参考 `backend/src/main/resources/application-example.yml` 创建自己的配置。

## GitHub

项目地址：[https://github.com/jiangqiu1/campus-recruitment](https://github.com/jiangqiu1/campus-recruitment)

---

**最后更新**：2026-07-11
