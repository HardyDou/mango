package io.mango.authorization.data.grant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Grant 签发测试。
 *
 * <p>覆盖正常签发、缺少密钥、缺少上下文、空集合、超限不签发以及数据范围指纹写入。</p>
 */
class DataGrantIssuerTest {

    private DataGrantProperties properties;

    private DataGrantCodec codec;

    @BeforeEach
    void setUp() {
        properties = GrantTestSupport.properties();
        codec = GrantTestSupport.codec(properties);
        GrantTestSupport.context(GrantTestSupport.TENANT_ID, GrantTestSupport.MEMBER_ID);
    }

    @AfterEach
    void tearDown() {
        GrantTestSupport.clearContext();
    }

    @Test
    void issuesTokenForCurrentContext() {
        DataGrantIssuer issuer = new DataGrantIssuer(properties, codec, GrantTestSupport.provider(null));

        DataGrantIssueResult result = issuer.issue(GrantTestSupport.DOMAIN, Set.of(1002L, 1001L));

        assertThat(result.isIssued()).isTrue();
        assertThat(result.skipReason()).isNull();
        DataGrantPayload payload = codec.decode(result.token());
        assertThat(payload.tenantId()).isEqualTo(GrantTestSupport.TENANT_ID);
        assertThat(payload.memberId()).isEqualTo(GrantTestSupport.MEMBER_ID);
        assertThat(payload.domain()).isEqualTo(GrantTestSupport.DOMAIN);
        assertThat(payload.ids()).containsExactly(1001L, 1002L);
        assertThat(payload.expiresAt() - payload.issuedAt()).isEqualTo(properties.getTtlSeconds());
        assertThat(payload.permissionVersion()).isNull();
    }

    @Test
    void includesPermissionVersionWhenProviderPresent() {
        DataGrantIssuer issuer = new DataGrantIssuer(properties, codec,
                GrantTestSupport.provider(resourceCode -> Optional
                        .of(GrantTestSupport.rule(Set.of("3", "1"), true))));

        DataGrantIssueResult result = issuer.issue(GrantTestSupport.DOMAIN, Set.of(1001L));

        DataGrantPayload payload = codec.decode(result.token());
        assertThat(payload.permissionVersion())
                .isEqualTo(PermissionVersion.fingerprint(GrantTestSupport.rule(Set.of("3", "1"), true)));
        assertThat(payload.permissionVersion()).hasSize(8);
    }

    @Test
    void omitsPermissionVersionWhenProviderMissing() {
        DataGrantIssuer issuer = new DataGrantIssuer(properties, codec, GrantTestSupport.provider(null));

        DataGrantIssueResult result = issuer.issue(GrantTestSupport.DOMAIN, Set.of(1001L));

        assertThat(codec.decode(result.token()).permissionVersion()).isNull();
    }

    @Test
    void skipsOversizeCollection() {
        DataGrantProperties small = GrantTestSupport.properties(DataGrantProperties.Mode.OPTIONAL, 2);
        DataGrantIssuer issuer = new DataGrantIssuer(small, GrantTestSupport.codec(small),
                GrantTestSupport.provider(null));

        DataGrantIssueResult result = issuer.issue(GrantTestSupport.DOMAIN, Set.of(1L, 2L, 3L));

        assertThat(result.isIssued()).isFalse();
        assertThat(result.token()).isNull();
        assertThat(result.skipReason()).isEqualTo(DataGrantIssueResult.SKIP_OVERSIZE);
    }

    @Test
    void skipsWhenSecretMissing() {
        DataGrantProperties noSecret = new DataGrantProperties();
        noSecret.setEnabled(true);
        noSecret.validate();
        DataGrantIssuer issuer = new DataGrantIssuer(noSecret, GrantTestSupport.codec(noSecret),
                GrantTestSupport.provider(null));

        DataGrantIssueResult result = issuer.issue(GrantTestSupport.DOMAIN, Set.of(1001L));

        assertThat(result.isIssued()).isFalse();
        assertThat(result.skipReason()).isEqualTo(DataGrantIssueResult.SKIP_NO_SECRET);
    }

    @Test
    void skipsWhenDisabled() {
        DataGrantProperties disabled = GrantTestSupport.properties();
        disabled.setEnabled(false);
        DataGrantIssuer issuer = new DataGrantIssuer(disabled, codec, GrantTestSupport.provider(null));

        DataGrantIssueResult result = issuer.issue(GrantTestSupport.DOMAIN, Set.of(1001L));

        assertThat(result.isIssued()).isFalse();
        assertThat(result.skipReason()).isEqualTo(DataGrantIssueResult.SKIP_DISABLED);
    }

    @Test
    void skipsWhenContextMissing() {
        GrantTestSupport.clearContext();
        DataGrantIssuer issuer = new DataGrantIssuer(properties, codec, GrantTestSupport.provider(null));

        DataGrantIssueResult result = issuer.issue(GrantTestSupport.DOMAIN, Set.of(1001L));

        assertThat(result.isIssued()).isFalse();
        assertThat(result.skipReason()).isEqualTo(DataGrantIssueResult.SKIP_NO_CONTEXT);
    }

    @Test
    void skipsWhenCollectionEmpty() {
        DataGrantIssuer issuer = new DataGrantIssuer(properties, codec, GrantTestSupport.provider(null));

        DataGrantIssueResult result = issuer.issue(GrantTestSupport.DOMAIN, Set.of());

        assertThat(result.isIssued()).isFalse();
        assertThat(result.skipReason()).isEqualTo(DataGrantIssueResult.SKIP_EMPTY);
    }

    @Test
    void ignoresNullRecordIds() {
        DataGrantIssuer issuer = new DataGrantIssuer(properties, codec, GrantTestSupport.provider(null));
        Collection<Long> ids = Arrays.asList(1001L, null);

        DataGrantIssueResult result = issuer.issue(GrantTestSupport.DOMAIN, ids);

        assertThat(result.isIssued()).isTrue();
        assertThat(codec.decode(result.token()).ids()).containsExactly(1001L);
    }

    @Test
    void permissionVersionIsStableForSameRule() {
        String first = PermissionVersion.fingerprint(GrantTestSupport.rule(Set.of("2", "1"), false));
        String second = PermissionVersion.fingerprint(GrantTestSupport.rule(Set.of("1", "2"), false));

        assertThat(first).isEqualTo(second).hasSize(8);
        assertThat(PermissionVersion.fingerprint(GrantTestSupport.rule(Set.of("1", "2"), true)))
                .isNotEqualTo(first);
    }
}
