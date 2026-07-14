package io.mango.cms.starter;

import io.mango.cms.starter.endpoint.CmsPublicFileEndpoint;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Configuration
@ConditionalOnProperty(prefix = "mango.cms", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan(basePackages = "io.mango.cms.core.mapper", annotationClass = Mapper.class)
@ComponentScan({
    "io.mango.cms.core.service",
    "io.mango.cms.starter.controller",
    "io.mango.cms.starter.endpoint"
})
public class CmsAutoConfiguration {

    @Bean
    public static CmsPermitPathBeanPostProcessor cmsPermitPathBeanPostProcessor() {
        return new CmsPermitPathBeanPostProcessor();
    }

    @Bean
    public RouterFunction<ServerResponse> cmsPublicFileRoutes(CmsPublicFileEndpoint endpoint) {
        return route(GET("/cms/open/files/public-preview"), endpoint::handle);
    }
}
