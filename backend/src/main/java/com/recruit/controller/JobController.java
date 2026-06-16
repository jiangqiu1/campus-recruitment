package com.recruit.controller;

import com.recruit.entity.Job;
import com.recruit.service.JobService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 岗位管理控制器
 * HR和教师可以访问
 */
@RestController
@RequestMapping("/jobs")
public class JobController {
    
    @Autowired
    private JobService jobService;
    
    /**
     * 获取所有岗位列表
     * 
     * @return 岗位列表
     */
    @GetMapping
    public Result<List<Job>> getAllJobs() {
        List<Job> jobs = jobService.list();
        return Result.success(jobs);
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
     * 
     * @param job 岗位实体
     * @return 创建结果
     */
    @PostMapping
    public Result<String> createJob(@RequestBody Job job) {
        job.setStatus(0); // 草稿状态
        jobService.save(job);
        return Result.success("岗位创建成功（草稿）");
    }
    
    /**
     * 更新岗位
     * 
     * @param id 岗位ID
     * @param job 岗位实体（包含要更新的字段）
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result<String> updateJob(@PathVariable Long id, @RequestBody Job job) {
        Job existJob = jobService.getById(id);
        if (existJob == null) {
            return Result.error(404, "岗位不存在");
        }
        
        job.setId(id);
        jobService.updateById(job);
        
        return Result.success("岗位更新成功");
    }
    
    /**
     * 发布岗位（草稿 -> 已发布）
     * 
     * @param id 岗位ID
     * @return 发布结果
     */
    @PutMapping("/{id}/publish")
    public Result<String> publishJob(@PathVariable Long id) {
        boolean success = jobService.publishJob(id);
        if (!success) {
            return Result.error("发布失败");
        }
        
        return Result.success("岗位发布成功");
    }
    
    /**
     * 关闭岗位
     * 
     * @param id 岗位ID
     * @return 关闭结果
     */
    @PutMapping("/{id}/close")
    public Result<String> closeJob(@PathVariable Long id) {
        boolean success = jobService.closeJob(id);
        if (!success) {
            return Result.error("关闭失败");
        }
        
        return Result.success("岗位已关闭");
    }
    
    /**
     * 暂停岗位
     * 
     * @param id 岗位ID
     * @return 暂停结果
     */
    @PutMapping("/{id}/pause")
    public Result<String> pauseJob(@PathVariable Long id) {
        boolean success = jobService.pauseJob(id);
        if (!success) {
            return Result.error("暂停失败");
        }
        
        return Result.success("岗位已暂停");
    }
    
    /**
     * 删除岗位（软删除）
     * 
     * @param id 岗位ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteJob(@PathVariable Long id) {
        Job job = jobService.getById(id);
        if (job == null) {
            return Result.error(404, "岗位不存在");
        }
        
        // 软删除（设置deleted=1）
        job.setDeleted(1);
        jobService.updateById(job);
        
        return Result.success("岗位删除成功");
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
