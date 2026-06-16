package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI解析日志表实体类
 */
@Data
@TableName("ai_parse_log")
public class AiParseLog {
    
    /**
     * 主键自增 - 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 发起解析的教师ID - 外键(sys_user.id)
     */
    private Long teacherId;
    
    /**
     * 原始转发消息（文本或图片描述）
     */
    private String rawMessage;
    
    /**
     * AI解析输出的结构化JSON
     */
    private String parsedResult;
    
    /**
     * 是否人工修正：0=未修正，1=已人工修正
     */
    private Integer isManualCorrected;
    
    /**
     * 人工修正后的JSON
     */
    private String correctedResult;
    
    /**
     * AI整体置信度（0-1）
     */
    private BigDecimal confidenceScore;
    
    /**
     * 解析时间
     */
    private LocalDateTime createTime;
}
