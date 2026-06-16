package com.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.recruit.entity.Delivery;

import java.util.List;
import java.util.Map;

/**
 * 投递记录表 服务接口
 */
public interface DeliveryService extends IService<Delivery> {
    
    /**
     * 根据学生ID查询投递记录
     * 
     * @param studentId 学生ID
     * @return 投递记录列表
     */
    List<Delivery> selectByStudentId(Long studentId);
    
    /**
     * 根据岗位ID查询投递记录
     * 
     * @param jobId 岗位ID
     * @return 投递记录列表
     */
    List<Delivery> selectByJobId(Long jobId);
    
    /**
     * 根据状态查询投递记录
     * 
     * @param status 状态（0=已投递，1=企业已查看，2=待面试，3=已录用，4=不合适）
     * @return 投递记录列表
     */
    List<Delivery> selectByStatus(Integer status);
    
    /**
     * 根据学生ID和状态查询投递记录
     * 
     * @param studentId 学生ID
     * @param status 状态
     * @return 投递记录列表
     */
    List<Delivery> selectByStudentIdAndStatus(Long studentId, Integer status);
    
    /**
     * 根据岗位ID和状态查询投递记录
     * 
     * @param jobId 岗位ID
     * @param status 状态
     * @return 投递记录列表
     */
    List<Delivery> selectByJobIdAndStatus(Long jobId, Integer status);
    
    /**
     * 学生投递简历
     * 
     * @param studentId 学生ID
     * @param jobId 岗位ID
     * @param resumeVersion 简历版本快照
     * @return 是否成功
     */
    boolean deliverResume(Long studentId, Long jobId, String resumeVersion);
    
    /**
     * 更新投递状态（企业查看、面试、录用、不合适）
     * 
     * @param deliveryId 投递记录ID
     * @param status 新状态
     * @param feedback 反馈信息
     * @return 是否成功
     */
    boolean updateDeliveryStatus(Long deliveryId, Integer status, String feedback);
    
    /**
     * 安排面试
     * 
     * @param deliveryId 投递记录ID
     * @param interviewTime 面试时间
     * @param interviewLocation 面试地点
     * @return 是否成功
     */
    boolean arrangeInterview(Long deliveryId, java.time.LocalDateTime interviewTime, String interviewLocation);
    
    /**
     * 统计岗位的投递数量
     * 
     * @param jobId 岗位ID
     * @return 投递数量
     */
    Integer countByJobId(Long jobId);
    
    /**
     * 统计学生投递数量
     * 
     * @param studentId 学生ID
     * @return 投递数量
     */
    Integer countByStudentId(Long studentId);
    
    /**
     * 统计岗位不同状态的投递数量（用于统计图表）
     * 
     * @param jobId 岗位ID
     * @return Map<状态, 数量>
     */
    Map<Integer, Integer> countByJobIdAndGroupByStatus(Long jobId);
}
