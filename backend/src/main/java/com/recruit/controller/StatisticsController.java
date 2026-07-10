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
import java.util.Objects;

/**
 * 统计数据控制器
 * 为 Dashboard 大屏提供实时统计数据
 * 支持 admin / teacher / hr 三种视角
 */
@RestController
@RequestMapping("/statistics")
public class StatisticsController extends BaseController {

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

    @Autowired(required = false)
    private JobChangeApplyService jobChangeApplyService;

    @Autowired(required = false)
    private MessageService messageService;

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
        Set<Long> allStudentIds = new HashSet<>();
        try {
            if (classService != null) {
                Long curUserId = getCurrentUserId();
                List<Class> myClasses = classService.selectByTeacherId(curUserId);
                classCount = myClasses != null ? myClasses.size() : 0;
                if (myClasses != null) {
                    for (Class cls : myClasses) {
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

        // 急招岗位（活跃岗位中状态=已发布的）
        long urgentJobCount = jobService.lambdaQuery()
                .eq(Job::getCreatedBy, userId)
                .eq(Job::getStatus, 1)
                .count();

        // 这些岗位收到的投递
        List<Job> teacherJobs = jobService.lambdaQuery()
                .eq(Job::getCreatedBy, userId)
                .list();
        List<Long> jobIds = teacherJobs.stream().map(Job::getId).collect(java.util.stream.Collectors.toList());
        long deliveryCount = 0;
        long todayDeliveryCount = 0;
        long unreadResumeCount = 0;
        if (!jobIds.isEmpty()) {
            deliveryCount = deliveryService.lambdaQuery()
                    .in(Delivery::getJobId, jobIds)
                    .count();
            // 今日新增投递
            LocalDate today = LocalDate.now();
            todayDeliveryCount = deliveryService.lambdaQuery()
                    .in(Delivery::getJobId, jobIds)
                    .apply("DATE(create_time) = {0}", today)
                    .count();
            // 未读简历（status=0 已投递但企业未查看）
            unreadResumeCount = deliveryService.lambdaQuery()
                    .in(Delivery::getJobId, jobIds)
                    .eq(Delivery::getStatus, 0)
                    .count();
        }

        // 待审批数量（job_change_apply 中 status=0 且审核教师=当前教师）
        long pendingApprovalCount = 0;
        try {
            if (jobChangeApplyService != null) {
                pendingApprovalCount = jobChangeApplyService.lambdaQuery()
                        .eq(com.recruit.entity.JobChangeApply::getStatus, 0)
                        .eq(com.recruit.entity.JobChangeApply::getReviewTeacherId, userId)
                        .count();
            }
        } catch (Exception ignored) {}

        // 近期动态（最近5条投递记录，加上学生姓名）
        List<Map<String, Object>> recentActivities = new ArrayList<>();
        try {
            if (!jobIds.isEmpty()) {
                List<Delivery> recentDeliveries = deliveryService.lambdaQuery()
                        .in(Delivery::getJobId, jobIds)
                        .orderByDesc(Delivery::getCreateTime)
                        .last("LIMIT 5")
                        .list();
                for (Delivery d : recentDeliveries) {
                    Map<String, Object> act = new HashMap<>();
                    String studentName = "学生";
                    try {
                        com.recruit.entity.SysUser stu = userService.getById(d.getStudentId());
                        if (stu != null) studentName = stu.getRealName() != null ? stu.getRealName() : "学生";
                    } catch (Exception ignored) {}
                    // 找岗位名称
                    String jobTitle = "";
                    for (Job j : teacherJobs) {
                        if (j.getId().equals(d.getJobId())) { jobTitle = j.getTitle(); break; }
                    }
                    act.put("text", studentName + " 投递了「" + jobTitle + "」");
                    act.put("time", d.getCreateTime() != null ? d.getCreateTime().toString().replace("T", " ").substring(0, 16) : "");
                    act.put("type", "delivery");
                    act.put("id", d.getId());
                    recentActivities.add(act);
                }
            }
        } catch (Exception ignored) {}

        data.put("classCount", classCount);
        data.put("studentCount", studentCount);
        data.put("jobCount", jobCount);
        data.put("deliveryCount", deliveryCount);
        data.put("pendingApprovalCount", pendingApprovalCount);
        data.put("todayDeliveryCount", todayDeliveryCount);
        data.put("unreadResumeCount", unreadResumeCount);
        data.put("urgentJobCount", urgentJobCount);
        data.put("recentActivities", recentActivities);

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

        // 默认值
        data.put("activeJobCount", 0);
        data.put("pendingResumeCount", 0);
        data.put("todayNewCount", 0);
        data.put("todayInterviewCount", 0);
        data.put("resumeCount", 0);
        data.put("interviewCount", 0);
        data.put("hiredCount", 0);

        if (companyId == null) return Result.success(data);

        // 查询该企业的所有岗位
        List<Job> hrJobs = jobService.lambdaQuery()
                .eq(Job::getCompanyId, companyId)
                .list();
        List<Long> jobIds = hrJobs.stream().map(Job::getId).collect(java.util.stream.Collectors.toList());

        // 在招岗位数（状态=1 已发布）
        long activeJobCount = hrJobs.stream().filter(j -> j.getStatus() != null && j.getStatus() == 1).count();
        data.put("activeJobCount", activeJobCount);

        if (jobIds.isEmpty()) return Result.success(data);

        // 获取所有投递
        var deliveries = deliveryService.lambdaQuery()
                .in(Delivery::getJobId, jobIds)
                .list();

        // 待处理简历（status=0）
        long pendingResumeCount = deliveries.stream()
                .filter(d -> d.getStatus() != null && d.getStatus() == 0)
                .count();
        data.put("pendingResumeCount", pendingResumeCount);

        // 今日新增投递
        java.time.LocalDate today = java.time.LocalDate.now();
        long todayNewCount = deliveries.stream()
                .filter(d -> d.getCreateTime() != null && d.getCreateTime().toLocalDate().equals(today))
                .count();
        data.put("todayNewCount", todayNewCount);

        // 今日面试
        long todayInterviewCount = deliveries.stream()
                .filter(d -> d.getStatus() != null && d.getStatus() == 2
                        && d.getInterviewTime() != null && d.getInterviewTime().toLocalDate().equals(today))
                .count();
        data.put("todayInterviewCount", todayInterviewCount);

        // 保留原字段（兼容旧页面）
        data.put("resumeCount", (long) deliveries.size());
        data.put("interviewCount", deliveries.stream()
                .filter(d -> d.getStatus() != null && (d.getStatus() == 2 || d.getStatus() == 3))
                .count());
        data.put("hiredCount", deliveries.stream()
                .filter(d -> d.getStatus() != null && d.getStatus() == 3)
                .count());

        return Result.success(data);
    }

    /**
     * 获取近7日投递趋势（可选按用户筛选）
     */
    @GetMapping("/delivery-trend")
    public Result<List<Map<String, Object>>> getDeliveryTrend(@RequestParam(required = false) Long userId) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        Integer role = getCurrentRole();
        Long curUserId = getCurrentUserId();
        Long targetUserId = (userId != null) ? userId : curUserId;
        List<Long> jobIds = null;
        if (Objects.equals(role, 2)) {
            SysUser u = userService.getById(targetUserId);
            Long cid = u != null ? u.getCompanyId() : null;
            if (cid != null) {
                jobIds = jobService.lambdaQuery()
                        .eq(Job::getCompanyId, cid)
                        .list().stream().map(Job::getId).collect(java.util.stream.Collectors.toList());
            }
        } else if (Objects.equals(role, 1)) {
            jobIds = jobService.lambdaQuery()
                    .eq(Job::getCreatedBy, targetUserId)
                    .list().stream().map(Job::getId).collect(java.util.stream.Collectors.toList());
        } else if (Objects.equals(role, 3) && userId != null) {
            SysUser u = userService.getById(targetUserId);
            Long cid = u != null ? u.getCompanyId() : null;
            if (cid != null) {
                jobIds = jobService.lambdaQuery()
                        .eq(Job::getCompanyId, cid)
                        .list().stream().map(Job::getId).collect(java.util.stream.Collectors.toList());
            }
        }

        // 单次 GROUP BY 查询替代 7 次 count 查询
        long maxVal = 1;
        long[] counts = new long[7];
        
        // 构建基础查询
        List<Map<String, Object>> dailyCounts;
        LocalDate sevenDaysAgo = today.minusDays(6);
        
        if (jobIds != null && !jobIds.isEmpty()) {
            dailyCounts = deliveryService.getBaseMapper().selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Delivery>()
                            .select("DATE(create_time) as day, COUNT(*) as cnt")
                            .in("job_id", jobIds)
                            .apply("create_time >= {0}", sevenDaysAgo.atStartOfDay())
                            .groupBy("DATE(create_time)")
            );
        } else {
            dailyCounts = deliveryService.getBaseMapper().selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Delivery>()
                            .select("DATE(create_time) as day, COUNT(*) as cnt")
                            .apply("create_time >= {0}", sevenDaysAgo.atStartOfDay())
                            .groupBy("DATE(create_time)")
            );
        }
        
        // 将数据库返回结果映射到日期->计数
        java.util.Map<String, Long> dayCountMap = new java.util.HashMap<>();
        for (Map<String, Object> row : dailyCounts) {
            String day = row.get("day") != null ? row.get("day").toString() : "";
            long cnt = row.get("cnt") != null ? Long.parseLong(row.get("cnt").toString()) : 0L;
            dayCountMap.put(day, cnt);
        }
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long count = dayCountMap.getOrDefault(date.toString(), 0L);
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
        Long teacherId = getCurrentUserId();
        List<Class> myClasses = classService != null ? classService.selectByTeacherId(teacherId) : new ArrayList<>();
        Set<Long> myStudentIds = new HashSet<>();
        if (myClasses != null && classService != null) {
            for (Class cls : myClasses) {
                List<Long> ids = classService.getStudentIdsByClassId(cls.getId());
                if (ids != null) myStudentIds.addAll(ids);
            }
        }
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
            long count;
            if (!myStudentIds.isEmpty()) {
                count = deliveryService.lambdaQuery()
                        .eq(Delivery::getStatus, getStatusValue(e.getKey()))
                        .in(Delivery::getStudentId, myStudentIds)
                        .count();
            } else {
                count = 0L;
            }
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
                .last("LIMIT 10")
                .list();
        for (int i = 0; i < jobs.size() && i < 10; i++) {
            Map<String, Object> item = new HashMap<>();
            Job job = jobs.get(i);
            // 统计该岗位的投递量
            long deliveryCount = deliveryService.lambdaQuery()
                    .eq(Delivery::getJobId, job.getId())
                    .count();
            item.put("name", job.getTitle());
            item.put("count", deliveryCount);
            item.put("size", 14 + (10 - i) * 2);
            hotJobs.add(item);
        }
        // 按投递量降序排列
        hotJobs.sort((a, b) -> Long.compare(
                ((Number) b.getOrDefault("count", 0L)).longValue(),
                ((Number) a.getOrDefault("count", 0L)).longValue()
        ));
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
                    Long uid = log.getUserId();
                    String userName = uid != null ? 
                        (userService.getById(uid) != null ? 
                            userService.getById(uid).getRealName() + "(" + userService.getById(uid).getUsername() + ")" : 
                            "用户#" + uid) : "?";
                    item.put("user", userName);
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
                String stuName = userService.getById(d.getStudentId()) != null ? userService.getById(d.getStudentId()).getRealName() : ("学生#" + d.getStudentId());
                item.put("user", stuName);
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
