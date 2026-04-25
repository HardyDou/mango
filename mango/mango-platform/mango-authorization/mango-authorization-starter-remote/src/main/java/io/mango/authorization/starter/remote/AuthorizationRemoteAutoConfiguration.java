package io.mango.authorization.starter.remote;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.AuthorizationSnapshot;
import io.mango.authorization.api.IAuthorizationProvider;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Authorization remote auto configuration - enables Feign clients
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
            return authorizationFeignClient.loadUserAuthorization(
                    query.subjectId(), query.tenantId(), query.systemCode());
        };
    }

}
