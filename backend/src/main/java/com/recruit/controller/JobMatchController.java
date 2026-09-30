package com.recruit.controller;

import com.recruit.annotation.LogOperation;
import com.recruit.entity.JobMatchRecord;
import com.recruit.entity.SysUser;
import com.recruit.service.JobMatchRecordService;
import com.recruit.service.UserService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 人岗匹配记录控制器
 * 只有教师可以访问
 */
@RestController
@RequestMapping("/job-matches")
public class JobMatchController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(JobMatchController.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private JobMatchRecordService jobMatchRecordService;

    @Autowired
    private UserService userService;
    
    /**
     * 获取所有人岗匹配记录
     *
     * @return 匹配记录列表
     */
    @GetMapping
    public Result<List<JobMatchRecord>> getAllJobMatches() {
        List<JobMatchRecord> records = jobMatchRecordService.list();
        return Result.success(records);
    }

    /**
     * 根据ID获取人岗匹配记录
     *
     * @param id 记录ID
     * @return 匹配记录实体
     */
    @GetMapping("/{id}")
    public Result<JobMatchRecord> getJobMatchById(@PathVariable Long id) {
        JobMatchRecord record = jobMatchRecordService.getById(id);
        if (record == null) {
            return Result.error(404, "匹配记录不存在");
        }
        return Result.success(record);
    }

    /**
     * 根据岗位ID查询匹配记录(按匹配度降序)
     *
     * @param jobId 岗位ID
     * @return 匹配记录列表
     */
    @GetMapping("/by-job/{jobId}")
    public Result<List<Map<String, Object>>> getJobMatchesByJobId(@PathVariable Long jobId) {
        List<JobMatchRecord> records = jobMatchRecordService.selectByJobIdOrderByScore(jobId);
        return Result.success(enrichWithStudentNames(records));
    }

    @GetMapping("/by-student/{studentId}")
    public Result<List<Map<String, Object>>> getJobMatchesByStudentId(@PathVariable Long studentId) {
        List<JobMatchRecord> records = jobMatchRecordService.selectByStudentId(studentId);
        return Result.success(enrichWithStudentNames(records));
    }

    @GetMapping("/pushed/by-job/{jobId}")
    public Result<List<Map<String, Object>>> getPushedJobMatchesByJobId(@PathVariable Long jobId) {
        List<JobMatchRecord> records = jobMatchRecordService.selectPushedByJobId(jobId);
        return Result.success(enrichWithStudentNames(records));
    }

    @GetMapping("/clicked/by-job/{jobId}")
    public Result<List<Map<String, Object>>> getClickedJobMatchesByJobId(@PathVariable Long jobId) {
        List<JobMatchRecord> records = jobMatchRecordService.selectClickedByJobId(jobId);
        return Result.success(enrichWithStudentNames(records));
    }

    /**
     * 根据匹配度范围查询匹配记录
     *
     * @param minScore 最小匹配度
     * @param maxScore 最大匹配度
     * @return 匹配记录列表
     */
    @GetMapping("/by-score-range")
    public Result<List<JobMatchRecord>> getJobMatchesByScoreRange(
            @RequestParam BigDecimal minScore,
            @RequestParam BigDecimal maxScore) {
        List<JobMatchRecord> records = jobMatchRecordService.selectByScoreRange(minScore, maxScore);
        return Result.success(records);
    }

    /**
     * 生成人岗匹配记录(AI算法)
     *
     * @param params 包含jobId和studentId的参数
     * @return 生成结果
     */
    @LogOperation("生成人岗匹配")
    @PostMapping("/generate")
    public Result<String> generateMatchRecord(@RequestBody Map<String, Long> params) {
        requireTeacher();
        Long jobId = params.get("jobId");
        Long studentId = params.get("studentId");

        if (jobId == null || studentId == null) {
            return Result.error("jobId和studentId不能为空");
        }

        boolean success = jobMatchRecordService.generateMatchRecord(jobId, studentId);
        if (!success) {
            return Result.error("生成匹配记录失败");
        }

        return Result.success("匹配记录生成成功");
    }
    
    /**
     * 批量生成人岗匹配记录(对某个岗位,匹配所有学生)
     *
     * @param jobId 岗位ID
     * @return 生成的记录数量
     */
    @LogOperation("批量生成人岗匹配")
    @PostMapping("/batch-generate/{jobId}")
    public Result<Map<String, Integer>> batchGenerateMatchRecords(
            @PathVariable Long jobId,
            @RequestParam(required = false) Long classId) {
        requireTeacher();
        int count = jobMatchRecordService.batchGenerateMatchRecords(jobId, classId);

        Map<String, Integer> result = new java.util.HashMap<>();
        result.put("generatedCount", count);

        return Result.success("批量生成完成", result);
    }

    /**
     * 更新推送状态
     *
     * @param id 记录ID
     * @return 更新结果
     */
    @LogOperation("更新推送状态")
    @PutMapping("/{id}/push")
    public Result<String> updatePushedStatus(@PathVariable Long id) {
        requireTeacher();
        boolean success = jobMatchRecordService.updatePushedStatus(id);
        if (!success) {
            return Result.error("更新推送状态失败");
        }

        return Result.success("推送状态更新成功");
    }
    
    /**
     * 更新点击状态
     *
     * @param id 记录ID
     * @return 更新结果
     */
    @PutMapping("/{id}/click")
    public Result<String> updateClickedStatus(@PathVariable Long id) {
        boolean success = jobMatchRecordService.updateClickedStatus(id);
        if (!success) {
            return Result.error("更新点击状态失败");
        }

        return Result.success("点击状态更新成功");
    }
    
    /**
     * 删除人岗匹配记录
     *
     * @param id 记录ID
     * @return 删除结果
     */
    @LogOperation("删除匹配记录")
    @DeleteMapping("/{id}")
    public Result<String> deleteJobMatch(@PathVariable Long id) {
        requireTeacher();
        boolean success = jobMatchRecordService.removeById(id);
        if (!success) {
            return Result.error("删除失败");
        }

        return Result.success("匹配记录删除成功");
    }

    /**
     * 根据岗位ID删除所有匹配记录
     *
     * @param jobId 岗位ID
     * @return 删除结果
     */
    @LogOperation("清空岗位匹配记录")
    @DeleteMapping("/by-job/{jobId}")
    public Result<String> deleteJobMatchesByJobId(@PathVariable Long jobId) {
        requireTeacher();
        int count = jobMatchRecordService.deleteByJobId(jobId);
        return Result.success("已删除 " + count + " 条匹配记录");
    }
    
    /**
     * 统计岗位的推送率(推送数/总匹配数)
     *
     * @param jobId 岗位ID
     * @return 推送率(0-1)
     */
    @GetMapping("/statistics/push-rate/{jobId}")
    public Result<Map<String, BigDecimal>> calculatePushRate(@PathVariable Long jobId) {
        BigDecimal pushRate = jobMatchRecordService.calculatePushRate(jobId);

        Map<String, BigDecimal> result = new java.util.HashMap<>();
        result.put("pushRate", pushRate);

        return Result.success(result);
    }

    /**
     * 统计岗位的点击率(点击数/推送数)
     *
     * @param jobId 岗位ID
     * @return 点击率(0-1)
     */
    @GetMapping("/statistics/click-rate/{jobId}")
    public Result<Map<String, BigDecimal>> calculateClickRate(@PathVariable Long jobId) {
        BigDecimal clickRate = jobMatchRecordService.calculateClickRate(jobId);

        Map<String, BigDecimal> result = new java.util.HashMap<>();
        result.put("clickRate", clickRate);

        return Result.success(result);
    }

    /**
     * 统计岗位的平均匹配度
     *
     * @param jobId 岗位ID
     * @return 平均匹配度
     */
    @GetMapping("/statistics/average-match-score/{jobId}")
    public Result<Map<String, BigDecimal>> calculateAverageMatchScore(@PathVariable Long jobId) {
        BigDecimal averageMatchScore = jobMatchRecordService.calculateAverageMatchScore(jobId);

        Map<String, BigDecimal> result = new java.util.HashMap<>();
        result.put("averageMatchScore", averageMatchScore);

        return Result.success(result);
    }

    /**
     * 为匹配记录批量填充学生姓名和子维度分数
     */
    private List<Map<String, Object>> enrichWithStudentNames(List<JobMatchRecord> records) {
        if (records.isEmpty()) return new ArrayList<>();
        // 批量查询学生信息
        Set<Long> studentIds = records.stream().map(JobMatchRecord::getStudentId).collect(Collectors.toSet());
        Map<Long, SysUser> studentMap = userService.listByIds(new ArrayList<>(studentIds))
                .stream().collect(Collectors.toMap(SysUser::getId, s -> s));

        ObjectMapper objectMapper = OBJECT_MAPPER;

        return records.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("jobId", r.getJobId());
            map.put("studentId", r.getStudentId());
            map.put("matchScore", r.getMatchScore());
            map.put("matchReason", r.getMatchReason());
            map.put("isPushed", r.getIsPushed());
            map.put("pushTime", r.getPushTime());
            map.put("isClicked", r.getIsClicked());
            map.put("createTime", r.getCreateTime());
            // 填充子维度分数
            Map<String, Object> scoreDetailMap = new HashMap<>();
            if (r.getScoreDetail() != null && !r.getScoreDetail().isEmpty()) {
                try {
                    scoreDetailMap = objectMapper.readValue(r.getScoreDetail(),
                            new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    log.warn("解析 scoreDetail 失败: id={}", r.getId(), e);
                }
            }
            map.put("scoreDetail", scoreDetailMap);
            // 填充学生姓名
            SysUser student = studentMap.get(r.getStudentId());
            String studentName = "";
            String username = "";
            if (student != null) {
                studentName = student.getRealName() != null ? student.getRealName() : "";
                username = student.getUsername() != null ? student.getUsername() : "";
            }
            map.put("studentName", studentName);
            Map<String, Object> studentObj = new HashMap<>();
            studentObj.put("realName", studentName);
            studentObj.put("username", username);
            map.put("student", studentObj);
            // 兼容字段
            map.put("pushed", r.getIsPushed() != null && r.getIsPushed() == 1);
            map.put("viewed", false);
            map.put("className", "");
            map.put("resumeScore", "");
            return map;
        }).collect(Collectors.toList());
    }
}
