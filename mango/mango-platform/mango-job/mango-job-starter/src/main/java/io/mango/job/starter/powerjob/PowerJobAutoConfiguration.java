package io.mango.job.starter.powerjob;

import io.mango.job.core.service.engine.IMangoJobEngine;
import io.mango.job.core.service.IMangoJobHandlerRegistry;
import io.mango.job.core.service.impl.MangoJobDataSourceRouter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import tech.powerjob.client.PowerJobClient;
import tech.powerjob.common.enums.Protocol;
import tech.powerjob.worker.PowerJobSpringWorker;
import tech.powerjob.worker.common.PowerJobWorkerConfig;
import tech.powerjob.worker.common.constants.StoreStrategy;

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
    IMangoJobEngine powerJobEngineAdapter(IPowerJobClientOperations client,
                                          PowerJobProperties properties,
                                          ObjectProvider<IPowerJobNativeLogReader> nativeLogReaderProvider,
                                          ObjectProvider<IPowerJobInstanceReader> instanceReaderProvider) {
        return new PowerJobEngineAdapter(client, properties, nativeLogReaderProvider.getIfAvailable(),
                instanceReaderProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.job.powerjob.native-log", name = "enabled", havingValue = "true")
    IPowerJobNativeLogReader powerJobNativeLogReader(PowerJobFileMapper fileMapper,
                                                     MangoJobDataSourceRouter dataSourceRouter,
                                                     PowerJobProperties properties) {
        return new PowerJobDatabaseNativeLogReader(fileMapper, dataSourceRouter, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.job.powerjob.native-log", name = "enabled", havingValue = "true")
    IPowerJobInstanceReader powerJobInstanceReader(PowerJobInstanceInfoMapper instanceInfoMapper,
                                                   MangoJobDataSourceRouter dataSourceRouter,
                                                   PowerJobProperties properties) {
        return new PowerJobDatabaseInstanceReader(instanceInfoMapper, dataSourceRouter, properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "mangoPowerJobProcessor")
    @ConditionalOnProperty(prefix = "mango.job.powerjob.worker", name = "enabled", havingValue = "true")
    MangoPowerJobProcessor mangoPowerJobProcessor(IMangoJobHandlerRegistry handlerRegistry,
                                                  PowerJobProperties properties) {
        return new MangoPowerJobProcessor(handlerRegistry, properties.getWorker().isCaptureConsoleOutput());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.job.powerjob.worker", name = "enabled", havingValue = "true")
    PowerJobSpringWorker powerJobSpringWorker(PowerJobProperties properties) {
        validateWorkerProperties(properties);
        return new PowerJobSpringWorker(toWorkerConfig(properties));
    }

    private void validateClientProperties(PowerJobProperties properties) {
        if (properties.getServerAddresses().isEmpty()) {
            throw new IllegalStateException("mango.job.powerjob.server-addresses 不能为空");
        }
        validatePowerJobServerAddressFormat(properties.getServerAddresses());
        if (!StringUtils.hasText(properties.getAppName())) {
            throw new IllegalStateException("mango.job.powerjob.app-name 不能为空");
        }
        if (!StringUtils.hasText(properties.getPassword())) {
            throw new IllegalStateException("mango.job.powerjob.password 不能为空");
        }
    }

    private void validateWorkerProperties(PowerJobProperties properties) {
        PowerJobProperties.Worker worker = properties.getWorker();
        if (!StringUtils.hasText(resolveWorkerAppName(properties))) {
            throw new IllegalStateException("mango.job.powerjob.worker.app-name 或 mango.job.powerjob.app-name 不能为空");
        }
        if (resolveWorkerServerAddresses(properties).isEmpty()) {
            throw new IllegalStateException("mango.job.powerjob.worker.server-addresses 或 mango.job.powerjob.server-addresses 不能为空");
        }
        validatePowerJobServerAddressFormat(resolveWorkerServerAddresses(properties));
        validatePowerJobServerAddressFormat(properties.getServerAddresses());
    }

    private PowerJobWorkerConfig toWorkerConfig(PowerJobProperties properties) {
        PowerJobProperties.Worker worker = properties.getWorker();
        PowerJobWorkerConfig config = new PowerJobWorkerConfig();
        config.setAppName(resolveWorkerAppName(properties));
        config.setServerAddress(resolveWorkerServerAddresses(properties));
        if (worker.getPort() != null) {
            config.setPort(worker.getPort());
        } else if (worker.getAkkaPort() != null) {
            config.setPort(worker.getAkkaPort());
        }
        config.setProtocol(Protocol.valueOf(worker.getProtocol()));
        config.setStoreStrategy(StoreStrategy.valueOf(worker.getStoreStrategy()));
        config.setAllowLazyConnectServer(worker.isAllowLazyConnectServer());
        config.setMaxResultLength(worker.getMaxResultLength());
        config.setMaxAppendedWfContextLength(worker.getMaxAppendedWfContextLength());
        config.setTag(worker.getTag());
        config.setMaxLightweightTaskNum(worker.getMaxLightweightTaskNum());
        config.setMaxHeavyweightTaskNum(worker.getMaxHeavyweightTaskNum());
        config.setHealthReportInterval(worker.getHealthReportInterval());
        return config;
    }

    private String resolveWorkerAppName(PowerJobProperties properties) {
        return StringUtils.hasText(properties.getWorker().getAppName())
                ? properties.getWorker().getAppName()
                : properties.getAppName();
    }

    private java.util.List<String> resolveWorkerServerAddresses(PowerJobProperties properties) {
        return properties.getWorker().getServerAddresses().isEmpty()
                ? properties.getServerAddresses()
                : properties.getWorker().getServerAddresses();
    }

    private void validatePowerJobServerAddressFormat(java.util.List<String> serverAddresses) {
        serverAddresses.stream()
                .filter(StringUtils::hasText)
                .filter(address -> address.startsWith("http://") || address.startsWith("https://"))
                .findFirst()
                .ifPresent(address -> {
                    throw new IllegalStateException(
                            "PowerJob server-addresses 使用 host:port 格式，不要携带 http(s)://：" + address);
                });
    }
}
