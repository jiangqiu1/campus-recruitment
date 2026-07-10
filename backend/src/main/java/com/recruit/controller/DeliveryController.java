package com.recruit.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recruit.entity.Delivery;
import com.recruit.entity.Job;
import com.recruit.entity.Resume;
import com.recruit.entity.SysUser;
import com.recruit.entity.Class;
import com.recruit.service.ClassService;
import com.recruit.service.CompanyService;
import com.recruit.service.DeliveryService;
import com.recruit.service.JobService;
import com.recruit.service.ResumeService;
import com.recruit.service.ResumeScoreLogService;
import com.recruit.service.UserService;
import com.recruit.dto.DeliveryStatusUpdateRequest;
import com.recruit.dto.InterviewArrangeRequest;
import com.recruit.utils.Result;
import javax.servlet.http.HttpServletRequest;
import com.recruit.dto.PageResult;
import com.recruit.annotation.LogOperation;
import com.baomidou.mybatisplus.core.metadata.IPage;

import javax.validation.Valid;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 投递记录管理控制器
 */
@RestController
@RequestMapping("/deliveries")
public class DeliveryController extends BaseController {

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private JobService jobService;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private UserService userService;

    @Autowired
    private CompanyService companyService;
    
    @Autowired(required = false)
    private ClassService classService;

    @Autowired(required = false)
    private ResumeScoreLogService resumeScoreLogService;

