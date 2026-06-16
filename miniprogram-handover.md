# 校企招聘与就业管理平台 — 小程序端交接文档

> 交接日期：2026-06-16
> 编译环境：HBuilderX + 微信开发者工具（测试号）

---

## 一、项目全景架构

```
校企项目/
├── backend/          ← Spring Boot 2.7.x 后端（已可运行）
│   ├── src/           Java 源码（Controller / Service / Mapper / Entity）
│   ├── sql/           建表 + 测试数据（schema.sql + data.sql）
│   └── target/        campus-recruitment-backend-1.0.0.jar
│
├── frontend/         ← Vue 3 + Element Plus PC 管理后台（教师/HR/管理员）
│   └── src/
│       ├── views/     PC 端页面（admin / teacher / hr）
│       └── api/       index.js（后台 API 封装）
│
├── job-miniprogram/  ★ ← 你负责的 uni-app 小程序（学生端）
│   ├── pages/
│   │   ├── index/     启动页
│   │   └── student/   学生端 9 个页面
│   ├── components/    TabBar 组件
│   ├── utils/         request.js（API 封装，核心）
│   ├── App.vue        全局逻辑 + 样式
│   └── pages.json     路由配置
│
└── docs/              项目文档
```

---

## 二、技术栈

| 层 | 技术 | 版本 |
|---|---|---|
| 后端框架 | Spring Boot | 2.7.18 |
| ORM | MyBatis-Plus | 3.5.3 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis（未启用，不影响使用） | - |
| 认证 | JWT + 黑名单 | 7天有效期 |
| 前端（PC） | Vue 3 + Element Plus + Pinia | - |
| **小程序** | **uni-app (Vue 3)** | - |

---

## 三、后端环境

### 3.1 启动方式

```bash
# 后端已经在 8080 端口运行中
# 如果关掉了，在 backend/ 目录下：
cd backend
mvn clean package -DskipTests
java -jar target/campus-recruitment-backend-1.0.0.jar
```

### 3.2 数据库配置

```
数据库名：campus_recruitment
账号：root / 123456
端口：3306
字符集：utf8mb4
```

建表 + 测试数据脚本见：`backend/sql/schema.sql` 和 `backend/sql/data.sql`

### 3.3 后端 API 前缀

所有接口通过 `http://localhost:8080/api/` 访问（context-path 为 `/api`）

### 3.4 测试账号

| 角色 | 账号 | 密码 | userId |
|---|---|---|---|
| 管理员 | admin | 123456 | - |
| 教师 | T001 | 123456 | 1 |
| 教师 | T002 | 123456 | 2 |
| HR | HR001 | 123456 | 3（关联腾讯） |
| HR | HR002 | 123456 | 4（关联阿里） |
| **学生** | **S001** | **123456** | **6（小明）** |
| **学生** | **S002** | **123456** | **7（小红）** |
| **学生** | S003-S005 | 123456 | 8-10 |

---

## 四、小程序端（你的主战场）

### 4.1 页面清单

| 路由 | 文件 | 功能 | API 对接状态 |
|---|---|---|---|
| `/pages/index/index` | 启动页 | 跳转逻辑 | 无 |
| `/pages/student/login` | login.vue | 登录 | ✅ 对接 /auth/login |
| `/pages/student/home` | home.vue | 首页推荐岗位列表 | ✅ 对接 /jobs/recommend 等 |
| `/pages/student/job-detail` | job-detail.vue | 岗位详情 + 投递/收藏 | ✅ 对接多个 API |
| `/pages/student/deliveries` | deliveries.vue | 投递记录列表 | ✅ 对接 /deliveries |
| `/pages/student/collect` | collect.vue | 收藏列表 | ✅ 对接 /favorites |
| `/pages/student/messages` | messages.vue | 消息列表 | ✅ 对接 /messages |
| `/pages/student/profile` | profile.vue | 个人中心 | ✅ 对接统计 |
| `/pages/student/security` | security.vue | 修改密码 | ✅ 对接 update-password |

