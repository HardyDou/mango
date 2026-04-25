package io.mango.infra.security.starter.authorization;

import io.mango.infra.security.api.IPermissionService;
import io.mango.infra.security.api.ISecurityContextProvider;
import io.mango.infra.security.api.Perm;
import io.mango.infra.security.api.SecurityPrincipal;
import io.mango.infra.security.starter.context.SpringSecurityContextProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(PermMethodSecurityTest.TestConfiguration.class)
@DisplayName("Perm method security tests")
class PermMethodSecurityTest {

    @jakarta.annotation.Resource
    private SecuredService securedService;

    @jakarta.annotation.Resource
    private RecordingPermissionService permissionService;

    @jakarta.annotation.Resource
    private ISecurityContextProvider securityContextProvider;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        permissionService.clear();
    }

    @Test
    @DisplayName("unauthenticated access should be denied")
    void unauthenticatedAccessShouldBeDenied() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> securedService.securedCall());
    }

    @Test
    @DisplayName("authentication authorities should satisfy @Perm directly")
    void authenticationAuthoritiesShouldSatisfyPerm() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        new SecurityPrincipal(1L, null, "tester"),
                        "token",
                        AuthorityUtils.createAuthorityList("user:test:add")));

        assertDoesNotThrow(() -> securedService.securedCall());
        verify(permissionService, times(0)).listUserPermissions(1L);
    }

    @Test
    @DisplayName("permission service should be used as fallback when authentication has no authorities")
    void permissionServiceShouldBeUsedAsFallback() {
        permissionService.put(42L, List.of("user:test:add"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        new SecurityPrincipal(42L, "t1", "tester"),
                        "token",
                        AuthorityUtils.NO_AUTHORITIES));

        assertDoesNotThrow(() -> securedService.securedCall());
        assertEquals(42L, securityContextProvider.currentContext().userId());
        assertEquals("t1", securityContextProvider.currentContext().tenantId());
    }

    @Test
    @DisplayName("missing permission should be denied")
    void missingPermissionShouldBeDenied() {
        permissionService.put(7L, List.of("user:test:view"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        new SecurityPrincipal(7L, null, "tester"),
                        "token",
                        AuthorityUtils.NO_AUTHORITIES));

        assertThrows(AccessDeniedException.class, () -> securedService.securedCall());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    @Import(PermMethodSecurityConfiguration.class)
    static class TestConfiguration {

        @Bean
        ISecurityContextProvider securityContextProvider() {
            return new SpringSecurityContextProvider();
        }

        @Bean
        SecuredService securedService() {
            return new SecuredService();
        }

        @Bean
        RecordingPermissionService permissionService() {
            return spy(new RecordingPermissionService());
        }
    }

    static class SecuredService {

        @Perm("user:test:add")
        public void securedCall() {
        }
    }

    static class RecordingPermissionService implements IPermissionService {

        private final Map<Long, List<String>> permissions = new ConcurrentHashMap<>();

        @Override
        public List<String> listUserPermissions(Long userId) {
            return permissions.getOrDefault(userId, List.of());
        }

        void put(Long userId, List<String> values) {
            permissions.put(userId, values);
        }

        void clear() {
            permissions.clear();
        }
    }
}
