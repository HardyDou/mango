package io.mango.authorization.data.grant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.mango.infra.web.util.HmacSignatures;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Grant 令牌编解码测试。
 *
 * <p>覆盖正常往返、签名篡改、密钥标识不匹配、增量 ID 编码及其畸形输入。</p>
 */
class DataGrantCodecTest {

    private DataGrantProperties properties;

    private DataGrantCodec codec;

    @BeforeEach
    void setUp() {
        properties = GrantTestSupport.properties();
        codec = GrantTestSupport.codec(properties);
    }

    @Test
    void encodesAndDecodesPayload() {
        String token = GrantTestSupport.token(codec, GrantTestSupport.TENANT_ID, GrantTestSupport.MEMBER_ID,
                GrantTestSupport.DOMAIN, Set.of(1001L, 1002L), GrantTestSupport.NOW, GrantTestSupport.NOW + 300, null);

        DataGrantPayload payload = codec.decode(token);

        assertThat(payload.tenantId()).isEqualTo(GrantTestSupport.TENANT_ID);
        assertThat(payload.memberId()).isEqualTo(GrantTestSupport.MEMBER_ID);
        assertThat(payload.domain()).isEqualTo(GrantTestSupport.DOMAIN);
        assertThat(payload.ids()).containsExactly(1001L, 1002L);
        assertThat(payload.issuedAt()).isEqualTo(GrantTestSupport.NOW);
        assertThat(payload.expiresAt()).isEqualTo(GrantTestSupport.NOW + 300);
        assertThat(payload.permissionVersion()).isNull();
    }

    @Test
    void tokenLayoutMatchesContract() {
        String token = GrantTestSupport.token(codec, GrantTestSupport.TENANT_ID, GrantTestSupport.MEMBER_ID,
                GrantTestSupport.DOMAIN, Set.of(1001L), GrantTestSupport.NOW, GrantTestSupport.NOW + 300, null);

        String[] parts = token.split("\\.");

        assertThat(parts).hasSize(5);
        assertThat(parts[0]).isEqualTo("m1");
        assertThat(parts[1]).isEqualTo("hs256");
        assertThat(parts[2]).isEqualTo("k1");
        assertThat(parts[4]).isEqualTo(HmacSignatures.hmacSha256Base64Url(GrantTestSupport.SECRET,
                String.join(".", parts[0], parts[1], parts[2], parts[3])));
    }

    @Test
    void rejectsTamperedPayload() {
        String token = GrantTestSupport.token(codec, GrantTestSupport.TENANT_ID, GrantTestSupport.MEMBER_ID,
                GrantTestSupport.DOMAIN, Set.of(1001L), GrantTestSupport.NOW, GrantTestSupport.NOW + 300, null);
        String[] parts = token.split("\\.");
        parts[3] = tamper(parts[3]);
        String tampered = String.join(".", parts);

        assertThatThrownBy(() -> codec.decode(tampered))
                .isInstanceOf(DataGrantException.class)
                .hasMessageContaining("签名校验失败");
    }

    @Test
    void rejectsTamperedSignature() {
        String token = GrantTestSupport.token(codec, GrantTestSupport.TENANT_ID, GrantTestSupport.MEMBER_ID,
                GrantTestSupport.DOMAIN, Set.of(1001L), GrantTestSupport.NOW, GrantTestSupport.NOW + 300, null);
        String[] parts = token.split("\\.");
        parts[4] = tamper(parts[4]);
        String tampered = String.join(".", parts);

        assertThatThrownBy(() -> codec.decode(tampered))
                .isInstanceOf(DataGrantException.class)
                .hasMessageContaining("签名校验失败");
    }

    @Test
    void rejectsUnknownKeyId() {
        String token = GrantTestSupport.token(codec, GrantTestSupport.TENANT_ID, GrantTestSupport.MEMBER_ID,
                GrantTestSupport.DOMAIN, Set.of(1001L), GrantTestSupport.NOW, GrantTestSupport.NOW + 300, null);
        String[] original = token.split("\\.");
        String signed = String.join(".", original[0], original[1], "k2", original[3]);
        String reSigned = signed + "." + HmacSignatures.hmacSha256Base64Url(GrantTestSupport.SECRET, signed);

        assertThatThrownBy(() -> codec.decode(reSigned))
                .isInstanceOf(DataGrantException.class)
                .hasMessageContaining("签名密钥标识不匹配");
    }

