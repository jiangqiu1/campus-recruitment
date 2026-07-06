package com.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruit.entity.BrowseHistory;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BrowseHistoryMapper extends BaseMapper<BrowseHistory> {

    @Select("SELECT * FROM browse_history WHERE student_id = #{studentId} ORDER BY create_time DESC")
    List<BrowseHistory> selectByStudentId(@Param("studentId") Long studentId);
}
