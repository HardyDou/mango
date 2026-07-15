package io.mango.infra.log.annotation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LogTypeTest {

    @Test
    void shouldKeepPublishedNamesAndOrder() {
        assertArrayEquals(new LogType[]{
                LogType.LOGIN,
                LogType.LOGOUT,
                LogType.REGISTER,
                LogType.PASSWORD,
                LogType.OPERATION,
                LogType.SECURITY,
                LogType.AUDIT
        }, LogType.values());
        assertEquals(LogType.LOGIN, LogType.valueOf("LOGIN"));
        assertEquals(LogType.AUDIT, LogType.valueOf("AUDIT"));
    }
}
