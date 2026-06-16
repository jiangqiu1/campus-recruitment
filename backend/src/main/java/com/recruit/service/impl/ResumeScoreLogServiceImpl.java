package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recruit.entity.Job;
import com.recruit.entity.ResumeScoreLog;
import com.recruit.mapper.JobMapper;
import com.recruit.mapper.ResumeScoreLogMapper;
import com.recruit.service.ResumeScoreLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历智能评分记录表 服务实现类
 */
@Service
public class ResumeScoreLogServiceImpl extends ServiceImpl<ResumeScoreLogMapper, ResumeScoreLog> implements ResumeScoreLogService {
    
    @Autowired
    private ResumeScoreLogMapper resumeScoreLogMapper;

    @Autowired
    private JobMapper jobMapper;
    
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
        // TODO：调用AI算法进行简历评分
        // 这里先生成模拟数据
        ResumeScoreLog scoreLog = new ResumeScoreLog();
        scoreLog.setJobId(jobId);
        scoreLog.setDeliveryId(deliveryId);
        scoreLog.setScore(85); // 模拟分数85分
        scoreLog.setScoreDetail("{\"技能得分\":90,\"经验得分\":80,\"教育得分\":85}");
        scoreLog.setCreateTime(java.time.LocalDateTime.now());
        
        return save(scoreLog);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchScoreResumes(Long jobId) {
        // TODO：实际项目中需要查询该岗位的所有投递记录，然后逐个评分
        // 这里先返回0（模拟）
        return 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rescoreResume(Long scoreLogId) {
        // TODO：重新评分（覆盖之前的评分）
        // 这里先模拟删除旧评分，生成新评分
        ResumeScoreLog oldScoreLog = getById(scoreLogId);
        if (oldScoreLog == null) {
            throw new RuntimeException("评分记录不存在");
        }
        
        // 删除旧评分
        removeById(scoreLogId);
        
        // 生成新评分
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
        return scoreLogs.get(0); // 第一个是最高分
    }
    
    @Override
    public ResumeScoreLog selectLowestScoreByJobId(Long jobId) {
        List<ResumeScoreLog> scoreLogs = resumeScoreLogMapper.selectByJobIdOrderByScore(jobId);
        if (scoreLogs.isEmpty()) {
            return null;
        }
        return scoreLogs.get(scoreLogs.size() - 1); // 最后一个是最低分
    }
    @Override
    public int batchScoreByCompany(Long companyId) {
        // 获取企业下所有岗位
        List<Job> jobs = jobMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<Job>lambdaQuery()
                    .eq(Job::getCompanyId, companyId)
        );
        int total = 0;
        for (com.recruit.entity.Job job : jobs) {
            total += batchScoreResumes(job.getId());
        }
        return total;
    }
}
