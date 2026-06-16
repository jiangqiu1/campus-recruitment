package com.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruit.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志表 Mapper 接口
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
    
    /**
     * 根据用户ID查询操作日志
     * 
     * @param userId 用户ID
     * @return 操作日志列表
     */
    @Select("SELECT * FROM operation_log WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<OperationLog> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 根据操作类型查询操作日志
     * 
     * @param operationType 操作类型
     * @return 操作日志列表
     */
    @Select("SELECT * FROM operation_log WHERE operation_type = #{operationType} ORDER BY create_time DESC")
    List<OperationLog> selectByOperationType(@Param("operationType") String operationType);
    
    /**
     * 根据用户ID和操作类型查询操作日志
     * 
     * @param userId 用户ID
     * @param operationType 操作类型
     * @return 操作日志列表
     */
    @Select("SELECT * FROM operation_log WHERE user_id = #{userId} AND operation_type = #{operationType} ORDER BY create_time DESC")
    List<OperationLog> selectByUserIdAndOperationType(@Param("userId") Long userId, @Param("operationType") String operationType);
    
    /**
     * 根据时间范围查询操作日志
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 操作日志列表
     */
    @Select("SELECT * FROM operation_log WHERE create_time BETWEEN #{startTime} AND #{endTime} ORDER BY create_time DESC")
    List<OperationLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 删除指定时间之前的日志（用于日志清理）
     * 
     * @param beforeTime 截止时间
     * @return 影响行数
     */
    int deleteBeforeTime(@Param("beforeTime") LocalDateTime beforeTime);
}
