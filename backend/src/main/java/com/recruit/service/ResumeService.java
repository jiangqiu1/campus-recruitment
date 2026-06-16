package com.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.recruit.entity.Resume;

import java.util.List;

/**
 * 学生简历表 服务接口
 */
public interface ResumeService extends IService<Resume> {
    
    /**
     * 根据学生ID查询简历
     * 
     * @param studentId 学生ID
     * @return 简历实体
     */
    Resume selectByStudentId(Long studentId);
    
    /**
     * 根据学生ID查询默认简历
     * 
     * @param studentId 学生ID
     * @return 默认简历实体
     */
    Resume selectDefaultByStudentId(Long studentId);
    
    /**
     * 设置默认简历
     * 
     * @param studentId 学生ID
     * @param resumeId 要设为默认的简历ID
     * @return 是否成功
     */
    boolean setDefaultResume(Long studentId, Long resumeId);
    
    /**
     * 解析简历PDF并提取信息
     * 
     * @param pdfUrl PDF文件路径
     * @return 解析后的简历信息（JSON格式）
     */
    String parseResumePdf(String pdfUrl);
    
    /**
     * 更新简历技能标签（从简历内容中提取）
     * 
     * @param resumeId 简历ID
     * @return 是否成功
     */
    boolean updateSkillTags(Long resumeId);
}
