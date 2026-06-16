# 后端总览文档

> 职业院校校企招聘与就业管理平台 — 后端
> 
> 最后更新: 2026-06-08
> 框架: Spring Boot 2.7.18 + MyBatis-Plus 3.5.5 + MySQL 8.0 + Redis + JWT
> 启动端口: 8080, 上下文路径: `/api`

---

## 1. 项目概述

### 1.1 功能介绍
本平台后端为高校就业管理系统提供 RESTful API，支持以下核心业务：
- **用户认证**：JWT 登录/注册/登出，BCrypt 密码加密，Redis 黑名单
- **系统管理**：用户 CRUD、企业管理、班级管理、企业审核
- **招聘管理**：岗位发布、简历投递、简历评分、人岗匹配
- **AI 智能**：简历智能解析、人岗匹配记录、反馈日志
- **运营维护**：操作日志、系统设置、数据导出、文件上传

### 1.2 技术架构

```
┌────────────────────────────────────────────────┐
│              Controller 层 (REST API)           │
│  Auth/User/Company/Job/Resume/Delivery/Class   │
│  File/OperationLog/AiParse/JobMatch/ResumeScore│
└──────────────────┬─────────────────────────────┘
                   │
┌──────────────────▼─────────────────────────────┐
│              Service 层 (业务逻辑)               │
│  接口 + 实现 (impl/ 下 12 个 ServiceImpl)       │
└──────────────────┬─────────────────────────────┘
                   │
┌──────────────────▼─────────────────────────────┐
│            Mapper 层 (MyBatis-Plus)             │
│  13 个 Mapper 接口 + 13 个 Mapper XML          │
└──────────────────┬─────────────────────────────┘
                   │
┌──────────────────▼─────────────────────────────┐
│            Entity 层 (数据模型)                  │
│  13 个实体类 (SysUser/Company/Job/Resume/...)  │
└────────────────────────────────────────────────┘
```

## 2. 目录结构

```
backend/src/main/java/com/recruit/
├── RecruitmentApplication.java       # 启动类（MapperScan + SpringBoot）
├── config/                           # 配置类
│   ├── PasswordEncoderConfig.java    # BCrypt 密码编码器
│   ├── RedisConfig.java              # RedisTemplate 手动配置
│   └── WebConfig.java                # CORS + JWT拦截器注册
├── controller/                       # 控制器（11个）
│   ├── AuthController.java           # 认证（登录/注册/登出）
│   ├── UserController.java           # 用户信息
│   ├── CompanyController.java        # 企业信息
│   ├── JobController.java            # 岗位管理
│   ├── ResumeController.java         # 简历管理
│   ├── DeliveryController.java       # 投递管理
│   ├── ClassController.java          # 班级管理
│   ├── FileController.java           # 文件上传
│   ├── OperationLogController.java   # 操作日志
│   ├── AiParseController.java        # AI 解析
│   ├── JobMatchController.java       # 人岗匹配
│   └── ResumeScoreController.java    # 简历评分
├── dto/                              # 数据传输对象（8个）
│   ├── LoginRequest.java             # 登录请求
│   ├── LoginResponse.java            # 登录响应
│   ├── JobRequest.java               # 岗位请求
│   ├── DeliveryRequest.java          # 投递请求
│   ├── ResumeRequest.java            # 简历请求
│   ├── CompanyRequest.java           # 企业请求
│   ├── JobMatchRequest.java          # 匹配请求
│   └── PasswordChangeRequest.java    # 密码修改请求
├── entity/                           # 实体类（13个）
│   ├── SysUser.java                  # 系统用户
│   ├── Company.java                  # 企业
│   ├── Job.java                      # 岗位
│   ├── Resume.java                   # 简历
│   ├── Delivery.java                 # 投递记录
│   ├── Class.java                    # 班级
│   ├── StudentClass.java             # 学生班级关联
│   ├── JobChangeApply.java           # 岗位变更申请
│   ├── OperationLog.java             # 操作日志
│   ├── AiParseLog.java               # AI 解析日志
│   ├── AiFeedbackLog.java            # AI 反馈日志
│   ├── JobMatchRecord.java           # 人岗匹配记录
│   └── ResumeScoreLog.java           # 简历评分记录
├── exception/                        # 异常处理
│   ├── BusinessException.java        # 业务异常
│   └── GlobalExceptionHandler.java   # 全局异常处理器
├── interceptor/                      # 拦截器
│   └── JwtInterceptor.java           # JWT 认证拦截器
├── mapper/                           # Mapper 接口（13个）
├── service/                          # Service 接口（12个）
│   └── impl/                         # Service 实现（12个）
└── utils/                            # 工具类
    ├── JwtUtil.java                  # JWT 生成/解析
    ├── RedisUtil.java                # Redis 操作封装
    ├── AESUtil.java                  # AES 加解密
    └── Result.java                   # 统一响应对象
```

## 3. 请求与响应规范

### 统一响应格式
```json
{
  "code": 200,        // 200=成功, 400=业务错误, 401=未授权, 404=未找到, 500=服务器错误
  "message": "成功",
  "data": {}
}
```

### 状态码说明
| Code | 含义 | 场景 |
|------|------|------|
| 200  | 成功 | 正常业务响应 |
| 400  | 业务错误 | 参数校验失败、业务校验失败 (BusinessException) |
| 401  | 未授权 | Token 缺失/过期/无效、异地登录 |
| 403  | 无权限 | 角色权限不足 (RequireRole) |
| 404  | 资源不存在 | 用户/岗位/简历等不存在 |
| 500  | 服务器错误 | 未捕获异常 (GlobalExceptionHandler 兜底) |

