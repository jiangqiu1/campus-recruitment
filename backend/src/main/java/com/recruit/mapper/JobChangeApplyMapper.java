package com.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruit.entity.JobChangeApply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 岗位变更申请记录表 Mapper 接口
 */
@Mapper
public interface JobChangeApplyMapper extends BaseMapper<JobChangeApply> {
    
    /**
     * 根据岗位ID查询申请记录
     * 
     * @param jobId 岗位ID
     * @return 申请记录列表
     */
    @Select("SELECT * FROM job_change_apply WHERE job_id = #{jobId}")
    List<JobChangeApply> selectByJobId(@Param("jobId") Long jobId);
    
    /**
     * 根据HR ID查询申请记录
     * 
     * @param hrId HR ID
     * @return 申请记录列表
     */
    @Select("SELECT * FROM job_change_apply WHERE hr_id = #{hrId}")
    List<JobChangeApply> selectByHrId(@Param("hrId") Long hrId);
    
    /**
     * 根据审核教师ID查询申请记录
     * 
     * @param reviewTeacherId 审核教师ID
     * @return 申请记录列表
     */
    @Select("SELECT * FROM job_change_apply WHERE review_teacher_id = #{reviewTeacherId}")
    List<JobChangeApply> selectByReviewTeacherId(@Param("reviewTeacherId") Long reviewTeacherId);
    
    /**
     * 根据状态查询申请记录
     * 
     * @param status 状态（0=待审核，1=通过，2=拒绝）
     * @return 申请记录列表
     */
    @Select("SELECT * FROM job_change_apply WHERE status = #{status}")
    List<JobChangeApply> selectByStatus(@Param("status") Integer status);
}
