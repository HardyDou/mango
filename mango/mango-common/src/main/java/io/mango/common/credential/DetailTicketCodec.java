package io.mango.common.credential;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 详情访问票据的编解码与签名工具。
 *
 * <p>票据文本格式：
 * <pre>mdt1.&lt;kid&gt;.&lt;iat&gt;.&lt;eps&gt;.&lt;sig&gt;.&lt;id&gt;</pre>
 * 各段之间用 {@code .} 分隔；{@code eps} 内的多个入口用 {@code ~} 分隔；
 * {@code sig} 是 HMAC-SHA256 截断到 8 字节后的 base64url 编码。
 * 签名原文包含调用方主体标识（租户、账号、权限指纹），因此票据不能跨主体重放。
 *
 * @author Mango
 */
public final class DetailTicketCodec {

    private static final String PREFIX = "mdt1";
    private static final String FIELD_SEPARATOR = ".";
    private static final String ENDPOINT_SEPARATOR = "~";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int SIGNATURE_BYTES = 8;

    private DetailTicketCodec() {
    }

    /**
     * 生成票据文本。
     *
     * @param ticket 票据内容
     * @param subject 主体标识，例如 {@code tenant|account|scope}
     * @param secret 签名密钥
     * @return 票据文本
     */
    public static String encode(DetailTicket ticket, String subject, byte[] secret) {
        if (ticket == null) {
            throw new IllegalArgumentException("票据不能为空");
        }
        if (ticket.endpoints() == null || ticket.endpoints().isEmpty()) {
            throw new IllegalArgumentException("票据必须包含至少一个入口");
        }
        if (ticket.dataId() == null || ticket.dataId().isBlank()) {
            throw new IllegalArgumentException("票据必须绑定数据标识");
        }
        String endpoints = String.join(ENDPOINT_SEPARATOR, ticket.endpoints());
        String signature = sign(ticket.kid(), ticket.issuedAt(), endpoints, ticket.dataId(), subject, secret);
        return String.join(FIELD_SEPARATOR, PREFIX, ticket.kid(), Long.toString(ticket.issuedAt()),
                endpoints, signature, ticket.dataId());
    }

    /**
     * 解析并验证票据。
     *
     * @param token 票据文本
     * @param subject 主体标识
     * @param secrets 密钥版本到密钥的映射，用于支持轮换
     * @return 解析结果
     */
    public static DetailTicket decode(String token, String subject, SecretLookup secrets) {
        if (token == null || token.isBlank()) {
            throw new DetailTicketException(DetailTicketException.Reason.MISSING);
        }
        String[] fields = token.trim().split("\\.", -1);
        if (fields.length != 6 || !PREFIX.equals(fields[0])) {
            throw new DetailTicketException(DetailTicketException.Reason.INVALID);
        }
        String kid = fields[1];
        long issuedAt;
        try {
            issuedAt = Long.parseLong(fields[2]);
        } catch (NumberFormatException exception) {
            throw new DetailTicketException(DetailTicketException.Reason.INVALID);
        }
        List<String> endpoints = new ArrayList<>(List.of(fields[3].split(ENDPOINT_SEPARATOR, -1)));
        if (endpoints.isEmpty() || endpoints.stream().anyMatch(String::isBlank)) {
            throw new DetailTicketException(DetailTicketException.Reason.INVALID);
        }
        String dataId = fields[5];
        byte[] secret = secrets.secret(kid);
        if (secret == null || secret.length == 0) {
            throw new DetailTicketException(DetailTicketException.Reason.INVALID);
        }
        String expected = sign(kid, issuedAt, fields[3], dataId, subject, secret);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                fields[4].getBytes(StandardCharsets.UTF_8))) {
            throw new DetailTicketException(DetailTicketException.Reason.INVALID);
        }
        return new DetailTicket(kid, issuedAt, endpoints, dataId);
    }

    /**
     * 密钥查询入口，便于支持多版本密钥并行验证。
     */
    @FunctionalInterface
    public interface SecretLookup {

        /**
         * 按版本取密钥。
         *
         * @param kid 密钥版本
         * @return 密钥；不存在时返回 {@code null}
         */
        byte[] secret(String kid);
    }

    private static String sign(String kid, long issuedAt, String endpoints, String dataId,
                               String subject, byte[] secret) {
        String payload = String.join("|", nullToEmpty(subject), kid, Long.toString(issuedAt), endpoints, dataId);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] truncated = new byte[SIGNATURE_BYTES];
            System.arraycopy(digest, 0, truncated, 0, SIGNATURE_BYTES);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(truncated);
        } catch (Exception exception) {
            throw new IllegalStateException("详情访问票据签名失败", exception);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