## 4. 核心流程

### 4.1 登录流程
```
① 用户提交 (username, password, role)
    → POST /api/auth/login
    → LoginRequest DTO 接收
② 验证身份
    → userService.login()
        → BCrypt.matches(password, user.password)
        → 校验 role 是否匹配
③ 生成 JWT Token
    → jwtUtil.generateToken(userId, username, roleStr)
        → Jwts.builder() + HMAC-SHA 签名
        → 7天有效期 (expiration: 604800000ms)
④ Token 存储到 Redis
    → redisUtil.setWithExpire("token:"+userId, token, 7天)
    → 实现单点登录 (Redis 中 Token 被覆盖 = 异地登录)
⑤ 返回 LoginResponse
    → token (带 Bearer 前缀), userId, username, realName, role, avatarUrl
```

### 4.2 请求鉴权流程
```
① 客户端请求携带 Authorization: Bearer <token>
② JwtInterceptor.preHandle() 拦截
    1. 取 header "Authorization"
    2. 校验 prefix "Bearer "
    3. jwtUtil.getClaimsFromToken() 解析
    4. jwtUtil.isTokenExpired() 检查过期
    5. redisUtil.get("token:"+userId) 检查单点登录
    6. request.setAttribute("userId", ...) 传递用户上下文
③ 放行 → Controller 处理
    → 可通过 request.getAttribute("userId") 获取当前用户
```

### 4.3 文件上传流程
```
① 客户端 POST /api/files/upload + multipart/form-data
    → FileController.upload()
② 校验文件大小 (max-file-size: 10MB, max-request-size: 50MB)
③ 生成唯一文件名 (UUID + 原始扩展名)
④ 保存到 file.upload-path 目录 (${user.home}/uploads/)
⑤ 返回文件访问URL (file.base-url + 文件名)
```

## 5. 核心配置

### 5.1 数据库 (MySQL 8.0)
- 地址: localhost:3306
- 库名: campus_recruitment
- 连接池: HikariCP (最大10, 最小5)
- 逻辑删除: `deleted` 字段 (1=已删除, 0=正常)

### 5.2 Redis
- 地址: localhost:6379
- DB: 0
- 连接池: Lettuce Pool (最大8)
- 超时: 6s

### 5.3 JWT
- 签名密钥: `recruitment-platform-secret-key-2025`
- 有效期: 7天 (604800000ms)
- Header: `Authorization`
- Prefix: `Bearer `

## 6. 数据库表清单

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| sys_user | 系统用户 | username, password(BCrypt), role(1=学生/2=教师/3=管理员/4=HR), phone(AES), id_card(AES) |
| company | 企业 | name, address, status(0=待审核/1=通过/2=拒绝) |
| job | 岗位 | company_id, title, description, status(0=关闭/1=开放) |
| resume | 简历 | student_id, content, file_url, status |
| delivery | 投递 | student_id, job_id, status |
| class | 班级 | name, grade, teacher_id |
| student_class | 学生班级关联 | student_id, class_id |
| job_change_apply | 岗位变更申请 | job_id, applicant_id, status |
| operation_log | 操作日志 | operator, action, target, detail |
| ai_parse_log | AI解析日志 | resume_id, parse_content, status |
| ai_feedback_log | AI反馈日志 | parse_log_id, feedback, rating |
| job_match_record | 人岗匹配 | job_id, resume_id, score |
| resume_score_log | 简历评分 | resume_id, score, dimension_scores |

## 7. 认证与授权设计

### 7.1 角色体系
| 角色 | ID | 访问范围 |
|------|-----|----------|
| 学生 (student) | 1 | 前端小程序 |
| 教师 (teacher) | 2 | /teacher/** |
| 管理员 (admin) | 3 | /admin/** |
| HR | 4 | /hr/** |

### 7.2 安全措施
1. **密码**: BCrypt 加密存储
2. **敏感字段**: 手机号、身份证使用 AES 加密
3. **Token**: JWT 签名防篡改 + 7天过期
4. **单点登录**: Redis 记录最新 Token，旧 Token 自动失效
5. **登出**: Token 加入 Redis 黑名单
6. **拦截器**: 所有 `/api/**` 路径需认证（`/auth/**` 除外）

## 8. 模块文档索引

| 模块 | 文档路径 | 包含 Controller | 包含 Service |
|------|----------|-----------------|--------------|
| 认证模块 | [modules/auth-module.md](modules/auth-module.md) | AuthController | UserService |
| 用户管理 | [modules/user-module.md](modules/user-module.md) | UserController | UserService |
| 企业管理 | [modules/company-module.md](modules/company-module.md) | CompanyController | CompanyService |
| 岗位管理 | [modules/job-module.md](modules/job-module.md) | JobController | JobService, JobChangeApplyService |
| 简历管理 | [modules/resume-module.md](modules/resume-module.md) | ResumeController | ResumeService |
| 投递管理 | [modules/delivery-module.md](modules/delivery-module.md) | DeliveryController | DeliveryService |
| 班级管理 | [modules/class-module.md](modules/class-module.md) | ClassController | ClassService, StudentClassService |
| AI 模块 | [modules/ai-module.md](modules/ai-module.md) | AiParseController, JobMatchController, ResumeScoreController | AiParseLogService, JobMatchRecordService, ResumeScoreLogService |
| 操作日志 | [modules/operation-log-module.md](modules/operation-log-module.md) | OperationLogController | OperationLogService |
| 配置详解 | [config/](config/) | — | — |

---

> **修改日志**: 本文档为项目全量梳理的产物，任何代码修改需同步更新本文档。
