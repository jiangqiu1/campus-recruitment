package com.recruit.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * AES 加密工具类（标准Java实现）
 * 用于加密/解密敏感字段（手机号、身份证等）
 * 使用 AES/CBC/PKCS5Padding 模式
 */
@Component
public class AESUtil {
    
    @Value("${aes.key}")
    private String key;
    
    @Value("${aes.iv:}")
    private String legacyIv;
    
    private SecretKeySpec secretKey;
    private IvParameterSpec legacyIvParameterSpec;
    
    private static final String PREFIX = "v1:";
    private static final SecureRandom RANDOM = new SecureRandom();
    
    @PostConstruct
    public void init() {
        // 校验密钥长度（AES 至少 16 字节）
        if (key == null || key.getBytes(StandardCharsets.UTF_8).length < 16) {
            throw new IllegalStateException("aes.key 未配置或长度不足 16 字节，请在 application.yml 中配置");
        }
        byte[] keyBytes = padKey(key.getBytes(StandardCharsets.UTF_8), 16);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        // 旧数据兼容用的固定 IV（可选，仅用于解密历史数据）
        if (legacyIv != null && !legacyIv.isEmpty()) {
            this.legacyIvParameterSpec = new IvParameterSpec(padKey(legacyIv.getBytes(StandardCharsets.UTF_8), 16));
        }
    }
    
    /**
     * 补齐密钥长度（AES要求16/24/32字节）
     */
    private byte[] padKey(byte[] key, int length) {
        byte[] padded = new byte[length];
        System.arraycopy(key, 0, padded, 0, Math.min(key.length, length));
        return padded;
    }
    
    /**
     * 加密（返回Hex字符串）
     */
    public String encrypt(String content) {
        if (content == null) {
            return null;
        }
        try {
            // 每次加密生成随机 IV，避免相同明文产生相同密文
            byte[] ivBytes = new byte[16];
            RANDOM.nextBytes(ivBytes);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(ivBytes));
            byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return PREFIX + bytesToHex(ivBytes) + bytesToHex(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败", e);
        }
    }
    
    /**
     * 解密（Hex字符串）
     */
    public String decrypt(String encrypted) {
        if (encrypted == null) {
            return null;
        }
        try {
            byte[] ivBytes;
            byte[] cipherBytes;
            if (encrypted.startsWith(PREFIX)) {
                // 新格式：v1: + IV(32 hex) + 密文
                String body = encrypted.substring(PREFIX.length());
                ivBytes = hexToBytes(body.substring(0, 32));
                cipherBytes = hexToBytes(body.substring(32));
            } else {
                // 旧格式：固定 IV 密文（兼容历史数据）
                if (legacyIvParameterSpec == null) {
                    throw new IllegalStateException("旧数据需配置 aes.iv 才能解密");
                }
                ivBytes = legacyIvParameterSpec.getIV();
                cipherBytes = hexToBytes(encrypted);
            }
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivBytes));
            byte[] decrypted = cipher.doFinal(cipherBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES解密失败", e);
        }
    }
    
    /**
     * Byte数组转Hex字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            String hexString = Integer.toHexString(0xff & b);
            if (hexString.length() == 1) {
                hex.append('0');
            }
            hex.append(hexString);
        }
        return hex.toString();
    }
    
    /**
     * Hex字符串转Byte数组
     */
    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
