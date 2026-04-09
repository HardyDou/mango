package io.mango.infra.crypto.impl.digest;

import io.mango.infra.crypto.impl.IDigester;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HMAC-SM3 digest implementation using BouncyCastle.
 */
@Component
public class HmacSm3Digester implements IDigester {

    private static final String MAC_ALGORITHM = "SM3withHMAC";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public String digest(String data) {
        throw new UnsupportedOperationException("HMAC-SM3 requires a key, use digest(data, key) instead");
    }

    @Override
    public byte[] digest(byte[] data) {
        throw new UnsupportedOperationException("HMAC-SM3 requires a key, use digest(data, key) instead");
    }

    /**
     * Digest data with HMAC-SM3.
     *
     * @param data data to digest
     * @param key  secret key
     * @return raw digest bytes
     */
    public byte[] digest(byte[] data, byte[] key) {
        try {
            Mac mac = Mac.getInstance(MAC_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
            SecretKeySpec keySpec = new SecretKeySpec(key, "SM3withHMAC");
            mac.init(keySpec);
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SM3 failed", e);
        }
    }

    /**
     * Digest data with HMAC-SM3, returns hex string.
     *
     * @param data data to digest
     * @param key  secret key
     * @return hex-encoded digest
     */
    public String digest(String data, byte[] key) {
        return bytesToHex(digest(data.getBytes(StandardCharsets.UTF_8), key));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
