package com.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruit.entity.ResumeScoreLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 简历智能评分记录表 Mapper 接口
 */
@Mapper
public interface ResumeScoreLogMapper extends BaseMapper<ResumeScoreLog> {
    
    /**
     * 根据岗位ID查询评分记录（按分数降序）
     * 
     * @param jobId 岗位ID
     * @return 评分记录列表
     */
    @Select("SELECT * FROM resume_score_log WHERE job_id = #{jobId} ORDER BY score DESC")
    List<ResumeScoreLog> selectByJobIdOrderByScore(@Param("jobId") Long jobId);
    
    /**
     * 根据投递记录ID查询评分记录
     * 
     * @param deliveryId 投递记录ID
     * @return 评分记录
     */
    @Select("SELECT * FROM resume_score_log WHERE delivery_id = #{deliveryId}")
    ResumeScoreLog selectByDeliveryId(@Param("deliveryId") Long deliveryId);
    
    /**
     * 根据分数范围查询评分记录
     * 
     * @param minScore 最低分数
     * @param maxScore 最高分数
     * @return 评分记录列表
     */
    @Select("SELECT * FROM resume_score_log WHERE score BETWEEN #{minScore} AND #{maxScore} ORDER BY score DESC")
    List<ResumeScoreLog> selectByScoreRange(@Param("minScore") Integer minScore, @Param("maxScore") Integer maxScore);
    
    /**
     * 统计岗位的平均分
     * 
     * @param jobId 岗位ID
     * @return 平均分数
     */
    @Select("SELECT AVG(score) FROM resume_score_log WHERE job_id = #{jobId}")
    Double calculateAverageScoreByJobId(@Param("jobId") Long jobId);
}
