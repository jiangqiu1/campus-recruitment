package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志表实体类
 */
@Data
@TableName("operation_log")
public class OperationLog {
    
    /**
     * 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 操作用户ID - 外键(sys_user.id)
     */
    private Long userId;
    
    /**
     * 操作类型（查看简历、下载等）
     */
    private String operationType;
    
    /**
     * 目标对象ID（如简历ID）
     */
    private String targetId;
    
    /**
     * 操作IP
     */
    private String ipAddress;
    
    /**
     * 操作时间
     */
    private LocalDateTime createTime;
}
