package com.recruit.controller;

import com.recruit.entity.Message;
import com.recruit.service.MessageService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * 消息通知控制器 - 学生端
 */
@RestController
@RequestMapping("/messages")
public class MessageController extends BaseController {

    @Autowired
    private MessageService messageService;

    /**
     * 获取学生的消息列表
     */
    @GetMapping
    public Result<List<Message>> getMessages(@RequestParam(required = false) Long studentId,
                                              @RequestParam(required = false) Integer type) {
        // 学生强制只读自己的消息，防传参越权
        if (Objects.equals(getCurrentRole(), 0)) {
            studentId = getCurrentUserId();
        }
        if (studentId == null) {
            return Result.success(List.of());
        }
        var wrapper = messageService.lambdaQuery()
                .eq(Message::getStudentId, studentId)
                .orderByDesc(Message::getCreateTime);
        if (type != null) {
            wrapper.eq(Message::getType, type);
        }
        return Result.success(wrapper.list());
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/{id}/read")
    public Result<String> readMessage(@PathVariable Long id) {
        Message msg = messageService.getById(id);
        if (msg == null) {
            return Result.error(404, "消息不存在");
        }
        if (Objects.equals(getCurrentRole(), 0) && !Objects.equals(msg.getStudentId(), getCurrentUserId())) {
            return Result.error(403, "无权操作他人消息");
        }
        msg.setIsRead(1);
        messageService.updateById(msg);
        return Result.success("ok");
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount(@RequestParam Long studentId) {
        if (Objects.equals(getCurrentRole(), 0)) {
            studentId = getCurrentUserId();
        }
        int count = messageService.lambdaQuery()
                .eq(Message::getStudentId, studentId)
                .eq(Message::getIsRead, 0)
                .count().intValue();
        return Result.success(count);
    }
}
