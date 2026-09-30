# AI 驱动的出海独立站 SEO 内容自动化生成与优化系统

## 详细设计方案

> 软件技术 · 工坊班实训课题 38 · 导师：陈婷
> 技术底座：校企合作项目「校园招聘就业平台」的工程经验复用
> 文档版本：v2.0（详细设计）

---

## 一、选题背景与意义

### 1.1 问题背景

独立站（DTC 品牌官网，如 Shopify 建站）已成为中国企业品牌出海的重要渠道。与入驻亚马逊、速卖通等平台不同，独立站没有平台的免费流量，**必须依靠搜索引擎自然流量（SEO）获取海外用户**。

传统 SEO 内容生产面临三个现实困境：

1. **产出效率低**：多语言博客、产品描述、视频 Meta 信息依赖人工撰写，一个英语文案月薪数万，且产能有限。
2. **多语种适配成本高**：北美、欧洲、东南亚市场需要英文、西班牙语、德语等不同语言内容，人工翻译 + 本地化周期以周计。
3. **缺乏数据闭环**：内容发出去之后"石沉大海"，不知道排名涨没涨、该不该优化、优化哪里。

### 1.2 技术契机

大语言模型（LLM）已具备生成高质量、符合搜索引擎偏好多语言内容的能力。将 LLM 与 SEO 数据分析结合，可实现"分析关键词 → 生成多语种内容 → 评估质量 → 追踪排名 → 再优化"的**全流程自动化**。

同时，生成式 AI 的普及正在催生 GEO（生成引擎优化）等新趋势，SEO 的边界从传统搜索引擎扩展到 AI 搜索，这为本课题提供了更前沿的研究视角。

### 1.3 选题意义

- **工程价值**：为出海中小企业提供降本增效的内容生产工具，具有真实商业场景。
- **技术价值**：涉及 LLM 集成、Prompt 工程、数据采集、文本质量评估、定时调度等多项可落地的软件工程技术。
- **个人价值**：延续作者校企项目积累的全栈能力，形成"招聘系统 → 出海营销系统"的能力迁移，是校招作品集的自然延伸。

---

## 二、课题目标与范围

### 2.1 系统目标

构建一套面向出海独立站的 SEO 内容自动化生产与优化系统，实现四大核心能力：

- **关键词分析**：挖掘目标市场搜索词，评估搜索量与竞争度，推荐高价值关键词。
- **多语种内容生成**：基于 LLM，将一套素材自动生成多语言博客、产品描述、Meta 信息。
- **质量评估**：对生成内容进行可读性、SEO 合规性、多语种质量的多维打分。
- **排名追踪**：定时记录关键词排名变化，形成趋势看板，驱动闭环优化。

### 2.2 功能边界

| 维度 | 范围 |
|------|------|
| 必做 | 关键词分析、内容生成、质量评估、排名追踪、数据看板、批量任务、模板管理 |
| 暂不做 | 真实支付、多租户 SaaS、内容自动发布到独立站（预留接口） |
| 弱化 | 保利威视频云对接（无资源时以说明性接口代替） |

### 2.3 验收标准

| 能力 | 验收指标 |
|------|---------|
| 关键词分析 | 输入主题可返回 ≥20 个候选词，含搜索量等级、竞争度、推荐度 |
| 内容生成 | 支持 ≥6 种语言、≥3 类内容（博客/产品描述/Meta），单次生成 ≤60s |
| 质量评估 | 输出 ≥4 维评分 + 改进建议，达标判定可配置阈值 |
| 排名追踪 | 定时任务可运行，排名数据形成时间序列并可可视化 |
| 系统演示 | 无外部数据源时仍可完整演示（mock 兜底） |

### 2.4 与校企项目的关系

本课题为**独立新系统**，业务上与校企招聘项目无关，但**复用其成熟技术栈与工程规范**：

