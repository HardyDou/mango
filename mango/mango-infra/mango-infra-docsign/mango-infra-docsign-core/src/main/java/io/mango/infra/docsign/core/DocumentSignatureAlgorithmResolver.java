package io.mango.infra.docsign.core;

import io.mango.common.result.Require;
import io.mango.infra.docsign.enums.DocumentSignatureAlgorithm;

import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.Signature;
import java.security.interfaces.ECKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;

/**
 * Resolves caller-selected algorithms and rejects incompatible key or certificate material.
 */
final class DocumentSignatureAlgorithmResolver {

    private static final byte[] KEY_MATCH_CHALLENGE =
            "mango-docsign-key-match-v1".getBytes(StandardCharsets.US_ASCII);
    private static final ECParameterSpec SM2_PARAMETERS = sm2Parameters();

    private DocumentSignatureAlgorithmResolver() {
    }

    static DocumentSignatureAlgorithm resolvePdf(DocumentSignatureAlgorithm requested,
                                                  LoadedKeyMaterial keyMaterial) {
        DocumentSignatureAlgorithm resolved = requested == DocumentSignatureAlgorithm.AUTO
                ? defaultAlgorithm(keyMaterial.privateKey()) : requested;
        requireCompatibleKeyMaterial(resolved, keyMaterial);
        return resolved;
    }

    static DocumentSignatureAlgorithm resolveOfd(DocumentSignatureAlgorithm requested,
                                                  LoadedKeyMaterial keyMaterial) {
        DocumentSignatureAlgorithm resolved = requested == DocumentSignatureAlgorithm.AUTO
                ? DocumentSignatureAlgorithm.SM3_WITH_SM2 : requested;
        Require.isTrue(resolved == DocumentSignatureAlgorithm.SM3_WITH_SM2,
                "OFD 标准签名只支持 SM3_WITH_SM2 算法");
        requireCompatibleKeyMaterial(resolved, keyMaterial);
        return resolved;
    }

    static String jcaName(DocumentSignatureAlgorithm algorithm) {
        return switch (algorithm) {
            case SHA256_WITH_RSA -> "SHA256withRSA";
            case SHA384_WITH_RSA -> "SHA384withRSA";
            case SHA512_WITH_RSA -> "SHA512withRSA";
            case SHA256_WITH_RSA_PSS -> "SHA256withRSAandMGF1";
            case SM3_WITH_SM2 -> "SM3withSM2";
            case AUTO -> throw new IllegalArgumentException("AUTO 签名算法必须先解析为具体算法");
        };
    }

    private static DocumentSignatureAlgorithm defaultAlgorithm(Key privateKey) {
        if ("RSA".equalsIgnoreCase(privateKey.getAlgorithm())) {
            return DocumentSignatureAlgorithm.SHA256_WITH_RSA;
        }
        Require.isTrue(isSm2Key(privateKey),
                "EC 私钥必须使用 sm2p256v1 曲线；普通 ECDSA 证书当前不受支持");
        return DocumentSignatureAlgorithm.SM3_WITH_SM2;
    }

    private static void requireCompatibleKeyMaterial(DocumentSignatureAlgorithm algorithm,
                                                     LoadedKeyMaterial keyMaterial) {
        if (algorithm == DocumentSignatureAlgorithm.SM3_WITH_SM2) {
            Require.isTrue(isSm2Key(keyMaterial.privateKey())
                            && isSm2Key(keyMaterial.certificate().getPublicKey()),
                    "SM3_WITH_SM2 要求私钥和签名证书均使用 sm2p256v1 曲线");
        } else {
            Require.isTrue("RSA".equalsIgnoreCase(keyMaterial.privateKey().getAlgorithm())
                            && "RSA".equalsIgnoreCase(keyMaterial.certificate().getPublicKey().getAlgorithm()),
                    algorithm + " 要求 RSA 私钥和 RSA 签名证书");
        }
        requireMatchingKeyPair(algorithm, keyMaterial);
    }

    private static boolean isSm2Key(Key key) {
        if (!(key instanceof ECKey ecKey) || ecKey.getParams() == null) {
            return false;
        }
        ECParameterSpec actual = ecKey.getParams();
        return SM2_PARAMETERS.getCurve().equals(actual.getCurve())
                && SM2_PARAMETERS.getGenerator().equals(actual.getGenerator())
                && SM2_PARAMETERS.getOrder().equals(actual.getOrder())
                && SM2_PARAMETERS.getCofactor() == actual.getCofactor();
    }

    private static ECParameterSpec sm2Parameters() {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance(
                    "EC", CryptoProviderSupport.provider());
            parameters.init(new ECGenParameterSpec("sm2p256v1"));
            return parameters.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("初始化 SM2 曲线参数失败", ex);
        }
    }

    private static void requireMatchingKeyPair(DocumentSignatureAlgorithm algorithm,
                                               LoadedKeyMaterial keyMaterial) {
        try {
            Signature signer = Signature.getInstance(jcaName(algorithm), CryptoProviderSupport.provider());
            signer.initSign(keyMaterial.privateKey());
            signer.update(KEY_MATCH_CHALLENGE);
            byte[] signature = signer.sign();
            signer.initVerify(keyMaterial.certificate().getPublicKey());
            signer.update(KEY_MATCH_CHALLENGE);
            Require.isTrue(signer.verify(signature), "签名私钥与叶子证书公钥不匹配");
        } catch (GeneralSecurityException ex) {
            throw new IllegalArgumentException("校验签名算法与证书密钥失败: " + algorithm, ex);
        }
    }
}
