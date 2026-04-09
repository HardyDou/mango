package io.mango.infra.feign.starter;

import feign.Logger;
import feign.Retryer;
import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign auto configuration
 * <p>
 * Provides OpenFeign infrastructure: timeout, retry, logging, and request interception.
 *
 * @author Mango
 */
@AutoConfiguration
@EnableConfigurationProperties(FeignProperties.class)
@ConditionalOnClass({feign.Feign.class, Filter.class})
@ConditionalOnProperty(prefix = "mango.feign", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FeignAutoConfiguration {

    /**
     * Configure Feign retryer based on properties
     */
    @Bean
    @ConditionalOnMissingBean
    public Retryer feignRetryer(FeignProperties properties) {
        return new Retryer.Default(properties.getConnectTimeout(),
                properties.getReadTimeout(),
                properties.getRetry());
    }

    /**
     * Configure Feign logger level
     */
    @Bean
    @ConditionalOnMissingBean
    public Logger.Level feignLoggerLevel(FeignProperties properties) {
        return properties.getLoggerLevel();
    }

    /**
     * Configure Feign request interceptor for context propagation
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.feign", name = "interceptor-enabled", havingValue = "true", matchIfMissing = true)
    public FeignRequestInterceptor feignRequestInterceptor() {
        return new FeignRequestInterceptor();
    }

    /**
     * Configure Feign request interceptor for internal call HMAC signature
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.feign", name = "internal-call-enabled", havingValue = "true", matchIfMissing = true)
    public InternalCallFeignInterceptor internalCallFeignInterceptor() {
        return new InternalCallFeignInterceptor();
    }

    /**
     * Configure Feign token filter to capture JWT from incoming requests
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.feign", name = "token-propagation-enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<FeignTokenFilter> feignTokenFilter() {
        FilterRegistrationBean<FeignTokenFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new FeignTokenFilter());
        registration.addUrlPatterns("/*");
        registration.setName("feignTokenFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 4);
        return registration;
    }
}