| 复用的能力 | 来源 |
|-----------|------|
| Spring Boot + MyBatis-Plus + MySQL + Redis 分层架构 | 校企项目后端 |
| Vue 3 + Vite + Element Plus + ECharts 前端骨架 | 校企项目 PC 端 |
| AI 调用管道（结构化 JSON Prompt + 降级 mock + 容错解析） | 校企项目 `AiService` |
| JWT + Redis 黑名单认证、AES 敏感字段加密 | 校企项目安全模块 |

校企项目作为"技术积累 / 相关工作"写入论文，本系统作为毕设主体。

---

## 三、系统总体设计

### 3.1 整体架构

采用前后端分离的分层架构：

```
┌─────────────────────────────────────────────┐
│              前端（Vue 3 + Element Plus）      │
│   关键词管理 · 内容生成 · 质量评估 · 排名看板    │
└──────────────────┬──────────────────────────┘
                   │ HTTP / JSON（JWT 认证）
┌──────────────────▼──────────────────────────┐
│            Spring Boot 应用层                 │
│  KeywordController · ContentController ·      │
│  QualityController · RankController ·         │
│  DashboardController · TemplateController ·   │
│  TaskController                               │
├───────────────────────────────────────────────┤
│            Service 业务层                     │
│  KeywordService · ContentService ·            │
│  QualityService · RankService ·               │
│  TaskService · DashboardService               │
├───────────────────────────────────────────────┤
│            AI 服务层（拆分重构）               │
│  AiClient · PromptTemplateService ·           │
│  AiResponseParser                             │
├───────────────────────────────────────────────┤
│           数据源适配层（可插拔）               │
│  DataSourceAdapter 接口                       │
│  ├─ GoogleSuggestSource（免费）               │
│  ├─ SerpCrawlSource（爬虫）                   │
│  ├─ ManualSource（手动导入）                  │
│  └─ MockDataSource（兜底）                    │
├───────────────────────────────────────────────┤
│       MySQL（业务数据） + Redis（缓存/限流）    │
└───────────────────────────────────────────────┘
```

### 3.2 技术栈选型

| 层 | 技术 | 版本 | 复用/新增 |
|----|------|------|----------|
| 后端框架 | Spring Boot | 2.7 | 复用 |
| ORM | MyBatis-Plus | 3.5.5 | 复用 |
| 数据库 | MySQL / Redis | 8.0 / 6+ | 复用 |
| 认证 | JWT + Redis 黑名单 | — | 复用 |
| AI 接入 | DeepSeek（可替换为其他 LLM） | — | 复用模式 |
| 定时调度 | Spring @Scheduled（MVP）/ XXL-Job（进阶） | — | 新增 |
| 网页抓取 | Jsoup | 1.17 | 新增 |
| Excel 导入 | Apache POI / EasyExcel | — | 新增 |
| 前端 | Vue 3 + Vite + Element Plus + Pinia + ECharts | — | 复用 |
| 构建 | Maven / npm | — | 复用 |

### 3.3 核心类设计

| 类 | 职责 |
|----|------|
| `DataSourceAdapter`（接口） | 统一数据源抽象：`List<Keyword> suggest(String seed)`、`List<RankResult> checkRank(List<String> keywords)` |
| `GoogleSuggestSource` | 抓 Google 自动补全建议 |
| `SerpCrawlSource` | 抓 SERP 结果页解析排名 |
| `ManualSource` | 从 CSV/Excel 导入 |
| `MockDataSource` | 内置模拟数据，兜底 |
| `AiClient` | 封装 LLM HTTP 调用：超时、重试、限流、成本统计 |
| `PromptTemplateService` | 管理 Prompt 模板：查库、渲染占位符、版本切换 |
| `AiResponseParser` | 解析 LLM 返回：剥 ```json 标记、截取、校验字段 |
| `KeywordAnalyzer` | 竞争度、推荐度计算 |
| `QualityEvaluator` | 可读性、SEO 合规、结构、多语种打分 |
| `RankScheduler` | 定时任务调度排名检查 |

---

## 四、核心模块详细设计

### 4.1 关键词分析与竞争度评估

**输入**：行业/产品主题（如 "led strip lights"）、目标市场、语言。

**流程**：

1. 调用 `DataSourceAdapter.suggest(seed)` 获取候选词列表。
2. 去重、过滤（去掉过短/含品牌词的），得到候选集合。
3. 对每个候选词计算指标（见下）。
4. 按推荐度排序，输出 Top N 推荐清单。

**核心算法**：

**（1）竞争度**（0-1，越大越难做）：

```
competition = 0.4 × 广告位占比（SERP 首页广告条数/10）
            + 0.3 × 权威站占比（首页结果中 Alexa 前 1 万域名占比）
            + 0.3 × 精确匹配度（首页标题精确含关键词的比例）
