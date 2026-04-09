package io.mango.infra.crypto.impl.rsa;

import io.mango.infra.crypto.impl.IAsymmetricCryptoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA encryption implementation using RSA/ECB/OAEPWithSHA-256AndMGF1Padding.
 *
 * Note: This is a public-key encryption for client-side use.
 * The decrypt() operation always throws UnsupportedOperationException
 * because private key is not available for client-side decryption.
 */
@Component
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

    private byte[] decode(String encoded) {
        return Base64.getDecoder().decode(encoded);
    }

    @Override
    public String encrypt(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getPublicKey());
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("RSA encryption failed", e);
        }
    }

    @Override
    public String decrypt(String encrypted) {
        // Client-side RSA cannot decrypt (no private key available)
        throw new UnsupportedOperationException(
            "RSA decryption is not supported on client side. " +
            "This operation requires server-side processing with private key access."
        );
    }
}
