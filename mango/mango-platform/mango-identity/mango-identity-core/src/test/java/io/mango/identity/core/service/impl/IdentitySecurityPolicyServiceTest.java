package io.mango.identity.core.service.impl;

import io.mango.identity.core.adapter.SysConfigValueAdapter;
import io.mango.system.api.spi.SystemConfigProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentitySecurityPolicyServiceTest {

    @Test
    void booleanSystemConfigOverridesSpringDefault() {
        SystemConfigProvider api = mock(SystemConfigProvider.class);
        when(api.getBooleanValue("identity.security.password-reset-after-create.enabled", true))
                .thenReturn(false);
        IdentitySecurityPolicyService service = newService(new IdentitySecurityProperties(), api);

        assertThat(service.resetRequiredAfterCreate()).isFalse();
    }

    @Test
    void integerSystemConfigOverridesSpringDefault() {
        SystemConfigProvider api = mock(SystemConfigProvider.class);
        when(api.getIntegerValue("sys.login.lockCount", 5))
                .thenReturn(3);
        IdentitySecurityPolicyService service = newService(new IdentitySecurityProperties(), api);

        assertThat(service.maxFailedAttempts()).isEqualTo(3);
    }

    @Test
    void failureWindowSystemConfigOverridesSpringDefault() {
        SystemConfigProvider api = mock(SystemConfigProvider.class);
        when(api.getIntegerValue("identity.security.login.failure-window-minutes", 60))
                .thenReturn(30);
        IdentitySecurityPolicyService service = newService(new IdentitySecurityProperties(), api);

        assertThat(service.failureWindowMinutes()).isEqualTo(30);
    }

    @Test
    void stringSystemConfigOverridesSpringDefault() {
        SystemConfigProvider api = mock(SystemConfigProvider.class);
        when(api.getValue("identity.security.password.pattern"))
                .thenReturn("(?=.*[A-Z]).{8,}");
        IdentitySecurityPolicyService service = newService(new IdentitySecurityProperties(), api);

        assertThat(service.passwordPattern()).isEqualTo("(?=.*[A-Z]).{8,}");
    }

    @Test
    void fallsBackToSpringDefaultWhenSystemConfigUnavailable() {
        SystemConfigProvider api = mock(SystemConfigProvider.class);
        when(api.getBooleanValue("identity.security.login-failure-lock.enabled", true))
                .thenThrow(new IllegalStateException("config unavailable"));
        IdentitySecurityProperties properties = new IdentitySecurityProperties();
        properties.getLogin().setFailureLockEnabled(true);
        IdentitySecurityPolicyService service = newService(properties, api);

        assertThat(service.loginFailureLockEnabled()).isTrue();
    }

    private static IdentitySecurityPolicyService newService(
            IdentitySecurityProperties properties, SystemConfigProvider api) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("systemConfigProvider", api);
        return new IdentitySecurityPolicyService(properties,
                new SysConfigValueAdapter(beanFactory.getBeanProvider(SystemConfigProvider.class)));
    }
}
