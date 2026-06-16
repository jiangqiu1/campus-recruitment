package com.recruit.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruit.entity.Resume;
import com.recruit.service.ResumeService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 简历管理控制器
 * 学生可以访问自己的简历，教师可以查看学生简历
 */
@RestController
@RequestMapping("/resumes")
public class ResumeController {
    
    @Autowired
    private ResumeService resumeService;
    
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
    public Result<Resume> getMyResume(@RequestParam Long studentId) {
        Resume resume = resumeService.selectByStudentId(studentId);
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
    public Result<String> createOrUpdateResume(@RequestParam Long studentId, @RequestBody Resume resume) {
        resume.setStudentId(studentId);
        
        if (resume.getId() == null) {
            // 创建简历
            resumeService.save(resume);
            return Result.success("简历创建成功");
        } else {
            // 更新简历
            resumeService.updateById(resume);
            return Result.success("简历更新成功");
        }
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
        
        // 软删除（设置deleted=1）
        resume.setDeleted(1);
        resumeService.updateById(resume);
        
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
        Resume resume = resumeService.selectByStudentId(studentId);
        if (resume == null) {
            return Result.error(404, "该学生简历不存在");
        }
        
        return Result.success(resume);
    }
}
