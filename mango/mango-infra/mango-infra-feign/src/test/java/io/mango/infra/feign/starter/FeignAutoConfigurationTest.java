package io.mango.infra.feign.starter;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class FeignAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    FeignAutoConfiguration.class,
                    FeignServletAutoConfiguration.class));

    @Test
    void autoConfiguration_masterSwitchDisabled_registersNoFeignInfrastructure() {
        contextRunner.withPropertyValues("mango.feign.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(FeignRequestInterceptor.class);
                    assertThat(context).doesNotHaveBean("feignTokenFilter");
                });
    }

    @Test
    void servletAutoConfiguration_unrelatedFilterExists_keepsTokenFilter() {
        contextRunner.withUserConfiguration(UnrelatedFilterConfiguration.class)
                .run(context -> {
                    assertThat(context).hasBean("unrelatedFilter");
                    assertThat(context).hasBean("feignTokenFilter");
                    FilterRegistrationBean<?> registration = context.getBean(
                            "feignTokenFilter", FilterRegistrationBean.class);
                    assertThat(registration.getFilter()).isInstanceOf(FeignTokenFilter.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class UnrelatedFilterConfiguration {

        @Bean
        FilterRegistrationBean<Filter> unrelatedFilter() {
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
            registration.setFilter((request, response, chain) -> chain.doFilter(request, response));
            registration.setName("unrelatedFilter");
            return registration;
        }
    }
}
