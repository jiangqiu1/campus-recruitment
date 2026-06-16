# 用户管理模块文档

> **所属项目**：校企招聘平台  
> **模块路径**：`com.recruit.*（controller/service/entity/mapper）`  
> **核心功能**：用户（管理员/教师/企业HR/学生）的增删改查、状态管理、密码重置  
> **文档版本**：v1.0  
> **更新日期**：2026-06-08

---

## 一、原理文档

### 1.1 模块职责

| 层级 | 类 | 职责 |
|------|-----|------|
| **Controller** | `UserController` | HTTP 接口暴露，参数校验，请求路由 |
| **Service** | `UserService`（接口）+ `UserServiceImpl`（实现） | 业务逻辑编排：注册、登录、密码修改、用户名/OAuth 查询 |
| **Entity** | `SysUser` | 用户表 ORM 映射（MyBatis-Plus） |
| **Mapper** | `SysUserMapper`（接口）+ `SysUserMapper.xml` | 数据库 SQL 访问（自定义查询+XML 扩展） |

**调用链路**：`Controller → Service → Mapper → MySQL（sys_user 表）`

---

### 1.2 角色体系

系统内置四种角色，通过 `SysUser.role` 字段区分（`Integer` 类型）：

| 值 | 角色 | 英文标识 | 典型权限范围 |
|----|------|---------|------------|
| `0` | 学生 | `ROLE_STUDENT` | 浏览职位、投递简历、查看面试通知 |
| `1` | 教师 | `ROLE_TEACHER` | 查看学生就业情况、推荐岗位、审核实习 |
| `2` | 企业HR | `ROLE_HR` | 发布职位、查看简历、发起面试邀请 |
| `3` | 管理员 | `ROLE_ADMIN` | **用户管理**（本模块）、系统配置、数据统计 |

> **注意**：本模块（`UserController`）所有接口均标注为管理员专用，实际鉴权通过 Spring Security 配置控制。

**认证流程（Service 层 `login()` 方法）**：
1. 根据 `username` 查询用户
2. `BCryptPasswordEncoder.matches()` 验证明文密码与加密存储密码是否匹配
3. 验证传入的 `role` 与数据库中的 `role` 是否一致
4. 检查 `status` 是否被禁用（`0`）
5. 任一环节不通过均抛出 `RuntimeException`

---

### 1.3 BCrypt 密码加密

- **算法**：`BCrypt`（Spring Security 提供，强度=10）
- **位置**：`UserServiceImpl` 与 `UserController` 中均持有 `BCryptPasswordEncoder` 实例
- **场景**：
  - 管理员创建用户（`createUser`）：明文 → `encoder.encode()` → 存库
  - 管理员重置密码（`resetPassword`）：同上
  - 用户注册（`register`）：同上
  - 用户登录（`login`）：`encoder.matches(明文, 密文)` 验证
  - 修改密码（`updatePassword`）：先校验旧密码，再加密新密码
- **特点**：每次加密结果不同（自动加盐），不可逆；即使数据库泄露也无法反解原始密码

---

### 1.4 AES 敏感字段加密

- **算法**：`AES`（对称加密）
- **工具类**：`com.recruit.utils.AESUtil`
- **加密字段**：`phone`（手机号）
- **使用模式**：

| 操作 | 加密/解密 | 说明 |
|------|----------|------|
| `createUser` | 加密（`aesUtil.encrypt()`） | 入库前加密手机号 |
| `updateUser` | 加密 | 更新时加密手机号 |
| `getAllUsers` | 解密（`aesUtil.decrypt()`） | 返回前端前解密 |
| `getUserById` | 解密 | 返回前端前解密 |
| `getUsersByRole` | 解密 | 返回前端前解密 |

> **安全策略**：数据库存密文，前端展示明文，既满足数据保护合规，又不影响功能使用。

---

### 1.5 软删除

