package io.mango.file.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class FileControllerAccessModeTest {

    @Test
    void basicFileEndpointsUseDefaultRolePermissions() throws NoSuchMethodException {
        assertPermission("get", "file:files:query", Long.class);
        assertPermission("upload", "file:files:upload",
                org.springframework.web.multipart.MultipartFile.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                Long.class);
        assertPermission("preview", "file:files:download", Long.class);
        assertPermission("downloadResponse", "file:files:download", Long.class, String.class, Long.class);
        assertPermission("previewContentResponse", "file:files:download", Long.class);
        assertPermission(FileSettingsController.class, "get", "file:settings:query");
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
