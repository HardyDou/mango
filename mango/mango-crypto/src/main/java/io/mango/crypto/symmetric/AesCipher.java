package io.mango.crypto.symmetric;

import io.mango.crypto.base.Base64Utils;
import io.mango.crypto.support.SymmetricCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * AES symmetric cipher implementation
 * Uses GCM mode for authenticated encryption
 *
 * @author Mango
 */
@Component
public class AesCipher implements SymmetricCipher {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    @Value("${mango.crypto.aes-key:}")
    private byte[] key;

    @Value("${mango.crypto.aes-iv:}")
    private byte[] iv;

    @Override
    public String encrypt(String data) {
        return Base64Utils.encode(encrypt(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public String decrypt(String encrypted) {
        return new String(decrypt(Base64Utils.decode(encrypted)), StandardCharsets.UTF_8);
    }

    @Override
    public byte[] encrypt(byte[] data) {
        try {
            byte[] ivBytes = iv != null && iv.length > 0 ? iv : generateIv();

            SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherText = cipher.doFinal(data);

            byte[] result = new byte[ivBytes.length + cipherText.length];
            System.arraycopy(ivBytes, 0, result, 0, ivBytes.length);
            System.arraycopy(cipherText, 0, result, ivBytes.length, cipherText.length);

            return result;
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    @Override
    public byte[] decrypt(byte[] encrypted) {
        try {
            byte[] ivBytes = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[encrypted.length - GCM_IV_LENGTH];
            System.arraycopy(encrypted, 0, ivBytes, 0, GCM_IV_LENGTH);
            System.arraycopy(encrypted, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }

    private byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
