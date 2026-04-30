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
 * SM2 签名验签基础实现。
 * <p>
 * 使用 SM2 算法和用户 ID 参与签名。
 */
public class Sm2SignService implements ISignService {

    private final CryptoProperties.Sm2Config config;
    private static final ECDomainParameters DOMAIN_PARAMS;

    static {
        BouncyCastleLoader.ensure();
        var domainParams = GMNamedCurves.getByName("sm2p256v1");
        DOMAIN_PARAMS = new ECDomainParameters(
                domainParams.getCurve(),
                domainParams.getG(),
                domainParams.getN(),
                domainParams.getH()
        );
    }

    public Sm2SignService(CryptoProperties properties) {
        this.config = properties.getSm2();
        if (config.getUserId() == null || config.getUserId().isEmpty()) {
            throw new IllegalStateException("SM2 userId 不能为空");
        }
        if (config.getPrivateKey() == null || config.getPrivateKey().isEmpty()) {
            throw new IllegalStateException("SM2 privateKey 不能为空");
        }
        if (config.getPublicKey() == null || config.getPublicKey().isEmpty()) {
            throw new IllegalStateException("SM2 publicKey 不能为空");
        }
    }

    @Override
    public String sign(String data) {
        if (data == null) {
            throw new IllegalArgumentException("data 不能为空");
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
            throw new RuntimeException("SM2 签名失败", e);
        }
    }

    @Override
    public boolean verify(String data, String signature) {
        if (data == null) {
            throw new IllegalArgumentException("data 不能为空");
        }
        if (signature == null) {
            throw new IllegalArgumentException("signature 不能为空");
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
            throw new RuntimeException("SM2 验签失败", e);
        }
    }

    byte[] decodeKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key 不能为空");
        }
        try {
            return Base64.decode(key);
        } catch (Exception e) {
                try {
                return Hex.decode(key);
            } catch (Exception hexEx) {
                throw new IllegalArgumentException(
                        "key 既不是有效 Base64，也不是有效十六进制：" +
                        (key.length() <= 64 ? key : key.substring(0, 64) + "..."), hexEx);
            }
        }
    }
}
