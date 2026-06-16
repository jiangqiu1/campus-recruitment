package com.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruit.entity.Class;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 班级表 Mapper 接口
 */
@Mapper
public interface ClassMapper extends BaseMapper<Class> {
    
    /**
     * 根据教师ID查询班级
     * 
     * @param teacherId 教师ID
     * @return 班级列表
     */
    @Select("SELECT * FROM class WHERE teacher_id = #{teacherId} AND deleted = 0")
    List<Class> selectByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 根据专业名称查询班级
     * 
     * @param major 专业名称
     * @return 班级列表
     */
    @Select("SELECT * FROM class WHERE major = #{major} AND deleted = 0")
    List<Class> selectByMajor(@Param("major") String major);
    
    /**
     * 根据年级查询班级
     * 
     * @param grade 年级（如2023级）
     * @return 班级列表
     */
    @Select("SELECT * FROM class WHERE grade = #{grade} AND deleted = 0")
    List<Class> selectByGrade(@Param("grade") String grade);
}
