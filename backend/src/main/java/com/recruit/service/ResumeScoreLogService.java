package com.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.recruit.entity.ResumeScoreLog;

import java.util.List;
import java.util.Map;

/**
 * 简历智能评分记录表 服务接口
 */
public interface ResumeScoreLogService extends IService<ResumeScoreLog> {
    
    /**
     * 根据岗位ID查询评分记录（按分数降序）
     * 
     * @param jobId 岗位ID
     * @return 评分记录列表
     */
    List<ResumeScoreLog> selectByJobIdOrderByScore(Long jobId);
    
    /**
     * 根据投递记录ID查询评分记录
     * 
     * @param deliveryId 投递记录ID
     * @return 评分记录
     */
    ResumeScoreLog selectByDeliveryId(Long deliveryId);
    
    /**
     * 根据分数范围查询评分记录
     * 
     * @param minScore 最低分数
     * @param maxScore 最高分数
     * @return 评分记录列表
     */
    List<ResumeScoreLog> selectByScoreRange(Integer minScore, Integer maxScore);
    
    /**
     * 统计岗位的平均分
     * 
     * @param jobId 岗位ID
     * @return 平均分数
     */
    Double calculateAverageScoreByJobId(Long jobId);
    
    /**
     * 对投递简历进行智能评分（AI算法）
     * 
     * @param jobId 岗位ID
     * @param deliveryId 投递记录ID
     * @return 是否成功
     */
    boolean scoreResume(Long jobId, Long deliveryId);
    
    /**
     * 批量评分（对某个岗位的所有投递简历进行评分）
     * 
     * @param jobId 岗位ID
     * @return 评分数量
     */
    int batchScoreResumes(Long jobId);
    
    /**
     * 重新评分（覆盖之前的评分）
     * 
     * @param scoreLogId 评分记录ID
     * @return 是否成功
     */
    boolean rescoreResume(Long scoreLogId);
    
    /**
     * 统计岗位的分数分布（用于统计图表）
     * 
     * @param jobId 岗位ID
     * @return Map<分数段, 数量> （如：0-59, 60-79, 80-100）
     */
    Map<String, Integer> calculateScoreDistribution(Long jobId);
    
    /**
     * 获取岗位的最高分简历
     * 
     * @param jobId 岗位ID
     * @return 最高分评分记录
     */
    ResumeScoreLog selectTopScoreByJobId(Long jobId);
    
    /**
     * 获取岗位的最低分简历
     * 
     * @param jobId 岗位ID
     * @return 最低分评分记录
     */
    ResumeScoreLog selectLowestScoreByJobId(Long jobId);

    /**
     * 对企业所有岗位的简历批量评分
     */
    int batchScoreByCompany(Long companyId);
}