package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recruit.entity.Job;
import com.recruit.mapper.JobMapper;
import com.recruit.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 岗位服务实现类
 */
@Service
public class JobServiceImpl extends ServiceImpl<JobMapper, Job> implements JobService {
    
    @Autowired
    private JobMapper jobMapper;
    
    @Override
    public List<Job> selectByCompanyId(Long companyId) {
        return jobMapper.selectByCompanyId(companyId);
    }
    
    @Override
    public List<Job> selectByCreatedBy(Long createdBy) {
        return jobMapper.selectByCreatedBy(createdBy);
    }
    
    @Override
    public List<Job> selectActiveJobs() {
        LocalDate today = LocalDate.now();
        return jobMapper.selectActiveJobs(today);
    }
    
    @Override
    public List<Job> selectByStatus(Integer status) {
        return jobMapper.selectByStatus(status);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean incrementViewCount(Long jobId) {
        int rows = jobMapper.incrementViewCount(jobId);
        return rows > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishJob(Long jobId) {
        Job job = getById(jobId);
        if (job == null) {
            throw new RuntimeException("岗位不存在");
        }
        
        // 只有草稿状态可以发布
        if (job.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的岗位可以发布");
        }
        
        job.setStatus(1); // 已发布
        
        // 生成追踪ID（如果为空）
        if (job.getTraceId() == null || job.getTraceId().isEmpty()) {
            job.setTraceId(generateTraceId());
        }
        
        // 生成二维码（TODO：实际项目需要调用OSS上传）
        if (job.getQrCodeUrl() == null || job.getQrCodeUrl().isEmpty()) {
            job.setQrCodeUrl(generateQrCode(jobId));
        }
        
        return updateById(job);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closeJob(Long jobId) {
        Job job = getById(jobId);
        if (job == null) {
            throw new RuntimeException("岗位不存在");
        }
        
        job.setStatus(2); // 已关闭
        return updateById(job);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean pauseJob(Long jobId) {
        Job job = getById(jobId);
        if (job == null) {
            throw new RuntimeException("岗位不存在");
        }
        
        job.setStatus(3); // 暂停
        return updateById(job);
    }
    
    @Override
    public Integer countByCompanyId(Long companyId) {
        return jobMapper.selectByCompanyId(companyId).size();
    }
    
    @Override
    public Integer countDeliveries(Long jobId) {
        return jobMapper.countByJobId(jobId);
    }
    
    @Override
    public String generateTraceId() {
        // 生成UUID作为追踪ID
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }
    
    @Override
    public String generateQrCode(Long jobId) {
        // TODO：实际项目中需要生成二维码并上传到OSS
        // 这里先返回一个占位符URL
        return "/uploads/qrcode/job_" + jobId + ".png";
    }
}
