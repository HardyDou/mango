package io.mango.file.starter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FilePermissionResourceTest {

    @Test
    void fileMenu_声明预览入口使用的下载权限() throws IOException {
        try (var input = getClass().getResourceAsStream(
                "/META-INF/mango/resources/file-common-menu.json")) {
            assertThat(input).isNotNull();
            String resource = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(resource).contains("\"version\": 2");
            assertThat(resource).contains("\"file:files:download\"");
        }
    }
}
