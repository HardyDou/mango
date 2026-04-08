package io.mango.infra.crypto.impl.sm;

import io.mango.infra.crypto.impl.ICryptoService;
import io.mango.infra.crypto.starter.CryptoProperties;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * SM4 symmetric encryption/decryption service implementation.
 * Supports ECB and CBC modes with PKCS5Padding.
 */
@Service
public class Sm4CryptoService implements ICryptoService {

    private static final String ALGORITHM = "SM4";
    private static final String ECB_TRANSFORMATION = "SM4/ECB/PKCS5Padding";
    private static final String CBC_TRANSFORMATION = "SM4/CBC/PKCS5Padding";

    private final CryptoProperties.Sm4Config config;

    public Sm4CryptoService(CryptoProperties properties) {
        this.config = properties.getSm4();
    }

    @Override
    public String encrypt(String plaintext) {
        return encrypt(plaintext, config.getIv());
    }

    @Override
    public String encrypt(String plaintext, String iv) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(decodeKey(config.getSecretKey()), ALGORITHM);
            String transformation = (iv != null) ? CBC_TRANSFORMATION : ECB_TRANSFORMATION;

            Cipher cipher = Cipher.getInstance(transformation);
            if (iv != null) {
                IvParameterSpec ivSpec = new IvParameterSpec(decodeKey(iv));
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            }

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.toBase64String(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("SM4 encryption failed", e);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        return decrypt(ciphertext, config.getIv());
    }

    @Override
    public String decrypt(String ciphertext, String iv) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(decodeKey(config.getSecretKey()), ALGORITHM);
            String transformation = (iv != null) ? CBC_TRANSFORMATION : ECB_TRANSFORMATION;

            Cipher cipher = Cipher.getInstance(transformation);
            if (iv != null) {
                IvParameterSpec ivSpec = new IvParameterSpec(decodeKey(iv));
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, keySpec);
            }

            byte[] decrypted = cipher.doFinal(Base64.decode(ciphertext));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("SM4 decryption failed", e);
        }
    }

    private byte[] decodeKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        // Try Base64 first, then Hex
        try {
            return Base64.decode(key);
        } catch (Exception e) {
            return Hex.decode(key);
        }
    }
}
