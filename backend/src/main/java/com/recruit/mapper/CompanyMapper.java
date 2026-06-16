package com.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruit.entity.Company;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 企业表 Mapper 接口
 */
@Mapper
public interface CompanyMapper extends BaseMapper<Company> {
    
    /**
     * 根据合作等级查询企业
     * 
     * @param cooperationLevel 合作等级（0=潜在，1=合作中，2=核心，3=已流失）
     * @return 企业列表
     */
    @Select("SELECT * FROM company WHERE cooperation_level = #{cooperationLevel} AND deleted = 0")
    List<Company> selectByCooperationLevel(@Param("cooperationLevel") Integer cooperationLevel);
    
    /**
     * 根据行业查询企业
     * 
     * @param industry 行业
     * @return 企业列表
     */
    @Select("SELECT * FROM company WHERE industry = #{industry} AND deleted = 0")
    List<Company> selectByIndustry(@Param("industry") String industry);
}