    @GetMapping
    public Result<PageResult<DeliveryVO>> getAllDeliveries(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Delivery> list;
        Integer role = getCurrentRole();
        long total;
        
        if (Objects.equals(role, 1)) {
            Long teacherId = getCurrentUserId();
            List<Class> classes = classService != null ? classService.selectByTeacherId(teacherId) : new ArrayList<>();
            List<Long> studentIds = new ArrayList<>();
            for (Class clazz : classes) {
                List<Long> ids = classService != null ? classService.getStudentIdsByClassId(clazz.getId()) : new ArrayList<>();
                studentIds.addAll(ids);
            }
            if (studentIds.isEmpty()) {
                list = new ArrayList<>();
                total = 0;
            } else {
                com.baomidou.mybatisplus.core.metadata.IPage<Delivery> p = deliveryService.lambdaQuery()
                        .in(Delivery::getStudentId, studentIds)
                        .orderByDesc(Delivery::getCreateTime)
                        .page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size));
                list = p.getRecords();
                total = p.getTotal();
            }
        } else {
            com.baomidou.mybatisplus.core.metadata.IPage<Delivery> p = deliveryService.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size));
            list = p.getRecords();
            total = p.getTotal();
        }
        List<DeliveryVO> voList = enrichDeliveries(list);
        return Result.success(PageResult.of(voList, total, page, size));
    }

    @GetMapping("/{id}")
    public Result<DeliveryVO> getDeliveryById(@PathVariable Long id) {
        Delivery d = deliveryService.getById(id);
        if (d == null) return Result.error(404, "投递记录不存在");
        return Result.success(enrichDelivery(d));
    }

    @GetMapping("/by-student/{studentId}")
    public Result<List<DeliveryVO>> getDeliveriesByStudentId(@PathVariable Long studentId) {
        List<Delivery> list = deliveryService.selectByStudentId(studentId);
        return Result.success(enrichDeliveries(list));
    }

    @GetMapping("/by-job/{jobId}")
    public Result<List<DeliveryVO>> getDeliveriesByJobId(@PathVariable Long jobId) {
        List<Delivery> list = deliveryService.selectByJobId(jobId);
        return Result.success(enrichDeliveries(list));
    }

    @GetMapping("/by-company/{companyId}")
    public Result<List<DeliveryVO>> getDeliveriesByCompanyId(@PathVariable Long companyId) {
        List<Job> jobs = jobService.lambdaQuery()
                .eq(Job::getCompanyId, companyId)
                .list();
        if (jobs.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        List<Long> jobIds = jobs.stream().map(Job::getId).collect(Collectors.toList());
        List<Delivery> list = deliveryService.lambdaQuery()
                .in(Delivery::getJobId, jobIds)
                .orderByDesc(Delivery::getCreateTime)
                .list();
        return Result.success(enrichDeliveries(list));
    }

    @GetMapping("/by-status/{status}")
    public Result<List<DeliveryVO>> getDeliveriesByStatus(@PathVariable Integer status) {
        List<Delivery> list = deliveryService.selectByStatus(status);
        return Result.success(enrichDeliveries(list));
    }

    @GetMapping("/by-student-and-status")
    public Result<List<DeliveryVO>> getByStudentAndStatus(@RequestParam Long studentId, @RequestParam Integer status) {
        List<Delivery> list = deliveryService.selectByStudentIdAndStatus(studentId, status);
        return Result.success(enrichDeliveries(list));
    }

    @GetMapping("/by-job-and-status")
    public Result<List<DeliveryVO>> getByJobAndStatus(@RequestParam Long jobId, @RequestParam Integer status) {
        List<Delivery> list = deliveryService.selectByJobIdAndStatus(jobId, status);
        return Result.success(enrichDeliveries(list));
    }

    @LogOperation("投递简历")
    @PostMapping("/deliver")
    public Result<String> deliverResume(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        Long studentId = (Long) request.getAttribute("userId");
        if (studentId == null) return Result.error("无法获取用户信息");
        Object jobIdObj = params.get("jobId");
        Object rvObj = params.get("resumeVersion");
        if (jobIdObj == null) return Result.error("缺少jobId参数");
        Long jobId;
        try {
            jobId = jobIdObj instanceof Number ? ((Number) jobIdObj).longValue() : Long.valueOf(jobIdObj.toString());
        } catch (Exception e) {
            return Result.error("jobId参数格式错误");
        }
        String resumeVersion = rvObj == null ? "latest" : rvObj.toString();
        boolean ok = deliveryService.deliverResume(studentId, jobId, resumeVersion);
        return ok ? Result.success("简历投递成功") : Result.error("投递失败");
    }

    @LogOperation("更新投递状态")
    @PutMapping("/{id}/status")
    public Result<String> updateDeliveryStatus(@PathVariable Long id, @Valid @RequestBody DeliveryStatusUpdateRequest request) {
        boolean ok = deliveryService.updateDeliveryStatus(id, request.getStatus(), request.getFeedback());
        return ok ? Result.success("投递状态更新成功") : Result.error("更新失败");
    }

    @LogOperation("安排面试")
    @PutMapping("/{id}/arrange-interview")
    public Result<String> arrangeInterview(@PathVariable Long id, @Valid @RequestBody InterviewArrangeRequest request) {
        boolean ok = deliveryService.arrangeInterview(id, request.getInterviewTime(), request.getInterviewLocation());
        return ok ? Result.success("面试安排成功") : Result.error("面试安排失败");
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteDelivery(@PathVariable Long id) {
        boolean ok = deliveryService.removeById(id);
        if (!ok) return Result.error(404, "投递记录不存在或已取消");
        return Result.success("投递记录删除成功");
    }

    /**
     * 投递记录视图对象（含学生姓名和岗位名称）
     */
    public static class DeliveryVO {
        private Long id;
        private Long studentId;
        private Long jobId;
        private String resumeVersion;
        private Integer status;
        private LocalDateTime interviewTime;
        private String interviewLocation;
        private String feedback;
        private LocalDateTime createTime;
        // 扩展字段
        private String studentName;
        private String jobTitle;
        private String companyName;
        private Integer score;
        private String salaryText;
        private String location;

        public DeliveryVO(Delivery d) {
            this.id = d.getId();
            this.studentId = d.getStudentId();
            this.jobId = d.getJobId();
            this.resumeVersion = d.getResumeVersion();
            this.status = d.getStatus();
            this.interviewTime = d.getInterviewTime();
            this.interviewLocation = d.getInterviewLocation();
            this.feedback = d.getFeedback();
            this.createTime = d.getCreateTime();
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        public Long getJobId() { return jobId; }
        public void setJobId(Long jobId) { this.jobId = jobId; }
        public String getResumeVersion() { return resumeVersion; }
        public void setResumeVersion(String resumeVersion) { this.resumeVersion = resumeVersion; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public LocalDateTime getInterviewTime() { return interviewTime; }
        public void setInterviewTime(LocalDateTime interviewTime) { this.interviewTime = interviewTime; }
        public String getInterviewLocation() { return interviewLocation; }
        public void setInterviewLocation(String interviewLocation) { this.interviewLocation = interviewLocation; }
        public String getFeedback() { return feedback; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        public String getJobTitle() { return jobTitle; }
        public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        public String getSalaryText() { return salaryText; }
        public void setSalaryText(String salaryText) { this.salaryText = salaryText; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }

    private DeliveryVO enrichDelivery(Delivery d) {
        DeliveryVO vo = new DeliveryVO(d);
        // 查询学生姓名
        if (d.getStudentId() != null) {
            SysUser student = userService.getById(d.getStudentId());
            if (student != null) {
                vo.setStudentName(student.getRealName() != null ? student.getRealName() : student.getUsername());
            }
        }
        // 查询岗位名称、企业名称
        if (d.getJobId() != null) {
            Job job = jobService.getById(d.getJobId());
            if (job != null) {
                vo.setJobTitle(job.getTitle());
                vo.setSalaryText(job.getSalaryRange());
                vo.setLocation(job.getLocation());
                com.recruit.entity.Company company = companyService.getById(job.getCompanyId());
                if (company != null) {
                    vo.setCompanyName(company.getName());
                }
            }
        }
        // 查询 AI 评分
        if (resumeScoreLogService != null) {
            com.recruit.entity.ResumeScoreLog scoreLog = resumeScoreLogService.lambdaQuery()
                    .eq(com.recruit.entity.ResumeScoreLog::getDeliveryId, d.getId())
                    .one();
            if (scoreLog != null) vo.setScore(scoreLog.getScore());
        }
        return vo;
    }

    /**
     * 批量组装投递VO（批量查询代替逐条查询，消除 N+1）
     * 原先每条投递记录独立查学生、岗位、企业，100条=301次查询
     * 优化后批量查3次，共4次查询
     */
    private List<DeliveryVO> enrichDeliveries(List<Delivery> list) {
        if (list.isEmpty()) return new ArrayList<>();

        // 1. 批量收集所有学生ID、岗位ID
        Set<Long> studentIds = new HashSet<>();
        Set<Long> jobIds = new HashSet<>();
        for (Delivery d : list) {
            if (d.getStudentId() != null) studentIds.add(d.getStudentId());
            if (d.getJobId() != null) jobIds.add(d.getJobId());
        }

        // 2. 批量查询学生信息
        Map<Long, SysUser> studentMap = new HashMap<>();
        if (!studentIds.isEmpty()) {
            List<SysUser> students = userService.listByIds(new ArrayList<>(studentIds));
            for (SysUser s : students) {
                studentMap.put(s.getId(), s);
            }
        }

        // 2.5 过滤：只保留学生角色（role=0）的投递记录，排除教师/HR等异常数据
        Set<Long> validStudentIds = studentMap.entrySet().stream()
                .filter(e -> Objects.equals(e.getValue().getRole(), 0))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        list = list.stream()
                .filter(d -> d.getStudentId() != null && validStudentIds.contains(d.getStudentId()))
                .collect(Collectors.toList());
        if (list.isEmpty()) return new ArrayList<>();
        // 重新收集 studentIds（已过滤）
        studentIds = list.stream().map(Delivery::getStudentId).collect(Collectors.toSet());

        // 3. 批量查询岗位信息
        Map<Long, Job> jobMap = new HashMap<>();
        Map<Long, Long> jobCompanyMap = new HashMap<>(); // jobId -> companyId
        if (!jobIds.isEmpty()) {
            List<Job> jobs = jobService.listByIds(new ArrayList<>(jobIds));
            for (Job j : jobs) {
                jobMap.put(j.getId(), j);
                if (j.getCompanyId() != null) jobCompanyMap.put(j.getId(), j.getCompanyId());
            }
        }

        // 4. 批量查询企业信息
        Map<Long, com.recruit.entity.Company> companyMap = new HashMap<>();
        Set<Long> companyIds = new HashSet<>(jobCompanyMap.values());
        if (!companyIds.isEmpty()) {
            List<com.recruit.entity.Company> companies = companyService.listByIds(new ArrayList<>(companyIds));
            for (com.recruit.entity.Company c : companies) {
                companyMap.put(c.getId(), c);
            }
        }

        // 4.5 批量查询 AI 评分
        Map<Long, Integer> scoreMap = new HashMap<>();
        if (resumeScoreLogService != null && !list.isEmpty()) {
            Set<Long> deliveryIds = list.stream().map(Delivery::getId).collect(Collectors.toSet());
            List<com.recruit.entity.ResumeScoreLog> scoreLogs = resumeScoreLogService.lambdaQuery()
                    .in(com.recruit.entity.ResumeScoreLog::getDeliveryId, deliveryIds)
                    .list();
            for (com.recruit.entity.ResumeScoreLog sl : scoreLogs) {
                if (sl.getScore() != null) scoreMap.put(sl.getDeliveryId(), sl.getScore());
            }
        }

        // 5. 组装结果
        return list.stream().map(d -> {
            DeliveryVO vo = new DeliveryVO(d);
            SysUser student = studentMap.get(d.getStudentId());
            if (student != null) {
                vo.setStudentName(student.getRealName() != null ? student.getRealName() : student.getUsername());
            }
            Job job = jobMap.get(d.getJobId());
            if (job != null) {
                vo.setJobTitle(job.getTitle());
                vo.setSalaryText(job.getSalaryRange());
                vo.setLocation(job.getLocation());
                com.recruit.entity.Company company = companyMap.get(job.getCompanyId());
                if (company != null) {
                    vo.setCompanyName(company.getName());
                }
            }
            vo.setScore(scoreMap.get(d.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    @GetMapping("/statistics/by-job/{jobId}")
    public Result<Map<Integer, Integer>> getDeliveryStatistics(@PathVariable Long jobId) {
        return Result.success(deliveryService.countByJobIdAndGroupByStatus(jobId));
    }

    /**
     * 批量更新投递状态
     */
    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/batch-status")
    public Result<String> batchUpdateStatus(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) params.get("ids");
        Integer status = (Integer) params.get("status");
        if (ids == null || ids.isEmpty() || status == null) {
            return Result.error("参数错误");
        }
        for (Integer id : ids) {
            Delivery d = deliveryService.getById(id);
            if (d != null) {
                d.setStatus(status);
                deliveryService.updateById(d);
            }
        }
        return Result.success("批量更新成功");
    }
}
