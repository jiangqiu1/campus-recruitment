package com.recruit.controller;

import com.recruit.entity.AiParseLog;
import com.recruit.service.AiParseLogService;
import com.recruit.service.ResumeService;
import com.recruit.utils.AiService;
import com.recruit.utils.Result;
import com.recruit.annotation.LogOperation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI解析日志控制器
 * 只有教师可以访问
 */
@Slf4j
@RestController
@RequestMapping("/ai-parse")
public class AiParseController extends BaseController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private AiParseLogService aiParseLogService;

    @Autowired
    private AiService aiService;

    @Autowired
    private ResumeService resumeService;

    /**
     * 获取所有AI解析日志
     *
     * @return AI解析日志列表
     */
    @GetMapping("/logs")
    public Result<List<AiParseLog>> getAllLogs() {
        List<AiParseLog> logs = aiParseLogService.list();
        return Result.success(logs);
    }

    /**
     * 根据ID获取AI解析日志
     *
     * @param id 日志ID
     * @return AI解析日志实体
     */
    @GetMapping("/logs/{id}")
    public Result<AiParseLog> getLogById(@PathVariable Long id) {
        AiParseLog log = aiParseLogService.getById(id);
        if (log == null) {
            return Result.error(404, "AI解析日志不存在");
        }
        return Result.success(log);
    }

    /**
     * 根据教师ID查询AI解析日志
     *
     * @param teacherId 教师ID
     * @return AI解析日志列表
     */
    @GetMapping("/logs/by-teacher/{teacherId}")
    public Result<List<AiParseLog>> getLogsByTeacherId(@PathVariable Long teacherId) {
        List<AiParseLog> logs = aiParseLogService.selectByTeacherId(teacherId);
        return Result.success(logs);
    }

    /**
     * 查询未人工修正的AI解析日志
     *
     * @return AI解析日志列表
     */
    @GetMapping("/logs/uncorrected")
    public Result<List<AiParseLog>> getUncorrectedLogs() {
        List<AiParseLog> logs = aiParseLogService.selectUncorrected();
        return Result.success(logs);
    }

    /**
     * 根据置信度范围查询AI解析日志
     *
     * @param minScore 最小置信度
     * @param maxScore 最大置信度
     * @return AI解析日志列表
     */
    @GetMapping("/logs/by-confidence-range")
    public Result<List<AiParseLog>> getLogsByConfidenceRange(
            @RequestParam BigDecimal minScore,
            @RequestParam BigDecimal maxScore) {
        List<AiParseLog> logs = aiParseLogService.selectByConfidenceRange(minScore, maxScore);
        return Result.success(logs);
    }

    /**
     * 根据时间范围查询AI解析日志
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return AI解析日志列表
     */
    @GetMapping("/logs/by-time-range")
    public Result<List<AiParseLog>> getLogsByTimeRange(
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime) {
        List<AiParseLog> logs = aiParseLogService.selectByTimeRange(startTime, endTime);
        return Result.success(logs);
    }

    /**
     * AI解析简历
     *
     * @param params 包含teacherId、rawMessage的参数
     * @return 解析结果(JSON格式)
     */
    @LogOperation("AI解析简历")
    @PostMapping("/parse")
    public Result<Map<String, String>> parse(
            @RequestBody Map<String, Object> params) {

        requireTeacher();
        Long teacherId = getCurrentUserId();
        String rawMessage = params.get("rawMessage").toString();

        // 调用 AI 解析服务
        Map<String, Object> aiResult = aiService.parseResume(rawMessage);

        // 转换为 JSON 字符串
        String parsedResult;
        try {
            parsedResult = OBJECT_MAPPER.writeValueAsString(aiResult);
        } catch (JsonProcessingException e) {
            parsedResult = "{}";
        }

        // 计算置信度（简单根据字段填充率估算）
        int filledFields = 0;
        int totalFields = 8; // name, phone, email, school, major, education, skills, experience
        for (String key : new String[]{"name", "phone", "email", "school", "major", "education", "experience"}) {
            Object val = aiResult.get(key);
            if (val != null && !val.toString().isEmpty()) filledFields++;
        }
        Object skills = aiResult.get("skills");
        if (skills instanceof Collection && !((Collection) skills).isEmpty()) filledFields++;
        
        BigDecimal confidenceScore = new BigDecimal(filledFields)
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(totalFields), 2, RoundingMode.HALF_UP)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        // 记录AI解析日志
        boolean success = aiParseLogService.logParse(teacherId, rawMessage, parsedResult, confidenceScore);
        if (!success) {
            return Result.error("AI解析失败");
        }

        Map<String, String> result = new HashMap<>();
        result.put("parsedResult", parsedResult);
        result.put("confidenceScore", confidenceScore.toString());

        return Result.success("AI解析成功", result);
    }

    /**
     * AI解析岗位描述：从岗位描述文本中提取结构化信息，
     * 用于自动填写岗位发布表单
     *
     * @param params 包含rawMessage（岗位描述文本）
     * @return 解析结果（title, salaryRange, location, education, experience, description, requirements, skills）
     */
    @LogOperation("AI解析岗位描述")
    @PostMapping("/parse-job")
    public Result<Map<String, Object>> parseJob(
            @RequestBody Map<String, Object> params) {

        requireTeacher();
        String rawMessage = params.get("rawMessage").toString();

        // 调用 AI 解析服务
        Map<String, Object> aiResult = aiService.parseJobDescription(rawMessage);

        return Result.success("岗位解析成功", aiResult);
    }

    /**
     * 简历分析：分析学生简历的不足并给出改进建议
     * 分析结果会保存到简历表中，供学生和教师随时查看
     * 简历在最近诊断后未变更时直接返回缓存结果，force=true 强制重新分析
     *
     * @param params 包含 studentId、force（可选）
     * @return 分析结果（评分、优势、不足、建议等，cached=true 表示命中缓存）
     */
    @LogOperation("AI分析简历")
    @PostMapping("/analyze-resume")
    public Result<Map<String, Object>> analyzeResume(
            @RequestBody Map<String, Object> params) {

        requireTeacher();
        Long studentId = Long.valueOf(params.get("studentId").toString());

        // 获取学生简历
        com.recruit.entity.Resume resume = resumeService.selectByStudentId(studentId);
        if (resume == null) {
            return Result.error(404, "该学生暂无简历");
        }

        boolean force = Boolean.parseBoolean(String.valueOf(params.get("force")));

        // 诊断（带缓存：简历未变更时复用已有结果，不重复调用 AI）
        Map<String, Object> aiResult = resumeService.analyzeWithCache(resume, force);
        boolean cached = Boolean.TRUE.equals(aiResult.get("cached"));

        return Result.success(cached ? "简历未变更，已返回最近诊断结果" : "简历分析成功", aiResult);
    }

    /**
     * 人工修正AI解析结果
     *
     * @param id 日志ID
     * @param params 包含correctedResult的参数
     * @return 修正结果
     */
    @LogOperation("人工修正AI解析结果")
    @PutMapping("/logs/{id}/correct")
    public Result<String> correctParseResult(
            @PathVariable Long id,
            @RequestBody Map<String, String> params) {

        requireTeacher();

        String correctedResult = params.get("correctedResult");
        if (correctedResult == null || correctedResult.isEmpty()) {
            return Result.error("correctedResult不能为空");
        }

        boolean success = aiParseLogService.correctParseResult(id, correctedResult);
        if (!success) {
            return Result.error("人工修正失败");
        }

        return Result.success("人工修正成功");
    }
    
    /**
     * 统计教师AI解析次数
     *
     * @param teacherId 教师ID
     * @return 解析次数
     */
    @GetMapping("/statistics/count-by-teacher/{teacherId}")
    public Result<Map<String, Integer>> countByTeacherId(@PathVariable Long teacherId) {
        Integer count = aiParseLogService.countByTeacherId(teacherId);

        Map<String, Integer> result = new java.util.HashMap<>();
        result.put("count", count);

        return Result.success(result);
    }

    /**
     * 统计平均置信度
     *
     * @return 平均置信度
     */
    @GetMapping("/statistics/average-confidence")
    public Result<Map<String, BigDecimal>> calculateAverageConfidence() {
        BigDecimal averageConfidence = aiParseLogService.calculateAverageConfidence();

        Map<String, BigDecimal> result = new java.util.HashMap<>();
        result.put("averageConfidence", averageConfidence);

        return Result.success(result);
    }

    /**
     * 统计需要人工修正的日志数量
     *
     * @return 需要修正的日志数量
     */
    @GetMapping("/statistics/uncorrected-count")
    public Result<Map<String, Integer>> countUncorrected() {
        Integer count = aiParseLogService.countUncorrected();

        Map<String, Integer> result = new java.util.HashMap<>();
        result.put("uncorrectedCount", count);

        return Result.success(result);
    }
}
