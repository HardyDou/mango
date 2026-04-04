package io.mango.common.crypto.symmetric;

import io.mango.common.crypto.base.Base64Utils;
import io.mango.common.crypto.support.SymmetricCipher;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Security;

/**
 * SM4 symmetric cipher implementation ( 国密对称加密 )
 * Uses GCM mode for authenticated encryption
 *
 * @author Mango
 */
@Component
public class Sm4Cipher implements SymmetricCipher {

    private static final String ALGORITHM = "SM4";
    private static final String TRANSFORMATION = "SM4/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Value("${mango.crypto.sm4-key:}")
    private byte[] key;

    @Value("${mango.crypto.sm4-iv:}")
    private byte[] iv;

    @PostConstruct
    public void validate() {
        if (this.key == null || this.key.length == 0) {
            throw new IllegalStateException(
                "SM4 key not configured. Set mango.crypto.sm4-key environment variable.");
        }
    }

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
            // Generate random IV if not provided
            byte[] ivBytes = iv != null && iv.length > 0 ? iv : generateIv();

            SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION, "BC");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherText = cipher.doFinal(data);

            // Prepend IV to ciphertext
            byte[] result = new byte[ivBytes.length + cipherText.length];
            System.arraycopy(ivBytes, 0, result, 0, ivBytes.length);
            System.arraycopy(cipherText, 0, result, ivBytes.length, cipherText.length);

            return result;
        } catch (Exception e) {
            throw new RuntimeException("SM4 encryption failed", e);
        }
    }

    @Override
    public byte[] decrypt(byte[] encrypted) {
        try {
            // Extract IV from ciphertext
            byte[] ivBytes = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[encrypted.length - GCM_IV_LENGTH];
            System.arraycopy(encrypted, 0, ivBytes, 0, GCM_IV_LENGTH);
            System.arraycopy(encrypted, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION, "BC");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("SM4 decryption failed", e);
        }
    }

    private byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new java.security.SecureRandom().nextBytes(iv);
        return iv;
    }
}
