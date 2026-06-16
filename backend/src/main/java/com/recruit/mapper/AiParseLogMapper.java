package com.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruit.entity.AiParseLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI解析日志表 Mapper 接口
 */
@Mapper
public interface AiParseLogMapper extends BaseMapper<AiParseLog> {
    
    /**
     * 根据教师ID查询AI解析日志
     * 
     * @param teacherId 教师ID
     * @return AI解析日志列表
     */
    @Select("SELECT * FROM ai_parse_log WHERE teacher_id = #{teacherId} ORDER BY create_time DESC")
    List<AiParseLog> selectByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 查询未人工修正的AI解析日志
     * 
     * @return AI解析日志列表
     */
    @Select("SELECT * FROM ai_parse_log WHERE is_manual_corrected = 0 ORDER BY create_time DESC")
    List<AiParseLog> selectUncorrected();
    
    /**
     * 根据置信度范围查询AI解析日志
     * 
     * @param minScore 最小置信度
     * @param maxScore 最大置信度
     * @return AI解析日志列表
     */
    @Select("SELECT * FROM ai_parse_log WHERE confidence_score BETWEEN #{minScore} AND #{maxScore} ORDER BY create_time DESC")
    List<AiParseLog> selectByConfidenceRange(@Param("minScore") BigDecimal minScore, @Param("maxScore") BigDecimal maxScore);
    
    /**
     * 根据时间范围查询AI解析日志
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return AI解析日志列表
     */
    @Select("SELECT * FROM ai_parse_log WHERE create_time BETWEEN #{startTime} AND #{endTime} ORDER BY create_time DESC")
    List<AiParseLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
