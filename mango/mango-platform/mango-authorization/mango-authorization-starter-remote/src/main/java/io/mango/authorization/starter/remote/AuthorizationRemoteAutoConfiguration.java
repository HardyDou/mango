package io.mango.authorization.starter.remote;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.AuthorizationSnapshot;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.common.result.R;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 授权远程自动配置，启用 Feign 客户端。
 *
 * @author Mango
 */
@AutoConfiguration
@EnableFeignClients(basePackages = "io.mango.authorization.starter.remote")
public class AuthorizationRemoteAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IAuthorizationProvider.class)
    public IAuthorizationProvider authorizationProvider(AuthorizationFeignClient authorizationFeignClient) {
        return query -> {
            if (!AuthorizationQuery.SUBJECT_TYPE_USER.equals(query.subjectType())) {
                return AuthorizationSnapshot.empty();
            }
            R<AuthorizationSnapshot> response = authorizationFeignClient.loadUserAuthorization(
                    query.subjectId(), query.tenantId(), query.systemCode());
            return response != null && response.isSuccess() && response.getData() != null
                    ? response.getData()
                    : AuthorizationSnapshot.empty();
        };
    }

}
