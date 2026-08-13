package io.mango.notice.starter;

import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.authorization.support.autoconfigure.context.SpringSecurityContextProvider;
import io.mango.notice.starter.endpoint.NoticeInboundPublicEndpoint;
import io.mango.notice.starter.resource.NoticeInboundPublicResourceProvider;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@AutoConfiguration
@ComponentScan(basePackages = "io.mango.notice")
@MapperScan("io.mango.notice.core.mapper")
public class NoticeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ISecurityContextProvider.class)
    public ISecurityContextProvider securityContextProvider() {
        return new SpringSecurityContextProvider();
    }

    @Bean
    public RouterFunction<ServerResponse> noticeInboundPublicRoutes(NoticeInboundPublicEndpoint endpoint) {
        return route(GET(NoticeInboundPublicResourceProvider.WECOM_PATH)
                .or(POST(NoticeInboundPublicResourceProvider.WECOM_PATH)), endpoint::handleWecom)
                .andRoute(POST(NoticeInboundPublicResourceProvider.MAIL_PATH), endpoint::handleMail);
    }
}
