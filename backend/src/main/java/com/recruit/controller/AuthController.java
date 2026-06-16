package com.recruit.controller;

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
import java.util.concurrent.TimeUnit;

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

    @Value("${jwt.prefix}")
    private String prefix;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        SysUser user = userService.login(
                request.getUsername(),
                request.getPassword(),
                request.getRole()
        );

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().toString()
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
        return Result.success(user);
    }

    @PutMapping("/update-password")
    public Result<String> updatePassword(@RequestBody Map<String, String> params) {
        String token = params.get("token");
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        if (token == null || token.isEmpty()) {
            return Result.error(401, "Token不能为空");
        }
        if (oldPassword == null || newPassword == null || newPassword.isEmpty()) {
            return Result.error("旧密码和新密码不能为空");
        }

        if (token.startsWith(prefix)) {
            token = token.substring(prefix.length());
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error(401, "Token无效");
        }

        String blacklistKey = "blacklist:" + token;
        Object blacklisted = redisUtil.get(blacklistKey);
        if (blacklisted != null) {
            return Result.error(401, "Token已失效");
        }

        SysUser user = userService.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        try {
            userService.updatePassword(userId, oldPassword, newPassword);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }

        return Result.success("密码修改成功");
    }

    @GetMapping("/me")
    public Result<SysUser> getCurrentUser(@RequestParam String token) {
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