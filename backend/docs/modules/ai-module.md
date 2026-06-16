# AI 智能模块文档

> 模块路径: `com.recruit.controller.AiParseController` + `com.recruit.controller.JobMatchController` + `com.recruit.controller.ResumeScoreController`
> 涉及文件: 3个 Controller / 3个 Service / 4个 Entity / 4个 Mapper

---

## 一、原理文档

### 1. 模块职责
AI 智能模块提供三个核心能力：
- **简历解析**：解析PDF简历，提取结构化信息
- **人岗匹配**：简历与岗位之间的智能匹配评分
- **简历评分**：对简历进行多维度智能评分

### 2. 三个子模块的关系

```
PDF简历
   │
   ▼
AiParseController → AiParseLog (解析日志)
   │                      │
   │                      └── AiFeedbackLog (用户反馈)
   │
   ▼
ResumeScoreController → ResumeScoreLog (评分记录)
                            │
                            ▼
JobMatchController → JobMatchRecord (匹配记录)
```

### 3. 设计要点
- **异步处理**：AI 解析/评分采用异步设计，Controller 提交后返回任务ID，前端轮询结果
- **结果持久化**：AI 处理结果写入数据库日志表，便于追溯和审计
- **用户反馈**：对 AI 解析结果可提交反馈（AiFeedbackLog），用于模型优化训练
- **批量处理**：支持按岗位或按企业批量触发评分

---

## 二、实现文档（函数级）

### 2.1 AiParseController — `/ai-parse-logs/**`

**文件**: `controller/AiParseController.java` (198行)

| 行号 | 方法 | HTTP | 路径 | 参数 | 功能 |
|------|------|------|------|------|------|
| ~28 | `getAllParseLogs()` | GET | `/ai-parse-logs` | — | 获取全部AI解析日志 |
| ~35 | `getParseLogById()` | GET | `/{id}` | @PathVariable id | 按ID查询解析日志 |
| ~45 | `getParseLogsByStatus()` | GET | `/by-status/{status}` | @PathVariable status | 按状态过滤 |
| ~55 | `getParseLogsByStudentId()` | GET | `/by-student/{studentId}` | @PathVariable studentId | 按学生查询 |
| ~65 | `parseResume()` | POST | `/ai-parse-logs` | @RequestBody params | 提交简历解析任务 |
| ~80 | `getParseFeedback()` | GET | `/{id}/feedback` | @PathVariable id | 获取解析反馈 |
| ~95 | `submitFeedback()` | POST | `/{id}/feedback` | @PathVariable id, @RequestBody params | 提交AI解析反馈 |
| ~110 | `deleteParseLog()` | DELETE | `/{id}` | @PathVariable id | 删除解析日志 |

### 2.2 JobMatchController — `/job-matches/**`

**文件**: `controller/JobMatchController.java` (239行)

| 行号 | 方法 | HTTP | 路径 | 参数 | 功能 |
|------|------|------|------|------|------|
| ~28 | `getAllMatches()` | GET | `/job-matches` | — | 获取全部匹配记录 |
| ~35 | `getMatchById()` | GET | `/{id}` | @PathVariable id | 按ID查询匹配详情 |
| ~45 | `getMatchesByJobId()` | GET | `/by-job/{jobId}` | @PathVariable jobId | 某岗位下的匹配列表 |
| ~55 | `getMatchesByStudentId()` | GET | `/by-student/{studentId}` | @PathVariable studentId | 某学生的匹配列表 |
| ~65 | `getMatchesByJobAndStudent()` | GET | `/by-job-and-student` | @RequestParam jobId, studentId | 联合查询 |
| ~80 | `getTopMatchesByJobId()` | GET | `/top-by-job/{jobId}` | @PathVariable jobId, @RequestParam limit | 某岗位Top-N匹配 |
| ~95 | `getTopMatchesByStudentId()` | GET | `/top-by-student/{studentId}` | @PathVariable studentId, @RequestParam limit | 某学生Top-N推荐 |
| ~110 | `generateMatch()` | POST | `/job-matches/generate` | @RequestBody JobMatchRequest | 单条匹配 |
| ~125 | `batchGenerateMatches()` | POST | `/job-matches/batch-generate/{jobId}` | @PathVariable jobId | 批量匹配岗位 |
| ~140 | `pushMatchToStudent()` | PUT | `/{id}/push` | @PathVariable id | 推送匹配结果给学生 |
| ~155 | `deleteMatch()` | DELETE | `/{id}` | @PathVariable id | 删除匹配记录 |