### 4.2 API 封装（utils/request.js）

所有 API 通过 `request.js` 统一封装，核心逻辑：

```
GET 请求 → data 自动拼接到 query string
POST/PUT 请求 → JSON.stringify(data) + Content-Type: application/json;charset=utf-8
请求头自动注入 token（从 storage 读取）
错误处理：code !== 200 时 reject
```

**已封装的 API 模块：**

| 模块 | 变量名 | 方法 |
|---|---|---|
| 认证 | `authAPI` | login, register, getUserInfo, updatePassword |
| 岗位 | `jobAPI` | getRecommendJobs, getJobDetail, getJobs, searchJobs, getActiveJobs |
| 投递 | `deliveryAPI` | createDelivery, getDeliveriesByStudentId, cancelDelivery |
| 简历 | `resumeAPI` | getResume, createOrUpdateResume, uploadResumeFile |
| 消息 | `messageAPI` | getMessages, readMessage |
| 收藏 | `favoriteAPI` | getFavorites, addFavorite, removeFavorite |
| 统计 | `statisticsAPI` | getStudentOverview |

### 4.3 常见注意事项

**① 获取路由参数：**
```js
// ❌ 错误写法 — 小程序没有 $route
const jobId = getCurrentPages().at(-1).$route?.query?.id

// ✅ 正确写法 — 使用 onLoad
onLoad((options) => {
  const id = options.id  // navigated from '/pages/student/job-detail?id=xxx'
})
```

**② POST 请求体必须 stringify：**
```js
// request.js 已自动处理，如果新加 POST API 注意：
// - 添加 method: 'POST'
// - data 传对象，request 内部会 JSON.stringify
// - 不需要自己设 Content-Type

// 调用示例：
deliveryAPI.createDelivery({ studentId: 6, jobId: 1 })
```

**③ 获取当前用户 ID：**
```js
// 每个页面都有 getStudentId() 函数
const getStudentId = () => {
  try {
    const raw = uni.getStorageSync('userInfo')
    if (!raw) return null
    const obj = JSON.parse(raw)
    const sid = obj.id || obj.userId
    return sid ? Number(sid) : null
  } catch (e) { return null }
}
```

**④ 登录后存储结构：**
```
uni.setStorageSync('token', 'Bearer eyJ...')
uni.setStorageSync('userInfo', JSON.stringify({
  id: 6,
  username: 'S001',
  realName: '小明',
  role: 0,
  avatarUrl: null
}))
```

**⑤ 编译时必须清理缓存：**
- HBuilderX → 运行 → 清缓存 → **全部清除**
- 微信开发者工具 → 清除 → **全部清除**
- 否则可能出现「旧代码 + 新 Storage」不兼容的问题

---

## 五、数据库完整表结构

共 **15 张表**：

| 表名 | 用途 | 数据量 |
|---|---|---|
| `sys_user` | 用户（学生/教师/HR/管理员） | 10 条 |
| `company` | 企业 | 5 条 |
| `job` | 岗位 | 6 条 |
| `resume` | 学生简历 | 5 条 |
| `delivery` | 投递记录 | 6 条 |
| `class` | 班级 | 3 条 |
| `student_class` | 学生-班级关联 | 5 条 |
| `favorite` | 收藏 | 2 条 |
| `message` | 消息 | 4 条 |
| `job_change_apply` | 岗位变更申请 | 空 |
| `operation_log` | 操作日志 | 4 条 |
| `ai_parse_log` | AI 解析日志 | 空 |
| `job_match_record` | 人岗匹配记录 | 5 条 |
| `resume_score_log` | 简历评分记录 | 3 条 |
| `ai_feedback_log` | AI 反馈日志 | 空 |

### 5.1 核心表关系

