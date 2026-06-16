# 职业院校校企招聘与就业管理平台

> 基于 Spring Boot + Vue3 的全栈应用，支持管理员、教师、企业HR、学生四种身份

## 📁 项目结构

```
家校项目/
├── backend/          # 后端（Spring Boot）
│   ├── src/
│   ├── target/
│   ├── sql/         # 数据库脚本
│   └── uploads/     # 上传文件存储
├── frontend/         # 前端（Vue3 + Vite）
│   ├── src/
│   ├── dist/
│   └── node_modules/
└── PROJECT_SUMMARY.md  # 项目总结
```

## 🛠️ 技术栈

### 后端
- **框架**: Spring Boot 2.7+
- **ORM**: MyBatis-Plus
- **数据库**: MySQL 8.0+
- **缓存**: Redis
- **认证**: JWT（7天有效期 + Redis黑名单）
- **加密**: BCrypt（密码）、AES（敏感字段）

### 前端
- **框架**: Vue 3
- **构建工具**: Vite
- **UI库**: Element Plus
- **路由**: Vue Router
- **HTTP**: Axios

## 🚀 快速启动

### 1️⃣ 数据库初始化

```bash
# 登录MySQL
mysql -u root -p

# 执行脚本（按顺序）
source E:/家校项目/backend/sql/schema.sql
source E:/家校项目/backend/sql/data.sql
```

**默认测试账号**：
- 管理员：`admin` / `123456`
- 教师：`T001` / `123456`
- HR：`HR001` / `123456`
- 学生：`S001` / `123456`

---

### 2️⃣ 后端启动

#### 环境要求
- JDK 11+
- Maven 3.6+
- Redis 服务运行中

#### 配置修改
编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_recruitment?...
    username: root
    password: 你的密码
  
  redis:
    host: localhost
    port: 6379
```

#### 启动方式

**方式1：IDE启动**
- 导入项目到 IntelliJ IDEA / Eclipse
- 运行 `RecruitApplication.java`

**方式2：Maven启动**
```bash
cd E:/家校项目/backend
mvn spring-boot:run
```

**方式3：打包后启动**
```bash
cd E:/家校项目/backend
mvn clean package
java -jar target/recruit-0.0.1-SNAPSHOT.jar
```

后端运行在：`http://localhost:8080`

---

### 3️⃣ 前端启动

#### 环境要求
- Node.js 16+
- npm 8+

#### 安装依赖
```bash
cd E:/家校项目/frontend
npm install
```

#### 启动开发服务器
```bash
npm run dev
```

前端运行在：`http://localhost:5173`

---

### 4️⃣ 访问系统

打开浏览器访问：`http://localhost:5173`

**登录页面功能**：
- 输入用户名、密码
- 选择角色（管理员/教师/HR/学生）
- 点击登录

**各角色功能模块**：

| 角色 | 功能模块 |
|------|---------|
| 管理员 | 用户管理、企业管理、班级管理、操作日志、数据大屏 |
| 教师 | 简历管理、AI解析、人岗匹配、班级管理、就业看板 |
| HR | 企业信息、岗位管理、简历评分、数据统计、岗位分析 |
| 学生 | 简历管理、岗位浏览、投递记录、消息通知 |

---

## 📂 核心功能说明

### 1. 认证模块
- JWT Token 认证（7天有效期）
- Redis 黑名单（实现登出）
- 密码 BCrypt 加密

### 2. 简历管理
- 学生可上传PDF简历
- 教师可查看/导出简历
- 支持人岗匹配推荐

### 3. 岗位管理
- HR可发布/编辑/关闭岗位
- 学生可浏览并投递
- 支持AI智能匹配

### 4. 数据统计
- 就业率统计
- 岗位投递分析
- 企业合作等级分析

---

## 🔧 配置文件说明

### 后端配置文件
`backend/src/main/resources/application.yml`

关键配置项：
```yaml
# JWT密钥（生产环境请修改！）
jwt:
  secret: recruitment-platform-secret-key-2025
  expiration: 604800000  # 7天

# 文件上传路径
file:
  upload-path: ${user.home}/uploads/
  resume-path: ${file.upload-path}resume/
```

### 前端配置文件
`frontend/vite.config.js`

```javascript
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // 后端地址
        changeOrigin: true
      }
    }
  }
})
```

---

## 📝 常见问题

### Q1: 后端启动失败（数据库连不上）
**解决**：检查 `application.yml` 中的数据库配置，确保MySQL服务运行中。

### Q2: 前端启动失败（npm install报错）
**解决**：
```bash
# 清除缓存重试
npm cache clean --force
rm -rf node_modules
npm install
```

### Q3: 文件上传失败
**解决**：确保 `uploads/` 目录存在，并且有写权限。

### Q4: JWT Token过期
**解决**：重新登录获取新Token，Token存储在 `localStorage` 中。

---

## 🎯 开发路线图

- [x] 后端API完成（13张表，全套CRUD）
- [x] 前端页面完成（4种角色，20+页面）
- [x] 文件上传功能
- [ ] 学生端小程序（微信小程序）
- [ ] AI深度集成（简历解析、人岗匹配优化）
- [ ] 数据可视化大屏（ECharts）
- [ ] 消息推送（WebSocket）
- [ ] 权限细化（按钮级权限）

---

## 👥 开发团队

- **后端开发**：AI Assistant
- **前端开发**：AI Assistant
- **架构设计**：AI Assistant
- **测试数据**：AI Assistant

---

## 📄 许可证

MIT License

---

## 📞 联系方式

如有问题，请提交Issue或联系开发团队。

**最后更新**：2026-06-03
