package com.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruit.entity.Job;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 岗位表 Mapper 接口
 */
@Mapper
public interface JobMapper extends BaseMapper<Job> {
    
    /**
     * 根据企业ID查询岗位
     * 
     * @param companyId 企业ID
     * @return 岗位列表
     */
    @Select("SELECT * FROM job WHERE company_id = #{companyId} AND deleted = 0")
    List<Job> selectByCompanyId(@Param("companyId") Long companyId);
    
    /**
     * 根据发布者ID查询岗位
     * 
     * @param createdBy 发布者ID（教师或HR）
     * @return 岗位列表
     */
    @Select("SELECT * FROM job WHERE created_by = #{createdBy} AND deleted = 0")
    List<Job> selectByCreatedBy(@Param("createdBy") Long createdBy);
    
    /**
     * 查询有效岗位（已发布 + 未截止）
     * 
     * @return 有效岗位列表
     */
    @Select("SELECT * FROM job WHERE status = 1 AND deadline >= #{today} AND deleted = 0")
    List<Job> selectActiveJobs(@Param("today") LocalDate today);
    
    /**
     * 根据状态查询岗位
     * 
     * @param status 状态（0=草稿，1=已发布，2=已关闭，3=暂停）
     * @return 岗位列表
     */
    @Select("SELECT * FROM job WHERE status = #{status} AND deleted = 0")
    List<Job> selectByStatus(@Param("status") Integer status);
    
    /**
     * 增加岗位浏览次数
     * 
     * @param jobId 岗位ID
     * @return 影响行数
     */
    int incrementViewCount(@Param("jobId") Long jobId);
    
    /**
     * 根据岗位ID统计投递数量
     * 
     * @param jobId 岗位ID
     * @return 投递数量
     */
    Integer countByJobId(@Param("jobId") Long jobId);
}
