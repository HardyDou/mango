package io.mango.file.preview.starter.remote;

import cn.keking.spi.FileDownloaders;
import io.mango.file.preview.support.remote.FilePreviewRemoteSourceDownloader;
import io.mango.file.preview.support.remote.FilePreviewSourceTransport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class FilePreviewRemoteAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class,
                    FilePreviewRemoteAutoConfiguration.class));

    @Test
    void 未配置开关时不装配远程直取() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(FilePreviewSourceTransport.class);
            assertThat(context).doesNotHaveBean(FilePreviewRemoteSourceDownloader.class);
            assertThat(FileDownloaders.hasProvider()).isFalse();
        });
    }

    @Test
    void 开关为false时不装配远程直取() {
        runner.withPropertyValues("mango.file-preview.remote.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(FilePreviewSourceTransport.class);
                    assertThat(context).doesNotHaveBean(FilePreviewRemoteSourceDownloader.class);
                });
    }

    @Test
    void 开关打开且配置Mango地址时装配传输与下载器并注册到引擎() {
        runner.withPropertyValues("mango.file-preview.remote.enabled=true",
                        "mango.file-preview.remote.base-url=http://127.0.0.1:18080")
                .run(context -> {
                    assertThat(context).hasSingleBean(FilePreviewSourceTransport.class);
                    assertThat(context).hasSingleBean(FilePreviewRemoteSourceDownloader.class);
                    assertThat(context).hasSingleBean(FilePreviewFeignClient.class);
                    assertThat(context).hasSingleBean(FilePreviewSourceFeignClient.class);
                    assertThat(FileDownloaders.hasProvider()).isTrue();
                });
        assertThat(FileDownloaders.hasProvider()).isFalse();
    }

    @Test
    void 开关打开但缺少Mango地址时启动失败并提示配置项() {
        runner.withPropertyValues("mango.file-preview.remote.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("mango.file-preview.remote.base-url");
                });
    }
}
