package io.mango.infra.realtime.starter.remote;

import io.mango.infra.realtime.support.inbound.IRealtimeInboundService;
import io.mango.infra.realtime.support.inbound.RealtimeInboundService;
import io.mango.infra.realtime.support.inbound.RealtimeInboundUnknownTypePolicy;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Auto-configuration for remote realtime publishing.
 */
@Configuration
@ConditionalOnMissingClass("io.mango.infra.realtime.starter.MangoRealtimeAutoConfiguration")
@EnableFeignClients(basePackageClasses = RealtimeFeignClient.class)
@EnableConfigurationProperties(RealtimeRemoteProperties.class)
public class RealtimeRemoteAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IRealtimeInboundService realtimeInboundService(ListableBeanFactory beanFactory,
                                                          RealtimeRemoteProperties properties) {
        return new RealtimeInboundService(
                beanFactory,
                properties.getInbound().isFailFast(),
                unknownTypePolicy(properties.getInbound().getUnknownTypePolicy()));
    }

    @Bean
    @ConditionalOnExpression("'${mango.infra.realtime.inbound.enabled:false}' == 'true' "
            + "&& '${mango.infra.realtime.inbound.remote.endpoint-enabled:true}' == 'true'")
    public RealtimeInboundRemoteController realtimeInboundRemoteController(
            IRealtimeInboundService realtimeInboundService) {
        return new RealtimeInboundRemoteController(realtimeInboundService);
    }

    @Bean
    @ConditionalOnProperty(prefix = "mango.infra.realtime.inbound", name = "enabled", havingValue = "true")
    public RealtimeInboundReceiverAutoRegistrar realtimeInboundReceiverAutoRegistrar(
            RealtimeInboundReceiverFeignClient realtimeInboundReceiverApi,
            IRealtimeInboundService realtimeInboundService,
            RealtimeRemoteProperties properties,
            Environment environment) {
        return new RealtimeInboundReceiverAutoRegistrar(
                realtimeInboundReceiverApi, realtimeInboundService, properties, environment);
    }

    private RealtimeInboundUnknownTypePolicy unknownTypePolicy(String value) {
        return RealtimeInboundUnknownTypePolicy.valueOf(value.trim().toUpperCase());
    }
}
