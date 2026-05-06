package io.mango.authorization.resource.access;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.security.IPermissionProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * API 资源运行时访问控制自动配置。
 *
 * @author hardy
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "io.mango.authorization.starter.AuthorizationAutoConfiguration",
        "io.mango.authorization.starter.remote.AuthorizationRemoteAutoConfiguration"
})
public class ApiResourceAccessAutoConfiguration {

    @Bean("apiResourceAuthorizationManager")
    @ConditionalOnBean({ApiResourceApi.class, IPermissionProvider.class})
    @ConditionalOnMissingBean(name = "apiResourceAuthorizationManager")
    @ConditionalOnProperty(name = "mango.authorization.resource-access.enabled", havingValue = "true", matchIfMissing = true)
    public AuthorizationManager<RequestAuthorizationContext> apiResourceAuthorizationManager(
            ApiResourceApi apiResourceApi,
            IPermissionProvider permissionService) {
        return new ApiResourceAuthorizationManager(apiResourceApi, permissionService);
    }
}
