package com.recruit.controller;

import com.recruit.entity.ResumeScoreLog;
import com.recruit.service.ResumeScoreLogService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/resume-scores")
public class ResumeScoreController {

    @Autowired
    private ResumeScoreLogService resumeScoreLogService;

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
    public Result<List<ResumeScoreLog>> getScoresByJobId(@PathVariable Long jobId) {
        return Result.success(resumeScoreLogService.selectByJobIdOrderByScore(jobId));
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
}
