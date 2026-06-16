# 认证模块文档

> 模块路径: `com.recruit.controller.AuthController` + `com.recruit.utils.JwtUtil` + `com.recruit.interceptor.JwtInterceptor`
> 涉及文件: 4个 Controller / 3个工具/配置 / 1个 Entity / 2个 DTO

---

## 一、原理文档

### 1. 模块职责
用户认证、身份校验、Token 管理，是系统安全的基础模块。

### 2. 架构设计

```
客户端 (Browser/App)
    │
    ├─ POST /api/auth/login ──────────▶ AuthController.login()
    │                                      │
    │                                      ├─ userService.login() [BCrypt验证]
    │                                      ├─ jwtUtil.generateToken() [JWT签发]
    │                                      └─ redisUtil.setWithExpire() [Redis缓存]
    │
    ├─ 后续请求 ──▶ JwtInterceptor.preHandle()
    │                  │
    │                  ├─ Token提取 + 前缀校验
    │                  ├─ jwtUtil.getClaimsFromToken() [解析]
    │                  ├─ 过期检查
    │                  ├─ Redis单点登录校验
    │                  └─ request.setAttribute() [传递用户上下文]
    │
    └─ POST /api/auth/logout ──────────▶ AuthController.logout()
                                           └─ Redis黑名单写入
```

### 3. 关键设计决策

| 决策 | 说明 |
|------|------|
| **JWT 7天有效期** | 平衡安全性与用户体验，7天后需重新登录 |
| **Redis 黑名单登出** | 登出后 Token 加入黑名单，黑名单Key = `blacklist:<token>`，过期时间=Token剩余有效期 |
| **Redis 单点登录** | Redis 存储 Key=`token:<userId>`，最新 Token 覆盖旧 Token，旧 Token 被判定为"已在其他地方登录" |
| **BCrypt 密码** | 不可逆加密，即使数据库泄露也无法还原密码 |
| **AES 敏感字段** | 手机号、身份证等用 AES 加密存储，Controller 中解密返回 |

### 4. DTO 说明

#### LoginRequest
```java
public class LoginRequest {
    private String username;   // 用户名
    private String password;   // 密码
    private Integer role;      // 角色
}
```

#### LoginResponse
```java
public class LoginResponse {
    private String token;      // JWT（带 Bearer 前缀）
    private Long userId;       // 用户ID
    private String username;   // 用户名
    private String realName;   // 真实姓名
    private Integer role;      // 角色
    private String avatarUrl;  // 头像URL
}
```

---

## 二、实现文档（函数级）

### 2.1 AuthController — `/auth/**`

**文件**: `controller/AuthController.java` (156行)

| 行号 | 方法 | HTTP | 路径 | 参数 | 功能 |
|------|------|------|------|------|------|
| 42-69 | `login()` | POST | `/auth/login` | @RequestBody LoginRequest | 用户登录，验证身份后生成 JWT+Redis 缓存 |
| 75-96 | `register()` | POST | `/auth/register` | @RequestBody LoginRequest | 用户注册，检查用户名唯一性 |
| 100-120 | `logout()` | POST | `/auth/logout` | @RequestBody Map<String,String> token | 登出，Token 加入 Redis 黑名单 |
| 124-155 | `getCurrentUser()` | GET | `/auth/me` | @RequestParam String token | 获取当前登录用户信息 |

### 2.2 JwtInterceptor — 请求拦截

**文件**: `interceptor/JwtInterceptor.java` (115行)

| 行号 | 逻辑 | 说明 |
|------|------|------|
| 31-34 | `preHandle()` 入口 | 实现 HandlerInterceptor 接口 |
| 37-44 | Token 提取+前缀校验 | request.getHeader(header), 校验 Bearer 前缀 |
| 47-55 | Claims 解析 | jwtUtil.getClaimsFromToken(token) |
| 58-66 | 过期检查 | jwtUtil.isTokenExpired(token) |
| 69-78 | Redis 单点登录校验 | redisUtil.get("token:"+userId) 与当前 Token 对比 |
| 81-85 | 设置请求属性 | request.setAttribute("userId"/"username"/"role") |

