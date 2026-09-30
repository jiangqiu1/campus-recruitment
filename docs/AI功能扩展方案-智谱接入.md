# AI 功能扩展方案：智谱 GLM 接入 + 简历诊断 + 模拟面试

> 更新时间：2026-09-30
> 目标：接入智谱 GLM 作为第二模型，落地两个学生向 AI 功能，同时为毕设"多模型对比实验"章节积累数据。

---

## 一、现状盘点（代码定位）

| 已有能力 | 位置 | 状态 |
|---|---|---|
| AI 调用统一入口 `callAI()` | `backend/src/main/java/com/recruit/utils/AiService.java`（`callAI` 方法，约 171 行） | 单一 DeepSeek 通道，OpenAI 兼容格式 |
| 简历评分 scoreResume | 同上，51 行 | 已有，PC 教师端在用 |
| 人岗匹配 matchJob | 同上，72 行 | 已有 |
| **简历改进建议 analyzeResume** | 同上，151 行；接口 `AiParseController.java:199`（`POST /ai-parse/analyze-resume`） | **后端就绪，前端零调用** |
| 模拟面试问答 | 不存在 | 全新开发 |

关键约束：`/ai-parse` 控制器是教师向的（类注释"只有教师可以访问"），学生端不能直接复用，需新建学生向控制器。

## 二、智谱开放平台操作步骤（你去做的部分）

