package com.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruit.entity.AiFeedbackLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI反馈日志表 Mapper 接口
 */
@Mapper
public interface AiFeedbackLogMapper extends BaseMapper<AiFeedbackLog> {
    
    /**
     * 根据用户ID查询AI反馈日志
     * 
     * @param userId 用户ID
     * @return AI反馈日志列表
     */
    @Select("SELECT * FROM ai_feedback_log WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<AiFeedbackLog> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 根据反馈类型查询AI反馈日志
     * 
     * @param feedbackType 反馈类型（parse_error / match_bad / match_good）
     * @return AI反馈日志列表
     */
    @Select("SELECT * FROM ai_feedback_log WHERE feedback_type = #{feedbackType} ORDER BY create_time DESC")
    List<AiFeedbackLog> selectByFeedbackType(@Param("feedbackType") String feedbackType);
    
    /**
     * 根据目标ID查询AI反馈日志
     * 
     * @param targetId 目标对象ID（如简历ID、岗位ID）
     * @return AI反馈日志列表
     */
    @Select("SELECT * FROM ai_feedback_log WHERE target_id = #{targetId} ORDER BY create_time DESC")
    List<AiFeedbackLog> selectByTargetId(@Param("targetId") Long targetId);
    
    /**
     * 根据用户ID和反馈类型查询AI反馈日志
     * 
     * @param userId 用户ID
     * @param feedbackType 反馈类型
     * @return AI反馈日志列表
     */
    @Select("SELECT * FROM ai_feedback_log WHERE user_id = #{userId} AND feedback_type = #{feedbackType} ORDER BY create_time DESC")
    List<AiFeedbackLog> selectByUserIdAndFeedbackType(@Param("userId") Long userId, @Param("feedbackType") String feedbackType);
    
    /**
     * 根据时间范围查询AI反馈日志
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return AI反馈日志列表
     */
    @Select("SELECT * FROM ai_feedback_log WHERE create_time BETWEEN #{startTime} AND #{endTime} ORDER BY create_time DESC")
    List<AiFeedbackLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计反馈类型数量（用于AI模型优化）
     * 
     * @param feedbackType 反馈类型
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM ai_feedback_log WHERE feedback_type = #{feedbackType}")
    Integer countByFeedbackType(@Param("feedbackType") String feedbackType);
}
