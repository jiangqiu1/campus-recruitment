# 岗位管理模块文档

> 模块路径: `com.recruit.controller.JobController` + `com.recruit.service.JobService` + `com.recruit.service.JobChangeApplyService`
> 涉及文件: 1 个 Controller / 2 个 Service / 2 个 Entity / 1 个 DTO / 2 个 Mapper Java / 2 个 Mapper XML

---

## 一、原理文档

### 1. 模块职责

岗位管理是校企招聘平台的核心数据模块，负责岗位的**全生命周期管理**（创建 → 发布 → 维护 → 关闭/删除），同时支撑**学生投递**、**智能匹配**等下游业务。

### 2. 实体关系

```
Company (1) ──────┐
                   ├── Job (N) ──────┐
SysUser:teacher/HR 之                 ├── Delivery (N)     ← 学生投递
                   │   (createdBy)   ├── JobChangeApply    ← HR申请变更
                   │                 └── JobMatchRecord    ← 智能匹配
                   │
Com.recruit.entity ┘
```

- **Company** ↔ **Job**: 一对多关系，一个企业可发布多个岗位
- **SysUser** (teacher/HR) → **Job**: 一个用户可创建多个岗位（`created_by` 字段）
- **Job** → **Delivery**: 一对多，一个岗位可被多名学生投递
- **Job** → **JobChangeApply**: 一对多，HR可对岗位提交多次变更申请

### 3. 岗位状态流转

```
     ┌──────────┐
     │   草稿    │  (0)
     │  (Draft) │
     └────┬─────┘
          │ PUT /{id}/publish
          ▼
     ┌──────────┐
     │  已发布   │  (1)  PUT /{id}/pause      ┌──────────┐
     │ (Active) │────────────────────────────▶│   暂停   │  (3)
     └────┬─────┘                              │ (Paused) │
          │ PUT /{id}/close                    └─────┬────┘
          ▼                                          │ PUT /{id}/publish (*)
     ┌──────────┐                                    │
     │  已关闭   │  (2)                               │
     │ (Closed) │◀────────────────────────────────────┘  (*) 实际未实现
     └──────────┘

  删除 → 软删除 (deleted=1, @TableLogic)

  截止日期到达 → 系统层面未自动关闭
                 (selectActiveJobs 按 deadline >= today 过滤)
```

**状态值对照表：**

| 状态值 | 名称 | 含义 | 可操作 |
|--------|------|------|--------|
| 0 | 草稿 | 刚创建，仅创建者可见 | 编辑、发布、删除 |
| 1 | 已发布 | 学生可查看并投递 | 暂停、关闭 |
| 2 | 已关闭 | 不再接受投递，历史数据保留 | — |
| 3 | 暂停 | 临时停止接收投递，可恢复 | 关闭 (`publishJob` 仅允许草稿→已发布) |

**核心约束：**
- 只有 **草稿(0)** 可以调用 `publishJob` 发布为 **已发布(1)**
- 发布时自动生成 `traceId`（唯一追踪ID）和 `qrCodeUrl`（二维码路径）
- **暂停(3)** 状态的岗位目前无法通过接口恢复正常发布（`publishJob` 校验 `status != 0` 时会抛异常）
- 删除为**软删除**，设置 `deleted = 1`，所有查询均自动过滤 `deleted = 0`

### 4. 岗位与企业关联逻辑

- 岗位必须关联一个企业（`job.company_id` 非空）
- 岗位发布者（`created_by`）可以是**教师**或**企业HR**
- 查询岗位时支持按 `companyId` 和 `createdBy` 分别检索
- 企业关联在岗位层面的作用：
  - 数据大屏统计：`countByCompanyId()` 统计某企业的岗位数量
  - 学生前端：按企业查看岗位，展现企业名称与岗位的关联

### 5. 岗位变更申请机制

HR 修改已发布的岗位时，需要通过 **JobChangeApply** 提交变更申请，由教师审核通过后生效：

```
HR 提交变更申请  ──▶  待审核 (0)  ──▶  通过 (1)  → 教师手动同步至岗位
                       │
                       └─────────────▶  拒绝 (2)
```

