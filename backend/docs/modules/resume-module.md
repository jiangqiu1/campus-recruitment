# 简历管理模块文档

> 模块路径: `com.recruit.controller.ResumeController` + `com.recruit.service.ResumeService`
> 涉及文件: 1个 Controller / 1个 Service 接口+实现 / 1个 Mapper 接口+XML / 1个 Entity

---

## 一、原理文档

### 1. 模块职责
学生的简历管理，包括创建/编辑简历、上传PDF简历、AI解析简历、设置默认简历。

### 2. 业务逻辑
- **学生只能管理自己的简历**：学生端Token解析出studentId，所有操作都绑定studentId
- **单简历模式**：每个学生默认一张简历，支持在线编辑和PDF上传两种方式
- **AI解析流程**：上传PDF → 解析 → 提取技能标签和求职意向 → 更新到简历实体
- **软删除**：`deleted` 字段标记删除

### 3. 数据流
```
学生
 ├─ POST /resumes + studentId + ResumeBody → 创建/更新简历
 ├─ POST /resumes/upload-pdf + file → PDF上传 → 存储路径
 ├─ POST /resumes/{id}/parse → AI解析 → 技能标签提取
 └─ PUT /resumes/{id}/set-default → 设置默认简历

教师
 └─ GET /resumes/student/{studentId} → 查看学生简历
```

---

## 二、实现文档（函数级）

### 2.1 ResumeController — `/resumes/**`

**文件**: `controller/ResumeController.java` (175行)

| 行号 | 方法 | HTTP | 路径 | 参数 | 功能 |
|------|------|------|------|------|------|
| 32-43 | `getMyResume()` | GET | `/resumes/my` | @RequestParam Long studentId | 获取当前学生自己的简历 |
| 50-62 | `getMyDefaultResume()` | GET | `/resumes/my/default` | @RequestParam Long studentId | 获取默认简历 |
| 69-84 | `createOrUpdateResume()` | POST | `/resumes` | @RequestParam Long studentId, @RequestBody Resume | 创建(id=null)或更新简历 |
| 91-103 | `uploadPdf()` | POST | `/resumes/upload-pdf` | @RequestParam Long studentId, @RequestParam MultipartFile | 上传PDF简历文件 |
| 109-121 | `setDefaultResume()` | PUT | `/{resumeId}/set-default` | @PathVariable resumeId | 设置默认简历 |
| 127-145 | `parseResume()` | POST | `/{resumeId}/parse` | @PathVariable resumeId | AI解析简历PDF |
| 151-163 | `deleteResume()` | DELETE | `/{resumeId}` | @PathVariable resumeId | 软删除简历 |
| 169-175 | `getStudentResume()` | GET | `/resumes/student/{studentId}` | @PathVariable Long studentId | 教师查看学生简历 |

### 2.2 ResumeService

**文件**: `service/ResumeService.java` + `service/impl/ResumeServiceImpl.java`

| 方法 | 功能 | 备注 |
|------|------|------|
| `selectByStudentId(studentId)` | 根据学生ID查询简历 | Mapper 自定义查询 |
| `selectDefaultByStudentId(studentId)` | 查询默认简历 | 根据 is_default 字段 |
| `setDefaultResume(studentId, resumeId)` | 设置默认简历 | 事务操作（先清空原默认标记）|
| `updateSkillTags(resumeId)` | 更新技能标签 | AI解析后的回调 |
| `parseResumePdf(pdfUrl)` | AI解析PDF内容 | 返回解析后的JSON字符串 |

### 2.3 Resume Entity

**文件**: `entity/Resume.java`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| studentId | Long | 学生ID（关联 sys_user） |
| content | String | 简历文本内容 |
| fileUrl | String | 简历PDF文件URL |
| pdfUrl | String | PDF路径（upload-pdf接口写入） |
| skillTags | String | 技能标签（逗号分隔） |
| jobIntention | String | 求职意向 |
| isDefault | Integer | 是否默认简历 (0=否, 1=是) |
| status | Integer | 状态 (0=编辑中, 1=已完成) |
| deleted | Integer | 逻辑删除 (0=正常, 1=已删) |

### 2.4 ResumeMapper

**文件**: `mapper/ResumeMapper.java` + `resources/mapper/ResumeMapper.xml`

| 方法 | 功能 |
|------|------|
| `selectByStudentId(studentId)` | 根据学生ID查询简历（XML自定义SQL） |

---

> **修改日志**: 首次全量梳理产出
