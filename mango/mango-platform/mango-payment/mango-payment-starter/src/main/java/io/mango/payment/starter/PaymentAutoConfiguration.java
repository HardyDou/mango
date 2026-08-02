package io.mango.payment.starter;

import io.mango.payment.core.service.PaymentObservabilityProperties;
import io.mango.payment.starter.endpoint.PaymentChannelPublicCallbackEndpoint;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@AutoConfiguration
@AutoConfigureAfter(name = "io.mango.workflow.starter.WorkflowAutoConfiguration")
@ConditionalOnProperty(prefix = "mango.payment", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PaymentProperties.class)
@MapperScan("io.mango.payment.core.mapper")
@ComponentScan({
    "io.mango.payment.core.service",
    "io.mango.payment.starter.notice",
    "io.mango.payment.starter.controller",
    "io.mango.payment.starter.endpoint",
    "io.mango.payment.starter.resource",
    "io.mango.payment.starter.workflow"
    })
public class PaymentAutoConfiguration {

    @Bean
    public PaymentObservabilityProperties paymentObservabilityProperties(PaymentProperties properties) {
        return properties.getObservability();
    }

    @Bean
    public RouterFunction<ServerResponse> paymentChannelPublicCallbackRoutes(
            PaymentChannelPublicCallbackEndpoint endpoint) {
        return route(GET("/payment/channel-callbacks/public")
                .or(POST("/payment/channel-callbacks/public")), endpoint::handle);
    }
}
