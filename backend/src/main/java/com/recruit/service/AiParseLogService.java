package com.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.recruit.entity.AiParseLog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI解析日志表 服务接口
 */
public interface AiParseLogService extends IService<AiParseLog> {
    
    /**
     * 根据教师ID查询AI解析日志
     * 
     * @param teacherId 教师ID
     * @return AI解析日志列表
     */
    List<AiParseLog> selectByTeacherId(Long teacherId);
    
    /**
     * 查询未人工修正的AI解析日志
     * 
     * @return AI解析日志列表
     */
    List<AiParseLog> selectUncorrected();
    
    /**
     * 根据置信度范围查询AI解析日志
     * 
     * @param minScore 最小置信度
     * @param maxScore 最大置信度
     * @return AI解析日志列表
     */
    List<AiParseLog> selectByConfidenceRange(BigDecimal minScore, BigDecimal maxScore);
    
    /**
     * 根据时间范围查询AI解析日志
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return AI解析日志列表
     */
    List<AiParseLog> selectByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 记录AI解析日志
     * 
     * @param teacherId 教师ID
     * @param rawMessage 原始转发消息
     * @param parsedResult 解析结果（JSON）
     * @param confidenceScore 置信度
     * @return 是否成功
     */
    boolean logParse(Long teacherId, String rawMessage, String parsedResult, BigDecimal confidenceScore);
    
    /**
     * 人工修正AI解析结果
     * 
     * @param logId 日志ID
     * @param correctedResult 修正后的结果（JSON）
     * @return 是否成功
     */
    boolean correctParseResult(Long logId, String correctedResult);
    
    /**
     * 统计教师AI解析次数（用于数据分析）
     * 
     * @param teacherId 教师ID
     * @return 解析次数
     */
    Integer countByTeacherId(Long teacherId);
    
    /**
     * 统计平均置信度（用于AI模型优化）
     * 
     * @return 平均置信度
     */
    BigDecimal calculateAverageConfidence();
    
    /**
     * 统计需要人工修正的日志数量（用于工作量评估）
     * 
     * @return 需要修正的日志数量
     */
    Integer countUncorrected();
}
