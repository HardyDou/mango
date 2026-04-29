package io.mango.auth.core.anti;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * 请求签名校验器。
 * 支持 SM2、RSA、MD5 算法。
 */
@Slf4j
@Component
public class SignatureValidator {

    static {
        // 注册 BouncyCastle Provider 以支持 SM2。
        if (java.security.Security.getProvider("BC") == null) {
            java.security.Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static final Map<String, String> ALGORITHM_MAP = Map.of(
        "SM2", "SM3withSM2",
        "RSA", "SHA256withRSA",
        "MD5", "MD5"
    );

    /**
     * 校验签名。
     * @param algorithm 签名算法，支持 SM2/RSA/MD5
     * @param appKey 应用标识
     * @param secret SM2/RSA 使用 Base64 公钥，MD5 使用密钥
     * @param timestamp 请求时间戳
     * @param body 请求体
     * @param sign 请求携带的签名
     * @return true 表示签名有效，false 表示签名无效
     */
    public boolean validate(String algorithm, String appKey, String secret,
                           String timestamp, String body, String sign) {
        if (algorithm == null || appKey == null || secret == null || timestamp == null || sign == null) {
            log.warn("Signature validation failed: missing required parameters");
            return false;
        }
        try {
            String data = buildSignatureData(appKey, secret, timestamp, body);
            String upperAlg = algorithm.toUpperCase();
            if ("SM2".equals(upperAlg)) {
                return validateSM2(secret, data, sign);
            } else if ("RSA".equals(upperAlg)) {
                return validateRSA(secret, data, sign);
            } else if ("MD5".equals(upperAlg)) {
                return validateMD5(data, sign);
            }
            log.warn("Unsupported algorithm: {}", algorithm);
            return false;
        } catch (Exception e) {
            log.error("Signature validation error: {}", e.getMessage());
            return false;
        }
    }

    private boolean validateMD5(String data, String sign) {
        String computed = md5(data);
        return computed.equalsIgnoreCase(sign);
    }

    /**
     * 构造待签名字符串。
     * 格式：appKey、secret、timestamp、body 按字典序拼接。
     */
    public String buildSignatureData(String appKey, String secret, String timestamp, String body) {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("appKey", appKey);
        params.put("secret", secret);
        params.put("timestamp", timestamp);
        if (body != null && !body.isBlank()) {
            params.put("body", body);
        }
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * 计算待签名字符串的签名，仅支持 MD5。
     */
    public String computeSignature(String algorithm, String data) {
        String algo = ALGORITHM_MAP.getOrDefault(algorithm.toUpperCase(), "MD5");
        try {
            if ("MD5".equals(algo)) {
                return md5(data);
            }
            throw new IllegalArgumentException("computeSignature only supports MD5, use validate() for SM2/RSA");
        } catch (Exception e) {
            log.error("Failed to compute signature: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 使用 BouncyCastle 校验 SM2 签名。
     * @param publicKeyBase64 Base64 编码的 SM2 公钥
     * @param data 已签名数据
     * @param signHex Hex 编码签名
     * @return 签名是否有效
     */
    private boolean validateSM2(String publicKeyBase64, String data, String signHex) {
        try {
            byte[] pubKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            byte[] signBytes = hexToBytes(signHex);

            // 使用 BouncyCastle SM2Signer 校验，SM3 哈希由 SM2Signer 内部处理。
            org.bouncycastle.crypto.signers.SM2Signer signer = new org.bouncycastle.crypto.signers.SM2Signer();
            org.bouncycastle.crypto.params.KeyParameter keyParam = new org.bouncycastle.crypto.params.KeyParameter(pubKeyBytes);
            signer.init(false, keyParam);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            signer.update(dataBytes, 0, dataBytes.length);
            return signer.verifySignature(signBytes);
        } catch (Exception e) {
            log.error("SM2 signature validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 使用 Java Signature API 校验 RSA 签名。
     * @param publicKeyBase64 Base64 编码的 RSA 公钥，X.509 格式
     * @param data 已签名数据
     * @param signBase64 Base64 编码签名
     * @return 签名是否有效
     */
    private boolean validateRSA(String publicKeyBase64, String data, String signBase64) {
        try {
            byte[] signBytes = Base64.getDecoder().decode(signBase64);
            byte[] pubKeyBytes = Base64.getDecoder().decode(publicKeyBase64);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(pubKeyBytes));

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(pubKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.verify(signBytes);
        } catch (Exception e) {
            log.error("RSA signature validation error: {}", e.getMessage());
            return false;
        }
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string");
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

    private String md5(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
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
