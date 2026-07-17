package io.mango.file.preview.api;

import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class FilePreviewApiTest {

    @Test
    void preview_文件Id约束由Api契约统一声明() throws NoSuchMethodException {
        Method preview = FilePreviewApi.class.getMethod("preview", Long.class);

        NotNull notNull = preview.getParameters()[0].getAnnotation(NotNull.class);

        assertThat(notNull).isNotNull();
        assertThat(notNull.message()).isEqualTo("文件ID不能为空");
    }
}
