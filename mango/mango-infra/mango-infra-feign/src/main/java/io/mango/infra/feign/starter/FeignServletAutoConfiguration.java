package io.mango.infra.feign.starter;

import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Servlet-only Feign support.
 */
@AutoConfiguration(after = FeignAutoConfiguration.class)
@ConditionalOnClass({Filter.class, FilterRegistrationBean.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "mango.feign", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FeignServletAutoConfiguration {

    /**
     * Configure Feign token filter to capture JWT from incoming requests.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.feign", name = "token-propagation-enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<FeignTokenFilter> feignTokenFilter() {
        FilterRegistrationBean<FeignTokenFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new FeignTokenFilter());
        registration.addUrlPatterns("/*");
        registration.setName("feignTokenFilter");
        registration.setOrder(FeignTokenFilter.ORDER);
        return registration;
    }
}
