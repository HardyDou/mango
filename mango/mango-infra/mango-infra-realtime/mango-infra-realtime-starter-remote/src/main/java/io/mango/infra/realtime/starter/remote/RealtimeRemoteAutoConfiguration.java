package io.mango.infra.realtime.starter.remote;

import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.api.RealtimePublisher;
import io.mango.infra.realtime.core.inbound.InboundUnknownTypePolicy;
import io.mango.infra.realtime.core.inbound.LocalRealtimeInboundDispatcher;
import io.mango.infra.realtime.core.inbound.RealtimeInboundDispatcher;
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
    @ConditionalOnMissingBean(RealtimePublisher.class)
    public RealtimePublisher remoteRealtimePublisher(RealtimeApi realtimeApi) {
        return new RemoteRealtimePublisher(realtimeApi);
    }

    @Bean
    @ConditionalOnMissingBean
    public RealtimeInboundDispatcher realtimeInboundDispatcher(ListableBeanFactory beanFactory,
                                                               RealtimeRemoteProperties properties) {
        return new LocalRealtimeInboundDispatcher(
                beanFactory,
                properties.getInbound().isFailFast(),
                unknownTypePolicy(properties.getInbound().getUnknownTypePolicy()));
    }

    @Bean
    @ConditionalOnExpression("'${mango.infra.realtime.inbound.enabled:false}' == 'true' "
            + "&& '${mango.infra.realtime.inbound.remote.endpoint-enabled:true}' == 'true'")
    public RealtimeInboundRemoteController realtimeInboundRemoteController(RealtimeInboundDispatcher dispatcher) {
        return new RealtimeInboundRemoteController(dispatcher);
    }

    @Bean
    @ConditionalOnProperty(prefix = "mango.infra.realtime.inbound", name = "enabled", havingValue = "true")
    public RealtimeSubscriberAutoRegistrar realtimeSubscriberAutoRegistrar(
            RealtimeSubscriberFeignClient subscriberApi,
            RealtimeInboundDispatcher dispatcher,
            RealtimeRemoteProperties properties,
            Environment environment) {
        return new RealtimeSubscriberAutoRegistrar(subscriberApi, dispatcher, properties, environment);
    }

    private InboundUnknownTypePolicy unknownTypePolicy(String value) {
        return InboundUnknownTypePolicy.valueOf(value.trim().toUpperCase());
    }
}
