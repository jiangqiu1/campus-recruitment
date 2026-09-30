package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recruit.entity.Resume;
import com.recruit.mapper.ResumeMapper;
import com.recruit.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 学生简历表 服务实现类
 */
@Service
public class ResumeServiceImpl extends ServiceImpl<ResumeMapper, Resume> implements ResumeService {
    
    @Autowired
    private ResumeMapper resumeMapper;
    
    @Override
    public Resume selectByStudentId(Long studentId) {
        return resumeMapper.selectByStudentId(studentId);
    }
    
    @Override
    public Resume selectDefaultByStudentId(Long studentId) {
        return resumeMapper.selectDefaultByStudentId(studentId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefaultResume(Long studentId, Long resumeId) {
        // 1. 将该学生的其他简历设为非默认
        resumeMapper.resetDefaultFlag(studentId, resumeId);
        
        // 2. 将指定简历设为默认
        Resume resume = getById(resumeId);
        if (resume == null || !resume.getStudentId().equals(studentId)) {
            throw new RuntimeException("简历不存在或不属于该学生");
        }
        
        resume.setIsDefault(1);
        return updateById(resume);
    }
    
    /**
     * 遗留假实现（⚠️ 勿用于真实场景）：返回硬编码模拟解析结果。
     * 真实 AI 简历解析走 ResumeController.upload-and-parse / AiParseController（DeepSeek），
     * 本方法对应 POST /resumes/{id}/parse，PC 前端无调用方，保留仅为兼容可能的小程序端。
     */
    @Override
    public String parseResumePdf(String pdfUrl) {
        // TODO：调用AI解析服务（讯飞星火OCR + NLP）
        // 这里先返回模拟数据
        return "{\"education\":\"本科\",\"skills\":[\"Java\",\"Spring\",\"MySQL\"],\"experience\":\"2年开发经验\"}";
    }
    
    /**
     * 遗留假实现（⚠️ 勿用于真实场景）：写死技能标签/求职意向。
     * 与 parseResumePdf 配套，同属 POST /resumes/{id}/parse 的 mock 链路。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSkillTags(Long resumeId) {
        Resume resume = getById(resumeId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }
        
        // TODO：调用AI提取技能标签
        // 这里先设置模拟数据
        resume.setSkillTags("[\"Java\",\"Spring Boot\",\"MySQL\",\"Redis\"]");
        resume.setJobTarget("Java开发工程师");
        
        return updateById(resume);
    }
}
