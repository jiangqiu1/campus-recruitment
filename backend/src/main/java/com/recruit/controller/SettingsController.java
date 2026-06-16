package com.recruit.controller;

import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统设置控制器
 * 提供系统设置的获取和保存功能，数据暂存内存中
 */
@RestController
@RequestMapping("/settings")
public class SettingsController {

    @Value("${spring.application.name:校园招聘平台}")
    private String systemName;

    @Value("${aes.key:recruitment-aes-key-12}")
    private String aesKey;

    @Value("${aes.iv:recruitment-iv123}")
    private String aesIv;

    @Value("${jwt.secret:RecruitmentSecretKey2024!@#$%}")
    private String jwtSecret;

    @Value("${jwt.expiration:604800}")
    private Long jwtExpiration;

    private final Map<String, Object> settings = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        settings.put("systemName", systemName);
        settings.put("pageSize", 20);
        settings.put("logo", "");
        settings.put("jwtSecret", jwtSecret);
        settings.put("jwtExpiration", jwtExpiration);
        settings.put("aesKey", aesKey);
        settings.put("aesIv", aesIv);
        settings.put("emailEnabled", false);
        settings.put("smtpHost", "smtp.example.com");
        settings.put("smtpPort", 587);
        settings.put("smtpUsername", "");
        settings.put("smtpPassword", "");
    }

    /**
     * 获取所有系统设置（返回嵌套结构以匹配前端格式）
     */
    @GetMapping
    public Result<Map<String, Object>> getSettings() {
        Map<String, Object> basic = new HashMap<>();
        basic.put("systemName", settings.get("systemName"));
        basic.put("pageSize", settings.get("pageSize"));
        basic.put("logo", settings.get("logo"));

        Map<String, Object> security = new HashMap<>();
        security.put("jwtSecret", settings.get("jwtSecret"));
        security.put("jwtExpiration", settings.get("jwtExpiration"));
        security.put("aesKey", settings.get("aesKey"));

        Map<String, Object> notification = new HashMap<>();
        notification.put("emailEnabled", settings.get("emailEnabled"));
        notification.put("smtpHost", settings.get("smtpHost"));
        notification.put("smtpPort", settings.get("smtpPort"));
        notification.put("smtpUsername", settings.get("smtpUsername"));
        notification.put("smtpPassword", settings.get("smtpPassword"));

        Map<String, Object> result = new HashMap<>();
        result.put("basic", basic);
        result.put("security", security);
        result.put("notification", notification);
        return Result.success(result);
    }

    /**
     * 保存基础设置
     */
    @PostMapping("/basic")
    public Result<String> saveBasic(@RequestBody Map<String, Object> basicSettings) {
        if (basicSettings.containsKey("systemName")) {
            settings.put("systemName", basicSettings.get("systemName"));
        }
        if (basicSettings.containsKey("pageSize")) {
            settings.put("pageSize", basicSettings.get("pageSize"));
        }
        if (basicSettings.containsKey("logo")) {
            settings.put("logo", basicSettings.get("logo"));
        }
        return Result.success("基础设置保存成功");
    }

    /**
     * 保存安全设置
     */
    @PostMapping("/security")
    public Result<String> saveSecurity(@RequestBody Map<String, Object> securitySettings) {
        if (securitySettings.containsKey("jwtSecret")) {
            settings.put("jwtSecret", securitySettings.get("jwtSecret"));
        }
        if (securitySettings.containsKey("jwtExpiration")) {
            settings.put("jwtExpiration", securitySettings.get("jwtExpiration"));
        }
        if (securitySettings.containsKey("aesKey")) {
            settings.put("aesKey", securitySettings.get("aesKey"));
        }
        if (securitySettings.containsKey("aesIv")) {
            settings.put("aesIv", securitySettings.get("aesIv"));
        }
        return Result.success("安全设置保存成功");
    }

    /**
     * 保存通知设置
     */
    @PostMapping("/notification")
    public Result<String> saveNotification(@RequestBody Map<String, Object> notificationSettings) {
        if (notificationSettings.containsKey("emailEnabled")) {
            settings.put("emailEnabled", notificationSettings.get("emailEnabled"));
        }
        if (notificationSettings.containsKey("smtpHost")) {
            settings.put("smtpHost", notificationSettings.get("smtpHost"));
        }
        if (notificationSettings.containsKey("smtpPort")) {
            settings.put("smtpPort", notificationSettings.get("smtpPort"));
        }
        if (notificationSettings.containsKey("smtpUsername")) {
            settings.put("smtpUsername", notificationSettings.get("smtpUsername"));
        }
        if (notificationSettings.containsKey("smtpPassword")) {
            settings.put("smtpPassword", notificationSettings.get("smtpPassword"));
        }
        return Result.success("通知设置保存成功");
    }
}
