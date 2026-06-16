# 企业管理模块文档

> 模块路径: `com.recruit.controller.CompanyController` + `com.recruit.service.CompanyService` + `com.recruit.service.impl.CompanyServiceImpl` + `com.recruit.mapper.CompanyMapper`
> 涉及文件: 1 Controller / 1 Service接口 / 1 Service实现 / 1 Mapper接口 / 1 Mapper XML / 1 Entity / 1 DTO

---

## 一、原理文档

### 1. 模块职责

管理合作企业的全生命周期信息，包括：
- 企业信息的增删改查（CRUD）
- 企业合作等级管理（潜在 → 合作中 → 核心 → 已流失）
- 联系电话 AES 加密存储与解密返回（敏感字段保护）
- 企业信息的模糊搜索
- 企业数据的逻辑删除

### 2. 架构设计

```
客户端 (Browser/App)
    │
    ├─ GET /companies ────────────────▶ CompanyController.getAllCompanies()
    │                                       │
    │                                       ├─ companyService.list()
    │                                       └─ aesUtil.decrypt() [批量解密联系方式]
    │
    ├─ GET /companies/{id} ───────────▶ CompanyController.getCompanyById(id)
    │                                       │
    │                                       ├─ companyService.getById(id)
    │                                       └─ aesUtil.decrypt() [单条解密联系方式]
    │
    ├─ POST /companies ───────────────▶ CompanyController.createCompany()
    │                                       │
    │                                       ├─ aesUtil.encrypt() [加密联系方式]
    │                                       └─ companyService.save()
    │
    ├─ PUT /companies/{id} ───────────▶ CompanyController.updateCompany()
    │                                       │
    │                                       ├─ companyService.getById(id) [验存在]
    │                                       ├─ aesUtil.encrypt() [加密联系方式]
    │                                       └─ companyService.updateById()
    │
    ├─ DELETE /companies/{id} ────────▶ CompanyController.deleteCompany()
    │                                       │
    │                                       ├─ companyService.getById(id) [验存在]
    │                                       └─ companyService.updateById() [逻辑删除]
    │
    ├─ GET /companies/search ─────────▶ CompanyController.searchCompanies(keyword)
    │                                       │
    │                                       ├─ 按名称/简称模糊匹配 (LambdaQuery)
    │                                       └─ aesUtil.decrypt() [批量解密联系方式]
    │
    ├─ GET /companies/by-cooperation-level/{lv} ──▶ CompanyController.getCompaniesByCooperationLevel(lv)
    │
    ├─ GET /companies/by-industry/{industry} ─────▶ CompanyController.getCompaniesByIndustry(industry)
    │
    └─ PUT /companies/{id}/cooperation-level ─────▶ CompanyController.updateCooperationLevel(id, params)
```

### 3. 企业合作等级机制

企业没有传统的"审核状态"概念，而是采用**合作等级**来描述校企合作深度：

| 等级值 | 含义 | 说明 |
|--------|------|------|
| 0 | 潜在 | 初步接触，尚未正式合作 |
| 1 | 合作中 | 已建立正式合作关系 |
| 2 | 核心 | 深度合作企业（长期实习生输送、订单班等） |
| 3 | 已流失 | 合作中断或已终止 |

> **注意**：`cooperationLevel` 是合作深度标记，**不是审核流程**。系统中企业信息创建后可直接使用，无需审核。

### 4. 敏感字段加密设计

- `contact_phone`（联系电话）在入库前通过 `AESUtil.encrypt()` 加密
- 查询出库时通过 `AESUtil.decrypt()` 解密后返回给前端
- 搜索场景同样解密后返回，确保前端获得明文
- 更新时如果传入了新的联系电话则重新加密存储

### 5. 级联删除逻辑（逻辑删除）

系统采用**全表逻辑删除**方案（MyBatis-Plus `@TableLogic`），`company`、`job`、`delivery` 等所有业务表均包含 `deleted` 字段。

**删除企业时不会自动删除关联岗位**，原因如下：

1. **外键约束未启用** — schema.sql 中的外键声明被注释掉了（`-- ALTER TABLE`）
2. **逻辑删除设计** — `deleted=1` 仅标记"不可见"，数据仍存在
3. **关联业务数据的完整性** — 即使企业被逻辑删除，其发布的岗位仍可能处于招聘周期中，投递记录仍需保留

**实际影响链：**

```
company (deleted=1)         ← 企业被软删除
  │
  ├─ job (company_id, deleted=0)  ← 岗位数据残留，但前台不可见企业信息
  │     └─ delivery (job_id)      ← 投递记录不受影响
  ├─ job_change_apply             ← 变更申请记录不受影响
  └─ job_match_record             ← 匹配记录不受影响
```

如需**物理级联删除**（实际代码中未实现），需在 Service 层额外编写事务方法：

```java
@Transactional(rollbackFor = Exception.class)
public void deleteCompanyCascade(Long companyId) {
    // 1. 物理删除岗位
    jobService.remove(lambdaQuery().eq(Job::getCompanyId, companyId));
    // 2. 删除相关投递记录
    deliveryService.remove(lambdaQuery().eq(Delivery::getCompanyId, companyId)); // 需联表
    // 3. 最后删除企业
    companyService.removeById(companyId);
}
```

