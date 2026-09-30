# AI 模块 · Prompt 与字段规范（深度补充）

> 本文档记录 `AiService`（`utils/AiService.java`）五大 AI 场景的完整 Prompt 原文、输入输出 JSON 字段结构、统一调用参数与降级数据。可直接照此范式扩展新的 AI 场景。
> **2026-09-30 起**：AI 配置已升级为多 provider 结构（DeepSeek + 智谱 GLM），新增 genQuestions / evalAnswer 两个场景，详见同目录 `双模型与学生AI助手-20260930.md`；本文档场景四、五之后的行号为旧版参考。

## 1. 配置项（已升级为多 provider，见 AiProperties）

| 配置 | 说明 |
|------|------|
| `ai.default-provider` | 默认通道名（deepseek / glm） |
| `ai.providers.<name>.key` | 各通道 API Key，空或 `sk-placeholder` 时该通道走 mock |
| `ai.providers.<name>.url` | 各通道接口地址（OpenAI 兼容） |
| `ai.providers.<name>.model` | 各通道模型名 |

> 旧键 `ai.api.key/url/model` 已废弃。

## 2. 统一调用参数（callAI）

| 参数 | 值 |
|------|-----|
| temperature | 0.3（保证一致性） |
| max_tokens | 默认 1024（genQuestions 用 2048） |
| messages | `[{role:"user", content:prompt}]` |
| HTTP | `RestTemplate.postForEntity` + Bearer 鉴权 |

**截断规则**（truncate）：评分/匹配截 2000 字符，解析/分析截 3000 字符。

**调用日志**：每次调用写一条 ai_parse_log（provider/task_name/latency_ms/user_id/mock_flag），详见 `双模型与学生AI助手-20260930.md`。

## 3. 场景一：简历评分 `scoreResume`（38 行）

**输入**：`jobDescription`（岗位描述）、`jobRequirements`（任职要求）、`resumeText`（简历文本）。

**Prompt 原文**：

```
你是一个专业的招聘专家。请评估以下简历与招聘岗位的匹配度。

## 岗位描述
{jobDescription}

## 任职要求
{jobRequirements}

## 简历内容
{truncate(resumeText, 2000)}

请严格按以下 JSON 格式返回评分结果（所有分数为0-100的整数）：
{
  "totalScore": 整体匹配度,
  "skillScore": 技能匹配度（考察技术栈匹配程度）,
  "expScore": 经验匹配度（考察工作/项目经验）,
  "eduScore": 学历匹配度（考察学历和专业）,
  "comment": "20字以内的综合评价"
}
只返回 JSON，不要包含其他文字。
```

**输出字段**：`totalScore` / `skillScore` / `expScore` / `eduScore` / `comment`（0-100 整数）。

## 4. 场景二：人岗匹配 `matchJob`（59 行）

**输入**：`jobTitle`、`jobDescription`、`jobRequirements`、`studentName`、`resumeText`。

**Prompt 原文**：

```
你是一个招聘匹配专家。请从多个维度评估以下学生与岗位的匹配程度。

## 岗位名称
{jobTitle}

## 岗位描述
{jobDescription}

## 任职要求
{jobRequirements}

## 学生简历
{truncate(resumeText, 2000)}

请严格按以下 JSON 格式返回（所有分数为0-100的整数）：
{
  "matchScore": 整体匹配度,
  "skillMatch": 技能匹配度（考察技术栈与岗位技能要求的重合程度，如Java,Spring等）,
  "eduMatch": 学历匹配度（考察学历层次与岗位要求的匹配，如本科/硕士/博士）,
  "expMatch": 经验匹配度（考察实习/项目经验与岗位方向的关联程度）,
  "majorFit": 专业契合度（考察所学专业与岗位类别的契合程度）,
  "matchReason": "20字以内的匹配理由"
}
只返回 JSON，不要包含其他文字。
```

**输出字段**：`matchScore` / `skillMatch` / `eduMatch` / `expMatch` / `majorFit` / `matchReason`（4 个子维度 + 总分 + 理由）。

> 落库时 `matchScore`（0-100）除以 100 转成 0-1 的 `BigDecimal`，子维度序列化进 `scoreDetail` JSON（见 `JobMatchRecordServiceImpl.generateMatchRecord` 82 行）。

## 5. 场景三：简历解析 `parseResume`（83 行）

**输入**：`rawText`（原始简历文本，截 3000）。

**Prompt 原文**：

