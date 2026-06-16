package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 岗位表实体类
 */
@Data
@TableName("job")
public class Job {
    
    /**
     * 主键自增 - 岗位ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 所属企业ID - 外键(company.id)
     */
    private Long companyId;
    
    /**
     * 岗位名称
     */
    private String title;
    
    /**
     * 薪资范围（如"8k-12k"）
     */
    private String salaryRange;
    
    /**
     * 学历要求
     */
    private String education;
    
    /**
     * 工作地点
     */
    private String location;
    
    /**
     * 岗位描述
     */
    private String description;
    
    /**
     * 任职要求
     */
    private String requirement;
    
    /**
     * 截止日期
     */
    private LocalDate deadline;
    
    /**
     * 状态：0=草稿，1=已发布，2=已关闭，3=暂停
     */
    private Integer status;
    
    /**
     * 发布者ID（教师或HR）- 外键(sys_user.id)
     */
    private Long createdBy;
    
    /**
     * 浏览次数
     */
    private Integer viewCount;
    
    /**
     * 专属二维码图片路径
     */
    private String qrCodeUrl;
    
    /**
     * 唯一追踪ID
     */
    private String traceId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 从岗位描述中提取的关键词（JSON）
     */
    private String aiKeywords;
    
    /**
     * 所需技能标签（JSON）
     */
    private String requiredSkills;
    
    /**
     * 逻辑删除标志
     */
    @TableLogic
    private Integer deleted;
}
