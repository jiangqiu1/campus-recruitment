# 投递管理模块文档

> 模块路径: `com.recruit.controller.DeliveryController` + `com.recruit.service.DeliveryService`
> 涉及文件: 1个 Controller / 1个 Service 接口+实现 / 1个 Mapper 接口+XML / 1个 Entity

---

## 一、原理文档

### 1. 模块职责
学生投递岗位、HR查看投递记录、更新投递状态、安排面试。

### 2. 投递状态流转
```
已投递(0) → 企业已查看(1) → 待面试(2) → 已录用(3)
                    ↓                     
                不合适(4)
```

### 3. 业务规则
- **重复投递检测**：学生不能对同一岗位重复投递（deliveryService.deliverResume 实现）
- **版本记录**：投递时记录简历版本号，便于追溯
- **统计支持**：按岗位统计各状态投递数量（数据大屏）

---

## 二、实现文档（函数级）

### 2.1 DeliveryController — `/deliveries/**`

**文件**: `controller/DeliveryController.java` (191行)

| 行号 | 方法 | HTTP | 路径 | 参数 | 功能 |
|------|------|------|------|------|------|
| 30-36 | `getAllDeliveries()` | GET | `/deliveries` | — | 获取全部投递记录 |
| 42-50 | `getDeliveryById()` | GET | `/deliveries/{id}` | @PathVariable id | 按ID查询 |
| 56-62 | `getDeliveriesByStudentId()` | GET | `/by-student/{studentId}` | @PathVariable studentId | 学生查询自己的投递 |
| 68-75 | `getDeliveriesByJobId()` | GET | `/by-job/{jobId}` | @PathVariable jobId | 岗位下所有投递 |
| 81-88 | `getDeliveriesByStatus()` | GET | `/by-status/{status}` | @PathVariable status | 按状态过滤 |
| 95-105 | `getDeliveriesByStudentIdAndStatus()` | GET | `/by-student-and-status` | @RequestParam studentId, status | 学生+状态多条件查询 |
| 111-121 | `getDeliveriesByJobIdAndStatus()` | GET | `/by-job-and-status` | @RequestParam jobId, status | 岗位+状态多条件查询 |
| 127-142 | `deliverResume()` | POST | `/deliveries/deliver` | @RequestParam studentId, @RequestBody params | 学生投递简历 |
| 148-163 | `updateDeliveryStatus()` | PUT | `/{id}/status` | @PathVariable id, @RequestBody params | 更新投递状态 |
| 169-183 | `arrangeInterview()` | PUT | `/{id}/arrange-interview` | @PathVariable id, @RequestBody params | HR安排面试 |
| 188-194 | `deleteDelivery()` | DELETE | `/{id}` | @PathVariable id | 软删除投递记录 |
| 199-204 | `getDeliveryStatisticsByJobId()` | GET | `/statistics/by-job/{jobId}` | @PathVariable jobId | 岗位投递统计 |

### 2.2 Delivery Entity

**文件**: `entity/Delivery.java`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| studentId | Long | 学生ID |
| jobId | Long | 岗位ID |
| status | Integer | 0=已投递, 1=企业已查看, 2=待面试, 3=已录用, 4=不合适 |
| resumeVersion | String | 投递时简历版本号 |
| feedback | String | HR反馈意见 |
| interviewTime | LocalDateTime | 面试时间 |
| interviewLocation | String | 面试地点 |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |
| deleted | Integer | 逻辑删除 (0=正常, 1=已删) |

### 2.3 DeliveryService

**文件**: `service/DeliveryService.java` + `service/impl/DeliveryServiceImpl.java`

| 方法 | 功能 |
|------|------|
| `selectByStudentId(studentId)` | 学生查询自己的投递列表 |
| `selectByJobId(jobId)` | 按岗位查询投递 |
| `selectByStatus(status)` | 按状态过滤 |
| `selectByStudentIdAndStatus(studentId, status)` | 多条件联合查询 |
| `selectByJobIdAndStatus(jobId, status)` | 岗位+状态查询 |
| `deliverResume(studentId, jobId, resumeVersion)` | 投递简历（含重复检测）|
| `updateDeliveryStatus(id, status, feedback)` | 更新投递状态+反馈 |
| `arrangeInterview(id, interviewTime, location)` | 安排面试 |
| `countByJobIdAndGroupByStatus(jobId)` | 岗位投递统计（大屏用）|

---

> **修改日志**: 首次全量梳理产出
