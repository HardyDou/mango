package io.mango.infra.crypto.impl.rsa;

import io.mango.infra.crypto.impl.ISignService;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 签名验签基础实现，使用 SHA256withRSA。
 */
public class RsaSigner implements ISignService {

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    @Value("${mango.crypto.rsa-private-key:}")
    private String privateKeyBase64;

    @Value("${mango.crypto.rsa-public-key:}")
    private String publicKeyBase64;

    private PrivateKey getPrivateKey() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

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
    public String sign(String data) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(getPrivateKey());
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return encodeToString(signature.sign());
        } catch (Exception e) {
            throw new RuntimeException("RSA 签名失败", e);
        }
    }

    @Override
    public boolean verify(String data, String signatureBase64) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(getPublicKey());
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.verify(decode(signatureBase64));
        } catch (Exception e) {
            throw new RuntimeException("RSA 验签失败", e);
        }
    }
}
