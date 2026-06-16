package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recruit.entity.JobChangeApply;
import com.recruit.mapper.JobChangeApplyMapper;
import com.recruit.service.JobChangeApplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 岗位变更申请记录表 服务实现类
 */
@Service
public class JobChangeApplyServiceImpl extends ServiceImpl<JobChangeApplyMapper, JobChangeApply> implements JobChangeApplyService {
    
    @Autowired
    private JobChangeApplyMapper jobChangeApplyMapper;
    
    @Override
    public List<JobChangeApply> selectByJobId(Long jobId) {
        return jobChangeApplyMapper.selectByJobId(jobId);
    }
    
    @Override
    public List<JobChangeApply> selectByHrId(Long hrId) {
        return jobChangeApplyMapper.selectByHrId(hrId);
    }
    
    @Override
    public List<JobChangeApply> selectByReviewTeacherId(Long reviewTeacherId) {
        return jobChangeApplyMapper.selectByReviewTeacherId(reviewTeacherId);
    }
    
    @Override
    public List<JobChangeApply> selectByStatus(Integer status) {
        return jobChangeApplyMapper.selectByStatus(status);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitChangeApply(Long jobId, Long hrId, String changeContent) {
        JobChangeApply apply = new JobChangeApply();
        apply.setJobId(jobId);
        apply.setHrId(hrId);
        apply.setChangeContent(changeContent);
        apply.setStatus(0); // 待审核
        apply.setCreateTime(LocalDateTime.now());
        
        return save(apply);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reviewChangeApply(Long applyId, Long reviewTeacherId, boolean approved, String feedback) {
        JobChangeApply apply = getById(applyId);
        if (apply == null) {
            throw new RuntimeException("申请记录不存在");
        }
        
        if (apply.getStatus() != 0) {
            throw new RuntimeException("该申请已审核，不能重复审核");
        }
        
        // 更新状态
        apply.setStatus(approved ? 1 : 2); // 1=通过，2=拒绝
        apply.setReviewTeacherId(reviewTeacherId);
        
        // TODO：这里应该保存审核反馈，但数据库设计中没有feedback字段
        // 可以先记录到操作日志中
        
        return updateById(apply);
    }
}
