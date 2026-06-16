# 班级管理模块文档（Class Module）

## 目录

1. [设计原理](#1-设计原理)
   - 1.1 功能概述
   - 1.2 班级-学生多对多关联设计
   - 1.3 教师分配班级逻辑
   - 1.4 软删除设计
2. [实体定义](#2-实体定义)
   - 2.1 Class 实体
   - 2.2 StudentClass 实体
   - 2.3 数据库表结构
3. [实现文档](#3-实现文档)
   - 3.1 模块文件清单
   - 3.2 Controller 层
   - 3.3 Service 层
   - 3.4 Mapper 层
4. [业务逻辑详解](#4-业务逻辑详解)
   - 4.1 添加/移除学生
   - 4.2 批量操作
   - 4.3 查询学生所在班级
5. [API 接口汇总](#5-api-接口汇总)
6. [安全与注意事项](#6-安全与注意事项)

---

## 1. 设计原理

### 1.1 功能概述

班级管理模块面向**教师角色**，提供以下核心功能：

- **班级 CRUD**：创建、查看、更新、删除班级（软删除）
- **学生管理**：将学生添加到班级、从班级移除、批量操作
- **多维查询**：按教师、专业、年级筛选班级列表
- **双向关联**：可查询班级下的学生列表，也可查询学生所属的所有班级

### 1.2 班级-学生多对多关联设计

一个学生可以加入多个班级（如：主修班 + 辅修班），一个班级包含多个学生，因此是**多对多关系**。实现方案通过**中间表 `student_class`** 进行解耦。

```
┌─────────────┐       ┌──────────────────┐       ┌─────────────┐
│    class    │       │  student_class   │       │  sys_user   │
├─────────────┤       ├──────────────────┤       ├─────────────┤
│ id (PK)     │──1:N──│ class_id         │       │ id (PK)     │
│ name        │       │ student_id       │──N:1──│ username    │
│ teacher_id  │       │ id (PK)          │       │ real_name   │
│ major       │       │ UNIQUE(student_id│       │ role=0(学生)│
│ grade       │       │       ,class_id) │       │ ...         │
│ deleted     │       └──────────────────┘       └─────────────┘
└─────────────┘
```

#### 中间表详解

`student_class` 表是最简设计，仅含三个字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT (PK, AUTO_INCREMENT) | 主键 |
| `student_id` | BIGINT (FK → sys_user.id) | 学生用户ID |
| `class_id` | BIGINT (FK → class.id) | 班级ID |

**唯一约束**：`UNIQUE(student_id, class_id)` 确保同一学生不会重复加入同一班级，数据库层防重。

#### 为何选择中间表而非外键聚合？

| 方式 | 缺点 |
|------|------|
| class 表加 `student_ids` 逗号分隔字段 | 违反第一范式，查询/更新困难，无完整性约束 |
| student 表加 `class_id` 单字段 | 仅支持一对多，无法一个学生多班级 |
| **中间表 `student_class`** | **最灵活，支持多对多，查询高效，约束完整** |

### 1.3 教师分配班级逻辑

#### 实体层面

`Class` 实体包含 `teacherId` 字段（外键 → `sys_user.id`），标识该班级的班主任/负责教师。

```java
private Long teacherId;  // 班主任/教师ID - 外键(sys_user.id)
```

`sys_user` 表中，`role=1` 的角色为教师。创建班级时 `teacherId` 必须指向一个有效的教师用户。

#### 查询层面

ClassController 提供 `GET /classes/by-teacher/{teacherId}` 接口，用于查询某教师所管理的所有班级。

```java
// ClassMapper.java
@Select("SELECT * FROM class WHERE teacher_id = #{teacherId} AND deleted = 0")
List<Class> selectByTeacherId(@Param("teacherId") Long teacherId);
```

#### 教师操作权限（隐含约束）

- `ClassController` 标记 `@RestController @RequestMapping("/classes")`，当前未显式加 `@PreAuthorize` 注解
- 注释写明"只有教师可以访问"，实际权限控制由上层 **JWT 拦截器** + 调用侧自行校验
- 建议前端在调用班级管理 API 时，仅对 `role=1` 的用户开放入口

### 1.4 软删除设计

`Class` 实体采用 MyBatis-Plus 的 `@TableLogic` 逻辑删除：

```java
@TableLogic
private Integer deleted;  // 0=未删除，1=已删除
```

- **删除班级**：不执行 `DELETE`，而是 `UPDATE class SET deleted=1 WHERE id=xxx`
- **查询自动过滤**：MyBatis-Plus 在 `list()`、`getById()` 等基础方法中自动附加 `AND deleted=0`
- **自定义查询显式过滤**：Mapper XML 中所有自定义 `SELECT` 均手动追加 `AND deleted = 0`
- **注意**：删除班级时**不会自动删除** `student_class` 中的关联记录。若需要级联清理，应在 Service 层补充逻辑（当前版本未实现）

---

## 2. 实体定义

### 2.1 Class 实体

**文件**：`entity/Class.java`

| 字段 | 类型 | 注解 | 说明 |
|------|------|------|------|
| `id` | `Long` | `@TableId(type = IdType.AUTO)` | 班级ID，主键自增 |
| `name` | `String` | — | 班级名称 |
| `teacherId` | `Long` | — | 班主任/教师ID（外键 → sys_user.id） |
| `major` | `String` | — | 专业名称 |
| `grade` | `String` | — | 年级（如"2023级"） |
| `createTime` | `LocalDateTime` | — | 创建时间 |
| `deleted` | `Integer` | `@TableLogic` | 逻辑删除标志（0=未删，1=已删） |

- 使用 Lombok `@Data` 自动生成 Getter/Setter/toString
- `@TableName("class")` 映射数据库表 `class`
- 继承 MyBatis-Plus `ServiceImpl<ClassMapper, Class>` 自动获得基础 CRUD

### 2.2 StudentClass 实体

**文件**：`entity/StudentClass.java`

| 字段 | 类型 | 注解 | 说明 |
|------|------|------|------|
| `id` | `Long` | `@TableId(type = IdType.AUTO)` | 主键，自增 |
| `studentId` | `Long` | — | 学生ID（外键 → sys_user.id） |
| `classId` | `Long` | — | 班级ID（外键 → class.id） |

- 纯关联表实体，无逻辑删除字段
- 数据库层通过 `UNIQUE(student_id, class_id)` 防止重复关联

### 2.3 数据库表结构

#### class 表

```sql
CREATE TABLE `class` (
    `id`          BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '班级ID',
    `name`        VARCHAR(50)  NOT NULL COMMENT '班级名称',
    `teacher_id`  BIGINT(20)   NOT NULL COMMENT '班主任/教师ID',
    `major`       VARCHAR(50)   DEFAULT NULL COMMENT '专业名称',
    `grade`       VARCHAR(10)   DEFAULT NULL COMMENT '年级（如2023级）',
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`     INT(1)        DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级表';
```

#### student_class 表

```sql
CREATE TABLE `student_class` (
    `id`         BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id` BIGINT(20) NOT NULL COMMENT '学生ID',
    `class_id`   BIGINT(20) NOT NULL COMMENT '班级ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_class` (`student_id`, `class_id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生-班级关联表';
```

---

## 3. 实现文档

### 3.1 模块文件清单

| 层级 | 文件路径 | 行数 | 说明 |
|------|---------|------|------|
| Controller | `controller/ClassController.java` | 245 | 班级管理 REST API |
| Service 接口 | `service/ClassService.java` | — | 班级服务接口定义 |
| Service 实现 | `service/impl/ClassServiceImpl.java` | — | 班级服务核心逻辑 |
| Service 接口 | `service/StudentClassService.java` | — | 学生-班级关联服务接口 |
| Service 实现 | `service/impl/StudentClassServiceImpl.java` | — | 关联表服务逻辑 |
| Mapper 接口 | `mapper/ClassMapper.java` | — | 班级 MyBatis Mapper |
| Mapper XML | `resources/mapper/ClassMapper.xml` | — | 班级自定义 SQL |
| Mapper 接口 | `mapper/StudentClassMapper.java` | — | 关联表 Mapper |
| Mapper XML | `resources/mapper/StudentClassMapper.xml` | — | 关联表自定义 SQL |
| Entity | `entity/Class.java` | — | 班级实体 |
| Entity | `entity/StudentClass.java` | — | 关联表实体 |

### 3.2 Controller 层

**文件**：`controller/ClassController.java`（共 245 行）

类级注解：`@RestController @RequestMapping("/classes")`

#### 方法对照表

| # | 行号 | HTTP 方法 | 路径 | 方法名 | 功能 |
|---|------|-----------|------|--------|------|
| 1 | 25-33 | GET | `/classes` | `getAllClasses()` | 获取所有班级列表 |
| 2 | 34-47 | GET | `/classes/{id}` | `getClassById(Long id)` | 根据ID查询班级 |
| 3 | 48-56 | GET | `/classes/by-teacher/{teacherId}` | `getClassesByTeacherId(Long teacherId)` | 按教师ID查询班级 |
| 4 | 57-65 | GET | `/classes/by-major/{major}` | `getClassesByMajor(String major)` | 按专业名称查询班级 |
| 5 | 66-74 | GET | `/classes/by-grade/{grade}` | `getClassesByGrade(String grade)` | 按年级查询班级 |
| 6 | 75-83 | POST | `/classes` | `createClass(Class clazz)` | 创建班级 |
| 7 | 84-97 | PUT | `/classes/{id}` | `updateClass(Long id, Class clazz)` | 更新班级信息 |
| 8 | 98-112 | DELETE | `/classes/{id}` | `deleteClass(Long id)` | 软删除班级 |
| 9 | 113-131 | POST | `/classes/{classId}/students` | `addStudentToClass(Long classId, Map params)` | 添加学生到班级 |
| 10 | 132-146 | DELETE | `/classes/{classId}/students/{studentId}` | `removeStudentFromClass(Long classId, Long studentId)` | 从班级移除学生 |
| 11 | 147-161 | GET | `/classes/{classId}/students` | `getClassStudents(Long classId)` | 获取班级学生列表 |
| 12 | 162-183 | POST | `/classes/{classId}/students/batch` | `batchAddStudentsToClass(Long classId, Map params)` | 批量添加学生 |
| 13 | 184-204 | DELETE | `/classes/{classId}/students/batch` | `batchRemoveStudentsFromClass(Long classId, Map params)` | 批量移除学生 |

> **注**：行号为源码中的实际行号范围（含 javadoc 和注解行）。

#### 详细方法说明

##### (1) getAllClasses — 获取所有班级列表
```
GET /classes
```
- **行号**: 25-33
- **参数**: 无
- **返回**: `Result<List<Class>>` — 所有未删除的班级
- **实现**: 直接调用 `classService.list()`，MyBatis-Plus 自动附加 `deleted=0`

##### (2) getClassById — 根据ID查询班级
```
GET /classes/{id}
```
- **行号**: 34-47
- **参数**: `@PathVariable Long id` — 班级ID
- **返回**: `Result<Class>` — 班级实体，不存在返回 404 错误
- **异常处理**: 如果班级不存在，返回 `Result.error(404, "班级不存在")`

##### (3) getClassesByTeacherId — 按教师查询班级
```
GET /classes/by-teacher/{teacherId}
```
- **行号**: 48-56
- **参数**: `@PathVariable Long teacherId` — 教师(班主任)ID
- **返回**: `Result<List<Class>>` — 该教师管理的所有班级
- **SQL**: `SELECT * FROM class WHERE teacher_id = #{teacherId} AND deleted = 0`

##### (4) getClassesByMajor — 按专业查询班级
```
GET /classes/by-major/{major}
```
- **行号**: 57-65
- **参数**: `@PathVariable String major` — 专业名称
- **返回**: `Result<List<Class>>` — 该专业下的所有班级

##### (5) getClassesByGrade — 按年级查询班级
```
GET /classes/by-grade/{grade}
```
- **行号**: 66-74
- **参数**: `@PathVariable String grade` — 年级（如"2023级"）
- **返回**: `Result<List<Class>>` — 该年级的所有班级

##### (6) createClass — 创建班级
```
POST /classes
```
- **行号**: 75-83
- **请求体**: `@RequestBody Class clazz` — 班级实体（JSON）
  - 必填：`name`, `teacherId`
  - 可选：`major`, `grade`
- **返回**: `Result<String>` — "班级创建成功"

##### (7) updateClass — 更新班级
```
PUT /classes/{id}
```
- **行号**: 84-97
- **参数**: `@PathVariable Long id` — 班级ID
- **请求体**: `@RequestBody Class clazz` — 要更新的字段
- **校验**: 先查询班级是否存在，不存在则返回 404
- **返回**: `Result<String>` — "班级更新成功"

##### (8) deleteClass — 删除班级（软删除）
```
DELETE /classes/{id}
```
- **行号**: 98-112
- **参数**: `@PathVariable Long id` — 班级ID
- **实现**: 设置 `deleted = 1` 后调用 `updateById()`
- **注意**: 当前版本删除班级时**不会级联清理** `student_class` 关联记录。这意味着：
  - 当班级被软删除后，`student_class` 表中仍保留关联数据
  - 如果后续恢复班级（`deleted=0`），之前的关联关系仍然有效
  - 查询时因 `student_class` 表无条件过滤，服务端获取 `studentIds` 不受影响

##### (9) addStudentToClass — 添加学生
```
POST /classes/{classId}/students
```
- **行号**: 113-131
- **参数**:
  - `@PathVariable Long classId` — 班级ID
  - `@RequestBody Map<String, Long> params` — 请求体，包含 `studentId`
- **返回**: `Result<String>` — "学生添加成功" 或错误信息
- **校验**: studentId 非空检查
- **服务层逻辑**（详见 4.1 节）

##### (10) removeStudentFromClass — 移除学生
```
DELETE /classes/{classId}/students/{studentId}
```
- **行号**: 132-146
- **参数**:
  - `@PathVariable Long classId` — 班级ID
  - `@PathVariable Long studentId` — 学生ID
- **返回**: `Result<String>` — "学生移除成功" 或错误信息

##### (11) getClassStudents — 获取班级学生列表
```
GET /classes/{classId}/students
```
- **行号**: 147-161
- **参数**: `@PathVariable Long classId` — 班级ID
- **返回**: `Result<List<SysUser>>` — 学生用户实体列表
- **实现流程**:
  1. `classService.getStudentIdsByClassId(classId)` → 获取学生ID列表
  2. 逐个调用 `userService.getById(studentId)` 获取完整用户信息
  3. 过滤掉 `null`（已被删除或禁用的用户）
- **性能注意**: 当前实现是 N+1 查询（逐个查询用户），学生较多时建议优化为批量查询

##### (12) batchAddStudentsToClass — 批量添加学生
```
POST /classes/{classId}/students/batch
```
- **行号**: 162-183
- **参数**:
  - `@PathVariable Long classId` — 班级ID
  - `@RequestBody Map<String, List<Long>> params` — 包含 `studentIds` 列表
- **返回**: `Result<Map<String, Integer>>`
  - `successCount`: 实际成功添加的数量
  - `totalCount`: 请求添加的总数

##### (13) batchRemoveStudentsFromClass — 批量移除学生
```
DELETE /classes/{classId}/students/batch
```
- **行号**: 184-204
- **参数**:
  - `@PathVariable Long classId` — 班级ID
  - `@RequestBody Map<String, List<Long>> params` — 包含 `studentIds` 列表
- **返回**: `Result<Map<String, Integer>>`
  - `successCount`: 实际成功移除的数量
  - `totalCount`: 请求移除的总数

### 3.3 Service 层

#### ClassService 接口 & ClassServiceImpl 实现

继承 `IService<Class>` + `ServiceImpl<ClassMapper, Class>`，获得 MyBatis-Plus 基础 CRUD。

| 方法 | 说明 |
|------|------|
| `selectByTeacherId(Long teacherId)` | 调用 Mapper 按教师查询 |
| `selectByMajor(String major)` | 调用 Mapper 按专业查询 |
| `selectByGrade(String grade)` | 调用 Mapper 按年级查询 |
| `addStudentToClass(Long classId, Long studentId)` | 单个添加，含校验，**事务** |
| `removeStudentFromClass(Long classId, Long studentId)` | 单个移除，通过中间表删除 |
| `batchAddStudentsToClass(Long classId, List<Long> studentIds)` | 批量添加，去重，**事务** |
| `batchRemoveStudentsFromClass(Long classId, List<Long> studentIds)` | 批量移除，**事务** |
| `getStudentIdsByClassId(Long classId)` | 查询班级中所有学生的ID列表 |
| `getClassesByStudentId(Long studentId)` | **反向查询**：学生所在的所有班级 |

> 所有涉及数据修改的方法均标注 `@Transactional(rollbackFor = Exception.class)`

#### StudentClassService 接口 & StudentClassServiceImpl 实现

提供中间表的直接操作，是 ClassService 的辅助服务层：

| 方法 | 说明 |
|------|------|
| `selectByStudentId(Long)` | 按学生查关联 |
| `selectByClassId(Long)` | 按班级查关联 |
| `batchAddStudentsToClass(Long, List<Long>)` | 批量添加（含去重） |
| `batchRemoveStudentsFromClass(Long, List<Long>)` | 批量移除 |

> 当前 Controller 层主要调用 ClassService，StudentClassService 更多作备用/扩展。

### 3.4 Mapper 层

#### ClassMapper — 班级 Mapper

```java
@Mapper
public interface ClassMapper extends BaseMapper<Class> {
    List<Class> selectByTeacherId(Long teacherId);
    List<Class> selectByMajor(String major);
    List<Class> selectByGrade(String grade);
}
```

- 使用 MyBatis-Plus `BaseMapper` 自动获得 `insert`、`deleteById`、`updateById`、`selectById`、`selectList` 等
- 三个自定义查询通过注解 `@Select` 实现 SQL
- **对应 XML**（`ClassMapper.xml`）中另有：`selectByNameLike`（模糊查询）、`updateStudentCount`（更新学生数）两个方法

#### StudentClassMapper — 关联表 Mapper

```java
@Mapper
public interface StudentClassMapper extends BaseMapper<StudentClass> {
    List<StudentClass> selectByStudentId(Long studentId);
    List<StudentClass> selectByClassId(Long classId);
    int deleteByStudentIdAndClassId(Long studentId, Long classId);
}
```

- **对应 XML** 中另有：`batchInsert`、`deleteByClassId`、`deleteByStudentId`

---

## 4. 业务逻辑详解

### 4.1 添加/移除学生（ClassServiceImpl）

#### addStudentToClass 流程

```
1. 校验班级是否存在 → getById(classId)，不存在则抛异常
2. 查班级现有学生 → studentClassMapper.selectByClassId(classId)
3. 去重检查 → 遍历检查 studentId 是否已存在
               若已存在则抛异常 "该学生已在班级中"
4. 创建关联记录 → new StudentClass(studentId, classId)
5. 写入中间表 → studentClassMapper.insert(studentClass)
```

**事务保证**：`@Transactional(rollbackFor = Exception.class)`，任意步骤失败回滚。

#### removeStudentFromClass 流程

```
1. 直接删除关联 → studentClassMapper.deleteByStudentIdAndClassId(studentId, classId)
2. 检查影响行数 → rows > 0 返回 true，否则返回 false
```

> 注意：当前实现不会校验班级是否存在，移除了不存在的关联返回 false，但不会抛异常。

### 4.2 批量操作

#### batchAddStudentsToClass 流程

```
1. 校验班级是否存在
2. 获取班级现有学生ID列表
3. 遍历待添加列表：
   - 如果 studentId 不在现有列表 → 插入关联，count++
   - 如果已在列表中 → 跳过（不抛异常，静默去重）
4. 返回实际成功添加的数量
```

#### batchRemoveStudentsFromClass 流程

```
1. 遍历 studentIds
2. 逐个调用 deleteByStudentIdAndClassId
3. 成功删除的 → count++
4. 返回实际成功移除的数量
```

### 4.3 查询学生所在班级（getClassesByStudentId）

提供**反向查询**能力：给定一个学生ID，返回该学生所属的所有班级（未删除的）。

```
1. 查中间表 → studentClassMapper.selectByStudentId(studentId)
2. 逐一查班级 → getById(sc.getClassId())，过滤 null
3. 返回班级列表
```

---

## 5. API 接口汇总

| 方法 | 路径 | 说明 | 请求体/参数 |
|------|------|------|-------------|
| GET | `/classes` | 所有班级列表 | — |
| GET | `/classes/{id}` | 按ID查班级 | 路径参数 `id` |
| GET | `/classes/by-teacher/{teacherId}` | 按教师查班级 | 路径参数 `teacherId` |
| GET | `/classes/by-major/{major}` | 按专业查班级 | 路径参数 `major` |
| GET | `/classes/by-grade/{grade}` | 按年级查班级 | 路径参数 `grade` |
| POST | `/classes` | 创建班级 | `Class` JSON |
| PUT | `/classes/{id}` | 更新班级 | 路径 `id` + `Class` JSON |
| DELETE | `/classes/{id}` | 删除班级（软删） | 路径参数 `id` |
| POST | `/classes/{classId}/students` | 添加学生 | `{"studentId": 123}` |
| DELETE | `/classes/{classId}/students/{studentId}` | 移除学生 | 路径参数 |
| GET | `/classes/{classId}/students` | 班级学生列表 | 路径参数 `classId` |
| POST | `/classes/{classId}/students/batch` | 批量添加 | `{"studentIds": [1,2,3]}` |
| DELETE | `/classes/{classId}/students/batch` | 批量移除 | `{"studentIds": [1,2,3]}` |

---

## 6. 安全与注意事项

### 权限控制

- 当前 Controller 未显式加 Spring Security 注解（如 `@PreAuthorize`）
- 注释要求"只有教师可以访问"，实际需依赖 JWT 拦截器 + 业务侧校验
- 建议前端限制：仅 `role=1`（教师）用户可见班级管理入口

### 性能优化建议

| 问题 | 现状 | 建议 |
|------|------|------|
| 班级学生列表 N+1 | `getClassStudents` 逐个调 `userService.getById` | 改为 `userService.listByIds(studentIds)` 批量查询 |
| 批量添加全表扫描 | 每次批量都查询全部已有学生 | 考虑使用 SQL `NOT IN` 一次性过滤已存在 |
| 班级下无 `student_count` | 当前未统计班级学生数 | 可在中间表 `insert`/`delete` 后更新 `class.student_count` |

### 边界情况与异常处理

| 场景 | 当前处理 |
|------|----------|
| 添加不存在班级的学生 | 抛 `RuntimeException("班级不存在")` |
| 添加已存在班级的学生 | 抛 `RuntimeException("该学生已在班级中")` |
| 从班级移除不存在的关联 | 返回 `false`，Controller 转为错误响应 |
| 操作用户不存在的学生 | 不做校验（通过关联表间接操作） |
| 操作已被软删除的班级 | 服务层 `getById` 可查到（MyBatis-Plus 逻辑删除自动过滤），不会阻止操作 |
| 班级名重复 | 数据库无唯一约束，需业务层处理 |