```

**（2）搜索量等级**：数据源若给精确值则直接用；否则映射为等级 1（低）/2（中）/3（高），兜底用等级。

**（3）推荐度**（0-100，越大越值得做，长尾词优先）：

```
recommend = 60 × normalize(searchVolume) + 40 × (1 - competition)
```

其中 `normalize` 将搜索量映射到 0-1（对数归一化）。

**（4）关键判断**：`recommend >= 60` 判定为"高价值关键词"，推荐给内容生成模块。

### 4.2 多语种内容生成

**输入**：关键词、目标语言、内容类型（blog / product / meta）。

**流程**：

1. 从 `prompt_template` 表按 `scene + language` 取模板。
2. 渲染占位符（关键词、市场、品牌信息）。
3. 调用 `AiClient.chat(prompt)` 获取结果。
4. `AiResponseParser` 解析为结构化 `Content` 对象。
5. 落库 `seo_content`，写 `seo_gen_log`（tokens、耗时、成本）。
6. 可选：自动触发质量评估。

**内容类型与字段**：

| 类型 | 生成字段 |
|------|---------|
| blog | title, slug, metaDescription, headings[], body, tags[] |
| product | title, metaDescription, shortDesc, features[], specs{} |
| meta | title, description, tags[]（用于视频/页面 Meta） |

**多语种支持**：en / es / de / fr / ja / ko 六种起步，模板可扩展。

### 4.3 内容质量评估

**流程**：对内容按四个维度打分 → 加权得到总分 → 低于阈值标记"需重写"并输出建议。

**维度与算法**：

**（1）可读性**（英文用 Flesch Reading Ease）：

```
FRE = 206.835 - 1.015 × (总词数 / 总句数) - 84.6 × (总音节数 / 总词数)
```

FRE 映射到 0-100 分（60-70 为良好）。

**（2）SEO 合规**（规则检查，每项给分）：

| 检查项 | 规则 |
|--------|------|
| 标题长度 | 50-60 字符为满分区间 |
| Meta 描述长度 | 150-160 字符为满分区间 |
| H1 唯一性 | 正文有且仅有一个 H1 |
| 关键词密度 | 1%-3% 为满分区间 |
| 关键词出现位置 | 标题、首段、结尾各 +分 |

**（3）结构**：段落数（≥5）、小节数（≥3）、内链数、图片 alt 完备度。

**（4）多语种**：调用 LLM 判断语言地道性、语法、文化适配（复用 `AiClient`）。

**综合分**：

```
total = 0.3 × readability + 0.3 × seo + 0.2 × structure + 0.2 × language
```

`total >= 70` 判"达标"，否则"需重写"，并输出具体改进建议清单。

### 4.4 排名追踪与闭环优化

**流程**：

1. `RankScheduler` 按 cron（如每天凌晨 2 点）触发。
2. 取所有 status=已生成 的关键词，调用 `DataSourceAdapter.checkRank()`。
3. 排名落库 `seo_keyword_ranking`（rank 值，0 表示未进前 100）。
4. 前端 ECharts 绘制趋势。
5. **闭环规则**：某关键词连续 2 周排名下滑或跌出前 100，触发"优化建议"（更新内容 / 调整关键词）。

**流量估算**（辅助看板）：`估算流量 = 搜索量 × 点击率(CTR by rank)`，CTR 参考业界曲线（第 1 名约 28%、第 3 名约 10%、第 10 名约 2%）。

---

## 五、数据源方案与兜底策略（本课题成败关键）

### 5.1 约束与风险

关键词搜索量、真实排名等数据，正规来源（Ahrefs / SEMrush / Google Search Console）大多付费或需资质。**本课题在无外部付费资源的前提下，采用"适配器 + 分级兜底"策略。**

### 5.2 数据源适配器接口设计

```java
public interface DataSourceAdapter {
    /** 根据种子词获取候选关键词 */
    List<KeywordSuggestion> suggest(String seed, String language, String market);

