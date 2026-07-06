package com.recruit.controller;

import com.recruit.entity.Job;
import com.recruit.service.JobService;
import com.recruit.utils.Result;
import com.recruit.dto.PageResult;
import com.recruit.annotation.LogOperation;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 岗位管理控制器
 * HR和教师可以访问
 */
@RestController
@RequestMapping("/jobs")
public class JobController extends BaseController {
    
    @Autowired
    private JobService jobService;
    
    /**
     * 获取岗位列表（按角色过滤，支持分页）
     */
    @GetMapping
    public Result<PageResult<Job>> getAllJobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Integer role = getCurrentRole();
        com.baomidou.mybatisplus.core.metadata.IPage<Job> pageResult;
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Job> pageReq = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        
        if (Objects.equals(role, 2)) {
            Long userId = getCurrentUserId();
            pageResult = jobService.lambdaQuery()
                    .eq(Job::getCreatedBy, userId)
                    .orderByDesc(Job::getCreateTime)
                    .page(pageReq);
        } else {
            pageResult = jobService.lambdaQuery()
                    .eq(Job::getStatus, 1)
                    .orderByDesc(Job::getCreateTime)
                    .page(pageReq);
        }
        return Result.success(PageResult.of(pageResult));
    }
    
    /**
     * 根据ID获取岗位
     * 
     * @param id 岗位ID
     * @return 岗位实体
     */
    @GetMapping("/{id}")
    public Result<Job> getJobById(@PathVariable Long id) {
        Job job = jobService.getById(id);
        if (job == null) {
            return Result.error(404, "岗位不存在");
        }
        
        // 增加浏览次数
        jobService.incrementViewCount(id);
        
        return Result.success(job);
    }
    
    /**
     * 根据企业ID查询岗位
     * 
     * @param companyId 企业ID
     * @return 岗位列表
     */
    @GetMapping("/by-company/{companyId}")
    public Result<List<Job>> getJobsByCompanyId(@PathVariable Long companyId) {
        List<Job> jobs = jobService.selectByCompanyId(companyId);
        return Result.success(jobs);
    }
    
    /**
     * 根据发布者ID查询岗位
     * 
     * @param createdBy 发布者ID（教师或HR）
     * @return 岗位列表
     */
    @GetMapping("/by-creator/{createdBy}")
    public Result<List<Job>> getJobsByCreatedBy(@PathVariable Long createdBy) {
        List<Job> jobs = jobService.selectByCreatedBy(createdBy);
        return Result.success(jobs);
    }
    
    /**
     * 教师端岗位管理专用：获取当前教师创建的所有岗位（所有状态）
     * 与通用 /jobs 接口不同，此接口：
     * 1. 不限制 status 过滤（草稿/已发布/已关闭全返回）
     * 2. 不返回分页包装，直接返回数组
     * 3. 仅限教师角色(role=1)访问
     */
    @GetMapping("/teacher")
    public Result<List<Job>> getTeacherJobs() {
        Integer role = getCurrentRole();
        if (!Objects.equals(role, 1)) {
            return Result.error(403, "仅教师可访问");
        }
        Long userId = getCurrentUserId();
        List<Job> jobs = jobService.lambdaQuery()
                .eq(Job::getCreatedBy, userId)
                .orderByDesc(Job::getCreateTime)
                .list();
        // 补充每个岗位的真实投递数量
        for (Job job : jobs) {
            job.setDeliveryCount(jobService.countDeliveries(job.getId()));
        }
        return Result.success(jobs);
    }

    /**
     * 查询有效岗位（已发布 + 未截止）
     * 
     * @return 有效岗位列表
     */
    @GetMapping("/active")
    public Result<List<Job>> getActiveJobs() {
        List<Job> jobs = jobService.selectActiveJobs();
        return Result.success(jobs);
    }
    
    /**
     * 根据状态查询岗位
     * 
     * @param status 状态（0=草稿，1=已发布，2=已关闭，3=暂停）
     * @return 岗位列表
     */
    @GetMapping("/by-status/{status}")
    public Result<List<Job>> getJobsByStatus(@PathVariable Integer status) {
        List<Job> jobs = jobService.selectByStatus(status);
        return Result.success(jobs);
    }
    
    /**
     * 创建岗位（草稿状态）
     * HR可创建（自动归属本企业），教师可创建（需关联合作企业）
     */
    @LogOperation("创建岗位")
    @PostMapping
    public Result<String> createJob(@RequestBody Job job) {
        Integer role = getCurrentRole();
        Long userId = getCurrentUserId();
        
        if (Objects.equals(role, 2)) {
            // HR：岗位归属HR本人
            job.setCreatedBy(userId);
            job.setStatus(0);
        } else if (Objects.equals(role, 1)) {
            // 教师创建的岗位必须关联企业
            requireTeacher();
            job.setCreatedBy(userId);
            if (job.getCompanyId() == null) {
                return Result.error("教师创建岗位必须关联企业");
            }
            job.setStatus(0);
        } else {
            return Result.error(403, "无权限创建岗位");
        }
        // 自动生成唯一追踪ID
        if (job.getTraceId() == null || job.getTraceId().isEmpty()) {
            job.setTraceId(java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        }
        jobService.save(job);
        return Result.success("岗位创建成功（草稿）");
    }
    
    /**
     * 更新岗位
     * 校验权限：HR只能编辑自己的岗位，教师不可编辑企业岗位
     */
    @LogOperation("更新岗位")
    @PutMapping("/{id}")
    public Result<String> updateJob(@PathVariable Long id, @RequestBody Job job) {
        Job existJob = jobService.getById(id);
        if (existJob == null) {
            return Result.error(404, "岗位不存在");
        }
        
        Integer role = getCurrentRole();
        Long userId = getCurrentUserId();
        
        if (Objects.equals(role, 2)) {
            // HR只能编辑自己的岗位
            if (!Objects.equals(existJob.getCreatedBy(), userId)) {
                return Result.error(403, "无权编辑其他HR创建的岗位");
            }
        } else if (Objects.equals(role, 1)) {
            // 教师不可编辑企业岗位内容（仅可标记问题或关闭）
            return Result.error(403, "教师不可编辑企业岗位内容");
        }
        
        job.setId(id);
        jobService.updateById(job);
        
        return Result.success("岗位更新成功");
    }
    
    /**
     * 发布岗位（草稿 -> 已发布）
     * 仅创建者可操作
     */
    @LogOperation("发布岗位")
    @PutMapping("/{id}/publish")
    public Result<String> publishJob(@PathVariable Long id) {
        checkJobOwnership(id);
        boolean success = jobService.publishJob(id);
        if (!success) {
            return Result.error("发布失败");
        }
        return Result.success("岗位发布成功");
    }
    
    /**
     * 关闭岗位
     * HR可关闭自己的岗位，教师可强制关闭任何岗位
     */
    @LogOperation("关闭岗位")
    @PutMapping("/{id}/close")
    public Result<String> closeJob(@PathVariable Long id) {
        Job job = jobService.getById(id);
        if (job == null) return Result.error(404, "岗位不存在");
        
        Integer role = getCurrentRole();
        Long userId = getCurrentUserId();
        
        // HR只能关闭自己的岗位（或者管理员创建的）
        if (Objects.equals(role, 2) && !Objects.equals(job.getCreatedBy(), userId) && !Objects.equals(job.getCreatedBy(), 1L)) {
            return Result.error(403, "无权关闭其他HR的岗位");
        }
        // 教师可强制关闭任何岗位（监管干预）
        if (Objects.equals(role, 1)) {
            requireTeacher();
        }
        
        boolean success = jobService.closeJob(id);
        if (!success) {
            return Result.error("关闭失败");
        }
        return Result.success("岗位已关闭");
    }
    
    /**
     * 暂停岗位
     * HR可暂停自己的，教师可强制暂停
     */
    @LogOperation("暂停岗位")
    @PutMapping("/{id}/pause")
    public Result<String> pauseJob(@PathVariable Long id) {
        Job job = jobService.getById(id);
        if (job == null) return Result.error(404, "岗位不存在");
        
        Integer role = getCurrentRole();
        Long userId = getCurrentUserId();
        
        if (Objects.equals(role, 2) && !Objects.equals(job.getCreatedBy(), userId)) {
            return Result.error(403, "无权暂停其他HR的岗位");
        }
        
        boolean success = jobService.pauseJob(id);
        if (!success) {
            return Result.error("暂停失败");
        }
        return Result.success("岗位已暂停");
    }
    
    /**
     * 删除岗位（软删除）
     * 仅HR可删除自己的草稿/已关闭岗位，教师无删除权限
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteJob(@PathVariable Long id) {
        Job job = jobService.getById(id);
        if (job == null) {
            return Result.error(404, "岗位不存在");
        }
        
        Integer role = getCurrentRole();
        Long userId = getCurrentUserId();
        
        // 教师无删除权限
        if (Objects.equals(role, 1)) {
            return Result.error(403, "教师无岗位删除权限");
        }
        
        // HR只能删除自己的岗位，且仅允许删除草稿或已关闭状态
        if (Objects.equals(role, 2)) {
            if (!Objects.equals(job.getCreatedBy(), userId)) {
                return Result.error(403, "无权删除其他HR的岗位");
            }
            if (job.getStatus() != 0 && job.getStatus() != 2) {
                return Result.error("仅允许删除草稿或已关闭的岗位");
            }
        }
        
        job.setDeleted(1);
        jobService.updateById(job);
        return Result.success("岗位删除成功");
    }
    
    /**
     * 校验岗位所属权（仅创建者可操作发布/编辑）
     */
    private void checkJobOwnership(Long jobId) {
        Job job = jobService.getById(jobId);
        if (job == null) {
            throw new com.recruit.exception.BusinessException(404, "岗位不存在");
        }
        Integer role = getCurrentRole();
        Long userId = getCurrentUserId();
        
        // 管理员可操作任何岗位
        if (Objects.equals(role, 3)) return;
        // 教师和HR可操作自己创建或管理员创建的岗位
        boolean isSelf = Objects.equals(job.getCreatedBy(), userId);
        boolean isAdminCreated = Objects.equals(job.getCreatedBy(), 1L);
        if ((Objects.equals(role, 1) || Objects.equals(role, 2)) && (isSelf || isAdminCreated)) return;
        
        throw new com.recruit.exception.BusinessException(403, "无权操作此岗位");
    }
    
    /**
     * 搜索岗位（根据标题模糊搜索）
     * 
     * @param keyword 关键词
     * @return 岗位列表
     */
    @GetMapping("/search")
    public Result<List<Job>> searchJobs(@RequestParam String keyword) {
        List<Job> jobs = jobService.lambdaQuery()
                .like(Job::getTitle, keyword)
                .list();
        
        return Result.success(jobs);
    }
    
    /**
     * 获取推荐岗位列表（学生端首页）
     * 取最近发布的已活跃岗位优先排序
     */
    @GetMapping("/recommend")
    public Result<List<Job>> getRecommendJobs() {
        List<Job> active = jobService.lambdaQuery()
                .eq(Job::getStatus, 1)
                .orderByDesc(Job::getCreateTime)
                .last("LIMIT 20")
                .list();
        return Result.success(active);
    }

    /**
     * 获取岗位统计信息（用于数据大屏）
     * 
     * @param id 岗位ID
     * @return 统计信息（浏览次数、投递数量等）
     */
    @GetMapping("/{id}/statistics")
    public Result<Map<String, Object>> getJobStatistics(@PathVariable Long id) {
        Job job = jobService.getById(id);
        if (job == null) {
            return Result.error(404, "岗位不存在");
        }
        
        Map<String, Object> statistics = new java.util.HashMap<>();
        statistics.put("viewCount", job.getViewCount());
        statistics.put("deliveryCount", jobService.countDeliveries(id));
        
        return Result.success(statistics);
    }
}
