package com.recruit.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求 DTO
 */
@Data
public class LoginRequest {
    
    /**
     * 用户名（学号/工号）
     */
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
    
    /**
     * 角色：0=学生，1=教师，2=企业HR，3=管理员
     */
    private Integer role;
}
