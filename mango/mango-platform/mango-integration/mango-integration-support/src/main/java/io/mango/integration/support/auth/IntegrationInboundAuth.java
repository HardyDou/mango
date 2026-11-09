package io.mango.integration.support.auth;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

/**
 * 入站认证与报文解密工具。
 *
 * <p>供业务域校验回调签名并解密报文，避免各业务域重复实现签名与 AES 逻辑。</p>
 */
public final class IntegrationInboundAuth {

    private final IntegrationAuthConfig config;

    private IntegrationInboundAuth(IntegrationAuthConfig config) {
        this.config = config;
    }

    /**
     * 创建入站认证工具。
     *
     * @param config 认证配置
     * @return 入站认证工具
     */
    public static IntegrationInboundAuth of(IntegrationAuthConfig config) {
        return new IntegrationInboundAuth(config);
    }

    /**
     * 校验回调签名。
     *
     * @param params 请求参数，包含签名、时间戳、随机串
     * @param body 原始报文
     * @return 是否通过
     */
    public boolean verifySignature(Map<String, String> params, String body) {
        String signatureParam = config.param("signatureParam", "signature");
        String provided = params.get(signatureParam);
        if (provided == null || provided.isBlank()) {
            return false;
        }
        String timestamp = params.get(config.param("timestampParam", "timestamp"));
        String nonce = params.get(config.param("nonceParam", "nonce"));
        String secret = config.credential(config.param("secretField", null));
        String template = config.param("signedContent", "SECRET\nTIMESTAMP\nNONCE\nBODY");
        String payload = template
                .replace("SECRET", secret)
                .replace("TIMESTAMP", timestamp == null ? "" : timestamp)
                .replace("NONCE", nonce == null ? "" : nonce)
                .replace("BODY", body == null ? "" : body);
        String expected = digest(config.param("algorithm", "HmacSHA256"), secret, payload);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 校验时间戳是否在允许窗口内。
     *
     * @param params 请求参数
     * @return 是否在窗口内
     */
    public boolean withinReplayWindow(Map<String, String> params) {
        String timestamp = params.get(config.param("timestampParam", "timestamp"));
        if (timestamp == null || timestamp.isBlank()) {
            return true;
        }
        long windowSeconds = Long.parseLong(config.param("replayWindowSeconds", "300"));
        try {
            long value = timestamp.length() > 10
                    ? Long.parseLong(timestamp) : Long.parseLong(timestamp) * 1000L;
            return Math.abs(System.currentTimeMillis() - value) <= windowSeconds * 1000L;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    /**
     * 解密回调报文。
     *
     * @param encrypted 加密报文（Base64）
     * @return 明文
     */
    public String decrypt(String encrypted) {
        if (!Boolean.parseBoolean(config.param("encryptionEnabled", "false"))) {
            return encrypted;
        }
        try {
            byte[] key = Base64.getDecoder().decode(config.credential(config.param("keyField", null)));
            byte[] payload = Base64.getDecoder().decode(encrypted);
            byte[] iv = Arrays.copyOfRange(payload, 0, 16);
            byte[] data = Arrays.copyOfRange(payload, 16, payload.length);
            Cipher cipher = Cipher.getInstance(config.param("cipher", "AES/CBC/PKCS5Padding"));
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return new String(cipher.doFinal(data), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("报文解密失败");
        }
    }

    private String digest(String algorithm, String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hex.append(String.format(Locale.ROOT, "%02x", value));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("签名计算失败");
        }
    }
}
