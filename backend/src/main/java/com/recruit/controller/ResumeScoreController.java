package com.recruit.controller;

import com.recruit.entity.Delivery;
import com.recruit.entity.ResumeScoreLog;
import com.recruit.entity.SysUser;
import com.recruit.service.DeliveryService;
import com.recruit.service.ResumeScoreLogService;
import com.recruit.service.UserService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/resume-scores")
public class ResumeScoreController {

    @Autowired
    private ResumeScoreLogService resumeScoreLogService;

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private UserService userService;

    @GetMapping
    public Result<List<ResumeScoreLog>> getAllScores() {
        return Result.success(resumeScoreLogService.list());
    }

    @GetMapping("/{id}")
    public Result<ResumeScoreLog> getScoreById(@PathVariable Long id) {
        ResumeScoreLog log = resumeScoreLogService.getById(id);
        if (log == null) return Result.error(404, "评分记录不存在");
        return Result.success(log);
    }

    @GetMapping("/by-job/{jobId}")
    public Result<List<Map<String, Object>>> getScoresByJobId(@PathVariable Long jobId) {
        List<ResumeScoreLog> logs = resumeScoreLogService.selectByJobIdOrderByScore(jobId);
        // 批量组装：一次查投递 + 一次查学生，替代循环内逐条查询
        List<Long> deliveryIds = logs.stream().map(ResumeScoreLog::getDeliveryId).filter(java.util.Objects::nonNull).collect(Collectors.toList());
        Map<Long, Delivery> deliveryMap = deliveryIds.isEmpty() ? new HashMap<>() : deliveryService.listByIds(deliveryIds).stream()
                .collect(Collectors.toMap(Delivery::getId, d -> d));
        List<Long> studentIds = deliveryMap.values().stream().map(Delivery::getStudentId).filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, SysUser> studentMap = studentIds.isEmpty() ? new HashMap<>() : userService.listByIds(studentIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ResumeScoreLog log : logs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", log.getId());
            item.put("jobId", log.getJobId());
            item.put("deliveryId", log.getDeliveryId());
            item.put("score", log.getScore());
            item.put("scoreDetail", log.getScoreDetail());
            item.put("createTime", log.getCreateTime());
            String studentName = "未知";
            if (log.getDeliveryId() != null) {
                Delivery delivery = deliveryMap.get(log.getDeliveryId());
                if (delivery != null && delivery.getStudentId() != null) {
                    SysUser student = studentMap.get(delivery.getStudentId());
                    if (student != null && student.getRealName() != null) {
                        studentName = student.getRealName();
                    }
                }
            }
            item.put("studentName", studentName);
            result.add(item);
        }
        return Result.success(result);
    }

    @GetMapping("/by-delivery/{deliveryId}")
    public Result<ResumeScoreLog> getScoreByDeliveryId(@PathVariable Long deliveryId) {
        ResumeScoreLog log = resumeScoreLogService.selectByDeliveryId(deliveryId);
        if (log == null) return Result.error(404, "该投递记录的评分不存在");
        return Result.success(log);
    }

    @GetMapping("/by-score-range")
    public Result<List<ResumeScoreLog>> getScoresByScoreRange(
            @RequestParam Integer minScore, @RequestParam Integer maxScore) {
        return Result.success(resumeScoreLogService.selectByScoreRange(minScore, maxScore));
    }

    @PostMapping("/score")
    public Result<String> scoreResume(@RequestBody Map<String, Long> params) {
        Long jobId = params.get("jobId");
        Long deliveryId = params.get("deliveryId");
        if (jobId == null || deliveryId == null) {
            return Result.error("jobId和deliveryId不能为空");
        }
        boolean ok = resumeScoreLogService.scoreResume(jobId, deliveryId);
        return ok ? Result.success("简历评分成功") : Result.error("评分失败");
    }

    @PostMapping("/batch-score/{jobId}")
    public Result<Map<String, Integer>> batchScoreResumes(@PathVariable Long jobId) {
        int count = resumeScoreLogService.batchScoreResumes(jobId);
        Map<String, Integer> r = new java.util.HashMap<>();
        r.put("scoredCount", count);
        return Result.success("批量评分完成", r);
    }

    @PostMapping("/batch-score-by-company/{companyId}")
    public Result<Map<String, Integer>> batchScoreByCompany(@PathVariable Long companyId) {
        int count = resumeScoreLogService.batchScoreByCompany(companyId);
        Map<String, Integer> r = new java.util.HashMap<>();
        r.put("scoredCount", count);
        return Result.success("批量评分完成", r);
    }

    @PutMapping("/{scoreLogId}/rescore")
    public Result<String> rescoreResume(@PathVariable Long scoreLogId) {
        boolean ok = resumeScoreLogService.rescoreResume(scoreLogId);
        return ok ? Result.success("重新评分成功") : Result.error("重新评分失败");
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteScore(@PathVariable Long id) {
        boolean ok = resumeScoreLogService.removeById(id);
        return ok ? Result.success("评分记录删除成功") : Result.error("删除失败");
    }

    @GetMapping("/statistics/average-score/{jobId}")
    public Result<Map<String, Double>> getAverageScoreByJobId(@PathVariable Long jobId) {
        Double avg = resumeScoreLogService.calculateAverageScoreByJobId(jobId);
        Map<String, Double> r = new java.util.HashMap<>();
        r.put("averageScore", avg != null ? avg : 0.0);
        return Result.success(r);
    }

    @GetMapping("/statistics/score-distribution/{jobId}")
    public Result<Map<String, Integer>> getScoreDistribution(@PathVariable Long jobId) {
        return Result.success(resumeScoreLogService.calculateScoreDistribution(jobId));
    }

    @GetMapping("/statistics/match-distribution/{jobId}")
    public Result<Map<String, Integer>> getMatchDistribution(@PathVariable Long jobId) {
        return Result.success(resumeScoreLogService.calculateScoreDistribution(jobId));
    }

    @GetMapping("/top-score/{jobId}")
    public Result<ResumeScoreLog> getTopScoreByJobId(@PathVariable Long jobId) {
        ResumeScoreLog top = resumeScoreLogService.selectTopScoreByJobId(jobId);
        if (top == null) return Result.error(404, "该岗位暂无评分记录");
        return Result.success(top);
    }

    @GetMapping("/lowest-score/{jobId}")
    public Result<ResumeScoreLog> getLowestScoreByJobId(@PathVariable Long jobId) {
        ResumeScoreLog lowest = resumeScoreLogService.selectLowestScoreByJobId(jobId);
        if (lowest == null) return Result.error(404, "该岗位暂无评分记录");
        return Result.success(lowest);
    }

    @GetMapping("/dimensions/{jobId}")
    public Result<Map<String, Object>> getDimensionScores(@PathVariable Long jobId) {
        return Result.success(resumeScoreLogService.getDimensionScores(jobId));
    }
}
