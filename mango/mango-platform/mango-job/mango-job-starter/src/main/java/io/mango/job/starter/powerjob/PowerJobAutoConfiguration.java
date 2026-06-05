package io.mango.job.starter.powerjob;

import io.mango.job.core.service.engine.IMangoJobEngine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import tech.powerjob.client.PowerJobClient;

/**
 * PowerJob Adapter 自动配置。
 */
@Configuration
@ConditionalOnClass(PowerJobClient.class)
@EnableConfigurationProperties(PowerJobProperties.class)
public class PowerJobAutoConfiguration {

    @Bean
    @ConditionalOnBean(PowerJobClient.class)
    @ConditionalOnMissingBean(IPowerJobClientOperations.class)
    @ConditionalOnProperty(prefix = "mango.job.powerjob", name = "enabled", havingValue = "true")
    IPowerJobClientOperations powerJobClientOperations(PowerJobClient client) {
        return new PowerJobClientOperations(client);
    }

    @Bean
    @ConditionalOnMissingBean({IPowerJobClientOperations.class, PowerJobClient.class})
    @ConditionalOnProperty(prefix = "mango.job.powerjob", name = "enabled", havingValue = "true")
    IPowerJobClientOperations lazyPowerJobClientOperations(PowerJobProperties properties) {
        validateClientProperties(properties);
        return new PowerJobClientOperations(properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "powerJobEngineAdapter")
    @ConditionalOnProperty(prefix = "mango.job.powerjob", name = "enabled", havingValue = "true")
    IMangoJobEngine powerJobEngineAdapter(IPowerJobClientOperations client, PowerJobProperties properties) {
        return new PowerJobEngineAdapter(client, properties);
    }

    private void validateClientProperties(PowerJobProperties properties) {
        if (properties.getServerAddresses().isEmpty()) {
            throw new IllegalStateException("mango.job.powerjob.server-addresses 不能为空");
        }
        if (!StringUtils.hasText(properties.getAppName())) {
            throw new IllegalStateException("mango.job.powerjob.app-name 不能为空");
        }
        if (!StringUtils.hasText(properties.getPassword())) {
            throw new IllegalStateException("mango.job.powerjob.password 不能为空");
        }
        if (properties.getAppId() == null) {
            throw new IllegalStateException("mango.job.powerjob.app-id 不能为空");
        }
    }
}
