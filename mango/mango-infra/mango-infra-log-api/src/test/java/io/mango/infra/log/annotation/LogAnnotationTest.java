package io.mango.infra.log.annotation;

import io.mango.infra.log.Loggers;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LogAnnotationTest {

    @Test
    void shouldExposeRuntimeMethodContractAndDefaultType() throws NoSuchMethodException {
        Method method = TestActions.class.getDeclaredMethod("update");
        Log annotation = method.getAnnotation(Log.class);
        Retention retention = Log.class.getAnnotation(Retention.class);
        Target target = Log.class.getAnnotation(Target.class);

        assertNotNull(annotation);
        assertEquals("更新配置", annotation.value());
        assertEquals(LogType.OPERATION, annotation.type());
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
        assertArrayEquals(new ElementType[]{ElementType.METHOD}, target.value());
    }

    @Test
    void shouldExposeExplicitType() throws NoSuchMethodException {
        Log annotation = TestActions.class.getDeclaredMethod("login").getAnnotation(Log.class);

        assertNotNull(annotation);
        assertEquals("用户登录", annotation.value());
        assertEquals(LogType.LOGIN, annotation.type());
    }

    @Test
    void shouldPublishStableOperationLoggerName() {
        assertEquals("io.mango.infra.log.annotation.Log", Loggers.OPERATION);
    }

    static class TestActions {
        @Log("更新配置")
        void update() {
        }

        @Log(value = "用户登录", type = LogType.LOGIN)
        void login() {
        }
    }
}
