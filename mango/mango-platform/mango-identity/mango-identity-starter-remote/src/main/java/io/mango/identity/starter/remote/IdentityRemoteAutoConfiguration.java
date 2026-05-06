package io.mango.identity.starter.remote;

import io.mango.common.result.R;
import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.query.AuthUsernameQuery;
import io.mango.identity.api.vo.AuthUserInfo;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

/**
 * 身份模块远程自动配置。
 */
@AutoConfiguration
@EnableFeignClients(basePackages = "io.mango.identity.starter.remote")
public class IdentityRemoteAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthUserProvider authUserProvider(AuthIdentityFeignClient authIdentityFeignClient) {
        return new RemoteAuthUserProvider(authIdentityFeignClient);
    }

    private static class RemoteAuthUserProvider implements AuthUserProvider {

        private final AuthIdentityFeignClient authIdentityFeignClient;

        RemoteAuthUserProvider(AuthIdentityFeignClient authIdentityFeignClient) {
            this.authIdentityFeignClient = authIdentityFeignClient;
        }

        @Override
        public AuthUserInfo getByUsernameForAuth(String username) {
            AuthUsernameQuery query = new AuthUsernameQuery();
            query.setUsername(username);
            return unwrap(authIdentityFeignClient.getByUsernameForAuth(query));
        }

        @Override
        public AuthUserInfo getByUsernameForAuth(String username, String realm) {
            AuthUsernameQuery query = new AuthUsernameQuery();
            query.setUsername(username);
            query.setRealm(realm);
            return unwrap(authIdentityFeignClient.getByUsernameForAuth(query));
        }

        @Override
        public AuthUserInfo getByIdForAuth(Long userId) {
            return unwrap(authIdentityFeignClient.getByIdForAuth(userId));
        }

        private AuthUserInfo unwrap(R<AuthUserInfo> response) {
            return response != null && response.isSuccess() ? response.getData() : null;
        }
    }
}
