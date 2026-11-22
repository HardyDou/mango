package io.mango.infra.web.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

/**
 * HMAC-SHA256 签名原语。
 * <p>
 * 本工具只提供「密钥 + 载荷 → 摘要」与常量时间比较，不承载任何协议语义（时间戳、nonce、query 规范化等）。
 * {@link InternalCallSignature} 保留其既有内部调用签名协议（固定 hex 输出、自有 canonical query 规则）不变；
 * 需要同一签名原语的新协议（例如无状态数据权限 Grant）复用本类，避免出现第三份 HMAC 实现。
 */
public final class HmacSignatures {

    private static final String ALGORITHM = "HmacSHA256";

    private HmacSignatures() {
    }

    /**
     * 计算 HMAC-SHA256 并输出小写十六进制摘要。
     *
     * @param secret  签名密钥明文，不得为 null。
     * @param payload 待签名载荷，按 UTF-8 编码，不得为 null。
     * @return 64 字符小写十六进制摘要。
     */
    public static String hmacSha256Hex(String secret, String payload) {
        return HexFormat.of().formatHex(hmacSha256(secret, payload));
    }

    /**
     * 计算 HMAC-SHA256 并输出无填充 base64url 摘要。
     *
     * @param secret  签名密钥明文，不得为 null。
     * @param payload 待签名载荷，按 UTF-8 编码，不得为 null。
     * @return 43 字符无填充 base64url 摘要。
     */
    public static String hmacSha256Base64Url(String secret, String payload) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacSha256(secret, payload));
    }

    /**
     * 以常量时间比较两段签名文本。
     *
     * @param actual   实际签名。
     * @param expected 期望签名。
     * @return 两者均非 null 且字节内容一致时返回 true。
     */
    public static boolean matches(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.US_ASCII),
                expected.getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] hmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256 signature", exception);
        }
    }
}
