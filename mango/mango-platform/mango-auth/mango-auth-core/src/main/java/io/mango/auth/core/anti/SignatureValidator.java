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
 * Request signature validator.
 * Supports SM2/RSA/MD5 algorithms.
 */
@Slf4j
@Component
public class SignatureValidator {

    static {
        // Register BouncyCastle provider for SM2 support
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
     * Validate signature.
     * @param algorithm signature algorithm (SM2/RSA/MD5)
     * @param appKey application key
     * @param secret for SM2/RSA: base64-encoded public key; for MD5: secret key
     * @param timestamp request timestamp
     * @param body request body
     * @param sign provided signature
     * @return true=valid, false=invalid
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
     * Build the data string to be signed.
     * Format: appKey + secret + timestamp + body (sorted alphabetically)
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
     * Compute signature for data string (only for MD5).
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
     * Validate SM2 signature using BouncyCastle.
     * @param publicKeyBase64 base64-encoded SM2 public key
     * @param data data that was signed
     * @param signHex hex-encoded signature
     * @return true if valid
     */
    private boolean validateSM2(String publicKeyBase64, String data, String signHex) {
        try {
            byte[] pubKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            byte[] signBytes = hexToBytes(signHex);

            // Use BouncyCastle SM2Signer for verification
            // SM2Signer handles SM3 hashing internally
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
     * Validate RSA signature using Java Signature API.
     * @param publicKeyBase64 base64-encoded RSA public key (X.509 encoded)
     * @param data data that was signed
     * @param signBase64 base64-encoded signature
     * @return true if valid
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
