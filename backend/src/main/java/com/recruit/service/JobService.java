package com.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.recruit.entity.Job;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 岗位服务接口
 */
public interface JobService extends IService<Job> {
    
    /**
     * 根据企业ID查询岗位
     * 
     * @param companyId 企业ID
     * @return 岗位列表
     */
    List<Job> selectByCompanyId(Long companyId);
    
    /**
     * 根据发布者ID查询岗位
     * 
     * @param createdBy 发布者ID（教师或HR）
     * @return 岗位列表
     */
    List<Job> selectByCreatedBy(Long createdBy);
    
    /**
     * 查询有效岗位（已发布 + 未截止）
     * 
     * @return 有效岗位列表
     */
    List<Job> selectActiveJobs();
    
    /**
     * 根据状态查询岗位
     * 
     * @param status 状态（0=草稿，1=已发布，2=已关闭，3=暂停）
     * @return 岗位列表
     */
    List<Job> selectByStatus(Integer status);
    
    /**
     * 增加岗位浏览次数
     * 
     * @param jobId 岗位ID
     * @return 是否成功
     */
    boolean incrementViewCount(Long jobId);
    
    /**
     * 发布岗位（草稿 -> 已发布）
     * 
     * @param jobId 岗位ID
     * @return 是否成功
     */
    boolean publishJob(Long jobId);
    
    /**
     * 关闭岗位
     * 
     * @param jobId 岗位ID
     * @return 是否成功
     */
    boolean closeJob(Long jobId);
    
    /**
     * 暂停岗位
     * 
     * @param jobId 岗位ID
     * @return 是否成功
     */
    boolean pauseJob(Long jobId);
    
    /**
     * 统计企业岗位数量
     * 
     * @param companyId 企业ID
     * @return 岗位数量
     */
    Integer countByCompanyId(Long companyId);
    
    /**
     * 统计岗位投递数量
     * 
     * @param jobId 岗位ID
     * @return 投递数量
     */
    Integer countDeliveries(Long jobId);
    
    /**
     * 生成岗位追踪ID（唯一）
     * 
     * @return 追踪ID（UUID格式）
     */
    String generateTraceId();
    
    /**
     * 生成岗位二维码（存储到OSS）
     * 
     * @param jobId 岗位ID
     * @return 二维码URL
     */
    String generateQrCode(Long jobId);
}
