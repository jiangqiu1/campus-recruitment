# 前端 HR 模块文档

> 视图: `src/views/pc/hr/` (4个页面)
> 布局: `src/layouts/HRLayout.vue`
> 路由: `/hr/**`

---

## 一、原理文档

### 1. 模块职责
HR 后台是企业管理招聘流程的核心工具，包括简历处理、数据统计、岗位分析和账号管理。

### 2. 权限说明
- 路由 meta 标记 `role: 'hr'`
- 路由守卫检查 `userRole === 'hr'`

### 3. 页面关系

```
HR后台
 ├── BatchResume.vue        ← 批量简历处理（查看/评分/筛选）
 ├── DataStats.vue          ← 数据统计（ECharts 招聘分析）
 ├── JobAnalysis.vue        ← 岗位分析（岗位需求 + 投递分析）
 └── AccountManagement.vue  ← 账号管理（企业信息 + 密码修改）
```

---

## 二、实现文档（函数级）

### 2.1 BatchResume.vue — 批量简历处理

**文件**: `src/views/pc/hr/BatchResume.vue`

| 功能 | 调用 API | 说明 |
|------|----------|------|
| 简历列表 | `resumeAPI.getResumes(params)` | 按岗位/企业过滤 |
| 简历评分 | `resumeScoreAPI.batchScoreResumes(jobId)` | AI批量评分 |
| 按企业评分 | `resumeScoreAPI.batchScoreByCompany(companyId)` | 企业下所有岗位的简历 |
| 查看评分 | `resumeScoreAPI.getScoreDetail(id)` | AI评分详情弹窗 |
| 筛选 | 状态/岗位/评分范围 | 多条件组合查询 |

关键交互：
```javascript
// 批量评分
async function batchScore(jobId) {
  const res = await resumeScoreAPI.batchScoreResumes(jobId)
  // 成功 → 刷新列表，显示评分结果
}

// 按评分排序
const sortedResumes = computed(() => {
  return [...resumeList.value].sort((a, b) => b.score - a.score)
})
```

### 2.2 DataStats.vue — 数据统计

**文件**: `src/views/pc/hr/DataStats.vue`

使用 ECharts 展示 HR 视角的招聘数据：

| 图表类型 | 数据内容 | 数据来源 |
|----------|----------|----------|
| 柱状图 | 各岗位投递数量 | `deliveryAPI.getDeliveries()` 统计 |
| 折线图 | 投递时间趋势 | 按日期分组统计 |
| 饼图 | 投递状态占比 | 各 status 计数 |
| 环形图 | 简历评分分布 | `resumeScoreAPI.getScores(params)` |
| 雷达图 | 学生技能分布 | 岗位要求 vs 学生技能 |

### 2.3 JobAnalysis.vue — 岗位分析

**文件**: `src/views/pc/hr/JobAnalysis.vue`

| 功能 | 调用 API | 说明 |
|------|----------|------|
| 岗位数据 | `jobAPI.getJobAnalysis()` | 企业下岗位列表+统计 |
| 投递统计 | `deliveryAPI.getDeliveriesByJob(jobId)` | 各岗位投递详情 |
| 分析指标 | 岗位热度、简历匹配度、投递转化率 | 前端计算 |

### 2.4 AccountManagement.vue — 账号管理

**文件**: `src/views/pc/hr/AccountManagement.vue`

| 功能 | 调用 API | 说明 |
|------|----------|------|
| 企业信息 | `companyAPI.getCompanyProfile(companyId)` | 查看/编辑本企业信息 |
| 修改密码 | `authAPI.changePassword(data)` | 密码修改表单 |
| 个人信息 | `userAPI.getUserInfo()` | 当前HR用户信息 |

---

## 三、API 引用清单

| API 模块 | 使用的页面 |
|----------|-----------|
| `resumeAPI` | BatchResume |
| `resumeScoreAPI` | BatchResume, DataStats |
| `deliveryAPI` | DataStats, JobAnalysis |
| `jobAPI` | JobAnalysis |
| `companyAPI` | AccountManagement |
| `authAPI` | AccountManagement |
| `userAPI` | AccountManagement |

---

> **修改日志**: 首次全量梳理产出