- **`submitChangeApply()`**: HR 提交变更内容（JSON 格式）
- **`reviewChangeApply()`**: 教师审核（通过/拒绝），已审核的记录不可重复审核
- **注意**: 审核通过后，系统**不会自动**将变更内容应用到 `job` 表，需教师手动编辑或由后续开发补全同步逻辑

### 6. 关键设计决策

| 决策 | 说明 |
|------|------|
| **软删除** | 使用 MyBatis-Plus `@TableLogic` + `deleted` 字段，保留历史数据 |
| **查看计数** | `view_count` 字段在每次获取详情时原子性自增，无需单独计数表 |
| **唯一追踪ID** | 发布时生成 UUID 作为 `traceId`，用于后续二维码/推广追踪 |
| **二维码预留** | `qrCodeUrl` 字段预留，当前返回占位路径 `/uploads/qrcode/job_<id>.png`，需对接OSS |
| **AI扩展字段** | `aiKeywords` 和 `requiredSkills` 以 JSON 格式存储，支撑智能匹配与AI解析 |
| **数据大屏** | `getJobStatistics()` 合并返回 `viewCount` + `deliveryCount` |

---

## 二、实现文档（函数级）

### 2.1 JobController — `/jobs/**`

**文件**: `controller/JobController.java` (~205行)

#### 查询类接口

| 行号 | 方法 | HTTP | 路径 | 参数 | 功能 |
|------|------|------|------|------|------|
| 33-37 | `getAllJobs()` | GET | `/jobs` | — | 获取全量岗位列表（含已关闭），**不建议前端直用** |
| 42-53 | `getJobById()` | GET | `/jobs/{id}` | `@PathVariable Long id` | 根据ID获取岗位详情，同时 `incrementViewCount` 增加浏览次数 |
| 58-64 | `getJobsByCompanyId()` | GET | `/jobs/by-company/{companyId}` | `@PathVariable Long companyId` | 按企业ID查询岗位列表 |
| 70-76 | `getJobsByCreatedBy()` | GET | `/jobs/by-creator/{createdBy}` | `@PathVariable Long createdBy` | 按发布者ID（教师或HR）查询岗位列表 |
| 82-87 | `getActiveJobs()` | GET | `/jobs/active` | — | 查询有效岗位（`status=1` + `deadline >= today`） |
| 93-98 | `getJobsByStatus()` | GET | `/jobs/by-status/{status}` | `@PathVariable Integer status` | 按状态筛选（0草稿/1已发布/2已关闭/3暂停） |
| 163-171 | `searchJobs()` | GET | `/jobs/search` | `@RequestParam String keyword` | 按标题模糊搜索（基于 MyBatis-Plus LambdaQuery） |
| 177-191 | `getJobStatistics()` | GET | `/jobs/{id}/statistics` | `@PathVariable Long id` | 返回岗位统计（viewCount + deliveryCount），用于数据大屏 |

#### 写操作接口

| 行号 | 方法 | HTTP | 路径 | 参数 | 功能 |
|------|------|------|------|------|------|
| 103-109 | `createJob()` | POST | `/jobs` | `@RequestBody Job` | 创建岗位，强制设为草稿状态(0) |
| 113-124 | `updateJob()` | PUT | `/jobs/{id}` | `@PathVariable Long id + @RequestBody Job` | 更新岗位，先校验存在性 |
| 131-138 | `publishJob()` | PUT | `/jobs/{id}/publish` | `@PathVariable Long id` | 发布岗位（草稿→已发布），自动生成 traceId + qrCodeUrl |
| 144-150 | `closeJob()` | PUT | `/jobs/{id}/close` | `@PathVariable Long id` | 关闭岗位，设置状态为2 |
| 155-161 | `pauseJob()` | PUT | `/jobs/{id}/pause` | `@PathVariable Long id` | 暂停岗位，设置状态为3 |
| 137-147 | `deleteJob()` | DELETE | `/jobs/{id}` | `@PathVariable Long id` | 软删除岗位，设置 `deleted = 1` |

