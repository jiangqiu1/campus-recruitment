package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recruit.entity.Delivery;
import com.recruit.mapper.DeliveryMapper;
import com.recruit.service.DeliveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 投递记录表 服务实现类
 */
@Service
public class DeliveryServiceImpl extends ServiceImpl<DeliveryMapper, Delivery> implements DeliveryService {
    
    @Autowired
    private DeliveryMapper deliveryMapper;
    
    @Override
    public List<Delivery> selectByStudentId(Long studentId) {
        return deliveryMapper.selectByStudentId(studentId);
    }
    
    @Override
    public List<Delivery> selectByJobId(Long jobId) {
        return deliveryMapper.selectByJobId(jobId);
    }
    
    @Override
    public List<Delivery> selectByStatus(Integer status) {
        return deliveryMapper.selectByStatus(status);
    }
    
    @Override
    public List<Delivery> selectByStudentIdAndStatus(Long studentId, Integer status) {
        return deliveryMapper.selectByStudentIdAndStatus(studentId, status);
    }
    
    @Override
    public List<Delivery> selectByJobIdAndStatus(Long jobId, Integer status) {
        return deliveryMapper.selectByJobIdAndStatus(jobId, status);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deliverResume(Long studentId, Long jobId, String resumeVersion) {
        // 1. 检查是否已投递
        List<Delivery> existDeliveries = deliveryMapper.selectByStudentIdAndStatus(studentId, 0);
        boolean alreadyDelivered = existDeliveries.stream()
                .anyMatch(d -> d.getJobId().equals(jobId));
        
        if (alreadyDelivered) {
            throw new RuntimeException("您已投递过该岗位，不能重复投递");
        }
        
        // 2. 创建投递记录
        Delivery delivery = new Delivery();
        delivery.setStudentId(studentId);
        delivery.setJobId(jobId);
        delivery.setResumeVersion(resumeVersion);
        delivery.setStatus(0); // 已投递
        delivery.setCreateTime(LocalDateTime.now());
        
        return save(delivery);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDeliveryStatus(Long deliveryId, Integer status, String feedback) {
        Delivery delivery = getById(deliveryId);
        if (delivery == null) {
            throw new RuntimeException("投递记录不存在");
        }
        
        delivery.setStatus(status);
        
        if (feedback != null && !feedback.isEmpty()) {
            delivery.setFeedback(feedback);
        }
        
        // 如果状态是"待面试"，需要设置面试时间和地点
        if (status == 2) {
            // TODO：这里应该从请求参数中获取面试时间和地点
            // 这里先设置为1小时后
            delivery.setInterviewTime(LocalDateTime.now().plusHours(1));
            delivery.setInterviewLocation("线上面试（腾讯会议）");
        }
        
        return updateById(delivery);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean arrangeInterview(Long deliveryId, LocalDateTime interviewTime, String interviewLocation) {
        Delivery delivery = getById(deliveryId);
        if (delivery == null) {
            throw new RuntimeException("投递记录不存在");
        }
        
        delivery.setStatus(2); // 待面试
        delivery.setInterviewTime(interviewTime);
        delivery.setInterviewLocation(interviewLocation);
        
        return updateById(delivery);
    }
    
    @Override
    public Integer countByJobId(Long jobId) {
        return deliveryMapper.countByJobId(jobId);
    }
    
    @Override
    public Integer countByStudentId(Long studentId) {
        return deliveryMapper.selectByStudentId(studentId).size();
    }
    
    @Override
    public Map<Integer, Integer> countByJobIdAndGroupByStatus(Long jobId) {
        List<Delivery> deliveries = deliveryMapper.selectByJobId(jobId);
        
        Map<Integer, Integer> statusCountMap = new HashMap<>();
        for (Delivery delivery : deliveries) {
            Integer status = delivery.getStatus();
            statusCountMap.put(status, statusCountMap.getOrDefault(status, 0) + 1);
        }
        
        return statusCountMap;
    }
}
