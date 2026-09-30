package com.recruit.controller;

import com.recruit.entity.Class;
import com.recruit.entity.Resume;
import com.recruit.entity.StudentClass;
import com.recruit.entity.SysUser;
import com.recruit.service.ClassService;
import com.recruit.service.StudentClassService;
import com.recruit.service.DeliveryService;
import com.recruit.service.ResumeService;
import com.recruit.service.UserService;
import com.recruit.utils.Result;
import com.recruit.utils.AESUtil;
import com.recruit.annotation.LogOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
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

    @Autowired
    private AESUtil aesUtil;
    
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
            classes = classService.selectByTeacherId(getCurrentUserId());
        } else {
            // 管理员或其他角色：返回全部
            classes = classService.list();
        }
        fillClassStatistics(classes);
        return Result.success(classes);
    }

    /**
     * 为班级列表填充学生数、投递数、就业率等统计字段
     */
    private void fillClassStatistics(List<Class> classes) {
        if (classes == null || classes.isEmpty()) return;
        Map<Long, List<Long>> classStudentIds = new HashMap<>();
        Set<Long> allStudentIds = new HashSet<>();
        for (Class clazz : classes) {
            List<Long> ids = classService.getStudentIdsByClassId(clazz.getId());
            classStudentIds.put(clazz.getId(), ids);
            if (ids != null) allStudentIds.addAll(ids);
        }
        Map<Long, List<com.recruit.entity.Delivery>> studentDeliveries = new HashMap<>();
        if (!allStudentIds.isEmpty()) {
            List<com.recruit.entity.Delivery> allDeliveries = deliveryService.lambdaQuery()
                    .in(com.recruit.entity.Delivery::getStudentId, allStudentIds)
                    .list();
            for (com.recruit.entity.Delivery d : allDeliveries) {
                studentDeliveries.computeIfAbsent(d.getStudentId(), k -> new ArrayList<>()).add(d);
            }
        }
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
    }
    
    /**
     * 获取当前学生的班级
     */
    @GetMapping("/student/my-class")
    public Result<Map<String, Object>> getMyClass() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        List<StudentClass> relations = studentClassService.selectByStudentId(userId);
        if (relations == null || relations.isEmpty()) {
            return Result.error(404, "您尚未加入任何班级");
        }
        Long classId = relations.get(0).getClassId();
        Class clazz = classService.getById(classId);
        if (clazz == null) {
            return Result.error(404, "班级不存在");
        }
        // 查询教师姓名
        Map<String, Object> result = new HashMap<>();
        result.put("id", clazz.getId());
        result.put("name", clazz.getName());
        result.put("major", clazz.getMajor());
        result.put("grade", clazz.getGrade());
        if (clazz.getTeacherId() != null) {
            SysUser teacher = userService.getById(clazz.getTeacherId());
            result.put("teacherName", teacher != null ? teacher.getRealName() : "");
            result.put("teacherId", clazz.getTeacherId());
        }
        // 学生数
        List<StudentClass> classmates = studentClassService.selectByClassId(classId);
        result.put("studentCount", classmates != null ? classmates.size() : 0);
        return Result.success(result);
    }

    /**
     * 根据ID获取班级
     */
    @GetMapping("/{id}")
    public Result<Class> getClassById(@PathVariable Long id) {
        // 校验归属：教师只能查自己班，管理员不受限
        checkClassOwnership(id);
        Class clazz = classService.getById(id);
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
        fillClassStatistics(classes);
        return Result.success(classes);
    }
    
    /**
     * 根据专业名称查询班级
     */
    @GetMapping("/by-major/{major}")
    public Result<List<Class>> getClassesByMajor(@PathVariable String major) {
        Integer role = getCurrentRole();
        List<Class> classes;
        if (Objects.equals(role, 3)) {
            // 管理员：查看全部
            classes = classService.selectByMajor(major);
        } else if (Objects.equals(role, 1)) {
            // 教师：只查看自己的班级
            classes = classService.selectByTeacherId(getCurrentUserId()).stream()
                    .filter(c -> major.equals(c.getMajor()))
                    .collect(Collectors.toList());
        } else {
            return Result.error(403, "无权限访问");
        }
        return Result.success(classes);
    }
    
    /**
     * 根据年级查询班级
     */
    @GetMapping("/by-grade/{grade}")
    public Result<List<Class>> getClassesByGrade(@PathVariable String grade) {
        Integer role = getCurrentRole();
        List<Class> classes;
        if (Objects.equals(role, 3)) {
            // 管理员：查看全部
            classes = classService.selectByGrade(grade);
        } else if (Objects.equals(role, 1)) {
            // 教师：只查看自己的班级
            classes = classService.selectByTeacherId(getCurrentUserId()).stream()
                    .filter(c -> grade.equals(c.getGrade()))
                    .collect(Collectors.toList());
        } else {
            return Result.error(403, "无权限访问");
        }
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
     * 批量删除班级（逻辑删除）
     * 逐个复用单删的归属校验（非管理员只能删自己的班级），全部通过后统一删除
     */
    @LogOperation("批量删除班级")
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/batch-delete")
    public Result<String> batchDeleteClasses(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的班级");
        }
        Integer role = getCurrentRole();
        Long userId = getCurrentUserId();
        for (Long id : ids) {
            Class clazz = classService.getById(id);
            if (clazz == null) {
                return Result.error(404, "班级不存在");
            }
            // 非管理员只能删除自己的班级
            if (!Objects.equals(role, 3) && !Objects.equals(clazz.getTeacherId(), userId)) {
                return Result.error(403, "无权删除其他教师的班级");
            }
        }
        boolean success = classService.removeByIds(ids);
        if (!success) {
            return Result.error("批量删除班级失败");
        }
        return Result.success("批量删除成功");
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
                    // 解密手机号
                    String phone = user.getPhone();
                    if (phone != null && !phone.isEmpty()) {
                        try { phone = aesUtil.decrypt(phone); } catch (Exception ignored) { }
                    }
                    map.put("phone", phone != null ? phone : "");
                    // 计算简历完整度
                    Resume resume = resumeService.selectByStudentId(studentId);
                    int resumeComplete = 0;
                    if (resume != null) {
                        if (resume.getEducation() != null && !resume.getEducation().isEmpty()) resumeComplete += 40;
                        if (resume.getSkills() != null && !resume.getSkills().isEmpty()) resumeComplete += 30;
                        if (resume.getSelfEvaluation() != null && !resume.getSelfEvaluation().isEmpty()) resumeComplete += 30;
                    }
                    map.put("resumeComplete", resumeComplete);
                    // 提取AI评分（从 resume.aiAnalysis JSON 中读取 overallScore）
                    Integer aiScore = null;
                    if (resume != null && resume.getAiAnalysis() != null && !resume.getAiAnalysis().isEmpty()) {
                        try {
                            Map<String, Object> analysis = OBJECT_MAPPER.readValue(resume.getAiAnalysis(), Map.class);
                            Object score = analysis.get("overallScore");
                            if (score instanceof Number) {
                                aiScore = ((Number) score).intValue();
                            } else if (score instanceof String) {
                                aiScore = Integer.parseInt((String) score);
                            }
                        } catch (Exception ignored) { }
                    }
                    map.put("aiScore", aiScore);
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
