package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 投递记录表实体类
 */
@Data
@TableName("delivery")
public class Delivery {
    
    /**
     * 主键自增 - 投递ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 学生ID - 外键(sys_user.id)
     */
    private Long studentId;
    
    /**
     * 岗位ID - 外键(job.id)
     */
    private Long jobId;
    
    /**
     * 投递时的简历版本快照
     */
    private String resumeVersion;
    
    /**
     * 状态：0=已投递，1=企业已查看，2=待面试，3=已录用，4=不合适
     */
    private Integer status;
    
    /**
     * 面试时间
     */
    private LocalDateTime interviewTime;
    
    /**
     * 面试地点
     */
    private String interviewLocation;
    
    /**
     * 企业反馈
     */
    private String feedback;
    
    /**
     * 投递时间
     */
    private LocalDateTime createTime;
    
    /**
     * 逻辑删除标志
     */
    @TableLogic
    private Integer deleted;
}
