package io.mango.identity.starter;

import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.AuthIdentitySecurityProvider;
import io.mango.identity.core.service.impl.IdentitySecurityProperties;
import io.mango.identity.core.service.IIdentityUserService;
import io.mango.identity.core.service.impl.IdentityUserSecurityService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 身份服务自动配置。
 */
@Configuration
@EnableConfigurationProperties(IdentitySecurityProperties.class)
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

    @Bean
    @ConditionalOnMissingBean
    public AuthIdentitySecurityProvider authIdentitySecurityProvider(IdentityUserSecurityService securityService) {
        return securityService;
    }

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
