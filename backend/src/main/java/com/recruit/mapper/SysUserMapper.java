package com.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruit.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户表 Mapper 接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    
    /**
     * 根据用户名查询用户
     * 
     * @param username 用户名（学号/工号）
     * @return 用户实体
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    SysUser selectByUsername(@Param("username") String username);
    
    /**
     * 根据微信OpenID查询用户
     * 
     * @param openid 微信OpenID
     * @return 用户实体
     */
    @Select("SELECT * FROM sys_user WHERE wechat_openid = #{openid} AND deleted = 0")
    SysUser selectByWechatOpenid(@Param("openid") String openid);
    
    /**
     * 根据角色查询用户列表
     * 
     * @param role 角色（0=学生，1=教师，2=企业HR，3=管理员）
     * @return 用户列表
     */
    @Select("SELECT * FROM sys_user WHERE role = #{role} AND deleted = 0")
    List<SysUser> selectByRole(@Param("role") Integer role);
    
    /**
     * 更新用户密码
     * 
     * @param userId 用户ID
     * @param password 新密码（加密后）
     * @return 影响行数
     */
    int updatePassword(@Param("userId") Long userId, @Param("password") String password);
}
