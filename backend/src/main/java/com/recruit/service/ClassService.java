package com.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.recruit.entity.Class;

import java.util.List;

/**
 * 班级表 服务接口
 */
public interface ClassService extends IService<Class> {
    
    /**
     * 根据教师ID查询班级
     * 
     * @param teacherId 教师ID
     * @return 班级列表
     */
    List<Class> selectByTeacherId(Long teacherId);
    
    /**
     * 根据专业名称查询班级
     * 
     * @param major 专业名称
     * @return 班级列表
     */
    List<Class> selectByMajor(String major);
    
    /**
     * 根据年级查询班级
     * 
     * @param grade 年级（如2023级）
     * @return 班级列表
     */
    List<Class> selectByGrade(String grade);
    
    /**
     * 添加学生到班级
     * 
     * @param classId 班级ID
     * @param studentId 学生ID
     * @return 是否成功
     */
    boolean addStudentToClass(Long classId, Long studentId);
    
    /**
     * 从班级移除学生
     * 
     * @param classId 班级ID
     * @param studentId 学生ID
     * @return 是否成功
     */
    boolean removeStudentFromClass(Long classId, Long studentId);
    
    /**
     * 批量添加学生到班级
     * 
     * @param classId 班级ID
     * @param studentIds 学生ID列表
     * @return 成功添加的学生数量
     */
    int batchAddStudentsToClass(Long classId, List<Long> studentIds);
    
    /**
     * 批量从班级移除学生
     * 
     * @param classId 班级ID
     * @param studentIds 学生ID列表
     * @return 成功移除的学生数量
     */
    int batchRemoveStudentsFromClass(Long classId, List<Long> studentIds);
    
    /**
     * 查询班级学生列表
     * 
     * @param classId 班级ID
     * @return 学生ID列表
     */
    List<Long> getStudentIdsByClassId(Long classId);
    
    /**
     * 查询学生所在班级
     * 
     * @param studentId 学生ID
     * @return 班级列表
     */
    List<Class> getClassesByStudentId(Long studentId);
}
