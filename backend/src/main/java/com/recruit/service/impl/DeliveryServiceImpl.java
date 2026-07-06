package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recruit.entity.Delivery;
import com.recruit.entity.Job;
import com.recruit.entity.Message;
import com.recruit.mapper.DeliveryMapper;
import com.recruit.mapper.JobMapper;
import com.recruit.service.DeliveryService;
import com.recruit.service.MessageService;
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
 * 投递记录表 服务实现类
 */
@Service
public class DeliveryServiceImpl extends ServiceImpl<DeliveryMapper, Delivery> implements DeliveryService {
    
    private static final Logger log = LoggerFactory.getLogger(DeliveryServiceImpl.class);

    @Autowired
    private DeliveryMapper deliveryMapper;
    
    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private MessageService messageService;
    
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
        
        boolean saved = save(delivery);
        
        // 3. 生成投递成功消息
        if (saved) {
            String jobTitle = getJobTitle(jobId);
            createMessage(studentId, "投递成功通知",
                    "您已成功投递「" + jobTitle + "」，请耐心等待企业反馈",
                    0, jobId);
        }
        
        return saved;
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
            delivery.setInterviewTime(LocalDateTime.now().plusHours(1));
            delivery.setInterviewLocation("线上面试（腾讯会议）");
        }
        
        boolean updated = updateById(delivery);
        
        // 根据状态变化生成消息通知学生
        if (updated && delivery.getStudentId() != null) {
            String jobTitle = getJobTitle(delivery.getJobId());
            switch (status) {
                case 1:
                    createMessage(delivery.getStudentId(), "简历被查看",
                            "您的简历已被企业查看，祝您好运！",
                            0, delivery.getJobId());
                    break;
                case 2:
                    createMessage(delivery.getStudentId(), "面试邀请",
                            "恭喜！您投递的「" + jobTitle + "」已通过初筛，面试安排：" + delivery.getInterviewLocation(),
                            1, delivery.getJobId());
                    break;
                case 3:
                    createMessage(delivery.getStudentId(), "录用通知",
                            "恭喜！您已被录用！岗位：「" + jobTitle + "」",
                            1, delivery.getJobId());
                    break;
                case 4:
                    createMessage(delivery.getStudentId(), "未通过通知",
                            "很遗憾，您投递的「" + jobTitle + "」未通过筛选",
                            1, delivery.getJobId());
                    break;
            }
        }
        
        return updated;
    }

    /**
     * 获取岗位名称
     */
    private String getJobTitle(Long jobId) {
        if (jobId == null) return "该岗位";
        Job job = jobMapper.selectById(jobId);
        return job != null ? job.getTitle() : "该岗位";
    }

    /**
     * 创建消息通知
     */
    private void createMessage(Long studentId, String title, String content, Integer type, Long relatedId) {
        try {
            Message msg = new Message();
            msg.setStudentId(studentId);
            msg.setTitle(title);
            msg.setContent(content);
            msg.setType(type);
            msg.setIsRead(0);
            msg.setRelatedId(relatedId);
            msg.setCreateTime(LocalDateTime.now());
            messageService.save(msg);
        } catch (Exception e) {
            log.error("创建消息通知失败", e);
        }
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
