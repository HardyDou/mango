package io.mango.resource.starter.remote;

import io.mango.infra.feign.starter.ModuleTargetResolver;
import io.mango.resource.support.ResourceTargetDispatcher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 资源注册中心远程自动配置。
 */
@Configuration
public class ResourceRemoteAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ResourceTargetDispatcher resourceTargetDispatcher(ModuleTargetResolver moduleTargetResolver,
                                                             ResourceTargetClient targetClient,
                                                             com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new RemoteResourceTargetDispatcher(moduleTargetResolver, targetClient, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ResourceTargetClient resourceTargetHttpClient(
            RestClient.Builder restClientBuilder,
            ObjectProvider<LoadBalancerClient> loadBalancerClientProvider,
            @Value("${mango.internal-call.secret:}") String sharedSecret,
            @Value("${mango.internal-call.secret-version:1}") int secretVersion) {
        return new ResourceTargetHttpClient(restClientBuilder.build(),
                loadBalancerClientProvider.getIfAvailable(), sharedSecret, secretVersion);
    }
}
