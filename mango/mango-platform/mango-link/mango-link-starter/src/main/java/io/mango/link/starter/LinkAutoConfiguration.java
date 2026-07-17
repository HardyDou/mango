package io.mango.link.starter;

import io.mango.link.starter.endpoint.LinkRedirectEndpoint;
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
@ConditionalOnProperty(prefix = "mango.link", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan(basePackages = "io.mango.link.core.mapper", annotationClass = Mapper.class)
@ComponentScan({
    "io.mango.link.core.service",
    "io.mango.link.core.integration",
    "io.mango.link.starter.controller",
    "io.mango.link.starter.endpoint",
    "io.mango.link.starter.resource"
})
public class LinkAutoConfiguration {

    @Bean
    public static LinkPermitPathBeanPostProcessor linkPermitPathBeanPostProcessor() {
        return new LinkPermitPathBeanPostProcessor();
    }

    @Bean
    public RouterFunction<ServerResponse> linkRedirectRoutes(LinkRedirectEndpoint endpoint) {
        return route(GET("/link/open/redirect"), endpoint::redirect)
                .andRoute(GET("/link/open/jump"), endpoint::jump)
                .andRoute(GET("/link/visible-links/redirect"), endpoint::redirect)
                .andRoute(GET("/link/visible-links/jump"), endpoint::jump);
    }
}