```
你是一个简历解析专家。请从以下简历文本中提取结构化信息。

## 简历文本
{truncate(rawText, 3000)}

请严格按以下 JSON 格式返回：
{
  "name": "姓名",
  "phone": "手机号",
  "email": "邮箱",
  "school": "毕业院校",
  "major": "专业",
  "education": "学历",
  "educationStart": "入学年份（如2023.09）",
  "educationEnd": "毕业年份（如2026.06）",
  "skills": ["技能1", "技能2"],
  "internshipCompany": "实习公司名称",
  "internshipPosition": "实习职位",
  "internshipDuration": "实习时间（如2025.07-2025.12）",
  "internshipDesc": "实习工作描述",
  "projects": [{"name": "项目名称", "role": "担任角色", "duration": "项目时间（如2025.03-2025.06）", "description": "项目描述：使用技术、取得的成果等"}],
  "selfEvaluation": "自我评价"
}
字段缺失时用空字符串或空数组代替。只返回 JSON，不要包含其他文字。
```

**输出字段**：共 16 个字段（含嵌套 `projects[]` 对象数组）。

## 6. 场景四：岗位解析 `parseJobDescription`（113 行）

**输入**：`rawText`（岗位描述文本，截 3000）。

**Prompt 原文**：

```
你是一个岗位描述解析专家。请从以下岗位描述文本中提取结构化信息。

## 岗位描述文本
{truncate(rawText, 3000)}

请严格按以下 JSON 格式返回：
{
  "title": "岗位名称",
  "companyName": "公司/企业名称（从文本中提取的公司名，若无则填空字符串）",
  "salaryRange": "薪资范围，如 8K-15K",
  "education": "学历要求，如大专及以上、本科及以上",
  "location": "工作地点",
  "deadline": "截止日期，如2026-07-16（标准日期格式）",
  "experience": "经验要求，如1-3年、3-5年、经验不限",
  "description": "岗位职责描述（保留原文核心内容）",
  "requirements": "任职要求（保留原文核心内容）",
  "skills": ["技能1", "技能2"]
}
字段缺失时用空字符串或空数组代替。只返回 JSON，不要包含其他文字。
```

**输出字段**：共 10 个字段。用途：教师粘贴 JD → AI 提取 → 自动填岗位表单。

## 7. 场景五：简历分析 `analyzeResume`（138 行）

**输入**：`resumeJson`（简历结构化数据，截 3000）。

**Prompt 原文**：

```
你是一个简历优化专家。请分析以下简历数据，找出不足并给出改进建议。

## 简历数据
{truncate(resumeJson, 3000)}

请严格按以下 JSON 格式返回：
{
  "overallScore": "综合评分（0-100的整数）",
  "strengths": ["优势1", "优势2"],
  "weaknesses": ["不足1", "不足2"],
  "suggestions": ["改进建议1", "改进建议2", "改进建议3"],
  "missingFields": ["缺失字段1", "缺失字段2"],
  "recommendedSkills": ["推荐补充的技能1", "推荐补充的技能2"]
}
字段缺失时用空数组代替。只返回 JSON，不要包含其他文字。
```

**输出字段**：`overallScore` + 5 个数组字段。用途：结果存回 `resume.ai_analysis`。

## 8. 降级 mock 数据（fallbackMock，225 行）

未配 Key 或调用失败时返回：

| 任务 | mock 值 |
|------|--------|
| scoreResume | totalScore 78 / skillScore 82 / expScore 72 / eduScore 80 |
| matchJob | matchScore 82 / skillMatch 85 / eduMatch 80 / expMatch 72 / majorFit 78 |
| parseResume | 全空字段模板 |
| parseJob | 前端开发工程师样例（8K-15K、广州、Vue.js 等） |
| analyzeResume | overallScore 72 + 2 优势 + 2 不足 + 3 建议 + 推荐技能 |

## 9. 关键实现细节

**callAI 流程（158 行）**：
1. 判断 apiKey 是否为空 / `sk-placeholder` → 是则直接 `fallbackMock`。
2. 组装请求头（JSON + Bearer）、请求体（model/temperature/max_tokens/messages）。
3. `postForEntity` 调用，取 `body.choices[0].message.content`。
4. 交 `parseJsonResponse` 解析。

**parseJsonResponse 容错（201 行）**：
1. `replaceAll("```(?:json)?", "")` 去除代码围栏。
2. `indexOf('{')` 到 `lastIndexOf('}')` 截取 JSON 段。
3. `ObjectMapper.readValue` 反序列化。
4. 失败 → `fallbackMock`。

**已知问题（后续优化点）**：
- RestTemplate 无超时配置（慢响应占线程）。
- `parseJsonResponse` 每次 `new ObjectMapper`（应复用单例）。
- 调用失败与"未配 Key"混淆：两者都返回 mock，真实系统应区分"未配置 → 降级"与"调用失败 → 报错"。