### 6. 搜索功能实现

支持按企业全称（`name`）和简称（`shortName`）进行模糊搜索：

```java
companyService.lambdaQuery()
    .like(Company::getName, keyword)
    .or()
    .like(Company::getShortName, keyword)
    .list();
```

使用 MyBatis-Plus `LambdaQueryWrapper` 实现 `OR` 条件组合，自动处理逻辑删除过滤。

---

## 二、实体定义

### 2.1 Company 实体

**文件**: `entity/Company.java`

| 字段 | 类型 | 数据库列 | 说明 |
|------|------|----------|------|
| id | Long | `id` (PK, AUTO) | 企业ID 主键 |
| name | String | `name` (UNIQUE) | 企业全称，唯一索引 |
| shortName | String | `short_name` | 企业简称 |
| licenseUrl | String | `license_url` | 营业执照图片路径 |
| industry | String | `industry` | 所属行业 |
| address | String | `address` | 企业地址 |
| contactPerson | String | `contact_person` | 联系人姓名 |
| contactPhone | String | `contact_phone` | 联系电话（AES加密存储） |
| cooperationLevel | Integer | `cooperation_level` | 合作等级：0=潜在，1=合作中，2=核心，3=已流失 |
| lastRecruitTime | LocalDateTime | `last_recruit_time` | 最近一次招聘时间 |
| createTime | LocalDateTime | `create_time` | 创建时间（自动填充） |
| deleted | Integer | `deleted` | 逻辑删除标志（`@TableLogic`） |

### 2.2 CompanyRequest DTO

**文件**: `dto/CompanyRequest.java`

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 企业全称 |
| shortName | String | 企业简称 |
| industry | String | 所属行业 |
| address | String | 企业地址 |
| contactPerson | String | 联系人 |
| contactPhone | String | 联系电话（入库前加密） |
| cooperationLevel | Integer | 合作等级 |

> **注意**：Controller 中创建/更新方法直接使用 `@RequestBody Company` 实体而非 `CompanyRequest` DTO，因此 `CompanyRequest` 目前**未被 Controller 引用**，可能是预留或前端适配用途。

---

## 三、实现文档（函数级）

### 3.1 CompanyController — `/companies/**`

**文件**: `controller/CompanyController.java` (207行)

#### 3.1.1 查询类接口

| 行号 | 方法 | HTTP | 路径 | 参数 | 功能说明 |
|------|------|------|------|------|----------|
| 30-43 | `getAllCompanies()` | **GET** | `/companies` | — | 获取所有企业列表（未删除），解密联系电话后返回 |
| 51-64 | `getCompanyById()` | **GET** | `/companies/{id}` | `@PathVariable Long id` | 根据 ID 获取企业详情，不存在返回 404 |
| 72-86 | `getCompaniesByCooperationLevel()` | **GET** | `/companies/by-cooperation-level/{cooperationLevel}` | `@PathVariable Integer cooperationLevel` | 按合作等级筛选企业列表 |
| 93-107 | `getCompaniesByIndustry()` | **GET** | `/companies/by-industry/{industry}` | `@PathVariable String industry` | 按行业筛选企业列表 |
| 163-175 | `searchCompanies()` | **GET** | `/companies/search` | `@RequestParam String keyword` | 按名称/简称模糊搜索 |

#### 3.1.2 写操作接口

| 行号 | 方法 | HTTP | 路径 | 参数 | 功能说明 |
|------|------|------|------|------|----------|
| 114-121 | `createCompany()` | **POST** | `/companies` | `@RequestBody Company` | 创建企业，联系电话自动 AES 加密后入库 |
| 128-144 | `updateCompany()` | **PUT** | `/companies/{id}` | `@PathVariable Long id`, `@RequestBody Company` | 更新企业，先验存在再更新，联系电话重新加密 |
| 152-161 | `deleteCompany()` | **DELETE** | `/companies/{id}` | `@PathVariable Long id` | **软删除**，设置 `deleted=1` |
| 180-195 | `updateCooperationLevel()` | **PUT** | `/companies/{id}/cooperation-level` | `@PathVariable Long id`, `@RequestBody Map<String, Integer>` | 更新合作等级，校验参数范围 0-3 |

#### 3.1.3 Controller 层逻辑要点

- **所有查询接口**返回前均通过 `aesUtil.decrypt()` 解密 `contactPhone`，确保前端拿到明文
- **创建/更新接口**在入库前通过 `aesUtil.encrypt()` 加密 `contactPhone`
- 更新时先查企业是否存在，不存在返回 `Result.error(404, "企业不存在")`
- 删除时查企业是否存在，不存在同样返回 404
- `updateCooperationLevel` 对 `cooperationLevel` 做范围校验（0-3），不合规返回错误

### 3.2 CompanyService — 服务接口

**文件**: `service/CompanyService.java`