    /** 检查一批关键词的排名，返回排名结果 */
    List<RankResult> checkRank(List<String> keywords, String domain);

    /** 数据源名称（用于日志与前端展示来源） */
    String name();
}
```

四个实现：

| 实现 | 数据来源 | 依赖 | 成本 |
|------|---------|------|------|
| `GoogleSuggestSource` | Google 自动补全建议（suggestqueries.google.com） | 无，免费 | 低 |
| `SerpCrawlSource` | 抓 SERP 结果页（Jsoup） | 无，免费 | 有反爬风险 |
| `ManualSource` | 用户 CSV/Excel 导入 | 无 | 低 |
| `MockDataSource` | 内置模拟数据 | 无 | 零 |

**运行时切换**：通过配置项 `seo.datasource.type=suggest|serp|manual|mock` 选择，业务代码零改动。

**这个适配器模式本身就是一个可答辩的技术亮点**：把"数据从哪来"与"业务怎么用"解耦，后续接入真实付费 API（如 Ahrefs）时无需改动业务代码。

### 5.3 关键词数据获取

- **首选**：Google Suggest API（`https://suggestqueries.google.com/complete/search?client=firefox&q={seed}`，返回 JSON，Jsoup/HttpClient 可抓）。
- **次选**：Google 相关搜索 + 自建种子词库（按行业分类的常用词表）。
- **兜底**：`MockDataSource` 内置模拟数据 + `ManualSource` 导入，保证无网络也能完整演示。

### 5.4 排名数据获取

- **首选**：Google Search Console API（免费，需站点验证 OAuth，作为进阶选项）。
- **次选**：`SerpCrawlSource` 抓 SERP 前 3 页，解析目标域名出现位置（加随机延迟 3-8s、限频）。
- **兜底**：手动录入排名 + 模拟趋势数据。

### 5.5 兜底原则

**系统必须保证"无任何外部数据源时仍可完整演示"**——这与校企项目 `AiService` 的 fallbackMock 降级思想一脉相承，是答辩演示的救命设计。兜底触发条件：数据源调用失败（网络异常/超时/反爬拦截）时自动降级到 mock，并在前端标注"演示数据"。

---

## 六、AI 服务层设计

### 6.1 校企项目 AiService 的拆分

校企项目的 `AiService` 是一个"上帝类"（AI 调用 + PDF 提取 + JSON 解析 + mock 混杂）。本课题**重构并复用**其核心思想，拆分为三个单一职责组件：

| 组件 | 职责 | 对应校企项目代码 |
|------|------|----------------|
| `AiClient` | HTTP 调用（超时、重试、限流、成本统计） | `callAI()` 的 HTTP 部分 |
| `PromptTemplateService` | Prompt 模板管理（存库、渲染、版本化） | 硬编码 Prompt 的抽象 |
| `AiResponseParser` | 解析 LLM 返回 JSON（容错、截取、校验） | `parseJsonResponse()` |

新增一个 SEO 场景，只需新增模板 + 一个调用方法，不再往单一类里堆代码。

### 6.2 Prompt 工程方案