    @Test
    void rejectsMalformedTokenShape() {
        assertThatThrownBy(() -> codec.decode("not-a-token"))
                .isInstanceOf(DataGrantException.class)
                .hasMessageContaining("Grant 格式不正确");
        assertThatThrownBy(() -> codec.decode("m1.hs256.k1"))
                .isInstanceOf(DataGrantException.class)
                .hasMessageContaining("Grant 格式不正确");
    }

    @Test
    void rejectsUnsupportedVersion() {
        String token = GrantTestSupport.token(codec, GrantTestSupport.TENANT_ID, GrantTestSupport.MEMBER_ID,
                GrantTestSupport.DOMAIN, Set.of(1001L), GrantTestSupport.NOW, GrantTestSupport.NOW + 300, null);
        String[] parts = token.split("\\.");
        String signed = String.join(".", "m2", parts[1], parts[2], parts[3]);
        String reSigned = signed + "." + HmacSignatures.hmacSha256Base64Url(GrantTestSupport.SECRET, signed);

        assertThatThrownBy(() -> codec.decode(reSigned))
                .isInstanceOf(DataGrantException.class)
                .hasMessageContaining("版本或算法不受支持");
    }

    @Test
    void encodesIdsIncrementally() {
        assertThat(codec.encodeIds(Set.of(1005L, 1001L, 1002L))).isEqualTo("1001,1,3");
        assertThat(codec.encodeIds(Set.of(1001L))).isEqualTo("1001");
    }

    @Test
    void decodesIncrementallyEncodedIds() {
        assertThat(codec.decodeIds("1001,1,3")).containsExactly(1001L, 1002L, 1005L);
        assertThat(codec.decodeIds("1001")).containsExactly(1001L);
    }

    @Test
    void rejectsNegativeDelta() {
        assertThatThrownBy(() -> codec.decodeIds("1001,-1"))
                .isInstanceOf(DataGrantException.class)
                .hasMessageContaining("Grant 记录集合");
    }

    @Test
    void rejectsNonNumericDelta() {
        assertThatThrownBy(() -> codec.decodeIds("1001,abc"))
                .isInstanceOf(DataGrantException.class)
                .hasMessageContaining("Grant 记录集合");
    }

    @Test
    void rejectsOverflowingDelta() {
        assertThatThrownBy(() -> codec.decodeIds("9223372036854775807,1"))
                .isInstanceOf(DataGrantException.class)
                .hasMessageContaining("Grant 记录集合");
    }

    @Test
    void rejectsEmptyOrBlankIds() {
        assertThatThrownBy(() -> codec.decodeIds(""))
                .isInstanceOf(DataGrantException.class);
        assertThatThrownBy(() -> codec.encodeIds(Set.of()))
                .isInstanceOf(io.mango.common.exception.BizException.class);
    }

    @Test
    void keepsPermissionVersionWhenPresent() {
        String token = GrantTestSupport.token(codec, GrantTestSupport.TENANT_ID, GrantTestSupport.MEMBER_ID,
                GrantTestSupport.DOMAIN, Set.of(1001L), GrantTestSupport.NOW, GrantTestSupport.NOW + 300, "abcdef12");

        assertThat(codec.decode(token).permissionVersion()).isEqualTo("abcdef12");
    }

    private static String tamper(String segment) {
        byte[] bytes = Base64.getUrlDecoder().decode(segment);
        bytes[0] = (byte) (bytes[0] ^ 0x01);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Test
    void payloadJsonUsesCompactKeyNames() {
        String token = GrantTestSupport.token(codec, GrantTestSupport.TENANT_ID, GrantTestSupport.MEMBER_ID,
                GrantTestSupport.DOMAIN, Set.of(1001L), GrantTestSupport.NOW, GrantTestSupport.NOW + 300, null);
        String json = new String(Base64.getUrlDecoder().decode(token.split("\\.")[3]), StandardCharsets.UTF_8);

        assertThat(json).startsWith("{\"t\":\"").contains("\"m\":").contains("\"d\":\"").contains("\"i\":\"")
                .contains("\"ia\":").contains("\"ex\":").doesNotContain("pv");
    }
}
