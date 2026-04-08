package io.mango.infra.security.starter;

import io.mango.infra.security.api.ITokenService;
import io.mango.infra.security.core.impl.JjwtTokenServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Token auto-configuration.
 * <p>
 * Provides {@link ITokenService} implementation using JJWT.
 * The implementation is always created when this starter is on classpath.
 *
 * @author Mango
 */
@AutoConfiguration
public class TokenAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ITokenService.class)
    public ITokenService tokenService() {
        return new JjwtTokenServiceImpl();
    }
}
