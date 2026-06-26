package io.mango.identity.core.service.impl;

import io.mango.system.api.SysConfigApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class IdentityPasswordPolicyServiceTest {

    private final IdentitySecurityProperties properties = new IdentitySecurityProperties();
    private final IdentityPasswordPolicyService service =
            new IdentityPasswordPolicyService(new IdentitySecurityPolicyService(properties, emptyProvider()));

    @Test
    void validatePlainPasswordRejectsWeakPasswords() {
        assertThatThrownBy(() -> service.validatePlainPassword("short1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("至少");
        assertThatThrownBy(() -> service.validatePlainPassword("Password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("数字");
        assertThatThrownBy(() -> service.validatePlainPassword("12345678"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("字母");
        assertThatThrownBy(() -> service.validatePlainPassword("Pass 1234"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("空白");
    }

    @Test
    void validatePlainPasswordAcceptsBaselinePassword() {
        assertThatCode(() -> service.validatePlainPassword("Mango@123456"))
                .doesNotThrowAnyException();
    }

    @Test
    void validatePlainPasswordRequiresSpecialCharWhenEnabled() {
        properties.getPassword().setRequireSpecialChar(true);

        assertThatThrownBy(() -> service.validatePlainPassword("Mango12345"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("特殊字符");
    }

    @Test
    void validatePlainPasswordRespectsCustomPattern() {
        properties.getPassword().setPattern("(?=.*[A-Z]).{8,}");

        assertThatThrownBy(() -> service.validatePlainPassword("mango1234"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("自定义规则");
        assertThatCode(() -> service.validatePlainPassword("Mango1234"))
                .doesNotThrowAnyException();
    }

    @Test
    void validatePlainPasswordSkipsComplexityWhenDisabled() {
        properties.getPassword().setComplexityEnabled(false);

        assertThatCode(() -> service.validatePlainPassword("1"))
                .doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<SysConfigApi> emptyProvider() {
        return mock(ObjectProvider.class);
    }
}