---

### 2.2 JobService（接口 + 实现）

**接口文件**: `service/JobService.java`
**实现文件**: `service/impl/JobServiceImpl.java` (~100行)

| 行号(Svc) | 方法 | 功能 |
|-----------|------|------|
| 接口 20 | `selectByCompanyId(Long)` | 按企业ID查岗位 |
| 接口 28 | `selectByCreatedBy(Long)` | 按发布者ID查岗位 |
| 接口 36 | `selectActiveJobs()` | 查询有效岗位（`status=1 + deadline >= today`） |
| 接口 44 | `selectByStatus(Integer)` | 按状态查询 |
| 接口 52 | `incrementViewCount(Long)` | 浏览次数原子自增（UPDATE SET view_count = view_count + 1） |
| 接口 60 | `publishJob(Long)` | 发布：校验草稿态 → 设 status=1 → 生成 traceId + qrCodeUrl |
| 接口 68 | `closeJob(Long)` | 关闭：设 status=2 |
| 接口 76 | `pauseJob(Long)` | 暂停：设 status=3 |
| 接口 84 | `countByCompanyId(Long)` | 统计某企业的岗位数量 |
| 接口 92 | `countDeliveries(Long)` | 统计某岗位的投递数量（跨 delivery 表） |
| 接口 99 | `generateTraceId()` | 生成 UUID 去横线 32 位追踪ID |
| 接口 106 | `generateQrCode(Long)` | 返回占位二维码路径（待对接OSS） |

**实现要点：**

- 所有写操作的 Service 方法均标注 `@Transactional`
- `publishJob()` 强制校验 `job.getStatus() != 0` → 抛出 `RuntimeException`
- `closeJob()` 和 `pauseJob()` 不做状态校验，任意状态的岗位均可调用
- `countByCompanyId()` 先用 `selectByCompanyId` 查列表再 `.size()`，**性能待优化**（应改为 `COUNT` SQL）

---

### 2.3 JobMapper + JobMapper.xml

**Java接口文件**: `mapper/JobMapper.java` (~65行)
**XML文件**: `resources/mapper/JobMapper.xml`

| 行号(Java) | 方法 | SQL 来源 | SQL 要点 |
|-----------|------|----------|----------|
| 23 | `selectByCompanyId(Long)` | @Select 注解 + XML | `WHERE company_id = ? AND deleted = 0` |
| 31 | `selectByCreatedBy(Long)` | @Select 注解 + XML | `WHERE created_by = ? AND deleted = 0` |
| 39 | `selectActiveJobs(LocalDate)` | @Select 注解 → **XML 用 `end_date`** | `WHERE status = 1 AND end_date >= ? AND deleted = 0` **⚠ 字段不一致** |
| 47 | `selectByStatus(Integer)` | @Select 注解 + XML | `WHERE status = ? AND deleted = 0` |
| 54 | `incrementViewCount(Long)` | XML 自定义 | `UPDATE job SET view_count = view_count + 1 WHERE id = ?` |
| 60 | `countByJobId(Long)` | XML 自定义 | `SELECT COUNT(*) FROM delivery WHERE job_id = ? AND deleted = 0` |

> **⚠ 字段不一致注意**: Java Mapper 注解中 `selectActiveJobs` 使用 `deadline >= #{today}`，但 XML 中使用 `end_date >= #{today}`。**MyBatis 中 XML 会覆盖注解**，实际运行时使用 XML 的 SQL（`end_date`）。请确认数据库列名是 `deadline` 还是 `end_date`，保持一致。

---

### 2.4 JobChangeApply 模块

**文件栈**: `controller/` → 暂未暴露独立 Controller / `service/JobChangeApplyService.java` / `service/impl/JobChangeApplyServiceImpl.java` / `mapper/JobChangeApplyMapper.java` / `mapper/JobChangeApplyMapper.xml`

#### JobChangeApplyService 方法

