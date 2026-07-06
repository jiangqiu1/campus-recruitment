package com.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.recruit.entity.JobMatchRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 人岗匹配记录表 服务接口
 */
public interface JobMatchRecordService extends IService<JobMatchRecord> {
    
    /**
     * 根据岗位ID查询匹配记录（按匹配度降序）
     * 
     * @param jobId 岗位ID
     * @return 匹配记录列表
     */
    List<JobMatchRecord> selectByJobIdOrderByScore(Long jobId);
    
    /**
     * 根据学生ID查询匹配记录
     * 
     * @param studentId 学生ID
     * @return 匹配记录列表
     */
    List<JobMatchRecord> selectByStudentId(Long studentId);
    
    /**
     * 查询已推送的匹配记录
     * 
     * @param jobId 岗位ID
     * @return 已推送的匹配记录列表
     */
    List<JobMatchRecord> selectPushedByJobId(Long jobId);
    
    /**
     * 查询已点击的匹配记录
     * 
     * @param jobId 岗位ID
     * @return 已点击的匹配记录列表
     */
    List<JobMatchRecord> selectClickedByJobId(Long jobId);
    
    /**
     * 根据匹配度范围查询匹配记录
     * 
     * @param minScore 最小匹配度
     * @param maxScore 最大匹配度
     * @return 匹配记录列表
     */
    List<JobMatchRecord> selectByScoreRange(BigDecimal minScore, BigDecimal maxScore);
    
    /**
     * 生成人岗匹配记录（AI算法）
     * 
     * @param jobId 岗位ID
     * @param studentId 学生ID
     * @return 是否成功
     */
    boolean generateMatchRecord(Long jobId, Long studentId);
    
    /**
     * 批量生成人岗匹配记录（对某个岗位，匹配所有学生）
     * 
     * @param jobId 岗位ID
     * @param classId 班级ID（可选，null=全部学生）
     * @return 生成的记录数量
     */
    int batchGenerateMatchRecords(Long jobId, Long classId);
    
    /**
     * 更新推送状态
     * 
     * @param recordId 记录ID
     * @return 是否成功
     */
    boolean updatePushedStatus(Long recordId);
    
    /**
     * 更新点击状态
     * 
     * @param recordId 记录ID
     * @return 是否成功
     */
    boolean updateClickedStatus(Long recordId);
    
    /**
     * 统计岗位的推送率（推送数/总匹配数）
     * 
     * @param jobId 岗位ID
     * @return 推送率（0-1）
     */
    BigDecimal calculatePushRate(Long jobId);
    
    /**
     * 统计岗位的点击率（点击数/推送数）
     * 
     * @param jobId 岗位ID
     * @return 点击率（0-1）
     */
    BigDecimal calculateClickRate(Long jobId);
    
    /**
     * 统计岗位的平均匹配度
     * 
     * @param jobId 岗位ID
     * @return 平均匹配度
     */
    BigDecimal calculateAverageMatchScore(Long jobId);
}
