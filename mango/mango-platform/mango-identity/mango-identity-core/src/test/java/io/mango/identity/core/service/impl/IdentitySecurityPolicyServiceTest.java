package io.mango.identity.core.service.impl;

import io.mango.common.result.R;
import io.mango.system.api.SysConfigApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentitySecurityPolicyServiceTest {

    @Test
    void booleanSystemConfigOverridesSpringDefault() {
        SysConfigApi api = mock(SysConfigApi.class);
        when(api.getBooleanValue("identity.security.password-reset-after-create.enabled", true))
                .thenReturn(R.ok(false));
        IdentitySecurityPolicyService service = newService(new IdentitySecurityProperties(), api);

        assertThat(service.resetRequiredAfterCreate()).isFalse();
    }

    @Test
    void integerSystemConfigOverridesSpringDefault() {
        SysConfigApi api = mock(SysConfigApi.class);
        when(api.getIntegerValue("sys.login.lockCount", 5))
                .thenReturn(R.ok(3));
        IdentitySecurityPolicyService service = newService(new IdentitySecurityProperties(), api);

        assertThat(service.maxFailedAttempts()).isEqualTo(3);
    }

    @Test
    void failureWindowSystemConfigOverridesSpringDefault() {
        SysConfigApi api = mock(SysConfigApi.class);
        when(api.getIntegerValue("identity.security.login.failure-window-minutes", 60))
                .thenReturn(R.ok(30));
        IdentitySecurityPolicyService service = newService(new IdentitySecurityProperties(), api);

        assertThat(service.failureWindowMinutes()).isEqualTo(30);
    }

    @Test
    void stringSystemConfigOverridesSpringDefault() {
        SysConfigApi api = mock(SysConfigApi.class);
        when(api.getValue("identity.security.password.pattern"))
                .thenReturn(R.ok("(?=.*[A-Z]).{8,}"));
        IdentitySecurityPolicyService service = newService(new IdentitySecurityProperties(), api);

        assertThat(service.passwordPattern()).isEqualTo("(?=.*[A-Z]).{8,}");
    }

    @Test
    void fallsBackToSpringDefaultWhenSystemConfigUnavailable() {
        SysConfigApi api = mock(SysConfigApi.class);
        when(api.getBooleanValue("identity.security.login-failure-lock.enabled", true))
                .thenThrow(new IllegalStateException("config unavailable"));
        IdentitySecurityProperties properties = new IdentitySecurityProperties();
        properties.getLogin().setFailureLockEnabled(true);
        IdentitySecurityPolicyService service = newService(properties, api);

        assertThat(service.loginFailureLockEnabled()).isTrue();
    }

    @SuppressWarnings("unchecked")
    private static IdentitySecurityPolicyService newService(IdentitySecurityProperties properties, SysConfigApi api) {
        ObjectProvider<SysConfigApi> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(api);
        return new IdentitySecurityPolicyService(properties, provider);
    }
}
