package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级表实体类
 */
@Data
@TableName("class")
public class Class {
    
    /**
     * 主键自增 - 班级ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 班级名称
     */
    private String name;
    
    /**
     * 班主任/教师ID - 外键(sys_user.id)
     */
    private Long teacherId;

    /**
     * 班主任姓名（列表展示用，非表字段）
     */
    @TableField(exist = false)
    private String teacherName;
    
    /**
     * 专业名称
     */
    private String major;
    
    /**
     * 年级（如2023级）
     */
    private String grade;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 逻辑删除标志
     */
    @TableLogic
    private Integer deleted;
    
    /**
     * 学生人数（非数据库字段）
     */
    @TableField(exist = false)
    private Integer studentCount;
    
    /**
     * 就业率（非数据库字段）- 返回纯数字，如 88 表示 88%
     */
    @TableField(exist = false)
    private Integer employmentRate;
    
    /**
     * 投递数（非数据库字段）
     */
    @TableField(exist = false)
    private Integer deliveryCount;
}
