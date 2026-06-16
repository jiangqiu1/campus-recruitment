package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历智能评分记录表实体类
 */
@Data
@TableName("resume_score_log")
public class ResumeScoreLog {
    
    /**
     * 主键自增 - 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 岗位ID - 外键(job.id)
     */
    private Long jobId;
    
    /**
     * 投递记录ID - 外键(delivery.id)
     */
    private Long deliveryId;
    
    /**
     * 总分（如0-100）
     */
    private Integer score;
    
    /**
     * 评分细则JSON（如"技能得分90，经验得分70"）
     */
    private String scoreDetail;
    
    /**
     * 评分时间
     */
    private LocalDateTime createTime;
}
