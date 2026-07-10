package com.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruit.entity.JobMatchRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 人岗匹配记录表 Mapper 接口
 */
@Mapper
public interface JobMatchRecordMapper extends BaseMapper<JobMatchRecord> {
    
    /**
     * 根据岗位ID查询匹配记录（按匹配度降序）
     * 
     * @param jobId 岗位ID
     * @return 匹配记录列表
     */
    @Select("SELECT * FROM job_match_record WHERE job_id = #{jobId} ORDER BY match_score DESC")
    List<JobMatchRecord> selectByJobIdOrderByScore(@Param("jobId") Long jobId);
    
    /**
     * 根据学生ID查询匹配记录
     * 
     * @param studentId 学生ID
     * @return 匹配记录列表
     */
    @Select("SELECT * FROM job_match_record WHERE student_id = #{studentId} ORDER BY create_time DESC")
    List<JobMatchRecord> selectByStudentId(@Param("studentId") Long studentId);
    
    /**
     * 查询已推送的匹配记录
     * 
     * @param jobId 岗位ID
     * @return 已推送的匹配记录列表
     */
    @Select("SELECT * FROM job_match_record WHERE job_id = #{jobId} AND is_pushed = 1 ORDER BY match_score DESC")
    List<JobMatchRecord> selectPushedByJobId(@Param("jobId") Long jobId);
    
    /**
     * 查询已点击的匹配记录
     * 
     * @param jobId 岗位ID
     * @return 已点击的匹配记录列表
     */
    @Select("SELECT * FROM job_match_record WHERE job_id = #{jobId} AND is_clicked = 1")
    List<JobMatchRecord> selectClickedByJobId(@Param("jobId") Long jobId);
    
    /**
     * 根据匹配度范围查询匹配记录
     * 
     * @param minScore 最小匹配度
     * @param maxScore 最大匹配度
     * @return 匹配记录列表
     */
    @Select("SELECT * FROM job_match_record WHERE match_score BETWEEN #{minScore} AND #{maxScore} ORDER BY match_score DESC")
    List<JobMatchRecord> selectByScoreRange(@Param("minScore") BigDecimal minScore, @Param("maxScore") BigDecimal maxScore);
    
    /**
     * 更新推送状态
     * 
     * @param id 记录ID
     * @param pushTime 推送时间
     * @return 影响行数
     */
    int updatePushedStatus(@Param("id") Long id, @Param("pushTime") java.time.LocalDateTime pushTime);
    
    /**
     * 更新点击状态
     * 
     * @param id 记录ID
     * @return 影响行数
     */
    int updateClickedStatus(@Param("id") Long id);
    
    /**
     * 根据岗位ID删除所有匹配记录
     * 
     * @param jobId 岗位ID
     * @return 删除数量
     */
    int deleteByJobId(@Param("jobId") Long jobId);
}
