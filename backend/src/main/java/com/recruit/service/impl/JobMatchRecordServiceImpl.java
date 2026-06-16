package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recruit.entity.JobMatchRecord;
import com.recruit.mapper.JobMatchRecordMapper;
import com.recruit.service.JobMatchRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 人岗匹配记录表 服务实现类
 */
@Service
public class JobMatchRecordServiceImpl extends ServiceImpl<JobMatchRecordMapper, JobMatchRecord> implements JobMatchRecordService {
    
    @Autowired
    private JobMatchRecordMapper jobMatchRecordMapper;
    
    @Override
    public List<JobMatchRecord> selectByJobIdOrderByScore(Long jobId) {
        return jobMatchRecordMapper.selectByJobIdOrderByScore(jobId);
    }
    
    @Override
    public List<JobMatchRecord> selectByStudentId(Long studentId) {
        return jobMatchRecordMapper.selectByStudentId(studentId);
    }
    
    @Override
    public List<JobMatchRecord> selectPushedByJobId(Long jobId) {
        return jobMatchRecordMapper.selectPushedByJobId(jobId);
    }
    
    @Override
    public List<JobMatchRecord> selectClickedByJobId(Long jobId) {
        return jobMatchRecordMapper.selectClickedByJobId(jobId);
    }
    
    @Override
    public List<JobMatchRecord> selectByScoreRange(BigDecimal minScore, BigDecimal maxScore) {
        return jobMatchRecordMapper.selectByScoreRange(minScore, maxScore);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean generateMatchRecord(Long jobId, Long studentId) {
        // TODO：调用AI算法计算匹配度
        // 这里先生成模拟数据
        JobMatchRecord record = new JobMatchRecord();
        record.setJobId(jobId);
        record.setStudentId(studentId);
        record.setMatchScore(new BigDecimal("0.85")); // 模拟匹配度85%
        record.setMatchReason("技能匹配：Java,Spring,MySQL");
        record.setIsPushed(0);
        record.setIsClicked(0);
        record.setCreateTime(LocalDateTime.now());
        
        return save(record);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchGenerateMatchRecords(Long jobId) {
        // TODO：实际项目中需要查询所有学生，然后逐个计算匹配度
        // 这里先返回0（模拟）
        return 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePushedStatus(Long recordId) {
        int rows = jobMatchRecordMapper.updatePushedStatus(recordId, LocalDateTime.now());
        return rows > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateClickedStatus(Long recordId) {
        int rows = jobMatchRecordMapper.updateClickedStatus(recordId);
        return rows > 0;
    }
    
    @Override
    public BigDecimal calculatePushRate(Long jobId) {
        List<JobMatchRecord> allRecords = jobMatchRecordMapper.selectByJobIdOrderByScore(jobId);
        if (allRecords.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        long pushedCount = allRecords.stream()
                .filter(r -> r.getIsPushed() == 1)
                .count();
        
        return new BigDecimal(pushedCount)
                .divide(new BigDecimal(allRecords.size()), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    @Override
    public BigDecimal calculateClickRate(Long jobId) {
        List<JobMatchRecord> pushedRecords = jobMatchRecordMapper.selectPushedByJobId(jobId);
        if (pushedRecords.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        long clickedCount = pushedRecords.stream()
                .filter(r -> r.getIsClicked() == 1)
                .count();
        
        return new BigDecimal(clickedCount)
                .divide(new BigDecimal(pushedRecords.size()), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    @Override
    public BigDecimal calculateAverageMatchScore(Long jobId) {
        List<JobMatchRecord> records = jobMatchRecordMapper.selectByJobIdOrderByScore(jobId);
        if (records.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sum = BigDecimal.ZERO;
        for (JobMatchRecord record : records) {
            sum = sum.add(record.getMatchScore());
        }
        
        return sum.divide(new BigDecimal(records.size()), 2, BigDecimal.ROUND_HALF_UP);
    }
}
