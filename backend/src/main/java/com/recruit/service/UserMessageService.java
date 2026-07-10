package com.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.recruit.entity.UserMessage;

import java.util.List;

/**
 * 通用消息通知服务接口
 */
public interface UserMessageService extends IService<UserMessage> {

    /**
     * 获取用户消息列表
     */
    List<UserMessage> getMessages(Long userId);

    /**
     * 获取未读消息数
     */
    Integer getUnreadCount(Long userId);

    /**
     * 标记单条消息已读
     */
    boolean markAsRead(Long id, Long userId);

    /**
     * 标记全部已读
     */
    boolean markAllRead(Long userId);

    /**
     * 发送消息
     */
    UserMessage sendMessage(Long userId, String title, String content, String type, Long relatedId);
}
