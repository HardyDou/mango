package io.mango.common.crypto.asymmetric;

import io.mango.common.crypto.base.Base64Utils;
import io.mango.common.crypto.support.Signer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Security;

/**
 * SM2 signer implementation ( 国密签名 )
 *
 * @author Mango
 */
@Component
public class Sm2Signer implements Signer {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Value("${mango.crypto.sm2-private-key:}")
    private byte[] privateKey;

    @Value("${mango.crypto.sm2-public-key:}")
    private byte[] publicKey;

    @Override
    public String sign(String data) {
        // SM2 signature implementation placeholder
        // In production, use BC's SM2Signer with proper key pair
        throw new UnsupportedOperationException("SM2 signing not yet fully implemented - requires SM2 key pair");
    }

    @Override
    public boolean verify(String data, String signature) {
        // SM2 signature verification implementation placeholder
        throw new UnsupportedOperationException("SM2 verification not yet fully implemented - requires SM2 key pair");
    }
}
