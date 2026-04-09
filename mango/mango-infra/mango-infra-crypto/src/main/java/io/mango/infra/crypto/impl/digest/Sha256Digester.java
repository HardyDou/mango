package io.mango.infra.crypto.impl.digest;

import io.mango.infra.crypto.impl.IDigester;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 digest implementation using pure JDK.
 */
@Component
public class Sha256Digester implements IDigester {

    @Override
    public String digest(String data) {
        return bytesToHex(digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public byte[] digest(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