**（1）结构化输出**：所有 Prompt 强制"只返回 JSON"，降低解析成本。

**（2）参数控制**：`temperature 0.3`（保证一致性）、`max_tokens` 按类型动态（blog 2000 / product 1000 / meta 500）。

**（3）多语种模板**：同一场景 × 6 语言，模板存表、可版本化、在线微调。

**（4）降级策略**（修复校企项目的混淆问题）：
- **未配置 Key** → 返回 mock（演示友好，前端标注"演示数据"）。
- **已配置但调用失败**（超时/网络/限流）→ **明确抛出异常并报错**，绝不喂假数据。

### 6.3 Prompt 模板示例（英文博客）

```
You are an expert SEO content writer for e-commerce brands going global.
Write a blog post that ranks for the keyword "{keyword}".

Market: {market}   Language: English
Tone: professional and informative
Length: 800-1200 words

Follow these rules:
- Use the keyword naturally in the title, first paragraph, and conclusion
- Keyword density: 1%-3%
- Use exactly one H1, and 3-6 H2 subheadings
- Include 2 internal-link suggestions and 3 meta tags

Return STRICT JSON only (no markdown, no extra text):
{
  "title": "string (50-60 chars)",
  "slug": "url-slug",
  "metaDescription": "string (150-160 chars)",
  "headings": ["H2-1", "H2-2", "H2-3"],
  "body": "full article text with markdown",
  "tags": ["tag1", "tag2", "tag3"]
}
```

### 6.4 成本与限流

- **成本统计**：记录每次生成的 prompt_tokens、completion_tokens、耗时，按模型单价折算成本，落 `seo_gen_log`。
- **限流**：基于 Redis 计数器，对 AI 接口做「每分钟 N 次 / 每用户 M 次」限流，防止额度被刷爆（校企项目缺失的能力）。
- **熔断**：连续失败 N 次后短时间熔断，避免无效重试消耗。

---

## 七、数据库设计

### 7.1 ER 关系

```
seo_site（站点）1 ── n seo_keyword（关键词）
seo_keyword（关键词）1 ── n seo_content（内容）
seo_content（内容）1 ── 1 seo_content_quality（质量评估）
seo_keyword（关键词）1 ── n seo_keyword_ranking（排名记录）
seo_site（站点）1 ── n seo_gen_task（批量任务）
seo_gen_task（任务）1 ── n seo_gen_log（生成日志）
seo_prompt_template（Prompt模板，独立配置表）
```

### 7.2 表结构（8 张，完整字段）

#### 7.2.1 `seo_site` 站点表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK 自增 | 主键 |
| name | VARCHAR(100) | NOT NULL | 站点/品牌名 |
| domain | VARCHAR(200) | NOT NULL | 域名 |
| industry | VARCHAR(100) | | 所属行业 |
| target_markets | VARCHAR(500) | | 目标市场，逗号分隔（US,DE,ES） |
| target_languages | VARCHAR(200) | | 目标语言，逗号分隔（en,es,de） |
| deleted | TINYINT | 默认 0 | 逻辑删除 0否 1是 |
| create_time | DATETIME | 默认当前 | 创建时间 |
| update_time | DATETIME | 自动更新 | 更新时间 |

#### 7.2.2 `seo_keyword` 关键词表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK 自增 | 主键 |
| site_id | BIGINT | FK | 所属站点 |
| keyword | VARCHAR(200) | NOT NULL | 关键词文本 |
| language | VARCHAR(10) | | 目标语言（en/es/...） |
| search_volume | INT | 默认 0 | 搜索量（无精确值时为 0） |
| volume_level | TINYINT | 默认 1 | 搜索量等级 1低 2中 3高 |
| competition | DECIMAL(5,2) | | 竞争度 0-1 |
| recommend_score | DECIMAL(5,2) | | 推荐度 0-100 |
| source | VARCHAR(20) | | 数据来源（suggest/serp/manual/mock） |
| status | TINYINT | 默认 1 | 1待分析 2已选 3已生成内容 |
| deleted | TINYINT | 默认 0 | 逻辑删除 |
| create_time / update_time | DATETIME | | 时间戳 |

