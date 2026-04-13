package io.mango.common.annotation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LogType 枚举测试
 *
 * @author Mango
 */
class LogTypeTest {

    @Test
    void shouldHaveCorrectLogTypes() {
        assertThat(LogType.values())
                .containsExactly(
                        LogType.LOGIN,
                        LogType.LOGOUT,
                        LogType.REGISTER,
                        LogType.PASSWORD,
                        LogType.OPERATION,
                        LogType.SECURITY,
                        LogType.AUDIT
                );
    }

    @Test
    void shouldHaveCorrectOrdinalValues() {
        assertThat(LogType.LOGIN.ordinal()).isEqualTo(0);
        assertThat(LogType.LOGOUT.ordinal()).isEqualTo(1);
        assertThat(LogType.REGISTER.ordinal()).isEqualTo(2);
        assertThat(LogType.PASSWORD.ordinal()).isEqualTo(3);
        assertThat(LogType.OPERATION.ordinal()).isEqualTo(4);
        assertThat(LogType.SECURITY.ordinal()).isEqualTo(5);
        assertThat(LogType.AUDIT.ordinal()).isEqualTo(6);
    }

    @Test
    void shouldParseFromString() {
        assertThat(LogType.valueOf("LOGIN")).isEqualTo(LogType.LOGIN);
        assertThat(LogType.valueOf("LOGOUT")).isEqualTo(LogType.LOGOUT);
        assertThat(LogType.valueOf("OPERATION")).isEqualTo(LogType.OPERATION);
    }

    @Test
    void shouldHaveReadableNames() {
        assertThat(LogType.LOGIN.name()).isEqualTo("LOGIN");
        assertThat(LogType.SECURITY.name()).isEqualTo("SECURITY");
        assertThat(LogType.AUDIT.name()).isEqualTo("AUDIT");
    }
}
