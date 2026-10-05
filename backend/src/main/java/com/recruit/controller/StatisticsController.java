package com.recruit.controller;

import com.recruit.entity.Delivery;
import com.recruit.entity.AiParseLog;
import com.recruit.entity.Class;
import com.recruit.entity.Job;
import com.recruit.entity.OperationLog;
import com.recruit.entity.ResumeScoreLog;
import com.recruit.entity.StudentClass;
import com.recruit.entity.SysUser;
import com.recruit.service.*;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

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

    @Autowired
    private ResumeScoreLogService resumeScoreLogService;

    @Autowired(required = false)
    private AiParseLogService aiParseLogService;

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
                if (myClasses != null && !myClasses.isEmpty() && studentClassService != null) {
                    // 一次批量查询所有班级的学生-班级关联，替代逐班查询
                    List<Long> classIds = myClasses.stream().map(Class::getId).collect(Collectors.toList());
                    List<StudentClass> relations = studentClassService.lambdaQuery()
                            .in(StudentClass::getClassId, classIds)
                            .list();
                    for (StudentClass rel : relations) {
                        allStudentIds.add(rel.getStudentId());
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
                // 批量取学生与岗位名，替代循环内逐条查询
                List<Long> stuIds = recentDeliveries.stream()
                        .map(Delivery::getStudentId).filter(Objects::nonNull).distinct()
                        .collect(Collectors.toList());
                Map<Long, SysUser> stuMap = stuIds.isEmpty() ? Collections.emptyMap()
                        : userService.listByIds(stuIds).stream()
                                .collect(Collectors.toMap(SysUser::getId, u -> u));
                Map<Long, String> jobTitleMap = teacherJobs.stream()
                        .collect(Collectors.toMap(Job::getId, Job::getTitle));
                for (Delivery d : recentDeliveries) {
                    Map<String, Object> act = new HashMap<>();
                    SysUser stu = d.getStudentId() != null ? stuMap.get(d.getStudentId()) : null;
                    String studentName = stu != null && stu.getRealName() != null ? stu.getRealName() : "学生";
                    String jobTitle = jobTitleMap.getOrDefault(d.getJobId(), "");
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

        // 一次按状态分组统计，替代全量拉取投递到内存过滤计数
        // CAST 是为了绕开 MySQL 驱动把 TINYINT(1) 读成 Boolean 的问题
        List<Map<String, Object>> statusRows = deliveryService.getBaseMapper().selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Delivery>()
                        .select("CAST(status AS SIGNED) AS status, COUNT(*) AS cnt")
                        .in("job_id", jobIds)
                        .groupBy("status"));
        Map<Integer, Long> cntByStatus = new HashMap<>();
        for (Map<String, Object> row : statusRows) {
            if (row.get("status") == null) continue;
            cntByStatus.put(Integer.valueOf(row.get("status").toString()),
                    row.get("cnt") != null ? Long.parseLong(row.get("cnt").toString()) : 0L);
        }

        // 待处理简历（status=0）
        data.put("pendingResumeCount", cntByStatus.getOrDefault(0, 0L));

        // 保留原字段（兼容旧页面）
        data.put("resumeCount", deliveryService.lambdaQuery().in(Delivery::getJobId, jobIds).count());
        data.put("interviewCount", cntByStatus.getOrDefault(2, 0L) + cntByStatus.getOrDefault(3, 0L));
        data.put("hiredCount", cntByStatus.getOrDefault(3, 0L));

        // 今日新增投递 / 今日面试
        java.time.LocalDate today = java.time.LocalDate.now();
        data.put("todayNewCount", deliveryService.lambdaQuery()
                .in(Delivery::getJobId, jobIds)
                .apply("DATE(create_time) = {0}", today)
                .count());
        data.put("todayInterviewCount", deliveryService.lambdaQuery()
                .in(Delivery::getJobId, jobIds)
                .eq(Delivery::getStatus, 2)
                .apply("DATE(interview_time) = {0}", today)
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
        if (myClasses != null && !myClasses.isEmpty() && studentClassService != null) {
            // 一次批量查询所有班级的学生-班级关联，替代逐班查询
            List<Long> classIds = myClasses.stream().map(Class::getId).collect(Collectors.toList());
            List<StudentClass> relations = studentClassService.lambdaQuery()
                    .in(StudentClass::getClassId, classIds)
                    .list();
            for (StudentClass rel : relations) {
                myStudentIds.add(rel.getStudentId());
            }
        }
        List<Map<String, Object>> distribution = new ArrayList<>();
        Map<String, String> statusMap = new LinkedHashMap<>() {{
            put("已录用", "已录用");
            put("面试中", "面试中");
            put("待查看", "待查看");
            put("未录用", "未录用");
        }};
        // 一次按状态分组统计，替代逐状态 count
        // CAST 是为了绕开 MySQL 驱动把 TINYINT(1) 读成 Boolean 的问题
        Map<Integer, Long> cntByStatus = new HashMap<>();
        if (!myStudentIds.isEmpty()) {
            List<Map<String, Object>> statusRows = deliveryService.getBaseMapper().selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Delivery>()
                            .select("CAST(status AS SIGNED) AS status, COUNT(*) AS cnt")
                            .in("student_id", myStudentIds)
                            .groupBy("status"));
            for (Map<String, Object> row : statusRows) {
                if (row.get("status") == null) continue;
                cntByStatus.put(Integer.valueOf(row.get("status").toString()),
                        row.get("cnt") != null ? Long.parseLong(row.get("cnt").toString()) : 0L);
            }
        }
        for (Map.Entry<String, String> e : statusMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", e.getValue());
            long count = cntByStatus.getOrDefault(getStatusValue(e.getKey()), 0L);
            item.put("value", count);
            distribution.add(item);
        }
        return Result.success(distribution);
    }

    /**
     * HR 端评分分布
     */
    @GetMapping("/hr/score-distribution")
    public Result<List<Map<String, Object>>> getScoreDistribution(@RequestParam(required = false) Long userId) {
        // 确定统计范围：指定用户或当前 HR 所属企业
        Long targetUserId = (userId != null) ? userId : getCurrentUserId();
        SysUser targetUser = userService.getById(targetUserId);
        Long companyId = targetUser != null ? targetUser.getCompanyId() : null;

        List<ResumeScoreLog> scoreLogs = new ArrayList<>();
        if (companyId != null) {
            List<Job> companyJobs = jobService.lambdaQuery().eq(Job::getCompanyId, companyId).list();
            if (companyJobs != null && !companyJobs.isEmpty()) {
                List<Long> jobIds = companyJobs.stream().map(Job::getId).collect(java.util.stream.Collectors.toList());
                scoreLogs = resumeScoreLogService.lambdaQuery().in(ResumeScoreLog::getJobId, jobIds).list();
            }
        }

        // 按分数区间统计真实评分分布
        String[] ranges = {"0-59", "60-69", "70-79", "80-89", "90-100"};
        int[][] boundaries = {{0, 59}, {60, 69}, {70, 79}, {80, 89}, {90, 100}};
        long[] counts = new long[ranges.length];
        for (ResumeScoreLog log : scoreLogs) {
            Integer score = log.getScore();
            if (score == null) continue;
            for (int i = 0; i < boundaries.length; i++) {
                if (score >= boundaries[i][0] && score <= boundaries[i][1]) {
                    counts[i]++;
                    break;
                }
            }
        }

        List<Map<String, Object>> distribution = new ArrayList<>();
        for (int i = 0; i < ranges.length; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", ranges[i]);
            item.put("value", counts[i]);
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
        // 一次按岗位分组统计投递量，替代逐岗位 count
        Map<Long, Long> cntByJob = new HashMap<>();
        if (!jobs.isEmpty()) {
            List<Long> hotJobIds = jobs.stream().map(Job::getId).collect(Collectors.toList());
            List<Map<String, Object>> rows = deliveryService.getBaseMapper().selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Delivery>()
                            .select("job_id, COUNT(*) as cnt")
                            .in("job_id", hotJobIds)
                            .groupBy("job_id"));
            for (Map<String, Object> row : rows) {
                if (row.get("job_id") == null) continue;
                cntByJob.put(Long.valueOf(row.get("job_id").toString()),
                        row.get("cnt") != null ? Long.parseLong(row.get("cnt").toString()) : 0L);
            }
        }
        for (int i = 0; i < jobs.size() && i < 10; i++) {
            Map<String, Object> item = new HashMap<>();
            Job job = jobs.get(i);
            item.put("name", job.getTitle());
            item.put("count", cntByJob.getOrDefault(job.getId(), 0L));
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
                // 批量取用户，替代循环内重复 getById
                List<Long> uids = logs.stream()
                        .map(OperationLog::getUserId).filter(Objects::nonNull).distinct()
                        .collect(Collectors.toList());
                Map<Long, SysUser> userMap = uids.isEmpty() ? Collections.emptyMap()
                        : userService.listByIds(uids).stream()
                                .collect(Collectors.toMap(SysUser::getId, u -> u));
                for (OperationLog log : logs) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("time", log.getCreateTime() != null ? log.getCreateTime().toString().replace("T", " ") : "");
                    Long uid = log.getUserId();
                    SysUser u = uid != null ? userMap.get(uid) : null;
                    String userName = u != null
                            ? u.getRealName() + "(" + u.getUsername() + ")"
                            : (uid != null ? "用户#" + uid : "?");
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
            // 批量取学生，替代循环内重复 getById
            List<Long> stuIds = deliveries.stream()
                    .map(Delivery::getStudentId).filter(Objects::nonNull).distinct()
                    .collect(Collectors.toList());
            Map<Long, SysUser> stuMap = stuIds.isEmpty() ? Collections.emptyMap()
                    : userService.listByIds(stuIds).stream()
                            .collect(Collectors.toMap(SysUser::getId, u -> u));
            for (Delivery d : deliveries) {
                Map<String, Object> item = new HashMap<>();
                item.put("time", d.getCreateTime() != null ? d.getCreateTime().toString().replace("T", " ") : "");
                SysUser stu = d.getStudentId() != null ? stuMap.get(d.getStudentId()) : null;
                item.put("user", stu != null && stu.getRealName() != null ? stu.getRealName() : ("学生#" + d.getStudentId()));
                item.put("action", "投递简历（岗位ID: " + d.getJobId() + "）");
                item.put("status", d.getStatus() != null && d.getStatus() == 1 ? "已查看" : "待查看");
                activities.add(item);
            }
        }
        return Result.success(activities);
    }

    /**
     * AI 多模型对比统计（毕设对比实验数据看板）
     * 按 provider 汇总调用量/平均耗时/降级次数，按任务汇总调用量
     */
    @GetMapping("/ai/model-comparison")
    public Result<Map<String, Object>> getAiModelComparison() {
        requireAdmin();

        // CAST 绕开 MySQL 驱动把 TINYINT(1) 读成 Boolean 的问题
        List<Map<String, Object>> providerRows = aiParseLogService.getBaseMapper().selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AiParseLog>()
                        .select("provider, COUNT(*) AS calls, AVG(latency_ms) AS avgLatency, SUM(CAST(mock_flag AS SIGNED)) AS mockCount")
                        .groupBy("provider"));
        List<Map<String, Object>> providers = new ArrayList<>();
        for (Map<String, Object> row : providerRows) {
            Map<String, Object> p = new HashMap<>();
            p.put("provider", row.get("provider") != null ? row.get("provider").toString() : "unknown");
            p.put("calls", row.get("calls") != null ? Integer.parseInt(row.get("calls").toString()) : 0);
            p.put("avgLatency", row.get("avgLatency") != null ? (int) Math.round(Double.parseDouble(row.get("avgLatency").toString())) : 0);
            p.put("mockCount", row.get("mockCount") != null ? Integer.parseInt(row.get("mockCount").toString()) : 0);
            providers.add(p);
        }

        List<Map<String, Object>> taskRows = aiParseLogService.getBaseMapper().selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AiParseLog>()
                        .select("task_name, COUNT(*) AS calls")
                        .groupBy("task_name"));
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (Map<String, Object> row : taskRows) {
            if (row.get("task_name") == null) continue;
            Map<String, Object> t = new HashMap<>();
            t.put("task", row.get("task_name").toString());
            t.put("calls", row.get("calls") != null ? Integer.parseInt(row.get("calls").toString()) : 0);
            tasks.add(t);
        }

        long total = providers.stream().mapToLong(p -> ((Number) p.get("calls")).longValue()).sum();
        long mockTotal = providers.stream().mapToLong(p -> ((Number) p.get("mockCount")).longValue()).sum();

        Map<String, Object> data = new HashMap<>();
        data.put("providers", providers);
        data.put("tasks", tasks);
        data.put("total", total);
        data.put("mockCount", mockTotal);
        return Result.success(data);
    }

    /**
     * HR 区间统计（时间筛选）：只统计真实可查口径，不编造数据
     * 新增投递=投递时间在区间内；待处理=区间内投递且仍为待查看；面试安排=面试时间在区间内
     *
     * @param days 区间天数（7/30，0或不传=全部）
     */
    @GetMapping("/hr/range-stats")
    public Result<Map<String, Object>> getHrRangeStats(HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "0") Integer days) {
        // 与 /hr/dashboard 同口径：公司归属从登录态解析，不信任前端传参
        Long userId = (Long) request.getAttribute("userId");
        SysUser currentUser = userId != null ? userService.getById(userId) : null;
        Long companyId = currentUser != null ? currentUser.getCompanyId() : null;
        Map<String, Object> data = new HashMap<>();
        data.put("days", days);
        if (companyId == null) {
            data.put("newDeliveries", 0);
            data.put("pendingCount", 0);
            data.put("interviewCount", 0);
            return Result.success(data);
        }
        List<Job> jobs = jobService.lambdaQuery()
                .eq(Job::getCompanyId, companyId)
                .list();
        List<Long> jobIds = jobs.stream().map(Job::getId).collect(Collectors.toList());
        if (jobIds.isEmpty()) {
            data.put("newDeliveries", 0);
            data.put("pendingCount", 0);
            data.put("interviewCount", 0);
            return Result.success(data);
        }

        LocalDateTime start = days > 0 ? LocalDate.now().minusDays(days).atStartOfDay() : null;
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Delivery> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Delivery>()
                        .in("job_id", jobIds);
        if (start != null) {
            qw.apply("create_time >= {0}", start);
        }
        data.put("newDeliveries", deliveryService.count(qw));

        // 区间内投递且仍待查看（status=0）
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Delivery> qwPending =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Delivery>()
                        .in("job_id", jobIds)
                        .eq("status", 0);
        if (start != null) {
            qwPending.apply("create_time >= {0}", start);
        }
        data.put("pendingCount", deliveryService.count(qwPending));

        // 区间内安排的面试（面试时间落在区间，days=0 时不设上界）
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Delivery> qwInterview =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Delivery>()
                        .in("job_id", jobIds)
                        .ge("interview_time", start != null ? start : LocalDate.of(2000, 1, 1).atStartOfDay());
        if (start != null) {
            qwInterview.le("interview_time", LocalDateTime.now());
        }
        data.put("interviewCount", deliveryService.count(qwInterview));

        return Result.success(data);
    }

    /**
     * 教师重点关注学生：从真实数据中筛出需要干预的学生
     * 标签口径：未建简历 / 简历待完善（6项分组填充不足4项）/ 从未投递 / 超过两周未投递
     */
    @GetMapping("/teacher/attention-students")
    public Result<List<Map<String, Object>>> getAttentionStudents() {
        requireTeacher();
        Long teacherId = getCurrentUserId();
        List<Class> myClasses = classService != null ? classService.selectByTeacherId(teacherId) : new ArrayList<>();
        if (myClasses == null || myClasses.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 班级名映射 + 全部学生ID
        Map<Long, String> classNames = new HashMap<>();
        List<Long> allStudentIds = new ArrayList<>();
        for (Class cls : myClasses) {
            List<Long> ids = classService.getStudentIdsByClassId(cls.getId());
            if (ids == null) continue;
            for (Long sid : ids) {
                if (!classNames.containsKey(sid)) {
                    classNames.put(sid, cls.getName());
                    allStudentIds.add(sid);
                }
            }
        }
        if (allStudentIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 学生姓名
        Map<Long, SysUser> userMap = userService.listByIds(allStudentIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        // 简历（一次批量）
        List<com.recruit.entity.Resume> resumes = resumeService.lambdaQuery()
                .in(com.recruit.entity.Resume::getStudentId, allStudentIds)
                .list();
        Map<Long, com.recruit.entity.Resume> resumeMap = new HashMap<>();
        for (com.recruit.entity.Resume r : resumes) {
            resumeMap.put(r.getStudentId(), r);
        }

        // 投递：按学生聚合 数量 + 最近投递时间
        List<Map<String, Object>> deliveryRows = deliveryService.getBaseMapper().selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Delivery>()
                        .select("student_id, COUNT(*) AS cnt, MAX(create_time) AS last_time")
                        .in("student_id", allStudentIds)
                        .groupBy("student_id"));
        Map<Long, Long> deliveryCnt = new HashMap<>();
        Map<Long, LocalDateTime> lastDelivery = new HashMap<>();
        for (Map<String, Object> row : deliveryRows) {
            if (row.get("student_id") == null) continue;
            Long sid = Long.valueOf(row.get("student_id").toString());
            deliveryCnt.put(sid, row.get("cnt") != null ? Long.valueOf(row.get("cnt").toString()) : 0L);
            if (row.get("last_time") != null) {
                try {
                    lastDelivery.put(sid, LocalDateTime.parse(row.get("last_time").toString().replace(" ", "T")));
                } catch (Exception ignored) {}
            }
        }

        // 逐个学生判定关注标签
        LocalDateTime twoWeeksAgo = LocalDateTime.now().minusDays(14);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long sid : allStudentIds) {
            List<String> tags = new ArrayList<>();
            com.recruit.entity.Resume r = resumeMap.get(sid);
            int filled = 0;
            if (r == null) {
                tags.add("未建简历");
            } else {
                if (notBlank(r.getEducation())) filled++;
                if (notBlank(r.getInternship())) filled++;
                if (notBlank(r.getProject())) filled++;
                if (notBlank(r.getSkills())) filled++;
                if (notBlank(r.getSelfEvaluation())) filled++;
                if (notBlank(r.getJobTarget())) filled++;
                if (filled < 4) tags.add("简历待完善");
            }
            long cnt = deliveryCnt.getOrDefault(sid, 0L);
            if (cnt == 0) {
                tags.add("从未投递");
            } else {
                LocalDateTime last = lastDelivery.get(sid);
                if (last != null && last.isBefore(twoWeeksAgo)) {
                    tags.add("超过两周未投递");
                }
            }
            if (tags.isEmpty()) continue;

            SysUser u = userMap.get(sid);
            Map<String, Object> item = new HashMap<>();
            item.put("studentId", sid);
            item.put("studentName", u != null && u.getRealName() != null ? u.getRealName() : "学生" + sid);
            item.put("className", classNames.getOrDefault(sid, ""));
            item.put("deliveryCount", cnt);
            item.put("tags", tags);
            result.add(item);
        }
        // 标签多的排前面（更需要关注）
        result.sort((a, b) -> ((List<?>) b.get("tags")).size() - ((List<?>) a.get("tags")).size());
        return Result.success(result);
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty() && !s.trim().equals("[]");
    }

    private int getStatusValue(String name) {
        switch (name) {
            case "已录用": return 3;   // ACCEPTED
            case "面试中": return 2;   // INTERVIEW
            case "待查看": return 0;   // PENDING
            case "未录用": return 4;   // REJECTED
            default: return 0;
        }
    }

    private String getDayLabel(LocalDate date, int daysAgo) {
        if (daysAgo == 0) return "今天";
        if (daysAgo == 1) return "昨天";
        String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        return weekDays[date.getDayOfWeek().getValue() % 7];
    }
}