| 行号(Svc接口) | 方法 | 功能 |
|---------------|------|------|
| 接口 15 | `selectByJobId(Long)` | 根据岗位ID查询变更申请 |
| 接口 21 | `selectByHrId(Long)` | 根据HR ID查询 |
| 接口 27 | `selectByReviewTeacherId(Long)` | 根据审核教师ID查询 |
| 接口 33 | `selectByStatus(Integer)` | 按状态查询（0待审核/1通过/2拒绝） |
| 接口 42 | `submitChangeApply(Long, Long, String)` | 提交申请，init status=0, createTime=now |
| 接口 52 | `reviewChangeApply(Long, Long, boolean, String)` | 审核：校验状态=0才可审核，设置通过/拒绝 + 审核人ID |

#### JobChangeApplyMapper（XML比Java接口多出的方法）

XML 中定义了但 **Java Mapper 接口未声明** 的 SQL：

| XML 方法 | SQL 功能 |
|----------|----------|
| `selectByHrIdAndStatus` | 按HR ID + 状态联合查询 |
| `countByJobId` | 统计岗位的变更申请数量 |
| `countByHrId` | 统计HR的变更申请数量 |

> **⚠ 已知差异**: 这三个方法在 Java 接口中无声明，Service 层无法直接调用。如需使用，需补全 Java Mapper 接口声明。

---

### 2.5 DTO — JobRequest

**文件**: `dto/JobRequest.java`

```java
public class JobRequest {
    private Long companyId;         // 企业ID
    private String title;           // 岗位名称
    private String salaryRange;     // 薪资范围（如"8k-12k"）
    private String education;       // 学历要求
    private String location;        // 工作地点
    private String description;     // 岗位描述
    private String requirement;     // 任职要求
    private String deadline;        // 截止日期 (YYYY-MM-DD)
    private Integer status;         // 状态（0=草稿，1=发布）
}
```

> **注意**: JobController 的 `createJob()` 和 `updateJob()` 直接接收 `@RequestBody Job` 实体而非 `JobRequest` DTO。DTO 当前**未被 Controller 使用**，可能为后续重构预留或前端自行组装。建议统一接口入参以减少暴露风险。

---

## 三、实体字段说明

### 3.1 Job — 岗位表

**表名**: `job`
**文件**: `entity/Job.java`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (自增) | 主键，岗位ID |
| company_id | Long | 外键 → company.id，所属企业 |
| title | String | 岗位名称 |
| salary_range | String | 薪资范围（如 "8k-12k"） |
| education | String | 学历要求 |
| location | String | 工作地点 |
| description | Text | 岗位描述 |
| requirement | Text | 任职要求 |
| deadline | Date | 截止日期（`LocalDate`，仅日期无时间） |
| status | Integer | **0=草稿 / 1=已发布 / 2=已关闭 / 3=暂停** |
| created_by | Long | 外键 → sys_user.id，发布者（教师或HR） |
| view_count | Integer | 浏览次数（每次详情查询自增） |
| qr_code_url | String | 专属二维码图片路径 |
| trace_id | String(32) | 唯一追踪ID（UUID去横线） |
| ai_keywords | Text/JSON | AI从岗位描述提取的关键词 |
| required_skills | Text/JSON | 所需技能标签 |
| create_time | DateTime | 创建时间 |
| update_time | DateTime | 更新时间 |
| deleted | Integer | 逻辑删除（0=正常，1=已删，@TableLogic） |

### 3.2 JobChangeApply — 岗位变更申请记录表

**表名**: `job_change_apply`
**文件**: `entity/JobChangeApply.java`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (自增) | 主键 |
| job_id | Long | 外键 → job.id，关联岗位 |
| hr_id | Long | 外键 → sys_user.id，申请HR |
| change_content | Text/JSON | 变更内容（JSON格式，记录变更的字段和值） |
| status | Integer | **0=待审核 / 1=通过 / 2=拒绝** |
| review_teacher_id | Long | 外键 → sys_user.id，审核教师ID |
| create_time | DateTime | 申请时间 |

