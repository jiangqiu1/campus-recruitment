package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recruit.entity.Class;
import com.recruit.entity.StudentClass;
import com.recruit.mapper.ClassMapper;
import com.recruit.mapper.StudentClassMapper;
import com.recruit.service.ClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 班级表 服务实现类
 */
@Service
public class ClassServiceImpl extends ServiceImpl<ClassMapper, Class> implements ClassService {
    
    @Autowired
    private ClassMapper classMapper;
    
    @Autowired
    private StudentClassMapper studentClassMapper;
    
    @Override
    public List<Class> selectByTeacherId(Long teacherId) {
        return classMapper.selectByTeacherId(teacherId);
    }
    
    @Override
    public List<Class> selectByMajor(String major) {
        return classMapper.selectByMajor(major);
    }
    
    @Override
    public List<Class> selectByGrade(String grade) {
        return classMapper.selectByGrade(grade);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addStudentToClass(Long classId, Long studentId) {
        // 1. 检查班级是否存在
        Class clazz = getById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        
        // 2. 检查是否已存在关联
        List<StudentClass> existList = studentClassMapper.selectByClassId(classId);
        boolean alreadyInClass = existList.stream()
                .anyMatch(sc -> sc.getStudentId().equals(studentId));
        
        if (alreadyInClass) {
            throw new RuntimeException("该学生已在班级中");
        }
        
        // 3. 创建关联
        StudentClass studentClass = new StudentClass();
        studentClass.setStudentId(studentId);
        studentClass.setClassId(classId);
        
        studentClassMapper.insert(studentClass);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeStudentFromClass(Long classId, Long studentId) {
        int rows = studentClassMapper.deleteByStudentIdAndClassId(studentId, classId);
        return rows > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAddStudentsToClass(Long classId, List<Long> studentIds) {
        // 1. 检查班级是否存在
        Class clazz = getById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        
        // 2. 获取班级已有学生
        List<StudentClass> existList = studentClassMapper.selectByClassId(classId);
        List<Long> alreadyInClassIds = new ArrayList<>();
        for (StudentClass sc : existList) {
            alreadyInClassIds.add(sc.getStudentId());
        }
        
        // 3. 批量添加不在班级中的学生
        int count = 0;
        for (Long studentId : studentIds) {
            if (!alreadyInClassIds.contains(studentId)) {
                StudentClass studentClass = new StudentClass();
                studentClass.setStudentId(studentId);
                studentClass.setClassId(classId);
                studentClassMapper.insert(studentClass);
                count++;
            }
        }
        
        return count;  // ✅ 返回成功数量
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchRemoveStudentsFromClass(Long classId, List<Long> studentIds) {
        // 批量移除学生
        int count = 0;
        for (Long studentId : studentIds) {
            int rows = studentClassMapper.deleteByStudentIdAndClassId(studentId, classId);
            if (rows > 0) {
                count++;
            }
        }
        
        return count;  // ✅ 返回成功数量
    }
    
    @Override
    public List<Long> getStudentIdsByClassId(Long classId) {
        List<StudentClass> studentClasses = studentClassMapper.selectByClassId(classId);
        List<Long> studentIds = new ArrayList<>();
        for (StudentClass sc : studentClasses) {
            studentIds.add(sc.getStudentId());
        }
        return studentIds;
    }
    
    @Override
    public List<Class> getClassesByStudentId(Long studentId) {
        List<StudentClass> studentClasses = studentClassMapper.selectByStudentId(studentId);
        List<Class> classes = new ArrayList<>();
        for (StudentClass sc : studentClasses) {
            Class clazz = getById(sc.getClassId());
            if (clazz != null) {
                classes.add(clazz);
            }
        }
        return classes;
    }
}
