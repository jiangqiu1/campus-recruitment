package com.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruit.entity.Resume;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学生简历表 Mapper 接口
 */
@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
    
    /**
     * 根据学生ID查询简历
     * 
     * @param studentId 学生ID
     * @return 简历实体
     */
    @Select("SELECT * FROM resume WHERE student_id = #{studentId} AND deleted = 0")
    Resume selectByStudentId(@Param("studentId") Long studentId);
    
    /**
     * 根据学生ID查询默认简历
     * 
     * @param studentId 学生ID
     * @return 默认简历实体
     */
    @Select("SELECT * FROM resume WHERE student_id = #{studentId} AND is_default = 1 AND deleted = 0")
    Resume selectDefaultByStudentId(@Param("studentId") Long studentId);
    
    /**
     * 设置默认简历（将该学生的其他简历设为非默认）
     * 
     * @param studentId 学生ID
     * @param resumeId 要设为默认的简历ID
     * @return 影响行数
     */
    int resetDefaultFlag(@Param("studentId") Long studentId, @Param("resumeId") Long resumeId);
}
