package com.recruit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置类
 * 使用 BCrypt 加密密码
 */
@Configuration
public class PasswordEncoderConfig {
    
    /**
     * 创建 BCryptPasswordEncoder Bean
     * 
     * @return PasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // strength=8：校内系统够安全，匹配速度约15ms（默认strength=10约70ms）
        return new BCryptPasswordEncoder(8);
    }
}
