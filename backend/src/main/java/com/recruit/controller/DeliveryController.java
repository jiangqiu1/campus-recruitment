package com.recruit.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recruit.entity.Delivery;
import com.recruit.entity.Job;
import com.recruit.entity.Resume;
import com.recruit.entity.SysUser;
import com.recruit.service.DeliveryService;
import com.recruit.service.JobService;
import com.recruit.service.ResumeService;
import com.recruit.service.UserService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 投递记录管理控制器
 */
@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private JobService jobService;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private UserService userService;

    @GetMapping
    public Result<List<DeliveryVO>> getAllDeliveries() {
        List<Delivery> list = deliveryService.list();
        return Result.success(enrichDeliveries(list));
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

    @PostMapping("/deliver")
    public Result<String> deliverResume(@RequestParam Long studentId, @RequestBody Map<String, Object> params) {
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

    @PutMapping("/{id}/status")
    public Result<String> updateDeliveryStatus(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Object statusObj = params.get("status");
        if (statusObj == null) return Result.error("缺少status参数");
        Integer status;
        try {
            status = statusObj instanceof Number ? ((Number) statusObj).intValue() : Integer.valueOf(statusObj.toString());
        } catch (Exception e) {
            return Result.error("status参数格式错误");
        }
        String feedback = params.containsKey("feedback") ? params.get("feedback").toString() : null;
        boolean ok = deliveryService.updateDeliveryStatus(id, status, feedback);
        return ok ? Result.success("投递状态更新成功") : Result.error("更新失败");
    }

    @PutMapping("/{id}/arrange-interview")
    public Result<String> arrangeInterview(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Object timeObj = params.get("interviewTime");
        Object locObj = params.get("interviewLocation");
        if (timeObj == null || locObj == null) return Result.error("缺少面试时间或地点参数");
        LocalDateTime t;
        try {
            t = LocalDateTime.parse(timeObj.toString());
        } catch (Exception e) {
            return Result.error("面试时间格式错误");
        }
        String loc = locObj.toString();
        boolean ok = deliveryService.arrangeInterview(id, t, loc);
        return ok ? Result.success("面试安排成功") : Result.error("面试安排失败");
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteDelivery(@PathVariable Long id) {
        Delivery d = deliveryService.getById(id);
        if (d == null) return Result.error(404, "投递记录不存在");
        d.setDeleted(1);
        deliveryService.updateById(d);
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
        // 查询岗位名称
        if (d.getJobId() != null) {
            Job job = jobService.getById(d.getJobId());
            if (job != null) {
                vo.setJobTitle(job.getTitle());
            }
        }
        return vo;
    }

    private List<DeliveryVO> enrichDeliveries(List<Delivery> list) {
        return list.stream().map(this::enrichDelivery).collect(Collectors.toList());
    }

    @GetMapping("/statistics/by-job/{jobId}")
    public Result<Map<Integer, Integer>> getDeliveryStatistics(@PathVariable Long jobId) {
        return Result.success(deliveryService.countByJobIdAndGroupByStatus(jobId));
    }

    /**
     * 批量更新投递状态
     */
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
