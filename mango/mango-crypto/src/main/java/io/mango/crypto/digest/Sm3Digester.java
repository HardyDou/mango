package io.mango.crypto.digest;

import io.mango.crypto.support.Digester;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Security;

/**
 * SM3 digest implementation ( 国密摘要算法 )
 *
 * @author Mango
 */
@Component
public class Sm3Digester implements Digester {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public String digest(String data) {
        return bytesToHex(digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public byte[] digest(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SM3", "BC");
            return digest.digest(data);
        } catch (Exception e) {
            throw new RuntimeException("SM3 digest failed", e);
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
