package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学校字典实体类
 * 统一学校名称，供学生资料等场景下拉选择
 */
@Data
@TableName("school_dict")
public class SchoolDict {

    /**
     * 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学校名称
     */
    private String name;

    /**
     * 省份
     */
    private String province;

    /**
     * 状态 1启用 0停用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
