package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI反馈日志表实体类
 */
@Data
@TableName("ai_feedback_log")
public class AiFeedbackLog {
    
    /**
     * 主键自增 - 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID - 外键(sys_user.id)
     */
    private Long userId;
    
    /**
     * 反馈类型：parse_error / match_bad / match_good
     */
    private String feedbackType;
    
    /**
     * 目标对象ID（如简历ID、岗位ID）
     */
    private Long targetId;
    
    /**
     * 反馈内容
     */
    private String feedbackContent;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
