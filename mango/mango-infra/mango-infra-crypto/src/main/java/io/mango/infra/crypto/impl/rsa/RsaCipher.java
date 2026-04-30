package io.mango.infra.crypto.impl.rsa;

import io.mango.infra.crypto.impl.IAsymmetricCryptoService;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 公钥加密实现，使用 RSA/ECB/OAEPWithSHA-256AndMGF1Padding。
 * <p>
 * 当前类只支持公钥加密，不提供私钥解密能力。
 */
public class RsaCipher implements IAsymmetricCryptoService {

    private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    @Value("${mango.crypto.rsa-public-key:}")
    private String publicKeyBase64;

    private PublicKey getPublicKey() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    private String encodeToString(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("plaintext 不能为空");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getPublicKey());
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("RSA 加密失败", e);
        }
    }
}
