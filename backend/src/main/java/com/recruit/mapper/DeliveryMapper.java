package com.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruit.entity.Delivery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 投递记录表 Mapper 接口
 */
@Mapper
public interface DeliveryMapper extends BaseMapper<Delivery> {
    
    /**
     * 根据学生ID查询投递记录
     * 
     * @param studentId 学生ID
     * @return 投递记录列表
     */
    @Select("SELECT * FROM delivery WHERE student_id = #{studentId} AND deleted = 0")
    List<Delivery> selectByStudentId(@Param("studentId") Long studentId);
    
    /**
     * 根据岗位ID查询投递记录
     * 
     * @param jobId 岗位ID
     * @return 投递记录列表
     */
    @Select("SELECT * FROM delivery WHERE job_id = #{jobId} AND deleted = 0")
    List<Delivery> selectByJobId(@Param("jobId") Long jobId);
    
    /**
     * 根据状态查询投递记录
     * 
     * @param status 状态（0=已投递，1=企业已查看，2=待面试，3=已录用，4=不合适）
     * @return 投递记录列表
     */
    @Select("SELECT * FROM delivery WHERE status = #{status} AND deleted = 0")
    List<Delivery> selectByStatus(@Param("status") Integer status);
    
    /**
     * 根据学生ID和状态查询投递记录
     * 
     * @param studentId 学生ID
     * @param status 状态
     * @return 投递记录列表
     */
    @Select("SELECT * FROM delivery WHERE student_id = #{studentId} AND status = #{status} AND deleted = 0")
    List<Delivery> selectByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") Integer status);
    
    /**
     * 根据岗位ID和状态查询投递记录
     * 
     * @param jobId 岗位ID
     * @param status 状态
     * @return 投递记录列表
     */
    @Select("SELECT * FROM delivery WHERE job_id = #{jobId} AND status = #{status} AND deleted = 0")
    List<Delivery> selectByJobIdAndStatus(@Param("jobId") Long jobId, @Param("status") Integer status);
    
    /**
     * 统计岗位的投递数量
     * 
     * @param jobId 岗位ID
     * @return 投递数量
     */
    @Select("SELECT COUNT(*) FROM delivery WHERE job_id = #{jobId} AND deleted = 0")
    Integer countByJobId(@Param("jobId") Long jobId);
}
