package com.recruit.controller;

import com.recruit.entity.Class;
import com.recruit.entity.SysUser;
import com.recruit.service.ClassService;
import com.recruit.service.UserService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 班级管理控制器
 */
@RestController
@RequestMapping("/classes")
public class ClassController {
    
    @Autowired
    private ClassService classService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取所有班级列表
     */
    @GetMapping
    public Result<List<Class>> getAllClasses() {
        List<Class> classes = classService.list();
        return Result.success(classes);
    }
    
    /**
     * 根据ID获取班级
     */
    @GetMapping("/{id}")
    public Result<Class> getClassById(@PathVariable Long id) {
        Class clazz = classService.getById(id);
        if (clazz == null) {
            return Result.error(404, "班级不存在");
        }
        return Result.success(clazz);
    }
    
    /**
     * 根据教师ID查询班级
     */
    @GetMapping("/by-teacher/{teacherId}")
    public Result<List<Class>> getClassesByTeacherId(@PathVariable Long teacherId) {
        List<Class> classes = classService.selectByTeacherId(teacherId);
        return Result.success(classes);
    }
    
    /**
     * 根据专业名称查询班级
     */
    @GetMapping("/by-major/{major}")
    public Result<List<Class>> getClassesByMajor(@PathVariable String major) {
        List<Class> classes = classService.selectByMajor(major);
        return Result.success(classes);
    }
    
    /**
     * 根据年级查询班级
     */
    @GetMapping("/by-grade/{grade}")
    public Result<List<Class>> getClassesByGrade(@PathVariable String grade) {
        List<Class> classes = classService.selectByGrade(grade);
        return Result.success(classes);
    }
    
    /**
     * 创建班级
     */
    @PostMapping
    public Result<String> createClass(@RequestBody Class clazz) {
        classService.save(clazz);
        return Result.success("班级创建成功");
    }
    
    /**
     * 更新班级
     */
    @PutMapping("/{id}")
    public Result<String> updateClass(@PathVariable Long id, @RequestBody Class clazz) {
        Class existClass = classService.getById(id);
        if (existClass == null) {
            return Result.error(404, "班级不存在");
        }
        clazz.setId(id);
        classService.updateById(clazz);
        return Result.success("班级更新成功");
    }
    
    /**
     * 删除班级（逻辑删除）
     * 使用 MyBatis-Plus 内置 deleteById，自动转 UPDATE SET deleted=1
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteClass(@PathVariable Long id) {
        Class clazz = classService.getById(id);
        if (clazz == null) {
            return Result.error(404, "班级不存在");
        }
        boolean success = classService.removeById(id);
        if (!success) {
            return Result.error("删除班级失败");
        }
        return Result.success("班级删除成功");
    }
    
    /**
     * 添加学生到班级
     */
    @PostMapping("/{classId}/students")
    public Result<String> addStudentToClass(
            @PathVariable Long classId,
            @RequestBody Map<String, Long> params) {
        
        Long studentId = params.get("studentId");
        if (studentId == null) {
            return Result.error("studentId不能为空");
        }
        boolean success = classService.addStudentToClass(classId, studentId);
        if (!success) {
            return Result.error("添加学生失败");
        }
        return Result.success("学生添加成功");
    }
    
    /**
     * 从班级移除学生
     */
    @DeleteMapping("/{classId}/students/{studentId}")
    public Result<String> removeStudentFromClass(
            @PathVariable Long classId,
            @PathVariable Long studentId) {
        
        boolean success = classService.removeStudentFromClass(classId, studentId);
        if (!success) {
            return Result.error("移除学生失败");
        }
        return Result.success("学生移除成功");
    }
    
    /**
     * 获取班级学生列表
     */
    @GetMapping("/{classId}/students")
    public Result<List<SysUser>> getClassStudents(@PathVariable Long classId) {
        List<Long> studentIds = classService.getStudentIdsByClassId(classId);
        List<SysUser> students = studentIds.stream()
                .map(studentId -> userService.getById(studentId))
                .filter(student -> student != null)
                .collect(Collectors.toList());
        return Result.success(students);
    }
    
    /**
     * 批量添加学生到班级
     */
    @PostMapping("/{classId}/students/batch")
    public Result<Map<String, Integer>> batchAddStudentsToClass(
            @PathVariable Long classId,
            @RequestBody Map<String, List<Long>> params) {
        
        List<Long> studentIds = params.get("studentIds");
        if (studentIds == null || studentIds.isEmpty()) {
            return Result.error("studentIds不能为空");
        }
        int count = classService.batchAddStudentsToClass(classId, studentIds);
        Map<String, Integer> result = new java.util.HashMap<>();
        result.put("successCount", count);
        result.put("totalCount", studentIds.size());
        return Result.success("批量添加完成", result);
    }
    
    /**
     * 批量从班级移除学生
     */
    @DeleteMapping("/{classId}/students/batch")
    public Result<Map<String, Integer>> batchRemoveStudentsFromClass(
            @PathVariable Long classId,
            @RequestBody Map<String, List<Long>> params) {
        
        List<Long> studentIds = params.get("studentIds");
        if (studentIds == null || studentIds.isEmpty()) {
            return Result.error("studentIds不能为空");
        }
        int count = classService.batchRemoveStudentsFromClass(classId, studentIds);
        Map<String, Integer> result = new java.util.HashMap<>();
        result.put("successCount", count);
        result.put("totalCount", studentIds.size());
        return Result.success("批量移除完成", result);
    }
}
