package io.mango.infra.crypto.impl.sm;

import io.mango.infra.crypto.impl.ISignService;
import io.mango.infra.crypto.starter.CryptoProperties;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithID;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * SM2 signature service implementation.
 * Uses SM2 algorithm with user ID for signature.
 */
public class Sm2SignService implements ISignService {

    private final CryptoProperties.Sm2Config config;
    private static final ECDomainParameters DOMAIN_PARAMS;

    static {
        BouncyCastleLoader.ensure();
        DOMAIN_PARAMS = new ECDomainParameters(
                GMNamedCurves.getByName("sm2p256v1").getCurve(),
                GMNamedCurves.getByName("sm2p256v1").getG(),
                GMNamedCurves.getByName("sm2p256v1").getN(),
                GMNamedCurves.getByName("sm2p256v1").getH()
        );
    }

    public Sm2SignService(CryptoProperties properties) {
        this.config = properties.getSm2();
        if (config.getUserId() == null || config.getUserId().isEmpty()) {
            throw new IllegalStateException("SM2 userId cannot be null or empty");
        }
    }

    @Override
    public String sign(String data) {
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }
        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodeKey(config.getPrivateKey()));
            KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleLoader.PROVIDER_NAME);
            ECPrivateKey privateKey = (ECPrivateKey) keyFactory.generatePrivate(keySpec);

            ECPrivateKeyParameters privateKeyParams = new ECPrivateKeyParameters(
                    privateKey.getS(),
                    DOMAIN_PARAMS
            );

            SM2Signer signer = new SM2Signer();
            ParametersWithID params = new ParametersWithID(privateKeyParams, config.getUserId().getBytes(StandardCharsets.UTF_8));
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            signer.init(true, params);
            signer.update(dataBytes, 0, dataBytes.length);

            byte[] signature = signer.generateSignature();
            return Base64.toBase64String(signature);
        } catch (Exception e) {
            throw new RuntimeException("SM2 signing failed", e);
        }
    }

    @Override
    public boolean verify(String data, String signature) {
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }
        if (signature == null) {
            throw new IllegalArgumentException("signature cannot be null");
        }
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodeKey(config.getPublicKey()));
            KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleLoader.PROVIDER_NAME);
            ECPublicKey publicKey = (ECPublicKey) keyFactory.generatePublic(keySpec);

            java.security.spec.ECPoint point = publicKey.getW();
            BigInteger x = point.getAffineX();
            BigInteger y = point.getAffineY();

            ECPublicKeyParameters publicKeyParams = new ECPublicKeyParameters(
                    DOMAIN_PARAMS.getCurve().createPoint(x, y),
                    DOMAIN_PARAMS
            );

            SM2Signer verifier = new SM2Signer();
            ParametersWithID params = new ParametersWithID(publicKeyParams, config.getUserId().getBytes(StandardCharsets.UTF_8));
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            verifier.init(false, params);
            verifier.update(dataBytes, 0, dataBytes.length);

            return verifier.verifySignature(Base64.decode(signature));
        } catch (Exception e) {
            throw new RuntimeException("SM2 signature verification failed", e);
        }
    }

    byte[] decodeKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        try {
            return Base64.decode(key);
        } catch (Exception e) {
            // Not Base64, try Hex
            try {
                return Hex.decode(key);
            } catch (Exception hexEx) {
                throw new IllegalArgumentException(
                        "Key is neither valid Base64 nor Hex: " +
                        (key.length() <= 64 ? key : key.substring(0, 64) + "..."), hexEx);
            }
        }
    }
}
