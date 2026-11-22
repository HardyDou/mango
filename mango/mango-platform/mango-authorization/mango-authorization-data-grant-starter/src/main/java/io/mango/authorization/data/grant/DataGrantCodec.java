package io.mango.authorization.data.grant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.infra.web.util.HmacSignatures;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;

/**
 * Grant 编解码器。
 * <p>
 * 令牌格式为 {@code m1.hs256.<keyId>.<payload>.<signature>}，payload 与 signature 均为无填充 base64url，
 * 签名覆盖 {@code m1.hs256.<keyId>.<payload>} 全串，因此任何一段被替换都会导致验签失败。
 * 记录 ID 使用增量编码（升序、首个为绝对值、后续为差值），在 100 个 ID 量级下可把载荷压到几百字节。
 */
public class DataGrantCodec {

    private static final String TOKEN_VERSION = "m1";
    private static final String ALGORITHM = "hs256";
    private static final String MESSAGE_FORMAT_INVALID = "Grant 格式不正确";
    private static final String MESSAGE_VERSION_UNSUPPORTED = "Grant 版本或算法不受支持";
    private static final String MESSAGE_KEY_ID_MISMATCH = "Grant 签名密钥标识不匹配";
    private static final String MESSAGE_SIGNATURE_MISMATCH = "Grant 签名校验失败";
    private static final char SEPARATOR = '.';
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    /** 解码时的记录数上限，用于阻断畸形载荷放大攻击，与签发侧 {@code max-ids} 无关。 */
    private static final int MAX_DECODE_IDS = 10_000;

    private final DataGrantProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 构造编解码器。
     *
     * @param properties   Grant 配置。
     * @param objectMapper JSON 序列化器。
     */
    public DataGrantCodec(DataGrantProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 签发 Grant 令牌。
     *
     * @param payload 载荷。
     * @return 令牌字符串。
     */
    public String encode(DataGrantPayload payload) {
        Require.notNull(payload, "Grant 载荷不能为空");
        Require.notBlank(payload.domain(), "Grant 数据权限域不能为空");
        Require.notBlank(payload.tenantId(), "Grant 租户标识不能为空");
        Require.notNull(payload.memberId(), "Grant 账号标识不能为空");
        Require.isTrue(!payload.ids().isEmpty(), "Grant 记录集合不能为空");
        String secret = requireSecret();
        String signed = TOKEN_VERSION + SEPARATOR + ALGORITHM + SEPARATOR + properties.getKeyId()
                + SEPARATOR + encodePayload(payload);
        String signature = HmacSignatures.hmacSha256Base64Url(secret, signed);
        return signed + SEPARATOR + signature;
    }

    /**
     * 判定解码失败是否属于签名类失败（签名不符或密钥标识不匹配）。
     *
     * <p>签名类失败说明令牌曾经被正确签发、但密钥或内容已对不上；它与「格式错误」的运维含义不同，
     * 调用方据此在日志上区分「伪造或密钥轮换残留」与「垃圾输入」。
     *
     * @param exception 解码抛出的异常。
     * @return 签名不符或密钥标识不匹配时为 {@code true}。
     */
    public static boolean isSignatureFailure(DataGrantException exception) {
        DataGrantException.Reason reason = exception.reason();
        return reason == DataGrantException.Reason.SIGNATURE_MISMATCH
                || reason == DataGrantException.Reason.KEY_ID_MISMATCH;
    }

    /**
     * 解码并校验 Grant 令牌。
     *
     * @param token 令牌字符串。
     * @return 载荷。
     * @throws DataGrantException 令牌格式、版本、密钥标识、签名或载荷结构不合法时抛出。
     */
    public DataGrantPayload decode(String token) {
        Require.notBlank(token, "Grant 令牌不能为空");
        String trimmed = token.trim();
        String[] parts = trimmed.split("\\" + SEPARATOR, -1);
        if (parts.length != 5) {
            throw new DataGrantException(DataGrantException.Reason.FORMAT, MESSAGE_FORMAT_INVALID);
        }
        if (!TOKEN_VERSION.equals(parts[0]) || !ALGORITHM.equals(parts[1])) {
            throw new DataGrantException(DataGrantException.Reason.VERSION_UNSUPPORTED, MESSAGE_VERSION_UNSUPPORTED);
        }
        if (!properties.getKeyId().equals(parts[2])) {
            throw new DataGrantException(DataGrantException.Reason.KEY_ID_MISMATCH, MESSAGE_KEY_ID_MISMATCH);
        }
        String signed = parts[0] + SEPARATOR + parts[1] + SEPARATOR + parts[2] + SEPARATOR + parts[3];
        String expected = HmacSignatures.hmacSha256Base64Url(requireSecret(), signed);
        if (!HmacSignatures.matches(parts[4], expected)) {
            throw new DataGrantException(DataGrantException.Reason.SIGNATURE_MISMATCH, MESSAGE_SIGNATURE_MISMATCH);
        }
        return readPayload(parts[3]);
    }

    /**
     * 编码记录 ID 集合为增量文本。
     *
     * @param ids 记录 ID 集合，不能为空。
     * @return 增量编码文本。
     */
    public String encodeIds(Set<Long> ids) {
        Require.notEmpty(ids, "Grant 记录集合不能为空");
        TreeSet<Long> sorted = new TreeSet<>(ids);
        StringJoiner joiner = new StringJoiner(",");
        Long previous = null;
        for (Long id : sorted) {
            Require.notNull(id, "Grant 记录 ID 不能为空");
            if (previous == null) {
                joiner.add(Long.toString(id));
            } else {
                try {
                    joiner.add(Long.toString(Math.subtractExact(id, previous)));
                } catch (ArithmeticException exception) {
                    throw new DataGrantException("Grant 记录 ID 差值溢出", exception);
                }
            }
            previous = id;
        }
        return joiner.toString();
    }

    /**
     * 解码增量编码的记录 ID 集合。
     *
     * @param encoded 增量编码文本。
     * @return 记录 ID 集合（升序）。
     * @throws DataGrantException 文本为空、非数字、差值非正或加和溢出时抛出。
     */
    public Set<Long> decodeIds(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            throw new DataGrantException("Grant 记录集合不能为空");
        }
        String[] parts = encoded.split(",", -1);
        if (parts.length > MAX_DECODE_IDS) {
            throw new DataGrantException("Grant 记录集合过大");
        }
        TreeSet<Long> ids = new TreeSet<>();
        Long previous = null;
        for (int index = 0; index < parts.length; index++) {
            String part = parts[index];
            if (!isUnsignedDecimal(part)) {
                throw new DataGrantException("Grant 记录集合包含非数字或负差值");
            }
            long value;
            try {
                value = Long.parseLong(part);
            } catch (NumberFormatException exception) {
                throw new DataGrantException("Grant 记录集合超出数值范围", exception);
            }
            if (index == 0) {
                ids.add(value);
                previous = value;
                continue;
            }
            if (value <= 0) {
                throw new DataGrantException("Grant 记录集合差值必须为正");
            }
            long current;
            try {
                current = Math.addExact(previous, value);
            } catch (ArithmeticException exception) {
                throw new DataGrantException("Grant 记录集合加和溢出", exception);
            }
            ids.add(current);
            previous = current;
        }
        return ids;
    }

