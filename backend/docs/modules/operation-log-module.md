# 操作日志模块文档

> 模块路径: `com.recruit.controller.OperationLogController` + `com.recruit.service.OperationLogService`
> 涉及文件: 1个 Controller / 1个 Service 接口+实现 / 1个 Mapper 接口+XML / 1个 Entity

---

## 一、原理文档

### 1. 模块职责
记录系统所有用户的关键操作，支持审计追溯、操作历史查询、异常行为分析。

### 2. 设计要点
- **被动记录**：操作日志通过 AOP 切面（或业务代码中手动调用）记录，而非 Controller 层直接写日志
- **OperationLogController.POST** 提供日志记录入口，实际生产环境通常由 AOP 自动触发
- **软删除**：日志采用物理删除（调用 MyBatis-Plus 的 `removeById`）
- **清理策略**：支持按时间批量清理旧日志，用于归档

### 3. 日志数据结构
每条日志记录：**谁(operator) + 何时(createTime) + 做了什么(action) + 对什么做的(target) + 从哪里来(ip)**

---

## 二、实现文档（函数级）

### 2.1 OperationLogController — `/operation-logs/**`

**文件**: `controller/OperationLogController.java` (173行)

| 行号 | 方法 | HTTP | 路径 | 参数 | 功能 |
|------|------|------|------|------|------|
| 28-34 | `getAllLogs()` | GET | `/operation-logs` | — | 获取全部操作日志 |
| 39-47 | `getLogById()` | GET | `/{id}` | @PathVariable id | 按ID查询日志详情 |
| 52-59 | `getLogsByUserId()` | GET | `/by-user/{userId}` | @PathVariable userId | 按用户查询操作记录 |
| 65-72 | `getLogsByOperationType()` | GET | `/by-operation-type/{operationType}` | @PathVariable operationType | 按操作类型过滤 |
| 78-87 | `getLogsByUserIdAndOperationType()` | GET | `/by-user-and-operation-type` | @RequestParam userId, operationType | 用户+类型联合查询 |
| 93-103 | `getLogsByTimeRange()` | GET | `/by-time-range` | @RequestParam startTime, endTime | 时间范围查询 |
| 109-118 | `logOperation()` | POST | `/operation-logs` | @RequestBody params | 记录操作日志 |
| 123-131 | `deleteLog()` | DELETE | `/{id}` | @PathVariable id | 删除单条日志 |
| 136-146 | `cleanupLogs()` | DELETE | `/cleanup` | @RequestParam beforeTime | 批量清理旧日志 |
| 152-159 | `countByUserIdAndGroupByOperationType()` | GET | `/statistics/count-by-user/{userId}` | @PathVariable userId | 用户操作统计 |
| 165-172 | `getRecentLogs()` | GET | `/recent` | @RequestParam (default=10) limit | 最近操作（大屏用） |

### 2.2 OperationLog Entity

**文件**: `entity/OperationLog.java`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| userId | Long | 操作用户ID |
| operationType | String | 操作类型 (如: LOGIN, CREATE_USER, UPDATE_JOB, DELETE_RESUME) |
| targetId | String | 操作对象ID (如: 被操作用户ID、岗位ID、简历ID) |
| target | String | 操作对象描述 |
| detail | String | 操作详情（JSON格式存储）|
| ipAddress | String | 操作IP地址 |
| createTime | LocalDateTime | 操作时间 |

### 2.3 OperationLogService

**文件**: `service/OperationLogService.java` + `service/impl/OperationLogServiceImpl.java`

| 方法 | 功能 |
|------|------|
| `logOperation(userId, operationType, targetId, ipAddress)` | 记录操作日志 |
| `selectByUserId(userId)` | 按用户查询 |
| `selectByOperationType(operationType)` | 按操作类型查询 |
| `selectByUserIdAndOperationType(userId, operationType)` | 联合查询 |
| `selectByTimeRange(startTime, endTime)` | 时间范围查询 |
| `cleanupLogsBeforeTime(beforeTime)` | 清理指定时间之前的日志 |
| `countByUserIdAndGroupByOperationType(userId)` | 用户操作统计（按类型分组）|

---

> **修改日志**: 首次全量梳理产出
