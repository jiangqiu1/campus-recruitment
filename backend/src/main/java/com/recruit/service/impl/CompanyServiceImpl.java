package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recruit.entity.Company;
import com.recruit.mapper.CompanyMapper;
import com.recruit.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 企业服务实现类
 */
@Service
public class CompanyServiceImpl extends ServiceImpl<CompanyMapper, Company> implements CompanyService {
    
    @Autowired
    private CompanyMapper companyMapper;
    
    @Override
    public List<Company> selectByCooperationLevel(Integer cooperationLevel) {
        return companyMapper.selectByCooperationLevel(cooperationLevel);
    }
    
    @Override
    public List<Company> selectByIndustry(String industry) {
        return companyMapper.selectByIndustry(industry);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCooperationLevel(Long companyId, Integer cooperationLevel) {
        Company company = getById(companyId);
        if (company == null) {
            throw new RuntimeException("企业不存在");
        }
        
        company.setCooperationLevel(cooperationLevel);
        return updateById(company);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLastRecruitTime(Long companyId) {
        Company company = getById(companyId);
        if (company == null) {
            throw new RuntimeException("企业不存在");
        }
        
        company.setLastRecruitTime(LocalDateTime.now());
        return updateById(company);
    }
}
