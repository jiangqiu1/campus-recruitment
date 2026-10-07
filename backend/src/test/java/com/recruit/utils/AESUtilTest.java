package com.recruit.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AES 加密工具单元测试（敏感字段加密口径）
 */
class AESUtilTest {

    private AESUtil aesUtil;

    @BeforeEach
    void setUp() {
        aesUtil = new AESUtil();
        ReflectionTestUtils.setField(aesUtil, "key", "recruitment-aes-key-12");
        ReflectionTestUtils.setField(aesUtil, "legacyIv", "recruitment-iv123");
        aesUtil.init();
    }

    @Test
    @DisplayName("加密后可解密还原（往返一致）")
    void encryptDecryptRoundtrip() {
        String phone = "13600000000";
        String encrypted = aesUtil.encrypt(phone);
        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("v1:"), "新格式应带 v1: 前缀");
        assertEquals(phone, aesUtil.decrypt(encrypted));
    }

    @Test
    @DisplayName("相同明文两次加密产生不同密文（随机 IV）")
    void randomIvProducesDifferentCipher() {
        String a = aesUtil.encrypt("13600000000");
        String b = aesUtil.encrypt("13600000000");
        assertNotEquals(a, b);
        assertEquals(aesUtil.decrypt(a), aesUtil.decrypt(b));
    }

    @Test
    @DisplayName("密文被篡改后解密应失败")
    void tamperedCipherRejected() {
        String encrypted = aesUtil.encrypt("13600000000");
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "ffff";
        assertThrows(Exception.class, () -> aesUtil.decrypt(tampered));
    }

    @Test
    @DisplayName("非密文输入解密失败（保护历史明文兼容逻辑）")
    void plainTextDecryptFails() {
        assertThrows(Exception.class, () -> aesUtil.decrypt("13800001111"));
    }

    @Test
    @DisplayName("null 输入返回 null 不抛异常")
    void nullSafe() {
        assertNull(aesUtil.encrypt(null));
        assertNull(aesUtil.decrypt(null));
    }

    @Test
    @DisplayName("中文内容往返一致")
    void chineseRoundtrip() {
        String text = "测试内容-聘";
        assertEquals(text, aesUtil.decrypt(aesUtil.encrypt(text)));
    }
}