#### 7.2.3 `seo_content` 内容表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK 自增 | 主键 |
| keyword_id | BIGINT | FK | 关联关键词 |
| content_type | VARCHAR(20) | NOT NULL | blog / product / meta |
| language | VARCHAR(10) | NOT NULL | 语言 |
| title | VARCHAR(200) | | 标题 |
| slug | VARCHAR(200) | | URL 别名 |
| meta_description | VARCHAR(300) | | Meta 描述 |
| body | TEXT | | 正文（markdown） |
| tags | VARCHAR(500) | | 标签，逗号分隔 |
| quality_score | DECIMAL(5,2) | | 质量总分 0-100 |
| status | TINYINT | 默认 1 | 1草稿 2已评估 3达标 4需重写 |
| deleted | TINYINT | 默认 0 | 逻辑删除 |
| create_time / update_time | DATETIME | | 时间戳 |

#### 7.2.4 `seo_content_quality` 质量评估表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK 自增 | 主键 |
| content_id | BIGINT | FK | 关联内容 |
| readability_score | DECIMAL(5,2) | | 可读性 0-100 |
| seo_score | DECIMAL(5,2) | | SEO 合规 0-100 |
| structure_score | DECIMAL(5,2) | | 结构 0-100 |
| language_score | DECIMAL(5,2) | | 多语种质量 0-100 |
| total_score | DECIMAL(5,2) | | 综合分 0-100 |
| keyword_density | DECIMAL(5,2) | | 关键词密度 % |
| suggestions | TEXT | | 改进建议 JSON |
| check_time | DATETIME | | 评估时间 |

#### 7.2.5 `seo_keyword_ranking` 排名表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK 自增 | 主键 |
| keyword_id | BIGINT | FK | 关联关键词 |
| platform | VARCHAR(20) | 默认 google | 平台 google/bing |
| rank | INT | 默认 0 | 排名（0=未进前 100） |
| check_date | DATE | | 检查日期 |
| create_time | DATETIME | | 记录时间 |

> 索引：`idx_keyword_date(keyword_id, check_date)` 加速趋势查询。

#### 7.2.6 `seo_prompt_template` Prompt 模板表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK 自增 | 主键 |
| scene | VARCHAR(30) | NOT NULL | 场景 blog/product/meta/quality |
| language | VARCHAR(10) | NOT NULL | 语言 |
| version | INT | 默认 1 | 版本号 |
| template | TEXT | NOT NULL | Prompt 模板文本 |
| enabled | TINYINT | 默认 1 | 是否启用 |
| create_time / update_time | DATETIME | | 时间戳 |

#### 7.2.7 `seo_gen_task` 批量任务表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK 自增 | 主键 |
| site_id | BIGINT | FK | 站点 |
| name | VARCHAR(100) | | 任务名 |
| keyword_ids | TEXT | | 关键词 id 列表（JSON） |
| content_type | VARCHAR(20) | | 内容类型 |
| languages | VARCHAR(200) | | 目标语言列表 |
| status | TINYINT | 默认 1 | 1排队 2执行中 3完成 4失败 |
| progress | INT | 默认 0 | 进度百分比 |
| total | INT | | 总条数 |
| success_count | INT | | 成功数 |
| fail_count | INT | | 失败数 |
| create_time / update_time | DATETIME | | 时间戳 |

#### 7.2.8 `seo_gen_log` 生成日志表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK 自增 | 主键 |
| content_id | BIGINT | | 关联内容（失败时为空） |
| task_id | BIGINT | | 关联任务 |
| scene | VARCHAR(30) | | 场景 |
| model | VARCHAR(50) | | 模型名（deepseek-chat） |
| language | VARCHAR(10) | | 语言 |
| prompt_tokens | INT | | 输入 tokens |
| completion_tokens | INT | | 输出 tokens |
| latency_ms | INT | | 耗时毫秒 |
| cost | DECIMAL(10,6) | | 成本（元） |
| status | TINYINT | | 1成功 2失败 |
| error_msg | VARCHAR(500) | | 错误信息 |
| create_time | DATETIME | | 时间 |

