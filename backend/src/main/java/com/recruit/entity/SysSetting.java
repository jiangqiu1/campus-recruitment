package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统设置实体类
 */
@Data
@TableName("sys_settings")
public class SysSetting {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分组标识: basic / security / notification */
    private String groupKey;

    /** 配置键名 */
    private String settingKey;

    /** 配置值（JSON 字符串） */
    private String settingValue;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
