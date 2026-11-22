package io.mango.authorization.data.grant;

import static org.assertj.core.api.Assertions.assertThat;

import io.mango.infra.persistence.api.scope.DataScopeProvider;
import io.mango.infra.persistence.api.scope.DataScopeRule;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Grant 校验测试。
 *
 * <p>覆盖正常命中、缺省、篡改、过期、跨租户、跨账号、数据域不符、集合不命中以及数据范围指纹变更。</p>
 */
class DataGrantVerifierTest {

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
    void passesWhenTokenMatches() {
        String token = validToken(Set.of(1001L, 1002L), null);

        DataGrantVerification verification = verifier(null).verify(List.of(token), GrantTestSupport.DOMAIN, 1002L);

        assertThat(verification.isPassed()).isTrue();
        assertThat(verification.status()).isEqualTo(DataGrantVerification.Status.PASS);
    }

    @Test
    void passesWhenRecordIdIsNull() {
        String token = validToken(Set.of(1001L), null);

        DataGrantVerification verification = verifier(null).verify(List.of(token), GrantTestSupport.DOMAIN, null);

        assertThat(verification.isPassed()).isTrue();
    }

    @Test
    void returnsAbsentWithoutToken() {
        DataGrantVerification verification = verifier(null).verify(List.of(), GrantTestSupport.DOMAIN, 1001L);

        assertThat(verification.status()).isEqualTo(DataGrantVerification.Status.ABSENT);
        assertThat(verification.message()).isEqualTo(DataGrantHeaders.MESSAGE_GRANT_REQUIRED);
    }

    @Test
    void returnsInvalidForTamperedToken() {
        String token = validToken(Set.of(1001L), null);
        String tampered = token.substring(0, token.length() - 2) + "ab";

        DataGrantVerification verification = verifier(null).verify(List.of(tampered), GrantTestSupport.DOMAIN, 1001L);

        assertThat(verification.status()).isEqualTo(DataGrantVerification.Status.INVALID);
        assertThat(verification.message()).isEqualTo(DataGrantHeaders.MESSAGE_GRANT_INVALID);
    }

    @Test
    void returnsExpiredForExpiredToken() {
        String token = GrantTestSupport.token(codec, GrantTestSupport.TENANT_ID, GrantTestSupport.MEMBER_ID,
                GrantTestSupport.DOMAIN, Set.of(1001L), Instant.now().getEpochSecond() - 600,
                Instant.now().getEpochSecond() - 300, null);

        DataGrantVerification verification = verifier(null).verify(List.of(token), GrantTestSupport.DOMAIN, 1001L);

        assertThat(verification.status()).isEqualTo(DataGrantVerification.Status.EXPIRED);
        assertThat(verification.message()).isEqualTo(DataGrantHeaders.MESSAGE_GRANT_EXPIRED);
    }

    @Test
    void returnsInvalidForCrossTenantToken() {
        String token = GrantTestSupport.token(codec, "2", GrantTestSupport.MEMBER_ID, GrantTestSupport.DOMAIN,
                Set.of(1001L), GrantTestSupport.NOW, future(), null);

        DataGrantVerification verification = verifier(null).verify(List.of(token), GrantTestSupport.DOMAIN, 1001L);

        assertThat(verification.status()).isEqualTo(DataGrantVerification.Status.INVALID);
    }

    @Test
    void returnsInvalidForCrossAccountToken() {
        String token = GrantTestSupport.token(codec, GrantTestSupport.TENANT_ID, 20000L, GrantTestSupport.DOMAIN,
                Set.of(1001L), GrantTestSupport.NOW, future(), null);

        DataGrantVerification verification = verifier(null).verify(List.of(token), GrantTestSupport.DOMAIN, 1001L);

        assertThat(verification.status()).isEqualTo(DataGrantVerification.Status.INVALID);
    }

