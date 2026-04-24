package io.mango.infra.realtime.e2e.apps.remote;

import io.mango.infra.realtime.starter.remote.RealtimeFeignClient;
import io.mango.infra.realtime.starter.remote.RealtimeInboundReceiverAutoRegistrar;
import io.mango.infra.realtime.starter.remote.RealtimeInboundReceiverFeignClient;
import io.mango.infra.realtime.starter.remote.RealtimeInboundRemoteController;
import io.mango.infra.realtime.starter.remote.RealtimeRemoteProperties;
import io.mango.infra.realtime.support.inbound.RealtimeInboundService;
import io.mango.infra.realtime.support.inbound.IRealtimeInboundService;
import io.mango.infra.realtime.support.inbound.RealtimeInboundUnknownTypePolicy;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication(scanBasePackages = "io.mango.infra.realtime.e2e.apps.remote")
@EnableFeignClients(basePackageClasses = RealtimeFeignClient.class)
@EnableConfigurationProperties(RealtimeRemoteProperties.class)
public class RealtimeRemoteTestApplication {

    @Bean
    IRealtimeInboundService realtimeInboundService(ListableBeanFactory beanFactory,
                                                   RealtimeRemoteProperties properties) {
        return new RealtimeInboundService(
                beanFactory,
                properties.getInbound().isFailFast(),
                RealtimeInboundUnknownTypePolicy.valueOf(
                        properties.getInbound().getUnknownTypePolicy().trim().toUpperCase()));
    }

    @Bean
    RealtimeInboundRemoteController realtimeInboundRemoteController(IRealtimeInboundService realtimeInboundService) {
        return new RealtimeInboundRemoteController(realtimeInboundService);
    }

    @Bean
    RealtimeInboundReceiverAutoRegistrar realtimeInboundReceiverAutoRegistrar(
            RealtimeInboundReceiverFeignClient realtimeInboundReceiverApi,
            IRealtimeInboundService realtimeInboundService,
            RealtimeRemoteProperties properties,
            Environment environment) {
        return new RealtimeInboundReceiverAutoRegistrar(
                realtimeInboundReceiverApi,
                realtimeInboundService,
                properties,
                environment);
    }
}
