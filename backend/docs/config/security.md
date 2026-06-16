# 安全与配置文档

> 文件: `config/WebConfig.java` / `config/RedisConfig.java` / `config/PasswordEncoderConfig.java` / `interceptor/JwtInterceptor.java`
> 配置文件: `resources/application.yml`

---

## 一、JWT 认证与拦截器

### 1.1 原理

```
请求 → WebConfig.addInterceptors() 注册 JwtInterceptor
         ↓
     匹配 addPathPatterns("/**") 
         ↓
     排除 excludePathPatterns("/auth/**", "/doc.html", "/files/**")
         ↓
     JwtInterceptor.preHandle()
         ↓
     1. 取 header "Authorization" → "Bearer <token>"
     2. substring(7) 去除前缀
     3. jwtUtil.getClaimsFromToken(token) 解析
     4. jwtUtil.isTokenExpired(token) 过期检查
     5. Redis 单点登录校验
     6. request.setAttribute("userId"/"username"/"role")
```

### 1.2 文件引用

| 文件 | 路径 | 行数 |
|------|------|------|
| WebConfig.java | `config/WebConfig.java` | ~60行 |
| JwtInterceptor.java | `interceptor/JwtInterceptor.java` | ~115行 |
| JwtUtil.java | `utils/JwtUtil.java` | ~125行 |

### 1.3 配置参数

```yaml
jwt:
  secret: recruitment-platform-secret-key-2025
  expiration: 604800000  # 7天
  header: Authorization
  prefix: "Bearer "
```

---

## 二、CORS 跨域配置

### 2.1 配置内容

```java
registry.addMapping("/**")
    .allowedOrigins("*")                    // 生产环境应限制具体域名
    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
    .allowedHeaders("*")
    .allowCredentials(false)                // 注意：* 通配符不允许 true
    .maxAge(3600);                          // 预检请求缓存1小时
```

### 2.2 安全建议
- 生产环境替换 `allowedOrigins("*")` 为具体域名，如 `http://localhost:5173`
- 如果前端与后端同域部署，可关闭 CORS 直接使用 Nginx 反向代理

---

## 三、Redis 配置

### 3.1 RedisConfig

**文件**: `config/RedisConfig.java`

Spring Boot 2.7.18 自动配置仅提供 `RedisTemplate<Object,Object>` 和 `StringRedisTemplate`，项目需要 `RedisTemplate<String, Object>`，因此手动定义 Bean：

```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    // Key 使用 StringRedisSerializer
    template.setKeySerializer(new StringRedisSerializer());
    // Value 使用 Jackson2JsonRedisSerializer
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    // Hash 同样配置
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.afterPropertiesSet();
    return template;
}
```

### 3.2 Redis 用途

| Key 模式 | 用途 | 过期时间 |
|----------|------|----------|
| `token:<userId>` | 存储最新 JWT（单点登录） | 7天 |
| `blacklist:<token>` | 登出黑名单 | 剩余有效期 |

---

## 四、密码编码器

**文件**: `config/PasswordEncoderConfig.java`

```java
@Bean
public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

在 `UserServiceImpl.java` 中注入使用：
```java
// 注册时加密
user.setPassword(passwordEncoder.encode(user.getPassword()));
// 登录时校验
passwordEncoder.matches(inputPassword, storedPassword)
```

---

## 五、文件上传配置

### 5.1 配置参数

```yaml
spring.servlet.multipart:
  max-file-size: 10MB      # 单文件上限
  max-request-size: 50MB   # 单次请求上限
  enabled: true

file:
  upload-path: ${user.home}/uploads/   # 系统用户家目录下的 uploads
  avatar-path: ${file.upload-path}avatar/
  resume-path: ${file.upload-path}resume/
  base-url: http://localhost:8080/api/files/
```

### 5.2 FileController

**文件**: `controller/FileController.java` (121行)

| 行号 | 方法 | HTTP | 路径 | 功能 |
|------|------|------|------|------|
| ~30 | `upload()` | POST | `/files/upload` | 通用文件上传（支持图片/简历类型）|
| ~65 | `uploadAvatar()` | POST | `/files/upload/avatar` | 头像上传 |
| ~85 | `uploadResume()` | POST | `/files/upload/resume` | 简历PDF上传 |
| ~100 | `getFile()` | GET | `/files/{filename}` | 获取文件（静态资源访问）|

---

## 六、全局异常处理

**文件**: `exception/GlobalExceptionHandler.java`

| 注解 | 处理异常 | 响应 Code |
|------|----------|-----------|
| `@ExceptionHandler(BusinessException.class)` | 业务异常 | 400 |
| `@ExceptionHandler(MethodArgumentNotValidException.class)` | 参数校验失败 | 400 |
| `@ExceptionHandler(Exception.class)` | 所有未捕获异常 | 500 |

---

> **修改日志**: 首次全量梳理产出
