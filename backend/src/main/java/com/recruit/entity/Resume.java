package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生简历表实体类
 */
@Data
@TableName("resume")
public class Resume {
    
    /**
     * 主键自增 - 简历ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 学生ID - 外键(sys_user.id)
     */
    private Long studentId;
    
    /**
     * 教育经历（JSON）
     */
    private String education;
    
    /**
     * 实习经历（JSON）
     */
    private String internship;
    
    /**
     * 项目经历（JSON，与 internship 结构一致）
     */
    private String project;
    
    /**
     * 技能证书
     */
    private String skills;
    
    /**
     * 自我评价
     */
    private String selfEvaluation;
    
    /**
     * 上传的PDF简历路径
     */
    private String pdfUrl;
    
    /**
     * 是否为当前默认简历：0=否，1=是
     */
    private Integer isDefault;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 从简历中提取的技能标签（JSON）
     */
    private String skillTags;
    
    /**
     * 求职意向
     */
    private String jobTarget;

    /**
     * AI简历分析结果（JSON）
     */
    private String aiAnalysis;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    private Integer deleted;
}
