package io.mango.file.preview.starter.controller;

import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 防止 Jakarta Bean Validation HV000151：
 * 覆写方法不得重复声明父接口已声明的参数约束。
 */
class FilePreviewControllerConstraintTest {

    @Test
    void preview_NotNull约束只在Api契约声明() throws NoSuchMethodException {
        Method preview = FilePreviewController.class.getMethod("preview", Long.class, Boolean.class);

        assertThat(preview.getParameters()[0].getAnnotation(NotNull.class)).isNull();
        assertThat(preview.getParameters()[1].getAnnotation(NotNull.class)).isNull();
    }

    @Test
    void status_NotNull约束只在Api契约声明() throws NoSuchMethodException {
        Method status = FilePreviewController.class.getMethod("status", Long.class, Boolean.class);

        assertThat(status.getParameters()[0].getAnnotation(NotNull.class)).isNull();
        assertThat(status.getParameters()[1].getAnnotation(NotNull.class)).isNull();
    }
}
