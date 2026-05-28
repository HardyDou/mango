package io.mango.infra.sensitive.starter;

import com.fasterxml.jackson.databind.Module;
import io.mango.infra.sensitive.api.ISensitiveMaskingService;
import io.mango.infra.sensitive.api.ISensitiveRawAccessProvider;
import io.mango.infra.sensitive.api.ISensitiveWordProvider;
import io.mango.infra.sensitive.core.word.SensitiveWordCustomizer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SensitiveAutoConfiguration.class));

    @Test
    void sensitiveAutoConfiguration_withDefaults_registersCoreBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Module.class);
            assertThat(context).hasSingleBean(ISensitiveMaskingService.class);
            assertThat(context).hasSingleBean(SensitiveWordCustomizer.class);
        });
    }

    @Test
    void sensitiveWordCustomizer_withProvider_mergesProviderWords() {
        contextRunner.withUserConfiguration(WordProviderConfiguration.class)
                .run(context -> {
                    SensitiveWordCustomizer customizer = context.getBean(SensitiveWordCustomizer.class);
                    assertThat(customizer.allow()).containsExactly("allow-a");
                    assertThat(customizer.deny()).containsExactly("deny-a");
                });
    }

    @Test
    void sensitiveMaskingService_withRawAccessProvider_allowsRawOutput() {
        contextRunner.withUserConfiguration(RawAccessProviderConfiguration.class)
                .run(context -> {
                    ISensitiveMaskingService maskingService = context.getBean(ISensitiveMaskingService.class);
                    assertThat(maskingService.shouldMask(null)).isFalse();
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class RawAccessProviderConfiguration {

        @Bean
        ISensitiveRawAccessProvider sensitiveRawAccessProvider() {
            return authority -> "no_mask".equals(authority);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class WordProviderConfiguration {

        @Bean
        ISensitiveWordProvider sensitiveWordProvider() {
            return new ISensitiveWordProvider() {
                @Override
                public List<String> allowWords() {
                    return List.of("allow-a");
                }

                @Override
                public List<String> denyWords() {
                    return List.of("deny-a");
                }
            };
        }
    }
}
