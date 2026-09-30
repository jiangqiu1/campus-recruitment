package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recruit.entity.AiParseLog;
import com.recruit.mapper.AiParseLogMapper;
import com.recruit.service.AiParseLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI解析日志表 服务实现类
 */
@Service
public class AiParseLogServiceImpl extends ServiceImpl<AiParseLogMapper, AiParseLog> implements AiParseLogService {
    
    @Autowired
    private AiParseLogMapper aiParseLogMapper;
    
    @Override
    public List<AiParseLog> selectByTeacherId(Long teacherId) {
        return aiParseLogMapper.selectByTeacherId(teacherId);
    }
    
    @Override
    public List<AiParseLog> selectUncorrected() {
        return aiParseLogMapper.selectUncorrected();
    }
    
    @Override
    public List<AiParseLog> selectByConfidenceRange(BigDecimal minScore, BigDecimal maxScore) {
        return aiParseLogMapper.selectByConfidenceRange(minScore, maxScore);
    }
    
    @Override
    public List<AiParseLog> selectByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return aiParseLogMapper.selectByTimeRange(startTime, endTime);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean logParse(Long teacherId, String rawMessage, String parsedResult, BigDecimal confidenceScore) {
        AiParseLog log = new AiParseLog();
        log.setTeacherId(teacherId);
        log.setRawMessage(rawMessage);
        log.setParsedResult(parsedResult);
        log.setConfidenceScore(confidenceScore);
        log.setIsManualCorrected(0); // 未修正
        log.setCreateTime(LocalDateTime.now());
        
        return save(log);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean correctParseResult(Long logId, String correctedResult) {
        AiParseLog log = getById(logId);
        if (log == null) {
            throw new RuntimeException("AI解析日志不存在");
        }
        
        log.setIsManualCorrected(1); // 已人工修正
        log.setCorrectedResult(correctedResult);
        
        return updateById(log);
    }
    
    @Override
    public Integer countByTeacherId(Long teacherId) {
        return aiParseLogMapper.selectByTeacherId(teacherId).size();
    }
    
    @Override
    public BigDecimal calculateAverageConfidence() {
        List<AiParseLog> logs = list();
        if (logs.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sum = BigDecimal.ZERO;
        for (AiParseLog log : logs) {
            if (log.getConfidenceScore() != null) {
                sum = sum.add(log.getConfidenceScore());
            }
        }
        
        return sum.divide(BigDecimal.valueOf(logs.size()), 2, RoundingMode.HALF_UP);
    }
    
    @Override
    public Integer countUncorrected() {
        return aiParseLogMapper.selectUncorrected().size();
    }
}