```
sys_user (学生, role=0) ────→ resume (1:1)
                     ├───→ delivery ───→ job
                     ├───→ favorite ───→ job
                     ├───→ message
                     └───→ student_class ───→ class

company ───→ job (1:N)
sys_user (HR, role=2) ───→ company (N:1, 通过 companyId)
sys_user (教师, role=1) ───→ class (1:N)
```

### 5.2 投递状态枚举

```sql
status: 0=已投递, 1=企业已查看, 2=待面试, 3=已录用, 4=不合适
```

### 5.3 岗位状态枚举

```sql
status: 0=草稿, 1=已发布, 2=已关闭, 3=暂停
```

### 5.4 角色枚举

```sql
role: 0=学生, 1=教师, 2=企业HR, 3=管理员
```

---

## 六、当前状态与已知问题

### ✅ 已完成
- 学生端 9 个页面全部开发完成，API 全链路打通
- 登录/岗位推荐/岗位详情/投递/收藏/消息/修改密码/个人中心 均可使用
- 后端 19 个 Controller 编译通过，8080 端口运行正常
- 数据库 15 张表 + 测试数据已就绪

### 🔄 需要继续完善

**1. 详情页 jobId 获取兼容性（不紧急）**
- 已改为 `onLoad(options)` 方式
- 如果从非 uni-app 方式跳转（如 PC 端扫码），需要额外兼容

**2. 页面加载状态的 UI 优化**
- 目前数据加载期间无 loading 动画
- 空数据时（如无投递记录）页面直接空白，应显示空状态提示

**3. 投递操作确认**
- 投递成功后没有 toast 提示（已封装在 `request.js` 但未被统一调用）
- 建议在 `handleDeliver` 成功后加 `uni.showToast({ title: '投递成功' })`

**4. 修改密码对话框**
- `security.vue` 中修改密码的对话框 UI 待实现完善

**5. PC 后台（教师/HR/管理员）**
- 这是 `frontend/` 目录下的另一个项目，端口 5173
- 教师端部分页面仍有 axios 直调，需要继续迁移到 API 模块
- HR 端切换账号时需要手动清缓存（localStorage 旧字段不兼容）

**6. Redis 未启用**
- 不影响小程序使用
- 启动后端时的 Redis 连接错误是已知的，暂时无害

### ❌ 已知 Bug（待修）

1.**教师模块 9 个文件未完成 API 模块化**：仍使用 `axios` 直调
2.**小程序详情页有问题**: mp.esm.js:529 跳过：studentId或jobId为空
---

## 七、常见开发操作

### HBuilderX 编译运行
```
1. 打开 HBuilderX
2. 导入项目：校企项目/job-miniprogram
3. 运行 → 运行到微信开发者工具
4. 每次修改代码后，重新运行（完整编译，不要热重载）
```

### 微信开发者工具调试
```
1. 工具 → 调试器（Console / Network / Storage）
2. 清除 → 全部清除（如果遇到奇怪的问题）
3. Storage 中检查 token 和 userInfo 是否正确
```

### 后端 Health Check
```bash
# 检查后端是否运行
curl http://localhost:8080/api/jobs/active

# 验证登录接口
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"S001","password":"123456","role":0}'
```

---

## 八、配套 SQL 文件

随附文件：
- `backend/sql/schema.sql` — 完整建表语句（15 张表，含外键注释）
- `backend/sql/data.sql` — 测试数据（10 用户 + 5 企业 + 6 岗位 + 5 简历 + 6 投递等）
- `job-miniprogram/temp_init.sql` — 额外表（message + favorite）的建表语句和数据

**初始化顺序：**
```sql
source backend/sql/schema.sql;     -- 建库 + 建所有表
source backend/sql/data.sql;       -- 插入全部测试数据
source job-miniprogram/temp_init.sql; -- 建 message 和 favorite 表并插入数据
```

---

> **你的重点：小程序学生端 9 个页面已经基本可用，剩下的主要是体验打磨和 bug 修复。** 如果遇到后端 API 问题，先确认 Java 进程在 8080 运行、数据库连接正常。大部分问题都可以通过「清缓存重编译」解决。