### 2.3 JwtUtil — JWT 工具

**文件**: `utils/JwtUtil.java` (125行)

| 行号 | 方法 | 功能 |
|------|------|------|
| 33-36 | `getSigningKey()` | HMAC-SHA 密钥生成 |
| 40-52 | `generateToken(userId, username, role)` | 生成 JWT（含 claims + 签名 + 过期时间） |
| 56-66 | `getClaimsFromToken(token)` | 解析 JWT 获取 Claims |
| 70-76 | `getUserIdFromToken(token)` | 从 Claims 提取 userId |
| 80-85 | `getUsernameFromToken(token)` | 从 Claims 提取 username |
| 89-94 | `getRoleFromToken(token)` | 从 Claims 提取 role |
| 98-103 | `isTokenExpired(token)` | 检查 Token 是否过期 |
| 107-112 | `getExpirationDateFromToken(token)` | 获取 Token 过期时间 |

### 2.4 RedisUtil — Redis 操作

**文件**: `utils/RedisUtil.java`

| 行号 | 方法 | 功能 |
|------|------|------|
| ~25 | `set(key, value)` | 设置值（无过期时间）|
| ~32 | `setWithExpire(key, value, timeout, unit)` | 设置值（带过期时间）|
| ~38 | `get(key)` | 获取值 |
| ~44 | `delete(key)` | 删除键 |
| ~50 | `hasKey(key)` | 检查键是否存在 |

### 2.5 Result — 统一响应

**文件**: `utils/Result.java`

| 行号 | 方法 | 功能 |
|------|------|------|
| ~20 | `success(data)` | 返回 200 成功响应 |
| ~30 | `success(message, data)` | 返回 200 成功响应（带消息）|
| ~40 | `error(message)` | 返回 400 错误响应 |
| ~50 | `error(code, message)` | 返回指定 code 错误响应 |

### 2.6 PasswordEncoderConfig

**文件**: `config/PasswordEncoderConfig.java`

| 行号 | 内容 | 说明 |
|------|------|------|
| ~12 | `@Bean BCryptPasswordEncoder` | 注册 BCrypt 密码编码器 Bean |

### 2.7 WebConfig — JWT 拦截器注册 + CORS

**文件**: `config/WebConfig.java`

| 行号 | 方法 | 功能 |
|------|------|------|
| 30-45 | `addInterceptors()` | 注册 JwtInterceptor，排除 /auth/**, /doc.html, /webjars/**, /files/** |
| 50-60 | `addCorsMappings()` | CORS 允许所有 Origins (生产环境应限制域名) |

### 2.8 SysUser Entity

**文件**: `entity/SysUser.java`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| username | String | 用户名 (唯一) |
| password | String | BCrypt 加密密码 |
| realName | String | 真实姓名 |
| role | Integer | 1=学生, 2=教师, 3=管理员, 4=HR |
| phone | String | AES 加密手机号 |
| wechatOpenid | String | 微信 OpenID |
| avatarUrl | String | 头像 URL |
| status | Integer | 0=禁用, 1=启用 |
| deleted | Integer | 逻辑删除 (0=正常, 1=已删) |

## 3. 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | 123456 | 管理员 (3) |
| teacher1 | 123456 | 教师 (2) |
| hr1 | 123456 | HR (4) |
| student1 | 123456 | 学生 (1) |

## 4. 常见问题

### Q: Token 过期怎么办？
A: 前端检测到 401 状态码 → 清除 localStorage → 跳转 /login

### Q: 要修改 Token 有效期？
A: 修改 `application.yml` 的 `jwt.expiration: 604800000`（毫秒）

### Q: 如何实现"记住我"功能？
A: 修改 `application.yml` 的 `jwt.expiration` 更长（如 30天），同时在 AuthController.login() 中增加 `rememberMe` 分支逻辑

---

> **修改日志**: 本文档为首次全量梳理产出，修改认证相关代码后需同步更新本文档。
