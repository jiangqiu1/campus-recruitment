package com.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruit.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 恢复逻辑删除的收藏记录（绕过 MyBatis-Plus 的 deleted=0 过滤）
     */
    @Update("UPDATE favorite SET deleted = 0, create_time = NOW() WHERE student_id = #{studentId} AND job_id = #{jobId}")
    int restoreFavorite(@Param("studentId") Long studentId, @Param("jobId") Long jobId);
}
