package io.mango.common.crypto.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Key manager for handling cryptographic keys
 * Keys should be provided via environment variables, never hardcoded
 *
 * @author Mango
 */
@Component
public class KeyManager {

    @Value("${mango.crypto.sm4-key:}")
    private String sm4Key;

    @Value("${mango.crypto.sm4-iv:}")
    private String sm4Iv;

    @Value("${mango.crypto.aes-key:}")
    private String aesKey;

    @Value("${mango.crypto.aes-iv:}")
    private String aesIv;

    @Value("${mango.crypto.enabled:true}")
    private boolean cryptoEnabled;

    /**
     * Check if crypto module is enabled
     */
    public boolean isCryptoEnabled() {
        return cryptoEnabled;
    }

    /**
     * Get SM4 key bytes
     */
    public byte[] getSm4Key() {
        if (sm4Key == null || sm4Key.isEmpty()) {
            throw new IllegalStateException("SM4 key not configured. Set mango.crypto.sm4-key environment variable.");
        }
        return sm4Key.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Get SM4 IV bytes
     */
    public byte[] getSm4Iv() {
        if (sm4Iv == null || sm4Iv.isEmpty()) {
            return null;
        }
        return sm4Iv.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Get AES key bytes
     */
    public byte[] getAesKey() {
        if (aesKey == null || aesKey.isEmpty()) {
            throw new IllegalStateException("AES key not configured. Set mango.crypto.aes-key environment variable.");
        }
        return aesKey.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Get AES IV bytes
     */
    public byte[] getAesIv() {
        if (aesIv == null || aesIv.isEmpty()) {
            return null;
        }
        return aesIv.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Derive a key from password using SM3
     */
    public byte[] deriveKey(String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SM3");
            digest.update(salt);
            return Arrays.copyOf(digest.digest(password.getBytes(StandardCharsets.UTF_8)), 32);
        } catch (Exception e) {
            throw new RuntimeException("Key derivation failed", e);
        }
    }
}
