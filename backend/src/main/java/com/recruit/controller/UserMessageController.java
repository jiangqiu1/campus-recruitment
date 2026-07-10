package com.recruit.controller;

import com.recruit.entity.UserMessage;
import com.recruit.service.UserMessageService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用消息通知控制器（管理员/教师/HR 端）
 */
@RestController
@RequestMapping("/user-messages")
public class UserMessageController extends BaseController {

    @Autowired
    private UserMessageService userMessageService;

    /**
     * 获取当前用户的消息列表
     */
    @GetMapping
    public Result<List<UserMessage>> getMessages() {
        Long userId = getCurrentUserId();
        return Result.success(userMessageService.getMessages(userId));
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Integer>> getUnreadCount() {
        Long userId = getCurrentUserId();
        int count = userMessageService.getUnreadCount(userId);
        Map<String, Integer> result = new HashMap<>();
        result.put("count", count);
        return Result.success(result);
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/{id}/read")
    public Result<String> markAsRead(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        boolean ok = userMessageService.markAsRead(id, userId);
        return ok ? Result.success("已标记为已读") : Result.error("消息不存在");
    }

    /**
     * 标记全部已读
     */
    @PutMapping("/read-all")
    public Result<String> markAllRead() {
        Long userId = getCurrentUserId();
        userMessageService.markAllRead(userId);
        return Result.success("已全部标记为已读");
    }
}
