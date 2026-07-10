package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recruit.entity.UserMessage;
import com.recruit.mapper.UserMessageMapper;
import com.recruit.service.UserMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通用消息通知服务实现
 */
@Service
public class UserMessageServiceImpl extends ServiceImpl<UserMessageMapper, UserMessage> implements UserMessageService {

    @Autowired
    private UserMessageMapper userMessageMapper;

    @Override
    public List<UserMessage> getMessages(Long userId) {
        return userMessageMapper.selectByUserId(userId);
    }

    @Override
    public Integer getUnreadCount(Long userId) {
        return userMessageMapper.countUnread(userId);
    }

    @Override
    public boolean markAsRead(Long id, Long userId) {
        return userMessageMapper.markAsRead(id, userId) > 0;
    }

    @Override
    public boolean markAllRead(Long userId) {
        return userMessageMapper.markAllRead(userId) > 0;
    }

    @Override
    public UserMessage sendMessage(Long userId, String title, String content, String type, Long relatedId) {
        UserMessage msg = new UserMessage();
        msg.setUserId(userId);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setType(type != null ? type : "system");
        msg.setIsRead(0);
        msg.setRelatedId(relatedId);
        msg.setCreateTime(LocalDateTime.now());
        save(msg);
        return msg;
    }
}
