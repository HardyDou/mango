package io.mango.file.preview.starter;

import cn.keking.ServerMain;
import io.mango.file.preview.core.config.FilePreviewProperties;
import io.mango.file.preview.core.service.IFilePreviewService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FilePreviewAutoConfigurationTest {

    @Test
    void embeddedStarter_loadsEngineDefaultsWithoutOverridingServerPort() {
        PropertySource propertySource = FilePreviewAutoConfiguration.class.getAnnotation(PropertySource.class);

        assertThat(propertySource).isNotNull();
        assertThat(propertySource.value()).containsExactly("classpath:/mango-file-preview-engine.properties");
        assertThat(propertySource.ignoreResourceNotFound()).isTrue();
    }

    @Test
    void embeddedStarter_engineDefaultsUseJdkCacheAndDoNotDeclareServerPort() throws IOException {
        String properties = new String(
                getClass().getResourceAsStream("/mango-file-preview-engine.properties").readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(properties).contains("cache.type = ${KK_CACHE_TYPE:jdk}");
        assertThat(properties.lines()).noneMatch(line -> line.trim().startsWith("server.port"));
    }

    @Test
    void embeddedStarter_excludesStandaloneServerMainFromHostComponentScan() {
        ComponentScan componentScan = FilePreviewAutoConfiguration.class.getAnnotation(ComponentScan.class);

        assertThat(componentScan).isNotNull();
        assertThat(componentScan.excludeFilters()).singleElement().satisfies(filter -> {
            assertThat(filter.type()).isEqualTo(FilterType.ASSIGNABLE_TYPE);
            assertThat(filter.classes()).containsExactly(ServerMain.class);
        });
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SwitchProbeConfiguration.class));

    @Test
    void internalSourceEnabledFalse_不注册进程内下载器() {
        contextRunner.withPropertyValues("mango.file-preview.internal-source-enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(FilePreviewEngineDownloader.class));
    }

    @Test
    void internalSourceEnabledDefault_注册进程内下载器() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(FilePreviewEngineDownloader.class));
    }

    @Configuration
    @EnableConfigurationProperties(FilePreviewProperties.class)
    static class SwitchProbeConfiguration {

        @Bean
        IFilePreviewService filePreviewService() {
            return mock(IFilePreviewService.class);
        }

        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(IFilePreviewService.class)
        @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
                prefix = "mango.file-preview", name = "internal-source-enabled",
                havingValue = "true", matchIfMissing = true)
        FilePreviewEngineDownloader filePreviewEngineDownloader(IFilePreviewService filePreviewService) {
            return new FilePreviewEngineDownloader(filePreviewService);
        }
    }
}
