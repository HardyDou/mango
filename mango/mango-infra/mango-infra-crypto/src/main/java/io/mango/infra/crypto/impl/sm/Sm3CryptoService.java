package io.mango.infra.crypto.impl.sm;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;

/**
 * SM3 hash service implementation.
 * Produces 256-bit (32-byte) hash output.
 */
public class Sm3CryptoService {

    /**
     * Hash data using SM3.
     *
     * @param data data to hash
     * @return hex-encoded hash (64 characters)
     */
    public String hash(String data) {
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }
        return hash(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Hash data using SM3.
     *
     * @param data data to hash
     * @return hex-encoded hash (64 characters)
     */
    public String hash(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }
        SM3Digest digest = new SM3Digest();
        digest.update(data, 0, data.length);
        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        return Hex.toHexString(result);
    }
}
