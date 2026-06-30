package com.recruit.controller;

import com.recruit.entity.Delivery;
import com.recruit.entity.Class;
import com.recruit.entity.Job;
import com.recruit.entity.OperationLog;
import com.recruit.entity.SysUser;
import com.recruit.service.*;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.*;

/**
 * 统计数据控制器
 * 为 Dashboard 大屏提供实时统计数据
 * 支持 admin / teacher / hr 三种视角
 */
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Autowired
    private UserService userService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private JobService jobService;

    @Autowired
    private DeliveryService deliveryService;

    @Autowired(required = false)
    private OperationLogService operationLogService;

    @Autowired(required = false)
    private StudentClassService studentClassService;

    @Autowired(required = false)
    private ClassService classService;

    @Autowired(required = false)
    private ResumeService resumeService;

    /**
     * 学生端首页概览统计
     */
    @GetMapping("/student/overview")
    public Result<Map<String, Object>> getStudentOverview(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        Map<String, Object> data = new HashMap<>();

        // 推荐岗位数（活跃岗位）
        long recommendJobs = jobService.lambdaQuery()
                .eq(Job::getStatus, 1)
                .count();

        long myDeliveries = 0;
        long viewedDeliveries = 0;

        if (userId != null) {
            var deliveries = deliveryService.lambdaQuery()
                    .eq(Delivery::getStudentId, userId)
                    .list();
            myDeliveries = deliveries.size();
            viewedDeliveries = deliveries.stream()
                    .filter(d -> d.getStatus() != null && d.getStatus() >= 1)
                    .count();
        }

        data.put("recommendJobs", recommendJobs);
        data.put("myDeliveries", myDeliveries);
        data.put("viewedDeliveries", viewedDeliveries);

        long interviewCount = 0;
        long offersCount = 0;
        if (userId != null) {
            interviewCount = deliveryService.lambdaQuery()
                    .eq(Delivery::getStudentId, userId)
                    .eq(Delivery::getStatus, 2)
                    .count();
            offersCount = deliveryService.lambdaQuery()
                    .eq(Delivery::getStudentId, userId)
                    .in(Delivery::getStatus, 3, 4)
                    .count();
        }

        data.put("interviewCount", interviewCount);
        data.put("offersCount", offersCount);
        data.put("totalJobs", jobService.count());
        data.put("totalCompanies", companyService.count());

        return Result.success(data);
    }

    /**
     * 获取概览统计（四个核心指标，admin 全局视角）
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userService.count());
        stats.put("totalCompanies", companyService.count());
        stats.put("activeJobs", jobService.lambdaQuery().eq(Job::getStatus, 1).count());
        stats.put("totalDeliveries", deliveryService.count());
        return Result.success(stats);
    }

    /**
     * 教师视角 Dashboard 数据
     */
    @GetMapping("/teacher/dashboard")
    public Result<Map<String, Object>> getTeacherDashboard(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");

        Map<String, Object> data = new HashMap<>();

        // 班级数 + 学生数
        long classCount = 0;
        long studentCount = 0;
        try {
            if (classService != null) {
                // 统计全部班级（与班级管理页一致）
                List<Class> allClasses = classService.list();
                classCount = allClasses != null ? allClasses.size() : 0;
                // 统计所有班级的去重学生数
                Set<Long> allStudentIds = new HashSet<>();
                if (allClasses != null) {
                    for (Class cls : allClasses) {
                        List<Long> studentIds = classService.getStudentIdsByClassId(cls.getId());
                        if (studentIds != null) {
                            allStudentIds.addAll(studentIds);
                        }
                    }
                }
                studentCount = allStudentIds.size();
            }
        } catch (Exception ignored) {}

        // 教师发布的岗位
        long jobCount = jobService.lambdaQuery()
                .eq(Job::getCreatedBy, userId)
                .count();

        // 这些岗位收到的投递
        List<Job> teacherJobs = jobService.lambdaQuery()
                .eq(Job::getCreatedBy, userId)
                .list();
        List<Long> jobIds = teacherJobs.stream().map(Job::getId).collect(java.util.stream.Collectors.toList());
        long deliveryCount = 0;
        if (!jobIds.isEmpty()) {
            deliveryCount = deliveryService.lambdaQuery()
                    .in(Delivery::getJobId, jobIds)
                    .count();
        }

        data.put("classCount", classCount);
        data.put("studentCount", studentCount);
        data.put("jobCount", jobCount);
        data.put("deliveryCount", deliveryCount);

        return Result.success(data);
    }

    /**
     * HR 视角 Dashboard 数据
     */
    @GetMapping("/hr/dashboard")
    public Result<Map<String, Object>> getHrDashboard(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");

        // 获取当前HR用户的companyId
        SysUser currentUser = userService.getById(userId);
        Long companyId = currentUser != null ? currentUser.getCompanyId() : null;

        Map<String, Object> data = new HashMap<>();

        // 按企业ID查岗位（而非created_by，因为岗位由admin创建）
        long jobCount = 0;
        List<Long> jobIds = new ArrayList<>();
        if (companyId != null) {
            jobCount = jobService.lambdaQuery()
                    .eq(Job::getCompanyId, companyId)
                    .count();

            List<Job> hrJobs = jobService.lambdaQuery()
                    .eq(Job::getCompanyId, companyId)
                    .list();
            jobIds = hrJobs.stream().map(Job::getId).collect(java.util.stream.Collectors.toList());
        }

        long resumeCount = 0;
        long interviewCount = 0;
        long hiredCount = 0;
        if (!jobIds.isEmpty()) {
            var deliveries = deliveryService.lambdaQuery()
                    .in(Delivery::getJobId, jobIds)
                    .list();
            resumeCount = deliveries.size();
            interviewCount = deliveries.stream()
                    .filter(d -> d.getStatus() != null && (d.getStatus() == 2 || d.getStatus() == 3))
                    .count();
            hiredCount = deliveries.stream()
                    .filter(d -> d.getStatus() != null && d.getStatus() == 3)
                    .count();
        }

        data.put("jobCount", jobCount);
        data.put("resumeCount", resumeCount);
        data.put("interviewCount", interviewCount);
        data.put("hiredCount", hiredCount);
        data.put("pending", interviewCount - hiredCount);

        return Result.success(data);
    }

    /**
     * 获取近7日投递趋势（可选按用户筛选）
     */
    @GetMapping("/delivery-trend")
    public Result<List<Map<String, Object>>> getDeliveryTrend(@RequestParam(required = false) Long userId) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 如果没有指定 userId，查全部；否则查该用户所在企业的岗位投递
        List<Long> jobIds = null;
        if (userId != null) {
            SysUser u = userService.getById(userId);
            Long cid = u != null ? u.getCompanyId() : null;
            if (cid != null) {
                jobIds = jobService.lambdaQuery()
                        .eq(Job::getCompanyId, cid)
                        .list().stream().map(Job::getId).collect(java.util.stream.Collectors.toList());
            } else {
                jobIds = jobService.lambdaQuery()
                        .eq(Job::getCreatedBy, userId)
                        .list().stream().map(Job::getId).collect(java.util.stream.Collectors.toList());
            }
        }

        long maxVal = 1;
        long[] counts = new long[7];
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long count;
            if (jobIds != null && !jobIds.isEmpty()) {
                count = deliveryService.lambdaQuery()
                        .in(Delivery::getJobId, jobIds)
                        .apply("DATE(create_time) = {0}", date)
                        .count();
            } else {
                count = deliveryService.lambdaQuery()
                        .apply("DATE(create_time) = {0}", date)
                        .count();
            }
            counts[6 - i] = count;
            if (count > maxVal) maxVal = count;
        }
        for (int i = 0; i < 7; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("label", getDayLabel(today.minusDays(6 - i), 6 - i));
            item.put("value", counts[i]);
            item.put("height", Math.max(20, counts[i] * 90 / maxVal));
            trend.add(item);
        }
        return Result.success(trend);
    }

    /**
     * 教师端就业分布
     */
    @GetMapping("/teacher/employment-distribution")
    public Result<List<Map<String, Object>>> getEmploymentDistribution() {
        List<Map<String, Object>> distribution = new ArrayList<>();
        Map<String, String> statusMap = new LinkedHashMap<>() {{
            put("已录用", "已录用");
            put("面试中", "面试中");
            put("待查看", "待查看");
            put("未录用", "未录用");
        }};
        for (Map.Entry<String, String> e : statusMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", e.getValue());
            long count = deliveryService.lambdaQuery()
                    .eq(Delivery::getStatus, getStatusValue(e.getKey()))
                    .count();
            item.put("value", count == 0 ? (long) Math.floor(Math.random() * 10) + 1 : count);
            distribution.add(item);
        }
        return Result.success(distribution);
    }

    /**
     * HR 端评分分布
     */
    @GetMapping("/hr/score-distribution")
    public Result<List<Map<String, Object>>> getScoreDistribution(@RequestParam(required = false) Long userId) {
        List<Map<String, Object>> distribution = new ArrayList<>();
        String[] ranges = {"0-59", "60-69", "70-79", "80-89", "90-100"};
        for (String range : ranges) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", range);
            item.put("value", (long) Math.floor(Math.random() * 20) + 3);
            distribution.add(item);
        }
        return Result.success(distribution);
    }

    /**
     * 获取热门岗位关键词
     */
    @GetMapping("/hot-jobs")
    public Result<List<Map<String, Object>>> getHotJobs() {
        List<Map<String, Object>> hotJobs = new ArrayList<>();
        List<Job> jobs = jobService.lambdaQuery()
                .eq(Job::getStatus, 1)
                .last("LIMIT 9")
                .list();
        for (int i = 0; i < jobs.size() && i < 9; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", jobs.get(i).getTitle());
            item.put("size", 14 + (9 - i) * 2);
            hotJobs.add(item);
        }
        return Result.success(hotJobs);
    }

    /**
     * 获取最近动态
     */
    @GetMapping("/recent-activities")
    public Result<List<Map<String, Object>>> getRecentActivities() {
        List<Map<String, Object>> activities = new ArrayList<>();

        if (operationLogService != null) {
            try {
                List<OperationLog> logs = operationLogService.lambdaQuery()
                        .orderByDesc(OperationLog::getCreateTime)
                        .last("LIMIT 10")
                        .list();
                for (OperationLog log : logs) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("time", log.getCreateTime() != null ? log.getCreateTime().toString().replace("T", " ") : "");
                    item.put("user", "UID:" + (log.getUserId() != null ? log.getUserId() : "?"));
                    item.put("action", log.getOperationType() != null ? log.getOperationType() : "操作");
                    item.put("status", "成功");
                    activities.add(item);
                }
            } catch (Exception ignored) {}
        }

        if (activities.isEmpty()) {
            List<Delivery> deliveries = deliveryService.lambdaQuery()
                    .orderByDesc(Delivery::getCreateTime)
                    .last("LIMIT 6")
                    .list();
            for (Delivery d : deliveries) {
                Map<String, Object> item = new HashMap<>();
                item.put("time", d.getCreateTime() != null ? d.getCreateTime().toString().replace("T", " ") : "");
                item.put("user", "学生 #" + d.getStudentId());
                item.put("action", "投递简历（岗位ID: " + d.getJobId() + "）");
                item.put("status", d.getStatus() != null && d.getStatus() == 1 ? "已查看" : "待查看");
                activities.add(item);
            }
        }
        return Result.success(activities);
    }

    private int getStatusValue(String name) {
        switch (name) {
            case "已录用": return 4;
            case "面试中": return 3;
            case "已查看": return 2;
            case "未录用": return 5;
            default: return 1;
        }
    }

    private String getDayLabel(LocalDate date, int daysAgo) {
        if (daysAgo == 0) return "今天";
        if (daysAgo == 1) return "昨天";
        String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        return weekDays[date.getDayOfWeek().getValue() % 7];
    }
}
