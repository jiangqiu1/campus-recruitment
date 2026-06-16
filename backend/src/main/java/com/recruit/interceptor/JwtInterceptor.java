package com.recruit.interceptor;

import com.recruit.utils.JwtUtil;
import com.recruit.utils.RedisUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

/**
 * JWT 拦截器 - 用于验证 Token
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private RedisUtil redisUtil;
    
    @Value("${jwt.header}")
    private String header;
    
    @Value("${jwt.prefix}")
    private String prefix;
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从请求头中获取 Token
        String token = request.getHeader(header);
        
        if (Objects.isNull(token) || !token.startsWith(prefix)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未授权，请重新登录\",\"data\":null}");
            return false;
        }
        
        // 2. 去除前缀，获取纯 Token
        token = token.substring(prefix.length());
        
        // 3. 解析 Token
        Claims claims = jwtUtil.getClaimsFromToken(token);
        
        if (Objects.isNull(claims)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token 无效\",\"data\":null}");
            return false;
        }
        
        // 4. 检查 Token 是否过期
        if (jwtUtil.isTokenExpired(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token 已过期，请重新登录\",\"data\":null}");
            return false;
        }
        
        // 5. 从 Redis 中检查 Token 是否存在（可选：实现单点登录）
        Long userId = jwtUtil.getUserIdFromToken(token);
        String redisKey = "token:" + userId;
        String cachedToken = redisUtil.get(redisKey);
        
        if (Objects.nonNull(cachedToken) && !cachedToken.equals(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"账号已在其他地方登录\",\"data\":null}");
            return false;
        }
        
        // 6. 将用户信息存储到请求属性中，方便后续使用
        request.setAttribute("userId", userId);
        request.setAttribute("username", jwtUtil.getUsernameFromToken(token));
        request.setAttribute("role", jwtUtil.getRoleFromToken(token));
        
        return true;
    }
}