> **⚠ 已知不足**: `JobChangeApply` 实体缺少 `feedback` 字段。`reviewChangeApply()` 方法的 `feedback` 参数被注释标记"应保存但无字段"，实际未持久化。如需审核反馈，需在实体中增加 `feedback` 字段。

---

## 四、SQL 关键查询一览

### 岗位列表

```sql
-- 有效岗位（学生端用）
SELECT * FROM job WHERE status = 1 AND deadline >= CURDATE() AND deleted = 0;

-- 按企业查询
SELECT * FROM job WHERE company_id = ? AND deleted = 0;

-- 按发布者查询
SELECT * FROM job WHERE created_by = ? AND deleted = 0;
```

### 统计查询

```sql
-- 增加浏览次数
UPDATE job SET view_count = view_count + 1 WHERE id = ?;

-- 投递数量
SELECT COUNT(*) FROM delivery WHERE job_id = ? AND deleted = 0;
```

---

## 五、潜在问题与优化建议

| 问题 | 影响 | 建议 |
|------|------|------|
| `selectActiveJobs` 注解用 `deadline`，XML 用 `end_date` | 运行时走 XML，若列名不匹配会 SQL 错误 | 统一为 `deadline` 或 `end_date` |
| `countByCompanyId()` 用列表 `.size()` | 数据量大时性能差 | 改为 `SELECT COUNT(*) FROM job WHERE company_id = ? AND deleted = 0` |
| `pauseJob()` 后无法恢复发布 | 暂停岗位无法通过接口恢复 | 扩展状态机：允许暂停→已发布，或改用独立状态 |
| `JobRequest` DTO 未使用 | 额外维护成本 | 启用 DTO 替代 `@RequestBody Job` |
| 变更审核通过后不自动同步 | 教师需手动编辑岗位 | 在 `reviewChangeApply(approved=true)` 中自动应用 `changeContent` 到 Job |
| 缺少 `feedback` 字段 | 审核反馈无法持久化 | 实体增加 `feedback` 字段 + 更新 SQL |
| XML 中隐藏方法 | `selectByHrIdAndStatus` 等 3 个方法不可用 | 补全 Java Mapper 接口声明 |

---

## 六、时序图

### 岗位发布流程

```
HR/Teacher                   JobController                JobService                 数据库
    │                             │                           │                        │
    │ POST /jobs                  │                           │                        │
    │────────────────────────────▶│                           │                        │
    │                             │ save(job)                 │                        │
    │                             │──────────────────────────▶│                        │
    │                             │                           │ INSERT job              │
    │                             │                           │────────────────────────▶│
    │  "创建成功（草稿）"          │                           │                        │
    │◀────────────────────────────│                           │                        │
    │                             │                           │                        │
    │ PUT /jobs/{id}/publish      │                           │                        │
    │────────────────────────────▶│                           │                        │
    │                             │ publishJob(id)            │                        │
    │                             │──────────────────────────▶│                        │
    │                             │                           │ getById → status=0?    │
    │                             │                           │────────────────────────▶│
    │                             │                           │ generateTraceId        │
    │                             │                           │ generateQrCode         │
    │                             │                           │ UPDATE status=1+...    │
    │                             │                           │────────────────────────▶│
    │  "发布成功"                  │                           │                        │
    │◀────────────────────────────│                           │                        │
```

### 岗位变更申请流程

```
HR                         JobChangeApplyService              Teacher
 │                                  │                           │
 │ submitChangeApply()             │                           │
 │────────────────────────────────▶│                           │
 │                                  │ INSERT(status=0)         │
 │                                  │───存入数据库───          │
 │ "提交成功，待审核"                │                           │
 │◀─────────────────────────────────│                           │
 │                                  │                           │
 │                                  │     reviewChangeApply()  │
 │                                  │◀──────────────────────────│
 │                                  │ getById → status=0?      │
 │                                  │   approved? 1:2          │
 │                                  │───存入数据库───          │
 │                                  │  "审核完成"               │
 │                                  │──────────────────────────▶│
```

---

> **修改日志**: 本文档基于 v1.0 源码（2026-06-08）产出，修改岗位相关代码后需同步更新本文档。
