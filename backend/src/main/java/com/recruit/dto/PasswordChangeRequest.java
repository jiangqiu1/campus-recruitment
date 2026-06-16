package com.recruit.dto;

import lombok.Data;

/**
 * 修改密码请求
 */
@Data
public class PasswordChangeRequest {
    private String oldPassword;
    private String newPassword;
}
