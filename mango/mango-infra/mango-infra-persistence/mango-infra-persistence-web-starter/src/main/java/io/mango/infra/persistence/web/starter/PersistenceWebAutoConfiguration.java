package io.mango.infra.persistence.web.starter;

import io.mango.infra.persistence.web.starter.excel.ExcelAdapter;
import io.mango.infra.persistence.web.starter.excel.RequestExcelArgumentResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 持久化 Web 能力自动配置入口。
 */
@AutoConfiguration
public class PersistenceWebAutoConfiguration {

    @Bean
    @ConditionalOnBean(ExcelAdapter.class)
    public WebMvcConfigurer requestExcelWebMvcConfigurer(ExcelAdapter excelAdapter) {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new RequestExcelArgumentResolver(excelAdapter));
            }
        };
    }
}
