package com.recruit.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recruit.entity.Delivery;
import com.recruit.entity.Job;
import com.recruit.entity.Resume;
import com.recruit.entity.ResumeScoreLog;
import com.recruit.entity.SysUser;
import com.recruit.mapper.DeliveryMapper;
import com.recruit.mapper.JobMapper;
import com.recruit.mapper.ResumeMapper;
import com.recruit.mapper.ResumeScoreLogMapper;
import com.recruit.service.ResumeScoreLogService;
import com.recruit.service.UserService;
import com.recruit.utils.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResumeScoreLogServiceImpl extends ServiceImpl<ResumeScoreLogMapper, ResumeScoreLog> implements ResumeScoreLogService {

    private static final Logger log = LoggerFactory.getLogger(ResumeScoreLogServiceImpl.class);
    
    @Autowired
    private ResumeScoreLogMapper resumeScoreLogMapper;

    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private DeliveryMapper deliveryMapper;

    @Autowired
    private ResumeMapper resumeMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private AiService aiService;
    
    @Override
    public List<ResumeScoreLog> selectByJobIdOrderByScore(Long jobId) {
        return resumeScoreLogMapper.selectByJobIdOrderByScore(jobId);
    }
    
    @Override
    public ResumeScoreLog selectByDeliveryId(Long deliveryId) {
        return resumeScoreLogMapper.selectByDeliveryId(deliveryId);
    }
    
    @Override
    public List<ResumeScoreLog> selectByScoreRange(Integer minScore, Integer maxScore) {
        return resumeScoreLogMapper.selectByScoreRange(minScore, maxScore);
    }
    
    @Override
    public Double calculateAverageScoreByJobId(Long jobId) {
        return resumeScoreLogMapper.calculateAverageScoreByJobId(jobId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean scoreResume(Long jobId, Long deliveryId) {
        // 查询岗位信息
        Job job = jobMapper.selectById(jobId);
        // 查询投递记录（获取学生ID）
        Delivery delivery = deliveryMapper.selectById(deliveryId);
        // 查询学生简历
        Resume resume = (delivery != null && delivery.getStudentId() != null)
                ? resumeMapper.selectByStudentId(delivery.getStudentId()) : null;

        String jobDesc = job != null ? job.getDescription() : "";
        String jobReq = job != null ? job.getRequirement() : "";
        String resumeText = buildResumeText(resume);

        // 调用 AI 评分
        Map<String, Object> result = aiService.scoreResume(jobDesc, jobReq, resumeText);

        int totalScore = toInt(result.get("totalScore"));
        int skillScore = toInt(result.get("skillScore"));
        int expScore = toInt(result.get("expScore"));
        int eduScore = toInt(result.get("eduScore"));
        String comment = result.getOrDefault("comment", "").toString();

        // 组装详细评分 JSON
        String scoreDetail = String.format(
                "{\"技能得分\":%d,\"经验得分\":%d,\"教育得分\":%d,\"评语\":\"%s\"}",
                skillScore, expScore, eduScore, comment
        );

        // 写入数据库
        ResumeScoreLog scoreLog = new ResumeScoreLog();
        scoreLog.setJobId(jobId);
        scoreLog.setDeliveryId(deliveryId);
        scoreLog.setScore(totalScore);
        scoreLog.setScoreDetail(scoreDetail);
        scoreLog.setCreateTime(LocalDateTime.now());
        
        return save(scoreLog);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchScoreResumes(Long jobId) {
        // 查询该岗位的所有投递记录
        List<Delivery> deliveries = deliveryMapper.selectByJobId(jobId);
        int count = 0;
        for (Delivery d : deliveries) {
            try {
                if (scoreResume(jobId, d.getId())) count++;
            } catch (Exception e) {
                log.error("批量评分失败: deliveryId={}", d.getId(), e);
            }
        }
        return count;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rescoreResume(Long scoreLogId) {
        ResumeScoreLog oldScoreLog = getById(scoreLogId);
        if (oldScoreLog == null) {
            throw new RuntimeException("评分记录不存在");
        }
        removeById(scoreLogId);
        return scoreResume(oldScoreLog.getJobId(), oldScoreLog.getDeliveryId());
    }
    
    @Override
    public Map<String, Integer> calculateScoreDistribution(Long jobId) {
        List<ResumeScoreLog> scoreLogs = resumeScoreLogMapper.selectByJobIdOrderByScore(jobId);
        
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("0-59", 0);
        distribution.put("60-79", 0);
        distribution.put("80-100", 0);
        
        for (ResumeScoreLog scoreLog : scoreLogs) {
            int score = scoreLog.getScore();
            if (score < 60) {
                distribution.put("0-59", distribution.get("0-59") + 1);
            } else if (score < 80) {
                distribution.put("60-79", distribution.get("60-79") + 1);
            } else {
                distribution.put("80-100", distribution.get("80-100") + 1);
            }
        }
        
        return distribution;
    }
    
    @Override
    public ResumeScoreLog selectTopScoreByJobId(Long jobId) {
        List<ResumeScoreLog> scoreLogs = resumeScoreLogMapper.selectByJobIdOrderByScore(jobId);
        if (scoreLogs.isEmpty()) {
            return null;
        }
        return scoreLogs.get(0);
    }
    
    @Override
    public ResumeScoreLog selectLowestScoreByJobId(Long jobId) {
        List<ResumeScoreLog> scoreLogs = resumeScoreLogMapper.selectByJobIdOrderByScore(jobId);
        if (scoreLogs.isEmpty()) {
            return null;
        }
        return scoreLogs.get(scoreLogs.size() - 1);
    }
    
    @Override
    public int batchScoreByCompany(Long companyId) {
        List<Job> jobs = jobMapper.selectList(
                Wrappers.<Job>lambdaQuery()
                    .eq(Job::getCompanyId, companyId)
        );
        int total = 0;
        for (Job job : jobs) {
            total += batchScoreResumes(job.getId());
        }
        return total;
    }

    // ========== 工具方法 ==========

    private String buildResumeText(Resume resume) {
        if (resume == null) return "暂无简历数据";
        StringBuilder sb = new StringBuilder();

        // 获取学生基本信息（姓名、电话等存在 sys_user 表）
        if (resume.getStudentId() != null) {
            SysUser user = userService.getById(resume.getStudentId());
            if (user != null) {
                if (user.getRealName() != null) sb.append("姓名：").append(user.getRealName()).append("\n");
                if (user.getPhone() != null) sb.append("电话：").append(user.getPhone()).append("\n");
            }
        }

        // 教育经历（JSON 数组）
        if (resume.getEducation() != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<Map> eduList = mapper.readValue(resume.getEducation(), List.class);
                for (Map edu : eduList) {
                    if (edu.get("school") != null) sb.append("学校：").append(edu.get("school")).append("\n");
                    if (edu.get("major") != null) sb.append("专业：").append(edu.get("major")).append("\n");
                    if (edu.get("degree") != null) sb.append("学历：").append(edu.get("degree")).append("\n");
                }
            } catch (Exception e) {
                sb.append("教育经历：").append(resume.getEducation()).append("\n");
            }
        }

        if (resume.getSkills() != null) sb.append("技能：").append(resume.getSkills()).append("\n");
        if (resume.getInternship() != null) sb.append("实习经历：").append(resume.getInternship()).append("\n");
        if (resume.getSelfEvaluation() != null) sb.append("自我评价：").append(resume.getSelfEvaluation()).append("\n");
        if (resume.getJobTarget() != null) sb.append("求职意向：").append(resume.getJobTarget()).append("\n");

        return sb.toString();
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return 0; }
    }
}
