package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业表实体类
 */
@Data
@TableName("company")
public class Company {
    
    /**
     * 主键自增 - 企业ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 企业全称 - 唯一
     */
    private String name;
    
    /**
     * 简称
     */
    private String shortName;
    
    /**
     * 营业执照图片路径
     */
    private String licenseUrl;
    
    /**
     * 行业
     */
    private String industry;
    
    /**
     * 地址
     */
    private String address;
    
    /**
     * 联系人
     */
    private String contactPerson;
    
    /**
     * 联系电话（加密）
     */
    private String contactPhone;
    
    /**
     * 审核状态：0=待审核，1=通过，2=拒绝
     */
    private Integer status;

    /**
     * 合作等级：0=潜在，1=合作中，2=核心，3=已流失
     */
    private Integer cooperationLevel;
    
    /**
     * 最近一次招聘时间
     */
    private LocalDateTime lastRecruitTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 逻辑删除标志
     */
    @TableLogic
    private Integer deleted;
}
