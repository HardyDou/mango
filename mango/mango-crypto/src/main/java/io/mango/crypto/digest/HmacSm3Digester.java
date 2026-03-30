package io.mango.crypto.digest;

import io.mango.crypto.support.Digester;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Security;

/**
 * HMAC-SM3 digest implementation ( 国密HMAC )
 *
 * @author Mango
 */
@Component
public class HmacSm3Digester implements Digester {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String ALGORITHM = "SM3";
    private static final String MAC_ALGORITHM = "SM3withHMAC";

    @Value("${mango.crypto.hmac-sm3-key:}")
    private byte[] key;

    @Override
    public String digest(String data) {
        return bytesToHex(digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public byte[] digest(byte[] data) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            Mac mac = Mac.getInstance(MAC_ALGORITHM, "BC");
            mac.init(keySpec);
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SM3 digest failed", e);
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
