package com.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruit.entity.UserMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 通用消息通知 Mapper
 */
@Mapper
public interface UserMessageMapper extends BaseMapper<UserMessage> {

    /**
     * 查询用户未读消息数量
     */
    @Select("SELECT COUNT(*) FROM user_message WHERE user_id = #{userId} AND is_read = 0")
    Integer countUnread(@Param("userId") Long userId);

    /**
     * 查询用户消息列表（按时间倒序）
     */
    @Select("SELECT * FROM user_message WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<UserMessage> selectByUserId(@Param("userId") Long userId);

    /**
     * 标记消息为已读
     */
    @Update("UPDATE user_message SET is_read = 1 WHERE id = #{id} AND user_id = #{userId}")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 批量标记已读
     */
    @Update("UPDATE user_message SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllRead(@Param("userId") Long userId);
}