    @Test
    void returnsInvalidForOtherDomain() {
        String token = GrantTestSupport.token(codec, GrantTestSupport.TENANT_ID, GrantTestSupport.MEMBER_ID,
                "workflow:business-apply", Set.of(1001L), GrantTestSupport.NOW, future(), null);

        DataGrantVerification verification = verifier(null).verify(List.of(token), GrantTestSupport.DOMAIN, 1001L);

        assertThat(verification.status()).isEqualTo(DataGrantVerification.Status.INVALID);
    }

    @Test
    void reportsIdNotInSet() {
        String token = validToken(Set.of(1001L, 1002L), null);

        DataGrantVerification verification = verifier(null).verify(List.of(token), GrantTestSupport.DOMAIN, 2002L);

        assertThat(verification.status()).isEqualTo(DataGrantVerification.Status.ID_NOT_FOUND);
        assertThat(verification.message()).isEqualTo(DataGrantHeaders.MESSAGE_RESOURCE_NOT_FOUND);
    }

    @Test
    void detectsScopeChangeThroughPermissionVersion() {
        AtomicReference<Optional<DataScopeRule>> scope = new AtomicReference<>(
                Optional.of(GrantTestSupport.rule(Set.of("1", "2"), true)));
        DataGrantIssuer issuer = new DataGrantIssuer(properties, codec, providerOfScope(scope));
        String token = issuer.issue(GrantTestSupport.DOMAIN, Set.of(1001L)).token();

        scope.set(Optional.of(GrantTestSupport.rule(Set.of("1"), true)));
        DataGrantVerification verification = verifier(providerOfScope(scope))
                .verify(List.of(token), GrantTestSupport.DOMAIN, 1001L);

        assertThat(verification.status()).isEqualTo(DataGrantVerification.Status.SCOPE_CHANGED);
        assertThat(verification.message()).isEqualTo(DataGrantHeaders.MESSAGE_GRANT_SCOPE_CHANGED);
    }

    @Test
    void ignoresPermissionVersionWhenProviderMissing() {
        AtomicReference<Optional<DataScopeRule>> scope = new AtomicReference<>(
                Optional.of(GrantTestSupport.rule(Set.of("1"), false)));
        DataGrantIssuer issuer = new DataGrantIssuer(properties, codec, providerOfScope(scope));
        String token = issuer.issue(GrantTestSupport.DOMAIN, Set.of(1001L)).token();

        DataGrantVerification verification = verifier(GrantTestSupport.provider(null))
                .verify(List.of(token), GrantTestSupport.DOMAIN, 1001L);

        assertThat(verification.isPassed()).isTrue();
    }

    @Test
    void passesWhenAnyTokenMatches() {
        String expired = GrantTestSupport.token(codec, GrantTestSupport.TENANT_ID, GrantTestSupport.MEMBER_ID,
                GrantTestSupport.DOMAIN, Set.of(1001L), Instant.now().getEpochSecond() - 600,
                Instant.now().getEpochSecond() - 300, null);
        String valid = validToken(Set.of(1003L), null);

        DataGrantVerification verification = verifier(null)
                .verify(List.of(expired, valid), GrantTestSupport.DOMAIN, 1003L);

        assertThat(verification.isPassed()).isTrue();
    }

    private DataGrantVerifier verifier(ObjectProvider<DataScopeProvider> provider) {
        ObjectProvider<DataScopeProvider> actual = provider == null ? GrantTestSupport.provider(null) : provider;
        return new DataGrantVerifier(codec, actual);
    }

    private static ObjectProvider<DataScopeProvider> providerOfScope(AtomicReference<Optional<DataScopeRule>> scope) {
        return GrantTestSupport.provider(resourceCode -> scope.get());
    }

    private String validToken(Set<Long> ids, String permissionVersion) {
        return GrantTestSupport.token(codec, GrantTestSupport.TENANT_ID, GrantTestSupport.MEMBER_ID,
                GrantTestSupport.DOMAIN, ids, Instant.now().getEpochSecond() - 1,
                Instant.now().getEpochSecond() + 300, permissionVersion);
    }

    private static long future() {
        return Instant.now().getEpochSecond() + 300;
    }
}
