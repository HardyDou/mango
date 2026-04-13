package io.mango.infra.test.log;

import io.mango.infra.log.starter.LogProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LogProperties 测试
 *
 * @author Mango
 */
class LogPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Configuration
    @EnableConfigurationProperties(LogProperties.class)
    static class TestConfig {
    }

    @Test
    void shouldLoadDefaultProperties() {
        runner.run(context -> {
            LogProperties properties = context.getBean(LogProperties.class);

            // 验证默认值
            assertThat(properties.getLevel().getRoot()).isEqualTo("INFO");
            assertThat(properties.getLevel().getMango()).isEqualTo("DEBUG");
            assertThat(properties.getLevel().getSpring()).isEqualTo("WARN");
            assertThat(properties.getLevel().getMybatis()).isEqualTo("WARN");
            assertThat(properties.getLevel().getHttp()).isEqualTo("INFO");

            assertThat(properties.getFile().getMaxSize()).isEqualTo("100MB");
            assertThat(properties.getFile().getMaxHistory()).isEqualTo(30);
            assertThat(properties.getFile().getTotalSizeCap()).isEqualTo("3GB");

            assertThat(properties.getOperation().isEnabled()).isTrue();
            assertThat(properties.getOperation().getMaxHistory()).isEqualTo(90);
            assertThat(properties.getOperation().getTotalSizeCap()).isEqualTo("10GB");

            assertThat(properties.getJson().isEnabled()).isFalse();
            assertThat(properties.getTrace().isEnabled()).isTrue();
        });
    }

    @Test
    void shouldBindCustomProperties() {
        runner
                .withPropertyValues(
                        "mango.log.level.root=DEBUG",
                        "mango.log.level.mango=TRACE",
                        "mango.log.level.spring=ERROR",
                        "mango.log.level.mybatis=INFO",
                        "mango.log.level.http=DEBUG",
                        "mango.log.file.maxSize=200MB",
                        "mango.log.file.maxHistory=60",
                        "mango.log.file.totalSizeCap=5GB",
                        "mango.log.operation.enabled=false",
                        "mango.log.operation.maxHistory=30",
                        "mango.log.operation.totalSizeCap=5GB",
                        "mango.log.json.enabled=true",
                        "mango.log.trace.enabled=true",
                        "mango.log.trace.headerName=X-Custom-Trace"
                )
                .run(context -> {
                    LogProperties properties = context.getBean(LogProperties.class);

                    assertThat(properties.getLevel().getRoot()).isEqualTo("DEBUG");
                    assertThat(properties.getLevel().getMango()).isEqualTo("TRACE");
                    assertThat(properties.getLevel().getSpring()).isEqualTo("ERROR");
                    assertThat(properties.getLevel().getMybatis()).isEqualTo("INFO");
                    assertThat(properties.getLevel().getHttp()).isEqualTo("DEBUG");

                    assertThat(properties.getFile().getMaxSize()).isEqualTo("200MB");
                    assertThat(properties.getFile().getMaxHistory()).isEqualTo(60);
                    assertThat(properties.getFile().getTotalSizeCap()).isEqualTo("5GB");

                    assertThat(properties.getOperation().isEnabled()).isFalse();
                    assertThat(properties.getOperation().getMaxHistory()).isEqualTo(30);
                    assertThat(properties.getOperation().getTotalSizeCap()).isEqualTo("5GB");

                    assertThat(properties.getJson().isEnabled()).isTrue();
                    assertThat(properties.getTrace().isEnabled()).isTrue();
                    assertThat(properties.getTrace().getHeaderName()).isEqualTo("X-Custom-Trace");
                });
    }
}
