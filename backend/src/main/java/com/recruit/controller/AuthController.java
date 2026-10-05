package com.recruit.controller;

import com.recruit.annotation.LogOperation;
import com.recruit.dto.LoginRequest;
import com.recruit.dto.LoginResponse;
import com.recruit.entity.SysUser;
import com.recruit.service.UserService;
import com.recruit.utils.JwtUtil;
import com.recruit.utils.RedisUtil;
import com.recruit.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.recruit.utils.AESUtil;

/**
 * 认证控制器
 * 处理登录、注册、登出等认证相关请求
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private AESUtil aesUtil;

    @Value("${jwt.prefix}")
    private String prefix;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        SysUser user = userService.login(
                request.getUsername(),
                request.getPassword(),
                request.getRole()
        );

        // 生成 Token 版本号（用于密码修改后失效）
        String tokenVersion = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String versionKey = "token:version:" + user.getId();
        redisUtil.setWithExpire(versionKey, tokenVersion, 7 * 24 * 60 * 60, TimeUnit.SECONDS);

        // 直接生成带版本号的Token，避免两次生成
        String token = jwtUtil.generateToken(
                user.getId(), user.getUsername(),
                user.getRole().toString(), tokenVersion
        );

        String redisKey = "token:" + user.getId();
        redisUtil.setWithExpire(redisKey, token, 7 * 24 * 60 * 60, TimeUnit.SECONDS);

        LoginResponse response = new LoginResponse();
        response.setToken(prefix + token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRole(user.getRole());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setCompanyId(user.getCompanyId());
        // 返回完整资料（手机号解密），保证小程序本地存储的基本信息完整，
        // 否则每次重新登录后简历完整度/个人信息会出现"待补充"假象
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            try { response.setPhone(aesUtil.decrypt(user.getPhone())); } catch (Exception ignored) { response.setPhone(user.getPhone()); }
        }
        response.setEmail(user.getEmail());
        response.setSchool(user.getSchool());
        response.setMajor(user.getMajor());

        return Result.success(response);
    }

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody LoginRequest request) {
        SysUser existUser = userService.selectByUsername(request.getUsername());
        if (existUser != null) {
            return Result.error("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRole(request.getRole());
        user.setStatus(1);
        userService.register(user);
        return Result.success("注册成功");
    }

    @PostMapping("/logout")
    public Result<String> logout(@RequestBody(required = false) Map<String, String> request) {
        String token = (request != null) ? request.get("token") : null;
        if (token == null || token.isEmpty()) {
            return Result.error("Token 不能为空");
        }

        if (token.startsWith(prefix)) {
            token = token.substring(prefix.length());
        }

        String blacklistKey = "blacklist:" + token;
        redisUtil.setWithExpire(blacklistKey, "1", 7 * 24 * 60 * 60, TimeUnit.SECONDS);
        return Result.success("登出成功");
    }

    @GetMapping("/userinfo")
    public Result<SysUser> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未授权");
        }
        SysUser user = userService.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        // 解密手机号
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            try { user.setPhone(aesUtil.decrypt(user.getPhone())); } catch (Exception ignored) { }
        }
        return Result.success(user);
    }

    @LogOperation("更新个人信息")
    @PutMapping("/profile")
    public Result<SysUser> updateProfile(HttpServletRequest request, @RequestBody SysUser profile) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未授权");
        }
        SysUser user = userService.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 只允许更新以下字段
        if (profile.getRealName() != null && !profile.getRealName().isEmpty()) {
            user.setRealName(profile.getRealName());
        }
        if (profile.getPhone() != null) {
            user.setPhone(aesUtil.encrypt(profile.getPhone()));
        }
        if (profile.getEmail() != null) {
            user.setEmail(profile.getEmail());
        }
        if (profile.getGender() != null) {
            user.setGender(profile.getGender());
        }
        if (profile.getAvatarUrl() != null) {
            user.setAvatarUrl(profile.getAvatarUrl());
        }
        if (profile.getSchool() != null) {
            user.setSchool(profile.getSchool());
        }
        if (profile.getMajor() != null) {
            user.setMajor(profile.getMajor());
        }

        userService.updateById(user);
        // 返回时解密手机号
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            user.setPhone(aesUtil.decrypt(user.getPhone()));
        }
        return Result.success(user);
    }

    /**
     * 获取指定用户的基本信息（仅返回 id、realName、role）
     * 用于 AI 匹配等场景显示学生姓名
     */
    @GetMapping("/user-basic/{id}")
    public Result<java.util.Map<String, Object>> getUserBasic(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        java.util.Map<String, Object> basic = new java.util.HashMap<>();
        basic.put("id", user.getId());
        basic.put("realName", user.getRealName());
        basic.put("role", user.getRole());
        return Result.success(basic);
    }

    @LogOperation("修改密码")
    @PutMapping("/update-password")
    public Result<String> updatePassword(HttpServletRequest request, @RequestBody Map<String, String> params) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未授权");
        }
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        if (oldPassword == null || newPassword == null || newPassword.isEmpty()) {
            return Result.error("旧密码和新密码不能为空");
        }

        try {
            userService.updatePassword(userId, oldPassword, newPassword);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }

        // 密码修改成功后，递增 Token 版本号使所有旧 Token 失效
        String versionKey = "token:version:" + userId;
        String newVersion = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        redisUtil.setWithExpire(versionKey, newVersion, 7 * 24 * 60 * 60, TimeUnit.SECONDS);

        return Result.success("密码修改成功");
    }

    @GetMapping("/me")
    public Result<SysUser> getCurrentUser(@RequestParam(required = false) String token,
                                          @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // token 优先从 header 取，避免进访问日志；兼容旧 query 方式
        if (token == null || token.isEmpty()) {
            token = authHeader;
        }
        if (token == null || token.isEmpty()) {
            return Result.error(401, "Token 不能为空");
        }

        if (token.startsWith(prefix)) {
            token = token.substring(prefix.length());
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error(401, "Token 无效");
        }

        String blacklistKey = "blacklist:" + token;
        Object blacklisted = redisUtil.get(blacklistKey);
        if (blacklisted != null) {
            return Result.error(401, "Token 已失效（已登出）");
        }

        SysUser user = userService.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        return Result.success(user);
    }
}