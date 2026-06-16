package com.recruit.controller;

import com.recruit.entity.JobChangeApply;
import com.recruit.entity.Job;
import com.recruit.entity.Company;
import com.recruit.service.JobChangeApplyService;
import com.recruit.service.JobService;
import com.recruit.service.CompanyService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 岗位变更申请控制器
 * 教师审核HR提交的岗位变更申请
 */
@RestController
@RequestMapping("/job-changes")
public class JobChangeApplyController {

    @Autowired
    private JobChangeApplyService jobChangeApplyService;

    @Autowired
    private JobService jobService;

    @Autowired
    private CompanyService companyService;

    /**
     * 获取岗位变更申请列表
     *
     * @param status 状态筛选（0=待审核，不传则返回全部）
     */
    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Integer status) {
        List<JobChangeApply> list;
        if (status != null) {
            list = jobChangeApplyService.lambdaQuery()
                    .eq(JobChangeApply::getStatus, status)
                    .orderByDesc(JobChangeApply::getCreateTime)
                    .list();
        } else {
            list = jobChangeApplyService.lambdaQuery()
                    .orderByDesc(JobChangeApply::getCreateTime)
                    .list();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (JobChangeApply apply : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", apply.getId());
            item.put("applyTime", apply.getCreateTime() != null ? apply.getCreateTime().toString().replace("T", " ") : "");
            item.put("jobId", apply.getJobId());
            item.put("hrId", apply.getHrId());
            item.put("status", apply.getStatus());
            item.put("changeContent", apply.getChangeContent());

            // 关联岗位和企业名称
            if (apply.getJobId() != null) {
                Job job = jobService.getById(apply.getJobId());
                if (job != null) {
                    item.put("jobTitle", job.getTitle());
                    Company company = companyService.getById(job.getCompanyId());
                    item.put("companyName", company != null ? company.getName() : "未知企业");
                } else {
                    item.put("jobTitle", "已删除岗位");
                    item.put("companyName", "");
                }
            }

            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 提交岗位变更申请（HR端用）
     */
    @PostMapping
    public Result<String> submit(@RequestBody Map<String, Object> params) {
        Long jobId = params.get("jobId") != null ? Long.valueOf(params.get("jobId").toString()) : null;
        Long hrId = params.get("hrId") != null ? Long.valueOf(params.get("hrId").toString()) : null;
        String changeContent = params.get("changeContent") != null ? params.get("changeContent").toString() : "";

        if (jobId == null || hrId == null) {
            return Result.error("岗位ID和HR ID不能为空");
        }

        boolean success = jobChangeApplyService.submitChangeApply(jobId, hrId, changeContent);
        return success ? Result.success("申请提交成功") : Result.error("提交失败");
    }

    /**
     * 审核通过
     */
    @PutMapping("/{id}/approve")
    public Result<String> approve(@PathVariable Long id) {
        JobChangeApply apply = jobChangeApplyService.getById(id);
        if (apply == null) return Result.error(404, "申请不存在");
        if (apply.getStatus() != 0) return Result.error("该申请已审核");

        apply.setStatus(1);
        apply.setReviewTeacherId(0L);
        jobChangeApplyService.updateById(apply);
        return Result.success("审核通过");
    }

    /**
     * 审核拒绝
     */
    @PutMapping("/{id}/reject")
    public Result<String> reject(@PathVariable Long id) {
        JobChangeApply apply = jobChangeApplyService.getById(id);
        if (apply == null) return Result.error(404, "申请不存在");
        if (apply.getStatus() != 0) return Result.error("该申请已审核");

        apply.setStatus(2);
        apply.setReviewTeacherId(0L);
        jobChangeApplyService.updateById(apply);
        return Result.success("已拒绝");
    }
}
