package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通用消息通知实体（管理员/教师/HR 端）
 */
@Data
@TableName("user_message")
public class UserMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收者用户ID */
    private Long userId;

    /** 消息标题 */
    private String title;

    /** 消息内容 */
    private String content;

    /** 消息类型: system / audit / delivery / interview */
    private String type;

    /** 是否已读: 0=未读, 1=已读 */
    private Integer isRead;

    /** 关联对象ID */
    private Long relatedId;

    /** 创建时间 */
    private LocalDateTime createTime;
}
