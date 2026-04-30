package io.mango.infra.crypto.impl.aes;

import io.mango.infra.crypto.impl.ICryptoService;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES/GCM 加解密基础实现。
 * <p>
 * 当前实现会把 12 字节 IV 前置到密文中，认证标签长度为 128 位。
 */
public class AesCipher implements ICryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    @Value("${mango.crypto.aes-key:}")
    private String keyBase64;

    @Value("${mango.crypto.aes-iv:}")
    private String ivBase64;

    private byte[] getKey() {
        return keyBase64 == null || keyBase64.isEmpty() ? null : Base64.getDecoder().decode(keyBase64);
    }

    private byte[] getIv() {
        return ivBase64 == null || ivBase64.isEmpty() ? null : Base64.getDecoder().decode(ivBase64);
    }

    private String encodeToString(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private byte[] decode(String encoded) {
        return Base64.getDecoder().decode(encoded);
    }

    @Override
    public String encrypt(String plaintext) {
        return encrypt(plaintext, getIv() != null ? encodeToString(getIv()) : null);
    }

    @Override
    public String encrypt(String plaintext, String iv) {
        if (plaintext == null) {
            throw new IllegalArgumentException("plaintext 不能为空");
        }
        try {
            byte[] ivBytes;
            if (iv != null && !iv.isEmpty()) {
                ivBytes = decode(iv);
            } else {
                ivBytes = new byte[IV_LENGTH];
                java.security.SecureRandom random = new java.security.SecureRandom();
                random.nextBytes(ivBytes);
            }

            SecretKeySpec keySpec = new SecretKeySpec(getKey(), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[ivBytes.length + ciphertext.length];
            System.arraycopy(ivBytes, 0, combined, 0, ivBytes.length);
            System.arraycopy(ciphertext, 0, combined, ivBytes.length, ciphertext.length);

            return encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES 加密失败", e);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        return decrypt(ciphertext, null);
    }

    @Override
    public String decrypt(String ciphertext, String iv) {
        if (ciphertext == null) {
            throw new IllegalArgumentException("ciphertext 不能为空");
        }
        try {
            byte[] combined = decode(ciphertext);

            byte[] ivBytes = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, ivBytes, 0, IV_LENGTH);

            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            SecretKeySpec keySpec = new SecretKeySpec(getKey(), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES 解密失败", e);
        }
    }
}