| 行号 | 方法 | 功能说明 |
|------|------|----------|
| 17-21 | `selectByCooperationLevel(Integer)` | 根据合作等级查询企业 |
| 26-29 | `selectByIndustry(String)` | 根据行业查询企业 |
| 34-37 | `updateCooperationLevel(Long, Integer)` | 更新企业合作等级 |
| 42-45 | `updateLastRecruitTime(Long)` | 更新最近招聘时间（由外部调用触发） |

接口继承 `IService<Company>`，自动获得 MyBatis-Plus 提供的 `save()`、`updateById()`、`getById()`、`list()`、`lambdaQuery()` 等标准方法。

### 3.3 CompanyServiceImpl — 服务实现

**文件**: `service/impl/CompanyServiceImpl.java`

| 行号 | 方法 | 功能说明 | 事务 |
|------|------|----------|------|
| 31-33 | `selectByCooperationLevel(Integer)` | 委托 Mapper 按合作等级查询 | — |
| 37-39 | `selectByIndustry(String)` | 委托 Mapper 按行业查询 | — |
| 42-48 | `updateCooperationLevel(Long, Integer)` | 查存在 → 设置等级 → 更新 | `@Transactional` |
| 51-58 | `updateLastRecruitTime(Long)` | 查存在 → 设置时间为 now → 更新 | `@Transactional` |

### 3.4 CompanyMapper — 数据访问层

**文件**: `mapper/CompanyMapper.java`

| 行号 | 方法 | 功能说明 |
|------|------|----------|
| 20-22 | `selectByCooperationLevel(Integer)` | `@Select` 注解 SQL，查询未删除的指定合作等级企业 |
| 29-31 | `selectByIndustry(String)` | `@Select` 注解 SQL，查询未删除的指定行业企业 |

**文件**: `resources/mapper/CompanyMapper.xml` — XML 映射

| XML ID | SQL 说明 |
|--------|----------|
| `selectByCooperationLevel` | 按合作等级查询（已删除过滤） |
| `selectByIndustry` | 按行业查询（已删除过滤） |
| `selectByNameLike` | 按名称模糊查询（Controller 未直接引用，使用 LambdaQuery 替代） |
| `updateCooperationLevel` | 更新合作等级（Controller 调用的是 Service 层版本） |
| `updateStatus` | 更新企业状态（启用/禁用，Controller 未使用此方法） |

### 3.5 辅助工具 — AESUtil

**文件**: `utils/AESUtil.java`

| 方法 | 说明 |
|------|------|
| `encrypt(plainText)` | AES 加密，用于入库前的联系电话加密 |
| `decrypt(cipherText)` | AES 解密，用于出库时的联系电话解密 |

---

## 四、与设计任务的差异说明

任务描述中提到的以下概念在**实际代码中不存在**：

| 任务描述概念 | 实际情况 | 说明 |
|------------|----------|------|
| `status` (0=待审核/1=通过/2=拒绝) | 不存在，改用 `cooperationLevel` (0=潜在/1=合作中/2=核心/3=已流失) | 企业无审核流程，只有合作等级标记 |
| `/companies/my-company` | 不存在 | 当前 Controller 未实现此接口 |
| `PUT /admin/companies/{id}/approve` | 不存在 | 无审核机制 |
| `PUT /admin/companies/{id}/reject` | 不存在 | 无审核机制 |
| `contact_name` 字段 | 实际为 `contactPerson` | 命名差异 |
| `contact_email` 字段 | 不存在 | — |
| `logo_url` 字段 | 不存在，但存在 `licenseUrl`（营业执照） | — |
| `scale` 字段 | 不存在 | — |

---

## 五、Database 关系

```sql
-- 企业表
CREATE TABLE `company` (
    `id`                  BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '企业ID',
    `name`                VARCHAR(100) NOT NULL COMMENT '企业全称',
    `short_name`          VARCHAR(50)  DEFAULT NULL COMMENT '简称',
    `license_url`         VARCHAR(255) DEFAULT NULL COMMENT '营业执照图片路径',
    `industry`            VARCHAR(50)  DEFAULT NULL COMMENT '行业',
    `address`             VARCHAR(200) DEFAULT NULL COMMENT '地址',
    `contact_person`      VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
    `contact_phone`       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话（AES加密）',
    `cooperation_level`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '合作等级',
    `last_recruit_time`   DATETIME     DEFAULT NULL COMMENT '最近招聘时间',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`             INT(1)       DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业表';
```

**关联表**：`job.company_id → company.id`（外键注释未启用）

---

## 六、异常处理参考

| 场景 | HTTP 状态码响应 | 错误消息 |
|------|----------------|----------|
| 查询不存在的企业 | 404 | "企业不存在" |
| 更新不存在的企业 | 404 | "企业不存在" |
| 删除不存在的企业 | 404 | "企业不存在" |
| 合作等级参数非法 | 400 | "cooperationLevel参数错误（应为0-3）" |
| 更新合作等级失败 | 400 | "更新失败" |

---

> **修改日志**: 本文档基于 v1.0 代码梳理产出，涉及企业模块的接口变更后需同步更新。
