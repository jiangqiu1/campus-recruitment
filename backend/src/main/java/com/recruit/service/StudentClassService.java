package com.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.recruit.entity.StudentClass;

import java.util.List;

/**
 * 学生-班级关联表 服务接口
 */
public interface StudentClassService extends IService<StudentClass> {
    
    /**
     * 根据学生ID查询关联记录
     * 
     * @param studentId 学生ID
     * @return 关联记录列表
     */
    List<StudentClass> selectByStudentId(Long studentId);
    
    /**
     * 根据班级ID查询关联记录
     * 
     * @param classId 班级ID
     * @return 关联记录列表
     */
    List<StudentClass> selectByClassId(Long classId);
    
    /**
     * 批量添加学生到班级
     * 
     * @param classId 班级ID
     * @param studentIds 学生ID列表
     * @return 成功添加的数量
     */
    int batchAddStudentsToClass(Long classId, List<Long> studentIds);
    
    /**
     * 批量从班级移除学生
     * 
     * @param classId 班级ID
     * @param studentIds 学生ID列表
     * @return 成功移除的数量
     */
    int batchRemoveStudentsFromClass(Long classId, List<Long> studentIds);
}
