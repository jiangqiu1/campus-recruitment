package com.recruit.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES 加密工具类（标准Java实现）
 * 用于加密/解密敏感字段（手机号、身份证等）
 * 使用 AES/CBC/PKCS5Padding 模式
 */
@Component
public class AESUtil {
    
    @Value("${aes.key:recruitment-aes-key-12}")
    private String key;
    
    @Value("${aes.iv:recruitment-iv123}")
    private String iv;
    
    private SecretKeySpec secretKey;
    private IvParameterSpec ivParameterSpec;
    
    @PostConstruct
    public void init() {
        // 初始化密钥和IV（确保长度正确）
        byte[] keyBytes = padKey(key.getBytes(StandardCharsets.UTF_8), 16);
        byte[] ivBytes = padKey(iv.getBytes(StandardCharsets.UTF_8), 16);
        
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.ivParameterSpec = new IvParameterSpec(ivBytes);
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
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encrypted);
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
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            byte[] decrypted = cipher.doFinal(hexToBytes(encrypted));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES解密失败", e);
        }
    }
    
    /**
     * 加密（返回Base64字符串）
     */
    public String encryptBase64(String content) {
        if (content == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败", e);
        }
    }
    
    /**
     * 解密（Base64字符串）
     */
    public String decryptBase64(String encrypted) {
        if (encrypted == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encrypted));
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
