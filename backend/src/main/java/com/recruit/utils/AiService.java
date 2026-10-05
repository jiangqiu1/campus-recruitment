package com.recruit.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.config.AiProperties;
import com.recruit.entity.AiParseLog;
import com.recruit.service.AiParseLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI 服务工具类
 * 通过 OpenAI 兼容接口调用多个大模型（DeepSeek / 智谱 GLM，见 AiProperties），
 * 完成简历评分、人岗匹配、简历解析、模拟面试等 AI 任务。
 * 每次调用（含降级 mock）都会写入 ai_parse_log 一条记录：provider/任务/耗时/mock标记，
 * 作为毕设"多模型对比实验"的数据来源。
 * 也提供 PDF 文本提取等工具方法。
 */
@Component
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiProperties aiProperties;

    private final AiParseLogService aiParseLogService;

    private final RestTemplate restTemplate = buildRestTemplate();

    public AiService(AiProperties aiProperties, AiParseLogService aiParseLogService) {
        this.aiProperties = aiProperties;
        this.aiParseLogService = aiParseLogService;
    }

    /**
     * 构建带连接/读取超时的 RestTemplate（AI 生成最长可达 30s+，读取超时需放宽到 60s）
     */
    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        return new RestTemplate(factory);
    }

    /**
     * 简历评分：根据岗位描述和简历内容打分
     */
    public Map<String, Object> scoreResume(String jobDescription, String jobRequirements, String resumeText) {
        String prompt = "你是一个专业的招聘专家。请评估以下简历与招聘岗位的匹配度。\n\n"
                + "## 岗位描述\n" + jobDescription + "\n\n"
                + "## 任职要求\n" + jobRequirements + "\n\n"
                + "## 简历内容\n" + truncate(resumeText, 2000) + "\n\n"
                + "请严格按以下 JSON 格式返回评分结果（所有分数为0-100的整数）：\n"
                + "{\n"
                + "  \"totalScore\": 整体匹配度,\n"
                + "  \"skillScore\": 技能匹配度（考察技术栈匹配程度）,\n"
                + "  \"expScore\": 经验匹配度（考察工作/项目经验）,\n"
                + "  \"eduScore\": 学历匹配度（考察学历和专业）,\n"
                + "  \"comment\": \"20字以内的综合评价\"\n"
                + "}\n"
                + "只返回 JSON，不要包含其他文字。";

        return callAI(prompt, "scoreResume");
    }

    /**
     * 人岗匹配：计算某学生与某岗位的匹配度（含子维度）
     */
    public Map<String, Object> matchJob(String jobTitle, String jobDescription, String jobRequirements,
                                          String studentName, String resumeText) {
        String prompt = "你是一个招聘匹配专家。请从多个维度评估以下学生与岗位的匹配程度。\n\n"
                + "## 岗位名称\n" + jobTitle + "\n\n"
                + "## 岗位描述\n" + jobDescription + "\n\n"
                + "## 任职要求\n" + jobRequirements + "\n\n"
                + "## 学生简历\n" + truncate(resumeText, 2000) + "\n\n"
                + "请严格按以下 JSON 格式返回（所有分数为0-100的整数）：\n"
                + "{\n"
                + "  \"matchScore\": 整体匹配度,\n"
                + "  \"skillMatch\": 技能匹配度（考察技术栈与岗位技能要求的重合程度，如Java,Spring等）,\n"
                + "  \"eduMatch\": 学历匹配度（考察学历层次与岗位要求的匹配，如本科/硕士/博士）,\n"
                + "  \"expMatch\": 经验匹配度（考察实习/项目经验与岗位方向的关联程度）,\n"
                + "  \"majorFit\": 专业契合度（考察所学专业与岗位类别的契合程度）,\n"
                + "  \"matchReason\": \"20字以内的匹配理由\"\n"
                + "}\n"
                + "只返回 JSON，不要包含其他文字。";

        return callAI(prompt, "matchJob");
    }

    /**
     * 简历解析：从原始文本中提取结构化信息
     */
    public Map<String, Object> parseResume(String rawText) {
        String prompt = "你是一个简历解析专家。请从以下简历文本中提取结构化信息。\n\n"
                + "## 简历文本\n" + truncate(rawText, 3000) + "\n\n"
                + "请严格按以下 JSON 格式返回：\n"
                + "{\n"
                + "  \"name\": \"姓名\",\n"
                + "  \"phone\": \"手机号\",\n"
                + "  \"email\": \"邮箱\",\n"
                + "  \"school\": \"毕业院校\",\n"
                + "  \"major\": \"专业\",\n"
                + "  \"education\": \"学历\",\n"
                + "  \"educationStart\": \"入学年份（如2023.09）\",\n"
                + "  \"educationEnd\": \"毕业年份（如2026.06）\",\n"
                + "  \"skills\": [\"技能1\", \"技能2\"],\n"
                + "  \"internshipCompany\": \"实习公司名称\",\n"
                + "  \"internshipPosition\": \"实习职位\",\n"
                + "  \"internshipDuration\": \"实习时间（如2025.07-2025.12）\",\n"
                + "  \"internshipDesc\": \"实习工作描述\",\n"
                + "  \"projects\": [{\"name\": \"项目名称\", \"role\": \"担任角色\", \"duration\": \"项目时间（如2025.03-2025.06）\", \"description\": \"项目描述：使用技术、取得的成果等\"}],\n"
                + "  \"selfEvaluation\": \"自我评价\"\n"
                + "}\n"
                + "字段缺失时用空字符串或空数组代替。只返回 JSON，不要包含其他文字。";

        return callAI(prompt, "parseResume");
    }

    /**
     * 岗位解析：从岗位描述文本中提取结构化信息（自动填写表单）
     * 教师粘贴一段岗位描述后，AI 提取标题、薪资、地点、学历、经验等字段
     */
    public Map<String, Object> parseJobDescription(String rawText) {
        String prompt = "你是一个岗位描述解析专家。请从以下岗位描述文本中提取结构化信息。\n\n"
                + "## 岗位描述文本\n" + truncate(rawText, 3000) + "\n\n"
                + "请严格按以下 JSON 格式返回：\n"
                + "{\n"
                + "  \"title\": \"岗位名称\",\n"
                + "  \"companyName\": \"公司/企业名称（从文本中提取的公司名，若无则填空字符串）\",\n"
                + "  \"salaryRange\": \"薪资范围，如 8K-15K\",\n"
                + "  \"education\": \"学历要求，如大专及以上、本科及以上\",\n"
                + "  \"location\": \"工作地点\",\n"
                + "  \"deadline\": \"截止日期，如2026-07-16（标准日期格式）\",\n"
                + "  \"experience\": \"经验要求，如1-3年、3-5年、经验不限\",\n"
                + "  \"description\": \"岗位职责描述（保留原文核心内容）\",\n"
                + "  \"requirements\": \"任职要求（保留原文核心内容）\",\n"
                + "  \"skills\": [\"技能1\", \"技能2\"]\n"
                + "}\n"
                + "字段缺失时用空字符串或空数组代替。只返回 JSON，不要包含其他文字。";

        return callAI(prompt, "parseJob");
    }

    /**
     * 简历分析：分析学生简历的不足并给出改进建议
     * 输入简历的教育、技能、实习等结构化数据，AI 给出评分和改进方向
     */
    public Map<String, Object> analyzeResume(String resumeJson) {
        String prompt = "你是一个简历优化专家。请分析以下简历数据，找出不足并给出改进建议。\n\n"
                + "## 简历数据\n" + truncate(resumeJson, 3000) + "\n\n"
                + "请严格按以下 JSON 格式返回：\n"
                + "{\n"
                + "  \"overallScore\": \"综合评分（0-100的整数）\",\n"
                + "  \"strengths\": [\"优势1\", \"优势2\"],\n"
                + "  \"weaknesses\": [\"不足1\", \"不足2\"],\n"
                + "  \"suggestions\": [\"改进建议1\", \"改进建议2\", \"改进建议3\"],\n"
                + "  \"missingFields\": [\"缺失字段1\", \"缺失字段2\"],\n"
                + "  \"recommendedSkills\": [\"推荐补充的技能1\", \"推荐补充的技能2\"]\n"
                + "}\n"
                + "字段缺失时用空数组代替。只返回 JSON，不要包含其他文字。";

        return callAI(prompt, "analyzeResume");
    }

    /**
     * 模拟面试：针对岗位和候选人简历生成面试题
     *
     * @return {questions: [{id, type: "技术|项目|行为", question}]}
     */
    public Map<String, Object> generateInterviewQuestions(String jobTitle, String jobDescription,
                                                          String jobRequirements, String resumeText, int count) {
        String prompt = "你是一个面试官。请针对以下岗位和候选人简历，出 " + count + " 道面试题。\n\n"
                + "## 岗位名称\n" + jobTitle + "\n\n"
                + "## 岗位职责\n" + jobDescription + "\n\n"
                + "## 任职要求\n" + jobRequirements + "\n\n"
                + "## 候选人简历\n" + truncate(resumeText, 2000) + "\n\n"
                + "出题要求：结合岗位要求与候选人简历背景，覆盖技术、项目、行为三类；题目要具体、可回答，不要泛泛而谈。\n\n"
                + "请严格按以下 JSON 格式返回：\n"
                + "{\n"
                + "  \"questions\": [\n"
                + "    {\"id\": 1, \"type\": \"技术\", \"question\": \"题目内容\"},\n"
                + "    {\"id\": 2, \"type\": \"项目\", \"question\": \"题目内容\"},\n"
                + "    {\"id\": 3, \"type\": \"行为\", \"question\": \"题目内容\"}\n"
                + "  ]\n"
                + "}\n"
                + "type 只能是「技术」「项目」「行为」三种之一，共 " + count + " 道题，id 从 1 递增。"
                + "只返回 JSON，不要包含其他文字。";

        // 题目数量多，max_tokens 放宽到 2048
        return callAI(prompt, "genQuestions", null, 2048);
    }

    /**
     * 模拟面试：点评候选人的面试作答
     *
     * @return {score: 0-100, comment: "点评", betterAnswer: "参考答案"}
     */
    public Map<String, Object> evaluateAnswer(String jobTitle, String question, String answer, String resumeText) {
        String prompt = "你是一个面试官。请点评候选人对以下面试题的回答。\n\n"
                + "## 岗位名称\n" + jobTitle + "\n\n"
                + "## 面试题\n" + question + "\n\n"
                + "## 候选人回答\n" + truncate(answer, 1500) + "\n\n"
                + "## 候选人简历背景\n" + truncate(resumeText, 1000) + "\n\n"
                + "请严格按以下 JSON 格式返回：\n"
                + "{\n"
                + "  \"score\": 评分（0-100的整数，考察切题程度、内容质量、表达条理）,\n"
                + "  \"comment\": \"点评（100字以内，先说优点再指出不足）\",\n"
                + "  \"betterAnswer\": \"参考答案（150字以内，示范一个更好的回答思路）\"\n"
                + "}\n"
                + "只返回 JSON，不要包含其他文字。";

        return callAI(prompt, "evalAnswer");
    }

    /**
     * 调用 AI（默认提供方，max_tokens=1024）
     */
    private Map<String, Object> callAI(String prompt, String taskName) {
        return callAI(prompt, taskName, null, 1024);
    }

    /**
     * 调用 AI（指定提供方与 max_tokens），并写入调用日志
     *
     * @param providerName 提供方名称（deepseek/glm），null 时用 ai.default-provider
     */
    @SuppressWarnings("unchecked")
    // Jackson 把响应反序列化为 Map 后，嵌套的 choices/message 结构只能运行时强转
    private Map<String, Object> callAI(String prompt, String taskName, String providerName, int maxTokens) {
        String name = (providerName == null || providerName.isEmpty())
                ? aiProperties.getDefaultProvider() : providerName;
        AiProperties.Provider provider = aiProperties.getProviders() != null
                ? aiProperties.getProviders().get(name) : null;

        long start = System.currentTimeMillis();

        // 没有配置该提供方或 API Key 缺失时返回模拟数据
        if (provider == null || provider.getKey() == null || provider.getKey().isEmpty()
                || "sk-placeholder".equals(provider.getKey())) {
            log.warn("[AiService] AI 提供方 [{}] 未配置，返回模拟数据 (task={})", name, taskName);
            Map<String, Object> mock = fallbackMock(taskName);
            logAiCall(taskName, name, System.currentTimeMillis() - start, true, prompt, toJson(mock));
            
            return mock;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(provider.getKey());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", provider.getModel());
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", maxTokens);
        // 智谱 GLM 是思考模型：不关思考链的话 max_tokens 会被 reasoning 耗尽，content 返回空
        if (provider.getUrl() != null && provider.getUrl().contains("bigmodel.cn")) {
            requestBody.put("thinking", Map.of("type", "disabled"));
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));
        requestBody.put("messages", messages);

        String content = null;
        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            // 小模型输出的 JSON 偶有概率性格式瑕疵（如字符串内未转义引号），解析失败时重试一次
            for (int attempt = 1; attempt <= 2; attempt++) {
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        provider.getUrl(), HttpMethod.POST, request, new ParameterizedTypeReference<Map<String, Object>>() {});

                Map<String, Object> body = response.getBody();
                if (body != null && body.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                        content = (String) message.get("content");
                        Map<String, Object> parsed = parseJsonResponse(content, taskName);
                        if (parsed != null) {
                            logAiCall(taskName, name, System.currentTimeMillis() - start, false, prompt, content);
                            return parsed;
                        }
                        if (attempt == 1) {
                            log.warn("[AiService] JSON 解析失败，重试一次 (task={}, provider={})", taskName, name);
                            continue;
                        }
                    }
                } else {
                    log.error("[AiService] API 返回异常, provider={}, response={}", name, body);
                    break;
                }
            }
        } catch (Exception e) {
            log.error("[AiService] API 调用失败 (task={}, provider={}), {}", taskName, name, e.getMessage());
        }

        Map<String, Object> mock = fallbackMock(taskName);
        logAiCall(taskName, name, System.currentTimeMillis() - start, true, prompt, content == null ? "" : content);
        return mock;
    }

    /**
     * 解析 LLM 返回的 JSON 字符串
     *
     * @return 解析失败时返回 null（由 callAI 统一降级 mock）
     */
    private Map<String, Object> parseJsonResponse(String content, String taskName) {
        try {
            // 尝试提取 JSON（LLM 有时会包含 ```json 标记）
            String json = content;
            if (json.contains("```")) {
                json = json.replaceAll("```(?:json)?", "").trim();
            }
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}') + 1;
            if (start >= 0 && end > start) {
                json = json.substring(start, end);
            }

            return OBJECT_MAPPER.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("[AiService] JSON 解析失败 (task={}), content={}", taskName, content, e);
            return null;
        }
    }

    /**
     * 降级：返回模拟数据（API 不可用时）
     */
    private Map<String, Object> fallbackMock(String taskName) {
        Map<String, Object> mock = new HashMap<>();
        switch (taskName) {
            case "scoreResume":
                mock.put("totalScore", 78);
                mock.put("skillScore", 82);
                mock.put("expScore", 72);
                mock.put("eduScore", 80);
                mock.put("comment", "整体匹配度良好，技能匹配突出");
                break;
            case "matchJob":
                mock.put("matchScore", 82);
                mock.put("skillMatch", 85);
                mock.put("eduMatch", 80);
                mock.put("expMatch", 72);
                mock.put("majorFit", 78);
                mock.put("matchReason", "技能和项目经验匹配度高");
                break;
            case "parseResume":
                mock.put("name", "");
                mock.put("phone", "");
                mock.put("email", "");
                mock.put("school", "");
                mock.put("major", "");
                mock.put("education", "");
                mock.put("educationStart", "");
                mock.put("educationEnd", "");
                mock.put("skills", new ArrayList<>());
                mock.put("internshipCompany", "");
                mock.put("internshipPosition", "");
                mock.put("internshipDuration", "");
                mock.put("internshipDesc", "");
                mock.put("projects", new ArrayList<>());
                mock.put("selfEvaluation", "");
                break;
            case "parseJob":
                mock.put("title", "前端开发工程师");
                mock.put("companyName", "");
                mock.put("salaryRange", "8K-15K");
                mock.put("education", "大专及以上");
                mock.put("location", "广州");
                mock.put("deadline", "2026-08-15");
                mock.put("experience", "1-3年");
                mock.put("description", "负责公司核心产品的前端开发与维护，使用 Vue.js 框架进行组件化开发，与后端工程师协作完成功能联调");
                mock.put("requirements", "1. 熟练掌握 HTML5、CSS3、JavaScript\n2. 熟练使用 Vue.js 框架\n3. 了解前端工程化\n4. 有良好的团队协作能力");
                mock.put("skills", new ArrayList<>() {{ add("Vue.js"); add("JavaScript"); add("HTML5"); add("CSS3"); }});
                break;
            case "analyzeResume":
                mock.put("overallScore", 72);
                mock.put("strengths", new ArrayList<>() {{ add("专业技能匹配度高"); add("实习经历丰富"); }});
                mock.put("weaknesses", new ArrayList<>() {{ add("自我评价过于简短"); add("缺少项目经验描述"); }});
                mock.put("suggestions", new ArrayList<>() {{ add("建议详细描述项目经验，按 STAR 法则展开"); add("建议补充个人技能标签"); add("建议增加求职意向说明"); }});
                mock.put("missingFields", new ArrayList<>() {{ add("求职意向"); add("项目经历"); }});
                mock.put("recommendedSkills", new ArrayList<>() {{ add("Git"); add("Linux"); }});
                break;
            case "genQuestions":
                mock.put("questions", buildMockQuestions());
                break;
            case "evalAnswer":
                mock.put("score", 75);
                mock.put("comment", "回答基本切题，结构清晰，但缺少具体例子和数据支撑");
                mock.put("betterAnswer", "建议采用「结论 + 具体事例 + 结果」的结构回答，结合自己的项目经历给出可验证的细节");
                break;
        }
        return mock;
    }

    /**
     * 模拟面试题（降级用）
     */
    private List<Map<String, Object>> buildMockQuestions() {
        String[][] defs = {
                {"技术", "请介绍一下你最熟悉的技术栈，并说明在实际项目中如何使用"},
                {"项目", "讲一个你最有成就感的项目：背景、你承担的角色、最终成果"},
                {"技术", "如果项目上线后出现异常，你会如何定位和解决问题"},
                {"行为", "与团队成员意见不一致时，你会怎么沟通处理"},
                {"项目", "项目推进中遇到的最大困难是什么，你是怎么解决的"}
        };
        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 0; i < defs.length; i++) {
            Map<String, Object> q = new HashMap<>();
            q.put("id", i + 1);
            q.put("type", defs[i][0]);
            q.put("question", defs[i][1]);
            questions.add(q);
        }
        return questions;
    }

    /**
     * AI 调用日志：每次调用（含降级 mock）写一条 ai_parse_log，
     * 记录 provider/任务/耗时/mock 标记，供多模型对比实验使用。
     * 任何异常只记 warn，不影响主流程。
     */
    private void logAiCall(String taskName, String providerName, long latencyMs, boolean mock,
                           String prompt, String resultContent) {
        try {
            AiParseLog entry = new AiParseLog();
            entry.setTaskName(taskName);
            entry.setProvider(providerName);
            entry.setLatencyMs((int) latencyMs);
            entry.setMockFlag(mock ? 1 : 0);
            entry.setUserId(currentUserIdOrNull());
            entry.setRawMessage(truncate(prompt, 300));
            entry.setParsedResult(truncate(resultContent == null ? "" : resultContent, 2000));
            entry.setIsManualCorrected(0);
            entry.setCreateTime(LocalDateTime.now());
            aiParseLogService.save(entry);
        } catch (Exception ex) {
            log.warn("[AiService] 写入AI调用日志失败: {}", ex.getMessage());
        }
    }

    /**
     * 尽力获取当前登录用户ID（无请求上下文或未登录时返回 null）
     */
    private Long currentUserIdOrNull() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            Object val = attrs.getRequest().getAttribute("userId");
            if (val == null) {
                return null;
            }
            return val instanceof Long ? (Long) val : Long.valueOf(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 从 PDF 文件中提取文本内容
     */
    public String extractTextFromPdf(MultipartFile file) throws IOException {
        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(file.getInputStream())) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(document.getNumberOfPages());
            String text = stripper.getText(document);
            return text != null ? text.trim() : "";
        }
    }

    /**
     * 截断文本（控制 token 消耗）
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "\n...(截断)";
    }
}
