package io.mango.common.crypto.asymmetric;

import io.mango.common.crypto.base.Base64Utils;
import io.mango.common.crypto.support.Signer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA signer implementation
 *
 * @author Mango
 */
@Component
public class RsaSigner implements Signer {

    private static final String ALGORITHM = "SHA256withRSA";

    @Value("${mango.crypto.rsa-private-key:}")
    private String privateKeyBase64;

    @Value("${mango.crypto.rsa-public-key:}")
    private String publicKeyBase64;

    @Override
    public String sign(String data) {
        try {
            PrivateKey privateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyBase64)));

            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));

            return Base64Utils.encode(signature.sign());
        } catch (Exception e) {
            throw new RuntimeException("RSA signing failed", e);
        }
    }

    @Override
    public boolean verify(String data, String signatureBase64) {
        try {
            java.security.PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));

            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));

            return signature.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            throw new RuntimeException("RSA signature verification failed", e);
        }
    }
}
