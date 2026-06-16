# 前端教师模块文档

> 视图: `src/views/pc/teacher/` (4个页面)
> 布局: `src/layouts/TeacherLayout.vue`
> 路由: `/teacher/**`

---

## 一、原理文档

### 1. 模块职责
教师后台是教师管理学生就业的核心工具，包括班级管理、岗位发布和投递跟踪。

### 2. 权限说明
- 路由 meta 标记 `role: 'teacher'`
- 路由守卫检查 `userRole === 'teacher'`

### 3. 页面关系

```
教师后台
 ├── Dashboard.vue          ← 工作台（班级概览 + 投递动态）
 ├── ClassManagement.vue    ← 班级管理（查看学生 + 分配岗位）
 ├── JobPosting.vue         ← 岗位发布（新增/编辑/关闭岗位）
 └── DeliveryBoard.vue      ← 投递看板（学生投递动态）
```

---

## 二、实现文档（函数级）

### 2.1 Dashboard.vue — 工作台

**文件**: `src/views/pc/teacher/Dashboard.vue`

| 区域 | 内容 | 数据来源 |
|------|------|----------|
| 统计卡片 | 班级数、学生数、岗位数、投递数 | `teacherAPI.getTeacherStats()` |
| 待处理 | 待审核的岗位变更申请 | `teacherAPI.getPendingJobChanges()` |
| 投递动态 | 最近投递记录 | `deliveryAPI.getDeliveries({ recent: true })` |
| 班级列表 | 教师负责的班级概览 | `teacherAPI.getTeacherClasses()` |

### 2.2 ClassManagement.vue — 班级管理

**文件**: `src/views/pc/teacher/ClassManagement.vue`

| 功能 | 调用 API | 说明 |
|------|----------|------|
| 班级列表 | `teacherAPI.getTeacherClasses()` | 教师负责的班级 |
| 学生列表 | `userAPI.getUsersByRole(1)` 按班级过滤 | 班级下的学生 |
| 查看简历 | `resumeAPI.getStudentResume(studentId)` | 查看学生简历详情 |
| 推荐岗位 | `jobMatchAPI.generateMatch()` | 为学生匹配岗位 |

### 2.3 JobPosting.vue — 岗位发布

**文件**: `src/views/pc/teacher/JobPosting.vue`

| 功能 | 调用 API | 说明 |
|------|----------|------|
| 岗位列表 | `jobAPI.getJobs(params)` | 教师发布的岗位 |
| 新增岗位 | `jobAPI.createJob(data)` | 含企业选择 |
| 编辑岗位 | `jobAPI.updateJob(id, data)` | — |
| 关闭岗位 | `jobAPI.updateJobStatus(id, 0)` | 0=关闭 |
| 开启岗位 | `jobAPI.updateJobStatus(id, 1)` | 1=开放 |

JobPosting 关键数据流：
```javascript
// 发布新岗位
async function submitJob(form) {
  // form: { companyId, title, description, requirement, salaryMin, salaryMax, location, type, headCount }
  const res = await jobAPI.createJob(form)
  // 成功 → 刷新列表
}
```

### 2.4 DeliveryBoard.vue — 投递看板

**文件**: `src/views/pc/teacher/DeliveryBoard.vue`

| 功能 | 调用 API | 说明 |
|------|----------|------|
| 投递列表 | `deliveryAPI.getDeliveries(params)` | 按岗位/学生/状态过滤 |
| 状态统计 | `deliveryAPI.getDeliveryStatisticsByJobId(jobId)` | 各状态数量（看板卡片）|
| 按状态过滤 | 点击状态卡片 | 带 status 参数重新查询 |
| 查看详情 | 弹窗展示投递详情 | 含学生信息和岗位信息 |

---

## 三、API 引用清单

教师模块的 API 调用横跨多个模块：

| API 模块 | 使用的页面 |
|----------|-----------|
| `teacherAPI` | Dashboard, ClassManagement |
| `jobAPI` | JobPosting |
| `deliveryAPI` | Dashboard, DeliveryBoard |
| `resumeAPI` | ClassManagement |
| `jobMatchAPI` | ClassManagement |
| `userAPI` | ClassManagement |

---

> **修改日志**: 首次全量梳理产出
