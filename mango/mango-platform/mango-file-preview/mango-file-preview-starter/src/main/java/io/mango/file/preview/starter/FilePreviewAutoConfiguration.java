package io.mango.file.preview.starter;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.file.preview.core.config.FilePreviewProperties;
import io.mango.file.preview.core.service.IFilePreviewService;
import io.mango.infra.kv.api.ITokenStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件预览自动配置。
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "io.mango.auth.starter.AuthAutoConfiguration",
        "io.mango.infra.kv.starter.KvCapabilityAutoConfiguration"
})
@ConditionalOnClass(IFilePreviewService.class)
@ConditionalOnProperty(prefix = "mango.file-preview", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(FilePreviewProperties.class)
@ComponentScan({
        "io.mango.file.preview.core",
        "io.mango.file.preview.starter",
        "cn.keking"
    })
public class FilePreviewAutoConfiguration {

    @Bean
    @ConditionalOnBean(ApiResourceApi.class)
    @ConditionalOnMissingBean
    public FilePreviewEngineResourceRegistrar filePreviewEngineResourceRegistrar(ApiResourceApi apiResourceApi) {
        return new FilePreviewEngineResourceRegistrar(apiResourceApi);
    }

    @Bean
    @ConditionalOnMissingBean
    public ITokenStore filePreviewTokenStore(Clock clock) {
        return new MemoryPreviewTokenStore(clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public static FilePreviewPermitPathBeanPostProcessor filePreviewPermitPathBeanPostProcessor() {
        return new FilePreviewPermitPathBeanPostProcessor();
    }

    @Bean
    @ConditionalOnMissingBean(name = "filePreviewSecurityCustomizer")
    public WebSecurityCustomizer filePreviewSecurityCustomizer() {
        return new FilePreviewSecurityCustomizer();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public SecurityFilterChain filePreviewPermitAllSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(FilePreviewFrameOptionsFilter.class)
    public FilterRegistrationBean<FilePreviewFrameOptionsFilter> filePreviewFrameOptionsFilter() {
        FilterRegistrationBean<FilePreviewFrameOptionsFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new FilePreviewFrameOptionsFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(FilePreviewStandaloneUiBlockFilter.class)
    public FilterRegistrationBean<FilePreviewStandaloneUiBlockFilter> filePreviewStandaloneUiBlockFilter(
            FilePreviewProperties properties) {
        FilterRegistrationBean<FilePreviewStandaloneUiBlockFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new FilePreviewStandaloneUiBlockFilter(properties));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.addUrlPatterns("/*");
        return registration;
    }

    private static class MemoryPreviewTokenStore implements ITokenStore {

        private final Clock clock;
        private final Map<String, TokenValue> values = new ConcurrentHashMap<>();

        MemoryPreviewTokenStore(Clock clock) {
            this.clock = clock;
        }

        @Override
        public void store(String token, String value, long ttlSeconds) {
            values.put(token, new TokenValue(value, Instant.now(clock).plusSeconds(ttlSeconds)));
        }

        @Override
        public String get(String token) {
            TokenValue tokenValue = values.get(token);
            if (tokenValue == null) {
                return null;
            }
            if (tokenValue.expiresAt().isBefore(Instant.now(clock))) {
                values.remove(token);
                return null;
            }
            return tokenValue.value();
        }

        @Override
        public void remove(String token) {
            values.remove(token);
        }

        private record TokenValue(String value, Instant expiresAt) {
        }
    }
}
