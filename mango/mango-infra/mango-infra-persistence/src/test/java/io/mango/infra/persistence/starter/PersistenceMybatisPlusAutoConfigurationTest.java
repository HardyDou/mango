package io.mango.infra.persistence.starter;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceMybatisPlusAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PersistenceMybatisPlusAutoConfiguration.class));

    @Test
    void defaultPaginationInterceptor_shouldBeCreated() {
        contextRunner.run(ctx -> assertThat(ctx).hasSingleBean(MybatisPlusInterceptor.class));
    }

    @Test
    void disabledPagination_shouldNotCreateInterceptor() {
        contextRunner
                .withPropertyValues("mango.persistence.mybatis-plus.pagination.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(MybatisPlusInterceptor.class));
    }

    @Test
    void customInterceptor_shouldNotBeOverridden() {
        contextRunner
                .withUserConfiguration(CustomInterceptorConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(ctx.getBean(MybatisPlusInterceptor.class)).isSameAs(ctx.getBean("customInterceptor"));
                });
    }

    @Configuration
    static class CustomInterceptorConfig {
        @Bean
        MybatisPlusInterceptor customInterceptor() {
            return new MybatisPlusInterceptor();
        }
    }
}
