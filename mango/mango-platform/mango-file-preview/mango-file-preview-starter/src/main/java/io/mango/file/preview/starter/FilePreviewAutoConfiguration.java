package io.mango.file.preview.starter;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.file.preview.core.config.FilePreviewProperties;
import io.mango.file.preview.core.service.IFilePreviewService;
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

/**
 * 文件预览自动配置。
 */
@AutoConfiguration
@AutoConfigureAfter(name = "io.mango.auth.starter.AuthAutoConfiguration")
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
}
