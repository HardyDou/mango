package io.mango.authorization.resource.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.AppModuleApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 应用模块资源清单同步自动配置。
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@EnableConfigurationProperties(AppModuleResourceManifestSyncProperties.class)
public class AppModuleResourceManifestSyncAutoConfiguration {

    @Bean
    @ConditionalOnBean(AppModuleApi.class)
    public AppModuleResourceManifestSyncRunner appModuleResourceManifestSyncRunner(
            AppModuleApi appModuleApi,
            ObjectMapper objectMapper,
            AppModuleResourceManifestSyncProperties properties) {
        return new AppModuleResourceManifestSyncRunner(appModuleApi, objectMapper, properties);
    }
}
