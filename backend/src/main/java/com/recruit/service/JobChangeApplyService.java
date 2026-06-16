package com.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.recruit.entity.JobChangeApply;

import java.util.List;

/**
 * 岗位变更申请记录表 服务接口
 */
public interface JobChangeApplyService extends IService<JobChangeApply> {
    
    /**
     * 根据岗位ID查询申请记录
     * 
     * @param jobId 岗位ID
     * @return 申请记录列表
     */
    List<JobChangeApply> selectByJobId(Long jobId);
    
    /**
     * 根据HR ID查询申请记录
     * 
     * @param hrId HR ID
     * @return 申请记录列表
     */
    List<JobChangeApply> selectByHrId(Long hrId);
    
    /**
     * 根据审核教师ID查询申请记录
     * 
     * @param reviewTeacherId 审核教师ID
     * @return 申请记录列表
     */
    List<JobChangeApply> selectByReviewTeacherId(Long reviewTeacherId);
    
    /**
     * 根据状态查询申请记录
     * 
     * @param status 状态（0=待审核，1=通过，2=拒绝）
     * @return 申请记录列表
     */
    List<JobChangeApply> selectByStatus(Integer status);
    
    /**
     * 提交岗位变更申请
     * 
     * @param jobId 岗位ID
     * @param hrId HR ID
     * @param changeContent 变更内容（JSON格式）
     * @return 是否成功
     */
    boolean submitChangeApply(Long jobId, Long hrId, String changeContent);
    
    /**
     * 审核岗位变更申请
     * 
     * @param applyId 申请ID
     * @param reviewTeacherId 审核教师ID
     * @param approved 是否通过
     * @param feedback 审核反馈
     * @return 是否成功
     */
    boolean reviewChangeApply(Long applyId, Long reviewTeacherId, boolean approved, String feedback);
}
