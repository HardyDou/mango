package io.mango.file.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class FileControllerAccessModeTest {

    @Test
    void basicFileReadEndpointsUseLoginBaselineAccess() throws NoSuchMethodException {
        assertLoginBaseline("get", Long.class);
        assertLoginBaseline("preview", Long.class);
        assertLoginBaseline("downloadResponse", Long.class, String.class, Long.class);
        assertLoginBaseline(FileSettingsController.class, "get");
    }

    private void assertLoginBaseline(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        assertLoginBaseline(FileController.class, methodName, parameterTypes);
    }

    private void assertLoginBaseline(Class<?> controllerClass, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = controllerClass.getMethod(methodName, parameterTypes);
        ApiAccess apiAccess = method.getAnnotation(ApiAccess.class);
        assertThat(apiAccess).isNotNull();
        assertThat(apiAccess.mode()).isEqualTo(ApiResourceAccessMode.LOGIN);
        assertThat(apiAccess.permission()).isBlank();
    }
}
