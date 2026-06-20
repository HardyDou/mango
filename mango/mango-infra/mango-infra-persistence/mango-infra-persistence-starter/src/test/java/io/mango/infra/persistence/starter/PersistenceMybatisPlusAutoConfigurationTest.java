package io.mango.infra.persistence.starter;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersistenceMybatisPlusAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PersistenceMybatisPlusAutoConfiguration.class));

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void defaultPaginationInterceptor_shouldBeCreated() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(MybatisPlusInterceptor.class);
            assertThat(ctx.getBean(MybatisPlusInterceptor.class).getInterceptors())
                    .anyMatch(TenantLineInnerInterceptor.class::isInstance)
                    .anyMatch(PaginationInnerInterceptor.class::isInstance);
        });
    }

    @Test
    void disabledPagination_shouldKeepTenantInterceptor() {
        contextRunner
                .withPropertyValues("mango.persistence.mybatis-plus.pagination.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(ctx.getBean(MybatisPlusInterceptor.class).getInterceptors())
                            .anyMatch(TenantLineInnerInterceptor.class::isInstance)
                            .noneMatch(PaginationInnerInterceptor.class::isInstance);
                });
    }

    @Test
    void disabledTenantAndPagination_shouldCreateEmptyInterceptor() {
        contextRunner
                .withPropertyValues(
                        "mango.persistence.mybatis-plus.tenant.enabled=false",
                        "mango.persistence.mybatis-plus.pagination.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(ctx.getBean(MybatisPlusInterceptor.class).getInterceptors()).isEmpty();
                });
    }

    @Test
    void tenantInterceptor_shouldRequireExplicitTenantContextByDefault() {
        contextRunner.run(ctx -> {
            TenantLineHandler handler = tenantLineHandler(ctx.getBean(MybatisPlusInterceptor.class));

            assertThatThrownBy(handler::getTenantId)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Missing tenant context");
        });
    }

    @Test
    void tenantInterceptor_shouldAllowConfiguredDefaultTenantForNonWebTasks() {
        contextRunner
                .withPropertyValues("mango.persistence.mybatis-plus.tenant.default-tenant-id=9")
                .run(ctx -> {
                    TenantLineHandler handler = tenantLineHandler(ctx.getBean(MybatisPlusInterceptor.class));
                    assertThat(handler.getTenantId()).isEqualTo(new LongValue(9));
                });
    }

    @Test
    void tenantInterceptor_shouldUseMangoContextTenant() {
        MangoContextHolder.set(MangoContextSnapshot.request(null, null, "8", null, null));

        contextRunner.run(ctx -> {
            TenantLineHandler handler = tenantLineHandler(ctx.getBean(MybatisPlusInterceptor.class));
            assertThat(handler.getTenantId()).isEqualTo(new LongValue(8));
        });
    }

    @Test
    void tenantInterceptor_shouldIgnoreResourceRegistryTablesByDefault() {
        contextRunner.run(ctx -> {
            TenantLineHandler handler = tenantLineHandler(ctx.getBean(MybatisPlusInterceptor.class));
            assertThat(handler.ignoreTable("resource_registry")).isTrue();
            assertThat(handler.ignoreTable("resource_sync_log")).isTrue();
            assertThat(handler.ignoreTable("resource_change_log")).isTrue();
        });
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

    private TenantLineHandler tenantLineHandler(MybatisPlusInterceptor interceptor) {
        TenantLineInnerInterceptor tenantInterceptor = interceptor.getInterceptors()
                .stream()
                .filter(TenantLineInnerInterceptor.class::isInstance)
                .map(TenantLineInnerInterceptor.class::cast)
                .findFirst()
                .orElseThrow();
        try {
            Field field = TenantLineInnerInterceptor.class.getDeclaredField("tenantLineHandler");
            field.setAccessible(true);
            return (TenantLineHandler) field.get(tenantInterceptor);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot read tenant line handler", e);
        }
    }
}
