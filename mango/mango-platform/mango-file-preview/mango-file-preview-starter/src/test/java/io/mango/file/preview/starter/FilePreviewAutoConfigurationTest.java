package io.mango.file.preview.starter;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

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
}
