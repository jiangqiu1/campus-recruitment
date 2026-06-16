package com.recruit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.recruit.entity.SysUser;

/**
 * 用户服务接口
 */
public interface UserService extends IService<SysUser> {
    
    /**
     * 根据用户名查询用户
     * 
     * @param username 用户名（学号/工号）
     * @return 用户实体
     */
    SysUser selectByUsername(String username);
    
    /**
     * 根据微信OpenID查询用户
     * 
     * @param openid 微信OpenID
     * @return 用户实体
     */
    SysUser selectByWechatOpenid(String openid);
    
    /**
     * 用户登录验证
     * 
     * @param username 用户名
     * @param password 密码（明文）
     * @param role 角色
     * @return 登录成功的用户实体
     * @throws RuntimeException 用户名或密码错误、角色不匹配、账号禁用
     */
    SysUser login(String username, String password, Integer role);
    
    /**
     * 用户注册
     * 
     * @param user 用户实体（包含用户名、密码、角色等）
     * @return 注册成功的用户实体
     * @throws RuntimeException 用户名已存在
     */
    SysUser register(SysUser user);
    
    /**
     * 更新用户密码
     * 
     * @param userId 用户ID
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文）
     * @return 是否成功
     * @throws RuntimeException 用户不存在或旧密码错误
     */
    boolean updatePassword(Long userId, String oldPassword, String newPassword);
}