- **实现方式**：MyBatis-Plus `@TableLogic` 注解 + `deleted` 字段
- **字段**：`SysUser.deleted`（`Integer`，`0`=未删除，`1`=已删除）
- **Controller 删除操作**：不执行 `DELETE` SQL，而是执行 `UPDATE sys_user SET deleted = 1 WHERE id = ?`
- **查询过滤**：MyBatis-Plus 自动在查询中追加 `AND deleted = 0`，开发者无需手动处理
- **自定义 XML Mapper 的一致性**：所有自定义查询（`selectByUsername`、`selectByWechatOpenid`、`selectByRole` 等）均显式包含 `AND deleted = 0`

> **优点**：数据可恢复，保留历史轨迹；不影响现有外键关联。

---

### 1.6 实体字段说明

**类**：`com.recruit.entity.SysUser`  
**表名**：`sys_user`  
**ORM**：MyBatis-Plus（`@TableName`、`@TableId`、`@TableLogic`）

| 字段 | Java 类型 | 数据库列 | 说明 |
|------|-----------|---------|------|
| `id` | `Long` | `id` (PK, AUTO) | 主键自增 |
| `username` | `String` | `username` (UNIQUE) | 用户名（学号/工号） |
| `password` | `String` | `password` | 密码（BCrypt 加密存储） |
| `realName` | `String` | `real_name` | 真实姓名 |
| `role` | `Integer` | `role` | 角色：0=学生，1=教师，2=HR，3=管理员 |
| `phone` | `String` | `phone` | 手机号（AES 加密存储） |
| `wechatOpenid` | `String` | `wechat_openid` (UNIQUE) | 微信 OpenID |
| `avatarUrl` | `String` | `avatar_url` | 头像 URL |
| `status` | `Integer` | `status` | 状态：0=禁用，1=正常 |
| `createTime` | `LocalDateTime` | `create_time` | 创建时间 |
| `updateTime` | `LocalDateTime` | `update_time` | 更新时间 |
| `deleted` | `Integer` | `deleted` | 逻辑删除标志（MP 自动处理） |

---

## 二、实现文档（函数级）

> 以下行号均基于 `UserController.java`（共 221 行）。

---

### 2.1 获取所有用户列表

| 项目 | 内容 |
|------|------|
| **方法** | `getAllUsers()` |
| **行号** | L36–L53 |
| **HTTP** | `GET /admin/users` |
| **请求参数** | 无 |
| **响应** | `Result<List<SysUser>>` |
| **功能** | 查询所有未删除的用户，解密手机号后返回 |
| **核心逻辑** | ① `userService.list()`（MP 自动过滤 `deleted=0`）→ ② 遍历解密 `phone` → ③ `Result.success()` |
| **安全说明** | 无需鉴权注解——类级别已限制管理员访问 |

---

### 2.2 根据 ID 获取用户

| 项目 | 内容 |
|------|------|
| **方法** | `getUserById(Long id)` |
| **行号** | L56–L74 |
| **HTTP** | `GET /admin/users/{id}` |
| **请求参数** | `@PathVariable Long id` |
| **响应** | `Result<SysUser>` |
| **功能** | 根据 ID 查询单个用户；不存在则返回 404 |
| **核心逻辑** | ① `userService.getById(id)` → ② 判空返回 404 → ③ 解密 `phone` → ④ 返回 |
| **异常处理** | 用户不存在：`Result.error(404, "用户不存在")` |

---

### 2.3 根据角色获取用户

| 项目 | 内容 |
|------|------|
| **方法** | `getUsersByRole(Integer role)` |
| **行号** | L77–L96 |
| **HTTP** | `GET /admin/users/by-role/{role}` |
| **请求参数** | `@PathVariable Integer role`（0=学生，1=教师，2=HR，3=管理员） |
| **响应** | `Result<List<SysUser>>` |
| **功能** | 按角色筛选所有未删除用户 |
| **核心逻辑** | ① `userService.list()` 全量 → ② Java Stream `filter(role.equals)` 过滤 → ③ 解密 `phone` → ④ 返回 |
| **性能说明** | 全表查询后在内存中过滤，用户量大时建议改为 Mapper 层 `selectByRole` 直接 SQL 过滤 |

