package com.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.recruit.entity.Resume;

import java.util.List;
import java.util.Map;

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
     * AI 简历诊断（带缓存）：简历在最近一次诊断后未变更时直接返回已有结果，不重复调用 AI；
     * 变更过或 force=true 时重新调用 AI，并把结果（含 analyzedAt 时间戳）写回 aiAnalysis 字段。
     *
     * @param resume 要诊断的简历
     * @param force  true=强制重新调用 AI；false=未变更时复用缓存
     * @return 诊断结果 Map（AI 返回字段 + analyzedAt + cached 标记，cached=true 表示命中缓存）
     */
    Map<String, Object> analyzeWithCache(Resume resume, boolean force);

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
