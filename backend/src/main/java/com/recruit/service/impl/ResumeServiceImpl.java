package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.entity.Resume;
import com.recruit.mapper.ResumeMapper;
import com.recruit.service.ResumeService;
import com.recruit.utils.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生简历表 服务实现类
 */
@Service
public class ResumeServiceImpl extends ServiceImpl<ResumeMapper, Resume> implements ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeServiceImpl.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 缓存判定容差（毫秒）：analyzedAt 与 updateTime 同刻写入，DB 秒级精度/自动更新
     * 可能造成毫秒级回读偏差，1 分钟内视为"简历未变更"
     */
    private static final long CACHE_TOLERANCE_MS = 60_000;

    @Autowired
    private ResumeMapper resumeMapper;

    @Autowired
    private AiService aiService;

    @Override
    public Resume selectByStudentId(Long studentId) {
        return resumeMapper.selectByStudentId(studentId);
    }

    @Override
    public Resume selectDefaultByStudentId(Long studentId) {
        return resumeMapper.selectDefaultByStudentId(studentId);
    }

    @Override
    public Map<String, Object> analyzeWithCache(Resume resume, boolean force) {
        // 1. 未强制且简历在最近诊断后未变更 → 复用缓存结果，不重复调用 AI
        if (!force) {
            Map<String, Object> cached = readFreshAnalysis(resume);
            if (cached != null) {
                return cached;
            }
        }

        // 2. 组装简历数据发给 AI
        Map<String, Object> resumeData = new HashMap<>();
        resumeData.put("education", resume.getEducation());
        resumeData.put("internship", resume.getInternship());
        resumeData.put("skills", resume.getSkills());
        resumeData.put("selfEvaluation", resume.getSelfEvaluation());
        resumeData.put("jobTarget", resume.getJobTarget());

        String resumeJson;
        try {
            resumeJson = OBJECT_MAPPER.writeValueAsString(resumeData);
        } catch (Exception e) {
            resumeJson = "{}";
        }

        // 3. 调用 AI 分析
        Map<String, Object> aiResult = aiService.analyzeResume(resumeJson);

        // 4. 写回简历：analyzedAt 与 updateTime 用同一时间戳，下次据此判断"诊断后是否变更过"
        LocalDateTime now = LocalDateTime.now();
        try {
            aiResult.put("analyzedAt", now.toString());
            aiResult.put("cached", false);
            resume.setAiAnalysis(OBJECT_MAPPER.writeValueAsString(aiResult));
            resume.setUpdateTime(now);
            updateById(resume);
        } catch (Exception e) {
            // 保存失败不影响返回结果，仅记录日志
            log.error("保存AI简历分析结果失败", e);
        }
        return aiResult;
    }

    /**
     * 读取仍然"新鲜"的缓存诊断结果：aiAnalysis 存在、含 analyzedAt、
     * 且 analyzedAt 不早于 updateTime（含容差）——即诊断后简历没再改过。
     * 旧数据无 analyzedAt 视为过期。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> readFreshAnalysis(Resume resume) {
        String aiAnalysis = resume.getAiAnalysis();
        if (aiAnalysis == null || aiAnalysis.isEmpty()) {
            return null;
        }
        try {
            Map<String, Object> cached = OBJECT_MAPPER.readValue(aiAnalysis, Map.class);
            Object analyzedAtVal = cached.get("analyzedAt");
            if (analyzedAtVal == null) {
                return null;
            }
            LocalDateTime analyzedAt = LocalDateTime.parse(analyzedAtVal.toString());
            LocalDateTime updateTime = resume.getUpdateTime();
            boolean fresh = updateTime == null
                    || !analyzedAt.plusSeconds(CACHE_TOLERANCE_MS / 1000).isBefore(updateTime);
            if (fresh) {
                cached.put("cached", true);
                return cached;
            }
            return null;
        } catch (Exception e) {
            log.warn("解析缓存诊断结果失败，将重新调用 AI: {}", e.getMessage());
            return null;
        }
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