---

### 2.4 创建用户

| 项目 | 内容 |
|------|------|
| **方法** | `createUser(SysUser user)` |
| **行号** | L99–L127 |
| **HTTP** | `POST /admin/users` |
| **请求参数** | `@RequestBody SysUser user`（JSON 格式） |
| **响应** | `Result<String>` |
| **功能** | 管理员创建新用户，包含密码加密 + 手机号加密 |
| **核心逻辑** | ① 用户名唯一性检查（`lambdaQuery().eq(username).one()`）→ ② `passwordEncoder.encode()` 加密密码 → ③ `aesUtil.encrypt()` 加密手机号 → ④ `userService.save()` |
| **异常处理** | 用户名已存在：`Result.error("用户名已存在")` |

---

### 2.5 更新用户

| 项目 | 内容 |
|------|------|
| **方法** | `updateUser(Long id, SysUser user)` |
| **行号** | L132–L151 |
| **HTTP** | `PUT /admin/users/{id}` |
| **请求参数** | `@PathVariable Long id` + `@RequestBody SysUser user` |
| **响应** | `Result<String>` |
| **功能** | 更新用户信息（手机号重新加密） |
| **核心逻辑** | ① 校验用户存在 → ② 加密 `phone` → ③ `user.setId(id)` 固定 ID → ④ `updateById()` |
| **注意事项** | 不重新加密密码（保留原密文）；仅更新传入的非空字段（MP 动态 SQL） |

---

### 2.6 删除用户（软删除）

| 项目 | 内容 |
|------|------|
| **方法** | `deleteUser(Long id)` |
| **行号** | L156–L168 |
| **HTTP** | `DELETE /admin/users/{id}` |
| **请求参数** | `@PathVariable Long id` |
| **响应** | `Result<String>` |
| **功能** | 软删除用户（设置 `deleted = 1`） |
| **核心逻辑** | ① 校验用户存在 → ② `user.setDeleted(1)` → ③ `updateById()`（MP 不拦截手动的 `deleted` 写入） |
| **注意** | MP 的 `@TableLogic` 仅自动追加查询过滤条件，手动设置 `deleted` 并调用 `updateById` 可正常写入 |

---

### 2.7 禁用 / 启用用户

| 项目 | 内容 |
|------|------|
| **方法** | `updateUserStatus(Long id, Map<String, Integer> params)` |
| **行号** | L177–L196 |
| **HTTP** | `PUT /admin/users/{id}/status` |
| **请求参数** | `@PathVariable Long id` + `@RequestBody Map<String, Integer>`（`status`: `0`=禁用 / `1`=启用） |
| **响应** | `Result<String>` |
| **功能** | 切换用户账号状态（禁用后无法登录） |
| **核心逻辑** | ① 校验用户存在 → ② 校验 `status` 合法性（必须为 0 或 1） → ③ `user.setStatus(status)` → ④ `updateById()` |
| **异常处理** | status 参数错误：`Result.error("status参数错误（应为0或1）")` |

---

### 2.8 重置用户密码

| 项目 | 内容 |
|------|------|
| **方法** | `resetPassword(Long id, Map<String, String> params)` |
| **行号** | L202–L221 |
| **HTTP** | `PUT /admin/users/{id}/reset-password` |
| **请求参数** | `@PathVariable Long id` + `@RequestBody Map<String, String>`（`newPassword`：新密码明文） |
| **响应** | `Result<String>` |
| **功能** | 管理员直接重置指定用户的密码（无需旧密码） |
| **核心逻辑** | ① 校验用户存在 → ② 校验 `newPassword` 非空 → ③ `passwordEncoder.encode()` 加密 → ④ 更新 |
| **用户端对应** | 用户自行修改密码走 Service 层 `updatePassword()`，需验证旧密码 |

