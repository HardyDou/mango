package io.mango.infra.security.starter;

import io.mango.infra.security.api.ITokenService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Token auto-configuration.
 * <p>
 * Provides {@link ITokenService} implementation using JJWT.
 * JjwtTokenServiceImpl is a @Component — this auto-configuration's
 * @ComponentScan picks it up from io.mango.infra.security.core.impl.
 *
 * @author Mango
 */
@AutoConfiguration
@ComponentScan(
        basePackages = "io.mango.infra.security.core.impl",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {} // no exclusions, just controls which package
        )
)
public class TokenAutoConfiguration {

    /**
     * Fallback: create ITokenService if component scanning missed it.
     */
    @Bean
    @ConditionalOnMissingBean(ITokenService.class)
    public ITokenService tokenServiceFallback() {
        throw new IllegalStateException(
                "No ITokenService bean found. Ensure mango-infra-security-starter is on classpath.");
    }
}
