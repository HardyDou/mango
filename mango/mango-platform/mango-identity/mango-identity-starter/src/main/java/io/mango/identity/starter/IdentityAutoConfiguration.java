package io.mango.identity.starter;

import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.core.service.IIdentityUserService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 身份服务自动配置。
 */
@Configuration
@MapperScan("io.mango.identity.core.mapper")
@ComponentScan({
        "io.mango.identity.core.service",
        "io.mango.identity.starter"
})
public class IdentityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthUserProvider authUserProvider(IIdentityUserService identityUserService) {
        return new IdentityAuthUserProvider(identityUserService);
    }
}