---

## 八、接口设计（REST API）

统一前缀 `/api`，鉴权 JWT，返回格式 `{ code, message, data }`（复用校企项目 `Result`）。

### 8.1 关键词模块

| 方法 | 路径 | 说明 | 关键参数 |
|------|------|------|---------|
| POST | /keyword/analyze | 分析主题获取候选词 | seed, language, market |
| GET | /keyword/list | 关键词分页列表 | siteId, status, page, size |
| POST | /keyword/import | 手动导入（CSV/Excel） | file |
| GET | /keyword/{id} | 关键词详情 | — |
| PUT | /keyword/{id}/status | 更新状态 | status |

### 8.2 内容模块

| 方法 | 路径 | 说明 | 关键参数 |
|------|------|------|---------|
| POST | /content/generate | 生成内容 | keywordId, type, language |
| GET | /content/list | 内容分页列表 | siteId, type, status, page |
| GET | /content/{id} | 内容详情 | — |
| PUT | /content/{id} | 编辑保存 | title, body, ... |
| DELETE | /content/{id} | 删除（逻辑） | — |

### 8.3 质量模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /quality/evaluate | 评估指定内容，返回评分+建议 |
| GET | /quality/{contentId} | 查询评估结果 |

### 8.4 排名模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /rank/trend | 关键词排名趋势（时间序列） |
| POST | /rank/check | 手动触发排名检查 |
| GET | /rank/top | Top 关键词排名 |

### 8.5 看板模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /dashboard/summary | 看板汇总（站点数/内容数/达标率/平均排名） |

### 8.6 模板与任务模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /template/list | 模板列表 |
| POST | /template/save | 新增模板 |
| PUT | /template/{id} | 编辑模板 |
| POST | /task/create | 创建批量生成任务 |
| GET | /task/{id} | 任务进度 |
| GET | /task/list | 任务列表 |

---

## 九、前端页面设计

### 9.1 页面清单

| 页面 | 路由 | 功能 |
|------|------|------|
| 工作台 | /dashboard | 看板汇总卡片 + 概览图表 |
| 站点管理 | /site | 站点/品牌的增删改查 |
| 关键词分析 | /keyword | 输入主题 → 候选词 → 指标 → 加入 |
| 内容生成 | /content | 选词+语言+类型 → 生成 → 预览编辑 |
| 质量评估 | /quality | 评分雷达 + 建议清单 + 达标标记 |
| 排名看板 | /rank | ECharts 趋势、Top 词、流量估算、告警 |
| 模板管理 | /template | Prompt 模板查看/编辑/版本 |
| 批量任务 | /task | 任务创建、进度条、结果列表 |

### 9.2 前端技术要点

- 路由 + 权限守卫复用校企项目结构（`router`、`stores`）。
- Axios 拦截器统一处理 401、loading、错误提示（复用）。
- ECharts 按需引入（`echarts/core` + 需要的图表），减小体积。
- 生成类操作用 loading 态 + 轮询任务进度（`/task/{id}`）。

---

## 十、开发排期（按工坊班节奏）

> 假设约 12 周、每周 1-2 次课，采用"课上推进 + 课后完善"节奏。

