package com.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.recruit.entity.Company;

import java.util.List;

/**
 * 企业服务接口
 */
public interface CompanyService extends IService<Company> {
    
    /**
     * 根据合作等级查询企业
     * 
     * @param cooperationLevel 合作等级（0=潜在，1=合作中，2=核心，3=已流失）
     * @return 企业列表
     */
    List<Company> selectByCooperationLevel(Integer cooperationLevel);
    
    /**
     * 根据行业查询企业
     * 
     * @param industry 行业
     * @return 企业列表
     */
    List<Company> selectByIndustry(String industry);
    
    /**
     * 更新企业合作等级
     * 
     * @param companyId 企业ID
     * @param cooperationLevel 新的合作等级
     * @return 是否成功
     */
    boolean updateCooperationLevel(Long companyId, Integer cooperationLevel);
    
    /**
     * 更新最近招聘时间
     * 
     * @param companyId 企业ID
     * @return 是否成功
     */
    boolean updateLastRecruitTime(Long companyId);
}
