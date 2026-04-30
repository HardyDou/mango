package io.mango.infra.crypto.impl.digest;

import io.mango.infra.crypto.impl.IKeyedDigester;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.Security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HMAC-SM3 摘要实现。
 * <p>
 * HMAC 必须显式传入密钥，因此不实现无密钥摘要接口。
 */
public class HmacSm3Digester implements IKeyedDigester {

    private static final String MAC_ALGORITHM = "HMACSM3";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public byte[] digest(byte[] data, byte[] key) {
        if (data == null) {
            throw new IllegalArgumentException("data 不能为空");
        }
        if (key == null || key.length == 0) {
            throw new IllegalArgumentException("key 不能为空");
        }
        try {
            Mac mac = Mac.getInstance(MAC_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
            SecretKeySpec keySpec = new SecretKeySpec(key, MAC_ALGORITHM);
            mac.init(keySpec);
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SM3 计算失败", e);
        }
    }

    @Override
    public String digest(String data, byte[] key) {
        if (data == null) {
            throw new IllegalArgumentException("data 不能为空");
        }
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
