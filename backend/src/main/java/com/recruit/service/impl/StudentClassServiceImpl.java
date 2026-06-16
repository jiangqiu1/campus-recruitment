package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recruit.entity.StudentClass;
import com.recruit.mapper.StudentClassMapper;
import com.recruit.service.StudentClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 学生-班级关联表 服务实现类
 */
@Service
public class StudentClassServiceImpl extends ServiceImpl<StudentClassMapper, StudentClass> implements StudentClassService {
    
    @Autowired
    private StudentClassMapper studentClassMapper;
    
    @Override
    public List<StudentClass> selectByStudentId(Long studentId) {
        return studentClassMapper.selectByStudentId(studentId);
    }
    
    @Override
    public List<StudentClass> selectByClassId(Long classId) {
        return studentClassMapper.selectByClassId(classId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAddStudentsToClass(Long classId, List<Long> studentIds) {
        int count = 0;
        for (Long studentId : studentIds) {
            // 检查是否已存在关联
            List<StudentClass> existList = studentClassMapper.selectByClassId(classId);
            boolean alreadyInClass = existList.stream()
                    .anyMatch(sc -> sc.getStudentId().equals(studentId));
            
            if (!alreadyInClass) {
                StudentClass studentClass = new StudentClass();
                studentClass.setStudentId(studentId);
                studentClass.setClassId(classId);
                save(studentClass);
                count++;
            }
        }
        return count;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchRemoveStudentsFromClass(Long classId, List<Long> studentIds) {
        int count = 0;
        for (Long studentId : studentIds) {
            int rows = studentClassMapper.deleteByStudentIdAndClassId(studentId, classId);
            if (rows > 0) {
                count++;
            }
        }
        return count;
    }
}
