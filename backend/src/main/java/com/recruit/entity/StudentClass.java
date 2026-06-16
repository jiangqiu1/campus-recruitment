package com.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 学生-班级关联表实体类
 */
@Data
@TableName("student_class")
public class StudentClass {
    
    /**
     * 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 学生ID - 外键(sys_user.id)
     */
    private Long studentId;
    
    /**
     * 班级ID - 外键(class.id)
     */
    private Long classId;
}
