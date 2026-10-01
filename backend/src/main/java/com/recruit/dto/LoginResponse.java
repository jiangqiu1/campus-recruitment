package com.recruit.dto;

import lombok.Data;

/**
 * 登录响应 DTO
 */
@Data
public class LoginResponse {

    /**
     * JWT Token
     */
    private String token;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 角色：0=学生，1=教师，2=企业HR，3=管理员
     */
    private Integer role;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 所属企业ID（HR角色使用）
     */
    private Long companyId;

    /**
     * 手机号（解密后返回）
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 学校名称
     */
    private String school;

    /**
     * 专业名称
     */
    private String major;
}
