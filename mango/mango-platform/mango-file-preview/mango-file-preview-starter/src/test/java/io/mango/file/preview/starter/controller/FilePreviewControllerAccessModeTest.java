package io.mango.file.preview.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.contract.BinaryHttpAdapter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class FilePreviewControllerAccessModeTest {

    @Test
    void sourceControllerDeclaresBinaryHttpAdapterContract() {
        assertThat(FilePreviewSourceController.class).hasAnnotation(BinaryHttpAdapter.class);
    }

    @Test
    void fileIdPreviewEndpointsAllowEveryLoggedInUser() throws NoSuchMethodException {
        assertAccessMode("preview", ApiResourceAccessMode.LOGIN, "", Long.class);
        assertAccessMode(FilePreviewPageController.class, "redirectPreview",
                ApiResourceAccessMode.LOGIN, "", Long.class);
    }

    @Test
    void tokenPreviewEndpointsRemainPublicBecauseTheyRequireShortLivedTokens() throws NoSuchMethodException {
        assertAccessMode(FilePreviewPageController.class, "redirectPreviewEntry",
                ApiResourceAccessMode.PUBLIC, "", String.class);
        assertAccessMode(FilePreviewSourceController.class, "source",
                ApiResourceAccessMode.PUBLIC, "", String.class);
        assertAccessMode(FilePreviewSourceController.class, "generated",
                ApiResourceAccessMode.PUBLIC, "", String.class, String.class);
    }

    private void assertAccessMode(String methodName,
                                  ApiResourceAccessMode mode,
                                  String permission,
                                  Class<?>... parameterTypes) throws NoSuchMethodException {
        assertAccessMode(FilePreviewController.class, methodName, mode, permission, parameterTypes);
    }

    private void assertAccessMode(Class<?> controllerType,
                                  String methodName,
                                  ApiResourceAccessMode mode,
                                  String permission,
                                  Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = controllerType.getMethod(methodName, parameterTypes);
        ApiAccess apiAccess = method.getAnnotation(ApiAccess.class);
        assertThat(apiAccess).isNotNull();
        assertThat(apiAccess.mode()).isEqualTo(mode);
        if (mode == ApiResourceAccessMode.PERMISSION) {
            assertThat(apiAccess.permission()).isEqualTo(permission);
        } else {
            assertThat(apiAccess.permission()).isBlank();
        }
    }
}
