package io.mango.infra.log.starter;

import io.mango.infra.log.layout.TraceIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Log 自动配置
 * <p>
 * 功能：
 * 1. 将 traceId（SkyWalking/Micrometer）自动注入 MDC
 * 2. 确保日志中可以打印 traceId
 * </p>
 *
 * @author Mango
 */
@AutoConfiguration
@EnableConfigurationProperties(LogProperties.class)
public class LogAutoConfiguration {

    /**
     * TraceId 过滤器
     * <p>
     * 自动从各种 APM 系统提取 traceId 并放入 MDC：
     * <ul>
     *   <li>SkyWalking: org.apache.skywalking.apm.toolkit.trace.TraceContext</li>
     *   <li>Micrometer/OTEL: io.micrometer.tracing.Tracer</li>
     *   <li>Zipkin: brave.Tracer</li>
     * </ul>
     * </p>
     *
     * @param properties 日志配置
     * @return TraceId 过滤器注册 Bean
     */
    @Bean
    @ConditionalOnProperty(prefix = "mango.log.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter(LogProperties properties) {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceIdFilter(properties.getTrace().getHeaderName()));
        registration.addUrlPatterns("/*");
        registration.setName("traceIdFilter");
        registration.setOrder(Integer.MIN_VALUE);
        return registration;
    }
}
