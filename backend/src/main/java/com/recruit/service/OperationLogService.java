package com.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.recruit.entity.OperationLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作日志表 服务接口
 */
public interface OperationLogService extends IService<OperationLog> {
    
    /**
     * 记录操作日志
     * 
     * @param userId 用户ID
     * @param operationType 操作类型（查看简历、下载等）
     * @param targetId 目标对象ID（如简历ID）
     * @param ipAddress 操作IP
     * @return 是否成功
     */
    boolean logOperation(Long userId, String operationType, String targetId, String ipAddress);
    
    /**
     * 根据用户ID查询操作日志
     * 
     * @param userId 用户ID
     * @return 操作日志列表
     */
    List<OperationLog> selectByUserId(Long userId);
    
    /**
     * 根据操作类型查询操作日志
     * 
     * @param operationType 操作类型
     * @return 操作日志列表
     */
    List<OperationLog> selectByOperationType(String operationType);
    
    /**
     * 根据用户ID和操作类型查询操作日志
     * 
     * @param userId 用户ID
     * @param operationType 操作类型
     * @return 操作日志列表
     */
    List<OperationLog> selectByUserIdAndOperationType(Long userId, String operationType);
    
    /**
     * 根据时间范围查询操作日志
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 操作日志列表
     */
    List<OperationLog> selectByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计用户操作次数（用于数据分析）
     * 
     * @param userId 用户ID
     * @return Map<操作类型, 次数>
     */
    Map<String, Integer> countByUserIdAndGroupByOperationType(Long userId);
    
    /**
     * 清理指定时间之前的日志（用于日志归档）
     * 
     * @param beforeTime 截止时间
     * @return 删除的行数
     */
    int cleanupLogsBeforeTime(LocalDateTime beforeTime);
}
