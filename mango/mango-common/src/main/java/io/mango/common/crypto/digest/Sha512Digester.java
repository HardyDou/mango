package io.mango.common.crypto.digest;

import io.mango.common.crypto.support.Digester;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * SHA-512 digest implementation
 *
 * @author Mango
 */
@Component
public class Sha512Digester implements Digester {

    @Override
    public String digest(String data) {
        return bytesToHex(digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public byte[] digest(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            return digest.digest(data);
        } catch (Exception e) {
            throw new RuntimeException("SHA-512 digest failed", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
