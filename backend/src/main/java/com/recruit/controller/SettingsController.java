package com.recruit.controller;

import com.recruit.service.SysSettingService;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统设置控制器
 * 仅返回前端展示所需的非敏感配置，密钥等敏感信息绝不透出
 */
@RestController
@RequestMapping("/settings")
public class SettingsController extends BaseController {

    @Autowired
    private SysSettingService sysSettingService;

    /**
     * 获取系统设置（不含密钥等敏感信息）
     */
    @GetMapping
    public Result<Map<String, Object>> getSettings() {
        Map<String, Map<String, Object>> all = sysSettingService.getAllSettings();

        Map<String, Object> basic = all.getOrDefault("basic", new HashMap<>());
        Map<String, Object> security = new HashMap<>();
        // 安全设置只返回非敏感字段
        if (all.containsKey("security")) {
            Object exp = all.get("security").get("jwtExpiration");
            if (exp != null) security.put("jwtExpiration", exp);
        }
        Map<String, Object> notification = all.getOrDefault("notification", new HashMap<>());

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
        sysSettingService.saveGroupSettings("basic", basicSettings);
        return Result.success("基础设置保存成功");
    }

    /**
     * 保存安全设置（仅管理员可操作）
     */
    @PostMapping("/security")
    public Result<String> saveSecurity(@RequestBody Map<String, Object> securitySettings) {
        requireAdmin();
        sysSettingService.saveGroupSettings("security", securitySettings);
        return Result.success("安全设置保存成功");
    }

    /**
     * 保存通知设置（仅管理员可操作）
     */
    @PostMapping("/notification")
    public Result<String> saveNotification(@RequestBody Map<String, Object> notificationSettings) {
        requireAdmin();
        sysSettingService.saveGroupSettings("notification", notificationSettings);
        return Result.success("通知设置保存成功");
    }
}