### 2.3 ResumeScoreController — `/resume-scores/**`

**文件**: `controller/ResumeScoreController.java` (207行)

| 行号 | 方法 | HTTP | 路径 | 参数 | 功能 |
|------|------|------|------|------|------|
| ~28 | `getAllScores()` | GET | `/resume-scores` | — | 获取全部评分记录 |
| ~35 | `getScoreById()` | GET | `/{id}` | @PathVariable id | 按ID查询评分详情 |
| ~45 | `getScoresByResumeId()` | GET | `/by-resume/{resumeId}` | @PathVariable resumeId | 按简历查询 |
| ~55 | `getScoresByJobId()` | GET | `/by-job/{jobId}` | @PathVariable jobId | 按岗位查询 |
| ~70 | `getTopScoresByJobId()` | GET | `/top-by-job/{jobId}` | @PathVariable jobId, @RequestParam limit | 岗位评分排名 |
| ~85 | `getScoreDistribution()` | GET | `/distribution/{jobId}` | @PathVariable jobId | 评分分布统计 |
| ~100 | `scoreSingleResume()` | POST | `/resume-scores/score` | @RequestBody params | AI单条评分 |
| ~120 | `batchScoreByJob()` | POST | `/resume-scores/batch-score/{jobId}` | @PathVariable jobId | 批量评分（按岗位）|
| ~140 | `batchScoreByCompany()` | POST | `/resume-scores/batch-score-by-company/{companyId}` | @PathVariable companyId | 批量评分（按企业）|
| ~155 | `deleteScore()` | DELETE | `/{id}` | @PathVariable id | 删除评分记录 |

### 2.4 实体说明

#### AiParseLog — AI 解析日志
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| resumeId | Long | 被解析的简历ID |
| parseContent | Text | 解析结果（JSON字符串）|
| parseTime | LocalDateTime | 解析时间 |
| status | Integer | 状态 (0=待解析, 1=解析中, 2=完成, 3=失败) |
| errorMessage | String | 错误信息 |

#### AiFeedbackLog — AI 反馈日志
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| parseLogId | Long | 关联的解析日志ID |
| feedbackType | String | 反馈类型 (如: PARSE_ERROR, SCORE_INACCURATE) |
| feedbackContent | Text | 反馈内容 |
| rating | Integer | 评分 (1-5) |
| userId | Long | 提交反馈的用户ID |
| createTime | LocalDateTime | 提交时间 |

#### JobMatchRecord — 人岗匹配记录
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| jobId | Long | 岗位ID |
| resumeId | Long | 简历ID |
| matchScore | Double | 匹配分数 (0-100) |
| matchDetail | Text | 匹配详情（JSON，含各维度得分）|
| status | Integer | 状态 (0=未推送, 1=已推送) |
| createTime | LocalDateTime | 匹配时间 |

#### ResumeScoreLog — 简历评分记录
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| resumeId | Long | 简历ID |
| jobId | Long | 岗位ID（可为空，通用评分）|
| totalScore | Double | 总分 |
| dimensionScores | Text | 维度评分（JSON，如{"基础匹配":80,"技能":85}）|
| scoreTime | LocalDateTime | 评分时间 |
| deleted | Integer | 逻辑删除 |

---

> **修改日志**: 首次全量梳理产出
