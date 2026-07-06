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
import com.recruit.annotation.LogOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 班级管理控制器
 */
@RestController
@RequestMapping("/classes")
public class ClassController extends BaseController {
    
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
     * 获取班级列表（按角色过滤数据范围）
     * 教师：仅返回自己管辖的班级
     * 管理员：返回全部班级
     */
    @GetMapping
    public Result<List<Class>> getAllClasses() {
        Integer role = getCurrentRole();
        List<Class> classes;
        
        if (Objects.equals(role, 1)) {
            // 教师：仅返回自己管辖的班级
            Long teacherId = getCurrentUserId();
            classes = classService.selectByTeacherId(teacherId);
        } else {
            // 管理员或其他角色：返回全部
            classes = classService.list();
        }
        // 为每个班级填充统计信息（批量查询消除 N+1）
        // 1. 收集所有班级的学生ID
        Map<Long, List<Long>> classStudentIds = new HashMap<>();
        Set<Long> allStudentIds = new HashSet<>();
        for (Class clazz : classes) {
            List<Long> ids = classService.getStudentIdsByClassId(clazz.getId());
            classStudentIds.put(clazz.getId(), ids);
            if (ids != null) allStudentIds.addAll(ids);
        }

        // 2. 批量查询所有相关学生的投递记录
        Map<Long, List<com.recruit.entity.Delivery>> studentDeliveries = new HashMap<>();
        if (!allStudentIds.isEmpty()) {
            List<com.recruit.entity.Delivery> allDeliveries = deliveryService.lambdaQuery()
                    .in(com.recruit.entity.Delivery::getStudentId, allStudentIds)
                    .list();
            for (com.recruit.entity.Delivery d : allDeliveries) {
                studentDeliveries.computeIfAbsent(d.getStudentId(), k -> new ArrayList<>()).add(d);
            }
        }

        // 3. 计算每个班级的统计
        for (Class clazz : classes) {
            List<Long> studentIds = classStudentIds.getOrDefault(clazz.getId(), new ArrayList<>());
            clazz.setStudentCount(studentIds.size());

            int deliveryCount = 0;
            int employedCount = 0;
            for (Long sid : studentIds) {
                List<com.recruit.entity.Delivery> deliveries = studentDeliveries.getOrDefault(sid, new ArrayList<>());
                deliveryCount += deliveries.size();
                boolean hasOffer = deliveries.stream().anyMatch(d -> d.getStatus() != null && d.getStatus() == 3);
                if (hasOffer) employedCount++;
            }
            clazz.setDeliveryCount(deliveryCount);
            clazz.setEmploymentRate(studentIds.size() > 0 ? (int) Math.round(employedCount * 100.0 / studentIds.size()) : 0);
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
        // 校验：非管理员只能查自己的班级
        Integer role = getCurrentRole();
        if (!Objects.equals(role, 3) && !Objects.equals(teacherId, getCurrentUserId())) {
            return Result.error(403, "无权限访问该数据");
        }
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
     * 创建班级（自动关联当前登录教师）
     */
    @LogOperation("创建班级")
    @PostMapping
    public Result<String> createClass(@RequestBody Class clazz) {
        requireTeacher();
        // 自动关联当前教师
        clazz.setTeacherId(getCurrentUserId());
        classService.save(clazz);
        return Result.success("班级创建成功");
    }
    
    /**
     * 更新班级（校验所属权）
     */
    @LogOperation("更新班级")
    @PutMapping("/{id}")
    public Result<String> updateClass(@PathVariable Long id, @RequestBody Class clazz) {
        Class existClass = classService.getById(id);
        if (existClass == null) {
            return Result.error(404, "班级不存在");
        }
        // 校验所属权：非管理员只能更新自己的班级
        Integer role = getCurrentRole();
        if (!Objects.equals(role, 3) && !Objects.equals(existClass.getTeacherId(), getCurrentUserId())) {
            return Result.error(403, "无权修改其他教师的班级");
        }
        clazz.setId(id);
        classService.updateById(clazz);
        return Result.success("班级更新成功");
    }
    
    /**
     * 删除班级（逻辑删除）
     * 非管理员只能删除自己的班级
     */
    @LogOperation("删除班级")
    @DeleteMapping("/{id}")
    public Result<String> deleteClass(@PathVariable Long id) {
        Class clazz = classService.getById(id);
        if (clazz == null) {
            return Result.error(404, "班级不存在");
        }
        // 校验所属权
        Integer role = getCurrentRole();
        if (!Objects.equals(role, 3) && !Objects.equals(clazz.getTeacherId(), getCurrentUserId())) {
            return Result.error(403, "无权删除其他教师的班级");
        }
        boolean success = classService.removeById(id);
        if (!success) {
            return Result.error("删除班级失败");
        }
        return Result.success("班级删除成功");
    }
    
    /**
     * 添加学生到班级（校验班级所属权）
     */
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/{classId}/students")
    public Result<String> addStudentToClass(
            @PathVariable Long classId,
            @RequestBody Map<String, Long> params) {
        checkClassOwnership(classId);
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
     * 从班级移除学生（校验班级所属权）
     */
    @Transactional(rollbackFor = Exception.class)
    @DeleteMapping("/{classId}/students/{studentId}")
    public Result<String> removeStudentFromClass(
            @PathVariable Long classId,
            @PathVariable Long studentId) {
        checkClassOwnership(classId);
        boolean success = classService.removeStudentFromClass(classId, studentId);
        if (!success) {
            return Result.error("移除学生失败");
        }
        return Result.success("学生移除成功");
    }
    
    /**
     * 获取班级学生列表（校验班级所属权）
     */
    @GetMapping("/{classId}/students")
    public Result<List<Map<String, Object>>> getClassStudents(@PathVariable Long classId) {
        checkClassOwnership(classId);
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
     * 批量添加学生到班级（校验班级所属权）
     */
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/{classId}/students/batch")
    public Result<Map<String, Integer>> batchAddStudentsToClass(
            @PathVariable Long classId,
            @RequestBody Map<String, List<Long>> params) {
        checkClassOwnership(classId);
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
     * 批量从班级移除学生（校验班级所属权）
     */
    @Transactional(rollbackFor = Exception.class)
    @DeleteMapping("/{classId}/students/batch")
    public Result<Map<String, Integer>> batchRemoveStudentsFromClass(
            @PathVariable Long classId,
            @RequestBody Map<String, List<Long>> params) {
        checkClassOwnership(classId);
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
    
    /**
     * 校验当前教师是否有权限操作该班级
     */
    private void checkClassOwnership(Long classId) {
        Integer role = getCurrentRole();
        // 管理员跳过校验
        if (Objects.equals(role, 3)) return;
        
        Class clazz = classService.getById(classId);
        if (clazz == null) {
            throw new com.recruit.exception.BusinessException(404, "班级不存在");
        }
        if (!Objects.equals(clazz.getTeacherId(), getCurrentUserId())) {
            throw new com.recruit.exception.BusinessException(403, "无权操作其他教师的班级");
        }
    }
}
