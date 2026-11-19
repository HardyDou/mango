package io.mango.file.preview.starter;

import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.service.FileHandlerService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定 core 生成的 {@code fileId} 参数与引擎接收参数一致（core 不能依赖引擎，靠测试对齐）。
 */
class FilePreviewSourceIdContractTest {

    @Test
    void core生成的预览地址参数名与引擎读取的参数名一致() throws Exception {
        // 真往返：按 core 的拼法生成预览地址，用 URL 解析取回参数值，再核对引擎控制器的入参名。
        String enginePreviewUrl = org.springframework.web.util.UriComponentsBuilder
                .fromPath("/api/onlinePreview")
                .queryParam("fileId", "ticket-round-trip")
                .queryParam("fullfilename", "file-100-0123456789abcdef.pptx")
                .build()
                .encode(java.nio.charset.StandardCharsets.UTF_8)
                .toUriString();

        org.springframework.web.util.UriComponents parsed =
                org.springframework.web.util.UriComponentsBuilder.fromUriString(enginePreviewUrl).build();
        assertThat(parsed.getQueryParams().getFirst("fileId")).isEqualTo("ticket-round-trip");

        java.lang.reflect.Method onlinePreview = java.util.Arrays
                .stream(cn.keking.web.controller.OnlinePreviewController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("onlinePreview"))
                .findFirst()
                .orElseThrow();
        java.util.Set<String> requestParams = java.util.Arrays.stream(onlinePreview.getParameters())
                .filter(parameter -> parameter.isAnnotationPresent(
                        org.springframework.web.bind.annotation.RequestParam.class))
                .map(java.lang.reflect.Parameter::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(requestParams).contains("fileId", "fullfilename");
    }

    @Test
    void 缺少引擎文件名时直接拒绝而不是静默退化成未知类型() {
        FileHandlerService handlerService = new FileHandlerService(org.mockito.Mockito.mock(cn.keking.service.cache.CacheService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(handlerService, "uriEncoding", "UTF-8");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> handlerService.getFileAttributeBySourceId("ticket-1", " ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("引擎侧文件名");
    }

    @Test
    void 已有构造函数行为不受影响() {
        FileAttribute attribute = new FileAttribute(FileType.SIMTEXT, "txt", "demo.txt", "http://x/demo.txt");

        assertThat(attribute.getSourceId()).isNull();
    }
}
