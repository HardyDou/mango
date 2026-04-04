package io.mango.infra.web.starter;

import io.mango.infra.web.filter.InternalCallFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Mango Infra Web Auto Configuration
 * Provides Spring Web MVC configuration with sensible defaults
 *
 * @author Mango
 */
@AutoConfiguration
public class WebAutoConfiguration implements WebMvcConfigurer {

    /**
     * Register InternalCallFilter
     */
    @org.springframework.context.annotation.Bean
    public InternalCallFilter internalCallFilter(InternalCallFilter filter) {
        return filter;
    }

    /**
     * Configure CORS for REST APIs
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * Configure static resource handlers
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Default static resource handling
    }
}
