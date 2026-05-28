package io.mango.authorization.support.autoconfigure.sensitive;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.AuthorizationSnapshot;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.authorization.api.SecurityContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationSensitiveRawAccessProviderTest {

    @Test
    void canViewRaw_withMemberAuthority_returnsTrueAndKeepsContextScope() {
        AtomicReference<AuthorizationQuery> capturedQuery = new AtomicReference<>();
        IAuthorizationProvider authorizationProvider = query -> {
            capturedQuery.set(query);
            return AuthorizationSnapshot.of(List.of(), List.of(), List.of("no_mask"));
        };
        AuthorizationSensitiveRawAccessProvider provider = new AuthorizationSensitiveRawAccessProvider(
                () -> authenticatedMemberContext(),
                () -> authorizationProvider);

        assertThat(provider.canViewRaw(" no_mask ")).isTrue();
        assertThat(capturedQuery.get())
                .isEqualTo(AuthorizationQuery.member(1001L)
                        .withTenantId("1")
                        .withSystemCode("internal-admin")
                        .withRealm("INTERNAL")
                        .withActorType("INTERNAL_USER")
                        .withParty("INTERNAL_ORG", 1L));
    }

    @Test
    void canViewRaw_withUserContext_usesUserSubjectWhenMemberIsMissing() {
        AtomicReference<AuthorizationQuery> capturedQuery = new AtomicReference<>();
        IAuthorizationProvider authorizationProvider = query -> {
            capturedQuery.set(query);
            return AuthorizationSnapshot.of(List.of(), List.of(), List.of("no_mask"));
        };
        AuthorizationSensitiveRawAccessProvider provider = new AuthorizationSensitiveRawAccessProvider(
                () -> new SecurityContext(1L, null, "1", true, "admin", null, null, null, null, "internal-admin"),
                () -> authorizationProvider);

        assertThat(provider.canViewRaw("no_mask")).isTrue();
        assertThat(capturedQuery.get())
                .isEqualTo(AuthorizationQuery.user(1L)
                        .withTenantId("1")
                        .withSystemCode("internal-admin"));
    }

    @Test
    void canViewRaw_withAnonymousContext_returnsFalseWithoutLoadingAuthorization() {
        AtomicInteger loadCount = new AtomicInteger();
        IAuthorizationProvider authorizationProvider = query -> {
            loadCount.incrementAndGet();
            return AuthorizationSnapshot.of(List.of(), List.of(), List.of("no_mask"));
        };
        AuthorizationSensitiveRawAccessProvider provider = new AuthorizationSensitiveRawAccessProvider(
                SecurityContext::anonymous,
                () -> authorizationProvider);

        assertThat(provider.canViewRaw("no_mask")).isFalse();
        assertThat(loadCount).hasValue(0);
    }

    @Test
    void canViewRaw_withBlankAuthority_returnsFalseWithoutLoadingAuthorization() {
        AtomicInteger loadCount = new AtomicInteger();
        IAuthorizationProvider authorizationProvider = query -> {
            loadCount.incrementAndGet();
            return AuthorizationSnapshot.of(List.of(), List.of(), List.of("no_mask"));
        };
        AuthorizationSensitiveRawAccessProvider provider = new AuthorizationSensitiveRawAccessProvider(
                () -> authenticatedMemberContext(),
                () -> authorizationProvider);

        assertThat(provider.canViewRaw(" ")).isFalse();
        assertThat(loadCount).hasValue(0);
    }

    @Test
    void canViewRaw_withoutAuthorizationProvider_returnsFalse() {
        AuthorizationSensitiveRawAccessProvider provider = new AuthorizationSensitiveRawAccessProvider(
                () -> authenticatedMemberContext(),
                () -> null);

        assertThat(provider.canViewRaw("no_mask")).isFalse();
    }

    @Test
    void canViewRaw_withNullAuthorizationSnapshot_returnsFalse() {
        IAuthorizationProvider authorizationProvider = query -> null;
        AuthorizationSensitiveRawAccessProvider provider = new AuthorizationSensitiveRawAccessProvider(
                () -> authenticatedMemberContext(),
                () -> authorizationProvider);

        assertThat(provider.canViewRaw("no_mask")).isFalse();
    }

    private SecurityContext authenticatedMemberContext() {
        return new SecurityContext(
                1L,
                1001L,
                "1",
                true,
                "admin",
                "INTERNAL",
                "INTERNAL_USER",
                "INTERNAL_ORG",
                1L,
                "internal-admin");
    }
}
