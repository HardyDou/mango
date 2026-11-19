package io.mango.file.preview.starter;

import cn.keking.spi.PreviewSourceProviders;
import io.mango.file.preview.core.service.IFilePreviewService;
import io.mango.file.preview.core.service.model.FilePreviewSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FilePreviewEngineSourceProviderTest {

    private FilePreviewEngineSourceProvider provider;

    @AfterEach
    void destroyProvider() {
        if (provider != null) {
            provider.destroy();
            provider = null;
        }
    }

    @Test
    void afterPropertiesSet_注册后引擎可以直取源文件() throws IOException {
        provider = providerReturning("hello-preview");
        provider.afterPropertiesSet();

        assertThat(PreviewSourceProviders.hasProvider()).isTrue();
        try (var inputStream = PreviewSourceProviders.open("token-1")) {
            ByteArrayOutputStream sink = new ByteArrayOutputStream();
            inputStream.transferTo(sink);
            assertThat(sink.toString(StandardCharsets.UTF_8)).isEqualTo("hello-preview");
        }
    }

    @Test
    void destroy_注销后能力消失() {
        provider = providerReturning("hello-preview");
        provider.afterPropertiesSet();
        provider.destroy();

        assertThat(PreviewSourceProviders.hasProvider()).isFalse();
    }

    @Test
    void internalSourceAvailable_内嵌引擎恒为真() {
        provider = providerReturning("hello-preview");

        assertThat(provider.internalSourceAvailable()).isTrue();
    }

    @Test
    void open_业务异常转换为IO异常() {
        IFilePreviewService service = mock(IFilePreviewService.class);
        when(service.openSource("expired")).thenThrow(new IllegalStateException("预览令牌无效或已过期"));
        provider = new FilePreviewEngineSourceProvider(service);

        assertThatThrownBy(() -> provider.open("expired"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("预览令牌无效或已过期");
    }

    private FilePreviewEngineSourceProvider providerReturning(String content) {
        IFilePreviewService service = mock(IFilePreviewService.class);
        when(service.openSource("token-1")).thenReturn(new FilePreviewSource(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), "demo.txt", "text/plain", content.length()));
        return new FilePreviewEngineSourceProvider(service);
    }
}
