package cn.keking.spi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreviewSourceProvidersTest {

    private PreviewSourceProvider provider;

    @AfterEach
    void unregister() {
        PreviewSourceProviders.unregister(provider);
        provider = null;
    }

    @Test
    void register_之后可以判断能力并打开源文件() throws IOException {
        provider = sourceId -> new ByteArrayInputStream(("content-" + sourceId).getBytes(StandardCharsets.UTF_8));
        PreviewSourceProviders.register(provider);

        assertThat(PreviewSourceProviders.hasProvider()).isTrue();
        try (InputStream inputStream = PreviewSourceProviders.open("tok-1")) {
            assertThat(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("content-tok-1");
        }
    }

    @Test
    void unregister_之后能力消失() {
        provider = sourceId -> new ByteArrayInputStream(new byte[0]);
        PreviewSourceProviders.register(provider);
        PreviewSourceProviders.unregister(provider);

        assertThat(PreviewSourceProviders.hasProvider()).isFalse();
    }

    @Test
    void open_没有实现时抛出可读错误() {
        assertThatThrownBy(() -> PreviewSourceProviders.open("tok-1"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("直取实现");
    }

    @Test
    void register_传入null不改变当前实现() {
        provider = sourceId -> new ByteArrayInputStream(new byte[0]);
        PreviewSourceProviders.register(provider);
        PreviewSourceProviders.register(null);

        assertThat(PreviewSourceProviders.hasProvider()).isTrue();
    }

    @Test
    void name_默认使用实现类名() {
        provider = new PreviewSourceProvider() {
            @Override
            public InputStream open(String sourceId) {
                return new ByteArrayInputStream(new byte[0]);
            }
        };

        assertThat(provider.name()).isNotBlank();
    }
}