---

### 2.9 Service 层补充接口

**`UserService` 接口（`com.recruit.service.UserService`）：**

| 方法 | 说明 | 参数 |
|------|------|------|
| `selectByUsername(username)` | 按用户名查询 | `String username` |
| `selectByWechatOpenid(openid)` | 按微信 OpenID 查询 | `String openid` |
| `login(username, password, role)` | 登录验证密码+角色+状态 | `(String, String, Integer)` |
| `register(user)` | 注册（加密密码、设默认状态） | `SysUser user` |
| `updatePassword(userId, oldPwd, newPwd)` | 修改密码（需旧密码） | `(Long, String, String)` |

**`UserServiceImpl`（`com.recruit.service.impl.UserServiceImpl`）关键实现细节：**

- **`login()`**：`BCryptPasswordEncoder.matches()` 验证明文 → 密文；角色不匹配或状态禁用均抛出异常
- **`register()`**：标注 `@Transactional(rollbackFor = Exception.class)`，新建用户默认 `status = 1`
- **`updatePassword()`**：标注 `@Transactional`，先校验旧密码，新密码 BCrypt 加密后通过 `SysUserMapper.updatePassword()` 直接 SQL 更新

---

### 2.10 Mapper 层 SQL 说明

**`SysUserMapper.java`（接口）** 定义了 5 个自定义查询方法，全部通过注解或 XML 实现：

| 方法 | SQL 说明 | 实现方式 |
|------|---------|---------|
| `selectByUsername` | `WHERE username = ? AND deleted = 0` | `@Select` 注解 |
| `selectByWechatOpenid` | `WHERE wechat_openid = ? AND deleted = 0` | `@Select` 注解 |
| `selectByRole` | `WHERE role = ? AND deleted = 0` | `@Select` 注解 |
| `updatePassword` | `UPDATE sys_user SET password = ? WHERE id = ?` | XML（`SysUserMapper.xml`） |
| `selectByLastLoginTimeBetween` | `WHERE last_login_time BETWEEN ? AND ? AND deleted = 0` | XML |
| `updateLastLoginTime` | `UPDATE sys_user SET last_login_time = ? WHERE id = ?` | XML |
| `selectAdmins` | `WHERE role = 3 AND deleted = 0` | XML |

> **注意**：`last_login_time` 相关方法在 `SysUserMapper.xml` 中定义，但 `SysUser` 实体类中未包含此字段，可能为历史遗留或未来预留。

---

## 三、关键安全策略总结

| 维度 | 策略 | 技术实现 |
|------|------|---------|
| **密码存储** | 不可逆加密 | BCrypt（自动加盐，强度 10） |
| **敏感字段** | 对称加密存储 | AES（加密入库，解密出库） |
| **数据删除** | 逻辑删除不丢数据 | MyBatis-Plus `@TableLogic` + `deleted` 字段 |
| **账号状态** | 禁用后拒绝登录 | `status = 0` → `login()` 抛出异常 |
| **接口权限** | 仅管理员可访问 | `@RestController` + 类级别 `@RequestMapping("/admin/users")` + Spring Security 配置 |

---

## 四、文件索引

| 文件 | 路径 |
|------|------|
| Controller | `backend/src/main/java/com/recruit/controller/UserController.java`（221 行） |
| Service 接口 | `backend/src/main/java/com/recruit/service/UserService.java` |
| Service 实现 | `backend/src/main/java/com/recruit/service/impl/UserServiceImpl.java` |
| Entity | `backend/src/main/java/com/recruit/entity/SysUser.java` |
| Mapper 接口 | `backend/src/main/java/com/recruit/mapper/SysUserMapper.java` |
| Mapper XML | `backend/src/main/resources/mapper/SysUserMapper.xml` |
