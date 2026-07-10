package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 人岗匹配记录表实体类
 */
@Data
@TableName("job_match_record")
public class JobMatchRecord {
    
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
     * 学生ID - 外键(sys_user.id)
     */
    private Long studentId;
    
    /**
     * 匹配度分数（0-1）
     */
    private BigDecimal matchScore;
    
    /**
     * 子维度分数 JSON（技能/学历/经验/专业等）
     * 格式示例：{"skillMatch":85,"eduMatch":80,"expMatch":72,"majorFit":78}
     */
    private String scoreDetail;
    
    /**
     * 匹配理由（如"技能匹配：Java,Spring"）
     */
    private String matchReason;
    
    /**
     * 是否推送：0=未推送，1=已推送
     */
    private Integer isPushed;
    
    /**
     * 推送时间
     */
    private LocalDateTime pushTime;
    
    /**
     * 是否点击：0=未点击，1=已点击（学生是否查看）
     */
    private Integer isClicked;
    
    /**
     * 计算时间
     */
    private LocalDateTime createTime;
}
