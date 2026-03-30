package io.mango.crypto.asymmetric;

import io.mango.crypto.base.Base64Utils;
import io.mango.crypto.support.AsymmetricCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA asymmetric cipher implementation
 *
 * @author Mango
 */
@Component
public class RsaCipher implements AsymmetricCipher {

    private static final String ALGORITHM = "RSA";
    private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    @Value("${mango.crypto.rsa-public-key:}")
    private String publicKeyBase64;

    @Value("${mango.crypto.rsa-private-key:}")
    private String privateKeyBase64;

    @Override
    public String encrypt(String data) {
        try {
            PublicKey publicKey = KeyFactory.getInstance(ALGORITHM)
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64Utils.encode(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("RSA encryption failed", e);
        }
    }

    @Override
    public String decrypt(String encrypted) {
        throw new UnsupportedOperationException("RSA decryption requires private key configuration");
    }

    public byte[] encrypt(byte[] data) {
        try {
            PublicKey publicKey = KeyFactory.getInstance(ALGORITHM)
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("RSA encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] encrypted) {
        throw new UnsupportedOperationException("RSA decryption requires private key configuration");
    }
}
