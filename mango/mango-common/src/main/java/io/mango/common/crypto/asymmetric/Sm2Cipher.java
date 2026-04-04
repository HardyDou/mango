package io.mango.common.crypto.asymmetric;

import io.mango.common.crypto.base.Base64Utils;
import io.mango.common.crypto.support.AsymmetricCipher;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Security;

/**
 * SM2 asymmetric cipher implementation ( 国密非对称加密 )
 *
 * @author Mango
 */
@Component
public class Sm2Cipher implements AsymmetricCipher {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Value("${mango.crypto.sm2-public-key:}")
    private byte[] publicKey;

    @Value("${mango.crypto.sm2-private-key:}")
    private byte[] privateKey;

    @Override
    public String encrypt(String data) {
        return Base64Utils.encode(encrypt(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public String decrypt(String encrypted) {
        return new String(decrypt(Base64Utils.decode(encrypted)), StandardCharsets.UTF_8);
    }

    public byte[] encrypt(byte[] data) {
        throw new UnsupportedOperationException("SM2 encryption not yet fully implemented - requires SM2 public key");
    }

    public byte[] decrypt(byte[] encrypted) {
        throw new UnsupportedOperationException("SM2 decryption not yet fully implemented - requires SM2 private key");
    }
}
