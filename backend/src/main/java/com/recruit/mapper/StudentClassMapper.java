package com.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruit.entity.StudentClass;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学生-班级关联表 Mapper 接口
 */
@Mapper
public interface StudentClassMapper extends BaseMapper<StudentClass> {
    
    /**
     * 根据学生ID查询关联记录
     * 
     * @param studentId 学生ID
     * @return 关联记录列表
     */
    @Select("SELECT * FROM student_class WHERE student_id = #{studentId}")
    List<StudentClass> selectByStudentId(@Param("studentId") Long studentId);
    
    /**
     * 根据班级ID查询关联记录
     * 
     * @param classId 班级ID
     * @return 关联记录列表
     */
    @Select("SELECT * FROM student_class WHERE class_id = #{classId}")
    List<StudentClass> selectByClassId(@Param("classId") Long classId);
    
    /**
     * 删除学生-班级关联
     * 
     * @param studentId 学生ID
     * @param classId 班级ID
     * @return 影响行数
     */
    int deleteByStudentIdAndClassId(@Param("studentId") Long studentId, @Param("classId") Long classId);
}
