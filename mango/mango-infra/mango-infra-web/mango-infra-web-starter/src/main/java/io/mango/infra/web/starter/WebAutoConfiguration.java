package io.mango.infra.web.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.mango.infra.kv.api.expression.KvContextContributor;
import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.web.api.IInternalPathProvider;
import io.mango.infra.web.api.IRequestContextProvider;
import io.mango.infra.web.filter.InternalCallFilter;
import io.mango.infra.web.filter.MangoContextWebFilter;
import io.mango.infra.web.filter.WebMdcFilter;
import io.mango.infra.web.support.AggregatingInternalPathProvider;
import io.mango.infra.web.support.InnerMappingInternalPathProvider;
import io.mango.infra.web.support.InnerMappingScanner;
import io.mango.infra.web.support.ServletRequestContextProvider;
import io.mango.infra.web.support.WebKvContextContributor;
import io.mango.infra.web.support.WebTraceIdResolver;
import io.mango.infra.web.util.JacksonUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Mango Infra Web 自动配置。
 */
@AutoConfiguration(before = JacksonAutoConfiguration.class)
@EnableScheduling
@EnableConfigurationProperties(MangoWebProperties.class)
public class WebAutoConfiguration implements WebMvcConfigurer {

    private static final String DATE_TIME_PATTERN = JacksonUtils.DATE_TIME_PATTERN;
    private static final String DATE_PATTERN = JacksonUtils.DATE_PATTERN;
    private static final String TIME_PATTERN = JacksonUtils.TIME_PATTERN;
    private static final String ASIA_SHANGHAI = JacksonUtils.ASIA_SHANGHAI;

    private final MangoWebProperties properties;

    public WebAutoConfiguration(MangoWebProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public WebTraceIdResolver webTraceIdResolver() {
        return new WebTraceIdResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean(name = "mangoLongToStringJacksonCustomizer")
    public Jackson2ObjectMapperBuilderCustomizer mangoLongToStringJacksonCustomizer() {
        // Snowflake IDs exceed JavaScript's safe integer range; Java time values must be HTTP strings.
        return builder -> builder
                .locale(Locale.CHINA)
                .timeZone(TimeZone.getTimeZone(ASIA_SHANGHAI))
                .simpleDateFormat(DATE_TIME_PATTERN)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .modules(JacksonUtils.javaTimeModule())
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }

    @Bean
    @ConditionalOnMissingBean(name = "mangoLongToStringObjectMapperPostProcessor")
    public BeanPostProcessor mangoLongToStringObjectMapperPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof ObjectMapper objectMapper) {
                    JacksonUtils.configure(objectMapper);
                }
                return bean;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(IRequestContextProvider.class)
    @ConditionalOnProperty(prefix = "mango.web.request-context", name = "enabled", havingValue = "true", matchIfMissing = true)
    public IRequestContextProvider requestContextProvider(WebTraceIdResolver traceIdResolver) {
        return new ServletRequestContextProvider(traceIdResolver);
    }

    @Bean
    @ConditionalOnClass(KvContextContributor.class)
    @ConditionalOnBean(IRequestContextProvider.class)
    public KvContextContributor webKvContextContributor(IRequestContextProvider requestContextProvider) {
        return new WebKvContextContributor(requestContextProvider);
    }

    @Bean
    @ConditionalOnProperty(prefix = "mango.web.inner", name = "enabled", havingValue = "true", matchIfMissing = true)
    public InnerMappingInternalPathProvider innerMappingInternalPathProvider() {
        return new InnerMappingInternalPathProvider();
    }

    @Bean
    @ConditionalOnBean(InnerMappingInternalPathProvider.class)
    public InnerMappingScanner innerMappingScanner(
                                                   @Qualifier("requestMappingHandlerMapping")
                                                   RequestMappingHandlerMapping handlerMapping,
                                                   InnerMappingInternalPathProvider provider) {
        return new InnerMappingScanner(handlerMapping, provider);
    }

    @Bean
    @Primary
    public IInternalPathProvider aggregatingInternalPathProvider(ObjectProvider<IInternalPathProvider> providers) {
        return new AggregatingInternalPathProvider(providers);
    }

    @Bean
    @ConditionalOnBean(IKvStore.class)
    public InternalCallFilter internalCallFilter(
            @Qualifier("aggregatingInternalPathProvider") IInternalPathProvider internalPathProvider,
            IKvStore kvStore,
            MangoWebProperties properties) {
        return new InternalCallFilter(internalPathProvider, kvStore, properties);
    }

    @Bean
    @ConditionalOnBean(IRequestContextProvider.class)
    @ConditionalOnProperty(prefix = "mango.web.context", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<MangoContextWebFilter> mangoContextWebFilter(IRequestContextProvider requestContextProvider) {
        FilterRegistrationBean<MangoContextWebFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new MangoContextWebFilter(requestContextProvider));
        registration.addUrlPatterns("/*");
        registration.setName("mangoContextWebFilter");
        registration.setOrder(Integer.MIN_VALUE + 5);
        return registration;
    }

    @Bean
    @ConditionalOnBean(IRequestContextProvider.class)
    @ConditionalOnProperty(prefix = "mango.web.mdc", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<WebMdcFilter> webMdcFilter() {
        FilterRegistrationBean<WebMdcFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new WebMdcFilter());
        registration.addUrlPatterns("/*");
        registration.setName("mangoWebMdcFilter");
        registration.setOrder(Integer.MIN_VALUE + 10);
        return registration;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setDateTimeFormatter(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
        registrar.setDateFormatter(DateTimeFormatter.ofPattern(DATE_PATTERN));
        registrar.setTimeFormatter(DateTimeFormatter.ofPattern(TIME_PATTERN));
        registrar.registerFormatters(registry);
        registry.addConverter(String.class, Long.class, value -> {
            if (value == null || value.isBlank()) {
                return null;
            }
            return Long.valueOf(value.trim());
        });
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        MangoWebProperties.Cors cors = properties.getCors();
        if (!cors.isEnabled()) {
            return;
        }
        registry.addMapping("/**")
                .allowedOriginPatterns(cors.getAllowedOriginPatterns().toArray(String[]::new))
                .allowedMethods(cors.getAllowedMethods().toArray(String[]::new))
                .allowedHeaders(cors.getAllowedHeaders().toArray(String[]::new))
                .allowCredentials(cors.isAllowCredentials())
                .maxAge(cors.getMaxAge());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 保留 Spring Boot 默认静态资源处理逻辑。
    }
}
