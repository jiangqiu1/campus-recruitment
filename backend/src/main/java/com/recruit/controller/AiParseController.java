package com.recruit.controller;

import com.recruit.entity.AiParseLog;
import com.recruit.service.AiParseLogService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI解析日志控制器
 * 只有教师可以访问
 */
@RestController
@RequestMapping("/ai-parse")
public class AiParseController {

    @Autowired
    private AiParseLogService aiParseLogService;

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
     * AI解析(模拟)
     *
     * @param params 包含teacherId、rawMessage的参数
     * @return 解析结果(JSON格式)
     */
    @PostMapping("/parse")
    public Result<Map<String, String>> parse(
            @RequestBody Map<String, Object> params) {

        Long teacherId = Long.valueOf(params.get("teacherId").toString());
        String rawMessage = params.get("rawMessage").toString();

        // TODO:调用AI解析服务(讯飞星火OCR + NLP)
        // 这里先返回模拟数据
        String parsedResult = "{\"education\":\"本科\",\"skills\":[\"Java\",\"Spring\",\"MySQL\"],\"experience\":\"2年开发经验\"}";
        BigDecimal confidenceScore = new BigDecimal("0.85"); // 模拟置信度85%

        // 记录AI解析日志
        boolean success = aiParseLogService.logParse(teacherId, rawMessage, parsedResult, confidenceScore);
        if (!success) {
            return Result.error("AI解析失败");
        }

        Map<String, String> result = new java.util.HashMap<>();
        result.put("parsedResult", parsedResult);
        result.put("confidenceScore", confidenceScore.toString());

        return Result.success("AI解析成功", result);
    }
    
    /**
     * 人工修正AI解析结果
     *
     * @param id 日志ID
     * @param params 包含correctedResult的参数
     * @return 修正结果
     */
    @PutMapping("/logs/{id}/correct")
    public Result<String> correctParseResult(
            @PathVariable Long id,
            @RequestBody Map<String, String> params) {

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
