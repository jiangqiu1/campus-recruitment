package com.recruit.controller;

import com.recruit.entity.Company;
import com.recruit.entity.Job;
import com.recruit.service.CompanyService;
import com.recruit.service.JobService;
import com.recruit.service.UserService;
import com.recruit.entity.SysUser;
import com.recruit.utils.Result;
import com.recruit.dto.PageResult;
import com.recruit.annotation.LogOperation;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 岗位管理控制器
 * HR和教师可以访问
 */
@RestController
@RequestMapping("/jobs")
public class JobController extends BaseController {
    
    @Autowired
    private JobService jobService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private UserService userService;
    
    /**
     * 获取岗位列表（按角色过滤，支持分页）
     */
    @GetMapping
    public Result<PageResult<Job>> getAllJobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        Integer role = getCurrentRole();
        com.baomidou.mybatisplus.core.metadata.IPage<Job> pageResult;
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Job> pageReq = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        
        var query = jobService.lambdaQuery();
        
        if (Objects.equals(role, 2)) {
            // HR：只看自己企业的岗位
            Long userId = getCurrentUserId();
            SysUser currentUser = userService.getById(userId);
            if (currentUser != null && currentUser.getCompanyId() != null) {
                query.eq(Job::getCompanyId, currentUser.getCompanyId());
            } else {
                query.eq(Job::getCreatedBy, userId);
            }
        } else if (!Objects.equals(role, 3)) {
            // 非管理员非HR：只看已发布的
            query.eq(Job::getStatus, 1);
        }
        // 管理员（role=3）：无限制，查看所有岗位
        
        // 状态筛选
        if (status != null) {
            query.eq(Job::getStatus, status);
        }
        // 关键词筛选
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.like(Job::getTitle, keyword.trim());
        }
        
        pageResult = query.orderByDesc(Job::getCreateTime).page(pageReq);

        // 批量填充公司名称
        List<Job> records = pageResult.getRecords();
        if (!records.isEmpty()) {
            Set<Long> companyIds = records.stream()
                    .map(Job::getCompanyId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, String> companyNameMap = companyService.listByIds(new ArrayList<>(companyIds))
                    .stream().collect(Collectors.toMap(Company::getId, c -> c.getName() != null ? c.getName() : ""));
            records.forEach(job -> job.setCompanyName(companyNameMap.getOrDefault(job.getCompanyId(), "")));
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

        // 填充公司名称
        if (job.getCompanyId() != null) {
            Company company = companyService.getById(job.getCompanyId());
            job.setCompanyName(company != null && company.getName() != null ? company.getName() : "");
        }

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
     * @return 岗位列表，包含关联企业名称
     */
    @GetMapping("/by-creator/{createdBy}")
    public Result<List<Job>> getJobsByCreatedBy(@PathVariable Long createdBy) {
        List<Job> jobs = jobService.selectByCreatedBy(createdBy);
        // 填充关联企业名称
        if (!jobs.isEmpty()) {
            Set<Long> companyIds = jobs.stream()
                    .map(Job::getCompanyId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!companyIds.isEmpty()) {
                Map<Long, String> companyNameMap = companyService.listByIds(new ArrayList<>(companyIds))
                        .stream().collect(Collectors.toMap(Company::getId, c -> c.getName() != null ? c.getName() : ""));
                jobs.forEach(job -> job.setCompanyName(companyNameMap.getOrDefault(job.getCompanyId(), "")));
            }
        }
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
     * 校验权限：HR可编辑自己或同企业HR的岗位，教师不可编辑企业岗位
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
            // HR可编辑自己或同企业其他HR创建的岗位
            if (!isSameCompanyHr(userId, existJob.getCreatedBy())) {
                return Result.error(403, "无权编辑其他企业HR创建的岗位");
            }
        } else if (Objects.equals(role, 1)) {
            // 教师不可编辑企业岗位内容（仅可标记问题或关闭）
            return Result.error(403, "教师不可编辑企业岗位内容");
        }
        
        job.setId(id);
        boolean success = jobService.updateById(job);
        if (!success) {
            return Result.error("更新失败，请重试");
        }
        
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
        
        // HR只能关闭自己（或同企业HR）的岗位
        if (Objects.equals(role, 2) && !isSameCompanyHr(userId, job.getCreatedBy())) {
            return Result.error(403, "无权关闭其他企业HR的岗位");
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
        
        if (Objects.equals(role, 2) && !isSameCompanyHr(userId, job.getCreatedBy())) {
            return Result.error(403, "无权暂停其他企业HR的岗位");
        }
        
        boolean success = jobService.pauseJob(id);
        if (!success) {
            return Result.error("暂停失败");
        }
        return Result.success("岗位已暂停");
    }
    
    /**
     * 删除岗位（软删除）
     * HR可删除自己的草稿/已关闭岗位，教师可删除自己创建的岗位（草稿/已关闭状态）
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteJob(@PathVariable Long id) {
        Job job = jobService.getById(id);
        if (job == null) {
            return Result.error(404, "岗位不存在");
        }
        
        Integer role = getCurrentRole();
        Long userId = getCurrentUserId();
        
        // 教师可删除自己创建的岗位（仅允许草稿或已关闭状态）
        if (Objects.equals(role, 1)) {
            if (!Objects.equals(job.getCreatedBy(), userId)) {
                return Result.error(403, "无权删除其他教师创建的岗位");
            }
            if (job.getStatus() != 0 && job.getStatus() != 2) {
                return Result.error("仅允许删除草稿或已关闭的岗位");
            }
        }
        // HR只能删除自己或同企业HR的岗位，且仅允许删除草稿或已关闭状态
        else if (Objects.equals(role, 2)) {
            if (!isSameCompanyHr(userId, job.getCreatedBy())) {
                return Result.error(403, "无权删除其他企业HR的岗位");
            }
            if (job.getStatus() != 0 && job.getStatus() != 2) {
                return Result.error("仅允许删除草稿或已关闭的岗位");
            }
        }
        
        jobService.removeById(id);
        return Result.success("岗位删除成功");
    }
    
    /**
     * 校验岗位所属权（同企业HR可操作）
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
        // 教师和HR可操作自己创建或同企业的岗位
        if (Objects.equals(role, 1) || Objects.equals(role, 2)) {
            if (isSameCompanyHr(userId, job.getCreatedBy())) return;
        }
        
        throw new com.recruit.exception.BusinessException(403, "无权操作此岗位");
    }
    
    /**
     * 判断两个HR用户是否属于同一企业
     */
    private boolean isSameCompanyHr(Long userId1, Long userId2) {
        if (Objects.equals(userId1, userId2)) return true;
        try {
            SysUser user1 = userService.getById(userId1);
            SysUser user2 = userService.getById(userId2);
            if (user1 == null || user2 == null) return false;
            if (user1.getCompanyId() == null || user2.getCompanyId() == null) return false;
            return Objects.equals(user1.getCompanyId(), user2.getCompanyId());
        } catch (Exception e) {
            return false;
        }
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