    private String encodePayload(DataGrantPayload payload) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("t", payload.tenantId());
        fields.put("m", payload.memberId());
        fields.put("d", payload.domain());
        fields.put("i", encodeIds(payload.ids()));
        fields.put("ia", payload.issuedAt());
        fields.put("ex", payload.expiresAt());
        if (payload.permissionVersion() != null) {
            fields.put("pv", payload.permissionVersion());
        }
        try {
            String json = objectMapper.writeValueAsString(fields);
            return ENCODER.encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException exception) {
            throw new DataGrantException("Grant 载荷序列化失败", exception);
        }
    }

    private DataGrantPayload readPayload(String encodedPayload) {
        try {
            byte[] jsonBytes = DECODER.decode(encodedPayload);
            JsonNode node = objectMapper.readTree(new String(jsonBytes, StandardCharsets.UTF_8));
            if (node == null || !node.isObject()) {
                throw new DataGrantException("Grant 载荷结构不正确");
            }
            return new DataGrantPayload(
                    requiredText(node, "t"),
                    requiredLong(node, "m"),
                    requiredText(node, "d"),
                    decodeIds(requiredText(node, "i")),
                    requiredLong(node, "ia"),
                    requiredLong(node, "ex"),
                    optionalText(node, "pv"));
        } catch (DataGrantException exception) {
            throw exception;
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new DataGrantException("Grant 载荷无法解析", exception);
        }
    }

    private static String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new DataGrantException("Grant 载荷缺少字段 " + field);
        }
        return value.asText();
    }

    private static Long requiredLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.canConvertToLong()) {
            throw new DataGrantException("Grant 载荷缺少数值字段 " + field);
        }
        return value.asLong();
    }

    private static String optionalText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static boolean isUnsignedDecimal(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }

    private String requireSecret() {
        Require.isTrue(properties.hasSecret(), "mango.data-grant.secret 未配置，无法签发或校验 Grant");
        return properties.getSecret();
    }
}
