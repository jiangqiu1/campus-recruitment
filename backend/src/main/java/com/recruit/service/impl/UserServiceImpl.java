package com.recruit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recruit.entity.SysUser;
import com.recruit.mapper.SysUserMapper;
import com.recruit.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {
    
    @Autowired
    private SysUserMapper userMapper;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @Override
    public SysUser selectByUsername(String username) {
        return userMapper.selectByUsername(username);
    }
    
    @Override
    public SysUser selectByWechatOpenid(String openid) {
        return userMapper.selectByWechatOpenid(openid);
    }
    
    @Override
    public SysUser login(String username, String password, Integer role) {
        // 1. 根据用户名查询用户
        SysUser user = userMapper.selectByUsername(username);
        
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 2. 验证密码（BCrypt 加密）
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 3. 验证角色（如果传了角色则校验，不传则跳过）
        if (role != null && !user.getRole().equals(role)) {
            throw new RuntimeException("用户角色不匹配");
        }
        
        // 4. 验证账号状态
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }
        
        return user;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser register(SysUser user) {
        // 1. 检查用户名是否已存在
        SysUser existUser = userMapper.selectByUsername(user.getUsername());
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 2. 加密密码（BCrypt）
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        
        // 3. 设置默认名称和状态
        if (user.getRealName() == null || user.getRealName().isEmpty()) {
            user.setRealName(user.getUsername());
        }
        user.setStatus(1); // 正常状态
        
        // 4. 保存用户
        save(user);
        
        return user;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePassword(Long userId, String oldPassword, String newPassword) {
        // 1. 查询用户
        SysUser user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 2. 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }
        
        // 3. 加密新密码
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        
        // 4. 更新密码
        int rows = userMapper.updatePassword(userId, encodedNewPassword);
        
        return rows > 0;
    }
}