1. 登录 [open.bigmodel.cn](https://open.bigmodel.cn)（智谱开放平台），进入控制台。
2. 「API 密钥」→ 新建 API Key。格式形如 `xxxxxxxx.yyyyyyyy`（和 DeepSeek 的 `sk-` 开头不同，注意别混）。
3. 记下两个信息：
   - **端点**（OpenAI 兼容）：`https://open.bigmodel.cn/api/paas/v4/chat/completions`
   - **免费模型**：`glm-4.5-flash`（开发验证用它，免费且够用）；需要更强效果可换 `glm-4.6`（付费，走你领的 token 额度）
4. 用 curl 先验证 key 可用（10 秒的事）：
   ```bash
   curl -X POST https://open.bigmodel.cn/api/paas/v4/chat/completions \
     -H "Authorization: Bearer 你的key" \
     -H "Content-Type: application/json" \
     -d '{"model":"glm-4.5-flash","messages":[{"role":"user","content":"回复ok"}]}'
   ```
   返回里有 `choices[0].message.content` 就说明通了。**格式和 DeepSeek 完全一致，现有 callAI() 的解析逻辑零改动。**

## 三、后端改造

### 3.1 双模型接入（先做，约 1~2 小时）

**application.yml** 改成多 provider 结构：

```yaml
ai:
  default-provider: glm          # 开发期默认走 GLM（免费 token）
  providers:
    deepseek:
      key: sk-xxx
      url: https://api.deepseek.com/v1/chat/completions
      model: deepseek-chat
    glm:
      key: 你的智谱key
      url: https://open.bigmodel.cn/api/paas/v4/chat/completions
      model: glm-4.5-flash
```

**AiService.java** 改造点：
- 新增内部配置类（或用 `@ConfigurationProperties(prefix = "ai")` 的 `Map<String, Provider>` 结构）替换现有 3 个 `@Value` 字段；
- `callAI(String prompt, String taskName)` 改为 `callAI(String prompt, String taskName, String provider)`，按 provider 取 key/url/model，其余逻辑（headers、解析、fallbackMock）不动；
- 旧签名保留重载，默认取 `ai.default-provider`，已有调用方（scoreResume/matchJob/parseResume/parseJobDescription/analyzeResume）全部不用改；
- **每条日志记录 provider 与耗时**：把 provider 名和调用毫秒数写进 AiParseLog（新增两个字段）或 log 里——这是毕设对比实验的数据来源，现在不记后面没得补。

### 3.2 新建学生向控制器 AiAssistantController（新文件）

`backend/src/main/java/com/recruit/controller/AiAssistantController.java`

```
@RestController
@RequestMapping("/ai-assistant")
```

| 接口 | 方法 | 说明 |
|---|---|---|
| `POST /ai-assistant/resume-review` | resumeReview() | 取当前登录学生自己的简历（复用 ResumeController.getMyResume 的取数逻辑），组装 JSON 后调 `aiService.analyzeResume()`，返回诊断结果。**必须校验学生角色 + 只能查自己的简历**（参考 P0-2 的修复方式） |
| `POST /ai-assistant/interview/questions` | genQuestions() | 入参 jobId；校验学生身份后，取岗位 JD + 学生简历，调新增的 `generateInterviewQuestions()` |
| `POST /ai-assistant/interview/evaluate` | evaluateAnswer() | 入参 jobId、question、answer；调新增的 `evaluateAnswer()`，返回点评 |

### 3.3 AiService 新增两个方法（模拟面试）

```java
// 生成面试题：返回 {questions: [{id, type: "技术|项目|行为", question}]}
public Map<String, Object> generateInterviewQuestions(String jobTitle, String jobDescription,
        String jobRequirements, String resumeText, int count)

// 点评作答：返回 {score: 0-100, comment: "点评", betterAnswer: "参考答案"}
public Map<String, Object> evaluateAnswer(String jobTitle, String question, String answer, String resumeText)
```

Prompt 写法参考现有 scoreResume（约束 JSON 输出 + `只返回 JSON，不要包含其他文字`），任务名分别用 `genQuestions` / `evalAnswer`，并在 `fallbackMock()` 补对应 case。面试题生成 max_tokens 建议放宽到 2048。

## 四、前端改造

### 4.1 学生端小程序（简历 AI 诊断）

文件：`job-miniprogram/src/pages/student/resume.vue`
- 页头加「AI 诊断」按钮 → 调 `POST /ai-assistant/resume-review` → 弹层或跳新页展示：综合评分（大数字）、优势、不足、改进建议、推荐补充技能，样式对齐现有 ai-matches.vue 的评分卡片风格；
- `api` 封装层新增对应方法（找到现有 request 封装文件照葫芦画瓢）。

### 4.2 学生端小程序（模拟面试练习，新页面）

新文件：`job-miniprogram/src/pages/student/interview-practice.vue`，并在 `src/pages.json` 注册。
- 流程：选择岗位（从「我的投递」列表取）→ AI 生成 5 道题 → 逐题 textarea 作答 → 每题「提交点评」→ 展示得分 + 点评 + 参考答案；
- 交互从简：单页顺序答题，不做流式打字机效果（后端是同步接口，做流式要上 SSE，性价比低，毕设不需要）。

### 4.3 PC 教师端（可选，顺手）

`frontend/src/views/pc/teacher/ResumeManage.vue` 简历详情里加同一个「AI 诊断」按钮，教师辅导学生时可用。走 `POST /ai-parse/analyze-resume` 现有接口即可，后端不用动。

## 五、实施顺序（建议半天到两天）

| 步骤 | 内容 | 耗时 |
|---|---|---|
| 1 | 智谱拿 key + curl 验证 | 10 分钟 |
| 2 | 双模型配置改造 + 日志记录 provider/耗时 | 1~2 小时 |
| 3 | AiAssistantController + resume-review 接口 | 1 小时 |
| 4 | 小程序简历页 AI 诊断入口 | 半天 |
| 5 | 模拟面试两个 AI 方法 + 两个接口 | 半天 |
| 6 | 小程序 interview-practice 页面 | 半天~1 天 |

每步做完跑一次 `mvn.cmd compile` + 小程序真机预览，按老规矩 bug-by-bug 修。

## 六、注意事项

1. **API Key 泄露风险**：`application.yml` 里 DeepSeek 的 key 是明文且已随仓库推到 GitHub。建议尽快去 DeepSeek 后台作废该 key 换新的，并考虑把仓库转私有或用环境变量注入。智谱新 key 不要再明文提交。
2. 免费模型 glm-4.5-flash 有并发/速率限制，个人项目开发足够；批量跑对比实验时串行调用即可。
3. 答辩演示建议默认 provider 配 GLM：免费、快、不怕欠费；DeepSeek 留作对比通道。
