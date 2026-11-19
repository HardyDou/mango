package io.mango.file.preview.starter;

import cn.keking.config.ConfigConstants;
import cn.keking.spi.PreviewSourceProviders;
import io.mango.file.preview.core.config.FilePreviewProperties;
import io.mango.file.preview.core.config.FilePreviewSourceMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilePreviewTrustHostInitializerTest {

    private cn.keking.spi.PreviewSourceProvider provider;

    @AfterEach
    void resetTrustedHosts() {
        ConfigConstants.setTrustHostValue("default");
        PreviewSourceProviders.unregister(this.provider);
        this.provider = null;
    }

    @Test
    void run_addsSourceBaseHostWithoutLosingConfiguredHosts() {
        ConfigConstants.setTrustHostValue("localhost,preview.mango.test");
        new FilePreviewTrustHostInitializer("http://127.0.0.1:18018/internal/").run(null);

        assertThat(ConfigConstants.getTrustHostSet())
                .contains("localhost", "preview.mango.test", "127.0.0.1");
    }

    @Test
    void run_whenSourceBaseUrlMissing_keepsConfiguredHosts() {
        ConfigConstants.setTrustHostValue("localhost");
        new FilePreviewTrustHostInitializer((String) null).run(null);

        assertThat(ConfigConstants.getTrustHostSet()).containsExactly("localhost");
    }

    @Test
    void run_whenSourceBaseUrlInvalid_keepsConfiguredHosts() {
        ConfigConstants.setTrustHostValue("localhost");
        new FilePreviewTrustHostInitializer("not a valid url").run(null);

        assertThat(ConfigConstants.getTrustHostSet()).containsExactly("localhost");
    }

    @Test
    void run_fileId模式不把对外域名写进信任列表() {
        ConfigConstants.setTrustHostValue("localhost");
        this.provider = sourceId -> new java.io.ByteArrayInputStream(new byte[0]);
        PreviewSourceProviders.register(this.provider);
        FilePreviewProperties properties = new FilePreviewProperties();
        properties.setSourceBaseUrl("https://business.example.com");
        properties.setSourceMode(FilePreviewSourceMode.FILE_ID);

        new FilePreviewTrustHostInitializer(properties).run(null);

        assertThat(ConfigConstants.getTrustHostSet()).containsExactly("localhost");
    }

    @Test
    void run_fileId模式缺少直取实现时启动失败() {
        FilePreviewProperties properties = new FilePreviewProperties();
        properties.setSourceMode(FilePreviewSourceMode.FILE_ID);

        assertThatThrownBy(() -> new FilePreviewTrustHostInitializer(properties).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("直取实现");
    }

    @Test
    void run_auto模式有直取实现时同样跳过信任列表() {
        ConfigConstants.setTrustHostValue("localhost");
        this.provider = sourceId -> new java.io.ByteArrayInputStream(new byte[0]);
        PreviewSourceProviders.register(this.provider);
        FilePreviewProperties properties = new FilePreviewProperties();
        properties.setSourceBaseUrl("https://business.example.com");

        new FilePreviewTrustHostInitializer(properties).run(null);

        assertThat(ConfigConstants.getTrustHostSet()).containsExactly("localhost");
    }

    @Test
    void run_auto模式没有直取实现时仍注册源地址host() {
        ConfigConstants.setTrustHostValue("localhost");
        FilePreviewProperties properties = new FilePreviewProperties();
        properties.setSourceBaseUrl("https://business.example.com");

        new FilePreviewTrustHostInitializer(properties).run(null);

        assertThat(ConfigConstants.getTrustHostSet()).contains("localhost", "business.example.com");
    }
}
