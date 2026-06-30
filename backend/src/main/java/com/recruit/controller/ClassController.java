package com.recruit.controller;

import com.recruit.entity.Class;
import com.recruit.entity.Resume;
import com.recruit.entity.SysUser;
import com.recruit.service.ClassService;
import com.recruit.service.StudentClassService;
import com.recruit.service.DeliveryService;
import com.recruit.service.ResumeService;
import com.recruit.service.UserService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    
    @Autowired
    private StudentClassService studentClassService;
    
    @Autowired
    private DeliveryService deliveryService;
    
    @Autowired
    private ResumeService resumeService;
    
    /**
     * 获取所有班级列表（含学生数、就业率、投递数）
     */
    @GetMapping
    public Result<List<Class>> getAllClasses() {
        List<Class> classes = classService.list();
        // 为每个班级填充统计信息
        for (Class clazz : classes) {
            List<Long> studentIds = classService.getStudentIdsByClassId(clazz.getId());
            int studentCount = studentIds.size();
            clazz.setStudentCount(studentCount);
            
            // 计算投递数：统计班级学生的投递记录
            int deliveryCount = 0;
            int employedCount = 0;
            for (Long sid : studentIds) {
                int count = Math.toIntExact(deliveryService.lambdaQuery()
                    .eq(com.recruit.entity.Delivery::getStudentId, sid)
                    .count());
                deliveryCount += count;
                // 检查是否有已接收的投递（status=3 表示已录用）
                boolean hasOffer = deliveryService.lambdaQuery()
                    .eq(com.recruit.entity.Delivery::getStudentId, sid)
                    .eq(com.recruit.entity.Delivery::getStatus, 3)
                    .count() > 0;
                if (hasOffer) {
                    employedCount++;
                }
            }
            clazz.setDeliveryCount(deliveryCount);
            clazz.setEmploymentRate(studentCount > 0 ? String.format("%.0f%%", employedCount * 100.0 / studentCount) : "0%");
        }
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
    public Result<List<Map<String, Object>>> getClassStudents(@PathVariable Long classId) {
        List<Long> studentIds = classService.getStudentIdsByClassId(classId);
        List<Map<String, Object>> students = studentIds.stream()
                .map(studentId -> {
                    SysUser user = userService.getById(studentId);
                    if (user == null) return null;
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("username", user.getUsername());
                    map.put("realName", user.getRealName());
                    map.put("phone", user.getPhone() != null ? user.getPhone() : "");
                    // 计算简历完整度
                    Resume resume = resumeService.selectByStudentId(studentId);
                    int resumeComplete = 0;
                    if (resume != null) {
                        if (resume.getEducation() != null && !resume.getEducation().isEmpty()) resumeComplete += 40;
                        if (resume.getSkills() != null && !resume.getSkills().isEmpty()) resumeComplete += 30;
                        if (resume.getSelfEvaluation() != null && !resume.getSelfEvaluation().isEmpty()) resumeComplete += 30;
                    }
                    map.put("resumeComplete", resumeComplete);
                    // 计算投递数
                    Integer deliveryCount = deliveryService.countByStudentId(studentId);
                    map.put("deliveryCount", deliveryCount != null ? deliveryCount : 0);
                    return map;
                })
                .filter(map -> map != null)
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
