package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recruit.entity.OperationLog;
import com.recruit.mapper.OperationLogMapper;
import com.recruit.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作日志表 服务实现类
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {
    
    @Autowired
    private OperationLogMapper operationLogMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean logOperation(Long userId, String operationType, String targetId, String ipAddress) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperationType(operationType);
        log.setTargetId(targetId);
        log.setIpAddress(ipAddress);
        log.setCreateTime(LocalDateTime.now());
        
        return save(log);
    }
    
    @Override
    public List<OperationLog> selectByUserId(Long userId) {
        return operationLogMapper.selectByUserId(userId);
    }
    
    @Override
    public List<OperationLog> selectByOperationType(String operationType) {
        return operationLogMapper.selectByOperationType(operationType);
    }
    
    @Override
    public List<OperationLog> selectByUserIdAndOperationType(Long userId, String operationType) {
        return operationLogMapper.selectByUserIdAndOperationType(userId, operationType);
    }
    
    @Override
    public List<OperationLog> selectByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return operationLogMapper.selectByTimeRange(startTime, endTime);
    }
    
    @Override
    public Map<String, Integer> countByUserIdAndGroupByOperationType(Long userId) {
        List<OperationLog> logs = operationLogMapper.selectByUserId(userId);
        
        Map<String, Integer> operationCountMap = new HashMap<>();
        for (OperationLog log : logs) {
            String operationType = log.getOperationType();
            operationCountMap.put(operationType, operationCountMap.getOrDefault(operationType, 0) + 1);
        }
        
        return operationCountMap;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupLogsBeforeTime(LocalDateTime beforeTime) {
        return operationLogMapper.deleteBeforeTime(beforeTime);
    }
}
