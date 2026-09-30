package com.recruit.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruit.entity.Class;
import com.recruit.entity.Delivery;
import com.recruit.entity.Job;
import com.recruit.entity.Resume;
import com.recruit.entity.SysUser;
import com.recruit.service.ClassService;
import com.recruit.service.DeliveryService;
import com.recruit.service.JobService;
import com.recruit.service.ResumeService;
import com.recruit.service.UserService;
import com.recruit.utils.AESUtil;
import com.recruit.utils.AiService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 简历管理控制器
 * 学生可以访问自己的简历，教师可以查看学生简历
 */
@RestController
@RequestMapping("/resumes")
public class ResumeController extends BaseController {
    
    @Autowired
    private ResumeService resumeService;

    @Autowired(required = false)
    private ClassService classService;

    @Autowired
    private AiService aiService;

    @Autowired
    private UserService userService;

    @Autowired
    private AESUtil aesUtil;

    @Autowired
    private JobService jobService;

    @Autowired
    private DeliveryService deliveryService;
    
    /**
     * 获取简历列表（教师/管理员）
     * 按创建时间倒序，支持分页和可选的学生ID/姓名筛选
     */
    @GetMapping
    public Result<IPage<Resume>> listResumes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long studentId) {
        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        if (studentId != null) {
            wrapper.eq(Resume::getStudentId, studentId);
        }
        wrapper.orderByDesc(Resume::getUpdateTime);
        IPage<Resume> result = resumeService.page(new Page<>(page, size), wrapper);
        return Result.success(result);
    }

    /**
     * 获取学生自己的简历
     * 
     * @param studentId 学生ID（从JWT中获取）
     * @return 简历实体
     */
    @GetMapping("/my")
    public Result<Resume> getMyResume(@RequestParam(required = false) Long studentId) {
        // 强制使用当前登录用户，忽略外部传入的 studentId，防止越权访问他人简历
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "未授权");
        }
        Resume resume = resumeService.selectByStudentId(currentUserId);
        if (resume == null) {
            return Result.error(404, "简历不存在");
        }
        
        return Result.success(resume);
    }
    
    /**
     * 获取学生的默认简历
     * 
     * @param studentId 学生ID
     * @return 默认简历实体
     */
    @GetMapping("/my/default")
    public Result<Resume> getMyDefaultResume(@RequestParam Long studentId) {
        Resume resume = resumeService.selectDefaultByStudentId(studentId);
        if (resume == null) {
            return Result.error(404, "默认简历不存在");
        }
        
        return Result.success(resume);
    }
    
    /**
     * 创建/更新简历
     * 
     * @param studentId 学生ID（从JWT中获取）
     * @param resume 简历实体
     * @return 创建/更新结果
     */
    @PostMapping
    public Result<String> createOrUpdateResume(
            @RequestParam Long studentId,
            @RequestBody Map<String, Object> body) {

        // 同步更新用户基本信息（姓名、性别、手机号、邮箱）
        Object name = body.get("name");
        Object gender = body.get("gender");
        Object phone = body.get("phone");
        Object email = body.get("email");
        if (name != null || phone != null || email != null || gender != null) {
            SysUser user = userService.getById(studentId);
            if (user != null) {
                if (name != null) user.setRealName(name.toString());
                if (phone != null) user.setPhone(aesUtil.encrypt(phone.toString()));
                if (email != null) user.setEmail(email.toString());
                if (gender != null) {
                    try { user.setGender(Integer.valueOf(gender.toString())); } catch (Exception ignored) {}
                }
                userService.updateById(user);
            }
        }

        // 保存简历数据
        Resume resume = new Resume();
        resume.setStudentId(studentId);
        if (body.get("id") != null) {
            resume.setId(Long.valueOf(body.get("id").toString()));
        }
        if (body.get("education") != null) resume.setEducation(body.get("education").toString());
        if (body.get("internship") != null) resume.setInternship(body.get("internship").toString());
        if (body.get("project") != null) resume.setProject(body.get("project").toString());
        if (body.get("skills") != null) resume.setSkills(body.get("skills").toString());
        if (body.get("selfEvaluation") != null) resume.setSelfEvaluation(body.get("selfEvaluation").toString());
        if (body.get("jobTarget") != null) resume.setJobTarget(body.get("jobTarget").toString());

        if (resume.getId() == null) {
            resumeService.save(resume);
        } else {
            resumeService.updateById(resume);
        }
        return Result.success("保存成功");
    }
    
    /**
     * 上传PDF简历
     * 
     * @param studentId 学生ID（从JWT中获取）
     * @param file PDF文件
     * @return 上传结果
     */
    @PostMapping("/upload-pdf")
    public Result<Map<String, String>> uploadPdf(
            @RequestParam Long studentId,
            @RequestParam("file") MultipartFile file) {
        
        // TODO：实际项目中需要调用OSS上传
        // 这里先返回模拟路径
        String pdfUrl = "/uploads/resume/resume_" + studentId + ".pdf";
        
        // 更新简历的PDF路径
        Resume resume = resumeService.selectByStudentId(studentId);
        if (resume == null) {
            resume = new Resume();
            resume.setStudentId(studentId);
            resume.setPdfUrl(pdfUrl);
            resumeService.save(resume);
        } else {
            resume.setPdfUrl(pdfUrl);
            resumeService.updateById(resume);
        }
        
        Map<String, String> result = new java.util.HashMap<>();
        result.put("pdfUrl", pdfUrl);
        
        return Result.success("PDF简历上传成功", result);
    }

    /**
     * 上传PDF简历并自动 AI 解析
     *
     * @param studentId 学生ID
     * @param file PDF文件
     * @return 解析结果（含结构化数据和PDF路径）
     */
    @PostMapping("/upload-and-parse")
    public Result<Map<String, Object>> uploadAndParse(
            @RequestParam Long studentId,
            @RequestParam("file") MultipartFile file) {

        // 1. 提取PDF文本
        String rawText;
        try {
            rawText = aiService.extractTextFromPdf(file);
            if (rawText == null || rawText.trim().isEmpty()) {
                return Result.error("无法提取文本，请确认PDF为文字版而非扫描件");
            }
        } catch (Exception e) {
            return Result.error("PDF读取失败: " + e.getMessage());
        }

        // 2. AI解析
        Map<String, Object> parsed = aiService.parseResume(rawText);
        if (parsed == null || parsed.isEmpty()) {
            return Result.error("AI解析失败，请稍后重试或手动填写");
        }

        // 3. 保存PDF路径
        String pdfUrl = "/uploads/resume/resume_" + studentId + ".pdf";
        Resume resume = resumeService.selectByStudentId(studentId);
        if (resume == null) {
            resume = new Resume();
            resume.setStudentId(studentId);
            resume.setPdfUrl(pdfUrl);
            resumeService.save(resume);
        } else {
            resume.setPdfUrl(pdfUrl);
            resumeService.updateById(resume);
        }

        // 4. 返回解析结果
        Map<String, Object> result = new HashMap<>();
        result.put("parsedData", parsed);
        result.put("pdfUrl", pdfUrl);

        return Result.success("解析成功", result);
    }

    /**
     * 设置默认简历
     * 
     * @param studentId 学生ID（从JWT中获取）
     * @param resumeId 简历ID
     * @return 设置结果
     */
    @PutMapping("/{resumeId}/set-default")
    public Result<String> setDefaultResume(
            @RequestParam Long studentId,
            @PathVariable Long resumeId) {
        
        boolean success = resumeService.setDefaultResume(studentId, resumeId);
        if (!success) {
            return Result.error("设置默认简历失败");
        }
        
        return Result.success("默认简历设置成功");
    }
    
    /**
     * 解析简历PDF（AI）
     * 
     * @param resumeId 简历ID
     * @return 解析结果
     */
    @PostMapping("/{resumeId}/parse")
    public Result<Map<String, String>> parseResume(@PathVariable Long resumeId) {
        Resume resume = resumeService.getById(resumeId);
        if (resume == null) {
            return Result.error(404, "简历不存在");
        }
        
        // 调用AI解析服务
        String parsedResult = resumeService.parseResumePdf(resume.getPdfUrl());
        
        // 更新简历的技能标签和求职意向
        resumeService.updateSkillTags(resumeId);
        
        Map<String, String> result = new java.util.HashMap<>();
        result.put("parsedResult", parsedResult);
        
        return Result.success("简历解析成功", result);
    }
    
    /**
     * 删除简历（软删除）
     * 
     * @param resumeId 简历ID
     * @return 删除结果
     */
    @DeleteMapping("/{resumeId}")
    public Result<String> deleteResume(@PathVariable Long resumeId) {
        Resume resume = resumeService.getById(resumeId);
        if (resume == null) {
            return Result.error(404, "简历不存在");
        }
        
        resumeService.removeById(resume.getId());
        return Result.success("简历删除成功");
    }
    
    /**
     * 根据ID获取简历详情
     */
    @GetMapping("/{id}")
    public Result<Resume> getResumeById(@PathVariable Long id) {
        Resume resume = resumeService.getById(id);
        if (resume == null) {
            return Result.error(404, "简历不存在");
        }
        return Result.success(resume);
    }

    /**
     * 教师查看学生简历
     * 
     * @param studentId 学生ID
     * @return 简历实体
     */
    @GetMapping("/student/{studentId}")
    public Result<Resume> getStudentResume(@PathVariable Long studentId) {
        Integer role = getCurrentRole();
        Long currentUserId = getCurrentUserId();

        if (Objects.equals(role, 1)) {
            // 教师：只能查看本班学生
            boolean inMyClass = false;
            try {
                List<Class> myClasses = classService.selectByTeacherId(currentUserId);
                for (Class cls : myClasses) {
                    List<Long> ids = classService.getStudentIdsByClassId(cls.getId());
                    if (ids != null && ids.contains(studentId)) { inMyClass = true; break; }
                }
            } catch (Exception ignored) {}
            if (!inMyClass) return Result.error(403, "无权查看该学生简历");
        } else if (Objects.equals(role, 0)) {
            // 学生：只能查看自己的简历
            if (!Objects.equals(studentId, currentUserId)) {
                return Result.error(403, "无权查看该学生简历");
            }
        } else if (Objects.equals(role, 2)) {
            // HR：只能查看投递了本企业岗位的学生
            boolean allowed = false;
            try {
                SysUser hr = userService.getById(currentUserId);
                Long companyId = hr != null ? hr.getCompanyId() : null;
                if (companyId != null) {
                    List<Job> companyJobs = jobService.lambdaQuery()
                            .eq(Job::getCompanyId, companyId)
                            .list();
                    if (companyJobs != null && !companyJobs.isEmpty()) {
                        List<Long> jobIds = companyJobs.stream().map(Job::getId).collect(Collectors.toList());
                        long deliveryCount = deliveryService.lambdaQuery()
                                .eq(Delivery::getStudentId, studentId)
                                .in(Delivery::getJobId, jobIds)
                                .count();
                        allowed = deliveryCount > 0;
                    }
                }
            } catch (Exception ignored) {}
            if (!allowed) return Result.error(403, "无权查看该学生简历");
        }
        // 管理员（role=3）：无限制

        Resume resume = resumeService.selectByStudentId(studentId);
        if (resume == null) {
            return Result.error(404, "该学生简历不存在");
        }
        
        return Result.success(resume);
    }
}
