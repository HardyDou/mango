package io.mango.infra.log.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class LogAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LogAutoConfiguration.class));

    @Test
    void shouldRegisterDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(LogProperties.class);
            LogProperties properties = context.getBean(LogProperties.class);
            assertThat(properties.getLevel().getRoot()).isEqualTo("INFO");
            assertThat(properties.getLevel().getMango()).isEqualTo("DEBUG");
            assertThat(properties.getFile().getMaxSize()).isEqualTo("100MB");
            assertThat(properties.getFile().getMaxHistory()).isEqualTo(30);
            assertThat(properties.getOperation().isEnabled()).isTrue();
            assertThat(properties.getJson().isEnabled()).isFalse();
        });
    }

    @Test
    void shouldBindDocumentedProperties() {
        contextRunner.withPropertyValues(
                        "mango.log.level.root=ERROR",
                        "mango.log.level.mango=TRACE",
                        "mango.log.file.max-size=20MB",
                        "mango.log.file.max-history=7",
                        "mango.log.file.total-size-cap=500MB",
                        "mango.log.operation.enabled=false",
                        "mango.log.operation.max-history=5",
                        "mango.log.operation.total-size-cap=100MB",
                        "mango.log.json.enabled=true")
                .run(context -> {
                    LogProperties properties = context.getBean(LogProperties.class);
                    assertThat(properties.getLevel().getRoot()).isEqualTo("ERROR");
                    assertThat(properties.getLevel().getMango()).isEqualTo("TRACE");
                    assertThat(properties.getFile().getMaxSize()).isEqualTo("20MB");
                    assertThat(properties.getFile().getMaxHistory()).isEqualTo(7);
                    assertThat(properties.getFile().getTotalSizeCap()).isEqualTo("500MB");
                    assertThat(properties.getOperation().isEnabled()).isFalse();
                    assertThat(properties.getOperation().getMaxHistory()).isEqualTo(5);
                    assertThat(properties.getOperation().getTotalSizeCap()).isEqualTo("100MB");
                    assertThat(properties.getJson().isEnabled()).isTrue();
                });
    }
}
