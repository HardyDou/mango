package io.mango.file.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class FileControllerAccessModeTest {

    @Test
    void basicFileEndpointsUseLoginBaseline() throws NoSuchMethodException {
        assertLogin("get", Long.class);
        assertLogin("upload",
                org.springframework.web.multipart.MultipartFile.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                Long.class);
        assertLogin("preview", Long.class);
        assertLogin("downloadResponse", Long.class, String.class, Long.class);
        assertLogin("previewContentResponse", Long.class);
        Method settings = FileSettingsController.class.getMethod("get");
        assertThat(settings.getAnnotation(ApiAccess.class).mode()).isEqualTo(ApiResourceAccessMode.LOGIN);
    }

    private void assertLogin(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = FileController.class.getMethod(methodName, parameterTypes);
        ApiAccess apiAccess = method.getAnnotation(ApiAccess.class);
        assertThat(apiAccess).isNotNull();
        assertThat(apiAccess.mode()).isEqualTo(ApiResourceAccessMode.LOGIN);
        assertThat(apiAccess.permission()).isBlank();
    }

    private void assertPermission(String methodName, String permission, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        assertPermission(FileController.class, methodName, permission, parameterTypes);
    }

    private void assertPermission(Class<?> controllerClass,
                                  String methodName,
                                  String permission,
                                  Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = controllerClass.getMethod(methodName, parameterTypes);
        ApiAccess apiAccess = method.getAnnotation(ApiAccess.class);
        assertThat(apiAccess).isNotNull();
        assertThat(apiAccess.mode()).isEqualTo(ApiResourceAccessMode.PERMISSION);
        assertThat(apiAccess.permission()).isEqualTo(permission);
    }
}
