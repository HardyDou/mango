package io.mango.file.preview.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class FilePreviewControllerAccessModeTest {

    @Test
    void fileIdPreviewEndpointsUseLoginBaselineAccess() throws NoSuchMethodException {
        assertAccessMode("preview", ApiResourceAccessMode.LOGIN, Long.class);
        assertAccessMode("redirectPreview", ApiResourceAccessMode.LOGIN, Long.class);
    }

    @Test
    void tokenPreviewEndpointsRemainPublicBecauseTheyRequireShortLivedTokens() throws NoSuchMethodException {
        assertAccessMode("redirectPreviewEntry", ApiResourceAccessMode.PUBLIC, String.class);
        assertAccessMode("source", ApiResourceAccessMode.PUBLIC, String.class);
    }

    private void assertAccessMode(String methodName,
                                  ApiResourceAccessMode mode,
                                  Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = FilePreviewController.class.getMethod(methodName, parameterTypes);
        ApiAccess apiAccess = method.getAnnotation(ApiAccess.class);
        assertThat(apiAccess).isNotNull();
        assertThat(apiAccess.mode()).isEqualTo(mode);
        if (mode != ApiResourceAccessMode.PERMISSION) {
            assertThat(apiAccess.permission()).isBlank();
        }
    }
}
