package com.recruit.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 工具单元测试（纯逻辑，不依赖 Spring 上下文）
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "recruitment-platform-secret-key-2025");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 604800000L); // 7天
        ReflectionTestUtils.setField(jwtUtil, "header", "Authorization");
        ReflectionTestUtils.setField(jwtUtil, "prefix", "Bearer ");
    }

    @Test
    @DisplayName("生成并解析 Token：userId/username/role 往返一致")
    void generateAndParse() {
        String token = jwtUtil.generateToken(6L, "S001", "0");
        assertNotNull(token);
        assertTrue(token.startsWith("eyJ"));

        assertEquals(6L, jwtUtil.getUserIdFromToken(token));
        assertEquals("S001", jwtUtil.getUsernameFromToken(token));
        assertEquals("0", jwtUtil.getRoleFromToken(token));
    }

    @Test
    @DisplayName("篡改 Token 解析返回 null（优雅拒绝，不抛未捕获异常）")
    void tamperedTokenRejected() {
        String token = jwtUtil.generateToken(6L, "S001", "0");
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertNull(jwtUtil.getUserIdFromToken(tampered));
        assertNull(jwtUtil.getUsernameFromToken(tampered));
    }

    @Test
    @DisplayName("垃圾字符串/空串解析返回 null")
    void garbageTokenRejected() {
        assertNull(jwtUtil.getUserIdFromToken("not-a-token"));
        assertNull(jwtUtil.getUserIdFromToken(""));
        assertNull(jwtUtil.getRoleFromToken("garbage"));
    }

    @Test
    @DisplayName("过期 Token 解析返回 null")
    void expiredTokenRejected() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L); // 已过期
        String token = jwtUtil.generateToken(6L, "S001", "0");
        assertNull(jwtUtil.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("不同用户生成的 Token 互相不可替换身份")
    void tokenIdentityIsolated() {
        String t1 = jwtUtil.generateToken(6L, "S001", "0");
        String t2 = jwtUtil.generateToken(7L, "S002", "0");
        assertNotEquals(jwtUtil.getUserIdFromToken(t1), jwtUtil.getUserIdFromToken(t2));
    }
}
