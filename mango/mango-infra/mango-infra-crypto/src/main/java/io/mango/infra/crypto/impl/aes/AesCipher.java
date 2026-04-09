package io.mango.infra.crypto.impl.aes;

import io.mango.infra.crypto.impl.ICryptoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES/GCM encryption implementation.
 * IV is prepended to ciphertext (first 12 bytes).
 * TAG is 128 bits.
 */
@Component
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

            // Prepend IV to ciphertext
            byte[] combined = new byte[ivBytes.length + ciphertext.length];
            System.arraycopy(ivBytes, 0, combined, 0, ivBytes.length);
            System.arraycopy(ciphertext, 0, combined, ivBytes.length, ciphertext.length);

            return encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        return decrypt(ciphertext, null);
    }

    @Override
    public String decrypt(String ciphertext, String iv) {
        try {
            byte[] combined = decode(ciphertext);

            // Extract IV from first 12 bytes
            byte[] ivBytes = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, ivBytes, 0, IV_LENGTH);

            // Extract actual ciphertext
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            SecretKeySpec keySpec = new SecretKeySpec(getKey(), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }
}
