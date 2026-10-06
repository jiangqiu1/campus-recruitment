package com.recruit.controller;

import com.recruit.entity.JobChangeApply;
import com.recruit.entity.Job;
import com.recruit.entity.Company;
import com.recruit.entity.SysUser;
import com.recruit.annotation.LogOperation;
import com.recruit.dto.JobChangeSubmitRequest;
import com.recruit.service.JobChangeApplyService;
import com.recruit.service.JobService;
import com.recruit.service.CompanyService;
import com.recruit.service.UserService;
import com.recruit.utils.Result;

import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 岗位变更申请控制器
 * 教师审核HR提交的岗位变更申请
 */
@RestController
@RequestMapping("/job-changes")
public class JobChangeApplyController extends BaseController {

    @Autowired
    private JobChangeApplyService jobChangeApplyService;

    @Autowired
    private JobService jobService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private UserService userService;

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

        // 批量组装：HR/岗位/企业各一次批查，替代循环内逐条查询
        List<Long> hrIds = list.stream().map(JobChangeApply::getHrId).filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, SysUser> hrMap = hrIds.isEmpty() ? new HashMap<>() : userService.listByIds(hrIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));
        List<Long> jobIds = list.stream().map(JobChangeApply::getJobId).filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Job> jobMap = jobIds.isEmpty() ? new HashMap<>() : jobService.listByIds(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, j -> j));
        List<Long> companyIds = jobMap.values().stream().map(Job::getCompanyId).filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Company> companyMap = companyIds.isEmpty() ? new HashMap<>() : companyService.listByIds(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, c -> c));

        List<Map<String, Object>> result = new ArrayList<>();
        for (JobChangeApply apply : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", apply.getId());
            item.put("applyTime", apply.getCreateTime() != null ? apply.getCreateTime().toString().replace("T", " ") : "");
            item.put("jobId", apply.getJobId());
            item.put("hrId", apply.getHrId());
            // 查询申请人姓名
            String applyUserName = "未知用户";
            if (apply.getHrId() != null) {
                SysUser hrUser = hrMap.get(apply.getHrId());
                if (hrUser != null) {
                    applyUserName = hrUser.getRealName() != null ? hrUser.getRealName() : hrUser.getUsername();
                }
            }
            item.put("applyUser", applyUserName);
            item.put("status", apply.getStatus());
            item.put("changeContent", apply.getChangeContent());

            // 关联岗位和企业名称
            if (apply.getJobId() != null) {
                Job job = jobMap.get(apply.getJobId());
                if (job != null) {
                    item.put("jobTitle", job.getTitle());
                    Company company = companyMap.get(job.getCompanyId());
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
        requireHr();
        Long jobId = params.get("jobId") != null ? Long.valueOf(params.get("jobId").toString()) : null;
        // hrId 一律取当前登录人，防伪造他人名义提交
        Long hrId = getCurrentUserId();
        String changeContent = params.get("changeContent") != null ? params.get("changeContent").toString() : "";

        if (jobId == null) {
            return Result.error("岗位ID不能为空");
        }

        boolean success = jobChangeApplyService.submitChangeApply(jobId, hrId, changeContent);
        return success ? Result.success("申请提交成功") : Result.error("提交失败");
    }

    /**
     * 审核通过
     */
    @LogOperation("审核通过岗位变更申请")
    @PutMapping("/{id}/approve")
    public Result<String> approve(@PathVariable Long id) {
        requireTeacher();
        JobChangeApply apply = jobChangeApplyService.getById(id);
        if (apply == null) return Result.error(404, "申请不存在");
        if (apply.getStatus() != 0) return Result.error("该申请已审核");

        apply.setStatus(1);
        apply.setReviewTeacherId(getCurrentUserId());
        jobChangeApplyService.updateById(apply);
        return Result.success("审核通过");
    }

    /**
     * 审核拒绝（需携带拒绝原因，随申请展示给 HR）
     */
    @LogOperation("审核拒绝岗位变更申请")
    @PutMapping("/{id}/reject")
    public Result<String> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> params) {
        requireTeacher();
        JobChangeApply apply = jobChangeApplyService.getById(id);
        if (apply == null) return Result.error(404, "申请不存在");
        if (apply.getStatus() != 0) return Result.error("该申请已审核");

        String reason = params != null ? params.get("reason") : null;
        if (reason == null || reason.trim().isEmpty()) {
            return Result.error("请填写拒绝原因");
        }

        apply.setStatus(2);
        apply.setReviewTeacherId(getCurrentUserId());
        apply.setRejectReason(reason.trim());
        jobChangeApplyService.updateById(apply);
        return Result.success("已拒绝");
    }
}
