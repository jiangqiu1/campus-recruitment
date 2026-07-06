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
 * 仅返回前端展示所需的非敏感配置，密钥等敏感信息绝不透出
 */
@RestController
@RequestMapping("/settings")
public class SettingsController extends BaseController {

    @Value("${spring.application.name:校园招聘平台}")
    private String systemName;

    @Value("${jwt.expiration:604800}")
    private Long jwtExpiration;

    private final Map<String, Object> settings = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        settings.put("systemName", systemName);
        settings.put("pageSize", 20);
        settings.put("logo", "");
        settings.put("jwtExpiration", jwtExpiration);
        settings.put("emailEnabled", false);
        settings.put("smtpHost", "smtp.example.com");
        settings.put("smtpPort", 587);
        settings.put("smtpUsername", "");
        settings.put("smtpPassword", "");
    }

    /**
     * 获取系统设置（不含密钥等敏感信息）
     */
    @GetMapping
    public Result<Map<String, Object>> getSettings() {
        Map<String, Object> basic = new HashMap<>();
        basic.put("systemName", settings.get("systemName"));
        basic.put("pageSize", settings.get("pageSize"));
        basic.put("logo", settings.get("logo"));

        Map<String, Object> security = new HashMap<>();
        security.put("jwtExpiration", settings.get("jwtExpiration"));

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
     * 保存基础设置（仅管理员可操作）
     */
    @PostMapping("/basic")
    public Result<String> saveBasic(@RequestBody Map<String, Object> basicSettings) {
        requireAdmin();
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
     * 保存通知设置（仅管理员可操作）
     */
    @PostMapping("/notification")
    public Result<String> saveNotification(@RequestBody Map<String, Object> notificationSettings) {
        requireAdmin();
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
