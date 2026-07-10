package com.recruit.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * AI 服务工具类
 * 调用 DeepSeek API 完成简历评分、人岗匹配、简历解析等 AI 任务。
 * 也提供 PDF 文本提取等工具方法。
 */
@Component
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${ai.api.model:deepseek-chat}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

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
     * 调用 DeepSeek API
     */
    private Map<String, Object> callAI(String prompt, String taskName) {
        // 没有配置 API Key 时返回模拟数据
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("sk-placeholder")) {
            log.warn("[AiService] API Key 未配置，返回模拟数据 (task={})", taskName);
            return fallbackMock(taskName);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 1024);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));
        requestBody.put("messages", messages);

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                List<Map> choices = (List<Map>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    return parseJsonResponse(content, taskName);
                }
            }
            log.error("[AiService] API 返回异常, response={}", response.getBody());
        } catch (Exception e) {
            log.error("[AiService] API 调用失败 (task={}), {}", taskName, e.getMessage());
        }

        return fallbackMock(taskName);
    }

    /**
     * 解析 LLM 返回的 JSON 字符串
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

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("[AiService] JSON 解析失败 (task={}), content={}", taskName, content, e);
            return fallbackMock(taskName);
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
        }
        return mock;
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
