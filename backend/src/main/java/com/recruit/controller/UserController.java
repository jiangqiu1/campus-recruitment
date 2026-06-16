package com.recruit.controller;

import com.recruit.entity.SysUser;
import com.recruit.service.UserService;
import com.recruit.utils.AESUtil;
import com.recruit.utils.Result;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 * 只有管理员可以访问
 */
@RestController
@RequestMapping("/admin/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AESUtil aesUtil;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * 获取所有用户列表
     * 
     * @return 用户列表
     */
    @GetMapping
    public Result<List<SysUser>> getAllUsers() {
        List<SysUser> users = userService.list();
        
        // AES解密敏感字段（手机号），失败时不阻断整个列表
        for (SysUser user : users) {
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                try {
                    user.setPhone(aesUtil.decrypt(user.getPhone()));
                } catch (Exception e) {
                    user.setPhone("[加密数据]");
                }
            }
        }
        
        return Result.success(users);
    }
    
    /**
     * 根据ID获取用户
     * 
     * @param id 用户ID
     * @return 用户实体
     */
    @GetMapping("/{id}")
    public Result<SysUser> getUserById(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        
        // AES解密敏感字段
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            user.setPhone(aesUtil.decrypt(user.getPhone()));
        }
        
        return Result.success(user);
    }
    
    /**
     * 根据角色获取用户列表
     * 
     * @param role 角色（0=学生，1=教师，2=企业HR，3=管理员）
     * @return 用户列表
     */
    @GetMapping("/by-role/{role}")
    public Result<List<SysUser>> getUsersByRole(@PathVariable Integer role) {
        List<SysUser> users = userService.list().stream()
                .filter(user -> user.getRole().equals(role))
                .collect(java.util.stream.Collectors.toList());
        
        // AES解密敏感字段（手机号），失败时显示占位符
        for (SysUser user : users) {
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                try {
                    user.setPhone(aesUtil.decrypt(user.getPhone()));
                } catch (Exception e) {
                    user.setPhone("[加密数据]");
                }
            }
        }
        
        return Result.success(users);
    }
    
    /**
     * 创建用户（管理员添加用户）
     * 
     * @param user 用户实体
     * @return 创建结果
     */
    @PostMapping
    public Result<String> createUser(@RequestBody SysUser user) {
        // 1. 检查用户名是否已存在
        SysUser existUser = userService.lambdaQuery()
                .eq(SysUser::getUsername, user.getUsername())
                .one();
        if (existUser != null) {
            return Result.error("用户名已存在");
        }
        
        // 2. 加密密码（BCrypt）
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        // 3. AES加密敏感字段（手机号）
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            user.setPhone(aesUtil.encrypt(user.getPhone()));
        }
        
        // 4. 保存用户
        userService.save(user);
        
        return Result.success("用户创建成功");
    }
    
    /**
     * 更新用户
     * 
     * @param id 用户ID
     * @param user 用户实体（包含要更新的字段）
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result<String> updateUser(@PathVariable Long id, @RequestBody SysUser user) {
        SysUser existUser = userService.getById(id);
        if (existUser == null) {
            return Result.error(404, "用户不存在");
        }
        
        // AES加密敏感字段（手机号）
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            user.setPhone(aesUtil.encrypt(user.getPhone()));
        }
        
        user.setId(id);
        userService.updateById(user);
        
        return Result.success("用户更新成功");
    }
    
    /**
     * 删除用户（软删除）
     * 
     * @param id 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteUser(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        
        // 软删除（设置deleted=1）
        user.setDeleted(1);
        userService.updateById(user);
        
        return Result.success("用户删除成功");
    }
    
    /**
     * 禁用/启用用户
     * 
     * @param id 用户ID
     * @param params 包含status的参数（0=禁用，1=启用）
     * @return 操作结果
     */
    @PutMapping("/{id}/status")
    public Result<String> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        SysUser user = userService.getById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        
        Integer status = params.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.error("status参数错误（应为0或1）");
        }
        
        user.setStatus(status);
        userService.updateById(user);
        
        return Result.success(status == 0 ? "用户已禁用" : "用户已启用");
    }
    
    /**
     * 重置用户密码
     * 
     * @param id 用户ID
     * @param params 包含newPassword的参数
     * @return 重置结果
     */
    @PutMapping("/{id}/reset-password")
    public Result<String> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> params) {
        SysUser user = userService.getById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        
        String newPassword = params.get("newPassword");
        if (newPassword == null || newPassword.isEmpty()) {
            return Result.error("新密码不能为空");
        }
        
        // 加密新密码
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userService.updateById(user);
        
        return Result.success("密码重置成功");
    }
}