| 周次 | 阶段 | 具体任务 | 产出 |
|------|------|---------|------|
| 第 1 周 | 调研 | 了解出海 SEO、确认选题、调研数据源 | 需求文档 |
| 第 2 周 | 验证 | 试抓 Google Suggest、验证可行性、搭环境 | 数据源验证报告 |
| 第 3 周 | 骨架 | 复用校企项目骨架、初始化项目、AI 层拆分设计 | 可启动空项目 |
| 第 4 周 | AI 层 | 实现 AiClient / Parser / TemplateService | AI 层组件（**止损评估点**） |
| 第 5 周 | 数据库 | 建 8 张表、DataSourceAdapter 接口 + Mock 实现 | 数据库 + 适配器 |
| 第 6 周 | 关键词 | GoogleSuggestSource + 竞争度/推荐度算法 | 关键词分析模块 |
| 第 7 周 | 生成 | 内容生成模块 + Prompt 模板 + 生成流程 | 内容生成模块 |
| 第 8 周 | 多语种 | 6 语言模板 + 批量任务 | 批量生成能力 |
| 第 9 周 | 质量 | 可读性 + SEO 检查 + 打分 | 质量评估模块 |
| 第 10 周 | 排名 | 定时任务 + 排名采集 + 看板 | 排名追踪 + 看板 |
| 第 11 周 | 联调 | 全链路联调、Bug 修复 | 可演示完整系统 |
| 第 12 周 | 演示 | 演示材料、开题/中期报告 | 演示 + 文档 |

**止损评估点**：第 4 周结束前，若数据源/保利威资源仍无眉目，则全量启用 `MockDataSource` 兜底，确保系统可演示；毕设主体不依赖外部付费资源。

**MVP 优先原则**：先打通「关键词 → 生成 → 评估 → 排名 → 看板」主链路，进阶功能（XXL-Job、GSC API、熔断、多租户）后置。

---

## 十一、测试方案

| 测试类型 | 覆盖内容 |
|---------|---------|
| 单元测试 | 竞争度/推荐度算法、Flesch 可读性、关键词密度、JSON 解析容错 |
| 接口测试 | REST API 用 Postman/Apifox 覆盖主流程 |
| 降级测试 | 断开网络/未配 Key 时系统仍可演示 |
| 联调测试 | 关键词 → 生成 → 评估 → 排名全链路 |
| 异常测试 | LLM 超时、数据源反爬、批量任务失败重试 |

---

## 十二、风险与应对

| 风险 | 影响 | 应对 |
|------|------|------|
| 关键词/排名数据源缺失 | 影响真实性 | 适配器 + mock 兜底，弱化数据真实性诉求 |
| LLM 生成质量不稳定 | 内容质量 | 结构化 Prompt + 质量评估拦截 + 重写机制 |
| 爬虫被反爬 | 排名追踪失效 | 随机延迟 + 限频 + 降级手动录入 |
| 工坊班时间紧（1-3 月） | 交付风险 | MVP 优先，进阶功能后置 |
| 外部 API 调用失败 | 演示中断 | fallbackMock 降级，保证演示不断 |
| 保利威资源拿不到 | 课题弱化 | 说明性接口代替，不影响主体 |

---

## 十三、预期成果与答辩亮点

### 预期成果

1. 可运行的 SEO 内容自动化系统（Spring Boot + Vue 3 + MySQL + Redis）。
2. 多语种内容生成能力（≥6 种语言、≥3 类内容）。
3. 数据源适配器框架 + 分级兜底方案。
4. 排名追踪看板与闭环优化机制。
5. 完整论文 + GitHub 开源仓库。

### 答辩亮点（技术深度清单）

1. **数据源适配器模式**——可插拔，业务与数据解耦，后续可无缝接付费 API。
2. **AI 服务层单一职责拆分**——对校企项目"上帝类"的重构实践。
3. **闭环优化机制**——从数据采集到内容再优化的完整反馈链。
4. **Prompt 工程与降级容错**——结构化 JSON、模板版本化、fallbackMock 与真实失败严格区分。
5. **安全与限流**——AI 接口限流、成本统计、熔断（补齐校企项目短板）。
6. **自研算法**——竞争度评估、Flesch 可读性、关键词密度、流量估算。

---

> 本方案为详细设计阶段文档，可作为工坊班开题与后续开发依据；待导师反馈后细化到代码级实现。
