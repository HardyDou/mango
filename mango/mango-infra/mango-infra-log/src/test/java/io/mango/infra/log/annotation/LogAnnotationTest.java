package io.mango.infra.log.annotation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Log 注解测试
 * Moved from mango-common to mango-infra-log.
 *
 * @author Mango
 */
class LogAnnotationTest {

    @Test
    void annotationShouldHaveCorrectTarget() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("testMethod");
        Log log = method.getAnnotation(Log.class);

        assertThat(log).isNotNull();
        assertThat(log.value()).isEqualTo("测试方法");
        assertThat(log.type()).isEqualTo(LogType.OPERATION);
    }

    @Test
    void annotationShouldSupportCustomLogType() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("loginMethod");
        Log log = method.getAnnotation(Log.class);

        assertThat(log).isNotNull();
        assertThat(log.value()).isEqualTo("用户登录");
        assertThat(log.type()).isEqualTo(LogType.LOGIN);
    }

    @Test
    void annotationShouldHaveDefaultOperationType() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("defaultTypeMethod");
        Log log = method.getAnnotation(Log.class);

        assertThat(log).isNotNull();
        assertThat(log.type()).isEqualTo(LogType.OPERATION);
    }

    @Test
    void shouldHaveRuntimeRetention() {
        Log annotation = TestClass.class.getAnnotation(Log.class);
        assertThat(annotation).isNull(); // 类上没有注解

        // 验证 Retention 是 RUNTIME
        java.lang.annotation.Retention retention = Log.class.getAnnotation(java.lang.annotation.Retention.class);
        assertThat(retention.value()).isEqualTo(java.lang.annotation.RetentionPolicy.RUNTIME);
    }

    @Test
    void shouldTargetMethodOnly() {
        java.lang.annotation.Target target = Log.class.getAnnotation(java.lang.annotation.Target.class);
        assertThat(target.value()).containsExactly(java.lang.annotation.ElementType.METHOD);
    }

    // 测试辅助类
    static class TestClass {
        @Log(value = "测试方法")
        public void testMethod() {
        }

        @Log(value = "用户登录", type = LogType.LOGIN)
        public void loginMethod() {
        }

        @Log(value = "默认类型")
        public void defaultTypeMethod() {
        }
    }
}
